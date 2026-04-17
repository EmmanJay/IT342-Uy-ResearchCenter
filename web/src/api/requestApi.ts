import { axiosClient } from './axiosClient';
import type { MaterialRequest, CreateRequestDto, FulfillRequestDto } from '../types';

export const requestApi = {
  getById: async (id: string): Promise<MaterialRequest> => {
    const response = await axiosClient.get(`/requests/${id}`);
    return response.data.data;
  },

  create: async (data: CreateRequestDto): Promise<MaterialRequest> => {
    const response = await axiosClient.post('/requests', data);
    return response.data.data;
  },

  fulfill: async (id: string, data: FulfillRequestDto): Promise<MaterialRequest> => {
    const response = await axiosClient.post(`/requests/${id}/fulfill`, data);
    return response.data.data;
  },

  updateMaterial: async (id: string, data: FulfillRequestDto): Promise<MaterialRequest> => {
    const response = await axiosClient.put(`/requests/${id}/material`, data);
    return response.data.data;
  },

  delete: async (id: string): Promise<void> => {
    await axiosClient.delete(`/requests/${id}`);
  },
};

export default requestApi;
