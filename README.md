# sleep_care-watch

수험생의 수면 데이터를 실시간으로 측정하고 분석하여 공부 중 졸음 방지와 수면 건강을 돕는 Wear OS 앱 프로젝트입니다.

현재 이 저장소는 `Kotlin + Jetpack Compose for Wear OS` 기반의 워치 앱 MVP 구현과 모바일 앱과의 연동 기능을 포함하고 있습니다.

## 현재 상태

- Wear OS 앱 프로젝트 생성 완료
- Jetpack Compose for Wear OS 기반 타일 및 워치 전용 UI 구현
- 실시간 심박수(HR) 및 수면 상태 모니터링
- 모바일 앱과의 Wear OS Data Layer 통신 (hr.ingest)
- 진동 패턴 알림 및 세션 관리Foreground 서비스 구현
- 단위 테스트 및 기본 동작 확인 완료

## 기술 스택

- Kotlin
- Jetpack Compose for Wear OS
- Horologist (Google Wear OS Libraries)
- Health Services SDK
- Wear OS Data Layer API
- Hilt
- Room

## 주요 폴더

- `app/src/main/java/com/sleepcare/watch/presentation`
  워치 화면, 타일, 컴포저블 테마
- `app/src/main/java/com/sleepcare/watch/service`
  심박수 측정 서비스, 세션 포그라운드 서비스
- `app/src/main/java/com/sleepcare/watch/data`
  Data Layer 연동 및 로컬 캐싱
- `docs`
  워치 전용 설계 및 UI 가이드

## 실행 및 테스트

### 필수 조건

- JDK 17
- Android SDK (Wear OS API level 30+)
- Wear OS 실기기 또는 에뮬레이터

### 빌드 및 실행

```bash
./gradlew clean :app:assembleDebug
```

### 단위 테스트

```bash
./gradlew testDebugUnitTest
```

## 모바일 앱 연동

- 워치 앱은 수집된 심박수 및 수면 상태 데이터를 모바일 앱으로 실시간 전송합니다.
- 모바일 앱에서 "공부 시작" 시 워치에서 진동 알림을 받거나 상태를 공유할 수 있습니다.
