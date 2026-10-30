package com.m4isper.contactsapp.presentation.details

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.presentation.common.share.qrCodeBitmap
import com.m4isper.contactsapp.ui.components.AppBottomSheet
import com.m4isper.contactsapp.ui.components.AppLoadingIndicator
import com.m4isper.contactsapp.ui.theme.LocalSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val QrPreviewSize = 240.dp

private val QrCardPadding = 8.dp

private val QrHintPadding = 12.dp

/**
 * Показывает QR-код контакта в нижнем листе: второй телефон сканирует его обычной камерой.
 * Кодирование идёт в фоне, поэтому лист сначала показывает заглушку.
 */
@Composable
internal fun QrPreviewSheet(
    contactName: String,
    vCardText: String,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
    encode: suspend (String) -> Bitmap? = { withContext(Dispatchers.Default) { qrCodeBitmap(it) } },
) {
    val spacing = LocalSpacing.current
    var bitmap by remember(vCardText) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(vCardText) { mutableStateOf(true) }

    LaunchedEffect(vCardText) {
        isLoading = true
        bitmap = encode(vCardText)
        isLoading = false
    }

    AppBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("qr_preview_dialog"),
    ) {
        Text(
            text = contactName,
            style = MaterialTheme.typography.titleLarge,
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.m),
        ) {
            QrCard {
                when {
                    isLoading ->
                        AppLoadingIndicator(
                            color = Color.Black,
                            trackColor = Color.Black.copy(alpha = 0.12f),
                            modifier = Modifier.testTag("qr_preview_placeholder"),
                        )

                    bitmap == null ->
                        Text(
                            text = stringResource(R.string.share_failed),
                            textAlign = TextAlign.Center,
                        )

                    else ->
                        Image(
                            bitmap = bitmap!!.asImageBitmap(),
                            contentDescription = null,
                            modifier =
                                Modifier
                                    .size(QrPreviewSize)
                                    .testTag("qr_preview_image"),
                        )
                }
            }
            Text(
                text = stringResource(R.string.qr_scan_hint),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = QrHintPadding),
            )
        }
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.s),
        ) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("qr_preview_close"),
            ) {
                Text(stringResource(R.string.action_close))
            }
            if (bitmap != null) {
                TextButton(
                    onClick = onShare,
                    modifier = Modifier.testTag("qr_preview_share"),
                ) {
                    Text(stringResource(R.string.qr_preview_share))
                }
            }
        }
    }
}

/** Белая подложка: тихая зона кодера не обрезается, контраст не зависит от темы. */
@Composable
private fun QrCard(content: @Composable () -> Unit) {
    Box(
        modifier =
            Modifier
                .clip(MaterialTheme.shapes.medium)
                .background(Color.White)
                .padding(QrCardPadding),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
