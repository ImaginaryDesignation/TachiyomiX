package eu.kanade.presentation.more.settings.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMap
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.coroutineScope
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBarTitle
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.util.Screen
import eu.kanade.presentation.util.collectAsState
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import tachiyomi.core.util.lang.launchIO
import tachiyomi.core.util.lang.launchUI
import tachiyomi.domain.backup.service.BackupPreferences
import tachiyomi.domain.backupManga.interactor.DeleteBackupsByMangaId
import tachiyomi.domain.backupManga.interactor.GetAllBackupManga
import tachiyomi.domain.backupManga.model.BackupWithMangaDetails
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.SortItem
import tachiyomi.presentation.core.components.material.Divider
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.util.selectedBackground
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.time.Duration.Companion.seconds

class ClearCoverBackupsScreen : Screen() {

    @Composable
    override fun Content() {
        val backupPreferences = Injekt.get<BackupPreferences>()
        val coverBackupSlotsPref = backupPreferences.coverBackupLimit()
        val coverBackupSlots by coverBackupSlotsPref.collectAsState()
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val model = rememberScreenModel {
            ClearCoverBackupsScreenModel()
        }
        val state by model.state.collectAsState()
        val scope = rememberCoroutineScope()

        when (val s = state) {
            is ClearCoverBackupsScreenModel.State.Loading -> LoadingScreen()
            is ClearCoverBackupsScreenModel.State.Ready -> {
                if (s.showConfirmation) {
                    AlertDialog(
                        onDismissRequest = model::hideConfirmation,
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    scope.launchUI {
                                        model.removeCoverBackups()
                                        model.clearSelection()
                                        model.hideConfirmation()
                                        context.toast(R.string.delete_covers_toast)
                                    }
                                },
                            ) {
                                Text(text = stringResource(android.R.string.ok))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = model::hideConfirmation) {
                                Text(text = stringResource(R.string.action_cancel))
                            }
                        },
                        text = {
                            Text(text = stringResource(R.string.clear_cover_backup_confirmation))
                        },
                    )
                } else if (s.showFilterDialog) {
                    FilterDialog(
                        filter = s.filterNumberOfBackups,
                        coverBackupSlots = coverBackupSlots,
                        onFilter = model::filterItems,
                        onDismissRequest = model::hideFilterDialog,
                    )
                } else if (s.showSortDialog) {
                    SortDialog(
                        initialCriteria = s.sortCriteria,
                        initialDirection = s.sortDirection,
                        sort = model::sortItems,
                        onDismissRequest = model::hideSortDialog,
                    )
                }

                val listState = rememberLazyListState()

                val enableScrollToTop by remember {
                    derivedStateOf {
                        listState.firstVisibleItemIndex > 0
                    }
                }

                val enableScrollToBottom by remember {
                    derivedStateOf {
                        listState.canScrollForward
                    }
                }

