<template>
  <div class="search">
    <h1>Search Results</h1>
    <div class="search-container">
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
          >{{ result.url }}</a>
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
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { search } from '../api/search';
import { SearchResponse, SearchRequest } from '../api/types';

defineProps<{
  query: string;
  offset: number;
}>();

const results = ref<SearchResponse[]>([]);
const loading = ref(false);
const error = ref<string | null>(null);
const limit = 10;

const router = useRouter();
const route = useRoute();

const fetchResults = async (query: string, offset: number) => {
  if (!query.trim()) {
    error.value = 'No search query provided';
    results.value = [];
    return;
  }
  loading.value = true;
  error.value = null;
  try {
    const request: SearchRequest = {
      query,
      limit,
      offset,
    };
    const data = await search(request);
    results.value = data;
  } catch (err: any) {
    error.value = err.message || 'Failed to fetch search results';
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  fetchResults(route.query.q as string, parseInt(route.query.offset as string) || 0);
});

watch(
  () => [route.query.q, route.query.offset],
  ([newQuery, newOffset]) => {
    fetchResults(newQuery as string, parseInt(newOffset as string) || 0);
  }
);

const handleNext = () => {
  const newOffset = (parseInt(route.query.offset as string) || 0) + limit;
  router.push({ path: '/search', query: { q: route.query.q, offset: newOffset.toString() } });
};

const handlePrevious = () => {
  const currentOffset = parseInt(route.query.offset as string) || 0;
  if (currentOffset >= limit) {
    const newOffset = currentOffset - limit;
    router.push({ path: '/search', query: { q: route.query.q, offset: newOffset.toString() } });
  }
};
</script>

<style scoped>
.search {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: var(--spacing-unit);
  width: 90%;
  max-width: 50rem;
  margin: 0 auto;
}

h1 {
  color: var(--color-primary);
  font-family: 'Poppins', sans-serif;
  font-size: 2rem;
  font-weight: 600;
  margin-bottom: var(--spacing-unit);
}

.search-container {
  width: 100%;
  background: var(--color-background);
  padding: var(--spacing-unit);
  border-radius: var(--border-radius);
  box-shadow: 0.5rem 0.5rem 1rem var(--shadow-dark), -0.5rem -0.5rem 1rem var(--shadow-light);
}

.error {
  color: var(--color-error);
  font-family: 'Poppins', sans-serif;
  font-size: 1rem;
  margin-bottom: var(--spacing-unit);
}

.no-results {
  color: var(--color-text-muted);
  font-family: 'Poppins', sans-serif;
  font-size: 1rem;
  margin-bottom: var(--spacing-unit);
}

.results {
  margin-top: var(--spacing-unit);
}

.result {
  background: var(--color-background);
  padding: 1rem;
  border-radius: var(--border-radius);
  margin-bottom: 1rem;
  box-shadow: 0.25rem 0.25rem 0.5rem var(--shadow-dark), -0.25rem -0.25rem 0.5rem var(--shadow-light);
}

.result-title {
  margin: 0 0 0.5rem;
  color: var(--color-primary);
  font-family: 'Poppins', sans-serif;
  font-size: 1.2rem;
  font-weight: 600;
}

.snippet {
  margin: 0.5rem 0;
  color: var(--color-text);
  font-family: 'Poppins', sans-serif;
  font-size: 1rem;
}

.snippet :deep(b) {
  background: var(--color-secondary);
  font-weight: 500;
}

.result-url {
  color: var(--color-primary);
  text-decoration: none;
  font-family: 'Poppins', sans-serif;
  font-size: 0.9rem;
  display: block;
  margin: 0.5rem 0;
}

.result-url:hover {
  text-decoration: underline;
  background: var(--color-secondary);
}

.last-modified {
  color: var(--color-text-muted);
  font-family: 'Poppins', sans-serif;
  font-size: 0.8rem;
  margin: 0.5rem 0 0;
}

.pagination {
  display: flex;
  gap: 1rem;
  justify-content: center;
  margin-top: var(--spacing-unit);
}

.pagination-button {
  padding: 0.6rem 1.2rem;
  border: none;
  border-radius: var(--border-radius);
  background: linear-gradient(145deg, #a279d1, #8855b6);
  color: #fff;
  font-family: 'Poppins', sans-serif;
  font-size: 0.9rem;
  cursor: pointer;
  box-shadow: 0.25rem 0.25rem 0.5rem var(--shadow-dark), -0.25rem -0.25rem 0.5rem var(--shadow-light);
  transition: all 0.3s ease;
}

.pagination-button:hover:not(:disabled) {
  background: linear-gradient(145deg, #8855b6, #a279d1);
  box-shadow: 0.15rem 0.15rem 0.3rem var(--shadow-dark), -0.15rem -0.15rem 0.3rem var(--shadow-light);
}

.pagination-button:disabled {
  background: #cccccc;
  cursor: not-allowed;
}

@media (min-width: 1024px) {
  h1 {
    font-size: 2.5rem;
  }

  .search-container {
    padding: 1.5rem;
  }

  .result-title {
    font-size: 1.4rem;
  }

  .snippet {
    font-size: 1.1rem;
  }

  .result-url {
    font-size: 1rem;
  }

  .last-modified {
    font-size: 0.9rem;
  }

  .pagination-button {
    font-size: 1rem;
    padding: 0.8rem 1.5rem;
  }
}
</style>c
