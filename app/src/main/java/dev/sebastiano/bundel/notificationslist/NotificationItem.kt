@file:OptIn(ExperimentalMaterialApi::class)

package dev.sebastiano.bundel.notificationslist

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Card
import androidx.compose.material.ContentAlpha
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import dev.sebastiano.bundel.R
import dev.sebastiano.bundel.notifications.ActiveNotification
import dev.sebastiano.bundel.notifications.PersistableNotification
import dev.sebastiano.bundel.storage.ImagesStorage
import dev.sebastiano.bundel.ui.BundelTheme
import dev.sebastiano.bundel.ui.iconSize
import dev.sebastiano.bundel.ui.notificationSnoozeBackground
import dev.sebastiano.bundel.ui.notificationSnoozeForeground
import dev.sebastiano.bundel.ui.singlePadding
import dev.sebastiano.bundel.util.asImageBitmap
import dev.sebastiano.bundel.util.rememberIconPainter
import kotlinx.coroutines.launch
import java.io.File
import android.graphics.drawable.Icon as GraphicsIcon

private fun previewNotification(context: Context) = ActiveNotification(
    persistableNotification = PersistableNotification(
        id = 1234,
        key = "1234",
        timestamp = 12345678L,
        showTimestamp = true,
        text = "Hello I'm Ivan and I'm here to complain about things.",
        title = "Ivan Morgillo",
        appInfo = PersistableNotification.SenderAppInfo(
            "com.yeah",
            "Yeah! messenger",
            GraphicsIcon.createWithResource(context, R.drawable.ic_whatever_24dp)
        ),
    ),
    interactions = ActiveNotification.Interactions(
        actions = listOf(
            ActiveNotification.Interactions.ActionItem(text = "Mai una gioia"),
            ActiveNotification.Interactions.ActionItem(text = "Mai una gioia"),
            ActiveNotification.Interactions.ActionItem(text = "Mai una gioia"),
            ActiveNotification.Interactions.ActionItem(text = "Mai una gioia"),
        )
    ),
    isSnoozed = false
)

@Preview
@Composable
private fun NotificationItemLightPreview() {
    BundelTheme {
        NotificationItem(previewNotification(LocalContext.current))
    }
}

@Preview
@Composable
private fun NotificationItemDarkPreview() {
    BundelTheme(darkModeOverride = true) {
        NotificationItem(previewNotification(LocalContext.current))
    }
}

@Composable
internal fun NotificationItem(
    activeNotification: ActiveNotification,
    onNotificationDismiss: (ActiveNotification) -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = activeNotification.ifClickable { onNotificationDismiss(activeNotification) }
    ) {
        Column(Modifier.padding(singlePadding())) {
            NotificationMetadata(activeNotification.persistableNotification)
            NotificationContent(
                notification = activeNotification.persistableNotification,
                iconPainter = rememberIconPainter(activeNotification.icons.large ?: activeNotification.icons.small),
                interactions = activeNotification.interactions
            )
        }
    }
}

@Composable
internal fun SwipeableNotificationItem(
    activeNotification: ActiveNotification,
    onNotificationDismiss: (ActiveNotification) -> Unit
) {
    var hasSnoozed by remember { mutableStateOf(false) }
    var hasTriedToSnooze by remember { mutableStateOf(false) }

    val dismissState = rememberDismissState(
        confirmStateChange = {
            when (it) {
                DismissValue.DismissedToEnd -> {
                    hasTriedToSnooze = !hasTriedToSnooze
                    hasSnoozed
                }
                DismissValue.DismissedToStart -> {
                    hasTriedToSnooze = false
                    false
                }
                else -> false
            }
        },
    )

    // TODO disable the item when it's snoozed
    val itemAlpha by animateFloatAsState(
        if (activeNotification.isSnoozed) ContentAlpha.disabled else 1f
    )

    val coroutineScope = rememberCoroutineScope()
    val contentOffset = if (hasTriedToSnooze) 48.dp else 0.dp
    SwipeToDismiss(
        modifier = Modifier.alpha(itemAlpha),
        state = dismissState,
        background = {
            SnoozeBackground(
                onSnoozeClicked = {
                    hasSnoozed = true

                    coroutineScope.launch {
                        dismissState.reset()
                        hasTriedToSnooze = false
                        hasSnoozed = false
                    }

                    onNotificationDismiss(activeNotification)
                }
            )
        },
        dismissThresholds = { direction ->
            val threshold = when (direction) {
                DismissDirection.StartToEnd -> 0.25F
                else -> 0F
            }

            FractionalThreshold(threshold)
        },
        directions = setOf(DismissDirection.StartToEnd),
    ) {
        val animatedOffset by animateDpAsState(targetValue = contentOffset)
        Box(
            modifier = Modifier.offset(x = animatedOffset)
        ) {
            NotificationItem(
                activeNotification = activeNotification,
                onNotificationDismiss = onNotificationDismiss
            )
        }
    }
}

