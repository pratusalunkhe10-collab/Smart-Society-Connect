import {
  Building2,
  CalendarDays,
  Megaphone,
  CreditCard,
  FileCheck2,
  LayoutDashboard,
  MessageSquareWarning,
  ShieldCheck,
  UserRoundCog,
  UserCheck,
  UsersRound,
} from 'lucide-react';

export const roleLabels = {
  ADMIN: 'Administrator',
  RESIDENT: 'Resident',
  SECURITY: 'Security Guard',
  ACCOUNTANT: 'Accountant',
  SECRETARY: 'Secretary',
};

const all = ['ADMIN', 'RESIDENT', 'SECURITY', 'ACCOUNTANT', 'SECRETARY'];

export const navigation = [
  { label: 'Dashboard', to: '/app/dashboard', icon: LayoutDashboard, roles: all },
  { label: 'Residents', to: '/app/residents', icon: UsersRound, roles: ['ADMIN', 'SECRETARY', 'RESIDENT'] },
  { label: 'User approvals', to: '/app/users', icon: UserCheck, roles: ['ADMIN'] },
  { label: 'Visitors', to: '/app/visitors', icon: ShieldCheck, roles: ['ADMIN', 'RESIDENT', 'SECURITY', 'SECRETARY'] },
  { label: 'Complaints', to: '/app/complaints', icon: MessageSquareWarning, roles: ['ADMIN', 'RESIDENT', 'SECRETARY'] },
  { label: 'Billing', roleLabel: { ACCOUNTANT: 'Billing & payments' }, to: '/app/billing', icon: CreditCard, roles: ['ADMIN', 'RESIDENT', 'ACCOUNTANT', 'SECRETARY'] },
  { label: 'Documents', roleLabel: { ACCOUNTANT: 'Expenses & records' }, to: '/app/documents', icon: FileCheck2, roles: all },
  { label: 'Meetings', to: '/app/meetings', icon: CalendarDays, roles: all },
  { label: 'Announcements', to: '/app/announcements', icon: Megaphone, roles: all },
  { label: 'My Profile', to: '/app/profile', icon: UserRoundCog, roles: all },
];

export const roleIcon = Building2;
