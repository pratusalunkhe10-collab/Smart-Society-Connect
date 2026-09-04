import {
  AlertTriangle,
  Banknote,
  CreditCard,
  Download,
  FileSpreadsheet,
  IndianRupee,
  Landmark,
  ReceiptText,
  TrendingUp,
  WalletCards,
} from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { societyService } from '../../services/societyService.js';
import { downloadCsv, formatCurrency, formatDate, formatDateTime } from '../../utils/format.js';
import useLiveRefresh from '../../hooks/useLiveRefresh.js';
import Badge from '../ui/Badge.jsx';
import EmptyState from '../ui/EmptyState.jsx';
import LoadingState from '../ui/LoadingState.jsx';
import PageHeader from '../ui/PageHeader.jsx';
import StatCard from '../ui/StatCard.jsx';

const methodColors = ['#f43f5e', '#14b8a6', '#8b5cf6', '#f59e0b', '#3b82f6'];
const successfulPayment = (payment) => !payment.paymentStatus || payment.paymentStatus === 'SUCCESS';
const sameCalendarDay = (left, right) => left && new Date(left).toDateString() === right.toDateString();
const dueDateAtEndOfDay = (value) => {
  if (!value) return null;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return null;
  date.setHours(23, 59, 59, 999);
  return date;
};

const buildMonthBuckets = () => {
  const now = new Date();
  return Array.from({ length: 6 }, (_, index) => {
    const date = new Date(now.getFullYear(), now.getMonth() - (5 - index), 1);
    return {
      key: `${date.getFullYear()}-${date.getMonth() + 1}`,
      label: date.toLocaleString('en-IN', { month: 'short' }),
      billed: 0,
      collected: 0,
    };
  });
};

