# UI/UX Design: Multi-Provider AI Configuration Management

**Created:** 2026-01-01 16:01:56+08:00
**Status:** Complete
**Current Date/Time:** 2026-01-01 16:01:56+08:00

## Overview

This document describes the UI/UX design for the multi-provider AI configuration management system, following Material Design 3 principles and existing app patterns.

## Design Principles

1. **Consistency:** Match existing Settings UI patterns
2. **Clarity:** Clear visual hierarchy and intuitive navigation
3. **Efficiency:** Minimize taps to complete tasks
4. **Safety:** Prevent accidental deletions and changes
5. **Accessibility:** Support screen readers and keyboard navigation

## Screen Architecture

### Screen Hierarchy

```
Settings Screen (existing)
    ↓
"Provider" menu item (renamed from "API key")
    ↓
Provider List Screen (NEW)
    ↓
├── Add Provider Button → Provider Edit Screen (add mode)
└── Edit Button (on item) → Provider Edit Screen (edit mode)
```

## Screen 1: Provider List Screen

### Purpose
Display all configured AI providers with options to add, edit, delete, and activate.

### Layout Structure

```
┌─────────────────────────────────────────┐
│ ← Back          Provider Management     │
├─────────────────────────────────────────┤
│                                         │
│  [Floating Action Button: + Add]       │
│                                         │
│  ┌───────────────────────────────────┐ │
│  │ ● OpenAI - GPT-4 (Active)        │ │
│  │                                   │ │
│  │ [Edit]  [Delete]                  │ │
│  └───────────────────────────────────┘ │
│                                         │
│  ┌───────────────────────────────────┐ │
│  │   Anthropic - Claude 3.5          │ │
│  │                                   │ │
│  │ [Edit]  [Delete]                  │ │
│  └───────────────────────────────────┘ │
│                                         │
│  ┌───────────────────────────────────┐ │
│  │   OpenAI - Azure (Work)           │ │
│  │                                   │ │
│  │ [Edit]  [Delete]                  │ │
│  └───────────────────────────────────┘ │
│                                         │
└─────────────────────────────────────────┘
```

### Component Details

#### Top Bar

```kotlin
@Composable
fun ProviderListTopBar(
    onNavigateUp: () -> Unit,
) {
    TopAppBar(
        title = { Text(stringResource(R.string.provider_list_title)) },
        navigationIcon = {
            IconButton(onClick = onNavigateUp) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        )
    )
}
```

#### Provider List Item

```kotlin
@Composable
fun ProviderListItem(
    provider: ProviderConfig,
    isActive: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(
                if (isActive) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.medium,
                    )
                } else {
                    Modifier
                }
            ),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left: Provider info
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isActive) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = stringResource(R.string.active_provider),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Text(
                        text = provider.getDisplayName(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Provider type icon
                    Icon(
                        imageVector = when (provider.providerType) {
                            AIProvider.OPENAI_COMPATIBLE -> Icons.Default.Cloud
                            AIProvider.ANTHROPIC -> Icons.Default.SmartToy
                        },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = when (provider.providerType) {
                            AIProvider.OPENAI_COMPATIBLE -> "OpenAI-compatible"
                            AIProvider.ANTHROPIC -> "Anthropic"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    if (!provider.isValid) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = stringResource(R.string.invalid_configuration),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            // Right: Action buttons
            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit_provider),
                    )
                }

                if (!isActive) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete_provider),
                        )
                    }
                }
            }
        }
    }
}
```

#### Empty State

```kotlin
@Composable
fun EmptyProvidersState(
    onAddProvider: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.CloudOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.no_providers_configured),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.add_provider_to_get_started),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onAddProvider) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.add_first_provider))
        }
    }
}
```

#### Delete Confirmation Dialog

```kotlin
@Composable
fun DeleteProviderDialog(
    provider: ProviderConfig,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.delete_provider_title))
        },
        text = {
            Text(
                stringResource(
                    R.string.delete_provider_confirmation,
                    provider.getDisplayName(),
                ),
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(stringResource(R.string.delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
```

### Screen Implementation

