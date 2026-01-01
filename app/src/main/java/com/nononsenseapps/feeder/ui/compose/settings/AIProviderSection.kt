package com.nononsenseapps.feeder.ui.compose.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nononsenseapps.feeder.R
import com.nononsenseapps.feeder.ai.model.AISettings
import com.nononsenseapps.feeder.ai.model.AnthropicSettings
import com.nononsenseapps.feeder.ai.model.OpenAISettings
import com.nononsenseapps.feeder.ai.model.SummaryLanguage
import com.nononsenseapps.feeder.ai.provider.AIProvider
import com.nononsenseapps.feeder.ui.compose.theme.LocalDimens

@Composable
fun AIProviderSection(
    state: AISettingsState,
    onEvent: (AISettingsEvent) -> Unit,
    onNavigateToProviders: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        AIProviderSectionItem(
            settings = state.settings,
            onEvent = onEvent,
            onNavigateToProviders = onNavigateToProviders,
            modifier = Modifier,
        )

        Spacer(modifier = Modifier.height(8.dp))

        SummaryLanguageSectionItem(
            summaryLanguage = state.summaryLanguage,
            onEvent = onEvent,
            modifier = Modifier,
        )
    }

    if (state.isEditMode) {
        var current by remember(state.settings) { mutableStateOf(state.settings) }
        AlertDialog(
            confirmButton = {
                Button(onClick = {
                    onEvent(AISettingsEvent.UpdateSettings(current))
                    onEvent(AISettingsEvent.SwitchEditMode(enabled = false))
                }) {
                    Text(text = stringResource(R.string.save))
                }
            },
            dismissButton = {
                Button(onClick = {
                    onEvent(AISettingsEvent.SwitchEditMode(enabled = false))
                }) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            },
            onDismissRequest = { onEvent(AISettingsEvent.SwitchEditMode(enabled = false)) },
            title = {
                Text(text = stringResource(R.string.openai_settings))
            },
            text = {
                AIProviderSectionEdit(
                    modifier = Modifier,
                    state = state,
                    current = current,
                    onEvent = {
                        if (it is AISettingsEvent.UpdateSettings) {
                            current = it.settings
                        } else {
                            onEvent(it)
                        }
                    },
                )
            },
        )
    }
}

@Composable
private fun AIProviderSectionItem(
    settings: AISettings,
    onEvent: (AISettingsEvent) -> Unit,
    onNavigateToProviders: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val key = when (settings) {
        is AISettings.OpenAI -> settings.openaiSettings.key
        is AISettings.Anthropic -> settings.anthropicSettings.key
    }

    Row(
        modifier =
            modifier
                .width(LocalDimens.current.maxContentWidth)
                .clickable { onNavigateToProviders() }
                .semantics { role = Role.Button },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(64.dp),
            contentAlignment = Alignment.Center,
        ) { }

        TitleAndSubtitle(
            title = {
                Text(
                    text = stringResource(R.string.provider_list_title),
                )
            },
            subtitle = {
                Text(
                    text = if (key.isNotBlank()) {
                        stringResource(R.string.provider_configured)
                    } else {
                        stringResource(R.string.no_providers_configured)
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            },
        )
    }
}

@Composable
private fun SummaryLanguageSectionItem(
    summaryLanguage: SummaryLanguage,
    onEvent: (AISettingsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var languageMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier =
            modifier
                .width(LocalDimens.current.maxContentWidth)
                .clickable { languageMenuExpanded = true }
                .semantics { role = Role.Button },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(64.dp),
            contentAlignment = Alignment.Center,
        ) { }

        TitleAndSubtitle(
            title = {
                Text(
                    text = stringResource(R.string.summary_language_title),
                )
            },
            subtitle = {
                Text(
                    text = stringResource(id = summaryLanguage.displayName),
                    style = MaterialTheme.typography.bodySmall,
                )
            },
        )
    }

    Box {
        DropdownMenu(
            expanded = languageMenuExpanded,
            onDismissRequest = { languageMenuExpanded = false },
        ) {
            SummaryLanguage.entries.forEach { language ->
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(id = language.displayName),
                        )
                    },
                    onClick = {
                        onEvent(AISettingsEvent.UpdateSummaryLanguage(language))
                        languageMenuExpanded = false
                    },
                )
            }
        }
    }
}

