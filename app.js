/**
 * TapBot & TapStream — Titanium & Monochrome Obsidian Motion Engine
 * Modernist Engineering Showcase · Zero Emojis · Zero AI Slop
 */

// ═══════════════════════════════════════════════════════════════════════════
// 0. CONFIG & REDUCED MOTION CHECK
// ═══════════════════════════════════════════════════════════════════════════
const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

// ═══════════════════════════════════════════════════════════════════════════
// 1. TACTILE AUDIO SYNTHESIZER (Mechanical Studio Clicks)
// ═══════════════════════════════════════════════════════════════════════════
class TactileAudio {
  constructor() {
    this.ctx = null;
    this.enabled = true;
  }

  init() {
    if (!this.ctx) {
      const AudioCtx = window.AudioContext || window.webkitAudioContext;
      if (AudioCtx) this.ctx = new AudioCtx();
    }
  }

  playClick(freq = 480, duration = 0.028) {
    if (!this.enabled || prefersReducedMotion) return;
    try {
      this.init();
      if (!this.ctx) return;
      if (this.ctx.state === 'suspended') this.ctx.resume();

      const osc = this.ctx.createOscillator();
      const gain = this.ctx.createGain();

      osc.type = 'sine';
      osc.frequency.setValueAtTime(freq, this.ctx.currentTime);
      osc.frequency.exponentialRampToValueAtTime(freq * 0.35, this.ctx.currentTime + duration);

      gain.gain.setValueAtTime(0.035, this.ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.0001, this.ctx.currentTime + duration);

      osc.connect(gain);
      gain.connect(this.ctx.destination);

      osc.start();
      osc.stop(this.ctx.currentTime + duration);
    } catch (_) {}
  }

  playChirp() {
    if (!this.enabled || prefersReducedMotion) return;
    try {
      this.init();
      if (!this.ctx) return;
      if (this.ctx.state === 'suspended') this.ctx.resume();

      const osc = this.ctx.createOscillator();
      const gain = this.ctx.createGain();

      osc.type = 'triangle';
      osc.frequency.setValueAtTime(440, this.ctx.currentTime);
      osc.frequency.exponentialRampToValueAtTime(680, this.ctx.currentTime + 0.05);

      gain.gain.setValueAtTime(0.03, this.ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.0001, this.ctx.currentTime + 0.06);

      osc.connect(gain);
      gain.connect(this.ctx.destination);

      osc.start();
      osc.stop(this.ctx.currentTime + 0.06);
    } catch (_) {}
  }
}

const sfx = new TactileAudio();

// ═══════════════════════════════════════════════════════════════════════════
// 2. MONOCHROMATIC OSCILLOSCOPE CANVAS (Clean Titanium Waveforms)
// ═══════════════════════════════════════════════════════════════════════════
class OscilloscopeVisualizer {
  constructor() {
    this.canvas = document.getElementById('bg-visualizer');
    if (!this.canvas) return;
    this.ctx = this.canvas.getContext('2d');
    this.width = (this.canvas.width = window.innerWidth);
    this.height = (this.canvas.height = window.innerHeight);

    this.mouse = { x: this.width / 2, y: this.height / 2 };
    this.phase = 0;
    this.genre = 'precision';

    this.presets = {
      precision: {
        color1: 'rgba(56, 189, 248, 0.7)',
        color2: 'rgba(99, 102, 241, 0.5)',
        glow: 'rgba(56, 189, 248, 0.35)',
        waves: 3, amp: 44, freq: 0.0035, speed: 0.012
      },
      lofi: {
        color1: 'rgba(168, 85, 247, 0.7)',
        color2: 'rgba(244, 63, 94, 0.5)',
        glow: 'rgba(244, 63, 94, 0.35)',
        waves: 3, amp: 38, freq: 0.0028, speed: 0.010
      },
      synth: {
        color1: 'rgba(236, 72, 153, 0.75)',
        color2: 'rgba(245, 158, 11, 0.55)',
        glow: 'rgba(245, 158, 11, 0.4)',
        waves: 4, amp: 62, freq: 0.0055, speed: 0.020
      },
      ambient: {
        color1: 'rgba(52, 211, 153, 0.6)',
        color2: 'rgba(14, 165, 233, 0.45)',
        glow: 'rgba(52, 211, 153, 0.3)',
        waves: 2, amp: 30, freq: 0.002, speed: 0.007
      }
    };

    window.addEventListener('resize', () => this.resize());
    window.addEventListener('mousemove', (e) => {
      this.mouse.x = e.clientX;
      this.mouse.y = e.clientY;
    });

    this.animate();
  }

