/**
 * TapBot & TapStream - Interactive Cyber Scrollytelling & Audio Engine
 * Pure Vanilla JavaScript & HTML5 Web Audio API
 */

// =============================================================================
// 1. PROCEDURAL WEB AUDIO SYNTHESIZER (NO EXTERNAL ASSETS NEEDED)
// =============================================================================
class WebAudioFeedback {
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

  playBeep(freq = 580, type = 'sine', duration = 0.08, gainVal = 0.08) {
    if (!this.soundEnabled) return;
    try {
      this.init();
      if (!this.ctx) return;
      if (this.ctx.state === 'suspended') {
        this.ctx.resume();
      }
      const osc = this.ctx.createOscillator();
      const gain = this.ctx.createGain();
      osc.type = type;
      osc.frequency.setValueAtTime(freq, this.ctx.currentTime);
      gain.gain.setValueAtTime(gainVal, this.ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.0001, this.ctx.currentTime + duration);
      osc.connect(gain);
      gain.connect(this.ctx.destination);
      osc.start();
      osc.stop(this.ctx.currentTime + duration);
    } catch (_) {}
  }

  playSuccessChime() {
    this.playBeep(523.25, 'sine', 0.1, 0.05); // C5
    setTimeout(() => this.playBeep(659.25, 'sine', 0.1, 0.05), 80); // E5
    setTimeout(() => this.playBeep(783.99, 'sine', 0.18, 0.05), 160); // G5
  }

  playClick() {
    this.playBeep(920, 'triangle', 0.04, 0.03);
  }
}

const audioFeedback = new WebAudioFeedback();

// =============================================================================
// 2. REAL-TIME CANVAS AUDIO SPECTRUM VISUALIZER
// =============================================================================
class SpectrumVisualizer {
  constructor(canvasId) {
    this.canvas = document.getElementById(canvasId);
    if (!this.canvas) return;
    this.ctx = this.canvas.getContext('2d');
    this.isPlaying = true;
    this.bars = 64;
    this.barHeights = new Float32Array(this.bars);
    this.targetHeights = new Float32Array(this.bars);
    this.phase = 0;
    this.resize();
    window.addEventListener('resize', () => this.resize());
    this.animate();
  }

  resize() {
    if (!this.canvas) return;
    this.canvas.width = this.canvas.parentElement.clientWidth * window.devicePixelRatio;
    this.canvas.height = this.canvas.clientHeight * window.devicePixelRatio;
    this.ctx.scale(window.devicePixelRatio, window.devicePixelRatio);
  }

  setPlaying(playing) {
    this.isPlaying = playing;
  }

  animate() {
    requestAnimationFrame(() => this.animate());
    if (!this.canvas || !this.ctx) return;

    const width = this.canvas.parentElement.clientWidth;
    const height = this.canvas.clientHeight;
    this.ctx.clearRect(0, 0, width, height);

    this.phase += this.isPlaying ? 0.07 : 0.01;
    const barWidth = Math.max(2, (width / this.bars) - 2.5);

    for (let i = 0; i < this.bars; i++) {
      if (this.isPlaying) {
        const wave1 = Math.sin(this.phase + i * 0.22);
        const wave2 = Math.cos(this.phase * 0.8 + i * 0.12);
        const rand = (Math.random() - 0.5) * 8;
        const target = Math.max(6, (Math.abs(wave1 * wave2) * (height * 0.85)) + rand);
        this.barHeights[i] += (target - this.barHeights[i]) * 0.25;
      } else {
        this.barHeights[i] += (4 - this.barHeights[i]) * 0.1;
      }

      const h = this.barHeights[i];
      const x = i * (barWidth + 2.5);
      const y = (height - h) / 2;

      // Color gradient transition from cyan to purple
      const gradient = this.ctx.createLinearGradient(0, y, 0, y + h);
      const ratio = i / this.bars;
      if (ratio < 0.5) {
        gradient.addColorStop(0, '#00f5d4');
        gradient.addColorStop(1, '#0284c7');
      } else {
        gradient.addColorStop(0, '#a855f7');
        gradient.addColorStop(1, '#ec4899');
      }

      this.ctx.fillStyle = gradient;
      this.ctx.beginPath();
      this.ctx.roundRect(x, y, barWidth, h, [3, 3, 3, 3]);
      this.ctx.fill();
    }
  }
}

