import type { Rule } from 'eslint';
import type { Identifier, Node } from 'estree';

const COMPOSABLE = /^use[A-Z]/;

export type FunctionNode = Rule.Node & { type: 'FunctionDeclaration' | 'FunctionExpression' | 'ArrowFunctionExpression' };

export function isFunction(node: Node): node is FunctionNode {
  return node.type === 'FunctionDeclaration' || node.type === 'FunctionExpression'
    || node.type === 'ArrowFunctionExpression';
}

export function isComposableName(name: string): boolean {
  return COMPOSABLE.test(name);
}

export function composableName(node: FunctionNode): string | undefined {
  if (node.type === 'FunctionDeclaration') {
    // A function exported by default has no name, whatever its type says
    const name = (node.id as Identifier | null)?.name;
    return name !== undefined && isComposableName(name) ? name : undefined;
  }

  const { parent } = node;
  return parent.type === 'VariableDeclarator' && parent.id.type === 'Identifier' && isComposableName(parent.id.name)
    ? parent.id.name
    : undefined;
}
