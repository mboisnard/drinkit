import type { Rule } from 'eslint';
import { composableName, isComposableName } from './nodes.ts';
import type { FunctionNode } from './nodes.ts';

const REACTIVITY_MODULES = new Set(['vue', 'vue-router', '#app', '#imports', 'nuxt/app']);

export const composableUsesReactivity: Rule.RuleModule = {
  meta: {
    type: 'suggestion',
    docs: { description: 'A function named useX calls Vue, Nuxt or another composable, or it is a plain function' },
    messages: {
      noReactivity: '{{name}} calls neither Vue, Nuxt nor a composable: it is a plain function, name it without use',
    },
    schema: [],
  },
  create(context) {
    const reactivity = new Set<string>();
    const composables: Array<{ name: string; node: FunctionNode; reactive: boolean }> = [];

    function enter(node: FunctionNode): void {
      const name = composableName(node);
      if (name !== undefined) {
        composables.push({ name, node, reactive: false });
      }
    }

    function exit(node: FunctionNode): void {
      const composable = composables.at(-1);
      if (composable?.node !== node) {
        return;
      }

      composables.pop();
      if (!composable.reactive) {
        context.report({ node, messageId: 'noReactivity', data: { name: composable.name } });
      }
    }

    return {
      ImportDeclaration(node) {
        if (REACTIVITY_MODULES.has(String(node.source.value))) {
          node.specifiers.forEach((specifier) => reactivity.add(specifier.local.name));
        }
      },
      CallExpression(node) {
        const isReactive = node.callee.type === 'Identifier'
          && (reactivity.has(node.callee.name) || isComposableName(node.callee.name));
        if (isReactive) {
          composables.forEach((composable) => {
            composable.reactive = true;
          });
        }
      },
      'FunctionDeclaration': enter,
      'FunctionExpression': enter,
      'ArrowFunctionExpression': enter,
      'FunctionDeclaration:exit': exit,
      'FunctionExpression:exit': exit,
      'ArrowFunctionExpression:exit': exit,
    };
  },
};
