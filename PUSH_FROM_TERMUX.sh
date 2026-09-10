#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
REPO="KAN1409/NEXUS"
BRANCH="main"

echo "NEXUS Update 1 — GitHub bootstrap"
if ! command -v gh >/dev/null 2>&1; then
  echo "gh is required" >&2
  exit 1
fi

gh auth status >/dev/null

if gh repo view "$REPO" >/dev/null 2>&1; then
  echo "Repository already exists: $REPO"
else
  gh repo create "$REPO" --private --description "NEXUS — local-first personal intelligence agent for Android"
fi

if [ ! -d .git ]; then git init -b "$BRANCH"; fi
git add .
git commit -m "NEXUS Update 1: foundation" || true
git remote remove origin >/dev/null 2>&1 || true
git remote add origin "https://github.com/$REPO.git"
git push -u origin "$BRANCH"

echo "Pushed. GitHub Actions will build the debug APK automatically."
