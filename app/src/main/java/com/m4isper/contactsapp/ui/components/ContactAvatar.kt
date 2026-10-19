package com.m4isper.contactsapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

/**
 * Круглый аватар: фото контакта поверх инициала.
 * Если фото нет или не загрузилось — виден инициал.
 */
@Composable
fun ContactAvatar(
    name: String,
    photoUri: String?,
    size: Dp,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    Box(
        modifier =
            modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.firstOrNull()?.toString() ?: "?",
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold,
            style = textStyle,
        )
        if (photoUri != null) {
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

@Composable
fun ListAvatar(
    name: String,
    photoUri: String?,
    modifier: Modifier = Modifier,
    size: Dp = LocalSpacing.current.avatarList,
) {
    ContactAvatar(
        name = name,
        photoUri = photoUri,
        size = size,
        textStyle = MaterialTheme.typography.titleMedium,
        modifier = modifier,
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
