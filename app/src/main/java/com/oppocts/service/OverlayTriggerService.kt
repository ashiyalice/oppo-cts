package com.oppocts.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.oppocts.R
import com.oppocts.trigger.CTSTrigger

class OverlayTriggerService : Service() {

    companion object {
        private const val TAG = "OverlayTriggerService"
        private const val NOTIFICATION_ID = 2002
        private const val CHANNEL_ID = "overlay_trigger_channel"

        // 터치 설정
        private const val LONG_PRESS_TIMEOUT = 400L // 0.4초
    }

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private val handler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null
    
    // 터치 이동량 판별용
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var touchSlop = 0

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        createOverlayView()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "com.oppocts.action.UPDATE_OVERLAY_PREFS") {
            updateOverlayFromPrefs()
        }
        return START_STICKY
    }

    private fun updateOverlayFromPrefs() {
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        val isDebug = prefs.getBoolean("overlay_debug_mode", false)
        val offsetY = prefs.getInt("overlay_offset_y", 0)
        val height = prefs.getInt("overlay_height", 90)

        // Update color
        if (isDebug) {
            overlayView?.setBackgroundColor(Color.argb(100, 255, 0, 0))
        } else {
            overlayView?.setBackgroundColor(Color.TRANSPARENT)
        }

        // Update layout params
        layoutParams?.let { params ->
            params.height = height
            
            // Adjust Y offset (negative moves up, positive moves down)
            params.y = offsetY
            
            try {
                windowManager.updateViewLayout(overlayView, params)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update overlay layout from prefs", e)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        removeOverlayView()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "플로팅 하단 트리거",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "시즈쿠 없이 서클 투 서치를 실행하기 위해 화면 하단 터치를 감지합니다."
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("하단 트리거 활성화됨")
            .setContentText("화면 하단을 길게 누르면 CTS가 실행됩니다.")
            .setSmallIcon(R.drawable.ic_cts) // Check icons
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createOverlayView() {
        if (overlayView != null) return

        overlayView = View(this).apply {
            setOnTouchListener { v, event ->
                handleTouch(event)
            }
        }

        // 초기 상태 로드
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        val isDebug = prefs.getBoolean("overlay_debug_mode", false)
        val height = prefs.getInt("overlay_height", 90)
        val offsetY = prefs.getInt("overlay_offset_y", 0)

        if (isDebug) {
            overlayView?.setBackgroundColor(Color.argb(100, 255, 0, 0))
        } else {
            overlayView?.setBackgroundColor(Color.TRANSPARENT)
        }

        // WindowInsets를 감지하기 위해 OnApplyWindowInsetsListener 세팅
        ViewCompat.setOnApplyWindowInsetsListener(overlayView!!) { v, insets ->
            updateOverlayBounds(insets)
            insets
        }

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            height,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, // 화면 끝까지 확장 허용
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = offsetY
        }

        try {
            windowManager.addView(overlayView, layoutParams)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add overlay view", e)
        }
    }

    private fun updateOverlayBounds(insets: WindowInsetsCompat) {
        val navInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
        // ColorOS나 특정 기기에서 플로팅 윈도우에 대해 isVisible이 false로 오거나 inset이 0으로 올 수 있음
        // 이제 높이는 SharedPreferences 값을 우선 사용함
        
        layoutParams?.let { params ->
            overlayView?.visibility = View.VISIBLE
            
            // 가로/세로 모드 힌트 확인 용도로만 사용
            if (navInsets.right > 0 && navInsets.bottom == 0) {
                // 가로 모드 우측
                params.width = prefsHeight() // 우측의 너비가 곧 설정값(두께)
                params.height = WindowManager.LayoutParams.MATCH_PARENT
                params.gravity = Gravity.END or Gravity.CENTER_VERTICAL
                params.x = prefsOffsetY()
                params.y = 0
            } else if (navInsets.left > 0 && navInsets.bottom == 0) {
                // 가로 모드 역전 (좌측)
                params.width = prefsHeight()
                params.height = WindowManager.LayoutParams.MATCH_PARENT
                params.gravity = Gravity.START or Gravity.CENTER_VERTICAL
                params.x = -prefsOffsetY() // 좌측이므로 음수일 가능성
                params.y = 0
            } else {
                // 세로 하단 기본 모드
                params.width = WindowManager.LayoutParams.MATCH_PARENT
                params.height = prefsHeight()
                params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                params.x = 0
                params.y = prefsOffsetY() // Y Offset 복원
            }
            
            try {
                windowManager.updateViewLayout(overlayView, params)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update overlay view layout", e)
            }
        }
    }

    private fun prefsHeight(): Int {
        return androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
            .getInt("overlay_height", 90)
    }

    private fun prefsOffsetY(): Int {
        return androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
            .getInt("overlay_offset_y", 0)
    }

    private fun handleTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = event.rawX
                touchStartY = event.rawY
                
                startLongPressTimer()
                
                // ACTION_DOWN은 소비해야 이후 이벤트(MOVE, UP)를 받을 수 있음.
                // 단, 우리가 처리하지 않을 제스처(스와이프)라면 다른 곳으로 넘겨야 하므로 true를 반환하면 안 됨.
                // FLAG_NOT_TOUCH_MODAL에서는 터치한 영역 내부 이벤트는 우리 앱이 소비하게 됨.
                // 시스템 제스처(홈으로 가기 등)는 시스템이 우선권을 가지므로 가로채이지 않을 수 있음.
                return true 
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = Math.abs(event.rawX - touchStartX)
                val dy = Math.abs(event.rawY - touchStartY)
                
                // 손가락이 조금이라도 움직이면 롱프레스 취소 (스와이프로 간주)
                if (dx > touchSlop || dy > touchSlop) {
                    cancelLongPressTimer()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                cancelLongPressTimer()
            }
        }
        return false
    }

    private fun startLongPressTimer() {
        cancelLongPressTimer()
        longPressRunnable = Runnable {
            // 시간이 다 되면 CTS 트리거 실행
            Log.i(TAG, "Overlay touch long press detected! Firing CTS...")
            CTSTrigger.trigger(this)
        }
        handler.postDelayed(longPressRunnable!!, LONG_PRESS_TIMEOUT)
    }

    private fun cancelLongPressTimer() {
        longPressRunnable?.let {
            handler.removeCallbacks(it)
        }
        longPressRunnable = null
    }

    private fun removeOverlayView() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove overlay view", e)
            }
            overlayView = null
        }
    }
}
