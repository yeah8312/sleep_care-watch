# Sleep Care 워치-모바일 연동 계약

## 1. 목적

이 문서는 `Sleep Care` 워치 앱과 외부 모바일 앱 사이의 연동 계약을 정의한다.  
구현자는 이 문서만 보고 다음을 맞추면 된다.

- 워치와 폰이 주고받는 Data Layer path
- 공통 envelope 구조
- `session.start`, `session.stop`, `flush_policy`, `alert.vibrate`, `backfill_req`, `hr.sample`, `hr.batch`, `hr.ack`
- 상태 동기화와 오류 보고
- 워치의 `Open on Phone` 기본 deep link

이 문서는 워치 앱 관점에서 작성한다. 모바일 앱은 워치를 제어하고, 워치에서 올라오는 심박 샘플과 상태를 받아 Pi 쪽으로 중계한다.

## 2. 공통 규칙

### 2.1 대상 통신 채널

| 채널 | 용도 |
|---|---|
| `MessageClient` | 세션 시작/종료, flush 정책, 경고, ACK, 백필 요청처럼 즉시성이 필요한 명령 |
| `DataClient` | 현재 상태, 커서, 마지막 동기화 시각처럼 현재 스냅샷 성격의 정보 |
| `ChannelClient` | 이 문서 범위에서는 사용하지 않는다 |

### 2.2 공통 envelope

모든 메시지는 아래 공통 envelope를 사용한다.

| 필드 | 타입 | 설명 |
|---|---|---|
| `v` | number | 프로토콜 버전. 현재 `1` |
| `t` | string | 메시지 타입 |
| `sid` | string | 세션 ID |
| `seq` | number | 송신자 기준 증가 시퀀스 |
| `src` | string | `phone` 또는 `watch` |
| `sent_at_ms` | number | 송신 시각, epoch ms |
| `ack_required` | boolean | 수신 ACK 필요 여부 |
| `body` | object | 타입별 페이로드 |

### 2.3 공통 규칙

- `sid` 는 세션 단위로 유지한다.
- `seq` 는 송신자별 단조 증가 값으로 취급한다.
- ACK 는 누적형이며, 마지막으로 연속 확인된 샘플 시퀀스를 기준으로 처리한다.
- 워치의 원시 심박 샘플은 메모리 버퍼에 남길 수 있지만, 디스크 영속 저장은 MVP 범위가 아니다.
- 모바일은 워치에서 올라온 중복 샘플을 허용하고, 필요 시 idempotent 하게 처리한다.

## 3. Data Layer path

### 3.1 폰 → 워치

| Path | 용도 |
|---|---|
| `/sc/v1/ctl/start` | 세션 시작 |
| `/sc/v1/ctl/stop` | 세션 종료 |
| `/sc/v1/ctl/flush_policy` | flush 정책 변경 |
| `/sc/v1/ctl/backfill_req` | 누락 구간 백필 요청 |
| `/sc/v1/alert/vibrate` | 워치 진동 경고 |

### 3.2 워치 → 폰

| Path | 용도 |
|---|---|
| `/sc/v1/hr/live` | 최신 심박 1건 전송 |
| `/sc/v1/hr/batch` | ACK 되지 않은 심박 묶음 전송 |
| `/sc/v1/hr/ack` | 모바일의 누적 ACK |
| `/sc/v1/session/current` | 현재 상태 스냅샷 |
| `/sc/v1/session/{sid}/cursor` | 세션 커서 / 마지막 전달 지점 |
| `/sc/v1/session/error` | 워치 오류 보고 |

## 4. 세션 제어

### 4.1 `session.start`

**Path:** `/sc/v1/ctl/start`  
**방향:** 폰 → 워치  
**ACK:** 필요

```json
{
  "v": 1,
  "t": "session.start",
  "sid": "01HTY8M4Q4M0Q9P7J9W9K8Z1AB",
  "seq": 1,
  "src": "phone",
  "sent_at_ms": 1775578000000,
  "ack_required": true,
  "body": {
    "study_mode": "exam_prep",
    "flush_policy": {
      "normal_sec": 15,
      "suspect_sec": 5,
      "alert_sec": 2
    },
    "hr_required": true,
    "watch_vibration_enabled": true
  }
}
```

**워치가 기대하는 값**

