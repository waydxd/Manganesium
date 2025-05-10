<template>
  <div class="search">
    <h1>Search Results</h1>
    <div class="search-container">
      <p v-if="error" class="error">{{ error }}</p>
      <p v-if="results.length === 0 && !loading && !error" class="no-results">
        No results found. Try a different query.
      </p>
      <div class="results">
        <div v-for="result in results" :key="result.url" class="result">
          <p class="score"><strong>Score - </strong><span class="score-value">{{ roundToSignificantFigures(result.score, 3) || 'N/A' }}</span></p>
          <p id="title"><strong>Page Title - </strong> {{ result.pageTitle || 'Untitled' }}</p>
          <a
            :href="result.url || '#'"
            target="_blank"
            rel="noopener noreferrer"
            class="result-url"
          >{{ result.url || 'No URL available' }}</a>
          <p v-if="result.lastModified && isValidDate(result.lastModified)">
            <strong>Last Modification Date - </strong> {{ formatDate(result.lastModified) }}
          </p>
          <p v-else><strong>Last Modification Date - </strong> N/A</p>
          <p><strong>Size of Page - </strong> {{ result.pageSize ? result.pageSize + ' bytes' : 'N/A' }}</p>
          <div v-if="result.keywords && result.keywords.length > 0" class="keywords">
            <p v-for="kw in result.keywords" :key="kw.keyword">
              {{ kw.keyword }} {{ kw.frequency }}
            </p>
          </div>
          <div v-else><p>No keywords available</p></div>
          <div v-if="result.parentLinks && result.parentLinks.length > 0" class="parent-links">
            <p v-for="link in result.parentLinks" :key="link">
              <a :href="link" target="_blank" rel="noopener noreferrer">{{ link }}</a>
            </p>
          </div>
          <div v-else><p>No parent links available</p></div>
          <div v-if="result.childLinks && result.childLinks.length > 0" class="child-links">
            <p v-for="link in result.childLinks" :key="link">
              <a :href="link" target="_blank" rel="noopener noreferrer">{{ link }}</a>
            </p>
          </div>
          <div v-else><p>No child links available</p></div>
          <div v-if="result.snippet" class="snippet" v-html="result.snippet"></div>
          <div v-else><p>No snippet available</p></div>
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
import { parse, isValid, format } from 'date-fns';
import { search } from '../api/search';
import type { SearchRequest, SearchResponse } from '../api/types';

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

const isValidDate = (dateStr: string) => {
  try {
    // Try date-fns parsing without timezone
    const parsed = parse(dateStr, 'EEE MMM dd HH:mm:ss yyyy', new Date());
    const isValidDate = isValid(parsed);
    console.log('Validating date with date-fns:', { dateStr, parsed, isValid: isValidDate });
    if (isValidDate) return true;

    // Fallback: Replace HKT with GMT+0800
    const fallbackStr = dateStr.replace('HKT', 'GMT+0800');
    const fallback = new Date(fallbackStr);
    const isValidFallback = !isNaN(fallback.getTime());
    console.log('Fallback date parsing:', { dateStr, fallbackStr, parsed: fallback, isValid: isValidFallback });
    return isValidFallback;
  } catch (error) {
    console.error('Date parsing error:', error, { dateStr });
    // Fallback: Replace HKT with GMT+0800
    const fallbackStr = dateStr.replace('HKT', 'GMT+0800');
    const fallback = new Date(fallbackStr);
    const isValidFallback = !isNaN(fallback.getTime());
    console.log('Fallback date parsing:', { dateStr, fallbackStr, parsed: fallback, isValid: isValidFallback });
    return isValidFallback;
  }
};

