#!/usr/bin/env python3
"""Regenerate XiKey's bundled VoraLex asset from the current exportable database path."""

from __future__ import annotations

import argparse
import json
import os
import sys
import tempfile
from pathlib import Path


def _resolve_voralex_root(value: str | None) -> Path:
    candidate = value or os.environ.get("VORALEX_ROOT") or str(Path(__file__).resolve().parents[1].parent / "VoraLex")
    return Path(candidate).expanduser().resolve()


def _load_exportable_words(voralex_repo: Path) -> tuple[int, list[str]]:
    sys.path.insert(0, str(voralex_repo))
    from voralex.database import VoraLexDatabase  # type: ignore
    from voralex.seed import import_dataset  # type: ignore

    datasets = [
        voralex_repo / "data" / "vorarlberg_v1.json",
        voralex_repo / "data" / "vorarlberg_core_v1.json",
        voralex_repo / "data" / "vorarlberg_regional_expansion_v1.json",
        voralex_repo / "data" / "vorarlberg_voice_v1_curated.json",
    ]

    with tempfile.TemporaryDirectory() as tmp:
        db = VoraLexDatabase.create(Path(tmp) / "voralex.sqlite3")
        imported = 0
        for dataset in datasets:
            imported += import_dataset(db, dataset)
        words = db.exportable_forms()
        db.close()
    return imported, words


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--voralex-root",
        help="Path to the read-only VoraLex repository (defaults to $VORALEX_ROOT or a sibling checkout)",
    )
    parser.add_argument(
        "--output",
        default=str(Path(__file__).resolve().parents[1] / "app" / "src" / "main" / "assets" / "voralex_words.json"),
        help="Destination asset path",
    )
    parser.add_argument(
        "--check",
        action="store_true",
        help="Compare the generated export to the bundled asset and exit non-zero on drift",
    )
    args = parser.parse_args()

    voralex_repo = _resolve_voralex_root(args.voralex_root)
    if not voralex_repo.exists():
        print(f"VoraLex repo not found: {voralex_repo}", file=sys.stderr)
        return 2

    imported, words = _load_exportable_words(voralex_repo)
    output_path = Path(args.output).expanduser().resolve()
    expected = json.dumps(words, ensure_ascii=False, separators=(",", ":"))

    if args.check:
        if not output_path.exists():
            print(f"missing asset: {output_path}", file=sys.stderr)
            return 1
        actual = output_path.read_text(encoding="utf-8")
        if actual != expected:
            print(f"drift detected: {output_path}", file=sys.stderr)
            print(f"generated={len(words)} unique forms from {imported} imported entries", file=sys.stderr)
            return 1
        print(f"ok: {output_path} matches {len(words)} generated unique forms")
        return 0

    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(expected, encoding="utf-8")
    print(f"exported {len(words)} unique forms from {imported} imported entries to {output_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
