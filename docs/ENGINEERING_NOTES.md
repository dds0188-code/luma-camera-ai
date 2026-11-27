# Engineering notes

## Platform scope
- Android: CameraX preview and image capture; save photos to MediaStore under Pictures/LumaCameraAI.
- Windows: Compose desktop shell and MSI packaging. Native Media Foundation camera integration is planned and is not yet implemented.
- macOS and web are explicitly out of scope.

## Composition analysis v0
The first common-code analyzer is a deterministic heuristic, not an ML model. It accepts normalized subject center, subject area ratio, and horizon tilt. It returns a transparent weighted score and Korean suggestions.

Current weighting:
- Rule-of-thirds proximity: 40%
- Subject framing/occupancy heuristic: 35%
- Horizon level: 25%

Scores must not be presented as objective artistic truth. The on-screen thirds grid is a visual guide; it is not yet connected to automatic subject detection or device-level sensors.

## Android capture flow
1. Request camera permission when the app opens camera mode.
2. Bind Preview and ImageCapture to the visible lifecycle.
3. Prevent duplicate capture requests while a shot is in progress.
4. Save JPEGs locally through MediaStore in Pictures/LumaCameraAI.
5. On Android 10+, use IS_PENDING during the save transaction.
6. Verify capture, orientation, and gallery visibility on physical Android devices; CI compilation alone is not hardware validation.

## Next integration sequence
- Enumerate camera capabilities and expose only supported manual settings.
- Add device orientation / horizon sensing.
- Add on-device subject detection with confidence-aware output.
- Add Windows Media Foundation capture source and camera control inspection.


## Android camera assistant v1
- CameraX now exposes the device-reported zoom ratio range and exposure compensation range. Unsupported ranges are disabled rather than simulated.
- Tapping the live preview requests CameraX focus and metering at that point.
- The Android app uses the bundled ML Kit face-detection model for on-device face bounding boxes. Frames are analyzed with KEEP_ONLY_LATEST and a single in-flight inference guard; frames are always closed on completion.
- Face center and normalized face area feed the common composition heuristic. The UI offers qualitative framing and zoom-in/zoom-out guidance, not a guaranteed ideal focal length.
- The rotation-vector sensor supplies a live roll / horizon-level guide, remapped for the current display orientation.
- Scope limitation: this model detects faces, not arbitrary objects or complete human bodies. It does not yet infer camera-to-subject height, exact camera angle, scene genre, lens equivalence, or lighting quality. Those require additional on-device models and calibration; do not present guesses as measured facts.
- Android hardware validation is still required for zoom/exposure ranges, sensor orientation, tap-to-focus, and sustained thermal/performance behavior.


## Shooting intent advisor v1
- Added four explicit shooting-intent profiles: portrait, landscape, architecture, and food.
- The common, offline SceneAdvisor returns concise composition/angle/scene guidance from available face-box position, face area, horizon roll, and device-reported zoom limits.
- Portrait zoom suggestions use the detected face area and are clamped to the camera's supported zoom range. The user must tap the recommendation to apply it; the app does not unexpectedly change framing.
- Landscape guidance prioritizes a wide view, horizon placement, and foreground depth. Architecture guidance prioritizes vertical lines, level camera roll, and avoiding excessive upward tilt. Food guidance is currently general technique guidance only.
- Important limitation: shooting intent is currently user-selected, not automatically classified. The ML Kit model still detects faces only. Face bounding boxes are never treated as food, object, or building detections. Exact subject distance, physical focal length, pitch angle, and scene recognition remain unimplemented.
- Shared tests cover zoom clamping, portrait area-to-zoom calculation, non-face-based landscape/food advice, and architecture guidance.

## Offline post-capture quality analysis groundwork

- Added a platform-independent analyzer for small, uniformly sampled grayscale previews.
- It reports exposure clipping (near-black / near-white), global contrast, and local-neighbour contrast as a detail/sharpness proxy, with Korean reshoot suggestions.
- Input validation rejects malformed grids and values outside 8-bit grayscale.
- The analyzer is deterministic and fully offline; it does not upload or retain image data.
- Important: local-neighbour contrast is not a calibrated focus or motion-blur detector. A noisy image can appear detailed, and a naturally smooth scene can score low.
- This commit adds the tested analysis engine only. The Android capture callback is not yet wired to feed a downsampled image into it, and no post-capture results screen is claimed complete.