- `sid` 는 현재 세션 식별자여야 한다.
- `flush_policy` 는 워치 버퍼의 전송 주기를 지정한다.
- `hr_required=true` 이면 워치는 심박 수집을 활성화한다.
- `watch_vibration_enabled=true` 이면 `alert.vibrate` 수신 시 진동을 수행한다.

**워치 동작**

- 앱 상태를 `STARTING` 으로 바꾼다.
- 센서 권한, 센서 지원 여부, Data Layer 연결을 확인한다.
- 모든 조건이 맞으면 `RUNNING` 으로 전환한다.

### 4.2 `session.stop`

**Path:** `/sc/v1/ctl/stop`  
**방향:** 폰 → 워치  
**ACK:** 필요

```json
{
  "v": 1,
  "t": "session.stop",
  "sid": "01HTY8M4Q4M0Q9P7J9W9K8Z1AB",
  "seq": 2,
  "src": "phone",
  "sent_at_ms": 1775578600000,
  "ack_required": true,
  "body": {}
}
```

**워치 동작**

- 수집을 중지한다.
- 남은 버퍼를 가능한 범위에서 flush 한다.
- 상태를 `STOPPING` 으로 전환한 뒤 `WAITING_PHONE` 으로 복귀한다.

## 5. 전송 정책

### 5.1 `flush_policy`

**Path:** `/sc/v1/ctl/flush_policy`  
**방향:** 폰 → 워치  
**ACK:** 필요

```json
{
  "v": 1,
  "t": "flush_policy",
  "sid": "01HTY8M4Q4M0Q9P7J9W9K8Z1AB",
  "seq": 3,
  "src": "phone",
  "sent_at_ms": 1775578010000,
  "ack_required": true,
  "body": {
    "normal_sec": 15,
    "suspect_sec": 5,
    "alert_sec": 2
  }
}
```

**워치 동작**

- 현재 세션의 flush 주기만 변경한다.
- 세션이 아직 시작되지 않았다면 메모리 상에 저장하고 `session.start` 후 적용한다.

### 5.2 `hr.sample`

**Path:** `/sc/v1/hr/live`  
**방향:** 워치 → 폰  
**ACK:** 필요

```json
{
  "v": 1,
  "t": "hr.sample",
  "sid": "01HTY8M4Q4M0Q9P7J9W9K8Z1AB",
  "seq": 42,
  "src": "watch",
  "sent_at_ms": 1775578012000,
  "ack_required": true,
  "body": {
    "sample_seq": 42,
    "sensor_ts_ms": 1775578011500,
    "bpm": 67,
    "hr_status": 1,
    "ibi_ms": [895],
    "ibi_status": [0],
    "quality_label": "ok",
    "delivery_mode": "live"
  }
}
```

**워치가 보내는 값**

- `sample_seq`: 세션 내 샘플 시퀀스
- `sensor_ts_ms`: 센서 측 timestamp
- `bpm`: 심박 수치
- `hr_status`: 원본 상태 코드
- `ibi_ms`: IBI 배열
- `ibi_status`: IBI 상태 배열
- `quality_label`: 앱 레벨 품질 라벨
- `delivery_mode`: `live`

**품질 라벨**

- `ok`
- `motion_or_weak`
- `detached`
- `busy_or_initial`

### 5.3 `hr.batch`

**Path:** `/sc/v1/hr/batch`  
**방향:** 워치 → 폰  
**ACK:** 필요

```json
{
  "v": 1,
  "t": "hr.batch",
  "sid": "01HTY8M4Q4M0Q9P7J9W9K8Z1AB",
  "seq": 80,
  "src": "watch",
  "sent_at_ms": 1775578030000,
  "ack_required": true,
  "body": {
    "from_sample_seq": 73,
    "to_sample_seq": 80,
    "delivery_mode": "batch",
    "samples": [
      {
        "sample_seq": 73,
        "sensor_ts_ms": 1775578022000,
        "bpm": 65,
        "hr_status": 1,
        "ibi_ms": [923],
        "ibi_status": [0],
        "quality_label": "ok"
      }
    ]
  }
}
```

**워치가 보내는 값**

- `from_sample_seq`, `to_sample_seq`
- `samples[]` 전체 목록
- 각 샘플은 `hr.sample` 과 같은 필드를 사용한다.

**워치 동작**

- ACK 되지 않은 샘플을 묶어 보낸다.
- 재연결 시 누락 구간이 있으면 백필 응답용으로 우선 전송한다.

### 5.4 `hr.ack`

