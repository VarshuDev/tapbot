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
      precision: { color1: 'rgba(255, 255, 255, 0.32)', color2: 'rgba(161, 161, 170, 0.18)', waves: 3, amp: 40, freq: 0.0035, speed: 0.012 },
      lofi: { color1: 'rgba(228, 228, 231, 0.28)', color2: 'rgba(113, 113, 122, 0.14)', waves: 3, amp: 32, freq: 0.0028, speed: 0.010 },
      synth: { color1: 'rgba(255, 255, 255, 0.38)', color2: 'rgba(161, 161, 170, 0.22)', waves: 4, amp: 58, freq: 0.006, speed: 0.022 },
      ambient: { color1: 'rgba(212, 212, 216, 0.22)', color2: 'rgba(82, 82, 91, 0.12)', waves: 2, amp: 26, freq: 0.002, speed: 0.007 }
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

    const dy = (this.mouse.y / this.height - 0.5) * 24;
    const baseAmp = cfg.amp + dy;

    for (let w = 0; w < cfg.waves; w++) {
      this.ctx.beginPath();
      const wavePhase = this.phase + (w * Math.PI) / 3;
      const waveFreq = cfg.freq * (1 + w * 0.25);
      const waveAmp = baseAmp * (1 - w * 0.2);
      const baseline = this.height * 0.52 + w * 16;

      this.ctx.moveTo(0, baseline);
      for (let x = 0; x <= this.width; x += 6) {
        const y =
          baseline +
          Math.sin(x * waveFreq + wavePhase) * waveAmp +
          Math.sin(x * waveFreq * 1.8 + wavePhase * 1.4) * (waveAmp * 0.3);
        this.ctx.lineTo(x, y);
      }

      const grad = this.ctx.createLinearGradient(0, baseline - 60, this.width, baseline + 60);
      grad.addColorStop(0, cfg.color1);
      grad.addColorStop(1, cfg.color2);

      this.ctx.strokeStyle = grad;
      this.ctx.lineWidth = 1.4;
      this.ctx.shadowBlur = 8;
      this.ctx.shadowColor = 'rgba(255, 255, 255, 0.15)';
      this.ctx.stroke();
    }
  }
}

