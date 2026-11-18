import type { Config } from 'stylelint';

const config: Config = {
  extends: ['@stylistic/stylelint-config'],
  overrides: [
    {
      files: ['**/*.vue'],
      customSyntax: 'postcss-html',
    },
  ],
  rules: {
    '@stylistic/indentation': [2, { baseIndentLevel: 1 }],
    '@stylistic/max-line-length': 120,
    '@stylistic/string-quotes': 'single',
  },
};

export default config;
