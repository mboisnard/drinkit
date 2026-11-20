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
  import { useCurrentUser } from '@drinkit/security-starter/use-current-user';

  const { user, signOut } = useCurrentUser();
</script>
