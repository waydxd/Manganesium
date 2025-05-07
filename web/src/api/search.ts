import axios, { type AxiosResponse } from 'axios';
import type { SearchRequest, SearchResponse } from './types';

const API_BASE = import.meta.env.VITE_API_BASE || '/api';

// Type for health check response
interface HealthCheckResponse {
  status: string;
  search: 'READY' | 'INITIALIZING';
}

export const search = async (
  request: SearchRequest
): Promise<SearchResponse[]> => {
  try {
    const params = {
      query: request.query,
      limit: request.limit,
      offset: request.offset
    };

    const response: AxiosResponse<SearchResponse[]> = await axios.get(
      `${API_BASE}/search`,
      { params }
    );

    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.status === 503) {
      throw new Error('Search service is initializing. Please try again later.');
    }
    throw error;
  }
};

export const checkHealth = async (): Promise<HealthCheckResponse> => {
  const response = await axios.get<HealthCheckResponse>(`${API_BASE}/health`);
  return response.data;
};

export const isSearchReady = async (): Promise<boolean> => {
  const health = await checkHealth();
  return health.search === 'READY';
};
