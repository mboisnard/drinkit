<template>
  <main>
    <h1>{{ t('signIn.title') }}</h1>
    <form
      v-if="!codeSent"
      @submit.prevent="sendCode"
    >
      <label for="email">{{ t('signIn.email') }}</label>
      <input
        id="email"
        v-model="email"
        type="email"
        autocomplete="email"
        required
      >
      <button
        type="submit"
        :disabled="pending"
      >
        {{ t('signIn.sendCode') }}
      </button>
      <button
        v-if="canUsePasskey"
        type="button"
        :disabled="pending"
        @click="signInWithPasskey"
      >
        {{ t('signIn.withPasskey') }}
      </button>
    </form>
    <form
      v-else
      @submit.prevent="signIn"
    >
      <p>{{ t('signIn.codeSent', { email }) }}</p>
      <label for="code">{{ t('signIn.code') }}</label>
      <input
        id="code"
        v-model="code"
        inputmode="numeric"
        autocomplete="one-time-code"
        pattern="[0-9]{6}"
        maxlength="6"
        required
      >
      <input
        id="remember-me"
        v-model="rememberMe"
        type="checkbox"
      >
      <label for="remember-me">{{ t('signIn.rememberMe') }}</label>
      <button
        type="submit"
        :disabled="pending"
      >
        {{ t('signIn.submit') }}
      </button>
    </form>
    <p
      v-if="problem"
      role="alert"
    >
      {{ problem }}
    </p>
  </main>
</template>

<script setup lang="ts">
  import { useSeoMeta } from '#app';
  import { definePageMeta } from '#imports';
  import { useTranslate } from '@drinkit/i18n-starter/translate';
  import { useSignInForm } from '../composables/useSignInForm';

  definePageMeta({ auth: 'public' });

  const { t } = useTranslate();
  useSeoMeta({ title: () => t('signIn.title') });

  const {
    canUsePasskey, email, code, rememberMe, codeSent, pending, problem, sendCode, signIn, signInWithPasskey,
  } = useSignInForm();
</script>
