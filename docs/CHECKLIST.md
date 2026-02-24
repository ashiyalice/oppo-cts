# 개발 체크리스트 (Checklist)

## Phase 1: 프로젝트 셋업 ✅
- [x] Android 프로젝트 초기화 (Kotlin + Gradle)
- [x] `settings.gradle.kts` — 루트 프로젝트 설정
- [x] `build.gradle.kts` (루트) — AGP 8.7.3 + Kotlin 2.1.0
- [x] `app/build.gradle.kts` — 의존성 (Shizuku, AndroidX, Material 3)
- [x] `gradle.properties` + `gradle-wrapper.properties`
- [x] `AndroidManifest.xml` — 권한, 서비스, Provider 등록
- [x] `proguard-rules.pro`

## Phase 2: CTS 트리거 (핵심) 🔲
- [ ] `CTSTrigger.kt` — VIS 서비스 기반 CTS 실행
  - [ ] `IVoiceInteractionManagerService` 리플렉션 호출
  - [ ] CTS 트리거 성공/실패 감지
  - [ ] 실패 시 Shizuku 우회 안내
- [ ] `CTSTileService.kt` — Quick Settings 타일
  - [ ] 타일 아이콘 및 레이블
  - [ ] 탭 이벤트 → CTSTrigger 호출
- [ ] `MainActivity.kt` — 앱 실행 즉시 트리거
  - [ ] 첫 실행 감지 → SetupWizard
  - [ ] 이후 실행 → CTSTrigger 즉시 호출
  - [ ] 투명 Activity (사용자에게 안 보임)

## Phase 3: Shizuku 연동 🔲
- [ ] `ShizukuHelper.kt` — Shizuku 서비스 관리
  - [ ] Shizuku 설치 여부 확인
  - [ ] Shizuku 실행 상태 확인
  - [ ] 권한 요청 (SHELL 또는 ROOT)
  - [ ] IPC 바인딩
- [ ] `AssistantSetter.kt` — 기본 어시스턴트 변경
  - [ ] 현재 어시스턴트 확인
  - [ ] `Settings.Secure.ASSISTANT` → Google 컴포넌트 설정
  - [ ] Breeno 비활성화 가이드
- [ ] `GmsFlagSetter.kt` — GMS Flag 설정
  - [ ] GMS Flag `45631784` 현재 값 확인
  - [ ] Flag → `true` 설정
  - [ ] Google 앱 캐시 클리어 (선택)

## Phase 4: 접근성 서비스 (OPPO 제스처) 🔲
- [ ] `OppoAccessibilityService.kt` — 제스처 감지
  - [ ] 접근성 서비스 활성화 상태 확인
  - [ ] 네비바 영역 터치 이벤트 감지
  - [ ] 길게 누르기 → CTSTrigger 호출
  - [ ] ColorOS 호환성 처리
- [ ] `accessibility_service_config.xml` — 서비스 설정

## Phase 5: 설정 UI 🔲
- [ ] `SetupWizardActivity.kt` — 단계별 가이드
  - [ ] Step 1: GMS 활성화 안내
  - [ ] Step 2: Google 앱 설치 확인
  - [ ] Step 3: Shizuku 설치/시작 가이드
  - [ ] Step 4: 어시스턴트 변경 (자동)
  - [ ] Step 5: GMS Flag 설정 (자동)
  - [ ] Step 6: Gemini 설치 안내
  - [ ] Step 7: 트리거 방식 선택
- [ ] `SettingsActivity.kt` — 설정 화면
  - [ ] 트리거 방식 토글
  - [ ] Shizuku 상태 표시
  - [ ] GMS/Google/Gemini 상태 대시보드

## Phase 6: 리소스 & 디자인 🔲
- [ ] 앱 아이콘 (`ic_launcher`)
- [ ] CTS 타일 아이콘 (`ic_cts`)
- [ ] 레이아웃 XML (SetupWizard, Settings)
- [ ] 문자열 리소스 (다국어: 중문/영문/한국어)
- [ ] 테마/스타일 (Material 3, 라이트/다크)

## Phase 7: 검증 🔲
- [ ] 빌드 성공 확인
- [ ] VIS 트리거 동작 테스트
- [ ] Shizuku 연동 테스트
- [ ] 접근성 제스처 테스트
- [ ] OPPO CN 기기 실기 테스트
