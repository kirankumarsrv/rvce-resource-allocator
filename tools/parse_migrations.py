#!/usr/bin/env python3
"""Parse Flyway SQL migration files to extract CREATE TABLE structures into Markdown.

Usage: python tools/parse_migrations.py <migrations_dir> [output.md]
"""
import os
import re
import sys

MIG_DIR = sys.argv[1] if len(sys.argv) > 1 else "backend/src/main/resources/db/migration"
OUT = sys.argv[2] if len(sys.argv) > 2 else "docs/db-schema.md"

create_re = re.compile(r"CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?([\w\.\"]+)\s*\((.*?)\);", re.S | re.I)
col_re = re.compile(r"^\s*([\"\w]+)\s+([^,]+)(?:,)?$", re.I)

def clean_identifier(s):
    return s.strip().strip('"')

tables = []

for fname in sorted(os.listdir(MIG_DIR)):
    if not fname.lower().endswith('.sql'):
        continue
    path = os.path.join(MIG_DIR, fname)
    txt = open(path, 'r', encoding='utf-8', errors='ignore').read()

    for m in create_re.finditer(txt):
        tname = clean_identifier(m.group(1))
        body = m.group(2)
        cols = []
        constraints = []
        for line in body.splitlines():
            line = line.strip()
            if not line:
                continue
            # column line
            cm = col_re.match(line.rstrip(','))
            if cm:
                cname = clean_identifier(cm.group(1))
                rest = cm.group(2).strip()
                cols.append((cname, rest))
            else:
                constraints.append(line.rstrip(','))

        tables.append((tname, cols, constraints, fname))

# write markdown
os.makedirs(os.path.dirname(OUT), exist_ok=True)
with open(OUT, 'w', encoding='utf-8') as f:
    f.write('# Database Schema (extracted from migrations)\n\n')
    for tname, cols, constraints, src in tables:
        f.write(f'## {tname}\n')
        f.write(f'*Source: {src}*\n\n')
        f.write('| Column | Type & Constraints |\n')
        f.write('|---|---|\n')
        for cname, rest in cols:
            f.write(f'| `{cname}` | {rest.replace("|","\\|")} |\n')
        if constraints:
            f.write('\n**Table constraints / indexes**:\n\n')
            for c in constraints:
                f.write(f'- {c}\n')
        f.write('\n---\n\n')

print(f'Wrote DB schema to {OUT}')