fun isTimeoutInputValid(input: String): Boolean = input.trim().isNotEmpty() && input.toIntOrNull()?.takeIf { it in 30..600 } != null

@Composable
fun AIProviderSectionEdit(
    state: AISettingsState,
    current: AISettings,
    onEvent: (AISettingsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val latestOnEvent by rememberUpdatedState(onEvent)
    LaunchedEffect(current) {
        latestOnEvent(AISettingsEvent.LoadModels(settings = current))
    }

    var providerMenuExpanded by remember { mutableStateOf(false) }
    var modelsMenuExpanded by remember { mutableStateOf(false) }

    val currentOpenAISettings = (current as? AISettings.OpenAI)?.openaiSettings ?: OpenAISettings()
    val currentAnthropicSettings = (current as? AISettings.Anthropic)?.anthropicSettings ?: AnthropicSettings()
    val timeoutSeconds = when (current) {
        is AISettings.OpenAI -> current.openaiSettings.timeoutSeconds
        is AISettings.Anthropic -> current.anthropicSettings.timeoutSeconds
    }
    var timeoutString by remember { mutableStateOf(timeoutSeconds.toString()) }

    val isTimeoutInputValid =
        remember(timeoutString) {
            isTimeoutInputValid(timeoutString)
        }
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val isAnthropic = current is AISettings.Anthropic

    Column(
        modifier = modifier.verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.openai_settings_info),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(bottom = 3.dp),
        )

        // Provider Selection
        Box {
            TextField(
                modifier = Modifier.fillMaxWidth(),
                value =
                    when (current.providerType) {
                        AIProvider.OPENAI_COMPATIBLE -> stringResource(R.string.ai_provider_openai_compatible)
                        AIProvider.ANTHROPIC -> stringResource(R.string.ai_provider_anthropic)
                    },
                onValueChange = {},
                label = {
                    Text(stringResource(R.string.ai_provider))
                },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { providerMenuExpanded = !providerMenuExpanded }) {
                        Icon(
                            if (providerMenuExpanded) Icons.Filled.ExpandLess
                            else Icons.Filled.ExpandMore,
                            contentDescription = null,
                        )
                    }
                },
            )

            DropdownMenu(
                expanded = providerMenuExpanded,
                onDismissRequest = { providerMenuExpanded = false },
            ) {
                AIProvider.entries.forEach { provider ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                when (provider) {
                                    AIProvider.OPENAI_COMPATIBLE -> stringResource(R.string.ai_provider_openai_compatible)
                                    AIProvider.ANTHROPIC -> stringResource(R.string.ai_provider_anthropic)
                                },
                            )
                        },
                        onClick = {
                            val newSettings = AISettings.defaultForProvider(provider)
                            onEvent(AISettingsEvent.UpdateSettings(newSettings))
                            providerMenuExpanded = false
                        },
                    )
                }
            }
        }

        // API Key
        val key = when (current) {
            is AISettings.OpenAI -> current.openaiSettings.key
            is AISettings.Anthropic -> current.anthropicSettings.key
        }
        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = key,
            label = {
                Text(stringResource(R.string.api_key))
            },
            keyboardOptions =
                KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Next,
                ),
            keyboardActions =
                KeyboardActions(
                    onNext = {
                        focusManager.moveFocus(focusDirection = FocusDirection.Down)
                    },
                ),
            onValueChange = {
                when (current) {
                    is AISettings.OpenAI ->
                        onEvent(AISettingsEvent.UpdateSettings(current.copy(openaiSettings = current.openaiSettings.copy(key = it))))
                    is AISettings.Anthropic ->
                        onEvent(AISettingsEvent.UpdateSettings(current.copy(anthropicSettings = current.anthropicSettings.copy(key = it))))
                }
            },
            visualTransformation = VisualTransformationApiKey(),
        )

        // Model ID
        val modelId = when (current) {
            is AISettings.OpenAI -> current.openaiSettings.modelId
            is AISettings.Anthropic -> current.anthropicSettings.modelId
        }
        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = modelId,
            label = {
                Text(stringResource(R.string.model_id))
            },
            keyboardOptions =
                KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Next,
                ),
            keyboardActions =
                KeyboardActions(
                    onNext = {
                        focusManager.moveFocus(focusDirection = FocusDirection.Down)
                    },
                ),
            onValueChange = {
                when (current) {
                    is AISettings.OpenAI ->
                        onEvent(AISettingsEvent.UpdateSettings(current.copy(openaiSettings = current.openaiSettings.copy(modelId = it))))
                    is AISettings.Anthropic ->
                        onEvent(AISettingsEvent.UpdateSettings(current.copy(anthropicSettings = current.anthropicSettings.copy(modelId = it))))
                }
            },
            trailingIcon = {
                IconButton(
                    onClick = { modelsMenuExpanded = true },
                    enabled = state.modelsResult is ModelsState.Success,
                ) {
                    if (state.modelsResult is ModelsState.Loading) {
                        CircularProgressIndicator()
                    } else {
                        Icon(Icons.Filled.ExpandMore, contentDescription = stringResource(R.string.list_of_available_models))
                        if (state.modelsResult is ModelsState.Success) {
                            AIModelsDropdown(
                                menuExpanded = modelsMenuExpanded,
                                state = state.modelsResult,
                                onValueChange = {
                                    when (current) {
                                        is AISettings.OpenAI ->
                                            onEvent(AISettingsEvent.UpdateSettings(current.copy(openaiSettings = current.openaiSettings.copy(modelId = it))))
                                        is AISettings.Anthropic ->
                                            onEvent(AISettingsEvent.UpdateSettings(current.copy(anthropicSettings = current.anthropicSettings.copy(modelId = it))))
                                    }
                                },
                                onDismissRequest = { modelsMenuExpanded = false },
                            )
                        }
                    }
                }
            },
        )

        AIModelsStatus(
            state = state.modelsResult,
            showError = state.showModelsError,
            onEvent = onEvent,
            isAnthropic = isAnthropic,
        )

        // Base URL
        val baseUrl = when (current) {
            is AISettings.OpenAI -> current.openaiSettings.baseUrl
            is AISettings.Anthropic -> current.anthropicSettings.baseUrl
        }
        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = baseUrl,
            placeholder = {
                Text(
                    when (current.providerType) {
                        AIProvider.OPENAI_COMPATIBLE -> "https://api.openai.com/v1"
                        AIProvider.ANTHROPIC -> "https://api.anthropic.com"
                    },
                )
            },
            label = {
                Text(stringResource(R.string.url))
            },
            keyboardOptions =
                KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next,
                ),
            keyboardActions =
                KeyboardActions(
                    onNext = {
                        focusManager.moveFocus(focusDirection = FocusDirection.Down)
                    },
                ),
            onValueChange = {
                when (current) {
                    is AISettings.OpenAI ->
                        onEvent(AISettingsEvent.UpdateSettings(current.copy(openaiSettings = current.openaiSettings.copy(baseUrl = it))))
                    is AISettings.Anthropic ->
                        onEvent(AISettingsEvent.UpdateSettings(current.copy(anthropicSettings = current.anthropicSettings.copy(baseUrl = it))))
                }
            },
        )

        // Timeout
        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = timeoutString,
            placeholder = { Text(text = stringResource(R.string.time_out_placeholder)) },
            label = {
                Text(stringResource(R.string.time_out))
            },
            keyboardOptions =
                KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Number,
                    imeAction = if (isAnthropic) ImeAction.Done else ImeAction.Next,
                ),
            keyboardActions =
                KeyboardActions(
                    onNext = {
                        if (isAnthropic) {
                            keyboardController?.hide()
                        } else {
                            focusManager.moveFocus(focusDirection = FocusDirection.Down)
                        }
                    },
                    onDone = {
                        keyboardController?.hide()
                    },
                ),
            supportingText = {
                if (!isTimeoutInputValid) {
                    Text(stringResource(R.string.time_out_validation_error))
                }
            },
            onValueChange = { input ->
                timeoutString = input
                if (isTimeoutInputValid(timeoutString)) {
                    when (current) {
                        is AISettings.OpenAI ->
                            onEvent(AISettingsEvent.UpdateSettings(current.copy(openaiSettings = current.openaiSettings.copy(timeoutSeconds = timeoutString.toInt()))))
                        is AISettings.Anthropic ->
                            onEvent(AISettingsEvent.UpdateSettings(current.copy(anthropicSettings = current.anthropicSettings.copy(timeoutSeconds = timeoutString.toInt()))))
                    }
                }
            },
            isError = !isTimeoutInputValid,
        )

        // Azure fields - only show for OpenAI-compatible provider
        if (current is AISettings.OpenAI) {
            TextField(
                modifier = Modifier.fillMaxWidth(),
                value = current.openaiSettings.azureDeploymentId,
                label = {
                    Text(stringResource(R.string.azure_deployment_id))
                },
                keyboardOptions =
                    KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Next,
                    ),
                keyboardActions =
                    KeyboardActions(
                        onNext = {
                            focusManager.moveFocus(focusDirection = FocusDirection.Down)
                        },
                    ),
                onValueChange = {
                    onEvent(AISettingsEvent.UpdateSettings(current.copy(openaiSettings = current.openaiSettings.copy(azureDeploymentId = it))))
                },
            )

            TextField(
                modifier = Modifier.fillMaxWidth(),
                value = current.openaiSettings.azureApiVersion,
                placeholder = {
                    Text("2024-02-15-preview")
                },
                label = {
                    Text(stringResource(R.string.azure_api_version))
                },
                keyboardOptions =
                    KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Done,
                    ),
                keyboardActions =
                    KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                        },
                    ),
                onValueChange = {
                    onEvent(AISettingsEvent.UpdateSettings(current.copy(openaiSettings = current.openaiSettings.copy(azureApiVersion = it))))
                },
            )
        }
    }
}

