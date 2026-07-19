const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:3001';

async function apiFetch(endpoint, options = {}) {
  const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;
  const headers = { 'Content-Type': 'application/json', ...options.headers };
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const res = await fetch(`${API_URL}${endpoint}`, { ...options, headers });
  const data = await res.json();
  if (!res.ok) throw new Error(data.error || 'Request failed');
  return data;
}

export const api = {
  login: (username, password) =>
    apiFetch('/api/auth/login', { method: 'POST', body: JSON.stringify({ username, password }) }),

  getMe: () => apiFetch('/api/auth/me'),

  getDashboard: () => apiFetch('/api/admin/dashboard'),
  getUsers: (params = {}) => apiFetch(`/api/admin/users?${new URLSearchParams(params)}`),
  banUser: (id, ban, reason) =>
    apiFetch(`/api/admin/users/${id}/ban`, { method: 'PUT', body: JSON.stringify({ ban, reason }) }),
  setAdmin: (id, admin) =>
    apiFetch(`/api/admin/users/${id}/admin`, { method: 'PUT', body: JSON.stringify({ admin }) }),

  getTournaments: (params = {}) => apiFetch(`/api/admin/tournaments?${new URLSearchParams(params)}`),
  updateTournamentStatus: (id, status) =>
    apiFetch(`/api/admin/tournaments/${id}/status`, { method: 'PUT', body: JSON.stringify({ status }) }),

  getTransactions: (params = {}) => apiFetch(`/api/admin/transactions?${new URLSearchParams(params)}`),
  getDisputes: (params = {}) => apiFetch(`/api/admin/disputes?${new URLSearchParams(params)}`),
  resolveDispute: (id, data) =>
    apiFetch(`/api/admin/disputes/${id}/resolve`, { method: 'PUT', body: JSON.stringify(data) }),

  getPayments: (params = {}) => apiFetch(`/api/admin/payments?${new URLSearchParams(params)}`),

  getAnalytics: () => apiFetch('/api/analytics/stats'),
};
