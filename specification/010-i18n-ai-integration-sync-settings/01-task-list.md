# Task List - I18n for AI Integration & Sync Settings

**Spec Index:** 010
**Spec Name:** i18n-ai-integration-sync-settings
**Date:** 2026-01-03

## Overview

This document outlines the specific tasks required to implement internationalization for the AI integration and sync settings features.

## Tasks

### Phase 1: Preparation

- [ ] **TASK-001**: Review existing default (English) strings to identify exact strings requiring translation
  - File: `app/src/main/res/values/strings.xml`
  - Lines: 271-346 (AI integration and auto fetch settings)
  - Deliverable: Confirm list of 42 strings

### Phase 2: Translation - Chinese Simplified (zh-rCN)

- [ ] **TASK-002**: Add Auto Fetch Full Article strings to `values-zh-rCN/strings.xml`
  - `setting_auto_fetch_full_article`
  - `setting_auto_fetch_full_article_description`

- [ ] **TASK-003**: Add AI Provider strings to `values-zh-rCN/strings.xml`
  - `ai_provider`, `ai_provider_openai_compatible`, `ai_provider_anthropic`

- [ ] **TASK-004**: Add Summary Language strings to `values-zh-rCN/strings.xml`
  - `summary_language_title`, `summary_language_description`
  - `summary_language_*` (14 language options)

- [ ] **TASK-005**: Add Summary Settings strings to `values-zh-rCN/strings.xml`
  - `summary_title`, `summary_subtitle`, `summary_settings_title`
  - `summary_enabled_title`, `summary_enabled_description`

- [ ] **TASK-006**: Add Provider Management strings to `values-zh-rCN/strings.xml`
  - `provider_list_title`, `add_provider`, `edit`, `delete`
  - `edit_provider`, `delete_provider`, `delete_provider_confirmation`
  - `no_providers_configured`, `provider_configured`, `add_provider_to_get_started`
  - `active_provider`, `provider_name`, `provider_name_hint`
  - `provider_name_required`, `api_key_required`, `provider_saved`
  - `save_provider`, `cancel`, `set_as_default_provider`

### Phase 3: Translation - Chinese Traditional (zh-rTW)

- [ ] **TASK-007**: Add Auto Fetch Full Article strings to `values-zh-rTW/strings.xml`
  - `setting_auto_fetch_full_article`
  - `setting_auto_fetch_full_article_description`

- [ ] **TASK-008**: Add AI Provider strings to `values-zh-rTW/strings.xml`
  - `ai_provider`, `ai_provider_openai_compatible`, `ai_provider_anthropic`

- [ ] **TASK-009**: Add Summary Language strings to `values-zh-rTW/strings.xml`
  - `summary_language_title`, `summary_language_description`
  - `summary_language_*` (14 language options)

- [ ] **TASK-010**: Add Summary Settings strings to `values-zh-rTW/strings.xml`
  - `summary_title`, `summary_subtitle`, `summary_settings_title`
  - `summary_enabled_title`, `summary_enabled_description`

- [ ] **TASK-011**: Add Provider Management strings to `values-zh-rTW/strings.xml`
  - `provider_list_title`, `add_provider`, `edit`, `delete`
  - `edit_provider`, `delete_provider`, `delete_provider_confirmation`
  - `no_providers_configured`, `provider_configured`, `add_provider_to_get_started`
  - `active_provider`, `provider_name`, `provider_name_hint`
  - `provider_name_required`, `api_key_required`, `provider_saved`
  - `save_provider`, `cancel`, `set_as_default_provider`

### Phase 4: Verification

- [ ] **TASK-012**: Verify project builds successfully
  - Run: `./gradlew assembleDebug`
  - Expected: Build completes with no errors

- [ ] **TASK-013**: Run lint check
  - Run: `./gradlew lint`
  - Expected: No lint warnings related to string resources

- [ ] **TASK-014**: Verify XML syntax
  - All XML files are well-formed
  - UTF-8 encoding is correct
  - No missing closing tags

- [ ] **TASK-015**: Verify parameter placeholders
  - All `%1$s`, `%1$d` placeholders are preserved
  - Parameter order is maintained

### Phase 5: Documentation

- [ ] **TASK-016**: Update spec summary document

## Task Summary

| Phase | Tasks | Status |
|-------|-------|--------|
| 1: Preparation | 1 | Pending |
| 2: zh-rCN Translation | 5 | Pending |
| 3: zh-rTW Translation | 5 | Pending |
| 4: Verification | 4 | Pending |
| 5: Documentation | 1 | Pending |
| **Total** | **16** | **0%** |

## Translation Reference

### Key Terminology

| English | Simplified Chinese (zh-rCN) | Traditional Chinese (zh-rTW) |
|---------|----------------------------|------------------------------|
| AI | AI | AI |
| Provider | 提供者 | 提供者 |
| Settings | 设置 | 設定 |
| Configuration | 配置 | 設定/配置 |
| Summary | 摘要 | 摘要 |
| Language | 语言 | 語言 |
| Enabled | 已启用 | 已啟用 |
| Auto | 自动 | 自動 |
| Fetch | 获取 | 擷取 |
| Full Article | 完整文章 | 完整文章 |
| Delete | 删除 | 刪除 |
| Edit | 编辑 | 編輯 |
| Save | 保存 | 儲存 |
| Cancel | 取消 | 取消 |
| Confirm | 确认 | 確認 |
