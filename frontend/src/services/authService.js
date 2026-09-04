import http, { unwrap } from './http.js';
import { API_ROUTES, USE_MOCK_API } from '../config/api.js';
import { getMockDb, nextId, saveMockDb, wait } from './mockDb.js';

const publicUser = (source) => {
  const user = { ...source };
  delete user.password;
  return user;
};

const toSessionUser = (data, username) => ({
  id: data.userId ?? data.user_id ?? data.id,
  name: data.fullName ?? data.full_name ?? data.name ?? username,
  email: data.email ?? username,
  mobile: data.mobile,
  role: data.roleName ?? data.role_name ?? data.role ?? 'RESIDENT',
  flat: data.flat ?? data.flatNumber,
  profileImageUrl: data.profileImageUrl ?? data.profile_image_url,
});

const toRegistrationRequest = ({ name, email, mobile, password }) => {
  const [firstName = '', ...lastNameParts] = name.trim().split(/\s+/);
  return {
    firstName,
    lastName: lastNameParts.join(' ') || firstName,
    email,
    mobile,
    password,
  };
};

export const authService = {
  async login(credentials) {
    if (!USE_MOCK_API) {
      const data = unwrap(await http.post(API_ROUTES.auth.login, credentials));
      return {
        token: data.token || data.accessToken || data.access_token || data.jwt,
        user: data.user ? toSessionUser(data.user, credentials.username) : toSessionUser(data, credentials.username),
      };
    }

    await wait();
    const db = getMockDb();
    const username = credentials.username.trim().toLowerCase();
    const user = db.users.find(
      (item) => item.email.toLowerCase() === username || item.mobile === credentials.username.trim(),
    );
    if (!user || user.password !== credentials.password) {
      throw new Error('Invalid email/mobile or password. Use one of the demo accounts shown below.');
    }
    return { token: `mock-jwt-${user.id}-${Date.now()}`, user: publicUser(user) };
  },

  async register(payload) {
    if (!USE_MOCK_API) return unwrap(await http.post(API_ROUTES.auth.register, toRegistrationRequest(payload)));
    await wait();
    const db = getMockDb();
    if (db.users.some((item) => item.email.toLowerCase() === payload.email.toLowerCase())) {
      throw new Error('An account already exists with this email address.');
    }
    const user = {
      id: nextId(db.users),
      name: payload.name,
      email: payload.email,
      mobile: payload.mobile,
      password: payload.password,
      role: null,
      approvalStatus: 'PENDING',
      flat: payload.flat || 'Pending allocation',
    };
    db.users.push(user);
    saveMockDb(db);
    return { message: 'Registration successful. Verify the demo OTP 123456.', user: publicUser(user) };
  },

  async verifyOtp(payload) {
    if (!USE_MOCK_API) return unwrap(await http.post(API_ROUTES.auth.verifyOtp, payload));
    await wait(180);
    if (payload.otpCode !== '123456') throw new Error('Invalid OTP. Use 123456 in demo mode.');
    return { message: 'OTP verified successfully.' };
  },

  async resendOtp(payload) {
    if (!USE_MOCK_API) return unwrap(await http.post(API_ROUTES.auth.resendOtp, payload.email, {
      headers: { 'Content-Type': 'text/plain' },
    }));
    await wait();
    return { message: `A new OTP was sent to ${payload.email}. Demo OTP: 123456` };
  },

  async logout() {
    if (!USE_MOCK_API) return unwrap(await http.post(API_ROUTES.auth.logout));
    await wait();
    return { message: 'Logged out successfully.' };
  },

  async uploadProfileImage(file) {
    if (!USE_MOCK_API) {
      const formData = new FormData();
      formData.append('file', file);
      return unwrap(await http.post('/auth/profile/image', formData, {
        // Do not set Content-Type here. The browser supplies the multipart
        // boundary when it serializes FormData.
        headers: { 'Content-Type': undefined },
      }));
    }
    await wait();
    return { profileImageUrl: URL.createObjectURL(file) };
  },

  async updateProfile(payload) {
    if (!USE_MOCK_API) return unwrap(await http.put(API_ROUTES.auth.profile, payload));
    await wait();
    return payload;
  },

  async getProfile() {
    if (!USE_MOCK_API) {
      const data = unwrap(await http.get(API_ROUTES.auth.profile));
      const profile = toSessionUser(data, data.email);
      // Older backend instances return profile data without a role. Never let
      // that missing field overwrite the active role established at login.
      if (!data.roleName && !data.role_name && !data.role) delete profile.role;
      return profile;
    }
    return null;
  },
};