  resize() {
    this.width = (this.canvas.width = window.innerWidth);
    this.height = (this.canvas.height = window.innerHeight);
  }

  setPreset(name) {
    if (this.presets[name]) {
      this.genre = name;
      sfx.playClick(560);
      const label = document.getElementById('vis-track-name');
      if (label) {
        label.innerHTML = `AUDIO OSCILLOSCOPE // ${name.toUpperCase()} // 320 KBPS LIVE`;
      }
    }
  }

  animate() {
    requestAnimationFrame(() => this.animate());
    this.ctx.clearRect(0, 0, this.width, this.height);

    const cfg = this.presets[this.genre] || this.presets.precision;
    this.phase += cfg.speed;

    const dy = (this.mouse.y / this.height - 0.5) * 20;
    const baseAmp = cfg.amp + dy;

    // Fast, zero-shadowBlur dual stroke for high-performance 120fps rendering
    for (let w = 0; w < cfg.waves; w++) {
      this.ctx.beginPath();
      const wavePhase = this.phase + (w * Math.PI) / 3;
      const waveFreq = cfg.freq * (1 + w * 0.25);
      const waveAmp = baseAmp * (1 - w * 0.2);
      const baseline = this.height * 0.52 + w * 16;

      this.ctx.moveTo(0, baseline);
      for (let x = 0; x <= this.width; x += 10) {
        const y =
          baseline +
          Math.sin(x * waveFreq + wavePhase) * waveAmp +
          Math.sin(x * waveFreq * 1.8 + wavePhase * 1.4) * (waveAmp * 0.3);
        this.ctx.lineTo(x, y);
      }

      const grad = this.ctx.createLinearGradient(0, baseline - 60, this.width, baseline + 60);
      grad.addColorStop(0, cfg.color1);
      grad.addColorStop(1, cfg.color2);

      // Pass 1: Soft translucent aura glow
      this.ctx.strokeStyle = cfg.glow;
      this.ctx.lineWidth = 4.5;
      this.ctx.stroke();

      // Pass 2: Core crisp line
      this.ctx.strokeStyle = grad;
      this.ctx.lineWidth = 1.6;
      this.ctx.stroke();
    }
  }
}

// ═══════════════════════════════════════════════════════════════════════════
// 3. SPOTLIGHT BORDER HIGHLIGHT (Isolated per-card listener, zero reflow lag)
// ═══════════════════════════════════════════════════════════════════════════
function initSpotlights() {
  const cards = document.querySelectorAll('.spotlight-card');
  if (!cards.length) return;

  cards.forEach(card => {
    card.addEventListener('mousemove', (e) => {
      const rect = card.getBoundingClientRect();
      const x = e.clientX - rect.left;
      const y = e.clientY - rect.top;
      card.style.setProperty('--mouse-x', `${x}px`);
      card.style.setProperty('--mouse-y', `${y}px`);
    }, { passive: true });
  });
}

// ═══════════════════════════════════════════════════════════════════════════
// 4. 3D PERSPECTIVE TILT
// ═══════════════════════════════════════════════════════════════════════════
function init3DTilt() {
  if (prefersReducedMotion || window.innerWidth < 768) return;

  document.querySelectorAll('[data-tilt]').forEach(el => {
    el.addEventListener('mousemove', (e) => {
      const rect = el.getBoundingClientRect();
      const x = (e.clientX - rect.left) / rect.width - 0.5;
      const y = (e.clientY - rect.top) / rect.height - 0.5;
      const maxAngle = el.classList.contains('phone-3d-wrap') ? 10 : 5;

      gsap.to(el, {
        rotateY: x * maxAngle,
        rotateX: -y * maxAngle,
        duration: 0.35,
        ease: 'power2.out',
        transformPerspective: 1000,
      });
    });

    el.addEventListener('mouseleave', () => {
      gsap.to(el, {
        rotateY: 0,
        rotateX: 0,
        duration: 0.65,
        ease: 'elastic.out(1, 0.45)',
      });
    });
  });
}

