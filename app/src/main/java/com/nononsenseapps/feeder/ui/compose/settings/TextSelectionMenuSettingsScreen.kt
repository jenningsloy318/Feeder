package com.nononsenseapps.feeder.ui.compose.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nononsenseapps.feeder.R
import com.nononsenseapps.feeder.archmodel.MenuItemType
import com.nononsenseapps.feeder.ui.compose.components.safeSemantics
import com.nononsenseapps.feeder.ui.compose.theme.LocalDimens
import com.nononsenseapps.feeder.ui.compose.theme.SensibleTopAppBar
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextSelectionMenuSettingsScreen(
    onNavigateUp: () -> Unit,
    viewModel: TextSelectionMenuSettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    var showResetDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier =
            modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal)),
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            SensibleTopAppBar(
                scrollBehavior = scrollBehavior,
                title = stringResource(R.string.text_selection_menu_settings),
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
    ) { padding ->
        TextSelectionMenuList(
            viewState = viewState,
            onToggleEnabled = viewModel::toggleItemEnabled,
            onMoveItem = viewModel::moveItem,
            onResetToDefaults = { showResetDialog = true },
            modifier =
                Modifier
                    .padding(padding)
                    .navigationBarsPadding(),
        )

        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text(stringResource(R.string.reset_to_defaults)) },
                text = { Text(stringResource(R.string.reset_text_menu_confirmation)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.resetToDefaults()
                            showResetDialog = false
                        },
                    ) {
                        Text(stringResource(R.string.reset))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                },
            )
        }
    }
}

@Composable
fun TextSelectionMenuList(
    viewState: TextSelectionMenuSettingsViewState,
    onToggleEnabled: (MenuItemType) -> Unit,
    onMoveItem: (Int, Int) -> Unit,
    onResetToDefaults: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalDimens.current
    val haptic = LocalHapticFeedback.current

    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val reorderableLazyListState =
        rememberReorderableLazyListState(
            lazyListState = lazyListState,
            onMove = { from, to ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onMoveItem(from.index, to.index)
            },
        )

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = dimens.margin),
    ) {
        LazyColumn(
            state = lazyListState,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
                Modifier
                    .width(dimens.maxContentWidth)
                    .fillMaxSize(),
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.text_selection_menu_description),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                HorizontalDivider()
            }

            itemsIndexed(
                items = viewState.menuItems,
                key = { _, item ->
                    // Create unique key based on type and specific app
                    if (item.specificApp != null) {
                        "${item.type}_${item.specificApp.packageName}"
                    } else {
                        item.type.name
                    }
                },
            ) { index, item ->
                ReorderableItem(
                    state = reorderableLazyListState,
                    key = if (item.specificApp != null) {
                        "${item.type}_${item.specificApp.packageName}"
                    } else {
                        item.type.name
                    },
                ) { isDragging ->
                    val dragModifier = Modifier.draggableHandle()
                    MenuItemRow(
                        itemType = item.type,
                        enabled = item.enabled,
                        onToggle = {
                            if (item.specificApp != null) {
                                // For specific apps, we need to handle them individually
                                // This would require a different approach in the ViewModel
                                onToggleEnabled(item.type)
                            } else {
                                onToggleEnabled(item.type)
                            }
                        },
                        isDragging = isDragging,
                        specificApp = item.specificApp,
                        dragModifier = dragModifier,
                    )
                }
            }

            item {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                ActionSetting(
                    title = stringResource(R.string.reset_to_defaults),
                    onClick = onResetToDefaults,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun MenuItemRow(
    itemType: MenuItemType,
    enabled: Boolean,
    onToggle: () -> Unit,
    isDragging: Boolean,
    specificApp: com.nononsenseapps.feeder.archmodel.DiscoveredApp? = null,
    modifier: Modifier = Modifier,
    dragModifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val dimens = LocalDimens.current

    val itemName = when {
        specificApp != null -> specificApp.label
        else -> stringResource(
            when (itemType) {
                MenuItemType.COPY -> R.string.copy
                MenuItemType.PASTE -> R.string.paste
                MenuItemType.CUT -> R.string.cut
                MenuItemType.SELECT_ALL -> R.string.select_all
                MenuItemType.TEXT_PROCESSOR -> R.string.text_processor
                MenuItemType.SHARE_HANDLER -> R.string.share_handler
                MenuItemType.ACTION_SEND_HANDLER -> R.string.action_send_handler
                MenuItemType.OTHER_SYSTEM_HANDLER -> R.string.other_system_handler
            }
        )
    }

    val itemDescription = when {
        specificApp != null -> {
            when (itemType) {
                MenuItemType.TEXT_PROCESSOR -> "Text processor"
                MenuItemType.SHARE_HANDLER -> "Share handler"
                MenuItemType.ACTION_SEND_HANDLER -> "Send handler"
                MenuItemType.OTHER_SYSTEM_HANDLER -> "System handler"
                else -> null
            }
        }
        else -> null
    }

    val moveItemContentDescription = stringResource(R.string.move_item, itemName)

    Card(
        modifier =
            modifier
                .width(dimens.maxContentWidth)
                .height(64.dp),
        elevation = if (isDragging) {
            CardDefaults.cardElevation(defaultElevation = 8.dp)
        } else {
            CardDefaults.cardElevation(defaultElevation = 0.dp)
        },
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .safeSemantics(mergeDescendants = true) {
                        stateDescription =
                            when (enabled) {
                                true -> context.getString(R.string.item_enabled)
                                else -> context.getString(R.string.item_disabled)
                            }
                        role = Role.Switch
                    },
            verticalAlignment = Alignment.CenterVertically,
        ) {
        Icon(
            imageVector = Icons.Default.DragHandle,
            contentDescription = stringResource(R.string.drag_to_reorder),
            modifier =
                Modifier
                    .padding(start = 8.dp, end = 16.dp)
                    .size(24.dp)
                    .then(dragModifier)
                    .semantics { contentDescription = moveItemContentDescription },
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = itemName,
                style = MaterialTheme.typography.titleMedium,
            )

            if (itemDescription != null) {
                Text(
                    text = itemDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = enabled,
            onCheckedChange = { onToggle() },
            modifier = Modifier.clearAndSetSemantics { },
        )
        }
    }
}
