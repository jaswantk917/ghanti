package io.github.jaswantk917.ghanti.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallMissed
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeliveryDining
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.jaswantk917.ghanti.data.Contact
import io.github.jaswantk917.ghanti.data.HomeState
import io.github.jaswantk917.ghanti.data.Kind
import io.github.jaswantk917.ghanti.data.RecentRow
import io.github.jaswantk917.ghanti.signals.DeliveryRules
import io.github.jaswantk917.ghanti.signals.Signal
import io.github.jaswantk917.ghanti.signals.Strength
import io.github.jaswantk917.ghanti.ui.components.Avatar
import io.github.jaswantk917.ghanti.ui.components.AvatarHint
import io.github.jaswantk917.ghanti.ui.components.pressMorph
import io.github.jaswantk917.ghanti.ui.util.Haptic
import io.github.jaswantk917.ghanti.ui.util.clockTime
import io.github.jaswantk917.ghanti.ui.util.dayLabel
import io.github.jaswantk917.ghanti.ui.util.edgeBlur
import io.github.jaswantk917.ghanti.ui.util.pressScale
import io.github.jaswantk917.ghanti.ui.util.rememberHaptics
import io.github.jaswantk917.ghanti.ui.util.sameDay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

interface HomeActions {
    fun call(number: String)
    fun message(number: String)
    fun copy(number: String)
    fun addContact(number: String)
    fun delete(row: RecentRow)
    fun keypad()
    fun settings()
    fun expecting()
    fun makeDefault()
    fun allowNotifications()
    fun recentsSeen()
}

/** [screensCalls]: Android asks the "Caller ID & spam app" whether a call rings, a separate role from the phone app */
data class Setup(val isPhoneApp: Boolean, val screensCalls: Boolean, val readsNotifications: Boolean, val silencing: Boolean) {
    val ready get() = isPhoneApp && screensCalls
}

private val tabs = listOf("Recents", "Contacts")

@Composable
fun HomeScreen(state: HomeState, setup: Setup, expecting: List<Signal>, actions: HomeActions) {
    val pager = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()
    val density = LocalDensity.current
    var headerHeight by remember { mutableStateOf(150.dp) }
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val recentsList = rememberLazyListState()
    val contactsList = rememberLazyListState()
    val current = if (pager.currentPage == 0) recentsList else contactsList
    val expandedFab by remember(current) { derivedStateOf { current.firstVisibleItemIndex == 0 || !current.lastScrolledForward } }

    LaunchedEffect(pager) {
        snapshotFlow { pager.currentPage }.distinctUntilChanged().drop(1).collect { haptics.play(Haptic.Segment) }
    }
    LaunchedEffect(pager.currentPage, state.recents.size) { if (pager.currentPage == 0) actions.recentsSeen() }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        if (!state.loaded) {
            LoadingIndicator(Modifier.align(Alignment.Center))
        } else {
            HorizontalPager(
                state = pager,
                beyondViewportPageCount = 1,
                modifier = Modifier.fillMaxSize().edgeBlur(top = headerHeight, bottom = navBottom + 24.dp),
            ) { page ->
                val padding = PaddingValues(top = headerHeight + 4.dp, bottom = navBottom + 112.dp)
                if (page == 0) {
                    RecentsPage(state.recents, setup, recentsList, padding, actions)
                } else {
                    ContactsPage(state.contacts, contactsList, padding, actions)
                }
            }
        }

        Header(
            selected = pager.currentPage,
            setup = setup,
            expecting = expecting,
            onSelect = { scope.launch { pager.animateScrollToPage(it) } },
            actions = actions,
            modifier = Modifier.onSizeChanged { headerHeight = with(density) { it.height.toDp() } },
        )
        val surface = MaterialTheme.colorScheme.surface
        Box(
            Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars)
                .background(Brush.verticalGradient(listOf(surface.copy(alpha = 0.75f), surface.copy(alpha = 0f)))),
        )

        ExtendedFloatingActionButton(
            onClick = { haptics.play(Haptic.Tap); actions.keypad() },
            expanded = expandedFab,
            icon = { Icon(Icons.Rounded.Dialpad, null) },
            text = { Text("Keypad") },
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(20.dp),
        )
    }
}

