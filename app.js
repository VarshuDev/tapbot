/**
 * TapBot & TapStream — Ultra-Craft Interactive Motion Engine
 * - Interactive Audio Waveform Canvas
 * - Spotlight Cursor Borders (Linear/Vercel effect)
 * - 3D Perspective Tilt on Hardware/Phone & Bento Cards
 * - Procedural Web Audio FX
 * - Telegram Bot Simulator with Live Audio Player & Scrubber
 * - Lenis Smooth Scroll + GSAP ScrollTrigger
 */

// ═══════════════════════════════════════════════════════════════════════════
// 0. CONFIG & REDUCED MOTION CHECK
// ═══════════════════════════════════════════════════════════════════════════
const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

// ═══════════════════════════════════════════════════════════════════════════
// 1. PROCEDURAL WEB AUDIO SFX (No external assets required)
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

  playClick(freq = 600, duration = 0.035) {
    if (!this.enabled || prefersReducedMotion) return;
    try {
      this.init();
      if (!this.ctx) return;
      if (this.ctx.state === 'suspended') this.ctx.resume();
      
      const osc = this.ctx.createOscillator();
      const gain = this.ctx.createGain();
      
      osc.type = 'sine';
      osc.frequency.setValueAtTime(freq, this.ctx.currentTime);
      osc.frequency.exponentialRampToValueAtTime(freq * 0.4, this.ctx.currentTime + duration);
      
      gain.gain.setValueAtTime(0.04, this.ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.0001, this.ctx.currentTime + duration);
      
      osc.connect(gain);
      gain.connect(this.ctx.destination);
      
      osc.start();
      osc.stop(this.ctx.currentTime + duration);
    } catch (_) {}
  }

  playChirp(type = 'success') {
    if (!this.enabled || prefersReducedMotion) return;
    try {
      this.init();
      if (!this.ctx) return;
      if (this.ctx.state === 'suspended') this.ctx.resume();

      const f1 = type === 'success' ? 520 : 380;
      const f2 = type === 'success' ? 880 : 240;

      const osc = this.ctx.createOscillator();
      const gain = this.ctx.createGain();

      osc.type = 'triangle';
      osc.frequency.setValueAtTime(f1, this.ctx.currentTime);
      osc.frequency.exponentialRampToValueAtTime(f2, this.ctx.currentTime + 0.08);

      gain.gain.setValueAtTime(0.05, this.ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.0001, this.ctx.currentTime + 0.09);

      osc.connect(gain);
      gain.connect(this.ctx.destination);

      osc.start();
      osc.stop(this.ctx.currentTime + 0.09);
    } catch (_) {}
  }
}

const sfx = new TactileAudio();

