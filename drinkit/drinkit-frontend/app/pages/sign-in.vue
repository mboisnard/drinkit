<template>
  <main>
    <h1>Sign in</h1>
    <form
      v-if="!codeSent"
      @submit.prevent="sendCode"
    >
      <label for="email">Email</label>
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
        Send me a code
      </button>
    </form>
    <form
      v-else
      @submit.prevent="signIn"
    >
      <p>We sent a code to {{ email }}.</p>
      <label for="code">Code</label>
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
      <label for="remember-me">Keep me signed in on this device</label>
      <button
        type="submit"
        :disabled="pending"
      >
        Sign in
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
  import { useSignInForm } from '../composables/useSignInForm';

  definePageMeta({ auth: 'public' });
  useSeoMeta({ title: 'Sign in · DrinkIt' });

  const { email, code, rememberMe, codeSent, pending, problem, sendCode, signIn } = useSignInForm();
</script>
