import { axiosClient } from '../../../shared/api/axiosClient';

export const repositoryExtraApi = {
  getBookmarkStatus: async (repositoryId: string) => {
    const res = await axiosClient.get(`/repositories/${repositoryId}/bookmark`);
    return res.data;
  },
  toggleBookmark: async (repositoryId: string) => {
    const res = await axiosClient.post(`/repositories/${repositoryId}/bookmark`);
    return res.data;
  },
  getPrivateNote: async (repositoryId: string) => {
    const res = await axiosClient.get(`/repositories/${repositoryId}/note`);
    return res.data;
  },
  savePrivateNote: async (repositoryId: string, content: string) => {
    const res = await axiosClient.put(`/repositories/${repositoryId}/note`, { content });
    return res.data;
  },
  getUpdates: async (repositoryId: string, page = 0, size = 10) => {
    const res = await axiosClient.get(`/repositories/${repositoryId}/updates?page=${page}&size=${size}`);
    return res.data;
  },
  addUpdate: async (repositoryId: string, content: string) => {
    const res = await axiosClient.post(`/repositories/${repositoryId}/updates`, { content });
    return res.data;
  },
  editUpdate: async (repositoryId: string, updateId: string, content: string) => {
    const res = await axiosClient.put(`/repositories/${repositoryId}/updates/${updateId}`, { content });
    return res.data;
  },
  deleteUpdate: async (repositoryId: string, updateId: string) => {
    const res = await axiosClient.delete(`/repositories/${repositoryId}/updates/${updateId}`);
    return res.data;
  }
};
