import { axiosClient } from '../../../shared/api/axiosClient';
import type { Repository, RepositoryDetail, RepositoryMember, CreateRepositoryRequest, InviteMemberRequest } from '../../../shared/types';

const cache = new Map<string, any>();

export const repositoryApi = {
  getAll: async (): Promise<Repository[]> => {
    const cacheKey = 'repos_all';
    const fetchPromise = axiosClient.get('/repositories').then(res => {
      cache.set(cacheKey, res.data.data || []);
      return res.data.data || [];
    });
    if (cache.has(cacheKey)) return cache.get(cacheKey);
    return fetchPromise;
  },

  getById: async (id: string): Promise<RepositoryDetail> => {
    const cacheKey = `repo_${id}`;
    const fetchPromise = axiosClient.get(`/repositories/${id}`).then(res => {
      cache.set(cacheKey, res.data.data);
      return res.data.data;
    });
    if (cache.has(cacheKey)) return cache.get(cacheKey);
    return fetchPromise;
  },

  create: async (data: CreateRepositoryRequest): Promise<Repository> => {
    const response = await axiosClient.post('/repositories', data);
    cache.delete('repos_all');
    return response.data.data;
  },

  update: async (id: string, data: Partial<CreateRepositoryRequest>): Promise<Repository> => {
    const response = await axiosClient.put(`/repositories/${id}`, data);
    cache.delete(`repo_${id}`);
    cache.delete('repos_all');
    return response.data.data;
  },

  delete: async (id: string): Promise<void> => {
    await axiosClient.delete(`/repositories/${id}`);
    cache.delete(`repo_${id}`);
    cache.delete('repos_all');
  },

  inviteMember: async (repositoryId: string, data: InviteMemberRequest): Promise<{ message: string }> => {
    const response = await axiosClient.post(`/repositories/${repositoryId}/invite`, data);
    cache.delete(`repo_${repositoryId}_members`);
    return response.data.data;
  },

  getMembers: async (repositoryId: string): Promise<RepositoryMember[]> => {
    const cacheKey = `repo_${repositoryId}_members`;
    const fetchPromise = axiosClient.get(`/repositories/${repositoryId}/members`).then(res => {
      cache.set(cacheKey, res.data.data || []);
      return res.data.data || [];
    });
    if (cache.has(cacheKey)) return cache.get(cacheKey);
    return fetchPromise;
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
    cache.delete(`repo_${repositoryId}_members`);
  },

  getMaterials: async (repositoryId: string): Promise<any[]> => {
    const cacheKey = `repo_${repositoryId}_materials`;
    const fetchPromise = axiosClient.get(`/repositories/${repositoryId}/materials`).then(res => {
      cache.set(cacheKey, res.data.data || []);
      return res.data.data || [];
    });
    if (cache.has(cacheKey)) return cache.get(cacheKey);
    return fetchPromise;
  },

  getRequests: async (repositoryId: string): Promise<any[]> => {
    const cacheKey = `repo_${repositoryId}_requests`;
    const fetchPromise = axiosClient.get(`/repositories/${repositoryId}/requests`).then(res => {
      cache.set(cacheKey, res.data.data || []);
      return res.data.data || [];
    });
    if (cache.has(cacheKey)) return cache.get(cacheKey);
    return fetchPromise;
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