// ═══════════════════════════════════════════════════════════════════════════
// 2. INTERACTIVE AUDIO WAVEFORM CANVAS (Hero & Ambient Background)
// ═══════════════════════════════════════════════════════════════════════════
class AudioVisualizerCanvas {
  constructor() {
    this.canvas = document.getElementById('bg-visualizer');
    if (!this.canvas) return;
    this.ctx = this.canvas.getContext('2d');
    this.width = (this.canvas.width = window.innerWidth);
    this.height = (this.canvas.height = window.innerHeight);

    this.mouse = { x: this.width / 2, y: this.height / 2 };
    this.phase = 0;
    this.genre = 'lofi';
    this.amplitudeMultiplier = 1;
    this.speed = 0.015;

    this.genres = {
      lofi: { color1: 'rgba(56, 189, 248, 0.35)', color2: 'rgba(16, 185, 129, 0.25)', waves: 3, amp: 45, freq: 0.004, speed: 0.014 },
      synthwave: { color1: 'rgba(236, 72, 153, 0.4)', color2: 'rgba(129, 140, 248, 0.3)', waves: 4, amp: 70, freq: 0.007, speed: 0.026 },
      rock: { color1: 'rgba(245, 158, 11, 0.4)', color2: 'rgba(239, 68, 68, 0.3)', waves: 5, amp: 85, freq: 0.009, speed: 0.035 },
      ambient: { color1: 'rgba(99, 102, 241, 0.3)', color2: 'rgba(16, 185, 129, 0.2)', waves: 2, amp: 30, freq: 0.002, speed: 0.008 }
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

  setGenre(genreName) {
    if (this.genres[genreName]) {
      this.genre = genreName;
      sfx.playClick(640);
      
      const trackLabel = document.getElementById('vis-track-name');
      if (trackLabel) {
        trackLabel.innerHTML = `AUDIO CANVAS: ${genreName.toUpperCase()} PRESET &middot; 320 KBPS LIVE`;
      }
    }
  }

  animate() {
    requestAnimationFrame(() => this.animate());
    this.ctx.clearRect(0, 0, this.width, this.height);

    const cfg = this.genres[this.genre] || this.genres.lofi;
    this.phase += cfg.speed;

    // React slightly to cursor proximity
    const dy = (this.mouse.y / this.height - 0.5) * 30;
    const baseAmp = cfg.amp * this.amplitudeMultiplier + dy;

    for (let w = 0; w < cfg.waves; w++) {
      this.ctx.beginPath();
      const wavePhase = this.phase + (w * Math.PI) / 3;
      const waveFreq = cfg.freq * (1 + w * 0.25);
      const waveAmp = baseAmp * (1 - w * 0.18);
      const baseline = this.height * 0.52 + w * 18;

      this.ctx.moveTo(0, baseline);
      for (let x = 0; x <= this.width; x += 6) {
        // Multi-frequency sinusoidal equation
        const y =
          baseline +
          Math.sin(x * waveFreq + wavePhase) * waveAmp +
          Math.sin(x * waveFreq * 2 + wavePhase * 1.5) * (waveAmp * 0.35);
        this.ctx.lineTo(x, y);
      }

      const grad = this.ctx.createLinearGradient(0, baseline - 80, this.width, baseline + 80);
      grad.addColorStop(0, cfg.color1);
      grad.addColorStop(1, cfg.color2);

      this.ctx.strokeStyle = grad;
      this.ctx.lineWidth = 1.8 + (cfg.waves - w) * 0.5;
      this.ctx.shadowBlur = 12;
      this.ctx.shadowColor = cfg.color1;
      this.ctx.stroke();
    }
  }
}

// ═══════════════════════════════════════════════════════════════════════════
// 3. SPOTLIGHT BORDER HIGHLIGHT (Linear / Vercel mouse cursor tracker)
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
      
      const maxAngle = el.classList.contains('phone-3d-wrap') ? 12 : 6;

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
        duration: 0.7,
        ease: 'elastic.out(1, 0.45)',
      });
    });
  });
}

