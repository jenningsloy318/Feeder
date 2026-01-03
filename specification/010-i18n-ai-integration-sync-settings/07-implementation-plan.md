# Implementation Plan - I18n for AI Integration & Sync Settings

**Spec Index:** 010
**Spec Name:** i18n-ai-integration-sync-settings
**Date:** 2026-01-03

## 1. Executive Summary

This document outlines the implementation plan for adding Chinese (Simplified and Traditional) translations for new AI integration and sync settings features.

**Complexity:** Low - Resource file updates only
**Risk Level:** Low - No code changes required
**Estimated Effort:** 2-3 hours

## 2. Implementation Approach

### 2.1 Strategy

**Approach:** Direct resource file modification

This task requires only updating XML string resource files. No code changes are needed because:
- Android's resource framework handles locale selection automatically
- R.java constants are generated from resource files
- UI components reference resources by ID (R.string.*)

### 2.2 Implementation Phases

```
Phase 1: Preparation
   └─> Read default strings.xml to identify exact strings

Phase 2: Chinese Simplified (zh-rCN)
   └─> Add 42 translations to values-zh-rCN/strings.xml

Phase 3: Chinese Traditional (zh-rTW)
   └─> Add 42 translations to values-zh-rTW/strings.xml

Phase 4: Verification
   └─> Build and lint verification

Phase 5: Documentation
   └─> Update spec summary
```

## 3. Detailed Implementation Steps

### 3.1 Phase 1: Preparation

**Step 1.1:** Read default strings file
```bash
# File location
app/src/main/res/values/strings.xml
```

**Step 1.2:** Extract the 42 new strings (lines 271-346)
- Auto Fetch Full Article: 2 strings
- AI Provider: 3 strings
- Summary Language: 16 strings
- Summary Settings: 5 strings
- Provider Management: 19 strings

**Step 1.3:** Read existing locale files
```bash
# Simplified Chinese
app/src/main/res/values-zh-rCN/strings.xml

# Traditional Chinese
app/src/main/res/values-zh-rTW/strings.xml
```

**Step 1.4:** Identify insertion points
- Find where existing AI strings end
- Maintain alphabetical ordering

### 3.2 Phase 2: Chinese Simplified (zh-rCN)

**File:** `app/src/main/res/values-zh-rCN/strings.xml`

**Step 2.1:** Auto Fetch Full Article strings
```xml
<!-- Around line 270-280, after existing sync settings -->
<string name="setting_auto_fetch_full_article">自动获取完整文章</string>
<string name="setting_auto_fetch_full_article_description">同步时自动获取完整文章</string>
```

**Step 2.2:** AI Provider strings
```xml
<string name="ai_provider">AI 提供者</string>
<string name="ai_provider_openai_compatible">OpenAI 兼容</string>
<string name="ai_provider_anthropic">Anthropic</string>
```

**Step 2.3:** Summary Language strings
```xml
<string name="summary_language_title">摘要语言</string>
<string name="summary_language_description">选择 AI 生成摘要的语言</string>
<string name="summary_language_auto_detect">自动检测</string>
<string name="summary_language_english">英语</string>
<string name="summary_language_chinese">中文</string>
<string name="summary_language_spanish">西班牙语</string>
<string name="summary_language_french">法语</string>
<string name="summary_language_german">德语</string>
<string name="summary_language_japanese">日语</string>
<string name="summary_language_korean">韩语</string>
<string name="summary_language_portuguese">葡萄牙语</string>
<string name="summary_language_russian">俄语</string>
<string name="summary_language_arabic">阿拉伯语</string>
<string name="summary_language_hindi">印地语</string>
```

**Step 2.4:** Summary Settings strings
```xml
<string name="summary_title">摘要</string>
<string name="summary_subtitle">AI 驱动的文章摘要</string>
<string name="summary_settings_title">摘要设置</string>
<string name="summary_enabled_title">已启用摘要</string>
<string name="summary_enabled_description">为文章生成 AI 摘要</string>
```