```kotlin
@Composable
fun ProviderListScreen(
    onNavigateUp: () -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    viewModel: ProviderListViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            ProviderListTopBar(onNavigateUp = onNavigateUp)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_provider))
            }
        },
        modifier = modifier,
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                uiState.providers.isEmpty() && !uiState.isLoading -> {
                    EmptyProvidersState(onAddProvider = onNavigateToAdd)
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(
                            items = uiState.providers,
                            key = { it.id },
                        ) { provider ->
                            ProviderListItem(
                                provider = provider,
                                isActive = provider.isActive,
                                onEdit = { onNavigateToEdit(provider.id) },
                                onDelete = { viewModel.onEvent(ProviderListEvent.DeleteProvider(provider.id)) },
                                onClick = { viewModel.onEvent(ProviderListEvent.ActivateProvider(provider.id)) },
                            )
                        }
                    }
                }
            }

            // Delete confirmation dialog
            if (uiState.providerToDelete != null) {
                DeleteProviderDialog(
                    provider = uiState.providerToDelete!!,
                    onConfirm = { viewModel.confirmDelete() },
                    onDismiss = { viewModel.onEvent(ProviderListEvent.DismissConfirmation) },
                )
            }
        }
    }
}
```

### Interactions

| User Action | System Response |
|-------------|----------------|
| Tap provider item | Activate provider (show checkmark) |
| Tap "Add" FAB | Navigate to edit screen (add mode) |
| Tap "Edit" button | Navigate to edit screen (edit mode) |
| Tap "Delete" button | Show confirmation dialog |
| Confirm delete | Remove provider from list |
| Tap back | Navigate back to Settings |

### Accessibility

- **Screen Reader Support:**
  - Active provider announced first
  - Button actions clearly labeled
  - Validation state announced

- **Keyboard Navigation:**
  - Tab order: Top items → Provider items → FAB
  - Enter activates focused provider
  - Delete requires confirmation (prevents accidents)

- **Visual Indicators:**
  - Active provider: Border + checkmark icon
  - Invalid provider: Warning icon
  - Disabled delete on active provider

## Screen 2: Provider Edit Screen

### Purpose
Add new provider or edit existing provider configuration.

### Layout Structure

```
┌─────────────────────────────────────────┐
│ ← Back          Add/Edit Provider        │
├─────────────────────────────────────────┤
│                                         │
│  Provider Name                          │
│  ┌───────────────────────────────────┐ │
│  │ My OpenAI Provider                │ │
│  └───────────────────────────────────┘ │
│                                         │
│  Provider Type                          │
│  ○ OpenAI-compatible                   │
│  ○ Anthropic                           │
│                                         │
│  ┌───────────────────────────────────┐ │
│  │  API Configuration                │ │
│  ├───────────────────────────────────┤ │
│  │  API Key *                        │ │
│  │  ┌─────────────────────────────┐ │ │
│  │  │ sk-...                       │ │ │
│  │  └─────────────────────────────┘ │ │
│  │                                   │ │
│  │  Model ID *                      │ │
│  │  ┌─────────────────────────────┐ │ │
│  │  │ gpt-4o-mini                 │ │ │
│  │  └─────────────────────────────┘ │ │
│  │                                   │ │
│  │  Base URL (optional)             │ │
│  │  ┌─────────────────────────────┐ │ │
│  │  │ https://api.openai.com/v1   │ │ │
│  │  └─────────────────────────────┘ │ │
│  │                                   │ │
│  │  Request Timeout (seconds)       │ │
│  │  ┌─────────────────────────────┐ │ │
│  │  │ 30                          │ │ │
│  │  └─────────────────────────────┘ │ │
│  │                                   │ │
│  │  [Azure Settings] (if Azure)     │ │
│  └───────────────────────────────────┘ │
│                                         │
│  [Cancel]                    [Save]     │
│                                         │
└─────────────────────────────────────────┘
```

### Component Details

#### Form Section (OpenAI)

