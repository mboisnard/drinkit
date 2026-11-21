import { isArgumentElement, isNumberElement, isPluralElement, isSelectElement, parse } from '@formatjs/icu-messageformat-parser';
import type { MessageFormatElement } from '@formatjs/icu-messageformat-parser';

/** The TypeScript type of a value a message places. */
export type ArgumentType = 'string | number' | 'number' | 'string';

/** The values a message places, by name, with the type each needs: a number to count, a text to choose a branch. */
export function argumentsOf(message: string): Record<string, ArgumentType> {
  const types: Record<string, ArgumentType> = {};
  for (const [name, type] of walk(parse(message, { ignoreTag: true })).flatMap(argumentOf)) {
    if (types[name] === undefined || types[name] === 'string | number') {
      types[name] = type;
    }
  }

  return types;
}

/** Every element of a message, those of each branch of a plural or a select included. */
export function walk(elements: MessageFormatElement[]): MessageFormatElement[] {
  return elements.flatMap((element) => [
    element,
    ...(isPluralElement(element) || isSelectElement(element)
      ? Object.values(element.options).flatMap((option) => walk(option.value))
      : []),
  ]);
}

function argumentOf(element: MessageFormatElement): Array<[string, ArgumentType]> {
  if (isNumberElement(element) || isPluralElement(element)) {
    return [[element.value, 'number']];
  }
  if (isSelectElement(element)) {
    return [[element.value, 'string']];
  }

  return isArgumentElement(element) ? [[element.value, 'string | number']] : [];
}