**Path:** `/sc/v1/hr/ack`  
**방향:** 폰 → 워치  
**ACK:** 불필요

```json
{
  "v": 1,
  "t": "hr.ack",
  "sid": "01HTY8M4Q4M0Q9P7J9W9K8Z1AB",
  "seq": 19,
  "src": "phone",
  "sent_at_ms": 1775578030500,
  "ack_required": false,
  "body": {
    "ack_sample_seq": 80
  }
}
```

**워치 동작**

- `ack_sample_seq` 까지 연속으로 확인된 샘플을 기준으로 버퍼 상태를 갱신한다.
- ACK 이전 샘플은 제거하지 않는다.

### 5.5 `backfill_req`

**Path:** `/sc/v1/ctl/backfill_req`  
**방향:** 폰 → 워치  
**ACK:** 필요

```json
{
  "v": 1,
  "t": "backfill_req",
  "sid": "01HTY8M4Q4M0Q9P7J9W9K8Z1AB",
  "seq": 21,
  "src": "phone",
  "sent_at_ms": 1775578030800,
  "ack_required": true,
  "body": {
    "from": 73
  }
}
```

**워치 동작**

- `from` 이후 샘플을 `hr.batch` 로 재전송한다.
- 누락 구간이 이미 ACK 범위에 포함되어 있으면 중복 전송해도 된다.

## 6. 진동 경고

### 6.1 `alert.vibrate`

**Path:** `/sc/v1/alert/vibrate`  
**방향:** 폰 → 워치  
**ACK:** 필요

```json
{
  "v": 1,
  "t": "alert.vibrate",
  "sid": "01HTY8M4Q4M0Q9P7J9W9K8Z1AB",
  "seq": 27,
  "src": "phone",
  "sent_at_ms": 1775578033200,
  "ack_required": true,
  "body": {
    "level": 2,
    "pattern": "200,100,200,100,400"
  }
}
```

**워치가 기대하는 값**

- `level`: 경고 강도
- `pattern`: 밀리초 단위 진동 패턴 문자열

**워치 동작**

- 패턴 문자열을 쉼표 구분 숫자 목록으로 파싱한다.
- 진동 수행 중에는 상태를 `ALERTING` 으로 둔다.
- 진동이 끝나면 세션 상태에 따라 `RUNNING` 또는 `DEGRADED` 로 복귀한다.
- DND 또는 시스템 제한으로 실패하면 `vibrationFailed` 오류를 보고한다.

## 7. 상태 동기화

### 7.1 `session.current`

**Path:** `/sc/v1/session/current`  
**방향:** 워치 → 폰  
**ACK:** 불필요

```json
{
  "v": 1,
  "t": "session.current",
  "sid": "01HTY8M4Q4M0Q9P7J9W9K8Z1AB",
  "seq": 91,
  "src": "watch",
  "sent_at_ms": 1775578040000,
  "ack_required": false,
  "body": {
    "session_state": "RUNNING",
    "phone_link_state": "connected",
    "sensor_state": "ready",
    "flush_policy": {
      "normal_sec": 15,
      "suspect_sec": 5,
      "alert_sec": 2
    },
    "last_sample_seq": 80,
    "last_sample_at_ms": 1775578039000,
    "last_ack_sample_seq": 80,
    "last_ack_at_ms": 1775578030500,
    "permissions": {
      "health_sensor": "granted",
      "notification": "granted"
    },
    "vibration_failed": false
  }
}
```

**의미**

- 모바일이 워치의 현재 화면/상태를 빠르게 복원하기 위한 스냅샷이다.
- 앱 재진입, 재연결, 디버그 화면 갱신에 사용한다.

### 7.2 `session.{sid}.cursor`

**Path:** `/sc/v1/session/{sid}/cursor`  
**방향:** 워치 → 폰  
**ACK:** 불필요

```json
{
  "v": 1,
  "t": "session.cursor",
  "sid": "01HTY8M4Q4M0Q9P7J9W9K8Z1AB",
  "seq": 92,
  "src": "watch",
  "sent_at_ms": 1775578040100,
  "ack_required": false,
  "body": {
    "last_sample_seq": 80,
    "last_ack_sample_seq": 80
  }
}
```

**의미**

- 커서성 정보만 최소 크기로 전달할 때 사용한다.
- `session.current` 의 축약판으로 보면 된다.

## 8. 오류 보고

