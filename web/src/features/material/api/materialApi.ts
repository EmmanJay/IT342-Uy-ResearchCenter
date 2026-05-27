import { axiosClient } from '../../../shared/api/axiosClient';
import type { Material, CreateMaterialRequest } from '../../../shared/types';

const serializeMetadata = (metadata: unknown): string | undefined => {
  if (!metadata) return undefined;
  if (typeof metadata === 'string') return metadata;
  return JSON.stringify(metadata);
};

export const materialApi = {
  create: async (data: CreateMaterialRequest): Promise<Material> => {
    const payload: any = {
      title: data.title,
      repositoryId: data.repositoryId,
      tags: data.tags,
      fileUrl: data.fileUrl,
      url: data.url,
      publisher: data.publisher,
      year: data.year,
      isbn: (data as any).isbn || (data.metadata ? (data.metadata as any).isbn : undefined),
      description: (data as any).description,
      materialType: (data as any).type || (data as any).materialType,
      metadata: serializeMetadata(data.metadata),
    };

    if (data.authors) {
      // authors may be an array on client; convert to comma-separated string for backend
      payload.authors = Array.isArray(data.authors) ? data.authors.join(', ') : data.authors;
    }

    const response = await axiosClient.post('/materials', payload);
    return response.data.data;
  },

  createWithFile: async (formData: FormData, repositoryId: string): Promise<Material> => {
    const response = await axiosClient.post(`/materials/upload?repositoryId=${repositoryId}`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data.data;
  },

  getById: async (id: string): Promise<Material> => {
    const response = await axiosClient.get(`/materials/${id}`);
    return response.data.data;
  },

  getBookmarked: async (): Promise<Material[]> => {
    const response = await axiosClient.get('/materials/bookmarked');
    return response.data.data || [];
  },

  delete: async (id: string): Promise<void> => {
    try {
      await axiosClient.delete(`/materials/${id}`);
    } catch (error: any) {
      // Re-throw with additional context
      throw {
        response: error.response,
        message: error.message,
        originalError: error
      };
    }
  },

  updateStatus: async (id: string, status: string): Promise<string> => {
    const response = await axiosClient.patch(`/materials/${id}/status`, { status });
    return response.data.data?.status;
  },
  toggleBookmark: async (id: string): Promise<boolean> => {
    const response = await axiosClient.post(`/materials/${id}/bookmark`);
    return Boolean(response.data.data?.bookmarked);
  },
  getNote: async (id: string): Promise<string> => {
    const response = await axiosClient.get(`/materials/${id}/note`);
    return response.data.data?.content || '';
  },
  saveNote: async (id: string, content: string): Promise<string> => {
    const response = await axiosClient.put(`/materials/${id}/note`, { content });
    return response.data.data?.content || '';
  },
  update: async (id: string, data: Partial<CreateMaterialRequest> & { fileUrl?: string; url?: string; description?: string; fileDeleted?: boolean; tags?: string[]; title?: string; metadata?: any, isbn?: string }) => {
    try {
      const payload: any = {
        title: data.title,
        description: data.description,
        tags: data.tags,
        materialType: (data as any).type || (data as any).materialType,
        metadata: serializeMetadata(data.metadata),
      };
      if (data.isbn) payload.isbn = data.isbn;
      if (data.metadata?.isbn) payload.isbn = data.metadata.isbn;
      if (data.publisher !== undefined) payload.publisher = data.publisher;
      if (data.year !== undefined) payload.year = data.year;
      if (data.authors !== undefined) payload.authors = Array.isArray(data.authors) ? data.authors.join(', ') : data.authors;
      if (data.url !== undefined) payload.url = data.url;
      if (data.fileUrl !== undefined) payload.fileUrl = data.fileUrl;
      const response = await axiosClient.put(`/materials/${id}`, payload);
      return response.data.data;
    } catch (error: any) {
      throw {
        response: error.response,
        message: error.message,
        originalError: error
      };
    }
  },
};

export default materialApi;
