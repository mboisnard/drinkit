import { defineNuxtPlugin } from '#app';

/** The errors that reached the error handler another layer installed, as an alerting module does. */
export const errorsOfAnotherHandler: unknown[] = [];

export default defineNuxtPlugin((nuxtApp) => {
  nuxtApp.vueApp.config.errorHandler = (error) => {
    errorsOfAnotherHandler.push(error);
  };
});
