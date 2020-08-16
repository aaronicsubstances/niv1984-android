package com.aaronicsubstances.niv1984.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.ui.about.AboutActivity
import com.aaronicsubstances.niv1984.ui.bookmarks.BookmarkListActivity
import com.aaronicsubstances.niv1984.ui.settings.SettingsActivity
import com.aaronicsubstances.niv1984.utils.AppUtils
import com.google.android.material.navigation.NavigationView

class CommonMenuActionProcessor(private val context: AppCompatActivity,
                                private val drawer: DrawerLayout)
        : NavigationView.OnNavigationItemSelectedListener {

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (!item.isChecked) {
            when (item.itemId) {
                R.id.nav_home -> AppUtils.showShortToast(context, "Clicked home")
                R.id.nav_bookmarks -> {
                    val intent = Intent(context, BookmarkListActivity::class.java)
                    context.startActivity(intent)
                }
                R.id.nav_comments -> AppUtils.showShortToast(context, "Clicked verse comments")
                R.id.nav_settings, R.id.action_settings -> {
                    launchSettings(context)
                }
                R.id.nav_about -> {
                    val intent = Intent(context, AboutActivity::class.java)
                    context.startActivity(intent)
                }
                R.id.nav_rate -> {
                    AppUtils.openAppOnPlayStore(context)
                }
                R.id.nav_share -> {
                    val appUrl = AppUtils.APP_PLAY_STORE_URL_PREFIX + context.packageName
                    val shareIntent = Intent()
                    shareIntent.action = Intent.ACTION_SEND
                    shareIntent.putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_message, appUrl))
                    shareIntent.type = "text/plain"
                    context.startActivity(Intent.createChooser(shareIntent, context.getText(R.string.share)))
                }
                R.id.nav_send -> {
                    val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
                    emailIntent.putExtra(Intent.EXTRA_EMAIL,
                        arrayOf(context.getString(R.string.feedback_email)))
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT,
                        context.getString(R.string.feedback_subject))
                    context.startActivity(Intent.createChooser(emailIntent,
                        context.getString(R.string.feedback_title)))
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