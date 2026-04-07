package com.bk.sleeptimer

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView

class SleepTimerService : AccessibilityService() {

    private val presets = longArrayOf(30, 45, 60, 90, 120)
    private val overlayHideDelayMs = 3000L

    private lateinit var windowManager: WindowManager
    private var overlayView: TextView? = null

    private var countdownTimer: CountDownTimer? = null
    private var remainingMinutes = 0L
    private var timerRunning = false

    private var cycleIndex = -1
    private var showedRemaining = false

    private val hideHandler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable { hideOverlay() }
    private val lastMinuteRunnable = Runnable { showOverlay("< 1 min") }

    private val redButtonReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            onRedButtonPressed()
        }
    }

    override fun onServiceConnected() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        registerReceiver(redButtonReceiver, IntentFilter(ACTION_RED_BUTTON), RECEIVER_EXPORTED)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        hideHandler.removeCallbacksAndMessages(null)
        try { unregisterReceiver(redButtonReceiver) } catch (_: Exception) {}
        countdownTimer?.cancel()
        removeOverlay()
        super.onDestroy()
    }

    companion object {
        const val ACTION_RED_BUTTON = "com.bk.sleeptimer.RED_BUTTON"
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_PROG_RED) return false
        if (event.action != KeyEvent.ACTION_DOWN) return true
        if (event.repeatCount > 0) return true

        onRedButtonPressed()
        return true
    }

    private fun onRedButtonPressed() {
        if (!timerRunning) {
            showedRemaining = false
            cycleIndex = 0
            startTimer(presets[0])
            showOverlay(formatMinutes(presets[0]))
            return
        }

        if (!showedRemaining) {
            showedRemaining = true
            cycleIndex = presets.indexOfFirst { it > remainingMinutes }
            showOverlay(formatMinutes(remainingMinutes))
            return
        }

        if (cycleIndex < 0 || cycleIndex >= presets.size) {
            cancelTimer()
            showedRemaining = false
            cycleIndex = -1
            hideHandler.removeCallbacks(hideRunnable)
            hideOverlay()
            return
        }

        val next = presets[cycleIndex]
        cycleIndex++
        if (cycleIndex >= presets.size) cycleIndex = -1
        startTimer(next)
        showOverlay(formatMinutes(next))
    }

    private fun startTimer(minutes: Long) {
        countdownTimer?.cancel()
        hideHandler.removeCallbacks(lastMinuteRunnable)
        remainingMinutes = minutes
        timerRunning = true

        if (minutes > 1) {
            hideHandler.postDelayed(lastMinuteRunnable, (minutes - 1) * 60_000L)
        }

        countdownTimer = object : CountDownTimer(minutes * 60_000L, 60_000L) {
            override fun onTick(millisUntilFinished: Long) {
                remainingMinutes = (millisUntilFinished + 59_999L) / 60_000L
            }

            override fun onFinish() {
                timerRunning = false
                remainingMinutes = 0
                cycleIndex = -1
                showedRemaining = false
                goToSleep()
            }
        }.start()
    }

    private fun cancelTimer() {
        countdownTimer?.cancel()
        countdownTimer = null
        hideHandler.removeCallbacks(lastMinuteRunnable)
        timerRunning = false
        remainingMinutes = 0
    }

    private fun goToSleep() {
        hideOverlay()
        performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
    }

    private fun showOverlay(text: String) {
        hideHandler.removeCallbacks(hideRunnable)

        if (overlayView == null) {
            val tv = TextView(this).apply {
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
                setBackgroundColor(Color.BLACK)
                setPadding(24, 12, 24, 12)
            }
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.END
                x = 48
                y = 48
            }
            windowManager.addView(tv, params)
            overlayView = tv
        }

        overlayView?.text = text
        overlayView?.visibility = View.VISIBLE
        hideHandler.postDelayed(hideRunnable, overlayHideDelayMs)
    }

    private fun hideOverlay() {
        overlayView?.visibility = View.GONE
    }

    private fun removeOverlay() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            overlayView = null
        }
    }

    private fun formatMinutes(minutes: Long): String {
        return if (minutes < 60) {
            "$minutes"
        } else {
            val h = minutes / 60
            val m = minutes % 60
            String.format("%d:%02d", h, m)
        }
    }
}
