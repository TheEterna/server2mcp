#!/usr/bin/env bash
#
# deploy-cf-pages.sh
#
# One-command Cloudflare Pages deployment for the api2mcp4j docs site.
# Bypasses the Cloudflare dashboard entirely — uses the Cloudflare API +
# wrangler CLI to create the Pages project, configure the custom domain,
# set DNS records, and trigger the first deployment.
#
# Prerequisites (one-time):
#   1. Cloudflare account with xiaohan.chat zone added
#   2. CF API token with these scopes:
#      - Account > Cloudflare Pages: Edit
#      - Account > DNS: Edit
#      - Zone > DNS: Edit
#        (https://dash.cloudflare.com/profile/api-tokens → Create Token →
#         Custom Token template)
#   3. Cloudflare Account ID (visible in dashboard right sidebar)
#
# Usage:
#   export CF_API_TOKEN=...
#   export CF_ACCOUNT_ID=...
#   bash scripts/deploy-cf-pages.sh
#
# @author han
# @time 2026/8/4
set -eu

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
PROJECT_NAME="api2mcp4j-docs"
CUSTOM_DOMAIN="docs.xiaohan.chat"
ZONE_NAME="xiaohan.chat"

echo "================================================="
echo "  api2mcp4j docs → Cloudflare Pages deployment"
echo "================================================="
echo
echo "Project name:    $PROJECT_NAME"
echo "Custom domain:   $CUSTOM_DOMAIN"
echo "Source:          $REPO_ROOT/docs-site"
echo

# ---------------------------------------------------------------------------
# 1. Validate prerequisites
# ---------------------------------------------------------------------------
if [ -z "${CF_API_TOKEN:-}" ]; then
    echo "❌ CF_API_TOKEN not set."
    echo "   Get one at: https://dash.cloudflare.com/profile/api-tokens"
    echo "   Required scopes: Account > Cloudflare Pages: Edit,"
    echo "                     Account > DNS: Edit,"
    echo "                     Zone > DNS: Edit"
    echo
    echo "   Then: export CF_API_TOKEN=..."
    exit 1
fi

if [ -z "${CF_ACCOUNT_ID:-}" ]; then
    echo "❌ CF_ACCOUNT_ID not set."
    echo "   Find it on the Cloudflare dashboard right sidebar after"
    echo "   logging in (or: curl -s -H \"Authorization: Bearer \$CF_API_TOKEN\" \\"
    echo "        https://api.cloudflare.com/client/v4/accounts | jq .result[0].id)"
    exit 1
fi

CF_API="https://api.cloudflare.com/client/v4"
CF_AUTH=(-H "Authorization: Bearer $CF_API_TOKEN" -H "Content-Type: application/json")

# ---------------------------------------------------------------------------
# 2. Ensure wrangler is available (for first deploy — Pages Direct Upload)
# ---------------------------------------------------------------------------
if ! command -v wrangler >/dev/null 2>&1; then
    echo ">> Installing wrangler..."
    npm install -g wrangler@latest
fi
echo ">> wrangler $(wrangler --version)"

# ---------------------------------------------------------------------------
# 3. Build docs locally (mirror + vitepress)
# ---------------------------------------------------------------------------
echo ">> Building docs site..."
bash "$REPO_ROOT/scripts/build-docs.sh"

cd "$REPO_ROOT/docs-site"
[ -d ".vitepress/dist" ] || { echo "❌ .vitepress/dist missing after build"; exit 1; }

# ---------------------------------------------------------------------------
# 4. Create Pages project (idempotent — fails silently if exists)
# ---------------------------------------------------------------------------
echo ">> Ensuring Pages project '$PROJECT_NAME' exists..."
EXISTING=$(curl -sf "${CF_API}/accounts/${CF_ACCOUNT_ID}/pages/projects" \
    "${CF_AUTH[@]}" | jq -r '.result[]?.name' | grep -Fx "$PROJECT_NAME" || true)

if [ -z "$EXISTING" ]; then
    CREATE_BODY=$(jq -n \
        --arg name "$PROJECT_NAME" \
        --arg prod "docs-site" \
        --arg cmd "cd .. && bash scripts/build-docs.sh && cd docs-site && npm ci && npm run docs:build" \
        --arg dest ".vitepress/dist" \
        '{name: $name, production_branch: $prod, deployment_configs: {production: {build_command: $cmd, destination_dir: $dest, root_dir: $prod}}}')
    curl -sf -X POST "${CF_API}/accounts/${CF_ACCOUNT_ID}/pages/projects" \
        "${CF_AUTH[@]}" --data "$CREATE_BODY" >/dev/null \
        && echo "   ✓ project created" \
        || { echo "❌ failed to create project"; exit 1; }
