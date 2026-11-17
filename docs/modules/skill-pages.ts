import { mkdirSync, readdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import { dirname, join, relative, resolve } from 'node:path'
import { defineNuxtModule } from '@nuxt/kit'

const repository = 'https://github.com/mboisnard/drinkit/blob/master/'

// Publishes each skill of the repository as pages of the site, under the Skills page: SKILL.md becomes the index of
// its skill, and its other markdown files its subpages. The copies are gitignored and written at each start.
export default defineNuxtModule({
  meta: { name: 'skill-pages' },
  setup(_options, nuxt) {
    const root = resolve(nuxt.options.rootDir, '..')
    const skills = join(root, '.claude/skills')
    const pages = join(nuxt.options.rootDir, 'content/en/1.engineering/6.coding-assistants/3.skills')

    for (const skill of readdirSync(skills, { withFileTypes: true }).filter(entry => entry.isDirectory())) {
      const target = join(pages, skill.name)
      rmSync(target, { recursive: true, force: true })
      mkdirSync(target, { recursive: true })

      for (const file of readdirSync(join(skills, skill.name)).filter(name => name.endsWith('.md'))) {
        const source = join(skills, skill.name, file)
        const page = join(target, file === 'SKILL.md' ? 'index.md' : file)
        writeFileSync(page, toPage(readFileSync(source, 'utf8'), skill.name, file, { source, page, root, skills, pages }))
      }
    }
  },
})

interface Paths { source: string, page: string, root: string, skills: string, pages: string }

function toPage(markdown: string, skill: string, file: string, paths: Paths) {
  const { frontmatter, body } = splitFrontmatter(markdown)
  const heading = body.match(/^# (.+)\n+/)
  const title = file === 'SKILL.md' ? skill : heading?.[1] ?? file.replace(/\.md$/, '')
  const content = file === 'SKILL.md' || !heading ? body : body.slice(heading[0].length)

  return `---\ntitle: ${JSON.stringify(title)}\n${frontmatter}---\n\n${relink(content, paths)}`
}

function splitFrontmatter(markdown: string) {
  const match = markdown.match(/^---\n([\s\S]*?\n)---\n+/)
  const frontmatter = (match?.[1] ?? '').split('\n').filter(line => !line.startsWith('name:')).join('\n')
  return { frontmatter, body: match ? markdown.slice(match[0].length) : markdown }
}

// A link between files of the skills follows the copy, a link to a page of the site points to that page, and a link
// to any other file of the repository goes to GitHub
function relink(body: string, { source, page, root, skills, pages }: Paths) {
  return body.replace(/(\]\(|\]:[ \t]*)((?![a-z]+:|\/|#)[^)\s#]+)(#[^)\s]*)?/g, (match, lead: string, link: string, anchor = '') => {
    const target = resolve(dirname(source), link)
    if (target.startsWith(skills + '/') && target.endsWith('.md')) {
      const copy = join(pages, relative(skills, target)).replace(/SKILL\.md$/, 'index.md')
      return lead + relative(dirname(page), copy) + anchor
    }

    if (target.startsWith(join(root, 'docs/content') + '/')) {
      return lead + relative(dirname(page), target) + anchor
    }

    return target.startsWith(root + '/') ? lead + repository + relative(root, target) + anchor : match
  })
}
