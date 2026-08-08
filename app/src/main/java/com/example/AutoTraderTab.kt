package com.example

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay

@Composable
fun AutoTraderTabContent(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val db = MyApplication.database

    val virtualTrades by db.virtualTradeDao().getAllTradesFlow().collectAsState(initial = emptyList())
    val profitLogs by db.profitLogDao().getAllLogsFlow().collectAsState(initial = emptyList())

    LaunchedEffect(Unit) {
        while (isActive) {
            try {
                withContext(Dispatchers.IO) {
                    MarketEngine.runEngineCycle(context)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            delay(15000) // Keep updating active trade prices every 15 seconds
        }
    }

    val activeTrades = virtualTrades.filter { it.status == "ACTIVE" }
    val closedTrades = virtualTrades.filter { it.status != "ACTIVE" }

    // Metrics calculations
    val totalClosed = closedTrades
    val profitBookedCount = totalClosed.count { it.status == "PROFIT_BOOKED" }
    val winRate = if (totalClosed.isNotEmpty()) (profitBookedCount.toDouble() / totalClosed.size * 100).toInt() else 0
    val netProfit = totalClosed.sumOf { it.profitAmount }
    
    val netInvested = activeTrades.sumOf { it.entryPrice }
    val totalInvestedClosed = totalClosed.sumOf { it.entryPrice }
    val netProfitPercent = if (totalInvestedClosed > 0) (netProfit / totalInvestedClosed) * 100.0 else 0.0

    var subTab by remember { mutableStateOf("ACTIVE") } // "ACTIVE", "CLOSED", "PERFORMANCE"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Metrics Summary Grid (2x2)
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Row 1: Net Profit & Win Rate
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Net Profit Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Net Profit / Loss", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "₹${String.format("%,.2f", netProfit)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (netProfit >= 0) Color(0xFF16A34A) else Color(0xFFDC2626)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${if (netProfit >= 0) "+" else ""}${String.format("%.2f", netProfitPercent)}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (netProfit >= 0) Color(0xFF16A34A) else Color(0xFFDC2626)
                            )
                        }
                    }

                    // Win Rate Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Win Rate", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$winRate%",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (winRate >= 50) Color(0xFF7C3AED) else Color(0xFF1E293B)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$profitBookedCount Wins / ${totalClosed.size} Trades",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }

                // Row 2: Net Invested & Closed Trades
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Net Invested Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Net Invested", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "₹${String.format("%,.2f", netInvested)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Active exposure",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }

                    // Closed Trades
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Closed Trades", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${totalClosed.size} Trades",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1E293B)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Total virtual completed",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }

                // Aligned active status below metrics row in small font
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF16A34A))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Auto Trading Status: Active",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A)
                    )
                }
            }
        }

        // Subtabs Selector
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    Triple("ACTIVE", "Active (${activeTrades.size})", Icons.Default.TrendingUp),
                    Triple("CLOSED", "Closed (${closedTrades.size})", Icons.Default.History),
                    Triple("PERFORMANCE", "Profit Logs", Icons.Default.BarChart)
                ).forEach { (key, label, icon) ->
                    val isSel = subTab == key
                    AssistChip(
                        onClick = { subTab = key },
                        label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(12.dp)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isSel) Color(0xFFEDE9FE) else Color.White,
                            labelColor = if (isSel) Color(0xFF7C3AED) else Color(0xFF64748B),
                            leadingIconContentColor = if (isSel) Color(0xFF7C3AED) else Color(0xFF64748B)
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSel) Color(0xFFC084FC) else Color(0xFFE2E8F0)
                        )
                    )
                }
            }
        }

        // Tab Specific Contents
        if (subTab == "ACTIVE") {
            if (activeTrades.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.AutoGraph, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No Active Trades Right Now", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("The automated system scans for breakouts and initiates virtual trades automatically during market hours starting at 9:00 AM.", fontSize = 11.sp, color = Color(0xFF64748B), textAlign = TextAlign.Center)
                        }
                    }
                }
            } else {
                items(activeTrades, key = { it.id }) { trade ->
                    ActiveTradeCardItem(trade = trade)
                }
            }
        } else if (subTab == "CLOSED") {
            if (closedTrades.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.HistoryToggleOff, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No Trades Closed Yet", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                        }
                    }
                }
            } else {
                items(closedTrades, key = { it.id }) { trade ->
                    ClosedTradeCardItem(trade = trade)
                }
            }
        } else if (subTab == "PERFORMANCE") {
            if (profitLogs.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.BarChart, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No Performance Logs Available", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Daily profit reports aggregated at 3:15 PM will be safely logged and shown here.", fontSize = 11.sp, color = Color(0xFF64748B), textAlign = TextAlign.Center)
                        }
                    }
                }
            } else {
                items(profitLogs, key = { it.id }) { log ->
                    PerformanceLogCardItem(log = log)
                }
            }
        }

        // SEBI Disclaimer card at the bottom of AutoTrader page
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "SEBI Disclaimer: I am not a SEBI registered analyst. All trade recommendations, automated/virtual trades, strategies, and content displayed here are strictly for educational and informational purposes only. Paper trading / virtual trading involves no real money, but actual trading involves market risks. Please consult a qualified financial advisor before making any investment decisions.",
                    fontSize = 9.5.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 13.sp,
                    modifier = Modifier.padding(10.dp),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ActiveTradeCardItem(trade: VirtualTrade) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(trade.ticker.replace(".NS", ""), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                    Text(trade.name, fontSize = 11.sp, color = Color(0xFF64748B), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                val pColor = if (trade.profitPercent >= 0) Color(0xFF16A34A) else Color(0xFFDC2626)
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${String.format("%.2f", trade.currentPrice)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "${if (trade.profitPercent >= 0) "+" else ""}${String.format("%.2f", trade.profitPercent)}% (₹${String.format("%.0f", trade.profitAmount)})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = pColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("ENTRY", fontSize = 9.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                    Text("₹${String.format("%.2f", trade.entryPrice)}", fontSize = 11.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("STOP LOSS", fontSize = 9.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                    Text("₹${String.format("%.2f", trade.stopLoss)}", fontSize = 11.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("TARGET (2%)", fontSize = 9.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                    Text("₹${String.format("%.2f", trade.targetPrice)}", fontSize = 11.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val lowerSL = trade.stopLoss
            val upperTarget = trade.targetPrice
            val totalSpan = upperTarget - lowerSL
            val pctVal = if (totalSpan > 0) ((trade.currentPrice - lowerSL) / totalSpan).toFloat() else 0.5f
            val boundedPct = pctVal.coerceIn(0f, 1f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFFE2E8F0))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(boundedPct)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFFEF4444), Color(0xFFFFD166), Color(0xFF22C55E))
                            )
                        )
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Trailing SL Bound", fontSize = 8.5.sp, color = Color(0xFF94A3B8))
                Text("Target Peak (2%)", fontSize = 8.5.sp, color = Color(0xFF94A3B8))
            }
        }
    }
}

