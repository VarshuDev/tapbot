/**
 * TapBot & TapStream - Industrial Cyberdeck Audio Engine & GSAP Scrollytelling
 */

// =============================================================================
// 1. WEB AUDIO SYNTH & SOUND EFFECTS (ZERO EXTERNAL ASSETS)
// =============================================================================
class HardwareAudioSynth {
  constructor() {
    this.ctx = null;
    this.soundEnabled = true;
  }

  init() {
    if (!this.ctx) {
      const AudioContext = window.AudioContext || window.webkitAudioContext;
      if (AudioContext) {
        this.ctx = new AudioContext();
      }
    }
  }

  // Mechanical switch click
  playSwitchClick() {
    if (!this.soundEnabled) return;
    this.init();
    if (!this.ctx) return;
    if (this.ctx.state === 'suspended') this.ctx.resume();

    const osc = this.ctx.createOscillator();
    const gain = this.ctx.createGain();
    osc.type = 'triangle';
    osc.frequency.setValueAtTime(120, this.ctx.currentTime);
    osc.frequency.exponentialRampToValueAtTime(30, this.ctx.currentTime + 0.03);

    gain.gain.setValueAtTime(0.08, this.ctx.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.001, this.ctx.currentTime + 0.03);

    osc.connect(gain);
    gain.connect(this.ctx.destination);
    osc.start();
    osc.stop(this.ctx.currentTime + 0.03);
  }

  // Analog rotary tick
  playRotaryTick(freq = 600) {
    if (!this.soundEnabled) return;
    this.init();
    if (!this.ctx) return;
    if (this.ctx.state === 'suspended') this.ctx.resume();

    const osc = this.ctx.createOscillator();
    const gain = this.ctx.createGain();
    osc.type = 'sine';
    osc.frequency.setValueAtTime(freq, this.ctx.currentTime);
    gain.gain.setValueAtTime(0.03, this.ctx.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.0001, this.ctx.currentTime + 0.02);

    osc.connect(gain);
    gain.connect(this.ctx.destination);
    osc.start();
    osc.stop(this.ctx.currentTime + 0.02);
  }

  // Cassette playback start tone
  playTapeHeadEngage() {
    if (!this.soundEnabled) return;
    this.init();
    if (!this.ctx) return;
    if (this.ctx.state === 'suspended') this.ctx.resume();

    // Low mechanical clunk + tape hiss burst
    this.playSwitchClick();
    setTimeout(() => {
      this.playRotaryTick(880);
    }, 60);
  }
}

const hwAudio = new HardwareAudioSynth();

// =============================================================================
// 2. ANALOG VU METERS WITH PHYSICAL NEEDLE INERTIA
// =============================================================================
class AnalogVUMeter {
  constructor(needleLeftId, needleRightId, peakLedId) {
    this.needleLeft = document.getElementById(needleLeftId);
    this.needleRight = document.getElementById(needleRightId);
    this.peakLed = document.getElementById(peakLedId);

    this.valL = -45;
    this.valR = -45;
    this.targetL = -45;
    this.targetR = -45;
    this.isPlaying = true;
    this.volumePct = 0.8;

    this.updateLoop();
  }

  setPlaying(playing) {
    this.isPlaying = playing;
  }

  setVolume(pct) {
    this.volumePct = pct / 100;
  }

  updateLoop() {
    requestAnimationFrame(() => this.updateLoop());

    if (this.isPlaying) {
      // Simulate real musical dynamics: base rhythm + transient peaks
      const now = performance.now() * 0.003;
      const beat = Math.sin(now * 3.5) * 0.5 + 0.5;
      const jitter = (Math.random() - 0.5) * 8;
      
      const targetDeg = -45 + (beat * 65 * this.volumePct) + jitter;
      this.targetL = Math.max(-45, Math.min(25, targetDeg));
      this.targetR = Math.max(-45, Math.min(25, targetDeg + (Math.random() - 0.5) * 6));

      // Overload peak LED trigger
      if (this.peakLed) {
        if (this.targetL > 10 || this.targetR > 10) {
          this.peakLed.classList.add('bg-red-500', 'shadow-[0_0_10px_#ff2a4b]');
        } else {
          this.peakLed.classList.remove('bg-red-500', 'shadow-[0_0_10px_#ff2a4b]');
        }
      }
    } else {
      this.targetL = -45;
      this.targetR = -45;
      if (this.peakLed) {
        this.peakLed.classList.remove('bg-red-500', 'shadow-[0_0_10px_#ff2a4b]');
      }
    }

    // Spring interpolation with inertia
    this.valL += (this.targetL - this.valL) * 0.22;
    this.valR += (this.targetR - this.valR) * 0.22;

    if (this.needleLeft) this.needleLeft.style.transform = `rotate(${this.valL}deg)`;
    if (this.needleRight) this.needleRight.style.transform = `rotate(${this.valR}deg)`;
  }
}

