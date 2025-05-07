// Request types
export interface SearchRequest {
  query: string;
  limit: number;
  offset: number;
}

// Response types
export interface SearchResponse {
  pageID: string;
  title: string;
  url: string;
  lastModified: string;
  snippet: string;
}

// For handling multiple results
export interface SearchResults {
  results: SearchResponse[];
  total: number; // Optional: if your backend provides total count
}
