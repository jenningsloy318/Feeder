package com.nononsenseapps.feeder.ui.compose.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nononsenseapps.feeder.R
import com.nononsenseapps.feeder.ai.model.TranslationLanguage
import com.nononsenseapps.feeder.ui.compose.theme.LocalDimens
import com.nononsenseapps.feeder.ui.compose.theme.SensibleTopAppBar
import androidx.compose.foundation.layout.size as importSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationSettingsScreen(
    onNavigateUp: () -> Unit,
    viewModel: TranslationSettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val translationEnabled by viewModel.translationEnabled.collectAsStateWithLifecycle()
    val enableTranslation by viewModel.enableTranslation.collectAsStateWithLifecycle()
    val translationLanguage by viewModel.translationLanguage.collectAsStateWithLifecycle()
    val translationTimeout by viewModel.translationTimeout.collectAsStateWithLifecycle()

    var languageMenuExpanded by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = modifier,
        contentWindowInsets = TopAppBarDefaults.windowInsets,
        topBar = {
            SensibleTopAppBar(
                scrollBehavior = scrollBehavior,
                title = stringResource(id = R.string.translation_settings_title),
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
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = LocalDimens.current.margin,
                        vertical = 8.dp,
                    ),
        ) {
            SwitchSetting(
                title = stringResource(R.string.enable_translation_title),
                checked = enableTranslation,
                onCheckedChange = { viewModel.setEnableTranslation(it) },
                description = stringResource(R.string.enable_translation_description),
            )

            Spacer(modifier = Modifier.height(8.dp))

            SwitchSetting(
                title = stringResource(R.string.translation_enabled_title),
                checked = translationEnabled,
                onCheckedChange = { viewModel.setTranslationEnabled(it) },
                description = stringResource(R.string.translation_enabled_description),
                enabled = enableTranslation,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Language Selector
            LanguageSelectorSetting(
                title = stringResource(R.string.translation_target_language_title),
                currentLanguage = translationLanguage,
                onLanguageSelected = { viewModel.setTranslationLanguage(it) },
                menuExpanded = languageMenuExpanded,
                onMenuExpandedChange = { languageMenuExpanded = it },
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Timeout Slider
            TimeoutSetting(
                title = stringResource(R.string.translation_timeout_title),
                description = stringResource(R.string.translation_timeout_description),
                timeoutSeconds = translationTimeout,
                onTimeoutChange = { viewModel.setTranslationTimeout(it) },
            )
        }
    }
}

@Composable
private fun LanguageSelectorSetting(
    title: String,
    currentLanguage: TranslationLanguage,
    onLanguageSelected: (TranslationLanguage) -> Unit,
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalDimens.current

    androidx.compose.foundation.layout.Row(
        modifier =
            modifier
                .width(dimens.maxContentWidth)
                .heightIn(min = 64.dp)
                .clickable {
                    onMenuExpandedChange(true)
                }.semantics {
                    role = Role.Button
                },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.importSize(64.dp),
            contentAlignment = Alignment.Center,
        ) {}

        TitleAndSubtitle(
            title = {
                Text(
                    text = title,
                )
            },
            subtitle = {
                Text(
                    text = stringResource(id = currentLanguage.displayName),
                    style = MaterialTheme.typography.bodySmall,
                )
            },
        )
    }

    // Dropdown Menu
    androidx.compose.material3.DropdownMenu(
        expanded = menuExpanded,
        onDismissRequest = { onMenuExpandedChange(false) },
    ) {
        TranslationLanguage.entries.forEach { language ->
            val isSelected = language == currentLanguage
            androidx.compose.material3.DropdownMenuItem(
                text = {
                    Text(
                        stringResource(id = language.displayName),
                    )
                },
                leadingIcon = {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                        )
                    }
                },
                onClick = {
                    onLanguageSelected(language)
                    onMenuExpandedChange(false)
                },
            )
        }
    }
}

@Composable
private fun TimeoutSetting(
    title: String,
    description: String,
    timeoutSeconds: Int,
    onTimeoutChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalDimens.current
    var inputValue by remember(timeoutSeconds) { mutableStateOf(timeoutSeconds.toString()) }

    Row(
        modifier =
            modifier
                .width(dimens.maxContentWidth)
                .heightIn(min = 64.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Empty 64dp box to align with other settings
        Box(
            modifier = Modifier.importSize(64.dp),
            contentAlignment = Alignment.Center,
        ) {}

        TitleAndSubtitle(
            title = {
                Text(text = title)
            },
            subtitle = {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                )
            },
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Compact input stepper on the right
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            // Minus button
            IconButton(
                onClick = {
                    val newValue = (timeoutSeconds - 1).coerceAtLeast(30)
                    inputValue = newValue.toString()
                    onTimeoutChange(newValue)
                },
                enabled = timeoutSeconds > 30,
                modifier = Modifier.importSize(32.dp),
            ) {
                Icon(
                    Icons.Filled.Remove,
                    contentDescription = null,
                    modifier = Modifier.importSize(16.dp),
                )
            }

            // Value display
            Text(
                text = inputValue,
                modifier = Modifier.width(40.dp),
                style = MaterialTheme.typography.bodyMedium,
            )

            // Plus button
            IconButton(
                onClick = {
                    val newValue = (timeoutSeconds + 1).coerceAtMost(600)
                    inputValue = newValue.toString()
                    onTimeoutChange(newValue)
                },
                enabled = timeoutSeconds < 600,
                modifier = Modifier.importSize(32.dp),
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.importSize(16.dp),
                )
            }
        }
    }
}
