package com.example

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.Manrope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Shared Cache for Video feeds
object VideoCache {
    var cachedVideos: List<VideoItem> = emptyList()
}

data class VideoItem(
    val id: String,
    val title: String,
    val channel: String,
    val tag: String,
    val tagBgColor: Color,
    val videoId: String,
    val directUrl: String,
    val timeAgo: String,
    val category: String,
    val isLive: Boolean = false,
    val isAvailable: Boolean = true,
    val pubDateMs: Long = 0L
)

fun parsePubDateToMillis(pubDateStr: String): Long {
    if (pubDateStr.isBlank()) return 0L
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val date = sdf.parse(pubDateStr)
        date?.time ?: 0L
    } catch (e: Exception) {
        0L
    }
}

suspend fun fetchYouTubeChannelVideos(
    channelId: String,
    channelName: String,
    tag: String,
    tagColor: Color
): List<VideoItem> = withContext(Dispatchers.IO) {
    val result = mutableListOf<VideoItem>()
    try {
        val rssUrl = "https://www.youtube.com/feeds/videos.xml?channel_id=$channelId"
        val apiUrl = "https://api.rss2json.com/v1/api.json?rss_url=" + URLEncoder.encode(rssUrl, "UTF-8")
        val conn = URL(apiUrl).openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
        conn.setRequestProperty("Accept", "application/json")

        if (conn.responseCode == 200) {
            val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
            val jsonObj = JSONObject(jsonStr)
            if (jsonObj.optString("status") == "ok") {
                val items = jsonObj.optJSONArray("items") ?: JSONArray()
                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    val rawTitle = item.optString("title", "")
                    val title = android.text.Html.fromHtml(rawTitle, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
                    val link = item.optString("link", "")
                    val guid = item.optString("guid", "")
                    val videoId = when {
                        guid.startsWith("yt:video:") -> guid.removePrefix("yt:video:")
                        link.contains("watch?v=") -> link.substringAfter("watch?v=").substringBefore("&")
                        link.contains("shorts/") -> link.substringAfter("shorts/").substringBefore("?")
                        else -> ""
                    }
                    if (videoId.isNotBlank()) {
                        val pubDate = item.optString("pubDate", "")
                        val timeAgo = parsePubDateToTimeAgo(pubDate)
                        val pubDateMs = parsePubDateToMillis(pubDate)
                        result.add(
                            VideoItem(
                                id = "yt_${channelId}_$videoId",
                                title = title,
                                channel = channelName,
                                tag = tag,
                                tagBgColor = tagColor,
                                videoId = videoId,
                                directUrl = if (link.isNotBlank()) link else "https://www.youtube.com/watch?v=$videoId",
                                timeAgo = timeAgo,
                                category = channelName.uppercase(),
                                pubDateMs = pubDateMs
                            )
                        )
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    result
}

fun parsePubDateToTimeAgo(pubDateStr: String): String {
    if (pubDateStr.isBlank()) return "Recently"
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val date = sdf.parse(pubDateStr) ?: return pubDateStr
        val now = System.currentTimeMillis()
        val diffMs = now - date.time
        val diffMins = diffMs / (1000 * 60)
        val diffHours = diffMins / 60
        val diffDays = diffHours / 24

        when {
            diffMins < 1 -> "Just now"
            diffMins < 60 -> "${diffMins}m ago"
            diffHours < 24 -> "${diffHours}h ago"
            diffDays < 7 -> "${diffDays}d ago"
            else -> SimpleDateFormat("MMM dd", Locale.US).format(date)
        }
    } catch (e: Exception) {
        "Recently"
    }
}

@Composable
fun NewsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activeTab by remember { mutableIntStateOf(0) } // 0 = News, 1 = Videos
    var selectedNewsCategory by remember { mutableStateOf("ALL") }
    var selectedVideoCategory by remember { mutableStateOf("ALL") }

    var isNewsLoading by remember { mutableStateOf(false) }
    var newsList by remember { mutableStateOf<List<NewsArticle>>(emptyList()) }

    var isRefreshingVideos by remember { mutableStateOf(false) }
    var videoList by remember { mutableStateOf<List<VideoItem>>(VideoCache.cachedVideos) }
    var removedVideoIds by remember { mutableStateOf(setOf<String>()) }

    // Fetch News Articles
    fun loadNews() {
        isNewsLoading = true
        scope.launch {
            try {
                newsList = NewsTickerService.fetchNewsArticles(selectedNewsCategory)
            } finally {
                isNewsLoading = false
            }
        }
    }

    // Fetch Video Feeds
    fun refreshVideos() {
        scope.launch {
            isRefreshingVideos = true
            try {
                val fetched = withContext(Dispatchers.IO) {
                    val d1 = async { fetchYouTubeChannelVideos("UCEzHCpvFWoF85UabbzKTkOQ", "Dhan", "DHAN LATEST", Color(0xFF5B21B6)) }
                    val d2 = async { fetchYouTubeChannelVideos("UCw5TLrz3qADabwezTEcOmgQ", "Groww", "GROWW LATEST", Color(0xFF00D09C)) }
                    val d3 = async { fetchYouTubeChannelVideos("UCkXopQ3ubd-rnXnStZqCl2w", "Zee Business", "ZEE BIZ LATEST", Color(0xFFDC2626)) }
                    val d4 = async { fetchYouTubeChannelVideos("UCQIycDaLsBpMKjOCeaKUYVg", "CNBC Awaaz", "CNBC AWAAZ", Color(0xFF0284C7)) }
                    val d5 = async { fetchYouTubeChannelVideos("UCD3CdwT8lTCe5ZGHbUBxmWA", "ET Now Swadesh", "ET NOW SWADESH", Color(0xFFD97706)) }
                    val all = listOf(d1.await(), d2.await(), d3.await(), d4.await(), d5.await()).flatten()
                    all.distinctBy { it.videoId }.sortedByDescending { it.pubDateMs }
                }
                if (fetched.isNotEmpty()) {
                    videoList = fetched
                    VideoCache.cachedVideos = fetched
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isRefreshingVideos = false
            }
        }
    }

    LaunchedEffect(selectedNewsCategory) {
        loadNews()
    }

    LaunchedEffect(Unit) {
        refreshVideos()
        while (isActive) {
            delay(30 * 60 * 1000L)
            refreshVideos()
        }
    }

    // Filtered lists
    val filteredNews = remember(newsList, selectedNewsCategory) {
        when (selectedNewsCategory) {
            "Nifty & Sensex" -> newsList.filter { it.category.contains("Nifty", ignoreCase = true) || it.title.contains("Nifty", ignoreCase = true) || it.title.contains("Sensex", ignoreCase = true) }
            "Corporate & Q3" -> newsList.filter { it.category.contains("Corporate", ignoreCase = true) || it.category.contains("Q3", ignoreCase = true) || it.title.contains("Corporate", ignoreCase = true) || it.title.contains("Q3", ignoreCase = true) }
            "FII/DII", "FII / DII" -> newsList.filter { it.category.contains("FII", ignoreCase = true) || it.title.contains("FII", ignoreCase = true) || it.title.contains("DII", ignoreCase = true) }
            else -> newsList
        }
    }

    val visibleVideos = remember(videoList, selectedVideoCategory, removedVideoIds) {
        videoList.filter { video ->
            video.id !in removedVideoIds &&
            when (selectedVideoCategory) {
                "DHAN" -> video.channel.contains("Dhan", ignoreCase = true) || video.tag.contains("DHAN", ignoreCase = true)
                "GROWW" -> video.channel.contains("Groww", ignoreCase = true) || video.tag.contains("GROWW", ignoreCase = true)
                "ZEE BIZ" -> video.channel.contains("Zee", ignoreCase = true) || video.tag.contains("ZEE", ignoreCase = true)
                "CNBC AWAAZ" -> video.channel.contains("CNBC", ignoreCase = true) || video.channel.contains("Awaaz", ignoreCase = true)
                else -> true
            }
        }
    }

    fun openUrlDirectly(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val isNewsSelected = activeTab == 0
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isNewsSelected) Color(0xFFEDE9FE) else Color.Transparent)
                        .clickable { activeTab = 0 }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Market News",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isNewsSelected) Color(0xFF7C3AED) else Color(0xFF94A3B8),
                        letterSpacing = (-0.3).sp
                    )
                }

                val isVideosSelected = activeTab == 1
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isVideosSelected) Color(0xFFEDE9FE) else Color.Transparent)
                        .clickable { activeTab = 1 }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Videos",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isVideosSelected) Color(0xFF7C3AED) else Color(0xFF94A3B8),
                        letterSpacing = (-0.3).sp
                    )
                }

                val isEngineSelected = activeTab == 2
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isEngineSelected) Color(0xFFEDE9FE) else Color.Transparent)
                        .clickable { activeTab = 2 }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Auto Trader",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isEngineSelected) Color(0xFF7C3AED) else Color(0xFF94A3B8),
                        letterSpacing = (-0.3).sp
                    )
                }
            }

            IconButton(
                onClick = {
                    if (activeTab == 0) loadNews() 
                    else if (activeTab == 1) refreshVideos()
                    else {
                        scope.launch {
                            MarketEngine.runEngineCycle(context)
                        }
                    }
                },
                modifier = Modifier.size(32.dp)
            ) {
                val isEngineScanning by MarketEngine.isScanning.collectAsState()
                if (isNewsLoading || isRefreshingVideos || (activeTab == 2 && isEngineScanning)) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color(0xFF0F172A),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Tab Content
        if (activeTab == 0) {
            // NEWS TAB CONTENT
            if (filteredNews.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Article,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No News Articles Found",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredNews, key = { it.id }) { article ->
                        NewsCardItem(
                            article = article,
                            onOpenUrl = { openUrlDirectly(it) }
                        )
                    }
                }
            }
        } else if (activeTab == 1) {
            // VIDEOS TAB CONTENT
            if (visibleVideos.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Videos Available",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(visibleVideos, key = { it.id }) { video ->
                        VideoCardRowItem(
                            video = video,
                            onOpenUrl = { openUrlDirectly(video.directUrl) },
                            onShare = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "Watch '${video.title}' on YouTube: ${video.directUrl}")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Video"))
                            },
                            onHide = {
                                removedVideoIds = removedVideoIds + video.id
                            }
                        )
                    }
                }
            }
        } else {
            AutoEngineTabContent()
        }
    }
}

