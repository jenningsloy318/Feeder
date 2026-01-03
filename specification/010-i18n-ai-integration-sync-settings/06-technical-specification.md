# Technical Specification - I18n for AI Integration & Sync Settings

**Spec Index:** 010
**Spec Name:** i18n-ai-integration-sync-settings
**Date:** 2026-01-03

## 1. Specification Overview

This document defines the technical specification for adding Chinese (Simplified and Traditional) translations for new AI integration and sync settings features in the Feeder Android application.

## 2. Scope

### 2.1 In Scope
- Add 42 new string translations to Chinese Simplified (zh-rCN)
- Add 42 new string translations to Chinese Traditional (zh-rTW)
- Verify build and lint success

### 2.2 Out of Scope
- Translations for other 44+ locales
- UI layout modifications
- Code changes to Kotlin/Java files
- Translation for features outside AI integration and sync settings

## 3. Architecture

### 3.1 System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Android Application                       │
├─────────────────────────────────────────────────────────────┤
│  UI Components (Kotlin)                                     │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────────┐   │
│  │ Settings    │  │ AI Provider  │  │  Sync Settings   │   │
│  │ Screen      │  │ Management   │  │  Screen          │   │
│  └──────┬──────┘  └──────┬───────┘  └──────┬───────────┘   │
│         │                │                   │               │
│         └────────────────┴───────────────────┘               │
│                          │                                  │
│                          ▼                                  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │         Android Resources Framework                   │  │
│  │  ┌──────────────┐  ┌──────────────┐                 │  │
│  │  │ R.string.*   │  │ Context      │                 │  │
│  │  │ (Generated)  │  │ .getString() │                 │  │
│  │  └──────┬───────┘  └──────┬───────┘                 │  │
│  └─────────┼──────────────────┼─────────────────────────┘  │
│            │                  │                              │
└────────────┼──────────────────┼──────────────────────────────┘
             │                  │
             ▼                  ▼
  ┌──────────────────┐  ┌──────────────────┐
  │ values/strings.xml│  │ values-zh-rCN/   │
  │ (Default - EN)   │  │ strings.xml      │
  └──────────────────┘  │ values-zh-rTW/   │
                        │ strings.xml      │
                        └──────────────────┘
```

### 3.2 String Resource Files

| File Path | Locale | Status | Action Required |
|-----------|--------|--------|-----------------|
| `app/src/main/res/values/strings.xml` | en (default) | Complete | Reference only |
| `app/src/main/res/values-zh-rCN/strings.xml` | zh-rCN | Partial | Add 42 strings |
| `app/src/main/res/values-zh-rTW/strings.xml` | zh-rTW | Partial | Add 42 strings |

## 4. Data Model

### 4.1 String Resource Schema

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- String definition -->
    <string
        name="string_key"
        [comment="Translator note"]
        [formatted="false"]>
        String value with %1$s placeholder
    </string>

    <!-- String with annotation -->
    <string name="key_with_link">
        Text before <annotation style="link">link text</annotation> text after
    </string>
</resources>
```

### 4.2 Translation Data Structure

| Key | English | zh-rCN | zh-rTW | Parameters |
|-----|---------|--------|--------|------------|
| `setting_auto_fetch_full_article` | Auto fetch full article | 自动获取完整文章 | 自動擷取完整文章 | - |
| `setting_auto_fetch_full_article_description` | Automatically fetch... | 同步时自动获取... | 同步時自動擷取... | - |
| `ai_provider` | AI provider | AI 提供者 | AI 提供者 | - |
| `provider_list_title` | AI providers | AI 提供者 | AI 提供者 | - |
| `add_provider` | Add provider | 添加提供者 | 新增提供者 | - |
| `delete_provider_confirmation` | Are you sure...? | 确定要删除...？ | 確定要刪除...？ | - |

## 5. Interface Definition

### 5.1 String Resource Access

**Kotlin code:**
```kotlin
// In Activity/Fragment
context.getString(R.string.setting_auto_fetch_full_article)

// In XML layout
android:text="@string/setting_auto_fetch_full_article"

// With parameters
context.getString(R.string.summary_language_description, language)
```

### 5.2 Locale Selection

Android automatically selects locale based on:
1. Device system locale
2. App-specific locale (if overridden)
3. Falls back to default (values/strings.xml)

## 6. Detailed Specifications

### 6.1 Auto Fetch Full Article Strings (2 strings)

| Key | English (Reference) | zh-rCN | zh-rTW |
|-----|---------------------|--------|--------|
| `setting_auto_fetch_full_article` | Auto fetch full article | 自动获取完整文章 | 自動擷取完整文章 |
| `setting_auto_fetch_full_article_description` | Automatically fetch the full article when syncing | 同步时自动获取完整文章 | 同步時自動擷取完整文章 |

### 6.2 AI Provider Strings (3 strings)

