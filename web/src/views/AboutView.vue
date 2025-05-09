<template>
  <div class="about">
    <h1>About Manganesium</h1>
    <div class="content">
      <p>
        Manganesium is a modern search engine built for efficiency and simplicity.
        Powered by a custom web crawler and a robust backend, it delivers fast and
        relevant search results. This project is developed as part of COMP4321,
        showcasing advanced web technologies and search algorithms.
      </p>
      <p>
        Our mission is to provide an intuitive search experience with a sleek,
        neumorphic design. Try it out on the <RouterLink to="/">Home</RouterLink>
        page!
      </p>
    </div>
  </div>
</template>

<style scoped>
.about {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: var(--spacing-unit);
  width: 90%;
  max-width: 40rem;
  margin: 0 auto;
}

h1 {
  color: var(--color-primary);
  font-family: 'Poppins', sans-serif;
  font-size: 2rem;
  font-weight: 600;
  margin-bottom: 3vh;
}

.content {
  background: var(--color-background);
  padding: var(--spacing-unit);
  border-radius: var(--border-radius);
  box-shadow: 0.5rem 0.5rem 1rem var(--shadow-dark), -0.5rem -0.5rem 1rem var(--shadow-light);
  width: 100%;
}

p {
  color: var(--color-text);
  font-family: 'Poppins', sans-serif;
  font-size: 1rem;
  margin-bottom: 0.8rem;
}

a {
  color: var(--color-primary);
  text-decoration: none;
  font-weight: 500;
}

a:hover {
  background: var(--color-secondary);
  text-decoration: underline;
}

@media (min-width: 1024px) {
  h1 {
    font-size: 2.5rem;
  }

  .content {
    padding: 1.5rem;
  }

  p {
    font-size: 1.1rem;
  }
}
</style>
