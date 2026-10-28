package com.m4isper.contactsapp.presentation.common.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.core.content.FileProvider
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.presentation.common.logw
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val SHARE_TAG = "ContactsShare"

/** Шарит один контакт: vCard-текст собирает слой данных, сюда приходит готовым. */
internal suspend fun shareContact(
    context: Context,
    vCardText: String,
) = shareContacts(context, vCardText)

internal suspend fun shareContacts(
    context: Context,
    vCardText: String,
) {
    if (vCardText.isBlank()) {
        showShareFailed(context)
        return
    }
    // Одно фиксированное имя: повторные шаринги перезаписывают файл, кэш не растёт.
    val file = File(sharedCacheDir(context), "shared_contacts.vcf")
    val written =
        withContext(Dispatchers.IO) {
            runCatching { file.writeText(vCardText) }
                .onFailure { logw(SHARE_TAG, "write shared vCard failed", it) }
                .isSuccess
        }
    if (!written) {
        showShareFailed(context)
        return
    }
    shareFile(context, file, "text/vcard", R.string.share_contact)
}

/** Отдаёт файл из кэша через выбор приложения. */
private fun shareFile(
    context: Context,
    file: File,
    mimeType: String,
    chooserTitle: Int,
) {
    runCatching {
        val uri =
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
        val sendIntent =
            Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(
            Intent
                .createChooser(sendIntent, context.getString(chooserTitle))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }.onFailure { e ->
        logw(SHARE_TAG, "share file failed", e)
        showShareFailed(context)
    }
}

/** Шарит контакт обычным текстом: понимают мессенджеры и заметки. */
internal suspend fun shareContactAsText(
    context: Context,
    text: String,
) {
    if (text.isBlank()) {
        showShareFailed(context)
        return
    }
    runCatching {
        val sendIntent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(
            Intent
                .createChooser(sendIntent, context.getString(R.string.share_contact))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }.onFailure { e ->
        logw(SHARE_TAG, "shareContactAsText failed", e)
        showShareFailed(context)
    }
}

/** Шарит контакт картинкой QR-кода: другой телефон сканирует обычной камерой. */
internal suspend fun shareContactAsQr(
    context: Context,
    vCardText: String,
) {
    val file = writeQrFile(context, vCardText)
    if (file == null) {
        showShareFailed(context)
        return
    }
    shareFile(context, file, "image/png", R.string.share_contact_qr)
}

/** Сохраняет QR-код контакта в кэш: одно имя, чтобы кэш не рос. */
private suspend fun writeQrFile(
    context: Context,
    vCardText: String,
): File? {
    val bitmap = vCardText.takeIf { it.isNotBlank() }?.let { qrCodeBitmap(it) }
    if (bitmap == null) return null
    val file = File(sharedCacheDir(context), QR_FILE_NAME)
    val written =
        withContext(Dispatchers.IO) {
            runCatching {
                file.outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            }.onFailure { logw(SHARE_TAG, "write shared qr failed", it) }.isSuccess
        }
    bitmap.recycle()
    return if (written) file else null
}

/** Имя файла с QR-кодом в кэше: одно, чтобы кэш не рос. */
private const val QR_FILE_NAME = "shared_contact_qr.png"

private fun showShareFailed(context: Context) {
    Toast
        .makeText(
            context,
            context.getString(R.string.share_failed),
            Toast.LENGTH_SHORT,
        ).show()
}

/** Каталог для файлов, отдаваемых наружу через FileProvider: гранты точечные, корень кэша не светится. */
internal fun sharedCacheDir(context: Context): File = File(context.cacheDir, "shared").apply { mkdirs() }
