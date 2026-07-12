#!/usr/bin/env python3
"""
Extract human user messages from Claude CLI project JSONL logs.

Example:
  python3 scripts/extract-claude-user-messages.py --since 2026-07-07
  python3 scripts/extract-claude-user-messages.py --since "2026-07-07 11:00" --until "2026-07-12 11:00"
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
from collections import Counter
from datetime import datetime, time, timedelta, timezone
from pathlib import Path
from typing import Any

try:
    from zoneinfo import ZoneInfo
except ImportError:  # pragma: no cover - Python < 3.9 fallback
    ZoneInfo = None  # type: ignore[assignment]


SYNTHETIC_PREFIXES = (
    "<local-command-",
    "<command-name>",
    "<command-message>",
    "<system-reminder>",
    "<task-notification>",
    "<skill-prompt>",
    "<ide_context>",
    "<context",
)


def default_timezone(name: str) -> timezone:
    if ZoneInfo is not None:
        return ZoneInfo(name)  # type: ignore[return-value]
    if name == "Asia/Kolkata":
        return timezone(timedelta(hours=5, minutes=30), name=name)
    raise SystemExit(f"Python zoneinfo is unavailable; pass --tz Asia/Kolkata or upgrade Python.")


def parse_datetime(value: str, tz: timezone, end_of_day: bool = False) -> datetime:
    value = value.strip()
    if not value:
        raise argparse.ArgumentTypeError("date value cannot be empty")

    lowered = value.lower()
    if lowered in {"now", "today"}:
        dt = datetime.now(tz)
        if lowered == "today":
            dt = datetime.combine(dt.date(), time.max if end_of_day else time.min, tz)
        return dt.astimezone(timezone.utc)

    normalized = value.replace("Z", "+00:00")
    if len(normalized) == 10:
        day = datetime.strptime(normalized, "%Y-%m-%d").date()
        clock = time.max if end_of_day else time.min
        return datetime.combine(day, clock, tz).astimezone(timezone.utc)

    # Accept "YYYY-MM-DD HH:MM[:SS]" as a friendlier ISO variant.
    if " " in normalized and "T" not in normalized:
        normalized = normalized.replace(" ", "T", 1)

    try:
        dt = datetime.fromisoformat(normalized)
    except ValueError as exc:
        raise argparse.ArgumentTypeError(
            f"could not parse date {value!r}; use YYYY-MM-DD, YYYY-MM-DD HH:MM, or ISO-8601"
        ) from exc

    if dt.tzinfo is None:
        dt = dt.replace(tzinfo=tz)
    return dt.astimezone(timezone.utc)


def encode_claude_project_dir(project_path: Path) -> str:
    # Claude stores /mnt/data/projects/Softland as -mnt-data-projects-Softland.
    absolute = str(project_path.expanduser().resolve())
    return absolute.replace(os.sep, "-")


def text_from_content(content: Any) -> tuple[str, str]:
    if isinstance(content, str):
        return content, "string"

    if isinstance(content, list):
        parts: list[str] = []
        content_types: list[str] = []
        for item in content:
            if not isinstance(item, dict):
                content_types.append(type(item).__name__)
                continue
            kind = str(item.get("type"))
            content_types.append(kind)
            if kind == "text":
                parts.append(str(item.get("text") or ""))
            elif kind == "image":
                source = item.get("source") or {}
                media_type = source.get("media_type") if isinstance(source, dict) else None
                suffix = f": {media_type}" if media_type else ""
                parts.append(f"[image attached{suffix}]")
        return "\n".join(parts), "content-list:" + ",".join(content_types)

    return "", type(content).__name__


def synthetic_skip_reason(text: str) -> str | None:
    stripped = text.strip()
    if not stripped:
        return "empty-or-tool-result-only"
    for prefix in SYNTHETIC_PREFIXES:
        if stripped.startswith(prefix):
            return prefix.strip("<>-") or "synthetic-tag"
    if stripped.startswith("Base directory for this skill:"):
        return "skill-injection"
    if stripped.startswith("Base directory for this command:"):
        return "command-injection"
    if stripped == "[Request interrupted by user]":
        return "interrupt-event"
    if stripped.startswith("This session is being continued from a previous conversation"):
        return "resume-summary"
    return None


def iter_jsonl_records(path: Path) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    records: list[dict[str, Any]] = []
    invalid: list[dict[str, Any]] = []
    with path.open("r", encoding="utf-8", errors="replace") as handle:
        for line_number, line in enumerate(handle, 1):
            if not line.strip():
                continue
            try:
                records.append(json.loads(line))
            except Exception as exc:  # noqa: BLE001 - log files may contain partial/corrupt lines
                invalid.append({"file": path.name, "line": line_number, "error": str(exc)})
    return records, invalid


def extract_entries(
    claude_project_dir: Path,
    since_utc: datetime,
    until_utc: datetime,
) -> tuple[list[dict[str, Any]], dict[str, Any]]:
    raw_rows: list[dict[str, Any]] = []
    skipped: Counter[str] = Counter()
    invalid: list[dict[str, Any]] = []
    files = sorted(claude_project_dir.glob("*.jsonl"))

    for path in files:
        records, bad_lines = iter_jsonl_records(path)
        invalid.extend(bad_lines)
        for record in records:
            timestamp = record.get("timestamp")
            if not timestamp:
                continue
            try:
                dt = datetime.fromisoformat(str(timestamp).replace("Z", "+00:00"))
            except ValueError:
                skipped["unparseable-timestamp"] += 1
                continue
            if dt < since_utc or dt > until_utc:
                continue

            message = record.get("message") or {}
            if not (record.get("type") == "user" and message.get("role") == "user"):
                continue

            text, content_kind = text_from_content(message.get("content"))
            reason = synthetic_skip_reason(text)
            if reason:
                skipped[reason] += 1
                continue

            raw_rows.append(
                {
                    "timestamp_utc": dt.astimezone(timezone.utc).isoformat().replace("+00:00", "Z"),
                    "session_id": record.get("sessionId") or path.stem,
                    "source_file": path.name,
                    "uuid": record.get("uuid"),
                    "prompt_id": record.get("promptId"),
                    "cwd": record.get("cwd"),
                    "content_kind": content_kind,
                    "text": text.replace("\r\n", "\n").replace("\r", "\n"),
                }
            )

    groups: dict[tuple[str, str], dict[str, Any]] = {}
    for row in raw_rows:
        key = (row["timestamp_utc"], row["text"])
        group = groups.setdefault(
            key,
            {
                "timestamp_utc": row["timestamp_utc"],
                "text": row["text"],
                "content_kinds": set(),
                "sources": [],
                "uuids": set(),
                "prompt_ids": set(),
                "cwd_values": set(),
            },
        )
        group["content_kinds"].add(row["content_kind"])
        group["sources"].append({"session_id": row["session_id"], "source_file": row["source_file"]})
        if row.get("uuid"):
            group["uuids"].add(row["uuid"])
        if row.get("prompt_id"):
            group["prompt_ids"].add(row["prompt_id"])
        if row.get("cwd"):
            group["cwd_values"].add(row["cwd"])

    entries: list[dict[str, Any]] = []
    for group in groups.values():
        unique_sources: list[dict[str, str]] = []
        seen_sources: set[tuple[str, str]] = set()
        for source in group["sources"]:
            pair = (source["session_id"], source["source_file"])
            if pair in seen_sources:
                continue
            seen_sources.add(pair)
            unique_sources.append(source)

        fingerprint = hashlib.sha256(
            (group["timestamp_utc"] + "\n" + group["text"]).encode("utf-8")
        ).hexdigest()
        entries.append(
            {
                "timestamp_utc": group["timestamp_utc"],
                "source_count": len(unique_sources),
                "sources": unique_sources,
                "uuids": sorted(group["uuids"]),
                "prompt_ids": sorted(group["prompt_ids"]),
                "cwd_values": sorted(group["cwd_values"]),
                "content_kinds": sorted(group["content_kinds"]),
                "sha256": fingerprint,
                "text": group["text"],
            }
        )

    entries.sort(key=lambda entry: entry["timestamp_utc"])
    audit = {
        "files_seen": len(files),
        "raw_included_records_before_dedupe": len(raw_rows),
        "deduped_message_count": len(entries),
        "skipped_counts": dict(sorted(skipped.items())),
        "invalid_json_lines": invalid,
        "dedupe_rule": "exact match on timestamp_utc plus message text",
    }
    return entries, audit


def localize_entry(entry: dict[str, Any], tz: timezone) -> dict[str, Any]:
    dt = datetime.fromisoformat(entry["timestamp_utc"].replace("Z", "+00:00"))
    return {**entry, "timestamp_local": dt.astimezone(tz).isoformat()}


def write_jsonl(
    path: Path,
    entries: list[dict[str, Any]],
    metadata: dict[str, Any],
) -> None:
    with path.open("w", encoding="utf-8") as handle:
        handle.write(json.dumps({"type": "metadata", **metadata}, ensure_ascii=False) + "\n")
        for entry in entries:
            handle.write(json.dumps({"type": "message", **entry}, ensure_ascii=False) + "\n")


def write_markdown(
    path: Path,
    entries: list[dict[str, Any]],
    metadata: dict[str, Any],
    tz: timezone,
) -> None:
    lines: list[str] = []
    lines.append(f"# Claude CLI user-message extract: {metadata['project_path']}")
    lines.append("")
    lines.append(f"Generated: {metadata['generated_at_local']} / {metadata['generated_at_utc']}")
    lines.append(f"Window: {metadata['window_start_local']} to {metadata['window_end_local']}")
    lines.append(f"Source: `{metadata['source_glob']}`")
    lines.append("")
    lines.append(
        "Interpretation: Claude CLI does not store a display name for the human. "
        "This extracts external human user prompts from the selected project logs "
        "and excludes synthetic CLI user-role records."
    )
    lines.append("")
    lines.append("## Audit")
    lines.append("")
    lines.append(f"- Claude session files scanned: {metadata['files_seen']}")
    lines.append(
        f"- Raw included user-message records before dedupe: "
        f"{metadata['raw_included_records_before_dedupe']}"
    )
    lines.append(f"- Deduped messages: {metadata['deduped_message_count']}")
    lines.append(f"- Invalid JSONL lines skipped: {len(metadata['invalid_json_lines'])}")
    lines.append(f"- Dedupe rule: {metadata['dedupe_rule']}")
    lines.append("- Skipped synthetic records:")
    for reason, count in metadata["skipped_counts"].items():
        lines.append(f"  - {reason}: {count}")
    lines.append("")

    current_day = None
    for index, entry in enumerate(entries, 1):
        dt = datetime.fromisoformat(entry["timestamp_utc"].replace("Z", "+00:00")).astimezone(tz)
        day = dt.date().isoformat()
        if day != current_day:
            current_day = day
            lines.append(f"## {day}")
            lines.append("")

        sources = ", ".join(source["source_file"].removesuffix(".jsonl") for source in entry["sources"][:5])
        if len(entry["sources"]) > 5:
            sources += f", +{len(entry['sources']) - 5} more"

        lines.append(f"### {index}. {dt.strftime('%H:%M:%S')} local / {entry['timestamp_utc']}")
        lines.append("")
        lines.append(f"- Sources: {sources}")
        lines.append(f"- Source count: {entry['source_count']}")
        lines.append(f"- Content kind: {', '.join(entry['content_kinds'])}")
        lines.append(f"- SHA256: `{entry['sha256']}`")
        lines.append("")
        lines.append("```text")
        lines.append(entry["text"].strip())
        lines.append("```")
        lines.append("")

    path.write_text("\n".join(lines), encoding="utf-8")


def build_arg_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Extract human user messages from Claude CLI project logs.",
    )
    parser.add_argument(
        "--since",
        help="Start date/time. Examples: 2026-07-07, '2026-07-07 11:00', 2026-07-07T05:30:00Z.",
    )
    parser.add_argument(
        "--until",
        default="now",
        help="End date/time, default: now. Date-only values include the whole day.",
    )
    parser.add_argument(
        "--last-days",
        type=float,
        help="Alternative to --since: extract the last N days.",
    )
    parser.add_argument(
        "--project",
        default=".",
        help="Project path whose Claude logs should be read. Default: current directory.",
    )
    parser.add_argument(
        "--claude-home",
        default="~/.claude",
        help="Claude home directory. Default: ~/.claude.",
    )
    parser.add_argument(
        "--claude-project-dir",
        help="Explicit Claude project log directory. Overrides --project and --claude-home.",
    )
    parser.add_argument(
        "--tz",
        default="Asia/Kolkata",
        help="Timezone used for date-only inputs and Markdown grouping. Default: Asia/Kolkata.",
    )
    parser.add_argument(
        "--out-dir",
        default="/tmp",
        help="Directory for generated Markdown and JSONL outputs. Default: /tmp.",
    )
    parser.add_argument(
        "--prefix",
        help="Output filename prefix. Default is derived from project name and date window.",
    )
    return parser


def main() -> int:
    parser = build_arg_parser()
    args = parser.parse_args()
    tz = default_timezone(args.tz)

    if args.last_days is not None:
        until_utc = parse_datetime(args.until, tz, end_of_day=True)
        since_utc = until_utc - timedelta(days=args.last_days)
    else:
        since_value = args.since
        if since_value is None:
            since_value = input("Extract messages since date/time: ").strip()
        since_utc = parse_datetime(since_value, tz)
        until_utc = parse_datetime(args.until, tz, end_of_day=True)

    if since_utc > until_utc:
        raise SystemExit("--since must be before --until")

    project_path = Path(args.project).expanduser().resolve()
    if args.claude_project_dir:
        claude_project_dir = Path(args.claude_project_dir).expanduser().resolve()
    else:
        claude_home = Path(args.claude_home).expanduser().resolve()
        claude_project_dir = claude_home / "projects" / encode_claude_project_dir(project_path)

    if not claude_project_dir.exists():
        raise SystemExit(f"Claude project log directory not found: {claude_project_dir}")

    entries, audit = extract_entries(claude_project_dir, since_utc, until_utc)
    localized_entries = [localize_entry(entry, tz) for entry in entries]

    generated_at = datetime.now(timezone.utc)
    start_label = since_utc.astimezone(tz).strftime("%Y%m%d-%H%M%S")
    end_label = until_utc.astimezone(tz).strftime("%Y%m%d-%H%M%S")
    prefix = args.prefix or f"{project_path.name}-claude-user-messages-{start_label}_to_{end_label}"

    out_dir = Path(args.out_dir).expanduser().resolve()
    out_dir.mkdir(parents=True, exist_ok=True)
    markdown_path = out_dir / f"{prefix}.md"
    jsonl_path = out_dir / f"{prefix}.jsonl"

    metadata = {
        "generated_at_utc": generated_at.isoformat().replace("+00:00", "Z"),
        "generated_at_local": generated_at.astimezone(tz).isoformat(),
        "window_start_utc": since_utc.isoformat().replace("+00:00", "Z"),
        "window_start_local": since_utc.astimezone(tz).isoformat(),
        "window_end_utc": until_utc.isoformat().replace("+00:00", "Z"),
        "window_end_local": until_utc.astimezone(tz).isoformat(),
        "project_path": str(project_path),
        "claude_project_dir": str(claude_project_dir),
        "source_glob": str(claude_project_dir / "*.jsonl"),
        "author_interpretation": (
            "Claude CLI logs do not store a display name; this extracts human user-role "
            "messages after excluding synthetic CLI records."
        ),
        **audit,
    }

    write_jsonl(jsonl_path, localized_entries, metadata)
    write_markdown(markdown_path, localized_entries, metadata, tz)

    print(f"Markdown: {markdown_path}")
    print(f"JSONL:    {jsonl_path}")
    print(f"Messages: {len(entries)}")
    print(f"Window:   {metadata['window_start_local']} to {metadata['window_end_local']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
