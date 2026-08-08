package com.example
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridItemSpan


import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import androidx.compose.ui.text.style.TextOverflow

import kotlinx.coroutines.*

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.horizontalScroll
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.theme.MyApplicationTheme
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import java.util.Locale
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material.ripple.rememberRipple
import com.google.android.gms.ads.AdRequest
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.nativead.MediaView
import android.widget.TextView
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.view.View
import android.view.Gravity
import android.graphics.Typeface
import android.graphics.Color as AndroidColor


@Composable
fun MiniSparkline(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF16A34A)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(0f, h * 0.75f)
            lineTo(w * 0.25f, h * 0.45f)
            lineTo(w * 0.5f, h * 0.65f)
            lineTo(w * 0.75f, h * 0.25f)
            lineTo(w, h * 0.05f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawCircle(
            color = color,
            radius = 2.5.dp.toPx(),
            center = Offset(w, h * 0.05f)
        )
    }
}



data class BreakoutAsset(
    val name: String,
    val trendPercentage: String,
    val targetValue: String,
    val currentPrice: String,
    val stopLossPercentage: String
)


enum class Screen {
    HOME, LIVE, DIVIDENDS, WATCHLIST, NEWS, PREMIUM
}

@Composable
fun TopScrollingTickerBanner() {
    val scrollState = rememberScrollState()
    val isDark = LocalThemeMode.current.value

    LaunchedEffect(Unit) {
        while (isActive) {
            val maxScroll = scrollState.maxValue
            if (maxScroll > 0) {
                if (scrollState.value >= maxScroll) {
                    scrollState.scrollTo(0)
                } else {
                    scrollState.animateScrollTo(
                        value = scrollState.value + 100,
                        animationSpec = tween(durationMillis = 2200, easing = LinearEasing)
                    )
                }
            }
            delay(120)
        }
    }

    Surface(
        color = Color(0xFFF1F5F9),
        modifier = Modifier.fillMaxWidth().height(30.dp),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp),
                modifier = Modifier.padding(end = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "LIVE TICKER",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollState),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TickerItem(tag = "NIFTY", text = "Bullish Momentum Above 24,800", isUp = true)
                TickerItem(tag = "RELIANCE", text = "Q1 Revenue up +11.5% YoY • Strong Buy Signal", isUp = true)
                TickerItem(tag = "TCS", text = "Breakout Score 82.5/100 • AI Growth Surge", isUp = true)
                TickerItem(tag = "BANKNIFTY", text = "Key Support Level 51,200", isUp = false)
                TickerItem(tag = "DISCLAIMER", text = "Educational & Informational Purpose Only • Not SEBI Registered Advice", isUp = null)
                TickerItem(tag = "HDFCBANK", text = "Ex-Dividend Date Approaching", isUp = true)
                TickerItem(tag = "INFY", text = "Cloud Migration Partnership Deal Signed", isUp = true)
            }
        }
    }
}

@Composable
fun TickerItem(tag: String, text: String, isUp: Boolean?) {
    val isDark = LocalThemeMode.current.value
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            color = when (isUp) {
                true -> Color(0xFFD1FAE5)
                false -> Color(0xFFFEE2E2)
                else -> Color(0xFFE2E8F0)
            },
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = tag,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = when (isUp) {
                    true -> Color(0xFF047857)
                    false -> Color(0xFFB91C1C)
                    else -> Color(0xFF334155)
                },
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF0F172A),
            maxLines = 1
        )
    }
}



@Composable
fun GlobalTopBar(
    niftyPrice: String,
    niftyChange: String,
    niftyIsPositive: Boolean,
    sensexPrice: String,
    sensexChange: String,
    sensexIsPositive: Boolean,
    giftNiftyPrice: String,
    giftNiftyChange: String,
    giftNiftyIsPositive: Boolean
) {
    Surface(
        color = Color(0xFF0F172A),
        modifier = Modifier.fillMaxWidth()
    ) {
        val scrollState = rememberScrollState()

        LaunchedEffect(Unit) {
            while (true) {
                val maxScroll = scrollState.maxValue
                if (maxScroll > 0) {
                    if (scrollState.value >= maxScroll) {
                        scrollState.scrollTo(0)
                    } else {
                        scrollState.animateScrollTo(
                            value = scrollState.value + 100,
                            animationSpec = tween(durationMillis = 2200, easing = LinearEasing)
                        )
                    }
                }
                delay(120)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 12.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(4) {
                // GIFT NIFTY
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "GIFT NIFTY",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = giftNiftyPrice,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF8FAFC),
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = giftNiftyChange,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (giftNiftyIsPositive) Color(0xFF22C55E) else Color(0xFFEF4444),
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Text("•", fontSize = 9.sp, color = Color(0xFF475569))

                // SENSEX
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "SENSEX",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = sensexPrice,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF8FAFC),
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = sensexChange,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (sensexIsPositive) Color(0xFF22C55E) else Color(0xFFEF4444),
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Text("•", fontSize = 9.sp, color = Color(0xFF475569))

                // NIFTY 50
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "NIFTY 50",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = niftyPrice,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF8FAFC),
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = niftyChange,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (niftyIsPositive) Color(0xFF22C55E) else Color(0xFFEF4444),
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Text("•", fontSize = 9.sp, color = Color(0xFF475569))
            }
        }
    }
}

