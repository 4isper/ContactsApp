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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Иконки действий: стандартные Material-иконки в цветах текущей цветовой схемы.
 * Цвета берутся из [MaterialTheme.colorScheme], поэтому иконки подчиняются
 * светлой/тёмной теме и динамическим цветам (Material You).
 *
 * Правила использования:
 * - Основные действия (звонок, активное избранное) — colorScheme.primary
 * - Вторичные/акцентные действия (сообщение, импорт, экспорт, настройки) — colorScheme.onSurfaceVariant
 * - Декоративные иконки (неактивное избранное, поиск) — colorScheme.onSurfaceVariant
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

/**
 * Иконка действия: отображает материал-иконку в цвете текущей цветовой схемы.
 * [color] — цвет иконки (по умолчанию colorScheme.primary).
 * [enabled] — если false, иконка окрашивается в выключенный оттенок onSurfaceVariant.
 */
@Composable
fun BrandIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    color: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
) {
    val disabledTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    val tint = if (enabled) color else disabledTint
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier.size(size),
    )
}
