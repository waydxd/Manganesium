<template>
  <div class="home">
    <img
      alt="Manganesium logo"
      class="logo"
      src="@/assets/logo.png"
      width="60"
      height="60"
    />
    <div class="header">
      <h1>Manganesium</h1>
    </div>
    <form @submit.prevent="handleSearch" aria-label="Search form" class="search-form">
      <input
        v-model="query"
        type="text"
        placeholder="Enter your search query..."
        aria-label="Search query"
        class="search-input"
      />
      <button type="submit" class="search-button" aria-label="Submit search">
        <font-awesome-icon :icon="['fas', 'magnifying-glass']" />
      </button>
    </form>

    <!-- Display Search History -->
    <div v-if="searchHistory.length > 0" class="search-history">
      <h3>Recent Searches:</h3>
      <ul>
        <li v-for="(item, index) in searchHistory" :key="index">
          <a href="#" @click.prevent="handleHistorySearch(item)">{{ item }}</a>
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';

const query = ref('');
const router = useRouter();
const searchHistory = ref<string[]>([]);

const LOCAL_STORAGE_KEY = 'searchHistory';
const MAX_HISTORY_LENGTH = 5;

// Load search history from local storage on component mount
onMounted(() => {
  loadSearchHistory();
});

const handleSearch = () => {
  if (query.value.trim()) {
    saveSearchQuery(query.value.trim());
    router.push({ path: '/search', query: { q: query.value } });
  }
};

const handleHistorySearch = (historyItem: string) => {
  query.value = historyItem;
  saveSearchQuery(historyItem); // Save the history item to local storage
  router.push({ path: '/search', query: { q: historyItem } });
};

// Save search query to local storage
const saveSearchQuery = (searchQuery: string) => {
  let history = [...searchHistory.value];

  // Check if the query already exists in history
  if (!history.includes(searchQuery)) {
    history.unshift(searchQuery); // Add to the beginning
    if (history.length > MAX_HISTORY_LENGTH) {
      history.pop(); // Remove the oldest entry
    }
    searchHistory.value = history;
    localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(history));
  }
};

// Load search history from local storage
const loadSearchHistory = () => {
  const storedHistory = localStorage.getItem(LOCAL_STORAGE_KEY);
  if (storedHistory) {
    searchHistory.value = JSON.parse(storedHistory);
  }
};
</script>

<style scoped>
.home {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  height: 100%;
  padding: var(--spacing-unit);
  padding-bottom: 3vh;
}

.header {
  display: flex;
  align-items: center;
  gap: 1vw;
  margin-bottom: 1.5rem;
}

.logo {
  width: 10rem !important;
  height: 10rem !important;
  border-radius: 50%;
  box-shadow: 0.25rem 0.25rem 0.5rem var(--shadow-dark), -0.25rem -0.25rem 0.5rem var(--shadow-light);
}

h1 {
  color: var(--color-primary);
  font-family: 'Poppins', sans-serif;
  font-size: 2rem;
  font-weight: 600;
  margin: 0;
}

.search-form {
  display: flex;
  align-items: center;
  gap: 1.5rem; /* Increased separation */
  width: 50rem;
  max-width: 60rem; /* Longer search bar */
}

.search-input {
  flex: 1;
  padding: 0.8rem;
  border: none;
  border-radius: var(--border-radius);
  background: var(--color-background);
  box-shadow: inset 0.25rem 0.25rem 0.5rem var(--shadow-dark), inset -0.25rem -0.25rem 0.5rem var(--shadow-light);
  font-family: 'Poppins', sans-serif;
  font-size: 1.1rem;
  color: var(--color-text);
  height: 3rem;
}

.search-input:focus {
  outline: none;
  box-shadow: inset 0.15rem 0.15rem 0.3rem var(--shadow-dark), inset -0.15rem -0.15rem 0.3rem var(--shadow-light);
}

.search-button {
  background: var(--color-primary);
  border-radius: 100px;
  box-shadow: rgba(153, 102, 204, 0.2) 0 -25px 18px -14px inset, rgba(153, 102, 204, 0.15) 0 1px 2px, rgba(153, 102, 204, 0.15) 0 2px 4px, rgba(153, 102, 204, 0.15) 0 4px 8px, rgba(153, 102, 204, 0.15) 0 8px 16px, rgba(153, 102, 204, 0.15) 0 16px 32px;
  color: #fff;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-family: 'Poppins', sans-serif;
  padding: 0 1.5rem;
  text-align: center;
  text-decoration: none;
  transition: all 250ms;
  border: 0;
  font-size: 1rem;
  user-select: none;
  -webkit-user-select: none;
  touch-action: manipulation;
  height: 3rem;
  width: 3rem;
}

.search-button:hover {
  box-shadow: rgba(153, 102, 204, 0.35) 0 -25px 18px -14px inset, rgba(153, 102, 204, 0.25) 0 1px 2px, rgba(153, 102, 204, 0.25) 0 2px 4px, rgba(153, 102, 204, 0.25) 0 4px 8px, rgba(153, 102, 204, 0.25) 0 8px 16px, rgba(153, 102, 204, 0.25) 0 16px 32px;
  background: var(--color-secondary);
  transform: scale(1.05) rotate(-1deg);
}

.search-button svg {
  font-size: 1.2rem;
}

.search-history {
  margin-top: 20px;
  width: 50rem;
  max-width: 60rem;
  text-align: left;
}

.search-history h3 {
  font-size: 1.2rem;
  margin-bottom: 0.5rem;
  color: var(--color-primary);
}

.search-history ul {
  list-style: none;
  padding: 0;
}

.search-history li {
  margin-bottom: 0.5rem;
}

.search-history a {
  color: var(--color-text);
  text-decoration: none;
  cursor: pointer;
}

.search-history a:hover {
  text-decoration: underline;
  color: var(--color-secondary);
}

@media (min-width: 1024px) {
  .header {
    gap: 1.5vw;
    margin-bottom: 2rem;
  }

  .logo {
    width: 4rem;
    height: 4rem;
  }

  h1 {
    font-size: 2.5rem;
  }

  .search-form {
    max-width: 70rem; /* Even longer on desktop */
    gap: 2rem; /* Larger separation */
  }

  .search-input {
    padding: 1rem;
    font-size: 1.2rem;
    height: 3.5rem;
  }

  .search-button {
    height: 3.5rem;
    width: 3.5rem;
    padding: 0 1.8rem;
  }

  .search-button svg {
    font-size: 1.4rem;
  }

  .search-history {
    max-width: 70rem;
  }
}
</style>
