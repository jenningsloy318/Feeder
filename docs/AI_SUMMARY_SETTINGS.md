# AI Summary Settings - User Guide

**Feature**: AI Summary Configuration
**Version**: 2.17.0 (Unreleased)
**Last Updated**: 2026-01-01

---

## Overview

Feeder's AI Summary feature allows you to automatically generate concise summaries of articles using your preferred AI provider. This guide explains how to configure and use the Summary settings.

---

## Supported AI Providers

Feeder currently supports the following AI providers:

1. **OpenAI-Compatible**: Works with OpenAI API and compatible services
2. **Anthropic Claude**: Uses Anthropic's Claude API

---

## How to Configure AI Summaries

### Step 1: Set Up AI Provider

1. Open Feeder
2. Go to **Settings** (gear icon)
3. Tap **AI Integration**
4. Select your preferred **AI Provider**:
   - **OpenAI-Compatible**: Requires API key from OpenAI or compatible service
   - **Anthropic**: Requires Anthropic API key
5. Enter your **API Key**

### Step 2: Configure Summary Settings

1. In **AI Integration** settings
2. Tap **Summary**
3. Toggle **Enable summaries** on or off
4. Select your preferred **Summary Language**

### Step 3: Use AI Summaries

Once configured:
- Open any article in Feeder
- The AI will automatically generate a summary
- Summaries appear in the article view

---

## Settings Explained

### Enable Summaries

- **On**: Automatically summarize articles when opened
- **Off**: Disable automatic summarization (manual summaries still available if implemented)

**Default**: On

### Summary Language

Choose the language in which summaries should be generated. Available languages include:
- English
- Spanish
- French
- German
- Chinese (Simplified)
- Japanese
- Korean
- And more...

**Default**: Device language (if available)

---

## OPML Backup and Restore

Your AI summary settings are included when you export your feeds via OPML:

**Exported Settings**:
- AI summary enabled state
- Summary language preference

To restore:
1. Import your OPML file
2. AI summary settings will be restored automatically

---

## Troubleshooting

### Summaries Not Appearing

**Possible Causes**:
1. **Summaries Disabled**: Check Settings > AI Integration > Summary > Enable summaries
2. **No API Key**: Ensure you've configured your AI provider and API key
3. **API Error**: Verify your API key is valid and has sufficient credits
4. **Network Issue**: Check your internet connection

### Summary Language Not Working

**Possible Causes**:
1. **Language Not Supported**: Your chosen language may not be supported by the AI provider
2. **API Limitation**: Some providers may have limited language support

**Solution**: Try a different language or switch AI provider

---

## Privacy and Data Usage

### What Data is Sent to AI Providers

When summaries are enabled, Feeder sends the following to your configured AI provider:
- Article title
- Article content (text only)

### What Data is NOT Sent

- Your personal information
- Feed URLs
- Reading history
- Analytics data

### Data Retention

- Feeder does not store AI summaries on your device permanently
- AI providers may retain data according to their privacy policies
- Review your AI provider's privacy policy for details

---

## Best Practices

1. **API Key Security**:
   - Keep your API key confidential
   - Don't share your OPML export publicly (contains API key)

2. **Cost Management**:
   - AI providers charge per API call
   - Disable summaries if you want to reduce costs
   - Check your AI provider's pricing

3. **Performance**:
   - Summaries may take a few seconds to generate
   - Poor network connection will slow down summary generation

---

## Frequently Asked Questions

### Q: Can I summarize articles in a different language than the original?

**A**: Yes. The summary language is independent of the article language. For example, you can summarize a Spanish article in English.

### Q: Do I need to pay for AI summaries?

**A**: Yes, most AI providers charge per API call. Check your provider's pricing details. Feeder itself is free and does not charge for AI features.

### Q: Can I use AI summaries offline?

**A**: No. AI summaries require an internet connection to communicate with the AI provider's API.

### Q: Are my articles stored by the AI provider?

**A**: This depends on the AI provider's privacy policy. Review your provider's policy for details on data retention.

### Q: Can I configure different settings for different feeds?

**A**: Currently, summary settings apply globally to all feeds. Per-feed configuration is not supported.

---

## Technical Details

### API Endpoints

- **OpenAI-Compatible**: Uses OpenAI API or compatible endpoint
- **Anthropic**: Uses Anthropic's Messages API

### Summary Generation

- Triggered automatically when article is opened (if enabled)
- Uses the configured language parameter
- Returns concise summary of article content
- Displays in article view

### Error Handling

If summary generation fails:
- Error message displayed to user
- Original article content remains accessible
- App continues to function normally

---

## Feedback and Support

### Report Issues

If you encounter issues with AI summaries:
1. Check this guide first
2. Search existing issues on [GitHub](https://github.com/spacecowboy/feeder/issues)
3. Create a new issue with details:
   - AI provider used
   - Error message (if any)
   - Steps to reproduce

### Feature Requests

For feature requests:
1. Check existing requests on GitHub
2. Create a new request with:
   - Clear description of desired feature
   - Use case explanation
   - Potential implementation ideas

---

## Changelog

### Version 2.17.0 (Unreleased)

**New Features**:
- Dedicated Summary settings screen
- Enable/disable toggle for AI summaries
- Improved UX for summary language selection
- OPML import/export support for summary settings

**Bug Fixes**:
- Summary generation now respects enabled state
- Improved error handling for API failures

---

## Related Documentation

- [AI Integration Guide](./AI_INTEGRATION.md)
- [OPML Import/Export Guide](./OPML_GUIDE.md)
- [Privacy Policy](./PRIVACY.md)

---

**Last Updated**: 2026-01-01
**Document Version**: 1.0