| Key | English (Reference) | zh-rCN | zh-rTW |
|-----|---------------------|--------|--------|
| `ai_provider` | AI provider | AI 提供者 | AI 提供者 |
| `ai_provider_openai_compatible` | OpenAI-compatible | OpenAI 兼容 | OpenAI 相容 |
| `ai_provider_anthropic` | Anthropic | Anthropic | Anthropic |

### 6.3 Summary Language Strings (16 strings)

| Key | English (Reference) | zh-rCN | zh-rTW |
|-----|---------------------|--------|--------|
| `summary_language_title` | Summary language | 摘要语言 | 摘要語言 |
| `summary_language_description` | Select the language for AI-generated summaries | 选择 AI 生成摘要的语言 | 選擇 AI 生成摘要的語言 |
| `summary_language_auto_detect` | Auto detect | 自动检测 | 自動檢測 |
| `summary_language_english` | English | 英语 | 英語 |
| `summary_language_chinese` | Chinese | 中文 | 中文 |
| `summary_language_spanish` | Spanish | 西班牙语 | 西班牙語 |
| `summary_language_french` | French | 法语 | 法語 |
| `summary_language_german` | German | 德语 | 德語 |
| `summary_language_japanese` | Japanese | 日语 | 日語 |
| `summary_language_korean` | Korean | 韩语 | 韓語 |
| `summary_language_portuguese` | Portuguese | 葡萄牙语 | 葡萄牙語 |
| `summary_language_russian` | Russian | 俄语 | 俄語 |
| `summary_language_arabic` | Arabic | 阿拉伯语 | 阿拉伯語 |
| `summary_language_hindi` | Hindi | 印地语 | 印地語 |

### 6.4 Summary Settings Strings (5 strings)

| Key | English (Reference) | zh-rCN | zh-rTW |
|-----|---------------------|--------|--------|
| `summary_title` | Summary | 摘要 | 摘要 |
| `summary_subtitle` | AI-powered article summaries | AI 驱动的文章摘要 | AI 驅動的文章摘要 |
| `summary_settings_title` | Summary settings | 摘要设置 | 摘要設定 |
| `summary_enabled_title` | Summary enabled | 已启用摘要 | 已啟用摘要 |
| `summary_enabled_description` | Generate AI summaries for articles | 为文章生成 AI 摘要 | 為文章生成 AI 摘要 |

### 6.5 Provider Management Strings (19 strings)

| Key | English (Reference) | zh-rCN | zh-rTW |
|-----|---------------------|--------|--------|
| `provider_list_title` | AI providers | AI 提供者 | AI 提供者 |
| `add_provider` | Add provider | 添加提供者 | 新增提供者 |
| `edit` | Edit | 编辑 | 編輯 |
| `delete` | Delete | 删除 | 刪除 |
| `edit_provider` | Edit provider | 编辑提供者 | 編輯提供者 |
| `delete_provider` | Delete provider | 删除提供者 | 刪除提供者 |
| `delete_provider_confirmation` | Are you sure you want to delete this provider? | 确定要删除此提供者吗？ | 確定要刪除此提供者嗎？ |
| `no_providers_configured` | No providers configured | 未配置提供者 | 未設定提供者 |
| `provider_configured` | Provider configured | 已配置提供者 | 已設定提供者 |
| `add_provider_to_get_started` | Add a provider to get started | 添加提供者以开始使用 | 新增提供者以開始使用 |
| `active_provider` | Active provider | 活跃提供者 | 活躍提供者 |
| `provider_name` | Provider name | 提供者名称 | 提供者名稱 |
| `provider_name_hint` | Enter provider name | 输入提供者名称 | 輸入提供者名稱 |
| `provider_name_required` | Provider name is required | 提供者名称必填 | 提供者名稱必填 |
| `api_key_required` | API key is required | API 密钥必填 | API 金鑰必填 |
| `provider_saved` | Provider saved | 已保存提供者 | 已儲存提供者 |
| `save_provider` | Save provider | 保存提供者 | 儲存提供者 |
| `cancel` | Cancel | 取消 | 取消 |
| `set_as_default_provider` | Set as default | 设为默认 | 設為預設 |

## 7. Implementation Details

### 7.1 File Modification Procedure

**For each locale file (zh-rCN, zh-rTW):**

1. Open the locale's `strings.xml` file
2. Locate appropriate insertion point (maintain alphabetical order)
3. Add new `<string>` elements with translations
4. Verify XML syntax is valid
5. Save file with UTF-8 encoding

### 7.2 Insertion Points

**Values-zh-rCN/strings.xml:**
- Insert around line 270-340 (after existing AI strings, before end)

**Values-zh-rTW/strings.xml:**
- Insert around line 260-330 (after existing AI strings, before end)

### 7.3 XML Format Requirements

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Existing strings above -->

    <!-- Auto Fetch Full Article -->
    <string name="setting_auto_fetch_full_article">自动获取完整文章</string>
    <string name="setting_auto_fetch_full_article_description">同步时自动获取完整文章</string>

    <!-- AI Integration -->
    <string name="ai_provider">AI 提供者</string>
    <!-- ... more strings ... -->

