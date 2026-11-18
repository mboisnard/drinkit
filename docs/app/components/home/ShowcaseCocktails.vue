<template>
  <div class="flex flex-col gap-4">
    <div class="flex items-start justify-between gap-3">
      <div>
        <p class="font-display text-lg font-bold">
          What can I make tonight?
        </p>
        <p class="text-sm text-muted">
          Matched against the 12 bottles of My Bar
        </p>
      </div>
      <UButton
        size="sm"
        color="neutral"
        variant="outline"
        icon="i-lucide-camera"
      >
        Snap bottles
      </UButton>
    </div>

    <ul class="flex flex-col gap-1">
      <li
        v-for="item in cocktails"
        :key="item.name"
      >
        <button
          type="button"
          class="flex w-full items-center gap-3 rounded-xl border px-2.5 py-2 text-start transition-colors"
          :class="picked === item.name ? 'border-default bg-elevated' : 'border-transparent hover:bg-elevated/60'"
          @click="picked = item.name"
        >
          <HomeDrinkTile
            icon="i-lucide-martini"
            :tile="item.tile"
            small
          />
          <span class="min-w-0 flex-1">
            <span class="block text-sm font-semibold">{{ item.name }}</span>
            <span class="block truncate text-xs text-muted">{{ ingredientNames(item) }}</span>
          </span>
          <UIcon
            v-if="item.favorite"
            name="i-lucide-heart"
            class="size-4 fill-current text-pink-600"
          />
          <UBadge
            :color="missing(item) ? 'error' : 'success'"
            variant="soft"
            size="sm"
            class="font-mono"
          >
            {{ item.ingredients.length - missing(item) }}/{{ item.ingredients.length }}
          </UBadge>
        </button>
      </li>
    </ul>

    <ul
      v-if="cocktail"
      class="flex flex-col gap-2 rounded-2xl border border-dashed border-default px-3.5 py-3"
    >
      <li
        v-for="ingredient in cocktail.ingredients"
        :key="ingredient.name"
        class="flex items-center gap-2 text-sm"
      >
        <UIcon
          :name="ingredient.from ? 'i-lucide-circle-check' : 'i-lucide-circle-dashed'"
          :class="['size-4', ingredient.from ? 'text-success' : 'text-error']"
        />
        {{ ingredient.name }}
        <span
          v-if="ingredient.from !== undefined"
          class="ms-auto text-xs text-muted"
        >{{ ingredient.from }}</span>
        <UButton
          v-else
          size="xs"
          variant="link"
          class="ms-auto p-0"
          @click="listed = true"
        >
          {{ listed ? 'On the shopping list' : 'Add to shopping list' }}
        </UButton>
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
  import { computed, ref } from 'vue';
  import { HomeDrinkTile, UBadge, UButton, UIcon } from '#components';

  interface Cocktail {
    name: string;
    tile: string;
    favorite?: boolean;
    ingredients: Array<{ name: string; from?: string }>;
  }

  const cocktails: Cocktail[] = [
    {
      name: 'Last Word',
      tile: 'lime',
      favorite: true,
      ingredients: [
        { name: 'Gin', from: 'Monkey 47' },
        { name: 'Green Chartreuse', from: 'My Bar' },
        { name: 'Maraschino', from: 'Luxardo' },
        { name: 'Lime', from: 'fresh' },
      ],
    },
    {
      name: 'Old Fashioned',
      tile: 'orange',
      ingredients: [{ name: 'Bourbon', from: 'Woodford Reserve' }, { name: 'Sugar', from: '' }, { name: 'Angostura', from: 'My Bar' }],
    },
    {
      name: 'Negroni',
      tile: 'purple',
      ingredients: [{ name: 'Gin', from: 'Monkey 47' }, { name: 'Sweet vermouth', from: 'Cocchi' }, { name: 'Campari' }],
    },
  ];

  const picked = ref('Last Word');
  const listed = ref(false);
  const cocktail = computed(() => cocktails.find((item) => item.name === picked.value));

  function ingredientNames(item: Cocktail): string {
    return item.ingredients.map((ingredient) => ingredient.name).join(' · ');
  }

  function missing(item: Cocktail) {
    return item.ingredients.filter((ingredient) => ingredient.from === undefined).length;
  }
</script>
