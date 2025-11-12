# Auto-Capture Pose Detection - Implementation Guide

## Overview

Implemented REAL pose detection for auto-capture mode. The system now detects when your head is facing left, right, up, or down based on face position in the frame.

---

## How It Works

### Face Position Detection

Instead of just using a timer, the system now:

1. **Detects face** using Haar Cascade classifier
2. **Calculates face center** position in the frame
3. **Determines pose** based on where the face is located
4. **Waits for stability** (15 frames = ~0.5 seconds)
5. **Auto-captures** when pose matches the recommended angle

### Pose Detection Logic

```
Frame Layout (Mirrored Camera):
┌─────────────────────────────┐
│     TOP (looking down)      │ relativeY < 0.45
├─────────────────────────────┤
│  LEFT │  CENTER  │  RIGHT   │
│  <0.45│ 0.4-0.6  │  >0.55   │
├─────────────────────────────┤
│    BOTTOM (looking up)      │ relativeY > 0.55
└─────────────────────────────┘

User Movement → Face Position:
- Look LEFT   → Face moves RIGHT   (relativeX > 0.55)
- Look RIGHT  → Face moves LEFT    (relativeX < 0.45)
- Look UP     → Face moves DOWN    (relativeY > 0.55)
- Look DOWN   → Face moves UP      (relativeY < 0.45)
- Look CENTER → Face centered      (0.4 < X < 0.6, 0.4 < Y < 0.6)
```

---

## Using Auto-Capture Mode

### Step-by-Step Guide

1. **Start Face Registration**

   - Admin Panel → Select user → Click "Register Faces"

2. **Enable Auto-Capture**

   - ✅ Check "🤖 Auto-Capture Mode"
   - Manual capture button will disable

3. **Follow Angle Instructions**

   ```
   Angle 1: Looking straight at camera
   → Face should be centered
   → Hold still for ~0.5 seconds
   → ✅ Auto-captures!

   Angle 2: Head tilted slightly left
   → Turn your head LEFT
   → Face will move to RIGHT side of frame
   → Hold still → ✅ Auto-captures!

   Angle 3: Head tilted slightly right
   → Turn your head RIGHT
   → Face will move to LEFT side of frame
   → Hold still → ✅ Auto-captures!

   ... and so on for all 10 angles
   ```

4. **Visual Feedback**

   - When pose matches: "✅ [Angle] - Hold still! (X/10)"
   - Counter shows stability progress
   - Auto-captures after 15 frames of stable pose

5. **Completion**
   - After 10 photos: Auto-trains model
   - Window closes automatically
   - Model ready for recognition!

---

## Pose Detection Parameters

### Adjustable Settings

```java
// Stability requirement
STABLE_FRAMES_REQUIRED = 15     // ~0.5 seconds at 30 FPS

// Cooldown between captures
AUTO_CAPTURE_COOLDOWN_MS = 2000 // 2 seconds

// Position thresholds
CENTER_MIN = 0.4                // 40% from edge
CENTER_MAX = 0.6                // 60% from edge
LEFT_THRESHOLD = 0.45           // Face on left
RIGHT_THRESHOLD = 0.55          // Face on right
UP_THRESHOLD = 0.45             // Face at top
DOWN_THRESHOLD = 0.55           // Face at bottom
```

### Why These Values?

- **15 frames**: Ensures user isn't just passing through the pose
- **2 seconds cooldown**: Gives time to move to next angle
- **40-60% center**: Reasonable tolerance for "centered" face
- **45/55% thresholds**: Clear distinction between center and sides

---

## Troubleshooting

### Auto-Capture Not Triggering

**Problem**: Checkbox enabled but not capturing

**Solutions**:

1. **Check face quality**: Must show "✅ Good quality"
2. **Hold pose longer**: Need 15 stable frames (~0.5 sec)
3. **Move face further**: LEFT/RIGHT need more head turn
4. **Better lighting**: Poor lighting prevents face detection
5. **Distance from camera**: Face too close or too far

### Wrong Angle Detection

**Problem**: System captures wrong angle

**Cause**: Mirror confusion - remember:

- Your left = Face moves right on screen
- Your right = Face moves left on screen

**Solution**: Watch the camera preview, not yourself

### Too Sensitive / Not Sensitive Enough

**Adjust thresholds in code**:

