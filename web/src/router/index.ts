// src/router/router.ts (apply this change manually)
import { createRouter, createWebHistory } from 'vue-router';
import HomeView from '../views/HomeView.vue';
import SearchView from '../views/SearchView.vue';

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/about',
      name: 'about',
      component: () => import('../views/AboutView.vue'),
    },
    {
      path: '/search',
      name: 'search',
      component: SearchView,
      props: (route) => ({
        query: route.query.q || '',
        offset: parseInt(route.query.offset as string) || 0,
      }),
    },
  ],
});

export default router;
