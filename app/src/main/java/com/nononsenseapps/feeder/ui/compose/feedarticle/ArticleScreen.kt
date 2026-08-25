package com.nononsenseapps.feeder.ui.compose.feedarticle

import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nononsenseapps.feeder.R
import com.nononsenseapps.feeder.ai.SummaryResponseParser
import com.nononsenseapps.feeder.archmodel.TextToDisplay
import com.nononsenseapps.feeder.db.room.ID_UNSET
import com.nononsenseapps.feeder.model.LocaleOverride
import com.nononsenseapps.feeder.ui.MainActivityViewModel
import com.nononsenseapps.feeder.ui.ScrollDirection
import com.nononsenseapps.feeder.ui.compose.components.safeSemantics
import com.nononsenseapps.feeder.ui.compose.feed.PlainTooltipBox
import com.nononsenseapps.feeder.ui.compose.html.ColumnArticleContent
import com.nononsenseapps.feeder.ui.compose.icons.CustomFilled
import com.nononsenseapps.feeder.ui.compose.icons.TextToSpeech
import com.nononsenseapps.feeder.ui.compose.readaloud.HideableTTSPlayer
import com.nononsenseapps.feeder.ui.compose.text.MarkdownContentSafe
import com.nononsenseapps.feeder.ui.compose.theme.SensibleTopAppBar
import com.nononsenseapps.feeder.ui.compose.utils.ImmutableHolder
import com.nononsenseapps.feeder.ui.compose.utils.ScreenType
import com.nononsenseapps.feeder.ui.compose.utils.onKeyEventLikeEscape
import com.nononsenseapps.feeder.util.ActivityLauncher
import com.nononsenseapps.feeder.util.stripTrackingParameters
import com.nononsenseapps.feeder.util.unicodeWrap
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import org.kodein.di.compose.LocalDI
import org.kodein.di.instance
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Date
import java.util.TimeZone

@Composable
fun ArticleScreen(
    onNavigateUp: () -> Unit,
    onNavigateToFeed: (Long) -> Unit,
    viewModel: ArticleViewModel,
    mainActivityViewModel: MainActivityViewModel,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onNavigateUp)
    val di = LocalDI.current
    val activityLauncher: ActivityLauncher by di.instance()

    val mavm = remember { mainActivityViewModel }
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val isPagingMode by mavm.isPagingMode.collectAsStateWithLifecycle()
    val isAnimatedPaging by mavm.isAnimatedPaging.collectAsStateWithLifecycle()

    val articleScrollState = rememberScrollState(initial = viewModel.scrollPosition)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(articleScrollState) {
        snapshotFlow { articleScrollState.value }
            .debounce(500)
            .collect { viewModel.saveScrollPosition(it) }
    }

    LaunchedEffect(Unit) {
        mavm.scrollCommand.collect { direction ->
            val scrollAmount = (articleScrollState.viewportSize * 0.9f).toInt()
            when (direction) {
                ScrollDirection.UP -> {
                    val target = (articleScrollState.value - scrollAmount).coerceAtLeast(0)
                    if (isAnimatedPaging) {
                        articleScrollState.animateScrollTo(target)
                    } else {
                        articleScrollState.scrollTo(target)
                    }
                }

                ScrollDirection.DOWN -> {
                    val target = (articleScrollState.value + scrollAmount).coerceAtMost(articleScrollState.maxValue)
                    if (isAnimatedPaging) {
                        articleScrollState.animateScrollTo(target)
                    } else {
                        articleScrollState.scrollTo(target)
                    }
                }
            }
        }
    }

    val toolbarColor = MaterialTheme.colorScheme.surface.toArgb()

    ArticleScreen(
        viewState = viewState,
        onToggleFullText = viewModel::toggleFullText,
        onMarkAsUnread = viewModel::markAsUnread,
        onShare = {
            if (viewState.articleId > ID_UNSET) {
                val intent =
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            val articleLink = viewState.articleLink
                            if (articleLink != null) {
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    stripTrackingParameters(articleLink),
                                )
                            }
                            putExtra(Intent.EXTRA_TITLE, viewState.articleTitle)
                            type = "text/plain"
                        },
                        null,
                    )
                activityLauncher.startActivity(
                    openAdjacentIfSuitable = false,
                    intent = intent,
                )
            }
        },
        onOpenInCustomTab = {
            viewState.articleLink?.let { link ->
                activityLauncher.openLinkInCustomTab(link, toolbarColor)
            }
        },
        onFeedTitleClick = {
            onNavigateToFeed(viewState.articleFeedId)
        },
        onOpenAudioPlayer = viewModel::openPodcastPlayer,
        onShowToolbarMenu = viewModel::setToolbarMenuVisible,
        ttsOnPlay = viewModel::ttsPlay,
        ttsOnPause = viewModel::ttsPause,
        ttsOnStop = viewModel::ttsStop,
        ttsOnSkipNext = viewModel::ttsSkipNext,
        ttsOnSelectLanguage = viewModel::ttsOnSelectLanguage,
        podcastOnPlay = viewModel::podcastPlay,
        podcastOnPause = viewModel::podcastPause,
        podcastOnStop = viewModel::stopPodcastPlayback,
        podcastOnSeekBack = { viewModel.podcastSeekBy(-10_000) },
        podcastOnSeekForward = { viewModel.podcastSeekBy(10_000) },
        podcastOnSeekTo = viewModel::podcastSeekTo,
        onToggleBookmark = {
            viewModel.setBookmarked(!viewState.isBookmarked)
        },
        articleScrollState = articleScrollState,
        onNavigateUp = onNavigateUp,
        onSummarize = {
            viewModel.summarize()
        },
        onTranslate = {
            val isAlreadyTranslated = viewState.translation is TranslationState.Translated
            viewModel.translate(forceRefresh = isAlreadyTranslated)
        },
        onCancelSummarize = {
            viewModel.cancelSummarize()
        },
        onCancelTranslation = {
            viewModel.cancelTranslation()
        },
        modifier = modifier,
        isPagingMode = isPagingMode,
        isAnimatedPaging = isAnimatedPaging,
    )
}

