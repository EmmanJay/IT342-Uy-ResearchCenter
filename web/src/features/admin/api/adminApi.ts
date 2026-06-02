import { axiosClient } from '../../../shared/api/axiosClient';

export const adminApi = {
  getStats: async () => {
    const res = await axiosClient.get('/admin/stats');
    return res.data;
  },
  getUsers: async () => {
    const res = await axiosClient.get('/admin/users');
    return res.data;
  },
  suspendUser: async (id: string) => {
    const res = await axiosClient.put(`/admin/users/${id}/suspend`);
    return res.data;
  },
  getRepositories: async () => {
    const res = await axiosClient.get('/admin/repositories');
    return res.data;
  },
  deleteRepository: async (id: number) => {
    const res = await axiosClient.delete(`/admin/repositories/${id}`);
    return res.data;
  },
  getMaterials: async () => {
    const res = await axiosClient.get('/admin/materials');
    return res.data;
  },
  deleteMaterial: async (id: number) => {
    const res = await axiosClient.delete(`/admin/materials/${id}`);
    return res.data;
  },
  getRequests: async () => {
    const res = await axiosClient.get('/admin/requests');
    return res.data;
  },
  updateRequest: async (id: number, status: string) => {
    const res = await axiosClient.put(`/admin/requests/${id}/status`, { status });
    return res.data;
  },
  deleteRequest: async (id: number) => {
    const res = await axiosClient.delete(`/admin/requests/${id}`);
    return res.data;
  }
};
