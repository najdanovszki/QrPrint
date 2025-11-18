package com.webtic.qrprint

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.webtic.qrprint.util.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        if (preferencesManager.cameraBack)
            menu?.findItem(R.id.back)?.isChecked = true
        else
            menu?.findItem(R.id.front)?.isChecked = true
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.front -> preferencesManager.cameraBack = false
            R.id.back -> preferencesManager.cameraBack = true
        }
        return true
    }
}