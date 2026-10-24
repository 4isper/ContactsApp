package com.m4isper.contactsapp.ui.theme

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Брендовые иконки: стандартные Material-иконки в фирменных цветах (тейл / янтарь).
 * Используются вместо неокрашенных Material-иконок в наиболее заметных местах UI,
 * чтобы приложение приобрело собственную визуальную идентичность.
 *
 * Цвета по бренд-гайдлайну:
 * - primary (тейл 0xFF00696B / 0xFF80D5C6) — основные действия
 * - tertiary (янтарь 0xFF7E5700 / 0xFFFFBE2E) — второстепенные и акцентные
 *
 * Правила использования:
 * - Call, Favorite (активный), Group — primary (тейл)
 * - Message, Import, Export, Settings — tertiary (янтарь) или onSurfaceVariant
 * - Favorite (неактивный), Search — onSurfaceVariant (декоративные)
 * - Размер иконок: 24 dp (кроме мелких acción-кнопок — 20 dp)
 */

object BrandIcons {
    val Call = Icons.Filled.Call
    val Message = Icons.AutoMirrored.Filled.Message
    val Favorite = Icons.Filled.Favorite
    val FavoriteBorder = Icons.Filled.FavoriteBorder
    val Group = Icons.Filled.Group
    val Settings = Icons.Filled.Settings
    val Search = Icons.Filled.Search
    val ImportExport = Icons.Filled.ImportExport
}

/** Брендовый цвет для основных действий (тейл). */
val BrandPrimaryColor = Color(0xFF00696B)

/** Брендовый цвет для акцентных действий (янтарь). */
val BrandAccentColor = Color(0xFF7E5700)

/**
 * Брендовая иконка действия: отображает материал-иконку в фирменном цвете.
 * [brandColor] — основной цвет иконки (primary или accent).
 * [enabled] — если false, иконка окрашивается в выключенный оттенок.
 */
@Composable
fun BrandIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    brandColor: Color = BrandPrimaryColor,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
) {
    val disabledTint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    val tint = if (enabled) brandColor else disabledTint
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier.size(size),
    )
}
