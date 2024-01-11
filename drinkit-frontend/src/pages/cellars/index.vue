<template>
  <div>
    <div v-if="cellars">
      <DataTable :value="cellars" tableStyle="min-width: 50rem">
        <Column field="id" header="Id"></Column>
        <Column field="name" header="Name"></Column>
      </DataTable>
    </div>
  </div>
</template>
<script setup lang="ts">
  import {type CellarResponse, CellarsApi} from "~/openapi";
  import {ref, useAsyncData} from "#imports";

  const api = new CellarsApi();
  const cellars = ref<CellarResponse[]>();

  const { data, error } = await useAsyncData('cellars', () => api.findCellars())
  if (data.value) {
    cellars.value = data.value.cellars
  }
</script>