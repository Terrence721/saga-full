# 📝 TODO

**Last Updated:** August 10, 2026 (build tooling decided, no services added yet)

A phase-by-phase log of what's been done on this repo and what's still open. This is the source of truth for progress.

**What this repo is:** a from-scratch personal demonstration workspace implementing the Distributed Saga pattern across independent microservices. This is not a fork or continuation of any existing project — the module boundaries and general shape of the problem are common territory for this class of system, but the code, design decisions, and tradeoffs recorded here are original. See [README.md](README.md) for the project description and [LICENSE](LICENSE) for licensing.

## At a glance

**Done, in full:**

| Item | Detail | Phases |
| - | - | - |
| Repo bootstrap | git init, README, MIT LICENSE (Terrence Daniels), docs/ skeletons, .gitignore, CONTRIBUTING.md | Phase 1 |
| Build tool: Gradle | Chosen over Maven (source's own tool) for incremental/cached builds across this six-module project — a build-time win, not a runtime one; reasoning in [docs/architecture.md](docs/architecture.md) | Phase 2 |

**Actually still open, right now:** CI workflows and every service module still need adding. See the **Still to do** table below.

## ✅ Done

### Repository bootstrap

| Phase | Date | What |
| - | - | - |
| 1 | 2026-08-10 | `git init`; README.md written describing the project and its scope; MIT LICENSE added under Terrence Daniels; `docs/architecture.md` and `docs/case-study.md` skeletons added; `.gitignore` added; `CONTRIBUTING.md` added |
| 2 | 2026-08-10 | Build tool decided: Gradle (Kotlin DSL) over Maven, for incremental compilation and build caching across this six-module project — faster local/CI build turnaround, with no effect on the running services' own performance. `.gitignore` and `CONTRIBUTING.md` updated from Maven to Gradle conventions; full reasoning in [docs/architecture.md](docs/architecture.md) |

## 🔧 Still to do

| Item | Detail |
| - | - |
| CI workflows | Build/test workflow + CodeQL, once there's code for them to run against |
| Service modules | `user-contract`, `user-service`, `order-service`, `payment-service`, `restaurant-service`, `api-gateway-service` — added one file/folder at a time, conferring at each step |
| `portfolio.html` | Case-study page for the portfolio site, written once there are real results/metrics to show |
