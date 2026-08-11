import importlib.util
import json
import tempfile
import unittest
from pathlib import Path


SCRIPT = Path(__file__).parents[2] / "scripts" / "report-codex-rollout.py"
SPEC = importlib.util.spec_from_file_location("report_codex_rollout", SCRIPT)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(MODULE)


def write_rollout(
    path: Path,
    session_id: str,
    *,
    parent_id: str | None = None,
    role: str | None = None,
    input_tokens: int = 0,
    total_tokens: int = 0,
    tools: tuple[str, ...] = (),
) -> None:
    source = {}
    if parent_id:
        source = {
            "subagent": {
                "thread_spawn": {
                    "parent_thread_id": parent_id,
                    "depth": 1,
                    "agent_path": f"/root/{role}",
                    "agent_role": role,
                }
            }
        }
    records = [
        {
            "timestamp": "2026-08-11T00:00:00Z",
            "type": "session_meta",
            "payload": {"id": session_id, "source": source},
        }
    ]
    records.extend(
        {
            "timestamp": "2026-08-11T00:00:01Z",
            "type": "response_item",
            "payload": {"type": "custom_tool_call", "name": tool, "input": "{}"},
        }
        for tool in tools
    )
    records.append(
        {
            "timestamp": "2026-08-11T00:00:02Z",
            "type": "event_msg",
            "payload": {
                "type": "token_count",
                "info": {
                    "total_token_usage": {
                        "input_tokens": input_tokens,
                        "cached_input_tokens": 0,
                        "cache_write_input_tokens": 0,
                        "output_tokens": total_tokens - input_tokens,
                        "reasoning_output_tokens": 0,
                        "total_tokens": total_tokens,
                    },
                    "last_token_usage": {"input_tokens": input_tokens},
                    "model_context_window": 1000,
                },
            },
        }
    )
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("".join(json.dumps(record) + "\n" for record in records))


class ReportCodexRolloutTest(unittest.TestCase):
    def test_discovers_descendants_from_session_metadata_only(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            parent_path = root / "2026/08/11/rollout-parent.jsonl"
            child_path = root / "2026/08/11/rollout-child.jsonl"
            grandchild_path = root / "2026/08/11/rollout-grandchild.jsonl"
            unrelated_path = root / "2026/08/11/rollout-unrelated.jsonl"
            write_rollout(parent_path, "parent", input_tokens=100, total_tokens=110)
            write_rollout(
                child_path,
                "child",
                parent_id="parent",
                role="collector",
                input_tokens=50,
                total_tokens=55,
            )
            write_rollout(
                grandchild_path,
                "grandchild",
                parent_id="child",
                role="explorer",
                input_tokens=25,
                total_tokens=30,
            )
            write_rollout(unrelated_path, "unrelated", input_tokens=999, total_tokens=999)

            paths = MODULE.discover_family_paths(parent_path, root)

            self.assertEqual(paths, [parent_path, child_path, grandchild_path])

    def test_family_summary_prices_children_roles_and_management(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            parent_path = root / "rollout-parent.jsonl"
            child_path = root / "rollout-child.jsonl"
            write_rollout(
                parent_path,
                "parent",
                input_tokens=100,
                total_tokens=120,
                tools=("spawn_agent", "wait_agent", "exec_command"),
            )
            write_rollout(
                child_path,
                "child",
                parent_id="parent",
                role="collector",
                input_tokens=100,
                total_tokens=130,
            )

            reports = [
                MODULE.analyze(parent_path, None),
                MODULE.analyze(child_path, None),
            ]
            summary = MODULE.family_summary(reports, "parent")

            self.assertEqual(summary["aggregate"]["final_cumulative_tokens_sum"]["input_tokens"], 200)
            self.assertEqual(summary["child_share_percent"]["input_tokens"], 50.0)
            self.assertEqual(summary["aggregate"]["agent_management_calls_sum"], 2)
            self.assertEqual(summary["by_role"]["collector"]["rollouts"], 1)
            self.assertEqual(summary["by_role"]["primary"]["rollouts"], 1)


if __name__ == "__main__":
    unittest.main()
