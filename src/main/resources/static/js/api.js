// api.js — Fetch wrapper that auto-attaches JWT to every request
// Requires auth.js to be loaded first

/**
 * Core fetch wrapper — attaches JWT, handles 401 auto-redirect
 */
async function apiFetch(path, options = {}) {
  const token = getToken ? getToken() : null;

  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { 'Authorization': 'Bearer ' + token } : {}),
    ...(options.headers || {})
  };

  const response = await fetch(path, { ...options, headers });

  // Auto-logout on 401 (token expired or revoked)
  if (response.status === 401) {
    if (typeof getToken === 'function' && getToken()) {
      // Only redirect if we had a token (not a public endpoint)
      localStorage.clear();
      window.location.href = '/login.html';
      return;
    }
  }

  return response;
}

/** Convenience helpers */
const api = {
  get:    (path)       => apiFetch(path, { method: 'GET' }),
  post:   (path, body) => apiFetch(path, { method: 'POST',   body: JSON.stringify(body) }),
  put:    (path, body) => apiFetch(path, { method: 'PUT',    body: JSON.stringify(body) }),
  patch:  (path, body) => apiFetch(path, { method: 'PATCH',  body: JSON.stringify(body) }),
  delete: (path)       => apiFetch(path, { method: 'DELETE' }),
};