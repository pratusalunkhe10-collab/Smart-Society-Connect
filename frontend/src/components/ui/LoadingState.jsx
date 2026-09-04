export default function LoadingState({ rows = 4 }) {
  return (
    <div className="animate-pulse space-y-3">
      {Array.from({ length: rows }).map((_, index) => (
        <div key={index} className="h-16 rounded-2xl bg-slate-200/70" />
      ))}
    </div>
  );
}
