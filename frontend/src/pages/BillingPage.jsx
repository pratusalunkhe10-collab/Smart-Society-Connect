import {
  CreditCard,
  Download,
  FileText,
  History,
  IndianRupee,
  Plus,
  Search,
} from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import Badge from "../components/ui/Badge.jsx";
import ConfirmDialog from "../components/ui/ConfirmDialog.jsx";
import EmptyState from "../components/ui/EmptyState.jsx";
import LoadingState from "../components/ui/LoadingState.jsx";
import Modal from "../components/ui/Modal.jsx";
import PageHeader from "../components/ui/PageHeader.jsx";
import { useAuth } from "../context/AuthContext.jsx";
import { useToast } from "../context/ToastContext.jsx";
import { societyService } from "../services/societyService.js";
import {
  downloadCsv,
  formatCurrency,
  formatDate,
  formatDateTime,
} from "../utils/format.js";
import useLiveRefresh from "../hooks/useLiveRefresh.js";

const emptyBill = {
  residentId: "",
  billingMonth: new Date().getMonth() + 1,
  billingYear: new Date().getFullYear(),
  maintenanceAmount: 2800,
  waterCharge: 0,
  electricityCharge: 0,
  parkingCharge: 0,
  penalty: 0,
  otherCharge: 0,
  dueDate: new Date(Date.now() + 10 * 86400000).toISOString().slice(0, 10),
  remarks: "",
};

const loadRazorpayCheckout = () =>
  new Promise((resolve) => {
    if (window.Razorpay) {
      resolve(true);
      return;
    }
    const existing = document.querySelector("script[data-ssc-razorpay]");
    if (existing) {
      existing.addEventListener("load", () => resolve(true), { once: true });
      existing.addEventListener("error", () => resolve(false), { once: true });
      return;
    }
    const script = document.createElement("script");
    script.src = "https://checkout.razorpay.com/v1/checkout.js";
    script.async = true;
    script.dataset.sscRazorpay = "true";
    script.onload = () => resolve(true);
    script.onerror = () => resolve(false);
    document.body.appendChild(script);
  });

