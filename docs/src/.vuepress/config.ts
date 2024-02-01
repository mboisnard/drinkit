import { defaultTheme } from '@vuepress/theme-default'
import { defineUserConfig } from 'vuepress/cli'
import { viteBundler } from '@vuepress/bundler-vite'

export default defineUserConfig({
  lang: 'en-US',

  title: 'Drink It',
  description: 'Drink It Manage your cellars and enjoy your drinks',

  theme: defaultTheme({
    logo: 'https://vuejs.press/images/hero.png',

    navbar: [{
      text: 'Home',
      link: '/'
    }, {
      text: 'Getting Started',
      link: '/guide/get-started'
    }, {
      text: 'Technology',
      link: '/tech'
    }],
  }),

  bundler: viteBundler(),
})
