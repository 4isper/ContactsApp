package com.m4isper.contactsapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.m4isper.contactsapp.ui.theme.LocalSpacing

/** Показывать ли фотографии контактов: выключается в настройках. */
val LocalShowPhotos = staticCompositionLocalOf { true }

/**
 * Круглый аватар: фото контакта поверх инициала.
 * Если фото нет, не загрузилось или отключено в настройках — виден инициал.
 *
 * Если `selected = true` (режим выделения), аватар становится индикатором выбора:
 * фон — `primary`, поверх — галочка `onPrimary`. Это отделяет аватар от фона
 * выбранной карточки и даёт нематериальный признак выделения.
 */
@Composable
fun ContactAvatar(
    name: String,
    photoUri: String?,
    size: Dp,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    testTag: String? = null,
) {
    val bg by animateColorAsState(
        if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.primaryContainer
        },
        label = "avatarBg",
    )
    Box(
        modifier =
            modifier
                .size(size)
                .clip(CircleShape)
                .background(bg)
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(size / 2),
            )
        } else {
            Text(
                text = name.firstOrNull()?.toString() ?: "?",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
                style = textStyle,
            )
            if (photoUri != null && LocalShowPhotos.current) {
                AsyncImage(
                    model =
                        ImageRequest
                            .Builder(LocalContext.current)
                            .data(photoUri)
                            .crossfade(true)
                            .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                )
            }
        }
    }
}

@Composable
fun ListAvatar(
    name: String,
    photoUri: String?,
    modifier: Modifier = Modifier,
    size: Dp = LocalSpacing.current.avatarList,
    selected: Boolean = false,
) {
    ContactAvatar(
        name = name,
        photoUri = photoUri,
        size = size,
        textStyle = MaterialTheme.typography.titleMedium,
        modifier = modifier,
        selected = selected,
    )
}

@Composable
fun DetailsAvatar(
    name: String,
    photoUri: String?,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    ContactAvatar(
        name = name,
        photoUri = photoUri,
        size = spacing.avatarDetails,
        textStyle = MaterialTheme.typography.headlineMedium,
        modifier = modifier,
    )
}
