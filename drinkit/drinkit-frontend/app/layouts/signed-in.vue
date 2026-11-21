<template>
  <div>
    <header>
      <NuxtLinkLocale to="/">
        {{ t('layout.home') }}
      </NuxtLinkLocale>
      <span v-if="user?.firstname">{{ user.firstname }}</span>
      <button
        type="button"
        @click="signOut"
      >
        {{ t('layout.signOut') }}
      </button>
    </header>
    <slot />
  </div>
</template>

<script setup lang="ts">
  import { NuxtLinkLocale } from '#components';
  import { useTranslate } from '@drinkit/i18n-starter/translate';
  import { withErrorMessage } from '@drinkit/observability-starter/error-message';
  import { useCurrentUser } from '@drinkit/security-starter/use-current-user';

  const { t } = useTranslate();
  const { user, signOut: signOutOfBackend } = useCurrentUser();
  const signOut = withErrorMessage(signOutOfBackend, t('layout.signOutFailed'));
</script>