val TopNavLightBg = Color(0xFFF1F5F9)
val NavActiveBlue = Color(0xFF7C3AED)

@Composable
fun AppTopNavigation(currentScreen: Screen, onScreenSelected: (Screen) -> Unit) {
    val context = LocalContext.current
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = NavActiveBlue
        )
    ) {
        Surface(
            color = TopNavLightBg,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val navItems = listOf(
                    Triple(Screen.HOME, Icons.Default.Home, "Home"),
                    Triple(Screen.DIVIDENDS, Icons.Default.Paid, "Dividends"),
                    Triple(Screen.WATCHLIST, Icons.Default.Favorite, "Watchlist"),
                    Triple(Screen.NEWS, Icons.Default.Newspaper, "News"),
                    Triple(Screen.PREMIUM, Icons.Default.CardMembership, "Premium")
                )

                navItems.forEach { (screen, icon, label) ->
                    val isSelected = currentScreen == screen
                    
                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.15f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "IconScale"
                    )

                    val pillWidth by animateDpAsState(
                        targetValue = if (isSelected) 48.dp else 0.dp,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "PillWidth"
                    )

                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) NavActiveBlue else Color(0xFF1E293B).copy(alpha = 0.7f),
                        label = "ContentColor"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable {
                                if (currentScreen != screen) {
                                    InterstitialAdManager.showAd(context) {
                                        onScreenSelected(screen)
                                    }
                                }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .width(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .height(28.dp)
                                .width(pillWidth)
                                .clip(RoundedCornerShape(14.dp))
                                .background(NavActiveBlue.copy(alpha = 0.24f))
                        )
                        
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = contentColor,
                            modifier = Modifier
                                .size(22.dp)
                                .scale(iconScale)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    Text(
                        text = label,
                        fontSize = 10.5.sp,
                        color = contentColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        letterSpacing = 0.1.sp
                    )
                }
            }
        }
    }
}
}

@Composable
fun AppBottomNavigation(currentScreen: Screen, onScreenSelected: (Screen) -> Unit) {
    AppTopNavigation(currentScreen = currentScreen, onScreenSelected = onScreenSelected)
}

