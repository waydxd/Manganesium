<template>
  <div class="search-container">
    <form @submit.prevent="handleSearch" aria-label="Search form">
      <input
        v-model="query"
        type="text"
        placeholder="Enter your search query..."
        :disabled="loading"
        aria-label="Search query"
        class="search-input"
      />
      <button
        type="submit"
        :disabled="loading"
        class="search-button"
        aria-label="Submit search"
      >
        {{ loading ? 'Searching...' : 'Search' }}
      </button>
    </form>
    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="results.length === 0 && !loading && !error" class="no-results">
      No results found. Try a different query.
    </p>
    <div class="results">
      <div v-for="result in results" :key="result.pageID" class="result">
        <h3 class="result-title">{{ result.title }}</h3>
        <div class="snippet" v-html="result.snippet"></div>
        <a
          :href="result.url"
          target="_blank"
          rel="noopener noreferrer"
          class="result-url"
        >{{ result.url }}</a
        >
        <p class="last-modified">Last Modified: {{ result.lastModified }}</p>
      </div>
    </div>
    <div v-if="results.length > 0" class="pagination">
      <button
        @click="handlePrevious"
        :disabled="offset === 0"
        class="pagination-button"
      >
        Previous
      </button>
      <button
        @click="handleNext"
        :disabled="results.length < limit"
        class="pagination-button"
      >
        Next
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { search } from '../api/search';
import { SearchResponse, SearchRequest } from '../api/types';

const query = ref('');
const results = ref<SearchResponse[]>([]);
const loading = ref(false);
const error = ref<string | null>(null);
const offset = ref(0);
const limit = 10;

const handleSearch = async () => {
  if (!query.value.trim()) {
    error.value = 'Please enter a search query';
    return;
  }
  loading.value = true;
  error.value = null;
  try {
    const request: SearchRequest = {
      query: query.value,
      limit,
      offset: offset.value,
    };
    const data = await search(request);
    results.value = data;
  } catch (err: any) {
    error.value = err.message || 'Failed to fetch search results';
  } finally {
    loading.value = false;
  }
};

const handleNext = () => {
  offset.value += limit;
  handleSearch();
};

const handlePrevious = () => {
  if (offset.value >= limit) {
    offset.value -= limit;
    handleSearch();
  }
};
</script>

<style scoped>
.search-container {
  background: var(--color-background);
  padding: 20px;
  border-radius: 20px;
  box-shadow: 8px 8px 16px var(--shadow-dark), -8px -8px 16px var(--shadow-light);
}

form {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.search-input {
  flex-grow: 1;
  padding: 12px;
  border: none;
  border-radius: 12px;
  background: var(--color-background);
  box-shadow: inset 4px 4px 8px var(--shadow-dark), inset -4px -4px 8px var(--shadow-light);
  font-family: 'Poppins', sans-serif;
  font-size: 16px;
  color: var(--color-text);
}

.search-input:focus {
  outline: none;
  box-shadow: inset 2px 2px 4px var(--shadow-dark), inset -2px -2px 4px var(--shadow-light);
}

.search-button {
  padding: 12px 24px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(145deg, #a279d1, #8855b6);
  color: #fff;
  font-family: 'Poppins', sans-serif;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  box-shadow: 4px 4px 8px var(--shadow-dark), -4px -4px 8px var(--shadow-light);
  transition: all 0.3s ease;
}

.search-button:hover:not(:disabled) {
  background: linear-gradient(145deg, #8855b6, #a279d1);
  box-shadow: 2px 2px 4px var(--shadow-dark), -2px -2px 4px var(--shadow-light);
}

.search-button:disabled {
  background: #cccccc;
  cursor: not-allowed;
}

.error {
  color: var(--color-error);
  font-family: 'Poppins', sans-serif;
  margin-bottom: 20px;
}

.no-results {
  color: var(--color-text-muted);
  font-family: 'Poppins', sans-serif;
  margin-bottom: 20px;
}

.results {
  margin-top: 20px;
}

.result {
  background: var(--color-background);
  padding: 16px;
  border-radius: 12px;
  margin-bottom: 16px;
  box-shadow: 4px 4px 8px var(--shadow-dark), -4px -4px 8px var(--shadow-light);
}

.result-title {
  margin: 0 0 8px;
  color: var(--color-primary);
  font-family: 'Poppins', sans-serif;
  font-size: 18px;
  font-weight: 600;
}

.snippet {
  margin: 8px 0;
  color: var(--color-text);
  font-family: 'Poppins', sans-serif;
}

.snippet :deep(b) {
  background: var(--color-secondary);
  font-weight: 500;
}

.result-url {
  color: var(--color-primary);
  text-decoration: none;
  font-family: 'Poppins', sans-serif;
  font-size: 14px;
  display: block;
  margin: 8px 0;
}

.result-url:hover {
  text-decoration: underline;
  background: var(--color-secondary);
}

.last-modified {
  color: var(--color-text-muted);
  font-family: 'Poppins', sans-serif;
  font-size: 12px;
  margin: 8px 0 0;
}

.pagination {
  display: flex;
  gap: 10px;
  justify-content: center;
  margin-top: 20px;
}

.pagination-button {
  padding: 10px 20px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(145deg, #a279d1, #8855b6);
  color: #fff;
  font-family: 'Poppins', sans-serif;
  font-size: 14px;
  cursor: pointer;
  box-shadow: 4px 4px 8px var(--shadow-dark), -4px -4px 8px var(--shadow-light);
  transition: all 0.3s ease;
}

.pagination-button:hover:not(:disabled) {
  background: linear-gradient(145deg, #8855b6, #a279d1);
  box-shadow: 2px 2px 4px var(--shadow-dark), -2px -2px 4px var(--shadow-light);
}

.pagination-button:disabled {
  background: #cccccc;
  cursor: not-allowed;
}
</style>
