const fs = require('fs')
const path = require('path')

const ROOT = path.resolve(__dirname, '..')
const SRC = path.join(ROOT, 'frontend', 'src')
const exts = ['.ts', '.tsx', '.js', '.jsx']

function walk(dir) {
  const files = []
  for (const name of fs.readdirSync(dir)) {
    const full = path.join(dir, name)
    const stat = fs.statSync(full)
    if (stat.isDirectory()) files.push(...walk(full))
    else if (exts.includes(path.extname(full))) files.push(full)
  }
  return files
}

const files = walk(SRC)

function readImports(file) {
  const src = fs.readFileSync(file, 'utf8')
  const importLines = []
  const re = /^\s*import\s+(type\s+)?([\s\S]+?)\s+from\s+['\"]([^'\"]+)['\"]/gm
  let m
  while ((m = re.exec(src))) {
    const isType = !!m[1]
    const spec = m[3]
    importLines.push({ spec, isType })
  }
  return importLines
}

function resolveImport(fromFile, spec) {
  if (spec.startsWith('@/')) {
    const rel = spec.slice(2)
    const candidate = path.join(SRC, rel)
    // try file with extensions or index
    for (const e of exts) {
      const f = candidate + e
      if (fs.existsSync(f)) return path.resolve(f)
    }
    for (const e of exts) {
      const f = path.join(candidate, 'index' + e)
      if (fs.existsSync(f)) return path.resolve(f)
    }
    return null
  }
  if (spec.startsWith('./') || spec.startsWith('../')) {
    const base = path.dirname(fromFile)
    const candidate = path.resolve(base, spec)
    for (const e of exts) {
      const f = candidate + e
      if (fs.existsSync(f)) return path.resolve(f)
    }
    for (const e of exts) {
      const f = path.join(candidate, 'index' + e)
      if (fs.existsSync(f)) return path.resolve(f)
    }
    return null
  }
  return null
}

const map = new Map()
for (const f of files) {
  const imports = readImports(f).map(({ spec, isType }) => ({ spec, isType, resolved: resolveImport(f, spec) }))
  map.set(path.resolve(f), imports)
}

// build adjacency for runtime imports only (isType=false)
const adj = new Map()
for (const [f, imps] of map.entries()) {
  const targets = []
  for (const imp of imps) {
    if (!imp.resolved) continue
    targets.push({ file: path.resolve(imp.resolved), isType: !!imp.isType })
  }
  adj.set(f, targets)
}

// detect cycles using DFS considering runtime edges (isType=false)
const visited = new Set()
const stack = []
const onStack = new Set()
const cycles = []

function dfs(node) {
  visited.add(node)
  stack.push(node)
  onStack.add(node)
  const edges = adj.get(node) || []
  for (const e of edges) {
    const to = e.file
    // treat type-only imports as non-runtime edges
    const corresponding = (map.get(node) || []).find(i => i.resolved === to)
    if (corresponding && corresponding.isType) continue
    if (!visited.has(to)) {
      dfs(to)
    } else if (onStack.has(to)) {
      // found cycle
      const cycle = stack.slice(stack.indexOf(to)).concat([to])
      cycles.push(cycle)
    }
  }
  stack.pop()
  onStack.delete(node)
}

for (const node of adj.keys()) {
  if (!visited.has(node)) dfs(node)
}

// dedupe cycles by normalized string
const uniq = new Map()
for (const c of cycles) {
  const key = c.join(' -> ')
  uniq.set(key, c)
}

const out = []
for (const [k, c] of uniq.entries()) {
  out.push(c)
}

console.log(JSON.stringify({ cycles: out, totalFiles: files.length }, null, 2))
