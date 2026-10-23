package com.m4isper.contactsapp.presentation.editor.rows

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.contact.AddressKind
import com.m4isper.contactsapp.domain.model.contact.EditableAddress
import com.m4isper.contactsapp.presentation.common.format.addressKindLabel
import com.m4isper.contactsapp.presentation.editor.EditorRow
import com.m4isper.contactsapp.presentation.editor.components.KindMenu
import com.m4isper.contactsapp.ui.theme.LocalSpacing

@Composable
internal fun AddressEditorRow(
    row: EditorRow<EditableAddress>,
    onStreetChange: (Long, String) -> Unit,
    onPoBoxChange: (Long, String) -> Unit,
    onNeighborhoodChange: (Long, String) -> Unit,
    onCityChange: (Long, String) -> Unit,
    onRegionChange: (Long, String) -> Unit,
    onPostcodeChange: (Long, String) -> Unit,
    onCountryChange: (Long, String) -> Unit,
    onKindChange: (Long, AddressKind) -> Unit,
    onCustomLabelChange: (Long, String) -> Unit,
    onRemove: (Long) -> Unit,
) {
    val spacing = LocalSpacing.current
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.m, vertical = spacing.xs),
    ) {
        Column(modifier = Modifier.padding(spacing.m)) {
            OutlinedTextField(
                value = row.value.streetLine,
                onValueChange = { onStreetChange(row.rowId, it) },
                label = { Text(stringResource(R.string.editor_address_street)) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                    ),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag("editor_address_field"),
            )
            Spacer(modifier = Modifier.height(spacing.s))
            OutlinedTextField(
                value = row.value.poBox,
                onValueChange = { onPoBoxChange(row.rowId, it) },
                label = { Text(stringResource(R.string.editor_address_po_box)) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag("editor_address_po_box_field"),
            )
            Spacer(modifier = Modifier.height(spacing.s))
            OutlinedTextField(
                value = row.value.neighborhood,
                onValueChange = { onNeighborhoodChange(row.rowId, it) },
                label = { Text(stringResource(R.string.editor_address_neighborhood)) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag("editor_address_neighborhood_field"),
            )
            Spacer(modifier = Modifier.height(spacing.s))
            Row {
                OutlinedTextField(
                    value = row.value.city,
                    onValueChange = { onCityChange(row.rowId, it) },
                    label = { Text(stringResource(R.string.editor_address_city)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier =
                        Modifier
                            .weight(1f)
                            .testTag("editor_address_city_field"),
                )
                Spacer(modifier = Modifier.width(spacing.xs))
                OutlinedTextField(
                    value = row.value.region,
                    onValueChange = { onRegionChange(row.rowId, it) },
                    label = { Text(stringResource(R.string.editor_address_region)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier =
                        Modifier
                            .weight(1f)
                            .testTag("editor_address_region_field"),
                )
            }
            Spacer(modifier = Modifier.height(spacing.s))
            Row {
                OutlinedTextField(
                    value = row.value.postcode,
                    onValueChange = { onPostcodeChange(row.rowId, it) },
                    label = { Text(stringResource(R.string.editor_address_postcode)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier =
                        Modifier
                            .weight(1f)
                            .testTag("editor_address_postcode_field"),
                )
                Spacer(modifier = Modifier.width(spacing.xs))
                OutlinedTextField(
                    value = row.value.country,
                    onValueChange = { onCountryChange(row.rowId, it) },
                    label = { Text(stringResource(R.string.editor_address_country)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier =
                        Modifier
                            .weight(1f)
                            .testTag("editor_address_country_field"),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                KindMenu(
                    current = row.value.kind,
                    options = AddressKind.entries,
                    label = { addressKindLabel(it) },
                    onSelect = { onKindChange(row.rowId, it) },
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { onRemove(row.rowId) }) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.action_delete_address),
                    )
                }
            }
            if (row.value.kind == AddressKind.OTHER) {
                OutlinedTextField(
                    value = row.value.customLabel.orEmpty(),
                    onValueChange = { onCustomLabelChange(row.rowId, it) },
                    label = { Text(stringResource(R.string.editor_kind_label)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done,
                        ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
