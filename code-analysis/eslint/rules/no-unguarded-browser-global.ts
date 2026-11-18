import type { Rule, Scope } from 'eslint';
import type { Node } from 'estree';
import { composableName, isFunction } from './nodes.ts';

const BROWSER_GLOBALS = new Set(['window', 'document', 'navigator', 'location', 'history', 'localStorage',
  'sessionStorage']);
const BROWSER_LIFECYCLE = new Set(['onMounted', 'onBeforeMount', 'onUpdated', 'onBeforeUpdate', 'onUnmounted',
  'onBeforeUnmount', 'onActivated', 'onDeactivated']);

export const noUnguardedBrowserGlobal: Rule.RuleModule = {
  meta: {
    type: 'problem',
    docs: { description: 'Forbids a browser global where the server runs the code too' },
    messages: {
      unguarded: '{{name}} does not exist on the server: read it in onMounted, under import.meta.client, '
        + 'or in a function that only runs in the browser',
    },
    schema: [],
  },
  create(context) {
    function check(reference: Scope.Reference): void {
      const { identifier } = reference;
      if (identifier.type !== 'Identifier') {
        return;
      }

      const ancestors = context.sourceCode.getAncestors(identifier);
      const parent = ancestors.at(-1);
      const isTypeofCheck = parent?.type === 'UnaryExpression' && parent.operator === 'typeof';
      if (!isTypeofCheck && !isGuarded(ancestors) && runsDuringSetup(ancestors)) {
        context.report({ node: identifier, messageId: 'unguarded', data: { name: identifier.name } });
      }
    }

    return {
      'Program:exit'(node) {
        const globalScope = context.sourceCode.getScope(node);
        const declared = globalScope.variables.flatMap((variable) => variable.references);
        [...declared, ...globalScope.through]
          .filter((reference) => BROWSER_GLOBALS.has(reference.identifier.name))
          .forEach(check);
      },
    };
  },
};

function isGuarded(ancestors: Node[]): boolean {
  return ancestors.some((node, index) => {
    const child = ancestors[index + 1];
    const isClientBranch = (node.type === 'IfStatement' || node.type === 'ConditionalExpression')
      && isClientCheck(node.test) && child === node.consequent;
    const isClientOperand = node.type === 'LogicalExpression' && node.operator === '&&' && isClientCheck(node.left)
      && child === node.right;
    return isClientBranch || isClientOperand || isBrowserLifecycleCallback(node, child);
  });
}

function isClientCheck(node: Node): boolean {
  return node.type === 'MemberExpression' && node.object.type === 'MetaProperty' && node.property.type === 'Identifier'
    && node.property.name === 'client';
}

function isBrowserLifecycleCallback(node: Node, child: Node | undefined): boolean {
  return node.type === 'CallExpression' && node.callee.type === 'Identifier' && BROWSER_LIFECYCLE.has(node.callee.name)
    && child !== undefined && isFunction(child);
}

function runsDuringSetup(ancestors: Node[]): boolean {
  const enclosingFunction = ancestors.findLast(isFunction);
  return enclosingFunction === undefined || composableName(enclosingFunction) !== undefined;
}
