<template>
  <Dialog v-model:visible="props.visible" modal header="Header" :style="{ width: '50rem' }" :breakpoints="{ '1199px': '75vw', '575px': '90vw' }">
    <template #header>
        <span class="font-bold white-space-nowrap">Create a new Cellar</span>
    </template>

    <span class="p-float-label">
        <InputText id="name" v-model="name" />
        <label for="name">Cellar's name</label>
    </span>

    <template #footer>
      <Button label="Ok" icon="pi pi-check" @click="submit" autofocus />
    </template>
  </Dialog>
</template>
<script setup lang="ts">
  import {ref} from "#imports";
  import {CellarsApi} from "~/openapi";

  const api = new CellarsApi();
  const props = defineProps({
    visible: Boolean
  });

  const name = ref('');

  function submit() {
    api.createCellar({
      createCellarRequest: {
        name: name.value,
        location: {
          city: 'Paris',
          country: {
            name: 'France',
            code: 'FR',
          },
          point: {
            latitude: 48.853,
            longitude: 2.348,
          }
        }
      }
    });
  }
</script>