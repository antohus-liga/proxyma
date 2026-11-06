import { describe, expect, it } from 'vitest'
import enGB from '../locales/en-GB'
import ptPT from '../locales/pt-PT'

function flattenMessages(
  value: unknown,
  prefix = '',
  target = new Map<string, string>(),
): Map<string, string> {
  if (typeof value === 'string') {
    target.set(prefix, value)
    return target
  }

  if (value && typeof value === 'object') {
    for (const [key, child] of Object.entries(value)) {
      flattenMessages(child, prefix ? `${prefix}.${key}` : key, target)
    }
  }

  return target
}

function placeholders(message: string): string[] {
  return [...message.matchAll(/\{(\w+)\}/g)]
    .map((match) => match[1])
    .filter((value): value is string => Boolean(value))
    .sort()
}

const english = flattenMessages(enGB)
const portuguese = flattenMessages(ptPT)

describe('locale catalogues', () => {
  it('register the same keys', () => {
    expect([...portuguese.keys()].sort()).toEqual([...english.keys()].sort())
  })

  it('do not contain empty messages', () => {
    for (const message of [...english.values(), ...portuguese.values()]) {
      expect(message.trim()).not.toBe('')
    }
  })

  it('keep the same interpolation placeholders', () => {
    for (const [key, message] of english) {
      expect(placeholders(portuguese.get(key) ?? '')).toEqual(placeholders(message))
    }
  })
})
