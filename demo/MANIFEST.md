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
| _(rehearsal PRs added below)_ | |

## Devin sessions

| Session | Role |
|---|---|
| https://devin-gtm.devinenterprise.com/sessions/3173d5f14f82437eabb5db678769cd42 | build session (this kit) |
| _(rehearsal sessions added below)_ | |

## Blueprint

- Repo blueprint suggestion for `michael-cog-gtm/jackrabbit-oak` (Maven 3.9.9 + Central mirror + Azurite image +
  warm `~/.m2`), proposed from the build session; pending Michael's approval in the session timeline.

## Recordings

- _(added after the testing-agent recording)_

## Automations / playbooks

- none
