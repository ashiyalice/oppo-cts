# OppoCTS - Circle to Search Enabler for ColorOS

[English](README.md) | [한국어](README_ko.md) | [简体中文](README_zh.md) | [日本語](README_ja.md)

중국 내수용 기기(ColorOS) 등 구글의 '서클 투 서치(Circle to Search, CTS)' 기능이 기본적으로 제한되어 있거나 트리거 방식이 불편한 기기들을 위해, **순정과 99% 동일한 사용자 경험을 제공**하는 앱입니다.

## 📱 테스트된 기기
- **최적화 기기**: OPPO Find X8 Ultra (CN 내수용 롬 / ColorOS)
- 그 외 안드로이드 14 이상 기반의 중국 제조사 내수 롬(MIUI, HyperOS, OriginOS 등)에서도 유사하게 작동할 수 있으나 보장하지 않습니다.

## ✨ 주요 기능 및 특징

### 1. 🌟 완벽한 하단 제스처 바 트리거 (강력 추천)
가장 핵심적인 기능입니다. 기존 시즈쿠(Shizuku) 기반이나 접근성 서비스 우회 방식과 다르게 **와이파이가 꺼져있거나 시즈쿠 백그라운드 프로세스가 죽어도 작동**합니다.
- 화면 맨 아래 제스처 바 위치에 **보이지 않는 투명 오버레이**를 띄워 터치를 감지합니다.
- 순정 기능과 똑같이 하단 바를 **0.4초간 길게 누르면 CTS가 즉시 실행**됩니다.
- 일반적인 스와이프 제스처(홈으로 이동, 최근 앱 보기)와 완벽하게 구분되어 일상 사용에 방해를 주지 않습니다.
- **위치 미세 조정 지원**: 기기마다 다른 하단 베젤 크기나 해상도에 맞춰 오버레이 영역을 1픽셀 단위로 상하 이동(-100~+100) 및 두께 조절이 가능합니다. 설정 시 영역을 붉은색으로 보여주는 **디버그 모드**를 지원합니다.
- 전체 화면 동영상 재생, 게임 등에서 내비게이션 바가 숨겨지면 투명 트리거 영역도 **시스템과 동기화되어 즉시 자동으로 숨겨집니다.**

### 2. 시즈쿠(Shizuku)를 활용한 근본 해결 및 권한 주입
중국 롬 특성상 구글 앱이 CTS를 활성화하지 않습니다. 이 앱은 최초 설정 시 Shizuku의 강력한 ADB 권한을 활용해 구글 앱을 속여 CTS를 강제 활성화합니다.
- 기본 디지털 어시스턴트를 일일이 설정에 찾아가 변경할 필요 없이 한 번에 자동(GmsFlagSetter, AssistantSetter) 세팅합니다.
- 백그라운드 서비스가 램 정리에 의해 죽더라도 재부팅 시 백그라운드에서 조용히 복구합니다.

### 3. 다양한 백업 트리거 방식 지원
물리 버튼을 선호하는 사용자를 위해 접근성 서비스를 활용한 트리거도 포함되어 있습니다.
- 볼륨 + / - 버튼 더블 클릭 또는 길게 누르기
- 카메라 셔터 버튼 활용

## ⚙️ 작동 원리 (Technical Details)

이 프로젝트는 크게 **초기 환경 세팅 레이어**와 **CTS 후킹/트리거 레이어**로 동작합니다.

1. **GMS Flag Injection**:
   - Google 플레이 서비스 및 Search App 내부에는 CTS 작동 여부를 결정짓는 숨겨진 Flag가 존재합니다.
   - 셋업 단계에서 Shizuku 권한을 사용해 내부 패키지 매니저의 기능 구성을 수정, `com.google.android.googlequicksearchbox`가 현재 기기를 지원 기기로 인식하도록 만듭니다.

2. **Native Service Intent Hooker (Shizuku-Free Trigger)**:
   - 본래 안드로이드 시스템은 화면 하단을 길게 누르면 `VoiceInteractionManagerService.showSessionFromSession` 혹은 `ContextualSearchManagerService.startContextualSearch`를 시스템 권한으로 호출하여 CTS를 띄웁니다.
   - 본 앱은 **Android HiddenApiBypass** 라브러리를 활용해 자바 리플렉션(Reflection)으로 이러한 내부 API에 직접 접근합니다.
   - 이때 호출자(Caller)를 서클 투 서치가 기본 탑재된 패키지(예: `hyperOS_home` 혹은 시스템 UI)인 것처럼 **위장(Spoofing)**하여 시스템의 보안 차단을 우회합니다.
   - 이로 인해 최초 GMS 권한 주입만 끝나면, 실제 팝업을 띄우는 액션 자체는 Shizuku 프로세스에 전혀 의존하지 않는 완전 독립형(Shizuku-free)으로 작동하게 됩니다.

3. **WindowInsets Overlay Matching**:
   - 투명 투명 오버레이 구현 시, 하드코딩된 크기가 아닌 안드로이드 공식 `WindowInsetsCompat.Type.navigationBars()` API를 실시간 모니터링합니다. 
   - 가로 모드일 때 네비게이션 바가 우측으로 가는지 좌측으로 가는지, 혹은 전체 화면에서 아예 사라지는지에 대한 Insets 상태 변화를 감지해 오버레이(`WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`)를 시스템 제스처 바 위로 정확히 동기화시킵니다.

## 🤝 기여 (Credits)

본 앱의 패키지 스푸핑을 통한 네이티브 서비스 트리거 핵심 아이디어 및 우회 파라미터(flags=7, omni.entry_point=1 등)는 [MiCTS](https://github.com/mizhiyong/MiCTS) 프로젝트의 로직 분석을 바탕으로 이식 및 재구성되었습니다.

## ⚠️ 주의 및 면책사항
이 애플리케이션은 시스템의 숨겨진 API를 리플렉션으로 호출하고, 타사 앱(Google)의 플래그를 변조합니다.
- OS 업데이트(특히 구글 앱 자체 업데이트) 시 동작 방식이 막히거나 코드가 동작하지 않을 수 있습니다.
- 이 앱을 사용하여 발생하는 기기의 오작동이나 무한 루프 등 모든 소프트웨어적 문제에 대한 책임은 사용자 본인에게 있습니다.
