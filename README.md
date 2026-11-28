# Luma Camera AI

한국어 기본 UI를 제공하는 오프라인 우선 개인용 AI 카메라 앱입니다.

## 지원 플랫폼
- Galaxy Android 네이티브 앱 (APK)
- 웹, Windows, macOS 버전은 개발 범위에서 제외

## 핵심 목표
- 촬영 전 구도, 촬영 각도, 거리감, 줌/렌즈 선택 가이드
- 수평선, 삼분할, 대칭, 여백, 피사체 위치 기반 구도 분석
- 지원되는 카메라에서 ISO, 셔터 속도, 초점, 화이트밸런스, 줌 수동 제어
- 촬영 후 구도, 수평, 선명도, 노출 평가 및 보정값 추천
- 온디바이스 AI만 사용하고 외부 AI API/유료 토큰을 사용하지 않음
- 사진은 기본적으로 기기 밖으로 전송하지 않음

## 현재 기술 구성
- Android UI/카메라: Kotlin + Jetpack Compose + CameraX/Camera2
- 분석 엔진: Kotlin 공통 모듈 (Android APK에서 사용)
- Android 카메라: CameraX 1.6.2 (CameraX Extensions 호환 기반)
- 영상/기하 분석: OpenCV 도입 검토
- 온디바이스 추론: LiteRT 또는 ONNX Runtime 검증 후 선정

## 구현 현황
- [x] Android 전용 앱 모듈 구성
- [x] 공통 한국어 다크 UI 초안
- [x] Android 앱 진입점 및 카메라 권한 선언
- [x] Android APK 빌드 작업 정의
- [x] 앱 내 GitHub 최신 릴리스 확인 및 버전 비교
- [x] APK 다운로드 후 SHA-256 무결성 확인
- [x] Android 패키지 설치 화면으로 업데이트 연결
- [x] 고정 서명키 기반 Release APK 배포 워크플로 준비
- [x] Android CameraX 전면/후면 카메라 전환 및 실시간 미리보기
- [x] 정규화된 피사체 위치/크기와 수평 기울기를 이용한 기본 구도 점수 엔진 및 단위 테스트
- [x] Android 사진 촬영/MediaStore 저장 기능
- [x] Android 후면 카메라 플래시 토글 제어
- [x] 즉시/3초/10초 셀프 타이머와 촬영 카운트다운
- [x] 단일 촬영/3장 연속 촬영 및 파일명 충돌 방지
- [x] 삼분할/중앙 십자/가이드 끄기 구도 오버레이 선택
- [x] Camera2 수동 센서 지원 기기에서 ISO·셔터 속도 제어 (지원 범위 자동 제한)
- [x] 지원 렌즈에서 Camera2 수동 초점 거리 조절 및 자동 AF 복귀
- [x] CameraX가 보고하는 렌즈별 실제 최소·최대 줌 범위 자동 조회 및 UI 반영
- [x] AI 인물 프레이밍 줌 추천을 현재 기기의 최소·최대 줌 범위 안으로 제한
- [x] 실시간 미리보기 밝기 히스토그램과 암부/하이라이트 클리핑 주의 안내
- [x] 선택형 실시간 포커스 피킹(밝기 경계 기반 초점 보조 오버레이)
- [x] 선택형 제브라 하이라이트 경고(고휘도 미리보기 영역 사선 오버레이)
- [x] Android 15+ 지원 기기의 CameraX 저조도 부스트(기기 지원 여부 확인 후 노출)
- [x] 카메라가 보고한 지원 모드에 한정한 화이트밸런스 프리셋
- [ ] 수동 화이트밸런스 Kelvin 제어
- [x] 카메라 프레임 기반 얼굴 감지 및 1초 간격 온디바이스 장면 라벨링
- [x] 인물·풍경·건축·음식 장면 후보 분류 및 신뢰도 기반 촬영 모드 제안
- [x] 회전 벡터 센서 기반 카메라 피치 추정치를 촬영 가이드에 전달 (물리 각도 측정이 아닌 방향 참고값)
- [x] 피사체 크기·위치·카메라 피치에 반응하는 실시간 사진작가식 구도/각도 가이드
- [x] 장면 분류가 신뢰 기준을 통과하면 수동 모드 선택보다 인식 장면을 우선해 촬영 조언 갱신
- [x] 전면 카메라 미러 좌표 보정 및 렌즈 전환 시 분석기 재생성
- [x] 공통 오프라인 사진 품질 분석 엔진(노출 클리핑·명암·디테일 프록시) 및 단위 테스트
- [x] 촬영 후 최근 촬영 사진의 오프라인 품질 분석 결과 표시
- [ ] Galaxy Android APK 실기기 검증

