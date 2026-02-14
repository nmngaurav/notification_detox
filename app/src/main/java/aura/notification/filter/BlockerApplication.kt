package aura.notification.filter

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BlockerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize things here if needed
    }
}