```kotlin
@Composable
fun OpenAIConfigForm(
    settings: OpenAISettings,
    onSettingsChange: (OpenAISettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAzureSettings by remember {
        mutableStateOf(settings.isAzure)
    }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // API Key
        OutlinedTextField(
            value = settings.key,
            onValueChange = { onSettingsChange(settings.copy(key = it)) },
            label = { Text(stringResource(R.string.api_key_label)) },
            leadingIcon = {
                Icon(Icons.Default.Key, contentDescription = null)
            },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            isError = settings.key.isBlank(),
            supportingText = if (settings.key.isBlank()) {
                { Text(stringResource(R.string.required_field)) }
            } else null,
        )

        // Model ID
        OutlinedTextField(
            value = settings.modelId,
            onValueChange = { onSettingsChange(settings.copy(modelId = it)) },
            label = { Text(stringResource(R.string.model_id_label)) },
            leadingIcon = {
                Icon(Icons.Default.ModelTraining, contentDescription = null)
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            isError = settings.modelId.isBlank(),
            supportingText = if (settings.modelId.isBlank()) {
                { Text(stringResource(R.string.required_field)) }
            } else {
                { Text(stringResource(R.string.default_model, OpenAISettings.DEFAULT_MODEL)) }
            },
        )

        // Base URL
        OutlinedTextField(
            value = settings.baseUrl,
            onValueChange = {
                onSettingsChange(settings.copy(baseUrl = it))
                showAzureSettings = it.contains("openai.azure.com", ignoreCase = true)
            },
            label = { Text(stringResource(R.string.base_url_label)) },
            leadingIcon = {
                Icon(Icons.Default.Link, contentDescription = null)
            },
            placeholder = {
                Text(stringResource(R.string.base_url_placeholder))
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            supportingText = {
                Text(stringResource(R.string.base_url_help))
            },
        )

        // Timeout
        var timeoutText by remember { mutableStateOf(settings.timeoutSeconds.toString()) }
        val isTimeoutValid = remember(timeoutText) {
            timeoutText.toIntOrNull()?.let { it in 30..600 } ?: false
        }

        OutlinedTextField(
            value = timeoutText,
            onValueChange = {
                timeoutText = it
                it.toIntOrNull()?.let { seconds ->
                    if (seconds in 30..600) {
                        onSettingsChange(settings.copy(timeoutSeconds = seconds))
                    }
                }
            },
            label = { Text(stringResource(R.string.timeout_label)) },
            leadingIcon = {
                Icon(Icons.Default.Schedule, contentDescription = null)
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            isError = !isTimeoutValid,
            supportingText = if (!isTimeoutValid) {
                { Text(stringResource(R.string.timeout_range_error)) }
            } else {
                { Text(stringResource(R.string.timeout_help)) }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )

        // Azure Settings (conditional)
        if (showAzureSettings || settings.isAzure) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.azure_settings),
                        style = MaterialTheme.typography.titleSmall,
                    )

                    // API Version
                    OutlinedTextField(
                        value = settings.azureApiVersion,
                        onValueChange = { onSettingsChange(settings.copy(azureApiVersion = it)) },
                        label = { Text(stringResource(R.string.azure_api_version)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = settings.isAzure && settings.azureApiVersion.isBlank(),
                        supportingText = if (settings.isAzure && settings.azureApiVersion.isBlank()) {
                            { Text(stringResource(R.string.required_for_azure)) }
                        } else null,
                    )

                    // Deployment ID
                    OutlinedTextField(
                        value = settings.azureDeploymentId,
                        onValueChange = { onSettingsChange(settings.copy(azureDeploymentId = it)) },
                        label = { Text(stringResource(R.string.azure_deployment_id)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = settings.isAzure && settings.azureDeploymentId.isBlank(),
                        supportingText = if (settings.isAzure && settings.azureDeploymentId.isBlank()) {
                            { Text(stringResource(R.string.required_for_azure)) }
                        } else null,
                    )
                }
            }
        }
    }
}
```

#### Provider Type Selector

```kotlin
@Composable
fun ProviderTypeSelector(
    selectedType: AIProvider,
    onTypeSelected: (AIProvider) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.provider_type_label),
            style = MaterialTheme.typography.titleSmall,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AIProvider.values().forEach { provider ->
                FilterChip(
                    selected = selectedType == provider,
                    onClick = { onTypeSelected(provider) },
                    label = {
                        Text(
                            when (provider) {
                                AIProvider.OPENAI_COMPATIBLE -> "OpenAI"
                                AIProvider.ANTHROPIC -> "Anthropic"
                            },
                        )
                    },
                    leadingIcon = {
                        if (selectedType == provider) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        } else {
                            null
                        }
                    },
                )
            }
        }
    }
}
```