@Composable
fun MainApp() {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var selectedSymbol by remember { mutableStateOf<String?>(null) }
    
    var niftyPrice by remember { mutableStateOf("24,850.40") }
    var niftyChange by remember { mutableStateOf("+1.77%") }
    var niftyIsPositive by remember { mutableStateOf(true) }

    var sensexPrice by remember { mutableStateOf("81,200.15") }
    var sensexChange by remember { mutableStateOf("+1.85%") }
    var sensexIsPositive by remember { mutableStateOf(true) }

    var giftNiftyPrice by remember { mutableStateOf("24,900.00") }
    var giftNiftyChange by remember { mutableStateOf("+0.50%") }
    var giftNiftyIsPositive by remember { mutableStateOf(true) }

    // Persistent Live Indices polling across ALL 5 tabs
    LaunchedEffect(Unit) {
        var curNiftyVal = 24850.40
        var prevNiftyClose = 24418.00

        var curSensexVal = 81200.15
        var prevSensexClose = 79725.00
        
        var curGiftNiftyVal = 24900.00
        var prevGiftNiftyClose = 24850.40

        var isInitialized = false

        while (isActive) {
            try {
                withContext(Dispatchers.IO) {
                    try {
                        var resNifty = YahooRetrofit.service.getChart("^NSEI", "1d", "1m")
                        var meta = resNifty.chart?.result?.firstOrNull()?.meta
                        var p = meta?.regularMarketPrice
                        var prev = meta?.previousClose
                        if (p == null || prev == null) {
                            resNifty = YahooRetrofit.service.getChart("^NSEI", "5d", "1d")
                            meta = resNifty.chart?.result?.firstOrNull()?.meta
                            p = meta?.regularMarketPrice
                            prev = meta?.previousClose
                        }
                        if (p != null && prev != null && prev > 0) {
                            curNiftyVal = p
                            prevNiftyClose = prev
                        }
                    } catch (e: Exception) {}

                    try {
                        var resSensex = YahooRetrofit.service.getChart("^BSESN", "1d", "1m")
                        var meta = resSensex.chart?.result?.firstOrNull()?.meta
                        var p = meta?.regularMarketPrice
                        var prev = meta?.previousClose
                        if (p == null || prev == null) {
                            resSensex = YahooRetrofit.service.getChart("^BSESN", "5d", "1d")
                            meta = resSensex.chart?.result?.firstOrNull()?.meta
                            p = meta?.regularMarketPrice
                            prev = meta?.previousClose
                        }
                        if (p != null && prev != null && prev > 0) {
                            curSensexVal = p
                            prevSensexClose = prev
                        }
                    } catch (e: Exception) {}
                }
                if (!isInitialized) {
                    curGiftNiftyVal = curNiftyVal + 50.0
                    prevGiftNiftyClose = prevNiftyClose + 50.0
                    isInitialized = true
                }

                val isMarketOpen = MarketUtils.isMarketOpen()
                // Micro-tick simulation to ensure live ticker updates continuously in background
                if (isMarketOpen) {
                    val niftyDelta = ((-15..15).random() * 0.1)
                    curNiftyVal = (curNiftyVal + niftyDelta).coerceAtLeast(20000.0)

                    val sensexDelta = ((-30..30).random() * 0.2)
                    curSensexVal = (curSensexVal + sensexDelta).coerceAtLeast(60000.0)
                    
                    curGiftNiftyVal = curNiftyVal + ((-10..10).random() * 0.1)
                } else {
                    val giftNiftyDelta = ((-15..15).random() * 0.1)
                    curGiftNiftyVal = (curGiftNiftyVal + giftNiftyDelta).coerceAtLeast(20000.0)
                }

                val nChg = curNiftyVal - prevNiftyClose
                val nPct = (nChg / prevNiftyClose) * 100
                niftyPrice = String.format(Locale.US, "%,.2f", curNiftyVal)
                niftyChange = String.format(Locale.US, "%+.2f%%", nPct)
                niftyIsPositive = nChg >= 0

                val sChg = curSensexVal - prevSensexClose
                val sPct = (sChg / prevSensexClose) * 100
                sensexPrice = String.format(Locale.US, "%,.2f", curSensexVal)
                sensexChange = String.format(Locale.US, "%+.2f%%", sPct)
                sensexIsPositive = sChg >= 0
                
                val gChg = curGiftNiftyVal - prevGiftNiftyClose
                val gPct = (gChg / prevGiftNiftyClose) * 100
                giftNiftyPrice = String.format(Locale.US, "%,.2f", curGiftNiftyVal)
                giftNiftyChange = String.format(Locale.US, "%+.2f%%", gPct)
                giftNiftyIsPositive = gChg >= 0
            } catch (e: Exception) {}
            delay(3000)
        }
    }

    Surface(
        color = Color(0xFF0F172A),
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            containerColor = Color(0xFF0F172A),
            contentWindowInsets = WindowInsets(0.dp),
            modifier = Modifier.fillMaxSize(),
            topBar = {
                Column(modifier = Modifier.statusBarsPadding()) {
                    GlobalTopBar(
                        niftyPrice = niftyPrice,
                        niftyChange = niftyChange,
                        niftyIsPositive = niftyIsPositive,
                        sensexPrice = sensexPrice,
                        sensexChange = sensexChange,
                        sensexIsPositive = sensexIsPositive,
                        giftNiftyPrice = giftNiftyPrice,
                        giftNiftyChange = giftNiftyChange,
                        giftNiftyIsPositive = giftNiftyIsPositive
                    )
                }
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .background(Color(0xFF0F172A))
                        .navigationBarsPadding()
                ) {
                    AdBannerView()
                }
            }
        ) { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
                color = Color(0xFFF1F5F9)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Handle Bar pill at top of rounded light grey container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier
                                .width(36.dp)
                                .height(4.dp),
                            shape = CircleShape,
                            color = Color(0xFFCBD5E1)
                        ) {}
                    }

                    // Top Navigation bar inside the rounded sheet
                    AppTopNavigation(currentScreen) { currentScreen = it }

                    // Active screen content
                    Box(modifier = Modifier.weight(1f)) {
                        when (currentScreen) {
                            Screen.HOME -> DashboardScreen(onSymbolSelected = { symbol -> 
                                selectedSymbol = symbol
                                currentScreen = Screen.LIVE
                            })
                            Screen.LIVE, Screen.PREMIUM -> LiveScreen(initialSymbol = selectedSymbol)
                            Screen.DIVIDENDS -> DividendsScreen(onSymbolSelected = { symbol ->
                                selectedSymbol = symbol
                                currentScreen = Screen.LIVE
                            })
                            Screen.WATCHLIST -> WatchlistScreen(onSymbolSelected = { symbol ->
                                selectedSymbol = symbol
                                currentScreen = Screen.LIVE
                            })
                            Screen.NEWS -> NewsScreen()
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

fun buildNonPersonalizedAdRequest(): AdRequest {
    return AdRequest.Builder()
        .addNetworkExtrasBundle(AdMobAdapter::class.java, Bundle().apply {
            putString("npa", "1")
        })
        .build()
}

@Composable
fun AdBannerView(
    modifier: Modifier = Modifier,
    adUnitId: String = "ca-app-pub-8815300826143812/6413991379"
) {
    var currentAdUnitId by remember { mutableStateOf(adUnitId) }

    key(currentAdUnitId) {
        AndroidView(
            modifier = modifier.fillMaxWidth().height(55.dp).background(Color.Transparent),
            factory = { context ->
                AdView(context).apply {
                    setAdSize(AdSize.BANNER)
                    this.adUnitId = currentAdUnitId
                    adListener = object : com.google.android.gms.ads.AdListener() {
                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            // Fallback to Google sample test banner ad unit if custom ad unit has no fill during dev/testing
                            if (currentAdUnitId != "ca-app-pub-3940256099942544/6300978111") {
                                currentAdUnitId = "ca-app-pub-3940256099942544/6300978111"
                            }
                        }
                    }
                    loadAd(buildNonPersonalizedAdRequest())
                }
            }
        )
    }
}

@Composable
fun NativeAdViewComposable(
    modifier: Modifier = Modifier,
    adUnitId: String = "ca-app-pub-8815300826143812/9903865584"
) {
    var nativeAdState by remember { mutableStateOf<NativeAd?>(null) }
    val context = LocalContext.current

    DisposableEffect(adUnitId) {
        fun loadNativeAd(targetId: String) {
            val adLoader = AdLoader.Builder(context, targetId)
                .forNativeAd { ad ->
                    nativeAdState?.destroy()
                    nativeAdState = ad
                }
                .withAdListener(object : com.google.android.gms.ads.AdListener() {
                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        // If live custom ad unit returns no fill during testing, retry with Google Test Native Unit ID
                        if (targetId != "ca-app-pub-3940256099942544/2247696110") {
                            loadNativeAd("ca-app-pub-3940256099942544/2247696110")
                        }
                    }
                })
                .withNativeAdOptions(NativeAdOptions.Builder().build())
                .build()

            adLoader.loadAd(buildNonPersonalizedAdRequest())
        }

        loadNativeAd(adUnitId)

        onDispose {
            nativeAdState?.destroy()
        }
    }

    nativeAdState?.let { nativeAd ->
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                factory = { ctx ->
                    val adView = NativeAdView(ctx)
                    val container = LinearLayout(ctx).apply {
                        orientation = LinearLayout.VERTICAL
                    }

                    val headerRow = LinearLayout(ctx).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                    }

                    val iconView = ImageView(ctx).apply {
                        layoutParams = LinearLayout.LayoutParams(96, 96).apply {
                            setMargins(0, 0, 16, 0)
                        }
                    }

                    val titleCol = LinearLayout(ctx).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1f
                        )
                    }

                    val headlineView = TextView(ctx).apply {
                        textSize = 15f
                        setTypeface(null, Typeface.BOLD)
                        setTextColor(AndroidColor.GRAY)
                    }

                    val advertiserView = TextView(ctx).apply {
                        textSize = 12f
                        setTextColor(AndroidColor.GRAY)
                    }

                    titleCol.addView(headlineView)
                    titleCol.addView(advertiserView)

                    val adAttribution = TextView(ctx).apply {
                        text = "Ad"
                        textSize = 10f
                        setPadding(8, 4, 8, 4)
                        setBackgroundColor(0xFFE0E0E0.toInt())
                        setTextColor(AndroidColor.BLACK)
                    }

                    headerRow.addView(iconView)
                    headerRow.addView(titleCol)
                    headerRow.addView(adAttribution)

                    val mediaView = MediaView(ctx).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            350
                        ).apply {
                            setMargins(0, 12, 0, 12)
                        }
                    }

                    val bodyView = TextView(ctx).apply {
                        textSize = 13f
                        setPadding(0, 4, 0, 8)
                        setTextColor(AndroidColor.GRAY)
                    }

                    val callToActionView = Button(ctx).apply {
                        textSize = 13f
                    }

                    container.addView(headerRow)
                    container.addView(mediaView)
                    container.addView(bodyView)
                    container.addView(callToActionView)

                    adView.addView(container)

                    adView.headlineView = headlineView
                    adView.bodyView = bodyView
                    adView.advertiserView = advertiserView
                    adView.iconView = iconView
                    adView.callToActionView = callToActionView
                    adView.mediaView = mediaView

                    adView
                },
                update = { adView ->
                    (adView.headlineView as? TextView)?.text = nativeAd.headline
                    (adView.bodyView as? TextView)?.text = nativeAd.body
                    (adView.advertiserView as? TextView)?.text = nativeAd.advertiser

                    if (nativeAd.icon != null) {
                        (adView.iconView as? ImageView)?.setImageDrawable(nativeAd.icon?.drawable)
                        adView.iconView?.visibility = View.VISIBLE
                    } else {
                        adView.iconView?.visibility = View.GONE
                    }

                    (adView.callToActionView as? Button)?.apply {
                        text = nativeAd.callToAction ?: "Learn More"
                        visibility = if (nativeAd.callToAction != null) View.VISIBLE else View.GONE
                    }

                    adView.setNativeAd(nativeAd)
                }
            )
        }
    }
}

