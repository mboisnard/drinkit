<!-- Docus's page, with the header of the A+ design: the tile and step of the folder, the facts of the page, and for a
     decision record its status and its summary set apart. The copy menu moves under the table of contents when the
     page has one. Keep the rest in step with Docus. -->
<template>
  <UPage
    v-if="page"
    :ui="isOpen ? { center: 'lg:col-span-10' } : undefined"
  >
    <UPageHeader
      :title="record ? page.title.replace(/^\d{3}\.\s*/, '') : page.title"
      :description="record ? undefined : page.description"
      :headline="headline"
      :ui="{
        wrapper: 'flex-row items-center flex-wrap justify-between',
      }"
    >
      <template #headline>
        <DocsPageHeadline
          :path="page.path"
          :record="record"
        />
      </template>

      <template #links>
        <UButton
          v-for="(link, index) in page.links"
          :key="index"
          size="sm"
          v-bind="link"
        />

        <LazyDocsPageHeaderLinks
          hydrate-on-idle
          :class="{ 'lg:hidden': page.body?.toc?.links?.length }"
        />
      </template>

      <DocsPageFacts :facts="page.meta" />
    </UPageHeader>

    <UPageBody>
      <ContentRenderer
        v-if="page"
        :value="page"
        :class="{ 'decision-record': record }"
      />

      <USeparator v-if="github">
        <div
          class="flex items-center gap-2 text-sm text-muted"
        >
          <UButton
            variant="link"
            color="neutral"
            :to="editLink"
            target="_blank"
            icon="i-lucide-pen"
            :ui="{ leadingIcon: 'size-4' }"
          >
            {{ t('docs.edit') }}
          </UButton>
          <template v-if="github?.url">
            <span>{{ t('common.or') }}</span>
            <UButton
              variant="link"
              color="neutral"
              :to="`${github.url}/issues/new/choose`"
              target="_blank"
              icon="i-lucide-alert-circle"
              :ui="{ leadingIcon: 'size-4' }"
            >
              {{ t('docs.report') }}
            </UButton>
          </template>
        </div>
      </USeparator>
      <LazyUContentSurround
        :surround="surround"
        hydrate-on-visible
      />
    </UPageBody>

    <template
      v-if="!isOpen"
      #right
    >
      <DocsAsideRight
        :page="page"
      />
    </template>
  </UPage>
</template>

<script setup lang="ts">
  import { computed, inject, ref, watch } from 'vue';
  import type { Ref } from 'vue';
  import {
    ContentRenderer,
    DocsAsideRight,
    DocsPageFacts,
    DocsPageHeadline,
    LazyDocsPageHeaderLinks,
    LazyUContentSurround,
    UButton,
    UPage,
    UPageBody,
    UPageHeader,
    USeparator,
  } from '#components';
  import {
    createError,
    defineOgImage,
    definePageMeta,
    findPageBreadcrumbs,
    formatOgDescription,
    formatOgTitle,
    queryCollection,
    queryCollectionItemSurroundings,
    useAppConfig,
    useAssistant,
    useAsyncData,
    useDocusI18n,
    useRoute,
    useSeo,
  } from '#imports';
  import type { ContentNavigationItem, Collections } from '@nuxt/content';
  import { findPageHeadline } from '@nuxt/content/utils';
  import { kebabCase } from 'scule';
  import type { Facts } from '~/types/page-facts';

  type DocsPage = Collections['docs_en'] & { meta: Facts };

  definePageMeta({
    layout: 'docs',
  });

  const route = useRoute();
  const { locale, isEnabled, t } = useDocusI18n();
  const { isOpen } = useAssistant();
  const appConfig = useAppConfig();
  const navigation = inject<Ref<ContentNavigationItem[]>>('navigation');
  const collectionName = computed(() => isEnabled.value ? `docs_${locale.value.toLowerCase().replace(/-/g, '_')}` : 'docs');

  const [{ data: page }, { data: surround }] = await Promise.all([
    useAsyncData(kebabCase(route.path), () => queryCollection(collectionName.value as keyof Collections)
      .path(route.path)
      .first() as Promise<DocsPage>),
    useAsyncData(`${kebabCase(route.path)}-surround`, () => {
      return queryCollectionItemSurroundings(collectionName.value as keyof Collections, route.path, {
        fields: ['description'],
      });
    }),
  ]);

  if (!page.value) {
    throw createError({ statusCode: 404, statusMessage: 'Page not found', fatal: true });
  }

  const title = page.value.seo.title ?? page.value.title;
  const description = page.value.seo.description ?? page.value.description;

  const record = page.value.meta.status ? (/^(\d{3})\./.exec(page.value.title))?.[1] : undefined;

  const headline = ref(findPageHeadline(navigation?.value, page.value.path));
  const breadcrumbs = computed(() => findPageBreadcrumbs(navigation?.value, page.value?.path ?? ''));

  useSeo({
    title,
    description,
    type: 'article',
    modifiedAt: page.value.meta.updatedAt,
    breadcrumbs,
  });
  watch(() => navigation?.value, () => {
    headline.value = findPageHeadline(navigation?.value, page.value?.path) ?? headline.value;
  });

  defineOgImage('Docs', {
    headline: headline.value,
    title: formatOgTitle(title),
    description: formatOgDescription(title, description),
  });

  const github = computed(() => appConfig.github ? appConfig.github : null);

  const editLink = computed(() => {
    if (!github.value) {
      return;
    }

    return [
      github.value.url,
      'edit',
      github.value.branch,
      github.value.rootDir,
      'content',
      `${page.value?.stem ?? ''}.${page.value?.extension ?? ''}`,
    ].filter(Boolean).join('/');
  });
</script>
