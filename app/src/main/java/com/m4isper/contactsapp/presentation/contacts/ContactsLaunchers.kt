package com.m4isper.contactsapp.presentation.contacts

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
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.presentation.common.logw
import com.m4isper.contactsapp.presentation.common.permissions.hasPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val EXPORT_FILE_NAME = "contacts.vcf"

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

/** .vcf-импорт и экспорт: выбор файлов, чтение документа и запрос разрешения на запись. */
internal class ContactsLaunchers(
    private val import: ManagedActivityResultLauncher<Array<String>, Uri?>,
    private val export: ManagedActivityResultLauncher<String, Uri?>,
    private val requestWritePermission: ManagedActivityResultLauncher<String, Boolean>,
) {
    /** Открывает выбор .vcf; без разрешения на запись сначала запрашивает его. */
    fun startImport(context: Context) {
        if (hasPermission(context, Manifest.permission.WRITE_CONTACTS)) {
            import.launch(VCARD_IMPORT_MIME_TYPES)
        } else {
            requestWritePermission.launch(Manifest.permission.WRITE_CONTACTS)
        }
    }

    /** Открывает диалог сохранения .vcf. */
    fun startExport() {
        export.launch(EXPORT_FILE_NAME)
    }
}

@Composable
internal fun rememberVCardLaunchers(
    onImportText: (String) -> Unit,
    onExportSelected: (Uri) -> Unit,
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

    val writePermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                importLauncher.launch(VCARD_IMPORT_MIME_TYPES)
            } else {
                Toast.makeText(context, R.string.import_no_permission, Toast.LENGTH_SHORT).show()
            }
        }

    return ContactsLaunchers(
        import = importLauncher,
        export = exportLauncher,
        requestWritePermission = writePermissionLauncher,
    )
}
