package eu.kanade.presentation.manga

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import eu.kanade.presentation.util.rememberResourceBitmapPainter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.manga.CoverBackupScreenState
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import tachiyomi.core.util.lang.launchUI
import tachiyomi.presentation.core.components.HorizontalPager
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.TextButton
import tachiyomi.presentation.core.components.rememberPagerState
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import kotlin.time.Duration.Companion.seconds

@Composable
fun CoverBackupScreen(
    state: CoverBackupScreenState,
    navigateUp: () -> Unit,
    onClickRestore: ((Boolean) -> Unit),
    onClickDelete: (() -> Unit),
    onPageChange: ((Int) -> Unit),
    showDeleteConfirmation: (() -> Unit),
    hideDeleteConfirmation: (() -> Unit),
    showBackupCurrentDialog: (() -> Unit),
    hideBackupCurrentDialog: (() -> Unit),
) {
    val pagerState = rememberPagerState()
    var height by remember {
        mutableStateOf(0)
    }
    var width by remember {
        mutableStateOf(0)
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            onPageChange(page)
        }
    }

    Scaffold(
        bottomBar = {
            CoverBackupScreenBottomBar(
                currentPage = pagerState.currentPage + 1,
                totalPages = state.items.size,
                height = height,
                width = width,
                enableActionButtons = state.items.isNotEmpty(),
                navigateUp = navigateUp,
                onClickRestore = showBackupCurrentDialog,
                onClickDelete = showDeleteConfirmation,
            )
        },
    ) { paddingValues ->
        when {
            state.isLoading -> LoadingScreen(modifier = Modifier.padding(paddingValues))
            state.items.isEmpty() -> EmptyScreen(
                textResource = R.string.info_empty_cover_backups,
                modifier = Modifier.padding(paddingValues),
            )

            else -> {
                if (state.showDeleteConfirmation) {
                    DeleteConfirmDialog(
                        hideDeleteConfirmation = hideDeleteConfirmation,
                        onClickDelete = onClickDelete,
                    )
                }
                if (state.showBackupCurrentDialog) {
                    BackupCurrentDialog(
                        hideBackupCurrentDialog = hideBackupCurrentDialog,
                        onClickRestore = onClickRestore,
                    )
                }
                HorizontalPager(
                    count = state.items.size,
                    state = pagerState,
                ) {
                    Box(
                        modifier = Modifier
                            .padding(10.dp)
                            .fillMaxSize(),
                    ) {
                        AsyncImage(
                            model = state.items[it].thumbnailUrl,
                            placeholder = ColorPainter(Color(0x1F888888)),
                            contentDescription = state.items[it].thumbnailUrl,
                            error = rememberResourceBitmapPainter(id = R.drawable.cover_error),
                            contentScale = ContentScale.Fit,
                            onSuccess = { result ->
                                height = result.result.drawable.toBitmap().height
                                width = result.result.drawable.toBitmap().width
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteConfirmDialog(
    hideDeleteConfirmation: (() -> Unit),
    onClickDelete: (() -> Unit),
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = hideDeleteConfirmation,
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launchUI {
                        onClickDelete()
                        hideDeleteConfirmation()
                        context.toast(R.string.delete_cover_toast)
                    }
                },
            ) {
                Text(
                    text = stringResource(R.string.dialog_restore_cover_yes),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = hideDeleteConfirmation,
            ) {
                Text(
                    text = stringResource(R.string.action_cancel),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        },
        text = {
            Text(
                text = stringResource(R.string.dialog_delete_cover_confirm),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelMedium,
            )
        },
    )
}

@Composable
private fun BackupCurrentDialog(
    hideBackupCurrentDialog: (() -> Unit),
    onClickRestore: ((Boolean) -> Unit),
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = hideBackupCurrentDialog,
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launchUI {
                        onClickRestore(false)
                        hideBackupCurrentDialog()
                        context.toast(R.string.restore_cover_toast)
                    }
                },
            ) {
                Text(
                    text = stringResource(R.string.dialog_restore_cover_no),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            TextButton(
                onClick = {
                    scope.launchUI {
                        onClickRestore(true)
                        hideBackupCurrentDialog()
                        context.toast(R.string.restore_cover_toast)
                    }
                },
            ) {
                Text(
                    text = stringResource(R.string.dialog_restore_cover_yes),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = hideBackupCurrentDialog,
            ) {
                Text(
                    text = stringResource(R.string.action_cancel),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        },
        text = {
            Text(
                text = stringResource(R.string.dialog_restore_cover_confirm),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelMedium,
            )
        },
    )
}

@Composable
private fun CoverBackupScreenBottomBar(
    modifier: Modifier = Modifier,
    currentPage: Int,
    totalPages: Int,
    height: Int,
    width: Int,
    enableActionButtons: Boolean,
    navigateUp: () -> Unit,
    onClickRestore: (() -> Unit),
    onClickDelete: (() -> Unit),
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val confirm = remember { mutableStateListOf(false, false, false) }
    var resetJob: Job? = remember { null }
    val onLongClickItem: (Int) -> Unit = { toConfirmIndex ->
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        (0 until 3).forEach { i -> confirm[i] = i == toConfirmIndex }
        resetJob?.cancel()
        resetJob = scope.launch {
            delay(1.seconds)
            if (isActive) confirm[toConfirmIndex] = false
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(horizontal = 4.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Text(
                text = "$currentPage/$totalPages",
                overflow = TextOverflow.Visible,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelSmall,
            )
            Text(
                text = "${width}px x ${height}px",
                overflow = TextOverflow.Visible,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.large.copy(
                bottomEnd = ZeroCornerSize,
                bottomStart = ZeroCornerSize,
            ),
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
        ) {
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
                    onClick = navigateUp,
                    toConfirm = confirm[0],
                    onLongClick = { onLongClickItem(0) },
                    enabled = true,
                )
                Button(
                    title = stringResource(R.string.cover_backup_button),
                    icon = Icons.Outlined.Check,
                    onClick = onClickRestore,
                    toConfirm = confirm[1],
                    onLongClick = { onLongClickItem(1) },
                    enabled = enableActionButtons,
                )
                Button(
                    title = stringResource(R.string.delete_cover_backup_button),
                    icon = Icons.Outlined.Delete,
                    onClick = onClickDelete,
                    toConfirm = confirm[2],
                    onLongClick = { onLongClickItem(2) },
                    enabled = enableActionButtons,
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
) {
    val animatedWeight by animateFloatAsState(if (toConfirm) 2f else 1f)
    val animatedColor by animateColorAsState(
        if (enabled) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurface.copy(
                alpha = 0.38f,
            )
        },
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
    }
}