// ═══════════════════════════════════════════════════════════════════════════
// 5. LIVE DEVICE SIMULATOR & TELEGRAM ENGINE (ZERO EMOJIS)
// ═══════════════════════════════════════════════════════════════════════════
class PhoneBotSimulator {
  constructor(visualizer) {
    this.visualizer = visualizer;
    this.activeBot = 'commander';

    this.chatFeed = document.getElementById('phone-chat-feed');
    this.simAvatar = document.getElementById('sim-avatar');
    this.simBotName = document.getElementById('sim-bot-name');
    this.simBotSub = document.getElementById('sim-bot-sub');
    this.simBotStatusText = document.getElementById('sim-bot-status-text');
    this.chipGroupLabel = document.getElementById('chip-group-label');
    this.phoneAudioPlayer = document.getElementById('phone-audio-player');
    this.modeSelectorBox = document.getElementById('mode-selector-box');

    this.trackTitle = document.getElementById('player-track-title');
    this.trackArtist = document.getElementById('player-track-artist');
    this.timeDisplay = document.getElementById('player-time-display');
    this.volDisplay = document.getElementById('player-vol-display');
    this.scrubberFill = document.getElementById('player-scrubber-fill');
    this.playBtn = document.getElementById('player-play-toggle');
    this.userInput = document.getElementById('phone-user-input');
    this.sendBtn = document.getElementById('phone-send-btn');
    this.modeBadge = document.getElementById('active-mode-badge');
    this.cardEqTitle = document.getElementById('card-eq-title');

    this.isPlaying = true;
    this.progressPct = 42;
    this.currentVolume = 80;
    this.currentMode = 'BOTH (SPEAKER + CHAT)';
    this.currentTrack = {
      title: 'Lofi Girl Study Beats',
      artist: 'Lofi Girl 24/7 &middot; 320kbps',
      preset: 'lofi'
    };

    this.initEvents();
    this.startScrubberTicker();
    this.switchBot('commander', true);
  }

