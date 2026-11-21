/** Who may open a page: anybody, any signed-in user, or a user holding a role. */
export type PageAccess = 'public' | 'signed-in' | (string & {});

/**
 * Whether a page shows in the language of its address, under the prefix of its locale: a public page, which a search
 * engine indexes. Any other page shows in the language of the account, without a prefix.
 */
export function hasLanguagePrefix(page: PageAccess | undefined): boolean {
  return page === 'public';
}

export type Access = { kind: 'open' } | { kind: 'sign-in' } | { kind: 'redirect'; to: string } | { kind: 'forbidden' };

/**
 * What a navigation to a page does for the current user, `null` for a visitor. A page that declares nothing is for
 * signed-in users: a page forgotten is closed, not open. The backend still checks every call: this decides what the
 * page shows, never what the user may do.
 *
 * OWASP: ASVS 5.0 V8.2.1 and V8.3.1, Authorization Cheat Sheet (deny by default)
 */
export function accessTo(
  page: PageAccess | undefined,
  user: { roles: readonly string[] } | null,
  forbiddenRedirects: Readonly<Record<string, string>>,
): Access {
  if (page === 'public') {
    return { kind: 'open' };
  }
  if (!user) {
    return { kind: 'sign-in' };
  }
  if (page === undefined || page === 'signed-in' || user.roles.includes(page)) {
    return { kind: 'open' };
  }

  const redirect = user.roles.map((role) => forbiddenRedirects[role]).find((to) => to !== undefined);

  return redirect ? { kind: 'redirect', to: redirect } : { kind: 'forbidden' };
}
