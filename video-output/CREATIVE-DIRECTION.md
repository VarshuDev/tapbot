# TapBot Launch Video — Creative Direction Document

**Project:** [TapBot (VarshuDev/tapbot)](https://github.com/VarshuDev/tapbot)  
**Deliverable:** `/video-output/CREATIVE-DIRECTION.md`  
**Target Release:** TapBot v2.4.0 (Build `240`)  
**Creative Theme:** **Deep Obsidian 3D**  
**Role:** Creative Director & Technical Storyboard Architect  
**Date:** November 11, 2026  
**Status:** Approved Creative Direction — Ready for Storyboard Review  

---

## 1. Executive Vision & Philosophy

### 1.1 The Narrative Problem & Opportunity
Most mobile software announcements rely on flat screencasts, floating abstract code, or generic neon tech montages. TapBot represents something fundamentally different: **a paradigm shift in personal compute infrastructure**. It repurposes physical Android hardware—devices that users already own or leave abandoned in desk drawers—into dedicated, sovereign 24/7 Telegram bot hosting servers.

The launch video must communicate this transformation with the weight, precision, and restrained confidence of a tier-1 developer infrastructure launch (in the design lineage of Linear, Stripe, and Framework).

### 1.2 Core Creative Principles
1. **The Smartphone is the Hero**:
   - The phone is not merely a screen running an app; it is an autonomous **hardware compute node**.
   - Every scene anchors around the physical device—its brushed metal edges, glass surfaces, internal silicon circuits, camera torch emitter, and hardware security enclave.
2. **Restrained Engineering Aesthetic (No AI Slop, No Neon Clutter)**:
   - Deep Obsidian dark-mode canvas (`#080A0E`) with high-contrast emerald and titanium accents.
   - Strictly avoid gratuitous floating code blocks, cartoon characters, excessive laser sweeps, or blinding sci-fi neon blooms.
   - Clean, geometric Three.js procedural lighting and ACES tone mapping.
3. **Hardware-Ground Truth**:
   - Every capability visualized in the video matches verified code in TapBot v2.4.0.
   - No simulated fake screenshots or exaggerated claims.
4. **Cinematic, Continuous Motion**:
   - The camera never abruptly halts. It utilizes continuous cinematic dolly pushes, orbital glides, and orthographic tilts with standardized cubic-bezier easing.

---

## 2. Technical Video Specifications

| Parameter | Approved Value | Engineering Rationale |
| :--- | :--- | :--- |
| **Duration** | **Exactly 40.00 Seconds** (960 Frames) | Optimal completion rate for YouTube Shorts, Reels, and X (Twitter) video cards. |
| **Aspect Ratio** | **9:16 Vertical** | Mobile-first consumption; matches the physical orientation of smartphones. |
| **Resolution** | **1080 x 1920 Pixels** | Full HD vertical standard. |
| **Frame Rate** | **24.000 FPS** | Cinematic cadence; reduces software GL rendering load while preserving film motion blur. |
| **Visual Engine** | **Nafay3D / Deep Dive 3D (Three.js)** | Procedural WebGL pipeline running in headless Chrome via `capture.mjs`. |
| **Voice Engine** | **Kokoro-82M Neural TTS** | Offline, high-fidelity neural voice synthesis (`am_michael`). |
| **Word Alignment** | **Faster-Whisper (`small.en`)** | Millisecond-accurate word boundary timestamps for kinetic caption cuts. |
| **Tone of Voice** | **Male, Deep, Confident, Developer-Native** | Measured pace (~2.0 words/second), authoritative, clear pauses. |

---

## 3. The Deep Obsidian Visual System

The visual design system directly mirrors TapBot's native Jetpack Compose Material 3 UI (`TapBotTheme.kt`), ensuring 100% brand consistency between the video and the actual application.

### 3.1 Color Palette & Token Hierarchy

```
┌────────────────────────────────────────────────────────────────────────┐
│  DEEP OBSIDIAN COLOR SYSTEM                                            │
├───────────────────┬───────────────────┬────────────────────────────────┤
│ Token Name        │ Hex Code          │ Three.js / Canvas Usage        │
├───────────────────┼───────────────────┼────────────────────────────────┤
│ Obsidian Canvas   │ #080A0E           │ World background, ambient fog  │
│ Obsidian Surface  │ #0E121A           │ Cards, phone back, HUD panels  │
│ Hardware Emerald  │ #10B981           │ Active state, signal, pulses   │
│ Titanium Bright   │ #DDE5E2           │ Typography, phone chassis      │
│ Dark Border       │ #1F2937           │ Wireframes, leader lines       │
│ Subdued Slate     │ #6B7280           │ Secondary labels, metrics      │
│ Alert Crimson     │ #EF4444           │ Battery warning, stop state    │
│ Telegram Azure    │ #229ED9           │ Telegram fly logo, chat badge  │
└───────────────────┴───────────────────┴────────────────────────────────┘
```

### 3.2 Material & Shader Definitions

1. **Brushed Titanium Phone Chassis**:
   - Three.js: `MeshStandardMaterial`
   - `color`: `#2A323D`
   - `metalness`: `0.85`
   - `roughness`: `0.28`
   - Reflects the subtle rim lights to define the phone's silhouette in the dark stage.
2. **Obsidian Front Glass & OLED Screen**:
   - Three.js: `MeshPhysicalMaterial`
   - `color`: `#080A0E`
   - `metalness`: `0.10`
   - `roughness`: `0.05`
   - `clearcoat`: `1.0`
   - `clearcoatRoughness`: `0.02`
   - Dynamic canvas texture mapped onto the screen face displaying crisp vector UI elements.
3. **Internal Silicon Substrate (Cutaway View)**:
   - Three.js: `MeshStandardMaterial`
   - `color`: `#0E121A`
   - `roughness`: `0.70`
   - Dark matte motherboard PCB featuring etched copper circuit traces (`#1F2937`) and glowing emerald signal conduits.
4. **Hardware Keystore Vault Core**:
   - Three.js: `MeshPhysicalMaterial` with active emissive channel
   - `color`: `#10B981`
   - `emissive`: `#10B981`
   - `emissiveIntensity`: `0.85`
   - `transmission`: `0.60`
   - `roughness`: `0.15`
   - A glowing cryptographic cube inside the phone's silicon, pulsed by procedural sine wave glow.
5. **Physical Camera LED Flash**:
   - Three.js: `MeshBasicMaterial` + directional volumetric spotlight cone
   - `color`: `#FFFFFF` with slight warm tint (`#FFF7ED`)
   - Emits an intense beam outward along the -Z axis when `/torch on` is triggered.

---

## 4. Lighting & Environment Setup

The 3D stage is an infinite dark obsidian studio designed to isolate the hero device with high dramatic contrast:

* **Key Rim Light**: Cool white/slate (`#DDE5E2`, intensity 1.8), positioned at `(-2.5, 3.0, 2.0)`, casting a crisp specular edge highlight along the phone's titanium rails.
* **Secondary Fill Light**: Subdued deep indigo (`#1E293B`, intensity 0.6), positioned at `(3.0, -1.0, 1.5)`, softening harsh shadows without washing out the OLED black floor.
* **Accent Emissive Bloom**: UnrealBloomPass (`threshold: 0.72, strength: 0.65, radius: 0.45`) giving glowing indicators (emerald status dots, keystore vault, flashlight) a controlled optical bloom.
* **Floor Shadow & Reflection Plane**: Ground plane at `y = -1.2` utilizing contact shadow maps with subtle specular falloff (`#080A0E`).
* **Depth of Field (BokehPass)**: Narrow focal plane (`aperture: 0.0035, maxblur: 0.012`) keeping the focus plane locked onto the active phone feature while allowing the background to drift into soft bokeh.

---

## 5. Kinetic Typography & 2D HUD Overlays

### 5.1 Word-Synced Captions
* **Font**: Inter-Black / IBM Plex Sans Bold (heavy, legible, modern).
* **Position**: Fixed safe zone at `y = 1520` (centered horizontally, well clear of bottom social media UI overlays).
* **Chunking**: 1 to 3 words per cadence beat (e.g., `WHY RENT *A SERVER | WHEN YOU *ALREADY | HAVE *ONE?`).
* **Active Word Styling**: Active spoken word highlighted in high-visibility Emerald `#10B981` or Gold `#FACC15`, accompanied by a subtle 1.08x scale pulse; inactive words rendered in clean Titanium `#DDE5E2` with a 2px black outline.

### 5.2 Technical HUD Pills & Callout Labels
* **Leader Line Labels (`label()`)**:
  - Horizontal line extending from 3D coordinates (via `screen(vec3, cam)`) to a dark pill container.
  - Border: 1px `#1F2937`.
  - Background: `#0E121A` (85% opacity).
  - Typography: JetBrains Mono, uppercase, letter-spacing 1.5px.
* **Telemetry Counters (`pill()`)**:
  - Compact rounded badges displaying verified device metrics (e.g., `BATTERY: 98% ⚡`, `OUTBOUND: 443`, `KEYSTORE: AES-256-GCM`).

---

## 6. Sound Design & Audio Direction

The audio landscape reinforces the tactile, physical reality of the hardware:

1. **Narration**:
   - Voice: `am_michael` via Kokoro-82M.
   - Character: Deep, resonant, calm, confident.
   - Processing: 80 Hz high-pass filter, subtle tube warmth, gentle compression to sit prominently over the audio bed.
2. **Musical Bed (`mix.py`)**:
   - Genre: Minimalist analog electronic drone + progressive sub-bass pulse.
   - Root Key: D Minor (dark, focused, modern).
   - Tempo: 110 BPM.
   - Structure: Low 55 Hz sub-drone foundation; rhythmic pulse enters at Scene 02 (5s); swells with textural risers into Scene 04 (Commander); tight, punchy conclusion.
3. **Diegetic Sound Effects (SFX)**:
   - **Scene 01**: Low-frequency resonant impact (VPS cancellation).
   - **Scene 02**: Sub-bass whoosh + mechanical latch (phone lock).
   - **Scene 03**: Gentle haptic pulse series (coroutine dispatch).
   - **Scene 04**: Crisp physical relay click (camera flashlight on) + haptic buzz.
   - **Scene 05**: Heavy vault servo latch + digital hum (Keystore lock).
   - **Scene 06**: Soft card flick / interface tap (catalog packages).
   - **Scene 07**: Authoritative sub-drop + clean metallic chime (final branding).

---

## 7. Creative Approval Sign-Off

- [x] Creative theme locked: **Deep Obsidian 3D**.
- [x] Exact duration verified: **40.00 seconds** (960 frames @ 24 FPS).
- [x] Resolution & format locked: **1080x1920 (9:16 vertical)**.
- [x] Color palette matched to TapBot production code (`#080A0E`, `#0E121A`, `#10B981`, `#DDE5E2`).
- [x] All 7 approved scenes detailed without non-existent features.
- [x] Engine constraints respected (Three.js procedural geometry, no photorealistic AI hallucinations).

**Document Approved for Phase 2 Production Planning.**