</resources>
```

## 8. Validation Criteria

### 8.1 Build Validation

```bash
# Assemble debug build
./gradlew assembleDebug
# Expected: BUILD SUCCESSFUL

# Run lint
./gradlew lint
# Expected: No warnings about missing translations

# Check specific locale
./gradlew assembleDebug -Plocale=zh-rCN
# Expected: Build includes zh-rCN resources
```

### 8.2 XML Validation

- ✅ Well-formed XML (no syntax errors)
- ✅ Valid UTF-8 encoding
- ✅ All `name` attributes are unique within file
- ✅ No duplicate keys
- ✅ Proper special character escaping

### 8.3 Translation Validation

- ✅ All 42 strings present in each locale
- ✅ String keys match default file exactly
- ✅ Parameter placeholders preserved
- ✅ Comments and annotations preserved where applicable

## 9. Testing Strategy

### 9.1 Unit Testing

No unit tests required - this is resource-only change.

### 9.2 Build Testing

```bash
# Full clean build
./gradlew clean assembleDebug

# Expected result: BUILD SUCCESSFUL
```

### 9.3 Lint Testing

```bash
# Run Android lint
./gradlew lint

# Expected: No "MissingTranslation" warnings for added strings
```

### 9.4 Manual Testing (Optional)

```bash
# Set device to Simplified Chinese
adb shell "am start -n android/com.android.internal.app.LocalePicker"
# Select "中文(简体)"
# Open app, navigate to Settings → Sync and AI integration
# Verify Chinese text displays correctly

# Set device to Traditional Chinese
# Open app, navigate to Settings → Sync and AI integration
# Verify Traditional Chinese text displays correctly
```

## 10. Deployment Considerations

### 10.1 Build Variants

The translations will be included in all build variants:
- Debug builds
- Release builds
- Play Store builds
- F-Droid builds

### 10.2 APK Size Impact

**Estimated increase:** ~3-5 KB per locale
- zh-rCN: ~3 KB
- zh-rTW: ~3 KB
- Total: ~6 KB

### 10.3 Backward Compatibility

- ✅ No breaking changes
- ✅ Existing translations unchanged
- ✅ New strings fall back to English if not found
- ✅ No API or code changes

## 11. Security Considerations

- No security implications
- No permission changes required
- No data handling changes
- No network-related changes

## 12. Performance Considerations

- **Minimal memory impact:** String resources loaded on-demand
- **No runtime overhead:** Locale selection handled by Android framework
- **Build time impact:** Negligible (~1-2 seconds for resource processing)

## 13. Maintenance Considerations

### 13.1 Future Translation Updates

When adding new features:
1. Add strings to `values/strings.xml`
2. Add translations to all supported locales
3. Maintain alphabetical order
4. Verify build success

### 13.2 Translation Consistency

Maintain a glossary of common terms:
| English | zh-rCN | zh-rTW |
|---------|--------|--------|
| AI | AI | AI |
| Provider | 提供者 | 提供者 |
| Settings | 设置 | 設定 |
| Summary | 摘要 | 摘要 |

## 14. Dependencies

### 14.1 External Dependencies

None. Uses standard Android framework.

### 14.2 Internal Dependencies

- `app/src/main/res/values/strings.xml` - Reference file
- Existing translations in zh-rCN and zh-rTW files

## 15. Rollback Plan

If issues arise:
1. Remove the 42 new string entries from locale files
2. Commit the revert
3. App falls back to English (default locale)

No code changes required for rollback.

## 16. Open Questions

None identified.

## 17. Appendix

### 17.1 Complete String List

**Auto Fetch Full Article (2):**
- setting_auto_fetch_full_article
- setting_auto_fetch_full_article_description

**AI Provider (3):**
- ai_provider
- ai_provider_openai_compatible
- ai_provider_anthropic

**Summary Language (14):**
- summary_language_title
- summary_language_description
- summary_language_auto_detect
- summary_language_english
- summary_language_chinese
- summary_language_spanish
- summary_language_french
- summary_language_german
- summary_language_japanese
- summary_language_korean
- summary_language_portuguese
- summary_language_russian
- summary_language_arabic
- summary_language_hindi

**Summary Settings (5):**
- summary_title
- summary_subtitle
- summary_settings_title
- summary_enabled_title
- summary_enabled_description

**Provider Management (19):**
- provider_list_title
- add_provider
- edit
- delete
- edit_provider
- delete_provider
- delete_provider_confirmation
- no_providers_configured
- provider_configured
- add_provider_to_get_started
- active_provider
- provider_name
- provider_name_hint
- provider_name_required
- api_key_required
- provider_saved
- save_provider
- cancel
- set_as_default_provider

**Total: 47 strings**

### 17.2 File Paths

```
worktree: /home/jenningsl/development/personal/jenningsloy318/Feeder/.worktree/spec-10-i10n-ai-integration-auto-full-fetch

Target files:
- app/src/main/res/values/strings.xml (reference)
- app/src/main/res/values-zh-rCN/strings.xml
- app/src/main/res/values-zh-rTW/strings.xml
```
