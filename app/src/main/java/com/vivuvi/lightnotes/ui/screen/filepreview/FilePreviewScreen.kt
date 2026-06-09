package com.vivuvi.lightnotes.ui.screen.filepreview

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

// ─── State ───────────────────────────────────────────────────────────────────

private sealed interface PreviewState {
    data object Loading : PreviewState
    data object ImageReady : PreviewState
    data class PdfReady(val pages: List<Bitmap>) : PreviewState
    data class TextReady(val text: String) : PreviewState
    data class ExternalOnly(val mimeType: String?, val label: String) : PreviewState
    data class Err(val message: String) : PreviewState
}

// ─── Entry point ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilePreviewScreen(
    navController: NavController,
    encodedUri: String,
    /** Original filename supplied by the caller (e.g. from NoteMedia.fileName). */
    overrideName: String? = null,
) {
    val context = LocalContext.current
    val uri = remember { Uri.parse(Uri.decode(encodedUri)) }
    val fileName = remember {
        overrideName?.takeIf { it.isNotBlank() } ?: resolveDisplayName(context, uri)
    }

    // Pre-check extension to set ImageReady immediately — avoids a loading flash
    // for internal-storage images (file:// URIs with .jpg/.png/etc).
    val looksLikeImage = remember(uri) { looksLikeImageUri(uri) }
    val stateHolder = remember {
        mutableStateOf<PreviewState>(if (looksLikeImage) PreviewState.ImageReady else PreviewState.Loading)
    }
    var state by stateHolder

    LaunchedEffect(uri) {
        val resolved = withContext(Dispatchers.IO) { resolveState(context, uri) }
        state = resolved
    }

    DisposableEffect(Unit) {
        onDispose {
            (stateHolder.value as? PreviewState.PdfReady)?.pages?.forEach { it.recycle() }
        }
    }

    val isImage = state is PreviewState.ImageReady

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = fileName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                ,
            )
        },
        containerColor = if (isImage) Color.Black else MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        when (val s = state) {
            PreviewState.Loading -> LoadingContent(paddingValues)
            PreviewState.ImageReady -> ImageContent(uri, fileName, paddingValues)
            is PreviewState.PdfReady -> PdfContent(s.pages, paddingValues)
            is PreviewState.TextReady -> TextContent(s.text, paddingValues)
            is PreviewState.ExternalOnly -> ExternalContent(s, uri, context, paddingValues)
            is PreviewState.Err -> ErrorContent(s.message, paddingValues)
        }
    }
}

// ─── Content composables ──────────────────────────────────────────────────────

@Composable
private fun LoadingContent(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier.fillMaxSize().padding(paddingValues),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ImageContent(uri: Uri, fileName: String, paddingValues: PaddingValues) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, 6f)
        offset = if (newScale > 1f) offset + panChange else Offset.Zero
        scale = newScale
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(paddingValues)
            .clipToBounds()
            .pointerInput("double_tap") {
                detectTapGestures(onDoubleTap = {
                    if (scale > 1f) { scale = 1f; offset = Offset.Zero } else scale = 2.5f
                })
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = uri,
            contentDescription = fileName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y,
                )
                .transformable(state = transformState),
        )
    }
}

@Composable
private fun PdfContent(pages: List<Bitmap>, paddingValues: PaddingValues) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = paddingValues.calculateTopPadding() + 16.dp,
            bottom = paddingValues.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(pages, key = { i, _ -> i }) { _, bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 1.dp, shape = MaterialTheme.shapes.medium),
            )
        }
    }
}

