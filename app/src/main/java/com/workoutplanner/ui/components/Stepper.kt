package com.workoutplanner.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** `− 12 +` control, so numbers can be changed without opening the keyboard. */
@Composable
fun Stepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    range: IntRange = 1..999,
    step: Int = 1,
    display: (Int) -> String = { it.toString() },
    buttonSize: Dp = 32.dp,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        StepButton("−", "Decrease $label", enabled = value > range.first, size = buttonSize) {
            onValueChange((value - step).coerceIn(range))
        }
        Text(
            display(value),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(if (buttonSize > 32.dp) 56.dp else 40.dp),
        )
        StepButton("+", "Increase $label", enabled = value < range.last, size = buttonSize) {
            onValueChange((value + step).coerceIn(range))
        }
    }
}

@Composable
private fun StepButton(symbol: String, description: String, enabled: Boolean, size: Dp, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.size(size).semantics { contentDescription = description },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                symbol,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.38f),
            )
        }
    }
}