  initEvents() {
    // Bot Switcher Tabs
    document.querySelectorAll('.bot-tab').forEach(tab => {
      tab.addEventListener('click', (e) => {
        const botId = e.currentTarget.getAttribute('data-bot');
        this.switchBot(botId);
      });
    });

    if (this.playBtn) {
      this.playBtn.addEventListener('click', () => this.togglePlay());
    }

    if (this.sendBtn && this.userInput) {
      this.sendBtn.addEventListener('click', () => {
        const val = this.userInput.value.trim();
        if (val) {
          this.executeCommand(val);
          this.userInput.value = '';
        }
      });

      this.userInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
          e.preventDefault();
          const val = this.userInput.value.trim();
          if (val) {
            this.executeCommand(val);
            this.userInput.value = '';
          }
        }
      });
    }

    document.querySelectorAll('.mode-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const mode = e.currentTarget.getAttribute('data-mode');
        document.querySelectorAll('.mode-btn').forEach(b => b.classList.remove('active'));
        e.currentTarget.classList.add('active');
        this.setMode(mode);
      });
    });

    document.querySelectorAll('.st-chip').forEach(chip => {
      chip.addEventListener('click', (e) => {
        document.querySelectorAll('.st-chip').forEach(c => c.classList.remove('active'));
        e.currentTarget.classList.add('active');
        const genre = e.currentTarget.getAttribute('data-st');
        this.executeCommand(`/radio ${genre}`);
      });
    });
  }

  switchBot(botId, isInitial = false) {
    this.activeBot = botId;
    if (!isInitial) sfx.playClick(620);

    // Update active tab button
    document.querySelectorAll('.bot-tab').forEach(t => {
      t.classList.toggle('active', t.getAttribute('data-bot') === botId);
    });

    // Show corresponding chips
    document.querySelectorAll('.bot-chips').forEach(grid => {
      grid.style.display = 'none';
    });
    const targetChips = document.getElementById(`chips-${botId}`);
    if (targetChips) targetChips.style.display = 'flex';

    // Show/hide music-specific widgets
    if (this.phoneAudioPlayer) {
      this.phoneAudioPlayer.style.display = botId === 'music' ? 'block' : 'none';
    }
    if (this.modeSelectorBox) {
      this.modeSelectorBox.style.display = botId === 'music' ? 'block' : 'none';
    }

    // Configure phone header according to bot
    if (botId === 'commander') {
      if (this.chipGroupLabel) this.chipGroupLabel.textContent = 'Commander Hardware Commands:';
      if (this.simAvatar) {
        this.simAvatar.textContent = 'CMD';
        this.simAvatar.style.background = 'linear-gradient(135deg, #0ea5e9, #6366f1)';
      }
      if (this.simBotName) this.simBotName.textContent = 'TapBot Commander';
      if (this.simBotStatusText) this.simBotStatusText.textContent = 'Hardware Node · Watchdog Active';
      this.resetChatFeed(
        '<b>TapBot Commander v2.2.0 Online</b><br/>' +
        'Native hardware controller bound to Android ART.<br/>' +
        'Click a command chip or type to trigger remote phone hardware.'
      );
    } else if (botId === 'music') {
      if (this.chipGroupLabel) this.chipGroupLabel.textContent = 'TapStream Music Commands:';
      if (this.simAvatar) {
        this.simAvatar.textContent = 'TS';
        this.simAvatar.style.background = 'linear-gradient(135deg, #f43f5e, #8b5cf6)';
      }
      if (this.simBotName) this.simBotName.textContent = 'TapStream Audio';
      if (this.simBotStatusText) this.simBotStatusText.textContent = 'Android ART · 320kbps Streamer';
      this.resetChatFeed(
        '<b>TapStream Music Engine Active</b><br/>' +
        'High-bitrate audio pipeline ready.<br/>' +
        'Routes to phone loudspeaker and in-chat audio tracks.'
      );
    } else if (botId === 'ai') {
      if (this.chipGroupLabel) this.chipGroupLabel.textContent = 'Gemini AI Assistant Commands:';
      if (this.simAvatar) {
        this.simAvatar.textContent = 'AI';
        this.simAvatar.style.background = 'linear-gradient(135deg, #8b5cf6, #fb7185)';
      }
      if (this.simBotName) this.simBotName.textContent = 'Gemini Assistant';
      if (this.simBotStatusText) this.simBotStatusText.textContent = 'Gemini 2.0 · Keystore Enclave';
      this.resetChatFeed(
        '<b>Gemini Smart Assistant Initialized</b><br/>' +
        'Generative AI runtime operating via on-device Keystore enclave.<br/>' +
        'Ask questions, request code, or summarize documents.'
      );
    } else if (botId === 'exon') {
      if (this.chipGroupLabel) this.chipGroupLabel.textContent = 'Exon Group Guardian Commands:';
      if (this.simAvatar) {
        this.simAvatar.textContent = 'EX';
        this.simAvatar.style.background = 'linear-gradient(135deg, #f59e0b, #f43f5e)';
      }
      if (this.simBotName) this.simBotName.textContent = 'Exon Guardian';
      if (this.simBotStatusText) this.simBotStatusText.textContent = 'Supergroup Shield · Anti-Flood';
      this.resetChatFeed(
        '<b>Exon Guardian v1.0.0 Active</b><br/>' +
        'Supergroup protection enabled on Android ART.<br/>' +
        'Anti-flood, spam filters, and admin tools standing by.'
      );
    }
  }

  resetChatFeed(initialBotHtml) {
    if (!this.chatFeed) return;
    this.chatFeed.innerHTML = `
      <div class="tg-bubble-bot">${initialBotHtml}</div>
      <div class="tg-typing-row" id="tg-typing" style="display:none;">
        <span class="tg-typing-dot"></span>
        <span class="tg-typing-dot"></span>
        <span class="tg-typing-dot"></span>
      </div>
    `;
  }

  togglePlay() {
    this.isPlaying = !this.isPlaying;
    sfx.playClick(this.isPlaying ? 600 : 380);
    const svgWrap = document.getElementById('player-play-svg');
    if (svgWrap) {
      svgWrap.innerHTML = this.isPlaying
        ? `<rect x="6" y="4" width="4" height="16"/><rect x="14" y="4" width="4" height="16"/>`
        : `<polygon points="5 3 19 12 5 21 5 3"/>`;
    }
    const vinyl = document.getElementById('player-vinyl');
    if (vinyl) {
      if (this.isPlaying) {
        vinyl.classList.remove('paused');
        vinyl.classList.add('spinning');
      } else {
        vinyl.classList.add('paused');
      }
    }
    const eqBars = document.querySelectorAll('.p-eq-bar, .eq-bar');
    eqBars.forEach(bar => {
      bar.style.animationPlayState = this.isPlaying ? 'running' : 'paused';
      bar.style.opacity = this.isPlaying ? '1' : '0.2';
    });
  }

  setMode(mode) {
    sfx.playClick(520);
    let label = 'BOTH (SPEAKER + CHAT)';
    if (mode === 'SPEAKER') label = 'PHONE SPEAKER ONLY';
    if (mode === 'CHAT') label = 'TELEGRAM CHAT ONLY';
    this.currentMode = label;
    if (this.modeBadge) this.modeBadge.textContent = label;
    this.appendBotMessage(`ROUTING: Playback routing set to <b>${label}</b>`);
  }

  startScrubberTicker() {
    setInterval(() => {
      if (this.isPlaying) {
        this.progressPct = (this.progressPct + 0.4) % 100;
        if (this.scrubberFill) {
          this.scrubberFill.style.width = `${this.progressPct}%`;
        }
      }
    }, 400);
  }

  showTyping() {
    const typing = document.getElementById('tg-typing');
    if (typing && this.chatFeed) {
      this.chatFeed.appendChild(typing);
      typing.style.display = 'flex';
      this.chatFeed.scrollTop = this.chatFeed.scrollHeight;
    }
  }

  hideTyping() {
    const typing = document.getElementById('tg-typing');
    if (typing) {
      typing.style.display = 'none';
    }
  }

  executeCommand(rawText, fromDemo = false) {
    const text = rawText.trim();
    if (!text) return;

    if (!fromDemo) sfx.playClick(580);
    this.appendUserMessage(text);
    this.showTyping();

    setTimeout(() => {
      this.hideTyping();
      this.resolveCommand(text);
      if (!fromDemo) sfx.playChirp();
    }, 420);
  }

  resolveCommand(text) {
    const parts = text.split(/\s+/);
    const cmd = parts[0].toLowerCase();
    const arg = parts.slice(1).join(' ').trim();

    // ─── COMMANDER BOT COMMANDS ───
    if (cmd === '/status') {
      this.appendBotMessage(
        `<b>TELEMETRY // ART PROCESS PID 8412</b><br/>` +
        `BATTERY: <b>88% [CHARGING_AC]</b><br/>` +
        `THERMAL: <b>31.4°C [NOMINAL]</b><br/>` +
        `HEAP RAM: <b>14.2 MB / 8 GB (0.17%)</b><br/>` +
        `STORAGE: <b>0 Bytes/hr [RAM_RING_BUFFER]</b><br/>` +
        `UPTIME: <b>42d 18h 34m</b><br/>` +
        `WATCHDOG: <b>ACTIVE [AUTO_RECOVERY_ARMED]</b>`
      );
    } else if (cmd === '/torch') {
      const state = arg.toLowerCase() === 'off' ? 'DISABLED' : 'ENABLED (MAX)';
      this.appendBotMessage(
        `<b>HARDWARE // CameraManager</b><br/>` +
        `TORCH STATE: <b>${state}</b><br/>` +
        `ACTION: Dispatched LED controller command to physical camera unit.`
      );
    } else if (cmd === '/vibrate') {
      const ms = parseInt(arg, 10) || 500;
      this.appendBotMessage(
        `<b>HAPTIC // Android Vibrator</b><br/>` +
        `DURATION: <b>${ms}ms</b> waveform<br/>` +
        `STATUS: Physical haptic motor pulse dispatched successfully.`
      );
    } else if (cmd === '/notify') {
      const msg = arg || 'Server health optimal';
      this.appendBotMessage(
        `<b>NOTIFICATION // NotificationHelper</b><br/>` +
        `CHANNEL: <code>tapbot_high_priority</code><br/>` +
        `MESSAGE: <i>"${msg}"</i><br/>` +
        `STATUS: Posted to Android system notification tray.`
      );
    } else if (cmd === '/apk' || cmd === '/download') {
      this.appendBotMessage(
        `<b>DELIVERY // ApkDeliveryService</b><br/>` +
        `PACKAGE: <code>tapbot-v2.2.0-tapstream-music.apk</code><br/>` +
        `SIZE: <b>19.3 MB</b> &middot; Android 8.0 &ndash; 15+<br/>` +
        `STATUS: Streaming signed binary directly to Telegram document pipe.`
      );
    } else if (cmd === '/fleet') {
      this.appendBotMessage(
        `<b>FLEET // Active On-Device Bots</b><br/>` +
        `1. <b>CommanderBot</b> &middot; PID 8412 &middot; <span style="color:#10b981">ONLINE</span> (3.2 MB)<br/>` +
        `2. <b>TapStream</b> &middot; PID 8413 &middot; <span style="color:#10b981">ONLINE</span> (5.8 MB)<br/>` +
        `3. <b>GeminiAI</b> &middot; PID 8414 &middot; <span style="color:#10b981">ONLINE</span> (4.1 MB)<br/>` +
        `TOTAL HEAP: <b>13.1 MB</b> (0.16% of device RAM)`
      );
    }
    // ─── TAPSTREAM MUSIC COMMANDS ───
    else if (cmd === '/play') {
      const songName = arg ? arg.charAt(0).toUpperCase() + arg.slice(1) : 'Lofi Study Beats';
      this.currentTrack = {
        title: songName,
        artist: 'TapStream Audio &middot; 320kbps',
        preset: 'lofi'
      };
      this.updatePlayerUI();
      if (this.visualizer) this.visualizer.setPreset('lofi');

      this.appendBotMessage(
        `NOW PLAYING: <b>${songName}</b><br/>` +
        `CODEC: 320 kbps (yt-dlp stream)<br/>` +
        `ROUTE: <b>${this.currentMode}</b><br/>` +
        `STATUS: Playback initiated through Android audio track.`
      );
    } else if (cmd === '/radio') {
      const genre = arg.toLowerCase() || 'synthwave';
      const radioMap = {
        lofi: { title: 'Lofi Girl Study Beats', artist: 'Lofi Girl 24/7', preset: 'lofi' },
        synthwave: { title: 'Nightwave Plaza 80s Cyber', artist: 'Plaza FM Live', preset: 'synth' },
        rock: { title: 'Classic Rock Live FM', artist: 'Planet Rock', preset: 'precision' },
        jazz: { title: 'Jazz Lounge & Bossa Beats', artist: 'Jazz Radio HQ', preset: 'ambient' },
        space: { title: 'Deep Space Ambient Drone', artist: 'SomaFM Zero Buffer', preset: 'ambient' }
      };

      const match = radioMap[genre] || radioMap.synthwave;
      this.currentTrack = match;
      this.updatePlayerUI();
      if (this.visualizer) this.visualizer.setPreset(match.preset);

      this.appendBotMessage(
        `TUNER: 24/7 Radio Stream Active<br/>` +
        `STATION: <b>${match.title}</b><br/>` +
        `CHANNEL: <i>${match.artist}</i> &middot; 320 kbps<br/>` +
        `BUFFER: 0ms (In-Memory RAM Ring Buffer)`
      );
    } else if (cmd === '/nowplaying' || cmd === '/np') {
      this.appendBotMessage(
        `STATUS: <b>${this.currentTrack.title}</b><br/>` +
        `<code>PLAY [01:42 / 03:45] ====o--------</code><br/>` +
        `STREAM: <i>${this.currentTrack.artist}</i> &middot; ${this.currentMode}`
      );
    } else if (cmd === '/volume') {
      const val = parseInt(arg, 10);
      if (!isNaN(val)) {
        this.currentVolume = Math.max(0, Math.min(100, val));
        if (this.volDisplay) this.volDisplay.textContent = `Vol: ${this.currentVolume}%`;
        this.appendBotMessage(`VOLUME: AudioManager hardware level adjusted to <b>${this.currentVolume}%</b>`);
      } else {
        this.appendBotMessage(`VOLUME: Current level is <b>${this.currentVolume}%</b> (use <code>/volume 0-100</code>)`);
      }
    } else if (cmd === '/mode') {
      this.setMode(arg.toUpperCase());
    }
    // ─── GEMINI AI ASSISTANT COMMANDS ───
    else if (cmd === '/ask') {
      this.appendBotMessage(
        `<b>GEMINI 2.0 // ON-DEVICE INFERENCE</b><br/>` +
        `Android phones make extraordinary autonomous servers: low-power multi-core ARM chips, internal battery failover, Wi-Fi connectivity, and native ART execution without heavy Docker or Node.js runtime layers.`
      );
    } else if (cmd === '/code') {
      this.appendBotMessage(
        `<b>KOTLIN // Non-Blocking Coroutine Flow</b><br/>` +
        `<pre style="background:#090d15;padding:7px;border-radius:5px;font-family:monospace;font-size:10px;color:#c084fc;margin-top:5px;overflow-x:auto;"><code>fun pollUpdates(): Flow&lt;Update&gt; = flow {
  while (isActive) {
    val updates = client.getUpdates(offset)
    emitAll(updates.asFlow())
  }
}</code></pre>`
      );
    } else if (cmd === '/summarize') {
      this.appendBotMessage(
        `<b>AI SPECIFICATION SUMMARY //</b><br/>` +
        `&bull; 100% on-device Android ART execution<br/>` +
        `&bull; Cryptographic Keystore token encryption<br/>` +
        `&bull; RAM ring buffers eliminate flash wear<br/>` +
        `&bull; Runs multiple production bots for $0.00/mo`
      );
    } else if (cmd === '/model') {
      this.appendBotMessage(
        `<b>AI MODEL CONFIGURATION //</b><br/>` +
        `ENGINE: Google Gemini 2.0 Flash<br/>` +
        `KEYSTORE: StrongBox / TEE Hardware Sealed (AES-256-GCM)<br/>` +
        `PRIVACY: Direct outbound TLS 1.3 to official endpoints.`
      );
    }
    // ─── EXON GUARDIAN COMMANDS ───
    else if (cmd === '/admin') {
      this.appendBotMessage(
        `<b>EXON // Supergroup Administrator Shield</b><br/>` +
        `COMMUNITY: TapBot Global Hub<br/>` +
        `ANTI-FLOOD: <span style="color:#10b981">ARMED [5 msgs / 10s]</span><br/>` +
        `SPAM FILTER: <span style="color:#10b981">ACTIVE</span><br/>` +
        `STATUS: All group members protected.`
      );
    } else if (cmd === '/antiflood') {
      this.appendBotMessage(
        `<b>EXON // Anti-Flood Settings</b><br/>` +
        `STATUS: <b>ENABLED</b><br/>` +
        `RATE LIMIT: Maximum 5 messages in 10 seconds.<br/>` +
        `ACTION ON FLOOD: Automatic 2-hour mute applied.`
      );
    } else if (cmd === '/rules') {
      this.appendBotMessage(
        `<b>COMMUNITY RULES //</b><br/>` +
        `1. Be respectful to all builders & users.<br/>` +
        `2. No unauthorized spam, ads, or referral links.<br/>` +
        `3. Enjoy autonomous, decentralized bot hosting!`
      );
    } else if (cmd === '/warn' || cmd === '/mute') {
      this.appendBotMessage(
        `<b>MODERATION // Action Dispatched</b><br/>` +
        `ACTION: Issued formal infraction warning.<br/>` +
        `AUDIT LOG: Timestamped and recorded in local ART memory.`
      );
    }
    // ─── DEFAULT FALLBACK ───
    else {
      this.appendBotMessage(
        `INPUT: <code>${text}</code><br/>` +
        `Active bot commands available: <code>/status</code>, <code>/torch</code>, <code>/play</code>, <code>/ask</code>, <code>/admin</code>, <code>/apk</code>`
      );
    }
  }

  updatePlayerUI() {
    if (this.trackTitle) this.trackTitle.innerHTML = this.currentTrack.title;
    if (this.trackArtist) this.trackArtist.innerHTML = this.currentTrack.artist;
    if (this.cardEqTitle) this.cardEqTitle.innerHTML = `${this.currentTrack.title} (${this.currentTrack.artist})`;
  }

  appendUserMessage(text) {
    if (!this.chatFeed) return;
    const div = document.createElement('div');
    div.className = 'tg-bubble-user';
    div.textContent = text;
    this.chatFeed.appendChild(div);
    this.chatFeed.scrollTop = this.chatFeed.scrollHeight;
  }

  appendBotMessage(html) {
    if (!this.chatFeed) return;
    this.hideTyping();
    const div = document.createElement('div');
    div.className = 'tg-bubble-bot';
    div.innerHTML = html;
    this.chatFeed.appendChild(div);
    this.chatFeed.scrollTop = this.chatFeed.scrollHeight;
  }
}

