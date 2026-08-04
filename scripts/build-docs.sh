#!/usr/bin/env bash
#
# build-docs.sh
#
# Mirror framework Markdown sources into docs-site/ so VitePress can
# build a Cloudflare Pages site from them. Run before `npm run docs:build`.
#
# Sources copied:
#   ../README.md                       → docs-site/index.md
#   ../README_zh.md                    → docs-site/zh-cn/index.md
#   ../docs/*.md                       → docs-site/docs/*.md
#   ../docs/reference/*.md             → docs-site/reference/*.md
#   ../docs/specs/*.md                 → docs-site/specs/*.md
#   ../docs/rules/global/*.md          → docs-site/rules/global/*.md
#   ../docs/logs/*.md                  → docs-site/logs/*.md
#
# @author han
# @time 2026/8/4
set -eu

# Resolve paths relative to repo root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
DOCS_SITE="$REPO_ROOT/docs-site"

echo ">> Building docs-site from $REPO_ROOT"
mkdir -p "$DOCS_SITE/docs"
mkdir -p "$DOCS_SITE/reference"
mkdir -p "$DOCS_SITE/specs"
mkdir -p "$DOCS_SITE/rules/global"
mkdir -p "$DOCS_SITE/logs"
mkdir -p "$DOCS_SITE/zh-cn"

# Mirror source files (rsync if available, otherwise cp)
mirror() {
    local src="$1"
    local dst="$2"
    if command -v rsync >/dev/null 2>&1; then
        rsync -a --delete "$src/" "$dst/"
    else
        rm -rf "$dst"/*
        cp -r "$src/." "$dst/"
    fi
}

mirror "$REPO_ROOT/docs" "$DOCS_SITE/docs"
mirror "$REPO_ROOT/docs/reference" "$DOCS_SITE/reference"
mirror "$REPO_ROOT/docs/specs" "$DOCS_SITE/specs"
mirror "$REPO_ROOT/docs/rules/global" "$DOCS_SITE/rules/global"
mirror "$REPO_ROOT/docs/logs" "$DOCS_SITE/logs"

# README files become the index pages
cp "$REPO_ROOT/README.md" "$DOCS_SITE/index.md"
cp "$REPO_ROOT/README_zh.md" "$DOCS_SITE/zh-cn/index.md"

echo ">> Mirror complete. docs-site/ contents:"
ls "$DOCS_SITE" | sed 's/^/   /'