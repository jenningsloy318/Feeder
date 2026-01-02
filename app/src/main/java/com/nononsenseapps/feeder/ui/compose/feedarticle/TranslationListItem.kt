package com.nononsenseapps.feeder.ui.compose.feedarticle

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nononsenseapps.feeder.R

/**
 * Composable for displaying translated article content.
 *
 * Shows:
 * - Loading indicator during translation
 * - Error messages with retry option
 * - Translated paragraphs with visual distinction from original
 */
@Composable
fun TranslationListItem(
    state: ArticleTranslationState,
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    when (state) {
        is ArticleTranslationState.Idle -> {
            // No translation requested yet, show nothing
        }
        is ArticleTranslationState.Loading -> {
            TranslationLoadingIndicator(
                progress = state.progress,
                total = state.total,
                modifier = modifier,
            )
        }
        is ArticleTranslationState.Success -> {
            // Translations are displayed inline with paragraphs, so show nothing here
        }
        is ArticleTranslationState.Error -> {
            TranslationError(
                message = state.message,
                retryable = state.retryable,
                onRetry = onRetry,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun TranslationLoadingIndicator(
    progress: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Column {
                Text(
                    text = stringResource(R.string.translation_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = stringResource(
                        R.string.translation_progress,
                        progress,
                        total,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(top = 8.dp),
                )
                LinearProgressIndicator(
                    progress = { progress.toFloat() / total.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun TranslationError(
    message: String,
    retryable: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.translation_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    if (retryable) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onRetry,
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.retry),
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
            }
        }
    }
}
