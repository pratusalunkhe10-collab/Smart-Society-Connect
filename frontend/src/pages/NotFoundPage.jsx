import { Home } from 'lucide-react';
import { Link } from 'react-router-dom';

export default function NotFoundPage() {
  return (
    <div className="grid min-h-screen place-items-center bg-slate-50 px-4">
      <div className="max-w-md text-center">
        <p className="text-7xl font-black tracking-tighter text-emerald-600">404</p>
        <h1 className="mt-4 text-3xl font-extrabold text-slate-900">Page not found</h1>
        <p className="mt-3 text-sm leading-7 text-slate-500">The requested Smart Society Connect page does not exist or has moved.</p>
        <Link to="/" className="btn-primary mt-6"><Home className="h-4 w-4" /> Go home</Link>
      </div>
    </div>
  );
}
