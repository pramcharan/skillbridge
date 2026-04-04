/**
 * utils.js — Shared helper functions for all SkillBridge pages
 * Load this FIRST before auth.js and api.js.
 */

/* ────────────────────── FORMAT HELPERS ─────────────────────── */

/** Format number as currency: 1500 → "$1,500" */
function formatCurrency(amount) {
  if (!amount && amount !== 0) return 'N/A';
  return '$' + Number(amount).toLocaleString();
}

/** Format ISO date string: "Apr 15, 2026" */
function formatDate(dateStr) {
  if (!dateStr) return 'N/A';
  return new Date(dateStr).toLocaleDateString('en-US', {
    year: 'numeric', month: 'short', day: 'numeric'
  });
}

/** Format relative time: "2 hours ago" */
function timeAgo(dateStr) {
  if (!dateStr) return '';
  const diff  = Date.now() - new Date(dateStr).getTime();
  const mins  = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days  = Math.floor(diff / 86400000);
  if (mins  < 1)  return 'just now';
  if (mins  < 60) return `${mins}m ago`;
  if (hours < 24) return `${hours}h ago`;
  if (days  < 7)  return `${days}d ago`;
  return formatDate(dateStr);
}

/** Truncate text with ellipsis */
function truncateText(text, maxLength = 120) {
  if (!text) return '';
  return text.length > maxLength ? text.slice(0, maxLength) + '…' : text;
}

/** Escape HTML to prevent XSS */
function escHtml(str) {
  if (!str) return '';
  return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
}

/** Debounce — prevents a function from firing too fast */
function debounce(fn, delay = 350) {
  let timer;
  return (...args) => {
    clearTimeout(timer);
    timer = setTimeout(() => fn(...args), delay);
  };
}

/* ────────────────────── BADGE HELPERS ──────────────────────── */

/** Get AI match badge CSS class from score */
function getBadgeClass(score) {
  if (score >= 70) return 'badge badge-success';
  if (score >= 40) return 'badge badge-warning';
  return 'badge badge-danger';
}

/** Get AI badge label from score */
function getBadgeLabel(score) {
  if (score >= 70) return 'Strong Match';
  if (score >= 40) return 'Partial Match';
  return 'Low Match';
}

/** Render star rating HTML */
function renderStars(rating) {
  if (!rating) return '—';
  const full = Math.round(rating);
  return '★'.repeat(full) + '☆'.repeat(5 - full);
}

/* ────────────────────── ANIMATED COUNTER ───────────────────── */

/**
 * Animate a numeric counter from 0 to target value.
 * @param {HTMLElement} el — the element to update
 * @param {number} target — the end value
 * @param {number} duration — animation ms (default 800)
 * @param {string} suffix — optional suffix e.g. '★' or '%'
 */
function animateCounter(el, target, duration = 800, suffix = '') {
  if (!el || isNaN(target)) return;
  const start = performance.now();
  function tick(now) {
    const elapsed = Math.min((now - start) / duration, 1);
    // ease-out cubic
    const ease = 1 - Math.pow(1 - elapsed, 3);
    const current = Math.round(target * ease);
    el.textContent = current + suffix;
    if (elapsed < 1) requestAnimationFrame(tick);
  }
  requestAnimationFrame(tick);
}

/* ────────────────────── TOAST SYSTEM ──────────────────────── */

/**
 * Unified toast notification system.
 * Replaces all per-page showToast() implementations.
 * Usage: Toast.show('Saved!', 'success')
 *        Toast.show('Error occurred', 'error')
 *        Toast.show('Info message', 'info')
 */
const Toast = (() => {
  // Inject keyframes once
  const style = document.createElement('style');
  style.textContent = `
    @keyframes toastIn  { from { opacity:0; transform:translateX(24px); } to { opacity:1; transform:none; } }
    @keyframes toastOut { from { opacity:1; transform:none; } to { opacity:0; transform:translateX(24px); } }
  `;
  document.head.appendChild(style);

  let container = null;

  function ensureContainer() {
    if (!container) {
      container = document.createElement('div');
      container.id = 'toast-container';
      container.style.cssText = `
        position: fixed; bottom: 24px; right: 24px;
        z-index: 9999; display: flex; flex-direction: column;
        gap: 8px; pointer-events: none;
      `;
      document.body.appendChild(container);
    }
  }

  const palette = {
    success: { bg: 'rgba(0,201,167,0.12)',   border: 'rgba(0,201,167,0.25)',   color: '#00C9A7', icon: '✓' },
    error:   { bg: 'rgba(255,107,138,0.12)',  border: 'rgba(255,107,138,0.25)', color: '#FF6B8A', icon: '✕' },
    info:    { bg: 'rgba(123,97,255,0.12)',   border: 'rgba(123,97,255,0.25)',  color: '#7B61FF', icon: 'ℹ' },
    warning: { bg: 'rgba(245,166,35,0.12)',   border: 'rgba(245,166,35,0.25)',  color: '#F5A623', icon: '!' },
  };

  return {
    show(message, type = 'success', duration = 3500) {
      ensureContainer();
      const c = palette[type] || palette.success;
      const el = document.createElement('div');
      el.style.cssText = `
        pointer-events: all;
        padding: 12px 16px;
        border-radius: 12px;
        font-size: 0.875rem;
        font-weight: 600;
        background: ${c.bg};
        border: 1px solid ${c.border};
        color: ${c.color};
        box-shadow: 0 8px 32px rgba(0,0,0,0.4);
        display: flex; align-items: center; gap: 10px;
        animation: toastIn 0.3s cubic-bezier(0.22,1,0.36,1) both;
        font-family: 'DM Sans', sans-serif;
        backdrop-filter: blur(8px);
        max-width: 320px;
      `;
      el.innerHTML = `<span style="font-size:0.9rem;flex-shrink:0;">${c.icon}</span><span>${escHtml(message)}</span>`;
      container.appendChild(el);
      setTimeout(() => {
        el.style.animation = 'toastOut 0.25s ease forwards';
        setTimeout(() => el.remove(), 260);
      }, duration);
    }
  };
})();

