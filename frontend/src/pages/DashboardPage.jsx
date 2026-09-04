import {
  Banknote,
  CalendarClock,
  CircleAlert,
  Clock3,
  ClipboardCheck,
  IndianRupee,
  LogIn,
  MessageSquareWarning,
  ShieldCheck,
  UsersRound,
} from 'lucide-react';
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Area,
  AreaChart,
  CartesianGrid,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import PageHeader from '../components/ui/PageHeader.jsx';
import StatCard from '../components/ui/StatCard.jsx';
import LoadingState from '../components/ui/LoadingState.jsx';
import Badge from '../components/ui/Badge.jsx';
import AccountantDashboard from '../components/dashboard/AccountantDashboard.jsx';
import { useAuth } from '../context/AuthContext.jsx';
import { societyService } from '../services/societyService.js';
import { formatCurrency, formatDateTime } from '../utils/format.js';
import useLiveRefresh from '../hooks/useLiveRefresh.js';

const complaintStatusColors = {
  OPEN: '#f59e0b',
  IN_PROGRESS: '#3b82f6',
  RESOLVED: '#10b981',
  CLOSED: '#64748b',
  REJECTED: '#ef4444',
};

const complaintColor = (status) => complaintStatusColors[String(status || '').toUpperCase()] || '#8b5cf6';

const activityIcons = {
  visitor: ShieldCheck,
  payment: IndianRupee,
  complaint: MessageSquareWarning,
  meeting: CalendarClock,
};

const greetingForCurrentTime = () => {
  const hour = new Date().getHours();
  if (hour < 12) return 'Good morning';
  if (hour < 18) return 'Good afternoon';
  return 'Good evening';
};