@Composable
fun ClosedTradeCardItem(trade: VirtualTrade) {
    val exitLabel = when (trade.status) {
        "PROFIT_BOOKED" -> "PROFIT BOOKED"
        "STOP_LOSS" -> "STOP LOSS HIT"
        else -> "SQUARED OFF"
    }
    
    val badgeColor = when (trade.status) {
        "PROFIT_BOOKED" -> Color(0xFFDCFCE7)
        "STOP_LOSS" -> Color(0xFFFEE2E2)
        else -> Color(0xFFF1F5F9)
    }

    val badgeTextColor = when (trade.status) {
        "PROFIT_BOOKED" -> Color(0xFF15803D)
        "STOP_LOSS" -> Color(0xFFB91C1C)
        else -> Color(0xFF475569)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(trade.ticker.replace(".NS", ""), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                    Text(trade.name, fontSize = 10.5.sp, color = Color(0xFF64748B))
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeColor)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(exitLabel, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = badgeTextColor)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Buy: ₹${String.format("%.2f", trade.entryPrice)}", fontSize = 11.sp, color = Color(0xFF64748B))
                        Text("➔", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        Text("Exit: ₹${String.format("%.2f", trade.exitPrice ?: trade.currentPrice)}", fontSize = 11.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    trade.exitTime?.let {
                        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                        Text("Closed: ${sdf.format(Date(it))}", fontSize = 9.5.sp, color = Color(0xFF94A3B8))
                    }
                }

                val pColor = if (trade.profitPercent >= 0) Color(0xFF16A34A) else Color(0xFFDC2626)
                Text(
                    text = "${if (trade.profitPercent >= 0) "+" else ""}${String.format("%.2f", trade.profitPercent)}% (₹${String.format("%,.0f", trade.profitAmount)})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = pColor
                )
            }
        }
    }
}

@Composable
fun PerformanceLogCardItem(log: ProfitLog) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when (log.type) {
                                "DAILY" -> Color(0xFFEDE9FE)
                                "WEEKLY" -> Color(0xFFE0F2FE)
                                else -> Color(0xFFFEF3C7)
                            }
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(log.type, fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold, color = when (log.type) {
                        "DAILY" -> Color(0xFF7C3AED)
                        "WEEKLY" -> Color(0xFF0284C7)
                        else -> Color(0xFFD97706)
                    })
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(log.dateString, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                Text("Aggregated over ${log.tradeCount} trades", fontSize = 10.sp, color = Color(0xFF64748B))
            }

            val pColor = if (log.profitAmount >= 0) Color(0xFF16A34A) else Color(0xFFDC2626)
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (log.profitAmount >= 0) "+" else ""}${String.format("%.2f", log.profitPercent)}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = pColor
                )
                Text(
                    text = "₹${String.format("%,.2f", log.profitAmount)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = pColor
                )
            }
        }
    }
}