                Scaffold(
                    topBar = { scrollBehavior ->
                        ClearCoverBackupsScreenAppBar(
                            title = stringResource(
                                R.string.label_clear_backups,
                                s.items.size,
                            ),
                            actionModeCounter = s.selection.size,
                            scrollBehavior = scrollBehavior,
                        )
                    },
                    bottomBar = {
                        ClearCoverBackupsScreenBottomBar(
                            selected = s.selection,
                            itemCount = s.items.size,
                            filter = s.filterNumberOfBackups,
                            sortCriteria = s.sortCriteria,
                            enableScrollToTop = enableScrollToTop,
                            enableScrollToBottom = enableScrollToBottom,
                            onDeleteClicked = model::showConfirmation,
                            onSelectAll = model::selectAll,
                            onInvertSelection = model::invertSelection,
                            navigateUp = navigator::pop,
                            scrollToTop = {
                                scope.launch {
                                    listState.scrollToItem(0)
                                }
                            },
                            scrollToBottom = {
                                scope.launch {
                                    listState.scrollToItem(s.items.size - 1)
                                }
                            },
                            onFilterClicked = model::showFilterDialog,
                            onSortClicked = model::showSortDialog,
                        )
                    },
                ) { paddingValues ->
                    if (s.items.isEmpty()) {
                        EmptyScreen(
                            textResource = R.string.info_empty_cover_backups,
                            modifier = Modifier.padding(paddingValues),
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .padding(paddingValues)
                                .fillMaxSize(),
                        ) {
                            FastScrollLazyColumn(
                                modifier = Modifier.weight(1f),
                                state = listState,
                            ) {
                                items(s.items) { backupWithMangaDetails ->
                                    ClearCoverBackupItem(
                                        modifier = Modifier.animateItemPlacement(),
                                        backupWithMangaDetails = backupWithMangaDetails,
                                        isSelected = s.selection.contains(backupWithMangaDetails.mangaId),
                                        onClick = { model.toggleSelection(backupWithMangaDetails) },
                                        onClickCover = {
                                            navigator.push(
                                                MangaScreen(
                                                    backupWithMangaDetails.mangaId,
                                                ),
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ClearCoverBackupItem(
        modifier: Modifier,
        backupWithMangaDetails: BackupWithMangaDetails,
        isSelected: Boolean,
        onClick: () -> Unit,
        onClickCover: (() -> Unit)?,
    ) {
        val haptic = LocalHapticFeedback.current

        Row(
            modifier = modifier
                .selectedBackground(isSelected)
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp)
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MangaCover.Square(
                modifier = Modifier
                    .padding(vertical = 6.dp)
                    .height(48.dp),
                data = backupWithMangaDetails.thumbnailUrl,
                onClick = onClickCover,
            )
            Column(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1f),
            ) {
                Text(
                    text = backupWithMangaDetails.mangaTitle,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(
                        R.string.label_count_backups,
                        backupWithMangaDetails.backups.size,
                    ),
                )
            }
            Checkbox(
                checked = isSelected,
                onCheckedChange = {
                    onClick()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
            )
        }
    }

    @Composable
    private fun FilterDialog(
        filter: Int,
        coverBackupSlots: Int,
        onFilter: (Int) -> Unit,
        onDismissRequest: () -> Unit,
    ) {
        val scope = rememberCoroutineScope()
        val (selection, setSelection) = remember { mutableStateOf(filter) }
        AlertDialog(
            onDismissRequest = onDismissRequest,
            confirmButton = {
                Row {
                    TextButton(
                        onClick = onDismissRequest,
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = {
                            scope.launchUI {
                                onFilter(0)
                                onDismissRequest()
                            }
                        },
                    ) {
                        Text(stringResource(R.string.action_reset))
                    }
                    TextButton(
                        onClick = {
                            scope.launchUI {
                                onFilter(selection)
                                onDismissRequest()
                            }
                        },
                    ) {
                        Text(stringResource(R.string.action_filter))
                    }
                }
            },
            title = {
                Text(stringResource(R.string.label_number_of_backups))
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
                    for (i in 1..coverBackupSlots) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (i == selection),
                                    onClick = { setSelection(i) },
                                    role = Role.RadioButton,
                                )
                                .padding(8.dp),
                        ) {
                            RadioButton(
                                selected = i == selection,
                                onClick = null,
                            )
                            Text(
                                text = ">= $i",
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
            },
        )
    }

    @Composable
    private fun SortDialog(
        initialCriteria: SortCriteria,
        initialDirection: SortDirection,
        sort: (SortCriteria, SortDirection) -> Unit,
        onDismissRequest: () -> Unit,
    ) {
        val scope = rememberCoroutineScope()
        val (criteria, setCriteria) = remember { mutableStateOf(initialCriteria) }
        val (direction, setDirection) = remember { mutableStateOf(initialDirection) }
        AlertDialog(
            onDismissRequest = onDismissRequest,
            confirmButton = {
                Row {
                    TextButton(
                        onClick = onDismissRequest,
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = {
                            scope.launchUI {
                                sort(criteria, direction)
                                onDismissRequest()
                            }
                        },
                    ) {
                        Text(stringResource(R.string.action_sort))
                    }
                }
            },
            title = {
                Text(stringResource(R.string.action_sort))
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
                    SortItem(
                        label = stringResource(id = R.string.action_sort_alpha),
                        sortDescending = if (criteria == SortCriteria.ALPHABETICAL) {
                            direction == SortDirection.DESCENDING
                        } else {
                            null
                        },
                        onClick = {
                            val isTogglingDirection = criteria == SortCriteria.ALPHABETICAL
                            val newDirection = when {
                                isTogglingDirection -> if (direction == SortDirection.DESCENDING) SortDirection.ASCENDING else SortDirection.DESCENDING
                                else -> if (direction == SortDirection.DESCENDING) SortDirection.DESCENDING else SortDirection.ASCENDING
                            }
                            setCriteria(SortCriteria.ALPHABETICAL)
                            setDirection(newDirection)
                        },
                    )
                    SortItem(
                        label = stringResource(id = R.string.label_number_of_backups),
                        sortDescending = if (criteria == SortCriteria.NUMBER_OF_BACKUPS) {
                            direction == SortDirection.DESCENDING
                        } else {
                            null
                        },
                        onClick = {
                            val isTogglingDirection = criteria == SortCriteria.NUMBER_OF_BACKUPS
                            val newDirection = when {
                                isTogglingDirection -> if (direction == SortDirection.DESCENDING) SortDirection.ASCENDING else SortDirection.DESCENDING
                                else -> if (direction == SortDirection.DESCENDING) SortDirection.DESCENDING else SortDirection.ASCENDING
                            }
                            setCriteria(SortCriteria.NUMBER_OF_BACKUPS)
                            setDirection(newDirection)
                        },
                    )
                }
            },
        )
    }

    @Composable
    private fun ClearCoverBackupsScreenAppBar(
        modifier: Modifier = Modifier,
        title: String,
        actionModeCounter: Int,
        scrollBehavior: TopAppBarScrollBehavior,
    ) {
        val isActionMode by remember(actionModeCounter) {
            derivedStateOf { actionModeCounter > 0 }
        }

        Column(
            modifier = modifier,
        ) {
            TopAppBar(
                title = {
                    if (isActionMode) {
                        AppBarTitle("$actionModeCounter selected")
                    } else {
                        AppBarTitle(title, null)
                    }
                },
                actions = {},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                        elevation = if (isActionMode) 3.dp else 0.dp,
                    ),
                ),
                scrollBehavior = scrollBehavior,
            )
        }
    }

    @Composable
    private fun ClearCoverBackupsScreenBottomBar(
        modifier: Modifier = Modifier,
        selected: List<Long>,
        itemCount: Int,
        sortCriteria: SortCriteria,
        filter: Int,
        enableScrollToTop: Boolean,
        enableScrollToBottom: Boolean,
        onDeleteClicked: (() -> Unit),
        onSelectAll: () -> Unit,
        onInvertSelection: () -> Unit,
        navigateUp: () -> Unit,
        scrollToTop: () -> Unit,
        scrollToBottom: () -> Unit,
        onFilterClicked: () -> Unit,
        onSortClicked: () -> Unit,
    ) {
        val scope = rememberCoroutineScope()
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.large.copy(
                bottomEnd = ZeroCornerSize,
                bottomStart = ZeroCornerSize,
            ),
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(
                elevation = 0.dp,
            ),
        ) {
            val haptic = LocalHapticFeedback.current
            val confirm = remember {
                mutableStateListOf(
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                )
            }
            var resetJob: Job? = remember { null }
            val onLongClickItem: (Int) -> Unit = { toConfirmIndex ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                (0 until 8).forEach { i -> confirm[i] = i == toConfirmIndex }
                resetJob?.cancel()
                resetJob = scope.launch {
                    delay(1.seconds)
                    if (isActive) confirm[toConfirmIndex] = false
                }
            }
            Column {
                Row(
                    modifier = Modifier
                        .padding(horizontal = MaterialTheme.padding.small),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                ) {
                    if (itemCount > 1 || filter != 0) {
                        FilterChip(
                            selected = filter != 0,
                            onClick = onFilterClicked,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.FilterList,
                                    contentDescription = "",
                                    modifier = Modifier
                                        .size(FilterChipDefaults.IconSize),
                                )
                            },
                            label = { Text(text = stringResource(id = R.string.action_filter)) },
                        )
                    }
                    if (itemCount > 1) {
                        FilterChip(
                            selected = true,
                            onClick = onSortClicked,
                            leadingIcon = {
                                if (sortCriteria == SortCriteria.ALPHABETICAL) {
                                    Icon(
                                        imageVector = Icons.Outlined.SortByAlpha,
                                        contentDescription = "",
                                        modifier = Modifier
                                            .size(FilterChipDefaults.IconSize),
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.Numbers,
                                        contentDescription = "",
                                        modifier = Modifier
                                            .size(FilterChipDefaults.IconSize),
                                    )
                                }
                            },
                            label = { Text(text = stringResource(id = R.string.action_sort)) },
                        )
                    }
                }
                Divider()
                Row(
                    modifier = Modifier
                        .padding(
                            WindowInsets.navigationBars
                                .only(WindowInsetsSides.Bottom)
                                .asPaddingValues(),
                        )
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                ) {
                    Button(
                        title = stringResource(R.string.abc_action_bar_up_description),
                        icon = Icons.Outlined.ArrowBack,
                        toConfirm = confirm[0],
                        onLongClick = { onLongClickItem(0) },
                        onClick = navigateUp,
                        enabled = true,
                    )
                    Button(
                        title = stringResource(R.string.action_select_all),
                        icon = Icons.Outlined.SelectAll,
                        toConfirm = confirm[1],
                        onLongClick = { onLongClickItem(1) },
                        onClick = if (selected.isEmpty() or (selected.size != itemCount)) {
                            onSelectAll
                        } else {
                            {}
                        },
                        enabled = selected.isEmpty() or (selected.size != itemCount),
                    )
                    Button(
                        title = stringResource(R.string.action_select_inverse),
                        icon = Icons.Outlined.FlipToBack,
                        toConfirm = confirm[2],
                        onLongClick = { onLongClickItem(2) },
                        onClick = if (selected.isNotEmpty()) {
                            onInvertSelection
                        } else {
                            {}
                        },
                        enabled = selected.isNotEmpty(),
                    )
                    Button(
                        title = stringResource(R.string.action_scroll_to_top),
                        icon = Icons.Outlined.ArrowUpward,
                        toConfirm = confirm[3],
                        onLongClick = { onLongClickItem(3) },
                        onClick = if (enableScrollToTop) {
                            scrollToTop
                        } else {
                            {}
                        },
                        enabled = enableScrollToTop,
                    )
                    Button(
                        title = stringResource(R.string.action_scroll_to_bottom),
                        icon = Icons.Outlined.ArrowDownward,
                        toConfirm = confirm[4],
                        onLongClick = { onLongClickItem(4) },
                        onClick = if (enableScrollToBottom) {
                            scrollToBottom
                        } else {
                            {}
                        },
                        enabled = enableScrollToBottom,
                    )
                    Button(
                        title = stringResource(R.string.action_delete),
                        icon = Icons.Outlined.Delete,
                        toConfirm = confirm[5],
                        onLongClick = { onLongClickItem(5) },
                        onClick = if (selected.isNotEmpty()) {
                            onDeleteClicked
                        } else {
                            {}
                        },
                        enabled = selected.isNotEmpty(),
                    )
                }
            }
        }
    }

    @Composable
    private fun RowScope.Button(
        title: String,
        icon: ImageVector,
        toConfirm: Boolean,
        enabled: Boolean,
        onLongClick: () -> Unit,
        onClick: (() -> Unit),
        content: (@Composable () -> Unit)? = null,
    ) {
        val animatedWeight by animateFloatAsState(
            if (toConfirm) 2f else 1f,
            label = "animatedWeightForBottomBarButton",
        )
        val animatedColor by animateColorAsState(
            if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(
                    alpha = 0.38f,
                )
            },
            label = "animatedColorForBottomBarButton",
        )
        Column(
            modifier = Modifier
                .size(48.dp)
                .weight(animatedWeight)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = false),
                    onLongClick = onLongClick,
                    onClick = onClick,
                ),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = animatedColor,
            )
            AnimatedVisibility(
                visible = toConfirm,
                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
            ) {
                Text(
                    text = title,
                    overflow = TextOverflow.Visible,
                    maxLines = 1,
                    style = MaterialTheme.typography.labelSmall,
                    color = animatedColor,
                )
            }
            content?.invoke()
        }
    }
}