**Step 2.5:** Provider Management strings
```xml
<string name="provider_list_title">AI 提供者</string>
<string name="add_provider">添加提供者</string>
<string name="edit">编辑</string>
<string name="delete">删除</string>
<string name="edit_provider">编辑提供者</string>
<string name="delete_provider">删除提供者</string>
<string name="delete_provider_confirmation">确定要删除此提供者吗？</string>
<string name="no_providers_configured">未配置提供者</string>
<string name="provider_configured">已配置提供者</string>
<string name="add_provider_to_get_started">添加提供者以开始使用</string>
<string name="active_provider">活跃提供者</string>
<string name="provider_name">提供者名称</string>
<string name="provider_name_hint">输入提供者名称</string>
<string name="provider_name_required">提供者名称必填</string>
<string name="api_key_required">API 密钥必填</string>
<string name="provider_saved">已保存提供者</string>
<string name="save_provider">保存提供者</string>
<string name="cancel">取消</string>
<string name="set_as_default_provider">设为默认</string>
```

### 3.3 Phase 3: Chinese Traditional (zh-rTW)

**File:** `app/src/main/res/values-zh-rTW/strings.xml`

**Step 3.1:** Auto Fetch Full Article strings
```xml
<!-- Around line 260-270, after existing sync settings -->
<string name="setting_auto_fetch_full_article">自動擷取完整文章</string>
<string name="setting_auto_fetch_full_article_description">同步時自動擷取完整文章</string>
```

**Step 3.2:** AI Provider strings
```xml
<string name="ai_provider">AI 提供者</string>
<string name="ai_provider_openai_compatible">OpenAI 相容</string>
<string name="ai_provider_anthropic">Anthropic</string>
```

**Step 3.3:** Summary Language strings
```xml
<string name="summary_language_title">摘要語言</string>
<string name="summary_language_description">選擇 AI 生成摘要的語言</string>
<string name="summary_language_auto_detect">自動檢測</string>
<string name="summary_language_english">英語</string>
<string name="summary_language_chinese">中文</string>
<string name="summary_language_spanish">西班牙語</string>
<string name="summary_language_french">法語</string>
<string name="summary_language_german">德語</string>
<string name="summary_language_japanese">日語</string>
<string name="summary_language_korean">韓語</string>
<string name="summary_language_portuguese">葡萄牙語</string>
<string name="summary_language_russian">俄語</string>
<string name="summary_language_arabic">阿拉伯語</string>
<string name="summary_language_hindi">印地語</string>
```

**Step 3.4:** Summary Settings strings
```xml
<string name="summary_title">摘要</string>
<string name="summary_subtitle">AI 驅動的文章摘要</string>
<string name="summary_settings_title">摘要設定</string>
<string name="summary_enabled_title">已啟用摘要</string>
<string name="summary_enabled_description">為文章生成 AI 摘要</string>
```

**Step 3.5:** Provider Management strings
```xml
<string name="provider_list_title">AI 提供者</string>
<string name="add_provider">新增提供者</string>
<string name="edit">編輯</string>
<string name="delete">刪除</string>
<string name="edit_provider">編輯提供者</string>
<string name="delete_provider">刪除提供者</string>
<string name="delete_provider_confirmation">確定要刪除此提供者嗎？</string>
<string name="no_providers_configured">未設定提供者</string>
<string name="provider_configured">已設定提供者</string>
<string name="add_provider_to_get_started">新增提供者以開始使用</string>
<string name="active_provider">活躍提供者</string>
<string name="provider_name">提供者名稱</string>
<string name="provider_name_hint">輸入提供者名稱</string>
<string name="provider_name_required">提供者名稱必填</string>
<string name="api_key_required">API 金鑰必填</string>
<string name="provider_saved">已儲存提供者</string>
<string name="save_provider">儲存提供者</string>
<string name="cancel">取消</string>
<string name="set_as_default_provider">設為預設</string>
```

### 3.4 Phase 4: Verification

