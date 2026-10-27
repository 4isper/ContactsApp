package com.m4isper.contactsapp.presentation.editor.rows

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.contact.NameMode
import com.m4isper.contactsapp.ui.theme.LocalSpacing

/**
 * Поле имени в двух режимах: одно поле «Полное имя» или компоненты Имя/Фамилия/Отчество
 * и префикс/суффикс под «Дополнительно». Переключение — chevron в конце первого поля;
 * при разворачивании он анимированно поворачивается вверх.
 */
@Composable
internal fun NameEditor(
    mode: NameMode,
    displayName: String,
    given: String,
    family: String,
    middle: String,
    prefix: String,
    suffix: String,
    phoneticGiven: String,
    phoneticMiddle: String,
    phoneticFamily: String,
    onDisplayNameChange: (String) -> Unit,
    onGivenNameChange: (String) -> Unit,
    onFamilyNameChange: (String) -> Unit,
    onMiddleNameChange: (String) -> Unit,
    onPrefixChange: (String) -> Unit,
    onSuffixChange: (String) -> Unit,
    onPhoneticGivenChange: (String) -> Unit,
    onPhoneticMiddleChange: (String) -> Unit,
    onPhoneticFamilyChange: (String) -> Unit,
    onToggleMode: () -> Unit,
) {
    val expanded = mode == NameMode.STRUCTURED
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "nameToggleRotation",
    )
    val toggle: @Composable () -> Unit = {
        NameToggleButton(expanded = expanded, rotation = rotation, onClick = onToggleMode)
    }
    if (expanded) {
        StructuredNameFields(
            given = given,
            family = family,
            middle = middle,
            prefix = prefix,
            suffix = suffix,
            phoneticGiven = phoneticGiven,
            phoneticMiddle = phoneticMiddle,
            phoneticFamily = phoneticFamily,
            onGivenNameChange = onGivenNameChange,
            onFamilyNameChange = onFamilyNameChange,
            onMiddleNameChange = onMiddleNameChange,
            onPrefixChange = onPrefixChange,
            onSuffixChange = onSuffixChange,
            onPhoneticGivenChange = onPhoneticGivenChange,
            onPhoneticMiddleChange = onPhoneticMiddleChange,
            onPhoneticFamilyChange = onPhoneticFamilyChange,
            toggle = toggle,
        )
    } else {
        val spacing = LocalSpacing.current
        NameField(
            value = displayName,
            onValueChange = onDisplayNameChange,
            labelRes = R.string.editor_name_label,
            testTag = "editor_name_field",
            trailingIcon = toggle,
            modifier = Modifier.padding(horizontal = spacing.m),
        )
    }
}

/** Chevron-тоггл режима имени; rotation анимирует поворот между «вниз» и «вверх». */
@Composable
private fun NameToggleButton(
    expanded: Boolean,
    rotation: Float,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.testTag("editor_toggle_name_mode"),
    ) {
        Icon(
            imageVector = Icons.Filled.ExpandMore,
            contentDescription =
                stringResource(
                    if (expanded) R.string.editor_name_switch_display else R.string.editor_name_switch_structured,
                ),
            modifier = Modifier.rotate(rotation),
        )
    }
}

@Composable
private fun StructuredNameFields(
    given: String,
    family: String,
    middle: String,
    prefix: String,
    suffix: String,
    phoneticGiven: String,
    phoneticMiddle: String,
    phoneticFamily: String,
    onGivenNameChange: (String) -> Unit,
    onFamilyNameChange: (String) -> Unit,
    onMiddleNameChange: (String) -> Unit,
    onPrefixChange: (String) -> Unit,
    onSuffixChange: (String) -> Unit,
    onPhoneticGivenChange: (String) -> Unit,
    onPhoneticMiddleChange: (String) -> Unit,
    onPhoneticFamilyChange: (String) -> Unit,
    toggle: @Composable () -> Unit,
) {
    val spacing = LocalSpacing.current
    var detailsExpanded by rememberSaveable { mutableStateOf(false) }
    val showDetails =
        detailsExpanded ||
            prefix.isNotBlank() ||
            suffix.isNotBlank() ||
            phoneticGiven.isNotBlank() ||
            phoneticMiddle.isNotBlank() ||
            phoneticFamily.isNotBlank()
    val detailsRotation by animateFloatAsState(
        targetValue = if (showDetails) 180f else 0f,
        label = "nameDetailsRotation",
    )
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.m),
    ) {
        NameField(
            value = given,
            onValueChange = onGivenNameChange,
            labelRes = R.string.editor_name_given,
            testTag = "editor_name_given",
            trailingIcon = toggle,
        )
        Spacer(modifier = Modifier.height(spacing.s))
        NameField(
            value = family,
            onValueChange = onFamilyNameChange,
            labelRes = R.string.editor_name_family,
            testTag = "editor_name_family",
        )
        Spacer(modifier = Modifier.height(spacing.s))
        NameField(
            value = middle,
            onValueChange = onMiddleNameChange,
            labelRes = R.string.editor_name_middle,
            testTag = "editor_name_middle",
        )
        if (showDetails) {
            Spacer(modifier = Modifier.height(spacing.s))
            NameField(
                value = prefix,
                onValueChange = onPrefixChange,
                labelRes = R.string.editor_name_prefix,
                testTag = "editor_name_prefix",
            )
            Spacer(modifier = Modifier.height(spacing.s))
            NameField(
                value = suffix,
                onValueChange = onSuffixChange,
                labelRes = R.string.editor_name_suffix,
                testTag = "editor_name_suffix",
            )
            Spacer(modifier = Modifier.height(spacing.s))
            NameField(
                value = phoneticGiven,
                onValueChange = onPhoneticGivenChange,
                labelRes = R.string.editor_name_phonetic_given,
                testTag = "editor_name_phonetic_given",
            )
            Spacer(modifier = Modifier.height(spacing.s))
            NameField(
                value = phoneticMiddle,
                onValueChange = onPhoneticMiddleChange,
                labelRes = R.string.editor_name_phonetic_middle,
                testTag = "editor_name_phonetic_middle",
            )
            Spacer(modifier = Modifier.height(spacing.s))
            NameField(
                value = phoneticFamily,
                onValueChange = onPhoneticFamilyChange,
                labelRes = R.string.editor_name_phonetic_family,
                testTag = "editor_name_phonetic_family",
                imeAction = ImeAction.Done,
            )
        }
        TextButton(
            onClick = { detailsExpanded = !detailsExpanded },
            modifier = Modifier.testTag("editor_toggle_name_details"),
        ) {
            Text(stringResource(R.string.editor_name_details))
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.rotate(detailsRotation),
            )
        }
    }
}

@Composable
private fun NameField(
    value: String,
    onValueChange: (String) -> Unit,
    labelRes: Int,
    testTag: String,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Next,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(labelRes)) },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = imeAction,
            ),
        trailingIcon = trailingIcon,
        modifier =
            modifier
                .fillMaxWidth()
                .testTag(testTag),
    )
}
