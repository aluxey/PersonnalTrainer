#!/usr/bin/env python3
"""Build daily summaries and a markdown report from normalized health metrics."""

from __future__ import annotations

import argparse
import csv
import json
from collections import defaultdict
from datetime import date
from pathlib import Path
from statistics import mean


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_GOALS = ROOT / "config" / "goals.json"

SUM_METRICS = {
    "steps",
    "distance_m",
    "sleep_duration_min",
    "sleep_deep_min",
    "sleep_rem_min",
    "sleep_awake_min",
    "active_energy_kcal",
    "workout_duration_min",
    "workout_energy_kcal",
    "calories_intake_kcal",
    "protein_g",
    "carbs_g",
    "fat_g",
    "fiber_g",
    "water_ml",
}

AVG_METRICS = {
    "sleep_score",
    "heart_rate_resting_bpm",
    "weight_kg",
    "body_fat_pct",
}

REPORT_METRICS = [
    "weight_kg",
    "calories_intake_kcal",
    "protein_g",
    "steps",
    "sleep_duration_min",
    "active_energy_kcal",
    "workout_duration_min",
]


def read_metrics(path: Path) -> dict[str, dict[str, list[float]]]:
    by_day: dict[str, dict[str, list[float]]] = defaultdict(lambda: defaultdict(list))
    with path.open(newline="", encoding="utf-8") as handle:
        reader = csv.DictReader(handle)
        for row in reader:
            try:
                value = float(row["value"])
            except (KeyError, TypeError, ValueError):
                continue
            metric = row.get("metric", "").strip()
            day = row.get("date", "").strip()
            if metric and day:
                by_day[day][metric].append(value)
    return by_day


def reduce_metric(metric: str, values: list[float]) -> float:
    if metric in AVG_METRICS:
        return mean(values)
    if metric in SUM_METRICS:
        return sum(values)
    return mean(values)


def build_summary(by_day: dict[str, dict[str, list[float]]]) -> list[dict[str, float | str]]:
    all_metrics = sorted({metric for metrics in by_day.values() for metric in metrics})
    rows: list[dict[str, float | str]] = []
    for day in sorted(by_day):
        row: dict[str, float | str] = {"date": day}
        for metric in all_metrics:
            values = by_day[day].get(metric, [])
            if values:
                row[metric] = round(reduce_metric(metric, values), 2)
        rows.append(row)
    return rows


def write_summary(rows: list[dict[str, float | str]], output_path: Path) -> None:
    output_path.parent.mkdir(parents=True, exist_ok=True)
    fields = ["date"] + sorted({key for row in rows for key in row if key != "date"})
    with output_path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields)
        writer.writeheader()
        writer.writerows(rows)


def load_goals(path: Path) -> dict:
    with path.open(encoding="utf-8") as handle:
        return json.load(handle)


def fmt(value: float | str | None, suffix: str = "") -> str:
    if value is None or value == "":
        return "-"
    if isinstance(value, float):
        text = f"{value:.1f}".rstrip("0").rstrip(".")
    else:
        text = str(value)
    return f"{text}{suffix}"


def average(rows: list[dict[str, float | str]], metric: str) -> float | None:
    values = [float(row[metric]) for row in rows if metric in row and row[metric] != ""]
    return mean(values) if values else None


def status_line(label: str, value: float | None, target: float, unit: str, higher_is_better: bool = True) -> str:
    if value is None:
        return f"- {label}: donnees manquantes."
    ok = value >= target if higher_is_better else value <= target
    marker = "OK" if ok else "A surveiller"
    return f"- {label}: {fmt(value, unit)} / cible {fmt(target, unit)} - {marker}."


def build_report(rows: list[dict[str, float | str]], goals: dict, output_dir: Path) -> Path:
    output_dir.mkdir(parents=True, exist_ok=True)
    report_path = output_dir / "latest_report.md"
    if not rows:
        report_path.write_text("# Rapport quotidien\n\nAucune donnee disponible.\n", encoding="utf-8")
        return report_path

    latest = rows[-1]
    last_7 = rows[-7:]
    previous_7 = rows[-14:-7]
    generated = date.today().isoformat()

    weight_7 = average(last_7, "weight_kg")
    previous_weight_7 = average(previous_7, "weight_kg")
    weight_delta = None
    if weight_7 is not None and previous_weight_7 is not None:
        weight_delta = weight_7 - previous_weight_7

    lines = [
        "# Rapport quotidien",
        "",
        f"Genere le {generated}. Dernier jour de donnees : {latest['date']}.",
        "",
        "## Dernier jour",
        "",
        "| Metrique | Valeur |",
        "| --- | ---: |",
    ]

    labels = {
        "weight_kg": ("Poids", " kg"),
        "calories_intake_kcal": ("Calories consommees", " kcal"),
        "protein_g": ("Proteines", " g"),
        "steps": ("Pas", ""),
        "sleep_duration_min": ("Sommeil", " min"),
        "active_energy_kcal": ("Calories actives", " kcal"),
        "workout_duration_min": ("Sport", " min"),
    }
    for metric in REPORT_METRICS:
        label, suffix = labels[metric]
        lines.append(f"| {label} | {fmt(latest.get(metric), suffix)} |")

    lines.extend(
        [
            "",
            "## Moyennes 7 jours",
            "",
            status_line(
                "Pas",
                average(last_7, "steps"),
                float(goals["activity"]["steps_target"]),
                "",
            ),
            status_line(
                "Sommeil",
                average(last_7, "sleep_duration_min"),
                float(goals["recovery"]["sleep_target_min"]),
                " min",
            ),
            status_line(
                "Proteines",
                average(last_7, "protein_g"),
                float(goals["weight_loss"]["protein_target_g"]),
                " g",
            ),
            status_line(
                "Calories",
                average(last_7, "calories_intake_kcal"),
                float(goals["weight_loss"]["daily_calorie_target_kcal"]),
                " kcal",
                higher_is_better=False,
            ),
        ]
    )

    if weight_7 is not None:
        lines.append(f"- Poids moyen 7 jours: {fmt(weight_7, ' kg')}.")
    if weight_delta is not None:
        direction = "baisse" if weight_delta < 0 else "hausse"
        lines.append(f"- Tendance poids vs 7 jours precedents: {direction} de {fmt(abs(weight_delta), ' kg')}.")

    lines.extend(
        [
            "",
            "## Lecture pratique",
            "",
            "- Si le poids moyen ne descend pas pendant 14 jours, ajuste d'abord l'apport calorique moyen ou les pas.",
            "- Si la faim augmente, controle d'abord sommeil, proteines, fibres et regularite des repas.",
            "- Les calories actives de la montre sont indicatives ; le poids moyen reste l'arbitre principal.",
            "",
        ]
    )

    report_path.write_text("\n".join(lines), encoding="utf-8")
    return report_path


def main() -> int:
    parser = argparse.ArgumentParser(description="Build a daily lifestyle report from normalized metrics.")
    parser.add_argument("--input", required=True, type=Path, help="Normalized daily metrics CSV.")
    parser.add_argument("--output-dir", required=True, type=Path, help="Folder for summary and report outputs.")
    parser.add_argument("--goals", default=DEFAULT_GOALS, type=Path, help="Goals JSON file.")
    args = parser.parse_args()

    by_day = read_metrics(args.input)
    rows = build_summary(by_day)
    write_summary(rows, args.output_dir / "daily_summary.csv")
    report = build_report(rows, load_goals(args.goals), args.output_dir)
    print(f"Wrote {len(rows)} daily summary row(s) and report {report}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
