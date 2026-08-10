#!/usr/bin/env python3
"""Report deterministic throughput metrics from Codex rollout JSONL files.

The token records are cumulative snapshots. This script reports the final
snapshot from each rollout; it never sums repeated ``total_token_usage`` rows.
It also avoids printing message, tool-output, or encrypted-reasoning content.
"""

from __future__ import annotations

import argparse
import json
import re
from collections import Counter
from datetime import datetime
from pathlib import Path
from typing import Any


NESTED_ACTIONS = {
    "shell": "tools.exec_command",
    "patch": "tools.apply_patch",
    "plan": "tools.update_plan",
    "stdin": "tools.write_stdin",
    "wait": "tools.wait",
}

PATCH_TARGET_RE = re.compile(
    r"\*\*\* (?:Add|Update|Delete) File: (?P<path>[^\r\n\\\"]+)"
)


def parse_timestamp(value: Any) -> datetime | None:
    if not isinstance(value, str):
        return None
    try:
        return datetime.fromisoformat(value.replace("Z", "+00:00"))
    except ValueError:
        return None


def output_text_chars(output: Any) -> int:
    if isinstance(output, str):
        return len(output)
    if isinstance(output, list):
        return sum(
            len(item.get("text", ""))
            for item in output
            if isinstance(item, dict) and isinstance(item.get("text", ""), str)
        )
    if isinstance(output, dict) and isinstance(output.get("text"), str):
        return len(output["text"])
    return 0


def sorted_dict(counter: Counter[str]) -> dict[str, int]:
    return dict(sorted(counter.items()))


def patch_targets(tool_input: str) -> set[str]:
    """Return exact file targets named by an apply_patch payload."""
    return {match.group("path") for match in PATCH_TARGET_RE.finditer(tool_input)}


