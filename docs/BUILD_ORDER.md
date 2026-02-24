# 구현 순서 (Build Order)

구현은 아래 순서를 따릅니다. 각 단계는 이전 단계의 결과에 의존합니다.

---

## 1단계: 프로젝트 기반 (✅ 완료)

```
build.gradle.kts (루트)
settings.gradle.kts
gradle.properties
gradle-wrapper.properties
app/build.gradle.kts
app/proguard-rules.pro
app/src/main/AndroidManifest.xml
```

**결과**: 프로젝트가 Android Studio에서 열리고 빌드 가능한 상태

---

## 2단계: CTS 트리거 — 최우선 구현

```
app/src/main/java/com/oppocts/trigger/CTSTrigger.kt      ← 핵심!
app/src/main/java/com/oppocts/ui/MainActivity.kt
app/src/main/java/com/oppocts/trigger/CTSTileService.kt
```

**구현 순서**:
1. `CTSTrigger.kt` — VIS 서비스를 통한 CTS 트리거 로직
2. `MainActivity.kt` — 앱 실행 시 CTSTrigger 호출 (최소한의 Activity)
3. `CTSTileService.kt` — Quick Settings 타일 등록

**테스트**: APK 설치 → 앱 아이콘 탭 → CTS가 뜨면 성공

---

## 3단계: Shizuku 연동 — CTS 실패 시 우회

```
app/src/main/java/com/oppocts/shizuku/ShizukuHelper.kt
app/src/main/java/com/oppocts/shizuku/AssistantSetter.kt
app/src/main/java/com/oppocts/shizuku/GmsFlagSetter.kt
```

**구현 순서**:
1. `ShizukuHelper.kt` — Shizuku 바인딩 + 상태 확인
2. `AssistantSetter.kt` — 기본 어시스턴트를 Google로 변경
3. `GmsFlagSetter.kt` — GMS Flag 45631784 활성화

**테스트**: CTS 실패 → Shizuku 설정 → Google 앱 재시작 → CTS 재시도 → 성공

---

## 4단계: 접근성 서비스 — 제스처 트리거

```
app/src/main/res/xml/accessibility_service_config.xml
app/src/main/java/com/oppocts/service/OppoAccessibilityService.kt
```

**구현 순서**:
1. `accessibility_service_config.xml` — 서비스 설정
2. `OppoAccessibilityService.kt` — 제스처 감지 + CTSTrigger 호출

**테스트**: 접근성 서비스 활성화 → 네비바 길게 누르기 → CTS 발동

---

## 5단계: 설정 UI — 사용자 경험

```
app/src/main/java/com/oppocts/ui/SetupWizardActivity.kt
app/src/main/java/com/oppocts/ui/SettingsActivity.kt
app/src/main/res/layout/activity_setup_wizard.xml
app/src/main/res/layout/activity_settings.xml
```

**구현 순서**:
1. `SetupWizardActivity.kt` — 7단계 설정 가이드
2. `SettingsActivity.kt` — 트리거 설정 + 상태 대시보드
3. 레이아웃 XML

**테스트**: 첫 실행 → SetupWizard → 모든 단계 완료 → 메인으로

---

## 6단계: 리소스 & 마무리

```
app/src/main/res/values/strings.xml
app/src/main/res/values/themes.xml
app/src/main/res/values/colors.xml
app/src/main/res/drawable/ic_cts.xml
app/src/main/res/mipmap-*/ic_launcher.*
```

**구현 순서**:
1. 문자열 리소스 (중문/영문/한국어)
2. 테마/색상
3. 아이콘

---

## 의존성 그래프

```
[1. 프로젝트 기반]
        │
        ▼
[2. CTSTrigger] ← 다른 모든 모듈이 이것에 의존
        │
   ┌────┴────┐
   ▼         ▼
[3. Shizuku]  [4. 접근성 서비스]
   │         │
   └────┬────┘
        ▼
[5. 설정 UI] ← Shizuku 상태, 접근성 상태 표시
        │
        ▼
[6. 리소스 & 마무리]
```