// ═══════════════════════════════════════════════════════════════════════════
// 6. MAGNETIC CURSOR LOGIC
// ═══════════════════════════════════════════════════════════════════════════
function initMagneticCursor() {
  if (prefersReducedMotion || window.innerWidth < 768) return;

  const cursor = document.getElementById('cursor');
  const follower = document.getElementById('cursor-follower');
  if (!cursor || !follower) return;

  let cx = window.innerWidth / 2;
  let cy = window.innerHeight / 2;
  let fx = cx;
  let fy = cy;

  window.addEventListener('mousemove', (e) => {
    cx = e.clientX;
    cy = e.clientY;
    cursor.style.left = `${cx}px`;
    cursor.style.top = `${cy}px`;
  });

  function tick() {
    fx += (cx - fx) * 0.14;
    fy += (cy - fy) * 0.14;
    follower.style.left = `${fx}px`;
    follower.style.top = `${fy}px`;
    requestAnimationFrame(tick);
  }
  tick();

  document.querySelectorAll('a, button, input, [data-magnetic]').forEach(el => {
    el.addEventListener('mouseenter', () => document.body.classList.add('cursor-hover'));
    el.addEventListener('mouseleave', () => document.body.classList.remove('cursor-hover'));
  });

  document.querySelectorAll('[data-magnetic]').forEach(el => {
    el.addEventListener('mousemove', (e) => {
      const rect = el.getBoundingClientRect();
      const dx = e.clientX - (rect.left + rect.width / 2);
      const dy = e.clientY - (rect.top + rect.height / 2);
      gsap.to(el, { x: dx * 0.24, y: dy * 0.24, duration: 0.25, ease: 'power2.out' });
    });
    el.addEventListener('mouseleave', () => {
      gsap.to(el, { x: 0, y: 0, duration: 0.5, ease: 'elastic.out(1, 0.45)' });
    });
  });
}

