package com.aaronicsubstances.niv1984.ui

import android.content.Context
import android.content.Intent
import android.view.MenuItem
import androidx.annotation.IdRes
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.ui.about.AboutActivity
import com.aaronicsubstances.niv1984.ui.settings.SettingsActivity
import com.aaronicsubstances.niv1984.utils.AppUtils
import com.google.android.material.navigation.NavigationView

class CommonMenuActionProcessor(private val context: Context,
                                private val drawer: DrawerLayout)
        : NavigationView.OnNavigationItemSelectedListener {

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (!item.isChecked) {
            when (item.itemId) {
                R.id.nav_home -> AppUtils.showShortToast(context, "Clicked item one")
                R.id.nav_gallery -> AppUtils.showShortToast(context, "Clicked item two")
                R.id.nav_slideshow -> AppUtils.showShortToast(context, "Clicked item three")
                R.id.nav_tools, R.id.action_settings -> {
                    launchSettings(context)
                }
                R.id.nav_about -> {
                    val intent = Intent(context, AboutActivity::class.java)
                    context.startActivity(intent)
                }
            }
        }
        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    companion object {
        fun launchSettings(context: Context) {
            val intent = Intent(context, SettingsActivity::class.java)
            context.startActivity(intent)
        }
    }
}