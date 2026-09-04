import { Inbox } from 'lucide-react';

export default function EmptyState({ title = 'No records found', description = 'Try changing your filters or create a new record.' }) {
  return (
    <div className="flex flex-col items-center justify-center px-6 py-16 text-center">
      <div className="rounded-2xl bg-slate-100 p-4 text-slate-400"><Inbox className="h-7 w-7" /></div>
      <h3 className="mt-4 font-bold text-slate-800">{title}</h3>
      <p className="mt-1 max-w-sm text-sm leading-6 text-slate-500">{description}</p>
    </div>
  );
}