### 8.1 `session.error`

**Path:** `/sc/v1/session/error`  
**방향:** 워치 → 폰  
**ACK:** 불필요

```json
{
  "v": 1,
  "t": "session.error",
  "sid": "01HTY8M4Q4M0Q9P7J9W9K8Z1AB",
  "seq": 93,
  "src": "watch",
  "sent_at_ms": 1775578040200,
  "ack_required": false,
  "body": {
    "code": "permission_denied",
    "severity": "blocking",
    "message": "Health sensor permission is missing",
    "recoverable": true,
    "action": "open_settings"
  }
}
```

### 8.2 오류 코드

| 코드 | 의미 | 기본 복구 방법 |
|---|---|---|
| `permission_denied` | 센서 권한 부족 | 권한 재요청 |
| `sensor_unsupported` | 기기 미지원 | 세션 시작 차단 |
| `sensor_unavailable` | 센서 초기화 실패 | 재시도 또는 기기 점검 |
| `phone_link_lost` | 폰 연결 끊김 | 재연결 후 backfill |
| `vibration_failed` | 진동 수행 실패 | 설정/시스템 상태 확인 |
| `session_mismatch` | 세션 ID 불일치 | 폰에서 세션 재동기화 |

### 8.3 상태 전이와 에러

- `STARTING` 중 `permission_denied` 가 발생하면 `ERROR_PERMISSION` 으로 전환한다.
- `STARTING` 중 센서 초기화 실패 또는 미지원이면 `ERROR_SENSOR` 로 전환한다.
- `RUNNING` 중 폰 연결이 끊기면 `DEGRADED` 로 전환한다.
- `ALERTING` 중 진동 실패가 발생하면 `ERROR_LINK` 가 아니라 `vibrationFailed` 상태를 별도로 남긴다.

## 9. 상태 전이 규칙

### 9.1 워치 상태

| 현재 상태 | 조건 | 다음 상태 |
|---|---|---|
| `IDLE` | 앱 시작 | `WAITING_PHONE` |
| `WAITING_PHONE` | `session.start` 수신 | `STARTING` |
| `STARTING` | 권한, 센서, 링크 준비 완료 | `RUNNING` |
| `STARTING` | 권한 부족 | `ERROR_PERMISSION` |
| `STARTING` | 센서 미지원 또는 초기화 실패 | `ERROR_SENSOR` |
| `STARTING` | 폰 링크 불가 | `ERROR_LINK` |
| `RUNNING` | 폰 연결 끊김 또는 품질 저하 | `DEGRADED` |
| `RUNNING` | `alert.vibrate` 수신 | `ALERTING` |
| `DEGRADED` | 링크 복구 및 샘플 재동기화 완료 | `RUNNING` |
| `DEGRADED` | `alert.vibrate` 수신 | `ALERTING` |
| `ALERTING` | 진동 종료 | 직전 세션 상태로 복귀 |
| `RUNNING` / `DEGRADED` | `session.stop` 수신 | `STOPPING` |
| `STOPPING` | 정리 완료 | `WAITING_PHONE` |

### 9.2 구현 메모

- `alert.vibrate` 는 세션 종료를 의미하지 않는다.
- `session.stop` 은 현재 샘플 전송을 마무리하는 종료 명령이다.
- 상태 동기화 메시지는 화면 복구용이며, 세션 제어를 대체하지 않는다.

## 10. Open on Phone

워치 설정 화면의 `Open on Phone` 버튼은 기본적으로 아래 deep link 를 연다.

```text
sleepcare://watch/setup
```

### 동작 규칙

- 모바일 앱이 설치되어 있고 deep link 를 처리할 수 있으면 해당 화면으로 이동한다.
- 모바일 앱이 없거나 라우트가 없으면 사용자는 설치/초기 설정 흐름으로 안내한다.
- 이 버튼은 워치에서 `Sleep Log` 연동, 권한 안내, 초기 설정 복구를 시작하는 용도다.

## 11. 구현 체크

- `session.start` 와 `session.stop` 은 반드시 `sid` 를 포함한다.
- `hr.sample` 과 `hr.batch` 는 같은 샘플 모델을 공유한다.
- `hr.ack` 는 누적 ACK 로만 해석한다.
- `session.current` 는 폰이 화면 상태를 복원할 때 사용한다.
- 오류 보고는 사용자 안내와 디버그 로그를 함께 만족해야 한다.

