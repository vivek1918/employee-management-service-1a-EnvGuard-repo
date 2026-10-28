#!/usr/bin/env python3
"""Validate deploy/env/*.yaml files against schema.json."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

try:
    import yaml
except ImportError:
    print("ERROR: PyYAML is required. Install with: pip install pyyaml jsonschema", file=sys.stderr)
    sys.exit(2)

try:
    import jsonschema
    from jsonschema import Draft202012Validator
except ImportError:
    print("ERROR: jsonschema is required. Install with: pip install pyyaml jsonschema", file=sys.stderr)
    sys.exit(2)


def load_schema(schema_path: Path) -> dict:
    with schema_path.open(encoding="utf-8") as f:
        return json.load(f)


def validate_file(yaml_path: Path, schema: dict) -> list[str]:
    errors: list[str] = []
    with yaml_path.open(encoding="utf-8") as f:
        try:
            data = yaml.safe_load(f)
        except yaml.YAMLError as exc:
            return [f"{yaml_path.name}: YAML parse error: {exc}"]

    if data is None:
        return [f"{yaml_path.name}: file is empty"]

    validator = Draft202012Validator(schema)
    for error in sorted(validator.iter_errors(data), key=lambda e: list(e.path)):
        path = ".".join(str(p) for p in error.path) or "(root)"
        errors.append(f"{yaml_path.name}: {path}: {error.message}")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate EnvGuard deploy YAML configs")
    parser.add_argument(
        "--env-dir",
        default=str(Path(__file__).resolve().parents[1] / "deploy" / "env"),
        help="Directory containing YAML configs and schema.json",
    )
    args = parser.parse_args()

    env_dir = Path(args.env_dir)
    schema_path = env_dir / "schema.json"
    if not schema_path.is_file():
        print(f"ERROR: schema not found: {schema_path}", file=sys.stderr)
        return 1

    schema = load_schema(schema_path)
    yaml_files = sorted(env_dir.glob("*.yaml")) + sorted(env_dir.glob("*.yml"))
    if not yaml_files:
        print(f"ERROR: no YAML files in {env_dir}", file=sys.stderr)
        return 1

    all_errors: list[str] = []
    for yaml_path in yaml_files:
        all_errors.extend(validate_file(yaml_path, schema))

    if all_errors:
        print("Validation FAILED:")
        for err in all_errors:
            print(f"  - {err}")
        return 1

    print(f"OK: validated {len(yaml_files)} file(s) against {schema_path.name}")
    for yaml_path in yaml_files:
        print(f"  - {yaml_path.name}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
