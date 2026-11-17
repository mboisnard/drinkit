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

      <p v-if="done < asked.steps.length" class="flex items-center gap-2 text-xs">
        <HomeCellarLoader class="size-5" />
        <Transition name="step" mode="out-in" type="transition">
          <span :key="asked.steps[done]" class="step-at-work inline-block">
            {{ asked.steps[done] }}
          </span>
        </Transition>
      </p>

      <p v-else class="rounded-2xl rounded-es-sm border border-default px-3 py-2 text-sm">
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
      steps: ['Looking through your cellar…', 'Planning the evening…', 'Setting a reminder…'],
      answer: 'Planned for Sat, Dec 6 with the Château Musar 2016. I will remind you on Dec 5.',
    },
    {
      question: 'Import my Vivino cellar',
      steps: ['Reading your Vivino export…', 'Matching your bottles…'],
      answer: '31 bottles imported into the living room cellar, 2 left for you to check.',
    },
    {
      question: 'What is my cellar worth?',
      steps: ['Updating prices…'],
      stepMs: 3200,
      answer: 'About €2,140 today, 6% more than last month.',
    },
  ]

  const props = defineProps<{ autoplay?: boolean }>()

  const asked = shallowRef<typeof chats[number]>()
  const done = ref(0)
  let timer: ReturnType<typeof setTimeout> | undefined

  // One line shows the step at work, then gives way to the answer. Prices take a turn of the loader
  function ask(chat: typeof chats[number]) {
    clearTimeout(timer)
    asked.value = chat
    done.value = 0
    next(chat)
  }

  function next(chat: typeof chats[number]) {
    if (done.value >= chat.steps.length) return
    timer = setTimeout(() => {
      done.value++
      next(chat)
    }, chat.stepMs ?? 1200)
  }

  // The tour asks the first question itself, while a visitor who picked the tab picks the question too
  onMounted(() => {
    if (props.autoplay) ask(chats[0]!)
  })

  onBeforeUnmount(() => clearTimeout(timer))
</script>

<style scoped>
  .step-at-work {
    background: linear-gradient(90deg, var(--ui-text-dimmed), var(--ui-text-highlighted), var(--ui-text-dimmed)) 0 0 / 200% 100%;
    background-clip: text;
    color: transparent;
    animation: shimmer 1.6s linear infinite;
  }

  .step-enter-active,
  .step-leave-active {
    transition: opacity 0.2s ease, transform 0.2s ease;
  }

  .step-enter-from {
    opacity: 0;
    transform: translateY(6px);
  }

  .step-leave-to {
    opacity: 0;
    transform: translateY(-6px);
  }

  @media (prefers-reduced-motion: reduce) {
    .step-at-work {
      animation: none;
    }

    .step-enter-active,
    .step-leave-active {
      transition: none;
    }
  }

  @keyframes shimmer {
    to { background-position: -200% 0; }
  }
</style>
