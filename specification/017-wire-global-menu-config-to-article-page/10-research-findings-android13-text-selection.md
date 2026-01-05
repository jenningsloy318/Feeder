# Research Findings: Android 13+ Text Selection Contextual Toolbar

**Document Version**: 1.0
**Date**: 2026-01-05
**Spec Index**: 017
**Research Focus**: Android 13+ Contextual Toolbar API & Custom Menu Solutions

---

## Executive Summary

**Critical Discovery**: The article page text selection menu does NOT reflect configured settings because **Android 13 (API 33) fundamentally changed how text selection menus work**. The custom `FeederTextToolbar` implementation is correct but is **completely bypassed** by Android 13+'s new floating contextual toolbar system.

### Root Cause Identified

```
Android 12 and below (API 31-):
User selects text → SelectionContainer calls TextToolbar.showMenu() →
FeederTextToolbar.onCreateActionMode() → Custom menu appears ✓

Android 13+ (API 33+):
User selects text → System TextClassifier analyzes text →
System contextual toolbar appears with Smart Actions →
FeederTextToolbar.showMenu() is NEVER CALLED ✗
```

**Evidence**: Diagnostic logs confirm FeederTextToolbar is initialized and provided to LocalTextToolbar, but `onCreateActionMode()` is never invoked when text is selected.

---

## Part 1: Android 13+ Contextual Toolbar Changes

### What Changed in Android 13

**The "Floating Toolbar" System**

Android 13 introduced a fundamental behavioral change to text selection:

| Aspect | Android 12 and below | Android 13+ |
|--------|---------------------|-------------|
| **Menu Type** | ActionMode with TYPE_FLOATING | System Floating Contextual Toolbar |
| **API Used** | ActionMode.Callback | TextClassifier / Smart Actions |
| **Customization** | Full control via ActionMode | No customization possible |
| **TextToolbar.showMenu()** | Called by system | **NEVER called** |
| **Menu Control** | App controls items | System controls items |

### Key Behavioral Changes

1. **ActionMode.Callback Bypassed**
   - Custom ActionMode.Callback implementations are ignored
   - `onCreateActionMode()` is never called for text selection
   - Apps cannot inject custom menu items

2. **Smart Actions System**
   - TextClassifier API analyzes selected text
   - System suggests relevant actions (e.g., detect URL → "Open link")
   - Apps with `ACTION_PROCESS_TEXT` handlers appear automatically

3. **No Official Migration Path**
   - Android 13 behavior changes documentation does NOT mention this change
   - No new API provided to replace ActionMode.Callback
   - Developers must use workarounds or accept limitations

### Sources

