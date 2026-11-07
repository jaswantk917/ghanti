package io.github.jaswantk917.ghanti.data

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import android.provider.ContactsContract
import io.github.jaswantk917.ghanti.GhantiApp
import io.github.jaswantk917.ghanti.screen.Outcome
import io.github.jaswantk917.ghanti.signals.DeliveryRules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RecentRow(val group: RecentGroup, val name: String?, val photo: Uri?, val outcome: Outcome?) {
    val title get() = name ?: group.number.ifBlank { "Private number" }
}

data class HomeState(
    val loaded: Boolean = false,
    val recents: List<RecentRow> = emptyList(),
    val contacts: List<Contact> = emptyList(),
)

/** Recents and contacts, reloaded whenever the call log or the address book changes. */
class Home(private val context: Context, private val scope: CoroutineScope) {
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state
    private var job: Job? = null

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = reload()
    }

    fun start() {
        context.contentResolver.registerContentObserver(CallLog.Calls.CONTENT_URI, true, observer)
        context.contentResolver.registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, observer)
        reload()
    }

    fun stop() = context.contentResolver.unregisterContentObserver(observer)

    fun reload() {
        job?.cancel()
        job = scope.launch {
            _state.value = withContext(Dispatchers.IO) { load() }
        }
    }

    private fun load(): HomeState {
        val contacts = Contacts.all(context)
        val byNumber = HashMap<String, Contact>()
        contacts.forEach { c -> c.numbers.forEach { byNumber[DeliveryRules.last10(it.number)] = c } }
        val log = GhantiApp.instance.screenLog
        val recents = Recents.group(Recents.load(context)).map { group ->
            val contact = byNumber[DeliveryRules.last10(group.number)]
            RecentRow(
                group = group,
                name = contact?.name ?: group.latest.cachedName,
                photo = contact?.photo,
                outcome = if (contact == null) log.forCall(group.number, group.latest.date) else null,
            )
        }
        return HomeState(loaded = true, recents = recents, contacts = contacts)
    }
}