// =============================================================================
// 3. PHOSPHOR OSCILLOSCOPE WAVEFORM CANVAS
// =============================================================================
class PhosphorOscilloscope {
  constructor(canvasId) {
    this.canvas = document.getElementById(canvasId);
    if (!this.canvas) return;
    this.ctx = this.canvas.getContext('2d');
    this.phase = 0;
    this.isPlaying = true;
    this.volume = 0.8;

    this.resize();
    window.addEventListener('resize', () => this.resize());
    this.render();
  }

  resize() {
    if (!this.canvas) return;
    this.canvas.width = this.canvas.parentElement.clientWidth * window.devicePixelRatio;
    this.canvas.height = this.canvas.clientHeight * window.devicePixelRatio;
    this.ctx.scale(window.devicePixelRatio, window.devicePixelRatio);
  }

  render() {
    requestAnimationFrame(() => this.render());
    if (!this.canvas || !this.ctx) return;

    const width = this.canvas.parentElement.clientWidth;
    const height = this.canvas.clientHeight;
    const cy = height / 2;

    // Dark decay fade for CRT phosphor glow effect
    this.ctx.fillStyle = 'rgba(6, 9, 14, 0.25)';
    this.ctx.fillRect(0, 0, width, height);

    this.phase += this.isPlaying ? 0.08 : 0.01;

    // Draw main phosphor oscilloscope line
    this.ctx.lineWidth = 2.5;
    this.ctx.strokeStyle = '#00ff88';
    this.ctx.shadowBlur = 12;
    this.ctx.shadowColor = '#00ff88';
    this.ctx.beginPath();

    const points = 120;
    const amp = this.isPlaying ? (height * 0.38 * this.volume) : 2;

    for (let i = 0; i <= points; i++) {
      const x = (i / points) * width;
      const rad = (i / points) * Math.PI * 6 + this.phase;
      const harmonic = Math.sin(rad * 2.3) * 0.35;
      const y = cy + (Math.sin(rad) + harmonic) * amp;

      if (i === 0) {
        this.ctx.moveTo(x, y);
      } else {
        this.ctx.lineTo(x, y);
      }
    }
    this.ctx.stroke();

    // Subtle cyan overlay secondary wave
    this.ctx.lineWidth = 1.2;
    this.ctx.strokeStyle = '#00e5ff';
    this.ctx.shadowBlur = 8;
    this.ctx.shadowColor = '#00e5ff';
    this.ctx.beginPath();

    for (let i = 0; i <= points; i++) {
      const x = (i / points) * width;
      const rad = (i / points) * Math.PI * 4 + (this.phase * 1.4);
      const y = cy + Math.cos(rad) * (amp * 0.65);
      if (i === 0) this.ctx.moveTo(x, y);
      else this.ctx.lineTo(x, y);
    }
    this.ctx.stroke();
  }
}

// =============================================================================
// 4. HARDWARE ROTARY VOLUME KNOB CONTROLLER
// =============================================================================
class RotaryVolumeKnob {
  constructor(knobId, textId, onVolumeChange) {
    this.knob = document.getElementById(knobId);
    this.text = document.getElementById(textId);
    this.onVolumeChange = onVolumeChange;
    this.currentVol = 80; // 0 - 100
    this.isDragging = false;
    this.startY = 0;
    this.startVol = 80;

    this.init();
  }

