<template>
  <main>
    <h1>{{ t('passkeys.title') }}</h1>
    <ul v-if="passkeys?.length">
      <li
        v-for="passkey in passkeys"
        :key="passkey.id"
      >
        {{ passkey.label }}
        <span>{{ t('passkeys.item', { created: dateOf(passkey.created), lastUsed: dateOf(passkey.lastUsed) }) }}</span>
        <button
          type="button"
          :aria-label="t('passkeys.removeLabel', { label: passkey.label })"
          :disabled="pending"
          @click="remove(passkey.id)"
        >
          {{ t('passkeys.remove') }}
        </button>
      </li>
    </ul>
    <p v-else>
      {{ t('passkeys.empty') }}
    </p>
    <button
      v-if="canCreate"
      type="button"
      @click="create"
    >
      {{ t('passkeys.add') }}
    </button>
    <p
      v-if="problem"
      role="alert"
    >
      {{ problem }}
    </p>
  </main>
</template>

<script setup lang="ts">
  import { onMounted, ref } from 'vue';
  import { useSeoMeta } from '#app';
  import { definePageMeta } from '#imports';
  import { useTranslate } from '@drinkit/i18n-starter/translate';
  import { usePasskeys } from '@drinkit/security-starter/use-passkeys';
  import { usePasskeyCreation } from '../composables/usePasskeyCreation';
  import { usePasskeyList } from '../composables/usePasskeyList';
  import { usePasskeyRemoval } from '../composables/usePasskeyRemoval';

  definePageMeta({ auth: 'ROLE_USER', layout: 'signed-in' });

  const { t, d } = useTranslate();
  useSeoMeta({ title: () => t('passkeys.title') });

  const { supported } = usePasskeys();
  const { data: passkeys, error, refresh } = await usePasskeyList();
  if (error.value) {
    throw error.value;
  }

  const create = usePasskeyCreation(refresh);
  const { pending, problem, remove } = usePasskeyRemoval(refresh);

  // The server renders no browser, so the button shows once the page knows this one creates passkeys
  const canCreate = ref(false);
  onMounted(() => {
    canCreate.value = supported();
  });

  // shortcut: in UTC, as the app knows no time zone of the user; take theirs once the account keeps one
  function dateOf(instant: string): string {
    return d(new Date(instant), 'date', 'UTC');
  }
</script>