fun getPublisherDomain(source: String, articleUrl: String = ""): String {
    val s = source.lowercase(Locale.ROOT)
    return when {
        s.contains("economic") || s.contains("et ") -> "economictimes.indiatimes.com"
        s.contains("cnbc") -> "cnbctv18.com"
        s.contains("moneycontrol") -> "moneycontrol.com"
        s.contains("mint") -> "livemint.com"
        s.contains("zee") -> "zeebiz.com"
        s.contains("business line") || s.contains("businessline") -> "thehindubusinessline.com"
        s.contains("business standard") -> "business-standard.com"
        s.contains("ndtv") -> "ndtv.com"
        s.contains("financial express") -> "financialexpress.com"
        s.contains("reuters") -> "reuters.com"
        s.contains("bloomberg") -> "bloomberg.com"
        s.contains("times") -> "timesofindia.indiatimes.com"
        s.contains("express") -> "indianexpress.com"
        else -> {
            if (articleUrl.isNotBlank()) {
                try {
                    val host = java.net.URI(articleUrl).host
                    if (!host.isNullOrBlank() && !host.contains("google")) host else "economictimes.indiatimes.com"
                } catch (e: Exception) {
                    "economictimes.indiatimes.com"
                }
            } else {
                "economictimes.indiatimes.com"
            }
        }
    }
}

