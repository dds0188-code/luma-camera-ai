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
