"""The source files the Levels page is built from, and the excerpt check.

`check()` confirms that every excerpt registered in content.py occurs in its
source file. Runs of whitespace (line wraps, indentation) are collapsed on
both sides first; every other character must match exactly.
"""

import pathlib
import re

HERE = pathlib.Path(__file__).resolve().parent
ROUNDS = HERE.parent
REPO = HERE.parents[3]

FILES = {
    "01": ROUNDS / "01-notebook-reading.md",
    "02": ROUNDS / "02-box-and-levels.md",
    "02b": ROUNDS / "02b-alt-branch-breakouts.md",
    "03": ROUNDS / "03-breakouts-A-to-F.md",
    "04": ROUNDS / "04-first-few-to-many.md",
    "05": ROUNDS / "05-breakouts-B-E-G-to-M.md",
    "06": ROUNDS / "06-three-math-bridges.md",
    "07": ROUNDS / "07-convergence.md",
    "13": HERE / "13-the-box.md",
    "MAP": ROUNDS / "MAP.md",
    "STARTER": ROUNDS / "STARTER.md",
    "LOG": REPO / "vision" / "LOG.md",
}
TEXT = {k: v.read_text(encoding="utf-8") for k, v in FILES.items()}


def path_of(src):
    """The file's path from the repository root, as the page shows it."""
    return str(FILES[src].relative_to(REPO))


def name_of(src):
    return FILES[src].name if src != "LOG" else "vision/LOG.md"


def norm(s):
    return re.sub(r"\s+", " ", s).strip()


NORM = {k: norm(v) for k, v in TEXT.items()}


def check(registry):
    """Return the excerpts that are not in their source file."""
    return [ex for ex in registry if norm(ex.md) not in NORM[ex.src]]


if __name__ == "__main__":
    import content

    bad = check(content.REGISTRY)
    print(f"{len(content.REGISTRY)} excerpts, {len(bad)} not found")
    for ex in bad:
        print(f"  [{ex.src}] {ex.md[:160]!r}")
