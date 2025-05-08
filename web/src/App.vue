<template>
  <div id="app">
    <header>
      <div class="left">
        <img
          alt="Manganesium logo"
          class="logo"
          src="@/assets/logo.png"
          width="60"
          height="60"
        />
        <h1>Manganesium</h1>
      </div>
      <nav>
        <RouterLink to="/">Home</RouterLink>
        <RouterLink to="/about">About</RouterLink>
      </nav>
    </header>
    <main>
      <RouterView />
    </main>
    <footer v-if="isLoading || !isReady" class="status">
      <p v-if="isLoading">Loading...</p>
      <p v-else>Search service is initializing. You can try searching, but results may be incomplete.</p>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { RouterLink, RouterView } from 'vue-router';
import { checkHealth, isSearchReady } from './api/search';

const isReady = ref(false);
const isLoading = ref(true);

const check = async () => {
  try {
    isReady.value = await isSearchReady();
  } catch {
    isReady.value = false;
  } finally {
    isLoading.value = false;
  }
};

onMounted(() => {
  check();
  const interval = setInterval(check, 5000);
  onUnmounted(() => clearInterval(interval));
});
</script>

<style scoped>
#app {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

header {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 5rem;
  background: var(--color-background);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 2vw;
  box-shadow: 0 0.5rem 1rem var(--shadow-dark);
  z-index: 1000;
}

.left {
  display: flex;
  align-items: center;
  gap: 1vw;
  margin-left: 2vw;
}

.logo {
  width: 3.5rem;
  height: 3.5rem;
  border-radius: 50%;
  box-shadow: 0.25rem 0.25rem 0.5rem var(--shadow-dark), -0.25rem -0.25rem 0.5rem var(--shadow-light);
}

h1 {
  color: var(--color-primary);
  font-size: 1.5rem;
  font-weight: 600;
  margin: 0;
}

nav {
  display: flex;
  gap: 1vw;
  margin-right: 2vw;
}

nav a {
  padding: 0.5rem 1rem;
  border-radius: var(--border-radius);
  background: var(--color-background);
  color: var(--color-primary);
  text-decoration: none;
  font-size: 0.9rem;
  font-weight: 500;
  box-shadow: 0.25rem 0.25rem 0.5rem var(--shadow-dark), -0.25rem -0.25rem 0.5rem var(--shadow-light);
  transition: all 0.3s ease;
}

nav a:hover {
  background: var(--color-secondary);
  box-shadow: 0.15rem 0.15rem 0.3rem var(--shadow-dark), -0.15rem -0.15rem 0.3rem var(--shadow-light);
}

nav a.router-link-exact-active {
  background: linear-gradient(145deg, #a279d1, #8855b6);
  color: #fff;
}

main {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  padding-top: 6rem;
  padding-bottom: 3rem;
}

.status {
  position: fixed;
  bottom: 0;
  left: 0;
  width: 100vw;
  background: var(--color-background);
  padding: 0.5rem;
  text-align: center;
  box-shadow: 0 -0.3rem 0.6rem var(--shadow-dark);
  border-radius: var(--border-radius) var(--border-radius) 0 0;
}

.status p {
  color: var(--color-text-muted);
  font-size: 0.8rem;
  margin: 0;
}

@media (min-width: 1024px) {
  header {
    padding: 0 3vw;
  }

  .left {
    margin-left: 3vw;
    gap: 1.5vw;
  }

  .logo {
    width: 4rem;
    height: 4rem;
  }

  h1 {
    font-size: 1.8rem;
  }

  nav {
    gap: 1.5vw;
    margin-right: 3vw;
  }

  nav a {
    font-size: 1rem;
    padding: 0.6rem 1.2rem;
  }

  main {
    padding-top: 7rem;
    padding-bottom: 3.5rem;
  }

  .status {
    padding: 0.6rem;
  }

  .status p {
    font-size: 0.9rem;
  }
}
</style>
