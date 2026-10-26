# Platform Tech Radar

<script setup>
    import TechRadar from '../../../components/TechRadar.vue'
</script>

<TechRadar
    title="Platform Tech Radar"
    :quadrants="['Platforms', 'Tools', 'Coding Assistants', 'Techniques']"
    :entries='[
    {
      "quadrant": "Platforms",
      "ring": "ADOPT",
      "label": "GitHub Actions",
      "description": "Runs the CI of every pull request and the deployment of this site",
      "link": "/drinkit/engineering/delivery/ci-cd.html"
    },
    {
      "quadrant": "Platforms",
      "ring": "ADOPT",
      "label": "GitHub Pages",
      "description": "Hosts this documentation site, built on every push to master",
      "link": "https://pages.github.com/"
    },
    {
      "quadrant": "Platforms",
      "ring": "ADOPT",
      "label": "Docker Compose",
      "description": "Starts the services the application needs locally",
      "link": "https://docs.docker.com/compose/"
    },
    {
      "quadrant": "Tools",
      "ring": "ADOPT",
      "label": "Renovate",
      "description": "Opens the dependency update pull requests, and merges the CI tooling ones once CI is green",
      "link": "https://docs.renovatebot.com/"
    },
    {
      "quadrant": "Tools",
      "ring": "ADOPT",
      "label": "actionlint",
      "description": "Checks the syntax and the expressions of every workflow",
      "link": "https://github.com/rhysd/actionlint"
    },
    {
      "quadrant": "Tools",
      "ring": "ADOPT",
      "label": "zizmor",
      "description": "Finds the security flaws of the workflows",
      "link": "https://docs.zizmor.sh/"
    },
    {
      "quadrant": "Tools",
      "ring": "ADOPT",
      "label": "ShellCheck",
      "description": "Lints the shell scripts of the git and agent hooks",
      "link": "https://www.shellcheck.net/"
    },
    {
      "quadrant": "Tools",
      "ring": "ADOPT",
      "label": "bats",
      "description": "Tests the hooks, under several shells",
      "link": "https://github.com/bats-core/bats-core"
    },
    {
      "quadrant": "Tools",
      "ring": "ADOPT",
      "label": "VitePress",
      "description": "Builds this documentation site from markdown",
      "link": "https://vitepress.dev/"
    },
    {
      "quadrant": "Tools",
      "ring": "ADOPT",
      "label": "Mermaid",
      "description": "Diagrams written as text in the documentation",
      "link": "https://mermaid.js.org/"
    },
    {
      "quadrant": "Tools",
      "ring": "TRIAL",
      "label": "CodeQL",
      "description": "Code scanning on every pull request, not blocking the merge",
      "link": "https://codeql.github.com/"
    },
    {
      "quadrant": "Tools",
      "ring": "TRIAL",
      "label": "OWASP ZAP",
      "description": "Scans the running application for vulnerabilities, run by hand",
      "link": "https://www.zaproxy.org/"
    },
    {
      "quadrant": "Tools",
      "ring": "ASSESS",
      "label": "Docus",
      "description": "A Nuxt-based documentation framework, weighed against VitePress",
      "link": "https://docus.dev/"
    },
    {
      "quadrant": "Coding Assistants",
      "ring": "ADOPT",
      "label": "Claude Code",
      "description": "The coding agent of the project, guided by AGENTS.md, skills and hooks",
      "link": "https://code.claude.com/docs/en/overview"
    },
    {
      "quadrant": "Coding Assistants",
      "ring": "ADOPT",
      "label": "Agent Skills",
      "description": "One folder of instructions per kind of change, loaded when the agent needs it",
      "link": "https://agentskills.io/"
    },
    {
      "quadrant": "Coding Assistants",
      "ring": "TRIAL",
      "label": "Review Agents",
      "description": "A fresh agent judges a branch against its issue, another reviews its security, before the pull request",
      "link": "/drinkit/engineering/delivery/harness.html"
    },
    {
      "quadrant": "Coding Assistants",
      "ring": "ASSESS",
      "label": "Gradle Skills",
      "description": "The agent skills Gradle publishes, such as the audit of a build against its best practices",
      "link": "https://github.com/gradle/gradle-skills"
    },
    {
      "quadrant": "Techniques",
      "ring": "ADOPT",
      "label": "Agent Harness",
      "description": "Hooks, rules and checks that keep humans and coding agents from breaking master",
      "link": "/drinkit/engineering/delivery/harness.html"
    },
    {
      "quadrant": "Techniques",
      "ring": "ADOPT",
      "label": "Path-Based CI Lanes",
      "description": "A pull request runs only the jobs its changed files can affect",
      "link": "/drinkit/engineering/delivery/ci-cd.html"
    },
    {
      "quadrant": "Techniques",
      "ring": "ADOPT",
      "label": "Actions Pinned by SHA",
      "description": "Every workflow references an action by commit, never by a tag that can move",
      "link": "https://docs.github.com/en/actions/security-for-github-actions/security-guides/security-hardening-for-github-actions#using-third-party-actions"
    },
    {
      "quadrant": "Techniques",
      "ring": "ADOPT",
      "label": "Dependency Verification",
      "description": "The build checks the checksum of every artifact it downloads",
      "link": "/drinkit/engineering/build-and-data/build-tool.html#trusting-what-the-build-downloads"
    },
    {
      "quadrant": "Techniques",
      "ring": "ADOPT",
      "label": "Pull Requests Only",
      "description": "Master changes through a pull request with a green CI gate, and nothing else",
      "link": "/drinkit/engineering/delivery/ci-cd.html"
    },
    {
      "quadrant": "Techniques",
      "ring": "ADOPT",
      "label": "Monorepo",
      "description": "The application, its libraries, its build and its documentation in one repository",
      "link": "/drinkit/engineering/architecture/monorepo.html"
    },
    {
      "quadrant": "Techniques",
      "ring": "ADOPT",
      "label": "Architecture Decision Records",
      "description": "Each structuring choice is recorded with its context and the options it beat",
      "link": "/drinkit/engineering/adr/"
    },
    {
      "quadrant": "Techniques",
      "ring": "ADOPT",
      "label": "Docs as Code",
      "description": "The documentation lives in the repository and changes in the same pull request as the code",
      "link": "https://www.writethedocs.org/guide/docs-as-code/"
    },
    ]'
/>
