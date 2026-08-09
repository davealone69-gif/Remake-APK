package com.example

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class DocumentType {
    PDF, IMAGE, TEXT, UNKNOWN
}

@Composable
fun PdfRendererContent(pdfPath: String, modifier: Modifier = Modifier) {
    val file = remember(pdfPath) { File(pdfPath) }
    var docType by remember(pdfPath) { mutableStateOf(DocumentType.UNKNOWN) }
    var pdfRenderer by remember(pdfPath) { mutableStateOf<PdfRenderer?>(null) }
    var fileDescriptor by remember(pdfPath) { mutableStateOf<ParcelFileDescriptor?>(null) }
    var pageCount by remember(pdfPath) { mutableIntStateOf(0) }
    var imageBitmap by remember(pdfPath) { mutableStateOf<Bitmap?>(null) }
    var textContent by remember(pdfPath) { mutableStateOf<String?>(null) }
    var errorMessage by remember(pdfPath) { mutableStateOf<String?>(null) }

    LaunchedEffect(pdfPath) {
        withContext(Dispatchers.IO) {
            if (!file.exists()) {
                errorMessage = "File not found: ${file.name}"
                return@withContext
            }

            val pathLower = pdfPath.lowercase()
            if (pathLower.endsWith(".png") || pathLower.endsWith(".jpg") || pathLower.endsWith(".jpeg") || pathLower.endsWith(".webp") || pathLower.endsWith(".bmp")) {
                val bitmap = BitmapFactory.decodeFile(pdfPath)
                if (bitmap != null) {
                    docType = DocumentType.IMAGE
                    imageBitmap = bitmap
                } else {
                    errorMessage = "Failed to load image format."
                }
            } else if (pathLower.endsWith(".txt") || pathLower.endsWith(".log") || pathLower.endsWith(".csv") || pathLower.endsWith(".json")) {
                try {
                    textContent = file.readText()
                    docType = DocumentType.TEXT
                } catch (e: Exception) {
                    errorMessage = "Failed to read document text."
                }
            } else {
                // Try decoding as image first, then as PDF
                val bitmap = BitmapFactory.decodeFile(pdfPath)
                if (bitmap != null) {
                    docType = DocumentType.IMAGE
                    imageBitmap = bitmap
                } else {
                    try {
                        val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                        fileDescriptor = fd
                        val renderer = PdfRenderer(fd)
                        pdfRenderer = renderer
                        pageCount = renderer.pageCount
                        docType = DocumentType.PDF
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // If it fails as PDF, check if text readable
                        try {
                            val text = file.readText().take(5000)
                            if (text.isNotBlank()) {
                                textContent = text
                                docType = DocumentType.TEXT
                            } else {
                                errorMessage = "Unable to render file format. File may be corrupted or unsupported."
                            }
                        } catch (ex: Exception) {
                            errorMessage = "Unable to render PDF or image format."
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(pdfPath) {
        onDispose {
            try {
                pdfRenderer?.close()
                fileDescriptor?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (docType) {
            DocumentType.IMAGE -> {
                if (imageBitmap != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Image(
                                bitmap = imageBitmap!!.asImageBitmap(),
                                contentDescription = "Uploaded Manual Image",
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.Fit
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Image Manual Viewer (${imageBitmap!!.width} x ${imageBitmap!!.height})", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            DocumentType.TEXT -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text("Imported Document Text", style = MaterialTheme.typography.titleMedium)
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(textContent ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
            DocumentType.PDF -> {
                if (pageCount > 0) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(pageCount) { index ->
                            PdfPage(pdfRenderer, index)
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            DocumentType.UNKNOWN -> {
                if (errorMessage != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.ErrorOutline,
                            contentDescription = "Error",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Document View Error",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            errorMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(12.dp))
                            Text("Loading Manual File...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PdfPage(pdfRenderer: PdfRenderer?, pageIndex: Int) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(pdfRenderer, pageIndex) {
        withContext(Dispatchers.IO) {
            if (pdfRenderer != null) {
                try {
                    val page = pdfRenderer.openPage(pageIndex)
                    val b = Bitmap.createBitmap(
                        page.width * 2,
                        page.height * 2,
                        Bitmap.Config.ARGB_8888
                    )
                    b.eraseColor(android.graphics.Color.WHITE)
                    page.render(b, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    bitmap = b
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    if (bitmap != null) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Page ${pageIndex + 1}",
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
            )
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .padding(8.dp)
                .background(Color.LightGray.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(strokeWidth = 2.dp)
        }
    }
}