## Android post-capture quality analysis v1

- After CameraX saves a JPEG to the local MediaStore, the app reopens that local URI and decodes a bounded thumbnail on a dedicated background executor.
- The thumbnail is reduced to a 64x64 grayscale sample and passed to the common offline quality analyzer. Full-resolution pixel arrays are not retained by the analyzer.
- The UI displays an overall heuristic score, exposure clipping, contrast, detail proxy, and reshoot suggestions after capture.
- Analysis is performed on-device; the original JPEG remains saved in Pictures/LumaCameraAI.
- Limitations: this is a luminance-statistics heuristic, not a trained image-quality model. The detail metric is local contrast and cannot reliably distinguish true focus from noise, texture, or motion blur. Scores are comparative hints, not objective image-quality grades.
- Physical-device validation is required for MediaStore URI decoding, orientation behavior, memory use, and responsiveness across device vendors. The current UI shows the most recent capture's result only; persistent history and editing-value recommendations are not yet implemented.


## Camera hardware capability inspection v1

- The Android rear camera reads Camera2 characteristics through the active CameraX camera.
- The UI reports device-advertised ISO sensitivity range, exposure-time range, manual-sensor capability, available white-balance modes, and flash availability.
- Capability inspection is read-only. It does not yet change ISO, shutter time, or white balance; those controls require safe auto/manual transitions and physical-device validation.
- Missing characteristics are shown as unavailable rather than guessed. Values describe the selected rear camera module and may differ across lenses.
- This is a capability inventory, not a guarantee that every reported mode behaves identically in every capture pipeline.


## Camera-first UI and native Android identity

- Reworked the shared Compose screen into a camera-first dark interface with a fixed live preview region and a separately scrollable controls/results area.
- Added a device-backed torch toggle. The control is disabled when the active rear camera does not report flash hardware.
- Added a native Android adaptive launcher icon using vector drawables; no HTML, remote image, or WebView is used for the installed app identity.
- The Android app remains a native Kotlin/Jetpack Compose application using CameraX for live preview, focus, and capture.
- Manual ISO/shutter/white-balance request controls remain a separate next implementation step; this UI pass does not imply those controls are already available.


## Self timer and composition overlay controls

- Added shared supported timer presets (0 / 3 / 10 seconds) with unit tests for allowed values and normalization.
- Android CameraX capture now supports a native pre-capture countdown. Duplicate capture requests are blocked while counting down or saving; pending callbacks are removed when the camera composable is disposed.
- Added selectable preview overlays: rule-of-thirds grid, center crosshair, and no overlay. The control strip is horizontally scrollable for narrow phone screens.
- The timer delays the existing CameraX capture and MediaStore save path; it does not create a web view, remote image dependency, or network requirement.
- Physical-device validation is still required for shutter timing, lifecycle interruption, and device-specific preview layout.


## Burst capture

- Added common single-shot / three-shot burst presets with unit tests for supported counts and normalization.
- Android burst capture executes CameraX ImageCapture sequentially rather than firing concurrent requests. Each successful shot is inserted as its own JPEG in MediaStore.
- The last saved image is passed to the existing local quality analyzer; earlier images are saved without redundant thumbnail analysis during the same burst.
- Capture filenames include milliseconds to reduce same-second filename collisions during rapid shooting.
- A burst remains marked in progress until all requested shots have either saved or an error occurs, preventing overlapping sequences.
- Hardware validation is still required to measure real inter-shot cadence and verify behavior on different CameraX camera implementations.


## On-device scene recognition v1

- Added the bundled ML Kit image-labeling model. It recognizes 400+ general labels locally and is available without a first-run model download; the trade-off is an approximately 5.7 MB increase to the Android app package according to Google's documentation.
- Live face detection remains active. General scene labeling is throttled to at most once per second to limit inference load.
- Camera analysis now runs on a dedicated single-thread executor rather than the main UI executor. Face and scene tasks share the same ImageProxy and the proxy is closed only after both tasks complete; synchronous setup failures also close it.
- A common, deterministic SceneClassifier maps only strong, unambiguous labels to portrait, landscape, architecture, or food. It rejects low-confidence (<55%) and ambiguous candidates (less than 12 percentage points of margin).
- The UI presents a candidate and confidence; the user explicitly applies the recommended shooting profile. This is not a claim of fully reliable scene understanding or generic object segmentation.
- The model is bundled to preserve offline-first behavior. No captured frame is uploaded.
- Physical-device performance, thermal impact, category accuracy, and low-light behavior still require validation.


