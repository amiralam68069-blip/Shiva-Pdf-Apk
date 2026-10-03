package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.model.SavedPdfItem
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(
    pdfTitle: String,
    filePath: String,
    originalItem: SavedPdfItem? = null,
    onBack: () -> Unit,
    onToggleBookmark: ((SavedPdfItem) -> Unit)? = null,
    onUpdateProgress: ((Long, Int) -> Unit)? = null
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isNightMode by remember { mutableStateOf(false) }
    var pageCount by remember { mutableIntStateOf(0) }
    var renderedPages by remember { mutableStateOf<Map<Int, Bitmap>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val listState = rememberLazyListState()

    // Determine current visible page
    val currentVisiblePage by remember {
        derivedStateOf {
            (listState.firstVisibleItemIndex + 1).coerceAtMost(pageCount.coerceAtLeast(1))
        }
    }

    // Update reading progress when visible page changes
    LaunchedEffect(currentVisiblePage) {
        if (originalItem != null && pageCount > 0) {
            onUpdateProgress?.invoke(originalItem.id, currentVisiblePage)
        }
    }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 4f)
        if (scale > 1f) {
            offsetX += offsetChange.x
            offsetY += offsetChange.y
        } else {
            offsetX = 0f
            offsetY = 0f
        }
    }

    // Load PDF using Android's native PdfRenderer
    LaunchedEffect(filePath) {
        isLoading = true
        errorMessage = null
        withContext(Dispatchers.IO) {
            try {
                val file = File(filePath)
                if (!file.exists() || file.length() == 0L) {
                    withContext(Dispatchers.Main) {
                        errorMessage = "PDF file not found or empty."
                        isLoading = false
                    }
                    return@withContext
                }

                val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val pdfRenderer = PdfRenderer(pfd)
                val count = pdfRenderer.pageCount

                withContext(Dispatchers.Main) {
                    pageCount = count
                }

                val pageMap = mutableMapOf<Int, Bitmap>()
                // Render initial pages safely using RGB_565 for 50% lower memory footprint
                val renderLimit = count.coerceAtMost(30)
                for (i in 0 until renderLimit) {
                    val page = pdfRenderer.openPage(i)
                    val width = 960
                    val height = (width.toFloat() / page.width.toFloat() * page.height.toFloat()).toInt()
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
                    bitmap.eraseColor(AndroidColor.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    pageMap[i] = bitmap

                    if (i == 0) {
                        withContext(Dispatchers.Main) {
                            renderedPages = pageMap.toMap()
                            isLoading = false
                        }
                    }
                }
                pdfRenderer.close()
                pfd.close()

                withContext(Dispatchers.Main) {
                    renderedPages = pageMap
                    isLoading = false
                }
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    errorMessage = "Failed to open PDF: ${e.localizedMessage ?: "File cannot be rendered"}"
                    isLoading = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = pdfTitle,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = TextWhite
                        )
                        if (pageCount > 0) {
                            Text(
                                text = "Page $currentVisiblePage of $pageCount",
                                style = MaterialTheme.typography.labelSmall,
                                color = PurpleLight
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextWhite
                        )
                    }
                },
                actions = {
                    // Night / Inverted Reading Mode
                    IconButton(onClick = { isNightMode = !isNightMode }) {
                        Icon(
                            imageVector = if (isNightMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Night Mode",
                            tint = if (isNightMode) AmberGold else TextWhite
                        )
                    }

                    // Share PDF
                    IconButton(onClick = {
                        sharePdfFile(context, filePath, pdfTitle)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share PDF",
                            tint = TextWhite
                        )
                    }

                    // Reset Zoom if zoomed
                    if (scale > 1f) {
                        IconButton(onClick = {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        }) {
                            Icon(
                                imageVector = Icons.Default.ZoomOutMap,
                                contentDescription = "Reset Zoom",
                                tint = CyanAccent
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ShivaSurface
                )
            )
        },
        containerColor = if (isNightMode) Color(0xFF0F111A) else ShivaBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isLoading && renderedPages.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = PurplePrimary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading PDF Pages...",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextWhite
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Rendering high quality document",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray
                        )
                    }
                }

                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = RedError,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Unable to Display PDF",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextWhite
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextGray
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                        ) {
                            Text("Go Back")
                        }
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            )
                            .transformable(state = transformState)
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(
                                items = (0 until pageCount).toList(),
                                key = { _, index -> index }
                            ) { pageIdx, _ ->
                                val bitmap = renderedPages[pageIdx]
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isNightMode) Color(0xFF1E2438) else Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = "Page ${pageIdx + 1}",
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .wrapContentHeight(),
                                                contentScale = ContentScale.FillWidth
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(400.dp)
                                                    .background(if (isNightMode) Color(0xFF1E2438) else Color(0xFFF1F5F9)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(
                                                    color = PurplePrimary,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                            }
                                        }
                                        // Page footer indicator
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(if (isNightMode) Color(0xFF131828) else Color(0xFFF8FAFC))
                                                .padding(vertical = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "- Page ${pageIdx + 1} -",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isNightMode) TextGray else Color.DarkGray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Floating Page Navigator badge at bottom center
                    AnimatedVisibility(
                        visible = pageCount > 0,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 20.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = ShivaSurface.copy(alpha = 0.92f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ShivaBorder),
                            shadowElevation = 8.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = PurpleLight,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "$currentVisiblePage / $pageCount",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun sharePdfFile(context: Context, filePath: String, title: String) {
    try {
        val file = File(filePath)
        if (!file.exists()) {
            Toast.makeText(context, "PDF file not available for sharing", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share PDF Notes"))
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to share: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}
