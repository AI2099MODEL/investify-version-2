package com.example

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

object MarketEngine {
    private const val TAG = "MarketEngine"
    const val MAX_CONCURRENT_TRADES = 5
    const val VIRTUAL_ALLOCATION_PER_TRADE = 100000.0 // ₹1,00,000 per trade

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    private var monitoringJob: Job? = null

    val isEngineRunning = MutableStateFlow(false)
    val engineLogs = MutableStateFlow<List<String>>(emptyList())
    val lastScanTime = MutableStateFlow(0L)
    val isScanning = MutableStateFlow(false)

    // A flag to simulate market hours even if the actual Indian stock market is closed
    val isSimulationMode = MutableStateFlow(false)

    fun addLog(message: String) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val time = sdf.format(Date())
        val log = "[$time] $message"
        val current = engineLogs.value.toMutableList()
        current.add(0, log)
        if (current.size > 100) current.removeAt(current.size - 1)
        engineLogs.value = current
        Log.d(TAG, log)
    }

    fun startEngine(context: Context) {
        if (isEngineRunning.value) return
        isEngineRunning.value = true
        addLog("Virtual Trading Engine STARTED.")
        
        monitoringJob = scope.launch {
            // First run immediately
            try {
                runEngineCycle(context)
            } catch (e: Exception) {
                addLog("Error in initial cycle: ${e.localizedMessage}")
            }

            while (isActive && isEngineRunning.value) {
                delay(30000) // Run price updates and trade checks every 30 seconds
                try {
                    runEngineCycle(context)
                } catch (e: Exception) {
                    addLog("Error in engine cycle: ${e.localizedMessage}")
                }
            }
        }
    }

    fun stopEngine() {
        if (!isEngineRunning.value) return
        isEngineRunning.value = false
        monitoringJob?.cancel()
        monitoringJob = null
        addLog("Virtual Trading Engine STOPPED.")
    }

    suspend fun runEngineCycle(context: Context) = withContext(Dispatchers.IO) {
        val db = MyApplication.database
        
        // Timezone conversion for Indian Standard Time (IST)
        val cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+05:30"))
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val timeInMinutes = hour * 60 + minute

        val isWeekday = dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY
        val isHoliday = MarketUtils.isMarketHoliday(cal)
        val isMarketHours = isWeekday && !isHoliday && (timeInMinutes in 555..930) // 9:15 AM - 3:30 PM IST

        if (!isMarketHours) {
            addLog("Market is Closed. Auto Trader only runs during Market Hours (Mon-Fri, 9:15 AM - 3:30 PM IST, excluding holidays). Updating active trade prices to latest close...")
            // Update prices of existing ACTIVE trades so they are up-to-date with latest market close!
            val activeTrades = db.virtualTradeDao().getActiveTrades()
            if (activeTrades.isNotEmpty()) {
                activeTrades.map { trade ->
                    async {
                        try {
                            val res = YahooRetrofit.service.getChart(trade.ticker, "1d", "1m")
                            val chartResult = res.chart?.result?.firstOrNull()
                            val currentPrice = chartResult?.meta?.regularMarketPrice ?: trade.currentPrice
                            
                            val newHighest = max(trade.highestPrice, currentPrice)
                            val profitPct = ((currentPrice - trade.entryPrice) / trade.entryPrice) * 100.0
                            val profitAmt = currentPrice - trade.entryPrice
                            
                            val updatedTrade = trade.copy(
                                currentPrice = currentPrice,
                                highestPrice = newHighest,
                                profitPercent = profitPct,
                                profitAmount = profitAmt
                            )
                            db.virtualTradeDao().updateTrade(updatedTrade)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }.awaitAll()
            }
            return@withContext
        }

        addLog("Cycle active. Time: IST ${String.format("%02d:%02d", hour, minute)}. Active trades check...")

        // 1. Update prices of existing ACTIVE trades
        val activeTrades = db.virtualTradeDao().getActiveTrades()
        if (activeTrades.isNotEmpty()) {
            activeTrades.map { trade ->
                async {
                    try {
                        val res = YahooRetrofit.service.getChart(trade.ticker, "1d", "1m")
                        val chartResult = res.chart?.result?.firstOrNull()
                        val currentPrice = chartResult?.meta?.regularMarketPrice ?: trade.currentPrice
                        
                        val newHighest = max(trade.highestPrice, currentPrice)
                        val profitPct = ((currentPrice - trade.entryPrice) / trade.entryPrice) * 100.0
                        val profitAmt = currentPrice - trade.entryPrice // 1 Share calculation
                        
                        var updatedTrade = trade.copy(
                            currentPrice = currentPrice,
                            highestPrice = newHighest,
                            profitPercent = profitPct,
                            profitAmount = profitAmt
                        )

                        // Trailing SL logic: SL after 1% profit is achieved
                        var activeStopLoss = trade.stopLoss
                        val onePercentGain = trade.entryPrice * 1.01
                        
                        if (newHighest >= onePercentGain) {
                            // Once 1% is achieved, trailing Stop Loss shifts up.
                            // We set trailing SL to 0.5% below highest price reached, but minimum breakeven (entryPrice)
                            val trailingSL = newHighest * 0.995 // 0.5% trailing cushion
                            activeStopLoss = max(trade.entryPrice, trailingSL)
                            updatedTrade = updatedTrade.copy(stopLoss = activeStopLoss)
                        }

                        // Check Exit Conditions
                        val targetReached = currentPrice >= trade.entryPrice * 1.02 // 2% target
                        val slHit = currentPrice <= activeStopLoss

                        if (targetReached) {
                            val finalProfitPct = ((currentPrice - trade.entryPrice) / trade.entryPrice) * 100.0
                            val finalProfitAmt = currentPrice - trade.entryPrice // 1 Share calculation
                            updatedTrade = updatedTrade.copy(
                                status = "PROFIT_BOOKED",
                                exitPrice = currentPrice,
                                exitTime = System.currentTimeMillis(),
                                profitPercent = finalProfitPct,
                                profitAmount = finalProfitAmt
                            )
                            db.virtualTradeDao().updateTrade(updatedTrade)
                            addLog("🎉 BOOKED PROFIT (+2%) on ${trade.ticker} at ₹${String.format("%.2f", currentPrice)} (+${String.format("%.2f", finalProfitPct)}% for 1 Share)")
                        } else if (slHit) {
                            val finalProfitPct = ((currentPrice - trade.entryPrice) / trade.entryPrice) * 100.0
                            val finalProfitAmt = currentPrice - trade.entryPrice // 1 Share calculation
                            updatedTrade = updatedTrade.copy(
                                status = "STOP_LOSS",
                                exitPrice = currentPrice,
                                exitTime = System.currentTimeMillis(),
                                profitPercent = finalProfitPct,
                                profitAmount = finalProfitAmt
                            )
                            db.virtualTradeDao().updateTrade(updatedTrade)
                            addLog("📉 STOP LOSS HIT on ${trade.ticker} at ₹${String.format("%.2f", currentPrice)} (${String.format("%.2f", finalProfitPct)}% for 1 Share)")
                        } else {
                            db.virtualTradeDao().updateTrade(updatedTrade)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to update price for ${trade.ticker}: ${e.localizedMessage}")
                    }
                }
            }.awaitAll()
        }

        // 2. Square-off check at 3:15 PM IST
        val isSquareOffTime = (timeInMinutes in 915..930)
        if (isSquareOffTime && isWeekday && !isHoliday) {
            val remaining = db.virtualTradeDao().getActiveTrades()
            if (remaining.isNotEmpty()) {
                addLog("⏰ Auto Square-off Time Triggered: Squaring off all ${remaining.size} active trades...")
                remaining.forEach { trade ->
                    val profitPct = ((trade.currentPrice - trade.entryPrice) / trade.entryPrice) * 100.0
                    val profitAmt = trade.currentPrice - trade.entryPrice // 1 Share calculation
                    val squared = trade.copy(
                        status = "SQUARED_OFF",
                        exitPrice = trade.currentPrice,
                        exitTime = System.currentTimeMillis(),
                        profitPercent = profitPct,
                        profitAmount = profitAmt
                    )
                    db.virtualTradeDao().updateTrade(squared)
                    addLog("⏹️ Squared Off ${trade.ticker} at ₹${String.format("%.2f", trade.currentPrice)} (${String.format("%.2f", profitPct)}% for 1 Share)")
                }
                
                // Save profit log for today
                logDailyProfit(db)
            }
        }

        // 3. Scan for breakouts and add trades if we have open slots
        val currentActive = db.virtualTradeDao().getActiveTrades()
        val canEnter = currentActive.size < MAX_CONCURRENT_TRADES && (timeInMinutes < 15 * 60 || isSimulationMode.value)
        
        if (canEnter) {
            val emptySlots = MAX_CONCURRENT_TRADES - currentActive.size
            addLog("Slots available: $emptySlots. Starting breakout scanner...")
            isScanning.value = true
            try {
                // Fetch random Nifty 50/100 tickers to scan and prevent rate limits
                val scanTickers = StockScanner.NIFTY_TICKERS.shuffled().take(25)
                val breakoutCandidates = mutableListOf<ScanResult>()

                scanTickers.map { ticker ->
                    async {
                        val res = StockScanner.analyzeStock(ticker, "Breakouts", requireBullish = true)
                        if (res != null && res.score >= 65 && res.signalStrength.contains("STRONG")) {
                            breakoutCandidates.add(res)
                        }
                    }
                }.awaitAll()

                breakoutCandidates.sortByDescending { it.score }

                // Save breakout candidates to the database to keep the main list updated automatically!
                if (breakoutCandidates.isNotEmpty()) {
                    val dbBreakouts = breakoutCandidates.map { candidate ->
                        ScannedBreakout(
                            ticker = candidate.ticker,
                            name = candidate.name,
                            price = candidate.price,
                            strategies = candidate.strategies,
                            score = candidate.score,
                            reasons = candidate.reasons,
                            signalStrength = candidate.signalStrength,
                            stopLoss = candidate.stopLoss,
                            target1 = candidate.target1,
                            target2 = candidate.target2,
                            previousClose = candidate.previousClose,
                            openPrice = candidate.openPrice,
                            change = candidate.change,
                            changePercent = candidate.changePercent,
                            isBtst = candidate.isBtst,
                            scannedAt = System.currentTimeMillis()
                        )
                    }
                    db.scannedBreakoutDao().clearAll()
                    db.scannedBreakoutDao().insertBreakouts(dbBreakouts)
                    addLog("Background Scanner updated ${dbBreakouts.size} stocks in the Breakouts list.")
                }

                val toBuy = breakoutCandidates.take(emptySlots)

                toBuy.forEach { candidate ->
                    val currentActiveCheck = db.virtualTradeDao().getActiveTrades()
                    if (currentActiveCheck.none { it.ticker == candidate.ticker }) {
                        val newTrade = VirtualTrade(
                            ticker = candidate.ticker,
                            name = candidate.name,
                            entryPrice = candidate.price,
                            currentPrice = candidate.price,
                            entryTime = System.currentTimeMillis(),
                            status = "ACTIVE",
                            targetPrice = candidate.price * 1.02,
                            trailingSLThreshold = candidate.price * 1.01,
                            stopLoss = candidate.price * 0.99,
                            highestPrice = candidate.price
                        )
                        db.virtualTradeDao().insertTrade(newTrade)
                        addLog("🚀 AUTO TRADE EXECUTED: Buy ${candidate.ticker} at ₹${String.format("%.2f", candidate.price)} (Technical Score: ${candidate.score}/100)")
                    }
                }
                
                lastScanTime.value = System.currentTimeMillis()
            } catch (e: Exception) {
                addLog("Scanner error: ${e.localizedMessage}")
            } finally {
                isScanning.value = false
            }
        }
    }

    suspend fun logDailyProfit(db: AppDatabase) {
        val sdf = SimpleDateFormat("yyyy-MM-year", Locale.getDefault())
        val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        val allTrades = db.virtualTradeDao().getAllTradesList()
        val todayTrades = allTrades.filter { trade ->
            trade.exitTime != null && 
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(trade.exitTime)) == dateString
        }

        if (todayTrades.isEmpty()) return

        val totalProfitPct = todayTrades.sumOf { it.profitPercent } / todayTrades.size
        val totalProfitAmt = todayTrades.sumOf { it.profitAmount }

        val newLog = ProfitLog(
            timestamp = System.currentTimeMillis(),
            dateString = dateString,
            type = "DAILY",
            profitPercent = totalProfitPct,
            profitAmount = totalProfitAmt,
            tradeCount = todayTrades.size
        )
        db.profitLogDao().insertLog(newLog)
        addLog("📊 DAILY PROFIT LOGGED: ₹${String.format("%.2f", totalProfitAmt)} across ${todayTrades.size} trades.")

        // Also aggregate weekly and monthly statistics and update
        aggregateWeeklyMonthlyLogs(db)
    }

    suspend fun forceManualSquareOff(db: AppDatabase) {
        val remaining = db.virtualTradeDao().getActiveTrades()
        if (remaining.isNotEmpty()) {
            addLog("Manual Overrule: Squaring off all active trades...")
            remaining.forEach { trade ->
                val profitPct = ((trade.currentPrice - trade.entryPrice) / trade.entryPrice) * 100.0
                val profitAmt = trade.currentPrice - trade.entryPrice // 1 Share calculation
                val squared = trade.copy(
                    status = "SQUARED_OFF",
                    exitPrice = trade.currentPrice,
                    exitTime = System.currentTimeMillis(),
                    profitPercent = profitPct,
                    profitAmount = profitAmt
                )
                db.virtualTradeDao().updateTrade(squared)
                addLog("⏹️ Manually Squared Off ${trade.ticker} at ₹${String.format("%.2f", trade.currentPrice)} (${String.format("%.2f", profitPct)}% for 1 Share)")
            }
            logDailyProfit(db)
        }
    }

    suspend fun manualSquareOffSingleTrade(tradeId: Int, db: AppDatabase) {
        val trade = db.virtualTradeDao().getAllTradesList().firstOrNull { it.id == tradeId }
        if (trade != null && trade.status == "ACTIVE") {
            val profitPct = ((trade.currentPrice - trade.entryPrice) / trade.entryPrice) * 100.0
            val profitAmt = trade.currentPrice - trade.entryPrice // 1 Share calculation
            val updated = trade.copy(
                status = "SQUARED_OFF",
                exitPrice = trade.currentPrice,
                exitTime = System.currentTimeMillis(),
                profitPercent = profitPct,
                profitAmount = profitAmt
            )
            db.virtualTradeDao().updateTrade(updated)
            addLog("⏹️ Manually Squared Off ${trade.ticker} at ₹${String.format("%.2f", trade.currentPrice)} (${String.format("%.2f", profitPct)}% for 1 Share)")
            logDailyProfit(db)
        }
    }

    private suspend fun aggregateWeeklyMonthlyLogs(db: AppDatabase) {
        val allDailyLogs = db.profitLogDao().getAllLogsList().filter { it.type == "DAILY" }
        if (allDailyLogs.isEmpty()) return

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val now = System.currentTimeMillis()

        // 1. Weekly Aggregations (Last 7 days)
        val oneWeekAgo = now - (7L * 24 * 60 * 60 * 1000)
        val weeklyLogs = allDailyLogs.filter { it.timestamp >= oneWeekAgo }
        if (weeklyLogs.isNotEmpty()) {
            val weeklyPct = weeklyLogs.sumOf { it.profitPercent }
            val weeklyAmt = weeklyLogs.sumOf { it.profitAmount }
            db.profitLogDao().insertLog(
                ProfitLog(
                    timestamp = now,
                    dateString = SimpleDateFormat("'Week of' yyyy-MM-dd", Locale.getDefault()).format(Date()),
                    type = "WEEKLY",
                    profitPercent = weeklyPct,
                    profitAmount = weeklyAmt,
                    tradeCount = weeklyLogs.sumOf { it.tradeCount }
                )
            )
        }

        // 2. Monthly Aggregations (Last 30 days)
        val oneMonthAgo = now - (30L * 24 * 60 * 60 * 1000)
        val monthlyLogs = allDailyLogs.filter { it.timestamp >= oneMonthAgo }
        if (monthlyLogs.isNotEmpty()) {
            val monthlyPct = monthlyLogs.sumOf { it.profitPercent }
            val monthlyAmt = monthlyLogs.sumOf { it.profitAmount }
            db.profitLogDao().insertLog(
                ProfitLog(
                    timestamp = now,
                    dateString = SimpleDateFormat("'Month of' yyyy-MM", Locale.getDefault()).format(Date()),
                    type = "MONTHLY",
                    profitPercent = monthlyPct,
                    profitAmount = monthlyAmt,
                    tradeCount = monthlyLogs.sumOf { it.tradeCount }
                )
            )
        }
    }
}