// =============================================================================
// 3. INTERACTIVE PHONE SIMULATOR ENGINE (TAPSTREAM MUSIC & COMMANDER)
// =============================================================================
class PhoneBotSimulator {
  constructor() {
    this.chatBody = document.getElementById('sim-chat-body');
    this.inputField = document.getElementById('sim-input-text');
    this.statusTitle = document.getElementById('sim-nowplaying-title');
    this.statusArtist = document.getElementById('sim-nowplaying-artist');
    this.volumeSlider = document.getElementById('sim-volume-slider');
    this.volumePct = document.getElementById('sim-volume-val');
    this.routingModeBadge = document.getElementById('sim-routing-mode');
    this.soundWaveContainer = document.getElementById('sim-sound-waves');

    this.currentTrack = {
      title: 'Lofi Girl Study Beats',
      artist: 'Lofi Girl 24/7',
      duration: 'LIVE 🔴',
      source: '📻 Radio Station'
    };
    this.isPlaying = true;
    this.currentMode = 'BOTH'; // SPEAKER, CHAT, BOTH
    this.currentVolume = 80;

    this.initEvents();
  }

  initEvents() {
    if (this.inputField) {
      this.inputField.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
          e.preventDefault();
          this.submitCommand(this.inputField.value);
          this.inputField.value = '';
        }
      });
    }

    if (this.volumeSlider) {
      this.volumeSlider.addEventListener('input', (e) => {
        this.setVolume(parseInt(e.target.value, 10));
      });
    }
  }

  setVolume(vol) {
    this.currentVolume = Math.max(0, Math.min(100, vol));
    if (this.volumePct) this.volumePct.innerText = `${this.currentVolume}%`;
    if (this.volumeSlider) this.volumeSlider.value = this.currentVolume;
    audioFeedback.playBeep(400 + this.currentVolume * 5, 'sine', 0.04, 0.03);
  }

  submitCommand(text) {
    const trimmed = text.trim();
    if (!trimmed) return;
    audioFeedback.playClick();

    // 1. Append user message bubble
    this.appendMessage(trimmed, 'user');

    // 2. Show typing indicator
    const typingIndicator = this.appendTypingIndicator();

    // 3. Dispatch and reply after short simulated latency (180ms)
    setTimeout(() => {
      typingIndicator.remove();
      this.processBotResponse(trimmed);
      audioFeedback.playSuccessChime();
    }, 280);
  }

  processBotResponse(rawText) {
    const parts = rawText.split(/\s+/);
    const cmd = parts[0].toLowerCase();
    const args = parts.slice(1).join(' ').trim();

    let reply = '';

    switch (cmd) {
      case '/start':
      case '/help':
        reply = `🎵 <b>TapStream v2.2.0 Active!</b>\n` +
          `• <code>/play &lt;query&gt;</code> - Stream YouTube, yt-dlp, or direct URL\n` +
          `• <code>/radio &lt;genre&gt;</code> - 24/7 stations: lofi, synthwave, rock, jazz\n` +
          `• <code>/pause</code> | <code>/resume</code> | <code>/skip</code>\n` +
          `• <code>/nowplaying</code> - Visual ASCII progress bar\n` +
          `• <code>/volume &lt;0-100&gt;</code> - Android phone speaker volume\n` +
          `• <code>/mode &lt;speaker|chat|both&gt;</code> - Audio routing`;
        break;

      case '/play':
        if (!args) {
          reply = `⚠️ Usage: <code>/play &lt;song title, artist, or YouTube URL&gt;</code>\nExample: <code>/play lofi</code> or <code>/play synthwave</code>`;
        } else {
          const trackTitle = args.charAt(0).toUpperCase() + args.slice(1);
          this.currentTrack = {
            title: trackTitle,
            artist: 'TapStream Cloud Audio',
            duration: '03:45',
            source: '⚡ Stream Extractor'
          };
          this.isPlaying = true;
          this.updateNowPlayingUI();
          reply = `🎧 <b>Queued & Streaming Now</b>\n` +
            `▶️ <b>${this.currentTrack.title}</b>\n` +
            `👤 <i>${this.currentTrack.artist}</i>\n` +
            `🔊 Output: <b>${this.currentMode}</b> (Vol: ${this.currentVolume}%)\n` +
            `📦 Delivering audio stream to Telegram chat...`;
        }
        break;

      case '/radio':
        const genre = args.toLowerCase() || 'lofi';
        const stations = {
          lofi: { title: 'Lofi Girl Study Beats', artist: 'Lofi Girl 24/7' },
          synthwave: { title: 'Nightwave Plaza 80s Cyber', artist: 'Plaza FM' },
          rock: { title: 'Classic Rock Live FM', artist: 'Planet Rock' },
          chillhop: { title: 'Chillhop Cafe Beats', artist: 'Chillhop Music' },
          jazz: { title: 'Jazz Cafe & Smooth Bossa', artist: 'Jazz Radio Lounge' },
          space: { title: 'Deep Space Ambient', artist: 'SomaFM Drone Zone' }
        };
        const station = stations[genre] || stations.lofi;
        this.currentTrack = {
          title: station.title,
          artist: station.artist,
          duration: 'LIVE 🔴',
          source: '📻 24/7 Radio Station'
        };
        this.isPlaying = true;
        this.updateNowPlayingUI();
        reply = `📻 <b>Tuned into 24/7 Radio Station</b>\n` +
          `📡 <b>${station.title}</b> (${genre.toUpperCase()})\n` +
          `🎙 <i>${station.artist}</i>\n` +
          `🔴 Streaming live at 320kbps with zero buffer lag.`;
        break;

      case '/nowplaying':
      case '/np':
        reply = `🎶 <b>Now Playing on Android Host</b>\n` +
          `<b>${this.currentTrack.title}</b>\n` +
          `<i>${this.currentTrack.artist}</i>\n` +
          `<code>▶ 01:42 ━━━━━●──────── 03:45</code>\n` +
          `🔊 Volume: <b>${this.currentVolume}%</b> | Route: <code>${this.currentMode}</code>`;
        break;

      case '/pause':
        this.isPlaying = false;
        this.updateNowPlayingUI();
        reply = `⏸ <b>Playback paused.</b> Send <code>/resume</code> to continue.`;
        break;

      case '/resume':
        this.isPlaying = true;
        this.updateNowPlayingUI();
        reply = `▶️ <b>Resumed playback:</b> ${this.currentTrack.title}`;
        break;

      case '/volume':
      case '/vol':
        const val = parseInt(args, 10);
        if (!isNaN(val)) {
          this.setVolume(val);
          reply = `🔊 Host device media speaker volume set to <b>${this.currentVolume}%</b>`;
        } else {
          reply = `🔊 Current volume: <b>${this.currentVolume}%</b>. Use <code>/volume &lt;0-100&gt;</code> to adjust.`;
        }
        break;

      case '/mode':
        const mode = args.toLowerCase();
        if (mode.includes('speaker') || mode.includes('phone')) {
          this.currentMode = 'SPEAKER';
          reply = `🔊 Playback mode: <b>SPEAKER ONLY</b>\nAudio streams through your phone's physical speaker & Bluetooth.`;
        } else if (mode.includes('chat') || mode.includes('telegram')) {
          this.currentMode = 'CHAT';
          reply = `💬 Playback mode: <b>TELEGRAM CHAT ONLY</b>\nAudio sent as playable music files directly into chat.`;
        } else {
          this.currentMode = 'BOTH';
          reply = `✨ Playback mode: <b>BOTH (SPEAKER + CHAT)</b>\nAudio plays on host phone speaker AND delivers to chat.`;
        }
        if (this.routingModeBadge) this.routingModeBadge.innerText = this.currentMode;
        break;

      case '/status':
        reply = `📊 <b>TapBot Hardware Telemetry</b>\n` +
          `🔋 Battery: <b>88% (AC Plugged)</b>\n` +
          `🧠 RAM: <b>14.2 MB / 8 GB (0.17%)</b>\n` +
          `⚡ CPU: <b>0.4% (ART Runtime)</b>\n` +
          `⏱ Uptime: <b>42d 18h 12m</b>\n` +
          `🛡 Flash Writes: <b>0 bytes / hr (Ring Buffer)</b>\n` +
          `🌐 Network: <b>Wi-Fi 5GHz (12ms latency)</b>`;
        break;

      default:
        reply = `🎵 Received: <code>${rawText}</code>\nSend <code>/help</code> or <code>/play lofi</code> to start music!`;
    }

    this.appendMessage(reply, 'bot');
  }

  updateNowPlayingUI() {
    if (this.statusTitle) this.statusTitle.innerText = this.currentTrack.title;
    if (this.statusArtist) this.statusArtist.innerText = `${this.currentTrack.artist} • ${this.currentTrack.duration}`;
    if (this.soundWaveContainer) {
      this.soundWaveContainer.style.opacity = this.isPlaying ? '1' : '0.2';
    }
  }

  appendMessage(htmlText, sender = 'bot') {
    if (!this.chatBody) return;
    const div = document.createElement('div');
    div.className = sender === 'user' ? 'chat-bubble-user' : 'chat-bubble-bot';
    div.innerHTML = htmlText.replace(/\n/g, '<br/>');
    this.chatBody.appendChild(div);
    this.chatBody.scrollTop = this.chatBody.scrollHeight;
    return div;
  }

  appendTypingIndicator() {
    const div = document.createElement('div');
    div.className = 'chat-bubble-bot flex items-center gap-1.5 py-2 px-3 text-slate-400 text-xs italic';
    div.innerHTML = `<span class="animate-pulse">●</span><span class="animate-pulse delay-100">●</span><span class="animate-pulse delay-200">●</span> <span class="ml-1 text-[11px]">TapStream is resolving...</span>`;
    this.chatBody.appendChild(div);
    this.chatBody.scrollTop = this.chatBody.scrollHeight;
    return div;
  }
}

