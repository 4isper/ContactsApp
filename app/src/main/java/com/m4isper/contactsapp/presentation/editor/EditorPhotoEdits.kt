package com.m4isper.contactsapp.presentation.editor

/** Правки формы: фото. */
internal fun EditorUiState.withPhoto(uri: String): EditorUiState = copy(photoUri = uri, photoChanged = true)

internal fun EditorUiState.withoutPhoto(): EditorUiState = copy(photoUri = null, photoChanged = true)