## 이번 대규모 업데이트
- 촬영 타이머는 Android CameraX 촬영 요청 전에 로컬 카운트다운을 실행합니다. 카운트다운/촬영 중 중복 요청을 차단하고 화면 종료 시 예약 작업을 정리합니다.
- 구도 오버레이를 삼분할, 중앙 십자, 끄기 중 선택할 수 있습니다.
- 타이머와 연사 프리셋/정규화 로직은 공통 코드로 분리하고 단위 테스트를 추가했습니다.
- 3장 연사는 CameraX 촬영을 순차 실행해 각 사진을 별도 JPEG로 저장하며, 마지막 사진의 품질 분석 결과를 표시합니다. 연속 촬영 파일명에는 밀리초를 포함해 이름 충돌 가능성을 줄였습니다.

## 최신 카메라 기능 추가
- Camera2 수동 센서 제어를 연결했습니다. 기기의 `MANUAL_SENSOR` 지원과 ISO/셔터 범위를 확인한 경우에만 수동 노출 UI를 표시합니다. ISO 슬라이더와 1/30~1/500초 셔터 프리셋을 선택한 뒤 적용할 수 있고, 자동 전환 시 Camera2 요청 옵션을 해제합니다.
- 수동 노출은 Camera2 capture request에 AE OFF, SENSOR_SENSITIVITY, SENSOR_EXPOSURE_TIME을 함께 적용합니다. ISO와 셔터는 실제 센서 범위로 제한하며, 노출 보정 슬라이더는 수동 노출 중 비활성화합니다.
- 화이트밸런스는 활성 렌즈의 `CONTROL_AWB_AVAILABLE_MODES`에 실제 포함된 모드만 노출합니다. Kelvin 직접 입력은 아직 구현하지 않았습니다.
- 포커스 피킹은 ImageAnalysis Y-plane을 40x30 격자로 샘플링하고 국소 밝기 변화가 큰 점을 최대 420개까지 초록색으로 강조합니다. 기본은 꺼짐이며, 켰을 때만 250ms 간격으로 계산합니다. 이는 고대비 경계 강조 기반의 보조 표시이지 실제 초점 정확도나 광학적 초점면을 측정하는 기능은 아닙니다.
- 제브라 경고는 Y-plane 32x24 샘플에서 밝기 245 이상인 위치를 최대 360개까지 노란 사선으로 표시합니다. 기본은 꺼짐이며 300ms 간격으로 계산합니다. 미리보기 밝기 기준의 하이라이트 주의 표시이며, RAW 클리핑 판정이나 최종 JPEG의 실제 손실 영역을 보증하지 않습니다.
- 저조도 부스트는 Android 15 이상이면서 활성 카메라가 CameraX `isLowLightBoostSupported()`를 보고한 경우에만 UI에 표시합니다. 실제 활성화는 `enableLowLightBoostAsync()` 완료 후 상태에 반영합니다. 플래시와 상호 배타적이며 수동 ISO·셔터가 켜져 있을 때는 충돌 방지를 위해 전환을 막습니다. 저조도 부스트는 미리보기/분석 스트림을 밝게 하는 기능이지 야간 모드의 다중 프레임 정지사진 합성과 동일하지 않습니다.
- 상단에서 전면/후면 카메라를 전환할 수 있습니다. 전면 카메라에서는 미리보기 좌우 반전을 고려해 얼굴 구도 좌표를 보정합니다.
- CameraX 모듈을 1.6.2로 통일해 2026년 11월 이후 일부 기기에서 제한되는 구버전 Extensions 호환 문제를 예방했습니다.

