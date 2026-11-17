<template>
  <div class="flex h-full flex-col gap-4">
    <div class="flex flex-wrap items-center gap-2">
      <UButton
        v-for="chat in chats"
        :key="chat.question"
        size="xs"
        color="neutral"
        :variant="asked === chat ? 'solid' : 'soft'"
        @click="ask(chat)"
      >
        {{ chat.question }}
      </UButton>
      <UBadge v-if="!asked" variant="soft" icon="i-lucide-pointer">
        Pick a question
      </UBadge>
    </div>

    <div v-if="asked" class="flex flex-1 flex-col gap-3">
      <p class="self-end rounded-2xl rounded-ee-sm bg-elevated px-3 py-2 text-sm">
        {{ asked.question }}
      </p>

      <ul class="flex flex-col gap-1.5">
        <li v-for="(tool, index) in asked.tools" :key="tool" v-show="index <= done" class="flex items-center gap-2 text-xs text-muted">
          <UIcon v-if="index < done" name="i-lucide-circle-check" class="size-4 text-success" />
          <HomeCellarLoader v-else-if="tool === 'refreshPrices'" class="size-5" />
          <UIcon v-else name="i-lucide-loader-circle" class="size-4 animate-spin motion-reduce:animate-none" />
          <span class="font-mono">{{ tool }}</span>
          <span v-if="tool === 'refreshPrices' && index === done">Updating prices…</span>
        </li>
      </ul>

      <p v-if="done > asked.tools.length" class="rounded-2xl rounded-es-sm border border-default px-3 py-2 text-sm">
        {{ asked.answer }}
      </p>
    </div>

    <div class="mt-auto flex items-center gap-2 rounded-full border border-default px-4 py-2 text-sm text-muted">
      <UIcon name="i-lucide-sparkles" class="size-4 text-primary" />
      Ask DrinkIt anything it can do
    </div>
  </div>
</template>

<script setup lang="ts">
  const chats = [
    {
      question: "Plan Tom's 40th",
      tools: ['searchBottles', 'planMoment', 'scheduleReminder'],
      answer: 'Planned for Sat, Dec 6 with the Château Musar 2016. I will remind you on Dec 5.',
    },
    {
      question: 'Import my Vivino cellar',
      tools: ['importVivino', 'matchBottles'],
      answer: '31 bottles imported into the living room cellar, 2 left for you to check.',
    },
    {
      question: 'What is my cellar worth?',
      tools: ['refreshPrices'],
      answer: 'About €2,140 today, 6% more than last month.',
    },
  ]

  const asked = shallowRef<typeof chats[number]>()
  const done = ref(0)
  let timer: ReturnType<typeof setTimeout> | undefined

  // Each tool step finishes after the previous one, then the answer comes. Prices take a turn of the loader
  function ask(chat: typeof chats[number]) {
    clearTimeout(timer)
    asked.value = chat
    done.value = 0
    next(chat)
  }

  function next(chat: typeof chats[number]) {
    if (done.value > chat.tools.length) return
    timer = setTimeout(() => {
      done.value++
      next(chat)
    }, chat.tools[done.value] === 'refreshPrices' ? 3200 : 1200)
  }

  onBeforeUnmount(() => clearTimeout(timer))
</script>
