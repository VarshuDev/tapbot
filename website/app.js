/**
 * TapBot — app.js
 * Real GSAP scroll animations + interactive terminal simulator
 */

// ════════════════════════════════════════════════════════════════
// 1. GSAP SCROLL ANIMATIONS
// ════════════════════════════════════════════════════════════════
function initAnimations() {
  gsap.registerPlugin(ScrollTrigger);

  const prefersReduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  if (prefersReduced) return; // respect accessibility

  // — Hero entrance —
  const heroTl = gsap.timeline({ defaults: { ease: 'expo.out', duration: 1 } });
  heroTl
    .from('.hero-eyebrow', { opacity: 0, y: 20 }, 0.1)
    .from('.hero-line', { opacity: 0, y: 40, stagger: 0.12 }, 0.2)
    .from('.hero-sub', { opacity: 0, y: 20 }, 0.5)
    .from('.hero-ctas', { opacity: 0, y: 20 }, 0.6)
    .from('.stats-ribbon', { opacity: 0, y: 30 }, 0.75);

  // — "How it works" horizontal scroll pin —
  const track = document.querySelector('.how-track');
  const cards = document.querySelectorAll('.how-card');
  if (track && cards.length) {
    const totalScroll = track.scrollWidth - window.innerWidth + 200;
    gsap.to(track, {
      x: () => -totalScroll,
      ease: 'none',
      scrollTrigger: {
        trigger: '#how',
        start: 'top top',
        end: () => `+=${totalScroll}`,
        scrub: 1,
        pin: true,
        anticipatePin: 1,
        invalidateOnRefresh: true,
      }
    });

    // Stagger-reveal each card as it enters
    cards.forEach((card, i) => {
      gsap.from(card, {
        opacity: 0,
        y: 40,
        duration: 0.8,
        ease: 'expo.out',
        scrollTrigger: {
          trigger: card,
          containerAnimation: gsap.getById && undefined, // not needed here
          start: 'top 85%',
          toggleActions: 'play none none none',
        }
      });
    });
  }

  // — Bento cards reveal —
  document.querySelectorAll('[data-reveal]').forEach((el, i) => {
    gsap.from(el, {
      opacity: 0,
      y: 50,
      duration: 0.9,
      ease: 'expo.out',
      scrollTrigger: {
        trigger: el,
        start: 'top 88%',
        toggleActions: 'play none none none',
      }
    });
  });

  // — Terminal section slide-in —
  gsap.from('.terminal-text-col', {
    opacity: 0, x: -40, duration: 1, ease: 'expo.out',
    scrollTrigger: { trigger: '#terminal', start: 'top 80%' }
  });
  gsap.from('.term', {
    opacity: 0, x: 40, duration: 1, ease: 'expo.out',
    scrollTrigger: { trigger: '#terminal', start: 'top 80%' }
  });

  // — Download section scale entrance —
  gsap.from('.dl-h2', {
    opacity: 0, scale: .92, duration: 1, ease: 'expo.out',
    scrollTrigger: { trigger: '#download', start: 'top 80%' }
  });
}


// ════════════════════════════════════════════════════════════════
// 2. TERMINAL SIMULATOR
// ════════════════════════════════════════════════════════════════
class TerminalSimulator {
  constructor() {
    this.body = document.getElementById('term-body');
    this.input = document.getElementById('term-input');
    this.volume = 80;
    this.mode = 'BOTH';
    this.track = { title: 'Lofi Girl Study Beats', artist: 'Lofi Girl 24/7' };
    this.initEvents();
  }

  initEvents() {
    this.input.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') {
        e.preventDefault();
        const val = this.input.value.trim();
        if (val) {
          this.exec(val);
          this.input.value = '';
        }
      }
    });
  }

  exec(raw) {
    this.addLine(raw, 'user');
    const parts = raw.split(/\s+/);
    const cmd = parts[0].toLowerCase();
    const args = parts.slice(1).join(' ').trim();
    let r = '';

    if (cmd === '/play') {
      const title = args ? args.charAt(0).toUpperCase() + args.slice(1) : 'Lofi Beats';
      this.track = { title, artist: 'TapStream Audio' };
      r = `▶ Playing: ${title}\n  Artist: TapStream Audio\n  Mode: ${this.mode} · Vol: ${this.volume}%`;
    } else if (cmd === '/radio') {
      const genre = args.toLowerCase() || 'synthwave';
      const map = {
        synthwave: ['Nightwave Plaza 80s', 'Plaza FM'],
        lofi: ['Lofi Girl Study Beats', 'Lofi Girl 24/7'],
        rock: ['Classic Rock Live FM', 'Planet Rock'],
        chillhop: ['Chillhop Cafe Beats', 'Chillhop Music'],
        jazz: ['Jazz Lounge & Bossa', 'Jazz Radio'],
        space: ['Deep Space Drone Zone', 'SomaFM'],
      };
      const s = map[genre] || map.synthwave;
      this.track = { title: s[0], artist: s[1] };
      r = `📻 Radio: ${s[0]}\n   Station: ${s[1]} · 320kbps · LIVE`;
    } else if (cmd === '/nowplaying' || cmd === '/np') {
      r = `♫ ${this.track.title}\n  ${this.track.artist}\n  ▶ ━━━━●──────── · ${this.mode} · ${this.volume}%`;
    } else if (cmd === '/volume') {
      const v = parseInt(args, 10);
      if (!isNaN(v)) { this.volume = Math.max(0, Math.min(100, v)); }
      r = `Volume: ${this.volume}%`;
    } else if (cmd === '/mode') {
      const m = args.toUpperCase();
      if (['SPEAKER','CHAT','BOTH'].includes(m)) { this.mode = m; }
      r = `Playback mode: ${this.mode}`;
    } else if (cmd === '/status') {
      r = `Battery: 88% (Charging)\nRAM: 14.2 MB / 8 GB (0.17%)\nUptime: 42d 18h\nFlash writes: 0 B/hr\nRuntime: Android ART`;
    } else {
      r = `Unknown command. Try /play, /radio, /status, /nowplaying, /volume, /mode`;
    }

    setTimeout(() => this.addLine(r, 'bot'), 120);
  }

  addLine(text, type) {
    const div = document.createElement('div');
    div.className = `term-line term-${type}`;
    div.textContent = text;
    this.body.appendChild(div);
    this.body.scrollTop = this.body.scrollHeight;
  }
}


// ════════════════════════════════════════════════════════════════
// 3. INIT
// ════════════════════════════════════════════════════════════════
document.addEventListener('DOMContentLoaded', () => {
  initAnimations();
  const sim = new TerminalSimulator();

  // chip buttons
  document.querySelectorAll('.chip[data-cmd]').forEach(btn => {
    btn.addEventListener('click', () => {
      sim.exec(btn.dataset.cmd);
      // scroll terminal into view on mobile
      document.querySelector('.term').scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    });
  });

  // nav background on scroll
  const nav = document.getElementById('nav');
  window.addEventListener('scroll', () => {
    nav.style.borderBottomColor = window.scrollY > 20
      ? 'rgba(255,255,255,.08)'
      : 'transparent';
  }, { passive: true });
});