private class ClearCoverBackupsScreenModel :
    StateScreenModel<ClearCoverBackupsScreenModel.State>(State.Loading) {

    private val backupPreferences: BackupPreferences = Injekt.get()
    private val getAllBackupManga: GetAllBackupManga = Injekt.get()
    private val deleteBackupsByMangaId: DeleteBackupsByMangaId = Injekt.get()
    private val coverCache: CoverCache = Injekt.get()

    init {
        coroutineScope.launchIO {
            getAllBackupManga.subscribe()
                .collectLatest { list ->
                    mutableState.update { old ->
                        when (old) {
                            State.Loading -> State.Ready(list)
                            is State.Ready -> old.copy(items = list)
                        }
                    }
                }
        }

        backupPreferences.clearCoverBackupFilterCriteria().changes()
            .onEach {
                mutableState.update { state ->
                    if (state !is State.Ready) return@update state
                    state.copy(
                        filterNumberOfBackups = it,
                    )
                }
            }
            .launchIn(coroutineScope)

        backupPreferences.clearCoverBackupSortCriteria().changes()
            .onEach {
                mutableState.update { state ->
                    if (state !is State.Ready) return@update state
                    state.copy(
                        sortCriteria = SortCriteria.valueOf(it),
                    )
                }
            }
            .launchIn(coroutineScope)

        backupPreferences.clearCoverBackupSortDirection().changes()
            .onEach {
                mutableState.update { state ->
                    if (state !is State.Ready) return@update state
                    state.copy(
                        sortDirection = SortDirection.valueOf(it),
                    )
                }
            }
            .launchIn(coroutineScope)
    }

    suspend fun removeCoverBackups() {
        val state = state.value as? State.Ready ?: return
        deleteBackupsByMangaId.await(state.selection)
        state.selection.forEach { mangaId ->
            coverCache.deleteFromCache(state.items.find { it.mangaId == mangaId }?.thumbnailUrl)
        }
    }

    fun filterItems(limit: Int) {
        backupPreferences.clearCoverBackupFilterCriteria().set(limit)
    }

    fun sortItems(sortCriteria: SortCriteria, sortDirection: SortDirection) {
        backupPreferences.clearCoverBackupSortDirection().set(sortDirection.name)
        backupPreferences.clearCoverBackupSortCriteria().set(sortCriteria.name)
    }

    fun toggleSelection(backupWithMangaDetails: BackupWithMangaDetails) =
        mutableState.update { state ->
            if (state !is State.Ready) return@update state
            val mutableList = state.selection.toMutableList()
            if (mutableList.contains(backupWithMangaDetails.mangaId)) {
                mutableList.remove(backupWithMangaDetails.mangaId)
            } else {
                mutableList.add(backupWithMangaDetails.mangaId)
            }
            state.copy(selection = mutableList)
        }

    fun clearSelection() = mutableState.update { state ->
        if (state !is State.Ready) return@update state
        state.copy(selection = emptyList())
    }

    fun selectAll() = mutableState.update { state ->
        if (state !is State.Ready) return@update state
        state.copy(selection = state.items.fastMap { it.mangaId })
    }

    fun invertSelection() = mutableState.update { state ->
        if (state !is State.Ready) return@update state
        state.copy(
            selection = state.items
                .fastMap { it.mangaId }
                .filterNot { it in state.selection },
        )
    }

    fun showConfirmation() = mutableState.update { state ->
        if (state !is State.Ready) return@update state
        state.copy(showConfirmation = true)
    }

    fun hideConfirmation() = mutableState.update { state ->
        if (state !is State.Ready) return@update state
        state.copy(showConfirmation = false)
    }

    fun showFilterDialog() = mutableState.update { state ->
        if (state !is State.Ready) return@update state
        state.copy(showFilterDialog = true)
    }

    fun hideFilterDialog() = mutableState.update { state ->
        if (state !is State.Ready) return@update state
        state.copy(showFilterDialog = false)
    }

    fun showSortDialog() = mutableState.update { state ->
        if (state !is State.Ready) return@update state
        state.copy(showSortDialog = true)
    }

    fun hideSortDialog() = mutableState.update { state ->
        if (state !is State.Ready) return@update state
        state.copy(showSortDialog = false)
    }

    sealed class State {
        object Loading : State()

        data class Ready(
            val items: List<BackupWithMangaDetails>,
            val selection: List<Long> = emptyList(),
            val filterNumberOfBackups: Int = 0,
            val sortCriteria: SortCriteria = SortCriteria.ALPHABETICAL,
            val sortDirection: SortDirection = SortDirection.ASCENDING,
            val showConfirmation: Boolean = false,
            val showFilterDialog: Boolean = false,
            val showSortDialog: Boolean = false,
        ) : State()
    }
}

enum class SortDirection {
    ASCENDING, DESCENDING
}

enum class SortCriteria {
    ALPHABETICAL, NUMBER_OF_BACKUPS
}
