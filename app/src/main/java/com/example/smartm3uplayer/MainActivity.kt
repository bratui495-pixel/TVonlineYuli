package com.example.smartm3uplayer

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : AppCompatActivity() {

    private val hideHandler = Handler(Looper.getMainLooper())
    private lateinit var topControls: View

    private val hideUiRunnable = Runnable {
        hideControlsAndSystemBars()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        topControls = findViewById(R.id.topControls)
        showControlsAndSystemBars()
        resetHideTimer()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        showControlsAndSystemBars()
        resetHideTimer()
        return super.dispatchTouchEvent(ev)
    }

    private fun resetHideTimer() {
        hideHandler.removeCallbacks(hideUiRunnable)
        hideHandler.postDelayed(hideUiRunnable, 4000L)
    }

    private fun showControlsAndSystemBars() {
        topControls.visibility = View.VISIBLE
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView)
            .show(WindowInsetsCompat.Type.systemBars())
    }

    private fun hideControlsAndSystemBars() {
        topControls.visibility = View.GONE
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onDestroy() {
        super.onDestroy()
        hideHandler.removeCallbacks(hideUiRunnable)
    }
}
