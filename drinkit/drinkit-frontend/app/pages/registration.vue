<template>
  <main>
    <h1>Complete your account</h1>
    <form @submit.prevent="register">
      <label for="firstname">First name</label>
      <input
        id="firstname"
        v-model="firstname"
        autocomplete="given-name"
        required
      >
      <label for="lastname">Last name</label>
      <input
        id="lastname"
        v-model="lastname"
        autocomplete="family-name"
        required
      >
      <label for="birthdate">Birth date</label>
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
        Continue
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
  import { useRegistrationForm } from '../composables/useRegistrationForm';

  definePageMeta({ auth: 'ROLE_REGISTRATION_IN_PROGRESS', layout: 'signed-in' });
  useSeoMeta({ title: 'Complete your account · DrinkIt' });

  const { firstname, lastname, birthdate, pending, problem, register } = useRegistrationForm();
</script>
