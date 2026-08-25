## [Unreleased]

### 🚀 Features
- **NEW: Async translation with progress tracking** - Added support for translating long-form content (30,000+ words) that previously timed out
  - Parallel chunk processing with configurable concurrency (3 concurrent chunks by default)
  - Real-time progress reporting via Flow<TranslationProgress>
  - Automatic content size detection - short content uses fast path, long content chunks automatically
  - Intelligent retry logic with exponential backoff for network errors
  - Per-chunk error tracking and partial success handling
  - Cancellation support via coroutine scope
  - Fixed critical bug in TranslationChunker where startIndex calculation used cleared chunk size
  - Added division-by-zero protection in TranslationProgress progress calculation
- Added import functionality for saved articles to complement existing export feature
- Added dedicated Summary settings screen with enable/disable toggle for AI summaries
- Improved AI Integration settings UX with separate configuration screen for summary language and enabled state
- Added OPML import/export support for AI summary settings

### 🐛 Bug Fixes & Minor Changes
- Refactored AI summary API to respect user's enabled state before generating summaries
- Updated AI Provider settings section to navigate to dedicated Summary screen
- Fixed TranslationChunker chunk index calculation bug that caused incorrect text slicing
- Added safety check to prevent division by zero in TranslationProgress.getProgressPercentage()
## [2.22.0] - 2026-08-04