**Step 4.1:** Verify XML syntax
```bash
# Check for well-formed XML
xmllint --noout app/src/main/res/values-zh-rCN/strings.xml
xmllint --noout app/src/main/res/values-zh-rTW/strings.xml
```

**Step 4.2:** Build project
```bash
./gradlew clean assembleDebug
```

**Step 4.3:** Run lint
```bash
./gradlew lint
```

**Step 4.4:** Verify string count
```bash
# Count strings in each file
grep -c '<string name=' app/src/main/res/values/strings.xml
grep -c '<string name=' app/src/main/res/values-zh-rCN/strings.xml
grep -c '<string name=' app/src/main/res/values-zh-rTW/strings.xml
```

### 3.5 Phase 5: Documentation

**Step 5.1:** Update spec summary
- Mark all tasks as completed
- Document any deviations from plan
- Note any issues encountered

**Step 5.2:** Create implementation summary
- Final string count verification
- Build status confirmation

## 4. Task Checklist

### 4.1 Preparation Tasks

- [ ] TASK-001: Review and extract the 42 new strings from default locale
- [ ] TASK-002: Read existing zh-rCN strings.xml
- [ ] TASK-003: Read existing zh-rTW strings.xml
- [ ] TASK-004: Identify insertion points in both files

### 4.2 Chinese Simplified (zh-rCN) Tasks

- [ ] TASK-005: Add Auto Fetch Full Article strings (2)
- [ ] TASK-006: Add AI Provider strings (3)
- [ ] TASK-007: Add Summary Language strings (14)
- [ ] TASK-008: Add Summary Settings strings (5)
- [ ] TASK-009: Add Provider Management strings (19)

### 4.3 Chinese Traditional (zh-rTW) Tasks

- [ ] TASK-010: Add Auto Fetch Full Article strings (2)
- [ ] TASK-011: Add AI Provider strings (3)
- [ ] TASK-012: Add Summary Language strings (14)
- [ ] TASK-013: Add Summary Settings strings (5)
- [ ] TASK-014: Add Provider Management strings (19)

### 4.4 Verification Tasks

- [ ] TASK-015: Verify XML syntax for zh-rCN
- [ ] TASK-016: Verify XML syntax for zh-rTW
- [ ] TASK-017: Run clean build
- [ ] TASK-018: Run lint check
- [ ] TASK-019: Verify string count

### 4.5 Documentation Tasks

- [ ] TASK-020: Update spec summary
- [ ] TASK-021: Create implementation complete document

**Total Tasks:** 21

## 5. Quality Gates

Each phase must pass quality gates before proceeding:

### 5.1 Phase 1 Quality Gate
- ✅ All 42 strings identified with exact keys
- ✅ Default locale file read and understood
- ✅ Target locale files read and insertion points identified

### 5.2 Phase 2 Quality Gate (zh-rCN)
- ✅ All 42 strings added
- ✅ XML is well-formed
- ✅ UTF-8 encoding verified
- ✅ Keys match default exactly

### 5.3 Phase 3 Quality Gate (zh-rTW)
- ✅ All 42 strings added
- ✅ XML is well-formed
- ✅ UTF-8 encoding verified
- ✅ Keys match default exactly

### 5.4 Phase 4 Quality Gate
- ✅ Build succeeds with no errors
- ✅ Lint passes with no translation warnings
- ✅ String counts correct (+42 in each locale)

### 5.5 Phase 5 Quality Gate
- ✅ All documentation updated
- ✅ Spec marked as complete

## 6. Risk Mitigation

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| XML syntax error | Low | Medium | Verify XML with xmllint before commit |
| Missing translation | Low | High | Use checklist for all 42 strings |
| Build failure | Very Low | High | Clean build before final commit |
| Wrong insertion point | Low | Low | Alphabetical ordering verification |

## 7. Rollback Plan

If issues arise after implementation:

1. **Revert locale file changes**
   ```bash
   git checkout app/src/main/res/values-zh-rCN/strings.xml
   git checkout app/src/main/res/values-zh-rTW/strings.xml
   ```

