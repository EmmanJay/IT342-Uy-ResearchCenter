import { axiosClient } from '../../../shared/api/axiosClient';
import type { AuthData, User } from '../../../shared/types';

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
        profilePicture: authResponse.profilePicture,
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
          profilePicture: authResponse.profilePicture,
        },
        token: authResponse.accessToken,
        refreshToken: authResponse.refreshToken,
      };
    } catch (error: any) {
      // Convert generic error to user-friendly message
      const message = (error.response?.data?.error?.message || '').toString().toLowerCase();
      if (error.response?.data?.error?.code === 'AUTH-003' || message.includes('suspend')) {
        throw new Error('Your account is suspended. Please contact administrator');
      }
      if (error.response?.status === 401) {
        throw new Error('Invalid email or password');
      }
      if (error.response?.status === 403) {
        throw new Error('Your account is suspended. Please contact administrator');
      }
      throw error;
    }
  },

  googleAuth: async (idToken: string): Promise<AuthData> => {
    try {
      const response = await axiosClient.post('/auth/google', { idToken });
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
          profilePicture: authResponse.profilePicture,
        },
        token: authResponse.accessToken,
        refreshToken: authResponse.refreshToken,
      };
    } catch (error: any) {
      const message = (error.response?.data?.error?.message || '').toString().toLowerCase();
      if (error.response?.data?.error?.code === 'AUTH-003' || message.includes('suspend')) {
        throw new Error('Your account is suspended. Please contact administrator');
      }
      if (error.response?.status === 401) {
        throw new Error(error.response?.data?.error?.message || error.response?.data?.message || 'Google sign-in failed. Please verify your account or try again.');
      }
      if (error.response?.status === 403) {
        throw new Error('Your account is suspended. Please contact administrator');
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

  updateProfile: async (data: { firstname: string; lastname: string; profilePicture?: string }): Promise<User> => {
    const response = await axiosClient.put('/users/me', data);
    return response.data.data;
  },
};

export default authApi;
