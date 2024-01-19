// https://nuxt.com/docs/api/configuration/nuxt-config
 export default defineNuxtConfig({
  srcDir: 'src',
  app: {
      baseURL: '/',
      buildAssetsDir: '/_nuxt/',
      head: {
          meta: [
              {charset: 'utf-8'},
              {name: 'viewport', content: 'width=device-width, initial-scale=1'},
              {name: 'description', content: 'toto'},
          ],
          title: 'DrinkIt',
      },
  },
  vite: {
      build: {
          target: ['safari12', 'chrome70', 'edge88', 'firefox78', 'opera95', 'ios12']
      }
  },
  modules: [
    'nuxt-primevue'
  ],
  devtools: { enabled: true },
  imports: {
      autoImport: false,
  },
  sourcemap: {
      server: true,
      client: !process.dev,
  },
  /*vue: {
    defineModel: true,
    propsDestructure: true,
  },*/
  css: [
      'primevue/resources/themes/lara-light-blue/theme.css',
      'primeicons/primeicons.css',
  ]
});