@OptIn(
    ExperimentalMaterial3Api::class,
)
@Composable
fun ArticleScreen(
    viewState: ArticleScreenViewState,
    onToggleFullText: () -> Unit,
    onMarkAsUnread: () -> Unit,
    onShare: () -> Unit,
    onOpenInCustomTab: () -> Unit,
    onFeedTitleClick: () -> Unit,
    onOpenAudioPlayer: (url: String) -> Unit,
    onShowToolbarMenu: (Boolean) -> Unit,
    ttsOnPlay: () -> Unit,
    ttsOnPause: () -> Unit,
    ttsOnStop: () -> Unit,
    ttsOnSkipNext: () -> Unit,
    ttsOnSelectLanguage: (LocaleOverride) -> Unit,
    podcastOnPlay: () -> Unit,
    podcastOnPause: () -> Unit,
    podcastOnStop: () -> Unit,
    podcastOnSeekBack: () -> Unit,
    podcastOnSeekForward: () -> Unit,
    podcastOnSeekTo: (Int) -> Unit,
    onToggleBookmark: () -> Unit,
    articleScrollState: ScrollState,
    onNavigateUp: () -> Unit,
    onSummarize: () -> Unit,
    onTranslate: () -> Unit,
    onCancelSummarize: () -> Unit,
    onCancelTranslation: () -> Unit,
    modifier: Modifier = Modifier,
    isPagingMode: Boolean = false,
    isAnimatedPaging: Boolean = false,
) {
    // Wrap with custom text toolbar to enable selection menu
    com.nononsenseapps.feeder.ui.compose.utils.WithFeederTextToolbar(
        onReadAloud = { ttsOnPlay() },
        onTranslate = { onTranslate() },
    ) {
        ArticleScreenInternal(
            viewState = viewState,
            onToggleFullText = onToggleFullText,
            onMarkAsUnread = onMarkAsUnread,
            onShare = onShare,
            onOpenInCustomTab = onOpenInCustomTab,
            onFeedTitleClick = onFeedTitleClick,
            onOpenAudioPlayer = onOpenAudioPlayer,
            onShowToolbarMenu = onShowToolbarMenu,
            ttsOnPlay = ttsOnPlay,
            ttsOnPause = ttsOnPause,
            ttsOnStop = ttsOnStop,
            ttsOnSkipNext = ttsOnSkipNext,
            ttsOnSelectLanguage = ttsOnSelectLanguage,
            podcastOnPlay = podcastOnPlay,
            podcastOnPause = podcastOnPause,
            podcastOnStop = podcastOnStop,
            podcastOnSeekBack = podcastOnSeekBack,
            podcastOnSeekForward = podcastOnSeekForward,
            podcastOnSeekTo = podcastOnSeekTo,
            onToggleBookmark = onToggleBookmark,
            articleScrollState = articleScrollState,
            onNavigateUp = onNavigateUp,
            onSummarize = onSummarize,
            onTranslate = onTranslate,
            onCancelSummarize = onCancelSummarize,
            onCancelTranslation = onCancelTranslation,
            modifier = modifier,
            isPagingMode = isPagingMode,
            isAnimatedPaging = isAnimatedPaging,
        )
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
)
@Composable
private fun ArticleScreenInternal(
    viewState: ArticleScreenViewState,
    onToggleFullText: () -> Unit,
    onMarkAsUnread: () -> Unit,
    onShare: () -> Unit,
    onOpenInCustomTab: () -> Unit,
    onFeedTitleClick: () -> Unit,
    onOpenAudioPlayer: (url: String) -> Unit,
    onShowToolbarMenu: (Boolean) -> Unit,
    ttsOnPlay: () -> Unit,
    ttsOnPause: () -> Unit,
    ttsOnStop: () -> Unit,
    ttsOnSkipNext: () -> Unit,
    ttsOnSelectLanguage: (LocaleOverride) -> Unit,
    podcastOnPlay: () -> Unit,
    podcastOnPause: () -> Unit,
    podcastOnStop: () -> Unit,
    podcastOnSeekBack: () -> Unit,
    podcastOnSeekForward: () -> Unit,
    podcastOnSeekTo: (Int) -> Unit,
    onToggleBookmark: () -> Unit,
    articleScrollState: ScrollState,
    onNavigateUp: () -> Unit,
    onSummarize: () -> Unit,
    onTranslate: () -> Unit,
    onCancelSummarize: () -> Unit,
    onCancelTranslation: () -> Unit,
    modifier: Modifier = Modifier,
    isPagingMode: Boolean = false,
    isAnimatedPaging: Boolean = false,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    val bottomBarVisibleState = remember { MutableTransitionState(viewState.isBottomBarVisible) }
    LaunchedEffect(viewState.isBottomBarVisible) {
        bottomBarVisibleState.targetState = viewState.isBottomBarVisible
    }

    val focusArticle = remember { FocusRequester() }
    val focusTopBar = remember { FocusRequester() }

    val closeMenuText = stringResource(id = R.string.close_menu)

    Scaffold(
        modifier =
            modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal)),
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            SensibleTopAppBar(
                modifier =
                    Modifier
                        .focusGroup()
                        .focusRequester(focusTopBar)
                        .focusProperties {
                            down = focusArticle
                        },
                scrollBehavior = scrollBehavior,
                title = viewState.feedDisplayTitle,
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.go_back),
                        )
                    }
                },
                actions = {
                    // Summarize button (conditional)
                    if (viewState.showSummarize) {
                        val isSummarizing = viewState.aiSummary is AISummaryState.Loading
                        PlainTooltipBox(
                            tooltip = {
                                Text(
                                    stringResource(
                                        if (isSummarizing) {
                                            R.string.cancel_summarize
                                        } else {
                                            R.string.summarize
                                        },
                                    ),
                                )
                            },
                        ) {
                            CircleProgressIconButton(
                                isInProgress = isSummarizing,
                                progressFraction = null,
                                icon = Icons.Default.AutoFixHigh,
                                idleContentDescription = stringResource(R.string.summarize),
                                progressContentDescription = stringResource(R.string.summarizing_tap_to_cancel),
                                onAction = onSummarize,
                                onCancel = onCancelSummarize,
                            )
                        }
                    }

                    // Translate button (conditional)
                    if (viewState.showTranslate) {
                        val isTranslating = viewState.translation is TranslationState.Translating
                        val translationProgressFraction: (() -> Float)? =
                            if (isTranslating) {
                                val articleTranslation = (viewState.translation as TranslationState.Translating).articleTranslation
                                val completed = articleTranslation.paragraphCompletedCount
                                val total = articleTranslation.paragraphTotalCount
                                { if (total > 0) completed.toFloat() / total else 0f }
                            } else {
                                null
                            }
                        PlainTooltipBox(
                            tooltip = {
                                Text(
                                    stringResource(
                                        if (isTranslating) {
                                            R.string.cancel_translation
                                        } else {
                                            R.string.translate
                                        },
                                    ),
                                )
                            },
                        ) {
                            CircleProgressIconButton(
                                isInProgress = isTranslating,
                                progressFraction = translationProgressFraction,
                                icon = Icons.Default.Translate,
                                idleContentDescription = stringResource(R.string.translate_article_content_description),
                                progressContentDescription =
                                    if (isTranslating) {
                                        val articleTranslation = (viewState.translation as TranslationState.Translating).articleTranslation
                                        stringResource(
                                            R.string.translating_x_of_y_tap_to_cancel,
                                            articleTranslation.paragraphCompletedCount,
                                            articleTranslation.paragraphTotalCount,
                                        )
                                    } else {
                                        stringResource(R.string.translate_article_content_description)
                                    },
                                onAction = onTranslate,
                                onCancel = onCancelTranslation,
                            )
                        }
                    }

                    PlainTooltipBox(tooltip = { Text(stringResource(R.string.fetch_full_article)) }) {
                        IconButton(
                            onClick = onToggleFullText,
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Article,
                                contentDescription = stringResource(R.string.fetch_full_article),
                            )
                        }
                    }

                    PlainTooltipBox(tooltip = { Text(stringResource(id = R.string.open_menu)) }) {
                        Box {
                            IconButton(
                                onClick = { onShowToolbarMenu(true) },
                            ) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = stringResource(id = R.string.open_menu),
                                )
                            }
                            DropdownMenu(
                                expanded = viewState.showToolbarMenu,
                                onDismissRequest = { onShowToolbarMenu(false) },
                                modifier =
                                    Modifier
                                        .onKeyEventLikeEscape {
                                            onShowToolbarMenu(false)
                                        },
                            ) {
                                DropdownMenuItem(
                                    onClick = {
                                        onShowToolbarMenu(false)
                                        onShare()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Share,
                                            contentDescription = null,
                                        )
                                    },
                                    text = {
                                        Text(stringResource(id = R.string.share))
                                    },
                                )

                                // Open in Web View
                                DropdownMenuItem(
                                    onClick = {
                                        onShowToolbarMenu(false)
                                        onOpenInCustomTab()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.OpenInBrowser,
                                            contentDescription = null,
                                        )
                                    },
                                    text = {
                                        Text(stringResource(id = R.string.open_in_web_view))
                                    },
                                )

                                if (viewState.showTranslate) {
                                    DropdownMenuItem(
                                        onClick = {
                                            onShowToolbarMenu(false)
                                            onTranslate()
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Translate,
                                                contentDescription = null,
                                            )
                                        },
                                        text = {
                                            Text(
                                                stringResource(R.string.translate_article),
                                            )
                                        },
                                    )
                                }

                                DropdownMenuItem(
                                    onClick = {
                                        onShowToolbarMenu(false)
                                        onMarkAsUnread()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.VisibilityOff,
                                            contentDescription = null,
                                        )
                                    },
                                    text = {
                                        Text(stringResource(id = R.string.mark_as_unread))
                                    },
                                )
                                DropdownMenuItem(
                                    onClick = {
                                        onShowToolbarMenu(false)
                                        onToggleBookmark()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = null,
                                        )
                                    },
                                    text = {
                                        Text(
                                            stringResource(
                                                if (viewState.isBookmarked) {
                                                    R.string.unsave_article
                                                } else {
                                                    R.string.save_article
                                                },
                                            ),
                                        )
                                    },
                                )
                                DropdownMenuItem(
                                    onClick = {
                                        onShowToolbarMenu(false)
                                        ttsOnPlay()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.CustomFilled.TextToSpeech,
                                            contentDescription = null,
                                        )
                                    },
                                    text = {
                                        Text(stringResource(id = R.string.read_article))
                                    },
                                )
                                // Hidden button for TalkBack
                                DropdownMenuItem(
                                    onClick = {
                                        onShowToolbarMenu(false)
                                    },
                                    text = {},
                                    modifier =
                                        Modifier
                                            .height(0.dp)
                                            .safeSemantics {
                                                contentDescription = closeMenuText
                                                role = Role.Button
                                            },
                                )
                            }
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (viewState.podcastPlayerState.isVisible) {
                HideablePodcastPlayer(
                    visibleState = bottomBarVisibleState,
                    viewState = viewState.podcastPlayerState,
                    onPlay = podcastOnPlay,
                    onPause = podcastOnPause,
                    onStop = podcastOnStop,
                    onSeekBack = podcastOnSeekBack,
                    onSeekForward = podcastOnSeekForward,
                    onSeekTo = podcastOnSeekTo,
                )
            } else {
                HideableTTSPlayer(
                    visibleState = bottomBarVisibleState,
                    currentlyPlaying = viewState.isTTSPlaying,
                    onPlay = ttsOnPlay,
                    onPause = ttsOnPause,
                    onStop = ttsOnStop,
                    onSkipNext = ttsOnSkipNext,
                    languages = ImmutableHolder(viewState.ttsLanguages),
                    onSelectLanguage = ttsOnSelectLanguage,
                )
            }
        },
    ) { padding ->
        // Box handles the dynamic padding so ArticleContent don't have to recompose on scroll
        Box(
            modifier =
                Modifier
                    .padding(padding),
        ) {
            // Handle text selection menu popup
            com.nononsenseapps.feeder.ui.compose.utils.TextSelectionMenuHandler(
                onReadAloud = { ttsOnPlay() },
                onTranslate = { onTranslate() },
            )

            val coroutineScope = rememberCoroutineScope()
            ArticleContent(
                viewState = viewState,
                screenType = ScreenType.SINGLE,
                articleScrollState = articleScrollState,
                onFeedTitleClick = onFeedTitleClick,
                onOpenAudioPlayer = onOpenAudioPlayer,
                onOpenInCustomTab = onOpenInCustomTab,
                modifier =
                    Modifier
                        .focusGroup()
                        .focusRequester(focusArticle)
                        .focusProperties {
                            up = focusTopBar
                        },
            )

            if (isPagingMode) {
                Row(modifier = Modifier.matchParentSize()) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxHeight()
                                .weight(0.2f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    val scrollAmount = (articleScrollState.viewportSize * 0.9f).toInt()
                                    val target = (articleScrollState.value - scrollAmount).coerceAtLeast(0)
                                    coroutineScope.launch {
                                        if (isAnimatedPaging) {
                                            articleScrollState.animateScrollTo(target)
                                        } else {
                                            articleScrollState.scrollTo(target)
                                        }
                                    }
                                },
                    )
                    Spacer(modifier = Modifier.weight(0.6f))
                    Box(
                        modifier =
                            Modifier
                                .fillMaxHeight()
                                .weight(0.2f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    val scrollAmount = (articleScrollState.viewportSize * 0.9f).toInt()
                                    val target = (articleScrollState.value + scrollAmount).coerceAtMost(articleScrollState.maxValue)
                                    coroutineScope.launch {
                                        if (isAnimatedPaging) {
                                            articleScrollState.animateScrollTo(target)
                                        } else {
                                            articleScrollState.scrollTo(target)
                                        }
                                    }
                                },
                    )
                }
            }
        }
    }
}

