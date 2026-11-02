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

## Checks

```sh
bun run lint
bun run type-check
bun run build
bun run test:unit
bun run test:e2e
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
