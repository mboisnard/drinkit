/** The URL a relayed path reaches on the backend, or none when it lies outside the API, its dot segments resolved. */
export function backendUrlOf(backendUrl: string, apiPath: string, path: string): URL | undefined {
  const url = new URL(path, backendUrl);

  return url.pathname.startsWith(`${apiPath}/`) ? url : undefined;
}
