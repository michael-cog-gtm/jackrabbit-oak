#!/usr/bin/env bash
# Put the repo back into the pre-demo state so the live run can be repeated.
#   demo/reset.sh            # park the rehearsal branch/PR (kept as fallback), verify demo/pre-fix
#   demo/reset.sh --delete   # delete the rehearsal branch instead of parking it
set -euo pipefail
REPO=michael-cog-gtm/jackrabbit-oak
PRE_FIX_SHA=${PRE_FIX_SHA:-}          # set to pin the expected tip; otherwise just reported
ISSUE_BRANCH=issue/OAK-12164
STAMP=$(date -u +%Y%m%d-%H%M%S)

for b in $(gh api "repos/$REPO/branches?per_page=100" --jq '.[].name' | grep -E "^${ISSUE_BRANCH}" || true); do
  if [ "${1:-}" = "--delete" ]; then
    echo "deleting $b"; gh api -X DELETE "repos/$REPO/git/refs/heads/$b" >/dev/null
  else
    new="rehearsal/$STAMP/${b//\//-}"
    echo "parking $b -> $new (its PR moves with it and stays open)"
    gh api -X POST "repos/$REPO/branches/$b/rename" -f new_name="$new" >/dev/null
  fi
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
