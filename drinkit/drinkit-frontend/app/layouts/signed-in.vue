<template>
  <div>
    <header>
      <NuxtLink to="/">
        DrinkIt
      </NuxtLink>
      <span v-if="user?.firstname">{{ user.firstname }}</span>
      <button
        type="button"
        @click="signOut"
      >
        Sign out
      </button>
    </header>
    <slot />
  </div>
</template>

<script setup lang="ts">
  import { NuxtLink } from '#components';
  import { withErrorMessage } from '@drinkit/observability-starter/error-message';
  import { useCurrentUser } from '@drinkit/security-starter/use-current-user';

  const { user, signOut: signOutOfBackend } = useCurrentUser();
  const signOut = withErrorMessage(signOutOfBackend, 'Sign-out failed. Try again.');
</script>
