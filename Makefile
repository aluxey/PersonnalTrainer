.PHONY: normalize report example all dev-backend dev-dashboard check test-ingest android-info

RAW_DIR ?= data/raw
PROCESSED ?= data/processed/daily_metrics.csv
REPORT_DIR ?= data/reports

all: normalize report

normalize:
	python3 scripts/normalize_daily.py --input $(RAW_DIR) --output $(PROCESSED)

report:
	python3 scripts/build_daily_report.py --input $(PROCESSED) --output-dir $(REPORT_DIR)

example:
	python3 scripts/normalize_daily.py --input examples/raw --output $(PROCESSED)
	python3 scripts/build_daily_report.py --input $(PROCESSED) --output-dir $(REPORT_DIR)

dev-backend:
	npm run dev:backend

dev-dashboard:
	npm run dev:dashboard

check:
	npm run check
	python3 -m py_compile scripts/normalize_daily.py scripts/build_daily_report.py

test-ingest:
	sh scripts/test_ingest.sh

android-info:
	@echo "Open android-exporter/ in Android Studio, sync Gradle, then run the app on your Android device."
	@echo "See android-exporter/README.md and docs/android-exporter.md."
