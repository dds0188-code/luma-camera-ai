# Engineering notes

## Composition analysis v0
The first common-code analyzer is a deterministic heuristic, not an ML model. It accepts normalized subject center, subject area ratio, and horizon tilt. It returns a transparent weighted score and Korean suggestions.

Current weighting:
- Rule-of-thirds proximity: 40%
- Subject framing/occupancy heuristic: 35%
- Horizon level: 25%

This is deliberately a baseline. Later camera-frame detection must provide measured inputs and confidence values. Scores must not be presented as objective artistic truth.

## Camera integration sequence
1. Request permission only when the user enters camera mode.
2. Enumerate available cameras and supported capabilities.
3. Bind preview and capture lifecycle to the visible screen.
4. Expose only controls supported by the selected camera.
5. Save to the user's chosen local destination; do not upload images.
6. Validate on real devices; emulator camera support is not sufficient evidence.

## Platform adapters
- Android: CameraX Preview + ImageCapture + Camera2Interop capability inspection.
- Windows: Media Foundation capture source and camera control capability inspection.
- macOS: AVFoundation capture session and device format/control inspection.

Desktop adapters are separate implementations; a JVM shared UI does not imply that desktop camera APIs are already integrated.
