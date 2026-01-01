# Migration Guide: AI Features - Multiple Providers

**Feature ID**: 001
**Status**: Implemented (Phase 1), Bug Fixes Applied (Phase 2)
**Created**: 2025-12-31
**Last Updated**: 2026-01-01

---

## Table of Contents

1. [Overview](#overview)
2. [What's New](#whats-new)
3. [Getting Started](#getting-started)
4. [Provider Configuration](#provider-configuration)
5. [Provider Switching](#provider-switching)
6. [Troubleshooting](#troubleshooting)
7. [API Key Security](#api-key-security)
8. [Version History](#version-history)
9. [FAQ](#faq)

---

## Overview

This guide helps users migrate from the previous OpenAI-only implementation to the new multi-provider AI feature that supports both OpenAI-compatible and Anthropic Claude providers.

### Key Changes

| Before | After |
|--------|-------|
| Only OpenAI supported | OpenAI-compatible + Anthropic Claude |
| Hardcoded model list | Dynamic model fetching (OpenAI) |
| Single settings form | Provider-specific settings forms |
| No provider selection | Provider dropdown in settings |

### Benefits

- ✅ **Multiple Provider Support**: Choose from OpenAI, Azure OpenAI, DeepSeek, Perplexity, Anthropic Claude, and more
- ✅ **Dynamic Model Discovery**: OpenAI models fetched automatically from API
- ✅ **Flexible Configuration**: Custom base URLs for enterprise deployments
- ✅ **Easy Switching**: Change providers without losing settings
- ✅ **Type-Safe**: Compile-time guarantees prevent invalid configurations

---

## What's New

### New Providers

#### OpenAI-Compatible

Supports all OpenAI-compatible APIs:
- **OpenAI**: `https://api.openai.com/v1`
- **Azure OpenAI**: Custom endpoint URL
- **DeepSeek**: `https://api.deepseek.com/v1`
- **Perplexity**: `https://api.perplexity.ai`
- **Other OpenAI-compatible APIs**: Any API following OpenAI's format

**Features**:
- Dynamic model listing from API
- Fallback to known models if API fails
- Custom base URL support
- GPT and O1 model filtering

#### Anthropic Claude

Native support for Anthropic's Claude models:
- **claude-3-5-sonnet-20241022**: Most capable model
- **claude-3-5-haiku-20241022**: Fastest model
- **claude-3-opus-20240229**: Previous generation flagship
- And any future Claude models

**Features**:
- Direct model ID input (no dropdown)
- Language detection in summaries
- Custom base URL support
- Optimized for long-form content

### New UI Features

| Feature | Description |
|---------|-------------|
| **Provider Dropdown** | Easy switching between providers |
| **API Key Masking** | Keys displayed as `•••••••` for security |
| **Dynamic Forms** | Form fields change based on selected provider |
| **Model Selection** | Dropdown for OpenAI, text input for Anthropic |
| **Real-time Validation** | Immediate feedback on settings validity |
| **Error Messages** | Clear explanations when configuration is invalid |

---

## Getting Started

### Step 1: Open AI Settings

1. Open Feeder app
2. Tap the menu icon (☰) in the top-left
3. Select **Settings**
4. Scroll down to **AI** section
5. Tap on **AI Provider**

### Step 2: Choose Your Provider

You'll see a dropdown with two options:
- **OpenAI-compatible** (default)
- **Anthropic (Claude)**

Select your preferred provider.

### Step 3: Configure Your Provider

#### For OpenAI-compatible:

1. **API Key**: Enter your API key
   - Format: `sk-...` for OpenAI
   - Format varies for other providers
2. **Base URL**: Enter the API endpoint
   - Default: `https://api.openai.com/v1`
   - Change this for Azure, DeepSeek, etc.
3. **Model**: Select from dropdown
   - Models are fetched automatically from the API
   - Common choices: `gpt-4o-mini`, `gpt-4o`, `o1-preview`

#### For Anthropic (Claude):

1. **API Key**: Enter your Anthropic API key
   - Format: `sk-ant-...`
2. **Base URL**: Enter the API endpoint
   - Default: `https://api.anthropic.com`
   - Rarely needs changing
3. **Model ID**: Enter the model ID
   - Common choices:
     - `claude-3-5-sonnet-20241022` (recommended)
     - `claude-3-5-haiku-20241022` (faster, cheaper)
     - `claude-3-opus-20240229` (previous generation)

### Step 4: Test Your Configuration

1. Navigate to any article in your feed
2. Scroll to the bottom of the article
3. Tap the **Summarize** button
4. Wait a few seconds for the summary to appear

If you see an error, check the [Troubleshooting](#troubleshooting) section.

---

## Provider Configuration

### OpenAI-Compatible Provider

#### OpenAI (Official)

**Recommended Models**:
- `gpt-4o-mini` - Fast, cost-effective (recommended)
- `gpt-4o` - Most capable
- `o1-preview` - Best for complex reasoning
- `o1-mini` - Faster reasoning

**Configuration**:
```
API Key: sk-proj-...
Base URL: https://api.openai.com/v1
Model: gpt-4o-mini
```

**Getting an API Key**:
1. Visit https://platform.openai.com/api-keys
2. Sign in or create an account
3. Click "Create new secret key"
4. Copy the key (starts with `sk-proj-`)
5. Paste it into Feeder

#### Azure OpenAI

**Configuration**:
```
API Key: your-azure-api-key
Base URL: https://your-resource-name.openai.azure.com/openai/deployments/your-deployment-name
Model: your-deployment-name (e.g., gpt-4)
```

**Note**: Azure uses a different URL format. Replace `your-resource-name` and `your-deployment-name` with your actual values.

#### DeepSeek

**Configuration**:
```
API Key: sk-...
Base URL: https://api.deepseek.com/v1
Model: deepseek-chat
```

**Getting an API Key**:
1. Visit https://platform.deepseek.com/
2. Sign in or create an account
3. Navigate to API Keys
4. Create a new key
5. Paste it into Feeder

#### Perplexity

**Configuration**:
```
API Key: pplx-...
Base URL: https://api.perplexity.ai
Model: sonar-small-online or llama-3.1-sonar-small-128k-online
```

**Getting an API Key**:
1. Visit https://www.perplexity.ai/settings/api
2. Sign in or create an account
3. Generate a new API key
4. Paste it into Feeder

### Anthropic Claude Provider

#### Recommended Models

| Model | Best For | Speed | Cost |
|-------|----------|-------|------|
| `claude-3-5-sonnet-20241022` | General use, complex tasks | Medium | Medium |
| `claude-3-5-haiku-20241022` | Fast summaries, simple tasks | Fast | Low |
| `claude-3-opus-20240229` | Legacy projects | Slow | High |

#### Configuration

**Default (Anthropic Official)**:
```
API Key: sk-ant-...
Base URL: https://api.anthropic.com
Model ID: claude-3-5-sonnet-20241022
```

**Getting an API Key**:
1. Visit https://console.anthropic.com/
2. Sign in or create an account
3. Navigate to API Keys
4. Create a new key
5. Paste it into Feeder

#### Custom Base URL

**When to Use**:
- Enterprise deployments
- Proxy servers
- Regional endpoints

**Example**:
```
Base URL: https://your-proxy.example.com/anthropic
```

---

## Provider Switching

### How to Switch Providers

1. Open **Settings** → **AI Provider**
2. Tap the **Provider** dropdown
3. Select a different provider:
   - From "OpenAI-compatible" to "Anthropic (Claude)"
   - From "Anthropic (Claude)" to "OpenAI-compatible"
4. Configure the new provider's settings (API key, model, etc.)
5. Your previous provider's settings are **automatically saved**

### Settings Preservation

**Important**: Switching providers does **not** delete your previous provider's settings.

| Scenario | Behavior |
|----------|----------|
| Switch from OpenAI to Anthropic | OpenAI settings saved |
| Switch back to OpenAI | Previous OpenAI settings restored |
| Update OpenAI settings | Anthropic settings unchanged |
| Update Anthropic settings | OpenAI settings unchanged |

### Tips for Switching

1. **Test Both Providers**: Try summarizing the same article with both providers to compare results
2. **Cost Considerations**: OpenAI `gpt-4o-mini` is typically cheaper than Anthropic `claude-3-5-sonnet`
3. **Speed vs Quality**: Anthropic Haiku is fast but less capable; OpenAI O1 is slow but highly capable
4. **Language Support**: Anthropic has better multilingual support with automatic language detection

---

## Troubleshooting

### Common Issues

#### Issue: "Invalid Setting" Error

**Symptoms**:
- "Invalid setting" message appears in AI settings
- "Summarize" button doesn't appear in article view
- Just entered API key but still shows as invalid

**Causes**:
1. API key is empty
2. Model ID is empty
3. **Phase 2 Bug (FIXED)**: Provider type not synchronized

**Solutions**:
1. **Check API Key**: Make sure you've entered a valid API key
2. **Check Model ID**: Make sure model is selected (OpenAI) or entered (Anthropic)
3. **Re-enter Settings**: Delete and re-enter your API key and model
4. **Restart App**: Close and reopen Feeder
5. **Check Provider**: Ensure you've selected the correct provider for your API key

**If Issue Persists** (Phase 2 Bug Fix Applied):
- The bug causing "invalid setting" for valid Anthropic credentials has been **fixed**
- If you still see this error with valid credentials, please report a bug

#### Issue: "No Models Were Found" Message

**Symptoms**:
- "No models were found" message appears for OpenAI provider
- Model dropdown is empty

**Causes**:
1. Invalid API key
2. Network connection issues
3. API service is down

**Solutions**:
1. **Verify API Key**: Check that your API key is correct
2. **Check Network**: Ensure you have internet connection
3. **Check API Status**: Visit the provider's status page
4. **Use Fallback**: The app will use fallback models if API fails

**Note**: For Anthropic provider, you should **not** see this message (Phase 2 fix). Users input model ID directly.

#### Issue: Summary Generation Fails

**Symptoms**:
- "Summarize" button appears but tapping it shows an error
- "Failed to generate summary" message

**Causes**:
1. Invalid API credentials
2. Rate limit exceeded
3. Insufficient API credits
4. Network timeout

**Solutions**:
1. **Check API Key**: Verify your API key is valid and active
2. **Check Credits**: Ensure you have sufficient credits/quota
3. **Check Rate Limits**: Some APIs have rate limits (e.g., 3 requests/minute)
4. **Try Different Article**: Test with a shorter article
5. **Check Network**: Ensure stable internet connection

#### Issue: Slow Summary Generation

**Symptoms**:
- Summary takes > 30 seconds to appear
- App appears frozen

**Causes**:
1. Slow API response
2. Large article content
3. Network latency

**Solutions**:
1. **Wait Longer**: Some models (like O1) take longer to process
2. **Try Faster Model**: Switch to `gpt-4o-mini` or `claude-3-5-haiku`
3. **Check Article Size**: Very long articles take longer
4. **Check Network**: Slow internet can cause delays

#### Issue: Settings Not Persisting

**Symptoms**:
- Settings disappear after closing app
- Provider selection resets to default

**Causes**:
1. App storage permissions
2. App data cleared
3. Bug in settings persistence

**Solutions**:
1. **Check Permissions**: Ensure Feeder has storage access
2. **Re-enter Settings**: Enter settings again
3. **Don't Clear Data**: Avoid clearing app data
4. **Restart App**: Close and reopen Feeder

---

## API Key Security

### Best Practices

1. **Never Share Your API Key**: Your API key is like a password
2. **Use Environment-Specific Keys**: Use different keys for dev/prod
3. **Rotate Keys Regularly**: Change your API keys periodically
4. **Monitor Usage**: Check your API dashboard for unusual activity
5. **Set Limits**: Configure rate limits and spending caps

### How Feeder Protects Your Key

| Protection | Description |
|------------|-------------|
| **Masked Display** | API keys shown as `•••••••` in UI |
| **Encrypted Storage** | Keys stored in encrypted SharedPreferences |
| **No Transmission** | Keys never sent to Feeder servers |
| **Local Only** | Keys only used for direct API calls |
| **No Logging** | Keys never logged or crash-reported |

### What We Don't Do

- ❌ We don't send your API key to our servers
- ❌ We don't log your API key
- ❌ We don't share your API key with third parties
- ❌ We don't use your API key for anything other than your requests

### Revoking a Compromised Key

If you suspect your API key has been compromised:

1. **Revoke Immediately**: Go to your provider's console and revoke the key
2. **Generate New Key**: Create a new API key
3. **Update Feeder**: Replace the old key with the new one in settings
4. **Monitor Usage**: Check for any unauthorized usage

---

## Version History

### Phase 2: Bug Fixes (2026-01-01)

**Fixed Issues**:
- ✅ Bug #002: Provider type synchronization fixed
  - Anthropic API no longer shows "invalid setting" with correct credentials
  - `setAIProviderType()` now called when settings updated
- ✅ Anthropic model list removed per user request
  - Users now input model ID directly
  - `listModels()` returns `emptyList()`
- ✅ "No models" message fix
  - Message no longer appears for Anthropic provider
  - Added `isAnthropic` parameter to `AIModelsStatus`

**Files Modified**:
- `SettingsViewModel.kt` - Added `setAIProviderType()` calls
- `AnthropicClient.kt` - Removed hardcoded model list
- `AIProviderSection.kt` - Added `isAnthropic` parameter

### Phase 1: Initial Implementation (2025-12-31)

**Commit**: `0ec80f2065c2dda4e34edb9ad4accb34e37964e1`

**New Features**:
- ✅ Multi-provider architecture (OpenAI + Anthropic)
- ✅ Provider dropdown in settings
- ✅ Dynamic model fetching for OpenAI
- ✅ Direct model input for Anthropic
- ✅ Settings persistence
- ✅ Provider switching without data loss

**Files Created**:
- `AIClient.kt` - Unified interface
- `AIApi.kt` - Factory pattern
- `AIProvider.kt` - Provider enum
- `AISettings.kt` - Sealed interface
- `OpenAICompatibleClient.kt` - OpenAI client
- `AnthropicClient.kt` - Anthropic client

**Files Modified**:
- `SettingsStore.kt` - Added `aiSettingsFlow`
- `Repository.kt` - Exposed AI methods
- `AIProviderSection.kt` - Multi-provider UI
- `SettingsViewModel.kt` - Event handling
- `ArticleViewModel.kt` - Uses `aiSettingsFlow`
- And 6 others...

---

## FAQ

### General Questions

**Q: Can I use both providers at the same time?**

A: No, you can only use one provider at a time. However, you can switch between providers instantly, and your settings for each provider are saved separately.

**Q: Do I need separate API keys for each provider?**

A: Yes, each provider requires its own API key. You'll need an OpenAI API key for the OpenAI-compatible provider, and an Anthropic API key for the Anthropic provider.

**Q: Which provider should I choose?**

A: It depends on your needs:
- **OpenAI**: Faster, cheaper, good for simple summaries
- **Anthropic**: Better at complex reasoning, better multilingual support

**Q: Can I use Azure OpenAI?**

A: Yes! Select "OpenAI-compatible" provider and enter your Azure endpoint URL in the "Base URL" field.

**Q: Are my API keys sent to Feeder servers?**

A: No, your API keys are stored locally on your device and used only for direct API calls to the provider. We never see your keys.

### Technical Questions

**Q: Why does Anthropic use text input instead of dropdown for models?**

A: Anthropic doesn't provide a public API endpoint for listing models. Users input the model ID directly, which allows using any model (including future ones) without waiting for app updates.

**Q: Why does OpenAI have a model dropdown?**

A: OpenAI provides a public API endpoint for listing available models. The app fetches this list automatically, so you always see the latest available models.

**Q: What happens if I switch providers while generating a summary?**

A: The current summary generation will complete using the original provider. New summaries will use the newly selected provider.

**Q: How long are my settings saved?**

A: Settings are saved indefinitely until you:
- Manually change them
- Clear app data
- Uninstall the app

### Cost and Billing Questions

**Q: How much does it cost to generate summaries?**

A: Costs vary by provider and model:
- OpenAI `gpt-4o-mini`: ~$0.15 per 1M tokens (very affordable)
- Anthropic `claude-3-5-sonnet`: ~$3 per 1M tokens
- See provider pricing pages for current rates

**Q: Will I be charged by Feeder?**

A: No, Feeder is free. You only pay the AI provider (OpenAI/Anthropic) based on your API usage.

**Q: How can I check my usage?**

A: Check your provider's dashboard:
- OpenAI: https://platform.openai.com/usage
- Anthropic: https://console.anthropic.com/

### Troubleshooting Questions

**Q: I'm getting "invalid setting" even with correct API key. What do I do?**

A: This bug has been **fixed** in Phase 2 (2026-01-01). If you still experience this issue:
1. Make sure you've selected the correct provider
2. Re-enter your API key and model
3. Restart the app
4. Report a bug if the issue persists

**Q: The model dropdown is empty for OpenAI. What's wrong?**

A: This usually means:
1. Your API key is invalid
2. You have no internet connection
3. The OpenAI API is down

The app will use fallback models if the API call fails.

**Q: Summary generation is very slow. Is this normal?**

A: It depends on the model:
- `gpt-4o-mini`: ~5-10 seconds
- `claude-3-5-sonnet`: ~10-20 seconds
- `o1-preview`: ~30-60 seconds (slow but accurate)

If it's taking longer than 60 seconds, check your network connection.

---

## Getting Help

### Documentation

- **Requirements**: [./01-requirement.md](./01-requirement.md)
- **Architecture**: [./02-architecture.md](./02-architecture.md)
- **API Documentation**: [./07-api-documentation.md](./07-api-documentation.md)
- **Implementation Summary**: [./09-implementation-summary.md](./09-implementation-summary.md)

### Support

If you encounter issues not covered in this guide:

1. **Check GitHub Issues**: Search for similar problems
2. **Create New Issue**: Provide detailed information:
   - Feeder version
   - Android version
   - Provider you're using
   - Steps to reproduce
   - Error messages (if any)
3. **Include Logs**: If possible, include logcat output

### Contributing

Found a bug or want to request a feature?

1. Check existing issues first
2. Create a new issue with clear description
3. For code contributions, see the project's contributing guide

---

## References

- Requirements: [./01-requirement.md](./01-requirement.md)
- Architecture: [./02-architecture.md](./02-architecture.md)
- Debug Analysis: [./03-debug-analysis.md](./03-debug-analysis.md)
- Technical Specification: [./04-specification.md](./04-specification.md)
- Implementation Plan: [./05-implementation-plan.md](./05-implementation-plan.md)
- Testing Strategy: [./06-testing-strategy.md](./06-testing-strategy.md)
- API Documentation: [./07-api-documentation.md](./07-api-documentation.md)
- Implementation Summary: [./09-implementation-summary.md](./09-implementation-summary.md)
