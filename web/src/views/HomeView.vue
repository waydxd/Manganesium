<template>
  <div class="home">
    <form @submit.prevent="handleSearch" aria-label="Search form" class="search-form">
      <input
        v-model="query"
        type="text"
        placeholder="Enter your search query..."
        aria-label="Search query"
        class="search-input"
      />
    </form>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';

const query = ref('');
const router = useRouter();

const handleSearch = () => {
  if (query.value.trim()) {
    router.push({ path: '/search', query: { q: query.value } });
  }
};
</script>

<style scoped>
.home {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: var(--spacing-unit);
}

h1 {
  color: var(--color-primary);
  font-family: 'Poppins', sans-serif;
  font-size: 2rem;
  font-weight: 600;
  margin-bottom: var(--spacing-unit);
}

.search-form {
  width: 90%;
  max-width: 40rem;
}

.search-input {
  width: 100%;
  padding: 0.8rem;
  border: none;
  border-radius: var(--border-radius);
  background: var(--color-background);
  box-shadow: inset 0.25rem 0.25rem 0.5rem var(--shadow-dark), inset -0.25rem -0.25rem 0.5rem var(--shadow-light);
  font-family: 'Poppins', sans-serif;
  font-size: 1.1rem;
  color: var(--color-text);
}

.search-input:focus {
  outline: none;
  box-shadow: inset 0.15rem 0.15rem 0.3rem var(--shadow-dark), inset -0.15rem -0.15rem 0.3rem var(--shadow-light);
}

@media (min-width: 1024px) {
  h1 {
    font-size: 2.5rem;
  }

  .search-input {
    padding: 1rem;
    font-size: 1.2rem;
  }
}
</style>
