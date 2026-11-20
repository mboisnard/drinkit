<template>
  <main>
    <h1>DrinkIt</h1>
    <p v-if="greeting">
      {{ greeting }}
    </p>
    <p>Your wine and spirit cellars, in one place.</p>
    <NuxtLink
      v-if="signedIn"
      to="/cellars"
    >
      My cellars
    </NuxtLink>
    <NuxtLink
      v-else
      to="/sign-in"
    >
      Sign in
    </NuxtLink>
  </main>
</template>

<script setup lang="ts">
  import { useSeoMeta } from '#app';
  import { NuxtLink } from '#components';
  import { definePageMeta } from '#imports';
  import { useCurrentUser } from '@drinkit/security-starter/use-current-user';
  import { useGreeting } from '../composables/useGreeting';

  definePageMeta({ auth: 'public' });

  useSeoMeta({
    title: 'DrinkIt',
    description: 'Manage your wine and spirit cellars: what you hold, where it lies and when to open it.',
  });

  const { signedIn } = useCurrentUser();
  const greeting = useGreeting();
</script>