export default function DashboardPage() {
  const { user } = useAuth();
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [health, setHealth] = useState(null);
  const [greeting, setGreeting] = useState(greetingForCurrentTime);
  const [gateVisitors, setGateVisitors] = useState([]);

  useEffect(() => {
    if (user?.role === 'ACCOUNTANT') return undefined;
    setData(null);
    setError('');
    societyService.getDashboard(user?.role, user?.id)
      .then((payload) => {
        if (!payload?.stats) throw new Error('Dashboard data is unavailable for this role.');
        setData(payload);
      })
      .catch((err) => setError(err.message || 'Unable to load dashboard.'));
    societyService.getHealth().then(setHealth).catch(() => setHealth({ status: 'DOWN' }));
    if (user?.role === 'SECURITY') {
      societyService.listVisitors().then(setGateVisitors).catch(() => setGateVisitors([]));
    } else {
      setGateVisitors([]);
    }
  }, [user?.id, user?.role]);

  useEffect(() => {
    const timer = window.setInterval(() => setGreeting(greetingForCurrentTime()), 60_000);
    return () => window.clearInterval(timer);
  }, []);

  useLiveRefresh(async () => {
    if (!user?.id || user?.role === 'ACCOUNTANT') return;
    try {
      const payload = await societyService.getDashboard(user.role, user.id);
      if (payload?.stats) {
        setData(payload);
        setError('');
      }
      if (user.role === 'SECURITY') setGateVisitors(await societyService.listVisitors());
    } catch (refreshError) {
      setError(refreshError.message || 'Unable to refresh dashboard.');
    }
  }, {
    enabled: Boolean(user?.id && user?.role !== 'ACCOUNTANT'),
    refreshKey: `${user?.id}-${user?.role}`,
  });

  if (user?.role === 'ACCOUNTANT') return <AccountantDashboard />;
  if (!data && !error) return <LoadingState rows={7} />;

  const isResident = user?.role === 'RESIDENT';
  const isSecurity = user?.role === 'SECURITY';
  const today = new Date().toDateString();
  const isToday = (value) => value && new Date(value).toDateString() === today;
  const todayVisitors = gateVisitors.filter((visitor) => isToday(visitor.visitDate) || isToday(visitor.checkIn));
  const pendingVisitors = todayVisitors.filter((visitor) => visitor.status === 'PENDING' || visitor.status === 'REQUESTED');
  const approvedVisitors = todayVisitors.filter((visitor) => visitor.status === 'APPROVED');
  const insideVisitors = gateVisitors.filter((visitor) => visitor.status === 'CHECKED_IN');
  const statCards = isResident
    ? [
      ['My documents', data.stats.documents, 'verified uploads', UsersRound, 'blue'],
      ['My visitors today', data.stats.visitorsToday, 'entries recorded', ShieldCheck, 'emerald'],
      ['My open complaints', data.stats.openComplaints, 'need attention', CircleAlert, 'amber'],
      ['My pending dues', formatCurrency(data.stats.pendingDues), 'to be paid', Banknote, 'rose'],
      ['My payments', formatCurrency(data.stats.collection), 'payments received', IndianRupee, 'emerald'],
      ['Upcoming meetings', data.stats.upcomingMeetings, 'scheduled events', CalendarClock, 'violet'],
    ]
    : isSecurity
      ? [
        ['Visitors today', data.stats.visitorsToday, 'gate entries recorded', ShieldCheck, 'emerald'],
        ['Awaiting approval', pendingVisitors.length, 'resident confirmation needed', Clock3, 'amber'],
        ['Approved to enter', approvedVisitors.length, 'ready for check-in', ClipboardCheck, 'blue'],
        ['Visitors inside', insideVisitors.length, 'check-out still pending', LogIn, 'violet'],
      ]
      : [
      ['Active residents', data.stats.residents, 'verified profiles', UsersRound, 'blue', '+3.2%'],
      ['Visitors today', data.stats.visitorsToday, 'entries recorded', ShieldCheck, 'emerald', '+1'],
      ['Open complaints', data.stats.openComplaints, 'need attention', CircleAlert, 'amber', '-2'],
      ['Pending dues', formatCurrency(data.stats.pendingDues), 'this billing cycle', Banknote, 'rose'],
      ['Collection', formatCurrency(data.stats.collection), 'received this month', IndianRupee, 'emerald', '+8.4%'],
      ['Upcoming meetings', data.stats.upcomingMeetings, 'scheduled events', CalendarClock, 'violet'],
    ];

  return (
    <div>
      <PageHeader
        eyebrow="Society overview"
        title={`${greeting}, ${user?.name?.split(' ')[0] || 'Member'} 👋`}
        subtitle="Here is what is happening across your society today. Figures update from the configured backend or local demo data."
        actions={<div className="flex items-center gap-3"><Badge value={health?.status === 'UP' ? 'API online' : health?.status === 'DOWN' ? 'API offline' : 'Checking API'} /><Link to="/app/visitors" className="btn-primary"><ShieldCheck className="h-4 w-4" /> Register visitor</Link></div>}
      />

      {error ? (
        <div className="rounded-2xl border border-rose-200 bg-rose-50 p-5 text-sm font-semibold text-rose-700">{error}</div>
      ) : (
        <>
          <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-6">
            {statCards.map(([label, value, hint, icon, tone, trend]) => <StatCard key={label} label={label} value={value} hint={hint} icon={icon} trend={trend} tone={tone} />)}
          </section>

          {isSecurity ? (
            <>
              <section className="mt-6 grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">
                <article className="panel overflow-hidden">
                  <div className="panel-header">
                    <div><h2 className="font-extrabold text-slate-900">Today&apos;s gate activity</h2><p className="mt-1 text-xs text-slate-500">Live visitor requests, approvals, and check-ins.</p></div>
                    <Link to="/app/visitors" className="text-xs font-bold text-emerald-700">Open visitor log</Link>
                  </div>
                  <div className="divide-y divide-slate-100">
                    {todayVisitors.length === 0 && <p className="px-5 py-12 text-center text-sm text-slate-500">No visitor activity has been recorded today.</p>}
                    {todayVisitors.slice(0, 7).map((visitor) => (
                      <div key={visitor.id} className="flex flex-wrap items-center gap-3 px-5 py-4 sm:px-6">
                        <div className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-emerald-50 text-emerald-700"><ShieldCheck className="h-5 w-5" /></div>
                        <div className="min-w-[160px] flex-1"><p className="text-sm font-bold text-slate-800">{visitor.name}</p><p className="mt-1 text-xs text-slate-500">For {visitor.residentName || 'resident'} {visitor.flat ? `· Flat ${visitor.flat}` : ''}</p></div>
                        <div className="hidden min-w-[110px] text-xs text-slate-500 sm:block">{visitor.purpose || 'Visit'}{visitor.expectedTime ? ` · ${visitor.expectedTime}` : ''}</div>
                        <Badge value={String(visitor.status || 'PENDING').replace('_', ' ')} />
                      </div>
                    ))}
                  </div>
                </article>
                <article className="panel overflow-hidden">
                  <div className="panel-header"><div><h2 className="font-extrabold text-slate-900">Gate quick actions</h2><p className="mt-1 text-xs text-slate-500">Use these during visitor entry and exit.</p></div></div>
                  <div className="grid gap-3 p-5">
                    <Link to="/app/visitors" className="btn-primary justify-center"><ShieldCheck className="h-4 w-4" /> Register visitor</Link>
                    <Link to="/app/visitors" className="btn-secondary justify-center"><ClipboardCheck className="h-4 w-4" /> Approve & check in</Link>
                    <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4"><p className="text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Gate reminder</p><p className="mt-2 text-sm font-semibold text-slate-700">Confirm a visitor&apos;s approval before check-in and record the exit when they leave.</p></div>
                  </div>
                </article>
              </section>
              <section className="mt-6 panel overflow-hidden">
                <div className="panel-header"><div><h2 className="font-extrabold text-slate-900">Visitors currently inside</h2><p className="mt-1 text-xs text-slate-500">These visitors still need a check-out entry.</p></div><Badge value={`${insideVisitors.length} inside`} /></div>
                <div className="divide-y divide-slate-100">
                  {insideVisitors.length === 0 ? <p className="px-5 py-10 text-center text-sm text-slate-500">Everyone has been checked out.</p> : insideVisitors.map((visitor) => <div key={visitor.id} className="flex items-center gap-3 px-5 py-4 sm:px-6"><LogIn className="h-5 w-5 text-violet-600" /><div className="min-w-0 flex-1"><p className="text-sm font-bold text-slate-800">{visitor.name}</p><p className="mt-1 text-xs text-slate-500">{visitor.residentName || 'Resident'} {visitor.flat ? `· Flat ${visitor.flat}` : ''} · checked in {visitor.checkIn ? formatDateTime(visitor.checkIn) : 'today'}</p></div><Link to="/app/visitors" className="text-xs font-bold text-emerald-700">Record exit</Link></div>)}
                </div>
              </section>
            </>
          ) : <>
          <section className="mt-6 grid gap-6 xl:grid-cols-[1.7fr_1fr]">
            <article className="panel overflow-hidden">
              <div className="panel-header">
                <div><h2 className="font-extrabold text-slate-900">{isResident ? 'My maintenance overview' : 'Maintenance collection'}</h2><p className="mt-1 text-xs text-slate-500">Collected amount versus pending dues over six months</p></div>
                <Badge value="Live summary" />
              </div>
              <div className="h-[315px] p-4 sm:p-6">
                <ResponsiveContainer width="100%" height="100%">
                  <AreaChart data={data.collectionTrend} margin={{ top: 10, right: 8, left: -12, bottom: 0 }}>
                    <defs>
                      <linearGradient id="collectedGradient" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="#10b981" stopOpacity={0.35} />
                        <stop offset="95%" stopColor="#10b981" stopOpacity={0.02} />
                      </linearGradient>
                      <linearGradient id="pendingGradient" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="#f59e0b" stopOpacity={0.28} />
                        <stop offset="95%" stopColor="#f59e0b" stopOpacity={0.02} />
                      </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="4 4" vertical={false} stroke="#e2e8f0" />
                    <XAxis dataKey="month" axisLine={false} tickLine={false} tick={{ fill: '#64748b', fontSize: 12 }} />
                    <YAxis axisLine={false} tickLine={false} tick={{ fill: '#64748b', fontSize: 11 }} tickFormatter={(value) => `₹${Math.round(value / 1000)}k`} />
                    <Tooltip formatter={(value) => formatCurrency(value)} contentStyle={{ borderRadius: 14, borderColor: '#e2e8f0', boxShadow: '0 12px 30px rgba(15,23,42,.12)' }} />
                    <Area type="monotone" dataKey="collected" stroke="#10b981" strokeWidth={3} fill="url(#collectedGradient)" />
                    <Area type="monotone" dataKey="pending" stroke="#f59e0b" strokeWidth={2} fill="url(#pendingGradient)" />
                  </AreaChart>
                </ResponsiveContainer>
              </div>
            </article>

            <article className="panel overflow-hidden">
              <div className="panel-header"><div><h2 className="font-extrabold text-slate-900">{isResident ? 'My complaint status' : 'Complaint health'}</h2><p className="mt-1 text-xs text-slate-500">Current distribution by status</p></div></div>
              <div className="grid min-h-[315px] place-items-center p-4">
                <div className="h-[220px] w-full">
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie data={data.complaintByStatus} dataKey="value" nameKey="name" innerRadius={58} outerRadius={88} paddingAngle={5}>
                        {data.complaintByStatus.map((entry) => <Cell key={entry.name} fill={complaintColor(entry.name)} />)}
                      </Pie>
                      <Tooltip />
                    </PieChart>
                  </ResponsiveContainer>
                </div>
                <div className="flex flex-wrap justify-center gap-4">
                  {data.complaintByStatus.map((item) => (
                    <div key={item.name} className="flex items-center gap-2 text-xs font-semibold text-slate-600"><span className="h-2.5 w-2.5 rounded-full" style={{ backgroundColor: complaintColor(item.name) }} />{item.name} ({item.value})</div>
                  ))}
                </div>
              </div>
            </article>
          </section>

          <section className="mt-6 grid gap-6 xl:grid-cols-[1.35fr_0.9fr]">
            <article className="panel overflow-hidden">
              <div className="panel-header"><div><h2 className="font-extrabold text-slate-900">Recent activity</h2><p className="mt-1 text-xs text-slate-500">{isResident ? 'Your latest requests, payments, and document updates' : 'Latest society operations and updates'}</p></div></div>
              <div className="divide-y divide-slate-100">
                {data.recentActivities.length === 0 && <p className="px-5 py-8 text-center text-sm text-slate-500">No recent society activity yet.</p>}
                {data.recentActivities.slice(0, 6).map((activity) => {
                  const Icon = activityIcons[activity.type] || Clock3;
                  return (
                    <div key={activity.id} className="flex gap-3 px-5 py-4 sm:px-6">
                      <div className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-slate-100 text-slate-600"><Icon className="h-5 w-5" /></div>
                      <div className="min-w-0 flex-1"><p className="text-sm font-bold text-slate-800">{activity.title}</p><p className="mt-1 text-xs leading-5 text-slate-500">{activity.detail}</p></div>
                      <span className="hidden whitespace-nowrap text-xs text-slate-400 sm:block">{formatDateTime(activity.time)}</span>
                    </div>
                  );
                })}
              </div>
            </article>

            <article className="panel overflow-hidden">
              <div className="panel-header"><div><h2 className="font-extrabold text-slate-900">Upcoming meetings</h2><p className="mt-1 text-xs text-slate-500">Notices and community discussions</p></div><Link to="/app/meetings" className="text-xs font-bold text-emerald-700">View all</Link></div>
              <div className="space-y-3 p-5">
                {data.upcomingMeetings.length === 0 && <p className="py-8 text-center text-sm text-slate-500">No upcoming meetings scheduled.</p>}
                {data.upcomingMeetings.map((meeting) => (
                  <div key={meeting.id} className="rounded-2xl border border-slate-200 p-4 transition hover:border-emerald-200 hover:bg-emerald-50/30">
                    <div className="flex items-start gap-3">
                      <div className="rounded-xl bg-violet-50 p-2.5 text-violet-700"><CalendarClock className="h-5 w-5" /></div>
                      <div><p className="text-sm font-extrabold text-slate-800">{meeting.title}</p><p className="mt-1 text-xs text-slate-500">{formatDateTime(meeting.date)} · {meeting.venue}</p></div>
                    </div>
                  </div>
                ))}
              </div>
            </article>
          </section>
          </>}
        </>
      )}
    </div>
  );
}
