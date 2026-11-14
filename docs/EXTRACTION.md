# History-Preserving Extraction Runbook

## Purpose

Move the Rust AR workspace from `andalus-ai/jezaku/Backend/ar-core` into this repository while preserving the commits that changed files under that path.

**Do not** copy files one by one through the GitHub Contents API. That produces a new history and loses attribution/blame for the original workspace.

This runbook deliberately keeps the current documentation commits in `arcore/main` and combines them with the filtered source history using a merge commit with two parents. The original Jezaku repository is not rewritten or modified.

## Verified source snapshot

At the time this runbook was prepared:

- Source repository: https://github.com/andalus-ai/jezaku
- Source path: `Backend/ar-core/`
- Source default branch: `main`
- Source main HEAD: `fc68bfbcb65e85d017dfd8fb2a7ebc50f2ddfdf8`
- Latest commit returned for the AR workspace path: `39b12b480cb553d8c60403f9a4db66e48e53b4ae`
- Destination repository: https://github.com/andalus-ai/arcore
- Destination branch: `main`

The source main HEAD may advance. The commands below clone the current source main. To reproduce the exact snapshot above, replace the source clone step with a checkout of the recorded source HEAD before filtering.

## Prerequisites

- Git
- Python 3
- `git-filter-repo` installed and available as `git filter-repo`
- Permission to push branches and open pull requests in `andalus-ai/arcore`

## Extraction procedure

Run these commands in a clean working directory. Do not run them inside your existing Jezaku working tree.

### 1. Clone and filter the source history

```bash
mkdir -p ../jse-migration
cd ../jse-migration

git clone https://github.com/andalus-ai/jezaku.git jezaku-filtered
cd jezaku-filtered

# Optional: pin to the exact source snapshot recorded above.
git checkout fc68bfbcb65e85d017dfd8fb2a7ebc50f2ddfdf8

git filter-repo \
  --path Backend/ar-core/ \
  --path-rename Backend/ar-core/:

git branch -M filtered-ar-core
```

The filter operation rewrites commit IDs because the tree paths change, but preserves the sequence and metadata of commits that touched the selected directory. Commits that did not affect the selected path are omitted.

### 2. Clone the destination and create a migration branch

```bash
cd ../
git clone https://github.com/andalus-ai/arcore.git arcore-migration
cd arcore-migration

git switch -c migration/history-preserving-import
git remote add filtered-source ../jezaku-filtered
git fetch filtered-source filtered-ar-core
```

### 3. Merge both histories without overwriting either

```bash
git merge --allow-unrelated-histories --no-commit filtered-source/filtered-ar-core
```

The destination currently has a project README and documentation. The imported workspace also has its original README. Resolve that one expected path collision by retaining the destination overview and preserving the original workspace README under docs:

```bash
git checkout --ours README.md
mkdir -p docs/upstream
git show filtered-source/filtered-ar-core:README.md > docs/upstream/AR-CORE-README.md

git add -A
git status --short
git commit -m "chore: import AR core workspace with preserved history"
```

If Git reports conflicts other than the README, stop and inspect them. Do not blindly choose ours/theirs for Cargo manifests, source files, or tests.

### 4. Verify the resulting tree and history

```bash
test -f Cargo.toml
test -f Cargo.lock
test -d crates

git log --graph --oneline --decorate --all --max-count=30
git show --stat --oneline HEAD
```

Confirm that:
- root `Cargo.toml` and `Cargo.lock` exist;
- all expected workspace crates are present;
- `docs/AGENTS.md` is not created (the repository-level `AGENTS.md` is authoritative);
- the merge commit has two parents;
- the filtered source history is reachable from the merge commit;
- the original Jezaku working tree and repository are unchanged.

### 5. Run the baseline checks

```bash
cargo fmt --all -- --check
cargo build --workspace --all-targets --locked
cargo test --workspace --locked
cargo clippy --workspace --all-targets --locked -- -D warnings
```

The source AR workspace's latest recorded validation reported a successful locked build and 47 tests at commit `39b12b480cb553d8c60403f9a4db66e48e53b4ae`. That is historical evidence only; rerun the checks on the extracted tree and record the actual result. Clippy must be run separately and any existing warnings documented.

### 6. Push the branch and open a pull request

```bash
git push -u origin migration/history-preserving-import
```

Open a PR from `migration/history-preserving-import` into `main`.

Suggested PR title:

`chore: extract AR workspace with preserved Git history`

Suggested PR checklist:
- [ ] Source path and source commit recorded.
- [ ] Filtered history is reachable from the merge commit.
- [ ] Root workspace manifests and all crates are present.
- [ ] Original upstream README preserved under `docs/upstream/`.
- [ ] No Jezaku product/business dependencies introduced.
- [ ] Formatting, build, tests, and Clippy results recorded.
- [ ] No unrelated changes to the Jezaku repository.

Merge the PR only after review and successful checks. Do not force-push to `main`.

## After extraction

1. Add standalone CI under `.github/workflows/` for formatting, locked build, tests, and Clippy.
2. Fix only extraction/build-path issues in the first follow-up PR.
3. Keep crate renames and API redesign out of the extraction PR.
4. Add the Android provider only after the standalone Rust baseline is green.
5. Update this runbook with the actual filtered source commit and resulting merge commit.

## Rollback

If validation fails before the PR is merged, close the PR and delete only the migration branch. Do not delete or rewrite the Jezaku source path. The destination documentation on `main` remains intact.