export default function BillingPage() {
  const { user } = useAuth();
  const { push } = useToast();
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState("ALL");
  const [billOpen, setBillOpen] = useState(false);
  const [paymentBill, setPaymentBill] = useState(null);
  const [paymentFlow, setPaymentFlow] = useState("ONLINE");
  const [receiptBill, setReceiptBill] = useState(null);
  const [paymentMode, setPaymentMode] = useState("UPI");
  const [paymentAmount, setPaymentAmount] = useState("");
  const [transactionReference, setTransactionReference] = useState("");
  const [bankName, setBankName] = useState("");
  const [chequeDate, setChequeDate] = useState("");
  const [paymentRemarks, setPaymentRemarks] = useState("");
  const [form, setForm] = useState(emptyBill);
  const [residentPreview, setResidentPreview] = useState("");
  const [paymentHistoryOpen, setPaymentHistoryOpen] = useState(false);
  const [paymentHistory, setPaymentHistory] = useState([]);
  const [paymentHistoryTitle, setPaymentHistoryTitle] =
    useState("Payment history");
  const [paymentHistoryLoading, setPaymentHistoryLoading] = useState(false);
  const [selectedPayment, setSelectedPayment] = useState(null);
  const [deletingBill, setDeletingBill] = useState(null);
  const [editingBill, setEditingBill] = useState(null);
  const [billDetails, setBillDetails] = useState(null);

  const canManage = ["ADMIN", "ACCOUNTANT", "SECRETARY"].includes(user?.role);

  const load = async ({ silent = false } = {}) => {
    if (!silent) setLoading(true);
    try {
      if (user?.role === "RESIDENT") {
        const resident = await societyService.getResidentForUser(user.id);
        setItems(await societyService.listBills(resident.id));
      } else if (status !== "ALL") {
        setItems(await societyService.listBillsByStatus(status));
      } else {
        setItems(await societyService.listBills());
      }
    } catch (error) {
      push(error.message || "Unable to load billing records.", "error");
    } finally {
      if (!silent) setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [user?.id, user?.role, status]);
  useLiveRefresh(() => load({ silent: true }), {
    enabled: Boolean(user?.id),
    refreshKey: `${user?.id}-${user?.role}-${status}`,
  });

  const filtered = useMemo(
    () =>
      items.filter((item) => {
        const text =
          `${item.billNo} ${item.residentName} ${item.flat} ${item.month}`.toLowerCase();
        return (
          text.includes(query.toLowerCase()) &&
          (status === "ALL" || item.status === status)
        );
      }),
    [items, query, status],
  );

  const totals = useMemo(
    () => ({
      billed: items.reduce((sum, item) => sum + Number(item.total || 0), 0),
      collected: items
        .filter((item) => item.status === "PAID")
        .reduce((sum, item) => sum + Number(item.total || 0), 0),
      pending: items
        .filter((item) => item.status !== "PAID")
        .reduce((sum, item) => sum + Number(item.total || 0), 0),
      overdue: items
        .filter((item) => item.status === "OVERDUE")
        .reduce((sum, item) => sum + Number(item.total || 0), 0),
    }),
    [items],
  );

  const exportCollectionReport = async () => {
    setBusy(true);
    try {
      const [allBills, payments] = await Promise.all([
        societyService.listBills(),
        societyService.listPayments(),
      ]);
      const paidByBill = payments
        .filter((payment) => payment.paymentStatus === "SUCCESS")
        .reduce(
          (totalsByBill, payment) => ({
            ...totalsByBill,
            [payment.billingId]:
              (totalsByBill[payment.billingId] || 0) +
              Number(payment.amountPaid || 0),
          }),
          {},
        );
      const activeBills = allBills.filter(
        (bill) => bill.status !== "CANCELLED",
      );
      const billed = activeBills.reduce(
        (sum, bill) => sum + Number(bill.total || 0),
        0,
      );
      const collected = Object.values(paidByBill).reduce(
        (sum, amount) => sum + amount,
        0,
      );
      const outstandingFor = (bill) =>
        Math.max(0, Number(bill.total || 0) - Number(paidByBill[bill.id] || 0));
      const pending = activeBills
        .filter((bill) => bill.status !== "PAID")
        .reduce((sum, bill) => sum + outstandingFor(bill), 0);
      const overdue = activeBills
        .filter((bill) => bill.status === "OVERDUE")
        .reduce((sum, bill) => sum + outstandingFor(bill), 0);
      downloadCsv(
        `collection-report-${new Date().toISOString().slice(0, 10)}.csv`,
        [
          {
            report: "Society collection summary",
            generated_at: new Date().toLocaleString(),
            total_bills: activeBills.length,
            total_billed: billed.toFixed(2),
            collection_received: collected.toFixed(2),
            outstanding_balance: pending.toFixed(2),
            overdue_balance: overdue.toFixed(2),
            collection_rate: billed
              ? `${((collected / billed) * 100).toFixed(1)}%`
              : "0.0%",
          },
        ],
      );
      push("Collection report exported.");
    } catch (error) {
      push(error.message || "Unable to export collection report.", "error");
    } finally {
      setBusy(false);
    }
  };

  const createBill = async (event) => {
    event.preventDefault();
    setBusy(true);
    try {
      const payload = {
        ...form,
        residentId: Number(form.residentId),
        billingMonth: Number(form.billingMonth),
        billingYear: Number(form.billingYear),
        maintenanceAmount: Number(form.maintenanceAmount),
        waterCharge: Number(form.waterCharge),
        electricityCharge: Number(form.electricityCharge),
        parkingCharge: Number(form.parkingCharge),
        penalty: Number(form.penalty),
        otherCharge: Number(form.otherCharge),
        remarks: form.remarks || null,
      };
      if (editingBill) {
        await societyService.updateBill(editingBill.id, payload);
        push("Bill updated successfully.");
      } else {
        await societyService.createBill(payload);
        push("Maintenance bill generated successfully.");
      }
      setBillOpen(false);
      setForm(emptyBill);
      setEditingBill(null);
      await load();
    } catch (error) {
      push(error.message || "Unable to generate bill.", "error");
    } finally {
      setBusy(false);
    }
  };

  const resolveResident = async () => {
    if (!form.residentId) {
      setResidentPreview("");
      return;
    }
    try {
      const resident = await societyService.getResident(form.residentId);
      const flat = `${resident.wing || ""}-${resident.flatNo || ""}`.replace(
        /^-|-$/g,
        "",
      );
      setResidentPreview(`${resident.name}${flat ? ` · Flat ${flat}` : ""}`);
    } catch {
      setResidentPreview("Resident not found");
    }
  };

  const payBill = async () => {
    if (!canManage) {
      push(
        "Resident online payments must be completed through Razorpay.",
        "error",
      );
      return;
    }
    if (paymentMode === "CHEQUE" && !transactionReference.trim()) {
      push("Cheque number is required.", "error");
      return;
    }
    if (paymentMode === "CHEQUE" && (!bankName.trim() || !chequeDate)) {
      push("Bank name and cheque date are required.", "error");
      return;
    }
    setBusy(true);
    try {
      const payment = await societyService.recordPayment({
        billingId: paymentBill.id,
        amountPaid: Number(paymentAmount),
        paymentMode,
        transactionReference: transactionReference.trim() || null,
        bankName: paymentMode === "CHEQUE" ? bankName.trim() : null,
        chequeDate: paymentMode === "CHEQUE" ? chequeDate : null,
        remarks: paymentRemarks.trim() || null,
      });
      const successful = payment.paymentStatus === "SUCCESS";
      const remaining = successful
        ? Math.max(0, Number(paymentBill.remaining ?? paymentBill.total) - Number(payment.amountPaid))
        : Number(paymentBill.remaining ?? paymentBill.total);
      push(
        payment.paymentStatus === "PENDING"
          ? "Cheque recorded and awaiting bank clearance."
          : remaining
          ? "Partial payment recorded successfully."
          : "Payment recorded successfully. Receipt is now available.",
      );
      if (successful) {
        setReceiptBill({
          ...paymentBill,
          paidAmount: Number(payment.amountPaid),
          remaining,
          paidAt: payment.paymentDate,
          paymentMode: payment.paymentMode,
          transactionReference: payment.transactionReference,
          recordedByName: payment.recordedByName,
        });
      }
      setPaymentBill(null);
      await load();
    } catch (error) {
      push(error.message || "Unable to record payment.", "error");
    } finally {
      setBusy(false);
    }
  };

  const payWithRazorpay = async () => {
    setBusy(true);
    try {
      const loaded = await loadRazorpayCheckout();
      if (!loaded || !window.Razorpay) {
        throw new Error(
          "Unable to load Razorpay Checkout. Check the internet connection and try again.",
        );
      }

      const order = await societyService.createRazorpayOrder(paymentBill.id);
      if (order.paymentAlreadyCompleted && order.completedPayment) {
        await finishRazorpayPayment(order.completedPayment, true);
        return;
      }
      const checkout = new window.Razorpay({
        key: order.keyId,
        amount: order.amount,
        currency: order.currency,
        order_id: order.orderId,
        name: order.name,
        description: order.description,
        prefill: order.prefill,
        theme: { color: "#e11d48" },
        retry: { enabled: true },
        modal: {
          confirm_close: true,
          ondismiss: () =>
            push("Payment window closed. Your bill was not changed.", "info"),
        },
        handler: async (response) => {
          setBusy(true);
          try {
            const payment = await societyService.verifyRazorpayPayment({
              razorpayOrderId: response.razorpay_order_id,
              razorpayPaymentId: response.razorpay_payment_id,
              razorpaySignature: response.razorpay_signature,
            });
            await finishRazorpayPayment(payment, false);
          } catch (error) {
            push(
              error.message || "Razorpay payment could not be verified.",
              "error",
            );
          } finally {
            setBusy(false);
          }
        },
      });

      checkout.on("payment.failed", (response) => {
        push(
          response?.error?.description ||
            "Razorpay payment failed. Please try again.",
          "error",
        );
      });
      checkout.open();
    } catch (error) {
      const message = error.message || "Unable to start Razorpay Checkout.";
      if (/already been paid|no outstanding balance/i.test(message)) {
        setPaymentBill(null);
        await load({ silent: true });
        push("This bill was already paid. Its latest status is now displayed.", "info");
      } else {
        push(message, "error");
      }
    } finally {
      setBusy(false);
    }
  };

  const finishRazorpayPayment = async (payment, recovered) => {
    const paidBill = paymentBill;
    const remaining = Math.max(
      0,
      Number(paidBill.remaining ?? paidBill.total) - Number(payment.amountPaid),
    );
    const nextStatus = remaining === 0 ? "PAID" : "PARTIALLY_PAID";
    setItems((current) =>
      current.map((item) =>
        String(item.id) === String(paidBill.id)
          ? {
              ...item,
              status: nextStatus,
              remaining,
              paidAt: payment.paymentDate,
              paymentMode: payment.paymentMode,
            }
          : item,
      ),
    );
    setReceiptBill({
      ...paidBill,
      status: nextStatus,
      paidAmount: Number(payment.amountPaid),
      remaining,
      paidAt: payment.paymentDate,
      paymentMode: payment.paymentMode,
      transactionReference: payment.transactionReference,
    });
    setPaymentBill(null);
    push(
      recovered
        ? "Your completed Razorpay payment was recovered and the bill was refreshed."
        : "Razorpay payment verified. Your receipt is ready.",
    );
    await load({ silent: true });
  };

  const openPayment = async (bill, flow = canManage ? "OFFLINE" : "ONLINE") => {
    setBusy(true);
    try {
      const payments = await societyService.listPaymentsByBill(bill.id);
      const alreadyPaid = payments
        .filter((payment) => payment.paymentStatus === "SUCCESS")
        .reduce((sum, payment) => sum + Number(payment.amountPaid || 0), 0);
      const remaining = Math.max(0, Number(bill.total) - alreadyPaid);
      setPaymentBill({ ...bill, remaining });
      setPaymentFlow(flow);
      setPaymentAmount(String(remaining));
      setPaymentMode(flow === "OFFLINE" ? "CASH" : "UPI");
      setTransactionReference("");
      setBankName("");
      setChequeDate("");
      setPaymentRemarks("");
    } catch (error) {
      push(
        error.message || "Unable to calculate the outstanding balance.",
        "error",
      );
    } finally {
      setBusy(false);
    }
  };

  const openPaymentHistory = async (bill = null) => {
    setPaymentHistoryOpen(true);
    setPaymentHistory([]);
    setSelectedPayment(null);
    setPaymentHistoryTitle(
      bill ? `Payment history · ${bill.billNo}` : "All payments",
    );
    setPaymentHistoryLoading(true);
    try {
      setPaymentHistory(
        bill
          ? await societyService.listPaymentsByBill(bill.id)
          : await societyService.listPayments(),
      );
    } catch (error) {
      push(error.message || "Unable to load payment history.", "error");
    } finally {
      setPaymentHistoryLoading(false);
    }
  };

  const openPaymentDetails = async (paymentId) => {
    setPaymentHistoryLoading(true);
    try {
      setSelectedPayment(await societyService.getPayment(paymentId));
    } catch (error) {
      push(error.message || "Unable to load payment details.", "error");
    } finally {
      setPaymentHistoryLoading(false);
    }
  };

  const updateChequeStatus = async (payment, nextStatus) => {
    setBusy(true);
    try {
      const updated = await societyService.updatePaymentStatus(payment.paymentId, {
        status: nextStatus,
        remarks: nextStatus === "SUCCESS" ? "Cheque cleared by society office." : "Cheque returned/bounced.",
      });
      setSelectedPayment(updated);
      setPaymentHistory((current) => current.map((item) => (
        item.paymentId === updated.paymentId ? updated : item
      )));
      push(nextStatus === "SUCCESS" ? "Cheque marked as cleared." : "Cheque marked as bounced.");
      await load({ silent: true });
    } catch (error) {
      push(error.message || "Unable to update cheque status.", "error");
    } finally {
      setBusy(false);
    }
  };

  const markOverdue = async (bill) => {
    setBusy(true);
    try {
      await societyService.markBillOverdue(bill.id);
      push("Bill marked overdue.");
      await load();
    } catch (error) {
      push(error.message || "Unable to mark bill overdue.", "error");
    } finally {
      setBusy(false);
    }
  };

  const openBillEdit = (bill) => {
    const [billingMonth = "", billingYear = ""] = String(
      bill.month || "",
    ).split("/");
    setEditingBill(bill);
    setForm({
      residentId: bill.residentId,
      billingMonth,
      billingYear,
      maintenanceAmount: bill.maintenance || 0,
      waterCharge: bill.water || 0,
      electricityCharge: bill.electricity || 0,
      parkingCharge: bill.parking || 0,
      penalty: bill.penalty || 0,
      otherCharge: bill.otherCharge || 0,
      dueDate: String(bill.dueDate || "").slice(0, 10),
      remarks: bill.remarks || "",
    });
    setBillOpen(true);
  };

  const openBillDetails = async (bill) => {
    setBusy(true);
    try {
      const [details, payments] = await Promise.all([
        societyService.getBill(bill.id),
        societyService.listPaymentsByBill(bill.id),
      ]);
      const paidAmount = payments
        .filter((payment) => payment.paymentStatus === "SUCCESS")
        .reduce((sum, payment) => sum + Number(payment.amountPaid || 0), 0);
      setBillDetails({
        ...details,
        paidAmount,
        balance: Math.max(0, Number(details.total) - paidAmount),
      });
    } catch (error) {
      push(error.message || "Unable to load bill details.", "error");
    } finally {
      setBusy(false);
    }
  };

  const confirmDeleteBill = async () => {
    setBusy(true);
    try {
      await societyService.deleteBill(deletingBill.id);
      push("Bill deleted.");
      setDeletingBill(null);
      await load();
    } catch (error) {
      push(error.message || "Unable to delete bill.", "error");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div>
      <PageHeader
        eyebrow="Billing service"
        title="Maintenance billing"
        subtitle="Generate monthly bills, collect payments, view receipts and monitor pending society dues."
        actions={
          <>
            <button
              className="btn-secondary"
              onClick={() => downloadCsv("billing-report.csv", filtered)}
            >
              <Download className="h-4 w-4" /> Export list
            </button>
            {canManage && (
              <>
                <button
                  className="btn-secondary"
                  disabled={busy}
                  onClick={exportCollectionReport}
                >
                  <Download className="h-4 w-4" /> Collection report
                </button>
                <button
                  className="btn-secondary"
                  onClick={() => openPaymentHistory()}
                >
                  <History className="h-4 w-4" /> Payment history
                </button>
                <button
                  className="btn-primary"
                  onClick={() => {
                    setEditingBill(null);
                    setForm(emptyBill);
                    setBillOpen(true);
                  }}
                >
                  <Plus className="h-4 w-4" /> Generate bill
                </button>
              </>
            )}
          </>
        }
      />

      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {[
          ["Total billed", totals.billed, "bg-blue-50 text-blue-700"],
          ["Collected", totals.collected, "bg-emerald-50 text-emerald-700"],
          ["Pending dues", totals.pending, "bg-amber-50 text-amber-700"],
          ["Overdue", totals.overdue, "bg-rose-50 text-rose-700"],
        ].map(([label, value, tone]) => (
          <div key={label} className="panel flex items-center gap-4 p-4">
            <div
              className={`grid h-11 w-11 place-items-center rounded-xl ${tone}`}
            >
              <IndianRupee className="h-5 w-5" />
            </div>
            <div>
              <p className="text-xl font-extrabold text-slate-900">
                {formatCurrency(value)}
              </p>
              <p className="text-xs font-semibold text-slate-500">{label}</p>
            </div>
          </div>
        ))}
      </section>

      <section className="panel mt-6 overflow-hidden">
        <div className="panel-header">
          <div>
            <h2 className="font-extrabold text-slate-900">Bills & payments</h2>
            <p className="text-xs text-slate-500">
              Current and historical maintenance transactions
            </p>
          </div>
          <div className="flex w-full flex-col gap-2 sm:w-auto sm:flex-row">
            <label className="relative block sm:w-64">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
              <input
                className="field-input pl-9"
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder="Search bill, resident or flat"
              />
            </label>
            <select
              className="field-input sm:w-40"
              value={status}
              onChange={(event) => setStatus(event.target.value)}
            >
              <option value="ALL">All status</option>
              <option value="PENDING">Pending</option>
              <option value="PARTIALLY_PAID">Partially paid</option>
              <option value="PAID">Paid</option>
              <option value="OVERDUE">Overdue</option>
              <option value="CANCELLED">Cancelled</option>
            </select>
          </div>
        </div>

        {loading ? (
          <div className="p-5">
            <LoadingState />
          </div>
        ) : filtered.length === 0 ? (
          <EmptyState />
        ) : (
          <div className="table-responsive">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Bill</th>
                  <th>Resident</th>
                  <th>Month</th>
                  <th>Amount</th>
                  <th>Due / Paid</th>
                  <th>Status</th>
                  <th className="text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((item) => (
                  <tr key={item.id}>
                    <td>
                      <p className="font-bold text-slate-800">{item.billNo}</p>
                      <p className="mt-0.5 text-xs text-slate-400">
                        Maintenance + utilities
                      </p>
                    </td>
                    <td>
                      <p className="font-semibold text-slate-700">
                        {item.residentName}
                      </p>
                      <p className="mt-0.5 text-xs text-slate-400">
                        {item.flat}
                      </p>
                    </td>
                    <td>{item.month}</td>
                    <td>
                      <p className="font-extrabold text-slate-800">
                        {formatCurrency(item.total)}
                      </p>
                      {Number(item.penalty) > 0 && (
                        <p className="mt-0.5 text-xs text-rose-500">
                          Includes {formatCurrency(item.penalty)} penalty
                        </p>
                      )}
                    </td>
                    <td>
                      {item.status === "PAID" ? (
                        <>
                          <p className="text-xs font-semibold text-emerald-700">
                            Paid {formatDate(item.paidAt)}
                          </p>
                          <p className="mt-1 text-xs text-slate-400">
                            {item.paymentMode}
                          </p>
                        </>
                      ) : (
                        <p className="text-xs">
                          Due {formatDate(item.dueDate)}
                        </p>
                      )}
                    </td>
                    <td>
                      <Badge value={item.status} />
                    </td>
                    <td>
                      <div className="flex justify-end gap-1">
                        <button
                          onClick={() => openPaymentHistory(item)}
                          className="rounded-lg px-2.5 py-1.5 text-xs font-bold text-slate-600 hover:bg-slate-100"
                        >
                          <History className="mr-1 inline h-3.5 w-3.5" />{" "}
                          History
                        </button>
                        <button
                          disabled={busy}
                          onClick={() => openBillDetails(item)}
                          className="rounded-lg px-2.5 py-1.5 text-xs font-bold text-slate-600 hover:bg-slate-100"
                        >
                          <FileText className="mr-1 inline h-3.5 w-3.5" />{" "}
                          Invoice
                        </button>
                        {canManage && item.status === "PENDING" && (
                          <button
                            disabled={busy}
                            onClick={() => markOverdue(item)}
                            className="rounded-lg px-2.5 py-1.5 text-xs font-bold text-rose-700 hover:bg-rose-50"
                          >
                            Overdue
                          </button>
                        )}
                        {canManage && item.status !== "PAID" && (
                          <button
                            disabled={busy}
                            onClick={() => openBillEdit(item)}
                            className="rounded-lg px-2.5 py-1.5 text-xs font-bold text-blue-700 hover:bg-blue-50"
                          >
                            Edit
                          </button>
                        )}
                        {canManage && item.status !== "PAID" && (
                          <button
                            disabled={busy}
                            onClick={() => setDeletingBill(item)}
                            className="rounded-lg px-2.5 py-1.5 text-xs font-bold text-rose-700 hover:bg-rose-50"
                          >
                            Delete
                          </button>
                        )}
                        {canManage && item.status !== "PAID" && (
                          <button
                            disabled={busy}
                            onClick={() => openPayment(item, "OFFLINE")}
                            className="rounded-lg px-2.5 py-1.5 text-xs font-bold text-emerald-700 hover:bg-emerald-50"
                          >
                            <CreditCard className="mr-1 inline h-3.5 w-3.5" />{" "}
                            Record offline
                          </button>
                        )}
                        {item.status !== "PAID" && (
                          <button
                            disabled={busy}
                            onClick={() => openPayment(item, "ONLINE")}
                            className="rounded-lg px-2.5 py-1.5 text-xs font-bold text-blue-700 hover:bg-blue-50"
                          >
                            <CreditCard className="mr-1 inline h-3.5 w-3.5" />{" "}
                            {canManage ? "Pay online for resident" : "Pay online"}
                          </button>
                        )}
                        {item.status === "PAID" && (
                          <button
                            onClick={() => setReceiptBill(item)}
                            className="rounded-lg px-2.5 py-1.5 text-xs font-bold text-blue-700 hover:bg-blue-50"
                          >
                            <FileText className="mr-1 inline h-3.5 w-3.5" />{" "}
                            Receipt
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <Modal
        open={billOpen}
        onClose={() => setBillOpen(false)}
        title={
          editingBill ? "Edit maintenance bill" : "Generate maintenance bill"
        }
        description="Create or update a bill for a resident and billing month."
      >
        <form onSubmit={createBill} className="grid gap-4 sm:grid-cols-2">
          <div>
            <label className="field-label">Resident ID</label>
            <input
              className="field-input"
              type="number"
              min="1"
              value={form.residentId}
              onChange={(event) => {
                setForm({ ...form, residentId: event.target.value });
                setResidentPreview("");
              }}
              onBlur={resolveResident}
              required
            />
            {residentPreview && (
              <p
                className={`mt-1 text-xs font-semibold ${residentPreview === "Resident not found" ? "text-rose-600" : "text-emerald-600"}`}
              >
                {residentPreview}
              </p>
            )}
          </div>
          <div>
            <label className="field-label">Billing month</label>
            <select
              className="field-input"
              value={form.billingMonth}
              onChange={(event) =>
                setForm({ ...form, billingMonth: event.target.value })
              }
            >
              {Array.from({ length: 12 }, (_, index) => (
                <option key={index + 1} value={index + 1}>
                  {index + 1}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="field-label">Billing year</label>
            <input
              className="field-input"
              type="number"
              min="2020"
              value={form.billingYear}
              onChange={(event) =>
                setForm({ ...form, billingYear: event.target.value })
              }
              required
            />
          </div>
          <div>
            <label className="field-label">Due date</label>
            <input
              className="field-input"
              type="date"
              value={form.dueDate}
              onChange={(event) =>
                setForm({ ...form, dueDate: event.target.value })
              }
              required
            />
          </div>
          <div>
            <label className="field-label">Maintenance</label>
            <input
              className="field-input"
              type="number"
              min="0"
              value={form.maintenanceAmount}
              onChange={(event) =>
                setForm({ ...form, maintenanceAmount: event.target.value })
              }
              required
            />
          </div>
          <div>
            <label className="field-label">Water charges</label>
            <input
              className="field-input"
              type="number"
              min="0"
              value={form.waterCharge}
              onChange={(event) =>
                setForm({ ...form, waterCharge: event.target.value })
              }
              required
            />
          </div>
          <div>
            <label className="field-label">Electricity charges</label>
            <input
              className="field-input"
              type="number"
              min="0"
              value={form.electricityCharge}
              onChange={(event) =>
                setForm({ ...form, electricityCharge: event.target.value })
              }
            />
          </div>
          <div>
            <label className="field-label">Parking charges</label>
            <input
              className="field-input"
              type="number"
              min="0"
              value={form.parkingCharge}
              onChange={(event) =>
                setForm({ ...form, parkingCharge: event.target.value })
              }
            />
          </div>
          <div>
            <label className="field-label">Penalty</label>
            <input
              className="field-input"
              type="number"
              min="0"
              value={form.penalty}
              onChange={(event) =>
                setForm({ ...form, penalty: event.target.value })
              }
            />
          </div>
          <div>
            <label className="field-label">Other charges</label>
            <input
              className="field-input"
              type="number"
              min="0"
              value={form.otherCharge}
              onChange={(event) =>
                setForm({ ...form, otherCharge: event.target.value })
              }
            />
          </div>
          <div className="sm:col-span-2">
            <label className="field-label">Remarks (optional)</label>
            <input
              className="field-input"
              value={form.remarks}
              onChange={(event) =>
                setForm({ ...form, remarks: event.target.value })
              }
            />
          </div>
          <div className="sm:col-span-2 rounded-xl bg-slate-50 p-4 text-right">
            <span className="text-sm font-semibold text-slate-500">
              Total bill:{" "}
            </span>
            <span className="text-xl font-extrabold text-slate-900">
              {formatCurrency(
                Number(form.maintenanceAmount) +
                  Number(form.waterCharge) +
                  Number(form.electricityCharge) +
                  Number(form.parkingCharge) +
                  Number(form.penalty) +
                  Number(form.otherCharge),
              )}
            </span>
          </div>
          <div className="sm:col-span-2 flex justify-end gap-3">
            <button
              type="button"
              className="btn-secondary"
              onClick={() => setBillOpen(false)}
            >
              Cancel
            </button>
            <button className="btn-primary" disabled={busy}>
              {busy ? "Saving…" : editingBill ? "Save bill" : "Generate bill"}
            </button>
          </div>
        </form>
      </Modal>

      <Modal
        open={Boolean(paymentBill)}
        onClose={() => setPaymentBill(null)}
        title={
          paymentFlow === "OFFLINE"
            ? "Record offline payment"
            : canManage
              ? "Pay online for resident"
              : "Pay maintenance bill"
        }
        description={
          paymentFlow === "OFFLINE"
            ? "Record cash or cheque received by the society office."
            : canManage
              ? "Use Razorpay to securely pay this resident's outstanding bill."
              : "Complete this payment securely through Razorpay."
        }
        maxWidth="max-w-md"
      >
        {paymentBill && (
          <div>
            <div className="rounded-2xl bg-slate-50 p-5 text-center">
              <p className="text-sm font-semibold text-slate-500">
                Outstanding balance
              </p>
              <p className="mt-2 text-3xl font-extrabold text-slate-900">
                {formatCurrency(paymentBill.remaining ?? paymentBill.total)}
              </p>
              <p className="mt-2 text-xs text-slate-500">
                {paymentBill.billNo} · {paymentBill.flat}
              </p>
            </div>
            {paymentFlow === "OFFLINE" ? (
              <div className="mt-5 space-y-4">
                <div>
                  <label className="field-label">Amount received</label>
                  <input
                    className="field-input"
                    type="number"
                    min="0.01"
                    max={paymentBill.remaining ?? paymentBill.total}
                    step="0.01"
                    value={paymentAmount}
                    onChange={(event) => setPaymentAmount(event.target.value)}
                    required
                  />
                </div>
                <div>
                  <label className="field-label">Offline payment mode</label>
                  <select
                    className="field-input"
                    value={paymentMode}
                    onChange={(event) => setPaymentMode(event.target.value)}
                  >
                    <option value="CASH">Cash</option>
                    <option value="CHEQUE">Cheque</option>
                  </select>
                </div>
                <div>
                  <label className="field-label">
                    {paymentMode === "CHEQUE"
                      ? "Cheque number (required)"
                      : "Cash receipt number (optional)"}
                  </label>
                  <input
                    className="field-input"
                    value={transactionReference}
                    onChange={(event) =>
                      setTransactionReference(event.target.value)
                    }
                    placeholder={
                      paymentMode === "CHEQUE"
                        ? "Enter cheque number"
                        : "Enter society receipt number"
                    }
                  />
                </div>
                {paymentMode === "CHEQUE" && (
                  <div className="grid gap-4 sm:grid-cols-2">
                    <div>
                      <label className="field-label">Bank name</label>
                      <input
                        className="field-input"
                        value={bankName}
                        onChange={(event) => setBankName(event.target.value)}
                        placeholder="e.g. State Bank of India"
                        required
                      />
                    </div>
                    <div>
                      <label className="field-label">Cheque date</label>
                      <input
                        className="field-input"
                        type="date"
                        value={chequeDate}
                        onChange={(event) => setChequeDate(event.target.value)}
                        required
                      />
                    </div>
                  </div>
                )}
                <div>
                  <label className="field-label">Remarks (optional)</label>
                  <textarea
                    className="field-input min-h-20 resize-y"
                    value={paymentRemarks}
                    onChange={(event) => setPaymentRemarks(event.target.value)}
                    placeholder="Add collection notes"
                  />
                </div>
                <p className="rounded-xl bg-slate-50 px-4 py-3 text-xs leading-5 text-slate-600">
                  {paymentMode === "CHEQUE"
                    ? "The cheque will remain pending until an authorized manager marks it cleared or bounced."
                    : "The cash payment is confirmed immediately. A receipt reference is generated automatically when left blank."}
                </p>
              </div>
            ) : (
              <div className="mt-5 rounded-2xl border border-emerald-200 bg-emerald-50 p-4">
                <p className="font-extrabold text-emerald-900">
                  Secure Razorpay Checkout
                </p>
                <p className="mt-1 text-sm leading-6 text-emerald-800">
                  Choose UPI, debit or credit card, or netbanking in the
                  Razorpay payment window. Your bill is updated only after
                  secure verification.
                </p>
                {canManage && (
                  <p className="mt-2 rounded-xl bg-white/80 px-3 py-2 text-xs font-semibold text-emerald-900">
                    You are paying {paymentBill.billNo} for {paymentBill.residentName}. The verified payment remains linked to this resident's bill.
                  </p>
                )}
                <div className="mt-3 flex flex-wrap gap-2 text-xs font-bold text-emerald-700">
                  <span className="rounded-full bg-white px-3 py-1">UPI</span>
                  <span className="rounded-full bg-white px-3 py-1">Cards</span>
                  <span className="rounded-full bg-white px-3 py-1">
                    Netbanking
                  </span>
                </div>
              </div>
            )}
            <div className="mt-6 flex justify-end gap-3">
              <button
                className="btn-secondary"
                onClick={() => setPaymentBill(null)}
              >
                Cancel
              </button>
              <button
                className="btn-primary"
                onClick={
                  paymentFlow === "OFFLINE" ? payBill : payWithRazorpay
                }
                disabled={busy || !Number(paymentAmount)}
              >
                {busy
                  ? "Processing…"
                  : paymentFlow === "OFFLINE"
                    ? paymentMode === "CHEQUE"
                      ? "Submit cheque for clearance"
                      : "Record cash payment"
                    : "Pay securely with Razorpay"}
              </button>
            </div>
          </div>
        )}
      </Modal>

      <Modal
        open={Boolean(receiptBill)}
        onClose={() => setReceiptBill(null)}
        title="Payment receipt"
        description="Digital receipt generated by Smart Society Connect."
        maxWidth="max-w-md"
      >
        {receiptBill && (
          <div className="rounded-2xl border border-dashed border-slate-300 p-5">
            <div className="text-center">
              <p className="text-xs font-extrabold uppercase tracking-[0.2em] text-emerald-600">
                Payment successful
              </p>
              <p className="mt-2 text-3xl font-extrabold text-slate-900">
                {formatCurrency(receiptBill.paidAmount ?? receiptBill.total)}
              </p>
            </div>
            <div className="mt-6 space-y-3 border-t border-slate-200 pt-5 text-sm">
              {[
                ["Receipt / Bill", receiptBill.billNo],
                ["Resident", receiptBill.residentName],
                ["Flat", receiptBill.flat],
                ["Billing month", receiptBill.month],
                ["Paid on", formatDateTime(receiptBill.paidAt)],
                ["Payment mode", receiptBill.paymentMode],
                [
                  "Transaction reference",
                  receiptBill.transactionReference || "—",
                ],
                [
                  "Recorded by",
                  receiptBill.recordedByName || "Razorpay / system",
                ],
                [
                  "Remaining balance",
                  formatCurrency(receiptBill.remaining || 0),
                ],
              ].map(([label, value]) => (
                <div key={label} className="flex justify-between gap-4">
                  <span className="text-slate-500">{label}</span>
                  <span className="text-right font-bold text-slate-800 break-all">
                    {value}
                  </span>
                </div>
              ))}
            </div>
            <button
              className="btn-secondary mt-6 w-full"
              onClick={() => window.print()}
            >
              <Download className="h-4 w-4" /> Print receipt
            </button>
          </div>
        )}
      </Modal>

      <Modal
        open={paymentHistoryOpen}
        onClose={() => setPaymentHistoryOpen(false)}
        title={paymentHistoryTitle}
        description="Recorded payment transactions from the billing API."
        maxWidth="max-w-2xl"
      >
        {paymentHistoryLoading ? (
          <LoadingState />
        ) : selectedPayment ? (
          <div>
            <button
              type="button"
              className="text-sm font-bold text-emerald-700"
              onClick={() => setSelectedPayment(null)}
            >
              ← Back to payments
            </button>
            <div className="mt-4 space-y-3 rounded-2xl bg-slate-50 p-5 text-sm">
              {[
                ["Payment ID", selectedPayment.paymentId],
                ["Billing ID", selectedPayment.billingId],
                ["Amount", formatCurrency(selectedPayment.amountPaid)],
                ["Mode", selectedPayment.paymentMode],
                ["Reference", selectedPayment.transactionReference || "—"],
                ["Bank", selectedPayment.bankName || "—"],
                [
                  "Cheque date",
                  selectedPayment.chequeDate
                    ? formatDate(selectedPayment.chequeDate)
                    : "—",
                ],
                ["Status", selectedPayment.paymentStatus],
                ["Date", formatDateTime(selectedPayment.paymentDate)],
                [
                  "Recorded by",
                  selectedPayment.recordedByName || "Razorpay / system",
                ],
                ["Remarks", selectedPayment.remarks || "—"],
              ].map(([label, value]) => (
                <div key={label} className="flex justify-between gap-4">
                  <span className="text-slate-500">{label}</span>
                  <span className="text-right font-bold text-slate-800 break-all">
                    {value}
                  </span>
                </div>
              ))}
            </div>
            {canManage &&
              selectedPayment.paymentMode === "CHEQUE" &&
              selectedPayment.paymentStatus === "PENDING" && (
                <div className="mt-4 flex justify-end gap-3">
                  <button
                    type="button"
                    className="btn-secondary text-rose-700"
                    disabled={busy}
                    onClick={() =>
                      updateChequeStatus(selectedPayment, "FAILED")
                    }
                  >
                    Mark bounced
                  </button>
                  <button
                    type="button"
                    className="btn-primary"
                    disabled={busy}
                    onClick={() =>
                      updateChequeStatus(selectedPayment, "SUCCESS")
                    }
                  >
                    Mark cleared
                  </button>
                </div>
              )}
          </div>
        ) : paymentHistory.length === 0 ? (
          <EmptyState
            title="No payments found"
            description="There are no recorded payments for this selection."
          />
        ) : (
          <div className="divide-y divide-slate-100">
            {paymentHistory.map((payment) => (
              <button
                type="button"
                key={payment.paymentId}
                onClick={() => openPaymentDetails(payment.paymentId)}
                className="flex w-full items-center justify-between gap-4 py-4 text-left hover:bg-slate-50"
              >
                <span>
                  <span className="block font-bold text-slate-800">
                    {formatCurrency(payment.amountPaid)} · {payment.paymentMode}
                  </span>
                  <span className="mt-1 block text-xs text-slate-500">
                    {formatDateTime(payment.paymentDate)} ·{" "}
                    {payment.transactionReference || "No reference"}
                  </span>
                </span>
                <Badge value={payment.paymentStatus} />
              </button>
            ))}
          </div>
        )}
      </Modal>
      <Modal
        open={Boolean(billDetails)}
        onClose={() => setBillDetails(null)}
        title="Maintenance invoice"
        description="Itemized bill generated by Smart Society Connect."
        maxWidth="max-w-md"
      >
        {billDetails && (
          <div className="rounded-2xl border border-dashed border-slate-300 p-5">
            <div className="text-center">
              <p className="text-xs font-extrabold uppercase tracking-[0.2em] text-emerald-600">
                Maintenance invoice
              </p>
              <p className="mt-2 text-2xl font-extrabold text-slate-900">
                {formatCurrency(billDetails.total)}
              </p>
              <p className="mt-1 text-xs text-slate-500">
                Bill {billDetails.billNo} · {billDetails.month}
              </p>
            </div>
            <div className="mt-6 space-y-3 border-t border-slate-200 pt-5 text-sm">
              {[
                ["Resident", billDetails.residentName],
                ["Flat", billDetails.flat],
                ["Maintenance", formatCurrency(billDetails.maintenance)],
                ["Water", formatCurrency(billDetails.water)],
                ["Electricity", formatCurrency(billDetails.electricity)],
                ["Parking", formatCurrency(billDetails.parking)],
                ["Penalty", formatCurrency(billDetails.penalty)],
                ["Other charges", formatCurrency(billDetails.otherCharge)],
                ["Due date", formatDate(billDetails.dueDate)],
                ["Status", billDetails.status],
              ].map(([label, value]) => (
                <div key={label} className="flex justify-between gap-4">
                  <span className="text-slate-500">{label}</span>
                  <span className="text-right font-bold text-slate-800">
                    {value}
                  </span>
                </div>
              ))}
            </div>
            <div className="mt-5 space-y-2 border-t border-slate-200 pt-4 text-base">
              <div className="flex justify-between">
                <span className="font-bold text-slate-700">Total billed</span>
                <span className="font-extrabold text-slate-900">
                  {formatCurrency(billDetails.total)}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="font-bold text-emerald-700">
                  Payments received
                </span>
                <span className="font-extrabold text-emerald-700">
                  {formatCurrency(billDetails.paidAmount)}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="font-bold text-rose-700">Balance due</span>
                <span className="font-extrabold text-rose-700">
                  {formatCurrency(billDetails.balance)}
                </span>
              </div>
            </div>
            <button
              className="btn-secondary mt-6 w-full"
              onClick={() => window.print()}
            >
              <Download className="h-4 w-4" /> Print invoice
            </button>
          </div>
        )}
      </Modal>
      <ConfirmDialog
        open={Boolean(deletingBill)}
        onClose={() => setDeletingBill(null)}
        onConfirm={confirmDeleteBill}
        busy={busy}
        title="Delete bill?"
        description={`Bill ${deletingBill?.billNo || ""} will be permanently removed.`}
      />
    </div>
  );
}