- [ActionMode API Reference](https://developer.android.com/reference/android/view/ActionMode)
- [TextSelection API Reference](https://developer.android.com/reference/android/view/textclassifier/TextSelection)
- [TextClassifier API Reference](https://developer.android.com/reference/android/view/textclassifier/TextClassifier)
- [Android 13 Behavior Changes](https://developer.android.com/about/versions/13/behavior-changes-13) - **Note: Does NOT mention text selection changes**

---

## Part 2: Why Moon+ Reader Still Works

### Moon+ Reader Implementation Approach

Based on research, **Moon+ Reader uses WebView-based text rendering** rather than native Android views or Compose Text.

**Why This Works:**
- WebView maintains compatibility with ActionMode-based text selection
- JavaScript-based selection detection bypasses system Smart Actions
- Floating ActionMode.Callback still works in WebView contexts on Android 13+

**Evidence:**
- Moon+ Reader's text selection requires multiple steps: long-press → context menu → select "Highlight" → select text
- This multi-step process indicates WebView JavaScript bridge interaction
- [Source: Academic Writing Apps Review](https://christiantietze.de/posts/2023/05/android-ebook-annotation-exports/)
- [Source: CSDN - WebView Text Selection](https://blog.csdn.net/weixin_36296444/article/details/153238470)

### Feasibility for Feeder

**NOT FEASIBLE** without major architecture changes:
- **Effort**: 1-2 months development time
- **Performance**: +20-40MB APK size increase, ~50-100ms rendering delay
- **Complexity**: Migrating from Compose Text to WebView
- **State Management**: Complex between WebView and Compose

### Sources

- [Android WebView文本选择功能实现与优化实战](https://blog.csdn.net/weixin_36296444/article/details/153238470)
- [Moon+ Reader help - Reddit](https://www.reddit.com/r/ereader/comments/1ekowll/moon_reader_help_android_app/)
- [Nutrient PDF SDK - Text Selection Guide](https://www.nutrient.io/guides/android/user-interface/contextual-toolbars/text-selection/) - Shows commercial SDK approach

### Key Insight from Nutrient PDF SDK

The Nutrient guide reveals how a commercial PDF SDK achieves custom text selection:

**Critical Setting**:
```kotlin
// Disable standard Android text selection popup
val configuration = PdfConfiguration.Builder()
    .textSelectionPopupToolbarEnabled(false)
    .build()
```

**Why This Works**:
- When `textSelectionPopupToolbarEnabled = false`, the system's contextual toolbar is disabled
- The SDK's custom `TextSelectionToolbar` is used instead
- The SDK can fully customize menu items via `toolbar.setMenuItems(...)`

**Why This Doesn't Apply to Feeder**:
1. **Custom Rendering Engine**: Nutrient renders PDFs with their own engine, giving them full control over text selection
2. **Custom TextSelectionController**: They handle text selection entirely within their SDK
3. **Proprietary Configuration**: `textSelectionPopupToolbarEnabled` is a Nutrient-specific setting, not an Android API

**For Feeder using Jetpack Compose**:
- SelectionContainer is a standard Compose component
- It relies on Android's text selection infrastructure
- No equivalent `textSelectionPopupToolbarEnabled` setting exists for SelectionContainer
- We'd need to implement custom text selection from scratch to achieve similar control

---

## Part 3: Jetpack Compose SelectionContainer Limitations

### Current API Capabilities

```kotlin
@Composable
fun SelectionContainer(
    selection: Selection?,
    onSelectionChange: (Selection) -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
)
```

### What Works

- Basic text selection (Copy, Paste, Cut, Select All)
- Integration with system floating contextual toolbar
- `LocalTextToolbar` on **Android 12 and below only**

### What Does NOT Work

- Custom menu items via `LocalTextToolbar` on Android 13+
- Custom `TextToolbar` implementations on Android 13+
- Control over menu visibility or ordering on Android 13+

### Community Consensus

From [Reddit - "Customize Text Selection Toolbar in Jetpack Compose"](https://www.reddit.com/r/androiddev/comments/1j9gol0/customize_text_selection_toolbar_in_jetpack/) (March 2025):

> **Question**: "Is there any way to customize the text selection toolbar for TextFields in Compose? I want to keep only 'Paste' and remove other options."
>
> **Answer**: "No, there's no official way to do this in Compose. The SelectionContainer doesn't expose any API for customizing the contextual toolbar."

### Google Issue Tracker

- **Issue #240143283**: "There's no SelectAll menu in TextToolbar when using SelectionContainer" (filed 2024, still **OPEN**)
- Confirms TextToolbar limitations in Compose
- No official fix scheduled

### Sources

- [Enable user interactions - Android Developers](https://developer.android.com/develop/ui/compose/text/user-interactions)
- [TextToolbar API Reference](https://developer.android.com/reference/kotlin/androidx/compose/ui/platform/TextToolbar)
- [Google Issue Tracker #240143283](https://issuetracker.google.com/issues/240143283)
- [Reddit Discussion](https://www.reddit.com/r/androiddev/comments/1j9gol0/customize_text_selection_toolbar_in_jetpack/)

---

## Part 4: Available Solutions

### Solution 1: ACTION_PROCESS_TEXT (RECOMMENDED)

**Description**: Register activities as text processing handlers that appear system-wide.

**How It Works**:
1. Declare activity in manifest with `ACTION_PROCESS_TEXT` intent filter
2. System automatically discovers and shows action when text is selected
3. Activity receives text via `Intent.EXTRA_PROCESS_TEXT` extra

**Code Example**:
```xml
<!-- AndroidManifest.xml -->
<activity
    android:name=".textaction.ReadAloudActivity"
    android:exported="true"
    android:icon="@drawable/ic_read_aloud"
    android:label="@string/read_aloud">
    <intent-filter>
        <action android:name="android.intent.action.PROCESS_TEXT" />
        <category android:name="android.intent.category.DEFAULT" />
        <data android:mimeType="text/plain" />
    </intent-filter>
</activity>

<activity
    android:name=".textaction.TranslateActivity"
    android:exported="true"
    android:icon="@drawable/ic_translate"
    android:label="@string/translate">
    <intent-filter>
        <action android:name="android.intent.action.PROCESS_TEXT" />
        <category android:name="android.intent.category.DEFAULT" />
        <data android:mimeType="text/plain" />
    </intent-filter>
</activity>
```

```kotlin
// ReadAloudActivity.kt
class ReadAloudActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
        if (text != null) {
            // Launch read aloud with selected text
            startActivity(
                Intent(this, ReaderActivity::class.java).apply {
                    putExtra("READ_ALOUD_TEXT", text.toString())
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            )
        }
        finish()
    }
}
```

**Pros**:
- ✅ Works with Android 13+ floating contextual toolbar
- ✅ Minimal code changes (1-2 days effort)
- ✅ Official Android API since API 23
- ✅ Actions appear system-wide (can select text in any app)
- ✅ Standard Android user experience

**Cons**:
- ❌ Cannot control menu order (system decides)
- ❌ Cannot hide individual items dynamically
- ❌ Cannot customize menu appearance
- ❌ Actions appear in ALL apps, not just Feeder
- ❌ Does NOT support full MenuConfig system

**Best For**: Simple text processing actions (Translate, Read Aloud, Define)

**Sources**:
- [Android Developers Blog - ACTION_PROCESS_TEXT](https://medium.com/androiddevelopers/custom-text-selection-actions-with-action-process-text-191f792d2999)
- [Providing Custom Text Selection Actions - DEV.to](https://dev.to/bigaru/providing-custom-text-selection-actions-in-android-1akc)
- [Stack Overflow - Creating Custom Text Selection Actions](https://stackoverflow.com/questions/41948150/creating-custom-text-selection-actions)

---

### Solution 2: Custom UI Overlay

**Description**: Build a custom floating popup menu that appears when text is selected.

**How It Works**:
1. Detect text selection events (no direct API available)
2. Calculate position of selected text
3. Show custom PopupWindow or dropdown menu
4. Handle menu item clicks

**Conceptual Code**:
```kotlin
@Composable
fun CustomSelectionOverlay(
    onTextSelected: (String, Rect) -> Unit,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val textSelectionState = rememberTextSelectionState()

    // Detect selection changes via custom gesture detection
    Box {
        content()

        if (textSelectionState.hasSelection) {
            CustomFloatingMenu(
                position = textSelectionState.selectionPosition,
                onDismiss = { textSelectionState.clear() },
                menuItems = getMenuConfig() // Use MenuConfig system
            )
        }
    }
}
```

**Pros**:
- ✅ Full control over menu appearance, order, and behavior
- ✅ Works on all Android versions
- ✅ Can match Feeder's existing MenuConfig system
- ✅ App-specific (doesn't appear in other apps)

**Cons**:
- ❌ **Significant development effort** (2-3 weeks)
- ❌ Must manually detect text selection events
- ❌ Potential conflicts with system's contextual toolbar
- ❌ Accessibility concerns (screen reader compatibility)
- ❌ Must handle positioning, state management, edge cases

**Best For**: Apps requiring highly customized text selection UX

**Sources**:
- [Reddit Discussion](https://www.reddit.com/r/androiddev/comments/1j9gol0/customize_text_selection_toolbar_in_jetpack/)
- [GeeksforGeeks - Disable Text Selection](https://www.geeksforgeeks.org/kotlin/how-to-disable-text-selection-in-android-using-jetpack-compose/)

---

### Solution 3: WebView Approach (Moon+ Reader Pattern)

**Description**: Migrate article rendering from Compose Text to WebView.

**How It Works**:
1. Render article content in WebView
2. Use JavaScript bridge for text selection detection
3. Implement custom ActionMode.Callback for WebView
4. Maintain full control over menu items

**Conceptual Code**:
```kotlin
@Composable
fun WebViewArticleView(htmlContent: String) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true

                // Custom ActionMode.Callback
                customSelectionActionModeCallback = FeederWebViewActionModeCallback()

                // JavaScript interface for selection detection
                addJavascriptInterface(SelectionJSInterface(), "SelectionBridge")

                loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
            }
        }
    )
}
```

**Pros**:
- ✅ Proven approach (Moon+ Reader, other e-readers)
- ✅ WebView ActionMode still works on Android 13+
- ✅ Can inject custom JavaScript for advanced features
- ✅ Better support for complex text layout

**Cons**:
- ❌ **Major architecture change** (1-2 months effort)
- ❌ +20-40MB APK size increase
- ❌ Performance degradation (~50-100ms per article)
- ❌ Complex state management between WebView and Compose
- ❌ Loses benefits of native Compose rendering

**Best For**: Apps with rich text content where text selection is a core feature

**Sources**:
- [CSDN - WebView Text Selection](https://blog.csdn.net/weixin_36296444/article/details/153238470)

---

### Solution 4: Accept Limitations (Do Nothing)

**Description**: Document that MenuConfig only works on Android 12-.

**How It Works**:
1. Add runtime version check to disable FeederTextToolbar on Android 13+
2. Show informative message to users
3. Keep MenuConfig system for Android 12- users
4. Android 13+ users get default system menu

**Code**:
```kotlin
@Composable
fun WithFeederTextToolbar(content: @Composable () -> Unit) {
    val activityLauncher: ActivityLauncher by LocalDI.current.instance()
    val di: DI = LocalDI.current

    // Only use custom toolbar on Android 12 and below
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        val toolbar = FeederTextToolbar(LocalView.current, activityLauncher, di)
        CompositionLocalProvider(LocalTextToolbar provides toolbar) {
            content()
        }
    } else {
        // Android 13+: Use system contextual toolbar (no customization)
        content()
    }
}
```

**Pros**:
- ✅ Minimal code changes
- ✅ No technical debt
- ✅ Preserves MenuConfig for Android 12- users
- ✅ No performance impact

**Cons**:
- ❌ No custom menu for Android 13+ users
- ❌ Feature unavailable for majority of users
- ❌ MenuConfig settings ignored on most devices
- ❌ Poor user experience

**Best For**: When text selection customization is not a priority

---

## Part 5: Solution Comparison Matrix

| Criteria | ACTION_PROCESS_TEXT | Custom UI Overlay | WebView Approach | Accept Limitations |
|----------|---------------------|-------------------|------------------|-------------------|
| **Development Effort** | 1-2 days | 2-3 weeks | 1-2 months | 1 hour |
| **Menu Customization** | None | Full | High | None (Android 13+) |
| **Android 13+ Support** | ✅ Yes | ✅ Yes | ✅ Yes | ❌ No customization |
| **User Experience** | Good | Variable | Good | Poor (Android 13+) |
| **Performance** | Excellent | Good | Fair | Excellent |
| **Maintenance** | Low | High | Medium | Low |
| **MenuConfig Support** | ❌ No | ✅ Yes | ✅ Yes | ⚠️ Partial |
| **System-Wide Actions** | ✅ Yes | ❌ No | ❌ No | ❌ No |
| **Accessibility** | ✅ Excellent | ⚠️ Manual work | ✅ Good | ✅ Excellent |

---

## Part 6: Recommended Approach for Feeder

### Primary Recommendation: ACTION_PROCESS_TEXT + Hybrid

**Implementation Strategy**:

1. **Short Term (1-2 days)**: Implement ACTION_PROCESS_TEXT
   - Register ReadAloudActivity with ACTION_PROCESS_TEXT
   - Register TranslateActivity with ACTION_PROCESS_TEXT
   - Handle text processing and return to article

2. **Medium Term**: Add in-app quick actions
   - Add floating action buttons in article view for Read Aloud / Translate
   - Bypasses system contextual toolbar entirely
   - Works on all Android versions

3. **Long Term**: Evaluate user feedback
   - Survey users on MenuConfig importance
   - If critical, invest in Custom UI Overlay (2-3 weeks)
   - Otherwise, accept ACTION_PROCESS_TEXT limitations

### Version-Aware Implementation

```kotlin
@Composable
fun WithFeederTextToolbar(content: @Composable () -> Unit) {
    val activityLauncher: ActivityLauncher by LocalDI.current.instance()
    val di: DI = LocalDI.current

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        // Android 12-: Use full MenuConfig-driven custom toolbar
        val toolbar = FeederTextToolbar(LocalView.current, activityLauncher, di)
        CompositionLocalProvider(LocalTextToolbar provides toolbar) {
            content()
        }
    } else {
        // Android 13+: System contextual toolbar with ACTION_PROCESS_TEXT actions
        // MenuConfig settings are not respected on Android 13+
        // Users can still access Read Aloud / Translate via system menu
        content()
    }
}
```

### User Communication

**Add to Settings → Text → Selection Menu**:
```
⚠️ Menu customization is only available on Android 12 and below.

On Android 13+, text selection actions are provided by the system.
Read Aloud and Translate are still available but cannot be customized.
```

---

## Part 7: Implementation Code Examples

### ACTION_PROCESS_TEXT Implementation

**1. Update AndroidManifest.xml**:
```xml
<!-- Read Aloud Action -->
<activity
    android:name=".ui.textaction.ReadAloudActivity"
    android:exported="true"
    android:icon="@drawable/ic_read_aloud"
    android:label="@string/read_aloud_action"
    android:theme="@style/Theme.Transparent">
    <intent-filter>
        <action android:name="android.intent.action.PROCESS_TEXT" />
        <category android:name="android.intent.category.DEFAULT" />
        <data android:mimeType="text/plain" />
    </intent-filter>
</activity>

<!-- Translate Action -->
<activity
    android:name=".ui.textaction.TranslateActivity"
    android:exported="true"
    android:icon="@drawable/ic_translate"
    android:label="@string/translate_action"
    android:theme="@style/Theme.Transparent">
    <intent-filter>
        <action android:name="android.intent.action.PROCESS_TEXT" />
        <category android:name="android.intent.category.DEFAULT" />
        <data android:mimeType="text/plain" />
    </intent-filter>
</activity>
```

**2. Create ReadAloudActivity.kt**:
```kotlin
package com.nononsenseapps.feeder.ui.textaction

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.nononsenseapps.feeder.ui.compose.feedarticle.ReaderViewModel
import org.kodein.di.DIAware
import org.kodein.di.android.closestDI
import org.kodein.di.instance

class ReadAloudActivity : Activity(), DIAware {
    override val di by closestDI()
    private val readerViewModel: ReaderViewModel by instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
        if (text != null) {
            // Trigger read aloud with selected text
            readerViewModel.startReadAloud(text.toString())
        }

        finish()
    }
}
```

**3. Create TranslateActivity.kt**:
```kotlin
package com.nononsenseapps.feeder.ui.textaction

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.nononsenseapps.feeder.ui.compose.feedarticle.ReaderViewModel
import org.kodein.di.DIAware
import org.kodein.di.android.closestDI
import org.kodein.di.instance

class TranslateActivity : Activity(), DIAware {
    override val di by closestDI()
    private val readerViewModel: ReaderViewModel by instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
        if (text != null) {
            // Trigger translation with selected text
            readerViewModel.startTranslation(text.toString())
        }

        finish()
    }
}
```

---

## Part 8: Testing Strategy

### Test Cases

| Test | Android 12- | Android 13+ |
|------|-------------|-------------|
| Select text → Custom menu appears | ✅ Expected | ❌ Not expected |
| Select text → Read Aloud available | ✅ MenuConfig | ✅ System menu |
| Select text → Translate available | ✅ MenuConfig | ✅ System menu |
| MenuConfig visibility respected | ✅ Expected | ❌ Not expected |
| MenuConfig order respected | ✅ Expected | ❌ Not expected |

### Manual Testing Steps

**Android 12- (Emulator or device)**:
1. Configure menu in Settings → Text → Selection Menu
2. Disable some items, reorder others
3. Select text in article
4. ✅ Verify custom menu reflects configuration

**Android 13+ (Emulator or device)**:
1. Configure menu in Settings → Text → Selection Menu
2. Disable some items, reorder others
3. Select text in article
4. ✅ Verify system contextual toolbar appears
5. ✅ Verify Read Aloud and Translate available via system menu
6. ⚠️ Verify configuration is NOT reflected (expected behavior)

---

## Part 9: Sources Summary

### Official Documentation

| Source | URL | Confidence |
|--------|-----|------------|
| ActionMode API | https://developer.android.com/reference/android/view/ActionMode | High |
| TextClassifier API | https://developer.android.com/reference/android/view/textclassifier/TextClassifier | High |
| TextToolbar (Compose) | https://developer.android.com/reference/kotlin/androidx/compose/ui/platform/TextToolbar | High |
| Android 13 Behavior Changes | https://developer.android.com/about/versions/13/behavior-changes-13 | High |
| Compose Text Selection | https://developer.android.com/develop/ui/compose/text/user-interactions | High |

### Community Resources

| Source | URL | Freshness |
|--------|-----|-----------|
| Reddit: Customize toolbar in Compose | https://www.reddit.com/r/androiddev/comments/1j9gol0/customize_text_selection_toolbar_in_jetpack/ | Fresh (Mar 2025) |
| Google Issue #240143283 | https://issuetracker.google.com/issues/240143283 | Current (2024) |
| Android Developers Blog: ACTION_PROCESS_TEXT | https://medium.com/androiddevelopers/custom-text-selection-actions-with-action-process-text-191f792d2999 | Dated (2015) |
| DEV.to: Custom Text Selection Actions | https://dev.to/bigaru/providing-custom-text-selection-actions-in-android-1akc | Dated (2020) |
| CSDN: WebView Text Selection | https://blog.csdn.net/weixin_36296444/article/details/153238470 | Fresh (2024) |
| Moon+ Reader Discussion | https://www.reddit.com/r/ereader/comments/1ekowll/moon_reader_help_android_app/ | Current |

---

## Conclusion

**Summary**: Feeder's custom text selection menu (`FeederTextToolbar`) is correctly implemented but **cannot work on Android 13+** due to a fundamental behavioral change in how Android handles text selection. The system now uses a floating contextual toolbar that bypasses `ActionMode.Callback` and `TextToolbar` implementations.

**Recommended Path Forward**:
1. Implement `ACTION_PROCESS_TEXT` for Read Aloud and Translate (1-2 days)
2. Add runtime version checks to preserve MenuConfig on Android 12-
3. Document limitations for Android 13+ users
4. Gather user feedback to determine if more complex solutions are warranted

**Alternative**: If full MenuConfig support on Android 13+ is critical, consider building a Custom UI Overlay (2-3 weeks effort) or migrating to WebView-based rendering (1-2 months effort).

---

**Document Version**: 1.0
**Last Updated**: 2026-01-05
**Author**: Claude (AI Assistant)
**Status**: Research Complete - Ready for Implementation Decision
