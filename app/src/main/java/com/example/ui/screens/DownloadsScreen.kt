package com.example.ui.screens

import android.content.Context
import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SavedPdfItem
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

enum class DownloadFilter {
    ALL, FAVORITES, PREMIUM, FREE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    downloadedList: List<SavedPdfItem>,
    onOpenPdf: (SavedPdfItem) -> Unit,
    onDeletePdf: (SavedPdfItem) -> Unit,
    onToggleFavorite: (SavedPdfItem) -> Unit,
    onBack: () -> Unit,
    onBrowseLibrary: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf(DownloadFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var itemToDelete by remember { mutableStateOf<SavedPdfItem?>(null) }

    val filteredList = remember(downloadedList, selectedFilter, searchQuery) {
        downloadedList.filter { item ->
            val matchesFilter = when (selectedFilter) {
                DownloadFilter.ALL -> true
                DownloadFilter.FAVORITES -> item.isFavorite
                DownloadFilter.PREMIUM -> item.isPremium
                DownloadFilter.FREE -> !item.isPremium
            }
            val matchesSearch = searchQuery.isBlank() ||
                    item.title.contains(searchQuery, ignoreCase = true) ||
                    item.subject.contains(searchQuery, ignoreCase = true) ||
                    item.category.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Offline Downloads",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${downloadedList.size} Saved PDF Documents",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextGray
                        )
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ShivaSurface)
            )
        },
        containerColor = ShivaBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search downloaded notes...", color = TextMuted) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = PurpleLight)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = TextGray)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PurplePrimary,
                    unfocusedBorderColor = ShivaBorder,
                    focusedContainerColor = ShivaCard,
                    unfocusedContainerColor = ShivaCard,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite
                )
            )

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == DownloadFilter.ALL,
                    onClick = { selectedFilter = DownloadFilter.ALL },
                    label = { Text("All (${downloadedList.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PurplePrimary,
                        selectedLabelColor = Color.White,
                        containerColor = ShivaCard,
                        labelColor = TextGray
                    )
                )
                FilterChip(
                    selected = selectedFilter == DownloadFilter.FAVORITES,
                    onClick = { selectedFilter = DownloadFilter.FAVORITES },
                    label = { Text("Favorites") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = if (selectedFilter == DownloadFilter.FAVORITES) Color.White else AmberGold,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PurplePrimary,
                        selectedLabelColor = Color.White,
                        containerColor = ShivaCard,
                        labelColor = TextGray
                    )
                )
                FilterChip(
                    selected = selectedFilter == DownloadFilter.PREMIUM,
                    onClick = { selectedFilter = DownloadFilter.PREMIUM },
                    label = { Text("Premium") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PurplePrimary,
                        selectedLabelColor = Color.White,
                        containerColor = ShivaCard,
                        labelColor = TextGray
                    )
                )
                FilterChip(
                    selected = selectedFilter == DownloadFilter.FREE,
                    onClick = { selectedFilter = DownloadFilter.FREE },
                    label = { Text("Free") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PurplePrimary,
                        selectedLabelColor = Color.White,
                        containerColor = ShivaCard,
                        labelColor = TextGray
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = ShivaCard,
                            modifier = Modifier.size(80.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ShivaBorder)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DownloadDone,
                                    contentDescription = null,
                                    tint = PurpleLight,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No Matching Downloads" else "No Saved PDFs Yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotBlank())
                                "Try searching for a different subject or keyword."
                            else
                                "Download study notes and PDFs from SHIVA PDF to read anytime offline without internet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextGray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onBrowseLibrary,
                            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Browse PDF Library")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.id }) { item ->
                        DownloadedPdfCard(
                            item = item,
                            context = context,
                            onOpen = { onOpenPdf(item) },
                            onToggleFavorite = { onToggleFavorite(item) },
                            onDelete = { itemToDelete = item }
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Delete Downloaded PDF?", color = TextWhite) },
            text = {
                Text(
                    "Are you sure you want to delete \"${itemToDelete?.title}\"? This will remove the offline file from your device storage.",
                    color = TextGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        itemToDelete?.let { onDeletePdf(it) }
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedError)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { itemToDelete = null },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite)
                ) {
                    Text("Cancel")
                }
            },
            containerColor = ShivaSurface
        )
    }
}

@Composable
private fun DownloadedPdfCard(
    item: SavedPdfItem,
    context: Context,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val formattedSize = remember(item.fileSizeBytes) {
        if (item.fileSizeBytes > 0) Formatter.formatFileSize(context, item.fileSizeBytes) else "PDF Note"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ShivaCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, ShivaBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon thumbnail
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (item.isPremium) Color(0xFF382305) else Color(0xFF1E1738),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (item.isPremium) AmberGold.copy(alpha = 0.5f) else PurplePrimary.copy(alpha = 0.4f)
                ),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = if (item.isPremium) AmberGold else PurpleLight,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.isPremium) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = AmberGold,
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = "PREMIUM",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                fontSize = 9.sp
                            )
                        }
                    }
                    Text(
                        text = item.subject,
                        style = MaterialTheme.typography.labelSmall,
                        color = PurpleLight
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextWhite,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formattedSize,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGray
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Text(
                        text = dateFormat.format(Date(item.downloadTimestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            // Actions
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (item.isFavorite) RedError else TextMuted
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = TextMuted
                    )
                }
            }
        }
    }
}
