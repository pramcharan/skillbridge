// utils.js — Shared helper functions used across all pages

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
  const diff = Date.now() - new Date(dateStr).getTime();
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
  return text.length > maxLength ? text.slice(0, maxLength) + '...' : text;
}

/**
 * Debounce — prevents a function from firing too fast
 * Used for search inputs
 */
function debounce(fn, delay = 350) {
  let timer;
  return (...args) => {
    clearTimeout(timer);
    timer = setTimeout(() => fn(...args), delay);
  };
}

/** Show an alert in a container element */
function showAlert(containerId, message, type = 'error') {
  const el = document.getElementById(containerId);
  if (!el) return;
  el.innerHTML = `<div class="alert alert-${type}">${escHtml(message)}</div>`;
  if (type === 'success') {
    setTimeout(() => { el.innerHTML = ''; }, 4000);
  }
}

/** Get AI badge CSS class from score */
function getBadgeClass(score) {
  if (score >= 70) return 'badge-green';
  if (score >= 40) return 'badge-amber';
  return 'badge-red';
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
  const full  = Math.round(rating);
  return '★'.repeat(full) + '☆'.repeat(5 - full);
}

/** Render pagination controls */
function renderPagination(containerId, currentPage, totalPages, onPageClick) {
  const el = document.getElementById(containerId);
  if (!el) return;
  if (!totalPages || totalPages <= 1) { el.innerHTML = ''; return; }

  let html = '<div style="display:flex;gap:0.5rem;align-items:center;justify-content:center;margin-top:1.5rem;">';
  if (currentPage > 0) {
    html += `<button onclick="${onPageClick}(${currentPage - 1})"
      style="padding:0.4rem 0.9rem;border-radius:8px;background:var(--card2,#111f38);
             border:1px solid var(--border2,#243650);color:var(--text2,#8BA3C7);
             cursor:pointer;font-size:0.85rem;font-family:DM Sans,sans-serif;">← Prev</button>`;
  }
  html += `<span style="font-size:0.85rem;color:var(--text2,#8BA3C7);">
             Page ${currentPage + 1} of ${totalPages}</span>`;
  if (currentPage < totalPages - 1) {
    html += `<button onclick="${onPageClick}(${currentPage + 1})"
      style="padding:0.4rem 0.9rem;border-radius:8px;background:var(--card2,#111f38);
             border:1px solid var(--border2,#243650);color:var(--text2,#8BA3C7);
             cursor:pointer;font-size:0.85rem;font-family:DM Sans,sans-serif;">Next →</button>`;
  }
  html += '</div>';
  el.innerHTML = html;
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