#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
overlay="$root/deployments/kubernetes/overlays/production"
rendered="$(mktemp)"
trap 'rm -f "$rendered"' EXIT

kubectl kustomize "$overlay" > "$rendered"

grep -q 'kind: ExternalSecret' "$rendered"
# Kelima instance MongoDB di-pin ke sync-wave "-2" (lihat mongo-wave.yaml) agar
# dibuat lebih dulu sebelum service aplikasi (wave "2"). stats-backfill jalan di
# Sync phase default (wave 0) tanpa anotasi eksplisit.
grep -q 'argocd.argoproj.io/sync-wave: "-2"' "$rendered"
! grep -Eq 'image: .*:(latest|1\.0)([[:space:]]|$)' "$rendered"
! grep -Eq 'port: 0([[:space:]]|$)' "$rendered"
! grep -Eq '^[[:space:]]+(DB_PASSWORD|POSTGRES_PASSWORD|SECRET_KEY|REDIS_PASSWORD|SMTP_PASS):[[:space:]]+"[^$]' "$root/deployments/kubernetes/core/secrets.yaml"

echo "Production manifest validation passed"
