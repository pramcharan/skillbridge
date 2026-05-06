/**
 * utils.js — Shared helper functions for all SkillBridge pages
 * Load this FIRST before auth.js and api.js.
 */

/* ────────────────────── FORMAT HELPERS ─────────────────────── */

/** Format number as currency: 1500 → "₹1,500" */
function formatCurrency(amount) {
  if (!amount && amount !== 0) return 'N/A';
  return '₹' + Number(amount).toLocaleString('en-IN');
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

/* ────────────────────── MODAL SYSTEM ──────────────────────── */

/**
 * Unified Modal notification system.
 * Replaces native alert(), confirm(), prompt().
 */
const Modal = (() => {
  const style = document.createElement('style');
  style.textContent = `
    .sb-modal-overlay {
      position: fixed; inset: 0;
      background: rgba(15,23,42,0.6); backdrop-filter: blur(4px);
      z-index: 10000; display: flex; align-items: center; justify-content: center;
      opacity: 0; transition: opacity 0.2s ease;
    }
    .sb-modal-box {
      background: var(--bg-surface, var(--card, #fff));
      border-radius: 16px; padding: 24px;
      width: 90%; max-width: 400px;
      box-shadow: 0 20px 40px rgba(0,0,0,0.2);
      transform: scale(0.95) translateY(10px);
      transition: all 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.275);
      border: 1px solid var(--border-color, var(--border2, #e2e8f0));
    }
    .sb-modal-overlay.open { opacity: 1; }
    .sb-modal-overlay.open .sb-modal-box { transform: scale(1) translateY(0); }
    .sb-modal-title { font-size: 1.25rem; font-weight: 700; color: var(--text, #1e293b); margin-bottom: 8px; font-family: 'Syne', 'DM Sans', sans-serif; }
    .sb-modal-msg { font-size: 0.95rem; color: var(--text2, #64748b); margin-bottom: 20px; line-height: 1.5; font-family: 'DM Sans', sans-serif; }
    .sb-modal-input {
        width: 100%; padding: 10px 12px; border: 1px solid var(--border2, #e2e8f0);
        border-radius: 8px; margin-bottom: 20px; font-size: 0.95rem;
        background: var(--bg2, #f8fafc); color: var(--text, #1e293b);
        outline: none; transition: border-color 0.2s; font-family: 'DM Sans', sans-serif;
    }
    .sb-modal-input:focus { border-color: var(--teal, #4f46e5); box-shadow: 0 0 0 3px rgba(0,201,167,0.1); }
    .sb-modal-actions { display: flex; justify-content: flex-end; gap: 12px; }
    .sb-btn { padding: 8px 16px; border-radius: 8px; font-weight: 600; font-size: 0.9rem; cursor: pointer; border: none; transition: all 0.2s; font-family: 'DM Sans', sans-serif;}
    .sb-btn-cancel { background: transparent; border: 1px solid var(--border2, #e2e8f0); color: var(--text2, #475569); }
    .sb-btn-cancel:hover { background: var(--bg2, #f1f5f9); color: var(--text, #000); }
    .sb-btn-confirm { background: var(--teal, #4f46e5); color: #000; }
    .sb-btn-confirm:hover { filter: brightness(1.1); transform: translateY(-1px); }
    .sb-btn-danger { background: transparent; border: 1px solid var(--rose, #ef4444); color: var(--rose, #ef4444); }
    .sb-btn-danger:hover { background: rgba(255,107,138,0.1); transform: translateY(-1px); }
  `;
  document.head.appendChild(style);

  function createOverlay() {
    const overlay = document.createElement('div');
    overlay.className = 'sb-modal-overlay';
    document.body.appendChild(overlay);
    return overlay;
  }

  function closeAndRemove(overlay) {
    overlay.classList.remove('open');
    setTimeout(() => overlay.remove(), 200);
  }

  return {
    alert(title, message) {
      return new Promise(resolve => {
        const overlay = createOverlay();
        overlay.innerHTML = `
          <div class="sb-modal-box">
            <div class="sb-modal-title">${escHtml(title)}</div>
            <div class="sb-modal-msg">${escHtml(message)}</div>
            <div class="sb-modal-actions">
              <button class="sb-btn sb-btn-confirm" id="sb-modal-ok">OK</button>
            </div>
          </div>
        `;
        requestAnimationFrame(() => overlay.classList.add('open'));
        
        document.getElementById('sb-modal-ok').onclick = () => {
          closeAndRemove(overlay);
          resolve();
        };
      });
    },

    confirm(title, message, isDanger = false, confirmText = 'Confirm') {
      return new Promise(resolve => {
        const overlay = createOverlay();
        const confirmClass = isDanger ? 'sb-btn-danger' : 'sb-btn-confirm';
        overlay.innerHTML = `
          <div class="sb-modal-box">
            <div class="sb-modal-title">${escHtml(title)}</div>
            <div class="sb-modal-msg">${escHtml(message)}</div>
            <div class="sb-modal-actions">
              <button class="sb-btn sb-btn-cancel" id="sb-modal-cancel">Cancel</button>
              <button class="sb-btn ${confirmClass}" id="sb-modal-ok">${escHtml(confirmText)}</button>
            </div>
          </div>
        `;
        requestAnimationFrame(() => overlay.classList.add('open'));

        document.getElementById('sb-modal-cancel').onclick = () => {
          closeAndRemove(overlay);
          resolve(false);
        };
        document.getElementById('sb-modal-ok').onclick = () => {
          closeAndRemove(overlay);
          resolve(true);
        };
      });
    },

    prompt(title, message, defaultValue = '') {
      return new Promise(resolve => {
        const overlay = createOverlay();
        overlay.innerHTML = `
          <div class="sb-modal-box">
            <div class="sb-modal-title">${escHtml(title)}</div>
            <div class="sb-modal-msg">${escHtml(message)}</div>
            <input type="text" class="sb-modal-input" id="sb-modal-input" value="${escHtml(defaultValue)}" />
            <div class="sb-modal-actions">
              <button class="sb-btn sb-btn-cancel" id="sb-modal-cancel">Cancel</button>
              <button class="sb-btn sb-btn-confirm" id="sb-modal-ok">OK</button>
            </div>
          </div>
        `;
        requestAnimationFrame(() => {
          overlay.classList.add('open');
          document.getElementById('sb-modal-input').focus();
        });

        const input = document.getElementById('sb-modal-input');
        input.addEventListener('keydown', (e) => {
          if (e.key === 'Enter') { closeAndRemove(overlay); resolve(input.value); }
          if (e.key === 'Escape') { closeAndRemove(overlay); resolve(null); }
        });

        document.getElementById('sb-modal-cancel').onclick = () => {
          closeAndRemove(overlay);
          resolve(null);
        };
        document.getElementById('sb-modal-ok').onclick = () => {
          closeAndRemove(overlay);
          resolve(input.value);
        };
      });
    }
  };
})();


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