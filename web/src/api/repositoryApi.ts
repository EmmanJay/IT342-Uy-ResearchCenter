import { axiosClient } from './axiosClient';
import type { Repository, RepositoryDetail, RepositoryMember, CreateRepositoryRequest, InviteMemberRequest } from '../types';

export const repositoryApi = {
  getAll: async (): Promise<Repository[]> => {
    const response = await axiosClient.get('/repositories');
    return response.data.data || [];
  },

  getById: async (id: string): Promise<RepositoryDetail> => {
    const response = await axiosClient.get(`/repositories/${id}`);
    return response.data.data;
  },

  create: async (data: CreateRepositoryRequest): Promise<Repository> => {
    const response = await axiosClient.post('/repositories', data);
    return response.data.data;
  },

  update: async (id: string, data: Partial<CreateRepositoryRequest>): Promise<Repository> => {
    const response = await axiosClient.put(`/repositories/${id}`, data);
    return response.data.data;
  },

  delete: async (id: string): Promise<void> => {
    await axiosClient.delete(`/repositories/${id}`);
  },

  inviteMember: async (repositoryId: string, data: InviteMemberRequest): Promise<{ message: string }> => {
    const response = await axiosClient.post(`/repositories/${repositoryId}/invite`, data);
    return response.data.data;
  },

  getMembers: async (repositoryId: string): Promise<RepositoryMember[]> => {
    const response = await axiosClient.get(`/repositories/${repositoryId}/members`);
    return response.data.data || [];
  },

  removeMember: async (repositoryId: string, userId: string): Promise<void> => {
    await axiosClient.delete(`/repositories/${repositoryId}/members/${userId}`);
  },

  getMaterials: async (repositoryId: string): Promise<any[]> => {
    const response = await axiosClient.get(`/repositories/${repositoryId}/materials`);
    return response.data.data || [];
  },

  getRequests: async (repositoryId: string): Promise<any[]> => {
    const response = await axiosClient.get(`/repositories/${repositoryId}/requests`);
    return response.data.data || [];
  },
};

export default repositoryApi;
