# TapBot Launch Video — Production Technical Plan (Phase 2)

**Project:** [TapBot (VarshuDev/tapbot)](https://github.com/VarshuDev/tapbot)  
**Deliverable:** `/video-output/PRODUCTION-TECHNICAL-PLAN.md`  
**Target Video:** 1080x1920 (9:16 Vertical) | 40.00 Seconds (960 Frames @ 24 FPS)  
**Pipeline:** Nafay3D / Deep Dive 3D (`faceless-shorts`) Three.js Engine  
**Execution Environment:** Windows PowerShell / Git Bash, Node.js 22+, Python 3.10+, Google Chrome  

---

## 1. Project Directory & Architecture

The video production project will be organized inside the standard `faceless-shorts` project structure located at:  
`C:\Users\varshan\faceless-shorts\tapbot-launch\` (or within `video-output/tapbot-launch/`).

```
tapbot-launch/
├── script.json                 # Master narrative lines, pauses, and voice parameters
├── index.html                  # Three.js canvas mount & WebGL viewport runner
├── sheet.html                  # Multi-frame contact sheet inspector
├── data/
│   ├── words.json              # Whisper word timestamps (millisecond precision)
│   ├── cues.json               # Sound effect cue timestamps synchronized with cuts
│   ├── lines.json              # Line boundary markers and chunk boundaries
│   └── words.txt               # Plaintext transcript
├── audio/
│   ├── voice.wav               # Raw Kokoro-82M synthesized voiceover (24 kHz)
│   ├── music.wav               # Procedural analog bass drone & pulse track
│   └── final_mix.wav           # Mastered audio track (voice + music + diegetic SFX)
├── fonts/
│   ├── Inter-Black.otf         # Primary bold kinetic caption font
│   └── JetBrainsMono-Bold.ttf  # Monospace technical HUD font
├── vendor/
│   ├── three.module.js         # Three.js core WebGL library (r160+)
│   └── addons/                 # Post-processing: UnrealBloomPass, BokehPass, OutputPass
├── src/
│   ├── main.js                 # Frame-by-frame timeline runner & canvas blitter
│   ├── shots.js                # Master shot definitions (7 scenes mapped to cuts)
│   └── core/
│       ├── engine.js           # Three.js renderer, camera, tone mapping, post-processing
│       ├── stage.js            # Obsidian infinite studio, floor reflection, rim lights
│       ├── phone.js            # Procedural 3D smartphone chassis, camera, cutaway
│       ├── textures.js         # 1024x2048 dynamic 2D canvas UI screen generator
│       ├── hud.js              # Vector leader-line labels, telemetry pills, callouts
│       ├── captions.js         # Word-synced kinetic caption renderer
│       ├── timing.js           # Time segmenting, easing curves, lerp utilities
│       └── lib.js              # Vector math, color helpers, string sanitizers
├── tools/
│   ├── capture.mjs             # Headless Chrome frame-by-frame renderer & MP4 assembler
│   └── mix.py                  # NumPy/SciPy audio synthesis, mixing & normalizer
└── renders/
    ├── review/                 # QA contact sheets (all.jpg, frame slices)
    └── tapbot-v240-launch.mp4  # Final verified 1080x1920 MP4 release
```

---

## 2. Step-by-Step Phase 3 Execution Workflow

Once Phase 2 receives explicit user approval, production proceeds through the following sequential stages:

```
┌────────────────────────────────────────────────────────────────────────┐
│                        PHASE 3 EXECUTION FLOW                          │
└────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 1. SCAFFOLD & SETUP                                                    │
│ • Execute `new_video.sh deep-dive tapbot-launch`                       │
│ • Inject verified fonts and SVG asset copies into project directory   │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 2. SCRIPTING & AUDIO SYNTHESIS                                         │
│ • Author `script.json` with approved 7-scene text lines               │
│ • Run `voice.py tapbot-launch` using Kokoro-82M (`am_michael`)         │
│ • Run `words.py tapbot-launch` with Whisper to extract `words.json`   │
│ • Verify total running duration is exactly 40.00 seconds               │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 3. 3D MODELING & PROCEDURAL ASSETS                                     │
│ • Build `src/core/phone.js`: Procedural chassis, glass, torch emitter  │
│ • Build `src/core/textures.js`: Vector canvas textures for all 5 UIs   │
│ • Build `src/core/stage.js`: Obsidian studio floor, rim lighting rigs │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 4. SCENE IMPLEMENTATION (`src/shots.js`)                               │
│ • Program Scene 01: Problem (VPS Invoice vs. Phone)                    │
│ • Program Scene 02: Reveal (Phone Hero & TapBot Emblem)                │
│ • Program Scene 03: Runtime (Multi-Bot Fleet & Telemetry)              │
│ • Program Scene 04: Commander (Torch Flashlight & Telegram Controls)   │
│ • Program Scene 05: Security (Keystore Silicon Vault Cutaway)          │
│ • Program Scene 06: Ecosystem (Bot Store Packages & Install Bar)       │
│ • Program Scene 07: Closing (Manifesto, Final Emblem & GitHub CTA)     │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 5. SOUND DESIGN & AUDIO MASTERING                                      │
│ • Configure `mix.py` with 55 Hz sub-drone, 110 BPM kick, and risers    │
│ • Add cue markers for torch click, vault latch, and branding chime    │
│ • Mix voice (-2 dBFS) + music (-14 dBFS) + SFX (-8 dBFS) to -14 LUFS   │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 6. QA INSPECTION & FRAME VERIFICATION                                  │
│ • Run `capture.mjs check` (assert zero JavaScript errors)              │
│ • Generate contact sheet: `capture.mjs sheet review/all.jpg 0.5 10`    │
│ • Inspect all 80 contact sheet frames against QA Checklist             │
│ • Adjust camera paths, leader lines, or lighting wash if detected      │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 7. FINAL MP4 RENDER & ENCODING                                         │
│ • Render frames via headless Chrome at 24 FPS (960 frames total)       │
│ • Assemble MP4 via ffmpeg: H.264 video + AAC audio + faststart         │
│ • Output: `renders/tapbot-v240-launch.mp4`                             │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Detailed Component Implementation Specs

### 3.1 Script JSON Specification (`script.json`)
```json
{
  "title": "TapBot v2.4.0 Launch",
  "style": "deep-dive",
  "fps": 24,
  "width": 1080,
  "height": 1920,
  "voice": "am_michael",
  "sources": [
    "https://github.com/VarshuDev/tapbot",
    "https://developer.android.com/reference/android/security/keystore/KeyGenParameterSpec"
  ],
  "lines": [
    "Why rent a server, when you already have one?",
    "Meet TapBot. Your phone. Your bot infrastructure.",
    "Run multiple Telegram bots, directly on Android. In-process coroutines. Zero cloud fees.",
    "Control your device remotely. From Telegram. Trigger hardware sensors, live telemetry, and direct APK delivery.",
    "Your credentials. Encrypted on your device. Hardware-backed Keystore security.",
    "Discover bots. Install. Deploy. Community packages at the edge.",
    "Stop renting servers. Start running bots. TapBot."
  ],
  "gaps": [1.2, 1.1, 1.4, 1.6, 1.2, 1.3, 1.4]
}
```

### 3.2 Dynamic Canvas UI Renderer (`src/core/textures.js`)
Instead of loading static bitmap screenshots, `textures.js` will export a class `PhoneScreenPainter` that draws vector UI frames onto a high-resolution `1024x2048` canvas:

1. **`drawHeroHeader(ctx, time)`**:
   - Clears canvas with `#080A0E`.
   - Draws TopBar with `#0E121A` fill and 1px `#1F2937` bottom border.
   - Draws vector antenna path from `assets/icons/bot-antenna.svg`.
   - Draws version badge: `v2.4.0` in rounded pill with `#10B981` text and outline.
   - Draws glowing radial gradient in center that breathes with `Math.sin(time * 2)`.

2. **`drawFleetScreen(ctx, time)`**:
   - Renders Telemetry HUD card: Battery bar, RAM meter (`9.4 MB`), storage bar.
   - Renders 3 bot instance cards:
     - Commander Bot with `#10B981` active status pill and uptime ticker.
     - Exon Robot with blue group shield badge.
     - TapStream Audio with live waveform audio bars animated by `time`.

3. **`drawTelegramConsole(ctx, time, sceneTime)`**:
   - Renders dark Telegram chat interface (`#0F141C`).
   - Draws incoming user bubble at `sceneTime > 0.6s`: `/torch on`.
   - Draws bot confirmation bubble at `sceneTime > 1.2s`: `🔦 Flashlight enabled.`
   - Draws system status card at `sceneTime > 3.0s`.
   - Draws APK document delivery card at `sceneTime > 5.0s`.

4. **`drawKeystoreVault(ctx, time)`**:
   - Renders circuit trace grid.
   - Renders cryptographic cipher badge: `AES-256-GCM`.
   - Draws dynamic token masking animation: plaintext characters transform into `[REDACTED_TOKEN]`.

---

## 4. Hardware Acceleration & Render Performance Strategy

Rendering 960 frames of Three.js post-processed shaders (UnrealBloomPass + BokehPass) requires careful performance planning:

### 4.1 System Capabilities Assessment
* Host OS: Windows (PowerShell / Git Bash).
* Headless Chrome: Google Chrome available on PATH.
* Node.js & npm: Node 22+ verified in environment.
* Python: Python 3.10+ with pip verified.

### 4.2 Render Modes
1. **GPU-Accelerated Mode (Preferred)**:
   - When running on hardware with an active GPU:
     ```bash
     node tools/capture.mjs video renders/tapbot-v240-launch.mp4
     ```
   - Throughput: ~8–12 FPS (Total render time: ~80–120 seconds).
2. **Software OpenGL Fallback Mode**:
   - If running inside a virtualized environment or CPU-only shell:
     ```bash
     SOFTWARE_GL=1 QS='&res=0.75&aa=0&shadow=1024' FPS=24 node tools/capture.mjs video renders/tapbot-v240-launch.mp4
     ```
   - Throughput: ~0.5–1.0 FPS (Total render time: ~15–30 minutes).

### 4.3 QA Fast-Path Protocol
To avoid wasting rendering time, the team will **never render the full video without checking the contact sheet first**:
```bash
# 1. Quick Syntax and Scene Sanity Check (< 5s)
node tools/capture.mjs check

# 2. Contact Sheet Inspection (1 frame every 0.5s = 80 frames on 1 image, ~45s)
node tools/capture.mjs sheet review/all.jpg 0.5 10

# 3. Targeted Single Shot Iteration (e.g. inspecting Scene 04 flashlight)
node tools/capture.mjs frames review/s04 "18.5,19.0,19.5"
```

---

## 5. Post-Processing & Audio Assembly Specs

Once individual PNG frames are captured to temporary disk storage:

### 5.1 FFMPEG MP4 Assembly Command
```bash
ffmpeg -y \
  -framerate 24 \
  -i "tmp/frames/%06d.png" \
  -i "audio/final_mix.wav" \
  -c:v libx264 \
  -profile:v high \
  -level 4.2 \
  -preset slow \
  -crf 18 \
  -pix_fmt yuv420p \
  -c:a aac \
  -b:a 192k \
  -ar 48000 \
  -movflags +faststart \
  "renders/tapbot-v240-launch.mp4"
```

### 5.2 Lightweight Share Copy Generation (< 35 MB)
For direct distribution via Telegram chat or mobile messaging:
```bash
ffmpeg -y \
  -i "renders/tapbot-v240-launch.mp4" \
  -c:v libx264 \
  -crf 22 \
  -preset slow \
  -c:a aac \
  -b:a 128k \
  -movflags +faststart \
  "renders/tapbot-v240-launch-share.mp4"
```

---

## 6. Production Safety & Boundary Verification

The production implementation will strictly adhere to the constraints established in Phase 1:
- [x] **No Unverified Features**: The video shows verified runtimes (`Commander`, `Exon`, `TapStream`, `Gemini`, `RSS`, `Custom`). No mention of arbitrary DEX compiling or inbound webhook hosting.
- [x] **Verified Metrics Only**: Telemetry displays verified figures (`9.4 MB RAM`, `port 443 outbound`, `AES-256-GCM`).
- [x] **Zero Code Modification**: No Android source files in `app/`, `core/`, or `feature/` will be altered during video production.
- [x] **On-Device Sovereignty Grounded**: All token flows illustrate strictly on-device encryption into Android Keystore with zero cloud sync.
- [x] **Exact 40-Second Alignment**: Audio script and scene cuts are strictly locked to 40.00 seconds (960 frames).

**Document Approved for Phase 2 Production Planning.**
