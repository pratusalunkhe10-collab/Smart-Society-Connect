import { titleCase } from '../../utils/format.js';

const tones = {
  ACTIVE: 'bg-emerald-50 text-emerald-700 ring-emerald-600/20',
  VERIFIED: 'bg-emerald-50 text-emerald-700 ring-emerald-600/20',
  APPROVED: 'bg-emerald-50 text-emerald-700 ring-emerald-600/20',
  PAID: 'bg-emerald-50 text-emerald-700 ring-emerald-600/20',
  RESOLVED: 'bg-emerald-50 text-emerald-700 ring-emerald-600/20',
  COMPLETED: 'bg-emerald-50 text-emerald-700 ring-emerald-600/20',
  CHECKED_IN: 'bg-blue-50 text-blue-700 ring-blue-600/20',
  IN_PROGRESS: 'bg-blue-50 text-blue-700 ring-blue-600/20',
  ONGOING: 'bg-blue-50 text-blue-700 ring-blue-600/20',
  UPCOMING: 'bg-violet-50 text-violet-700 ring-violet-600/20',
  PENDING: 'bg-amber-50 text-amber-700 ring-amber-600/20',
  AWAITING_OTP: 'bg-amber-50 text-amber-700 ring-amber-600/20',
  OPEN: 'bg-amber-50 text-amber-700 ring-amber-600/20',
  HIGH: 'bg-rose-50 text-rose-700 ring-rose-600/20',
  OVERDUE: 'bg-rose-50 text-rose-700 ring-rose-600/20',
  REJECTED: 'bg-rose-50 text-rose-700 ring-rose-600/20',
  INACTIVE: 'bg-slate-100 text-slate-600 ring-slate-500/20',
  CHECKED_OUT: 'bg-slate-100 text-slate-600 ring-slate-500/20',
  CLOSED: 'bg-slate-100 text-slate-600 ring-slate-500/20',
  MEDIUM: 'bg-orange-50 text-orange-700 ring-orange-600/20',
  LOW: 'bg-sky-50 text-sky-700 ring-sky-600/20',
};

export default function Badge({ value, className = '' }) {
  const key = String(value || '').toUpperCase();
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-1 text-xs font-bold ring-1 ring-inset ${tones[key] || 'bg-slate-100 text-slate-600 ring-slate-500/20'} ${className}`}>
      {titleCase(key || 'Unknown')}
    </span>
  );
}
