# Bevoya environment and local setup

Version 1 · 20 October 2026

## 1. Rules

- Real values live only in `.env.local` files (git-ignored) and in host secret stores (Render, GitHub Actions). Committed `.env.example` files contain names and placeholders only.
- Anything named `EXPO_PUBLIC_*` is compiled into the APK and is public by definition. Never put a secret in it.
- Never paste a key into a chat, an issue or a commit. If a key leaks, rotate it immediately.
- The Supabase service-role key goes only in `backend-java/.env.local`, never in `mobile/`.

## 2. Files

| File | Holds |
|---|---|
| `mobile/.env.local` | `EXPO_PUBLIC_API_BASE_URL`, `EXPO_PUBLIC_SUPABASE_URL`, `EXPO_PUBLIC_SUPABASE_ANON_KEY`, later Sentry and PostHog public keys |
| `backend-java/.env.local` | Database URL and `bevoya_app` credentials, Supabase URL, JWKS URL, issuer, audience, service-role key, AI service URL, internal token, AI quotas, Flyway admin credentials (migrations only) |
| `ai-python/.env.local` | Internal token, `GEMINI_API_KEY`, model IDs, embedding settings, token ceilings |

On the Android emulator, the PC is reachable at `10.0.2.2`. On a real phone use the PC's LAN IP in `EXPO_PUBLIC_API_BASE_URL`.

## 3. AI model variables (verified IDs, 20 October 2026)

```text
GEMINI_FAST_MODEL=gemini-3.5-flash-lite
GEMINI_TEXT_MODEL=gemini-3.8-flash
GEMINI_STRONG_MODEL=gemini-3.8-flash
GEMINI_EMBEDDING_MODEL=<decide: gemini-embedding-001 or gemini-embedding-2, see ADR-0004>
EMBEDDING_DIMENSIONS=768
```

## 4. Check that variables are set without showing them

```bash
cd /c/Projects/bevoya
for f in backend-java/.env.local ai-python/.env.local mobile/.env.local; do
  echo "== $f"
  grep -E '^[A-Z_]+=' "$f" | awk -F= '{ printf "%-34s %s\n", $1, ($2=="" ? "EMPTY" : "set") }'
done
```

Check that the two services share the same internal token:

```bash
diff <(grep '^INTERNAL_SERVICE_TOKEN' backend-java/.env.local) <(grep '^INTERNAL_SERVICE_TOKEN' ai-python/.env.local) && echo "tokens match"
```

Check that no secret was committed (should print nothing):

```bash
git ls-files | xargs grep -lE 'AIza[0-9A-Za-z_-]{20,}|sb_secret_|eyJ[A-Za-z0-9_-]{20,}\.' 2>/dev/null
git ls-files | grep -E '(^|/)\.env(\..*)?$' | grep -v '\.env\.example$'
```

## 5. Supabase

- Projects: `bevoya-dev` (now) and `bevoya-prod` (later), both in South Asia (Mumbai, ap-south-1).
- The database password is shown once at creation; keep it in a password manager.
- Use the **session-mode pooler** connection string for Java and Flyway (the direct connection is IPv6-only).
- Turn the Data API off; Row Level Security stays on for every table.
- Create the `bevoya_app` role by hand with a real password (see the blueprint, section 6).
- Confirm in the dashboard that the project uses JWT signing keys, and use the JWKS URL for token validation.

## 6. Windows and tools

- Terminal: Git Bash in VS Code. `core.autocrlf` is `input`; `.gitattributes` forces LF.
- Installed and working: Git, Node (22 or newer), Java 21, Python, uv, GitHub CLI. Android Studio is installed.
- **Docker Desktop does not run** on this laptop (ADR-0006). BIOS virtualization shows Enabled and `HypervisorPresent` is True, but the Windows services `vmcompute` and `hns` are missing, so WSL2 cannot start. A DISM repair and feature reinstall did not fix it. If you retry later: run `sfc /scannow` again after a restart, then consider enabling the full Hyper-V feature.
- Run Java as `./mvnw.cmd` in PowerShell-style paths, or `./mvnw` in Git Bash.

## 7. Run commands (once the services exist)

```bash
# Python AI service
cd ai-python && uv run uvicorn app.main:app --reload --port 8081
# Java API
cd backend-java && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
# Mobile
cd mobile && npx expo start --dev-client
```