fun getPublisherLogoUrl(source: String, articleUrl: String = ""): String {
    val domain = getPublisherDomain(source, articleUrl)
    return "https://www.google.com/s2/favicons?domain=$domain&sz=128"
}

fun getPublisherBadgeInfo(source: String): Pair<Color, String> {
    val s = source.lowercase(Locale.ROOT)
    return when {
        s.contains("economic") || s.contains("et ") || s.contains("times") -> Pair(Color(0xFFDC2626), "ET")
        s.contains("cnbc") -> Pair(Color(0xFF0284C7), "CNBC")
        s.contains("moneycontrol") -> Pair(Color(0xFFEA580C), "MC")
        s.contains("mint") -> Pair(Color(0xFF0D9488), "MINT")
        s.contains("zee") -> Pair(Color(0xFFB91C1C), "ZEE")
        s.contains("business standard") -> Pair(Color(0xFF7C3AED), "BS")
        s.contains("ndtv") -> Pair(Color(0xFF1E3A8A), "NDTV")
        s.contains("financial express") -> Pair(Color(0xFF2563EB), "FE")
        s.contains("reuters") -> Pair(Color(0xFFD97706), "RTRS")
        s.contains("bloomberg") -> Pair(Color(0xFF000000), "BBG")
        else -> Pair(Color(0xFF475569), source.take(2).uppercase(Locale.ROOT))
    }
}

