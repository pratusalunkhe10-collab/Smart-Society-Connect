import { Check, Plus, ShieldX, UsersRound, X } from 'lucide-react';
import { useEffect, useState } from 'react';
import Badge from '../components/ui/Badge.jsx';
import EmptyState from '../components/ui/EmptyState.jsx';
import LoadingState from '../components/ui/LoadingState.jsx';
import PageHeader from '../components/ui/PageHeader.jsx';
import { useToast } from '../context/ToastContext.jsx';
import { API_ROUTES } from '../config/api.js';
import http, { unwrap } from '../services/http.js';
import { formatDate } from '../utils/format.js';

const roles = ['RESIDENT', 'SECURITY', 'SECRETARY', 'ACCOUNTANT', 'ADMIN'];

export default function AdminUsersPage() {
  const { push } = useToast();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState(null);
  const [status, setStatus] = useState('PENDING');
  const [selectedRoles, setSelectedRoles] = useState({});

  const load = async () => {
    setLoading(true);
    try {
      setUsers(unwrap(await http.get(API_ROUTES.adminUsers, { params: { status } })));
    } catch (error) {
      push(error.message || 'Unable to load user accounts.', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [status]);

  const roleFor = (user) => selectedRoles[user.id]
    || (user.approvalStatus === 'PENDING'
      ? 'RESIDENT'
      : roles.find((role) => !user.roles?.includes(role)) || 'RESIDENT');

  const save = async (user, action) => {
    setBusyId(user.id);
    try {
      const url = action === 'approve'
        ? `${API_ROUTES.adminUsers}/${user.id}/approve`
        : action === 'reject'
          ? `${API_ROUTES.adminUsers}/${user.id}/reject`
          : `${API_ROUTES.adminUsers}/${user.id}/role`;

      await http.put(url, ['approve', 'role'].includes(action) ? { role: roleFor(user) } : undefined);
      push(
        action === 'approve'
          ? `Account approved as ${roleFor(user)}.`
          : action === 'reject'
            ? 'User rejected.'
            : 'Additional role assigned.',
      );
      await load();
    } catch (error) {
      push(error.message || 'Unable to update this user.', 'error');
    } finally {
      setBusyId(null);
    }
  };

  const removeRole = async (user, role) => {
    setBusyId(user.id);
    try {
      await http.delete(`${API_ROUTES.adminUsers}/${user.id}/role/${role}`);
      push(`${role} role removed.`);
      await load();
    } catch (error) {
      push(error.message || 'Unable to remove this role.', 'error');
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div>
      <PageHeader
        eyebrow="Administration"
        title="User approvals & roles"
        subtitle="Review verified applicants and assign their initial system role during approval."
      />

      <section className="panel overflow-hidden">
        <div className="panel-header flex-wrap gap-3">
          <div className="flex items-center gap-3">
            <div className="rounded-xl bg-emerald-50 p-2.5 text-emerald-700">
              <UsersRound className="h-5 w-5" />
            </div>
            <div>
              <h2 className="font-extrabold text-slate-900">User accounts</h2>
              <p className="text-xs text-slate-500">
                Pending applicants have no access role until an administrator approves them.
              </p>
            </div>
          </div>
          <select
            className="field-input w-40"
            value={status}
            onChange={(event) => setStatus(event.target.value)}
          >
            <option value="PENDING">Pending</option>
            <option value="APPROVED">Approved</option>
            <option value="REJECTED">Rejected</option>
            <option value="">All users</option>
          </select>
        </div>

        {loading ? (
          <div className="p-6"><LoadingState /></div>
        ) : users.length === 0 ? (
          <EmptyState title="No user accounts found" description="There are no accounts in this status." />
        ) : (
          <div className="table-responsive">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Applicant</th>
                  <th>Mobile</th>
                  <th>Registered</th>
                  <th>Verification</th>
                  <th>Status</th>
                  <th>Assigned roles</th>
                  <th>{status === 'PENDING' ? 'Initial role' : 'Role selection'}</th>
                  <th className="text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {users.map((user) => (
                  <tr key={user.id}>
                    <td>
                      <div className="font-bold text-slate-900">{user.name}</div>
                      <div className="text-xs text-slate-500">{user.email}</div>
                    </td>
                    <td>{user.mobile}</td>
                    <td>{formatDate(user.registeredAt)}</td>
                    <td><Badge value={user.verified ? 'VERIFIED' : 'AWAITING OTP'} /></td>
                    <td><Badge value={user.approvalStatus || 'PENDING'} /></td>
                    <td>
                      <div className="flex min-w-40 flex-wrap gap-1.5">
                        {user.roles?.length ? user.roles.map((role) => {
                          const removalDisabled = busyId === user.id
                            || user.roles.length === 1;
                          return (
                            <span
                              key={role}
                              className="inline-flex items-center gap-1 rounded-full bg-rose-500/10 px-2 py-1 text-[11px] font-bold text-rose-500"
                            >
                              {role}
                              <button
                                type="button"
                                title={user.roles.length === 1 ? 'A user must retain at least one role' : `Remove ${role}`}
                                disabled={removalDisabled}
                                onClick={() => removeRole(user, role)}
                                className="rounded-full text-rose-400 hover:text-rose-200 disabled:cursor-not-allowed disabled:opacity-40"
                              >
                                <X className="h-3 w-3" />
                              </button>
                            </span>
                          );
                        }) : <span className="text-xs text-slate-500">No roles</span>}
                      </div>
                    </td>
                    <td>
                      {['PENDING', 'APPROVED'].includes(user.approvalStatus) ? (
                        <select
                          className="field-input min-w-32 py-2"
                          value={roleFor(user)}
                          onChange={(event) => setSelectedRoles({
                            ...selectedRoles,
                            [user.id]: event.target.value,
                          })}
                        >
                          {roles.map((role) => (
                            <option key={role} disabled={user.approvalStatus === 'APPROVED' && user.roles?.includes(role)}>{role}</option>
                          ))}
                        </select>
                      ) : (
                        <span className="inline-flex rounded-full bg-emerald-500/10 px-3 py-1.5 text-xs font-bold text-emerald-600">
                          No role assigned
                        </span>
                      )}
                    </td>
                    <td>
                      <div className="flex justify-end gap-2">
                        {user.approvalStatus === 'PENDING' && (
                          <>
                            <button
                              title={user.verified ? `Approve as ${roleFor(user)}` : 'The user must verify OTP first'}
                              disabled={busyId === user.id || !user.verified}
                              className="btn-primary px-3 py-2"
                              onClick={() => save(user, 'approve')}
                            >
                              <Check className="h-4 w-4" /> Approve as {roleFor(user)}
                            </button>
                            <button
                              disabled={busyId === user.id}
                              className="btn-danger px-3 py-2"
                              onClick={() => save(user, 'reject')}
                            >
                              <ShieldX className="h-4 w-4" /> Reject
                            </button>
                          </>
                        )}
                        {user.approvalStatus === 'APPROVED' && (
                          <button
                            disabled={busyId === user.id || user.roles?.length === roles.length}
                            className="btn-secondary px-3 py-2"
                            onClick={() => save(user, 'role')}
                          >
                            <Plus className="h-4 w-4" /> Add role
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
    </div>
  );
}