#### Screen Implementation

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderEditScreen(
    providerId: String?,
    onNavigateUp: () -> Unit,
    viewModel: ProviderEditViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (providerId == null) {
                            stringResource(R.string.add_provider)
                        } else {
                            stringResource(R.string.edit_provider)
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState),
            ) {
                // Provider Name
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = { viewModel.onEvent(ProviderEditEvent.SetName(it)) },
                    label = { Text(stringResource(R.string.provider_name_label)) },
                    placeholder = {
                        Text(
                            when (uiState.providerType) {
                                AIProvider.OPENAI_COMPATIBLE -> "My OpenAI Provider"
                                AIProvider.ANTHROPIC -> "My Anthropic Provider"
                            },
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.name.isBlank() && uiState.error != null,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Provider Type Selector
                ProviderTypeSelector(
                    selectedType = uiState.providerType,
                    onTypeSelected = { viewModel.onEvent(ProviderEditEvent.SetProviderType(it)) },
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Configuration Form
                when (uiState.providerType) {
                    AIProvider.OPENAI_COMPATIBLE -> {
                        OpenAIConfigForm(
                            settings = uiState.openaiSettings,
                            onSettingsChange = {
                                viewModel.onEvent(ProviderEditEvent.UpdateOpenAISettings(it))
                            },
                        )
                    }
                    AIProvider.ANTHROPIC -> {
                        AnthropicConfigForm(
                            settings = uiState.anthropicSettings,
                            onSettingsChange = {
                                viewModel.onEvent(ProviderEditEvent.UpdateAnthropicSettings(it))
                            },
                        )
                    }
                }

                // Error Message
                if (uiState.error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                    ) {
                        Text(
                            text = uiState.error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = onNavigateUp,
                        enabled = !uiState.isSaving,
                    ) {
                        Text(stringResource(R.string.cancel))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.onEvent(ProviderEditEvent.Save)
                        },
                        enabled = !uiState.isSaving,
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (providerId == null) {
                                stringResource(R.string.add)
                            } else {
                                stringResource(R.string.save)
                            },
                        )
                    }
                }
            }
        }
    }
}
```

### Form Validation

| Field | Validation | Error Message |
|-------|------------|---------------|
| Provider Name | Not blank | "Provider name is required" |
| API Key | Not blank | "API key is required" |
| Model ID | Not blank | "Model ID is required" |
| Base URL | Valid URL (optional) | "Invalid URL format" |
| Timeout | 30-600 seconds | "Must be between 30 and 600" |
| Azure API Version | Required if Azure | "Required for Azure OpenAI" |
| Azure Deployment ID | Required if Azure | "Required for Azure OpenAI" |

### Interactions

| User Action | System Response |
|-------------|----------------|
| Change provider type | Switch configuration form |
| Enter Azure URL | Show Azure settings section |
| Tap "Save" | Validate and save, or show errors |
| Tap "Cancel" | Discard changes, navigate back |
| Press back | Same as cancel |

## String Resources

### New Strings Required

```xml
<!-- Provider List -->
<string name="provider_list_title">AI Providers</string>
<string name="no_providers_configured">No providers configured</string>
<string name="add_provider_to_get_started">Add a provider to get started with AI features</string>
<string name="add_first_provider">Add First Provider</string>
<string name="active_provider">Active provider</string>
<string name="invalid_configuration">Invalid configuration</string>

<!-- Provider Item Actions -->
<string name="edit_provider">Edit provider</string>
<string name="delete_provider">Delete provider</string>
<string name="delete_provider_title">Delete Provider</string>
<string name="delete_provider_confirmation">Are you sure you want to delete \"%s\"? This action cannot be undone.</string>

<!-- Provider Edit -->
<string name="add_provider">Add Provider</string>
<string name="edit_provider">Edit Provider</string>
<string name="provider_name_label">Provider Name</string>
<string name="provider_type_label">Provider Type</string>
<string name="save">Save</string>
<string name="cancel">Cancel</string>

