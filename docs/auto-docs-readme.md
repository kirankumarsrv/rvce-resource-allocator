# Auto Documentation Generator

Files added:

- [tools/generate_docs.py](tools/generate_docs.py#L1) — Python generator that scans the repo and writes `final_output.csv`.
- [tools/requirements.txt](tools/requirements.txt#L1) — optional dependencies (`PyYAML`, `javalang`).
- [.git/hooks/post-commit](.git/hooks/post-commit#L1) — post-commit hook that installs requirements and runs the generator.

Usage

1. (Optional) install requirements:

```bash
python -m pip install -r tools/requirements.txt
```

2. Run generator manually:

```bash
python tools/generate_docs.py /path/to/project_root
```

3. After you make the hook executable (on Unix/macOS):

```bash
chmod +x .git/hooks/post-commit
```

Notes & next steps

- The script uses `javalang` if available to extract classes/methods. For a full JavaParser-based extractor, add a Java tool (see original design) and update the hook to call it.
- Output CSV: `final_output.csv` in the project root.
