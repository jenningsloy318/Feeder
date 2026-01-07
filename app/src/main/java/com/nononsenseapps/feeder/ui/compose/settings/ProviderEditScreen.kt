package com.nononsenseapps.feeder.ui.compose.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nononsenseapps.feeder.R
import com.nononsenseapps.feeder.ai.provider.AIProvider
import com.nononsenseapps.feeder.ui.compose.theme.SensibleTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderEditScreen(
    onNavigateUp: () -> Unit,
    viewModel: ProviderEditViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val scrollState = rememberScrollState()

    // Handle save result
    if (uiState.saveResult != null) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val successMessage = context.getString(R.string.provider_saved)
        val errorMessage =
            uiState.saveResult?.exceptionOrNull()?.message
                ?: context.getString(R.string.something_went_wrong)

        LaunchedEffect(uiState.saveResult) {
            uiState.saveResult?.let { result ->
                if (result.isSuccess) {
                    // Navigate immediately without waiting for snackbar
                    onNavigateUp()
                    viewModel.clearSaveResult()
                    // Show snackbar in background after navigation
                    snackbarHostState.showSnackbar(message = successMessage)
                } else {
                    snackbarHostState.showSnackbar(message = errorMessage)
                    viewModel.clearSaveResult()
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = TopAppBarDefaults.windowInsets,
        topBar = {
            SensibleTopAppBar(
                scrollBehavior = scrollBehavior,
                title =
                    stringResource(
                        id =
                            if (uiState.isNewProvider) {
                                R.string.add_provider
                            } else {
                                R.string.edit_provider
                            },
                    ),
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
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            when {
                uiState.isLoading && uiState.isNewProvider -> {
                    CircularProgressIndicator(
                        modifier =
                            Modifier
                                .align(androidx.compose.ui.Alignment.Center)
                                .padding(16.dp),
                    )
                }

                else -> {
                    ProviderEditForm(
                        uiState = uiState,
                        onNameChange = viewModel::updateName,
                        onProviderTypeChange = viewModel::updateProviderType,
                        onApiKeyChange = viewModel::updateApiKey,
                        onBaseUrlChange = viewModel::updateBaseUrl,
                        onModelIdChange = viewModel::updateModelId,
                        onMaxTokensChange = viewModel::updateMaxTokens,
                        onIsActiveChange = viewModel::updateIsActive,
                        onSave = {
                            focusManager.clearFocus()
                            viewModel.saveProvider()
                        },
                        onCancel = onNavigateUp,
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState),
                    )
                }
            }
        }
    }
}

@Composable
fun ProviderEditForm(
    uiState: ProviderEditUiState,
    onNameChange: (String) -> Unit,
    onProviderTypeChange: (AIProvider) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onModelIdChange: (String) -> Unit,
    onMaxTokensChange: (String) -> Unit,
    onIsActiveChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showProviderTypeMenu by remember { mutableStateOf(false) }
    var showValidationError by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val isFormValid =
        uiState.name.isNotBlank() &&
            uiState.apiKey.isNotBlank()

    Column(
        modifier =
            modifier
                .padding(16.dp)
                .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Provider Name
        OutlinedTextField(
            value = uiState.name,
            onValueChange = onNameChange,
            label = {
                Text(stringResource(R.string.provider_name))
            },
            placeholder = {
                Text(stringResource(R.string.provider_name_hint))
            },
            singleLine = true,
            isError = showValidationError && uiState.name.isBlank(),
            supportingText = {
                if (showValidationError && uiState.name.isBlank()) {
                    Text(stringResource(R.string.provider_name_required))
                }
            },
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
            keyboardActions =
                androidx.compose.foundation.text.KeyboardActions(
                    onNext = {
                        focusManager.moveFocus(FocusDirection.Down)
                    },
                ),
            modifier = Modifier.fillMaxWidth(),
        )

        // Provider Type Selector
        @OptIn(ExperimentalMaterial3Api::class)
        androidx.compose.material3.ExposedDropdownMenuBox(
            expanded = showProviderTypeMenu,
            onExpandedChange = { showProviderTypeMenu = it },
        ) {
            OutlinedTextField(
                value =
                    stringResource(
                        id =
                            when (uiState.providerType) {
                                AIProvider.OPENAI_COMPATIBLE -> R.string.ai_provider_openai_compatible
                                AIProvider.ANTHROPIC -> R.string.ai_provider_anthropic_compatible
                            },
                    ),
                onValueChange = {},
                readOnly = true,
                label = {
                    Text(stringResource(R.string.ai_provider))
                },
                trailingIcon = {
                    Icon(
                        Icons.Filled.ExpandMore,
                        contentDescription = null,
                    )
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .menuAnchor(
                            type = androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                            enabled = true,
                        ),
            )

            androidx.compose.material3.DropdownMenu(
                expanded = showProviderTypeMenu,
                onDismissRequest = { showProviderTypeMenu = false },
                modifier = Modifier.fillMaxWidth(0.9f),
            ) {
                AIProvider.entries.forEach { provider ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    id =
                                        when (provider) {
                                            AIProvider.OPENAI_COMPATIBLE -> R.string.ai_provider_openai_compatible
                                            AIProvider.ANTHROPIC -> R.string.ai_provider_anthropic_compatible
                                        },
                                ),
                            )
                        },
                        onClick = {
                            onProviderTypeChange(provider)
                            showProviderTypeMenu = false
                        },
                    )
                }
            }
        }

        // API Key
        OutlinedTextField(
            value = uiState.apiKey,
            onValueChange = onApiKeyChange,
            label = {
                Text(stringResource(R.string.api_key))
            },
            singleLine = true,
            isError = showValidationError && uiState.apiKey.isBlank(),
            supportingText = {
                if (showValidationError && uiState.apiKey.isBlank()) {
                    Text(stringResource(R.string.api_key_required))
                }
            },
            visualTransformation = VisualTransformationApiKey(),
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Next,
                ),
            keyboardActions =
                androidx.compose.foundation.text.KeyboardActions(
                    onNext = {
                        focusManager.moveFocus(FocusDirection.Down)
                    },
                ),
            modifier = Modifier.fillMaxWidth(),
        )

        // Base URL
        val baseUrlPlaceholder =
            when (uiState.providerType) {
                AIProvider.OPENAI_COMPATIBLE -> "https://api.openai.com/v1"
                AIProvider.ANTHROPIC -> "https://api.anthropic.com"
            }

        OutlinedTextField(
            value = uiState.baseUrl,
            onValueChange = onBaseUrlChange,
            label = {
                Text(stringResource(R.string.url))
            },
            placeholder = {
                Text(baseUrlPlaceholder)
            },
            singleLine = true,
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next,
                ),
            keyboardActions =
                androidx.compose.foundation.text.KeyboardActions(
                    onNext = {
                        focusManager.moveFocus(FocusDirection.Down)
                    },
                ),
            modifier = Modifier.fillMaxWidth(),
        )

        // Model ID
        OutlinedTextField(
            value = uiState.modelId,
            onValueChange = onModelIdChange,
            label = {
                Text(stringResource(R.string.model_id))
            },
            placeholder = {
                Text(
                    when (uiState.providerType) {
                        AIProvider.OPENAI_COMPATIBLE -> "gpt-4o"
                        AIProvider.ANTHROPIC -> "claude-3-5-sonnet-20241022"
                    },
                )
            },
            singleLine = true,
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Next,
                ),
            keyboardActions =
                androidx.compose.foundation.text.KeyboardActions(
                    onNext = {
                        focusManager.moveFocus(FocusDirection.Down)
                    },
                ),
            modifier = Modifier.fillMaxWidth(),
        )

        // Max Tokens
        OutlinedTextField(
            value = uiState.maxTokens,
            onValueChange = onMaxTokensChange,
            label = {
                Text(stringResource(R.string.max_tokens))
            },
            placeholder = {
                Text(stringResource(R.string.max_tokens_hint))
            },
            singleLine = true,
            supportingText = {
                Text(stringResource(R.string.max_tokens_supporting))
            },
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
            keyboardActions =
                androidx.compose.foundation.text.KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (isFormValid) {
                            onSave()
                        } else {
                            showValidationError = true
                        }
                    },
                ),
            modifier = Modifier.fillMaxWidth(),
        )

        // Set as Default checkbox
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = uiState.isActive,
                onCheckedChange = onIsActiveChange,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.set_as_default_provider))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.cancel))
            }

            Button(
                onClick = {
                    if (isFormValid) {
                        onSave()
                    } else {
                        showValidationError = true
                    }
                },
                enabled = !uiState.isSaving && isFormValid,
                modifier = Modifier.weight(1f),
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(24.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(R.string.save_provider))
                }
            }
        }
    }
}
