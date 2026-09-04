import axios from 'axios';
import { API_BASE_URL } from '../config/api.js';

const http = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('ssc_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  // Axios' instance defaults to JSON for normal API requests.  A FormData
  // request must not carry that header: the browser adds multipart/form-data
  // together with its required boundary.
  if (typeof FormData !== 'undefined' && config.data instanceof FormData) {
    config.headers.delete?.('Content-Type');
    if (!config.headers.delete) delete config.headers['Content-Type'];
  }
  return config;
});

const readErrorMessage = (error) => {
  const payload = error.response?.data;
  if (typeof payload === 'string' && payload.trim()) return payload;
  if (payload?.message) return payload.message;
  if (payload?.error) return payload.error;
  if (payload?.errors && typeof payload.errors === 'object') {
    const first = Object.values(payload.errors).flat().find(Boolean);
    if (first) return String(first);
  }
  if (error.code === 'ECONNABORTED') return 'The backend request timed out.';
  if (!error.response) return 'Cannot reach the backend. Confirm Spring Boot is running on http://localhost:8080.';
  return `Backend request failed with status ${error.response.status}.`;
};

http.interceptors.response.use(
  (response) => response,
  (error) => {
    error.message = readErrorMessage(error);
    if (error.response?.status === 401) {
      localStorage.removeItem('ssc_token');
      localStorage.removeItem('ssc_user');
      window.dispatchEvent(new Event('ssc:unauthorized'));
    }
    return Promise.reject(error);
  },
);

export const unwrap = (response) => response?.data?.data ?? response?.data;
export default http;
