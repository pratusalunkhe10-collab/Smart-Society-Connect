import { BadgeCheck, CalendarDays, Moon, ShieldCheck, Sun, UsersRound } from 'lucide-react';
import { Outlet } from 'react-router-dom';
import Brand from '../../components/layout/Brand.jsx';
import { useTheme } from '../../context/ThemeContext.jsx';

export default function AuthLayout() {
  const { theme, toggleTheme } = useTheme();
  const isDark = theme === 'dark';
  const benefits = [
    [ShieldCheck, 'Secure access', 'Role-based access for every member.'],
    [UsersRound, 'Community first', 'Simple updates for everyday living.'],
    [CalendarDays, 'Stay organised', 'Visitors, bills and meetings in one place.'],
  ];

  return (
    <div className={`min-h-screen transition-colors duration-300 lg:grid lg:grid-cols-[0.95fr_1.05fr] ${isDark ? 'bg-[#10141f]' : 'bg-slate-50'}`}>
      <section className={`relative hidden overflow-hidden p-12 transition-colors duration-300 lg:flex lg:flex-col xl:p-16 ${isDark ? 'bg-gradient-to-br from-[#151b2b] via-[#252033] to-[#54223a] text-white' : 'bg-gradient-to-br from-rose-50 via-white to-violet-100 text-slate-900'}`}>
        <div className="absolute -left-24 top-20 h-72 w-72 rounded-full bg-rose-500/20 blur-3xl" />
        <div className="absolute -right-28 bottom-0 h-80 w-80 rounded-full bg-violet-400/10 blur-3xl" />
        <div className="relative z-10"><Brand light={isDark} /></div>

        <div className="relative z-10 my-auto max-w-md">
          <p className={`text-xs font-extrabold uppercase tracking-[0.2em] ${isDark ? 'text-rose-300' : 'text-rose-700'}`}>Smart Society Connect</p>
          <h1 className="mt-3 text-4xl font-extrabold leading-tight tracking-tight xl:text-5xl">Your community, simply connected.</h1>
          <p className={`mt-5 text-base leading-7 ${isDark ? 'text-slate-300' : 'text-slate-600'}`}>One secure place for residents, visitors, payments, meetings and the daily work of your society.</p>
          <p className={`mt-8 border-l-2 border-rose-400 pl-4 text-sm leading-6 ${isDark ? 'text-rose-100' : 'text-rose-800'}`}>Sign in to see the updates that matter to your home.</p>

          <div className="mt-9 grid gap-3 sm:grid-cols-2">
            {benefits.map(([Icon, title, description], index) => (
              <div
                key={title}
                className={`rounded-2xl border p-4 backdrop-blur-sm transition duration-200 hover:-translate-y-0.5 ${isDark ? 'border-white/15 bg-white/[0.08] hover:bg-white/[0.13]' : 'border-white/80 bg-white/70 shadow-sm hover:bg-white'} ${index === 2 ? 'sm:col-span-2' : ''}`}
              >
                <div className="flex items-start gap-3">
                  <span className={`grid h-9 w-9 shrink-0 place-items-center rounded-xl ${isDark ? 'bg-rose-400/15 text-rose-100' : 'bg-rose-100 text-rose-700'}`}><Icon className="h-4 w-4" /></span>
                  <div>
                    <p className={`text-sm font-bold ${isDark ? 'text-white' : 'text-slate-900'}`}>{title}</p>
                    <p className={`mt-1 text-xs leading-5 ${isDark ? 'text-slate-300' : 'text-slate-600'}`}>{description}</p>
                  </div>
                </div>
              </div>
            ))}
          </div>
          <div className={`mt-7 flex items-center gap-2 text-xs font-semibold ${isDark ? 'text-rose-100' : 'text-rose-800'}`}>
            <BadgeCheck className={`h-4 w-4 ${isDark ? 'text-rose-300' : 'text-rose-600'}`} />
            Built for secure, connected communities
          </div>
        </div>
        <p className="relative z-10 text-xs text-slate-500">Smart Society Connect · Secure community management</p>
      </section>

      <section className={`relative flex min-h-screen items-center justify-center overflow-hidden px-4 py-8 transition-colors duration-300 sm:px-8 ${isDark ? 'bg-[#10141f]' : 'bg-slate-50'}`}>
        <div className="absolute -right-20 -top-24 h-72 w-72 rounded-full bg-rose-600/15 blur-3xl" />
        <div className="absolute -bottom-28 -left-20 h-72 w-72 rounded-full bg-violet-500/10 blur-3xl" />
        <button
          type="button"
          onClick={toggleTheme}
          className={`absolute right-4 top-4 z-20 inline-flex items-center gap-2 rounded-xl border px-3 py-2 text-xs font-bold shadow-sm transition sm:right-8 sm:top-8 ${isDark ? 'border-white/10 bg-white/[0.06] text-slate-200 hover:bg-white/10' : 'border-slate-200 bg-white text-slate-700 hover:bg-slate-100'}`}
          aria-label={`Switch to ${isDark ? 'light' : 'dark'} theme`}
          title={`Switch to ${isDark ? 'light' : 'dark'} theme`}
        >
          {isDark ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
          {isDark ? 'Light' : 'Dark'}
        </button>
        <div className="relative w-full max-w-md">
          <div className="mb-8 pr-24 lg:hidden"><Brand light={isDark} /></div>
          <Outlet />
        </div>
      </section>
    </div>
  );
}
