package com.aura.data

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class FocusMode {
    FOCUS,  // High filtering, "Deep Work"
    RELAX   // Low filtering, "Personal Time"
}

@Singleton
class FocusModeManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("aura_focus_prefs", Context.MODE_PRIVATE)

    fun getMode(): FocusMode = FocusMode.FOCUS

    fun setMode(mode: FocusMode) {
        // No-op in single profile mode
    }
}
