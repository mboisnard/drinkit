<template>
  <div>
    <header>
      <NuxtLinkLocale to="/">
        {{ t('layout.home') }}
      </NuxtLinkLocale>
      <span v-if="user?.firstname">{{ user.firstname }}</span>
      <NuxtLink to="/passkeys">
        {{ t('layout.passkeys') }}
      </NuxtLink>
      <label>
        {{ t('layout.language') }}
        <select
          :value="user?.locale"
          :disabled="pending"
          @change="choose(($event.target as HTMLSelectElement).value)"
        >
          <option
            v-for="language in languages"
            :key="language.code"
            :value="language.code"
          >
            {{ language.name }}
          </option>
        </select>
      </label>
      <button
        type="button"
        @click="signOut"
      >
        {{ t('layout.signOut') }}
      </button>
    </header>
    <p
      v-if="problem"
      role="alert"
    >
      {{ problem }}
    </p>
    <slot />
  </div>
</template>

<script setup lang="ts">
  import { NuxtLink, NuxtLinkLocale } from '#components';
  import { useTranslate } from '@drinkit/i18n-starter/translate';
  import { withErrorMessage } from '@drinkit/observability-starter/error-message';
  import { useCurrentUser } from '@drinkit/security-starter/use-current-user';
  import { useLanguageChoice } from '../composables/useLanguageChoice';

  const { t } = useTranslate();
  const { user, signOut: signOutOfBackend } = useCurrentUser();
  const signOut = withErrorMessage(signOutOfBackend, t('layout.signOutFailed'));
  const { pending, problem, languages, choose } = useLanguageChoice();
</script>
