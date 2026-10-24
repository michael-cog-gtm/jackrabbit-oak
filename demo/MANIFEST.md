# Demo A manifest — everything created outside this repo's trunk

All objects live in the shared Cognition GTM Devin org / the `michael-cog-gtm` GitHub org and were created
by the build session https://devin-gtm.devinenterprise.com/sessions/3173d5f14f82437eabb5db678769cd42.

## Branches (michael-cog-gtm/jackrabbit-oak)

| Branch | Contents |
|---|---|
| `demo/pre-fix` | `f14f6e3e` + demo kit commit — live demo start state |
| `demo/kit-on-trunk` | `trunk` (`52f2591e`) + the same kit — "after" comparison |
| `replay-base` | `54b40abd` (parent of #2409) |
| `replay/OAK-11267-pr2409` | `replay-base` + `f14f6e3e` (exact #2409 diff) |
| `rehearsal/<stamp>/issue-OAK-12164` | parked rehearsal branches (created by `demo/reset.sh`) |

## Pull requests

| PR | Purpose |
|---|---|
| https://github.com/michael-cog-gtm/jackrabbit-oak/pull/1 | Replay of #2409 for Devin Review — never merge |
| https://github.com/michael-cog-gtm/jackrabbit-oak/pull/2 | Rehearsal 1 fix PR (closed by `reset.sh` to free `issue/OAK-12164`; branch archived as `rehearsal/20261024-005021/issue-OAK-12164`) |
| https://github.com/michael-cog-gtm/jackrabbit-oak/pull/3 | Rehearsal 2 fix PR — **fallback, keep open, never merge** (`issue/OAK-12164-v8-part-size` → `demo/pre-fix`) |
| https://github.com/michael-cog-gtm/jackrabbit-oak/pull/4 | Rehearsal 3 fix PR — open, second fallback (`issue/OAK-12164-v8-limits` → `demo/pre-fix`) |

## Devin sessions

| Session | Role |
|---|---|
| https://devin-gtm.devinenterprise.com/sessions/3173d5f14f82437eabb5db678769cd42 | build session (this kit) |
| https://devin-gtm.devinenterprise.com/sessions/20d3392b3fe046e785dec812bf5433a9 | rehearsal 1 |
| https://devin-gtm.devinenterprise.com/sessions/bd84e88629e242d2a105c5265e648e34 | rehearsal 2 |
| https://devin-gtm.devinenterprise.com/sessions/8ef1f062d48c447cac3297d6d5af7b9c | rehearsal 3 (warm snapshot) |

## Blueprint

- Repo blueprint suggestion for `michael-cog-gtm/jackrabbit-oak` (Maven 3.9.9 + Central mirror + Azurite image +
  warm `~/.m2`), proposed from the build session; **approved by Michael 00:36 UTC 2026-10-24**, snapshot rebuild triggered automatically.

## Recordings

- Proof video (testing agent, build session): https://devin-gtm.devinenterprise.com/attachments/2f074816-cd7c-4251-ad88-da3a570b4270/oak-demo-a-full-proof-edited.mp4
- Key screenshots + transcript committed under `demo/evidence/recording/`

## Automations / playbooks

- none
