---
seo:
  title: DrinkIt
  description: Cellar management for wines, spirits, beers and cocktails, and how it is built.
---

::u-page-hero
---
orientation: horizontal
ui:
  root: bg-[url(/drinkit/oak-rings.svg)] bg-no-repeat bg-[position:right_-220px_top_-180px] dark:bg-none
---
#headline
  :::u-badge
  ---
  variant: soft
  class: rounded-full
  ---
  Open source · Kotlin, Spring Boot, Nuxt
  :::

#title
Every bottle, every moment, [shared.]{.font-accent .italic .font-medium .text-primary}

#description
DrinkIt keeps your wines, spirits, beers and cocktails, and the evenings you opened them. It is also an open
engineering sandbox, where every choice is written down.

#links
  :::u-button
  ---
  size: xl
  to: /en/engineering
  trailing-icon: i-lucide-arrow-right
  class: rounded-full
  ---
  Read the engineering
  :::

  :::u-button
  ---
  color: neutral
  icon: i-simple-icons-github
  size: xl
  to: https://github.com/mboisnard/drinkit
  variant: outline
  class: rounded-full
  ---
  GitHub
  :::

#default
  :::home-showcase
  :::
::

::u-page-section
---
ui:
  container: lg:py-24
---
#headline
Product

#title
Everything your cellar [remembers]{.font-accent .italic .font-medium .text-primary}

#description
What DrinkIt is built to do: keep the bottles, the moments they were opened, and the recipes they make.

#body
  :::u-page-grid

    ::::u-page-card
    ---
    title: Cellars and bottles
    description: Wines, champagnes, beers and spirits, with price, quantity, photos and tasting notes.
    icon: i-lucide-wine
    ui:
      leadingIcon: bg-rose-50 text-rose-700 dark:bg-rose-400/15 dark:text-rose-300
    ---
    ::::

    ::::u-page-card
    ---
    title: Moments
    description: When, where and with whom a bottle was opened. Plan one with a reminder, or offer it.
    icon: i-lucide-calendar-heart
    ui:
      leadingIcon: bg-pink-50 text-pink-600 dark:bg-pink-400/15 dark:text-pink-300
    ---
    ::::

    ::::u-page-card
    ---
    title: Scan a label
    description: Find a bottle by name, origin, or a photo of its label read by a vision model.
    icon: i-lucide-scan-line
    ui:
      leadingIcon: bg-blue-50 text-blue-700 dark:bg-blue-400/15 dark:text-blue-300
    ---
    ::::

    ::::u-page-card
    ---
    title: Import and prices
    description: Bring bottles in from Vivino and other sources, and keep their prices up to date.
    icon: i-lucide-download
    ui:
      leadingIcon: bg-yellow-50 text-yellow-700 dark:bg-yellow-400/15 dark:text-yellow-300
    ---
    ::::

    ::::u-page-card
    ---
    title: Cocktails
    description: Recipes whose ingredients link to your bottles, and what you can make tonight.
    icon: i-lucide-martini
    ui:
      leadingIcon: bg-lime-50 text-lime-700 dark:bg-lime-400/15 dark:text-lime-300
    ---
    ::::

    ::::u-page-card
    ---
    title: Sharing
    description: Shared cellars, activity from the people you follow, and bottles they recommend.
    icon: i-lucide-users
    ui:
      leadingIcon: bg-purple-50 text-purple-700 dark:bg-purple-400/15 dark:text-purple-300
    ---
    ::::
  :::
::

::u-page-section
#headline
Engineering

#title
The engineering, [in seven pours]{.font-accent .italic .font-medium .text-primary}

#description
How DrinkIt is built, and why: hexagonal architecture, event sourcing, tech starters and coding agents. Each step
presents a practice, how DrinkIt applies it, and sources to go further.

#body
  :::u-theme
  ---
  ui:
    pageCard:
      container: flex-row items-center gap-3 p-4 sm:p-4
      title: text-sm
      description: text-xs
  ---
    ::::u-page-grid
    ---
    class: lg:grid-cols-4
    ---

      :::::u-page-card
      ---
      icon: i-lucide-hexagon
      to: /en/engineering/architecture/monorepo
      ui:
        leadingIcon: bg-rose-50 text-rose-700 dark:bg-rose-400/15 dark:text-rose-300
      ---
      #title
      [01]{.block .font-mono .text-xs .font-normal .text-muted} The shape of the code

      #description
      Monorepo, modules, hexagon
      :::::

      :::::u-page-card
      ---
      icon: i-lucide-layers
      to: /en/engineering/domain/event-sourcing
      ui:
        leadingIcon: bg-purple-50 text-purple-700 dark:bg-purple-400/15 dark:text-purple-300
      ---
      #title
      [02]{.block .font-mono .text-xs .font-normal .text-muted} Inside the domain

      #description
      Event sourcing, platform events
      :::::

      :::::u-page-card
      ---
      icon: i-lucide-database
      to: /en/engineering/build-and-data/build-tool
      ui:
        leadingIcon: bg-cognac-50 text-cognac-700 dark:bg-cognac-400/15 dark:text-cognac-300
      ---
      #title
      [03]{.block .font-mono .text-xs .font-normal .text-muted} Building and storing

      #description
      Gradle, schema changes
      :::::

      :::::u-page-card
      ---
      icon: i-lucide-code
      to: /en/engineering/writing-code/best-practices/craft-manifesto
      ui:
        leadingIcon: bg-lime-50 text-lime-700 dark:bg-lime-400/15 dark:text-lime-300
      ---
      #title
      [04]{.block .font-mono .text-xs .font-normal .text-muted} Writing code here

      #description
      Practices and tests
      :::::

      :::::u-page-card
      ---
      icon: i-lucide-bot
      to: /en/engineering/coding-assistants/harness
      ui:
        leadingIcon: bg-blue-50 text-blue-700 dark:bg-blue-400/15 dark:text-blue-300
      ---
      #title
      [05]{.block .font-mono .text-xs .font-normal .text-muted} With a coding assistant

      #description
      Harness, hooks, skills
      :::::

      :::::u-page-card
      ---
      icon: i-lucide-git-merge
      to: /en/engineering/delivery/ci-cd
      ui:
        leadingIcon: bg-pink-50 text-pink-600 dark:bg-pink-400/15 dark:text-pink-300
      ---
      #title
      [06]{.block .font-mono .text-xs .font-normal .text-muted} From a change to master

      #description
      CI and the merge gate
      :::::

      :::::u-page-card
      ---
      icon: i-lucide-book-open
      to: /en/decisions
      ui:
        leadingIcon: bg-yellow-50 text-yellow-700 dark:bg-yellow-400/15 dark:text-yellow-300
      ---
      #title
      [07]{.block .font-mono .text-xs .font-normal .text-muted} Why it is this way

      #description
      Every structuring choice
      :::::

      :::::u-page-card
      ---
      icon: i-lucide-arrow-right
      to: /en/engineering
      variant: solid
      ui:
        root: bg-cognac-500 hover:bg-cognac-400
        container: flex-row-reverse items-center justify-between gap-3 p-4 sm:p-4
        title: text-sm text-neutral-900
        leadingIcon: size-5 p-0 text-neutral-900
      ---
      #title
      [New here? Start at step 01.]{.block .text-xs .font-normal} Follow the reading path
      :::::
    ::::
  :::
::
