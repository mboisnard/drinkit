import type { Rule } from 'eslint';
import { composableName } from './nodes.ts';
import type { FunctionNode } from './nodes.ts';

export const noAsyncComposable: Rule.RuleModule = {
  meta: {
    type: 'problem',
    docs: { description: 'Forbids an async composable, whose code after its first await runs outside the component' },
    messages: {
      async: 'A composable after an await has lost its component: return a ref the composable fills, '
        + 'or use useAsyncData',
    },
    schema: [],
  },
  create(context) {
    function check(node: FunctionNode): void {
      if (node.async && composableName(node) !== undefined) {
        context.report({ node, messageId: 'async' });
      }
    }

    return { FunctionDeclaration: check, FunctionExpression: check, ArrowFunctionExpression: check };
  },
};
