package com.nononsenseapps.feeder.ui.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import sh.calvin.reorderable.ReorderableCollectionItemScope
import com.nononsenseapps.feeder.R
import com.nononsenseapps.feeder.ui.compose.theme.LocalDimens
import com.nononsenseapps.feeder.ui.compose.theme.SensibleTopAppBar

/**
 * Main screen for Selection Menu Configuration.
 *
 * @param onNavigateUp Callback when back navigation is requested
 * @param viewModel ViewModel managing screen state
 * @param modifier Modifier for the screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionMenuSettingsScreen(
    onNavigateUp: () -> Unit,
    viewModel: SelectionMenuSettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = modifier,
        contentWindowInsets = TopAppBarDefaults.windowInsets,
        topBar = {
            SensibleTopAppBar(
                scrollBehavior = scrollBehavior,
                title = stringResource(id = R.string.selection_menu_title),
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
    ) { paddingValues ->
        SelectionMenuContent(
            viewState = viewState,
            onEvent = { event -> viewModel.onEvent(event) },
            modifier =
                Modifier
                    .padding(paddingValues)
                    .padding(
                        horizontal = LocalDimens.current.margin,
                        vertical = 8.dp,
                    ),
        )
    }
}

/**
 * Content area for the Selection Menu Configuration screen.
 *
 * @param viewState Current view state
 * @param onEvent Callback for user events
 * @param modifier Modifier for the content
 */
@Composable
private fun SelectionMenuContent(
    viewState: SelectionMenuViewState,
    onEvent: (SelectionMenuEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .width(LocalDimens.current.maxContentWidth),
    ) {
        when {
            viewState.isLoading -> {
                LoadingState(modifier = Modifier.align(Alignment.Center))
            }

            viewState.error != null -> {
                ErrorState(
                    error = viewState.error,
                    onRetry = { onEvent(SelectionMenuEvent.LoadMenus) },
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            viewState.items.isEmpty() -> {
                EmptyState(modifier = Modifier.align(Alignment.Center))
            }

            else -> {
                MenuList(
                    items = viewState.items,
                    onEvent = onEvent,
                )
            }
        }
    }
}

/**
 * Menu list with toggle and drag-to-reorder functionality.
 *
 * Uses Calvin-LL/Reorderable library to enable drag-and-drop reordering of menu items.
 * Follows Moon+ Reader pattern: single flat list with cross-section reordering.
 *
 * @param items List of menu items to display
 * @param onEvent Callback for user events
 */
@Composable
private fun MenuList(
    items: List<SelectionMenuItem>,
    onEvent: (SelectionMenuEvent) -> Unit,
) {
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
        onMove = { from, to ->
            onEvent(SelectionMenuEvent.ReorderMenu(from.index, to.index))
        }
    )

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize()
    ) {
        items(items, key = { it.id }) { item ->
            ReorderableItem(state = reorderableState, key = item.id) { isDragging ->
                MenuItemRow(
                    item = item,
                    onToggle = { onEvent(SelectionMenuEvent.ToggleItem(item.id)) },
                    isDragging = isDragging,
                    dragHandleScope = this,
                )
            }
        }
    }
}

/**
 * Single menu item row with toggle switch.
 *
 * Layout: [Switch] [Icon] [Name + Description] [DragHandle]
 *
 * Note: ReorderableItem provides drag-to-reorder functionality.
 * Long-press on the drag handle initiates drag.
 *
 * @param item The menu item to display
 * @param onToggle Callback when toggle is clicked
 * @param isDragging Whether this item is currently being dragged
 * @param dragHandleScope Scope for the drag handle modifier
 */
@Composable
private fun MenuItemRow(
    item: SelectionMenuItem,
    onToggle: () -> Unit,
    isDragging: Boolean,
    dragHandleScope: ReorderableCollectionItemScope,
) {
    val elevation = if (isDragging) 8.dp else 0.dp

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    if (isDragging) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.surface
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Toggle switch
        Switch(
            checked = item.visible,
            onCheckedChange = { onToggle() },
            modifier =
                Modifier.semantics {
                    this.heading()
                },
        )

        // Icon (if available)
        if (item.icon != null) {
            Icon(
                imageVector = Icons.Outlined.Menu,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Name and description
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (item.description != null) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Drag handle - THIS IS THE KEY FIX
        // Using draggableHandle() modifier to designate drag area
        // This prevents touch conflicts with the Switch component
        Icon(
            imageVector = Icons.Filled.DragHandle,
            contentDescription = stringResource(R.string.selection_menu_drag_to_reorder),
            modifier = with(dragHandleScope) {
                Modifier
                    .size(24.dp)
                    .draggableHandle()
            },
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Loading state displayed while menu items are being loaded.
 *
 * @param modifier Modifier for the loading state
 */
@Composable
private fun LoadingState(
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        CircularProgressIndicator()
    }
}

/**
 * Error state displayed when loading fails.
 *
 * @param error The error message to display
 * @param onRetry Callback when user clicks retry
 * @param modifier Modifier for the error state
 */
@Composable
private fun ErrorState(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = error,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
        )

        // TODO: Add retry button
    }
}

/**
 * Empty state displayed when no menu items are configured.
 *
 * @param modifier Modifier for the empty state
 */
@Composable
private fun EmptyState(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier =
                Modifier.semantics {
                    this.heading()
                },
        ) {
            Icon(
                imageVector = Icons.Outlined.Menu,
                contentDescription = null,
                modifier =
                    Modifier
                        .size(64.dp)
                        .alpha(0.5f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = stringResource(R.string.selection_menu_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Text(
                text = stringResource(R.string.selection_menu_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
