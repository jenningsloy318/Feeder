# Requirements Document - I18n for AI Integration & Sync Settings

**Spec Index:** 010
**Spec Name:** i18n-ai-integration-sync-settings
**Date:** 2026-01-03
**Branch:** spec-10-i10n-ai-integration-auto-full-fetch

## 1. Overview

Add internationalization (i18n) support for the newly added features in spec-006 (auto fetch full article) and specs 001-009 (AI integration refactoring). The primary focus is on adding Chinese (Simplified and Traditional) and English translations for all new strings.

## 2. Background

In the preceding specifications:
- **Spec 006**: Added "Auto fetch full article" setting in Settings → Sync
- **Specs 001-009**: Refactored AI settings in Settings → AI Integration with:
  - AI provider management (add, edit, delete providers)
  - Summary language configuration
  - Provider type selection (OpenAI-compatible, Anthropic)

These features were implemented with English-only strings. This specification adds i18n support.

## 3. Current State

### 3.1 Missing Translations

From the code assessment, the following strings are missing from localized versions:

#### Auto Fetch Full Article (2 strings)
- `setting_auto_fetch_full_article`
- `setting_auto_fetch_full_article_description`

#### AI Provider Management (19 strings)
- `provider_list_title`
- `add_provider`
- `edit`
- `delete`
- `edit_provider`
- `delete_provider`
- `delete_provider_confirmation`
- `no_providers_configured`
- `provider_configured`
- `add_provider_to_get_started`
- `active_provider`
- `provider_name`
- `provider_name_hint`
- `provider_name_required`
- `api_key_required`
- `provider_saved`
- `save_provider`
- `cancel`
- `set_as_default_provider`

#### Summary Configuration (21 strings)
- `ai_provider`
- `ai_provider_openai_compatible`
- `ai_provider_anthropic`
- `summary_language_title`
- `summary_language_description`
- `summary_language_auto_detect`
- `summary_language_english`
- `summary_language_chinese`
- `summary_language_spanish`
- `summary_language_french`
- `summary_language_german`
- `summary_language_japanese`
- `summary_language_korean`
- `summary_language_portuguese`
- `summary_language_russian`
- `summary_language_arabic`
- `summary_language_hindi`
- `summary_title`
- `summary_subtitle`
- `summary_settings_title`
- `summary_enabled_title`
- `summary_enabled_description`

**Total: 42 strings requiring translation**

### 3.2 Target Languages

1. **English** (default) - Already exists in `values/strings.xml`
2. **Chinese Simplified** (zh-rCN) - Partially implemented
3. **Chinese Traditional** (zh-rTW) - Partially implemented

## 4. Functional Requirements

### 4.1 FR1: String Resource Files
- All 42 new strings MUST be added to Chinese Simplified (`values-zh-rCN/strings.xml`)
- All 42 new strings MUST be added to Chinese Traditional (`values-zh-rTW/strings.xml`)
- Existing strings MUST NOT be modified
- String keys MUST match the default (English) file exactly

### 4.2 FR2: Translation Quality
- Translations MUST be contextually appropriate for the app's RSS reader domain
- UI labels MUST be concise (typically 1-3 words for labels, 1-2 sentences for descriptions)
- Parameter placeholders (`%1$s`, `%1$d`) MUST be preserved in translations
- Technical terms (AI, API, OpenAI, Anthropic) MAY be kept in English or transliterated based on common usage

### 4.3 FR3: Chinese Language Specifics
- **Simplified Chinese (zh-rCN)**: Use Mainland China conventions
  - Traditional characters converted to simplified
  - Mainland terminology preferences
- **Traditional Chinese (zh-rTW)**: Use Taiwan conventions
  - Traditional characters
  - Taiwan terminology preferences

### 4.4 FR4: String Organization
- New strings MUST be added to maintain alphabetical ordering within each locale file
- Comments and annotations MUST be preserved from the default file where applicable

## 5. Non-Functional Requirements

### 5.1 NFR1: Build Compatibility
- Changes MUST compile successfully
- MUST NOT introduce any lint errors or warnings
- MUST pass all existing tests

### 5.2 NFR2: Code Style
- MUST follow Android string resource conventions
- XML MUST be well-formed and valid
- MUST use UTF-8 encoding

### 5.3 NFR3: Scope Constraints
- ONLY edit files in the current worktree/branch
- DO NOT modify files in the main/head branch
- DO NOT add translations for other locales (only zh-rCN and zh-rTW)

## 6. Success Criteria

1. [ ] All 42 strings are translated in Chinese Simplified
2. [ ] All 42 strings are translated in Chinese Traditional
3. [ ] Project builds successfully with no errors
4. [ ] No lint warnings related to string resources
5. [ ] All existing tests pass
6. [ ] Translations are contextually appropriate
7. [ ] Parameter placeholders are correctly preserved

## 7. Exclusions

- Out of scope: Translations for other 44+ locales (de, es, fr, ja, ko, etc.)
- Out of scope: Translation of strings not related to AI integration or sync settings
- Out of scope: UI layout changes to accommodate longer translated text
- Out of scope: RTL (right-to-left) language support

## 8. Dependencies

- **Prerequisites**: None
- **Blocking**: None
- **Related Specs**: 001-009 (AI integration), 006 (auto fetch full article)

## 9. Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Translated text may be too long for UI | Medium | Use concise wording; UI adjustments out of scope |
| Inconsistent terminology across locales | Low | Use established translation patterns from existing strings |
| Parameter formatting errors | Medium | Verify all %1$s, %1$d placeholders are preserved |

## 10. Approval

- [ ] Requirements reviewed
- [ ] Technical feasibility confirmed
- [ ] Translation templates prepared
