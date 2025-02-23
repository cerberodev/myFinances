@file:OptIn(ExperimentalMaterial3Api::class)

package utils.views.textfield

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import theme.spacing_2
import theme.spacing_4

@Composable
fun DayPicker(
    dateValue: String,
    showError: Boolean,
    dayValueInMillis: (Long) -> Unit,
    modifier: Modifier = Modifier,
    dateInMillis: Long? = null,
) {
    val initialDateMillis =
        if (dateInMillis != null && dateInMillis > 0L) {
            dateInMillis
        } else {
            null
        }
    var isVisible by remember { mutableStateOf(false) }
    val datePickerState =
        rememberDatePickerState(
            initialSelectedDateMillis = initialDateMillis,
        )
    val keyboard = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value = dateValue,
        onValueChange = {},
        label = {
            Text(
                text = "Enter day",
                color = Color.Black,
            )
        },
        keyboardActions = KeyboardActions(),
        shape = MaterialTheme.shapes.small,
        readOnly = true,
        modifier = modifier
            .padding(top = spacing_2)
            .fillMaxWidth()
            .clickable {
                keyboard?.hide()
                isVisible = true
            },
        enabled = false,
        leadingIcon = {
            Icon(
                Icons.Default.CalendarMonth,
                contentDescription = null,
            )
        },
    )
    AnimatedVisibility(showError) {
        Text(
            text = "Enter a date",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(start = spacing_4),
        )
    }
    AnimatedVisibility(isVisible) {
        DatePickerDialog(
            onDismissRequest = {
                isVisible = false
            },
            shape = MaterialTheme.shapes.small,
            modifier = Modifier,
            tonalElevation = 8.dp,
            confirmButton = {
                Button(
                    onClick = {
                        isVisible = false
                    },
                ) {
                    Text("Ok")
                }
            },
            content = {
                DatePicker(
                    state = datePickerState,
                )
            },
        )
    }
    datePickerState.selectedDateMillis?.let {
        dayValueInMillis(it)
    }
}
