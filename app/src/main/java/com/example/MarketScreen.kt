package com.example

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
    val category: String, // "BASICS", "TECHNICALS", "OPTIONS", "FUNDAMENTALS", "LIVE"
    val isLive: Boolean = false,
    val isAvailable: Boolean = true
)

// Helper to fetch live YouTube videos in guest mode via RSS-to-JSON API
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
        // Guest mode headers (no user auth or cookies)
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
                        val category = when {
                            title.contains("Option", ignoreCase = true) || title.contains("F&O", ignoreCase = true) -> "OPTIONS"
                            title.contains("Pattern", ignoreCase = true) || title.contains("Breakout", ignoreCase = true) ||
                            title.contains("Chart", ignoreCase = true) || title.contains("Intraday", ignoreCase = true) ||
                            title.contains("Strategy", ignoreCase = true) -> "TECHNICALS"
                            title.contains("Fund", ignoreCase = true) || title.contains("Stock", ignoreCase = true) ||
                            title.contains("Result", ignoreCase = true) || title.contains("Earning", ignoreCase = true) -> "FUNDAMENTALS"
                            else -> "BASICS"
                        }

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
                                category = category,
                                isLive = false,
                                isAvailable = true
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
fun MarketScreen(modifier: Modifier = Modifier) {
    var isRefreshingVideos by remember { mutableStateOf(false) }
    var refreshToastMessage by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var removedVideoIds by remember { mutableStateOf(setOf<String>()) }
    var lastRefreshedTime by remember { mutableStateOf("Just now") }
    var liveVideos by remember { mutableStateOf<List<VideoItem>>(VideoCache.cachedVideos) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Fetch dynamic videos from YouTube channels (Dhan, Groww, Zee Biz, CNBC Awaaz, ET Now Swadesh) in parallel
    fun refreshLiveFeeds() {
        coroutineScope.launch {
            isRefreshingVideos = true
            if (liveVideos.isEmpty()) {
                refreshToastMessage = "Fetching latest YouTube videos from top financial channels..."
            }
            try {
                val fetched = withContext(Dispatchers.IO) {
                    coroutineScope {
                        listOf(
                            async { fetchYouTubeChannelVideos("UCEzHCpvFWoF85UabbzKTkOQ", "Dhan", "DHAN LATEST", Color(0xFF5B21B6)) },
                            async { fetchYouTubeChannelVideos("UCw5TLrz3qADabwezTEcOmgQ", "Groww", "GROWW LATEST", Color(0xFF00D09C)) },
                            async { fetchYouTubeChannelVideos("UCkXopQ3ubd-rnXnStZqCl2w", "Zee Business", "ZEE BIZ LATEST", Color(0xFFDC2626)) },
                            async { fetchYouTubeChannelVideos("UCQIycDaLsBpMKjOCeaKUYVg", "CNBC Awaaz", "CNBC AWAAZ", Color(0xFF0284C7)) },
                            async { fetchYouTubeChannelVideos("UCD3CdwT8lTCe5ZGHbUBxmWA", "ET Now Swadesh", "ET NOW SWADESH", Color(0xFFD97706)) }
                        ).awaitAll().flatten().distinctBy { it.videoId }
                    }
                }
                if (fetched.isNotEmpty()) {
                    liveVideos = fetched
                    VideoCache.cachedVideos = fetched
                    val currentTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                    lastRefreshedTime = "Refreshed at $currentTime"
                    refreshToastMessage = "Loaded ${fetched.size} live videos from top 5 channels!"
                } else if (liveVideos.isEmpty()) {
                    refreshToastMessage = "No live video feeds found. Tap refresh to retry."
                }
            } catch (e: Exception) {
                if (liveVideos.isEmpty()) {
                    refreshToastMessage = "Unable to refresh feeds: ${e.message}"
                }
            } finally {
                isRefreshingVideos = false
                delay(3000)
                refreshToastMessage = null
            }
        }
    }

    // Automatic Initial & 30-Minute Refresh Loop
    LaunchedEffect(Unit) {
        refreshLiveFeeds()
        while (isActive) {
            delay(30 * 60 * 1000L) // Refresh every 30 minutes
            refreshLiveFeeds()
        }
    }

    val categories = listOf("ALL", "DHAN", "GROWW", "ZEE BIZ", "CNBC AWAAZ", "ET NOW")

    val visibleVideos = remember(selectedCategory, removedVideoIds, liveVideos) {
        liveVideos.filter { video ->
            video.id !in removedVideoIds &&
            when (selectedCategory) {
                "ALL" -> true
                "DHAN" -> video.channel.contains("Dhan", ignoreCase = true)
                "GROWW" -> video.channel.contains("Groww", ignoreCase = true)
                "ZEE BIZ" -> video.channel.contains("Zee", ignoreCase = true)
                "CNBC AWAAZ" -> video.channel.contains("CNBC", ignoreCase = true) || video.channel.contains("Awaaz", ignoreCase = true)
                "ET NOW" -> video.channel.contains("ET Now", ignoreCase = true) || video.channel.contains("Swadesh", ignoreCase = true)
                else -> true
            }
        }
    }

    fun openYouTubeDirectly(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            refreshToastMessage = "Could not open YouTube link"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header
        Column(modifier = Modifier.padding(14.dp)) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AnimatedHeaderIcon(
                            icon = Icons.Default.PlayArrow,
                            backgroundColor = Color(0xFF10B981),
                            shape = RoundedCornerShape(12.dp),
                            useSurface = true
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            AnimatedHeadingText(
                                text = "Live Market Video Feeds",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Feeds: Dhan, Groww, Zee Biz, CNBC Awaaz & ET Now",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = {
                                if (!isRefreshingVideos) {
                                    removedVideoIds = emptySet()
                                    refreshLiveFeeds()
                                }
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            if (isRefreshingVideos) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh Videos",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = refreshToastMessage != null, enter = fadeIn(), exit = fadeOut()) {
                        Surface(
                            color = Color(0xFF10B981).copy(alpha = 0.25f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = refreshToastMessage ?: "",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }
            }
        }

        // Filter Bar (Category Chips)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            contentPadding = PaddingValues(end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                val isSelected = selectedCategory == category
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = category },
                    label = {
                        Text(
                            text = category,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        if (visibleVideos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VisibilityOff,
                        contentDescription = "No Videos",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "No available videos found in this category.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Button(
                        onClick = {
                            selectedCategory = "ALL"
                            removedVideoIds = emptySet()
                        }
                    ) {
                        Text("Reset Video Filter")
                    }
                }
            }
        } else {
            // Video Feed List (Small Thumbnail Compact Cards fetched from YouTube)
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(visibleVideos, key = { it.id }) { video ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { openYouTubeDirectly(video.directUrl) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Small Thumbnail Container (YouTube Image)
                            Box(
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(80.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0F172A))
                                    .clickable { openYouTubeDirectly(video.directUrl) }
                            ) {
                                val ctx = LocalContext.current
                                val primaryThumbnailUrl = remember(video.videoId) { "https://i.ytimg.com/vi/${video.videoId}/hqdefault.jpg" }
                                val fallbackThumbnailUrl = remember(video.videoId) { "https://img.youtube.com/vi/${video.videoId}/hqdefault.jpg" }

                                SubcomposeAsyncImage(
                                    model = ImageRequest.Builder(ctx)
                                        .data(primaryThumbnailUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = video.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                    loading = {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color(0xFF0F172A)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                color = Color(0xFF38BDF8),
                                                strokeWidth = 2.dp
                                            )
                                        }
                                    },
                                    error = {
                                        SubcomposeAsyncImage(
                                            model = ImageRequest.Builder(ctx)
                                                .data(fallbackThumbnailUrl)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = video.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize(),
                                            error = {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(
                                                            Brush.linearGradient(
                                                                colors = listOf(video.tagBgColor.copy(alpha = 0.8f), Color(0xFF0F172A))
                                                            )
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = video.channel,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        )
                                    }
                                )

                                // Red Play Button Center Overlay
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFEF4444),
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(28.dp),
                                    shadowElevation = 4.dp
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play Embedded Video",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .padding(4.dp)
                                            .fillMaxSize()
                                    )
                                }

                                // Live or Duration Badge
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color.Black.copy(alpha = 0.8f),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        if (video.isLive) {
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFEF4444))
                                            )
                                            Text(
                                                text = "LIVE",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFFEF4444)
                                            )
                                        } else {
                                            Text(
                                                text = video.timeAgo,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Text Details & External Actions Column
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                // Tag Badge
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = video.tagBgColor.copy(alpha = 0.15f),
                                    border = BorderStroke(0.5.dp, video.tagBgColor.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = video.tag,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = video.tagBgColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Text(
                                    text = video.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 16.sp
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = video.channel,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        IconButton(
                                            onClick = { openYouTubeDirectly(video.directUrl) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.OpenInNew,
                                                contentDescription = "Watch on YouTube",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                val sendIntent = Intent().apply {
                                                    action = Intent.ACTION_SEND
                                                    putExtra(
                                                        Intent.EXTRA_TEXT,
                                                        "Watch '${video.title}' on YouTube: ${video.directUrl}"
                                                    )
                                                    type = "text/plain"
                                                }
                                                context.startActivity(Intent.createChooser(sendIntent, "Share Video"))
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = "Share Video",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        // Hide/Remove button for broken/unavailable items
                                        IconButton(
                                            onClick = {
                                                removedVideoIds = removedVideoIds + video.id
                                                refreshToastMessage = "Removed video from feed"
                                                coroutineScope.launch {
                                                    delay(2000)
                                                    refreshToastMessage = null
                                                }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.VisibilityOff,
                                                contentDescription = "Remove video",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                modifier = Modifier.size(16.dp)
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
    }
}

