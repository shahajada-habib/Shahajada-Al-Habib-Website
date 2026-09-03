#!/usr/bin/env bash
# Make this VM the primary host for <apex> and www.<apex>, keeping the existing
# gcp.<apex> address working too. Run this AFTER the apex + www DNS records have
# been pointed at this VM's IP.
#
#   bash set-primary-domain.sh shahajadaalhabib.com
#
# Rollback: point the apex/www DNS records back at Render, then re-run
#   bash enable-https.sh gcp.<apex>
set -euo pipefail
cd "$(dirname "$0")"

APEX="${1:-}"
[ -n "$APEX" ] || { echo "Usage: bash set-primary-domain.sh <apex-domain>" >&2; exit 1; }
[ -f .env ] || { echo "ERROR: .env not found in $(pwd)" >&2; exit 1; }

WWW="www.${APEX}"
GCP="gcp.${APEX}"
here=$(curl -s https://api.ipify.org)

# Resolve the apex through several independent resolvers; accept if ANY of them
# already sees this VM's IP. (Google's public resolver negative-caches a missing
# record for up to an hour, so relying on it alone can block for no reason.)
resolves_here() {
  getent hosts "$APEX" 2>/dev/null | grep -qw "$here" && return 0
  curl -s "https://dns.google/resolve?name=${APEX}&type=A" \
    | grep -oE '"data":"[0-9.]+"' | grep -oE '[0-9.]+' | grep -qx "$here" && return 0
  curl -s -H 'accept: application/dns-json' \
    "https://cloudflare-dns.com/dns-query?name=${APEX}&type=A" \
    | grep -oE '"data":"[0-9.]+"' | grep -oE '[0-9.]+' | grep -qx "$here" && return 0
  return 1
}

if [ "${FORCE:-}" = "1" ]; then
  echo "FORCE=1 set — skipping the DNS check."
elif resolves_here; then
  echo "DNS check: ${APEX} resolves to ${here}."
else
  echo "  ${APEX} does not resolve to ${here} yet on any resolver checked." >&2
  echo "  Wait for the A record to propagate, or re-run with:  FORCE=1 bash $0 $APEX" >&2
  exit 1
fi

echo "Updating .env ..."
set_kv() {
  if grep -q "^$1=" .env; then sed -i "s#^$1=.*#$1=$2#" .env
  else printf '%s=%s\n' "$1" "$2" >> .env; fi
}
set_kv SITE_DOMAIN          "${APEX} ${WWW} ${GCP}"
set_kv SITE_URL             "https://${APEX}"
set_kv CORS_ALLOWED_ORIGINS "https://${APEX},https://${WWW}"

echo "Recreating containers + restarting Caddy to fetch certs for ${APEX} and ${WWW} ..."
docker compose up -d
docker compose restart caddy

echo "Waiting for https://${APEX} (up to ~5 min)..."
for i in $(seq 1 60); do
  if curl -sfI "https://${APEX}/api/health" >/dev/null 2>&1; then
    echo
    echo "  LIVE: https://${APEX}"
    curl -s "https://${APEX}/api/health"; echo
    echo
    echo "  Next: point the cron-job.org keep-alive at https://${APEX}/api/health"
    exit 0
  fi
  sleep 5
done

echo >&2
echo "Timed out. Check Caddy:  docker compose logs --tail=50 caddy" >&2
echo "The apex + www DNS must resolve to ${here} for the certificate to issue." >&2
exit 1
