package io.github.jaswantk917.ghanti

import android.app.Application
import io.github.jaswantk917.ghanti.data.Memos
import io.github.jaswantk917.ghanti.data.Prefs
import io.github.jaswantk917.ghanti.incall.CallNotifications
import io.github.jaswantk917.ghanti.screen.ScreenLog
import io.github.jaswantk917.ghanti.signals.Signals

class GhantiApp : Application() {
    val prefs by lazy { Prefs(this) }
    val signals by lazy { Signals(this) }
    val screenLog by lazy { ScreenLog(this) }
    val memos by lazy { Memos(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        CallNotifications.createChannels(this)
    }

    companion object {
        lateinit var instance: GhantiApp
            private set
    }
}
