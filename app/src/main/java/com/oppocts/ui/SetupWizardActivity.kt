package com.oppocts.ui

import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.oppocts.R
import com.oppocts.shizuku.AssistantSetter
import com.oppocts.shizuku.GmsFlagSetter
import com.oppocts.shizuku.ShizukuHelper
import com.oppocts.trigger.CTSTrigger
import com.oppocts.util.PackageUtils
import kotlinx.coroutines.*

/**
 * 설정 위저드 — 7단계 가이드.
 *
 * 사용자를 단계별로 안내하여 CTS 사용에 필요한
 * 모든 설정을 완료하도록 합니다.
 */
class SetupWizardActivity : AppCompatActivity() {

    private val prefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var currentStep = 0
    private val totalSteps = 8

    // UI 요소
    private lateinit var tvStepTitle: TextView
    private lateinit var tvStepDescription: TextView
    private lateinit var tvStepNumber: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnNext: Button
    private lateinit var btnBack: Button
    private lateinit var btnAction: Button
    private lateinit var statusContainer: LinearLayout
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup_wizard)

        initViews()
        updateStep()
    }

    private fun initViews() {
        tvStepTitle = findViewById(R.id.tv_step_title)
        tvStepDescription = findViewById(R.id.tv_step_description)
        tvStepNumber = findViewById(R.id.tv_step_number)
        progressBar = findViewById(R.id.progress_bar)
        btnNext = findViewById(R.id.btn_next)
        btnBack = findViewById(R.id.btn_back)
        btnAction = findViewById(R.id.btn_action)
        statusContainer = findViewById(R.id.status_container)
        tvStatus = findViewById(R.id.tv_status)

        progressBar.max = totalSteps

        btnNext.setOnClickListener {
            if (currentStep < totalSteps) {
                currentStep++
                updateStep()
            } else {
                completeSetup()
            }
        }

        btnBack.setOnClickListener {
            handleBackNavigation()
        }
    }

    override fun onBackPressed() {
        if (!handleBackNavigation()) {
            super.onBackPressed()
        }
    }

    /** @return true if navigation handled (stepped back), false if already at first step */
    private fun handleBackNavigation(): Boolean {
        return if (currentStep > 0) {
            currentStep--
            updateStep()
            true
        } else {
            false
        }
    }

    private fun updateStep() {
        try {
            progressBar.progress = currentStep + 1
            tvStepNumber.text = "${currentStep + 1} / $totalSteps"
            btnBack.visibility = if (currentStep > 0) View.VISIBLE else View.INVISIBLE
            statusContainer.visibility = View.GONE

            // 리스너 초기화 (Step 7에서 덮어쓴 리스너가 이전 단계로 돌아갔을 때 유지되는 문제 방지)
            btnNext.setOnClickListener {
                if (currentStep < totalSteps - 1) {
                    currentStep++
                    updateStep()
                } else {
                    completeSetup()
                }
            }

            when (currentStep) {
                0 -> showGmsStep()
                1 -> showGoogleAppStep()
                2 -> showShizukuStep()
                3 -> showAssistantStep()
                4 -> showGmsFlagStep()
                5 -> showGeminiStep()
                6 -> showTriggerStep()
                7 -> showCompleteStep()
            }
        } catch (e: Exception) {
            Log.e("SetupWizard", "Error updating step", e)
            showStatus("오류 발생: ${e.message}\n이전 단계로 돌아가거나 로그를 캡처해 주세요.")
        }
    }

    // Step 0: GMS 활성화
    private fun showGmsStep() {
        tvStepTitle.text = getString(R.string.setup_step_gms)
        tvStepDescription.text = getString(R.string.setup_step_gms_desc)
        btnAction.visibility = View.VISIBLE
        btnAction.text = getString(R.string.btn_check)
        btnAction.setOnClickListener {
            // GMS 활성화 상태 확인
            val gmsInstalled = isPackageInstalled("com.google.android.gms")
            showStatus(if (gmsInstalled) "GMS 활성화됨 ✅" else "GMS를 찾을 수 없습니다 ❌")
        }
        btnNext.text = getString(R.string.btn_next)
    }

    // Step 1: Google 앱 설치
    private fun showGoogleAppStep() {
        tvStepTitle.text = getString(R.string.setup_step_google_app)
        tvStepDescription.text = getString(R.string.setup_step_google_app_desc)
        btnAction.visibility = View.VISIBLE
        btnAction.text = getString(R.string.btn_check)
        btnAction.setOnClickListener {
            val googleInstalled = isPackageInstalled("com.google.android.googlequicksearchbox")
            if (googleInstalled) {
                showStatus("Google 앱 설치됨 ✅")
            } else {
                showStatus("Google 앱 미설치 ❌")
                // APKMirror 등에서 설치 안내
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(
                        "https://play.google.com/store/apps/details?id=com.google.android.googlequicksearchbox"
                    )))
                } catch (_: Exception) { }
            }
        }
        btnNext.text = getString(R.string.btn_next)
    }

    // Step 2: Shizuku 설치 & 시작
    private fun showShizukuStep() {
        tvStepTitle.text = getString(R.string.setup_step_shizuku)
        tvStepDescription.text = getString(R.string.setup_step_shizuku_desc)
        btnAction.visibility = View.VISIBLE
        btnAction.text = getString(R.string.btn_check)
        btnAction.setOnClickListener {
            val status = ShizukuHelper.getStatusSummary(this)
            val tip = if (ShizukuHelper.isRunning()) "\n\n💡 팁: OPPO 기기는 개발자 옵션에서 '권한 감시(Permission Monitoring)'를 꺼야 기능이 정상 작동합니다." else ""
            showStatus(status + tip)
        }
        btnNext.text = getString(R.string.btn_next)
    }

    // Step 3: 기본 어시스턴트 변경
    private fun showAssistantStep() {
        tvStepTitle.text = getString(R.string.setup_step_assistant)
        tvStepDescription.text = getString(R.string.setup_step_assistant_desc)
        btnAction.visibility = View.VISIBLE
        btnAction.text = getString(R.string.btn_apply)
        btnAction.setOnClickListener {
            if (!ShizukuHelper.isRunning() || !ShizukuHelper.hasPermission()) {
                showStatus("Shizuku가 실행 중이 아니거나 권한이 없습니다 ❌")
                return@setOnClickListener
            }
            scope.launch {
                val success = withContext(Dispatchers.IO) {
                    AssistantSetter.setGoogleAssistant()
                }
                showStatus(
                    if (success) getString(R.string.assistant_set_success) + " ✅"
                    else getString(R.string.assistant_set_failed) + " ❌"
                )
            }
        }
        btnNext.text = getString(R.string.btn_next)
    }

    // Step 4: GMS Flag 설정
    private fun showGmsFlagStep() {
        tvStepTitle.text = getString(R.string.setup_step_gms_flag)
        tvStepDescription.text = getString(R.string.setup_step_gms_flag_desc)
        btnAction.visibility = View.VISIBLE
        btnAction.text = getString(R.string.btn_apply)
        btnAction.setOnClickListener {
            if (!ShizukuHelper.isRunning() || !ShizukuHelper.hasPermission()) {
                showStatus("Shizuku가 실행 중이 아니거나 권한이 없습니다 ❌")
                return@setOnClickListener
            }
            scope.launch {
                val success = withContext(Dispatchers.IO) {
                    GmsFlagSetter.enableCTSFlag()
                }
                showStatus(
                    if (success) getString(R.string.gms_flag_set_success) + " ✅"
                    else getString(R.string.gms_flag_set_failed) + " ❌"
                )
            }
        }
        btnNext.text = getString(R.string.btn_next)
    }

    // Step 5: Gemini 설치
    private fun showGeminiStep() {
        tvStepTitle.text = getString(R.string.setup_step_gemini)
        tvStepDescription.text = getString(R.string.setup_step_gemini_desc)
        btnAction.visibility = View.VISIBLE
        btnAction.text = getString(R.string.btn_check)
        btnAction.setOnClickListener {
            val geminiInstalled = isPackageInstalled("com.google.android.apps.bard")
            showStatus(if (geminiInstalled) "Gemini 설치됨 ✅" else "Gemini 미설치 (선택사항) ⚠️")
        }
        btnNext.text = getString(R.string.btn_next)
    }

    // Step 6: 트리거 방식 선택 & 테스트 (VIS 방식)
    private fun showTriggerStep() {
        tvStepTitle.text = getString(R.string.setup_step_trigger)
        tvStepDescription.text = getString(R.string.setup_step_trigger_desc) +
                "\n\n" +
                "사용 가능한 트리거 방법:\n\n" +
                "① 앱 아이콘 탭\n" +
                "   홈 화면에서 앱 아이콘을 누르면 CTS가 바로 실행됩니다.\n\n" +
                "② Quick Settings 타일\n" +
                "   알림창 → 빠른 설정 편집(✏️) → 'Circle to Search' 타일 추가\n\n" +
                "③ 접근성 서비스 (HOME 길게 누르기)\n" +
                "   3버튼 네비게이션에서 HOME 키 길게 누르기로 CTS를 실행합니다.\n" +
                "   ⚠️ 접근성 활성화 시 '제한된 설정' 차단이 뜰 수 있습니다:\n" +
                "   설정 → 앱 → 앱 관리 → OPPO CTS → ⋮ 메뉴 → 제한된 설정 허용\n" +
                "   허용 후 다시 접근성에서 활성화하세요.\n\n" +
                "아래 버튼으로 CTS가 정상 작동하는지 먼저 테스트하세요!"

        // Shizuku UserService 바인딩 시작
        CTSTrigger.bindService()

        btnAction.visibility = View.VISIBLE
        btnAction.text = "🔍 CTS 테스트 하기"
        btnAction.setOnClickListener {
            scope.launch {
                showStatus("VIS 트리거 시도 중...")

                // 서비스 연결 대기 (최대 3초)
                withContext(Dispatchers.IO) {
                    var wait = 0
                    while (!CTSTrigger.isServiceBound() && wait < 3000) {
                        Thread.sleep(200)
                        wait += 200
                    }
                }

                val result = withContext(Dispatchers.IO) {
                    CTSTrigger.triggerWithLog(this@SetupWizardActivity)
                }
                showStatus(result)
            }
        }

        btnNext.text = getString(R.string.btn_done)
    }

    // 완료
    private fun showCompleteStep() {
        tvStepTitle.text = getString(R.string.setup_complete)
        tvStepDescription.text = getString(R.string.setup_complete_desc)
        btnAction.visibility = View.GONE
        btnBack.visibility = View.INVISIBLE
        btnNext.text = getString(R.string.btn_done)
        btnNext.setOnClickListener { completeSetup() }
    }

    private fun completeSetup() {
        prefs.edit().putBoolean("first_run", false).apply()
        Toast.makeText(this, getString(R.string.setup_complete), Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun showStatus(message: String) {
        statusContainer.visibility = View.VISIBLE
        tvStatus.text = message
    }

    private fun isPackageInstalled(packageName: String): Boolean =
        PackageUtils.isInstalled(packageManager, packageName)

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