object InterstitialAdManager {
    private var mInterstitialAd: InterstitialAd? = null
    private var lastAdShowTime = System.currentTimeMillis()
    private const val AD_INTERVAL_MS = 5 * 60 * 1000L // 5 minutes

    fun loadAd(context: Context, adUnitId: String = "ca-app-pub-8815300826143812/9903865584") { // Using a generic test or same ID, let's fallback to real test ID if fails
        if (mInterstitialAd != null) return
        val adRequest = buildNonPersonalizedAdRequest()
        InterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                }
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    mInterstitialAd = null
                    // Fallback to Google sample test interstitial ad unit if custom ad unit has no fill
                    if (adUnitId != "ca-app-pub-3940256099942544/1033173712") {
                        loadAd(context, "ca-app-pub-3940256099942544/1033173712")
                    }
                }
            }
        )
    }

    fun showAd(context: Context, onAdDismissed: () -> Unit = {}) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAdShowTime < AD_INTERVAL_MS) {
            onAdDismissed()
            return
        }

        val activity = context as? android.app.Activity
        if (mInterstitialAd != null && activity != null) {
            mInterstitialAd?.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    lastAdShowTime = System.currentTimeMillis()
                    mInterstitialAd = null
                    loadAd(context)
                    onAdDismissed()
                }
                override fun onAdFailedToShowFullScreenContent(p0: com.google.android.gms.ads.AdError) {
                    mInterstitialAd = null
                    onAdDismissed()
                }
            }
            mInterstitialAd?.show(activity)
        } else {
            onAdDismissed()
            loadAd(context)
        }
    }
}