// Legacy shim — pages that call showToast() directly still work
function showToast(message, type = 'success') {
  Toast.show(message, type);
}

/* ────────────────────── NOTIFICATION BADGE ─────────────────── */

/**
 * Update a notification badge element.
 * @param {number} count — unread count
 * @param {string} badgeId — element ID (default 'notif-badge')
 * @param {string} titlePrefix — optional document title prefix
 */
function updateNotifBadge(count, badgeId = 'notif-badge', titlePrefix = '') {
  const badge = document.getElementById(badgeId);
  if (!badge) return;
  if (count > 0) {
    badge.style.display = 'block';
    badge.textContent = count > 99 ? '99+' : count;
    if (titlePrefix) document.title = `(${count}) ${titlePrefix}`;
  } else {
    badge.style.display = 'none';
    if (titlePrefix) document.title = titlePrefix;
  }
}

/* ────────────────────── USER AVATAR ────────────────────────── */

/**
 * Set the user avatar element from profile data.
 * Shows image if available, else initials.
 * @param {string} avatarId — element ID (default 'user-avatar')
 * @param {object} profile  — { name, avatarUrl }
 */
function setAvatar(profile, avatarId = 'user-avatar') {
  const el = document.getElementById(avatarId);
  if (!el) return;
  const saved = localStorage.getItem('sb_avatar') || profile.avatarUrl;
  if (saved) {
    el.innerHTML = `<img src="${saved}" alt="Avatar" style="width:100%;height:100%;border-radius:50%;object-fit:cover;">`;
    if (profile.avatarUrl) localStorage.setItem('sb_avatar', profile.avatarUrl);
  } else {
    el.textContent = (profile.name || '?')[0].toUpperCase();
  }
}

/* ────────────────────── PAGINATION ─────────────────────────── */

/** Render pagination controls into a container element. */
function renderPagination(containerId, currentPage, totalPages, onPageClickFn) {
  const el = document.getElementById(containerId);
  if (!el) return;
  if (!totalPages || totalPages <= 1) { el.innerHTML = ''; return; }

  let html = `<div class="pagination">`;
  html += `<button class="pg-btn" onclick="${onPageClickFn}(${currentPage - 1})" ${currentPage === 0 ? 'disabled' : ''}>← Prev</button>`;

  for (let i = 0; i < totalPages; i++) {
    if (totalPages > 7 && Math.abs(i - currentPage) > 2 && i !== 0 && i !== totalPages - 1) {
      if (i === 1 || i === totalPages - 2) {
        html += `<span class="pg-info">…</span>`;
      }
      continue;
    }
    html += `<button class="pg-btn ${i === currentPage ? 'active' : ''}" onclick="${onPageClickFn}(${i})">${i + 1}</button>`;
  }

  html += `<span class="pg-info">Page ${currentPage + 1} of ${totalPages}</span>`;
  html += `<button class="pg-btn" onclick="${onPageClickFn}(${currentPage + 1})" ${currentPage >= totalPages - 1 ? 'disabled' : ''}>Next →</button>`;
  html += `</div>`;
  el.innerHTML = html;
}

/* ────────────────────── SKELETON LOADERS ───────────────────── */

/** Generate N skeleton card placeholder strings */
function skeletonCards(n = 3, extraClass = '') {
  return Array(n).fill(`<div class="skeleton skeleton-card ${extraClass}"></div>`).join('');
}

function skeletonRows(n = 3) {
  return Array(n).fill(`<div class="skeleton skeleton-row" style="margin-bottom:10px;"></div>`).join('');
}

/* ────────────────────── MOBILE NAV ─────────────────────────── */

/** Toggle mobile nav drawer */
function toggleMobileNav() {
  const drawer = document.getElementById('nav-drawer');
  if (drawer) drawer.classList.toggle('open');
}

/** Close mobile nav drawer */
function closeMobileNav() {
  const drawer = document.getElementById('nav-drawer');
  if (drawer) drawer.classList.remove('open');
}

/* ────────────────────── ALERT HELPER ───────────────────────── */

function showAlert(containerId, message, type = 'error') {
  const el = document.getElementById(containerId);
  if (!el) return;
  el.innerHTML = `<div class="alert alert-${type}">${escHtml(message)}</div>`;
  if (type === 'success') {
    setTimeout(() => { if (el) el.innerHTML = ''; }, 4000);
  }
}