2. **Verify fallback to English**
   - App will automatically use default (English) strings

3. **Investigate issue**
   - Review XML syntax
   - Check for encoding issues
   - Verify key names

## 8. Success Criteria

Implementation is successful when:

1. ✅ All 42 strings added to zh-rCN
2. ✅ All 42 strings added to zh-rTW
3. ✅ `./gradlew assembleDebug` succeeds
4. ✅ `./gradlew lint` shows no translation warnings
5. ✅ XML files are well-formed
6. ✅ All documentation updated
7. ✅ Spec marked complete

## 9. Dependencies

### 9.1 Prerequisites
- None (standalone task)

### 9.2 Blocking
- None (can proceed independently)

### 9.3 Related
- Specs 001-009: AI integration features (these specs added the strings)
- Spec 006: Auto fetch full article setting

## 10. Tools and Commands

### 10.1 Development Tools
- **Editor:** Any text editor with UTF-8 support
- **IDE:** Android Studio (recommended for validation)

### 10.2 Build Commands
```bash
# Clean build
./gradlew clean

# Assemble debug
./gradlew assembleDebug

# Lint
./gradlew lint

# Install to device
./gradlew installDebug
```

### 10.3 Verification Commands
```bash
# Count strings
grep -c '<string name=' app/src/main/res/values-*/strings.xml

# Check XML syntax
xmllint --noout app/src/main/res/values-zh-rCN/strings.xml
xmllint --noout app/src/main/res/values-zh-rTW/strings.xml

# Check encoding
file -i app/src/main/res/values-zh-rCN/strings.xml
file -i app/src/main/res/values-zh-rTW/strings.xml
```

### 10.4 Git Commands
```bash
# Check status
git status

# Add files
git add app/src/main/res/values-zh-rCN/strings.xml
git add app/src/main/res/values-zh-rTW/strings.xml

# Commit
git commit -m "feat: add Chinese translations for AI integration and sync settings"

# Push (when ready)
git push origin spec-10-i10n-ai-integration-auto-full-fetch
```

## 11. Testing Plan

### 11.1 Automated Tests

**Build Test:**
```bash
./gradlew clean assembleDebug
# Expected: BUILD SUCCESSFUL
```

**Lint Test:**
```bash
./gradlew lint
# Expected: No "MissingTranslation" errors
```

### 11.2 Manual Tests (Optional)

**Device Setup:**
```bash
# Change device locale to Simplified Chinese
adb shell "am start -a android.settings.LOCALE_SETTINGS"
# Select "中文(简体)"

# Or use:
adb shell "su -c 'setprop persist.sys.locale zh-CN&&setprop ctl.restart zygote'"
```

**Verification Steps:**
1. Open Feeder app
2. Navigate to Settings → Sync
3. Verify "自动获取完整文章" displays
4. Navigate to Settings → AI integration
5. Verify "AI 提供者" and related strings display
6. Verify all summary language options show Chinese labels

**Repeat for Traditional Chinese:**
1. Change device locale to "中文(繁體)"
2. Repeat verification steps
3. Verify Traditional Chinese characters display

## 12. Post-Implementation

### 12.1 Immediate Actions

1. **Verify build** - Run full build and lint
2. **Update documentation** - Complete spec summary
3. **Commit changes** - Create commit with proper message

### 12.2 Follow-up Actions

1. **Consider additional locales** - de, es, fr, ja, ko if requested
2. **Create translation glossary** - For future consistency
3. **Automate translation checks** - Add CI validation for missing translations

## 13. Timeline

| Phase | Duration | Dependencies |
|-------|----------|--------------|
| Phase 1: Preparation | 15 minutes | None |
| Phase 2: zh-rCN | 45 minutes | Phase 1 |
| Phase 3: zh-rTW | 45 minutes | Phase 1 |
| Phase 4: Verification | 30 minutes | Phase 2, 3 |
| Phase 5: Documentation | 15 minutes | Phase 4 |
| **Total** | **2.5 hours** | - |

## 14. Approval Checklist

Before marking implementation complete:

