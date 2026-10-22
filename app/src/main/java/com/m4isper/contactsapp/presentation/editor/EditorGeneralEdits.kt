package com.m4isper.contactsapp.presentation.editor

import com.m4isper.contactsapp.domain.model.Birthday

/** Правки формы: общие поля. */
internal fun EditorUiState.withNickname(value: String): EditorUiState = copy(nickname = nickname.copy(nickname = value))

internal fun EditorUiState.withOrganizationCompany(value: String): EditorUiState =
    copy(organization = organization.copy(company = value))

internal fun EditorUiState.withOrganizationTitle(value: String): EditorUiState =
    copy(organization = organization.copy(title = value))

internal fun EditorUiState.withOrganizationDepartment(value: String): EditorUiState =
    copy(organization = organization.copy(department = value))

internal fun EditorUiState.withOrganizationJobDescription(value: String): EditorUiState =
    copy(organization = organization.copy(jobDescription = value))

internal fun EditorUiState.withOrganizationOfficeLocation(value: String): EditorUiState =
    copy(organization = organization.copy(officeLocation = value))

internal fun EditorUiState.withNote(value: String): EditorUiState = copy(note = note.copy(note = value))

internal fun EditorUiState.withBirthday(value: Birthday?): EditorUiState = copy(birthday = birthday.copy(value = value))

internal fun EditorUiState.withToggledGroup(groupId: Long): EditorUiState =
    copy(
        selectedGroupIds =
            if (groupId in selectedGroupIds) {
                selectedGroupIds - groupId
            } else {
                selectedGroupIds + groupId
            },
    )