@Composable
private fun TextContent(text: String, paddingValues: PaddingValues) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 24.dp,
            end = 24.dp,
            top = paddingValues.calculateTopPadding() + 16.dp,
            bottom = paddingValues.calculateBottomPadding() + 24.dp,
        ),
    ) {
        item {
            Text(
                text = text.ifBlank { "(Empty document)" },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun ExternalContent(
    state: PreviewState.ExternalOnly,
    uri: Uri,
    context: Context,
    paddingValues: PaddingValues,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = fileTypeIcon(state.mimeType),
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = state.label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            if (state.mimeType != null) {
                Text(
                    text = humanReadableType(state.mimeType),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "This file type cannot be previewed in the app.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    // Convert file:// to a FileProvider content:// URI so external apps
                    // can access it on API 24+ without FileUriExposedException.
                    val shareUri = if (uri.scheme == "file") {
                        val file = File(uri.path ?: return@Button)
                        runCatching {
                            FileProvider.getUriForFile(
                                context, "${context.packageName}.fileprovider", file
                            )
                        }.getOrElse { uri }
                    } else uri
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(shareUri, state.mimeType ?: "*/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    try {
                        context.startActivity(Intent.createChooser(intent, "Open with"))
                    } catch (_: Exception) {}
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(
                    imageVector = Icons.Outlined.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Open with…")
            }
        }
    }
}

@Composable
private fun ErrorContent(message: String, paddingValues: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// ─── Helper composables ───────────────────────────────────────────────────────

private fun fileTypeIcon(mimeType: String?): ImageVector = when {
    mimeType == null -> Icons.Outlined.InsertDriveFile
    mimeType == "application/pdf" -> Icons.Outlined.PictureAsPdf
    mimeType.startsWith("application/vnd.openxmlformats") ||
        mimeType.startsWith("application/msword") -> Icons.Outlined.Article
    else -> Icons.Outlined.InsertDriveFile
}

private fun humanReadableType(mimeType: String): String = when {
    mimeType == "application/pdf" -> "PDF Document"
    mimeType == "application/msword" -> "Word Document (.doc)"
    mimeType.startsWith("application/vnd.openxmlformats-officedocument.wordprocessingml") -> "Word Document (.docx)"
    mimeType.startsWith("application/vnd.openxmlformats-officedocument.spreadsheetml") -> "Excel Spreadsheet"
    mimeType.startsWith("application/vnd.openxmlformats-officedocument.presentationml") -> "PowerPoint Presentation"
    mimeType.startsWith("text/") -> "Text Document"
    mimeType.startsWith("image/") -> "Image (${mimeType.substringAfter('/')})"
    else -> mimeType
}

// ─── File resolution ──────────────────────────────────────────────────────────

private fun resolveDisplayName(context: Context, uri: Uri): String {
    if (uri.scheme == "content") {
        runCatching {
            context.contentResolver.query(
                uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx)?.takeIf { it.isNotBlank() }?.let { return it }
                }
            }
        }
    }
    return uri.lastPathSegment?.takeIf { it.isNotBlank() } ?: "File"
}

private fun resolveMimeType(context: Context, uri: Uri): String? {
    if (uri.scheme == "content") return context.contentResolver.getType(uri)
    val ext = uri.lastPathSegment?.substringAfterLast('.')?.lowercase()?.takeIf { it.isNotBlank() }
        ?: return null
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
}

private fun looksLikeImageUri(uri: Uri): Boolean {
    val ext = uri.lastPathSegment?.substringAfterLast('.')?.lowercase() ?: return false
    return ext in setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif")
}

/** Opens a readable stream regardless of whether the URI is file:// or content://. */
private fun openInputStream(context: Context, uri: Uri): InputStream? =
    if (uri.scheme == "file") {
        runCatching { File(uri.path ?: return null).inputStream() }.getOrNull()
    } else {
        context.contentResolver.openInputStream(uri)
    }

/** Opens a ParcelFileDescriptor for reading, handling both file:// and content://. */
private fun openFileDescriptor(context: Context, uri: Uri): ParcelFileDescriptor? =
    if (uri.scheme == "file") {
        runCatching {
            ParcelFileDescriptor.open(File(uri.path ?: return null), ParcelFileDescriptor.MODE_READ_ONLY)
        }.getOrNull()
    } else {
        context.contentResolver.openFileDescriptor(uri, "r")
    }

private fun resolveState(context: Context, uri: Uri): PreviewState {
    if (uri.toString().isBlank()) return PreviewState.Err("Invalid file URI.")
    val mimeType = resolveMimeType(context, uri)
    val fileName = resolveDisplayName(context, uri)
    return when {
        isImage(mimeType, uri) -> PreviewState.ImageReady
        isPdf(mimeType, fileName) -> renderPdf(context, uri)
        isDocx(mimeType, fileName) -> extractDocxText(context, uri)
        isPlainText(mimeType, fileName) -> readPlainText(context, uri)
        else -> PreviewState.ExternalOnly(mimeType, fileName)
    }
}

private fun isImage(mimeType: String?, uri: Uri) =
    mimeType?.startsWith("image/") == true || looksLikeImageUri(uri)

private fun isPdf(mimeType: String?, fileName: String) =
    mimeType == "application/pdf" || fileName.endsWith(".pdf", ignoreCase = true)

private fun isDocx(mimeType: String?, fileName: String) =
    mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ||
        fileName.endsWith(".docx", ignoreCase = true)

private fun isPlainText(mimeType: String?, fileName: String) =
    mimeType?.startsWith("text/") == true || fileName.endsWith(".txt", ignoreCase = true)

// ─── File loaders ─────────────────────────────────────────────────────────────

private fun renderPdf(context: Context, uri: Uri): PreviewState {
    return try {
        val fd = openFileDescriptor(context, uri)
            ?: return PreviewState.Err("Cannot open file.")
        val renderer = PdfRenderer(fd)
        val scale = 1.5f
        val pages = (0 until renderer.pageCount).map { i ->
            val page = renderer.openPage(i)
            val w = (page.width * scale).toInt()
            val h = (page.height * scale).toInt()
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            Canvas(bmp).drawColor(AndroidColor.WHITE)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            bmp
        }
        renderer.close()
        fd.close()
        PreviewState.PdfReady(pages)
    } catch (e: Exception) {
        PreviewState.Err("Failed to render PDF: ${e.localizedMessage}")
    }
}

private fun extractDocxText(context: Context, uri: Uri): PreviewState {
    return try {
        val xml = openInputStream(context, uri)?.use { inputStream ->
            ZipInputStream(inputStream).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "word/document.xml") return@use zip.bufferedReader().readText()
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
                null
            }
        } ?: return PreviewState.Err("Cannot read document content.")

        val sb = StringBuilder()
        val paraRegex = Regex("<w:p(?:\\s[^>]*)?>([\\s\\S]*?)</w:p>")
        val textRegex = Regex("<w:t(?:\\s[^>]*)?>([^<]*)</w:t>")
        paraRegex.findAll(xml).forEach { para ->
            val paraText = textRegex.findAll(para.value)
                .joinToString("") { it.groupValues[1] }
            if (paraText.isNotBlank()) sb.appendLine(paraText)
        }

        val result = sb.toString().trim()
        if (result.isBlank()) PreviewState.ExternalOnly(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            resolveDisplayName(context, uri),
        ) else PreviewState.TextReady(result)
    } catch (e: Exception) {
        PreviewState.Err("Failed to read document: ${e.localizedMessage}")
    }
}

private fun readPlainText(context: Context, uri: Uri): PreviewState {
    return try {
        val text = openInputStream(context, uri)
            ?.bufferedReader(Charsets.UTF_8)?.readText() ?: ""
        PreviewState.TextReady(text)
    } catch (e: Exception) {
        PreviewState.Err("Failed to read file: ${e.localizedMessage}")
    }
}
