import { execFileSync } from 'node:child_process'
import { defineNuxtModule } from '@nuxt/kit'

const wordsPerMinute = 200

// The reading time and the day of the last change of a page, shown under its title. The day comes from git, which
// is why the docs job checks out the whole history; a page git does not track, such as a generated one, has none.
export default defineNuxtModule({
  meta: { name: 'page-facts' },
  setup(_options, nuxt) {
    nuxt.hook('content:file:afterParse', ({ file, content }) => {
      if (!file.path.endsWith('.md')) return

      const words = file.body.split(/\s+/).filter(Boolean).length
      content.meta = {
        ...content.meta as object,
        readingTime: Math.max(1, Math.round(words / wordsPerMinute)),
        updatedAt: lastChange(file.path),
      }
    })
  },
})

function lastChange(path: string): string | undefined {
  try {
    return execFileSync('git', ['log', '-1', '--format=%cs', '--', path], { encoding: 'utf8' }).trim() || undefined
  }
  catch {
    return undefined
  }
}