## Camera switching and analyzer lifecycle hardening

- Added front/rear lens switching through CameraX `CameraSelector` and lifecycle re-binding. The active camera is checked with `ProcessCameraProvider.hasCamera` before binding.
- Face bounding-box normalization now accounts for 90°/270° image rotation and mirrors the horizontal coordinate for the front-facing preview.
- The face detector and bundled image labeler are keyed to the selected lens. This avoids reusing ML Kit clients after they have been closed when `DisposableEffect` restarts during camera switching.
- Switching cameras triggers full disposal of the old analysis executor and camera use cases before the new lens is bound.


## CameraX 1.6.2 compatibility and lifecycle audit

- Unified CameraX dependencies across the Android app and shared Android target at stable version 1.6.2.
- This upgrade addresses Google's notice that CameraX Extensions support is removed on some devices for applications using CameraX 1.5 or lower starting November 1, 2026. CameraX 1.6 and later retain Extensions support.
- CameraX Extensions are a future candidate for OEM-provided Night, HDR, Auto, Bokeh, and Face Retouch modes. They are not enabled yet: ExtensionSessionConfig does not support ImageAnalysis, so live face/scene analysis must be paused or redesigned while a vendor extension session is active.
- Hardened camera disposal: clear the ImageAnalysis analyzer, unbind the active provider, then shut down its dedicated executor and close ML Kit clients. This reduces the chance of analyzer callbacks being submitted to an already-shutdown executor during lens switching or screen disposal.
- The 1.6.2 Android build and Windows compile were observed successful in CI for the dependency-upgrade commit; the subsequent lifecycle-order change still requires its own final CI confirmation.


## Camera2 manual exposure controls

- Added an opt-in manual exposure panel only when the active camera advertises `REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR` and both ISO and exposure-time ranges are present.
- Manual mode applies `CONTROL_AE_MODE_OFF`, `SENSOR_SENSITIVITY`, and `SENSOR_EXPOSURE_TIME` together through CameraX's Camera2 interop control. This is applied to the active CameraX camera control; the app does not open or manage CameraDevice directly.
- ISO and shutter requests are clamped to the active camera's reported hardware ranges. The shared `ManualExposureOptions` helpers validate ranges and are covered by common tests.
- Automatic mode clears the interop request options. The exposure-compensation slider is disabled while manual exposure is active because AE compensation is not meaningful with AE disabled.
- Switching between front/rear cameras resets manual exposure mode to Auto; each lens is independently capability-checked. No support is assumed from a different camera.
- Device-specific caveat: the app build can validate API usage but cannot guarantee a particular OEM camera HAL accepts manual requests. Physical validation is still required on each target phone/lens. Unsupported cameras keep the manual panel hidden.


## Manual focus and live exposure histogram

- Added manual lens focus only when the active camera reports MANUAL_SENSOR capability, a finite positive LENS_INFO_MINIMUM_FOCUS_DISTANCE, and Camera2CameraControl interop is available. Values are diopters from 0 (infinity) through the lens-reported maximum.
- Manual focus uses CONTROL_AF_MODE_OFF plus LENS_FOCUS_DISTANCE. Returning to auto focus switches CONTROL_AF_MODE_CONTINUOUS_PICTURE. Lens support is re-evaluated on every camera switch.
- Added a low-cost preview luminance meter: sample 24x14 points from the Y plane at most every 350 ms, build a 16-bin histogram, and warn when at least 12% of samples are near black (<=5) or near white (>=250).
- This is a downsampled preview-luma guide, not a calibrated RAW histogram, waveform, zebra overlay, or final-capture exposure guarantee. UI explicitly communicates this limitation.
- Histogram samples are computed on the dedicated ImageAnalysis executor; only the compact histogram and warning are posted to the main executor. The existing KEEP_ONLY_LATEST strategy and shared analysis busy gate remain in place.
- Shared tests cover histogram binning, clipping warnings, empty samples, and out-of-range sample rejection. Manual focus normalization has separate common tests.