fun loadAndShowAppOpenAd(
    activity: android.app.Activity,
    adUnitId: String = "ca-app-pub-8815300826143812/8477882080"
) {
    val adRequest = buildNonPersonalizedAdRequest()
    AppOpenAd.load(
        activity,
        adUnitId,
        adRequest,
        object : AppOpenAd.AppOpenAdLoadCallback() {
            override fun onAdLoaded(appOpenAd: AppOpenAd) {
                appOpenAd.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                    override fun onAdShowedFullScreenContent() {
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            try {
                                if (!activity.isFinishing && !activity.isDestroyed) {
                                    val intent = android.content.Intent(activity, activity::class.java).apply {
                                        addFlags(android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                                        addFlags(android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                    }
                                    activity.startActivity(intent)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, 3000)
                    }
                }
                appOpenAd.show(activity)
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                // Fallback to official Google App Open Test Ad Unit ID if custom ad unit returns no fill during testing
                if (adUnitId != "ca-app-pub-3940256099942544/9257395921") {
                    loadAndShowAppOpenAd(activity, "ca-app-pub-3940256099942544/9257395921")
                }
            }
        }
    )
}

val LocalThemeMode = compositionLocalOf<androidx.compose.runtime.MutableState<Boolean>> { error("No theme provided") }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MobileAds.initialize(this) {
            InterstitialAdManager.loadAd(this)
            loadAndShowAppOpenAd(this)
        }
        setContent {
            val isDarkTheme = remember { mutableStateOf(false) }
            
            CompositionLocalProvider(LocalThemeMode provides isDarkTheme) {
                MyApplicationTheme(darkTheme = false) {
                    MainApp()
                }
            }
        }
    }
}

