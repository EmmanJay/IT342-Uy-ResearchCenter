import { axiosClient } from './axiosClient';
import type { AuthData, User } from '../types';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  firstname: string;
  lastname: string;
  email: string;
  password: string;
}

export const authApi = {
  register: async (data: RegisterRequest): Promise<AuthData> => {
    const response = await axiosClient.post('/auth/register', data);
    const authResponse = response.data.data;
    return {
      user: {
        id: authResponse.id,
        email: authResponse.email,
        firstname: authResponse.firstname,
        lastname: authResponse.lastname,
        role: authResponse.role,
      },
      token: authResponse.accessToken,
      refreshToken: authResponse.refreshToken,
    };
  },

  login: async (data: LoginRequest): Promise<AuthData> => {
    try {
      const response = await axiosClient.post('/auth/login', data);
      const authResponse = response.data.data;
      
      if (!authResponse?.id || !authResponse?.accessToken) {
        throw new Error('Invalid auth response: missing id or accessToken');
      }

      return {
        user: {
          id: authResponse.id,
          email: authResponse.email,
          firstname: authResponse.firstname,
          lastname: authResponse.lastname,
          role: authResponse.role,
        },
        token: authResponse.accessToken,
        refreshToken: authResponse.refreshToken,
      };
    } catch (error: any) {
      // Convert generic error to user-friendly message
      if (error.response?.status === 401) {
        throw new Error('Invalid email or password');
      }
      throw error;
    }
  },

  logout: async (): Promise<void> => {
    await axiosClient.post('/auth/logout');
  },

  getMe: async (): Promise<User> => {
    const response = await axiosClient.get('/users/me');
    return response.data.data;
  },
};

export default authApi;
