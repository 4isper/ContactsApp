package com.m4isper.contactsapp.presentation.editor.dialogs

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.exifinterface.media.ExifInterface
import com.m4isper.contactsapp.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

/** Максимальный размер стороны исходной картинки: этого хватает для аватара. */
private const val MAX_SOURCE_PX = 1600

private const val JPEG_QUALITY = 90

/** Множители шага кнопок зума: примерно четверть диапазона MIN..MAX. */
private const val ZOOM_STEP_IN = 1.25f

private const val ZOOM_STEP_OUT = 0.8f

private const val EXIF_DEGREES_90 = 90f

private const val EXIF_DEGREES_180 = 180f

private const val EXIF_DEGREES_270 = 270f

private const val MIN_ZOOM = 1f

private const val MAX_ZOOM = 4f

/**
 * Обрезка фото: пользователь двигает и масштабирует картинку, потом сохраняет кадр.
 * Возвращает URI сохранённого файла — его использует редактор контакта.
 */
@Composable
internal fun CropPhotoDialog(
    source: Uri,
    onConfirm: (Uri) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var failed by remember { mutableStateOf(false) }
    var loadAttempt by remember { mutableStateOf(0) }
    var saveFailed by remember { mutableStateOf(false) }

    LaunchedEffect(source, loadAttempt) {
        failed = false
        val loaded = withContext(Dispatchers.IO) { readBitmap(context, source) }
        if (loaded == null) failed = true else bitmap = loaded
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .systemBarsPadding()
                    .testTag("crop_photo_dialog"),
            contentAlignment = Alignment.Center,
        ) {
            when {
                failed ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.editor_photo_crop_failed),
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 16.dp),
                        ) {
                            TextButton(onClick = { loadAttempt++ }) {
                                Text(stringResource(R.string.action_retry))
                            }
                            TextButton(onClick = onDismiss) {
                                Text(stringResource(R.string.action_cancel))
                            }
                        }
                    }
                bitmap == null -> CircularProgressIndicator()
                else ->
                    CropContent(
                        bitmap = bitmap!!,
                        saveFailed = saveFailed,
                        onConfirm = { cropped ->
                            saveFailed = false
                            scope.launch {
                                val uri = withContext(Dispatchers.IO) { writeCropped(context, cropped) }
                                if (uri != null) {
                                    onConfirm(uri)
                                } else {
                                    // Кадр не выбрасываем: пользователь может повторить сохранение.
                                    saveFailed = true
                                }
                            }
                        },
                        onDismiss = onDismiss,
                    )
            }
        }
    }
}

@Composable
private fun CropContent(
    bitmap: Bitmap,
    saveFailed: Boolean,
    onConfirm: (Bitmap) -> Unit,
    onDismiss: () -> Unit,
) {
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    val canvasDescription = stringResource(R.string.editor_photo_crop_canvas)
    // Картинка вписывается в квадрат, дальше пользователь её двигает и увеличивает.
    val fit =
        if (viewport.width == 0 || viewport.height == 0) {
            1f
        } else {
            minOf(
                viewport.width.toFloat() / bitmap.width,
                viewport.height.toFloat() / bitmap.height,
            )
        }
    val scale = fit * zoom

    fun zoomBy(factor: Float) {
        zoom = (zoom * factor).coerceIn(MIN_ZOOM, MAX_ZOOM)
        // После каждого изменения зума пан пересчитывается: картинка не уезжает за канву.
        pan = clampedPan(pan, Offset.Zero, bitmap.width, bitmap.height, fit * zoom, viewport)
    }

    fun resetCrop() {
        zoom = 1f
        pan = Offset.Zero
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier
                .fillMaxWidth()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState()),
    ) {
        Box(
            modifier =
                Modifier
                    .sizeIn(maxWidth = 420.dp, maxHeight = 420.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(Color.Black)
                    .onSizeChanged { viewport = it }
                    .semantics { contentDescription = canvasDescription }
                    .pointerInput(bitmap) {
                        detectTransformGestures { _, drag, gestureZoom, _ ->
                            zoom = (zoom * gestureZoom).coerceIn(MIN_ZOOM, MAX_ZOOM)
                            pan = clampedPan(pan, drag, bitmap.width, bitmap.height, scale, viewport)
                        }
                    }.testTag("crop_photo_canvas"),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = zoom,
                            scaleY = zoom,
                            translationX = pan.x,
                            translationY = pan.y,
                        ),
            )
        }
        if (saveFailed) {
            Text(
                text = stringResource(R.string.editor_photo_crop_save_failed),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 16.dp),
        ) {
            IconButton(onClick = { zoomBy(ZOOM_STEP_IN) }) {
                Icon(Icons.Filled.ZoomIn, contentDescription = stringResource(R.string.editor_photo_zoom_in))
            }
            IconButton(onClick = { zoomBy(ZOOM_STEP_OUT) }) {
                Icon(Icons.Filled.ZoomOut, contentDescription = stringResource(R.string.editor_photo_zoom_out))
            }
            IconButton(onClick = { resetCrop() }) {
                Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.editor_photo_crop_reset))
            }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("crop_photo_cancel"),
            ) {
                Text(stringResource(R.string.action_cancel))
            }
            TextButton(
                onClick = { onConfirm(cropBitmap(bitmap, viewport, scale, pan)) },
                modifier = Modifier.testTag("crop_photo_confirm"),
            ) {
                Text(stringResource(R.string.editor_photo_crop))
            }
        }
    }
}

