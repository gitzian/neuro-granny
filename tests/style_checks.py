"""Small dependency-free source hygiene check (not a complete Java formatter)."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
files = list((root / "app" / "src").rglob("*.java"))
problems = []
for path in files:
    source = path.read_text(encoding="utf-8")
    if not source.endswith("\n"):
        problems.append(f"{path.name}: missing final newline")
    for line_number, line in enumerate(source.splitlines(), start=1):
        if "\t" in line:
            problems.append(f"{path.name}:{line_number}: tab indentation")
        if line.rstrip() != line:
            problems.append(f"{path.name}:{line_number}: trailing whitespace")

if problems:
    raise SystemExit("\n".join(problems))
print(f"Source whitespace checks passed: {len(files)} Java files")