def analyze(path: Path, now_path: str | None) -> dict[str, Any]:
    top_types: Counter[str] = Counter()
    response_types: Counter[str] = Counter()
    event_types: Counter[str] = Counter()
    custom_tool_names: Counter[str] = Counter()
    function_names: Counter[str] = Counter()
    nested_actions: Counter[str] = Counter()
    tool_events: list[tuple[datetime | None, dict[str, int]]] = []
    now_patch_times: list[datetime] = []
    compacted_at: list[str] = []
    aborted_at: list[datetime] = []
    malformed_lines = 0
    physical_lines = 0
    valid_records = 0
    first_timestamp: datetime | None = None
    last_timestamp: datetime | None = None
    final_token_timestamp: str | None = None
    final_tokens: dict[str, int] = {}
    token_count_events = 0
    peak_last_input = 0
    model_context_window = 0
    tool_output_chars = 0
    max_tool_output_chars = 0
    tool_outputs_over_20k = 0
    session_id: str | None = None

    with path.open("r", encoding="utf-8") as handle:
        for physical_lines, line in enumerate(handle, start=1):
            try:
                record = json.loads(line)
            except (json.JSONDecodeError, UnicodeDecodeError):
                malformed_lines += 1
                continue
            if not isinstance(record, dict):
                malformed_lines += 1
                continue

            valid_records += 1
            record_type = record.get("type", "<missing>")
            top_types[str(record_type)] += 1
            payload = record.get("payload")
            payload = payload if isinstance(payload, dict) else {}

            timestamp_value = record.get("timestamp")
            timestamp = parse_timestamp(timestamp_value)
            if timestamp is not None:
                first_timestamp = min(first_timestamp, timestamp) if first_timestamp else timestamp
                last_timestamp = max(last_timestamp, timestamp) if last_timestamp else timestamp

            if record_type == "session_meta":
                maybe_id = payload.get("id")
                if isinstance(maybe_id, str):
                    session_id = maybe_id

            if record_type == "compacted" and isinstance(timestamp_value, str):
                compacted_at.append(timestamp_value)

            if record_type == "event_msg":
                event_type = str(payload.get("type", "<missing>"))
                event_types[event_type] += 1
                if event_type == "turn_aborted" and timestamp is not None:
                    aborted_at.append(timestamp)
                if event_type == "token_count":
                    info = payload.get("info")
                    info = info if isinstance(info, dict) else {}
                    total = info.get("total_token_usage")
                    last = info.get("last_token_usage")
                    total = total if isinstance(total, dict) else {}
                    last = last if isinstance(last, dict) else {}
                    token_count_events += 1
                    final_tokens = {
                        key: int(total.get(key, 0) or 0)
                        for key in (
                            "input_tokens",
                            "cached_input_tokens",
                            "cache_write_input_tokens",
                            "output_tokens",
                            "reasoning_output_tokens",
                            "total_tokens",
                        )
                    }
                    final_token_timestamp = timestamp_value if isinstance(timestamp_value, str) else None
                    peak_last_input = max(peak_last_input, int(last.get("input_tokens", 0) or 0))
                    model_context_window = max(
                        model_context_window,
                        int(info.get("model_context_window", 0) or 0),
                    )

            if record_type != "response_item":
                continue

            response_type = str(payload.get("type", "<missing>"))
            response_types[response_type] += 1

            if response_type == "custom_tool_call":
                tool_name = str(payload.get("name", "<missing>"))
                custom_tool_names[tool_name] += 1
                tool_input = payload.get("input", "")
                tool_input = tool_input if isinstance(tool_input, str) else ""
                action_row = {
                    action: tool_input.count(needle)
                    for action, needle in NESTED_ACTIONS.items()
                }
                if tool_name in {"exec_command", "apply_patch", "update_plan", "write_stdin"}:
                    direct_name = {
                        "exec_command": "shell",
                        "apply_patch": "patch",
                        "update_plan": "plan",
                        "write_stdin": "stdin",
                    }[tool_name]
                    action_row[direct_name] += 1
                nested_actions.update(action_row)
                tool_events.append((timestamp, action_row))
                targets = patch_targets(tool_input)
                if now_path:
                    is_now_patch = now_path in targets
                else:
                    is_now_patch = any(target.endswith("NOW.md") for target in targets)
                if is_now_patch and timestamp is not None:
                    now_patch_times.append(timestamp)

            elif response_type == "function_call":
                function_name = str(payload.get("name", "<missing>"))
                function_names[function_name] += 1

            elif response_type in {"custom_tool_call_output", "function_call_output"}:
                chars = output_text_chars(payload.get("output"))
                tool_output_chars += chars
                max_tool_output_chars = max(max_tool_output_chars, chars)
                if chars > 20_000:
                    tool_outputs_over_20k += 1

    first_now = min(now_patch_times) if now_patch_times else None
    first_abort = min(aborted_at) if aborted_at else None
    post_now_calls = 0
    post_now_actions: Counter[str] = Counter()
    pre_abort_post_now_calls = 0
    pre_abort_post_now_actions: Counter[str] = Counter()
    if first_now is not None:
        for timestamp, action_row in tool_events:
            if timestamp is not None and timestamp > first_now:
                post_now_calls += 1
                post_now_actions.update(action_row)
                if first_abort is not None and timestamp < first_abort:
                    pre_abort_post_now_calls += 1
                    pre_abort_post_now_actions.update(action_row)

    elapsed_seconds = None
    if first_timestamp is not None and last_timestamp is not None:
        elapsed_seconds = round((last_timestamp - first_timestamp).total_seconds(), 3)

    return {
        "path": str(path),
        "session_id": session_id,
        "file_bytes": path.stat().st_size,
        "physical_lines": physical_lines,
        "valid_records": valid_records,
        "malformed_lines": malformed_lines,
        "elapsed_seconds": elapsed_seconds,
        "top_level_type_counts": sorted_dict(top_types),
        "response_item_type_counts": sorted_dict(response_types),
        "event_type_counts": sorted_dict(event_types),
        "custom_tool_names": sorted_dict(custom_tool_names),
        "function_names": sorted_dict(function_names),
        "nested_action_counts": sorted_dict(nested_actions),
        "token_count_events": token_count_events,
        "final_token_timestamp": final_token_timestamp,
        "final_cumulative_tokens": final_tokens,
        "peak_last_input_tokens": peak_last_input,
        "model_context_window": model_context_window,
        "peak_context_percent": (
            round(100 * peak_last_input / model_context_window, 2)
            if model_context_window
            else None
        ),
        "compacted_at": compacted_at,
        "tool_output_text_chars": tool_output_chars,
        "max_tool_output_text_chars": max_tool_output_chars,
        "tool_outputs_over_20k_chars": tool_outputs_over_20k,
        "now_patch_timestamps": [timestamp.isoformat() for timestamp in sorted(now_patch_times)],
        "custom_tool_calls_after_first_now_patch": post_now_calls,
        "nested_actions_after_first_now_patch": sorted_dict(post_now_actions),
        "first_abort_timestamp": first_abort.isoformat() if first_abort else None,
        "custom_tool_calls_after_first_now_before_first_abort": pre_abort_post_now_calls,
        "nested_actions_after_first_now_before_first_abort": sorted_dict(
            pre_abort_post_now_actions
        ),
    }