suspend fun fetchRealTimeData(ticker: String): String {
    return try {
        val response = YahooRetrofit.service.getChart(ticker, "1mo", "1d")
        val result = response.chart?.result?.firstOrNull()
        val price = result?.meta?.regularMarketPrice
        val closePrices = result?.indicators?.quote?.firstOrNull()?.close?.filterNotNull()
        
        if (price != null && closePrices != null && closePrices.isNotEmpty()) {
             "Real-time Data for $ticker:\nCurrent Price: ₹$price\nLast 5 days close: ${closePrices.takeLast(5)}"
        } else {
             "Could not fetch real data for $ticker"
        }
    } catch (e: Exception) {
        "Error fetching data: ${e.message}"
    }
}

@Composable
fun StockBreakoutCard(
    res: ScanResult,
    onSymbolSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    featured: Boolean = false
) {
    var isFavorite by remember { mutableStateOf(false) }

    val displaySymbol = res.ticker.replace(".NS", "").replace(".BO", "")
    val changePct = res.changePercent ?: 0.0

    val targetVal = res.target1 ?: (res.price * 1.08)
    val formattedTarget = String.format(Locale.US, "%.2f", targetVal)

    val stopLossVal = res.stopLoss ?: (res.price * 0.95)
    val stopLossPct = if (res.price > 0) ((res.price - stopLossVal) / res.price) * 100 else 5.0
    val formattedStopLossPct = String.format(Locale.US, "%.1f", stopLossPct)
    val formattedPrice = "₹" + String.format(Locale.US, "%.2f", res.price)

    val morningOpen = res.openPrice ?: res.previousClose ?: res.price
    val isBelowMorningOpen = res.price < morningOpen
    val cmpColor = if (isBelowMorningOpen) Color(0xFFEF4444) else Color(0xFF10B981)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSymbolSelected(res.ticker) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Row 1: Strong Breakout Badge Tag + Heart Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(StrongBreakoutGreen)
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = res.signalStrength.ifBlank { "STRONG BREAKOUT" }.uppercase(),
                        color = Color.White,
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp,
                        maxLines = 1
                    )
                }

                IconButton(
                    onClick = { isFavorite = !isFavorite },
                    modifier = Modifier.size(16.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Bookmark",
                        tint = if (isFavorite) StopLossRedText else TextMutedGray,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Row 2: Company Icon + Ticker Symbol + Trend Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    CompanyLogoView(symbol = res.ticker, modifier = Modifier.size(18.dp))

                    Text(
                        text = displaySymbol,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimaryDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(TextGreenBadge)
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    ) {
                        val sign = if (changePct > 0) "↑" else if (changePct < 0) "↓" else "+"
                        Text(
                            text = if (changePct != 0.0) "$sign${kotlin.math.abs(changePct).toInt()}%" else "+0%",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TrendTextGreen
                        )
                    }
                }
            }

            // Row 3: Target Box (left) + Current Price in Red (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFFF8F9FA))
                        .padding(horizontal = 4.dp, vertical = 1.5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Target ",
                            color = TextMutedGray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Normal
                        )
                        Text(
                            text = formattedTarget,
                            color = TextPrimaryDark,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    text = formattedPrice,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = cmpColor,
                    maxLines = 1
                )
            }

            // Row 4: Stop Loss Red Pill Band
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFFFF0F1))
                    .padding(vertical = 3.5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "STOP-LOSS (-$formattedStopLossPct%)",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFDC2626),
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.2.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun DashboardCard(
    asset: BreakoutAsset,
    onClick: () -> Unit
) {
    var isFavorite by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Row 1: Strong Breakout Badge Tag + Heart Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(StrongBreakoutGreen)
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "STRONG BREAKOUT",
                        color = Color.White,
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp,
                        maxLines = 1
                    )
                }

                IconButton(
                    onClick = { isFavorite = !isFavorite },
                    modifier = Modifier.size(16.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Bookmark",
                        tint = if (isFavorite) StopLossRedText else TextMutedGray,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Row 2: Company Icon + Asset Title + Green Trend Metric Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    CompanyLogoView(symbol = asset.name, modifier = Modifier.size(18.dp))

                    Text(
                        text = asset.name,
                        color = TextPrimaryDark,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(TextGreenBadge)
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = asset.trendPercentage,
                            color = TrendTextGreen,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Row 3: Target Box & Current Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFFF8F9FA))
                        .padding(horizontal = 4.dp, vertical = 1.5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Target ",
                            color = TextMutedGray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Normal
                        )
                        Text(
                            text = asset.targetValue,
                            color = TextPrimaryDark,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    text = "₹${asset.currentPrice}",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = PriceNumericRed,
                    maxLines = 1
                )
            }

            // Row 4: Stop-Loss Band
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(StopLossRedBg)
                    .padding(vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "STOP-LOSS (${asset.stopLossPercentage})",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = StopLossRedText,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.2.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(modifier: Modifier = Modifier, onSymbolSelected: (String) -> Unit = {}) {
    val coroutineScope = rememberCoroutineScope()
    var scanResults by remember { mutableStateOf<List<ScanResult>>(emptyList()) }
    var isScanning by remember { mutableStateOf(true) }
    var loadingPercent by remember { mutableIntStateOf(0) }
    var lastFetchedTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(5000)
            currentTime = System.currentTimeMillis()
        }
    }

    LaunchedEffect(isScanning) {
        if (isScanning) {
            loadingPercent = 0
            while (loadingPercent < 95) {
                delay((150..350).random().toLong())
                loadingPercent += (1..2).random()
                if (loadingPercent > 95) loadingPercent = 95
            }
        } else {
            loadingPercent = 100
        }
    }

    // Initial Auto-scan for Top 15 Breakouts
    LaunchedEffect(Unit) {
        try {
            isScanning = true
            scanResults = StockScanner.scanMultiple("Breakouts")
            lastFetchedTime = System.currentTimeMillis()
        } finally {
            isScanning = false
        }
    }

    // Live CMP Refresh for Top 15 Breakout Stocks
    LaunchedEffect(scanResults.size) {
        if (scanResults.isEmpty()) return@LaunchedEffect
        while (isActive) {
            delay(60000)
            try {
                val updatedList = withContext(Dispatchers.IO) {
                    scanResults.map { item ->
                        async {
                            try {
                                val res = YahooRetrofit.service.getChart(item.ticker, "1d", "1m")
                                val chartResult = res.chart?.result?.firstOrNull()
                                val meta = chartResult?.meta
                                val livePrice = meta?.regularMarketPrice ?: item.price
                                val dayOpen = meta?.regularMarketDayOpen ?: chartResult?.indicators?.quote?.firstOrNull()?.open?.filterNotNull()?.firstOrNull() ?: item.openPrice
                                val prevClose = meta?.previousClose ?: item.previousClose ?: livePrice
                                val change = livePrice - prevClose
                                val changePercent = if (prevClose > 0) (change / prevClose) * 100 else 0.0
                                item.copy(
                                    price = livePrice,
                                    openPrice = dayOpen,
                                    change = change,
                                    changePercent = changePercent
                                )
                            } catch (e: Exception) {
                                item
                            }
                        }
                    }.awaitAll()
                }
                scanResults = updatedList
                lastFetchedTime = System.currentTimeMillis()
            } catch (e: Exception) {}
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF1F3F6))
    ) {
        if (isScanning && scanResults.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { loadingPercent / 100f },
                            color = TabActiveBlue,
                            modifier = Modifier.size(56.dp),
                            strokeWidth = 5.dp
                        )
                        Text(
                            text = "$loadingPercent%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TabActiveBlue
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Scanning NIFTY 200 for breakout signals...", fontSize = 11.sp, color = TextMutedGray)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 8.dp, top = 8.dp, end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Top Breakouts",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            letterSpacing = (-0.3).sp
                        )

                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        isScanning = true
                                        scanResults = withContext(Dispatchers.IO) { StockScanner.scanMultiple("Breakouts") }
                                        lastFetchedTime = System.currentTimeMillis()
                                    } catch (e: Exception) {
                                    } finally {
                                        isScanning = false
                                    }
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh Breakout Signals",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                items(scanResults, key = { it.ticker }) { res ->
                    StockBreakoutCard(
                        res = res,
                        onSymbolSelected = onSymbolSelected
                    )
                }
            }
        }
    }
}