@Composable
private fun Header(
    selected: Int,
    setup: Setup,
    expecting: List<Signal>,
    onSelect: (Int) -> Unit,
    actions: HomeActions,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    val surface = MaterialTheme.colorScheme.surface
    Column(
        modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(0f to surface.copy(alpha = 0.85f), 0.65f to surface.copy(alpha = 0.65f), 1f to surface.copy(alpha = 0f)))
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(bottom = 10.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 8.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Ghanti", style = MaterialTheme.typography.displaySmallEmphasized)
                StatusLine(setup, expecting, onClick = {
                    haptics.play(Haptic.Tap)
                    if (setup.ready) actions.expecting() else actions.makeDefault()
                })
            }
            val press = remember { MutableInteractionSource() }
            FilledTonalIconButton(
                onClick = { haptics.play(Haptic.Tap); actions.settings() },
                shape = pressMorph(MaterialShapes.Clover4Leaf, MaterialShapes.Cookie4Sided, press),
                interactionSource = press,
                modifier = Modifier.size(48.dp),
            ) { Icon(Icons.Rounded.Settings, "Settings") }
        }
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            tabs.forEachIndexed { i, label ->
                ToggleButton(checked = i == selected, onCheckedChange = { onSelect(i) }) {
                    Text(label, style = MaterialTheme.typography.labelLargeEmphasized)
                }
            }
        }
    }
}