  init() {
    if (!this.knob) return;
    this.updateAngle(this.currentVol);

    this.knob.addEventListener('mousedown', (e) => {
      this.isDragging = true;
      this.startY = e.clientY;
      this.startVol = this.currentVol;
      hwAudio.playRotaryTick();
      document.body.style.cursor = 'grabbing';
    });

    window.addEventListener('mousemove', (e) => {
      if (!this.isDragging) return;
      const dy = this.startY - e.clientY;
      const newVol = Math.max(0, Math.min(100, Math.round(this.startVol + dy * 0.8)));
      if (newVol !== this.currentVol) {
        this.setVolume(newVol);
        hwAudio.playRotaryTick(300 + newVol * 6);
      }
    });

    window.addEventListener('mouseup', () => {
      if (this.isDragging) {
        this.isDragging = false;
        document.body.style.cursor = '';
      }
    });

    // Mouse wheel support on knob
    this.knob.addEventListener('wheel', (e) => {
      e.preventDefault();
      const step = e.deltaY < 0 ? 5 : -5;
      this.setVolume(Math.max(0, Math.min(100, this.currentVol + step)));
      hwAudio.playRotaryTick(300 + this.currentVol * 6);
    }, { passive: false });
  }

  setVolume(vol) {
    this.currentVol = vol;
    this.updateAngle(vol);
    if (this.text) this.text.innerText = `${vol}%`;
    if (this.onVolumeChange) this.onVolumeChange(vol);
  }

  updateAngle(vol) {
    // Map 0-100% to -135deg to +135deg (270 degree rotation)
    const deg = -135 + (vol / 100) * 270;
    if (this.knob) this.knob.style.transform = `rotate(${deg}deg)`;
  }
}

// =============================================================================
// 5. MASTER AUDIO CONTROL DECK & TELEGRAM SIMULATOR
// =============================================================================
class MasterConsoleEngine {
  constructor() {
    this.reels = document.querySelectorAll('.cassette-reel');
    this.chatBody = document.getElementById('chat-stream');
    this.inputBox = document.getElementById('chat-input');
    this.tapeTitle = document.getElementById('tape-label-title');
    this.tapeArtist = document.getElementById('tape-label-artist');
    this.routeSpeakerLed = document.getElementById('led-route-spk');
    this.routeChatLed = document.getElementById('led-route-chat');
    this.bitrateIndicator = document.getElementById('deck-bitrate');

    this.isPlaying = true;
    this.currentRouting = 'BOTH'; // SPEAKER, CHAT, BOTH
    this.currentTrack = {
      title: 'Lofi Girl Study Beats',
      artist: 'Lofi Girl 24/7',
      station: 'lofi',
      bitrate: '320 KBPS'
    };

    this.initEvents();
  }