// ═══════════════════════════════════════════════════════════════════════════
// 7. HIGH-PERFORMANCE GPU SCROLL ANIMATIONS (Native & Silky 120fps)
// ═══════════════════════════════════════════════════════════════════════════
function initGSAPAnimations() {
  if (typeof gsap === 'undefined' || typeof ScrollTrigger === 'undefined') return;
  gsap.registerPlugin(ScrollTrigger);

  const progBar = document.getElementById('scroll-progress');
  if (progBar) {
    gsap.to(progBar, {
      width: '100%',
      ease: 'none',
      scrollTrigger: {
        trigger: document.body,
        start: 'top top',
        end: 'bottom bottom',
        scrub: 0.1,
      }
    });
  }

  ScrollTrigger.create({
    start: 24,
    onUpdate: (self) => {
      const nav = document.getElementById('nav');
      if (nav) nav.classList.toggle('scrolled', self.progress > 0);
    }
  });

  if (prefersReducedMotion) return;

  // Lightweight single-pass reveals: Zero DOM fragmentation, zero recalculation lag
  document.querySelectorAll('[data-reveal="chars"], [data-reveal="words"]').forEach(el => {
    gsap.from(el, {
      opacity: 0,
      y: 24,
      duration: 0.7,
      ease: 'power2.out',
      scrollTrigger: {
        trigger: el,
        start: 'top 88%',
        once: true,
      }
    });
  });

  document.querySelectorAll('[data-reveal="fade"]').forEach(el => {
    gsap.from(el, {
      opacity: 0,
      y: 18,
      duration: 0.65,
      ease: 'power2.out',
      scrollTrigger: {
        trigger: el,
        start: 'top 90%',
        once: true,
      }
    });
  });

  document.querySelectorAll('[data-reveal="slide-up"]').forEach((el, i) => {
    gsap.from(el, {
      opacity: 0,
      y: 32,
      duration: 0.7,
      delay: (i % 4) * 0.08,
      ease: 'power2.out',
      scrollTrigger: {
        trigger: el,
        start: 'top 92%',
        once: true,
      }
    });
  });

  document.querySelectorAll('[data-count]').forEach(el => {
    const target = parseFloat(el.getAttribute('data-count'));
    const prefix = el.getAttribute('data-prefix') || '';
    const suffix = el.getAttribute('data-suffix') || '';
    const isFloat = String(target).includes('.');

    ScrollTrigger.create({
      trigger: el,
      start: 'top 92%',
      once: true,
      onEnter: () => {
        const valObj = { val: 0 };
        gsap.to(valObj, {
          val: target,
          duration: 1.6,
          ease: 'power2.out',
          onUpdate: () => {
            el.textContent = `${prefix}${isFloat ? valObj.val.toFixed(1) : Math.round(valObj.val)}${suffix}`;
          }
        });
      }
    });
  });
}

