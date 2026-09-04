import {
  CalendarDays,
  CheckCircle2,
  Edit3,
  ExternalLink,
  MapPin,
  PlayCircle,
  Plus,
  Search,
  Trash2,
  UsersRound,
  Video,
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
import { formatDateTime } from "../utils/format.js";
import useLiveRefresh from "../hooks/useLiveRefresh.js";

const emptyForm = {
  title: "",
  type: "COMMITTEE",
  date: new Date(Date.now() + 3 * 86400000).toISOString().slice(0, 16),
  endTime: "18:00",
  venue: "Club House",
  audience: "COMMITTEE_ONLY",
  mode: "IN_PERSON",
  meetingLink: "",
  agenda: "",
};
const typeLabel = (type) => String(type || "GENERAL_BODY").replaceAll("_", " ");
const canJoinOnline = (meeting) =>
  ["ONLINE", "HYBRID"].includes(meeting?.mode || meeting?.meetingMode) &&
  Boolean(meeting?.meetingLink) &&
  ["UPCOMING", "IN_PROGRESS"].includes(meeting?.status);
const toRequest = (form) => ({
  title: form.title,
  description: form.agenda,
  agenda: form.agenda,
  meetingType: form.type,
  meetingDate: form.date.slice(0, 10),
  startTime: form.date.slice(11, 16),
  endTime: form.endTime,
  venue: form.venue,
  audience: form.audience,
  meetingMode: form.mode,
  meetingLink: form.meetingLink || null,
});

export default function MeetingsPage() {
  const { user } = useAuth();
  const { push } = useToast();
  const canManage = ["ADMIN", "SECRETARY"].includes(user?.role);
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState("ALL");
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [details, setDetails] = useState(null);
  const [attendance, setAttendance] = useState([]);
  const [counts, setCounts] = useState({});
  const [myRsvp, setMyRsvp] = useState(null);
  const [minutes, setMinutes] = useState(null);
  const [minutesSummary, setMinutesSummary] = useState("");
  const [history, setHistory] = useState(null);
  const [deleting, setDeleting] = useState(null);
  const [cancelling, setCancelling] = useState(null);

  const load = async ({ silent = false } = {}) => {
    if (!silent) setLoading(true);
    try {
      if (query.trim())
        setItems(await societyService.searchMeetings(query.trim()));
      else if (status === "UPCOMING")
        setItems(await societyService.listUpcomingMeetings());
      else if (status === "COMPLETED")
        setItems(await societyService.listCompletedMeetings());
      else if (status !== "ALL")
        setItems(await societyService.listMeetingsByStatus(status));
      else setItems(await societyService.listMeetings());
    } catch (error) {
      push(error.message || "Unable to load meetings.", "error");
    } finally {
      if (!silent) setLoading(false);
    }
  };
  useEffect(() => {
    load();
  }, [status, query]);
  useLiveRefresh(() => load({ silent: true }), {
    refreshKey: `${status}-${query}-meetings`,
  });

  const filtered = useMemo(
    () =>
      items.filter(
        (item) =>
          `${item.title} ${item.type} ${item.venue} ${item.agenda || ""}`
            .toLowerCase()
            .includes(query.toLowerCase()) &&
          (status === "ALL" || item.status === status),
      ),
    [items, query, status],
  );

  const changeStatus = async (item, nextStatus, message) => {
    setBusy(true);
    try {
      await societyService.updateMeetingStatus(item.id, nextStatus);
      push(message);
      await load();
    } catch (error) {
      push(error.message || "Unable to update meeting.", "error");
    } finally {
      setBusy(false);
    }
  };
  const openCreate = () => {
    setEditing(null);
    setForm(emptyForm);
    setModalOpen(true);
  };
  const openEdit = async (item) => {
    setBusy(true);
    try {
      const meeting = await societyService.getMeeting(item.id);
      setEditing(meeting);
      setForm({
        ...emptyForm,
        ...meeting,
        type: meeting.meetingType || meeting.type || "GENERAL_BODY",
        mode: meeting.mode || meeting.meetingMode || "IN_PERSON",
        meetingLink: meeting.meetingLink || "",
        date: String(meeting.date).slice(0, 16),
        endTime: meeting.endTime || "18:00",
      });
      setModalOpen(true);
    } catch (error) {
      push(error.message || "Unable to load meeting for editing.", "error");
    } finally {
      setBusy(false);
    }
  };
  const submit = async (event) => {
    event.preventDefault();
    if (form.endTime <= form.date.slice(11, 16))
      return push("End time must be after the start time.", "error");
    if (["ONLINE", "HYBRID"].includes(form.mode) && !form.meetingLink.trim())
      return push(
        "Add the secure meeting link for an online or hybrid meeting.",
        "error",
      );
    setBusy(true);
    try {
      if (editing) {
        await societyService.updateMeeting(editing.id, toRequest(form));
        push("Meeting updated and audience notified.");
      } else {
        await societyService.createMeeting(toRequest(form));
        push("Meeting scheduled and audience notified.");
      }
      setModalOpen(false);
      await load();
    } catch (error) {
      push(error.message || "Unable to save meeting.", "error");
    } finally {
      setBusy(false);
    }
  };
  const cancel = async () => {
    setBusy(true);
    try {
      await societyService.cancelMeeting(cancelling.id);
      push("Meeting cancelled and audience notified.");
      setCancelling(null);
      await load();
    } catch (error) {
      push(error.message || "Unable to cancel meeting.", "error");
    } finally {
      setBusy(false);
    }
  };
  const deleteMeeting = async () => {
    setBusy(true);
    try {
      await societyService.deleteMeeting(deleting.id);
      push("Meeting deleted.");
      setDeleting(null);
      await load();
    } catch (error) {
      push(error.message || "Unable to delete meeting.", "error");
    } finally {
      setBusy(false);
    }
  };
  const openDetails = async (item) => {
    setDetails(item);
    setAttendance([]);
    setCounts({});
    setMyRsvp(null);
    setMinutes(null);
    setMinutesSummary("");
    setBusy(true);
    try {
      const meeting = await societyService.getMeeting(item.id);
      setDetails(meeting);
      if (canManage) {
        const [attendees, totals] = await Promise.all([
          societyService.listMeetingAttendance(item.id),
          Promise.all(
            ["GOING", "MAYBE", "NOT_GOING", "PENDING"].map(async (value) => [
              value,
              await societyService.getMeetingAttendanceCount(item.id, value),
            ]),
          ),
        ]);
        setAttendance(attendees);
        setCounts(Object.fromEntries(totals));
      } else if (user?.role === "RESIDENT") {
        const resident = await societyService.getResidentForUser(user.id);
        setMyRsvp(
          await societyService.getMyMeetingAttendance(item.id, resident.id),
        );
      }
      try {
        const result = await societyService.getMeetingMinutes(item.id);
        setMinutes(result);
        setMinutesSummary(result.summary || "");
      } catch {
        /* minutes are optional */
      }
    } catch (error) {
      push(error.message || "Unable to load meeting details.", "error");
    } finally {
      setBusy(false);
    }
  };
  const saveRsvp = async (attendanceStatus) => {
    setBusy(true);
    try {
      const resident = await societyService.getResidentForUser(user.id);
      const payload = { attendanceStatus, remarks: null };
      if (myRsvp)
        await societyService.updateMeetingAttendance(
          details.id,
          resident.id,
          payload,
        );
      else
        await societyService.submitMeetingAttendance(
          details.id,
          resident.id,
          payload,
        );
      push("Your RSVP has been saved.");
      await openDetails(details);
    } catch (error) {
      push(error.message || "Unable to save RSVP.", "error");
    } finally {
      setBusy(false);
    }
  };
  const withdraw = async () => {
    setBusy(true);
    try {
      const resident = await societyService.getResidentForUser(user.id);
      await societyService.deleteMeetingAttendance(details.id, resident.id);
      push("Your RSVP has been withdrawn.");
      await openDetails(details);
    } catch (error) {
      push(error.message || "Unable to withdraw RSVP.", "error");
    } finally {
      setBusy(false);
    }
  };
  const saveMinutes = async () => {
    setBusy(true);
    try {
      const result = await societyService.saveMeetingMinutes(details.id, {
        summary: minutesSummary,
        uploadedFile: null,
      });
      setMinutes(result);
      push("Meeting minutes saved.");
    } catch (error) {
      push(error.message || "Unable to save minutes.", "error");
    } finally {
      setBusy(false);
    }
  };
  const openHistory = async () => {
    setBusy(true);
    try {
      const resident = await societyService.getResidentForUser(user.id);
      setHistory(
        await societyService.getResidentAttendanceHistory(resident.id),
      );
    } catch (error) {
      push(error.message || "Unable to load RSVP history.", "error");
    } finally {
      setBusy(false);
    }
  };

  const lifecycleCards = [
    [
      "Upcoming",
      items.filter((item) => item.status === "UPCOMING").length,
      "bg-violet-50 text-violet-700",
    ],
    [
      "In progress",
      items.filter((item) => item.status === "IN_PROGRESS").length,
      "bg-blue-50 text-blue-700",
    ],
    [
      "Completed",
      items.filter((item) => item.status === "COMPLETED").length,
      "bg-emerald-50 text-emerald-700",
    ],
    [
      "Attendance responses",
      items.reduce((sum, item) => sum + Number(item.attendees || 0), 0),
      "bg-sky-50 text-sky-700",
    ],
  ];

  return (
    <div>
      <PageHeader
        eyebrow="Meeting management"
        title="Meetings & notices"
        subtitle={
          canManage
            ? "Schedule meetings, notify the right audience, collect attendance responses, and publish minutes."
            : user?.role === "RESIDENT"
              ? "View society meetings, respond to invitations, and read published minutes."
              : "View published society meeting notices and minutes."
        }
        actions={
          canManage ? (
            <button className="btn-primary" onClick={openCreate}>
              <Plus className="h-4 w-4" /> Schedule meeting
            </button>
          ) : user?.role === "RESIDENT" ? (
            <button className="btn-secondary" onClick={openHistory}>
              My RSVP history
            </button>
          ) : null
        }
      />
      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {lifecycleCards.map(([label, value, tone]) => (
          <div key={label} className="panel flex items-center gap-4 p-4">
            <div
              className={`grid h-11 w-11 place-items-center rounded-xl ${tone}`}
            >
              <CalendarDays className="h-5 w-5" />
            </div>
            <div>
              <p className="text-2xl font-extrabold text-slate-900">{value}</p>
              <p className="text-xs font-semibold text-slate-500">{label}</p>
            </div>
          </div>
        ))}
      </section>
      <section className="panel mt-6 overflow-hidden">
        <div className="panel-header">
          <div>
            <h2 className="font-extrabold text-slate-900">Meeting calendar</h2>
            <p className="text-xs text-slate-500">
              Committee, general body, emergency, and vendor meetings
            </p>
          </div>
          <div className="flex w-full flex-col gap-2 sm:w-auto sm:flex-row">
            <label className="relative block sm:w-64">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
              <input
                className="field-input pl-9"
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder="Search meetings"
              />
            </label>
            <select
              className="field-input sm:w-40"
              value={status}
              onChange={(event) => setStatus(event.target.value)}
            >
              <option value="ALL">All status</option>
              <option value="UPCOMING">Upcoming</option>
              <option value="IN_PROGRESS">In progress</option>
              <option value="COMPLETED">Completed</option>
              <option value="CANCELLED">Cancelled</option>
            </select>
          </div>
        </div>
        {loading ? (
          <div className="p-5">
            <LoadingState />
          </div>
        ) : filtered.length === 0 ? (
          <EmptyState
            title="No meetings found"
            description="Try another status or search term."
          />
        ) : (
          <div className="grid gap-4 p-5 md:grid-cols-2 xl:grid-cols-3">
            {filtered.map((item) => (
              <article
                key={item.id}
                className="rounded-2xl border border-slate-200 bg-white p-5 transition hover:-translate-y-0.5 hover:border-emerald-200 hover:shadow-lg"
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="rounded-xl bg-violet-50 p-2.5 text-violet-700">
                    <CalendarDays className="h-5 w-5" />
                  </div>
                  <Badge value={item.status} />
                </div>
                <p className="mt-4 text-xs font-extrabold uppercase tracking-wider text-emerald-700">
                  {item.audience === "COMMITTEE_ONLY"
                    ? "Committee only"
                    : "All residents"}{" "}
                  · {typeLabel(item.type)}
                </p>
                <h3 className="mt-1 text-lg font-extrabold text-slate-900">
                  {item.title}
                </h3>
                <div className="mt-4 space-y-2 text-sm text-slate-500">
                  <p className="flex items-center gap-2">
                    <CalendarDays className="h-4 w-4" />{" "}
                    {formatDateTime(item.date)}
                  </p>
                  <p className="flex items-center gap-2">
                    <MapPin className="h-4 w-4" /> {item.venue}
                  </p>
                  <p className="flex items-center gap-2">
                    <UsersRound className="h-4 w-4" /> {item.attendees || 0}{" "}
                    attendance responses
                  </p>
                </div>
                <p className="mt-4 line-clamp-3 text-sm leading-6 text-slate-500">
                  {item.agenda || "No agenda published."}
                </p>
                <div className="mt-5 flex flex-wrap gap-2 border-t border-slate-100 pt-4">
                  <button
                    className="btn-secondary px-3 py-2"
                    onClick={() => openDetails(item)}
                  >
                    View details
                  </button>
                  {canJoinOnline(item) && (
                    <a
                      href={item.meetingLink}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="inline-flex items-center gap-1 rounded-xl bg-rose-600 px-3 py-2 text-xs font-extrabold text-white shadow-sm transition hover:bg-rose-700"
                    >
                      <Video className="h-4 w-4" />
                      Join meeting
                      <ExternalLink className="h-3.5 w-3.5" />
                    </a>
                  )}
                  {canManage && item.status === "UPCOMING" && (
                    <button
                      disabled={busy}
                      className="inline-flex items-center gap-1 rounded-xl bg-blue-50 px-3 py-2 text-xs font-bold text-blue-700 hover:bg-blue-100"
                      onClick={() =>
                        changeStatus(
                          item,
                          "ONGOING",
                          "Meeting started and invited members notified.",
                        )
                      }
                    >
                      <PlayCircle className="h-4 w-4" /> Start
                    </button>
                  )}
                  {canManage && item.status === "IN_PROGRESS" && (
                    <button
                      disabled={busy}
                      className="inline-flex items-center gap-1 rounded-xl bg-emerald-50 px-3 py-2 text-xs font-bold text-emerald-700 hover:bg-emerald-100"
                      onClick={() =>
                        changeStatus(
                          item,
                          "COMPLETED",
                          "Meeting completed and invited members notified.",
                        )
                      }
                    >
                      <CheckCircle2 className="h-4 w-4" /> Complete
                    </button>
                  )}
                  {canManage &&
                    ["UPCOMING", "IN_PROGRESS"].includes(item.status) && (
                      <button
                        disabled={busy}
                        className="rounded-xl px-2 py-1 text-xs font-bold text-amber-700 hover:bg-amber-50"
                        onClick={() => setCancelling(item)}
                      >
                        Cancel
                      </button>
                    )}
                  {canManage && (
                    <button
                      disabled={busy}
                      className="rounded-xl p-2 text-slate-500 hover:bg-blue-50 hover:text-blue-700"
                      onClick={() => openEdit(item)}
                      title="Edit"
                    >
                      <Edit3 className="h-4 w-4" />
                    </button>
                  )}
                  {user?.role === "ADMIN" && (
                    <button
                      className="rounded-xl p-2 text-rose-600 hover:bg-rose-50"
                      onClick={() => setDeleting(item)}
                      title="Delete"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  )}
                </div>
              </article>
            ))}
          </div>
        )}
      </section>
      <Modal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        title={editing ? "Edit meeting" : "Schedule meeting"}
        description="Publish a clear notice with the right audience, schedule, and access method."
      >
        <form onSubmit={submit} className="grid gap-4 sm:grid-cols-2">
          <div className="sm:col-span-2">
            <label className="field-label">Meeting title</label>
            <input
              className="field-input"
              value={form.title}
              onChange={(event) =>
                setForm({ ...form, title: event.target.value })
              }
              required
            />
          </div>
          <div>
            <label className="field-label">Meeting type</label>
            <select
              className="field-input"
              value={form.type}
              onChange={(event) =>
                setForm({ ...form, type: event.target.value })
              }
            >
              {[
                "COMMITTEE",
                "GENERAL_BODY",
                "EMERGENCY",
                "VENDOR_REVIEW",
                "OTHER",
              ].map((type) => (
                <option key={type} value={type}>
                  {typeLabel(type)}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="field-label">Notify and show to</label>
            <select
              className="field-input"
              value={form.audience}
              onChange={(event) =>
                setForm({ ...form, audience: event.target.value })
              }
            >
              <option value="COMMITTEE_ONLY">Committee members only</option>
              <option value="ALL_RESIDENTS">All residents</option>
            </select>
          </div>
          <div>
            <label className="field-label">Attendance mode</label>
            <select
              className="field-input"
              value={form.mode}
              onChange={(event) =>
                setForm({ ...form, mode: event.target.value })
              }
            >
              <option value="IN_PERSON">In person</option>
              <option value="ONLINE">Online</option>
              <option value="HYBRID">Hybrid</option>
            </select>
          </div>
          <div>
            <label className="field-label">Venue / location</label>
            <input
              className="field-input"
              value={form.venue}
              onChange={(event) =>
                setForm({ ...form, venue: event.target.value })
              }
              placeholder={
                form.mode === "ONLINE" ? "Online meeting" : "Club House"
              }
              required
            />
          </div>
          {["ONLINE", "HYBRID"].includes(form.mode) && (
            <div className="sm:col-span-2">
              <label className="field-label">Secure meeting link</label>
              <input
                className="field-input"
                type="url"
                value={form.meetingLink}
                onChange={(event) =>
                  setForm({ ...form, meetingLink: event.target.value })
                }
                placeholder="https://meet.google.com/..."
                required
              />
              <p className="mt-1 text-xs text-slate-500">
                Visible only to the invited audience. Use an HTTPS Google Meet,
                Zoom, or Teams link.
              </p>
            </div>
          )}
          <div>
            <label className="field-label">Date & time</label>
            <input
              className="field-input"
              type="datetime-local"
              value={String(form.date).slice(0, 16)}
              onChange={(event) =>
                setForm({ ...form, date: event.target.value })
              }
              required
            />
          </div>
          <div>
            <label className="field-label">End time</label>
            <input
              className="field-input"
              type="time"
              value={form.endTime}
              onChange={(event) =>
                setForm({ ...form, endTime: event.target.value })
              }
              required
            />
          </div>
          <div className="sm:col-span-2">
            <label className="field-label">Agenda</label>
            <textarea
              className="field-input min-h-32 resize-y"
              value={form.agenda}
              onChange={(event) =>
                setForm({ ...form, agenda: event.target.value })
              }
              required
            />
          </div>
          <div className="sm:col-span-2 flex justify-end gap-3">
            <button
              type="button"
              className="btn-secondary"
              onClick={() => setModalOpen(false)}
            >
              Cancel
            </button>
            <button className="btn-primary" disabled={busy}>
              {busy ? "Saving…" : editing ? "Save changes" : "Publish meeting"}
            </button>
          </div>
        </form>
      </Modal>
      <Modal
        open={Boolean(details)}
        onClose={() => setDetails(null)}
        title={details?.title || "Meeting details"}
        description={
          details
            ? `${typeLabel(details.type)} meeting · ${formatDateTime(details.date)}`
            : ""
        }
        maxWidth="max-w-xl"
      >
        {details && (
          <div className="space-y-5">
            <div className={`grid gap-3 ${canManage ? "sm:grid-cols-2" : ""}`}>
              <div className="rounded-xl bg-slate-50 p-4">
                <p className="text-xs font-bold uppercase tracking-wider text-slate-400">
                  Venue
                </p>
                <p className="mt-1 font-bold text-slate-800">{details.venue}</p>
              </div>
              {canManage && (
                <div className="rounded-xl bg-slate-50 p-4">
                  <p className="text-xs font-bold uppercase tracking-wider text-slate-400">
                    Attendance responses
                  </p>
                  <p className="mt-1 font-bold text-slate-800">
                    {counts.GOING || 0} going · {counts.MAYBE || 0} maybe
                  </p>
                  <p className="mt-1 text-xs text-slate-500">
                    {counts.NOT_GOING || 0} not going · {counts.PENDING || 0}{" "}
                    pending
                  </p>
                </div>
              )}
            </div>
            {canJoinOnline(details) && (
              <div className="rounded-2xl border border-rose-200 bg-rose-50 p-4">
                <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                  <div>
                    <p className="flex items-center gap-2 font-extrabold text-rose-900">
                      <Video className="h-5 w-5" />
                      Online meeting access
                    </p>
                    <p className="mt-1 text-xs text-rose-700">
                      This secure link is available only to the invited audience.
                    </p>
                  </div>
                  <a
                    href={details.meetingLink}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="inline-flex shrink-0 items-center justify-center gap-2 rounded-xl bg-rose-600 px-4 py-3 text-sm font-extrabold text-white shadow-sm transition hover:bg-rose-700"
                  >
                    Join meeting
                    <ExternalLink className="h-4 w-4" />
                  </a>
                </div>
              </div>
            )}
            {user?.role === "RESIDENT" && (
              <div>
                <p className="text-sm font-extrabold text-slate-800">
                  Your RSVP{" "}
                  {myRsvp && <Badge value={myRsvp.attendanceStatus} />}
                </p>
                <p className="mt-1 text-xs text-slate-500">
                  RSVP tells the organizer whether you plan to attend.
                </p>
                <div className="mt-2 flex flex-wrap gap-2">
                  {["GOING", "MAYBE", "NOT_GOING"].map((value) => (
                    <button
                      key={value}
                      disabled={busy || details.status !== "UPCOMING"}
                      onClick={() => saveRsvp(value)}
                      className="btn-secondary px-3 py-2"
                    >
                      {value.replace("_", " ")}
                    </button>
                  ))}
                  {myRsvp && (
                    <button
                      disabled={busy || details.status !== "UPCOMING"}
                      onClick={withdraw}
                      className="btn-danger px-3 py-2"
                    >
                      Withdraw
                    </button>
                  )}
                </div>
                {details.status !== "UPCOMING" && (
                  <p className="mt-2 text-xs text-slate-500">
                    RSVP is closed because this meeting has started or ended.
                  </p>
                )}
              </div>
            )}
            <div>
              <p className="text-sm font-extrabold text-slate-800">Agenda</p>
              <p className="mt-2 rounded-2xl border border-slate-200 bg-white p-4 text-sm leading-7 text-slate-600">
                {details.agenda || "No agenda published."}
              </p>
            </div>
            {canManage && (
              <div>
                <p className="text-sm font-extrabold text-slate-800">
                  Resident RSVP responses
                </p>
                <div className="mt-2 max-h-32 divide-y overflow-auto rounded-xl border border-slate-200">
                  {attendance.length ? (
                    attendance.map((entry) => (
                      <div
                        key={entry.residentId}
                        className="flex items-center justify-between p-3 text-sm"
                      >
                        <span>{entry.residentName}</span>
                        <Badge value={entry.attendanceStatus} />
                      </div>
                    ))
                  ) : (
                    <p className="p-3 text-sm text-slate-500">
                      No responses yet.
                    </p>
                  )}
                </div>
              </div>
            )}
            <div>
              <p className="text-sm font-extrabold text-slate-800">
                Meeting minutes
              </p>
              {canManage && details.status === "COMPLETED" ? (
                <>
                  <textarea
                    className="field-input mt-2 min-h-28 resize-y"
                    value={minutesSummary}
                    onChange={(event) => setMinutesSummary(event.target.value)}
                    placeholder="Record meeting decisions and action items."
                  />
                  <button
                    disabled={busy || !minutesSummary.trim()}
                    onClick={saveMinutes}
                    className="btn-primary mt-2 px-3 py-2"
                  >
                    Save minutes
                  </button>
                </>
              ) : canManage ? (
                <p className="mt-2 rounded-xl bg-slate-50 p-3 text-sm text-slate-600">
                  Minutes can be recorded after the meeting is completed.
                </p>
              ) : (
                <p className="mt-2 rounded-xl bg-slate-50 p-3 text-sm text-slate-600">
                  {minutes?.summary || "Minutes have not been published yet."}
                </p>
              )}
            </div>
            <div className="flex items-center justify-between border-t border-slate-100 pt-4">
              <span className="text-xs text-slate-400">
                Created by {details.createdBy}
              </span>
              <Badge value={details.status} />
            </div>
          </div>
        )}
      </Modal>
      <Modal
        open={Boolean(history)}
        onClose={() => setHistory(null)}
        title="My RSVP history"
        description="Your own meeting responses."
      >
        {history &&
          (history.length ? (
            <div className="divide-y divide-slate-100">
              {history.map((entry) => (
                <div
                  key={`${entry.meetingUuid || entry.meetingId}-${entry.respondedAt}`}
                  className="flex items-center justify-between gap-4 py-3"
                >
                  <span>
                    <span className="block font-bold text-slate-800">
                      {entry.meetingTitle || entry.meetingUuid || "Meeting"}
                    </span>
                    <span className="mt-1 block text-xs text-slate-500">
                      {formatDateTime(entry.respondedAt)}
                    </span>
                  </span>
                  <Badge value={entry.attendanceStatus} />
                </div>
              ))}
            </div>
          ) : (
            <EmptyState
              title="No RSVP history"
              description="Your meeting responses will appear here."
            />
          ))}
      </Modal>
      <ConfirmDialog
        open={Boolean(deleting)}
        onClose={() => setDeleting(null)}
        onConfirm={deleteMeeting}
        busy={busy}
        title="Delete meeting?"
        description={`The meeting “${deleting?.title || ""}” will be removed.`}
      />
      <ConfirmDialog
        open={Boolean(cancelling)}
        onClose={() => setCancelling(null)}
        onConfirm={cancel}
        busy={busy}
        title="Cancel meeting?"
        description={`The meeting “${cancelling?.title || ""}” will be cancelled and the audience notified.`}
      />
    </div>
  );
}