else
    echo "   ✓ project already exists"
fi

# ---------------------------------------------------------------------------
# 5. Direct Upload first deployment (so the project has a live version)
# ---------------------------------------------------------------------------
echo ">> Uploading first deployment..."
DEPLOY_OUT=$(wrangler pages deploy .vitepress/dist \
    --project-name "$PROJECT_NAME" \
    --branch main \
    --commit-dirty=true 2>&1) && echo "$DEPLOY_OUT" | tail -5

# ---------------------------------------------------------------------------
# 6. Configure custom domain (CNAME docs.xiaohan.chat → <project>.pages.dev)
# ---------------------------------------------------------------------------
echo ">> Configuring custom domain $CUSTOM_DOMAIN..."
DOMAIN_BODY=$(jq -n --arg d "$CUSTOM_DOMAIN" '{hostname: $d}')
curl -sf -X POST "${CF_API}/accounts/${CF_ACCOUNT_ID}/pages/projects/${PROJECT_NAME}/domains" \
    "${CF_AUTH[@]}" --data "$DOMAIN_BODY" >/dev/null \
    && echo "   ✓ custom domain attached (CF will issue SSL automatically)" \
    || echo "   (custom domain may already be configured — continuing)"

# ---------------------------------------------------------------------------
# 7. Configure DNS — CNAME docs.xiaohan.chat → api2mcp4j-docs.pages.dev
# ---------------------------------------------------------------------------
echo ">> Ensuring DNS CNAME for $CUSTOM_DOMAIN..."
ZONE_ID=$(curl -sf "${CF_API}/zones?name=${ZONE_NAME}" "${CF_AUTH[@]}" \
    | jq -r '.result[0]?.id')
if [ -z "$ZONE_ID" ]; then
    echo "❌ Zone $ZONE_NAME not found in this account."
    echo "   Add the xiaohan.chat zone to Cloudflare first:"
    echo "   https://dash.cloudflare.com → Add a Site"
    exit 1
fi

CNAME_TARGET="${PROJECT_NAME}.pages.dev"
EXISTING_REC=$(curl -sf "${CF_API}/zones/${ZONE_ID}/dns_records?name=docs" \
    "${CF_AUTH[@]}" | jq -r '.result[]?.id' | head -1)
if [ -z "$EXISTING_REC" ]; then
    DNS_BODY=$(jq -n \
        --arg t "CNAME" --arg n "docs" --arg c "$CNAME_TARGET" \
        --argjson proxied true \
        '{type: $t, name: $n, content: $c, proxied: $proxied}')
    curl -sf -X POST "${CF_API}/zones/${ZONE_ID}/dns_records" \
        "${CF_AUTH[@]}" --data "$DNS_BODY" >/dev/null \
        && echo "   ✓ DNS CNAME created (proxied through CF edge)" \
        || { echo "❌ DNS create failed"; exit 1; }
else
    echo "   ✓ DNS record already exists"
fi

# ---------------------------------------------------------------------------
# 8. Configure Git integration for automatic rebuilds on push
# ---------------------------------------------------------------------------
echo ">> Configuring Git integration (rebuilds on every push to master)..."
# We can't directly add a Git repo via API (CF requires OAuth App install)
# but the dashboard link is below for one-time setup.
cat <<EOF

   ┌──────────────────────────────────────────────────────────────────┐
   │  ONE-TIME SETUP for auto-rebuild on git push:                    │
   │                                                                  │
   │  Cloudflare dashboard → Pages → $PROJECT_NAME → Settings →       │
   │  Builds → Connect to Git → TheEterna/api2mcp4j → master          │
   │                                                                  │
   │  Then every push triggers rebuild + deploy automatically.        │
   │  No further manual intervention needed.                          │
   └──────────────────────────────────────────────────────────────────┘

EOF

# ---------------------------------------------------------------------------
# 9. Print access URLs
# ---------------------------------------------------------------------------
PAGES_URL="https://${PROJECT_NAME}.pages.dev"
CUSTOM_URL="https://${CUSTOM_DOMAIN}"
echo "================================================="
echo "  ✓ Deployment complete"
echo "================================================="
echo
echo "  Pages URL:    $PAGES_URL  (immediate)"
echo "  Custom URL:   $CUSTOM_URL  (after DNS + SSL, ~30s)"
echo
echo "  Next steps:"
echo "    1. Visit $CUSTOM_URL to verify"
echo "    2. (One-time) Connect Git for auto-rebuild via dashboard link above"
echo "    3. Future deploys: re-run this script, or push to master after step 2"