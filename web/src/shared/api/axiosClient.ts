import axios from 'axios';
import { SessionManager } from '../auth/sessionManager';

// In dev mode, use relative path to leverage Vite proxy
// In production, use full URL or env variable
const API_BASE_URL = import.meta.env.DEV 
  ? '/api/v1'
  : (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1');

export const axiosClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to add JWT token
axiosClient.interceptors.request.use(
  (config) => {
    const token = SessionManager.getToken();
    // Only add Authorization header if token exists, is a valid string, and is not "undefined"
    if (token && typeof token === 'string' && token.trim() && token !== 'undefined') {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor to handle 401
axiosClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Only redirect to login if we're not already on the login page
      if (window.location.pathname !== '/login') {
        SessionManager.clear();
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export default axiosClient;
