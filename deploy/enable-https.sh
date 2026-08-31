#!/usr/bin/env bash
# Milestone 2 — move the VM deploy behind Caddy with an automatic Let's Encrypt
# certificate.
#
# Do these first (one-time, cannot be scripted from here):
#   1. DNS: add an A record  <domain> -> this VM's external IP
#   2. GCP firewall: allow TCP 443 (you already allow 80)
#   3. git pull   (to get docker-compose.yml + Caddyfile + this script)
#
# Then:
#   bash enable-https.sh gcp.shahajadaalhabib.com
set -euo pipefail
cd "$(dirname "$0")"

DOMAIN="${1:-}"
[ -n "$DOMAIN" ] || { echo "Usage: bash enable-https.sh <domain>" >&2; exit 1; }
[ -f .env ] || { echo "ERROR: .env not found in $(pwd)" >&2; exit 1; }

echo "1/4  Checking DNS for $DOMAIN ..."
here=$(curl -s https://api.ipify.org)
dns=$(curl -s "https://dns.google/resolve?name=${DOMAIN}&type=A" \
        | grep -oE '"data":"[0-9.]+"' | grep -oE '[0-9.]+' | head -1 || true)
echo "     this VM: ${here}    DNS says: ${dns:-<no record>}"
if [ "${dns:-}" != "$here" ]; then
  echo "     -> DNS not ready. Add/fix the A record, wait a few minutes, re-run." >&2
  exit 1
fi

echo "2/4  Updating .env ..."
set_kv() {
  if grep -q "^$1=" .env; then
    sed -i "s#^$1=.*#$1=$2#" .env
  else
    printf '%s=%s\n' "$1" "$2" >> .env
  fi
}
set_kv WEB_HTTP_PORT       "127.0.0.1:8083"
set_kv SITE_DOMAIN         "$DOMAIN"
set_kv SITE_URL            "https://$DOMAIN"
set_kv CORS_ALLOWED_ORIGINS "https://$DOMAIN"
set_kv COMPOSE_PROFILES    "tls"

echo "3/4  Recreating containers (web moves off :80, Caddy takes over)..."
docker compose up -d

echo "4/4  Waiting for the app + certificate (this VM is slow — up to ~7 min)..."
for i in $(seq 1 84); do
  if curl -sfI "https://${DOMAIN}/api/health" >/dev/null 2>&1; then
    echo
    echo "  HTTPS is live:  https://${DOMAIN}"
    curl -s "https://${DOMAIN}/api/health"; echo
    echo
    echo "  Now do:"
    echo "   - cron-job.org: change the URL to  https://${DOMAIN}/api/health"
    echo "   - test the site + /admin/ over https"
    exit 0
  fi
  sleep 5
done

echo >&2
echo "Timed out. Check what Caddy is doing:" >&2
echo "   docker compose logs --tail=50 caddy" >&2
echo "Common causes: TCP 443 not open in the GCP firewall, or DNS still propagating." >&2
exit 1