// ═══════════════════════════════════════════════════════════════════════════
// 3. SPOTLIGHT BORDER HIGHLIGHT (Linear/Vercel Monochromatic)
// ═══════════════════════════════════════════════════════════════════════════
function initSpotlights() {
  const cards = document.querySelectorAll('.spotlight-card');
  if (!cards.length) return;

  window.addEventListener('mousemove', (e) => {
    cards.forEach(card => {
      const rect = card.getBoundingClientRect();
      const x = e.clientX - rect.left;
      const y = e.clientY - rect.top;
      card.style.setProperty('--mouse-x', `${x}px`);
      card.style.setProperty('--mouse-y', `${y}px`);
    });
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
    this.chatFeed = document.getElementById('phone-chat-feed');
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
  }

  initEvents() {
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

  togglePlay() {
    this.isPlaying = !this.isPlaying;
    sfx.playClick(this.isPlaying ? 600 : 380);
    const svgWrap = document.getElementById('player-play-svg');
    if (svgWrap) {
      svgWrap.innerHTML = this.isPlaying
        ? `<rect x="6" y="4" width="4" height="16"/><rect x="14" y="4" width="4" height="16"/>`
        : `<polygon points="5 3 19 12 5 21 5 3"/>`;
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

  executeCommand(rawText, fromDemo = false) {
    const text = rawText.trim();
    if (!text) return;

    if (!fromDemo) sfx.playClick(580);
    this.appendUserMessage(text);

    setTimeout(() => {
      this.resolveCommand(text);
      if (!fromDemo) sfx.playChirp();
    }, 200);
  }

  resolveCommand(text) {
    const parts = text.split(/\s+/);
    const cmd = parts[0].toLowerCase();
    const arg = parts.slice(1).join(' ').trim();

    if (cmd === '/play') {
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
        `STATUS: In-chat track delivered.`
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
        `BUFFER: 0ms (In-Memory RAM)`
      );
    } else if (cmd === '/nowplaying' || cmd === '/np') {
      this.appendBotMessage(
        `STATUS: <b>${this.currentTrack.title}</b><br/>` +
        `STREAM: <i>${this.currentTrack.artist}</i><br/>` +
        `ROUTE: <b>${this.currentMode}</b> (Vol: ${this.currentVolume}%)`
      );
    } else if (cmd === '/volume') {
      const val = parseInt(arg, 10);
      if (!isNaN(val)) {
        this.currentVolume = Math.max(0, Math.min(100, val));
        if (this.volDisplay) this.volDisplay.textContent = `Vol: ${this.currentVolume}%`;
        this.appendBotMessage(`VOLUME: Media volume adjusted to <b>${this.currentVolume}%</b>`);
      } else {
        this.appendBotMessage(`VOLUME: Current level is <b>${this.currentVolume}%</b> (use <code>/volume 0-100</code>)`);
      }
    } else if (cmd === '/mode') {
      this.setMode(arg.toUpperCase());
    } else if (cmd === '/status') {
      this.appendBotMessage(
        `TELEMETRY // ART PROCESS PID 8412<br/>` +
        `BATTERY: <b>88% [CHARGING_AC]</b><br/>` +
        `HEAP RAM: <b>14.2 MB / 8 GB (0.17%)</b><br/>` +
        `STORAGE: <b>0 Bytes/hr [RAM_RING_BUFFER]</b><br/>` +
        `UPTIME: <b>42d 18h 34m</b><br/>` +
        `DAEMON: <b>ACTIVE [FOREGROUND_SERVICE]</b>`
      );
    } else {
      this.appendBotMessage(`INPUT: <code>${text}</code><br/>Commands: <code>/play</code>, <code>/radio</code>, <code>/status</code>, <code>/nowplaying</code>`);
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
// 7. TEXT SPLITTING & GSAP SCROLL ANIMATIONS
// ═══════════════════════════════════════════════════════════════════════════
function splitIntoChars(el) {
  function traverse(node, container) {
    if (node.nodeType === Node.TEXT_NODE) {
      const words = node.textContent.split(/(\s+)/);
      for (const token of words) {
        if (!token) continue;
        if (/^\s+$/.test(token)) {
          container.appendChild(document.createTextNode(' '));
        } else {
          const wordSpan = document.createElement('span');
          wordSpan.className = 'split-word';
          wordSpan.style.display = 'inline-block';
          wordSpan.style.whiteSpace = 'nowrap';
          for (const ch of token) {
            const span = document.createElement('span');
            span.className = 'char';
            span.textContent = ch;
            wordSpan.appendChild(span);
          }
          container.appendChild(wordSpan);
        }
      }
    } else if (node.nodeType === Node.ELEMENT_NODE) {
      const wrapper = node.cloneNode(false);
      for (const child of node.childNodes) {
        traverse(child, wrapper);
      }
      container.appendChild(wrapper);
    }
  }

  const frag = document.createDocumentFragment();
  for (const child of Array.from(el.childNodes)) {
    traverse(child, frag);
  }
  el.innerHTML = '';
  el.appendChild(frag);
  return el.querySelectorAll('.char');
}

function splitIntoWords(el) {
  const text = el.textContent;
  const words = text.split(/\s+/).filter(Boolean);
  el.innerHTML = '';
  const frag = document.createDocumentFragment();
  words.forEach((w, i) => {
    const wrap = document.createElement('span');
    wrap.className = 'word-wrap';
    const inner = document.createElement('span');
    inner.className = 'word-inner';
    inner.textContent = w;
    wrap.appendChild(inner);
    frag.appendChild(wrap);
    if (i < words.length - 1) frag.appendChild(document.createTextNode(' '));
  });
  el.appendChild(frag);
  return el.querySelectorAll('.word-inner');
}

function initGSAPAnimations() {
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
        scrub: 0.2,
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

  document.querySelectorAll('[data-reveal="chars"]').forEach(el => {
    const chars = splitIntoChars(el);
    gsap.from(chars, {
      opacity: 0,
      y: 35,
      rotateX: -25,
      duration: 0.8,
      stagger: 0.018,
      ease: 'expo.out',
      scrollTrigger: {
        trigger: el,
        start: 'top 85%',
        toggleActions: 'play none none none',
      }
    });
  });

  document.querySelectorAll('[data-reveal="words"]').forEach(el => {
    const words = splitIntoWords(el);
    gsap.from(words, {
      opacity: 0,
      y: 18,
      duration: 0.7,
      stagger: 0.02,
      ease: 'expo.out',
      scrollTrigger: {
        trigger: el,
        start: 'top 85%',
        toggleActions: 'play none none none',
      }
    });
  });

  document.querySelectorAll('[data-reveal="fade"]').forEach(el => {
    gsap.from(el, {
      opacity: 0,
      y: 22,
      duration: 0.9,
      ease: 'expo.out',
      scrollTrigger: {
        trigger: el,
        start: 'top 88%',
        toggleActions: 'play none none none',
      }
    });
  });

  document.querySelectorAll('[data-reveal="slide-up"]').forEach((el, i) => {
    gsap.from(el, {
      opacity: 0,
      y: 45,
      duration: 0.85,
      delay: (i % 3) * 0.08,
      ease: 'expo.out',
      scrollTrigger: {
        trigger: el,
        start: 'top 90%',
        toggleActions: 'play none none none',
      }
    });
  });

  const track = document.querySelector('.how-track');
  if (track) {
    const totalDist = track.scrollWidth - window.innerWidth + 240;
    gsap.to(track, {
      x: () => -totalDist,
      ease: 'none',
      scrollTrigger: {
        trigger: '#how',
        start: 'top top',
        end: () => `+=${totalDist}`,
        scrub: 1,
        pin: true,
        anticipatePin: 1,
        invalidateOnRefresh: true,
      }
    });
  }

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
          duration: 1.8,
          ease: 'expo.out',
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
// 9. LENIS SMOOTH SCROLL
// ═══════════════════════════════════════════════════════════════════════════
function initLenis() {
  if (prefersReducedMotion) return;
  try {
    const lenis = new Lenis({
      duration: 1.2,
      easing: (t) => Math.min(1, 1.001 - Math.pow(2, -10 * t)),
      smoothWheel: true,
    });
    lenis.on('scroll', ScrollTrigger.update);
    gsap.ticker.add((time) => lenis.raf(time * 1000));
    gsap.ticker.lagSmoothing(0);
  } catch (_) {}
}

// ═══════════════════════════════════════════════════════════════════════════
// 10. BOOTSTRAP ON LOAD
// ═══════════════════════════════════════════════════════════════════════════
document.addEventListener('DOMContentLoaded', () => {
  initLenis();
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