/** Кадр в исходных координатах: видимая область превью. */
private fun cropBitmap(
    bitmap: Bitmap,
    viewport: IntSize,
    scale: Float,
    pan: Offset,
): Bitmap {
    if (viewport.width == 0 || viewport.height == 0 || scale <= 0f) return bitmap
    val left = (viewport.width - bitmap.width * scale) / 2 + pan.x
    val top = (viewport.height - bitmap.height * scale) / 2 + pan.y
    val x = ((-left) / scale).roundToInt().coerceIn(0, bitmap.width - 1)
    val y = ((-top) / scale).roundToInt().coerceIn(0, bitmap.height - 1)
    val width = (viewport.width / scale).roundToInt().coerceIn(1, bitmap.width - x)
    val height = (viewport.height / scale).roundToInt().coerceIn(1, bitmap.height - y)
    return Bitmap.createBitmap(bitmap, x, y, width, height)
}

/** Читает картинку с учётом EXIF-ориентации, уменьшая её до разумного размера. */
internal fun readBitmap(
    context: Context,
    uri: Uri,
): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    val options =
        BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, MAX_SOURCE_PX)
        }
    val decoded =
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    val readable = decoded != null && bounds.outWidth > 0 && bounds.outHeight > 0
    return if (readable) upright(context, uri, decoded!!) else null
}

/** Поворачивает картинку по EXIF: портретные снимки с камер часто лежат боком. */
private fun upright(
    context: Context,
    uri: Uri,
    bitmap: Bitmap,
): Bitmap {
    val orientation =
        context.contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
    val degrees =
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> EXIF_DEGREES_90
            ExifInterface.ORIENTATION_ROTATE_180 -> EXIF_DEGREES_180
            ExifInterface.ORIENTATION_ROTATE_270 -> EXIF_DEGREES_270
            else -> return bitmap
        }
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

/** Степень двойки, после которой картинка станет меньше [max]. */
private fun sampleSize(
    width: Int,
    height: Int,
    max: Int,
): Int {
    var sample = 1
    while (width / (sample * 2) >= max || height / (sample * 2) >= max) {
        sample *= 2
    }
    return sample
}

/** Сохраняет обрезанную картинку во временный файл и отдаёт его URI. */
internal fun writeCropped(
    context: Context,
    bitmap: Bitmap,
): Uri? =
    runCatching {
        val file = File(context.cacheDir, "contact_photo_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }
        Uri.fromFile(file)
    }.getOrNull()

/** Пан с учётом сдвига [drag], зажатый по границам картинки для текущего [scale]. */
internal fun clampedPan(
    pan: Offset,
    drag: Offset,
    bitmapWidth: Int,
    bitmapHeight: Int,
    scale: Float,
    viewport: IntSize,
): Offset {
    val limitX = ((bitmapWidth * scale - viewport.width) / 2f).coerceAtLeast(0f)
    val limitY = ((bitmapHeight * scale - viewport.height) / 2f).coerceAtLeast(0f)
    return Offset(
        x = (pan.x + drag.x).coerceIn(-limitX, limitX),
        y = (pan.y + drag.y).coerceIn(-limitY, limitY),
    )
}
