import { Building2 } from 'lucide-react';

export default function Brand({ compact = false, light = false }) {
  return (
    <div className="flex items-center gap-3">
      <div className={`grid h-11 w-11 shrink-0 place-items-center rounded-2xl ${light ? 'bg-white/15 text-white ring-1 ring-white/30' : 'bg-gradient-to-br from-rose-600 to-red-700 text-white shadow-lg shadow-rose-700/20'}`}>
        <Building2 className="h-6 w-6" />
      </div>
      {!compact && (
        <div className="min-w-0">
          <p className={`truncate text-base font-extrabold tracking-tight ${light ? 'text-white' : 'text-slate-900'}`}>Smart Society</p>
          <p className={`truncate text-[11px] font-bold uppercase tracking-[0.16em] ${light ? 'text-rose-100' : 'text-rose-700'}`}>Connect</p>
        </div>
      )}
    </div>
  );
}
