package eu.kanade.tachiyomi.ui.browse.migration.advanced.design

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FindReplace
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.ToggleOn
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.PreMigrationListBinding
import eu.kanade.tachiyomi.ui.browse.migration.advanced.process.MigrationListScreen
import eu.kanade.tachiyomi.ui.browse.migration.advanced.process.MigrationProcedureConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import tachiyomi.presentation.core.components.material.Scaffold
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

class PreMigrationScreen(val mangaIds: List<Long>) : Screen {

    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { PreMigrationScreenModel() }
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
        val navigator = LocalNavigator.currentOrThrow
        val items by screenModel.state.collectAsState()
        val context = LocalContext.current
        DisposableEffect(screenModel) {
            screenModel.dialog = MigrationBottomSheetDialog(context, screenModel.listener)
            onDispose {}
        }

        LaunchedEffect(screenModel) {
            screenModel.startMigration.collect { extraParam ->
                navigator replace MigrationListScreen(MigrationProcedureConfig(mangaIds, extraParam))
            }
        }

        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(R.string.select_sources),
                    scrollBehavior = scrollBehavior,
                )
            },
            bottomBar = {
                PreMigrationScreenBottomBar(
                    modifier = Modifier,
                    onSelectAll = { screenModel.massSelect(true) },
                    onSelectNone = { screenModel.massSelect(false) },
                    onSelectPinned = { screenModel.matchSelection(false) },
                    onSelectEnabled = { screenModel.matchSelection(true) },
                    onMigrateClick = {
                        if (!screenModel.dialog.isShowing) {
                            screenModel.dialog.show()
                        }
                    },
                    navigateUp = navigator::pop,
                )
            },
        ) { contentPadding ->
            val density = LocalDensity.current
            val layoutDirection = LocalLayoutDirection.current
            val left = with(density) { contentPadding.calculateLeftPadding(layoutDirection).toPx().roundToInt() }
            val top = with(density) { contentPadding.calculateTopPadding().toPx().roundToInt() }
            val right = with(density) { contentPadding.calculateRightPadding(layoutDirection).toPx().roundToInt() }
            val bottom = with(density) { contentPadding.calculateBottomPadding().toPx().roundToInt() }
            Box(modifier = Modifier) {
                AndroidView(
                    factory = { context ->
                        screenModel.controllerBinding = PreMigrationListBinding.inflate(LayoutInflater.from(context))
                        screenModel.adapter = MigrationSourceAdapter(screenModel.clickListener)
                        screenModel.controllerBinding.recycler.adapter = screenModel.adapter
                        screenModel.adapter?.isHandleDragEnabled = true
                        screenModel.adapter?.fastScroller = screenModel.controllerBinding.fastScroller
                        screenModel.controllerBinding.recycler.layoutManager = LinearLayoutManager(context)

                        ViewCompat.setNestedScrollingEnabled(screenModel.controllerBinding.root, true)

                        screenModel.controllerBinding.root
                    },
                    update = {
                        screenModel.controllerBinding.recycler
                            .updatePadding(
                                left = left,
                                top = top,
                                right = right,
                                bottom = bottom,
                            )

                        screenModel.controllerBinding.fastScroller
                            .updateLayoutParams<ViewGroup.MarginLayoutParams> {
                                leftMargin = left
                                topMargin = top
                                rightMargin = right
                                bottomMargin = bottom
                            }

                        screenModel.adapter?.updateDataSet(items)
                    },
                )
            }
        }
    }

    @Composable
    fun PreMigrationScreenBottomBar(
        modifier: Modifier,
        onSelectAll: () -> Unit,
        onSelectNone: () -> Unit,
        onSelectPinned: () -> Unit,
        onSelectEnabled: () -> Unit,
        onMigrateClick: () -> Unit,
        navigateUp: () -> Unit,
    ) {
        val scope = rememberCoroutineScope()
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.large.copy(
                bottomEnd = ZeroCornerSize,
                bottomStart = ZeroCornerSize,
            ),
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(elevation = 0.dp),
        ) {
            val haptic = LocalHapticFeedback.current
            val confirm = remember { mutableStateListOf(false, false, false, false, false, false) }
            var resetJob: Job? = remember { null }
            val onLongClickItem: (Int) -> Unit = { toConfirmIndex ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                (0 until 6).forEach { i -> confirm[i] = i == toConfirmIndex }
                resetJob?.cancel()
                resetJob = scope.launch {
                    delay(1.seconds)
                    if (isActive) confirm[toConfirmIndex] = false
                }
            }
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
                )
                Button(
                    title = stringResource(R.string.action_select_all),
                    icon = Icons.Outlined.SelectAll,
                    toConfirm = confirm[1],
                    onLongClick = { onLongClickItem(1) },
                    onClick = onSelectAll,
                )
                Button(
                    title = stringResource(R.string.deselect_all),
                    icon = Icons.Outlined.FlipToBack,
                    toConfirm = confirm[2],
                    onLongClick = { onLongClickItem(2) },
                    onClick = onSelectNone,
                )
                Button(
                    title = stringResource(R.string.match_enabled_sources),
                    icon = Icons.Outlined.ToggleOn,
                    toConfirm = confirm[3],
                    onLongClick = { onLongClickItem(3) },
                    onClick = onSelectEnabled,
                )
                Button(
                    title = stringResource(R.string.match_pinned_sources),
                    icon = Icons.Outlined.PushPin,
                    toConfirm = confirm[4],
                    onLongClick = { onLongClickItem(4) },
                    onClick = onSelectPinned,
                )
                Button(
                    title = stringResource(R.string.migrate),
                    icon = Icons.Outlined.FindReplace,
                    toConfirm = confirm[5],
                    onLongClick = { onLongClickItem(5) },
                    onClick = onMigrateClick,
                )
            }
        }
    }

    @Composable
    private fun RowScope.Button(
        title: String,
        icon: ImageVector,
        toConfirm: Boolean,
        onLongClick: () -> Unit,
        onClick: (() -> Unit),
        content: (@Composable () -> Unit)? = null,
    ) {
        val animatedWeight by animateFloatAsState(if (toConfirm) 2f else 1f)
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
            )
            AnimatedVisibility(
                visible = toConfirm,
                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
            ) {
                Text(
                    text = title,
                    overflow = TextOverflow.Visible,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            content?.invoke()
        }
    }

    companion object {
        fun navigateToMigration(skipPre: Boolean, navigator: Navigator, mangaIds: List<Long>) {
            navigator.push(
                if (skipPre) {
                    MigrationListScreen(
                        MigrationProcedureConfig(mangaIds, null),
                    )
                } else {
                    PreMigrationScreen(mangaIds)
                },
            )
        }
    }
}