export default function AccountantDashboard() {
  const [bills, setBills] = useState([]);
  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = async ({ silent = false } = {}) => {
    if (!silent) setLoading(true);
    setError('');
    try {
      const [billData, paymentData] = await Promise.all([societyService.listBills(), societyService.listPayments()]);
      setBills(billData);
      setPayments(paymentData);
    } catch (requestError) {
      setError(requestError.message || 'Unable to load finance records.');
    } finally {
      if (!silent) setLoading(false);
    }
  };

  useEffect(() => {
    let active = true;
    Promise.all([societyService.listBills(), societyService.listPayments()])
      .then(([billData, paymentData]) => {
        if (!active) return;
        setBills(billData);
        setPayments(paymentData);
      })
      .catch((requestError) => {
        if (active) setError(requestError.message || 'Unable to load finance records.');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => { active = false; };
  }, []);

  useLiveRefresh(() => load({ silent: true }), { refreshKey: 'accountant-dashboard' });

  const analysis = useMemo(() => {
    const now = new Date();
    const validBills = bills.filter((bill) => bill.status !== 'CANCELLED');
    const validPayments = payments.filter(successfulPayment);
    const paidByBill = validPayments.reduce((result, payment) => {
      result[payment.billingId] = (result[payment.billingId] || 0) + Number(payment.amountPaid || 0);
      return result;
    }, {});
    const balanceFor = (bill) => Math.max(0, Number(bill.total || 0) - Number(paidByBill[bill.id] || 0));

    const billed = validBills.reduce((sum, bill) => sum + Number(bill.total || 0), 0);
    const collected = validPayments.reduce((sum, payment) => sum + Number(payment.amountPaid || 0), 0);
    const outstanding = validBills.reduce((sum, bill) => sum + balanceFor(bill), 0);
    const overdueBills = validBills
      .filter((bill) => {
        const dueDate = dueDateAtEndOfDay(bill.dueDate);
        return balanceFor(bill) > 0 && dueDate && dueDate < now;
      })
      .map((bill) => ({ ...bill, balance: balanceFor(bill) }))
      .sort((left, right) => new Date(left.dueDate) - new Date(right.dueDate));
    const overdue = overdueBills.reduce((sum, bill) => sum + bill.balance, 0);

    const currentMonthBilled = validBills
      .filter((bill) => Number(bill.billingMonth) === now.getMonth() + 1 && Number(bill.billingYear) === now.getFullYear())
      .reduce((sum, bill) => sum + Number(bill.total || 0), 0);
    const currentMonthCollected = validPayments
      .filter((payment) => {
        const date = new Date(payment.paymentDate);
        return date.getMonth() === now.getMonth() && date.getFullYear() === now.getFullYear();
      })
      .reduce((sum, payment) => sum + Number(payment.amountPaid || 0), 0);
    const todayCollection = validPayments
      .filter((payment) => sameCalendarDay(payment.paymentDate, now))
      .reduce((sum, payment) => sum + Number(payment.amountPaid || 0), 0);

    const trend = buildMonthBuckets();
    validBills.forEach((bill) => {
      const bucket = trend.find((item) => item.key === `${bill.billingYear}-${bill.billingMonth}`);
      if (bucket) bucket.billed += Number(bill.total || 0);
    });
    validPayments.forEach((payment) => {
      const date = new Date(payment.paymentDate);
      const bucket = trend.find((item) => item.key === `${date.getFullYear()}-${date.getMonth() + 1}`);
      if (bucket) bucket.collected += Number(payment.amountPaid || 0);
    });

    const paymentMethods = Object.entries(validPayments.reduce((result, payment) => {
      const mode = String(payment.paymentMode || 'OTHER').replaceAll('_', ' ');
      result[mode] = (result[mode] || 0) + Number(payment.amountPaid || 0);
      return result;
    }, {})).map(([name, value]) => ({ name, value }));

    const aging = [
      { label: '1–30 days', min: 1, max: 30, amount: 0, count: 0 },
      { label: '31–60 days', min: 31, max: 60, amount: 0, count: 0 },
      { label: '61–90 days', min: 61, max: 90, amount: 0, count: 0 },
      { label: '90+ days', min: 91, max: Infinity, amount: 0, count: 0 },
    ];
    overdueBills.forEach((bill) => {
      const days = Math.max(1, Math.floor((now - new Date(bill.dueDate)) / 86_400_000));
      const bucket = aging.find((item) => days >= item.min && days <= item.max);
      if (bucket) {
        bucket.amount += bill.balance;
        bucket.count += 1;
      }
    });

    const billById = Object.fromEntries(validBills.map((bill) => [bill.id, bill]));
    const recentPayments = [...validPayments]
      .sort((left, right) => new Date(right.paymentDate) - new Date(left.paymentDate))
      .slice(0, 6)
      .map((payment) => ({ ...payment, bill: billById[payment.billingId] }));

    return {
      validBills,
      paidByBill,
      balanceFor,
      billed,
      collected,
      outstanding,
      overdue,
      overdueBills,
      currentMonthBilled,
      currentMonthCollected,
      todayCollection,
      collectionRate: billed ? (collected / billed) * 100 : 0,
      trend,
      paymentMethods,
      aging,
      recentPayments,
    };
  }, [bills, payments]);

  const exportReport = () => {
    const rows = analysis.validBills.map((bill) => ({
      billing_id: bill.id,
      resident: bill.residentName,
      flat: bill.flat,
      billing_period: bill.month,
      total_billed: Number(bill.total || 0).toFixed(2),
      payments_received: Number(analysis.paidByBill[bill.id] || 0).toFixed(2),
      outstanding: analysis.balanceFor(bill).toFixed(2),
      due_date: bill.dueDate || '',
      status: bill.status,
    }));
    downloadCsv(`finance-report-${new Date().toISOString().slice(0, 10)}.csv`, rows.length ? rows : [{
      billing_id: '', resident: '', flat: '', billing_period: '', total_billed: '0.00',
      payments_received: '0.00', outstanding: '0.00', due_date: '', status: 'NO RECORDS',
    }]);
  };

  if (loading) return <LoadingState rows={7} />;

  return <div>
    <PageHeader
      eyebrow="Finance operations"
      title="Finance dashboard"
      subtitle="Live billing, collection, overdue, payment-mode, and reconciliation analysis."
      actions={<div className="flex flex-wrap gap-2"><button type="button" className="btn-secondary" onClick={exportReport}><Download className="h-4 w-4" /> Export report</button><Link to="/app/billing" className="btn-primary"><ReceiptText className="h-4 w-4" /> Manage billing</Link></div>}
    />

    {error ? <div className="rounded-2xl border border-rose-200 bg-rose-50 p-5 text-sm font-semibold text-rose-700">{error}</div> : <>
      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-6">
        <StatCard label="Billed this month" value={formatCurrency(analysis.currentMonthBilled)} hint={`${analysis.validBills.length} active bills`} icon={ReceiptText} tone="blue" />
        <StatCard label="Collected this month" value={formatCurrency(analysis.currentMonthCollected)} hint={`${formatCurrency(analysis.todayCollection)} today`} icon={IndianRupee} tone="emerald" />
        <StatCard label="Outstanding" value={formatCurrency(analysis.outstanding)} hint="remaining balance" icon={Banknote} tone="rose" />
        <StatCard label="Overdue" value={formatCurrency(analysis.overdue)} hint={`${analysis.overdueBills.length} overdue bills`} icon={AlertTriangle} tone="amber" />
        <StatCard label="Collection rate" value={`${analysis.collectionRate.toFixed(1)}%`} hint={`${formatCurrency(analysis.collected)} received`} icon={TrendingUp} tone="violet" />
        <StatCard label="Transactions" value={payments.filter(successfulPayment).length} hint="successful payments" icon={WalletCards} tone="emerald" />
      </section>

      <section className="mt-6 grid gap-6 xl:grid-cols-[1.55fr_0.85fr]">
        <article className="panel overflow-hidden">
          <div className="panel-header"><div><h2 className="font-extrabold text-slate-900">Billed vs collected</h2><p className="mt-1 text-xs text-slate-500">Six-month cash-flow comparison</p></div><Badge value="Live finance data" /></div>
          <div className="h-[320px] p-4 sm:p-6">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={analysis.trend} margin={{ top: 8, right: 8, left: -8, bottom: 0 }}>
                <CartesianGrid strokeDasharray="4 4" vertical={false} stroke="#e2e8f0" />
                <XAxis dataKey="label" axisLine={false} tickLine={false} tick={{ fill: '#64748b', fontSize: 12 }} />
                <YAxis axisLine={false} tickLine={false} tick={{ fill: '#64748b', fontSize: 11 }} tickFormatter={(value) => `₹${Math.round(value / 1000)}k`} />
                <Tooltip formatter={(value) => formatCurrency(value)} contentStyle={{ borderRadius: 14, borderColor: '#e2e8f0' }} />
                <Bar dataKey="billed" name="Billed" fill="#8b5cf6" radius={[7, 7, 0, 0]} />
                <Bar dataKey="collected" name="Collected" fill="#14b8a6" radius={[7, 7, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </article>

        <article className="panel overflow-hidden">
          <div className="panel-header"><div><h2 className="font-extrabold text-slate-900">Payment methods</h2><p className="mt-1 text-xs text-slate-500">Successful collection breakdown</p></div></div>
          {analysis.paymentMethods.length === 0 ? <EmptyState title="No payments yet" description="Payment methods appear after transactions are recorded." /> : <div className="p-5">
            <div className="h-[210px]">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart><Pie data={analysis.paymentMethods} dataKey="value" nameKey="name" innerRadius={55} outerRadius={82} paddingAngle={4}>{analysis.paymentMethods.map((item, index) => <Cell key={item.name} fill={methodColors[index % methodColors.length]} />)}</Pie><Tooltip formatter={(value) => formatCurrency(value)} /></PieChart>
              </ResponsiveContainer>
            </div>
            <div className="grid gap-2">{analysis.paymentMethods.map((item, index) => <div key={item.name} className="flex items-center justify-between text-xs"><span className="flex items-center gap-2 font-semibold text-slate-600"><span className="h-2.5 w-2.5 rounded-full" style={{ backgroundColor: methodColors[index % methodColors.length] }} />{item.name}</span><span className="font-extrabold text-slate-800">{formatCurrency(item.value)}</span></div>)}</div>
          </div>}
        </article>
      </section>

      <section className="mt-6 grid gap-6 xl:grid-cols-[1.25fr_0.75fr]">
        <article className="panel overflow-hidden">
          <div className="panel-header"><div><h2 className="font-extrabold text-slate-900">Outstanding dues</h2><p className="mt-1 text-xs text-slate-500">Highest-priority balances requiring follow-up</p></div><Link to="/app/billing" className="text-xs font-bold text-rose-600">Open billing register</Link></div>
          {analysis.overdueBills.length === 0 ? <EmptyState title="No overdue balances" description="All bills are current or fully paid." /> : <div className="table-responsive"><table className="data-table"><thead><tr><th>Resident</th><th>Flat</th><th>Due date</th><th>Balance</th><th>Status</th></tr></thead><tbody>{analysis.overdueBills.slice(0, 7).map((bill) => <tr key={bill.id}><td className="font-bold text-slate-800">{bill.residentName || `Resident #${bill.residentId}`}</td><td>{bill.flat || '—'}</td><td>{formatDate(bill.dueDate)}</td><td className="font-extrabold text-rose-600">{formatCurrency(bill.balance)}</td><td><Badge value="OVERDUE" /></td></tr>)}</tbody></table></div>}
        </article>

        <article className="panel overflow-hidden">
          <div className="panel-header"><div><h2 className="font-extrabold text-slate-900">Overdue aging</h2><p className="mt-1 text-xs text-slate-500">Balance grouped by days past due</p></div></div>
          <div className="space-y-3 p-5">{analysis.aging.map((bucket) => <div key={bucket.label} className="rounded-2xl border border-slate-200 bg-slate-50 p-4"><div className="flex items-center justify-between"><span className="text-sm font-bold text-slate-700">{bucket.label}</span><span className="font-extrabold text-slate-900">{formatCurrency(bucket.amount)}</span></div><p className="mt-1 text-xs text-slate-500">{bucket.count} bill{bucket.count === 1 ? '' : 's'}</p></div>)}</div>
        </article>
      </section>

      <section className="mt-6 grid gap-6 xl:grid-cols-[1.25fr_0.75fr]">
        <article className="panel overflow-hidden">
          <div className="panel-header"><div><h2 className="font-extrabold text-slate-900">Recent payments</h2><p className="mt-1 text-xs text-slate-500">Latest successful transactions and references</p></div></div>
          {analysis.recentPayments.length === 0 ? <EmptyState title="No payments recorded" description="New transactions will appear here." /> : <div className="divide-y divide-slate-100">{analysis.recentPayments.map((payment) => <div key={payment.paymentId} className="flex flex-wrap items-center gap-3 px-5 py-4 sm:px-6"><div className="grid h-10 w-10 place-items-center rounded-xl bg-emerald-50 text-emerald-700"><CreditCard className="h-5 w-5" /></div><div className="min-w-[180px] flex-1"><p className="text-sm font-bold text-slate-800">{payment.bill?.residentName || payment.billingId}</p><p className="mt-1 text-xs text-slate-500">{payment.bill?.flat ? `Flat ${payment.bill.flat} · ` : ''}{payment.transactionReference || 'No reference'}</p></div><div className="text-right"><p className="font-extrabold text-emerald-700">{formatCurrency(payment.amountPaid)}</p><p className="mt-1 text-xs text-slate-400">{formatDateTime(payment.paymentDate)}</p></div></div>)}</div>}
        </article>

        <article className="panel overflow-hidden">
          <div className="panel-header"><div><h2 className="font-extrabold text-slate-900">Finance actions</h2><p className="mt-1 text-xs text-slate-500">Common accountant workflows</p></div></div>
          <div className="grid gap-3 p-5">
            <Link to="/app/billing" className="btn-primary justify-center"><ReceiptText className="h-4 w-4" /> Generate or update bills</Link>
            <Link to="/app/billing" className="btn-secondary justify-center"><Landmark className="h-4 w-4" /> Record and reconcile payment</Link>
            <Link to="/app/documents" className="btn-secondary justify-center"><FileSpreadsheet className="h-4 w-4" /> Upload expense or invoice</Link>
            <button type="button" className="btn-secondary justify-center" onClick={exportReport}><Download className="h-4 w-4" /> Download detailed report</button>
          </div>
        </article>
      </section>
    </>}
  </div>;
}
