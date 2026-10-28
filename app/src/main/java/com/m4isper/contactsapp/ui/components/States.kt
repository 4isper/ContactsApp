package com.m4isper.contactsapp.ui.components

import android.provider.Settings
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.common.AppError
import com.m4isper.contactsapp.presentation.common.message
import com.m4isper.contactsapp.ui.theme.LocalSpacing
import com.m4isper.contactsapp.ui.theme.PillShape

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        AppLoadingIndicator()
    }
}

/** Скелетон-карточки контактов: показывают структуру списка пока загружаются реальные данные. */
@Composable
fun ListSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 5,
) {
    val spacing = LocalSpacing.current
    val loadingText = stringResource(R.string.a11y_loading_contacts)
    // Пульс живёт только когда система вообще анимирует: при выключенных
    // анимациях скелетон статичен, но остаётся читаемым.
    val animationsEnabled =
        Settings.Global.getFloat(
            LocalContext.current.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) > 0f
    val pulse =
        if (animationsEnabled) {
            rememberInfiniteTransition(label = "skeleton")
                .animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec =
                        infiniteRepeatable(
                            animation = tween(durationMillis = 1000),
                            repeatMode = RepeatMode.Reverse,
                        ),
                    label = "skeleton_pulse",
                ).value
        } else {
            1f
        }
    val animatedAlpha = skeletonAlpha(pulse)

    Column(
        // Скелетон для TalkBack — одно «загружаем контакты», а не набор пустых карточек.
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = spacing.m)
                .clearAndSetSemantics { contentDescription = loadingText },
    ) {
        repeat(itemCount) {
            Card(
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = spacing.xs)
                        .clip(MaterialTheme.shapes.medium),
            ) {
                Column(modifier = Modifier.padding(spacing.m)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(spacing.avatarList)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        )
                        Spacer(modifier = Modifier.width(spacing.m))
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SkeletonLine(width = 60f, height = 16f, alpha = animatedAlpha)
                            Spacer(modifier = Modifier.height(spacing.xs))
                            SkeletonLine(width = 90f, height = 14f, alpha = animatedAlpha)
                        }
                    }
                }
            }
        }
    }
}

internal const val SKELETON_ALPHA_MIN = 0.35f

/** Альфа скелетона по прогрессу пульса: монотонно от [SKELETON_ALPHA_MIN] до 1. */
internal fun skeletonAlpha(progress: Float): Float = SKELETON_ALPHA_MIN + (1f - SKELETON_ALPHA_MIN) * progress

/** Шиммер-линия: пульсирующая полоса, имитирующая текст. */
@Composable
private fun SkeletonLine(
    width: Float,
    height: Float,
    alpha: Float,
) {
    Box(
        modifier =
            Modifier
                .size(width.dp, height.dp)
                .background(
                    Brush.linearGradient(
                        colors =
                            listOf(
                                MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = alpha),
                                MaterialTheme.colorScheme.surfaceContainer.copy(alpha = alpha),
                                MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = alpha),
                            ),
                    ),
                ),
    )
}

@Composable
fun ErrorState(
    error: AppError?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(spacing.xl),
        ) {
            Text(
                text = error?.message() ?: stringResource(R.string.contacts_load_error),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(spacing.m))
            Button(
                onClick = onRetry,
                shape = PillShape,
            ) {
                Text(stringResource(R.string.action_retry))
            }
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    primaryLabel: String? = null,
    onPrimaryClick: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
) {
    val spacing = LocalSpacing.current
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            // Причина и действия читаются TalkBack одним фокусом; кнопки остаются
            // отдельными целями из-за собственной семантики клика.
            modifier =
                Modifier
                    .padding(spacing.xl)
                    .semantics(mergeDescendants = true) {},
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(spacing.xxl + spacing.m),
            )
            Spacer(modifier = Modifier.height(spacing.m))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (primaryLabel != null && onPrimaryClick != null) {
                Spacer(modifier = Modifier.height(spacing.l))
                Button(
                    onClick = onPrimaryClick,
                    shape = PillShape,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = primaryLabel)
                }
            }
            if (secondaryLabel != null && onSecondaryClick != null) {
                Spacer(modifier = Modifier.height(spacing.s))
                OutlinedButton(
                    onClick = onSecondaryClick,
                    shape = PillShape,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = secondaryLabel)
                }
            }
        }
    }
}