  initEvents() {
    if (this.inputBox) {
      this.inputBox.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
          e.preventDefault();
          this.handleCommand(this.inputBox.value);
          this.inputBox.value = '';
        }
      });
    }

    // Set initial cassette state
    this.updateDeckState();
  }

  updateDeckState() {
    this.reels.forEach(r => {
      if (this.isPlaying) r.classList.add('is-spinning');
      else r.classList.remove('is-spinning');
    });

    if (this.tapeTitle) this.tapeTitle.innerText = this.currentTrack.title.toUpperCase();
    if (this.tapeArtist) this.tapeArtist.innerText = this.currentTrack.artist.toUpperCase();
    if (this.bitrateIndicator) this.bitrateIndicator.innerText = this.currentTrack.bitrate;

    // Routing LEDs
    if (this.routeSpeakerLed) {
      if (this.currentRouting === 'SPEAKER' || this.currentRouting === 'BOTH') {
        this.routeSpeakerLed.className = 'led-dot green-on';
      } else {
        this.routeSpeakerLed.className = 'led-dot';
      }
    }

    if (this.routeChatLed) {
      if (this.currentRouting === 'CHAT' || this.currentRouting === 'BOTH') {
        this.routeChatLed.className = 'led-dot cyan-on';
      } else {
        this.routeChatLed.className = 'led-dot';
      }
    }
  }

  setRouting(mode) {
    hwAudio.playSwitchClick();
    this.currentRouting = mode;
    this.updateDeckState();

    document.querySelectorAll('[data-route-mode]').forEach(btn => {
      if (btn.getAttribute('data-route-mode') === mode) {
        btn.classList.add('is-active');
      } else {
        btn.classList.remove('is-active');
      }
    });
  }

  tuneStation(genre) {
    hwAudio.playTapeHeadEngage();
    const stations = {
      lofi: { title: 'Lofi Girl Study Beats', artist: 'Lofi Girl 24/7', bitrate: '320 KBPS' },
      synthwave: { title: 'Nightwave Plaza 80s Cyber', artist: 'Plaza FM', bitrate: '320 KBPS' },
      rock: { title: 'Classic Rock Live FM', artist: 'Planet Rock', bitrate: '256 KBPS' },
      chillhop: { title: 'Chillhop Cafe Beats', artist: 'Chillhop Music', bitrate: '320 KBPS' },
      jazz: { title: 'Jazz Lounge & Bossa', artist: 'Jazz Radio FM', bitrate: '320 KBPS' },
      space: { title: 'Deep Space Drone Ambient', artist: 'SomaFM Drone', bitrate: '192 KBPS' }
    };

    const s = stations[genre] || stations.lofi;
    this.currentTrack = { ...s, station: genre };
    this.isPlaying = true;
    this.updateDeckState();

    if (window.vuMeters) window.vuMeters.setPlaying(true);
    if (window.oscilloscope) window.oscilloscope.isPlaying = true;

    // Update preset button active states
    document.querySelectorAll('[data-tune-btn]').forEach(btn => {
      if (btn.getAttribute('data-tune-btn') === genre) {
        btn.classList.add('is-active');
      } else {
        btn.classList.remove('is-active');
      }
    });

    this.appendChatMsg(`/radio ${genre}`, 'user');
    setTimeout(() => {
      this.appendChatMsg(
        `📻 <b>Tuned 24/7 Internet Radio:</b>\n` +
        `▶️ <b>${s.title}</b> (${genre.toUpperCase()})\n` +
        `🎙 <i>${s.artist}</i>\n` +
        `🔊 Output: <b>${this.currentRouting}</b> • Bitrate: <b>${s.bitrate}</b>`,
        'bot'
      );
    }, 200);
  }

  handleCommand(text) {
    const raw = text.trim();
    if (!raw) return;
    hwAudio.playSwitchClick();

    this.appendChatMsg(raw, 'user');
    const parts = raw.split(/\s+/);
    const cmd = parts[0].toLowerCase();
    const arg = parts.slice(1).join(' ').trim();

    setTimeout(() => {
      let reply = '';
      if (cmd === '/play') {
        if (!arg) {
          reply = `⚠️ Usage: <code>/play &lt;song title, artist, or YouTube URL&gt;</code>`;
        } else {
          this.currentTrack = {
            title: arg.charAt(0).toUpperCase() + arg.slice(1),
            artist: 'YouTube Audio Stream',
            station: 'custom',
            bitrate: '320 KBPS'
          };
          this.isPlaying = true;
          this.updateDeckState();
          if (window.vuMeters) window.vuMeters.setPlaying(true);
          if (window.oscilloscope) window.oscilloscope.isPlaying = true;

          reply = `🎧 <b>Queued & Resolving Stream:</b>\n` +
            `▶️ <b>${this.currentTrack.title}</b>\n` +
            `⚡ Source: <b>yt-dlp Engine</b>\n` +
            `🔊 Output Mode: <b>${this.currentRouting}</b>`;
        }
      } else if (cmd === '/nowplaying' || cmd === '/np') {
        reply = `🎶 <b>Now Playing:</b>\n` +
          `<b>${this.currentTrack.title}</b>\n` +
          `<i>${this.currentTrack.artist}</i>\n` +
          `<code>▶ 01:23 ━━━━●──────── 03:45</code>\n` +
          `🔊 Mode: <b>${this.currentRouting}</b> • Bitrate: <b>${this.currentTrack.bitrate}</b>`;
      } else if (cmd === '/pause') {
        this.isPlaying = false;
        this.updateDeckState();
        if (window.vuMeters) window.vuMeters.setPlaying(false);
        if (window.oscilloscope) window.oscilloscope.isPlaying = false;
        reply = `⏸ <b>Playback paused.</b> Send <code>/resume</code> to restart tape.`;
      } else if (cmd === '/resume') {
        this.isPlaying = true;
        this.updateDeckState();
        if (window.vuMeters) window.vuMeters.setPlaying(true);
        if (window.oscilloscope) window.oscilloscope.isPlaying = true;
        reply = `▶️ <b>Resumed playback:</b> ${this.currentTrack.title}`;
      } else if (cmd === '/mode') {
        const mode = arg.toUpperCase();
        if (mode === 'SPEAKER' || mode === 'CHAT' || mode === 'BOTH') {
          this.setRouting(mode);
          reply = `🎛 Active Routing Mode set to: <b>${mode}</b>`;
        } else {
          reply = `Usage: <code>/mode speaker</code> | <code>/mode chat</code> | <code>/mode both</code>`;
        }
      } else if (cmd === '/status') {
        reply = `📊 <b>Android ART System Telemetry:</b>\n` +
          `🔋 Battery: <b>88% (AC Charger Plugged)</b>\n` +
          `🧠 Heap Memory: <b>14.2 MB / 8 GB (0.17%)</b>\n` +
          `⚡ CPU Load: <b>0.4%</b>\n` +
          `⏱ Uptime: <b>42 Days 18 Hours</b>\n` +
          `🛡 Flash Wear: <b>0 Bytes / hr (Ring Buffer)</b>`;
      } else {
        reply = `🎵 Received: <code>${raw}</code>\nTry: <code>/radio synthwave</code> or <code>/nowplaying</code>`;
      }

      this.appendChatMsg(reply, 'bot');
    }, 220);
  }

  appendChatMsg(html, sender = 'bot') {
    if (!this.chatBody) return;
    const msg = document.createElement('div');
    msg.className = sender === 'user' 
      ? 'p-2.5 rounded-lg bg-cyan-950/80 border border-cyan-800/60 text-cyan-200 text-xs font-mono ml-auto max-w-[85%]'
      : 'p-2.5 rounded-lg bg-[#141824] border border-[#262e42] text-slate-200 text-xs font-mono max-w-[90%]';
    msg.innerHTML = html.replace(/\n/g, '<br/>');
    this.chatBody.appendChild(msg);
    this.chatBody.scrollTop = this.chatBody.scrollHeight;
  }
}