### 🚀 Features
- Added button for renaming tags (#1173) by @EmaYYellow in [#1173](https://github.com/spacecowboy/feeder/pull/1173) 
- Added audio player for media attachments (#1172) by @deprov447 in [#1172](https://github.com/spacecowboy/feeder/pull/1172) 

### 🐛 Bug Fixes & Minor Changes
- Format dates with device preferred 12/24-hour clock (#1183) by @arpitagarwal1301 in [#1183](https://github.com/spacecowboy/feeder/pull/1183) 
- Honored custom tab preference in notifications (#1182) by @arpitagarwal1301 in [#1182](https://github.com/spacecowboy/feeder/pull/1182) 
- Fixed crash introduced by audio player feature by @spacecowboy in [commit](https://github.com/spacecowboy/feeder/commit/1d885a6080999e9dc420fea90051b81bb2fb1afa)

### 🌐 Translations
- Updated translations from Hosted Weblate (#1174) by @weblate in [#1174](https://github.com/spacecowboy/feeder/pull/1174) 
- Updated German translation using Weblate in [commit](https://github.com/spacecowboy/feeder/commit/79d075a006f35b48d6f7f72507f51ccae65a376e)
- Updated Serbian translation using Weblate by @eevan78 in [commit](https://github.com/spacecowboy/feeder/commit/07424402af050ee6cfcb8b61b1e2e41380dbac3a)
- Updated Ukrainian translation using Weblate by @andm18 in [#1186](https://github.com/spacecowboy/feeder/pull/1186) 

### ❤️  New Contributors
* @deprov447 made their first contribution in [#1172](https://github.com/spacecowboy/feeder/pull/1172)
* @EmaYYellow made their first contribution in [#1173](https://github.com/spacecowboy/feeder/pull/1173)
* @arpitagarwal1301 made their first contribution in [#1182](https://github.com/spacecowboy/feeder/pull/1182)

## [2.21.2] - 2026-07-03

### 🐛 Bug Fixes & Minor Changes
- Exposed missing strings to translations (#1168) by @himu-gupta in [#1168](https://github.com/spacecowboy/feeder/pull/1168) 

### 🌐 Translations
- Updated French translation using Weblate by @Matth7878 in [commit](https://github.com/spacecowboy/feeder/commit/86887347c2d9905b0cadd85603eb2f01a78dc259)
- Updated Latvian translation using Weblate by @Coool in [commit](https://github.com/spacecowboy/feeder/commit/97d2f2eacce26949586cbce3898ea4f9924da220)
- Updated Portuguese (Portugal) translation using Weblate by @AntonioOliveira2 in [commit](https://github.com/spacecowboy/feeder/commit/ad417e2830d3132526a3016a869c57af19a63b98)
- Updated Estonian translation using Weblate in [commit](https://github.com/spacecowboy/feeder/commit/1a8bb533201fce31c20c03978195fde5eb756e3b)
- Updated Czech translation using Weblate in [commit](https://github.com/spacecowboy/feeder/commit/191acff4fae091b3ff109b71bc609323e295590e)
- Updated Hungarian translation using Weblate by @summoner001 in [commit](https://github.com/spacecowboy/feeder/commit/fc601a155adbcdd977a22ce6fb5d2fb20d884a67)
- Updated Polish translation using Weblate by @Aga-C in [commit](https://github.com/spacecowboy/feeder/commit/ba3158bc8a736613de8e8f58cf9170c7106f9c2b)
- Updated Chinese (Simplified Han script) translation using Weblate in [commit](https://github.com/spacecowboy/feeder/commit/de16d9f98a4bd020ce27fdf84c7bb5408e6aab97)
- Updated German translation using Weblate in [commit](https://github.com/spacecowboy/feeder/commit/56492b82462f2c798a830c1a29a7a2e5e744b62b)
- Updated Bosnian translation using Weblate in [commit](https://github.com/spacecowboy/feeder/commit/17aecfaf5be2c0b3440f31dadacc30b2245a05a4)
- Updated Italian translation using Weblate by @Wiccio in [commit](https://github.com/spacecowboy/feeder/commit/8e3772eb0e7f70007f055e391cee4791efce1db9)
- Updated Dutch translation using Weblate in [commit](https://github.com/spacecowboy/feeder/commit/628ea46179327b0e2d6c06e1eb95e31ed4c60c65)
- Updated Serbian translation using Weblate by @eevan78 in [#1167](https://github.com/spacecowboy/feeder/pull/1167) 

### ❤️  New Contributors
* @AntonioOliveira2 made their first contribution
* @himu-gupta made their first contribution in [#1168](https://github.com/spacecowboy/feeder/pull/1168)

## [2.21.1] - 2026-06-13

### 🐛 Bug Fixes & Minor Changes
- Increased max article download size to 3 MB from 1 MB (#1164) by @spacecowboy in [#1164](https://github.com/spacecowboy/feeder/pull/1164) 
- Restored scroll position indicator in the article reader (#1165) by @mvanhorn in [#1165](https://github.com/spacecowboy/feeder/pull/1165) 

### 🌐 Translations
- Updated Tamil translation using Weblate in [commit](https://github.com/spacecowboy/feeder/commit/f7b9422f8ddbfa752b5461dbaf740e6b41bff0c5)
- Updated Czech translation using Weblate in [commit](https://github.com/spacecowboy/feeder/commit/afb682c96b4d44eb1d909a72ed7217f0b29f4304)
- Updated Latvian translation using Weblate by @Coool in [commit](https://github.com/spacecowboy/feeder/commit/ac6e0694ab808303cd626219c0fc9e5c4af7da49)
- Updated Italian translation using Weblate by @Wiccio in [#1159](https://github.com/spacecowboy/feeder/pull/1159) 


## [2.21.0] - 2026-06-08

### 🚀 Features
- Use feed entry id as fallback link. (#1144) by @fictiontoreality in [#1144](https://github.com/spacecowboy/feeder/pull/1144) 
- Upgraded export format so saved articles can be imported again (#1131) by @edd255 in [#1131](https://github.com/spacecowboy/feeder/pull/1131) 
- Added on-device local as an option for translation (#1143) by @JaredTweed in [#1143](https://github.com/spacecowboy/feeder/pull/1143) 

### 🐛 Bug Fixes & Minor Changes
- Widget stuck in loading (#1137) by @MatthewTighe in [#1137](https://github.com/spacecowboy/feeder/pull/1137) 
- Corrected three sync read-status bugs (#1141) by @spacecowboy in [#1141](https://github.com/spacecowboy/feeder/pull/1141) 
- Resolved flaky test caused by identical generated pubDates for undated feed items (#1146) by @spacecowboy in [#1146](https://github.com/spacecowboy/feeder/pull/1146) 
- Corrected defaults causing AI translation appear enabled on upgrade (#1153) by @spacecowboy in [#1153](https://github.com/spacecowboy/feeder/pull/1153) 
- Prevented OOM crash when fetching or rendering large articles (#1154) by @spacecowboy in [#1154](https://github.com/spacecowboy/feeder/pull/1154) 

### 🚜 Refactoring
- Cleaned up sync read-status layer (#1142) by @spacecowboy in [#1142](https://github.com/spacecowboy/feeder/pull/1142) 

### 📚 Documentation
- Added logging conventions to AGENTS.md (#1148) by @spacecowboy in [#1148](https://github.com/spacecowboy/feeder/pull/1148) 

### 🌐 Translations
- Updated Chinese (Simplified Han script) translation using Weblate in [commit](https://github.com/spacecowboy/feeder/commit/46728ac0b5227258252ec1804064b505e125c961)
- Updated French translation using Weblate by @Matth7878 in [commit](https://github.com/spacecowboy/feeder/commit/1681f83ab4d8fe77d57311bce93d851407d4ca02)
- Updated Czech translation using Weblate in [commit](https://github.com/spacecowboy/feeder/commit/4ed2e847637f98c63a28e6c20c62574e8306545c)
- Updated Latvian translation using Weblate by @Coool in [commit](https://github.com/spacecowboy/feeder/commit/2153f7ed0d2a304e9566ac9c9271a6359789493f)
- Updated Polish translation using Weblate by @Aga-C in [commit](https://github.com/spacecowboy/feeder/commit/ad2bf33a47b78c6a57f0f7446b1e4d13c6aaa66e)
- Updated German translation using Weblate in [commit](https://github.com/spacecowboy/feeder/commit/df54bfc1c9c2cfec6c40e74ad87e73b0b5b415a1)
- Updated Hungarian translation using Weblate by @summoner001 in [commit](https://github.com/spacecowboy/feeder/commit/2576e176174da6e525cfb157ffb4b3e6a875f05b)
- Updated Estonian translation using Weblate in [commit](https://github.com/spacecowboy/feeder/commit/8bbbf21b7b2e9b2b267358b64976cfea06dc3bb8)
- Updated Indonesian translation using Weblate by @arifpedia in [commit](https://github.com/spacecowboy/feeder/commit/5bef23f131dfc6ca33519ff5c4e636c2b70d0ee6)
- Updated Serbian translation using Weblate by @eevan78 in [commit](https://github.com/spacecowboy/feeder/commit/14c4bd56a2a64a2a468412197a886acd065deedc)
- Updated Spanish translation using Weblate in [commit](https://github.com/spacecowboy/feeder/commit/235f79d6b102a08696b7fd4aa550b037255ea6ab)
- Updated Ukrainian translation using Weblate in [commit](https://github.com/spacecowboy/feeder/commit/e821ad042b084cea2dca8d4768f70682cf2eb2a9)

### ❤️  New Contributors
* @arifpedia made their first contribution
* @edd255 made their first contribution in [#1131](https://github.com/spacecowboy/feeder/pull/1131)
* @fictiontoreality made their first contribution in [#1144](https://github.com/spacecowboy/feeder/pull/1144)

## [2.20.0] - 2026-05-15

### 🚀 Features
- Added stripping of tracking parameters before opening URLs (#1097) by @mvanhorn in [#1097](https://github.com/spacecowboy/feeder/pull/1097) 
- Preserve article scroll position when Android recreates the process (#1115) by @boringrgb in [#1115](https://github.com/spacecowboy/feeder/pull/1115) 
- Added ability to translate articles with AI service (#1072) by @JaredTweed in [#1072](https://github.com/spacecowboy/feeder/pull/1072) 
- Add per-feed article metadata thumbnail enrichment (#1130) by @tokenflood in [#1130](https://github.com/spacecowboy/feeder/pull/1130) 
- Enabled arm memory tagging extension (#1132) by @flexxxxer in [#1132](https://github.com/spacecowboy/feeder/pull/1132) 

### 🐛 Bug Fixes & Minor Changes
- Added initial widget state and timeouts for bitmap loading (#1113) (#1114) by @MatthewTighe in [#1114](https://github.com/spacecowboy/feeder/pull/1114) 

### ❤️  New Contributors
* @flexxxxer made their first contribution in [#1132](https://github.com/spacecowboy/feeder/pull/1132)
* @tokenflood made their first contribution in [#1130](https://github.com/spacecowboy/feeder/pull/1130)
* @boringrgb made their first contribution in [#1115](https://github.com/spacecowboy/feeder/pull/1115)
* @mvanhorn made their first contribution in [#1097](https://github.com/spacecowboy/feeder/pull/1097)
