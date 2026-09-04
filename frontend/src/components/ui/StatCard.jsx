import { ArrowDownRight, ArrowUpRight } from 'lucide-react';

export default function StatCard({ label, value, hint, icon: Icon, trend, tone = 'emerald' }) {
  const tones = {
    emerald: 'bg-emerald-50 text-emerald-700',
    blue: 'bg-blue-50 text-blue-700',
    violet: 'bg-violet-50 text-violet-700',
    amber: 'bg-amber-50 text-amber-700',
    rose: 'bg-rose-50 text-rose-700',
  };
  const positive = !String(trend || '').startsWith('-');
  const TrendIcon = positive ? ArrowUpRight : ArrowDownRight;

  return (
    <article className="panel p-5">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-sm font-semibold text-slate-500">{label}</p>
          <p className="mt-2 text-2xl font-extrabold tracking-tight text-slate-900">{value}</p>
        </div>
        <div className={`rounded-2xl p-3 ${tones[tone] || tones.emerald}`}>
          <Icon className="h-6 w-6" />
        </div>
      </div>
      <div className="mt-4 flex items-center gap-2 text-xs">
        {trend && (
          <span className={`inline-flex items-center gap-0.5 font-bold ${positive ? 'text-emerald-600' : 'text-rose-600'}`}>
            <TrendIcon className="h-3.5 w-3.5" /> {trend}
          </span>
        )}
        <span className="text-slate-400">{hint}</span>
      </div>
    </article>
  );
}
