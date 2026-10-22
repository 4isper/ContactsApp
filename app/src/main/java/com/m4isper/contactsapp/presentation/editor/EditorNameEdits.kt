package com.m4isper.contactsapp.presentation.editor

/** Правки формы: имя. */
internal fun EditorUiState.withDisplayName(value: String): EditorUiState = copy(displayName = value)

internal fun EditorUiState.withGivenName(value: String): EditorUiState = copy(givenName = value)

internal fun EditorUiState.withFamilyName(value: String): EditorUiState = copy(familyName = value)

internal fun EditorUiState.withMiddleName(value: String): EditorUiState = copy(middleName = value)

internal fun EditorUiState.withNamePrefix(value: String): EditorUiState = copy(namePrefix = value)

internal fun EditorUiState.withNameSuffix(value: String): EditorUiState = copy(nameSuffix = value)

internal fun EditorUiState.withPhoneticGivenName(value: String): EditorUiState = copy(phoneticGivenName = value)

internal fun EditorUiState.withPhoneticMiddleName(value: String): EditorUiState = copy(phoneticMiddleName = value)

internal fun EditorUiState.withPhoneticFamilyName(value: String): EditorUiState = copy(phoneticFamilyName = value)

internal fun EditorUiState.withToggledNameMode(): EditorUiState = toggledNameMode()
