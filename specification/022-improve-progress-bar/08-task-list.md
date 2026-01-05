# Task List: Improve Progress Bar for Summary and Translation

## Tasks

### Task 1: Add String Resources
- [ ] Add `summarizing_progress` to `values/strings.xml`
- [ ] Add `translating_progress` to `values/strings.xml`
- [ ] Verify XML syntax is correct

**File:** `app/src/main/res/values/strings.xml`

### Task 2: Update SummarySection Composable
- [ ] Add required imports (Column, Alignment, Spacer, height)
- [ ] Replace `LinearProgressIndicator` with `Column` layout
- [ ] Add `Text` composable with "Summarizing..."
- [ ] Add 8dp `Spacer` between text and progress
- [ ] Move `LinearProgressIndicator` into `Column`
- [ ] Test that summary loading shows text

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt`

### Task 3: Update TranslationStatusSection Composable
- [ ] Replace loading state `OutlinedCard` content with `Column` layout
- [ ] Add `Text` composable with "Translating..."
- [ ] Add 8dp `Spacer` between text and progress
- [ ] Keep `LinearProgressIndicator` in new layout
- [ ] Test that translation loading shows text

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt`

### Task 4: Manual Testing
- [ ] Build and run app
- [ ] Open article and tap summarize button
- [ ] Verify "Summarizing..." appears
- [ ] Wait for summary to complete
- [ ] Tap translate button
- [ ] Verify "Translating..." appears
- [ ] Wait for translation to complete
- [ ] Check error states still work

### Task 5: Code Review Checklist
- [ ] No compilation errors
- [ ] No warnings
- [ ] Code follows project style
- [ ] String resources properly defined
- [ ] Imports are organized
- [ ] No unused imports
- [ ] Layout looks correct
- [ ] Text is centered
- [ ] Spacing is consistent

## Completion Criteria
All tasks completed and verified through manual testing.
