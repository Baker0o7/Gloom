package dev.materii.gloom.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import dev.materii.gloom.util.deeplink.DeepLinkHandler

class DeepLinkActivity : GloomActivity() {

    override fun setupDefaultContent() {
        val backstack = DeepLinkHandler.handle(intent).ifEmpty {
            // Unknown route — open in browser instead of just failing
            val url = intent.dataString
            if (!url.isNullOrBlank()) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    // Exclude Gloom to avoid infinite loop
                    setPackage(null)
                })
            } else {
                Toast.makeText(this, "Unimplemented GitHub route", Toast.LENGTH_SHORT).show()
            }
            finish()
            return
        }
        setupContent(backstack)

        // On first open via a link, offer to make Gloom the default for github.com
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val prefs = getSharedPreferences("gloom_prefs", MODE_PRIVATE)
            val shown = prefs.getBoolean("default_browser_prompt_shown", false)
            if (!shown) {
                prefs.edit().putBoolean("default_browser_prompt_shown", true).apply()
                AlertDialog.Builder(this)
                    .setTitle("Open GitHub links in Gloom?")
                    .setMessage("You can set Gloom as the default app for github.com links so they open directly without a dialog.")
                    .setPositiveButton("Open Settings") { _, _ ->
                        // Open app's link opening settings (Android 12+)
                        try {
                            startActivity(Intent(Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS).apply {
                                data = Uri.parse("package:$packageName")
                            })
                        } catch (_: Exception) {
                            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:$packageName")
                            })
                        }
                    }
                    .setNegativeButton("Not now", null)
                    .show()
            }
        }
    }
}
