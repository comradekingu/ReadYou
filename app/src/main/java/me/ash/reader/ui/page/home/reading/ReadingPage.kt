package me.ash.reader.ui.page.home.reading

import android.content.Intent
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import me.ash.reader.R
import me.ash.reader.data.entity.ArticleWithFeed
import me.ash.reader.ui.component.base.FeedbackIconButton
import me.ash.reader.ui.component.base.RYScaffold
import me.ash.reader.ui.component.reader.reader
import me.ash.reader.ui.ext.collectAsStateValue
import me.ash.reader.ui.ext.drawVerticalScrollbar

@Composable
fun ReadingPage(
    navController: NavHostController,
    readingViewModel: ReadingViewModel = hiltViewModel(),
) {
    val readingUiState = readingViewModel.readingUiState.collectAsStateValue()
    val isScrollDown = readingUiState.listState.isScrollDown()

    LaunchedEffect(Unit) {
        navController.currentBackStackEntryFlow.collect {
            it.arguments?.getString("articleId")?.let {
                readingViewModel.initData(it)
            }
        }
    }

    LaunchedEffect(readingUiState.articleWithFeed?.article?.id) {
        Log.i("RLog", "ReadPage: ${readingUiState.articleWithFeed}")
        readingUiState.articleWithFeed?.let {
            if (it.article.isUnread) {
                readingViewModel.markUnread(false)
            }
        }
    }

    RYScaffold(
        content = {
            Box(Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(1f),
                    contentAlignment = Alignment.TopCenter
                ) {
                    TopBar(
                        isShow = readingUiState.articleWithFeed == null || !isScrollDown,
                        title = readingUiState.articleWithFeed?.article?.title,
                        link = readingUiState.articleWithFeed?.article?.link,
                        onClose = {
                            navController.popBackStack()
                        },
                    )
                }
                Content(
                    content = readingUiState.content ?: "",
                    articleWithFeed = readingUiState.articleWithFeed,
                    isLoading = readingUiState.isLoading,
                    listState = readingUiState.listState,
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(1f),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    BottomBar(
                        isShow = readingUiState.articleWithFeed != null && !isScrollDown,
                        articleWithFeed = readingUiState.articleWithFeed,
                        unreadOnClick = {
                            readingViewModel.markUnread(it)
                        },
                        starredOnClick = {
                            readingViewModel.markStarred(it)
                        },
                        fullContentOnClick = { afterIsFullContent ->
                            if (afterIsFullContent) readingViewModel.renderFullContent()
                            else readingViewModel.renderDescriptionContent()
                        },
                    )
                }
            }
        }
    )
}

@Composable
fun LazyListState.isScrollDown(): Boolean {
    var isScrollDown by remember { mutableStateOf(false) }
    var preItemIndex by remember { mutableStateOf(0) }
    var preScrollStartOffset by remember { mutableStateOf(0) }

    LaunchedEffect(this) {
        snapshotFlow { isScrollInProgress }.collect {
            if (isScrollInProgress) {
                isScrollDown = when {
                    firstVisibleItemIndex > preItemIndex -> true
                    firstVisibleItemScrollOffset < preItemIndex -> false
                    else -> firstVisibleItemScrollOffset > preScrollStartOffset
                }
            } else {
                preItemIndex = firstVisibleItemIndex
                preScrollStartOffset = firstVisibleItemScrollOffset
            }
        }
    }

    return isScrollDown
}

@Composable
private fun TopBar(
    isShow: Boolean,
    title: String? = "",
    link: String? = "",
    onClose: () -> Unit = {},
) {
    val context = LocalContext.current

    AnimatedVisibility(
        visible = isShow,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        SmallTopAppBar(
            modifier = Modifier.statusBarsPadding(),
            colors = TopAppBarDefaults.smallTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            title = {},
            navigationIcon = {
                FeedbackIconButton(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.close),
                    tint = MaterialTheme.colorScheme.onSurface
                ) {
                    onClose()
                }
            },
            actions = {
                FeedbackIconButton(
                    modifier = Modifier.size(20.dp),
                    imageVector = Icons.Outlined.Share,
                    contentDescription = stringResource(R.string.share),
                    tint = MaterialTheme.colorScheme.onSurface,
                ) {
                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                        putExtra(
                            Intent.EXTRA_TEXT,
                            title?.takeIf { it.isNotBlank() }?.let { it + "\n" } + link
                        )
                        type = "text/plain"
                    }, context.getString(R.string.share)))
                }
            }
        )
    }
}

@Composable
private fun Content(
    content: String,
    articleWithFeed: ArticleWithFeed?,
    listState: LazyListState,
    isLoading: Boolean,
) {
    if (articleWithFeed == null) return
    val context = LocalContext.current

    SelectionContainer {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .drawVerticalScrollbar(listState),
            state = listState,
        ) {
            item {
                Spacer(modifier = Modifier.height(64.dp))
                Spacer(modifier = Modifier.height(22.dp))
                Column(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                ) {
                    DisableSelection {
                        Header(articleWithFeed)
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(22.dp))
                AnimatedVisibility(
                    visible = isLoading,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(22.dp))
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(30.dp),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(22.dp))
                        }
                    }
                }
            }
            if (!isLoading) {
                reader(
                    context = context,
                    link = articleWithFeed.article.link,
                    content = content
                )
            }
            item {
                Spacer(modifier = Modifier.height(64.dp))
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }
}

@Composable
private fun BottomBar(
    isShow: Boolean,
    articleWithFeed: ArticleWithFeed?,
    unreadOnClick: (afterIsUnread: Boolean) -> Unit = {},
    starredOnClick: (afterIsStarred: Boolean) -> Unit = {},
    fullContentOnClick: (afterIsFullContent: Boolean) -> Unit = {},
) {
    articleWithFeed?.let {
        AnimatedVisibility(
            visible = isShow,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            ReadBar(
                disabled = false,
                isUnread = articleWithFeed.article.isUnread,
                isStarred = articleWithFeed.article.isStarred,
                isFullContent = articleWithFeed.feed.isFullContent,
                unreadOnClick = unreadOnClick,
                starredOnClick = starredOnClick,
                fullContentOnClick = fullContentOnClick,
            )
        }
    }
}