fun getArticleTickerTag(article: NewsArticle): String {
    val text = (article.title + " " + article.source + " " + article.category).uppercase(Locale.ROOT)
    return when {
        text.contains("NIFTY") -> "NIFTY"
        text.contains("SENSEX") -> "SENSEX"
        text.contains("RELIANCE") -> "RELIANCE"
        text.contains("TCS") -> "TCS"
        text.contains("INFOSYS") -> "INFY"
        text.contains("TATA MOTORS") || text.contains("TATA") -> "TATAMOTORS"
        text.contains("VEDANTA") -> "VEDL"
        text.contains("RBI") -> "RBI"
        text.contains("FED") -> "US FED"
        text.contains("CNBC") -> "CNBC"
        text.contains("ECONOMIC") -> "NTSK"
        text.contains("ZEE") -> "Red TV"
        else -> "NSE"
    }
}

@Composable
fun NewsCardItem(
    article: NewsArticle,
    onOpenUrl: (String) -> Unit
) {
    val sentimentColor = when (article.sentiment.uppercase(Locale.ROOT)) {
        "BULLISH" -> Color(0xFF16A34A)
        "BEARISH" -> Color(0xFFDC2626)
        else -> Color(0xFF2563EB)
    }

    val (pubBgColor, pubInitial) = getPublisherBadgeInfo(article.source)
    val tickerTag = getArticleTickerTag(article)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenUrl(article.url) },
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Left Content Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Publisher Logo + Publisher Name + Green Solid Sentiment Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SubcomposeAsyncImage(
                        model = getPublisherLogoUrl(article.source, article.url),
                        contentDescription = article.source,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(0.5.dp, Color(0xFFCBD5E1), CircleShape),
                        error = {
                            Surface(
                                shape = CircleShape,
                                color = pubBgColor,
                                modifier = Modifier.size(20.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = pubInitial,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                            }
                        },
                        loading = {
                            Surface(
                                shape = CircleShape,
                                color = pubBgColor,
                                modifier = Modifier.size(20.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = pubInitial,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    )

                    Text(
                        text = article.source,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = Manrope,
                        color = Color(0xFF334155),
                        letterSpacing = (-0.1).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Solid Green/Red/Blue Sentiment Capsule Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = sentimentColor
                    ) {
                        Text(
                            text = article.sentiment.uppercase(Locale.ROOT),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = Manrope,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            letterSpacing = 0.2.sp
                        )
                    }
                }

                // Headline: Bold Black Title (max 2 lines)
                Text(
                    text = article.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Manrope,
                    color = Color(0xFF0F172A),
                    lineHeight = 19.sp,
                    letterSpacing = (-0.15).sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Subtitle / Date line e.g. "2 hours ago · Feb 9, 2023"
                Text(
                    text = article.timeAgo,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = Manrope,
                    color = Color(0xFF64748B),
                    letterSpacing = (-0.1).sp
                )
            }

            // Right Side Image Container with rounded corners & ticker overlay on image bottom-right
            val hasRealImage = article.imageUrl.isNotBlank() && !isGoogleNewsOrDefaultLogo(article.imageUrl)

            var imageLoadFailed by remember(article.id) { mutableStateOf(false) }
            var logoLoadFailed by remember(article.id) { mutableStateOf(false) }

            val imageModel = remember(article.imageUrl, hasRealImage, imageLoadFailed, logoLoadFailed) {
                if (hasRealImage && !imageLoadFailed) {
                    article.imageUrl
                } else if (!logoLoadFailed) {
                    getPublisherLogoUrl(article.source, article.url)
                } else {
                    NewsTickerService.getCategoryImage(article.category, article.title)
                }
            }

            val isShowingLogo = !logoLoadFailed && (!hasRealImage || imageLoadFailed)

            Box(
                modifier = Modifier
                    .size(width = 82.dp, height = 76.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = imageModel,
                    contentDescription = article.title,
                    contentScale = if (isShowingLogo) ContentScale.Fit else ContentScale.Crop,
                    modifier = if (isShowingLogo) Modifier.size(36.dp) else Modifier.fillMaxSize(),
                    onError = {
                        if (hasRealImage && !imageLoadFailed) {
                            imageLoadFailed = true
                        } else {
                            logoLoadFailed = true
                        }
                    }
                )

                if (hasRealImage) {
                    // Dark gradient overlay at bottom of thumbnail image
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                )
                            )
                    )

                    // News Channel Favicon Overlay inside top-left of thumbnail picture
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.9f))
                            .border(0.5.dp, Color(0xFFE2E8F0), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        SubcomposeAsyncImage(
                            model = getPublisherLogoUrl(article.source, article.url),
                            contentDescription = article.source,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(12.dp)
                        )
                    }

                    // White ticker badge tag overlay at bottom-right corner of thumbnail image
                    Text(
                        text = tickerTag,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 5.dp, bottom = 4.dp),
                        maxLines = 1
                    )
                } else {
                    // Minimalist pill for ticker tag on a clean light container
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(0.5.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 4.dp, bottom = 4.dp)
                    ) {
                        Text(
                            text = tickerTag,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VideoCardRowItem(
    video: VideoItem,
    onOpenUrl: () -> Unit,
    onShare: () -> Unit,
    onHide: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenUrl() },
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Side-by-Side Row Layout: Text LEFT, Thumbnail RIGHT (as requested)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Text Block to the LEFT of Thumbnail
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Headline Above: Bold Black, Max 2 Lines, Ellipsis if longer
                    Text(
                        text = video.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 17.sp
                    )

                    // Source Icon and Name Below the headline (slightly bigger)
                    val s = video.channel.lowercase(Locale.ROOT)
                    val domain = when {
                        s.contains("dhan") -> "dhan.co"
                        s.contains("groww") -> "groww.in"
                        s.contains("zee") -> "zeebiz.com"
                        s.contains("cnbc") -> "cnbctv18.com"
                        s.contains("et now") || s.contains("swadesh") -> "economictimes.indiatimes.com"
                        else -> "youtube.com"
                    }
                    val logoUrl = "https://www.google.com/s2/favicons?domain=$domain&sz=128"

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F5F9))
                                .border(0.5.dp, Color(0xFFE2E8F0), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            val context = LocalContext.current
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(logoUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = video.channel,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape),
                                error = {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = video.channel.take(1).uppercase(Locale.ROOT),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                            )
                        }

                        Text(
                            text = video.channel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569)
                        )
                    }
                }

                // Right Thumbnail Container (110x70px)
                Box(
                    modifier = Modifier
                        .width(110.dp)
                        .height(70.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F172A))
                ) {
                    val context = LocalContext.current
                    val primaryUrl = remember(video.videoId) { "https://i.ytimg.com/vi/${video.videoId}/hqdefault.jpg" }
                    val fallbackUrl = remember(video.videoId) { "https://img.youtube.com/vi/${video.videoId}/hqdefault.jpg" }

                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(primaryUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = video.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        error = {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(fallbackUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = video.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    )

                    // Red Play Button Icon Centered
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFEF4444),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(24.dp),
                        shadowElevation = 3.dp
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play Video",
                            tint = Color.White,
                            modifier = Modifier
                                .padding(3.dp)
                                .fillMaxSize()
                        )
                    }

                    // Small Dark Time Badge in Bottom-Right Corner
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Black.copy(alpha = 0.8f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(3.dp)
                    ) {
                        Text(
                            text = video.timeAgo,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            // Below Each Row: 3 Icons Aligned Right (open/external-link, share, hide)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onOpenUrl() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Open Link",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = { onShare() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = { onHide() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VisibilityOff,
                        contentDescription = "Hide Video",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AutoEngineTabContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = MyApplication.database

    val isEngineRunning by MarketEngine.isEngineRunning.collectAsState()
    val isScanningEngine by MarketEngine.isScanning.collectAsState()
    val engineLogs by MarketEngine.engineLogs.collectAsState()
    val isSimulationMode by MarketEngine.isSimulationMode.collectAsState()

    val virtualTrades by db.virtualTradeDao().getAllTradesFlow().collectAsState(initial = emptyList())
    val profitLogs by db.profitLogDao().getAllLogsFlow().collectAsState(initial = emptyList())

    val activeTrades = virtualTrades.filter { it.status == "ACTIVE" }
    val closedTrades = virtualTrades.filter { it.status != "ACTIVE" }

    // Metrics calculations
    val totalClosed = closedTrades
    val profitBookedCount = totalClosed.count { it.status == "PROFIT_BOOKED" }
    val stopLossCount = totalClosed.count { it.status == "STOP_LOSS" }
    val squaredOffCount = totalClosed.count { it.status == "SQUARED_OFF" }
    val winRate = if (totalClosed.isNotEmpty()) (profitBookedCount.toDouble() / totalClosed.size * 100).toInt() else 0
    val netProfit = totalClosed.sumOf { it.profitAmount }

    var subTab by remember { mutableStateOf("ACTIVE") } // "ACTIVE", "CLOSED", "PERFORMANCE", "CONSOLE"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Status & Controls Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isEngineRunning) Color(0xFF22C55E) else Color(0xFFEF4444))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isEngineRunning) "ENGINE: RUNNING" else "ENGINE: STANDBY",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isEngineRunning) Color(0xFF16A34A) else Color(0xFFDC2626)
                            )
                        }
                        
                        Switch(
                            checked = isEngineRunning,
                            onCheckedChange = { running ->
                                if (running) {
                                    MarketEngine.startEngine(context)
                                } else {
                                    MarketEngine.stopEngine()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF7C3AED)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Simulation / Testing Mode",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "Bypasses market hours (Mon-Fri, 9:15 AM - 3:30 PM IST) for immediate virtual testing.",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        
                        Switch(
                            checked = isSimulationMode,
                            onCheckedChange = { enabled ->
                                MarketEngine.isSimulationMode.value = enabled
                                MarketEngine.addLog("Simulation Mode changed to: $enabled")
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF7C3AED)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    MarketEngine.runEngineCycle(context)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isScanningEngine
                        ) {
                            if (isScanningEngine) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Scanning...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Scan Breakouts", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (activeTrades.isNotEmpty()) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        MarketEngine.forceManualSquareOff(db)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Square-Off All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Metrics Summary Row
        item {
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
                        Text("Net Profit", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹${String.format("%,.2f", netProfit)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (netProfit >= 0) Color(0xFF16A34A) else Color(0xFFDC2626)
                        )
                    }
                }

                // Win Rate Card
                Card(
                    modifier = Modifier.weight(0.8f),
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
                    }
                }

                // Total Trades
                Card(
                    modifier = Modifier.weight(0.8f),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Closed", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${totalClosed.size} Trades",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E293B)
                        )
                    }
                }
            }
        }

        // Subtabs Selector
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    Triple("ACTIVE", "Active (${activeTrades.size})", Icons.Default.TrendingUp),
                    Triple("CLOSED", "Closed (${closedTrades.size})", Icons.Default.History),
                    Triple("PERFORMANCE", "Profit Logs", Icons.Default.BarChart),
                    Triple("CONSOLE", "Live Logs", Icons.Default.Terminal)
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
                            Text("Turn on background engine or trigger manual scanner above to identify high-potential breakout stocks.", fontSize = 11.sp, color = Color(0xFF64748B), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
            } else {
                items(activeTrades, key = { it.id }) { trade ->
                    ActiveTradeCardItem(trade = trade, onSquareOff = {
                        scope.launch {
                            MarketEngine.manualSquareOffSingleTrade(trade.id, db)
                        }
                    })
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
                            Text("No Trades Closed Today", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
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
                            Text("Profits aggregated at 3:15 PM daily close will be saved here up to 6 months.", fontSize = 11.sp, color = Color(0xFF64748B), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
            } else {
                items(profitLogs, key = { it.id }) { log ->
                    PerformanceLogCardItem(log = log)
                }
            }
        } else {
            // CONSOLE LOGS
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Engine Terminal Console", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            db.virtualTradeDao().clearAllTrades()
                                            db.profitLogDao().clearAllLogs()
                                        }
                                        MarketEngine.engineLogs.value = emptyList()
                                        MarketEngine.addLog("Data Cleared manually.")
                                    }
                                },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = Color(0xFF334155))
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (engineLogs.isEmpty()) {
                                item {
                                    Text("Terminal listening... Toggle engine to start console.", color = Color(0xFF64748B), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                            } else {
                                items(engineLogs) { log ->
                                    Text(
                                        text = log,
                                        color = if (log.contains("🎉") || log.contains("DAILY")) Color(0xFF34D399) 
                                                else if (log.contains("📉") || log.contains("Error")) Color(0xFFF87171)
                                                else if (log.contains("🚀") || log.contains("STARTED")) Color(0xFF60A5FA)
                                                else Color(0xFFE2E8F0),
                                        fontSize = 10.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveTradeCardItem(trade: VirtualTrade, onSquareOff: () -> Unit) {
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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

                    Button(
                        onClick = onSquareOff,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFECEF), contentColor = Color(0xFFEF4444)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Exit", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(10.dp))

            // Trading values metadata row
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

            // Custom Interactive Progress bar displaying relative current price
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
                Text("Target Target Peak", fontSize = 8.5.sp, color = Color(0xFF94A3B8))
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
