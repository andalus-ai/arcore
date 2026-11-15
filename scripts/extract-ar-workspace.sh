#!/usr/bin/env bash
set -Eeuo pipefail

# Run from any directory with Git, Python, git-filter-repo, and Cargo installed.
# Optional: SOURCE_COMMIT=<sha> to pin the Jezaku snapshot before filtering.
ORG="andalus-ai"
SOURCE_REPO="https://github.com/${ORG}/jezaku.git"
DEST_REPO="https://github.com/${ORG}/arcore.git"
SOURCE_PATH="Backend/ar-core/"
MIGRATION_BRANCH="migration/history-preserving-import"

for command in git python3 cargo; do
  command -v "$command" >/dev/null 2>&1 || {
    echo "Missing required command: $command" >&2
    exit 1
  }
done
git filter-repo --help >/dev/null 2>&1 || {
  echo "Install git-filter-repo and ensure 'git filter-repo' is available." >&2
  exit 1
}

WORK_DIR="$(mktemp -d "${TMPDIR:-/tmp}/jse-history-import.XXXXXX")"
cleanup() {
  if [[ "${KEEP_WORK_DIR:-0}" == "1" ]]; then
    echo "Keeping migration workspace: $WORK_DIR"
  else
    rm -rf "$WORK_DIR"
  fi
}
trap cleanup EXIT

echo "==> Clone source repository"
git clone "$SOURCE_REPO" "$WORK_DIR/jezaku"
cd "$WORK_DIR/jezaku"

if [[ -n "${SOURCE_COMMIT:-}" ]]; then
  git checkout --detach "$SOURCE_COMMIT"
fi
SOURCE_HEAD="$(git rev-parse HEAD)"
echo "Source snapshot: $SOURCE_HEAD"
# Ensure HEAD is attached so filter-repo can rewrite a named branch predictably.
git switch -c extraction-source

echo "==> Filter source history to $SOURCE_PATH"
git filter-repo --force --path "$SOURCE_PATH" --path-rename "$SOURCE_PATH:"
git branch -M filtered-ar-core

echo "==> Clone destination and create migration branch"
cd "$WORK_DIR"
git clone "$DEST_REPO" arcore
cd "$WORK_DIR/arcore"
if git ls-remote --exit-code --heads origin "$MIGRATION_BRANCH" >/dev/null 2>&1; then
  echo "Remote branch $MIGRATION_BRANCH already exists; refusing to overwrite it." >&2
  exit 1
fi
git switch -c "$MIGRATION_BRANCH"
git remote add filtered-source "$WORK_DIR/jezaku"
git fetch filtered-source filtered-ar-core

echo "==> Merge source history; README.md is the one expected collision"
if git merge --allow-unrelated-histories --no-commit filtered-source/filtered-ar-core; then
  :
else
  # A non-zero merge is expected only when the two repositories' READMEs conflict.
  :
fi

UNMERGED="$(git diff --name-only --diff-filter=U)"
UNEXPECTED="$(printf '%s\n' "$UNMERGED" | sed '/^$/d; /^README\.md$/d')"
if [[ -n "$UNEXPECTED" ]]; then
  echo "Unexpected merge conflicts; stop and resolve manually:" >&2
  printf '%s\n' "$UNEXPECTED" >&2
  exit 1
fi

if printf '%s\n' "$UNMERGED" | grep -qx 'README.md'; then
  git checkout --ours -- README.md
fi
mkdir -p docs/upstream
git show filtered-source/filtered-ar-core:README.md > docs/upstream/AR-CORE-README.md
cat > docs/upstream/EXTRACTION-SOURCE.txt <<EOF
Source repository: $SOURCE_REPO
Source path: $SOURCE_PATH
Source commit before filtering: $SOURCE_HEAD
Filtered branch: filtered-ar-core
EOF

git add -A
git diff --cached --check
git commit -m "chore: import AR core workspace with preserved history"

echo "==> Verify imported workspace and two-parent merge"
test -f Cargo.toml
test -f Cargo.lock
test -d crates
PARENT_COUNT="$(git rev-list --parents -n 1 HEAD | awk '{print NF-1}')"
if [[ "$PARENT_COUNT" != "2" ]]; then
  echo "Expected a two-parent history merge; found $PARENT_COUNT parents." >&2
  exit 1
fi
for crate in ar-core ar-geo ar-fusion ar-planes ar-vo ar-service ar-replay; do
  test -d "crates/$crate" || {
    echo "Missing expected crate directory: crates/$crate" >&2
    exit 1
  }
done

echo "==> Run Rust baseline checks"
cargo fmt --all -- --check
cargo build --workspace --all-targets --locked
cargo test --workspace --locked
cargo clippy --workspace --all-targets --locked -- -D warnings

echo "==> Push migration branch"
git push -u origin "$MIGRATION_BRANCH"
echo
echo "Migration branch pushed. Open a PR:"
echo "https://github.com/$ORG/arcore/compare/main...$MIGRATION_BRANCH"
echo "Source snapshot: $SOURCE_HEAD"
echo "Migration commit: $(git rev-parse HEAD)"