// ═══════════════════════════════════════════════════════════════════════════
// 5. LIVE SMARTPHONE SIMULATOR & TELEGRAM BOT ENGINE
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
    this.playIcon = document.getElementById('player-play-icon');
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
      genre: 'lofi'
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

    // Audio Output Mode switcher buttons
    document.querySelectorAll('.mode-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const mode = e.currentTarget.getAttribute('data-mode');
        document.querySelectorAll('.mode-btn').forEach(b => b.classList.remove('active'));
        e.currentTarget.classList.add('active');
        this.setMode(mode);
      });
    });

    // Radio quick stations inside Bento Card
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
    sfx.playClick(this.isPlaying ? 700 : 420);
    if (this.playIcon) {
      this.playIcon.textContent = this.isPlaying ? '❚❚' : '▶';
    }
    const eqBars = document.querySelectorAll('.p-eq-bar, .eq-bar');
    eqBars.forEach(bar => {
      bar.style.animationPlayState = this.isPlaying ? 'running' : 'paused';
      bar.style.opacity = this.isPlaying ? '1' : '0.25';
    });
  }

  setMode(mode) {
    sfx.playClick(580);
    let label = 'BOTH (SPEAKER + CHAT)';
    if (mode === 'SPEAKER') label = 'PHONE SPEAKER ONLY';
    if (mode === 'CHAT') label = 'TELEGRAM CHAT ONLY';
    this.currentMode = label;
    if (this.modeBadge) this.modeBadge.textContent = label;
    this.appendBotMessage(`🎛 Playback routing updated to: <b>${label}</b>`);
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

    if (!fromDemo) sfx.playClick(620);
    this.appendUserMessage(text);

    // Typing delay simulation
    setTimeout(() => {
      this.resolveCommand(text);
      if (!fromDemo) sfx.playChirp('success');
    }, 220);
  }

  resolveCommand(text) {
    const parts = text.split(/\s+/);
    const cmd = parts[0].toLowerCase();
    const arg = parts.slice(1).join(' ').trim();

    if (cmd === '/play') {
      const songName = arg ? arg.charAt(0).toUpperCase() + arg.slice(1) : 'Lofi Chill Beats';
      this.currentTrack = {
        title: songName,
        artist: 'TapStream Cloud &middot; 320kbps',
        genre: 'lofi'
      };
      this.updatePlayerUI();
      if (this.visualizer) this.visualizer.setGenre('lofi');

      this.appendBotMessage(
        `🎧 <b>Now Streaming: ${songName}</b><br/>` +
        `📦 Resolved via yt-dlp API (320 kbps)<br/>` +
        `🔊 Output: <b>${this.currentMode}</b><br/>` +
        `🎵 Audio player attached to Telegram chat.`
      );
    } else if (cmd === '/radio') {
      const genre = arg.toLowerCase() || 'synthwave';
      const radioMap = {
        lofi: { title: 'Lofi Girl Study Beats', artist: 'Lofi Girl 24/7', genre: 'lofi' },
        synthwave: { title: 'Nightwave Plaza 80s Cyber', artist: 'Plaza FM Live', genre: 'synthwave' },
        rock: { title: 'Classic Rock Live FM', artist: 'Planet Rock', genre: 'rock' },
        jazz: { title: 'Jazz Lounge & Bossa Beats', artist: 'Jazz Radio HQ', genre: 'ambient' },
        space: { title: 'Deep Space Ambient Drone', artist: 'SomaFM Zero Buffer', genre: 'ambient' }
      };

      const match = radioMap[genre] || radioMap.synthwave;
      this.currentTrack = match;
      this.updatePlayerUI();
      if (this.visualizer) this.visualizer.setGenre(match.genre);

      this.appendBotMessage(
        `📻 <b>Tuned to 24/7 Internet Radio:</b><br/>` +
        `▶ <b>${match.title}</b><br/>` +
        `🎙 <i>${match.artist}</i> &middot; 320kbps<br/>` +
        `🔴 Zero-buffer stream active on native Android ART.`
      );
    } else if (cmd === '/nowplaying' || cmd === '/np') {
      this.appendBotMessage(
        `🎶 <b>Currently Streaming:</b><br/>` +
        `<b>${this.currentTrack.title}</b><br/>` +
        `<i>${this.currentTrack.artist}</i><br/>` +
        `<code>▶ 01:42 ━━━━●────── 03:45</code><br/>` +
        `🔊 Route: <b>${this.currentMode}</b> (${this.currentVolume}%)`
      );
    } else if (cmd === '/volume') {
      const val = parseInt(arg, 10);
      if (!isNaN(val)) {
        this.currentVolume = Math.max(0, Math.min(100, val));
        if (this.volDisplay) this.volDisplay.textContent = `Vol: ${this.currentVolume}%`;
        this.appendBotMessage(`🔊 Media stream volume set to <b>${this.currentVolume}%</b>`);
      } else {
        this.appendBotMessage(`🔊 Current volume: <b>${this.currentVolume}%</b> (use <code>/volume 0-100</code>)`);
      }
    } else if (cmd === '/mode') {
      this.setMode(arg.toUpperCase());
    } else if (cmd === '/status') {
      this.appendBotMessage(
        `📊 <b>Android ART Telemetry:</b><br/>` +
        `🔋 Battery: <b>88% (Charging AC)</b><br/>` +
        `🧠 Heap Memory: <b>14.2 MB / 8 GB (0.17%)</b><br/>` +
        `🛡 Flash Writes: <b>0 Bytes/hr (RAM Buffer)</b><br/>` +
        `⏱ Service Uptime: <b>42d 18h 34m</b><br/>` +
        `⚡ Process: <b>PID 8412 [FOREGROUND_SERVICE]</b>`
      );
    } else {
      this.appendBotMessage(`🎵 Received: <code>${text}</code><br/>Try <code>/play lofi</code>, <code>/radio synthwave</code>, or <code>/status</code>.`);
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

  // Hover states on clickable targets
  document.querySelectorAll('a, button, input, [data-magnetic]').forEach(el => {
    el.addEventListener('mouseenter', () => document.body.classList.add('cursor-hover'));
    el.addEventListener('mouseleave', () => document.body.classList.remove('cursor-hover'));
  });

  // Magnetic button pull
  document.querySelectorAll('[data-magnetic]').forEach(el => {
    el.addEventListener('mousemove', (e) => {
      const rect = el.getBoundingClientRect();
      const dx = e.clientX - (rect.left + rect.width / 2);
      const dy = e.clientY - (rect.top + rect.height / 2);
      gsap.to(el, { x: dx * 0.28, y: dy * 0.28, duration: 0.25, ease: 'power2.out' });
    });
    el.addEventListener('mouseleave', () => {
      gsap.to(el, { x: 0, y: 0, duration: 0.55, ease: 'elastic.out(1, 0.45)' });
    });
  });
}

// ═══════════════════════════════════════════════════════════════════════════
// 7. TEXT SPLITTING UTILITIES & GSAP SCROLL ANIMATIONS
// ═══════════════════════════════════════════════════════════════════════════
function splitIntoChars(el) {
  // Split words first, then characters inside words to prevent mid-word line breaks
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

  // Scroll Progress Bar
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

  // Nav border toggle on scroll
  ScrollTrigger.create({
    start: 24,
    onUpdate: (self) => {
      const nav = document.getElementById('nav');
      if (nav) nav.classList.toggle('scrolled', self.progress > 0);
    }
  });

  if (prefersReducedMotion) return;

  // Kinetic Hero Character Reveal
  document.querySelectorAll('[data-reveal="chars"]').forEach(el => {
    const chars = splitIntoChars(el);

    gsap.from(chars, {
      opacity: 0,
      y: 40,
      rotateX: -30,
      duration: 0.8,
      stagger: 0.02,
      ease: 'expo.out',
      scrollTrigger: {
        trigger: el,
        start: 'top 85%',
        toggleActions: 'play none none none',
      }
    });
  });

  // Word-by-word reveal
  document.querySelectorAll('[data-reveal="words"]').forEach(el => {
    const words = splitIntoWords(el);
    gsap.from(words, {
      opacity: 0,
      y: 20,
      duration: 0.7,
      stagger: 0.025,
      ease: 'expo.out',
      scrollTrigger: {
        trigger: el,
        start: 'top 85%',
        toggleActions: 'play none none none',
      }
    });
  });

  // Fade reveals
  document.querySelectorAll('[data-reveal="fade"]').forEach(el => {
    gsap.from(el, {
      opacity: 0,
      y: 24,
      duration: 0.9,
      ease: 'expo.out',
      scrollTrigger: {
        trigger: el,
        start: 'top 88%',
        toggleActions: 'play none none none',
      }
    });
  });

  // Slide-up card reveals
  document.querySelectorAll('[data-reveal="slide-up"]').forEach((el, i) => {
    gsap.from(el, {
      opacity: 0,
      y: 50,
      duration: 0.85,
      delay: (i % 3) * 0.1,
      ease: 'expo.out',
      scrollTrigger: {
        trigger: el,
        start: 'top 90%',
        toggleActions: 'play none none none',
      }
    });
  });

  // Horizontal Pinned Timeline (#how)
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

  // Animated Numbers Counter
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
  const visualizer = new AudioVisualizerCanvas();
  const sim = new PhoneBotSimulator(visualizer);

  initSpotlights();
  init3DTilt();
  initMagneticCursor();
  initGSAPAnimations();
  initUptimeTicker();

  // Waveform Visualizer controller preset buttons
  document.querySelectorAll('.vis-btn').forEach(btn => {
    btn.addEventListener('click', (e) => {
      document.querySelectorAll('.vis-btn').forEach(b => b.classList.remove('active'));
      e.currentTarget.classList.add('active');
      const g = e.currentTarget.getAttribute('data-genre');
      if (visualizer) visualizer.setGenre(g);
    });
  });

  // Sound toggle button in header
  const soundBtn = document.getElementById('sound-toggle-btn');
  const soundLabel = document.getElementById('sound-label');
  if (soundBtn && soundLabel) {
    soundBtn.addEventListener('click', () => {
      sfx.enabled = !sfx.enabled;
      soundLabel.textContent = sfx.enabled ? 'Audio FX: ON' : 'Audio FX: OFF';
      soundBtn.querySelector('.sound-dot').style.background = sfx.enabled ? '#10b981' : '#64748b';
      if (sfx.enabled) sfx.playClick(600);
    });
  }

  // Interactive Command chips in playground
  document.querySelectorAll('.cmd-chip').forEach(chip => {
    chip.addEventListener('click', (e) => {
      const cmd = e.currentTarget.getAttribute('data-cmd');
      if (cmd && sim) {
        sim.executeCommand(cmd);
      }
    });
  });
});
