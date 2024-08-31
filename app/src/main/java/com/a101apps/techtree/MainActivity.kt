package com.a101apps.techtree

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import java.util.UUID

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Handle night mode for navigation and status bar colors
        handleNightMode()

        // Set up Navigation Controller
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
    }

    override fun onSupportNavigateUp(): Boolean {
        return findNavController(R.id.nav_host_fragment).navigateUp()
    }

    // Function to handle night mode
    private fun handleNightMode() {
        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val darkColor = ContextCompat.getColor(this, R.color.dark)
        val lightColor = ContextCompat.getColor(this, R.color.light)
        window.navigationBarColor = if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) darkColor else lightColor
        window.statusBarColor = if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) darkColor else lightColor

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val visibilityFlags = if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            } else {
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
            window.decorView.systemUiVisibility = visibilityFlags
        }
    }

}