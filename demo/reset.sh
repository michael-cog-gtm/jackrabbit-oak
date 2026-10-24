#!/usr/bin/env bash
# Put the repo back into the pre-demo state so the live run can be repeated.
#   demo/reset.sh            # archive issue/OAK-12164* branches under rehearsal/<stamp>/…, verify demo/pre-fix
#   demo/reset.sh --delete   # delete them instead of archiving
# The fallback PR's branch (KEEP_BRANCHES) is never touched.
#
# Why copy+delete instead of GitHub's "rename branch": a rename leaves a redirect, so the next
# session's push to issue/OAK-12164 re-attaches the parked PR to the old name (seen in rehearsal 2).
# Deleting the branch closes its PR; the closed PR keeps its diff and before/after evidence.
set -euo pipefail
REPO=michael-cog-gtm/jackrabbit-oak
ISSUE_PREFIX=issue/OAK-12164
KEEP_BRANCHES=${KEEP_BRANCHES:-issue/OAK-12164-v8-part-size}   # fallback PR #3
PRE_FIX_SHA=${PRE_FIX_SHA:-}          # set to pin the expected tip; otherwise just reported
STAMP=$(date -u +%Y%m%d-%H%M%S)

for b in $(gh api "repos/$REPO/branches?per_page=100" --jq '.[].name' | grep -E "^${ISSUE_PREFIX}" || true); do
  case " $KEEP_BRANCHES " in *" $b "*) echo "keeping $b (fallback PR)"; continue;; esac
  sha=$(gh api "repos/$REPO/git/ref/heads/$b" --jq '.object.sha')
  if [ "${1:-}" != "--delete" ]; then
    new="rehearsal/$STAMP/${b//\//-}"
    gh api -X POST "repos/$REPO/git/refs" -f ref="refs/heads/$new" -f sha="$sha" >/dev/null
    echo "archived $b ($sha) -> $new"
  fi
  gh api -X DELETE "repos/$REPO/git/refs/heads/$b" >/dev/null
  echo "deleted $b (its PR, if any, is now closed but still readable)"
done

tip=$(gh api "repos/$REPO/branches/demo/pre-fix" --jq '.commit.sha')
parent=$(gh api "repos/$REPO/commits/$tip" --jq '.parents[0].sha')
echo "demo/pre-fix tip: $tip (parent $parent)"
if [ "$parent" != "f14f6e3ee6bb838308b8c997d794756932ec4ae7" ]; then
  echo "WARNING: demo/pre-fix is not f14f6e3e + kit commit(s) as expected"; exit 1
fi
if [ -n "$PRE_FIX_SHA" ] && [ "$tip" != "$PRE_FIX_SHA" ]; then
  echo "WARNING: demo/pre-fix tip != $PRE_FIX_SHA"; exit 1
fi
echo "open PRs:"; gh pr list --repo "$REPO" --state open --json number,title,headRefName,baseRefName \
  --template '{{range .}}  #{{.number}} {{.headRefName}} -> {{.baseRefName}}: {{.title}}{{"\n"}}{{end}}'
docker rm -f oak-demo-azurite >/dev/null 2>&1 && echo "local Azurite container removed" || true
echo "ready: demo/pre-fix intact"
