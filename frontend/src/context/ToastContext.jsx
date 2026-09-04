import { createContext, useCallback, useContext, useMemo, useState } from 'react';
import { CheckCircle2, CircleAlert, Info, X } from 'lucide-react';

const ToastContext = createContext(null);

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);

  const dismiss = useCallback((id) => {
    setToasts((current) => current.filter((item) => item.id !== id));
  }, []);

  const push = useCallback((message, type = 'success') => {
    const id = `${Date.now()}-${Math.random()}`;
    setToasts((current) => [...current, { id, message, type }]);
    window.setTimeout(() => dismiss(id), 3600);
  }, [dismiss]);

  const value = useMemo(() => ({ push }), [push]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="fixed right-4 top-4 z-[100] flex w-[min(92vw,380px)] flex-col gap-3">
        {toasts.map((toast) => {
          const Icon = toast.type === 'error' ? CircleAlert : toast.type === 'info' ? Info : CheckCircle2;
          const tone = toast.type === 'error' ? 'border-rose-200 text-rose-700' : toast.type === 'info' ? 'border-blue-200 text-blue-700' : 'border-emerald-200 text-emerald-700';
          return (
            <div key={toast.id} className={`flex items-start gap-3 rounded-2xl border bg-white p-4 shadow-xl ${tone}`}>
              <Icon className="mt-0.5 h-5 w-5 shrink-0" />
              <p className="flex-1 text-sm font-semibold leading-5">{toast.message}</p>
              <button type="button" onClick={() => dismiss(toast.id)} className="rounded-lg p-1 hover:bg-slate-100" aria-label="Dismiss notification">
                <X className="h-4 w-4" />
              </button>
            </div>
          );
        })}
      </div>
    </ToastContext.Provider>
  );
}

export const useToast = () => {
  const context = useContext(ToastContext);
  if (!context) throw new Error('useToast must be used inside ToastProvider');
  return context;
};
