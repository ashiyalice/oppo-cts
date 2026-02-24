# 구현 계획서 (Implementation Plan)

## 아키텍처

```
사용자 트리거 (앱 실행 / 타일 / 제스처)
        │
        ▼
    CTS 트리거 (VIS Service)
        │
        ├─ 성공 → Google Circle to Search UI 표시
        │
        └─ 실패 (CTS 비활성화) → Shizuku 우회
                                    ├─ 기본 어시스턴트 → Google 변경
                                    └─ GMS Flag 45631784 → true
                                            │
                                            ▼
                                        재시도 → CTS 발동
```

## 핵심 모듈 설명

### 1. CTS 트리거 (`trigger/`)

**CTSTrigger.kt** — 핵심 트리거
- MiCTS의 VIS(VisualInteractionService) 방식 구현
- `IVoiceInteractionManagerService`를 리플렉션으로 호출하여 CTS 실행
- 루팅 불필요, Android 9-15 지원
- Google이 기본 어시스턴트로 설정되어 있어야 동작

**CTSTileService.kt** — Quick Settings 타일
- 빠른 설정 패널에 CTS 타일 추가
- 한 번 탭으로 CTS 즉시 실행

### 2. Shizuku 연동 (`shizuku/`)

**ShizukuHelper.kt** — Shizuku 서비스 관리
- Shizuku 설치/실행 상태 확인
- 권한 요청 및 바인딩
- ADB 레벨 명령어 실행 래퍼

**AssistantSetter.kt** — 기본 어시스턴트 변경
- `Settings.Secure.ASSISTANT` 값을 Google로 변경
- Breeno 대신 Google 어시스턴트 활성화

**GmsFlagSetter.kt** — GMS Flag 수정
- `flag 45631784` → `true` 설정
- CTS 기능 강제 활성화

### 3. 접근성 서비스 (`service/`)

**OppoAccessibilityService.kt** — 제스처 트리거
- 접근성 서비스로 제스처 감지
- 화면 하단 길게 누르기 (네비바 영역) → CTS 실행
- ColorOS 최적화

### 4. UI (`ui/`)

**MainActivity.kt** — 앱 실행 = CTS 즉시 트리거
- MiCTS와 동일 동작: 앱 아이콘 탭 → CTS 발동
- 첫 실행 시 SetupWizard로 이동

**SetupWizardActivity.kt** — 단계별 설정 가이드
1. GMS 활성화
2. Google 앱 설치/업데이트
3. Shizuku 설치 & 시작
4. 기본 어시스턴트를 Google로 변경
5. GMS Flag 설정
6. Gemini 설치
7. 트리거 방식 선택

**SettingsActivity.kt** — 설정 화면
- 트리거 방식 설정
- 상태 대시보드

## 프로젝트 구조

```
oppocts/
├── README.md
├── docs/
│   ├── IMPLEMENTATION_PLAN.md    ← 이 문서
│   ├── CHECKLIST.md              ← 체크리스트
│   └── BUILD_ORDER.md            ← 구현 순서
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/oppocts/
│       │   ├── trigger/
│       │   │   ├── CTSTrigger.kt
│       │   │   └── CTSTileService.kt
│       │   ├── shizuku/
│       │   │   ├── ShizukuHelper.kt
│       │   │   ├── AssistantSetter.kt
│       │   │   └── GmsFlagSetter.kt
│       │   ├── service/
│       │   │   └── OppoAccessibilityService.kt
│       │   └── ui/
│       │       ├── MainActivity.kt
│       │       ├── SetupWizardActivity.kt
│       │       └── SettingsActivity.kt
│       └── res/
│           ├── xml/
│           │   └── accessibility_service_config.xml
│           ├── layout/
│           ├── values/
│           └── drawable/
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## 제약 사항

- **기기 위장(Device Spoof) 불가**: 루팅/Xposed 없이는 `ro.product.*` 시스템 속성 변경 불가. GMS Flag로 우회
- **Shizuku 재부팅 시 재시작 필요**: 비루팅 환경에서 Wireless ADB는 재부팅 시 끊김
- **VPN 필요**: CN 환경에서 Google 서비스 접근 시 VPN 필수
