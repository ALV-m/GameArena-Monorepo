'use client';
import { useState, useEffect, useCallback } from 'react';
import { api } from '../lib/api';

const styles = {
  body: { margin: 0, fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif', background: '#0a0a0a', color: '#e0e0e0', minHeight: '100vh' },
  login: { display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh' },
  loginBox: { background: '#1a1a2e', padding: '40px', borderRadius: '12px', width: '360px', boxShadow: '0 4px 24px rgba(0,0,0,0.5)' },
  input: { width: '100%', padding: '12px', margin: '8px 0', border: '1px solid #333', borderRadius: '8px', background: '#0f0f1a', color: '#fff', fontSize: '14px', boxSizing: 'border-box' },
  btn: { width: '100%', padding: '12px', background: '#6C5CE7', color: '#fff', border: 'none', borderRadius: '8px', fontSize: '16px', cursor: 'pointer', marginTop: '16px', fontWeight: 'bold' },
  header: { background: '#1a1a2e', padding: '16px 32px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid #333' },
  nav: { display: 'flex', gap: '8px', flexWrap: 'wrap' },
  navBtn: (active) => ({ padding: '8px 16px', background: active ? '#6C5CE7' : 'transparent', color: active ? '#fff' : '#888', border: 'none', borderRadius: '8px', cursor: 'pointer', fontSize: '14px', fontWeight: active ? 'bold' : 'normal' }),
  cards: { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '16px', padding: '32px' },
  card: { background: '#1a1a2e', padding: '24px', borderRadius: '12px', border: '1px solid #333', transition: 'border-color 0.2s' },
  cardTitle: { color: '#888', fontSize: '12px', textTransform: 'uppercase', letterSpacing: '1px', margin: '0 0 8px 0' },
  cardValue: { color: '#fff', fontSize: '28px', fontWeight: 'bold', margin: 0 },
  cardSub: { color: '#666', fontSize: '12px', marginTop: '4px' },
  table: { width: '100%', borderCollapse: 'collapse', padding: '0 32px' },
  th: { textAlign: 'left', padding: '12px', borderBottom: '2px solid #333', color: '#888', fontSize: '12px', textTransform: 'uppercase' },
  td: { padding: '12px', borderBottom: '1px solid #222', fontSize: '14px' },
  badge: (color) => ({ padding: '4px 8px', borderRadius: '4px', fontSize: '11px', fontWeight: 'bold', background: color + '22', color }),
  section: { padding: '32px' },
  sectionTitle: { fontSize: '20px', fontWeight: 'bold', marginBottom: '16px' },
  err: { color: '#ff6b6b', padding: '8px', fontSize: '14px' },
  modal: { position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.8)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1000, padding: '20px' },
  modalBox: { background: '#1a1a2e', borderRadius: '16px', border: '1px solid #333', maxWidth: '800px', width: '100%', maxHeight: '90vh', overflow: 'auto', padding: '32px' },
  select: { padding: '8px 12px', border: '1px solid #333', borderRadius: '8px', background: '#0f0f1a', color: '#fff', fontSize: '14px' },
  textarea: { width: '100%', padding: '12px', border: '1px solid #333', borderRadius: '8px', background: '#0f0f1a', color: '#fff', fontSize: '14px', boxSizing: 'border-box', minHeight: '80px', resize: 'vertical' },
  smallBtn: (color = '#6C5CE7') => ({ padding: '6px 14px', borderRadius: '6px', border: 'none', cursor: 'pointer', background: color, color: '#fff', fontSize: '12px', fontWeight: 'bold' }),
  chartBar: (height, color) => ({ height: `${Math.max(height, 2)}%`, background: color, borderRadius: '4px 4px 0 0', transition: 'height 0.3s' }),
  tabRow: { display: 'flex', gap: '8px', marginBottom: '16px', flexWrap: 'wrap' },
  tab: (active) => ({ padding: '6px 14px', borderRadius: '6px', border: '1px solid ' + (active ? '#6C5CE7' : '#333'), cursor: 'pointer', background: active ? '#6C5CE722' : 'transparent', color: active ? '#6C5CE7' : '#888', fontSize: '12px', fontWeight: active ? 'bold' : 'normal' }),
};

const statusColors = {
  registration: '#4ecdc4', in_progress: '#f9ca24', completed: '#6ab04c', cancelled: '#ff6b6b',
  open: '#4ecdc4', pending: '#f9ca24', disputed: '#ff6b6b', resolved: '#6ab04c',
  reviewing: '#a78bfa', dismissed: '#888',
  deposit: '#6ab04c', withdrawal: '#ff6b6b', challenge_entry: '#f9ca24',
};

const gameIcons = {
  efootball: '⚽', ea_fc_mobile: '⚽', pubg: '🔫', cod_mobile: '🔫',
  mobile_legends: '🛡️', free_fire_max: '🔥', free_fire: '🔥',
  clash_royale: '👑', chess: '♟️', clash_of_clans: '⚔️',
};

function BarChart({ data, labelKey, valueKey, color = '#6C5CE7', maxVal }) {
  const max = maxVal || Math.max(...data.map(d => d[valueKey]), 1);
  return (
    <div style={{ display: 'flex', alignItems: 'flex-end', gap: '6px', height: '120px', padding: '8px 0' }}>
      {data.map((d, i) => (
        <div key={i} style={{ flex: 1, textAlign: 'center' }}>
          <div style={{ height: '100px', display: 'flex', alignItems: 'flex-end' }}>
            <div style={{ ...styles.chartBar((d[valueKey] / max) * 100, color), width: '100%' }} title={`${d[labelKey]}: ${d[valueKey]}`} />
          </div>
          <p style={{ fontSize: '10px', color: '#666', marginTop: '4px' }}>{d[labelKey]}</p>
        </div>
      ))}
    </div>
  );
}

export default function AdminDashboard() {
  const [token, setToken] = useState(null);
  const [user, setUser] = useState(null);
  const [page, setPage] = useState('dashboard');
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [loginForm, setLoginForm] = useState({ username: '', password: '' });
  const [loading, setLoading] = useState(false);

  const [disputeModal, setDisputeModal] = useState(null);
  const [resolveForm, setResolveForm] = useState({ winner_id: '', resolution: '' });
  const [userModal, setUserModal] = useState(null);

  const [disputeFilter, setDisputeFilter] = useState('open');
  const [userSearch, setUserSearch] = useState('');

  useEffect(() => {
    const saved = localStorage.getItem('token');
    if (saved) { setToken(saved); loadDashboard(); }
  }, []);

  async function login(e) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await api.login(loginForm.username, loginForm.password);
      localStorage.setItem('token', res.token);
      setToken(res.token);
      setUser(res.user);
      if (!res.user.is_admin) { setError('Admin access only'); setLoading(false); return; }
      loadDashboard();
    } catch (err) { setError(err.message); setLoading(false); }
  }

  function logout() {
    localStorage.removeItem('token');
    setToken(null);
    setUser(null);
    setData(null);
  }

  async function loadDashboard() {
    setLoading(true);
    try {
      const d = await api.getDashboard();
      setData(d);
      setPage('dashboard');
    } catch (err) { setError(err.message); }
    setLoading(false);
  }

  async function loadPage(p) {
    setPage(p);
    setLoading(true);
    try {
      if (p === 'users') setData({ users: await api.getUsers() });
      else if (p === 'tournaments') setData({ tournaments: await api.getTournaments() });
      else if (p === 'transactions') setData({ transactions: await api.getTransactions() });
      else if (p === 'disputes') setData({ disputes: await api.getDisputes({ status: disputeFilter === 'all' ? undefined : disputeFilter }) });
      else if (p === 'payments') setData({ payments: await api.getPayments() });
    } catch (err) { setError(err.message); }
    setLoading(false);
  }

  async function handleBan(userId, ban) {
    try {
      await api.banUser(userId, ban, ban ? 'Banned by admin' : null);
      loadPage('users');
    } catch (err) { setError(err.message); }
  }

  async function handleAdminToggle(userId, admin) {
    try {
      await api.setAdmin(userId, admin);
      loadPage('users');
    } catch (err) { setError(err.message); }
  }

  async function handleResolveDispute() {
    if (!disputeModal) return;
    try {
      await api.resolveDispute(disputeModal.id, {
        winner_id: resolveForm.winner_id || null,
        resolution: resolveForm.resolution,
      });
      setDisputeModal(null);
      setResolveForm({ winner_id: '', resolution: '' });
      loadPage('disputes');
    } catch (err) { setError(err.message); }
  }

  async function loadUserDetail(userId) {
    try {
      const users = await api.getUsers();
      const found = users.find(u => u.id === userId);
      if (found) setUserModal(found);
    } catch (err) { setError(err.message); }
  }

  useEffect(() => {
    if (page === 'disputes') loadPage('disputes');
  }, [disputeFilter]);

  if (!token) {
    return (
      <div style={styles.body}>
        <div style={styles.login}>
          <form style={styles.loginBox} onSubmit={login}>
            <h2 style={{ color: '#6C5CE7', textAlign: 'center', marginBottom: '24px', fontSize: '24px' }}>GameArena Admin</h2>
            <p style={{ color: '#888', textAlign: 'center', fontSize: '13px', marginBottom: '24px' }}>Dispute Resolution & Management</p>
            {error && <p style={styles.err}>{error}</p>}
            <input style={styles.input} placeholder="Username" value={loginForm.username}
              onChange={e => setLoginForm({ ...loginForm, username: e.target.value })} />
            <input style={styles.input} type="password" placeholder="Password" value={loginForm.password}
              onChange={e => setLoginForm({ ...loginForm, password: e.target.value })} />
            <button style={{ ...styles.btn, opacity: loading ? 0.7 : 1 }} type="submit" disabled={loading}>
              {loading ? 'Logging in...' : 'Login'}
            </button>
          </form>
        </div>
      </div>
    );
  }

  return (
    <div style={styles.body}>
      <header style={styles.header}>
        <div>
          <h3 style={{ color: '#6C5CE7', margin: 0, fontSize: '18px' }}>GameArena Admin</h3>
          {user && <p style={{ color: '#666', margin: 0, fontSize: '11px' }}>{user.username} — Admin</p>}
        </div>
        <nav style={styles.nav}>
          {['dashboard', 'users', 'tournaments', 'transactions', 'disputes', 'payments'].map(p => (
            <button key={p} style={styles.navBtn(page === p)} onClick={() => { setPage(p); loadPage(p); }}>
              {p === 'disputes' && data?.disputes?.some(d => d.status === 'open')
                ? `⚠️ ${p.charAt(0).toUpperCase() + p.slice(1)}`
                : p.charAt(0).toUpperCase() + p.slice(1)
              }
            </button>
          ))}
          <button style={{ ...styles.navBtn(false), color: '#ff6b6b' }} onClick={logout}>Logout</button>
        </nav>
      </header>

      {error && (
        <div style={{ ...styles.err, padding: '12px 32px', background: '#ff6b6b11', borderBottom: '1px solid #ff6b6b33', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          {error}
          <button onClick={() => setError('')} style={{ background: 'none', color: '#ff6b6b', border: 'none', cursor: 'pointer', fontSize: '16px' }}>✕</button>
        </div>
      )}

      {loading && <div style={{ padding: '8px 32px', color: '#6C5CE7', fontSize: '13px' }}>Loading...</div>}

      {page === 'dashboard' && data && (
        <>
          <div style={styles.cards}>
            <div style={styles.card}>
              <p style={styles.cardTitle}>Total Users</p>
              <p style={styles.cardValue}>{data.users?.total || 0}</p>
              <p style={styles.cardSub}>+{data.users?.today || 0} today</p>
            </div>
            <div style={styles.card}>
              <p style={styles.cardTitle}>Active Tournaments</p>
              <p style={{ ...styles.cardValue, color: '#4ecdc4' }}>{data.tournaments?.active || 0}</p>
              <p style={styles.cardSub}>{data.tournaments?.completed || 0} completed</p>
            </div>
            <div style={styles.card}>
              <p style={styles.cardTitle}>Completed Matches</p>
              <p style={{ ...styles.cardValue, color: '#6ab04c' }}>{data.matches?.completed || 0}</p>
              <p style={styles.cardSub}>{data.matches?.disputed || 0} disputed</p>
            </div>
            <div style={styles.card}>
              <p style={styles.cardTitle}>Open Disputes</p>
              <p style={{ ...styles.cardValue, color: data.disputes?.pending > 0 ? '#ff6b6b' : '#6ab04c' }}>
                {data.disputes?.pending || 0}
              </p>
              <p style={styles.cardSub}>{data.disputes?.total || 0} total</p>
            </div>
            <div style={styles.card}>
              <p style={styles.cardTitle}>Total Deposited</p>
              <p style={{ ...styles.cardValue, color: '#22C55E' }}>KES {parseFloat(data.transactions?.total_deposited || 0).toLocaleString()}</p>
            </div>
            <div style={styles.card}>
              <p style={styles.cardTitle}>Platform Revenue</p>
              <p style={{ ...styles.cardValue, color: '#6C5CE7' }}>KES {parseFloat(data.revenue?.platform_fees || 0).toLocaleString()}</p>
            </div>
          </div>

          <div style={{ padding: '0 32px 32px', display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
            <div style={{ ...styles.card, gridColumn: 'span 2' }}>
              <p style={styles.cardTitle}>Quick Actions</p>
              <div style={{ display: 'flex', gap: '12px', marginTop: '12px', flexWrap: 'wrap' }}>
                {data.disputes?.pending > 0 && (
                  <button style={styles.smallBtn('#ff6b6b')} onClick={() => { setDisputeFilter('open'); loadPage('disputes'); }}>
                    ⚠️ Review {data.disputes.pending} Open Disputes
                  </button>
                )}
                <button style={styles.smallBtn()} onClick={() => loadPage('users')}>View Users</button>
                <button style={styles.smallBtn()} onClick={() => loadPage('tournaments')}>View Tournaments</button>
                <button style={styles.smallBtn('#22C55E')} onClick={loadDashboard}>Refresh Dashboard</button>
              </div>
            </div>
          </div>
        </>
      )}

      {page === 'users' && data?.users && (
        <div style={styles.section}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <h2 style={styles.sectionTitle}>Users ({data.users.length})</h2>
            <input style={{ ...styles.input, width: '250px', margin: 0 }} placeholder="Search users..."
              value={userSearch} onChange={e => setUserSearch(e.target.value)} />
          </div>
          <table style={styles.table}>
            <thead>
              <tr>
                <th style={styles.th}>Username</th>
                <th style={styles.th}>Email</th>
                <th style={styles.th}>Balance</th>
                <th style={styles.th}>ELO</th>
                <th style={styles.th}>W/L</th>
                <th style={styles.th}>Admin</th>
                <th style={styles.th}>Status</th>
                <th style={styles.th}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {data.users
                .filter(u => !userSearch || u.username?.toLowerCase().includes(userSearch.toLowerCase()) || u.email?.toLowerCase().includes(userSearch.toLowerCase()))
                .map(u => (
                <tr key={u.id}>
                  <td style={styles.td}>
                    <span style={{ cursor: 'pointer', color: '#6C5CE7' }} onClick={() => loadUserDetail(u.id)}>{u.username}</span>
                  </td>
                  <td style={styles.td}>{u.email || '-'}</td>
                  <td style={styles.td}>KES {parseFloat(u.balance || 0).toFixed(2)}</td>
                  <td style={styles.td}>{u.elo_rating || 1000}</td>
                  <td style={styles.td}>
                    <span style={{ color: '#6ab04c' }}>{u.total_wins || 0}</span>
                    <span style={{ color: '#666' }}> / </span>
                    <span style={{ color: '#ff6b6b' }}>{u.total_losses || 0}</span>
                  </td>
                  <td style={styles.td}>
                    <button onClick={() => handleAdminToggle(u.id, !u.is_admin)}
                      style={{ padding: '4px 10px', borderRadius: '4px', border: 'none', cursor: 'pointer',
                        background: u.is_admin ? '#6C5CE7' : '#333', color: '#fff', fontSize: '11px' }}>
                      {u.is_admin ? 'Admin ✓' : 'Make Admin'}
                    </button>
                  </td>
                  <td style={styles.td}>
                    <span style={styles.badge(u.is_banned ? '#ff6b6b' : '#6ab04c')}>
                      {u.is_banned ? 'BANNED' : 'Active'}
                    </span>
                  </td>
                  <td style={styles.td}>
                    <div style={{ display: 'flex', gap: '6px' }}>
                      <button onClick={() => handleBan(u.id, !u.is_banned)}
                        style={{ padding: '4px 12px', borderRadius: '4px', border: 'none', cursor: 'pointer',
                          background: u.is_banned ? '#6ab04c' : '#ff6b6b', color: '#fff', fontSize: '12px' }}>
                        {u.is_banned ? 'Unban' : 'Ban'}
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {page === 'tournaments' && data?.tournaments && (
        <div style={styles.section}>
          <h2 style={styles.sectionTitle}>Tournaments ({data.tournaments.length})</h2>
          <table style={styles.table}>
            <thead>
              <tr>
                <th style={styles.th}>Name</th>
                <th style={styles.th}>Game</th>
                <th style={styles.th}>Players</th>
                <th style={styles.th}>Entry Fee</th>
                <th style={styles.th}>Prize Pool</th>
                <th style={styles.th}>Status</th>
                <th style={styles.th}>Creator</th>
                <th style={styles.th}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {data.tournaments.map(t => (
                <tr key={t.id}>
                  <td style={styles.td}>{t.name}</td>
                  <td style={styles.td}>{gameIcons[t.game] || '🎮'} {t.game}</td>
                  <td style={styles.td}>{t.current_players}/{t.max_players}</td>
                  <td style={styles.td}>KES {parseFloat(t.entry_fee).toFixed(2)}</td>
                  <td style={styles.td}>KES {parseFloat(t.prize_pool || 0).toFixed(2)}</td>
                  <td style={styles.td}>
                    <span style={styles.badge(statusColors[t.status] || '#888')}>{t.status}</span>
                  </td>
                  <td style={styles.td}>{t.creator_name}</td>
                  <td style={styles.td}>
                    <select style={styles.select} value={t.status}
                      onChange={async (e) => {
                        try {
                          await api.updateTournamentStatus(t.id, e.target.value);
                          loadPage('tournaments');
                        } catch (err) { setError(err.message); }
                      }}>
                      {['draft', 'registration', 'in_progress', 'completed', 'cancelled'].map(s => (
                        <option key={s} value={s}>{s}</option>
                      ))}
                    </select>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {page === 'transactions' && data?.transactions && (
        <div style={styles.section}>
          <h2 style={styles.sectionTitle}>Transactions ({data.transactions.length})</h2>
          <table style={styles.table}>
            <thead>
              <tr>
                <th style={styles.th}>User</th>
                <th style={styles.th}>Type</th>
                <th style={styles.th}>Amount</th>
                <th style={styles.th}>Status</th>
                <th style={styles.th}>Gateway</th>
                <th style={styles.th}>Description</th>
                <th style={styles.th}>Date</th>
              </tr>
            </thead>
            <tbody>
              {data.transactions.map(t => (
                <tr key={t.id}>
                  <td style={styles.td}>{t.username}</td>
                  <td style={styles.td}>
                    <span style={styles.badge(t.type === 'deposit' ? '#6ab04c' : t.type === 'withdrawal' ? '#ff6b6b' : '#f9ca24')}>
                      {t.type}
                    </span>
                  </td>
                  <td style={{ ...styles.td, color: t.type === 'deposit' ? '#6ab04c' : '#ff6b6b', fontWeight: 'bold' }}>
                    {t.type === 'deposit' ? '+' : '-'} KES {parseFloat(t.amount).toFixed(2)}
                  </td>
                  <td style={styles.td}>
                    <span style={styles.badge(statusColors[t.status] || '#888')}>{t.status}</span>
                  </td>
                  <td style={styles.td}>{t.payment_gateway || '-'}</td>
                  <td style={styles.td}>{t.description || '-'}</td>
                  <td style={styles.td}>{new Date(t.created_at).toLocaleDateString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {page === 'disputes' && (
        <div style={styles.section}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <h2 style={styles.sectionTitle}>Disputes</h2>
            <div style={styles.tabRow}>
              {['open', 'reviewing', 'resolved', 'dismissed', 'all'].map(f => (
                <button key={f} style={styles.tab(disputeFilter === f)} onClick={() => setDisputeFilter(f)}>
                  {f.charAt(0).toUpperCase() + f.slice(1)}
                </button>
              ))}
            </div>
          </div>
          {data?.disputes && (
            <table style={styles.table}>
              <thead>
                <tr>
                  <th style={styles.th}>Reporter</th>
                  <th style={styles.th}>Match</th>
                  <th style={styles.th}>Stake</th>
                  <th style={styles.th}>Reason</th>
                  <th style={styles.th}>Evidence</th>
                  <th style={styles.th}>Status</th>
                  <th style={styles.th}>Date</th>
                  <th style={styles.th}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {data.disputes.map(d => (
                  <tr key={d.id}>
                    <td style={styles.td}>{d.reporter_name}</td>
                    <td style={styles.td}>{d.player1_name} vs {d.player2_name}</td>
                    <td style={styles.td}>KES {parseFloat(d.stake_amount || 0).toFixed(2)}</td>
                    <td style={styles.td}>{d.reason?.substring(0, 50)}{d.reason?.length > 50 ? '...' : ''}</td>
                    <td style={styles.td}>
                      {d.evidence_url ? (
                        <a href={d.evidence_url} target="_blank" rel="noopener noreferrer" style={{ color: '#6C5CE7', fontSize: '12px' }}>View</a>
                      ) : '-'}
                    </td>
                    <td style={styles.td}>
                      <span style={styles.badge(statusColors[d.status] || '#888')}>{d.status}</span>
                    </td>
                    <td style={styles.td}>{new Date(d.created_at).toLocaleDateString()}</td>
                    <td style={styles.td}>
                      {d.status !== 'resolved' && d.status !== 'dismissed' && (
                        <button style={styles.smallBtn()} onClick={() => {
                          setDisputeModal(d);
                          setResolveForm({ winner_id: '', resolution: '' });
                        }}>
                          Resolve
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {page === 'payments' && data?.payments && (
        <div style={styles.section}>
          <h2 style={styles.sectionTitle}>Payments ({data.payments.length})</h2>
          <table style={styles.table}>
            <thead>
              <tr>
                <th style={styles.th}>User</th>
                <th style={styles.th}>Gateway</th>
                <th style={styles.th}>Type</th>
                <th style={styles.th}>Amount</th>
                <th style={styles.th}>Status</th>
                <th style={styles.th}>Phone</th>
                <th style={styles.th}>Date</th>
              </tr>
            </thead>
            <tbody>
              {data.payments.map(p => (
                <tr key={p.id}>
                  <td style={styles.td}>{p.username}</td>
                  <td style={styles.td}>{p.gateway}</td>
                  <td style={styles.td}>
                    <span style={styles.badge(p.type === 'deposit' ? '#6ab04c' : '#ff6b6b')}>{p.type}</span>
                  </td>
                  <td style={styles.td}>KES {parseFloat(p.amount).toFixed(2)}</td>
                  <td style={styles.td}>
                    <span style={styles.badge(statusColors[p.status] || '#888')}>{p.status}</span>
                  </td>
                  <td style={styles.td}>{p.phone_number || '-'}</td>
                  <td style={styles.td}>{new Date(p.created_at).toLocaleDateString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {disputeModal && (
        <div style={styles.modal} onClick={() => setDisputeModal(null)}>
          <div style={styles.modalBox} onClick={e => e.stopPropagation()}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
              <h3 style={{ margin: 0, color: '#fff' }}>Resolve Dispute</h3>
              <button onClick={() => setDisputeModal(null)} style={{ background: 'none', color: '#888', border: 'none', cursor: 'pointer', fontSize: '20px' }}>✕</button>
            </div>

            <div style={{ background: '#0f0f1a', borderRadius: '8px', padding: '16px', marginBottom: '20px' }}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                <div>
                  <p style={{ color: '#888', fontSize: '12px', margin: '0 0 4px 0' }}>Reporter</p>
                  <p style={{ color: '#fff', margin: 0, fontWeight: 'bold' }}>{disputeModal.reporter_name}</p>
                </div>
                <div>
                  <p style={{ color: '#888', fontSize: '12px', margin: '0 0 4px 0' }}>Match</p>
                  <p style={{ color: '#fff', margin: 0, fontWeight: 'bold' }}>
                    {disputeModal.player1_name} vs {disputeModal.player2_name}
                  </p>
                </div>
                <div style={{ gridColumn: 'span 2' }}>
                  <p style={{ color: '#888', fontSize: '12px', margin: '0 0 4px 0' }}>Reason</p>
                  <p style={{ color: '#fff', margin: 0 }}>{disputeModal.reason}</p>
                </div>
                {disputeModal.evidence_url && (
                  <div style={{ gridColumn: 'span 2' }}>
                    <p style={{ color: '#888', fontSize: '12px', margin: '0 0 8px 0' }}>Evidence</p>
                    <a href={disputeModal.evidence_url} target="_blank" rel="noopener noreferrer"
                      style={{ color: '#6C5CE7', fontSize: '14px' }}>
                      📎 View Screenshot Evidence
                    </a>
                    {disputeModal.evidence_url.match(/\.(jpg|jpeg|png|webp)$/i) && (
                      <div style={{ marginTop: '12px' }}>
                        <img src={disputeModal.evidence_url} alt="Evidence" style={{ maxWidth: '100%', borderRadius: '8px', border: '1px solid #333' }} />
                      </div>
                    )}
                  </div>
                )}
              </div>
            </div>

            <div style={{ marginBottom: '20px' }}>
              <label style={{ color: '#888', fontSize: '12px', display: 'block', marginBottom: '6px' }}>Award Winner</label>
              <select style={{ ...styles.select, width: '100%' }} value={resolveForm.winner_id}
                onChange={e => setResolveForm({ ...resolveForm, winner_id: e.target.value })}>
                <option value="">No winner (dismiss)</option>
                <option value={disputeModal.player1_id}>{disputeModal.player1_name} (Player 1)</option>
                <option value={disputeModal.player2_id}>{disputeModal.player2_name} (Player 2)</option>
              </select>
            </div>

            <div style={{ marginBottom: '24px' }}>
              <label style={{ color: '#888', fontSize: '12px', display: 'block', marginBottom: '6px' }}>Resolution Notes</label>
              <textarea style={styles.textarea} placeholder="Explain the resolution..."
                value={resolveForm.resolution} onChange={e => setResolveForm({ ...resolveForm, resolution: e.target.value })} />
            </div>

            <div style={{ display: 'flex', gap: '12px' }}>
              <button style={{ ...styles.btn, width: 'auto', flex: 1, background: resolveForm.winner_id ? '#22C55E' : '#ff6b6b', marginTop: 0 }}
                onClick={handleResolveDispute}>
                {resolveForm.winner_id ? 'Resolve & Pay Winner' : 'Dismiss Dispute'}
              </button>
              <button style={{ ...styles.btn, width: 'auto', background: '#333', marginTop: 0 }} onClick={() => setDisputeModal(null)}>
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}

      {userModal && (
        <div style={styles.modal} onClick={() => setUserModal(null)}>
          <div style={styles.modalBox} onClick={e => e.stopPropagation()}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
              <h3 style={{ margin: 0, color: '#fff' }}>User Details</h3>
              <button onClick={() => setUserModal(null)} style={{ background: 'none', color: '#888', border: 'none', cursor: 'pointer', fontSize: '20px' }}>✕</button>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
              <div style={{ background: '#0f0f1a', borderRadius: '8px', padding: '16px' }}>
                <p style={{ color: '#888', fontSize: '12px', margin: '0 0 4px 0' }}>Username</p>
                <p style={{ color: '#fff', margin: 0, fontSize: '18px', fontWeight: 'bold' }}>{userModal.username}</p>
              </div>
              <div style={{ background: '#0f0f1a', borderRadius: '8px', padding: '16px' }}>
                <p style={{ color: '#888', fontSize: '12px', margin: '0 0 4px 0' }}>Email</p>
                <p style={{ color: '#fff', margin: 0 }}>{userModal.email || 'Not set'}</p>
              </div>
              <div style={{ background: '#0f0f1a', borderRadius: '8px', padding: '16px' }}>
                <p style={{ color: '#888', fontSize: '12px', margin: '0 0 4px 0' }}>Balance</p>
                <p style={{ color: '#22C55E', margin: 0, fontSize: '18px', fontWeight: 'bold' }}>KES {parseFloat(userModal.balance || 0).toFixed(2)}</p>
              </div>
              <div style={{ background: '#0f0f1a', borderRadius: '8px', padding: '16px' }}>
                <p style={{ color: '#888', fontSize: '12px', margin: '0 0 4px 0' }}>ELO Rating</p>
                <p style={{ color: '#6C5CE7', margin: 0, fontSize: '18px', fontWeight: 'bold' }}>{userModal.elo_rating || 1000}</p>
              </div>
              <div style={{ background: '#0f0f1a', borderRadius: '8px', padding: '16px' }}>
                <p style={{ color: '#888', fontSize: '12px', margin: '0 0 4px 0' }}>Record</p>
                <p style={{ margin: 0 }}>
                  <span style={{ color: '#6ab04c', fontWeight: 'bold' }}>{userModal.total_wins || 0}W</span>
                  <span style={{ color: '#666' }}> / </span>
                  <span style={{ color: '#ff6b6b', fontWeight: 'bold' }}>{userModal.total_losses || 0}L</span>
                </p>
              </div>
              <div style={{ background: '#0f0f1a', borderRadius: '8px', padding: '16px' }}>
                <p style={{ color: '#888', fontSize: '12px', margin: '0 0 4px 0' }}>Status</p>
                <span style={styles.badge(userModal.is_banned ? '#ff6b6b' : '#6ab04c')}>
                  {userModal.is_banned ? 'BANNED' : 'Active'}
                </span>
                {userModal.is_admin && <span style={{ ...styles.badge('#6C5CE7'), marginLeft: '6px' }}>Admin</span>}
              </div>
              <div style={{ background: '#0f0f1a', borderRadius: '8px', padding: '16px' }}>
                <p style={{ color: '#888', fontSize: '12px', margin: '0 0 4px 0' }}>Joined</p>
                <p style={{ color: '#fff', margin: 0 }}>{new Date(userModal.created_at).toLocaleDateString()}</p>
              </div>
            </div>

            <div style={{ display: 'flex', gap: '12px', marginTop: '20px' }}>
              <button style={{ ...styles.smallBtn(userModal.is_banned ? '#6ab04c' : '#ff6b6b') }}
                onClick={() => { handleBan(userModal.id, !userModal.is_banned); setUserModal(null); }}>
                {userModal.is_banned ? 'Unban User' : 'Ban User'}
              </button>
              <button style={styles.smallBtn(userModal.is_admin ? '#333' : '#6C5CE7')}
                onClick={() => { handleAdminToggle(userModal.id, !userModal.is_admin); setUserModal(null); }}>
                {userModal.is_admin ? 'Remove Admin' : 'Make Admin'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