/** one line that says what Ghanti will do with the next stranger's call; tapping it explains why */
@Composable
private fun StatusLine(setup: Setup, expecting: List<Signal>, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    fun names(list: List<Signal>) = list.sortedByDescending { it.at }.map { if (it.brand == DeliveryRules.DELIVERY) "A delivery" else it.brand }.distinct().joinToString(", ")
    val live = expecting.filter { it.strength == Strength.Live }
    val (icon, text, tint) = when {
        !setup.isPhoneApp -> Triple(Icons.Rounded.Call, "Tap to make Ghanti your phone app", scheme.primary)
        !setup.screensCalls -> Triple(Icons.Rounded.NotificationsOff, "Tap to let Ghanti silence strangers", scheme.primary)
        !setup.silencing -> Triple(Icons.Rounded.NotificationsActive, "Everyone rings", scheme.onSurfaceVariant)
        live.isNotEmpty() -> Triple(Icons.Rounded.DeliveryDining, "${names(live)} on the way · strangers ring", scheme.tertiary)
        expecting.isNotEmpty() -> Triple(Icons.Rounded.DeliveryDining, "${names(expecting)} due today · strangers ring", scheme.tertiary)
        else -> Triple(Icons.Rounded.NotificationsOff, "Strangers ring silently", scheme.onSurfaceVariant)
    }
    AnimatedContent(text, label = "status") { shown ->
        Row(
            Modifier.clip(CircleShape).combinedClickable(onClick = onClick).padding(vertical = 2.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(shown, style = MaterialTheme.typography.labelLarge, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun RecentsPage(
    recents: List<RecentRow>,
    setup: Setup,
    list: androidx.compose.foundation.lazy.LazyListState,
    padding: PaddingValues,
    actions: HomeActions,
) {
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    LazyColumn(state = list, contentPadding = padding, modifier = Modifier.fillMaxSize()) {
        if (!setup.isPhoneApp) {
            item(key = "setup-phone") {
                SetupCard(
                    "Make Ghanti your phone app",
                    "Strangers stop ringing out loud, unless a delivery is on its way.",
                    "Switch", actions::makeDefault, Modifier.animateItem(),
                )
            }
        } else if (!setup.screensCalls) {
            item(key = "setup-screening") {
                SetupCard(
                    "Let Ghanti silence strangers",
                    "Android asks its \"Caller ID & spam app\" which calls ring. Make that Ghanti too.",
                    "Allow", actions::makeDefault, Modifier.animateItem(),
                )
            }
        } else if (!setup.readsNotifications) {
            item(key = "setup-listener") {
                SetupCard(
                    "Let delivery calls through",
                    "Ghanti looks at notifications for words like \"on the way\", so the rider's call rings. Nothing leaves this phone.",
                    "Allow", actions::allowNotifications, Modifier.animateItem(),
                )
            }
        }
        if (recents.isEmpty()) {
            item(key = "empty") { Empty("No calls yet", Modifier.animateItem()) }
        }
        recents.forEachIndexed { i, row ->
            if (i == 0 || !sameDay(recents[i - 1].group.latest.date, row.group.latest.date)) {
                item(key = "day-${row.group.key}") { DayHeader(dayLabel(row.group.latest.date), Modifier.animateItem()) }
            }
            item(key = row.group.key) {
                RecentItem(
                    row = row,
                    open = open == row.group.key,
                    onToggle = { open = if (open == row.group.key) null else row.group.key },
                    actions = actions,
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
private fun RecentItem(row: RecentRow, open: Boolean, onToggle: () -> Unit, actions: HomeActions, modifier: Modifier = Modifier) {
    val haptics = rememberHaptics()
    val scheme = MaterialTheme.colorScheme
    val latest = row.group.latest
    val silenced = row.outcome?.rang == false && latest.kind == Kind.Missed
    val missed = latest.kind == Kind.Missed && !silenced
    val interaction = remember { MutableInteractionSource() }
    val guess = row.outcome?.guess
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (open) scheme.surfaceContainer else Color.Transparent),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .pressScale(interaction, 0.98f)
                .combinedClickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = { haptics.play(Haptic.Tap); onToggle() },
                    onLongClick = { if (row.group.number.isNotBlank()) { haptics.play(Haptic.Copy); actions.copy(row.group.number) } },
                )
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(
                name = row.name, photo = row.photo, seed = row.group.number, size = 48.dp,
                hint = guess?.let { if (it.ride) AvatarHint.Ride else AvatarHint.Delivery } ?: AvatarHint.None,
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        row.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (missed) scheme.error else scheme.onSurface,
                        fontWeight = if (missed && latest.isNew) FontWeight.Bold else null,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (row.group.entries.size > 1) {
                        Text("  ${row.group.entries.size}", style = MaterialTheme.typography.titleMedium, color = scheme.onSurfaceVariant)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (icon, label) = kindOf(latest.kind, silenced)
                    Icon(icon, null, tint = if (missed) scheme.error else scheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        listOfNotNull(guess?.title ?: label, clockTime(latest.date)).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (guess != null) scheme.tertiary else scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (row.group.number.isNotBlank()) {
                val press = remember { MutableInteractionSource() }
                FilledTonalIconButton(
                    onClick = { haptics.play(Haptic.Send); actions.call(row.group.number) },
                    shape = pressMorph(MaterialShapes.Circle, MaterialShapes.Cookie6Sided, press),
                    interactionSource = press,
                ) { Icon(Icons.Rounded.Call, "Call") }
            }
        }
        AnimatedVisibility(open, enter = expandVertically(spring(dampingRatio = 0.8f)) + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(Modifier.padding(start = 70.dp, end = 12.dp, bottom = 12.dp)) {
                row.group.entries.forEach { e ->
                    Text(
                        listOfNotNull(clockTime(e.date), kindOf(e.kind, false).second, e.durationSec.takeIf { it > 0 }?.let(::talkTime)).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 1.dp),
                    )
                }
                if (row.group.number.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Action(Icons.Rounded.Sms, "Message") { actions.message(row.group.number) }
                        if (row.name == null) Action(Icons.Rounded.PersonAdd, "Save") { actions.addContact(row.group.number) }
                        Action(Icons.Rounded.ContentCopy, "Copy") { actions.copy(row.group.number) }
                        Action(Icons.Rounded.Delete, "Remove") { haptics.play(Haptic.Delete); actions.delete(row) }
                    }
                }
            }
        }
    }
}

private fun kindOf(kind: Kind, silenced: Boolean): Pair<ImageVector, String> = when {
    silenced -> Icons.Rounded.NotificationsOff to "Silenced"
    kind == Kind.Missed -> Icons.AutoMirrored.Rounded.CallMissed to "Missed"
    kind == Kind.Declined -> Icons.AutoMirrored.Rounded.CallMissed to "Declined"
    kind == Kind.Blocked -> Icons.Rounded.Block to "Blocked"
    kind == Kind.Outgoing -> Icons.AutoMirrored.Rounded.CallMade to "Outgoing"
    else -> Icons.AutoMirrored.Rounded.CallReceived to "Incoming"
}

private fun talkTime(sec: Long) = if (sec < 60) "${sec}s" else "${sec / 60} min"

@Composable
private fun Action(icon: ImageVector, label: String, onClick: () -> Unit) {
    val haptics = rememberHaptics()
    val interaction = remember { MutableInteractionSource() }
    Column(
        Modifier
            .widthIn(min = 64.dp)
            .pressScale(interaction, 0.92f)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(interactionSource = interaction, indication = null, onClick = { haptics.play(Haptic.Tap); onClick() })
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DayHeader(label: String, modifier: Modifier = Modifier) {
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 24.dp, top = 14.dp, bottom = 4.dp),
    )
}

@Composable
private fun SetupCard(title: String, line: String, button: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val haptics = rememberHaptics()
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clip(RoundedCornerShape(28.dp))
            .background(scheme.primaryContainer).padding(20.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleLargeEmphasized, color = scheme.onPrimaryContainer)
        Spacer(Modifier.height(4.dp))
        Text(line, style = MaterialTheme.typography.bodyMedium, color = scheme.onPrimaryContainer)
        Spacer(Modifier.height(14.dp))
        Button(onClick = { haptics.play(Haptic.Tap); onClick() }) { Text(button) }
    }
}

@Composable
private fun Empty(line: String, modifier: Modifier = Modifier) {
    Text(
        line,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxWidth().padding(top = 96.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

@Composable
private fun ContactsPage(
    contacts: List<Contact>,
    list: androidx.compose.foundation.lazy.LazyListState,
    padding: PaddingValues,
    actions: HomeActions,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var open by rememberSaveable { mutableStateOf<Long?>(null) }
    val shown = remember(contacts, query) {
        if (query.isBlank()) contacts
        else {
            val q = query.trim().lowercase()
            val digits = q.filter(Char::isDigit)
            contacts.filter { c ->
                c.name.lowercase().split(' ').any { it.startsWith(q) } || c.name.lowercase().contains(q) ||
                    (digits.length >= 3 && c.numbers.any { it.number.filter(Char::isDigit).contains(digits) })
            }
        }
    }
    val favorites = remember(contacts) { contacts.filter { it.starred } }
    LazyColumn(state = list, contentPadding = padding, modifier = Modifier.fillMaxSize()) {
        item(key = "search") {
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search contacts") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, "Clear") }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }
        if (query.isBlank() && favorites.isNotEmpty()) {
            item(key = "favorites") { Favorites(favorites, actions) }
        }
        if (shown.isEmpty()) item(key = "empty") { Empty(if (query.isBlank()) "No contacts" else "No one called \"$query\"") }
        shown.forEachIndexed { i, contact ->
            val letter = contact.name.firstOrNull()?.uppercaseChar()?.takeIf { it.isLetter() } ?: '#'
            val prev = shown.getOrNull(i - 1)?.name?.firstOrNull()?.uppercaseChar()?.takeIf { it.isLetter() } ?: '#'
            if (query.isBlank() && (i == 0 || letter != prev)) {
                item(key = "letter-$letter-${contact.id}") { DayHeader(letter.toString()) }
            }
            item(key = contact.id) {
                ContactItem(contact, open == contact.id, onToggle = { open = if (open == contact.id) null else contact.id }, actions = actions, modifier = Modifier.animateItem())
            }
        }
    }
}

/** starred people as big faces: a tap calls, holding opens a message */
@Composable
private fun Favorites(favorites: List<Contact>, actions: HomeActions) {
    val haptics = rememberHaptics()
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(favorites, key = { it.id }) { c ->
            val interaction = remember { MutableInteractionSource() }
            val number = c.numbers.first().number
            Column(
                Modifier
                    .width(72.dp)
                    .pressScale(interaction, 0.92f)
                    .combinedClickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = { haptics.play(Haptic.Send); actions.call(number) },
                        onLongClick = { haptics.play(Haptic.Hold); actions.message(number) },
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Avatar(c.name, c.photo, c.id.toString(), size = 64.dp)
                Spacer(Modifier.height(6.dp))
                Text(c.name.substringBefore(' '), style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ContactItem(contact: Contact, open: Boolean, onToggle: () -> Unit, actions: HomeActions, modifier: Modifier = Modifier) {
    val haptics = rememberHaptics()
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier.fillMaxWidth().padding(horizontal = 8.dp).clip(RoundedCornerShape(24.dp))
            .background(if (open) scheme.surfaceContainer else Color.Transparent),
    ) {
        Row(
            Modifier.fillMaxWidth().pressScale(interaction, 0.98f)
                .combinedClickable(interactionSource = interaction, indication = null, onClick = { haptics.play(Haptic.Tap); onToggle() })
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(contact.name, contact.photo, contact.id.toString(), size = 44.dp)
            Spacer(Modifier.width(14.dp))
            Text(contact.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        }
        AnimatedVisibility(open, enter = expandVertically(spring(dampingRatio = 0.8f)) + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(Modifier.padding(start = 66.dp, end = 8.dp, bottom = 8.dp)) {
                contact.numbers.forEach { n ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                .combinedClickable(onClick = {}, onLongClick = { haptics.play(Haptic.Copy); actions.copy(n.number) })
                                .padding(vertical = 4.dp),
                        ) {
                            Text(n.number, style = MaterialTheme.typography.bodyLarge)
                            Text(n.label, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { haptics.play(Haptic.Tap); actions.message(n.number) }) { Icon(Icons.Rounded.Sms, "Message") }
                        val press = remember { MutableInteractionSource() }
                        FilledTonalIconButton(
                            onClick = { haptics.play(Haptic.Send); actions.call(n.number) },
                            shape = pressMorph(MaterialShapes.Circle, MaterialShapes.Cookie6Sided, press),
                            interactionSource = press,
                        ) { Icon(Icons.Rounded.Call, "Call") }
                    }
                }
            }
        }
    }
}
