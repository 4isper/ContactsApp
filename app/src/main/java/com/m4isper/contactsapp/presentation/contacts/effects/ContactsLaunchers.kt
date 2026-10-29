package com.m4isper.contactsapp.presentation.contacts.effects

import android.Manifest
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.presentation.common.logw
import com.m4isper.contactsapp.presentation.common.permissions.hasPermission
import com.m4isper.contactsapp.presentation.common.permissions.openSettingsIfPermanentlyDenied
import com.m4isper.contactsapp.presentation.common.share.QrScanContent
import com.m4isper.contactsapp.presentation.common.share.qrScanContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val EXPORT_FILE_NAME = "contacts.vcf"

private const val EXPORT_SELECTION_FILE_NAME = "selected-contacts.vcf"

private const val IMPORT_TAG = "ContactsImport"

/** MIME-типы, под которыми файловые менеджеры отдают .vcf. */
private val VCARD_IMPORT_MIME_TYPES =
    arrayOf(
        "text/vcard",
        "text/x-vcard",
        "text/directory",
        "text/plain",
        "application/octet-stream",
    )

/** .vcf-импорт и экспорт: выбор файлов, чтение документа, сканер QR и запросы разрешений. */
internal class ContactsLaunchers(
    private val import: ManagedActivityResultLauncher<Array<String>, Uri?>,
    private val export: ManagedActivityResultLauncher<String, Uri?>,
    private val exportSelection: ManagedActivityResultLauncher<String, Uri?>,
    private val scan: ManagedActivityResultLauncher<ScanOptions, ScanIntentResult>,
    private val requestWritePermission: ManagedActivityResultLauncher<String, Boolean>,
    private val requestCameraPermission: ManagedActivityResultLauncher<String, Boolean>,
) {
    /** Открывает выбор .vcf; без разрешения на запись сначала запрашивает его. */
    fun startImport(context: Context) {
        if (hasPermission(context, Manifest.permission.WRITE_CONTACTS)) {
            import.launch(VCARD_IMPORT_MIME_TYPES)
        } else {
            requestWritePermission.launch(Manifest.permission.WRITE_CONTACTS)
        }
    }

    /** Открывает диалог сохранения .vcf всех контактов. */
    fun startExport() {
        export.launch(EXPORT_FILE_NAME)
    }

    /** Открывает диалог сохранения .vcf выделенных контактов. */
    fun startExportSelection() {
        exportSelection.launch(EXPORT_SELECTION_FILE_NAME)
    }

    /** Открывает сканер QR; без разрешения на камеру сначала запрашивает его. */
    fun startScan(context: Context) {
        if (hasPermission(context, Manifest.permission.CAMERA)) {
            scan.launch(scanOptions(context))
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }
}

/** Сканируем только QR: другие форматы контакт не переносят. */
private fun scanOptions(context: Context): ScanOptions =
    ScanOptions()
        .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        .setPrompt(context.getString(R.string.scan_qr_prompt))
        .setBeepEnabled(false)

@Composable
internal fun rememberVCardLaunchers(
    onImportText: (String) -> Unit,
    onExportSelected: (Uri) -> Unit,
    onExportSelectionSelected: (Uri) -> Unit,
): ContactsLaunchers {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                scope.launch {
                    // Битый или отозванный документ не должен ронять экран: читаем безопасно,
                    // при любой ошибке пользователь увидит общий тост о неудачном импорте.
                    val text =
                        withContext(Dispatchers.IO) {
                            runCatching {
                                context.contentResolver
                                    .openInputStream(uri)
                                    ?.bufferedReader()
                                    ?.use { it.readText() }
                            }.onFailure { logw(IMPORT_TAG, "import file read failed", it) }.getOrNull()
                        }
                    if (text.isNullOrBlank()) {
                        Toast.makeText(context, R.string.import_failed, Toast.LENGTH_SHORT).show()
                    } else {
                        onImportText(text)
                    }
                }
            }
        }

    val exportLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("text/vcard"),
        ) { uri ->
            uri?.let(onExportSelected)
        }

    val exportSelectionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("text/vcard"),
        ) { uri ->
            uri?.let(onExportSelectionSelected)
        }

    val writePermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                importLauncher.launch(VCARD_IMPORT_MIME_TYPES)
            } else {
                Toast.makeText(context, R.string.import_no_permission, Toast.LENGTH_SHORT).show()
            }
        }

    val scanLauncher =
        rememberLauncherForActivityResult(ScanContract()) { result ->
            // Отмена сканера приходит без содержимого: молча остаёмся на списке.
            val contents = result.contents ?: return@rememberLauncherForActivityResult
            when (val content = qrScanContent(contents)) {
                is QrScanContent.VCard -> onImportText(content.text)
                QrScanContent.NotContact ->
                    Toast.makeText(context, R.string.scan_qr_not_contact, Toast.LENGTH_SHORT).show()
            }
        }

    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                scanLauncher.launch(scanOptions(context))
            } else {
                Toast.makeText(context, R.string.scan_qr_no_permission, Toast.LENGTH_SHORT).show()
                openSettingsIfPermanentlyDenied(context, Manifest.permission.CAMERA)
            }
        }

    return ContactsLaunchers(
        import = importLauncher,
        export = exportLauncher,
        exportSelection = exportSelectionLauncher,
        scan = scanLauncher,
        requestWritePermission = writePermissionLauncher,
        requestCameraPermission = cameraPermissionLauncher,
    )
}
