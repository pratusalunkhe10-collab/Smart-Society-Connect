import { Bell, CheckCheck, Menu, Moon, Search, Sun } from 'lucide-react';
import { useEffect, useMemo, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { navigation, roleLabels } from '../../data/navigation.js';
import { useAuth } from '../../context/AuthContext.jsx';
import { useTheme } from '../../context/ThemeContext.jsx';
import { initials } from '../../utils/format.js';
import { societyService } from '../../services/societyService.js';

const searchTerms = {
  Dashboard: 'home overview statistics', Residents: 'resident flat family occupant', Visitors: 'visitor gate approval security',
  Complaints: 'complaint issue maintenance support', Billing: 'bill payment maintenance due', Meetings: 'meeting agenda committee schedule',
  'My Profile': 'profile account settings photo image',
};

export default function Topbar({ onMenu }) {
  const { user } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const location = useLocation();
  const navigate = useNavigate();
  const [query, setQuery] = useState('');
  const [open, setOpen] = useState(false);
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const searchRef = useRef(null);
  const notificationRef = useRef(null);
  const isDark = theme === 'dark';
  const current = navigation.find((item) => location.pathname.startsWith(item.to));
  const results = useMemo(() => {
    const term = query.trim().toLowerCase();
    if (!term) return [];
    return navigation.filter((item) => item.roles.includes(user?.role))
      .filter((item) => `${item.label} ${searchTerms[item.label] || ''}`.toLowerCase().includes(term));
  }, [query, user?.role]);
  const unreadCount = notifications.filter((notification) => !notification.isRead).length;

  useEffect(() => {
    let active = true;
    const loadNotifications = async () => {
      if (!user?.id) return;
      try {
        const items = await societyService.listNotifications();
        if (active) setNotifications(items);
      } catch {
        if (active) setNotifications([]);
      }
    };
    loadNotifications();
    const refreshTimer = window.setInterval(loadNotifications, 8000);
    const refreshOnFocus = () => { if (document.visibilityState === 'visible') loadNotifications(); };
    window.addEventListener('focus', loadNotifications);
    document.addEventListener('visibilitychange', refreshOnFocus);
    return () => {
      active = false;
      window.clearInterval(refreshTimer);
      window.removeEventListener('focus', loadNotifications);
      document.removeEventListener('visibilitychange', refreshOnFocus);
    };
  }, [user?.id]);

  useEffect(() => {
    const closeOverlays = (event) => {
      if (!searchRef.current?.contains(event.target)) setOpen(false);
      if (!notificationRef.current?.contains(event.target)) setNotificationsOpen(false);
    };
    document.addEventListener('mousedown', closeOverlays);
    return () => document.removeEventListener('mousedown', closeOverlays);
  }, []);

  const selectResult = (to) => { navigate(to); setQuery(''); setOpen(false); };
  const markAllRead = async () => {
    try { await societyService.markAllNotificationsRead(); setNotifications((items) => items.map((item) => ({ ...item, isRead: true }))); } catch { /* The bell remains usable if the API is temporarily unavailable. */ }
  };
  const openNotification = async (notification) => {
    if (!notification.isRead) {
      try { await societyService.markNotificationRead(notification.notificationId); } catch { /* local state still reflects the user's action */ }
      setNotifications((items) => items.map((item) => item.notificationId === notification.notificationId ? { ...item, isRead: true } : item));
    }
    setNotificationsOpen(false);
    if (notification.targetPath) navigate(notification.targetPath);
  };
  const imageUrl = user?.profileImageUrl;
  const topbarClass = isDark ? 'border-white/[0.06] bg-[#151b28]/90' : 'border-slate-200/80 bg-white/90';
  const controlClass = isDark ? 'border-white/10 bg-white/[0.05] text-slate-300 hover:bg-white/10' : 'border-slate-200 bg-white text-slate-600 hover:bg-slate-50';
  const titleClass = isDark ? 'text-white' : 'text-slate-900';
  const mutedClass = isDark ? 'text-slate-400' : 'text-slate-500';

  return (
    <header className={`sticky top-0 z-30 border-b backdrop-blur-xl ${topbarClass}`}>
      <div className="flex h-[76px] items-center gap-3 px-4 sm:px-6 lg:px-8">
        <button type="button" onClick={onMenu} className={`rounded-xl p-2.5 lg:hidden ${controlClass}`} aria-label="Open navigation"><Menu className="h-5 w-5" /></button>
        <div className="min-w-0"><p className={`truncate text-sm font-extrabold ${titleClass}`}>{current?.label || 'Smart Society Connect'}</p><p className={`hidden text-xs sm:block ${mutedClass}`}>Manage your society securely and efficiently</p></div>

        <div ref={searchRef} className="relative ml-auto hidden w-full max-w-sm md:block">
          <label className="relative block">
            <Search className={`pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 ${mutedClass}`} />
            <input className={`w-full rounded-xl border py-2.5 pl-10 pr-3 text-sm outline-none transition placeholder:text-slate-500 focus:border-rose-500 focus:ring-4 focus:ring-rose-500/10 ${isDark ? 'border-white/10 bg-white/[0.05] text-white focus:bg-white/[0.08]' : 'border-slate-200 bg-slate-50 text-slate-800 focus:bg-white'}`} value={query} onChange={(event) => { setQuery(event.target.value); setOpen(true); }} onFocus={() => setOpen(true)} onKeyDown={(event) => { if (event.key === 'Enter' && results[0]) selectResult(results[0].to); if (event.key === 'Escape') setOpen(false); }} placeholder="Search pages: residents, bills..." />
          </label>
          {open && query.trim() && <div className={`absolute right-0 top-full z-50 mt-2 w-full overflow-hidden rounded-xl border p-1 shadow-xl ${isDark ? 'border-white/10 bg-[#202838] shadow-black/30' : 'border-slate-200 bg-white'}`}>{results.length ? results.map(({ label, to, icon: Icon }) => <button key={to} type="button" onClick={() => selectResult(to)} className={`flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-left text-sm font-semibold ${isDark ? 'text-slate-200 hover:bg-rose-500/15 hover:text-rose-200' : 'text-slate-700 hover:bg-rose-50 hover:text-rose-800'}`}><Icon className="h-4 w-4" />{label}</button>) : <p className={`px-3 py-3 text-sm ${mutedClass}`}>No matching workspace page.</p>}</div>}
        </div>

        <button type="button" onClick={toggleTheme} className={`rounded-xl border p-2.5 transition ${controlClass}`} aria-label={`Switch to ${isDark ? 'light' : 'dark'} theme`} title={`Switch to ${isDark ? 'light' : 'dark'} theme`}>
          {isDark ? <Sun className="h-5 w-5" /> : <Moon className="h-5 w-5" />}
        </button>

        <div ref={notificationRef} className="relative">
          <button type="button" onClick={() => setNotificationsOpen((value) => !value)} className={`relative rounded-xl border p-2.5 transition ${controlClass}`} aria-label={`Notifications${unreadCount ? ` (${unreadCount} unread)` : ''}`} aria-expanded={notificationsOpen}>
            <Bell className="h-5 w-5" />{unreadCount > 0 && <span className={`absolute right-2 top-2 grid h-4 min-w-4 place-items-center rounded-full bg-rose-500 px-1 text-[9px] font-bold text-white ring-2 ${isDark ? 'ring-[#151b28]' : 'ring-white'}`}>{unreadCount}</span>}
          </button>
          {notificationsOpen && <div className={`absolute right-0 top-full z-50 mt-2 w-[min(360px,calc(100vw-2rem))] overflow-hidden rounded-2xl border shadow-2xl ${isDark ? 'border-white/10 bg-[#202838] shadow-black/35' : 'border-slate-200 bg-white shadow-slate-900/10'}`}>
            <div className={`flex items-center justify-between border-b px-4 py-3 ${isDark ? 'border-white/10' : 'border-slate-100'}`}><div><p className={`text-sm font-extrabold ${titleClass}`}>Notifications</p><p className={`text-xs ${mutedClass}`}>{unreadCount ? `${unreadCount} unread update${unreadCount > 1 ? 's' : ''}` : 'You are all caught up'}</p></div><button type="button" onClick={markAllRead} disabled={!unreadCount} className="inline-flex items-center gap-1 text-xs font-bold text-rose-500 hover:text-rose-400 disabled:cursor-not-allowed disabled:opacity-40"><CheckCheck className="h-4 w-4" /> Mark all read</button></div>
            <div className="max-h-80 overflow-y-auto">{notifications.length ? notifications.map((notification) => <button key={notification.notificationId} type="button" onClick={() => openNotification(notification)} className={`flex w-full gap-3 border-b px-4 py-3 text-left last:border-0 ${isDark ? 'border-white/[0.07] hover:bg-white/[0.04]' : 'border-slate-100 hover:bg-slate-50'}`}><span className={`mt-1.5 h-2 w-2 shrink-0 rounded-full ${!notification.isRead ? 'bg-rose-500' : 'bg-transparent'}`} /><span><span className={`block text-sm font-semibold ${titleClass}`}>{notification.title}</span><span className={`mt-0.5 block text-xs leading-5 ${mutedClass}`}>{notification.message}</span><span className="mt-1 block text-[11px] text-rose-500">{notification.createdAt ? new Date(notification.createdAt).toLocaleString() : 'Just now'}</span></span></button>) : <p className={`px-4 py-8 text-center text-sm ${mutedClass}`}>No notifications yet.</p>}</div>
            <button type="button" onClick={() => { setNotificationsOpen(false); navigate('/app/profile'); }} className={`w-full border-t px-4 py-3 text-center text-xs font-bold text-rose-500 hover:bg-rose-500/10 ${isDark ? 'border-white/10' : 'border-slate-100'}`}>Manage notification preferences</button>
          </div>}
        </div>

        <button type="button" onClick={() => navigate('/app/profile')} className={`hidden items-center gap-3 border-l pl-4 text-left sm:flex ${isDark ? 'border-white/10' : 'border-slate-200'}`} aria-label="Open my profile">
          {imageUrl ? <img src={imageUrl} alt="Profile" className="h-10 w-10 rounded-xl object-cover" /> : <div className="grid h-10 w-10 place-items-center rounded-xl bg-rose-600 text-sm font-extrabold text-white">{initials(user?.name)}</div>}
          <div className="hidden min-w-0 xl:block"><p className={`max-w-[150px] truncate text-sm font-bold ${titleClass}`}>{user?.name}</p><p className={`max-w-[150px] truncate text-xs ${mutedClass}`}>{roleLabels[user?.role] || user?.role}</p></div>
        </button>
      </div>
    </header>
  );
}
