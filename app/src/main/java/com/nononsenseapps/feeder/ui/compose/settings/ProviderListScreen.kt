package com.nononsenseapps.feeder.ui.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nononsenseapps.feeder.R
import com.nononsenseapps.feeder.ai.model.ProviderConfig
import com.nononsenseapps.feeder.ui.compose.theme.LocalDimens
import com.nononsenseapps.feeder.ui.compose.theme.SensibleTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderListScreen(
    onNavigateUp: () -> Unit,
    onNavigateToEditProvider: (String) -> Unit,
    onNavigateToAddProvider: (com.nononsenseapps.feeder.ai.provider.AIProvider) -> Unit,
    viewModel: ProviderListViewModel,
    modifier: Modifier = Modifier,
) {
    val providers by viewModel.providers.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf<ProviderConfig?>(null) }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier =
            modifier
                .semantics { },
        contentWindowInsets = TopAppBarDefaults.windowInsets,
        topBar = {
            SensibleTopAppBar(
                scrollBehavior = scrollBehavior,
                title = stringResource(id = R.string.provider_list_title),
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.go_back),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Default to OpenAI compatible when adding new provider
                    onNavigateToAddProvider(com.nononsenseapps.feeder.ai.provider.AIProvider.OPENAI_COMPATIBLE)
                },
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_provider),
                )
            }
        },
    ) { padding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            when {
                providers.isEmpty() -> {
                    EmptyProvidersState(
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                else -> {
                    ProviderList(
                        providers = providers,
                        onProviderClick = { provider ->
                            onNavigateToEditProvider(provider.id)
                        },
                        onDeleteProvider = { provider ->
                            showDeleteDialog = provider
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    showDeleteDialog?.let { provider ->
        DeleteProviderDialog(
            providerName = provider.name,
            onConfirm = {
                viewModel.deleteProvider(provider.id)
                showDeleteDialog = null
            },
            onDismiss = {
                showDeleteDialog = null
            },
        )
    }
}

@Composable
fun ProviderList(
    providers: List<ProviderConfig>,
    onProviderClick: (ProviderConfig) -> Unit,
    onDeleteProvider: (ProviderConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding =
            LocalDimens.current.let { dimens ->
                androidx.compose.foundation.layout.PaddingValues(
                    horizontal = dimens.margin,
                    vertical = 8.dp,
                )
            },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = providers,
            key = { it.id },
        ) { provider ->
            ProviderListItem(
                provider = provider,
                onClick = { onProviderClick(provider) },
                onDelete = { onDeleteProvider(provider) },
            )
            HorizontalDivider()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderListItem(
    provider: ProviderConfig,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    @Suppress("DEPRECATION")
    val dismissState =
        rememberSwipeToDismissBoxState(
            confirmValueChange = { dismissValue ->
                if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                    onDelete()
                    true
                } else {
                    false
                }
            },
        )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color =
                when (dismissState.dismissDirection) {
                    SwipeToDismissBoxValue.StartToEnd -> Color.Transparent
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                    SwipeToDismissBoxValue.Settled -> Color.Transparent
                }

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(color)
                        .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.scale(1.2f),
                )
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
    ) {
        ProviderItemContent(
            provider = provider,
            onClick = onClick,
            modifier = modifier,
        )
    }
}

@Composable
fun ProviderItemContent(
    provider: ProviderConfig,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .semantics { role = Role.Button }
                .padding(vertical = 16.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Provider icon/type indicator
        Box(
            modifier =
                Modifier
                    .size(48.dp)
                    .background(
                        color =
                            MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text =
                    provider.name
                        .firstOrNull()
                        ?.uppercase()
                        ?.toString() ?: "?",
                style =
                    MaterialTheme.typography.titleLarge.copy(
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Provider details
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = provider.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text =
                    stringResource(
                        id =
                            when (provider.providerType) {
                                com.nononsenseapps.feeder.ai.provider.AIProvider.OPENAI_COMPATIBLE -> R.string.ai_provider_openai_compatible
                                com.nononsenseapps.feeder.ai.provider.AIProvider.ANTHROPIC -> R.string.ai_provider_anthropic_compatible
                            },
                    ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (provider.isActive) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.active_provider),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        // Edit icon
        Icon(
            Icons.Default.Edit,
            contentDescription = stringResource(R.string.edit),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun EmptyProvidersState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.no_providers_configured),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.add_provider_to_get_started),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun DeleteProviderDialog(
    providerName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.delete_provider))
        },
        text = {
            Text(
                stringResource(
                    R.string.delete_provider_confirmation,
                    providerName,
                ),
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = onConfirm,
            ) {
                Text(stringResource(R.string.delete))
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(
                onClick = onDismiss,
            ) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}
