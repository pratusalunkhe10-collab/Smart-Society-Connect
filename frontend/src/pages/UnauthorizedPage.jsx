import { ShieldX } from 'lucide-react';
import { Link } from 'react-router-dom';

export default function UnauthorizedPage() {
  return (
    <div className="grid min-h-[65vh] place-items-center">
      <div className="max-w-md text-center">
        <div className="mx-auto grid h-20 w-20 place-items-center rounded-3xl bg-rose-50 text-rose-600"><ShieldX className="h-9 w-9" /></div>
        <h1 className="mt-6 text-3xl font-extrabold text-slate-900">Access restricted</h1>
        <p className="mt-3 text-sm leading-7 text-slate-500">Your current role does not have permission to open this module. Contact the society administrator if access is required.</p>
        <Link to="/app/dashboard" className="btn-primary mt-6">Return to dashboard</Link>
      </div>
    </div>
  );
}
