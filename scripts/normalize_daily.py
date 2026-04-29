#!/usr/bin/env python3
"""Normalize daily health exports into a single long CSV.

The script intentionally uses only the Python standard library so it can run in
cron or a phone-sync folder without dependency setup.
"""

from __future__ import annotations

import argparse
import csv
import json
import re
from datetime import datetime
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_ALIASES = ROOT / "config" / "metric_aliases.json"
OUTPUT_COLUMNS = ["date", "source", "metric", "value", "unit", "start_time", "end_time", "notes"]


def slug(value: str) -> str:
    value = value.strip().lower()
    value = re.sub(r"[\s\-/]+", "_", value)
    value = re.sub(r"[^a-z0-9_%]+", "", value)
    return value.strip("_")


def parse_number(value: str) -> float | None:
    cleaned = str(value).strip().replace("\u202f", "").replace(" ", "")
    if not cleaned:
        return None
    cleaned = cleaned.replace(",", ".")
    match = re.search(r"-?\d+(?:\.\d+)?", cleaned)
    if not match:
        return None
    return float(match.group(0))


def parse_date(value: str) -> str | None:
    raw = str(value).strip()
    if not raw:
        return None

    raw = raw.replace("T", " ")
    candidates = [
        raw[:10],
        raw,
    ]
    formats = [
        "%Y-%m-%d",
        "%d/%m/%Y",
        "%m/%d/%Y",
        "%d-%m-%Y",
        "%Y/%m/%d",
        "%Y-%m-%d %H:%M:%S",
        "%d/%m/%Y %H:%M:%S",
    ]
    for candidate in candidates:
        for fmt in formats:
            try:
                return datetime.strptime(candidate, fmt).date().isoformat()
            except ValueError:
                continue
    return None


def load_aliases(path: Path) -> tuple[dict[str, dict[str, object]], dict[str, str]]:
    with path.open(encoding="utf-8") as handle:
        aliases = json.load(handle)

    alias_to_metric: dict[str, str] = {}
    for metric, spec in aliases.items():
        alias_to_metric[slug(metric)] = metric
        for alias in spec.get("aliases", []):
            alias_to_metric[slug(str(alias))] = metric
    return aliases, alias_to_metric


def source_from_path(path: Path) -> str:
    name = slug(path.stem)
    if any(token in name for token in ["zepp", "amazfit", "mifit", "mi_fit"]):
        return "zepp"
    if any(token in name for token in ["nutrition", "food", "yazio", "lifesum", "myfitnesspal", "cronometer"]):
        return "nutrition"
    if any(token in name for token in ["apple", "healthkit", "health_connect", "google_fit"]):
        return "health"
    return "unknown"


def first_existing(headers: dict[str, str], candidates: list[str]) -> str | None:
    for candidate in candidates:
        key = slug(candidate)
        if key in headers:
            return headers[key]
    return None


def normalize_metric_name(metric: str, alias_to_metric: dict[str, str]) -> str | None:
    return alias_to_metric.get(slug(metric))


def normalize_file(
    path: Path,
    aliases: dict[str, dict[str, object]],
    alias_to_metric: dict[str, str],
) -> list[dict[str, str]]:
    rows: list[dict[str, str]] = []
    fallback_source = source_from_path(path)

    with path.open(newline="", encoding="utf-8-sig") as handle:
        sample = handle.read(4096)
        handle.seek(0)
        dialect = csv.Sniffer().sniff(sample, delimiters=",;\t")
        reader = csv.DictReader(handle, dialect=dialect)
        if not reader.fieldnames:
            return rows

        header_map = {slug(name): name for name in reader.fieldnames}
        date_col = first_existing(
            header_map,
            ["date", "day", "start_date", "startdate", "start_time", "starttime", "timestamp", "time"],
        )
        source_col = first_existing(header_map, ["source", "app", "provider"])
        metric_col = first_existing(header_map, ["metric", "type", "name", "data_type"])
        value_col = first_existing(header_map, ["value", "amount", "count", "quantity"])
        unit_col = first_existing(header_map, ["unit", "units"])
        start_col = first_existing(header_map, ["start_time", "start", "startdate", "start_date"])
        end_col = first_existing(header_map, ["end_time", "end", "enddate", "end_date"])
        notes_col = first_existing(header_map, ["notes", "note", "comment"])

        metric_columns = {
            original: alias_to_metric[slugged]
            for slugged, original in header_map.items()
            if slugged in alias_to_metric
        }

        for raw_row in reader:
            date = parse_date(raw_row.get(date_col, "")) if date_col else None
            if not date:
                continue

            source = raw_row.get(source_col, "").strip() if source_col else ""
            source = slug(source) or fallback_source
            start_time = raw_row.get(start_col, "").strip() if start_col else ""
            end_time = raw_row.get(end_col, "").strip() if end_col else ""
            notes = raw_row.get(notes_col, "").strip() if notes_col else ""

            if metric_col and value_col:
                metric = normalize_metric_name(raw_row.get(metric_col, ""), alias_to_metric)
                value = parse_number(raw_row.get(value_col, ""))
                if metric and value is not None:
                    unit = raw_row.get(unit_col, "").strip() if unit_col else ""
                    rows.append(
                        {
                            "date": date,
                            "source": source,
                            "metric": metric,
                            "value": f"{value:g}",
                            "unit": unit or str(aliases[metric]["unit"]),
                            "start_time": start_time,
                            "end_time": end_time,
                            "notes": notes,
                        }
                    )
                continue

            for original_col, metric in metric_columns.items():
                value = parse_number(raw_row.get(original_col, ""))
                if value is None:
                    continue
                rows.append(
                    {
                        "date": date,
                        "source": source,
                        "metric": metric,
                        "value": f"{value:g}",
                        "unit": str(aliases[metric]["unit"]),
                        "start_time": start_time,
                        "end_time": end_time,
                        "notes": notes,
                    }
                )
    return rows


def iter_csv_files(input_path: Path) -> list[Path]:
    if input_path.is_file():
        return [input_path]
    return sorted(path for path in input_path.rglob("*.csv") if path.is_file())


def write_rows(rows: list[dict[str, str]], output_path: Path) -> None:
    output_path.parent.mkdir(parents=True, exist_ok=True)
    deduped = {
        tuple(row[column] for column in OUTPUT_COLUMNS): row
        for row in rows
    }
    sorted_rows = sorted(
        deduped.values(),
        key=lambda row: (row["date"], row["source"], row["metric"], row["start_time"], row["end_time"]),
    )
    with output_path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=OUTPUT_COLUMNS)
        writer.writeheader()
        writer.writerows(sorted_rows)


def main() -> int:
    parser = argparse.ArgumentParser(description="Normalize Zepp/Health/Nutrition CSV exports.")
    parser.add_argument("--input", required=True, type=Path, help="CSV file or folder containing raw exports.")
    parser.add_argument("--output", required=True, type=Path, help="Destination normalized CSV.")
    parser.add_argument("--aliases", default=DEFAULT_ALIASES, type=Path, help="Metric alias JSON file.")
    args = parser.parse_args()

    aliases, alias_to_metric = load_aliases(args.aliases)
    files = iter_csv_files(args.input)
    rows: list[dict[str, str]] = []
    for path in files:
        rows.extend(normalize_file(path, aliases, alias_to_metric))

    write_rows(rows, args.output)
    print(f"Normalized {len(rows)} metric rows from {len(files)} CSV file(s) into {args.output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
