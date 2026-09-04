const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL || "/api";

export const API_BASE_URL = configuredBaseUrl.replace(/\/$/, "");
export const USE_MOCK_API =
  String(import.meta.env.VITE_USE_MOCK_API ?? "false").toLowerCase() === "true";

export const API_ROUTES = {
  health: "/health",
  auth: {
    login: "/auth/login",
    register: "/auth/register",
    verifyOtp: "/auth/verify-otp",
    resendOtp: "/auth/resend-otp",
    logout: "/auth/logout",
    profile: "/auth/profile",
  },
  dashboard: {
    admin: "/dashboard/admin",
    resident: "/dashboard/resident",
    security: "/dashboard/security",
    accountant: "/dashboard/accountant",
  },
  residents: "/residents",
  visitors: {
    list: "/visitors",
    approve: (id) => `/visitors/${id}/approve`,
    checkIn: (id) => `/visitors/${id}/check-in`,
    checkOut: (id) => `/visitors/${id}/check-out`,
  },
  complaints: "/complaints",
  bills: "/billing",
  payments: "/payments",
  razorpay: "/razorpay",
  meetings: "/meetings",
  announcements: "/announcements",
  flats: "/flats",
  familyMembers: "/family-members",
  documents: "/documents",
  societyDocuments: "/society-documents",
  staffDocuments: "/staff-documents",
  notifications: "/notifications",
  adminUsers: "/admin/users",
};
