/**
 * The types of an app's session, which each app registers from its own contract:
 *
 * declare module '@drinkit/security-starter/types' {
 *   interface AuthTypes { user: ConnectedUserInformation }
 * }
 */
// eslint-disable-next-line @typescript-eslint/no-empty-object-type -- each app merges its own user into it
export interface AuthTypes {}

// Before an app registers its user, any role: `string & {}` keeps the other literals of PageAuth in the editor
type RegisteredUser = AuthTypes extends { user: infer User }
  ? User
  : { roles: ReadonlyArray<string & {}>; locale?: string };

/**
 * The user the backend answers on the current user path: whatever its fields, it holds roles, and the language of the
 * account in `locale`, such as `fr`, when the pages without a language prefix follow it.
 */
export type CurrentUser = RegisteredUser extends { roles: readonly string[] } ? RegisteredUser : never;

export type Role = CurrentUser['roles'][number];

/** Who may open a page: anybody, any signed-in user, or a user holding a role. */
// eslint-disable-next-line @typescript-eslint/no-redundant-type-constituents -- any string until an app registers
export type PageAuth = 'public' | 'signed-in' | Role;
