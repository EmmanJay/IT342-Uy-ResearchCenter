import { axiosClient } from '../../../shared/api/axiosClient';
import type { Repository, RepositoryDetail, RepositoryMember, CreateRepositoryRequest, InviteMemberRequest } from '../../../shared/types';

export const repositoryApi = {
  getAll: async (): Promise<Repository[]> => {
    const res = await axiosClient.get('/repositories');
    return res.data.data || [];
  },

  getById: async (id: string): Promise<RepositoryDetail> => {
    const res = await axiosClient.get(`/repositories/${id}`);
    return res.data.data;
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
    const res = await axiosClient.get(`/repositories/${repositoryId}/members`);
    return res.data.data || [];
  },

  getInvitations: async (repositoryId: string): Promise<any[]> => {
    const response = await axiosClient.get(`/repositories/${repositoryId}/invitations`);
    return response.data.data || [];
  },

  revokeInvitation: async (invitationId: string): Promise<void> => {
    await axiosClient.delete(`/invitations/${invitationId}`);
  },

  removeMember: async (repositoryId: string, userId: string): Promise<void> => {
    await axiosClient.delete(`/repositories/${repositoryId}/members/${userId}`);
  },

  getMaterials: async (repositoryId: string): Promise<any[]> => {
    const res = await axiosClient.get(`/repositories/${repositoryId}/materials`);
    return res.data.data || [];
  },

  getRequests: async (repositoryId: string): Promise<any[]> => {
    const res = await axiosClient.get(`/repositories/${repositoryId}/requests`);
    return res.data.data || [];
  },

  toggleBookmark: async (repositoryId: string): Promise<boolean> => {
    const response = await axiosClient.post(`/repositories/${repositoryId}/bookmark`);
    return Boolean(response.data.data?.bookmarked);
  },

  getNotes: async (repositoryId: string): Promise<any[]> => {
    const response = await axiosClient.get(`/repositories/${repositoryId}/notes`);
    return response.data.data || [];
  },

  addNote: async (repositoryId: string, content: string): Promise<any> => {
    const response = await axiosClient.post(`/repositories/${repositoryId}/notes`, { content });
    return response.data.data;
  },

  updateNote: async (repositoryId: string, noteId: string, content: string): Promise<any> => {
    const response = await axiosClient.put(`/repositories/${repositoryId}/notes/${noteId}`, { content });
    return response.data.data;
  },

  deleteNote: async (repositoryId: string, noteId: string): Promise<void> => {
    await axiosClient.delete(`/repositories/${repositoryId}/notes/${noteId}`);
  },

  leaveRepository: async (repositoryId: string): Promise<void> => {
    await axiosClient.post(`/repositories/${repositoryId}/leave`);
  },
};

export default repositoryApi;