// ═══════════════════════════════════════════════════════════════════════════
// 8. LIVE TICKING UPTIME CLOCK
// ═══════════════════════════════════════════════════════════════════════════
function initUptimeTicker() {
  const el = document.getElementById('live-uptime-counter');
  if (!el) return;
  let seconds = 42 * 86400 + 18 * 3600 + 34 * 60 + 12;

  setInterval(() => {
    seconds++;
    const d = Math.floor(seconds / 86400);
    const h = Math.floor((seconds % 86400) / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = seconds % 60;
    el.textContent = `${d}d ${h}h ${m}m ${s < 10 ? '0' : ''}${s}s`;
  }, 1000);
}

// ═══════════════════════════════════════════════════════════════════════════
// 9. BOOTSTRAP ON LOAD (Native Smooth Scroll Enabled)
// ═══════════════════════════════════════════════════════════════════════════
document.addEventListener('DOMContentLoaded', () => {
  const visualizer = new OscilloscopeVisualizer();
  const sim = new PhoneBotSimulator(visualizer);

  initSpotlights();
  init3DTilt();
  initMagneticCursor();
  initGSAPAnimations();
  initUptimeTicker();

  document.querySelectorAll('.vis-btn').forEach(btn => {
    btn.addEventListener('click', (e) => {
      document.querySelectorAll('.vis-btn').forEach(b => b.classList.remove('active'));
      e.currentTarget.classList.add('active');
      const g = e.currentTarget.getAttribute('data-genre');
      if (visualizer) visualizer.setPreset(g);
    });
  });

  const soundBtn = document.getElementById('sound-toggle-btn');
  const soundLabel = document.getElementById('sound-label');
  if (soundBtn && soundLabel) {
    soundBtn.addEventListener('click', () => {
      sfx.enabled = !sfx.enabled;
      soundLabel.textContent = sfx.enabled ? 'Audio: ON' : 'Audio: OFF';
      soundBtn.querySelector('.sound-dot').style.opacity = sfx.enabled ? '1' : '0.3';
      if (sfx.enabled) sfx.playClick(560);
    });
  }

  document.querySelectorAll('.cmd-chip').forEach(chip => {
    chip.addEventListener('click', (e) => {
      const cmd = e.currentTarget.getAttribute('data-cmd');
      if (cmd && sim) {
        sim.executeCommand(cmd);
      }
    });
  });
});
