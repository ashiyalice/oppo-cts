package com.oppocts.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.oppocts.R
import com.oppocts.service.OppoAccessibilityService
import com.oppocts.shizuku.AssistantSetter
import com.oppocts.shizuku.GmsFlagSetter
import com.oppocts.shizuku.ShizukuHelper
import com.oppocts.trigger.CTSTrigger
import com.oppocts.util.PackageUtils
import kotlinx.coroutines.*

/**
 * 설정 화면 — 상태 대시보드 + 트리거 방식 설정.
 */
class SettingsActivity : AppCompatActivity() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private lateinit var tvGoogleStatus: TextView
    private lateinit var tvGmsStatus: TextView
    private lateinit var tvShizukuStatus: TextView
    private lateinit var tvAccessibilityStatus: TextView
    private lateinit var tvAssistantStatus: TextView
    private lateinit var tvFlagStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        initViews()
    }

    private fun initViews() {
        tvGoogleStatus = findViewById(R.id.tv_google_status)
        tvGmsStatus = findViewById(R.id.tv_gms_status)
        tvShizukuStatus = findViewById(R.id.tv_shizuku_status)
        tvAccessibilityStatus = findViewById(R.id.tv_accessibility_status)
        tvAssistantStatus = findViewById(R.id.tv_assistant_status)
        tvFlagStatus = findViewById(R.id.tv_flag_status)

        // CTS 테스트 버튼
        findViewById<Button>(R.id.btn_test_cts).setOnClickListener {
            scope.launch {
                CTSTrigger.bindService()
                withContext(Dispatchers.IO) {
                    var wait = 0
                    while (!CTSTrigger.isServiceBound() && wait < 2000) {
                        Thread.sleep(100)
                        wait += 100
                    }
                }
                val success = withContext(Dispatchers.IO) {
                    CTSTrigger.trigger(applicationContext)
                }
                if (!success) {
                    android.widget.Toast.makeText(
                        this@SettingsActivity,
                        "CTS 트리거 실패. Shizuku가 실행 중인지 확인하세요.",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        // 트리거 방식 선택 스피너
        setupTriggerSpinner()

        // 접근성 설정 열기 버튼
        findViewById<Button>(R.id.btn_open_accessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        // 설정 다시 실행 버튼
        findViewById<Button>(R.id.btn_rerun_setup).setOnClickListener {
            startActivity(Intent(this, SetupWizardActivity::class.java))
        }

        // 새로고침 버튼
        findViewById<Button>(R.id.btn_refresh).setOnClickListener {
            refreshStatus()
        }

        // 키 감지 테스트 버튼 (getevent 기반)
        val tvKeyLog = findViewById<TextView>(R.id.tv_key_log)
        findViewById<Button>(R.id.btn_key_test).setOnClickListener {
            if (!CTSTrigger.isServiceBound()) {
                tvKeyLog.text = "⚠️ Shizuku 서비스 미연결.\n먼저 Shizuku가 실행 중인지 확인하세요."
                return@setOnClickListener
            }
            scope.launch {
                val keys = withContext(Dispatchers.IO) {
                    CTSTrigger.getDetectedKeys()
                }
                if (keys.isBlank()) {
                    tvKeyLog.text = "키 이벤트가 감지되지 않았습니다.\n버튼 매핑을 선택하고 아무 버튼을 눌러보세요."
                } else {
                    tvKeyLog.text = "최근 감지된 키 이벤트:\n$keys"
                }
            }
        }

        // Shizuku 없이 인텐트로 CTS 트리거 테스트
        findViewById<Button>(R.id.btn_intent_test).setOnClickListener {
            tvKeyLog.text = "⏳ 인텐트 방법 테스트 중..."
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    CTSTrigger.triggerViaIntentNoShizuku(this@SettingsActivity)
                }
                tvKeyLog.text = result
            }
        }

        // 플로팅 하단 트리거 설정
        val switchOverlay = findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_overlay_trigger)
        val switchDebug = findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_overlay_debug)
        val seekbarOffset = findViewById<android.widget.SeekBar>(R.id.seekbar_overlay_offset)
        val seekbarHeight = findViewById<android.widget.SeekBar>(R.id.seekbar_overlay_height)
        
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        switchOverlay.isChecked = prefs.getBoolean("overlay_trigger_enabled", false)
        switchDebug.isChecked = prefs.getBoolean("overlay_debug_mode", false)
        
        // Offset range: -100 to 100 (seekbar max is 200, so progress 100 is offset 0)
        seekbarOffset.progress = prefs.getInt("overlay_offset_y", 0) + 100
        // Height range: 10 to 200
        seekbarHeight.progress = prefs.getInt("overlay_height", 90)
        
        fun notifyOverlayUpdate() {
            if (switchOverlay.isChecked) {
                val intent = Intent(this, com.oppocts.service.OverlayTriggerService::class.java).apply {
                    action = "com.oppocts.action.UPDATE_OVERLAY_PREFS"
                }
                startService(intent)
            }
        }

        switchOverlay.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (!Settings.canDrawOverlays(this)) {
                    // 권한 요청
                    switchOverlay.isChecked = false
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:$packageName"))
                    startActivity(intent)
                    android.widget.Toast.makeText(this, "CTS 실행을 위해 '다른 앱 위에 표시' 권한을 허용해주세요.", android.widget.Toast.LENGTH_LONG).show()
                } else {
                    prefs.edit().putBoolean("overlay_trigger_enabled", true).apply()
                    startService(Intent(this, com.oppocts.service.OverlayTriggerService::class.java))
                }
            } else {
                prefs.edit().putBoolean("overlay_trigger_enabled", false).apply()
                stopService(Intent(this, com.oppocts.service.OverlayTriggerService::class.java))
            }
        }

        switchDebug.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("overlay_debug_mode", isChecked).apply()
            notifyOverlayUpdate()
        }

        seekbarOffset.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    prefs.edit().putInt("overlay_offset_y", progress - 100).apply()
                    notifyOverlayUpdate()
                }
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        seekbarHeight.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val height = java.lang.Math.max(10, progress) // minimum 10px
                    prefs.edit().putInt("overlay_height", height).apply()
                    notifyOverlayUpdate()
                }
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
        // 키 모니터링 재시작
        startMonitoringIfNeeded()
        
        // 플로팅 트리거 스위치 & 권한 싱크
        val switchOverlay = findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_overlay_trigger)
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        val isOverlayEnabled = prefs.getBoolean("overlay_trigger_enabled", false)
        
        if (isOverlayEnabled && !Settings.canDrawOverlays(this)) {
            // 권한이 없는데 켜져 있는 상태면 끎
            prefs.edit().putBoolean("overlay_trigger_enabled", false).apply()
            switchOverlay.isChecked = false
        } else if (isOverlayEnabled) {
            switchOverlay.isChecked = true
            startService(Intent(this, com.oppocts.service.OverlayTriggerService::class.java))
        } else {
            switchOverlay.isChecked = false
            stopService(Intent(this, com.oppocts.service.OverlayTriggerService::class.java))
        }
    }

    private fun startMonitoringIfNeeded() {
        if (!CTSTrigger.isServiceBound()) return
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        val method = prefs.getString(OppoAccessibilityService.PREF_TRIGGER_METHOD, "none") ?: "none"
        if (method != "none") {
            CTSTrigger.startKeyMonitoring(method)
        }
    }

    private fun setupTriggerSpinner() {
        val spinner = findViewById<android.widget.Spinner>(R.id.spinner_trigger_method)
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)

        val labels = arrayOf(
            "사용 안 함",
            "🔽 볼륨 아래 더블 클릭",
            "🔼 볼륨 위 더블 클릭",
            "🔽 볼륨 아래 길게 누르기",
            "🔼🔽 볼륨 위+아래 동시",
            "📷 카메라 버튼 길게 누르기",
            "⚡ 단축키 버튼"
        )
        val values = arrayOf(
            OppoAccessibilityService.TRIGGER_NONE,
            OppoAccessibilityService.TRIGGER_VOL_DOWN_DOUBLE,
            OppoAccessibilityService.TRIGGER_VOL_UP_DOUBLE,
            OppoAccessibilityService.TRIGGER_VOL_DOWN_LONG,
            OppoAccessibilityService.TRIGGER_VOL_BOTH,
            OppoAccessibilityService.TRIGGER_CAMERA_LONG,
            OppoAccessibilityService.TRIGGER_SHORTCUT_KEY
        )

        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        // 저장된 값 복원
        val saved = prefs.getString(OppoAccessibilityService.PREF_TRIGGER_METHOD, OppoAccessibilityService.DEFAULT_TRIGGER)
        val idx = values.indexOf(saved)
        if (idx >= 0) spinner.setSelection(idx)

        spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, pos: Int, id: Long) {
                val method = values[pos]
                prefs.edit().putString(OppoAccessibilityService.PREF_TRIGGER_METHOD, method).apply()

                // getevent 모니터링 시작/중지
                if (CTSTrigger.isServiceBound()) {
                    if (method == "none") {
                        CTSTrigger.stopKeyMonitoring()
                    } else {
                        CTSTrigger.startKeyMonitoring(method)
                    }
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun refreshStatus() {
        // Google 앱
        val googleInstalled = isPackageInstalled("com.google.android.googlequicksearchbox")
        tvGoogleStatus.text = statusText(googleInstalled)

        // GMS
        val gmsInstalled = isPackageInstalled("com.google.android.gms")
        tvGmsStatus.text = statusText(gmsInstalled)

        // Shizuku
        val shizukuInstalled = ShizukuHelper.isInstalled(this)
        val shizukuRunning = if (shizukuInstalled) ShizukuHelper.isRunning() else false
        tvShizukuStatus.text = when {
            !shizukuInstalled -> getString(R.string.settings_status_not_installed)
            !shizukuRunning -> getString(R.string.settings_status_inactive)
            else -> getString(R.string.settings_status_active)
        }

        // 접근성 서비스
        tvAccessibilityStatus.text = statusText(OppoAccessibilityService.isRunning)

        // 어시스턴트 & Flag (Shizuku 필요)
        if (shizukuRunning && ShizukuHelper.hasPermission()) {
            scope.launch {
                val isGoogle = withContext(Dispatchers.IO) { AssistantSetter.isGoogleAssistant() }
                tvAssistantStatus.text = if (isGoogle) "Google ✅" else "Google 아님 ❌"

                val flagEnabled = withContext(Dispatchers.IO) { GmsFlagSetter.isCTSEnabled() }
                tvFlagStatus.text = statusText(flagEnabled)
            }
        } else {
            tvAssistantStatus.text = "확인 불가 (Shizuku 필요)"
            tvFlagStatus.text = "확인 불가 (Shizuku 필요)"
        }
    }

    private fun statusText(active: Boolean): String {
        return if (active) getString(R.string.settings_status_active) + " ✅"
        else getString(R.string.settings_status_inactive) + " ❌"
    }

    private fun isPackageInstalled(packageName: String): Boolean =
        PackageUtils.isInstalled(packageManager, packageName)

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
