/**
 * utils.ts
 *
 * $Since: 2026-05-11
 */

// Two independent 32-bit hashes (djb2 + FNV-1a) combined as a string key,
// giving ~64-bit collision resistance without BigInt overhead.
const DJB2_SEED = 5381
const FNV32_OFFSET = 2166136261  // 0x811C9DC5
const FNV32_PRIME = 16777619     // 0x01000193

/**
 * Hashes an arbitrary JSON-serialisable value to a compact string key.
 *
 * Uses djb2 and FNV-1a 32-bit in parallel and concatenates their outputs,
 * providing ~64-bit collision resistance at the cost of a single JSON
 * serialisation pass.
 *
 * @param value any JSON-serialisable value
 * @return hex string in the form `"<djb2>,<fnv1a>"`
 * @since 0.1.0
 * @version 0.1.0
 */
export function hashJson(value: unknown): string {
  const json = JSON.stringify(value)
  let a = DJB2_SEED
  let b = FNV32_OFFSET
  for (let i = 0; i < json.length; i++) {
    const c = json.charCodeAt(i)
    a = (Math.imul(a, 33) ^ c) >>> 0
    b = (Math.imul(b ^ c, FNV32_PRIME)) >>> 0
  }
  return `${a.toString(16)},${b.toString(16)}`
}
