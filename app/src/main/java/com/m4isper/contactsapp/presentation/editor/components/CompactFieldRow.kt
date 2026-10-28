package com.m4isper.contactsapp.presentation.editor.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.m4isper.contactsapp.ui.theme.LocalSpacing

/**
 * Строка поля в одну линию: ручка порядка, вид поля, значение и действия.
 * Дополнительные поля (свой ярлык вида) остаются вторым рядом под значением.
 */
@Composable
internal fun CompactFieldRow(
    testTag: String,
    kind: @Composable () -> Unit,
    value: @Composable (Modifier) -> Unit,
    trailing: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dragHandle: (@Composable () -> Unit)? = null,
    extra: @Composable ColumnScope.() -> Unit = {},
) {
    val spacing = LocalSpacing.current
    Box(modifier = modifier.padding(horizontal = spacing.m, vertical = spacing.xs)) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag(testTag),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                dragHandle?.invoke()
                kind()
                value(Modifier.weight(1f))
                trailing()
            }
            extra()
        }
    }
}
