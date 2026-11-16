import { joinURL, withoutTrailingSlash } from 'ufo'

// nuxt-agent-discovery builds the links of llms.txt from the origin of the site, without the path GitHub Pages
// serves it under: this hook, registered after the modules' own, puts that path back
export default defineNitroPlugin((nitroApp) => {
  const base = withoutTrailingSlash(useRuntimeConfig().app.baseURL)

  nitroApp.hooks.hook('llms:generate', (_event, options) => {
    const origin = withoutTrailingSlash(options.domain)
    for (const link of options.sections.flatMap(section => section.links ?? [])) {
      if (link.href.startsWith(`${origin}/`) && !link.href.startsWith(`${origin}${base}/`)) {
        link.href = joinURL(origin, base, link.href.slice(origin.length))
      }
    }
  })
})
