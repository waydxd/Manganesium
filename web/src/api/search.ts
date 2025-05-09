import axios, { type AxiosResponse } from 'axios';
import type { SearchRequest, SearchResponse, SearchResults } from './types';

const API_BASE = import.meta.env.VITE_API_BASE || '/api';
const FALLBACK_API_BASE = 'http://localhost:8080/api';

// Type for health check response
interface HealthCheckResponse {
  status: string;
  search: 'READY' | 'INITIALIZING';
}

// Retry logic for network errors
const retryRequest = async <T>(fn: () => Promise<T>, retries: number = 3, delay: number = 1000): Promise<T> => {
  for (let i = 0; i < retries; i++) {
    try {
      return await fn();
    } catch (error) {
      if (i === retries - 1) throw error;
      console.warn(`Retry ${i + 1}/${retries} failed. Retrying in ${delay}ms...`);
      await new Promise(resolve => setTimeout(resolve, delay));
    }
  }
  throw new Error('Retry limit reached');
};

export const search = async (
  request: SearchRequest
): Promise<SearchResponse[]> => {
  const params = {
    query: request.query,
    limit: request.limit,
    offset: request.offset
  };

  console.log('Environment details:', {
    VITE_API_BASE: import.meta.env.VITE_API_BASE,
    API_BASE,
    FALLBACK_API_BASE,
    NODE_ENV: import.meta.env.MODE
  });

  const attemptRequest = async (baseUrl: string): Promise<AxiosResponse> => {
    console.log('Sending search request:', {
      url: `${baseUrl}/search`,
      params
    });
    return await axios.get(`${baseUrl}/search`, {
      params,
      headers: {
        'Accept': 'application/json'
      }
    });
  };

  try {
    let response: AxiosResponse;
    try {
      response = await retryRequest(() => attemptRequest(API_BASE));
    } catch (error) {
      console.warn('Primary API request failed. Attempting fallback URL:', FALLBACK_API_BASE);
      response = await retryRequest(() => attemptRequest(FALLBACK_API_BASE));
    }

    console.log('Raw /api/search response:', {
      status: response.status,
      headers: response.headers,
      data: response.data
    });

    let data: SearchResponse[] = [];
    if (Array.isArray(response.data)) {
      data = response.data;
    } else if (response.data && typeof response.data === 'object' && Array.isArray(response.data.results)) {
      data = response.data.results;
    } else {
      console.warn('Unexpected response format: Expected array or SearchResults, got:', response.data);
      data = [];
    }

    if (data.length === 0) {
      console.warn('No results returned for query:', request.query);
    }

    console.log('Raw search results before mapping:', data);

    const mappedData = data.map((item: any) => {
      const mapped = {
        pageID: item.pageID?.toString() || `id-${Math.random()}`,
        pageTitle: item.title || 'Untitled',
        url: item.url || '',
        lastModified: item.lastModified || '',
        snippet: item.snippet || '',
        score: item.score ?? undefined,
        pageSize: item.pageSize ?? undefined,
        keywords: item.keywords ? item.keywords.map((kw: any) => ({
          keyword: kw.keyword || '',
          frequency: kw.frequency || 0
        })) : undefined,
        parentLinks: item.parentLinks ? item.parentLinks.map((link: any) => link.toString()) : undefined,
        childLinks: item.childLinks ? item.childLinks.map((link: any) => link.toString()) : undefined
      };
      console.log('Mapped item:', { input: item, output: mapped });
      return mapped;
    });

    console.log('Mapped search results:', mappedData);

    return mappedData;
  } catch (error) {
    if (axios.isAxiosError(error)) {
      console.error('Axios error:', {
        status: error.response?.status,
        data: error.response?.data,
        message: error.message,
        headers: error.response?.headers,
        code: error.code,
        requestUrl: error.config?.url
      });
      if (error.response?.status === 503) {
        throw new Error('Search service is initializing. Please try again later.');
      }
      if (error.response?.status === 404) {
        console.error('Search endpoint not found. Check API_BASE or FALLBACK_API_BASE:', { API_BASE, FALLBACK_API_BASE });
        throw new Error('Search endpoint not found. Please check if the backend is running.');
      }
      if (error.code === 'ERR_NETWORK') {
        console.error('Network error details:', {
          message: 'Unable to connect to backend. Ensure the backend is running and accessible.',
          API_BASE,
          FALLBACK_API_BASE,
          possibleIssues: [
            'Backend not running on port 8080',
            'Vite proxy misconfigured (check vite.config.ts)',
            'CORS issue despite proxy',
            'Port 8080 blocked or in use',
            'Incorrect VITE_API_BASE in .env'
          ]
        });
        throw new Error('Cannot connect to the search service. Please ensure the backend is running and try again.');
      }
    } else {
      console.error('Unexpected error during search:', error);
    }
    throw error;
  }
};

export const checkHealth = async (): Promise<HealthCheckResponse> => {
  try {
    console.log('Checking health at:', `${API_BASE}/health`);
    const response = await retryRequest(async () => {
      return await axios.get<HealthCheckResponse>(`${API_BASE}/health`, {
        headers: {
          'Accept': 'application/json'
        }
      });
    });
    console.log('Health check response:', response.data);
    return response.data;
  } catch (error) {
    console.error('Health check failed:', error);
    throw error;
  }
};

export const isSearchReady = async (): Promise<boolean> => {
  try {
    const health = await checkHealth();
    return health.search === 'READY';
  } catch {
    console.warn('Failed to check health. Assuming service is not ready.');
    return false;
  }
};