<!-- Configuration Form -->
<string name="api_key_label">API Key</string>
<string name="model_id_label">Model ID</string>
<string name="base_url_label">Base URL</string>
<string name="base_url_placeholder">https://api.openai.com/v1</string>
<string name="base_url_help">Leave empty to use default endpoint</string>
<string name="timeout_label">Request Timeout</string>
<string name="timeout_help">Seconds (30-600)</string>
<string name="timeout_range_error">Must be between 30 and 600</string>
<string name="azure_settings">Azure OpenAI Settings</string>
<string name="azure_api_version">API Version</string>
<string name="azure_deployment_id">Deployment ID</string>
<string name="required_for_azure">Required for Azure OpenAI</string>
<string name="required_field">Required</string>
<string name="default_model">Default: %s</string>

<!-- Settings Menu (UPDATE EXISTING) -->
<string name="api_key">Provider</string> <!-- Changed from "API key" -->
```

## Design Tokens

### Colors

```kotlin
// Active Provider
val activeProviderBorder = MaterialTheme.colorScheme.primary
val activeProviderBackground = MaterialTheme.colorScheme.primaryContainer

// Validation States
val errorColor = MaterialTheme.colorScheme.error
val successColor = MaterialTheme.colorScheme.primary

// Disabled States
val disabledButtonAlpha = 0.38f
```

### Spacing

```kotlin
val spacingExtraSmall = 4.dp
val spacingSmall = 8.dp
val spacingMedium = 16.dp
val spacingLarge = 24.dp
val spacingExtraLarge = 32.dp
```

### Typography

```kotlin
// Screen Titles
val screenTitleStyle = MaterialTheme.typography.headlineSmall

// Provider Names
val providerNameStyle = MaterialTheme.typography.titleMedium

// Labels
val labelStyle = MaterialTheme.typography.titleSmall

// Body Text
val bodyStyle = MaterialTheme.typography.bodyMedium

// Helper Text
val helperStyle = MaterialTheme.typography.bodySmall
```

### Shapes

```kotlin
val cardShape = MaterialTheme.shapes.medium
val dialogShape = MaterialTheme.shapes.large
val buttonShape = MaterialTheme.shapes.small
```

## Accessibility Checklist

- [ ] All interactive elements have content descriptions
- [ ] Focus order is logical (tab navigation)
- [ ] Minimum touch target size: 48x48dp
- [ ] Color contrast ratio ≥ 4.5:1
- [ ] Screen reader announces active provider first
- [ ] Validation errors are announced
- [ ] Keyboard shortcuts work where applicable
- [ ] Progress indicators announced during save

## Responsive Design

### Phone (Portrait)
- Full-width cards
- Vertical scrolling
- FAB in bottom-right corner

### Tablet (Landscape)
- Cards in two-column grid
- More horizontal space for forms
- FAB positioned optimally

### Foldable
- Adapt to hinge position
- Single-column when folded
- Two-column when unfolded

## Animation

### Transitions

```kotlin
// Provider List Item
val scale by animateFloatAsState(
    targetValue = if (isActive) 1.02f else 1f,
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow,
    ),
)

// Delete Confirmation
val dialogAlpha by animateFloatAsState(
    targetValue = if (showDialog) 1f else 0f,
    animationSpec = tween(durationMillis = 300),
)

// Save Button
val loadingRotation by animateFloatAsState(
    targetValue = if (isSaving) 360f else 0f,
    animationSpec = infiniteRepeatable(
        animation = tween(1000, easing = LinearEasing),
        repeatMode = RepeatMode.Restart,
    ),
)
```

### Micro-interactions

- Button press: Scale down slightly
- Provider activation: Border fade-in
- Delete button: Shake animation (prevent accidental)
- Form field focus: Label color change
- Validation error: Shake field

## Dark Mode

All screens support dark mode with:
- High contrast for active provider
- Error colors remain visible
- Surface colors adapt to theme
- Icons use theme colors

## Next Steps

1. ✅ UI/UX design complete
2. ⏭️ Specification Writing (Phase 6) - Create implementation plan
3. ⏭️ Create detailed wireframes/mockups (optional)

## Conclusion

The UI/UX design follows Material Design 3 principles and existing app patterns. The design prioritizes:

- **Clarity:** Clear visual hierarchy with active providers highlighted
- **Safety:** Confirmation dialogs prevent accidental deletions
- **Efficiency:** Minimal taps to complete tasks
- **Accessibility:** Full screen reader and keyboard support
- **Consistency:** Matches existing Settings UI patterns

The design is ready for implementation.