```java
// More sensitive (captures easier)
LEFT_THRESHOLD = 0.48
RIGHT_THRESHOLD = 0.52

// Less sensitive (requires more movement)
LEFT_THRESHOLD = 0.42
RIGHT_THRESHOLD = 0.58
```

---

## Comparison: Old vs New

| Feature              | Old Auto-Capture   | New Auto-Capture              |
| -------------------- | ------------------ | ----------------------------- |
| **Detection Method** | Timer only (2 sec) | Face position detection       |
| **Pose Awareness**   | ❌ None            | ✅ Detects left/right/up/down |
| **Stability Check**  | ❌ No              | ✅ 15 frames required         |
| **User Feedback**    | Generic timer      | ✅ Shows pose match status    |
| **Accuracy**         | Random angles      | ✅ Captures correct angles    |
| **User Experience**  | Just wait          | ✅ Move to pose and hold      |

---

## Advanced: How Pose Detection Works

### 1. Face Detection

```java
CascadeClassifier detector = new CascadeClassifier();
detector.load("haarcascade_frontalface_default.xml");

RectVector faces = new RectVector();
detector.detectMultiScale(currentFrame, faces);
```

### 2. Face Center Calculation

```java
Rect faceRect = faces.get(0);
int faceCenterX = faceRect.x() + faceRect.width() / 2;
int faceCenterY = faceRect.y() + faceRect.height() / 2;
```

### 3. Relative Position

```java
double relativeX = (double) faceCenterX / frameWidth;  // 0.0 to 1.0
double relativeY = (double) faceCenterY / frameHeight; // 0.0 to 1.0
```

### 4. Pose Matching

```java
switch (requiredAngle) {
    case FRONT:
        return (relativeX > 0.4 && relativeX < 0.6) &&
               (relativeY > 0.4 && relativeY < 0.6);

    case LEFT:
        return relativeX > 0.55; // Face on right side

    case RIGHT:
        return relativeX < 0.45; // Face on left side

    // etc...
}
```

### 5. Stability Counter

```java
if (poseMatches) {
    stableFrameCount++;
    if (stableFrameCount >= STABLE_FRAMES_REQUIRED) {
        capturePhoto(); // Capture!
    }
} else {
    stableFrameCount = 0; // Reset if pose changes
}
```

---

## Future Enhancements (Optional)

### More Precise Detection

Could be improved with:

1. **Face Landmarks** (68-point facial keypoints)

   - Detect exact head rotation angles
   - More accurate than position-based detection

2. **Head Pose Estimation** (Pitch, Yaw, Roll)

   - Calculate exact 3D head orientation
   - Requires solvePnP with 3D face model

3. **Eye Gaze Tracking**

   - Ensure eyes are looking at camera
   - Better photo quality

4. **Expression Detection**
   - Detect smiling vs. neutral
   - Capture variety of expressions

### Implementation Example (Advanced)

```java
// Using dlib face landmarks
FacemarkLBF facemark = FacemarkLBF.create();
facemark.loadModel("lbfmodel.yaml");

MatVector landmarks = new MatVector();
facemark.fit(frame, faces, landmarks);

// Calculate head pose from landmarks
Mat rotationVector = new Mat();
Mat translationVector = new Mat();
solvePnP(objectPoints, landmarks.get(0),
         cameraMatrix, distCoeffs,
         rotationVector, translationVector);

// Get yaw, pitch, roll angles
double yaw = rotationVector.ptr(0).get(1);
double pitch = rotationVector.ptr(0).get(0);
double roll = rotationVector.ptr(0).get(2);
```

---

## Summary

### What Changed

- ✅ Removed timer-only auto-capture
- ✅ Added real face position detection
- ✅ Added pose matching logic for 10 angles
- ✅ Added stability counter (15 frames)
- ✅ Added visual feedback for pose match
- ✅ Fixed camera overlay visibility

### Benefits

- 🎯 **Accurate angle capture** - Captures correct poses
- 👤 **User-friendly** - Clear guidance on what to do
- ⚡ **Fast** - ~0.5 seconds per angle when done right
- 🤖 **Automated** - No manual clicking needed
- 💯 **Quality** - Better training data for recognition

---

**Status**: ✅ Implemented and Ready to Test  
**Date**: November 12, 2025  
**Version**: IceFX 3.1 - Intelligent Auto-Capture
