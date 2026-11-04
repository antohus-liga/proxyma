# Proxyma frontend

Vue 3 + TypeScript frontend for Proxyma, the managed reverse proxy.

## Stack

- Vue 3, Vue Router, Pinia
- Vite
- Reka UI primitives with custom Proxyma CSS
- Lucide icons
- Self-hosted Inter and JetBrains Mono
- vue-i18n, default locale `en-GB`
- Vitest and Playwright

No utility CSS framework, component kit, or preprocessor is used.

## Design source of truth

Read [docs/DESIGN_GUIDE.md](./docs/DESIGN_GUIDE.md) before changing UI. Design tokens live in
`src/assets/tokens.css`; components must use semantic `--px-*` tokens only.

## Setup

```sh
bun install
bun dev
```

## Local API development

Vite proxies `/api` to the backend at `http://localhost:8080`, so the browser sees same-origin
requests and the refresh cookie is sent normally.

The backend marks the refresh cookie `Secure` by default. Because local development uses plain
HTTP, refreshing a session can fail unless secure cookies are disabled for the backend process,
for example:

```sh
proxyma.auth.cookie.secure=false
```

## Checks

```sh
bun run lint
bun run lint:check
bun run type-check
bun run build
bun run test:unit
bun run test:e2e
```

### WebKit on Fedora

Playwright's WebKit bundle is built for Ubuntu 24.04 and needs older shared
libraries than Fedora ships (`libicu*.so.74`, `libjpeg.so.8`, `libjxl.so.0.8`,
`libbacktrace.so.0`). Installing Fedora's packages does not help, and symlinking
the newer libraries is unsafe because their ABIs differ. Run WebKit through the
official Playwright container instead:

```sh
bun run test:e2e:webkit
```

The script builds the app, then runs `playwright test --project=webkit` inside
`mcr.microsoft.com/playwright:v<installed-version>-noble` using Podman or Docker.
It reuses the local `node_modules`, so only the image needs pulling on first use.
Set `SKIP_BUILD=1` to skip the rebuild when `dist/` is already current
(`SKIP_BUILD=1 bun run test:e2e:webkit`), and pass extra Playwright arguments
after the script name, for example
`bun run test:e2e:webkit --grep account`.

Chromium and Firefox still run natively:

```sh
bunx playwright test --project=chromium --project=firefox
```

## Structure

```text
src/
├── assets/        # tokens, base styles, brand artwork
├── components/    # layout, homepage, and shared UI components
├── composables/   # theme and formatting helpers
├── i18n/          # locale setup and message catalogues
├── router/        # public and application routes
└── views/         # route views
```
