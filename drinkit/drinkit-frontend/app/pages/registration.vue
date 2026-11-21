<template>
  <main>
    <h1>{{ t('registration.title') }}</h1>
    <form @submit.prevent="register">
      <label for="firstname">{{ t('registration.firstname') }}</label>
      <input
        id="firstname"
        v-model="firstname"
        autocomplete="given-name"
        required
      >
      <label for="lastname">{{ t('registration.lastname') }}</label>
      <input
        id="lastname"
        v-model="lastname"
        autocomplete="family-name"
        required
      >
      <label for="birthdate">{{ t('registration.birthdate') }}</label>
      <input
        id="birthdate"
        v-model="birthdate"
        type="date"
        autocomplete="bday"
        required
      >
      <button
        type="submit"
        :disabled="pending"
      >
        {{ t('registration.submit') }}
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
  import { useRegistrationForm } from '../composables/useRegistrationForm';

  definePageMeta({ auth: 'ROLE_REGISTRATION_IN_PROGRESS', layout: 'signed-in' });

  const { t } = useTranslate();
  useSeoMeta({ title: () => t('registration.title') });

  const { firstname, lastname, birthdate, pending, problem, register } = useRegistrationForm();
</script>
