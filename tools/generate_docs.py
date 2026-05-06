#!/usr/bin/env python3
"""
Lightweight project documentation generator.

Features:
- Scans project files and emits `final_output.csv` with rows for files, Java symbols, TODOs, YAML keys, ENV vars, SQL comments, and Docker commands.
- Tries to parse Java with `javalang` if available; otherwise uses heuristics.

Run: `python tools/generate_docs.py [root_dir]`
"""
import os
import sys
import csv
import re

try:
    import yaml
except Exception:
    yaml = None

try:
    import javalang
except Exception:
    javalang = None

ROOT_DIR = sys.argv[1] if len(sys.argv) > 1 else os.getcwd()
OUTPUT_FILE = os.path.join(ROOT_DIR, "final_output.csv")

# Performance / filtering settings
# Directories (substrings) to skip entirely
EXCLUDE_DIRS = (
    ".git",
    "node_modules",
    "venv",
    "__pycache__",
    "build",
    "target",
    "out",
    "dist",
    "coverage",
    "\.gradle",
    "\.idea",
)

# File extensions to skip (binary / heavy files)
SKIP_EXTENSIONS = (
    ".jar",
    ".class",
    ".exe",
    ".dll",
    ".so",
    ".png",
    ".jpg",
    ".jpeg",
    ".gif",
    ".mp4",
    ".mp3",
    ".zip",
    ".tar",
    ".gz",
    ".7z",
    ".pdf",
    ".woff",
    ".woff2",
    ".bin",
)

# Skip files larger than this (bytes). Set to None to disable.
MAX_FILE_SIZE_BYTES = 1_000_000  # 1 MB

# How often to print progress (files processed)
PROGRESS_INTERVAL = 200

# Verbose prints file-level skips when True
VERBOSE = True

def extract_todos(content):
    results = []
    for i, line in enumerate(content.splitlines(), 1):
        if "TODO" in line or "FIXME" in line:
            results.append(("TODO", f"L{i}", line.strip()))
    return results

def extract_sql(content):
    return [("SQL", f"L{i+1}", l.strip()) for i, l in enumerate(content.splitlines()) if l.strip().upper().startswith("--")]

def extract_yaml(content):
    if not yaml:
        return []
    try:
        data = yaml.safe_load(content)
        out = []
        if isinstance(data, dict):
            for k, v in data.items():
                out.append(("YAML", str(k), str(v)))
        return out
    except Exception:
        return []

def extract_env(content):
    results = []
    for line in content.splitlines():
        if "=" in line and not line.strip().startswith("#"):
            k, v = line.split("=", 1)
            results.append(("ENV", k.strip(), v.strip()))
    return results

def extract_docker(content):
    out = []
    for i, line in enumerate(content.splitlines(), 1):
        s = line.strip()
        if not s or s.startswith("#"):
            continue
        if s.split()[0].upper() in ("FROM", "RUN", "CMD", "ENTRYPOINT", "EXPOSE", "ENV", "WORKDIR", "COPY", "ADD"):
            out.append(("Docker", f"L{i}", s))
    return out

def parse_java_with_javalang(path, content):
    out = []
    try:
        tree = javalang.parse.parse(content)
    except Exception:
        return out

    package = getattr(tree.package, 'name', '') if tree.package else ''

    for path_, node in tree.filter((javalang.tree.ClassDeclaration, javalang.tree.InterfaceDeclaration, javalang.tree.EnumDeclaration)):
        kind = 'Class' if isinstance(node, javalang.tree.ClassDeclaration) else ('Interface' if isinstance(node, javalang.tree.InterfaceDeclaration) else 'Enum')
        name = node.name
        out.append((path, package, kind, name, '', '', ''))

        for method in getattr(node, 'methods', []) or []:
            mname = method.name
            rtype = getattr(method.return_type, 'name', '') if method.return_type else ''
            params = ", ".join([p.type.name + ("[]" if getattr(p.type, 'dimensions', None) else "") + " " + n.name for n, p in zip([None]*len(method.parameters), method.parameters)]) if method.parameters else ''
            out.append((path, package, 'Method', mname, params, rtype, ''))

    return out