// =============================================================================
// 6. GSAP PINNED SCROLLYTELLING PIPELINE
// =============================================================================
function initGsapScrollytelling() {
  if (typeof gsap === 'undefined' || typeof ScrollTrigger === 'undefined') {
    console.warn('GSAP or ScrollTrigger not loaded; using native scroll fallback.');
    return;
  }

  gsap.registerPlugin(ScrollTrigger);

  // Pin the Pipeline Stage Section
  const pipelineTl = gsap.timeline({
    scrollTrigger: {
      trigger: '#pipeline-stage',
      start: 'top top',
      end: '+=180%',
      pin: true,
      scrub: 0.8,
      anticipatePin: 1
    }
  });

  // Step 1: Telegram Ingress (0% -> 25%)
  pipelineTl.to('#pipe-node-1', { 
    borderColor: '#00e5ff', 
    backgroundColor: '#101d2c', 
    boxShadow: '0 0 25px rgba(0, 229, 255, 0.4)', 
    scale: 1.03, 
    duration: 1,
    onStart: () => {
      const el = document.getElementById('pipeline-active-stage');
      if (el) el.innerText = '1 / 4 • INGRESS PACKET';
    }
  });

  // Step 2: In-Memory Ring Buffer (25% -> 50%)
  pipelineTl.to('#pipe-node-1', { scale: 1, boxShadow: 'none', borderColor: '#273044', backgroundColor: '#0f131c', duration: 1 })
    .to('#pipe-node-2', { 
      borderColor: '#00ff88', 
      backgroundColor: '#0d2218', 
      boxShadow: '0 0 25px rgba(0, 255, 136, 0.4)', 
      scale: 1.03, 
      duration: 1,
      onStart: () => {
        const el = document.getElementById('pipeline-active-stage');
        if (el) el.innerText = '2 / 4 • IN-MEMORY RING BUFFER (0 DISK WRITES)';
      }
    }, '<');

  // Step 3: Audio Stream Resolver (50% -> 75%)
  pipelineTl.to('#pipe-node-2', { scale: 1, boxShadow: 'none', borderColor: '#273044', backgroundColor: '#0f131c', duration: 1 })
    .to('#pipe-node-3', { 
      borderColor: '#ff8400', 
      backgroundColor: '#24170d', 
      boxShadow: '0 0 25px rgba(255, 132, 0, 0.4)', 
      scale: 1.03, 
      duration: 1,
      onStart: () => {
        const el = document.getElementById('pipeline-active-stage');
        if (el) el.innerText = '3 / 4 • AUDIO STREAM RESOLVER';
      }
    }, '<');

  // Step 4: Dual Egress Delivery (75% -> 100%)
  pipelineTl.to('#pipe-node-3', { scale: 1, boxShadow: 'none', borderColor: '#273044', backgroundColor: '#0f131c', duration: 1 })
    .to('#pipe-node-4', { 
      borderColor: '#00e5ff', 
      backgroundColor: '#101d2c', 
      boxShadow: '0 0 25px rgba(0, 229, 255, 0.4)', 
      scale: 1.03, 
      duration: 1,
      onStart: () => {
        const el = document.getElementById('pipeline-active-stage');
        if (el) el.innerText = '4 / 4 • DUAL STREAM EGRESS (SPEAKER + CHAT)';
      }
    }, '<');
}

