# Research Report - I18n for AI Integration & Sync Settings

**Spec Index:** 010
**Spec Name:** i18n-ai-integration-sync-settings
**Date:** 2026-01-03

## 1. Android Internationalization Best Practices

### 1.1 String Resources Structure

Android uses standard resource directory structure for internationalization:

```
res/
  values/              # Default locale (English)
    strings.xml
  values-zh-rCN/       # Simplified Chinese (China)
    strings.xml
  values-zh-rTW/       # Traditional Chinese (Taiwan)
    strings.xml
```

### 1.2 String Resource Format

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
  <string name="key_name">String value</string>
  <string name="key_with_param">Value with %1$s parameter</string>
  <string name="key_with_comment" comment="Translator note">Value</string>
  <string name="key_with_annotation">Text with <annotation style="link">link</annotation></string>
</resources>
```

### 1.3 Parameter Placeholders

- `%1$s`, `%2$s` - String parameters
- `%1$d`, `%2$d` - Integer parameters
- `%1$f` - Float parameters
- Order must be preserved in translations

### 1.4 Chinese Locale Variants

| Locale Code | Name | Region |
|-------------|------|--------|
| `zh-rCN` | Simplified Chinese | Mainland China |
| `zh-rTW` | Traditional Chinese | Taiwan |
| `zh-rHK` | Traditional Chinese | Hong Kong |
| `zh-rSG` | Simplified Chinese | Singapore |

## 2. Feeder Project I18n Patterns

### 2.1 Current I18n Structure

The Feeder project has extensive i18n support:
- **Default (en)**: `values/strings.xml` - 347 lines, ~305 strings
- **Chinese Simplified**: `values-zh-rCN/strings.xml` - 287 lines
- **Chinese Traditional**: `values-zh-rTW/strings.xml` - 264 lines
- **44 other locales** supported

### 2.2 Naming Conventions

**Observed Patterns:**
1. Flat keys with underscores: `save`, `cancel`, `delete`
2. Feature prefixes: `sync_*`, `theme_*`, `summary_*`, `provider_*`
3. Settings prefix: `setting_auto_fetch_full_article`

**Inconsistency Found:**
- `openai_settings` vs `ai_provider_*` - different prefixes for related AI features

### 2.3 Translation Style Examples

From existing Chinese translations:

| English | Simplified Chinese |
|---------|-------------------|
| AI integration | AI 集成 |
| Compatible with many AI providers, but not all | 兼容众多 AI 提供者，但非全部 |
| OpenAI integration | OpenAI 整合 |
| Settings | 设置 |
| Sync | 同步 |
| Theme | 主题 |

### 2.4 Translation Completeness Analysis

**Well-localized areas:**
- Basic UI (navigation, actions)
- Feed management
- Synchronization features
- Theme settings
- Reading features

**Missing translations (new features):**
- AI provider management (19 strings)
- Summary language configuration (21 strings)
- Auto fetch full article (2 strings)

## 3. Chinese Translation Guidelines

### 3.1 Simplified Chinese (zh-rCN) - Mainland China

**Character Set:** Simplified Chinese characters
**Regional Preferences:**
- Use Mainland terminology
- Metric system for measurements
- Date format: YYYY年MM月DD日
- Time format: HH:mm

**Technical Terminology:**
| English | Simplified Chinese |
|---------|-------------------|
| AI | AI |
| Provider | 提供者 |
| Settings | 设置 |
| Configuration | 配置 |
| Summary | 摘要 |
| Language | 语言 |
| API key | API 密钥 |

### 3.2 Traditional Chinese (zh-rTW) - Taiwan

**Character Set:** Traditional Chinese characters
**Regional Preferences:**
- Use Taiwan terminology
- Metric system for measurements
- Date format: 民國YYY年MM月DD日 or YYYY年MM月DD日
- Time format: HH:mm

**Technical Terminology:**
| English | Traditional Chinese |
|---------|---------------------|
| AI | AI |
| Provider | 提供者 |
| Settings | 設定 |
| Configuration | 設定/配置 |
| Summary | 摘要 |
| Language | 語言 |
| API key | API 金鑰/密鑰 |

## 4. Translation Quality Standards

### 4.1 UI Text Length Guidelines

| Element | Max Length (Chinese) |
|---------|---------------------|
| Button label | 4-6 characters |
| Menu item | 8-12 characters |
| Title | 12-16 characters |
| Description | 30-50 characters |
| Dialog message | 50-100 characters |

Note: Chinese characters are more compact than English, typically conveying 2x more meaning per character.

### 4.2 Contextual Translation Rules

1. **Preserve technical terms**: AI, API, OpenAI, Anthropic may remain in English
2. **Be concise**: UI labels should be short and clear
3. **Maintain consistency**: Use same translation for same English term across all strings
4. **Reserve formality**: Use polite but not overly formal language for app UI

### 4.3 Parameter Handling

When translating strings with parameters:
- Preserve placeholder syntax: `%1$s`, `%1$d`
- Maintain parameter order (unless language requires reordering)
- Test with actual parameter values to ensure proper formatting

## 5. Common Pitfalls to Avoid

1. **Hard-coded strings**: All user-facing text must be in strings.xml
2. **Missing translations**: Ensure all new strings in default locale are added to target locales
3. **Parameter loss**: Double-check all `%1$s` placeholders are preserved
4. **Encoding issues**: Always use UTF-8 encoding
5. **Malformed XML**: Verify XML structure is valid
6. **Inconsistent terminology**: Use translation glossary for consistency

## 6. Tools and Resources

### 6.1 Translation Tools
- Android Studio: Built-in Translations Editor
- ADB: Test locale changes with `adb shell am instrument`
- Crowdin/Weblate: For collaborative translation management

### 6.2 Testing Commands
```bash
# Change device language to Simplified Chinese
adb shell "su -c 'setprop persist.sys.locale zh-CN&&setprop ctl.restart zygote'"

# Or use:
adb shell am start -n android/com.android.internal.app.LocalePicker

# Build and verify
./gradlew assembleDebug
./gradlew lint
```

### 6.3 Online Resources
- Android Localization Guide: https://developer.android.com/guide/topics/resources/localization
- Unicode Chinese characters: https://unicode.org/charts/
- Google Translate (for initial draft, requires human review)

## 7. Recommended Approach

For this specification, the recommended approach is:

1. **Extract strings**: Identify all 42 new strings from default locale
2. **Create translation table**: English → zh-rCN → zh-rTW
3. **Translate systematically**: Group by feature (auto fetch, AI provider, summary)
4. **Review for consistency**: Check terminology consistency
5. **Verify parameters**: Ensure all placeholders preserved
6. **Test on device**: Switch device locale to verify appearance
7. **Build verification**: Run full build to catch any errors

## 8. References

- Android Developer Guide: Localization
- Android Resources Overview: String Resources
- Unicode Standard: Chinese Characters
- Feeder Project: Existing i18n implementation

## 9. Conclusion

The Feeder project has a well-established i18n infrastructure with 46+ locales. Adding Chinese translations for the new AI integration and sync settings features is a straightforward process of:

1. Adding 42 new string entries to `values-zh-rCN/strings.xml`
2. Adding 42 new string entries to `values-zh-rTW/strings.xml`
3. Ensuring XML validity and proper encoding
4. Verifying build success

No architectural changes or code modifications are required.