// =============================================================================
// 4. SCROLLYTELLING PROGRESS & INTERSECTION OBSERVER
// =============================================================================
function initScrollytelling() {
  const progressBar = document.getElementById('scroll-progress-line');
  const storySteps = document.querySelectorAll('.story-step');

  window.addEventListener('scroll', () => {
    const scrollTop = window.scrollY || document.documentElement.scrollTop;
    const docHeight = document.documentElement.scrollHeight - document.documentElement.clientHeight;
    const progress = Math.min(100, Math.max(0, (scrollTop / docHeight) * 100));
    if (progressBar) progressBar.style.width = `${progress}%`;
  }, { passive: true });

  const observer = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting) {
        entry.target.classList.add('is-active');
      }
    });
  }, { threshold: 0.25 });

  storySteps.forEach(step => observer.observe(step));
}

// =============================================================================
// 5. BOTPKG CODE INSPECTOR TABS
// =============================================================================
function initPackageInspector() {
  const tabs = document.querySelectorAll('[data-pkg-tab]');
  const codeContent = document.getElementById('pkg-code-display');
  const codeLang = document.getElementById('pkg-code-lang');

  const files = {
    manifest: {
      lang: 'JSON',
      code: `{\n  "$schema": "https://tapbot.dev/schemas/v1/manifest.json",\n  "id": "tapstream-music",\n  "name": "TapStream Music Bot",\n  "version": "1.0.0",\n  "runtime": "native_art",\n  "entrypoint": "com.tapbot.core.runner.runtime.MusicBotRuntime",\n  "credentials": [\n    {\n      "key": "bot_token",\n      "label": "Telegram Bot Token",\n      "required": true,\n      "secret": true\n    },\n    {\n      "key": "stream_resolver_url",\n      "label": "Custom yt-dlp API URL",\n      "required": false\n    }\n  ],\n  "permissions": [\n    "INTERNET",\n    "FOREGROUND_SERVICE",\n    "MODIFY_AUDIO_SETTINGS"\n  ]\n}`
    },
    entrypoint: {
      lang: 'PROPERTIES',
      code: `# TapStream Music Bot Bytecode Entrypoint\nclass=com.tapbot.core.runner.runtime.MusicBotRuntime\nruntime=native_art\nversion=1.0.0\ntargetSdk=35\nminSdk=26`
    },
    resolver: {
      lang: 'KOTLIN',
      code: `// AudioStreamResolver.kt - Curated 24/7 Internet Radio & yt-dlp API\nval curatedStations = listOf(\n    AudioTrack("radio_lofi", "Lofi Girl Study Beats", "https://stream.zeno.fm/0r0xa792kwzuv"),\n    AudioTrack("radio_synthwave", "Nightwave Plaza 80s", "https://plaza.one/mp3"),\n    AudioTrack("radio_chillhop", "Chillhop Cafe Beats", "https://stream.chillhop.com/live.mp3"),\n    AudioTrack("radio_rock", "Classic Rock Live FM", "https://icecast.walmradio.com:8443/classic")\n)`
    },
    security: {
      lang: 'SHA-256 INTEGRITY',
      code: `Package: tapstream-music-1.0.0.botpkg\nSize: 1,827 bytes\nSHA-256: 178434b2d9ff07199455a3ae04373be2d13bec341e398baa99529c4635550b0a\nEnclave: Android Keystore AES-256-GCM\nVerification: 100% Cryptographically Matched`
    }
  };

  tabs.forEach(tab => {
    tab.addEventListener('click', () => {
      audioFeedback.playClick();
      tabs.forEach(t => {
        t.classList.remove('bg-cyan-500/20', 'text-cyan-400', 'border-cyan-500/40');
        t.classList.add('text-slate-400');
      });
      tab.classList.remove('text-slate-400');
      tab.classList.add('bg-cyan-500/20', 'text-cyan-400', 'border-cyan-500/40');

      const key = tab.getAttribute('data-pkg-tab');
      if (files[key] && codeContent) {
        codeContent.textContent = files[key].code;
        if (codeLang) codeLang.textContent = files[key].lang;
      }
    });
  });
}

