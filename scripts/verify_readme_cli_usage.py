#!/usr/bin/env python3

from __future__ import annotations

import re
import sys
from pathlib import Path


def usage() -> str:
    return (
        "Usage: verify_readme_cli_usage.py <README.md> <cli-help.txt>"
    )


def normalize(value: str) -> str:
    normalized = value.replace("\r\n", "\n").replace("\r", "\n")
    stripped_lines = [line.rstrip() for line in normalized.strip().split("\n")]
    return "\n".join(stripped_lines)


def extract_readme_usage_block(readme_text: str) -> str:
    pattern = re.compile(
        r"Current CLI usage:\n\n```text\n(?P<block>.*?)\n```",
        re.DOTALL,
    )
    match = pattern.search(readme_text)
    if match is None:
        raise ValueError("Unable to locate the README 'Current CLI usage' block.")
    return match.group("block")


def main() -> int:
    if len(sys.argv) != 3:
        print(usage(), file=sys.stderr)
        return 2

    readme_path = Path(sys.argv[1])
    help_output_path = Path(sys.argv[2])

    readme_usage_block = extract_readme_usage_block(readme_path.read_text())
    help_output = help_output_path.read_text()

    expected = normalize(readme_usage_block)
    actual = normalize(help_output)
    if expected == actual:
        return 0

    print("README CLI usage block does not match built '--help' output.", file=sys.stderr)
    print("--- README ---", file=sys.stderr)
    print(expected, file=sys.stderr)
    print("--- BUILT HELP ---", file=sys.stderr)
    print(actual, file=sys.stderr)
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