const formatDate = (dateStr: string) => {
  try {
    // Check if the date string matches the expected format
    const dateRegex = /^[A-Za-z]{3} [A-Za-z]{3} \d{2} \d{2}:\d{2}:\d{2} HKT \d{4}$/;
    if (dateRegex.test(dateStr)) {
      // Return the raw string if it matches the backend format
      console.log('Using raw date string:', { dateStr });
      return dateStr;
    }

    // Try date-fns parsing without timezone
    const parsed = parse(dateStr, 'EEE MMM dd HH:mm:ss yyyy', new Date());
    if (isValid(parsed)) {
      const formatted = format(parsed, "EEE MMM dd yyyy HH:mm:ss 'HKT'");
      console.log('Formatted date with date-fns:', { dateStr, parsed, formatted });
      return formatted;
    }

    // Fallback: Replace HKT with GMT+0800
    const fallbackStr = dateStr.replace('HKT', 'GMT+0800');
    const fallback = new Date(fallbackStr);
    if (!isNaN(fallback.getTime())) {
      const formatted = format(fallback, "EEE MMM dd yyyy HH:mm:ss 'HKT'");
      console.log('Fallback formatted date:', { dateStr, fallbackStr, parsed: fallback, formatted });
      return formatted;
    }

    console.warn('Invalid date format:', { dateStr, parsed });
    return 'N/A';
  } catch (error) {
    console.error('Date formatting error:', error, { dateStr });
    // Fallback: Replace HKT with GMT+0800
    const fallbackStr = dateStr.replace('HKT', 'GMT+0800');
    const fallback = new Date(fallbackStr);
    if (!isNaN(fallback.getTime())) {
      const formatted = fallback.toLocaleString('en-US', {
        weekday: 'short',
        month: 'short',
        day: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false
      }).replace(/(\d{2}:\d{2}:\d{2})/, '$1 HKT').replace(/,/, '');
      console.log('Fallback formatted date:', { dateStr, fallbackStr, parsed: fallback, formatted });
      return formatted;
    }
    return 'N/A';
  }
};

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
    console.log('Processed search results:', data);
    if (data.length === 0) {
      error.value = 'No results returned. The search service may be initializing or the query yielded no matches.';
    }
    results.value = normalizeScores(data);
  } catch (err: any) {
    console.error('Search error:', err);
    error.value = err.message || 'Failed to fetch search results';
    results.value = [];
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  const query = route.query.q as string;
  const offset = parseInt(route.query.offset as string) || 0;
  console.log('Fetching results for query:', query, 'offset:', offset);
  fetchResults(query, offset);
});

watch(
  () => [route.query.q, route.query.offset],
  ([newQuery, newOffset]) => {
    console.log('Query or offset changed:', newQuery, newOffset);
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

const normalizeScores = (results: SearchResponse[]) => {
  if (results.length === 0) return results;

  const scores = results.map(result => result.score || 0);
  const minScore = Math.min(...scores);
  const maxScore = Math.max(...scores);

  return results.map(result => {
    const score = result.score || 0;
    const normalizedScore = maxScore === minScore ? 1 : (score - minScore) / (maxScore - minScore);
    return {
      ...result,
      normalizedScore: normalizedScore.toFixed(2), // Optional: Limit to 2 decimal places
    };
  });
};
const roundToSignificantFigures = (num: number, significantFigures: number): number => {
  return parseFloat(num.toPrecision(significantFigures));
};
</script>

<style scoped>
.search {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding-top: 6vh;
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
  position: relative;
  background: var(--color-background);
  padding: 1rem;
  border-radius: var(--border-radius);
  margin-bottom: 1rem;
  box-shadow: 0.25rem 0.25rem 0.5rem var(--shadow-dark), -0.25rem -0.25rem 0.5rem var(--shadow-light);
}

.score {
  position: absolute;
  top: 0.5rem;
  right: 0.5rem;
  color: var(--color-primary); /* Purple, ~#9966CC */
  font-family: 'Poppins', sans-serif;
  font-size: 0.9rem;
  font-weight: 600;
  margin: 0;
}

.score-value {
  background: var(--color-secondary); /* Secondary color */
  padding: 0.2rem 0.6rem 0.2rem 0.4rem; /* Extra right padding for spacing */
  border-radius: 0.3rem;
}

.result p {
  color: var(--color-text);
  font-family: 'Poppins', sans-serif;
  font-size: 0.9rem;
  margin: 0.5rem 0;
}

.result-url {
  color: var(--color-primary);
  text-decoration: none;
  font-family: 'Poppins', sans-serif;
  font-size: 0.9rem;
  display: inline-block; /* Highlight text length with block behavior */
  padding: 0 1.2rem; /* Padding for highlight */
}

.result-url:hover {
  text-decoration: underline;
  background: var(--color-secondary); /* Secondary color */
}

.snippet {
  color: var(--color-text);
  font-family: 'Poppins', sans-serif;
  font-size: 0.9rem;
  margin: 0.5rem 0;
  border-radius: 16px;
  background: var(--color-background);
  box-shadow: inset 5px 5px 11px #d9d9d9,
  inset -5px -5px 11px #ffffff;
  padding: 1rem;
}

.snippet :deep(b) {
  font-weight: 700;
  background: var(--color-secondary); /* Secondary color */
  padding: 0.1rem 0.2rem;
  border-radius: 0.2rem;
}

.keywords, .parent-links, .child-links {
  margin: 0.5rem 0;
}

.keywords p, .parent-links p, .child-links p {
  margin: 0.2rem 0;
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

#title {
  font-size: x-large;
  font-weight: bold;
}

@media (min-width: 1024px) {
  h1 {
    font-size: 2.5rem;
  }

  .search-container {
    padding: 1.5rem;
  }

  .score {
    font-size: 1rem;
  }

  .result p {
    font-size: 1rem;
  }

  .result-url {
    font-size: 1rem;
  }

  .snippet {
    font-size: 1rem;
  }

  .pagination-button {
    font-size: 1rem;
    padding: 0.8rem 1.5rem;
  }
}
</style>