// =============================================================================
// 6. INITIALIZATION ON DOM READY
// =============================================================================
document.addEventListener('DOMContentLoaded', () => {
  window.spectrumViz = new SpectrumVisualizer('audio-spectrum-canvas');
  window.botSimulator = new PhoneBotSimulator();
  initScrollytelling();
  initPackageInspector();

  // Quick command buttons on page
  document.querySelectorAll('[data-quick-cmd]').forEach(btn => {
    btn.addEventListener('click', (e) => {
      const cmd = e.currentTarget.getAttribute('data-quick-cmd');
      if (cmd && window.botSimulator) {
        window.botSimulator.submitCommand(cmd);
        // smooth scroll to phone simulator if on mobile
        const sim = document.getElementById('interactive-simulator');
        if (sim && window.innerWidth < 768) {
          sim.scrollIntoView({ behavior: 'smooth' });
        }
      }
    });
  });

  // Sound toggle button
  const soundBtn = document.getElementById('toggle-sound-btn');
  if (soundBtn) {
    soundBtn.addEventListener('click', () => {
      audioFeedback.soundEnabled = !audioFeedback.soundEnabled;
      soundBtn.innerHTML = audioFeedback.soundEnabled 
        ? `<svg class="w-4 h-4 text-cyan-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.536 8.464a5 5 0 010 7.072m2.828-9.9a9 9 0 010 12.728M5.586 15H4a1 1 0 01-1-1v-4a1 1 0 011-1h1.586l4.707-4.707C10.923 3.663 12 4.109 12 5v14c0 .891-1.077 1.337-1.707.707L5.586 15z"/></svg> Sound: ON`
        : `<svg class="w-4 h-4 text-slate-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5.586 15H4a1 1 0 01-1-1v-4a1 1 0 011-1h1.586l4.707-4.707C10.923 3.663 12 4.109 12 5v14c0 .891-1.077 1.337-1.707.707L5.586 15z"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2"/></svg> Sound: OFF`;
      if (audioFeedback.soundEnabled) audioFeedback.playBeep(600, 'sine', 0.08, 0.05);
    });
  }
});