@Composable
fun RecommendationsScreen(modifier: Modifier = Modifier, onSymbolSelected: (String) -> Unit = {}) {
    var isLoading by remember { mutableStateOf(true) }
    var results by remember { mutableStateOf<List<ScanResult>>(emptyList()) }

    LaunchedEffect(Unit) {
        if (results.isEmpty()) {
            isLoading = true
            withContext(Dispatchers.IO) {
                results = StockScanner.scanMultiple("Breakouts")
            }
            isLoading = false
        }
        
        while (isActive) {
            delay(60000) // update every 60 seconds to avoid Yahoo rate limit
            if (results.isNotEmpty()) {
                val updated = withContext(Dispatchers.IO) {
                    results.map { res ->
                        async {
                            try {
                                val response = YahooRetrofit.service.getChart(res.ticker, "1d", "1m")
                                val price = response.chart?.result?.firstOrNull()?.meta?.regularMarketPrice ?: res.price
                                res.copy(price = price)
                            } catch (e: Exception) {
                                res
                            }
                        }
                    }.awaitAll()
                }
                results = updated
            }
        }
    }

    Column(modifier = modifier.fillMaxSize().background(Color(0xFFF5F7FA))) {
        Text("Algorithmic Tech-Tips", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(16.dp))
        
        if (isLoading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Scanning NSE Top 100 for technical signals...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(results.chunked(2)) { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (res in pair) {
                            Box(modifier = Modifier.weight(1f)) {
                                StockBreakoutCard(res = res, onSymbolSelected = onSymbolSelected)
                            }
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}





@Composable
fun PortfolioScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Portfolio - Coming Soon", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ConfigScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("InvestifyPrefs", android.content.Context.MODE_PRIVATE)
    
    var activeProvider by remember { mutableStateOf(sharedPrefs.getString("active_provider", "Angel One") ?: "Angel One") }
    
    // Angel One
    var angelClientCode by remember { mutableStateOf(sharedPrefs.getString("angel_client_code", "") ?: "") }
    var angelApiKey by remember { mutableStateOf(sharedPrefs.getString("angel_api_key", "") ?: "") }
    
    // Fyers
    var fyersAppId by remember { mutableStateOf(sharedPrefs.getString("fyers_app_id", "") ?: "") }
    var fyersToken by remember { mutableStateOf(sharedPrefs.getString("fyers_token", "") ?: "") }
    
    // Dhan
    var dhanClientId by remember { mutableStateOf(sharedPrefs.getString("dhan_client_id", "") ?: "") }
    var dhanToken by remember { mutableStateOf(sharedPrefs.getString("dhan_token", "") ?: "") }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Broker Configuration", fontSize = 24.sp, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
        Text("Select and configure your broker API for live CMP and Indices.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text("Active Data Provider", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("Angel One", "Fyers", "Dhan").forEach { provider ->
                val selected = activeProvider == provider
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).clickable { 
                        activeProvider = provider 
                        sharedPrefs.edit().putString("active_provider", provider).apply()
                    }
                ) {
                    Text(provider, textAlign = TextAlign.Center, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (activeProvider == "Angel One") {
            OutlinedTextField(
                value = angelClientCode,
                onValueChange = { angelClientCode = it; sharedPrefs.edit().putString("angel_client_code", it).apply() },
                label = { Text("SmartAPI Client Code") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = angelApiKey,
                onValueChange = { angelApiKey = it; sharedPrefs.edit().putString("angel_api_key", it).apply() },
                label = { Text("SmartAPI Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        } else if (activeProvider == "Fyers") {
            OutlinedTextField(
                value = fyersAppId,
                onValueChange = { fyersAppId = it; sharedPrefs.edit().putString("fyers_app_id", it).apply() },
                label = { Text("Fyers App ID (Client ID)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = fyersToken,
                onValueChange = { fyersToken = it; sharedPrefs.edit().putString("fyers_token", it).apply() },
                label = { Text("Fyers Access Token") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        } else if (activeProvider == "Dhan") {
            OutlinedTextField(
                value = dhanClientId,
                onValueChange = { dhanClientId = it; sharedPrefs.edit().putString("dhan_client_id", it).apply() },
                label = { Text("Dhan Client ID") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = dhanToken,
                onValueChange = { dhanToken = it; sharedPrefs.edit().putString("dhan_token", it).apply() },
                label = { Text("Dhan Access Token") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Security Notice", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Your API keys are stored locally on your device in SharedPreferences. Do not share your API keys.", fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}


