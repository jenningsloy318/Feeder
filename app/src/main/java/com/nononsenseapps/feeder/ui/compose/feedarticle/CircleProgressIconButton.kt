package com.nononsenseapps.feeder.ui.compose.feedarticle

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * A toolbar icon button that switches between idle (normal icon) and
 * in-progress (circular progress ring + stop square) states.
 *
 * Implements SCENARIO-001, SCENARIO-006, SCENARIO-007, SCENARIO-013,
 * SCENARIO-014, SCENARIO-015 from the BDD scenarios.
 */
@Composable
fun CircleProgressIconButton(
    isInProgress: Boolean,
    progressFraction: (() -> Float)?,
    icon: ImageVector,
    idleContentDescription: String,
    progressContentDescription: String,
    onAction: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isInProgress) {
        Box(
            modifier =
                modifier
                    .size(48.dp)
                    .clickable(
                        onClick = onCancel,
                        role = Role.Button,
                    ).semantics {
                        this.contentDescription = progressContentDescription
                        this.role = Role.Button
                    },
            contentAlignment = Alignment.Center,
        ) {
            if (progressFraction != null) {
                CircularProgressIndicator(
                    progress = progressFraction,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    gapSize = 0.dp,
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                )
            }
            Box(
                modifier =
                    Modifier
                        .size(8.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurface,
                            RoundedCornerShape(1.dp),
                        ),
            )
        }
    } else {
        IconButton(
            onClick = onAction,
            modifier = modifier,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = idleContentDescription,
            )
        }
    }
}
