import type { RouterConfig } from '@nuxt/schema'

// Docus declares its pages under an optional /:lang? that i18n prefixes again with the locale, which gives
// /en/:lang? to the landing and /en/:lang?/:slug* to the pages: the landing then takes /en/engineering, the pages
// take /en. With the locale already in the prefix, the landing keeps /en and a page needs at least one segment.
export default {
  routes: routes => routes.map(route => ({
    ...route,
    path: route.path.replace('/:lang?', '').replace(':slug(.*)*', ':slug(.*)+'),
  })),
} satisfies RouterConfig
