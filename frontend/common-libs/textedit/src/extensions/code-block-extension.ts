/**
 * code-block-extension.ts
 *
 * Configures CodeBlockLowlight with a shared lowlight instance that has the
 * built-in languages pre-registered. Callers may supply extra languages via
 * the extraLanguages parameter.
 *
 * $Since: 2026-05-15
 */

import CodeBlockLowlight from '@tiptap/extension-code-block-lowlight'
import {createLowlight} from 'lowlight'
import type {Extensions} from '@tiptap/core'
import bashGrammar from 'highlight.js/lib/languages/bash'
import cGrammar from 'highlight.js/lib/languages/c'
import cppGrammar from 'highlight.js/lib/languages/cpp'
import csharpGrammar from 'highlight.js/lib/languages/csharp'
import cssGrammar from 'highlight.js/lib/languages/css'
import dockerfileGrammar from 'highlight.js/lib/languages/dockerfile'
import goGrammar from 'highlight.js/lib/languages/go'
import graphqlGrammar from 'highlight.js/lib/languages/graphql'
import xmlGrammar from 'highlight.js/lib/languages/xml'
import iniGrammar from 'highlight.js/lib/languages/ini'
import javaGrammar from 'highlight.js/lib/languages/java'
import jsGrammar from 'highlight.js/lib/languages/javascript'
import jsonGrammar from 'highlight.js/lib/languages/json'
import kotlinGrammar from 'highlight.js/lib/languages/kotlin'
import lessGrammar from 'highlight.js/lib/languages/less'
import luaGrammar from 'highlight.js/lib/languages/lua'
import makefileGrammar from 'highlight.js/lib/languages/makefile'
import mdGrammar from 'highlight.js/lib/languages/markdown'
import phpGrammar from 'highlight.js/lib/languages/php'
import pythonGrammar from 'highlight.js/lib/languages/python'
import rubyGrammar from 'highlight.js/lib/languages/ruby'
import rustGrammar from 'highlight.js/lib/languages/rust'
import scalaGrammar from 'highlight.js/lib/languages/scala'
import scssGrammar from 'highlight.js/lib/languages/scss'
import sqlGrammar from 'highlight.js/lib/languages/sql'
import swiftGrammar from 'highlight.js/lib/languages/swift'
import tsGrammar from 'highlight.js/lib/languages/typescript'
import yamlGrammar from 'highlight.js/lib/languages/yaml'
import type {CodeLanguage} from '../types'

/** All languages registered by default, sorted alphabetically by label. */
export const BUILT_IN_LANGUAGES: CodeLanguage[] = [
  {label: 'Bash', value: 'bash', grammar: bashGrammar},
  {label: 'C', value: 'c', grammar: cGrammar},
  {label: 'C++', value: 'cpp', grammar: cppGrammar},
  {label: 'C#', value: 'csharp', grammar: csharpGrammar},
  {label: 'CSS', value: 'css', grammar: cssGrammar},
  {label: 'Dockerfile', value: 'dockerfile', grammar: dockerfileGrammar},
  {label: 'Go', value: 'go', grammar: goGrammar},
  {label: 'GraphQL', value: 'graphql', grammar: graphqlGrammar},
  {label: 'HTML', value: 'xml', grammar: xmlGrammar},
  {label: 'INI', value: 'ini', grammar: iniGrammar},
  {label: 'Java', value: 'java', grammar: javaGrammar},
  {label: 'JavaScript', value: 'javascript', grammar: jsGrammar},
  {label: 'JSON', value: 'json', grammar: jsonGrammar},
  {label: 'Kotlin', value: 'kotlin', grammar: kotlinGrammar},
  {label: 'LESS', value: 'less', grammar: lessGrammar},
  {label: 'Lua', value: 'lua', grammar: luaGrammar},
  {label: 'Makefile', value: 'makefile', grammar: makefileGrammar},
  {label: 'Markdown', value: 'markdown', grammar: mdGrammar},
  {label: 'PHP', value: 'php', grammar: phpGrammar},
  {label: 'Python', value: 'python', grammar: pythonGrammar},
  {label: 'Ruby', value: 'ruby', grammar: rubyGrammar},
  {label: 'Rust', value: 'rust', grammar: rustGrammar},
  {label: 'Scala', value: 'scala', grammar: scalaGrammar},
  {label: 'SCSS', value: 'scss', grammar: scssGrammar},
  {label: 'SQL', value: 'sql', grammar: sqlGrammar},
  {label: 'Swift', value: 'swift', grammar: swiftGrammar},
  {label: 'TypeScript', value: 'typescript', grammar: tsGrammar},
  {label: 'YAML', value: 'yaml', grammar: yamlGrammar},
]

export interface LanguageItem {
  label: string
  value: string
}

export interface CodeBlockSetup {
  /** Configured CodeBlockLowlight extension ready to pass to TipTap. */
  extension: Extensions[number]
  /** Ordered list of language items for the language-picker overlay. */
  languageItems: LanguageItem[]
}

/**
 * Creates a CodeBlockLowlight extension and a list of language picker items.
 *
 * @param extraLanguages additional languages to register alongside the built-ins
 */
export function buildCodeBlockExtension(extraLanguages: CodeLanguage[] = []): CodeBlockSetup {
  const allLanguages = [...BUILT_IN_LANGUAGES, ...extraLanguages]

  const lowlight = createLowlight()
  for (const lang of allLanguages) {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    lowlight.register(lang.value, lang.grammar as any)
  }

  const extension = CodeBlockLowlight
    .extend({
      addNodeView() {
        return ({node}) => {
          const wrapper = document.createElement('div')
          wrapper.className = 'code-block-with-lines'

          const gutter = document.createElement('div')
          gutter.className = 'code-line-numbers'
          gutter.setAttribute('aria-hidden', 'true')

          const pre = document.createElement('pre')
          const code = document.createElement('code')
          pre.appendChild(code)
          wrapper.appendChild(gutter)
          wrapper.appendChild(pre)

          const updateGutter = (text: string): void => {
            const parts = text.split('\n')
            const count = parts.length > 1 && parts[parts.length - 1] === ''
              ? parts.length - 1
              : parts.length
            gutter.innerHTML = ''
            for (let i = 1; i <= Math.max(1, count); i++) {
              const span = document.createElement('span')
              span.textContent = String(i)
              gutter.appendChild(span)
            }
          }

          updateGutter(node.textContent)

          return {
            dom: wrapper,
            contentDOM: code,
            update(updatedNode) {
              if (updatedNode.type.name !== 'codeBlock') return false
              updateGutter(updatedNode.textContent)
              return true
            },
          }
        }
      },
    })
    .configure({lowlight})
  const languageItems = allLanguages.map(({label, value}) => ({label, value}))

  return {extension, languageItems}
}
