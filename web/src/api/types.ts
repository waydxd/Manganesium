// Request types
export interface SearchRequest {
  query: string;
  limit: number;
  offset: number;
}

// Response types
export interface SearchResponse {
  pageID: string;
  pageTitle: string; // Changed from 'title' to match SearchView.vue
  url: string;
  lastModified: string;
  snippet: string;
  score?: number;
  pageSize?: number;
  keywords?: Keyword[];
  parentLinks?: string[];
  childLinks?: string[];
}

// For handling multiple results
export interface SearchResults {
  results: SearchResponse[];
  total: number; // Optional: if your backend provides total count
}

// Response types
export interface Keyword {
  keyword: string;
  frequency: number;
}
