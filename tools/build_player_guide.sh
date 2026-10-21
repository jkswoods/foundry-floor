#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
# Fixed release-source timestamp; this affects documentation metadata only.
export SOURCE_DATE_EPOCH="${SOURCE_DATE_EPOCH:-1792540800}"
"${FF_DOCS_PYTHON:-python3}" -m mkdocs build --strict