## 최신 AI 기능 추가
- ML Kit 번들 이미지 라벨링 모델(400개 이상 라벨)을 앱에 포함해 네트워크/첫 실행 모델 다운로드 없이 장면을 분석합니다.
- 카메라 분석 프레임에서 얼굴 감지는 계속 수행하고, 장면 라벨링은 최대 초당 1회로 제한해 CPU 사용량을 줄입니다. 분석은 전용 단일 스레드 executor에서 처리합니다.
- 공통 SceneClassifier는 인물·풍경·건축·음식 키워드를 보수적으로 분류하고, 신뢰도 55% 미만 또는 상위 후보 간 차이가 12%p 미만이면 추천을 표시하지 않습니다.
- AI가 추정한 장면은 후보로만 보여주며, 사용자가 '모드 적용'을 눌러야 촬영 프로필이 바뀝니다.

## 개발 원칙
기기가 지원하지 않는 카메라 설정은 노출하지 않습니다. 단일 카메라 영상으로 정확한 물리적 거리나 3D 각도를 확정할 수 없는 경우에는 추정 가이드로 표시합니다. 모델은 라이선스, 오프라인 동작, 기기 성능을 검토한 뒤 포함합니다.


## 실시간 AI 포토그래퍼 디렉팅 업데이트
- 촬영 장면 분류 결과가 신뢰 기준을 통과하면 해당 장면 프로필을 촬영 조언 엔진에 바로 반영합니다. 사용자가 선택한 촬영 의도는 장면 인식이 불확실할 때의 기준으로 유지합니다.
- 회전 벡터 센서의 표시 기준 피치 추정값을 공통 SceneAdvisor에 전달해 인물 눈높이, 풍경 카메라 방향, 건축물 상단 수렴 주의, 음식 탑뷰/45도 연출 가이드를 갱신합니다.
- 인물 얼굴의 화면 점유율과 위치에 따라 가까이/멀리 이동, 프레이밍 여백, 줌 제안을 보강합니다. 얼굴 감지 결과를 일반 사물의 위치나 거리로 오인하지 않습니다.
- 추천은 프리뷰 분석 신호에 기반한 오프라인 휴리스틱이며, 실제 피사체 거리·정확한 3D 각도·시선 방향을 측정했다고 주장하지 않습니다. 센서 방향 기준과 전면 카메라 차이는 실기기 검증이 필요합니다.


## AI 포토그래퍼 디렉팅 고도화 (2026-11-28)
- 인물 가이드는 얼굴 박스의 화면 위치에 따라 중앙 구도에서 삼분할 구도로 옮겨볼지, 가장자리 여백을 확보할지 구분합니다. 얼굴 위치가 없으면 일반 피사체 위치로 오인하지 않고 눈 위치/헤드룸에 대한 일반 가이드만 표시합니다.
- 음식 각도 가이드는 센서 피치 구간을 탑뷰/45도/낮은 각도로 나눠 연출 목적을 설명합니다. 건축은 카메라 위쪽 기울기로 인한 수직선 수렴 주의를 추가합니다. 풍경은 수평 오차와 전경·중경·원경의 층 구성을 안내합니다.
- 얼굴 크기 기반 줌은 인물 모드에만 적용합니다. 얼굴 감지 박스를 음식·건축·풍경의 일반 피사체 크기나 거리로 잘못 사용하지 않습니다.
- 센서/카메라 값의 NaN·무한대·잘못된 줌 범위를 방어하고, 각 장면별 추천 변화와 경계값 테스트를 추가했습니다. 모든 추천은 오프라인 휴리스틱이며 전문 사진가의 미학 판단을 대체하는 학습형 모델이라고 주장하지 않습니다.


