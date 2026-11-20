import type { ConnectedUserInformation } from '#api';

declare module '@drinkit/security-starter/types' {
  interface AuthTypes {
    user: ConnectedUserInformation;
  }
}

export {};
