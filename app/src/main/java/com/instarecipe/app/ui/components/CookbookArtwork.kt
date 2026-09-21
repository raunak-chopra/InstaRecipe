package com.instarecipe.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.instarecipe.app.ui.theme.AccentTomatoBright
import com.instarecipe.app.ui.theme.AppRadii
import com.instarecipe.app.ui.theme.SurfaceCanvas
import com.instarecipe.app.ui.theme.SurfaceRaised
import com.instarecipe.app.ui.theme.TextPrimary
import com.instarecipe.app.ui.theme.TextSecondary

/** A flat, text-led placeholder for recipes without a verified image provenance URL. */
@Composable
fun CookbookArtwork(
    category: String,
    title: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(SurfaceCanvas)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 34.dp, y = 28.dp)
                .size(170.dp)
                .clip(CircleShape)
                .background(AccentTomatoBright.copy(alpha = 0.9f))
        )
        Box(
            modifier = Modifier
                .size(148.dp)
                .rotate(-8f)
                .clip(CircleShape)
                .background(SurfaceRaised)
                .border(1.dp, TextPrimary.copy(alpha = 0.25f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(width = 118.dp, height = 54.dp)
                .rotate(7f)
                .clip(RoundedCornerShape(AppRadii.hairline))
                .background(TextPrimary.copy(alpha = 0.1f))
        )
        Column(
            modifier = Modifier.align(Alignment.TopStart).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                category.ifBlank { "Kitchen Journal" }.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "A recipe worth keeping",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Text(
            title.take(1).uppercase(),
            style = MaterialTheme.typography.displayMedium.copy(fontSize = 52.sp),
            color = TextPrimary.copy(alpha = 0.82f),
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
