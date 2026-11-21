<template>
  <main>
    <h1>{{ t('home.title') }}</h1>
    <p v-if="greeting">
      {{ greeting }}
    </p>
    <p>{{ t('home.tagline') }}</p>
    <NuxtLink
      v-if="signedIn"
      to="/cellars"
    >
      {{ t('home.myCellars') }}
    </NuxtLink>
    <NuxtLinkLocale
      v-else
      to="/sign-in"
    >
      {{ t('home.signIn') }}
    </NuxtLinkLocale>
  </main>
</template>

<script setup lang="ts">
  import { useSeoMeta } from '#app';
  import { NuxtLink, NuxtLinkLocale } from '#components';
  import { definePageMeta } from '#imports';
  import { useTranslate } from '@drinkit/i18n-starter/translate';
  import { useCurrentUser } from '@drinkit/security-starter/use-current-user';
  import { useGreeting } from '../composables/useGreeting';

  definePageMeta({ auth: 'public' });

  const { t } = useTranslate();
  // No title of its own: the titleTemplate of app.vue then shows the name of the site alone
  useSeoMeta({ description: () => t('home.description') });

  const { signedIn } = useCurrentUser();
  const greeting = useGreeting();
</script>