@Composable
private fun AIModelsDropdown(
    menuExpanded: Boolean,
    state: ModelsState.Success,
    onValueChange: (String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    DropdownMenu(
        expanded = menuExpanded,
        onDismissRequest = onDismissRequest,
    ) {
        state.ids.forEach { id ->
            DropdownMenuItem(
                text = { Text(text = id) },
                onClick = {
                    onValueChange(id)
                    onDismissRequest()
                },
            )
        }
    }
}

@Composable
private fun AIModelsStatus(
    state: ModelsState,
    showError: Boolean,
    onEvent: (AISettingsEvent) -> Unit,
    isAnthropic: Boolean = false,
) {
    when (state) {
        is ModelsState.Success -> {
            // Don't show "no models" message for Anthropic - users input model ID directly
            if (state.ids.isEmpty() && !isAnthropic) {
                OutlinedCard {
                    Text(
                        text = stringResource(R.string.no_models_were_found),
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }
        }

        is ModelsState.Error -> {
            val hasError by remember(state.message) { mutableStateOf(state.message.isNotEmpty()) }
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onEvent(AISettingsEvent.ShowModelsError(show = !showError)) },
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                        )
                        Text(
                            text = stringResource(R.string.unable_to_load_models),
                            modifier =
                                Modifier
                                    .padding(start = 4.dp)
                                    .weight(1f),
                        )
                        if (hasError) {
                            Icon(
                                imageVector = if (showError) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = stringResource(R.string.show_message),
                            )
                        }
                    }

                    if (hasError && showError) {
                        Text(
                            text = state.message,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }
            }
        }

        ModelsState.Loading -> {}
        ModelsState.None -> {}
    }
}

// Data classes and sealed interfaces for the new AI provider section
data class AISettingsState(
    val settings: AISettings = AISettings.OpenAI(),
    val modelsResult: ModelsState = ModelsState.None,
    val isEditMode: Boolean = false,
    val showModelsError: Boolean = false,
    val summaryLanguage: SummaryLanguage = SummaryLanguage.AUTO_DETECT,
)

sealed interface ModelsState {
    data object None : ModelsState

    data object Loading : ModelsState

    data class Success(
        val ids: List<String>,
    ) : ModelsState

    data class Error(
        val message: String,
    ) : ModelsState
}

sealed interface AISettingsEvent {
    data class UpdateSettings(
        val settings: AISettings,
    ) : AISettingsEvent

    data class LoadModels(
        val settings: AISettings,
    ) : AISettingsEvent

    data class SwitchEditMode(
        val enabled: Boolean,
    ) : AISettingsEvent

    data class ShowModelsError(
        val show: Boolean,
    ) : AISettingsEvent

    data class UpdateSummaryLanguage(
        val language: SummaryLanguage,
    ) : AISettingsEvent
}
