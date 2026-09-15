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
import java.math.BigDecimal
import java.math.RoundingMode

object ServingScaler {

    private const val NUMBER_TOKEN =
        "(?:\\d+\\s+\\d+/\\d+|\\d+/\\d+|\\d+(?:\\.\\d+)?|\\d*\\s*[½⅓⅔¼¾⅛⅜⅝⅞])"
    private val quantityRegex = Regex("^($NUMBER_TOKEN(?:\\s*[-–]\\s*$NUMBER_TOKEN)?)\\s*(.*)$")

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

        val scaledQuantity = numberPart.split(Regex("\\s*[-–]\\s*"))
            .map { parseQuantity(it) ?: return trimmed }
            .joinToString("–") { value -> format(value * BigDecimal.valueOf(factor.toDouble())) }
        return "$scaledQuantity $rest".trim()
    }

    private fun parseQuantity(raw: String): BigDecimal? {
        val normalized = raw.trim()
        UNICODE_FRACTIONS.entries.firstOrNull { normalized.endsWith(it.key) }?.let { (symbol, fraction) ->
            val whole = normalized.removeSuffix(symbol.toString()).trim()
                .takeIf(String::isNotEmpty)?.toBigDecimalOrNull() ?: BigDecimal.ZERO
            return whole + fraction
        }

        val pieces = normalized.split(Regex("\\s+"))
        if (pieces.size == 2 && pieces[1].contains('/')) {
            return pieces[0].toBigDecimalOrNull()?.plus(parseFraction(pieces[1]) ?: return null)
        }
        return if (normalized.contains('/')) parseFraction(normalized) else normalized.toBigDecimalOrNull()
    }

    private fun parseFraction(raw: String): BigDecimal? {
        val pieces = raw.split('/')
        if (pieces.size != 2) return null
        val numerator = pieces[0].toBigDecimalOrNull() ?: return null
        val denominator = pieces[1].toBigDecimalOrNull()?.takeUnless { it.compareTo(BigDecimal.ZERO) == 0 }
            ?: return null
        return numerator.divide(denominator, 8, RoundingMode.HALF_UP)
    }

    private fun format(value: BigDecimal): String = value.setScale(2, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()

    private val UNICODE_FRACTIONS = mapOf(
        '½' to BigDecimal("0.5"),
        '⅓' to BigDecimal.ONE.divide(BigDecimal(3), 8, RoundingMode.HALF_UP),
        '⅔' to BigDecimal(2).divide(BigDecimal(3), 8, RoundingMode.HALF_UP),
        '¼' to BigDecimal("0.25"),
        '¾' to BigDecimal("0.75"),
        '⅛' to BigDecimal("0.125"),
        '⅜' to BigDecimal("0.375"),
        '⅝' to BigDecimal("0.625"),
        '⅞' to BigDecimal("0.875")
    )
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
