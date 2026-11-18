import type { Rule } from 'eslint';
import { isFunction } from './nodes.ts';

const REACTIVE_STATE = new Set(['ref', 'shallowRef', 'reactive', 'shallowReactive', 'computed', 'customRef']);

export const noModuleState: Rule.RuleModule = {
  meta: {
    type: 'problem',
    docs: { description: 'Forbids reactive state at module level, which the server would share between requests' },
    messages: {
      moduleState: 'Reactive state at module level is shared by every request the server renders: '
        + 'create it inside a composable, or use useState',
    },
    schema: [],
  },
  create(context) {
    if (context.filename.endsWith('.vue')) {
      return {};
    }

    return {
      CallExpression(node) {
        const isReactiveState = node.callee.type === 'Identifier' && REACTIVE_STATE.has(node.callee.name);
        if (isReactiveState && !context.sourceCode.getAncestors(node).some(isFunction)) {
          context.report({ node, messageId: 'moduleState' });
        }
      },
    };
  },
};
