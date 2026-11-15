# TapBot Launch Video — Master Technical Storyboard (Phase 2)

**Project:** [TapBot (VarshuDev/tapbot)](https://github.com/VarshuDev/tapbot)  
**Deliverable:** `/video-output/PHASE-2-STORYBOARD.md`  
**Target Specs:** 1080x1920 (9:16 Vertical) | 40.00 Seconds (960 Frames @ 24 FPS)  
**Engine:** Nafay3D / Deep Dive 3D Procedural Three.js Engine  
**Visual Style:** Deep Obsidian 3D (`#080A0E`, `#0E121A`, `#10B981`, `#DDE5E2`)  
**Audio:** Kokoro-82M (`am_michael`) + Procedural Analog Audio Bed (`mix.py`)  

---

## Storyboard Overview & Timeline Map

```
0s        5s        10s       17s              25s            30s          35s          40s
├─────────┼─────────┼─────────┼────────────────┼──────────────┼────────────┼────────────┤
│ SCENE 01│ SCENE 02│ SCENE 03│    SCENE 04    │   SCENE 05   │  SCENE 06  │  SCENE 07  │
│ PROBLEM │ REVEAL  │ RUNTIME │   COMMANDER    │   SECURITY   │ ECOSYSTEM  │  CLOSING   │
│ (5.0s)  │ (5.0s)  │ (7.0s)  │     (8.0s)     │    (5.0s)    │   (5.0s)   │   (5.0s)   │
└─────────┴─────────┴─────────┴────────────────┴──────────────┴────────────┴────────────┘
0f       120f      240f      408f             600f           720f         840f         960f
```

---

## Detailed Scene-by-Scene Specifications

### Scene 01 — The Problem: Cloud VPS vs. Idle Hardware
* **Timeline**: `0.00s – 5.00s` (Frames `0 – 120`, Duration: `5.00s`)
* **Narration**: *"Why rent a server… when you already have one?"*
  - Timing: Voiceover active from `0.60s` to `3.80s`. Natural acoustic pause from `3.80s` to `5.00s`.
* **Visual Narrative**:
  - The scene opens in a stark obsidian void. A floating, translucent red cloud server invoice card hovers on the left showing recurring charges (`$20.00 / MONTH`).
  - To the right, resting dormant on an obsidian pedestal, sits an idle smartphone, darkened and unutilized.
  - As the question is voiced, a glowing red slash cuts across the VPS invoice; the phone stirs, its brushed titanium edge catching a solitary rim light as subtle emerald data traces ignite around its base.
* **Three.js Objects & Geometries**:
  - `phoneGroup`: Stylized smartphone chassis (`BoxGeometry(0.72, 1.48, 0.08, 4, 4, 4)` with rounded chamfers), bezel mesh, and dark OLED screen plane.
  - `invoiceCard`: Thin procedural glass slab (`BoxGeometry(0.65, 0.90, 0.02)`) with semi-transparent material, displaying an embossed cloud icon and `$20/mo` red badge.
  - `pedestal`: Low obsidian cylinder (`CylinderGeometry(0.9, 0.9, 0.1, 32)`).
  - `particleGrid`: Ground-level subtle grid plane with faded wireframe lines (`#1F2937`).
* **Camera Setup & Movement**:
  - Initial Position: `(0.40, 0.35, 2.30)` looking at `(0.0, 0.10, 0.0)` with FOV `42°`.
  - Final Position: `(0.18, 0.15, 1.75)` looking at `(0.10, 0.05, 0.0)`.
  - Motion: Slow dolly push-in tracking toward the phone while the invoice drifts into the blurred background.
* **Lighting Setup**:
  - Key Rim Light: Cool white (`#DDE5E2`, intensity 1.4) at `(-2.0, 2.5, 1.5)`.
  - Invoice Warning Light: Dim red point light (`#EF4444`, intensity 0.8) behind the invoice card.
  - Ambient: Deep indigo ambient light (`#080A0E`, intensity 0.35).
* **Materials & Colors**:
  - Phone Chassis: `MeshStandardMaterial` (`color: #2A323D, metalness: 0.85, roughness: 0.28`).
  - Invoice Slab: `MeshPhysicalMaterial` (`color: #1A0E12, transmission: 0.65, roughness: 0.35, ior: 1.4`).
  - Red Strikethrough Line: `MeshBasicMaterial` (`color: #EF4444`).
* **HUD Components**:
  - Left HUD Pill: `[CLOUD VPS: $20/MO]` with red dot indicator.
  - Right HUD Pill: `[PHONE: $0/MO]` with pulsing emerald dot (`#10B981`).
* **Typography & Placement**:
  - Captions at `y = 1520`: `WHY RENT *A SERVER…` -> `WHEN YOU *ALREADY…` -> `HAVE *ONE?`.
  - Active word highlighted in Gold (`#FACC15`) with 1.08x scale pulse.
* **Sound Effects**:
  - `0.0s`: Deep 55 Hz sub-bass drone begins.
  - `2.2s`: Low-frequency resonant impact (sub-thud) as the invoice is struck out.
  - `3.8s`: Soft electrical hum as phone edges illuminate.
* **Transition to Scene 02**:
  - Camera glides forward directly along the phone’s optical axis; background invoice fades to zero opacity; phone pivots smoothly to center frame.
* **Required SVG Assets**: `assets/icons/cloud-edge.svg`, `assets/icons/ram-chip.svg`.
* **Engine Implementation Notes**:
  - Uses `core/engine.js` ACES tone mapping.
  - Avoid bright light wash: clamp red point light radius to 1.2 units so the obsidian background stays pitch black.

---

### Scene 02 — The Reveal: The Phone as Infrastructure
* **Timeline**: `5.00s – 10.00s` (Frames `120 – 240`, Duration: `5.00s`)
* **Narration**: *"Meet TapBot. Your phone. Your bot infrastructure."*
  - Timing: Voiceover active from `5.40s` to `8.90s`. Natural acoustic pause from `8.90s` to `10.00s`.
* **Visual Narrative**:
  - The smartphone occupies center stage in an authoritative vertical hero posture.
  - The OLED screen awakens from pure black: a cyan and emerald pulse ripples outward from the center, unveiling the official **TapBot antenna logo** followed by the native Deep Obsidian top bar and version badge (`v2.4.0`).
  - The phone rotates slowly by 12 degrees, revealing its slim hardware profile and titanium rails.
* **Three.js Objects & Geometries**:
  - `phoneHero`: Centered 3D smartphone model with dynamic canvas texture `#phoneScreenTexture` mapped to screen face.
  - `antennaLogoEmblem`: Extruded 3D vector logo based on `assets/icons/bot-antenna.svg` hovering subtly 0.05 units above the screen with an emerald emissive glow.
  - `orbitRing`: Faint orbital wireframe circle surrounding the phone along the Y-axis representing local coroutine concurrency.
* **Camera Setup & Movement**:
  - Initial Position: `(0.00, 0.00, 1.85)` looking at `(0.0, 0.0, 0.0)` with FOV `38°`.
  - Final Position: `(0.25, 0.05, 1.45)` looking at `(0.0, 0.0, 0.0)`.
  - Motion: Low-angle cinematic rise and slight rightward orbit (`lerp` with `TapBotMotion.StandardEasing`).
* **Lighting Setup**:
  - Dual Studio Rim Lights: Left rim (`#DDE5E2`, intensity 1.6) at `(-2.5, 1.5, 1.0)`, Right rim (`#10B981`, intensity 1.2) at `(2.5, 1.5, 1.0)`.
  - Screen Glow: RectAreaLight positioned 0.02 units off screen plane casting soft `#10B981` light onto the chassis bezel.
* **Materials & Colors**:
  - Screen Face: `MeshBasicMaterial` driven by procedural canvas drawing of the TapBot header HUD.
  - Antenna Emblem: `MeshPhysicalMaterial` (`color: #10B981, emissive: #10B981, emissiveIntensity: 0.6, roughness: 0.2`).
* **HUD Components**:
  - Top Badge: `[TAPBOT v2.4.0 • NATIVE ART RUNTIME]`.
  - Screen Top Bar: Brand text "TapBot" with verified antenna icon and active green node status.
* **Typography & Placement**:
  - Captions at `y = 1520`: `MEET *TAPBOT.` -> `YOUR *PHONE.` -> `YOUR *BOT INFRASTRUCTURE.`.
* **Sound Effects**:
  - `5.0s`: Tempo pulse introduces rhythmic beat (110 BPM, subtle analog bass kick).
  - `5.4s`: Clean digital wake chime (440 Hz soft harmonic).
  - `8.5s`: Crisp metallic latch sound as the phone aligns into hero lock.
* **Transition to Scene 03**:
  - Camera pushes in smoothly toward the phone’s lower screen, focusing on the Fleet section; UI smoothly transitions from brand hero to the multi-bot runtime grid.
* **Required SVG Assets**: `assets/icons/bot-antenna.svg`, `assets/banner.svg`.
* **Engine Implementation Notes**:
  - Screen texture rendered via 2D canvas at 1024x2048 resolution (`THREE.CanvasTexture`) to ensure pin-sharp typography in 1080x1920 output.

---

### Scene 03 — The Runtime: Multi-Bot Fleet on Android
* **Timeline**: `10.00s – 17.00s` (Frames `240 – 408`, Duration: `7.00s`)
* **Narration**: *"Run multiple Telegram bots, directly on Android. In-process coroutines. Zero cloud fees."*
  - Timing: Voiceover active from `10.50s` to `15.60s`. Pause from `15.60s` to `17.00s`.
* **Visual Narrative**:
  - Close-up macro on the phone's screen displaying the **My Fleet** HUD.
  - Three distinct bot cards materialize in sequence with verified green pulse badges:
    1. `Commander Bot` (`🟢 RUNNING`)
    2. `Exon Robot 2.0` (`🟢 RUNNING`)
    3. `TapStream Audio` (`🟢 RUNNING`)
  - A real-time hardware telemetry HUD highlights the minimal footprint: `RAM: 9.4 MB`, `BATTERY: 94%`, `OUTBOUND: PORT 443`.
  - Small orbital data packets glide smoothly between the cards, illustrating concurrent Kotlin coroutines in a single in-process ART service.
* **Three.js Objects & Geometries**:
  - `phoneMacro`: Phone tilted 15° forward for high-readability screen inspection.
  - `fleetScreenCanvas`: Dynamic canvas showing native Deep Obsidian bot cards with real SVG category icons.
  - `dataStreamParticles`: Instanced spheres (`THREE.InstancedMesh`, count 24, radius 0.008) flowing in a closed loop around the phone's perimeter.
* **Camera Setup & Movement**:
  - Initial Position: `(0.12, 0.10, 1.35)` looking at `(0.02, 0.05, 0.0)` with FOV `35°`.
  - Final Position: `(-0.08, -0.05, 1.15)` looking at `(0.0, -0.02, 0.0)`.
  - Motion: Slow downward tracking shot across the three running bot cards, ending on the hardware telemetry meters.
* **Lighting Setup**:
  - Primary Overhead Softbox: Neutral white (`#FFFFFF`, intensity 1.2) angled at 60° above the screen.
  - Green Specular Accents: Narrow spotlight (`#10B981`, intensity 1.5) highlighting the green running indicator dots.
* **Materials & Colors**:
  - Bot Card Backgrounds: Obsidian Charcoal (`#0E121A`) with 1px border (`#1F2937`).
  - Active Pills: Deep emerald fill (`#064E3B`) with emerald text (`#10B981`).
* **HUD Components**:
  - Floating Leader Lines: Callout pointing to RAM meter: `IN-PROCESS ART COROUTINES`.
  - Bottom Status Bar: `FOREGROUND SERVICE: ACTIVE • DATA SYNC | MEDIA PLAYBACK`.
* **Typography & Placement**:
  - Captions at `y = 1520`: `RUN MULTIPLE *TELEGRAM BOTS,` -> `DIRECTLY ON *ANDROID.` -> `IN-PROCESS *COROUTINES.` -> `ZERO *CLOUD FEES.`.
* **Sound Effects**:
  - `10.5s`: Triple tactile UI click sequence (spaced 0.4s apart) as the three bots activate.
  - `13.5s`: Soft electronic hum representing coroutine concurrency.
* **Transition to Scene 04**:
  - Camera glides up toward the top of the phone and rotates 90 degrees to showcase the camera module and top edge, setting up Commander hardware interactions.
* **Required SVG Assets**: `assets/icons/coroutine-orbit.svg`, `assets/icons/fleet-shield.svg`, `assets/icons/watchdog-scan.svg`.
* **Engine Implementation Notes**:
  - Telemetry numbers are displayed as exact verified values (`9.4 MB RAM`, not fabricated claims).
  - Maintain shallow depth of field: camera rails slightly soft to emphasize screen content.

---

### Scene 04 — The Commander: Hardware Remote Control
* **Timeline**: `17.00s – 25.00s` (Frames `408 – 600`, Duration: `8.00s`)
* **Narration**: *"Control your device remotely. From Telegram. Trigger hardware sensors, live telemetry, and direct APK delivery."*
  - Timing: Voiceover active from `17.50s` to `23.40s`. Pause from `23.40s` to `25.00s`.
* **Visual Narrative**:
  - The camera pulls back to reveal the back of the phone angled toward the viewer.
  - A sleek floating Telegram chat bubble appears in front of the device showing the incoming owner command: `/torch on`.
  - **Instant Hardware Reaction**: The phone's physical LED camera flash instantly triggers, casting a bright, volumetric cone of light across the obsidian studio.
  - Next, a second Telegram bubble appears: `/status`. The phone responds in chat with the live system HUD.
  - Finally, a `/apk` command triggers a floating document card showing `tapbot-v2.4.0.apk (12.4 MB • SHA-256 Verified)` transmitting directly into Telegram.
* **Three.js Objects & Geometries**:
  - `phoneRear`: Detailed rear phone case with camera bump, dual lenses, and LED flashlight emitter cylinder.
  - `torchVolumetricCone`: SpotLight with volumetric mesh cone (`CylinderGeometry(0.01, 1.2, 3.5, 32, 1, true)`) using additive blending (`THREE.AdditiveBlending`) and opacity 0.35.
  - `chatBubble1`: Telegram incoming user command card (`/torch on`).
  - `chatBubble2`: Telegram bot response card (`Flashlight enabled`).
  - `apkDocumentCard`: Stylized document file pill with APK package icon and verified SHA-256 badge.
* **Camera Setup & Movement**:
  - Initial Position: `(-0.45, 0.20, 1.80)` looking at camera bump `(-0.15, 0.45, 0.0)` with FOV `40°`.
  - Flash Trigger (`18.8s`): Dramatic micro-shake (amplitude 0.004 for 3 frames), then camera tracks around the cone of light to `(0.35, -0.10, 1.60)`.
* **Lighting Setup**:
  - Ambient Studio: Dim obsidian (`#080A0E`, intensity 0.2).
  - Torch Light: Intense directional SpotLight (`#FFF7ED`, intensity 4.5, angle 0.65, penumbra 0.8) pointing toward the bottom-left camera perspective.
* **Materials & Colors**:
  - Phone Back Chassis: Matte Obsidian Glass (`color: #0E121A, roughness: 0.45, metalness: 0.2`).
  - Camera Lens Ring: Brushed Titanium (`color: #384252, metalness: 0.9, roughness: 0.2`).
  - Volumetric Cone: ShaderMaterial with falloff from `#FFFFFF` at apex to `#10B981` at periphery.
* **HUD Components**:
  - Telegram Chat Pill: Floating card with Telegram paper plane icon (`#229ED9`) and monospace text: `OWNER: /torch on`.
  - Telemetry Badge: `[TELEMETRY: BATTERY 94% • 28.4°C • WIFI CONNECTED]`.
* **Typography & Placement**:
  - Captions at `y = 1520`: `CONTROL YOUR *DEVICE REMOTELY.` -> `FROM *TELEGRAM.` -> `TRIGGER *HARDWARE SENSORS,` -> `LIVE *TELEMETRY,` -> `AND DIRECT *APK DELIVERY.`.
* **Sound Effects**:
  - `18.2s`: Telegram incoming message pop (subtle marimba tone).
  - `18.8s`: Solid mechanical relay click + crisp photographic flash pulse.
  - `20.5s`: Tactile haptic buzz (low 150 Hz double vibration).
  - `22.8s`: Smooth document send whoosh.
* **Transition to Scene 05**:
  - As the flashlight dims, the camera rotates back to the front face while an x-ray cutaway shader smoothly sweeps across the phone body from top to bottom.
* **Required SVG Assets**: `assets/icons/remote-hardware.svg`, `assets/icons/apk-package.svg`, `assets/icons/telegram-fly.svg`.
* **Engine Implementation Notes**:
  - Ensure the camera stays outside the flashlight's volumetric cone mesh to prevent washed-out flat frames (following Nafay3D QA rule #1).

---

### Scene 05 — The Security: Hardware Keystore & Encryption
* **Timeline**: `25.00s – 30.00s` (Frames `600 – 720`, Duration: `5.00s`)
* **Narration**: *"Your credentials. Encrypted on your device. Hardware-backed Keystore security."*
  - Timing: Voiceover active from `25.40s` to `28.80s`. Pause from `28.80s` to `30.00s`.
* **Visual Narrative**:
  - An architectural cutaway slices through the front glass, exposing the internal silicon substrate of the phone.
  - In the center of the motherboard, the **Hardware Keystore Vault** is revealed as a glowing isometric emerald cube (`AES-256-GCM`).
  - Telegram tokens (`1234****:[REDACTED]`) flow toward the cube and instantly crystallize into encrypted hash blocks.
  - A secure lock emblem clamps shut over the vault, and a transparent shield overlay (`FLAG_SECURE`) reinforces that zero secrets ever leave the device.
* **Three.js Objects & Geometries**:
  - `motherboardPlate`: Dark textured silicon circuit board with metallic copper traces.
  - `keystoreCube`: Beveled glass cube (`BoxGeometry(0.24, 0.24, 0.16)`) with internal glowing cryptographic core.
  - `shieldOverlay`: Semi-transparent security shield contour (`ExtrudeGeometry` based on `assets/icons/keystore-vault.svg`).
  - `tokenStream`: Faint alphanumeric text glyphs flying into the cube and collapsing into redacted asterisks.
* **Camera Setup & Movement**:
  - Initial Position: `(0.00, 0.15, 1.30)` looking at Keystore vault `(0.0, 0.05, 0.0)` with FOV `36°`.
  - Final Position: `(0.10, 0.08, 1.05)` looking at `(0.0, 0.05, 0.0)`.
  - Motion: Slow orthographic push-in directly into the cryptographic core.
* **Lighting Setup**:
  - Internal Vault Core Light: PointLight (`#10B981`, intensity 2.2, distance 1.5) pulsing inside the cube.
  - Top Rake Light: Cool white directional light raking across circuit board traces at a 75° grazing angle.
* **Materials & Colors**:
  - Silicon Board: `MeshStandardMaterial` (`color: #0E121A, roughness: 0.8`).
  - Circuit Traces: `MeshStandardMaterial` (`color: #1F2937, metalness: 0.95, roughness: 0.15`).
  - Vault Core: `MeshPhysicalMaterial` (`color: #10B981, transmission: 0.75, roughness: 0.1, emissive: #10B981, emissiveIntensity: 0.9`).
* **HUD Components**:
  - Top Security Pill: `[HARDWARE KEYSTORE: STRONGBOX / TEE]`.
  - Bottom Encryption Pill: `[AES-256-GCM • ZERO TOKEN TRANSMISSION]`.
  - Redaction Badge: `[SECRET REDACTOR: ACTIVE]`.
* **Typography & Placement**:
  - Captions at `y = 1520`: `YOUR *CREDENTIALS.` -> `ENCRYPTED ON *YOUR DEVICE.` -> `HARDWARE-BACKED *KEYSTORE SECURITY.`.
* **Sound Effects**:
  - `25.4s`: Deep magnetic servo hum as the cutaway exposes the internal vault.
  - `27.2s`: Heavy mechanical lock engagement (vault latch click).
  - `28.8s`: Resonant sub-bass pulse affirming cryptographic security.
* **Transition to Scene 06**:
  - The cutaway closes smoothly; the phone pulls back into three-quarter perspective as floating modular package cards assemble around it.
* **Required SVG Assets**: `assets/icons/keystore-vault.svg`, `assets/icons/android-shield.svg`.
* **Engine Implementation Notes**:
  - Silicon cutaway uses a smooth clipping plane (`renderer.clippingPlanes = [clipPlane]`) animated along the Y-axis.

---

### Scene 06 — The Ecosystem: Bot Store & .botpkg Packages
* **Timeline**: `30.00s – 35.00s` (Frames `720 – 840`, Duration: `5.00s`)
* **Narration**: *"Discover bots. Install. Deploy. Community packages at the edge."*
  - Timing: Voiceover active from `30.40s` to `33.70s`. Pause from `33.70s` to `35.00s`.
* **Visual Narrative**:
  - The camera pulls back to a dynamic three-quarter view of the smartphone.
  - Floating around the phone in an elegant carousel arc are four curated `.botpkg` cards:
    1. **Commander Bot** (Flagship hardware node)
    2. **Exon Robot 2.0** (Supergroup moderation)
    3. **TapStream Audio** (Music & radio streamer)
    4. **Gemini AI** (Conversational assistant)
  - One card glides forward, an animated `[INSTALL]` progress bar fills in 0.4 seconds, and a green checkmark confirms atomic deployment.
  - A subtle Cloudflare edge network arc in the background indicates global serverless catalog resolution.
* **Three.js Objects & Geometries**:
  - `phoneCenter`: Phone resting slightly tilted at `(-0.25, 0.0, 0.0)`.
  - `packageCards`: Array of 4 rounded rectangular cards (`BoxGeometry(0.48, 0.64, 0.015)` with corner radiuses), arranged along an orbital arc `x = cos(θ), z = sin(θ)`.
  - `installProgressBar`: Animated emerald mesh bar scaling along X from 0.0 to 1.0.
  - `edgeArc`: Subtle glowing curved line connecting Cloudflare edge to the device.
* **Camera Setup & Movement**:
  - Initial Position: `(0.50, 0.25, 2.10)` looking at `(0.0, 0.0, 0.0)` with FOV `42°`.
  - Final Position: `(0.15, 0.10, 1.70)` looking at the active installing card `(0.10, 0.0, 0.0)`.
  - Motion: Dynamic orbital pan following the installing card into the phone.
* **Lighting Setup**:
  - Primary Stage Light: Neutral white (`#DDE5E2`, intensity 1.5) overhead.
  - Edge Arc Accent: Cyan-emerald point light (`#10B981`, intensity 1.0) under the active card.
* **Materials & Colors**:
  - Package Cards: Obsidian Surface (`#0E121A`) with glassmorphic border (`#1F2937`) and embossed vector category icons.
  - Progress Fill: Emissive Emerald (`#10B981`).
* **HUD Components**:
  - Store Badge: `[EDGE BOT STORE • CLOUDFLARE WORKERS & D1]`.
  - Checksum Pill: `[SHA-256 VERIFIED • ATOMIC ROLLBACK READY]`.
* **Typography & Placement**:
  - Captions at `y = 1520`: `DISCOVER *BOTS.` -> `INSTALL. *DEPLOY.` -> `COMMUNITY *PACKAGES AT THE EDGE.`.
* **Sound Effects**:
  - `30.4s`: Crisp card fan whoosh (cards fanning into position).
  - `31.8s`: Quick ascending data-install chime (3 harmonic notes).
  - `32.6s`: Confirmed installation click (tactile latch).
* **Transition to Scene 07**:
  - Package cards fold back into the phone’s display; phone re-aligns to pure vertical center as lighting intensifies for the final climax.
* **Required SVG Assets**: `assets/icons/cloud-edge.svg`, `assets/icons/rollback-loop.svg`, `assets/icons/apk-package.svg`.
* **Engine Implementation Notes**:
  - Package cards use instanced or cloned meshes with unique canvas textures generated dynamically.

---

### Scene 07 — The Closing: Manifesto & Call to Action
* **Timeline**: `35.00s – 40.00s` (Frames `840 – 960`, Duration: `5.00s`)
* **Narration**: *"Stop renting servers. Start running bots. TapBot."*
  - Timing: Voiceover active from `35.40s` to `38.60s`. Logo and link hold from `38.60s` to `40.00s`.
* **Visual Narrative**:
  - The smartphone stands in majestic center frame, framed by sharp titanium rim highlights.
  - The screen displays the full TapBot launch console with all systems operational (`FLEET: ACTIVE • 0 CLOUD FEES`).
  - The 3D TapBot antenna emblem rises above the device with a rich, soft emerald optical bloom.
  - The bold closing manifesto typography locks onto screen:
    - **STOP RENTING SERVERS.**
    - **START RUNNING BOTS.**
  - At `38.5s`, the final CTA locks into view with a clean GitHub icon and link badge:
    - **`github.com/VarshuDev/tapbot`**
    - `TapBot v2.4.0 • Android 10+ • Apache 2.0`
* **Three.js Objects & Geometries**:
  - `phoneClimax`: Phone hero at `(0.0, -0.15, 0.0)` in pure vertical orientation.
  - `tapbotCrownEmblem`: Sculpted 3D antenna logo floating at `(0.0, 0.65, 0.1)`.
  - `ctaBadge`: Rounded 2D/3D pill container displaying the GitHub repository destination.
  - `backgroundAura`: Deep radial emerald ambient glow behind the silhouette.
* **Camera Setup & Movement**:
  - Initial Position: `(0.00, 0.00, 1.65)` looking at `(0.0, 0.10, 0.0)` with FOV `38°`.
  - Final Position: `(0.00, 0.08, 1.55)` looking at `(0.0, 0.15, 0.0)`.
  - Motion: Majestic slow pull-back and subtle rise, giving the final logo lockup breathing room.
* **Lighting Setup**:
  - Symmetrical Dual Rim Lighting: Balanced left/right studio rims (`#DDE5E2` and `#10B981`, intensity 1.8 each).
  - Center Climax Spotlight: Narrow top beam illuminating the antenna logo emblem.
* **Materials & Colors**:
  - Phone Chassis: Polished Titanium (`color: #3B4654, metalness: 0.90, roughness: 0.18`).
  - Antenna Emblem: Brilliant Emissive Emerald (`#10B981`, emissiveIntensity 1.2).
  - Text & Accents: Crisp Titanium White (`#DDE5E2`).
* **HUD Components**:
  - Final Brand Badge: TapBot Antenna Logo + "TAPBOT" in bold uppercase tracking.
  - GitHub Link Pill: `[GitHub: VarshuDev/tapbot]`.
  - Architecture Tag: `ANDROID ART • HARDWARE KEYSTORE • CLOUDFLARE EDGE`.
* **Typography & Placement**:
  - Captions at `y = 1520`: `STOP *RENTING SERVERS.` -> `START *RUNNING BOTS.` -> `*TAPBOT.`.
  - Final lockup at `38.6s – 40.0s`:
    - Top: `github.com/VarshuDev/tapbot`
    - Subtitle: `Open Source • Apache 2.0`
* **Sound Effects**:
  - `35.4s`: Deep, resonant sub-bass drop (40 Hz sweep).
  - `37.6s`: Heavy metallic branding chime with long reverberant decay.
  - `38.6s`: Final tactile click as the GitHub CTA badge locks into place.
  - `40.0s`: Smooth fade to silence.
* **Transition to End**:
  - Clean hold on the final frame (no abrupt black cut).
* **Required SVG Assets**: `assets/banner.svg`, `assets/icons/bot-antenna.svg`, `assets/icons/crown-author.svg`.
* **Engine Implementation Notes**:
  - Bloom pass threshold set to 0.70 to allow the antenna emblem to bloom gently into the upper third of the vertical frame.

---

## Storyboard Verification Checklist

- [x] All 7 approved scenes covered in full technical detail.
- [x] Exact timeline accounts for 0.0s to 40.0s without timing gaps.
- [x] Camera vectors, targets, FOV, and easing curves specified for every scene.
- [x] Procedural Three.js geometries and shaders documented without photorealistic hallucinations.
- [x] Kinetic captions and voiceover timing synchronized.
- [x] Sound effects and musical bed cues integrated.
- [x] All required SVG assets mapped directly to files in `tapbot/assets/`.
