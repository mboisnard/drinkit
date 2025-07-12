import { h } from 'vue'
import DefaultTheme from 'vitepress/theme'
import './wooden-barrel.css'

export default {
  ...DefaultTheme,
  Layout() {
    return h(DefaultTheme.Layout, null, {
      // You can add custom layout slots here if needed
    })
  },
  enhanceApp({ app, router, siteData }) {
    // app is the Vue 3 app instance from `createApp()`.
    // router is VitePress' custom router.
    // siteData is a ref of current site-level metadata.
  }
}