@Composable
fun ArticleContent(
    viewState: ArticleScreenViewState,
    screenType: ScreenType,
    onFeedTitleClick: () -> Unit,
    onOpenAudioPlayer: (url: String) -> Unit,
    onOpenInCustomTab: () -> Unit,
    articleScrollState: ScrollState,
    modifier: Modifier = Modifier,
) {
    val toolbarColor = MaterialTheme.colorScheme.surface.toArgb()

    val context = LocalContext.current
    val activityLauncher: ActivityLauncher by LocalDI.current.instance()
    val coroutineScope = rememberCoroutineScope()

    // Track Y positions of article elements by index for anchor link scrolling
    val elementPositions = remember { mutableMapOf<Int, Float>() }
    val contentImageUrls = remember(viewState.articleContent) { viewState.articleContent.imageUrls }

    ReaderView(
        screenType = screenType,
        wordCount = viewState.wordCount,
        onEnclosureClick = {
            if (viewState.enclosure.present) {
                if (viewState.useInAppAudioPlayer && shouldOpenInPodcastPlayer(viewState.enclosure.link, viewState.enclosure)) {
                    onOpenAudioPlayer(viewState.enclosure.link)
                } else {
                    activityLauncher.openLinkInBrowser(link = viewState.enclosure.link)
                }
            }
        },
        onFeedTitleClick = onFeedTitleClick,
        enclosure = viewState.enclosure,
        articleTitle = viewState.articleTitle,
        feedTitle = viewState.feedDisplayTitle,
        authorDate =
            when {
                viewState.author == null && viewState.pubDate != null ->
                    stringResource(
                        R.string.on_date,
                        formatArticleDate(context, viewState.pubDate),
                    )

                viewState.author != null && viewState.pubDate != null ->
                    stringResource(
                        R.string.by_author_on_date,
                        // Must wrap author in unicode marks to ensure it formats
                        // correctly in RTL
                        context.unicodeWrap(viewState.author ?: ""),
                        formatArticleDate(context, viewState.pubDate),
                    )

                else -> null
            },
        image = viewState.image,
        showHeaderImage = viewState.textToDisplay == TextToDisplay.CONTENT,
        contentImageUrls = contentImageUrls,
        modifier = modifier,
        articleScrollState = articleScrollState,
    ) { indexOffset ->
        var offsetCounter = indexOffset

        if (viewState.aiSummary is AISummaryState.Result) {
            offsetCounter++
            SummarySection(viewState.aiSummary)
        }

        // Translation status section (loading or error)
        if (viewState.translation is TranslationState.Translated || viewState.translation is TranslationState.Error) {
            offsetCounter++
            TranslationStatusSection(viewState.translation)
        }

        // Can take a composition or two before viewstate is set to its actual values
        if (viewState.articleId > ID_UNSET) {
            when (viewState.textToDisplay) {
                TextToDisplay.CONTENT -> {
                    // Extract translated paragraphs if available
                    val translatedParagraphs =
                        when (val translation = viewState.translation) {
                            is TranslationState.Translating ->
                                translation.articleTranslation
                                    .buildTranslatedParagraphsList()
                                    .map { it ?: "" }
                            is TranslationState.Translated ->
                                translation.articleTranslation
                                    .buildTranslatedParagraphsList()
                                    .map { it ?: "" }
                            else -> null
                        }

                    ColumnArticleContent(
                        articleContent = viewState.articleContent,
                        translatedParagraphs = translatedParagraphs,
                        onLinkClick = { link, index ->
                            if (index != null && elementPositions.containsKey(index)) {
                                // Anchor link - scroll to the element position
                                val yPosition = elementPositions[index]
                                if (yPosition != null) {
                                    coroutineScope.launch {
                                        articleScrollState.animateScrollTo(yPosition.toInt())
                                    }
                                }
                            } else {
                                if (viewState.useInAppAudioPlayer && shouldOpenInPodcastPlayer(link, viewState.enclosure)) {
                                    onOpenAudioPlayer(link)
                                } else {
                                    // External link - open in browser/custom tab
                                    activityLauncher.openLink(
                                        link = link,
                                        toolbarColor = toolbarColor,
                                    )
                                }
                            }
                        },
                        onElementPosition = { index, yPosition ->
                            elementPositions[offsetCounter + index] = yPosition
                        },
                    )
                }

                TextToDisplay.LOADING_FULLTEXT -> {
                    Text(text = stringResource(id = R.string.fetching_full_article))
                }

                TextToDisplay.FAILED_TO_LOAD_FULLTEXT -> {
                    Text(text = stringResource(id = R.string.failed_to_fetch_full_article))
                }

                TextToDisplay.FAILED_MISSING_BODY -> {
                    Text(text = stringResource(id = R.string.failed_to_fetch_full_article_missing_body))
                }

                TextToDisplay.FAILED_MISSING_LINK -> {
                    Text(text = stringResource(id = R.string.failed_to_fetch_full_article_missing_link))
                }

                TextToDisplay.FAILED_NOT_HTML -> {
                    Text(text = stringResource(id = R.string.failed_to_fetch_full_article_not_html))
                }

                TextToDisplay.FAILED_FULLTEXT_TOO_LARGE -> {
                    Text(text = stringResource(id = R.string.failed_to_fetch_full_article_too_large))
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onOpenInCustomTab) {
                        Text(text = stringResource(id = R.string.open_in_web_view))
                    }
                }
            }
        }
    }
}