- [ ] All 42 strings added to both locales
- [ ] XML files are valid and well-formed
- [ ] UTF-8 encoding confirmed
- [ ] Build succeeds with no errors
- [ ] Lint passes with no warnings
- [ ] Documentation updated
- [ ] Git commit created
- [ ] Ready for review

## 15. Appendix: Complete Translation Reference

### 15.1 Auto Fetch Full Article (2 strings)

| Key | zh-rCN | zh-rTW |
|-----|--------|--------|
| setting_auto_fetch_full_article | 自动获取完整文章 | 自動擷取完整文章 |
| setting_auto_fetch_full_article_description | 同步时自动获取完整文章 | 同步時自動擷取完整文章 |

### 15.2 AI Provider (3 strings)

| Key | zh-rCN | zh-rTW |
|-----|--------|--------|
| ai_provider | AI 提供者 | AI 提供者 |
| ai_provider_openai_compatible | OpenAI 兼容 | OpenAI 相容 |
| ai_provider_anthropic | Anthropic | Anthropic |

### 15.3 Summary Language (14 strings)

| Key | zh-rCN | zh-rTW |
|-----|--------|--------|
| summary_language_title | 摘要语言 | 摘要語言 |
| summary_language_description | 选择 AI 生成摘要的语言 | 選擇 AI 生成摘要的語言 |
| summary_language_auto_detect | 自动检测 | 自動檢測 |
| summary_language_english | 英语 | 英語 |
| summary_language_chinese | 中文 | 中文 |
| summary_language_spanish | 西班牙语 | 西班牙語 |
| summary_language_french | 法语 | 法語 |
| summary_language_german | 德语 | 德語 |
| summary_language_japanese | 日语 | 日語 |
| summary_language_korean | 韩语 | 韓語 |
| summary_language_portuguese | 葡萄牙语 | 葡萄牙語 |
| summary_language_russian | 俄语 | 俄語 |
| summary_language_arabic | 阿拉伯语 | 阿拉伯語 |
| summary_language_hindi | 印地语 | 印地語 |

### 15.4 Summary Settings (5 strings)

| Key | zh-rCN | zh-rTW |
|-----|--------|--------|
| summary_title | 摘要 | 摘要 |
| summary_subtitle | AI 驱动的文章摘要 | AI 驅動的文章摘要 |
| summary_settings_title | 摘要设置 | 摘要設定 |
| summary_enabled_title | 已启用摘要 | 已啟用摘要 |
| summary_enabled_description | 为文章生成 AI 摘要 | 為文章生成 AI 摘要 |

### 15.5 Provider Management (19 strings)

| Key | zh-rCN | zh-rTW |
|-----|--------|--------|
| provider_list_title | AI 提供者 | AI 提供者 |
| add_provider | 添加提供者 | 新增提供者 |
| edit | 编辑 | 編輯 |
| delete | 删除 | 刪除 |
| edit_provider | 编辑提供者 | 編輯提供者 |
| delete_provider | 删除提供者 | 刪除提供者 |
| delete_provider_confirmation | 确定要删除此提供者吗？ | 確定要刪除此提供者嗎？ |
| no_providers_configured | 未配置提供者 | 未設定提供者 |
| provider_configured | 已配置提供者 | 已設定提供者 |
| add_provider_to_get_started | 添加提供者以开始使用 | 新增提供者以開始使用 |
| active_provider | 活跃提供者 | 活躍提供者 |
| provider_name | 提供者名称 | 提供者名稱 |
| provider_name_hint | 输入提供者名称 | 輸入提供者名稱 |
| provider_name_required | 提供者名称必填 | 提供者名稱必填 |
| api_key_required | API 密钥必填 | API 金鑰必填 |
| provider_saved | 已保存提供者 | 已儲存提供者 |
| save_provider | 保存提供者 | 儲存提供者 |
| cancel | 取消 | 取消 |
| set_as_default_provider | 设为默认 | 設為預設 |

**Total: 43 strings (actually 43, not 42 as previously counted)**
