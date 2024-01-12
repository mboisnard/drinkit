<template>
  <div>
    <div>
      <!-- Hydration issue: https://github.com/primefaces/primevue/issues/5046 -->
      <DataTable :value="cellars" tableStyle="min-width: 50rem" :loading="pending">
        <template #header>
          <Button label="Create a Cellar" @click="showDialog = true"/>
        </template>
        <template #empty> No cellar found. </template>
        <template #loading> Loading cellar data. Please wait. </template>

        <Column field="id" header="Id"></Column>
        <Column field="name" header="Name"></Column>
        <Column header="Delete">
          <template #body="slotProps">
            <Button icon="pi pi-trash" aria-label="Delete" @click="deleteCellar(slotProps.data.id)" />
          </template>
        </Column>
      </DataTable>
    </div>

    <CreateCellarDialog :visible="showDialog"/>
  </div>
</template>
<script setup lang="ts">
  import {type CellarResponse, CellarsApi} from "~/openapi";
  import {ref, useAsyncData} from "#imports";

  const api = new CellarsApi();
  const cellars = ref<CellarResponse[]>([]);
  const showDialog = ref<boolean>(false);

  const { pending } = await useAsyncData('cellars', () => findCellars());

  async function findCellars() {
    const data = await api.findCellars();
    if (data) {
      cellars.value = data.cellars
    }
  }

  async function deleteCellar(cellarId: any) {
    await api.deleteCellar({ cellarId: cellarId.value });
    await findCellars();
  }
</script>