def aggregate(reports: list[dict[str, Any]]) -> dict[str, Any]:
    token_keys = {
        key
        for report in reports
        for key in report["final_cumulative_tokens"].keys()
    }
    action_keys = {
        key for report in reports for key in report["nested_action_counts"].keys()
    }
    return {
        "rollouts": len(reports),
        "valid_records_sum": sum(report["valid_records"] for report in reports),
        "malformed_lines_sum": sum(report["malformed_lines"] for report in reports),
        "elapsed_seconds_sum": round(
            sum(report["elapsed_seconds"] or 0 for report in reports), 3
        ),
        "final_cumulative_tokens_sum": {
            key: sum(report["final_cumulative_tokens"].get(key, 0) for report in reports)
            for key in sorted(token_keys)
        },
        "nested_action_counts_sum": {
            key: sum(report["nested_action_counts"].get(key, 0) for report in reports)
            for key in sorted(action_keys)
        },
        "tool_output_text_chars_sum": sum(
            report["tool_output_text_chars"] for report in reports
        ),
        "compactions_sum": sum(len(report["compacted_at"]) for report in reports),
        "now_patches_sum": sum(
            len(report["now_patch_timestamps"]) for report in reports
        ),
        "post_now_custom_tool_calls_sum": sum(
            report["custom_tool_calls_after_first_now_patch"] for report in reports
        ),
        "peak_last_input_tokens_max": max(
            (report["peak_last_input_tokens"] for report in reports), default=0
        ),
        "peak_context_percent_max": max(
            (
                report["peak_context_percent"]
                for report in reports
                if report["peak_context_percent"] is not None
            ),
            default=None,
        ),
    }


def print_human(report: dict[str, Any]) -> None:
    print(f"Codex rollout: {report['path']}")
    print(f"session_id: {report['session_id'] or '-'}")
    print(
        "records: "
        f"{report['valid_records']} valid / {report['physical_lines']} physical / "
        f"{report['malformed_lines']} malformed; {report['file_bytes']} bytes"
    )
    print(f"elapsed_seconds: {report['elapsed_seconds']}")
    print(f"top_level_types: {json.dumps(report['top_level_type_counts'], sort_keys=True)}")
    print(f"response_items: {json.dumps(report['response_item_type_counts'], sort_keys=True)}")
    print(f"events: {json.dumps(report['event_type_counts'], sort_keys=True)}")
    print(f"nested_actions: {json.dumps(report['nested_action_counts'], sort_keys=True)}")
    print(
        "final_cumulative_tokens: "
        f"{json.dumps(report['final_cumulative_tokens'], sort_keys=True)} "
        f"at {report['final_token_timestamp']} ({report['token_count_events']} snapshots)"
    )
    print(
        "peak_context: "
        f"{report['peak_last_input_tokens']} / {report['model_context_window']} "
        f"({report['peak_context_percent']}%)"
    )
    print(f"compacted_at: {json.dumps(report['compacted_at'])}")
    print(
        "tool_output: "
        f"{report['tool_output_text_chars']} text chars total; "
        f"max {report['max_tool_output_text_chars']}; "
        f">20k {report['tool_outputs_over_20k_chars']}"
    )
    print(f"now_patch_timestamps: {json.dumps(report['now_patch_timestamps'])}")
    print(
        "after_first_now_patch: "
        f"{report['custom_tool_calls_after_first_now_patch']} custom calls; "
        f"{json.dumps(report['nested_actions_after_first_now_patch'], sort_keys=True)}"
    )
    print(
        "after_first_now_before_first_abort: "
        f"{report['custom_tool_calls_after_first_now_before_first_abort']} custom calls; "
        f"{json.dumps(report['nested_actions_after_first_now_before_first_abort'], sort_keys=True)} "
        f"before {report['first_abort_timestamp']}"
    )


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("paths", type=Path, nargs="+", help="Codex rollout JSONL path(s)")
    parser.add_argument(
        "--now-path",
        help="Only treat apply_patch calls targeting this exact path as terminal NOW writes",
    )
    parser.add_argument("--json", action="store_true", help="Emit stable JSON")
    args = parser.parse_args()

    canonical_paths = [path.expanduser().resolve() for path in args.paths]
    if len(set(canonical_paths)) != len(canonical_paths):
        parser.error("duplicate rollout paths would double-count the aggregate")
    reports = [analyze(path, args.now_path) for path in canonical_paths]
    payload: dict[str, Any] = {"reports": reports}
    if len(reports) > 1:
        payload["aggregate"] = aggregate(reports)

    if args.json:
        print(json.dumps(payload, indent=2, sort_keys=True))
    else:
        for index, report in enumerate(reports):
            if index:
                print()
            print_human(report)
        if len(reports) > 1:
            print()
            print(f"Aggregate: {json.dumps(payload['aggregate'], sort_keys=True)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
