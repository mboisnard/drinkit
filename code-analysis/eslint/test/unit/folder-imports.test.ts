import { Linter } from 'eslint';
import { describe, expect, test } from 'vitest';
import { folderImportRules } from '../../folder-imports.ts';

function lintImport(folder: string, source: string): string[] {
  const messages = new Linter({ configType: 'flat' }).verify(
    `import '${source}';`,
    folderImportRules(),
    `starter/${folder}/file.ts`,
  );

  return messages.map((message) => message.ruleId ?? message.message);
}

describe('the folders of a Nuxt app or layer', () => {
  test.each(['#app', '#imports', '#components', '#build/components', 'vue', '../app/use-api.ts', '../server/relay.ts'])(
    'shared code runs in the Vue app and in Nitro, so it imports neither of them: %s',
    (source) => {
      // When
      const errors = lintImport('shared', source);

      // Then
      expect(errors).toEqual(['no-restricted-imports']);
    },
  );

  test.each(['h3', './cookie-jar.ts', '@drinkit/nuxt-starter/problem-detail'])(
    'shared code imports plain libraries and other shared code: %s',
    (source) => {
      // When
      const errors = lintImport('shared', source);

      // Then
      expect(errors).toEqual([]);
    },
  );

  test.each(['#server/utils', '../server/relay.ts', '../../server/api/cellars.ts'])(
    'the Vue app never imports the code of the server: %s',
    (source) => {
      // When
      const errors = lintImport('app', source);

      // Then
      expect(errors).toEqual(['no-restricted-imports']);
    },
  );

  test.each(['#app', '#components', '#build/components', '../app/use-api.ts'])(
    'the server never imports the Vue app: %s',
    (source) => {
      // When
      const errors = lintImport('server', source);

      // Then
      expect(errors).toEqual(['no-restricted-imports']);
    },
  );

  test.each([['app', '../shared/cookie-jar.ts'], ['server', '#imports'], ['server', '../shared/api-path.ts']])(
    'the Vue app and the server import shared code and their own runtime: %s imports %s',
    (folder, source) => {
      // When
      const errors = lintImport(folder, source);

      // Then
      expect(errors).toEqual([]);
    },
  );

  test.each(['#app', '#imports', '#components', '#server/utils'])(
    'a module runs at build time, where no runtime alias exists: %s',
    (source) => {
      // When
      const errors = lintImport('modules', source);

      // Then
      expect(errors).toEqual(['no-restricted-imports']);
    },
  );
});
