package com.instarecipe.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.instarecipe.app.ui.theme.DeepBasil
import com.instarecipe.app.ui.theme.DeepBasilContainer
import com.instarecipe.app.ui.theme.HerbMuted
import java.text.DecimalFormat

object ServingScaler {

    private val decimalFormat = DecimalFormat("#.##")
    private val quantityRegex = Regex("""^(\d+(?:[./]\d+)?|\d+\s+\d+/\d+)\s*(.*)$""")

    /**
     * Scales an ingredient string by the given multiplier (1.0f, 2.0f, 4.0f).
     * e.g. "200g paneer cubes" * 2 -> "400g paneer cubes"
     * e.g. "1 tsp chilli flakes" * 2 -> "2 tsp chilli flakes"
     */
    fun scaleIngredient(ingredient: String, factor: Float): String {
        if (factor == 1.0f) return ingredient
        val trimmed = ingredient.trim()
        val match = quantityRegex.find(trimmed) ?: return trimmed

        val numberPart = match.groupValues[1]
        val rest = match.groupValues[2]

        val parsedNumber = parseFractionOrDecimal(numberPart) ?: return trimmed
        val scaledNumber = parsedNumber * factor

        val formattedNumber = decimalFormat.format(scaledNumber)
        return "$formattedNumber $rest".trim()
    }

    private fun parseFractionOrDecimal(str: String): Float? {
        val trimmed = str.trim()
        // Fraction "1/2"
        if (trimmed.contains("/")) {
            val parts = trimmed.split("/")
            if (parts.size == 2) {
                val num = parts[0].toFloatOrNull()
                val den = parts[1].toFloatOrNull()
                if (num != null && den != null && den != 0f) {
                    return num / den
                }
            }
        }
        return trimmed.toFloatOrNull()
    }
}

@Composable
fun ServingScalerSelector(
    currentScale: Float,
    onScaleSelected: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val scales = listOf(1f, 2f, 4f)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(end = 4.dp)
        ) {
            Icon(
                Icons.Default.People,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Text(
                "Servings",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }

        scales.forEach { scale ->
            val isSelected = currentScale == scale
            FilterChip(
                selected = isSelected,
                onClick = { onScaleSelected(scale) },
                label = {
                    Text(
                        "${scale.toInt()}x",
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                    containerColor = Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}
