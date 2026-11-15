# TapBot Launch Video — Scene Asset Plan (Phase 2)

**Project:** [TapBot (VarshuDev/tapbot)](https://github.com/VarshuDev/tapbot)  
**Deliverable:** `/video-output/SCENE-ASSET-PLAN.md`  
**Target:** 1080x1920 (9:16 Vertical) | 40.00 Seconds | Nafay3D Procedural Three.js Engine  
**Visual Style:** Deep Obsidian 3D  

---

## 1. Asset Inventory Matrix

Every asset required across all 7 scenes is cataloged below, along with its source location, type, and implementation method:

| Scene | Asset Identifier | Type | Source Location / Generation Method | Status in Repo |
| :---: | :--- | :---: | :--- | :---: |
| **All** | **Procedural 3D Phone Model** | 3D Mesh | `src/core/phone.js` (Three.js procedural geometry & shaders) | Needs Code |
| **All** | **Deep Obsidian Studio Stage** | 3D Stage | `src/core/stage.js` (Floor reflection plane, ambient lighting rig) | Needs Code |
| **All** | **Kinetic Caption Engine** | 2D Canvas | `src/core/captions.js` (Inter-Black font, yellow active word highlight) | Built into Skill |
| **All** | **Technical HUD Pills & Labels** | 2D Canvas | `src/core/hud.js` (Vector pills, leader lines, monospace callouts) | Needs Code |
| **01** | `cloud-edge.svg` | Vector SVG | `assets/icons/cloud-edge.svg` | **VERIFIED IN REPO** |
| **01** | `ram-chip.svg` | Vector SVG | `assets/icons/ram-chip.svg` | **VERIFIED IN REPO** |
| **01** | **VPS Invoice 3D Card** | 3D Mesh | Procedural glass slab with red `$20/mo` embossed badge | Needs Code |
| **02** | `bot-antenna.svg` | Vector SVG | `assets/icons/bot-antenna.svg` | **VERIFIED IN REPO** |
| **02** | `banner.svg` | Vector SVG | `assets/banner.svg` | **VERIFIED IN REPO** |
| **02** | **TapBot 3D Antenna Emblem** | 3D Extrusion| `THREE.SVGLoader` extrusion of `bot-antenna.svg` with emerald emissive | Needs Code |
| **02** | **App Header Screen Texture** | 2D Canvas | Dynamic canvas texture rendering TapBot top bar, version pill, and status | Needs Code |
| **03** | `fleet-shield.svg` | Vector SVG | `assets/icons/fleet-shield.svg` | **VERIFIED IN REPO** |
| **03** | `coroutine-orbit.svg` | Vector SVG | `assets/icons/coroutine-orbit.svg` | **VERIFIED IN REPO** |
| **03** | `watchdog-scan.svg` | Vector SVG | `assets/icons/watchdog-scan.svg` | **VERIFIED IN REPO** |
| **03** | **My Fleet Screen Texture** | 2D Canvas | Dynamic canvas rendering 3 bot cards (`Commander`, `Exon`, `TapStream`) & telemetry | Needs Code |
| **03** | **Coroutine Data Stream** | 3D Particles| `THREE.InstancedMesh` flowing particles along orbital splines | Needs Code |
| **04** | `remote-hardware.svg` | Vector SVG | `assets/icons/remote-hardware.svg` | **VERIFIED IN REPO** |
| **04** | `apk-package.svg` | Vector SVG | `assets/icons/apk-package.svg` | **VERIFIED IN REPO** |
| **04** | `telegram-fly.svg` | Vector SVG | `assets/icons/telegram-fly.svg` | **VERIFIED IN REPO** |
| **04** | **Camera Flashlight Emitter** | 3D Mesh | Volumetric cone mesh with SpotLight beam & procedural lens flare | Needs Code |
| **04** | **Telegram Chat Bubble Meshes**| 2D/3D Cards | Rounded rectangle cards with `/torch on`, `/status`, and `/apk` responses | Needs Code |
| **05** | `keystore-vault.svg` | Vector SVG | `assets/icons/keystore-vault.svg` | **VERIFIED IN REPO** |
| **05** | `android-shield.svg` | Vector SVG | `assets/icons/android-shield.svg` | **VERIFIED IN REPO** |
| **05** | **Motherboard Circuit Plate** | 3D Mesh | Dark silicon PCB plane with procedural copper traces and clipping plane | Needs Code |
| **05** | **Cryptographic Vault Cube** | 3D Mesh | Beveled physical glass cube with pulsating green emissive core (`AES-256-GCM`) | Needs Code |
| **06** | `rollback-loop.svg` | Vector SVG | `assets/icons/rollback-loop.svg` | **VERIFIED IN REPO** |
| **06** | **.botpkg Package Cards** | 3D Meshes | Array of 4 floating cards with category icons, versions, and install bar | Needs Code |
| **07** | `crown-author.svg` | Vector SVG | `assets/icons/crown-author.svg` | **VERIFIED IN REPO** |
| **07** | **Final GitHub CTA Badge** | 2D/3D Card | Rounded pill card with GitHub emblem and URL `github.com/VarshuDev/tapbot` | Needs Code |

---

## 2. 3D Procedural Model Specifications

Because the Nafay3D engine renders through Three.js in headless Chrome, 3D models are constructed procedurally in code for maximum sharpness, zero download latency, and full programmatic animation control.

### 2.1 The Hero Smartphone Model (`src/core/phone.js`)
* **Chassis Body**:
  - Dimensions: Width `0.74m`, Height `1.52m`, Depth `0.082m` (scaled down by 10x in Three.js space to `0.74 x 1.52 x 0.082`).
  - Edge Profile: Chamfered rounded rectangle using `ExtrudeGeometry` from a 2D rounded path (corner radius `0.08`).
  - Material: `MeshStandardMaterial` with brushed titanium normal map, `metalness: 0.85`, `roughness: 0.28`, color `#2A323D`.
* **Bezel & Display**:
  - Display Face: Plane positioned at `z = 0.0415` (flush with front edge).
  - Screen Aspect Ratio: Modern 19.5:9 display with subtle curved corners and camera punch-hole cutout at top center.
  - Material: `MeshBasicMaterial` driven by dynamic `THREE.CanvasTexture` (1024x2048 canvas).
* **Rear Panel & Camera Island**:
  - Rear Plate: Matte obsidian glass (`#0E121A`, `roughness: 0.45`).
  - Camera Bump: Rounded rectangle raised `0.015` units at top left with dual dark glass lenses.
  - Flash LED Emitter: Circular cylinder (`radius: 0.025, depth: 0.005`) with bright white emissive face linked to a controllable Three.js SpotLight.
* **Internal Hardware Cutaway (Scene 05)**:
  - Motherboard substrate plane at `z = 0.0` beneath the screen.
  - Animated clipping plane: `new THREE.Plane(new THREE.Vector3(0, 1, 0), clipHeight)` smoothly reveals internal hardware components.

---

## 3. 2D Dynamic Canvas Texture Plan (`src/core/textures.js`)

To guarantee 100% visual authenticity without relying on raster screenshots, all phone screen states are drawn directly onto an offscreen 1024x2048 HTML5 canvas using native vector drawing instructions:

### Texture 1: TapBot Header & Idle State (Scene 02)
* Deep Obsidian background (`#080A0E`).
* Top App Bar: TapBot Antenna icon, title "TapBot", green status indicator dot.
* Version Pill: "v2.4.0" in `#10B981` border.
* Center Graphic: Large faint vector watermark of `bot-antenna.svg` with glowing concentric circular rings.

### Texture 2: My Fleet HUD State (Scene 03)
* Top Telemetry HUD Grid (2x2 layout):
  - Battery: `94% ⚡ (Discharging • 1-Tap Doze Exemption)`
  - RAM: `9.4 MB / 7.8 GB (ART Coroutine Heap)`
  - Storage: `54.2 GB / 128 GB Free`
  - Network: `Wi-Fi (5 GHz) • Outbound TLS 1.3`
* Active Bot Cards (Vertical stack):
  - Card 1: `Commander Bot` • `v2.4.0` • Status: `RUNNING (Uptime: 48h 12m)`
  - Card 2: `Exon Robot 2.0` • `v2.0.0` • Status: `RUNNING (12 Groups)`
  - Card 3: `TapStream Audio` • `v2.0.0` • Status: `RUNNING (Speaker & Chat)`

### Texture 3: Telegram Chat Console (Scene 04)
* Dark Telegram Chat Theme (`#0F141C`).
* Header: "TapBot Commander" • "bot • 🟢 active on Android".
* User Chat Bubble (Right-aligned, `#2B5278`):
  - Text: `/torch on` (Timestamp: `12:42 PM`)
* Bot Response Bubble (Left-aligned, `#18222D`):
  - Text: `🔦 Camera flashlight enabled remotely.`
* User Chat Bubble 2: `/status`
* Bot Response Bubble 2: System Telemetry diagnostic card with ASCII meters.
* User Chat Bubble 3: `/apk`
* Bot File Attachment: `tapbot-v2.4.0.apk` with blue document icon and verified hash.

### Texture 4: Hardware Keystore Cryptographic Vault (Scene 05)
* Dark PCB substrate with etched circuit buses.
* Center HUD: `ANDROID KEYSTORE SECURITY MODULE`.
* Cipher specification: `AES-256-GCM (128-bit Tag)`.
* Master Key status: `STRONGBOX / TEE • NON-EXPORTABLE`.
* Display Security: `FLAG_SECURE ENFORCED`.

### Texture 5: Bot Store Package Catalog (Scene 06)
* Store Tab Header: Search bar + Category chips (`Utilities`, `Media`, `Management`, `AI`).
* Featured package grid with package size badges (`1.2 MB .botpkg`) and download metrics.

---

## 4. Audio Asset & Sound Synthesis Plan (`src/tools/mix.py`)

All audio is generated locally with zero external dependencies:

### 4.1 Voiceover Audio
* File: `audio/voice.wav`
* Engine: Kokoro-82M via `voice.py --voice am_michael`.
* Output: 24 kHz 16-bit mono WAV.
* Timestamps: `data/words.json` generated by Whisper `small.en` via `words.py`.

### 4.2 Musical Bed Synthesis (Procedural NumPy / SciPy)
* File: `audio/music.wav`
* Composition:
  - Sub-Drone (0s–40s): 55 Hz sine wave with subtle 0.15 Hz pitch modulation, low-passed at 120 Hz.
  - Rhythm Pulse (5s–40s): 110 BPM muted 808 kick pulse entering in Scene 02.
  - Atmospheric Pad (10s–40s): Detuned saw chord (D minor: D2, A2, F3) with slow resonant filter sweep.
  - Tension Riser (20s–25s): Exponential frequency sweep into Scene 05.
  - Climax Hit (35s): 40 Hz sub-drop with 1.8s decay.

### 4.3 Diegetic Sound Effects (SFX)
* `sfx_vps_strike.wav`: 80 Hz resonant click with sharp cutoff (Scene 01, 2.2s).
* `sfx_wake_chime.wav`: 440 Hz harmonic bell chime with gentle stereo spread (Scene 02, 5.4s).
* `sfx_bot_activate.wav`: Crisp 1200 Hz short blip x3 (Scene 03, 10.5s).
* `sfx_torch_relay.wav`: Real mechanical camera shutter / relay solenoid click (Scene 04, 18.8s).
* `sfx_haptic_buzz.wav`: Dual 160 Hz low-frequency vibration pulse (Scene 04, 20.5s).
* `sfx_vault_latch.wav`: Heavy metallic servo latch engage sound (Scene 05, 27.2s).
* `sfx_install_success.wav`: Upward arpeggio (C-E-G-B) soft sine tone (Scene 06, 32.6s).
* `sfx_climax_gong.wav`: Deep bronze cinematic gong hit with 3.5s decay (Scene 07, 37.6s).

---

## 5. Asset Readiness & Gap Resolution

| Asset Area | Current Repo Status | Gap Resolution for Phase 3 |
| :--- | :--- | :--- |
| **Vector SVGs** | All 13 icons verified in `assets/icons/` | Ready. Ingest directly via `THREE.SVGLoader` or canvas drawing. |
| **Branding SVGs** | `assets/banner.svg` verified | Ready. Use for top-level brand logo extrusion. |
| **Screen UI Assets** | No raster screenshots in repo | **Positive Advantage**: Avoids raster blur. Will be procedurally drawn via `src/core/textures.js` at 2x retina canvas resolution. |
| **3D Smartphone Model** | Not yet modeled | Will be authored in `src/core/phone.js` using Three.js CSG / standard geometries. |
| **Voiceover File** | Script drafted in Phase 2 | Will be synthesized in Phase 3 step 1 using `voice.py`. |
| **Word Timings** | Formulated in Phase 2 | Will be extracted in Phase 3 step 2 using `words.py`. |
| **Audio Mix Script** | Sample exists in `faceless-shorts` | Custom sound parameters for TapBot will be scripted in `tools/mix.py`. |