## 기기별 줌 범위 및 APK 배포
- 줌 범위는 고정된 1×~10× 같은 숫자를 강제하지 않고, 현재 활성 카메라의 CameraX `ZoomState.minZoomRatio` / `maxZoomRatio` 값을 읽어 표시하고 적용합니다. 기기·렌즈별 지원 범위가 다르면 해당 값을 그대로 사용합니다.
- 줌 슬라이더는 최소 배율부터 최대 배율까지 전체 범위를 사용합니다. AI 인물 프레이밍 추천도 같은 하드웨어 범위로 제한하므로 지원하지 않는 배율을 요청하지 않습니다.
- GitHub Actions는 Android 공통 로직 테스트와 Debug APK 빌드만 수행하며, 성공 시 APK를 `luma-camera-ai-android-debug-apk` 아티팩트로 업로드합니다. Windows 빌드는 제거했습니다.

## 앱 내 업데이트 시스템 (v0.4.0)
- 앱 실행 시 GitHub Releases의 최신 버전을 확인하고, 현재 설치 버전보다 새 버전이 있을 때 앱 화면에 업데이트 카드를 표시합니다.
- APK를 앱 캐시에 내려받은 뒤 GitHub Release asset의 SHA-256 digest와 실제 파일 해시를 비교합니다. 검증에 실패한 APK는 삭제하고 설치하지 않습니다.
- 검증이 끝나면 Android 패키지 설치 화면을 엽니다. Android 보안 정책상 사용자의 설치 확인은 필요하며, 출처를 알 수 없는 앱 설치 권한도 최초 1회 허용해야 할 수 있습니다. 앱이 사용자 몰래 설치를 완료할 수는 없습니다.
- 배포 파이프라인은 `v*` 태그에서 고정 업로드 키로 서명한 APK를 GitHub Release에 `luma-camera-ai.apk` 이름으로 게시합니다. 앱은 이 Release asset을 업데이트 대상으로 사용합니다.
- 업데이트를 활성화하려면 저장소 Settings → Secrets and variables → Actions에 `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD` 네 가지 비밀값을 등록해야 합니다. 키스토어는 최초 한 번 생성하고 안전하게 백업해야 하며, 잃어버리면 기존 앱을 같은 서명으로 업데이트할 수 없습니다.
- **서명키 전환 주의:** 이전 0.3.0 Debug APK는 GitHub 러너의 임시 Debug 키로 서명되었으므로, 첫 고정키 Release APK로 전환할 때는 Android 서명 일치가 보장되지 않습니다. 고정키 Release로 한 번 설치한 이후부터 앱 내 업데이트가 같은 서명으로 이어집니다. 기존 설치본과 데이터 보존 여부는 실제 기기에서 확인해야 합니다.
- 자동 업데이트는 무인/강제 설치가 아니라, 새 버전 확인 → 검증된 다운로드 → Android 설치 확인 화면으로 이어지는 안전한 업데이트 방식입니다.

### Live subject framing (offline)

- Portrait mode uses the bundled face detector for face position and face-scale guidance.
- Landscape, architecture, and food modes use the bundled ML Kit stream-mode object detector to estimate subject bounding boxes and labels on-device.
- Subject boxes are normalized against the rotated camera frame and mirrored for the front camera before composition analysis.
- The shared subject-composition analyzer recommends safer edge margins, rule-of-thirds placement, and framing scale based on the largest reliable detected subject.
- Detection is throttled to roughly twice per second in non-portrait modes to limit camera-analysis load. No cloud inference, API token, or network connection is required.
- These are geometric framing hints from object boxes, not a learned aesthetic-quality score or guaranteed semantic identification.