internal fun formatArticleDate(
    context: Context,
    publicationDate: ZonedDateTime?,
    zoneId: ZoneId = ZoneId.systemDefault(),
): String =
    publicationDate?.let {
        val skeleton = if (DateFormat.is24HourFormat(context)) "yMMMMEEEEdHm" else "yMMMMEEEEdhm"
        val locale = context.resources.configuration.locales[0]
        SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
            .apply { timeZone = TimeZone.getTimeZone(zoneId) }
            .format(Date.from(it.toInstant()))
    } ?: ""

@Composable
private fun SummarySection(summary: AISummaryState) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        when (summary) {
            AISummaryState.Empty -> {}
            AISummaryState.Loading -> {}
            is AISummaryState.Result -> {
                val displayContent = summary.value.content.trim()
                val safeContent =
                    if (
                        SummaryResponseParser.containsRawJson(displayContent)
                    ) {
                        android.util.Log.w("ArticleScreen", "Detected raw JSON in summary, replacing with error message")
                        "Could not generate summary. Please try again."
                    } else {
                        displayContent
                    }

                MarkdownText(
                    modifier = Modifier.padding(8.dp),
                    markdown = safeContent,
                )
            }
        }
    }
}

/**
 * Displays markdown-formatted text.
 *
 * Renders markdown with comprehensive support including:
 * - Bold and italic text
 * - Links
 * - Lists (including nested)
 * - Headings
 * - Code blocks
 * - Blockquotes
 * - Tables
 * - Task lists
 * - Strikethrough
 *
 * Falls back to plain text if markdown parsing fails.
 */
@Composable
private fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    MarkdownContentSafe(
        markdown = markdown,
        modifier = modifier,
    )
}

/**
 * Displays the translation status section in the article content.
 *
 * Handles three states:
 * - Empty: Shows nothing
 * - Loading: Shows progress indicator with text
 * - Result: Delegates to TranslationErrorSection for errors, or returns nothing for success
 * (success translations are displayed inline with paragraphs)
 */
@Composable
private fun TranslationStatusSection(translation: TranslationState) {
    when (translation) {
        TranslationState.Empty -> {}

        is TranslationState.Translating -> {}

        is TranslationState.Translated -> {
            val failedCount = translation.articleTranslation.paragraphFailedCount
            if (failedCount > 0) {
                TranslationErrorSection(
                    errorMessage = "$failedCount paragraph(s) failed to translate",
                )
            }
        }

        is TranslationState.Error ->
            TranslationErrorSection(errorMessage = translation.errorMessage)
    }
}

/**
 * Displays an error message when translation fails.
 *
 * Note: There is no retry button. Users tap the translate button again to retry.
 */
@Composable
private fun TranslationErrorSection(errorMessage: String) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        border =
            BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.error,
            ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = stringResource(R.string.translation_error),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
