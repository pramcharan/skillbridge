// auth.js — JWT storage, auth guard, logout
// Include this file on EVERY protected HTML page

const TOKEN_KEY = 'sb_jwt';
const ROLE_KEY  = 'sb_role';
const USER_KEY  = 'sb_user';

/** Save auth data after login */
function saveAuth(token, role, userId) {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(ROLE_KEY,  role);
  if (userId) localStorage.setItem(USER_KEY, userId);
}

/** Get stored JWT token */
function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

/** Get stored role */
function getRole() {
  return localStorage.getItem(ROLE_KEY);
}

/** Get stored userId */
function getUserId() {
  return localStorage.getItem(USER_KEY);
}

/**
 * Protect a page — call at the top of every protected page script.
 * Redirects to login.html if not authenticated or wrong role.
 * @param {string|null} requiredRole - 'FREELANCER', 'CLIENT', 'ADMIN', or null for any
 */
function requireAuth(requiredRole = null) {
  const token = getToken();
  if (!token) {
    window.location.href = '/login.html';
    return false;
  }
  if (requiredRole) {
    const role = getRole();
    if (role !== requiredRole) {
      window.location.href = '/login.html';
      return false;
    }
  }
  return true;
}

/** Logout — clear storage and redirect to home */
async function logout() {
  try {
    await apiFetch('/api/v1/auth/logout', { method: 'POST' });
  } catch (e) {
    // Continue logout even if API fails
  }
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(ROLE_KEY);
  localStorage.removeItem(USER_KEY);
  window.location.href = '/login.html';
}