// =============================================================================
// 7. INITIALIZE ON DOM READY
// =============================================================================
document.addEventListener('DOMContentLoaded', () => {
  // Initialize VU meters
  window.vuMeters = new AnalogVUMeter('vu-needle-left', 'vu-needle-right', 'vu-peak-led');

  // Initialize phosphor oscilloscope
  window.oscilloscope = new PhosphorOscilloscope('oscilloscope-canvas');

  // Initialize master console
  window.consoleDeck = new MasterConsoleEngine();

  // Initialize rotary knob with live VU and oscilloscope coupling
  window.rotaryKnob = new RotaryVolumeKnob('vol-rotary-knob', 'vol-rotary-val', (vol) => {
    if (window.vuMeters) window.vuMeters.setVolume(vol);
    if (window.oscilloscope) window.oscilloscope.volume = vol / 100;
  });

  // Radio preset buttons
  document.querySelectorAll('[data-tune-btn]').forEach(btn => {
    btn.addEventListener('click', (e) => {
      const genre = e.currentTarget.getAttribute('data-tune-btn');
      if (window.consoleDeck && genre) {
        window.consoleDeck.tuneStation(genre);
      }
    });
  });

  // Routing buttons
  document.querySelectorAll('[data-route-mode]').forEach(btn => {
    btn.addEventListener('click', (e) => {
      const mode = e.currentTarget.getAttribute('data-route-mode');
      if (window.consoleDeck && mode) {
        window.consoleDeck.setRouting(mode);
      }
    });
  });

  // Transport buttons (Play/Pause)
  const playBtn = document.getElementById('transport-play');
  const pauseBtn = document.getElementById('transport-pause');

  if (playBtn) {
    playBtn.addEventListener('click', () => {
      if (window.consoleDeck) window.consoleDeck.handleCommand('/resume');
      playBtn.classList.add('is-active');
      if (pauseBtn) pauseBtn.classList.remove('is-active');
    });
  }

  if (pauseBtn) {
    pauseBtn.addEventListener('click', () => {
      if (window.consoleDeck) window.consoleDeck.handleCommand('/pause');
      pauseBtn.classList.add('is-active');
      if (playBtn) playBtn.classList.remove('is-active');
    });
  }

  // Audio mute/unmute toggle
  const soundMuteBtn = document.getElementById('toggle-sound-btn');
  if (soundMuteBtn) {
    soundMuteBtn.addEventListener('click', () => {
      hwAudio.soundEnabled = !hwAudio.soundEnabled;
      soundMuteBtn.innerHTML = hwAudio.soundEnabled
        ? `<span class="led-dot green-on mr-1.5"></span> AUDIO FX: ON`
        : `<span class="led-dot mr-1.5"></span> AUDIO FX: OFF`;
      if (hwAudio.soundEnabled) hwAudio.playSwitchClick();
    });
  }

  // Initialize GSAP scroll animation
  setTimeout(() => {
    initGsapScrollytelling();
  }, 100);
});
