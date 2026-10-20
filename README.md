<div align="center">

# Bevoya

**Your life, moving forward.**

A private, Android-first personal growth app. Turn long-term goals into daily actions, record real evidence of progress, reflect honestly, and get practical AI guidance while you stay in control of your data.

<img src=".github/assets/language-bar.svg" alt="Stack: TypeScript, Java, Python, PL/pgSQL" width="100%">

![React Native](https://img.shields.io/badge/React_Native-Expo-20232a?logo=react&logoColor=61dafb)
![TypeScript](https://img.shields.io/badge/TypeScript-3178c6?logo=typescript&logoColor=white)
![Java](https://img.shields.io/badge/Java_21-Spring_Boot-b07219?logo=openjdk&logoColor=white)
![Python](https://img.shields.io/badge/Python-FastAPI-3572A5?logo=python&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-pgvector-336790?logo=postgresql&logoColor=white)
![Supabase](https://img.shields.io/badge/Supabase-Mumbai-3ecf8e?logo=supabase&logoColor=white)
![Gemini](https://img.shields.io/badge/Google-Gemini-8e75b2?logo=googlegemini&logoColor=white)

![Status](https://img.shields.io/badge/status-early_development-F2C98D)
![Platform](https://img.shields.io/badge/platform-Android-244C37?logo=android&logoColor=white)
![Audience](https://img.shields.io/badge/audience-18%2B-DDE8D8)
![License](https://img.shields.io/badge/license-proprietary-lightgrey)

</div>

---

## What it is

Bevoya is built around one loop:

```text
Goal → Roadmap → Daily plan → Session → Evidence → Reflection → Review → Next action
```

Every feature supports that loop, or it waits. AI proposes; you accept, edit or dismiss. Bevoya is not a to-do app, a habit tracker, a chatbot, or a source of medical, legal or financial advice.

> **Status:** early development. Nothing here is production-ready. First market: India (Pune). Launch audience: adults (18+). First language: English.

## Principles

- **You stay in control.** AI never acts on its own, and every suggestion shows its sources.
- **Privacy first.** Journals are private, AI memory is opt-in, deletion is real.
- **Calm, never shaming.** No guilt, no fake urgency, no streak threats.
- **Accessible by default.** TalkBack, 200% font scale, 44 dp touch targets, status never by colour alone.

## Architecture

```text
Android app (React Native, Expo, TypeScript)
   │ sign-in                 │ HTTPS + Bearer token
   ▼                         ▼
Supabase Auth        Java Spring Boot API  (Render, Singapore)
(Mumbai)                │ JDBC                 │ HTTPS + internal token
                        ▼                      ▼
        Supabase PostgreSQL + pgvector     Python FastAPI AI service
        (Mumbai, ap-south-1)                    │ HTTPS + API key
                                                ▼
                                          Google Gemini API
```

| Service | Folder | Owns |
|---|---|---|
| Mobile app | `mobile/` | UI, offline drafts, sign-in with Supabase Auth |
| Core API | `backend-java/` | Auth, records, consent, quotas, jobs, billing, audit, **all database access** |
| AI service | `ai-python/` | LLM calls, embeddings, structured extraction. **No database access** |

Python proposes. Java validates and persists. Details: [`docs/architecture/ARCHITECTURE.md`](docs/architecture/ARCHITECTURE.md) and the decision records in [`docs/decisions/`](docs/decisions/).

## Repository layout

```text
bevoya/
├── CLAUDE.md            rules for AI coding assistants (read first)
├── README.md
├── mobile/              React Native app (routes in mobile/src/app)
├── backend-java/        Spring Boot modular monolith
├── ai-python/           FastAPI AI service
├── docs/                product, design, architecture, security, privacy, decisions
├── infrastructure/      compose file and database bootstrap
└── .github/             workflows, PR template, assets
```

## Documentation

| Area | Start here |
|---|---|
| Product | [`docs/product/PRODUCT.md`](docs/product/PRODUCT.md), [`AI_RULES.md`](docs/product/AI_RULES.md) |
| Design | [`DESIGN.md`](docs/design/DESIGN.md), [`COLORS.md`](docs/design/COLORS.md), [`TYPOGRAPHY.md`](docs/design/TYPOGRAPHY.md), [`COMPONENTS.md`](docs/design/COMPONENTS.md), [`ACCESSIBILITY.md`](docs/design/ACCESSIBILITY.md), [`VOICE_AND_TONE.md`](docs/design/VOICE_AND_TONE.md) |
| Security and privacy | [`THREAT_MODEL.md`](docs/security/THREAT_MODEL.md), [`DATA_INVENTORY.md`](docs/privacy/DATA_INVENTORY.md) |
| Decisions | [`docs/decisions/`](docs/decisions/) |

> If a path above is wrong for your folder layout, fix the link. The files are listed where `CLAUDE.md` says they live.

## Getting started (Windows, PowerShell 7)

```powershell
git clone https://github.com/YOUR_ORG/bevoya.git
cd bevoya
git checkout develop

# Mobile
cd mobile
npm ci
npx expo run:android      # development build; needs Android Studio and a device or emulator
```

Java and Python setup commands will be added when those services are scaffolded. Copy each `.env.example` to `.env.local` and fill in values. **Never commit real secrets.** Local development uses the hosted Supabase dev project and synthetic data only.

## Contributing

- Branch from `develop` using `type/short-kebab-description`; use Conventional Commits.
- Read [`CLAUDE.md`](CLAUDE.md) and tick the Definition of Done in the pull request template.
- Every user-data endpoint needs a cross-user test.

## Security

Report vulnerabilities privately. See [`SECURITY.md`](SECURITY.md). Do not open public issues for security problems.

## License

Copyright (c) 2026 Bevoya. All rights reserved.
