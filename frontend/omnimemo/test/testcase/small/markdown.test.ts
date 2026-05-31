/**
 * markdown.test.ts
 *
 * $Since: 2026-05-31
 */

import {describe, expect, it} from 'vitest'
import {renderMarkdown} from '@src/markdown'

describe('renderMarkdown:', () => {
  it('renders a heading', () => {
    expect(renderMarkdown('# Hello')).toBe('<h1>Hello</h1>\n')
  })

  it('renders bold text', () => {
    expect(renderMarkdown('**bold**')).toBe('<p><strong>bold</strong></p>\n')
  })

  it('renders a fenced code block', () => {
    const result = renderMarkdown('```\ncode\n```')
    expect(result).toContain('<code>')
    expect(result).toContain('code')
  })

  it('returns empty string for empty input', () => {
    expect(renderMarkdown('')).toBe('')
  })
})