@Composable
private fun SnoozeBackground(
    onSnoozeClicked: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                color = MaterialTheme.colors.notificationSnoozeBackground,
                shape = MaterialTheme.shapes.medium,
            ),
    ) {
        IconButton(
            onClick = onSnoozeClicked,
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            Icon(
                Icons.Default.Timer,
                contentDescription = "Snooze",
                tint = MaterialTheme.colors.notificationSnoozeForeground,
            )
        }
    }
}

private fun ActiveNotification.ifClickable(onClick: (ActiveNotification) -> Unit) =
    if (isClickable()) {
        { onClick(this) }
    } else {
        {} // No-op
    }

@OptIn(ExperimentalCoilApi::class)
@Composable
internal fun NotificationItem(
    persistableNotification: PersistableNotification,
    imagesStorage: ImagesStorage,
    isLastItem: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .run { if (!isLastItem) padding(bottom = singlePadding()) else this }
    ) {
        Column(Modifier.padding(singlePadding())) {
            NotificationMetadata(persistableNotification)

            val iconPainter = rememberImagePainter(
                data = getIconFile(imagesStorage, persistableNotification)
            )
            NotificationContent(persistableNotification, iconPainter, interactions = null)
        }
    }
}

@Composable
private fun getIconFile(
    imagesStorage: ImagesStorage,
    persistableNotification: PersistableNotification
) =
    File(imagesStorage.getIconPath(persistableNotification.uniqueId, ImagesStorage.NotificationIconSize.LARGE))
        .takeIf { it.exists() }
        ?: File(imagesStorage.getIconPath(persistableNotification.uniqueId, ImagesStorage.NotificationIconSize.SMALL))

@Composable
private fun NotificationMetadata(notification: PersistableNotification) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = singlePadding()),
        verticalAlignment = Alignment.Bottom
    ) {
        val appIcon = notification.appInfo.icon?.asImageBitmap()
        val appName = notification.appInfo.name ?: notification.appInfo.packageName
        if (appIcon != null) {
            Image(appIcon, stringResource(id = R.string.app_icon_content_description, appName), Modifier.size(16.dp))
            Spacer(modifier = Modifier.size(singlePadding()))
        }
        CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.caption) {
            Text(appName, Modifier.weight(1F, fill = false), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (notification.showTimestamp) Timestamp(notification)
        }
    }
}

@Composable
private fun Timestamp(notification: PersistableNotification) {
    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
        Text(text = " · ")
        Text(
            DateUtils.getRelativeTimeSpanString(
                notification.timestamp,
                System.currentTimeMillis(),
                DateUtils.SECOND_IN_MILLIS
            ).toString()
        )
    }
}

@Composable
private fun NotificationContent(
    notification: PersistableNotification,
    iconPainter: Painter?,
    interactions: ActiveNotification.Interactions?
) {
    Row(Modifier.fillMaxWidth()) {
        if (iconPainter != null) { // TODO reevaluate nullability
            Image(iconPainter, stringResource(R.string.notification_icon_content_description), modifier = Modifier.size(iconSize()))
        } else {
            Image(
                Icons.Rounded.BrokenImage,
                stringResource(R.string.notification_no_icon_content_description),
                modifier = Modifier.size(iconSize()),
                colorFilter = ColorFilter.tint(MaterialTheme.colors.onSurface)
            )
        }
        Column {
            if (notification.title != null) {
                Text(
                    text = notification.title.trim(),
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = singlePadding())
                )
            }

            if (notification.text != null) {
                Text(
                    text = notification.text.trim(),
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.padding(start = singlePadding())
                )
            }

            if (interactions != null) NotificationActions(interactions)
        }
    }
}

@Composable
private fun NotificationActions(interactions: ActiveNotification.Interactions) {
    if (interactions.actions.isEmpty()) return

    Spacer(modifier = Modifier.height(singlePadding()))
    val scrollState = rememberScrollState()
    Row(Modifier.horizontalScroll(scrollState)) {
        val items = interactions.actions.take(3)
        for ((index, action) in items.withIndex()) {
            TextButton(onClick = { action.pendingIntent?.send() }) {
                Text(action.text.trim().toString())
            }
            if (index < items.size - 1) {
                Spacer(modifier = Modifier.size(singlePadding()))
            }
        }
    }
}