def heuristic_java_parse(path, content):
    out = []
    package = ''
    m = re.search(r'package\s+([\w.]+)\s*;', content)
    if m:
        package = m.group(1)

    for cls in re.finditer(r'(?m)^(public\s+)?(class|interface|enum)\s+(\w+)', content):
        kind = cls.group(2).capitalize()
        name = cls.group(3)
        out.append((path, package, kind, name, '', '', ''))

    # methods (very loose)
    for meth in re.finditer(r'(?m)^(?:public|protected|private|static|\s)+[\w\<\>\[\]]+\s+(\w+)\s*\(([^)]*)\)\s*\{', content):
        name = meth.group(1)
        params = meth.group(2).strip()
        out.append((path, package, 'Method', name, params, '', ''))

    return out

def process_file(path):
    _, ext = os.path.splitext(path)
    ext = ext.lower()
    # skip by extension or size
    try:
        st = os.stat(path)
    except Exception:
        return []

    ext = os.path.splitext(path)[1].lower()
    if ext in SKIP_EXTENSIONS:
        if VERBOSE:
            print(f"SKIP ext: {ext}\t{path}")
        return []

    if MAX_FILE_SIZE_BYTES and st.st_size > MAX_FILE_SIZE_BYTES:
        if VERBOSE:
            print(f"SKIP size: {st.st_size} bytes\t{path}")
        return []

    try:
        with open(path, 'r', encoding='utf-8', errors='ignore') as f:
            content = f.read()
    except Exception:
        if VERBOSE:
            print(f"SKIP unreadable: {path}")
        return []

    results = []

    # global file entry
    file_name = os.path.basename(path)
    results.append((path, '', 'File', file_name, '', '', ''))

    # todos
    for t in extract_todos(content):
        results.append((path, '', t[0], t[1], '', '', t[2]))

    if ext == '.sql':
        for it in extract_sql(content):
            results.append((path, '', it[0], it[1], '', '', it[2]))
    elif ext in ('.yml', '.yaml'):
        for it in extract_yaml(content):
            results.append((path, '', it[0], it[1], '', '', it[2]))
    elif ext in ('.env', '.properties'):
        for it in extract_env(content):
            results.append((path, '', it[0], it[1], '', '', it[2]))
    elif 'dockerfile' in path.lower() or file_name.lower() == 'dockerfile':
        for it in extract_docker(content):
            results.append((path, '', it[0], it[1], '', '', it[2]))
    elif ext == '.java':
        if javalang:
            for row in parse_java_with_javalang(path, content):
                results.append(row)
        else:
            for row in heuristic_java_parse(path, content):
                results.append(row)

    return results

def main():
    all_data = []
    total_seen = 0
    total_processed = 0

    for root, dirs, files in os.walk(ROOT_DIR):
        # skip excluded directories by substring match
        if any(ex in root for ex in EXCLUDE_DIRS):
            if VERBOSE:
                print(f"SKIP dir: {root}")
            continue

        for file in files:
            total_seen += 1
            path = os.path.join(root, file)

            try:
                rows = process_file(path)
            except KeyboardInterrupt:
                print("\nInterrupted by user. Writing partial output...")
                rows = []

            if rows:
                total_processed += 1
                all_data.extend(rows)

            if total_seen % PROGRESS_INTERVAL == 0:
                print(f"Progress: seen={total_seen} processed={total_processed} last={path}")

    # write CSV
    with open(OUTPUT_FILE, 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)
        writer.writerow(["File", "Package", "Type", "Name", "Parameters", "ReturnType", "Details"])
        for row in all_data:
            writer.writerow(row)

    print(f"✅ Documentation written to: {OUTPUT_FILE} (seen={total_seen} processed={total_processed})")

if __name__ == '__main__':
    main()
