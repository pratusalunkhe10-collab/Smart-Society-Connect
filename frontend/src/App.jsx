import { Navigate, Outlet, Route, Routes, useLocation } from 'react-router-dom';
import AppShell from './components/layout/AppShell.jsx';
import { useAuth } from './context/AuthContext.jsx';
import BillingPage from './pages/BillingPage.jsx';
import ComplaintsPage from './pages/ComplaintsPage.jsx';
import DashboardPage from './pages/DashboardPage.jsx';
import DocumentsPage from './pages/DocumentsPage.jsx';
import MeetingsPage from './pages/MeetingsPage.jsx';
import AnnouncementsPage from './pages/AnnouncementsPage.jsx';
import NotFoundPage from './pages/NotFoundPage.jsx';
import ProfilePage from './pages/ProfilePage.jsx';
import ResidentsPage from './pages/ResidentsPage.jsx';
import UnauthorizedPage from './pages/UnauthorizedPage.jsx';
import AdminUsersPage from './pages/AdminUsersPage.jsx';
import VisitorsPage from './pages/VisitorsPage.jsx';
import AuthLayout from './pages/auth/AuthLayout.jsx';
import ForgotPasswordPage from './pages/auth/ForgotPasswordPage.jsx';
import LoginPage from './pages/auth/LoginPage.jsx';
import RegisterPage from './pages/auth/RegisterPage.jsx';
import VerifyOtpPage from './pages/auth/VerifyOtpPage.jsx';

function ProtectedRoute() {
  const { isAuthenticated } = useAuth();
  const location = useLocation();
  return isAuthenticated ? <Outlet /> : <Navigate to="/login" replace state={{ from: location }} />;
}

function PublicOnlyRoute() {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? <Navigate to="/app/dashboard" replace /> : <Outlet />;
}

function RoleRoute({ roles }) {
  const { user } = useAuth();
  return roles.includes(user?.role) ? <Outlet /> : <Navigate to="/app/unauthorized" replace />;
}

function RootRedirect() {
  const { isAuthenticated } = useAuth();
  return <Navigate to={isAuthenticated ? '/app/dashboard' : '/login'} replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<RootRedirect />} />

      <Route element={<PublicOnlyRoute />}>
        <Route element={<AuthLayout />}>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/verify-otp" element={<VerifyOtpPage />} />
        </Route>
      </Route>

      <Route element={<ProtectedRoute />}>
        <Route path="/app" element={<AppShell />}>
          <Route index element={<Navigate to="dashboard" replace />} />
          <Route path="dashboard" element={<DashboardPage />} />

          <Route element={<RoleRoute roles={['ADMIN', 'SECRETARY', 'RESIDENT']} />}>
            <Route path="residents" element={<ResidentsPage />} />
          </Route>

          <Route element={<RoleRoute roles={['ADMIN']} />}>
            <Route path="users" element={<AdminUsersPage />} />
          </Route>

          <Route element={<RoleRoute roles={['ADMIN', 'RESIDENT', 'SECURITY', 'SECRETARY']} />}>
            <Route path="visitors" element={<VisitorsPage />} />
          </Route>

          <Route element={<RoleRoute roles={['ADMIN', 'RESIDENT', 'SECRETARY']} />}>
            <Route path="complaints" element={<ComplaintsPage />} />
          </Route>

          <Route element={<RoleRoute roles={['ADMIN', 'RESIDENT', 'ACCOUNTANT', 'SECRETARY']} />}>
            <Route path="billing" element={<BillingPage />} />
          </Route>

          <Route element={<RoleRoute roles={['ADMIN', 'RESIDENT', 'SECURITY', 'ACCOUNTANT', 'SECRETARY']} />}>
            <Route path="documents" element={<DocumentsPage />} />
          </Route>

          <Route element={<RoleRoute roles={['ADMIN', 'RESIDENT', 'SECURITY', 'ACCOUNTANT', 'SECRETARY']} />}>
          <Route path="meetings" element={<MeetingsPage />} />
          <Route path="announcements" element={<AnnouncementsPage />} />
          </Route>

          <Route path="profile" element={<ProfilePage />} />
          <Route path="unauthorized" element={<UnauthorizedPage />} />
        </Route>
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
