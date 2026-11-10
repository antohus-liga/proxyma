# Proxyma Design Guide

> **Status:** v1.0 — 2 November 2026  
> **Scope:** authentication, the Proxyma dashboard, platform administration, and the public homepage.  
> **Audience:** designers, frontend engineers, and AI coding agents.

Proxyma is a reverse proxy you don't have to babysit. A person or a small team gets an isolated proxy container and a dashboard for routes, rate limits, detection, bans, live traffic, audit history, and team access.

This guide is the visual and interaction source of truth. It defines the design language, tokens, components, layouts, copy, and implementation rules. It does **not** claim that planned backend behaviour is already live. Product behaviour lives in [`docs/api.md`](../../docs/api.md), [`docs/proxy-config.md`](../../docs/proxy-config.md), [`docs/architecture.md`](../../docs/architecture.md), [`docs/proxyma-security.md`](../../docs/proxyma-security.md), and the implemented-endpoint contract in [`API_CONTRACT.md`](./API_CONTRACT.md).

---

## Table of contents

1. [How to use this guide](#1-how-to-use-this-guide)
2. [Brand identity](#2-brand-identity)
3. [Design philosophy](#3-design-philosophy)
4. [Visual direction](#4-visual-direction)
5. [Design tokens](#5-design-tokens)
6. [Typography](#6-typography)
7. [Layout and spacing](#7-layout-and-spacing)
8. [Components](#8-components)
9. [Application patterns](#9-application-patterns)
10. [Data visualisation](#10-data-visualisation)
11. [Interaction design](#11-interaction-design)
12. [Responsive behaviour](#12-responsive-behaviour)
13. [Accessibility](#13-accessibility)
14. [Brand voice](#14-brand-voice)
15. [Do's and don'ts](#15-dos-and-donts)
16. [Implementation guidelines](#16-implementation-guidelines)
17. [Appendices](#17-appendices)

---

## 1. How to use this guide

### 1.1 Source-of-truth order

When sources disagree, use this order:

1. **Implemented API behaviour** — [`frontend/docs/API_CONTRACT.md`](./API_CONTRACT.md) and the backend tests.
2. **This guide** — visual language, tokens, component behaviour, layout, copy.
3. **Product design documents** — [`docs/api.md`](../../docs/api.md), [`docs/proxy-config.md`](../../docs/proxy-config.md), [`docs/architecture.md`](../../docs/architecture.md), [`docs/proxyma-security.md`](../../docs/proxyma-security.md).
4. **The codebase** — once components exist, their documented patterns become the reference, but this guide wins over accidental drift.

Planned endpoints in `docs/api.md` may be designed against, but screens must label them as unavailable until the backend implements them. Never display invented data.

### 1.2 Rule language

- **MUST** — required for consistency or accessibility. A deviation is a defect.
- **SHOULD** — the default. Deviate only with a written reason in the pull request or decision log.
- **MAY** — optional, for future needs.

### 1.3 What every change must satisfy

Before a UI change is considered done:

- [ ] It uses semantic tokens, never raw hex values outside `tokens.css`.
- [ ] It defines default, hover, active, focus-visible, disabled, loading, error, and empty states where relevant.
- [ ] It works at 320px, 768px, 1024px, and 1440px.
- [ ] It works in light and dark mode.
- [ ] It works with a keyboard only and with reduced motion enabled.
- [ ] Text and control boundaries meet the contrast thresholds in [section 13](#13-accessibility).
- [ ] Every string comes from the message catalogue, in British English.
- [ ] Log content is rendered as plain text, never with `v-html`.
- [ ] It adds no unapproved dependency.
- [ ] `bun run lint`, `bun run type-check`, and `bun run build` pass.
- [ ] The relevant Playwright test and axe check pass.

### 1.4 How AI agents should use this guide

1. Read sections 2–7 before writing any markup.
2. Find the component in section 8. Reuse the specified wrapper; do not restyle a Reka primitive from scratch.
3. Find the screen in section 9 before inventing a layout.
4. Use tokens from section 5. If a value is missing, propose a token addition here first; do not inline a one-off value.
5. Use copy patterns from section 14 and keys from the message catalogue. Do not hardcode English text in a component.
6. Run the checklist in section 1.3 and the review checklist in section 16.9.
7. If the guide and a product document conflict, stop and record the question in section 17.4 rather than guessing.

### 1.5 Current repository reality

At the time of writing, the frontend implements the design tokens and primitives, the public homepage, authentication, the application shell, account profile and password settings, and platform administration. Organisation and proxy surfaces are still planned. This guide therefore describes both the implemented system and the target for surfaces that do not exist yet; status notes in section 9 mark what is live. Accidental drift in the codebase does not override this guide.

---

## 2. Brand identity

### 2.1 Positioning

**Proxyma is a reverse proxy you don't have to babysit.**

It gives solo developers and small teams a working proxy without server setup, hand-edited configuration, or terminal archaeology. It is intentionally not an enterprise traffic platform. It should feel like a well-made instrument: approachable, minimal, reliable, transparent, and calm.

### 2.2 Name and tagline

- The name is always **Proxyma**, with a capital P, in prose.
- The wordmark is lowercase: **`proxyma`**.
- The tagline is **“Always near, always here.”** Use it on the public homepage and authentication surfaces. Do not repeat it inside the dashboard.
- The name comes from Latin *proxima*, “nearest”. Use that explanation on the homepage at most once, as supporting copy, never as a decorative flourish.

### 2.3 Personality

| Proxyma is | Proxyma is not |
|---|---|
| Calm and confident | Loud, hyped, or salesy |
| Precise and technical when it matters | Jargon-heavy for its own sake |
| Warm but not cute | Playful to the point of undermining trust |
| Transparent about state | Vague about failures |
| Minimal by subtraction | Minimal by hiding necessary information |
| Developer-friendly | Developer-exclusive |

### 2.4 Logo and wordmark

No official logo exists yet.

**MUST**

- Use a **text wordmark placeholder** until the official logo arrives: `proxyma`, lowercase, Inter 600, `--px-color-text-primary`, tracking `-0.02em`.
- Keep clear space of at least `0.5em` around the wordmark.
- Do not invent a symbol, monogram, globe mark, or proxy glyph.
- Do not use the globe art as a logo, app icon, favicon, avatar, or loading mark.

**When the logo arrives**

- Place the official mark in the sidebar header, authentication card, homepage header, and favicon.
- Provide a monochrome variant that inherits `currentColor`.
- Define a minimum size and clear-space rule in this section before shipping it.
- Keep the text wordmark as a fallback if the mark is unavailable.

### 2.5 Favicon

The current `frontend/public/favicon.ico` is the Vue favicon and must not ship.

**MUST**

- Leave the favicon empty until the official logo exists.
- Remove the Vue favicon reference, or use `<link rel="icon" href="data:,">` to suppress the browser's automatic request.
- Do not substitute the globe art.
- When the logo arrives, provide `.ico` and SVG favicons plus an Apple touch icon.

### 2.6 Globe artwork

`frontend/src/assets/globe-proxyma.webp` (1024×1024, transparent, ~281 KB), a 512×512 WebP variant for 1× displays, and its PNG fallback are editorial artwork: a hand-drawn wireframe globe with a connected network mesh in oxblood/brown, sampled around `#6F3127`.

**Purpose:** proximity and connected traffic. It supports the homepage narrative; it is not decoration for the dashboard.

**MUST**

- Use it on the **public homepage only** for now.
- Preserve its oxblood colour in light mode. In dark mode, invert the ink to a warm light tone with CSS: `filter: invert(1) hue-rotate(180deg) saturate(1.15) brightness(0.94)`. Do not tint it teal, apply a duotone, or add a glow.
- On light paper, place it directly on the canvas with transparency.
- In dark mode, keep the treated artwork directly on the canvas. Do not put it on a plate, card, or light background.
- Mark it `alt=""` when it is purely decorative.
- Do not render it below 240px wide; the mesh detail collapses.
- Prefer WebP. Keep the PNG only as a fallback. Serve the 512×512 WebP (~100 KB) at 1× and the 1024×1024 WebP (~281 KB) at 2×.

**SHOULD**

- Hide the artwork below 480px, or place it below the hero copy at 280–320px.
- Lazy-load it if it falls below the fold.

---

## 3. Design philosophy

### 3.1 Data is the decoration

The interface is full of real information: states, counts, routes, latency, logs. That is enough. Do not add illustrations, gradients, glows, or decorative charts to fill space.

**Implication:** if a visual element does not carry meaning or improve scanability, remove it.

### 3.2 Ink and paper, not card soup

Use a warm neutral canvas, white surfaces, 1px rules, and restrained radii. Group with spacing and borders before reaching for a card.

**Implication:** a page may contain several sections without wrapping each one in a floating card. Shadows are reserved for overlays.

### 3.3 One signal colour

Petrol teal means **interactive or live**. It is used for links, focus, selected navigation, live indicators, and “allowed” data. It is not a decorative brand wash.

**Implication:** primary buttons use ink, not teal. Success, warning, danger, and info colours are state-only.

### 3.4 Precision over ornament

Use a 4px spacing rhythm, tabular numerals, monospace for technical values, and honest status text. Alignment should look deliberate at every breakpoint.

**Implication:** no arbitrary 13px gaps, no mixed icon styles, no centred paragraphs of interface copy.

### 3.5 Calm by default

The product should not shout. Primary actions are ink; destructive actions are the only loud buttons. Motion is short and functional. Empty space is allowed to remain empty.

**Implication:** one primary action per view, one accent signal per region, no animated counters.

### 3.6 Transparent about state

Never hide whether a proxy is online, whether a configuration is applied, whether a stream is live, or whether data is stale. A truthful “pending” is better than a fake “done”.

**Implication:** every asynchronous action has a visible state, and every failure explains what happens next.

---

## 4. Visual direction

### 4.1 “Quiet Signal”

Proxyma looks like a carefully made instrument panel, not a SaaS landing page.

- **Canvas:** warm paper in light mode, near-black in dark mode.
- **Type:** ink, compact, high-contrast, with a clear hierarchy.
- **Structure:** precise 1px rules, quiet surfaces, controlled radii.
- **Colour:** one petrol-teal signal, plus semantic state colours.
- **Density:** comfortable but efficient; tables and logs are allowed to be dense.
- **Motion:** short, purposeful, and optional.

### 4.2 Why this direction

**Warm paper, not cool white.** It makes a developer tool feel less clinical without becoming beige or decorative. It also gives light mode an identity beside the near-black dark canvas.

**Petrol teal, not Vue green or enterprise blue.** Teal reads as signal and telemetry rather than “success”. It is calmer than saturated mint and avoids the generic blue networking-platform look. Because primary buttons are ink, teal stays a signal rather than decoration.

**Ink primary actions, not coloured ones.** A page should have one obvious action, but it should feel confident rather than promotional. Destructive actions are the exception: they are red because the consequence is different.

**Borders before shadows.** Borders survive dark mode, print, and high-contrast settings. Shadows imply elevation; most dashboard content is not elevated.

### 4.3 Anti-patterns

**MUST NOT**

- Use gradient hero sections, gradient buttons, or gradient chart fills.
- Use neon colours, glow effects, bloom, or coloured drop shadows.
- Use glassmorphism, frosted panels, or translucent blur.
- Use 16–24px rounded cards everywhere.
- Use decorative illustrations, fake dashboards, fake customer logos, or animated counters.
- Use purple “AI SaaS” gradients.
- Use pill-shaped buttons, badges, or inputs. Pills are reserved for counts and status dots.
- Use emoji as interface icons.
- Use stock photography inside the product.
- Use more than one accent hue for interaction.

### 4.4 What good looks like

A well-built Proxyma screen has:

- One page title, one short description, one primary action.
- A status band that answers “is my proxy working?” before the user asks.
- Real numbers with units and a time context.
- Tables that align numbers right and paths in monospace.
- A visible focus ring and a keyboard path through every action.
- Enough whitespace to separate sections, not enough to make the page feel empty.

---

## 5. Design tokens

### 5.1 Token model

Proxyma uses a two-layer CSS custom property system:

1. **Primitives** describe raw values and are never used directly in components. Examples: `--px-paper-50`, `--px-ink-900`, `--px-teal-700`.
2. **Semantic tokens** describe purpose and are the only tokens components may use. Examples: `--px-color-canvas`, `--px-color-text-secondary`, `--px-color-border-subtle`.

**MUST**

- Put every token in `src/assets/tokens.css` (proposed file).
- Use `--px-` as the namespace for all Proxyma tokens.
- Use the naming pattern `--px-{category}-{role}-{state}`.
- Never use a raw hex value, `rgb()`, or `hsl()` outside `tokens.css`.
- Never use a primitive token in a component.
- Define light values on `:root` and dark values on `:root[data-theme='dark']`.

**Token categories**

| Prefix | Purpose | Example |
|---|---|---|
| `--px-color-*` | Semantic colour | `--px-color-text-primary` |
| `--px-chart-*` | Data visualisation colour | `--px-chart-allowed` |
| `--px-space-*` | Spacing scale | `--px-space-4` |
| `--px-radius-*` | Corner radius | `--px-radius-card` |
| `--px-shadow-*` | Elevation | `--px-shadow-dialog` |
| `--px-border-*` | Border widths | `--px-border-width-strong` |
| `--px-font-*` | Families, sizes, weights | `--px-font-size-body` |
| `--px-line-*` | Line heights | `--px-line-body` |
| `--px-tracking-*` | Letter spacing | `--px-tracking-heading` |
| `--px-duration-*` | Motion durations | `--px-duration-fast` |
| `--px-ease-*` | Motion curves | `--px-ease-out` |
| `--px-z-*` | Stacking order | `--px-z-dialog` |

### 5.2 Approved dependencies

The frontend already includes these packages. They are approved; adding another requires a decision record.

| Package | Purpose | Rule |
|---|---|---|
| `reka-ui` | Accessible headless primitives | Use through Proxyma wrappers in `src/components/ui/`; do not style pages with raw primitives. |
| `@lucide/vue` | Icons | Explicit named imports only. Do not import the full icon set. |
| `@fontsource-variable/inter` | UI typeface | Self-hosted, imported once in `main.ts`. |
| `@fontsource-variable/jetbrains-mono` | Technical typeface | Self-hosted, imported once in `main.ts`. |
| `@unovis/vue` + `@unovis/ts` | Charts | Lazy-load the chart chunk; do not ship it in the initial shell. |
| `vue-i18n` | Translation and locale formatting | All UI strings live in catalogues. Locales `en-GB` and `pt-PT`; default `en-GB`. |
| `@axe-core/playwright` | Accessibility testing (dev) | Run in the existing Playwright suite. |

**Rejected:** Tailwind, UnoCSS, shadcn-vue, PrimeVue, Vuetify, Element Plus, Naive UI, Ant Design Vue, Headless UI, Font Awesome, icon fonts, `lucide-vue-next`, `moment`, `date-fns`, `lodash`, `vue-toastification`, `floating-vue`, `vuedraggable`.

Rationale: Proxyma's identity depends on owning its CSS. A pre-styled kit would require overriding most of its defaults, and a utility framework would create a second token layer. Reka UI supplies behaviour and accessibility without visual opinion.

### 5.3 Colour primitives

**Light**

| Token | Value | Notes |
|---|---|---|
| `--px-paper-0` | `#FFFFFF` | Surface |
| `--px-paper-50` | `#F7F6F3` | Canvas |
| `--px-paper-100` | `#F1EFEA` | Subtle surface |
| `--px-ink-900` | `#17191C` | Primary text |
| `--px-ink-700` | `#555B61` | Secondary text |
| `--px-ink-600` | `#6B7178` | Tertiary text |
| `--px-ink-400` | `#8A8F88` | Strong border, disabled text |
| `--px-line-200` | `#E4E1DA` | Subtle border |
| `--px-teal-700` | `#0E6E75` | Accent |
| `--px-teal-800` | `#0A5A60` | Accent hover |
| `--px-teal-100` | `#E4F2F2` | Accent subtle |
| `--px-green-700` | `#157F3D` | Success |
| `--px-amber-800` | `#8A4B00` | Warning |
| `--px-red-700` | `#B42318` | Danger |
| `--px-blue-700` | `#1D4ED8` | Info |
| `--px-orange-700` | `#C2410C` | Blocked chart series |
| `--px-amber-700` | `#B45309` | Rate-limited chart series |

**Dark**

| Token | Value | Notes |
|---|---|---|
| `--px-night-950` | `#0D0F10` | Canvas |
| `--px-night-900` | `#141719` | Surface |
| `--px-night-850` | `#1A1E20` | Subtle surface |
| `--px-night-800` | `#272B2D` | Subtle border |
| `--px-night-600` | `#6B7170` | Strong border |
| `--px-paper-50` | `#F2F1ED` | Primary text |
| `--px-ink-950` | `#0D0F10` | Text on light surfaces |
| `--px-teal-300` | `#5CC8C0` | Accent |
| `--px-teal-200` | `#7AD8D1` | Accent hover |
| `--px-teal-950` | `#123033` | Accent subtle |
| `--px-green-300` | `#5BCB8A` | Success |
| `--px-amber-300` | `#F0B45A` | Warning |
| `--px-red-300` | `#FF8A7A` | Danger |
| `--px-blue-300` | `#7FB0FF` | Info |
| `--px-orange-300` | `#FF9A62` | Blocked chart series |

### 5.4 Semantic colours

| Token | Light | Dark | Use |
|---|---|---|---|
| `--px-color-canvas` | `#F7F6F3` | `#0D0F10` | Page background |
| `--px-color-surface` | `#FFFFFF` | `#141719` | Cards, tables, panels |
| `--px-color-surface-subtle` | `#F1EFEA` | `#1A1E20` | Hover rows, inset areas |
| `--px-color-surface-inverse` | `#17191C` | `#F2F1ED` | Primary button, inverse panels |
| `--px-color-surface-inverse-hover` | `#2A2D31` | `#FFFFFF` | Primary button hover |
| `--px-color-surface-inverse-active` | `#3A3E43` | `#DCD9D2` | Primary button pressed |
| `--px-color-text-primary` | `#17191C` | `#F2F1ED` | Headings, body |
| `--px-color-text-secondary` | `#555B61` | `#A8ADA9` | Descriptions, labels |
| `--px-color-text-tertiary` | `#6B7178` | `#858B87` | Captions, timestamps |
| `--px-color-text-disabled` | `#8A8F88` | `#6B7170` | Disabled labels |
| `--px-color-text-inverse` | `#FFFFFF` | `#0D0F10` | Text on inverse surfaces |
| `--px-color-border-subtle` | `#E4E1DA` | `#272B2D` | Dividers, card edges |
| `--px-color-border-strong` | `#8A8F88` | `#6B7170` | Input rest border, essential boundaries |
| `--px-color-border-disabled` | `#E4E1DA` | `#272B2D` | Disabled control border |
| `--px-color-accent` | `#0E6E75` | `#5CC8C0` | Links, selected nav, live signal |
| `--px-color-accent-hover` | `#0A5A60` | `#7AD8D1` | Accent hover |
| `--px-color-accent-active` | `#08494E` | `#8EE4DE` | Accent pressed |
| `--px-color-accent-subtle` | `#E4F2F2` | `#123033` | Selected row, active nav |
| `--px-color-accent-contrast` | `#FFFFFF` | `#062022` | Text on accent solid |
| `--px-color-success-fg` | `#157F3D` | `#5BCB8A` | Success text |
| `--px-color-success-bg` | `#EDF8F1` | `#10261A` | Success surface |
| `--px-color-success-border` | `#B7E2C5` | `#1F4A31` | Success border |
| `--px-color-success-solid` | `#157F3D` | `#5BCB8A` | Success dot or solid |
| `--px-color-warning-fg` | `#8A4B00` | `#F0B45A` | Warning text |
| `--px-color-warning-bg` | `#FDF5E8` | `#2A2010` | Warning surface |
| `--px-color-warning-border` | `#EBCF9A` | `#57431F` | Warning border |
| `--px-color-warning-solid` | `#B45309` | `#F0B45A` | Warning dot or solid |
| `--px-color-danger-fg` | `#B42318` | `#FF8A7A` | Danger text |
| `--px-color-danger-bg` | `#FDF0EE` | `#2A1412` | Danger surface |
| `--px-color-danger-border` | `#F0BDB7` | `#5A2925` | Danger border |
| `--px-color-danger-solid` | `#B42318` | `#C93535` | Destructive button |
| `--px-color-danger-contrast` | `#FFFFFF` | `#FFFFFF` | Text on destructive button |
| `--px-color-info-fg` | `#1D4ED8` | `#7FB0FF` | Info text |
| `--px-color-info-bg` | `#EEF3FE` | `#111C33` | Info surface |
| `--px-color-info-border` | `#C3D4FA` | `#233A66` | Info border |
| `--px-color-info-solid` | `#1D4ED8` | `#7FB0FF` | Info dot or solid |
| `--px-color-focus` | `#0E6E75` | `#5CC8C0` | Focus ring |
| `--px-color-overlay` | `rgba(10, 12, 13, 0.5)` | `rgba(0, 0, 0, 0.7)` | Modal backdrop |
| `--px-color-selection` | `#D5EBEA` | `#1C4447` | Text selection |
| `--px-color-skeleton` | `#EDEAE4` | `#1D2224` | Loading skeleton |
| `--px-color-live` | `#0E6E75` | `#5CC8C0` | Live indicator |

Semantic `-solid` tokens are for dots, icons, and 2px indicators. Only `--px-color-danger-solid` is approved as a filled button surface; the other solid tones do not carry a guaranteed text contrast pair.

**State overlays**

| Token | Light | Dark |
|---|---|---|
| `--px-state-hover` | `rgba(23, 25, 28, 0.04)` | `rgba(242, 241, 237, 0.06)` |
| `--px-state-active` | `rgba(23, 25, 28, 0.08)` | `rgba(242, 241, 237, 0.10)` |
| `--px-state-selected` | `rgba(14, 110, 117, 0.08)` | `rgba(92, 200, 192, 0.12)` |
| `--px-state-disabled` | `0.45` | `0.45` |
| `--px-state-disabled-text` | `0.55` | `0.55` |

Disabled opacity is `0.45` for controls and `0.55` for text. Disabled is never communicated by colour alone; also set `disabled`, `aria-disabled="true"`, and `cursor: not-allowed`.

### 5.5 Contrast

Measured against the canvas unless stated. All values are WCAG 2.x contrast ratios.

| Pair | Light | Dark | Requirement |
|---|---:|---:|---|
| Primary text | 16.30:1 | 17.00:1 | 4.5:1 |
| Secondary text | 6.36:1 | 8.43:1 | 4.5:1 |
| Tertiary text | 4.56:1 | 5.53:1 | 4.5:1 |
| Accent text | 5.54:1 | 9.59:1 | 4.5:1 |
| Strong border on canvas | 3.05:1 | 3.86:1 | 3:1 |
| Strong border on surface | 3.30:1 | 3.50:1 | 3:1 |
| White on danger solid | 6.57:1 | 5.19:1 | 4.5:1 |
| White on success solid | 5.08:1 | — | 4.5:1 |
| Ink/paper inverse | 17.61:1 | 17.00:1 | 4.5:1 |

**Rules**

- Body text MUST meet 4.5:1. Large text (24px regular or 18.66px bold) and non-text indicators MUST meet 3:1.
- `--px-color-text-tertiary` is only for captions and metadata, never for body copy.
- Accent text on `--px-color-accent-subtle` is allowed: it measures 5.21:1 light and 7.0:1 dark.
- Never use colour alone to convey status. Pair it with a dot, icon, label, or pattern.

### 5.6 Spacing

| Token | Value | Use |
|---|---:|---|
| `--px-space-0` | `0` | Reset |
| `--px-space-1` | `4px` | Icon gaps, tight label spacing |
| `--px-space-2` | `8px` | Inline groups, compact padding |
| `--px-space-3` | `12px` | Control padding, table cells |
| `--px-space-4` | `16px` | Default gap, mobile page padding |
| `--px-space-5` | `20px` | Card padding |
| `--px-space-6` | `24px` | Section gap, tablet page padding |
| `--px-space-8` | `32px` | Desktop page padding, large section gap |
| `--px-space-10` | `40px` | Section separation |
| `--px-space-12` | `48px` | Page block separation |
| `--px-space-16` | `64px` | Hero vertical rhythm |
| `--px-space-20` | `80px` | Homepage section |
| `--px-space-24` | `96px` | Homepage hero |

A **2px** optical nudge is permitted only for icon alignment, using `transform: translateY(1px)` or `translateX(1px)`, not as a spacing token. Every other gap MUST use the scale.

### 5.7 Radius, border, elevation

| Token | Value | Use |
|---|---:|---|
| `--px-radius-none` | `0` | Tables, full-bleed sections |
| `--px-radius-badge` | `4px` | Badges, tags |
| `--px-radius-control` | `6px` | Buttons, inputs, selects |
| `--px-radius-card` | `8px` | Cards and panels |
| `--px-radius-dialog` | `12px` | Dialogs, sheets |
| `--px-radius-full` | `999px` | Status dots, avatars, count pills only |
| `--px-border-width` | `1px` | Default border |
| `--px-border-width-strong` | `2px` | Focus ring, selected tab indicator |

**Elevation hierarchy**

| Level | Token | Use |
|---|---|---|
| 0 | none | Canvas, surface, cards, tables |
| 1 | `--px-shadow-sm` | Dropdown, popover, select menu |
| 2 | `--px-shadow-md` | Dialog |
| 3 | `--px-shadow-lg` | Toast |
| 4 | `--px-shadow-sm` | Tooltip |

**Shadows**

```css
:root {
  --px-shadow-sm: 0 1px 2px rgba(23, 25, 28, 0.08), 0 4px 12px rgba(23, 25, 28, 0.06);
  --px-shadow-md: 0 8px 24px rgba(23, 25, 28, 0.12), 0 2px 6px rgba(23, 25, 28, 0.06);
  --px-shadow-lg: 0 16px 48px rgba(23, 25, 28, 0.16), 0 4px 12px rgba(23, 25, 28, 0.08);
}

:root[data-theme='dark'] {
  --px-shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.4), 0 4px 12px rgba(0, 0, 0, 0.3);
  --px-shadow-md: 0 8px 24px rgba(0, 0, 0, 0.5), 0 2px 6px rgba(0, 0, 0, 0.35);
  --px-shadow-lg: 0 16px 48px rgba(0, 0, 0, 0.6), 0 4px 12px rgba(0, 0, 0, 0.4);
}
```

**Rules**

- Cards MUST NOT have a shadow by default.
- Sticky headers use a bottom border, not a shadow.
- A dropdown, dialog, or toast MUST have a shadow appropriate to its level.
- Do not use coloured shadows.

### 5.8 Focus

```css
:root {
  --px-focus-width: 2px;
  --px-focus-offset: 2px;
  --px-focus-ring: 0 0 0 var(--px-focus-offset) var(--px-color-canvas),
    0 0 0 calc(var(--px-focus-offset) + var(--px-focus-width)) var(--px-color-focus);
}
```

**MUST**

- Apply focus styles with `:focus-visible` only.
- Keep a visible focus indicator on every interactive element, including links, rows, tabs, chart points, and custom controls.
- Never use `outline: none` without an equivalent visible replacement.
- On dark surfaces, use the dark focus token, which is brighter.
- The focus ring must have at least 3:1 contrast against adjacent colours.

### 5.9 Motion tokens

| Token | Value | Use |
|---|---|---|
| `--px-duration-instant` | `0ms` | State change under reduced motion |
| `--px-duration-fast` | `120ms` | Hover, focus, small state changes |
| `--px-duration-normal` | `180ms` | Dropdown, tooltip, toast |
| `--px-duration-slow` | `240ms` | Dialog, drawer |
| `--px-ease-out` | `cubic-bezier(0.2, 0, 0, 1)` | Entering and moving |
| `--px-ease-in-out` | `cubic-bezier(0.4, 0, 0.2, 1)` | Reversible transitions |

**MUST**

- Wrap all non-essential motion in `@media (prefers-reduced-motion: reduce)` and set it to `0ms`.
- Do not animate layout properties such as `width`, `height`, `top`, or `left` when a transform or opacity will do.
- Do not animate numbers, bars growing on load, or page transitions.

### 5.10 Breakpoints and layout tokens

CSS custom properties cannot be used inside media queries. Media queries use literal values; the tokens below document them.

| Token | Value | Name |
|---|---:|---|
| `--px-breakpoint-sm` | `480px` | Small mobile |
| `--px-breakpoint-md` | `768px` | Tablet |
| `--px-breakpoint-lg` | `1024px` | Desktop |
| `--px-breakpoint-xl` | `1280px` | Wide desktop |
| `--px-breakpoint-2xl` | `1536px` | Ultra-wide |

| Token | Value | Use |
|---|---:|---|
| `--px-sidebar-width` | `232px` | Desktop sidebar |
| `--px-sidebar-rail-width` | `64px` | Tablet icon rail |
| `--px-topbar-height` | `56px` | Top bar |
| `--px-content-max` | `1280px` | Main content maximum |
| `--px-form-max` | `640px` | Form column |
| `--px-prose-max` | `720px` | Long-form copy |
| `--px-auth-card-max` | `400px` | Authentication card |
| `--px-dialog-sm` | `480px` | Confirmation dialog |
| `--px-dialog-md` | `560px` | Standard dialog |
| `--px-dialog-lg` | `720px` | Complex dialog |
| `--px-drawer-width` | `320px` | Navigation and filter drawer |
| `--px-page-padding` | `32px` | Desktop page padding |
| `--px-page-padding-md` | `24px` | Tablet page padding |
| `--px-page-padding-sm` | `16px` | Mobile page padding |

### 5.11 Z-index

| Token | Value | Use |
|---|---:|---|
| `--px-z-base` | `0` | Normal flow |
| `--px-z-sticky` | `100` | Sticky table header, sticky save bar |
| `--px-z-dropdown` | `200` | Menus, popovers, selects |
| `--px-z-overlay` | `300` | Modal and drawer backdrop |
| `--px-z-dialog` | `400` | Dialog and drawer |
| `--px-z-toast` | `500` | Toasts |
| `--px-z-tooltip` | `600` | Tooltips |
| `--px-z-skip-link` | `700` | Skip link |

Never invent a z-index outside this scale.

---

## 6. Typography

### 6.1 Families

```css
:root {
  --px-font-sans: 'Inter Variable', ui-sans-serif, system-ui, -apple-system,
    BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  --px-font-mono: 'JetBrains Mono Variable', ui-monospace, 'SFMono-Regular',
    'Cascadia Code', 'Roboto Mono', Menlo, Consolas, monospace;
}
```

**Loading**

Import the variable weight files once, in `src/main.ts`, before the app styles:

```ts
import '@fontsource-variable/inter/wght.css'
import '@fontsource-variable/jetbrains-mono/wght.css'
import './assets/tokens.css'
import './assets/base.css'
import './assets/main.css'
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { i18n } from './i18n'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(i18n)

app.mount('#app')
```

**MUST**

- Self-host both fonts. Do not call Google Fonts or any external font domain.
- Keep the fallback stacks. A missing font must degrade to a readable system font.
- Use Inter for all UI text and JetBrains Mono only for technical values.
- Do not use synthetic bold. Load the variable weight axis and use 400, 500, or 600.
- Do not use more than three weights on one screen.

**SHOULD**

- Fontsource emits `unicode-range` rules, so the browser only downloads the subset it needs. Do not ship a separate Latin-only CSS file unless the build process requires it.
- Set `text-rendering: optimizeLegibility` on the root.
- Use `font-variant-numeric: tabular-nums` for tables, metrics, timestamps, and counters.
- Disable ligatures in monospace where a path or IP could become ambiguous. JetBrains Mono is designed for code; keep default ligatures off for technical values.

### 6.2 Scale

| Token | Size | Weight | Line height | Tracking | Use |
|---|---:|---:|---:|---:|---|
| `--px-font-size-display` | `48px` | 600 | 1.05 | -0.03em | Homepage hero, large auth moment |
| `--px-font-size-h1` | `32px` | 600 | 1.15 | -0.02em | Page title |
| `--px-font-size-h2` | `24px` | 600 | 1.25 | -0.015em | Section title |
| `--px-font-size-h3` | `18px` | 600 | 1.35 | -0.01em | Card or panel title |
| `--px-font-size-h4` | `15px` | 600 | 1.4 | 0 | Subsection, field group |
| `--px-font-size-body-lg` | `17px` | 400 | 1.55 | 0 | Auth description, homepage body |
| `--px-font-size-body` | `15px` | 400 | 1.6 | 0 | Default UI text |
| `--px-font-size-body-strong` | `15px` | 500 | 1.6 | 0 | Emphasis, labels |
| `--px-font-size-control` | `14px` | 500 | 1.4 | 0 | Buttons, menu items |
| `--px-font-size-input` | `14px` | 400 | 1.4 | 0 | Text inputs, selects, textareas |
| `--px-font-size-small` | `13px` | 400 | 1.5 | 0 | Table cells, help text |
| `--px-font-size-small-strong` | `13px` | 500 | 1.5 | 0 | Table headers, field labels |
| `--px-font-size-caption` | `12px` | 400 | 1.4 | 0 | Timestamps, metadata |
| `--px-font-size-mono` | `13px` | 400 | 1.5 | 0 | Paths, IPs, versions |
| `--px-font-size-mono-sm` | `12px` | 400 | 1.5 | 0 | Dense tables, code chips |
| `--px-font-size-metric` | `28px` | 600 | 1.15 | -0.02em | Metric card value |

**Line-height tokens:** `--px-line-display: 1.05`, `--px-line-heading: 1.2`, `--px-line-title: 1.3`, `--px-line-body: 1.6`, `--px-line-control: 1.4`, `--px-line-compact: 1.5`.

### 6.3 Rules

**MUST**

- Use sentence case for headings, buttons, labels, menu items, and table headers. The only uppercase text permitted is a short group label in a dense table, with `letter-spacing: 0.06em` and 12px size.
- Use second person: “your proxy”, “you”.
- Use present tense for state: “Waiting for your proxy”, not “Your proxy will be waiting”.
- Keep body copy at 15px minimum. 13px is for supporting text; 12px is for captions.
- Keep button and input labels at 14px on desktop.
- Use 16px input text at widths below 768px to prevent iOS zoom.
- Keep headings at 600. Do not use 700 or 800.
- Do not use italics for interface text. Use weight or colour for emphasis.
- Do not centre paragraphs longer than two lines.

**SHOULD**

- Limit body line length to 65–75 characters (`--px-prose-max`).
- Use `text-wrap: balance` on headings of three lines or fewer, and `text-wrap: pretty` on short descriptions. Both must degrade gracefully.
- Break long technical strings with `overflow-wrap: anywhere` inside their container, not by shrinking the font.

### 6.4 Technical text

Use `--px-font-mono` for:

- IP addresses and CIDR ranges.
- URL paths, hostnames, upstreams, and route prefixes.
- HTTP methods and status codes when shown as values.
- Config versions (`v7`), request IDs, log IDs, and invite tokens.
- Durations when they are data (`18 ms`), but not when they are prose (“this may take a few seconds”).
- Code blocks and JSON.

Use tabular numerals for:

- Metric values.
- Table numeric columns.
- Timestamps.
- Latency and request counts.
- Pagination counts.

### 6.5 Translation readiness

**MUST**

- Default locale: `en-GB`. Shipped locales: `en-GB` and `pt-PT`.
- Every user-facing string comes from a catalogue in `src/i18n/locales/`. No hardcoded English in templates.
- Use interpolation and pluralisation, never string concatenation.
- Allow 30–40% text expansion in buttons, tabs, table headers, and navigation.
- Do not fix widths on text containers unless truncation with a tooltip is deliberate.
- Set `document.documentElement.lang` from the active locale.
- Use `Intl.DateTimeFormat`, `Intl.NumberFormat`, and `Intl.RelativeTimeFormat` with the active locale.
- Detect the locale from an explicit stored choice, then `navigator.languages`, then `en-GB`.
- Keep the language switcher hidden while only one locale is registered. Show it in public, auth, and app chrome once more than one catalogue ships.

Namespaced message keys:

```ts
// src/i18n/locales/en-GB.ts (proposed)
export default {
  nav: {
    overview: 'Overview',
    routes: 'Routes',
    traffic: 'Traffic',
    bans: 'Bans',
    team: 'Team',
    audit: 'Audit',
    settings: 'Settings',
    admin: 'Admin',
  },
  auth: {
    signIn: {
      title: 'Sign in',
      submit: 'Sign in',
    },
  },
  config: {
    status: {
      applied: 'Applied',
      pending: 'Pending',
      rejected: 'Rejected',
    },
  },
  errors: {
    EMAIL_TAKEN: 'An account with this email already exists.',
  },
}
```

---

## 7. Layout and spacing

### 7.1 Grid

- **Desktop (≥1024px):** 12 columns, 24px gutter, 32px page padding.
- **Tablet (768–1023px):** 8 columns, 16px gutter, 24px page padding.
- **Mobile (<768px):** 4 columns, 16px gutter, 16px page padding.
- Use CSS Grid for page-level structure and flexbox for inline alignment. Do not build a global utility-class system.
- Content maximum width: `1280px`, centred inside the main area.
- Full-bleed elements (tables, chart panels) may extend to the content edge, never beyond the page padding.

### 7.2 Application shell

```text
┌──────────────┬─────────────────────────────────────────────┐
│ Wordmark     │ Top bar: breadcrumb · live status · account │ 56px
│ Org switcher ├─────────────────────────────────────────────┤
│              │                                             │
│ Navigation   │ Page header: title, description, actions    │
│              │                                             │
│              │ Content                                     │
│              │                                             │
│ Account      │                                             │
│ Admin        │                                             │
└──────────────┴─────────────────────────────────────────────┘
   232px
```

| Region | Specification |
|---|---|
| Sidebar | 232px wide, full height, `--px-color-canvas`, right border `--px-color-border-subtle`, 16px inner padding |
| Sidebar header | Wordmark at top, 20px high, 16px padding; org switcher below with 8px gap. Do not repeat the page-context marker here; the top bar carries it |
| Navigation item | 36px high, 12px horizontal padding, 6px radius, 16px icon + 10px gap, 14px/500 label |
| Active navigation | `--px-color-accent-subtle` background, `--px-color-accent` text and icon, 2px left indicator |
| Hover navigation | `--px-color-surface-subtle` background, primary text |
| Sidebar footer | Settings link, then Sign out; preferences live in Settings → General. Separated by a 1px rule. Sign out reuses the navigation item footprint at ghost emphasis; do not shrink it below 36px or a 14px label |
| Admin section | Below a 1px rule at the bottom, visually separated, shield icon and section label, only when `is_platform_admin` is true |
| Top bar | 56px high, sticky, `--px-color-canvas`, bottom border, 16–24px horizontal padding |
| Content | `--px-content-max`, 32px top padding, 48px bottom padding |
| Page header | Title, one-line description, actions right-aligned on desktop; 24px bottom margin |

**Navigation order**

1. Overview
2. Routes
3. Traffic
4. Bans
5. Team
6. Audit
7. Settings (sidebar footer, above Sign out)
8. Admin (separate section, bottom of the body, platform admins only)

Until organisation and proxy surfaces exist, the sidebar shows only **Settings**. Settings contains Account, General, and Security; Account is a section within Settings rather than a competing top-level item. Add an Organisations section when organisation surfaces exist.

Settings sits in the sidebar footer directly above Sign out. The primary navigation and the platform-administration section stay in the body.

**Icons:** `LayoutDashboard`, `Route`, `Activity`, `ShieldBan`, `Users`, `ScrollText`, `Settings`, `ShieldCheck`.

**MUST**

- Keep the navigation order stable. Do not add a top-level item without updating this guide.
- Show the org switcher even when the user has one organisation; it establishes context and scales.
- Keep preferences in Settings → General. Do not duplicate theme or language controls in the sidebar footer.
- Never hide the Admin entry from an admin on mobile.

### 7.3 Page templates

**Standard page**

```text
Page title                                      [Secondary] [Primary]
Short description

[Optional status band or filter bar]

[Content]
```

**Data page**

```text
Page title                                      [Refresh] [Export?]
Description · last updated · timezone

[Filter bar]

[Chart or table panel]

[Pagination or load more]
```

**Settings page**

```text
Settings title
Description

[Section navigation]   [Form column 640px]
```

**Authentication page**

```text
                  proxyma
            Sign in to Proxyma
        [Email]
        [Password]
        [Sign in]
     Forgot password? · Create an account
```

**Homepage**

See [section 9.12](#912-public-homepage).

### 7.4 Vertical rhythm

- Page title to description: 8px.
- Description to content: 24px.
- Section to section: 32px.
- Card title to body: 12px.
- Form field to field: 16px.
- Form group to group: 24px.
- Table row height: 44px standard, 36px compact (logs only).
- Panel padding: 20px mobile, 24px desktop.

### 7.5 Sidebar and top bar behaviour

- **≥1024px:** persistent 232px sidebar. Top bar spans the main column only.
- **768–1023px:** 64px icon rail, labels hidden, tooltips on hover and focus. Main content starts after the rail.
- **<768px:** sidebar becomes a drawer opened from the top bar. Drawer width `min(320px, 88vw)`, full height, backdrop, focus trap, Escape closes, focus returns to the trigger.
- The top bar remains sticky at every size.
- The org switcher moves into the drawer header on mobile.
- Page actions remain visible; if there are more than two, the secondary actions collapse into an overflow menu.

### 7.6 Forms

- Form column maximum: 640px.
- Labels above inputs, never placeholder-only.
- One column on all sizes except short paired fields (for example city and postcode), which may use two columns at ≥768px.
- Primary action left-aligned under the form on mobile; right-aligned on desktop.
- Destructive actions separated from the primary action by at least 24px and a 1px rule.
- Sticky save bar when a form has unsaved changes: full content width, top border, surface background, 12px vertical padding, actions right-aligned.

### 7.7 Empty, loading, and error layout

- Empty state occupies the content panel, minimum 240px high, centred content, one action.
- Loading skeletons mirror the final layout. Do not replace the whole page with a spinner.
- Error states occupy the same footprint as the content they replace and always offer a retry or next step.
- Do not collapse the page layout when data is missing; keep the header and filters stable.

---

## 8. Components

### 8.1 Component rules

**MUST**

- Build shared primitives in `src/components/ui/` (proposed) with a `Px` prefix: `PxButton`, `PxInput`, `PxSelect`, `PxDialog`, `PxTable`.
- Use Reka UI for behaviour that native HTML cannot provide accessibly: dialogs, dropdown menus, comboboxes, tooltips, toasts, switches, tabs, and popovers.
- Keep styling in the wrapper. Pages must not import Reka primitives directly unless the wrapper cannot express the required pattern, and then the reason is documented.
- Keep classes BEM-like: `.px-button`, `.px-button--primary`, `.px-button--sm`, `.px-button__icon`.
- Express state with native attributes where possible: `disabled`, `aria-invalid="true"`, `aria-busy="true"`, `data-state="open"`, `aria-selected="true"`.
- Every component must work in light and dark mode using semantic tokens only.
- Every interactive component must have a visible `:focus-visible` state.

**SHOULD**

- Keep one component per file in `src/components/ui/`.
- Use `<script setup lang="ts">` and typed props.
- Use slots for content rather than long prop lists.
- Document any component with more than three variants in a short comment block at the top of the file.

**Component inventory**

| Component | Wrapper | Reka primitive | Priority |
|---|---|---|---|
| Button | `PxButton` | — | Now |
| Icon button | `PxIconButton` | — | Now |
| Input | `PxInput` | — | Now |
| Textarea | `PxTextarea` | — | Now |
| Select | `PxSelect` | `SelectRoot` when needed | Now |
| Checkbox | `PxCheckbox` | `CheckboxRoot` | Now |
| Radio group | `PxRadioGroup` | `RadioGroupRoot` | Now |
| Switch | `PxSwitch` | `SwitchRoot` | Now |
| Card | `PxCard` | — | Now |
| Dialog | `PxDialog` | `DialogRoot` | Now |
| Dropdown menu | `PxDropdownMenu` | `DropdownMenuRoot` | Now |
| Tabs | `PxTabs` | `TabsRoot` | Now |
| Table | `PxTable` | — | Now |
| Badge | `PxBadge` | — | Now |
| Tooltip | `PxTooltip` | `TooltipRoot` | Now |
| Toast | `PxToast` | `ToastRoot` | Now |
| Empty state | `PxEmptyState` | — | Now |
| Skeleton | `PxSkeleton` | — | Now |
| Pagination | `PxPagination` | — | Later |
| Combobox | `PxCombobox` | `ComboboxRoot` | Later |
| Command palette | — | — | Out of scope |

### 8.2 Buttons

**Purpose:** trigger an action or navigate to a closely related task.

**Variants**

| Variant | Appearance | Use |
|---|---|---|
| Primary | Inverse surface, inverse text | The single main action on a view |
| Secondary | Surface, strong border, primary text | Alternative action, cancel, filters |
| Ghost | Transparent, secondary text, subtle hover surface | Toolbar actions, low-emphasis actions |
| Danger | Danger solid, white text | Destructive confirmation only |
| Link | Accent text, underline on hover | Navigation inside prose |

**Sizes**

| Size | Height | Horizontal padding | Font | Icon | Use |
|---|---:|---:|---:|---:|---|
| `sm` | 28px | 10px | 13px/500 | 14px | Table toolbars, dense actions |
| `md` | 36px | 14px | 14px/500 | 16px | Default |
| `lg` | 44px | 18px | 15px/500 | 20px | Auth, homepage, mobile primary |

**States**

| State | Treatment |
|---|---|
| Default | Variant colours, radius 6px |
| Hover | One step darker/lighter; 120ms; no lift |
| Active | Pressed colour; no scale |
| Focus-visible | 2px accent ring, 2px offset |
| Disabled | 45% opacity, `cursor: not-allowed`, no hover |
| Loading | Spinner replaces the leading icon; label remains; `aria-busy="true"`; disabled |
| Full width | Only on mobile or in auth cards |

**Anatomy**

```vue
<template>
  <button class="px-button px-button--primary" type="button">
    <Plus class="px-button__icon" :size="16" aria-hidden="true" />
    <span>Add route</span>
  </button>
</template>
```

```css
.px-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--px-space-2);
  min-height: 36px;
  padding: 0 14px;
  border: var(--px-border-width) solid transparent;
  border-radius: var(--px-radius-control);
  font-family: var(--px-font-sans);
  font-size: var(--px-font-size-control);
  font-weight: 500;
  line-height: var(--px-line-control);
  cursor: pointer;
  transition:
    background-color var(--px-duration-fast) var(--px-ease-out),
    border-color var(--px-duration-fast) var(--px-ease-out),
    color var(--px-duration-fast) var(--px-ease-out);
}

.px-button--primary {
  background: var(--px-color-surface-inverse);
  color: var(--px-color-text-inverse);
}

.px-button--primary:hover {
  background: var(--px-color-surface-inverse-hover);
}

.px-button:focus-visible {
  outline: none;
  box-shadow: var(--px-focus-ring);
}

.px-button:disabled {
  opacity: var(--px-state-disabled);
  cursor: not-allowed;
}
```

**MUST**

- One primary button per view or dialog.
- Use verb-first labels: “Save changes”, “Create organisation”, “Ban IP”. Never “Submit”, “OK”, or “Yes”.
- Keep the label visible while loading. Do not replace it with “Loading…”.
- Use `type="button"` unless the button submits a form.
- Put the icon before the label by default; use a trailing icon only for “next” or external navigation.
- Do not use icon-only buttons for destructive or primary actions without a tooltip and an accessible name.

**Icon buttons**

- Sizes 28, 36, 44px square. Icon 14, 16, 20px.
- Must have `aria-label` and a tooltip.
- Use `PxIconButton` for row actions, close buttons, and toolbar actions.

### 8.3 Text inputs and textareas

**Purpose:** collect short text, secrets, numbers, or longer notes.

**Anatomy**

```text
Label
[Input]
Help text
Error text (when invalid)
```

**Specification**

| Property | Value |
|---|---|
| Height | 36px desktop, 44px mobile |
| Padding | 0 12px |
| Border | 1px `--px-color-border-strong` |
| Background | `--px-color-surface` |
| Radius | `--px-radius-control` |
| Font | 14px/400 desktop (`--px-font-size-input`), 16px/400 mobile |
| Placeholder | `--px-color-text-tertiary` |
| Label | 13px/500, 6px gap |
| Help text | 12px/400, 6px gap |
| Error text | 12px/500, danger foreground, 6px gap |
| Textarea min height | 96px, resize vertical only |

**States**

| State | Treatment |
|---|---|
| Default | Strong border |
| Hover | Border stays strong; no colour change needed |
| Focus | Accent border + focus ring; no layout shift |
| Filled | Primary text |
| Disabled | Subtle border, disabled text, subtle surface |
| Read-only | Subtle surface, primary text, selectable |
| Invalid | Danger border, danger help text, `aria-invalid="true"` |
| Loading | Not applicable; use a disabled state while saving |

**MUST**

- Always render a visible `<label for="...">`. Placeholder is never a label.
- Connect help and error text with `aria-describedby`.
- Keep the user's input after a validation or server error.
- Use `autocomplete` attributes correctly: `email`, `current-password`, `new-password`, `one-time-code`, `organization`.
- Use `inputmode="numeric"` for numeric fields where appropriate.
- Use `type="password"` with a show/hide control that has an accessible name.
- Do not use a text input for a boolean or a small fixed set of options.

**Example**

```vue
<template>
  <div class="px-field" :data-invalid="Boolean(error)">
    <label class="px-field__label" for="route-name">Route name</label>
    <input
      id="route-name"
      v-model="name"
      class="px-input"
      type="text"
      autocomplete="off"
      :aria-invalid="Boolean(error)"
      :aria-describedby="error ? 'route-name-error' : 'route-name-help'"
    />
    <p v-if="error" id="route-name-error" class="px-field__error">
      {{ error }}
    </p>
    <p v-else id="route-name-help" class="px-field__help">
      Shown in logs and the route list.
    </p>
  </div>
</template>
```

### 8.4 Selects

**Purpose:** choose one option from a known set.

**MUST**

- Use a native `<select>` when the list is short, flat, and does not need search. Style it with `appearance: none` and a Lucide chevron.
- Use `PxSelect` (Reka `SelectRoot`) only when the design needs grouping, descriptions, or a custom trigger. It must support keyboard navigation, type-ahead, and Escape.
- Never use a div-based dropdown as a form control.
- Always provide a visible label. A placeholder option is allowed only when “no selection” is valid.
- Match input height, border, radius, and focus treatment.
- The chevron is 16px, `--px-color-text-tertiary`, and decorative.

**Native select example**

```vue
<template>
  <div class="px-field">
    <label class="px-field__label" for="role">Role</label>
    <div class="px-select">
      <select id="role" v-model="role" class="px-select__control">
        <option value="member">Member</option>
        <option value="co_owner">Co-owner</option>
      </select>
      <ChevronDown class="px-select__icon" :size="16" aria-hidden="true" />
    </div>
  </div>
</template>
```

### 8.5 Checkboxes and radio groups

- Control size: 16×16px. Radius 4px for checkbox; circle for radio.
- Hit target: at least 44×44px, achieved with padding or a wrapping label.
- Label to the right, 13–15px, clickable.
- Use a native input visually hidden with a styled indicator, or Reka Checkbox/RadioGroup. Either way, keyboard and screen-reader behaviour must match native.
- Checked state: accent solid with a white check or dot. Indeterminate state: a horizontal dash.
- Do not use a checkbox as a switch. Use a switch for immediate on/off settings.
- Radios are for mutually exclusive options when the set is small (2–5). Use a select above five.

### 8.6 Switches

- Track 36×20px, thumb 16px, radius full. Accent track when on, strong border when off.
- Label to the left or right, never inside the track.
- The entire row is clickable; the target is at least 44px high.
- Immediate effect only when the action is safe and reversible. If the change is destructive, asynchronous, or needs a save, use a checkbox or a form action instead.
- If saving is required, show a “Save changes” action and do not imply the change is live.
- Announce the new state with text, not colour alone.

### 8.7 Cards and panels

**Purpose:** group related content when a border or surface improves scanability. Not every section needs one.

- Background: `--px-color-surface`.
- Border: 1px `--px-color-border-subtle`.
- Radius: 8px.
- Padding: 24px desktop, 20px mobile.
- Shadow: none.
- Header: title 15px/600 and optional description 13px/400, 12px gap to content.
- Footer actions: 1px top border, 16px top padding.
- Clickable card: hover background `--px-color-surface-subtle`, border `--px-color-border-strong`; no lift or scale.
- A card must have a heading or an accessible name. Do not use cards purely as decoration.

**Panel vs card:** use a panel for a full-width chart or table region (border, radius, no shadow). Use a card for a smaller grouped unit inside a grid. Do not nest cards more than one level.

### 8.8 Dialogs and sheets

**Purpose:** confirm a destructive action, collect a short focused input, or show details that would disrupt the page.

**Sizes:** 480px (confirmation), 560px (standard), 720px (complex). On screens below 480px, dialogs become full-screen sheets.

- Backdrop: `--px-color-overlay`.
- Surface: `--px-color-surface`.
- Radius: 12px desktop, 0 mobile full-screen.
- Padding: 24px.
- Title: 18px/600. Description: 14px/400, secondary text.
- Actions: right-aligned on desktop, stacked full-width on mobile, primary last in DOM order.
- Close button: 36px icon button, top-right, accessible label “Close”.
- Focus: move to the first meaningful control, trap within the dialog, restore to the trigger on close.
- Escape closes unless the action is in progress. Do not close while a destructive request is pending.
- Scroll lock on the body.
- Use `PxDialog`, which wraps Reka `DialogRoot`.

**Destructive confirmation**

- State the object and consequence in the title: “Delete Acme?”
- Explain what disappears and whether it can be undone.
- Require typing the resource name for organisation deletion, ownership changes, and other irreversible actions.
- Primary button label repeats the verb: “Delete organisation”, not “Confirm”.

**Example copy**

> **Delete Acme?**  
> This stops the proxy for everyone in this organisation and removes its routes, members, and history. This can't be undone.  
> Type **Acme** to confirm.  
> [Cancel] [Delete organisation]

### 8.9 Dropdown menus

- Trigger: icon button or secondary button with a chevron.
- Surface: `--px-color-surface`, 1px subtle border, 6px radius, `--px-shadow-sm`.
- Inner padding: 4px. Item height: 32px, 8px horizontal padding, 6px radius.
- Item text: 13–14px, primary. Icon 16px, secondary.
- Hover/focus: `--px-color-surface-subtle`.
- Destructive item: danger text, separated by a 1px rule.
- Keyboard: Arrow Up/Down, Home/End, Enter/Space, Escape, type-ahead.
- Checkable items show a check, not a coloured background alone.
- Use `PxDropdownMenu` (Reka `DropdownMenuRoot`).
- Do not nest submenus more than one level. Prefer a dialog or a dedicated page.

### 8.10 Navigation

- Sidebar navigation is the primary navigation. Top bar breadcrumbs are secondary and never duplicate the sidebar label.
- Active item: accent-subtle background, accent text, 2px left indicator. Do not use a filled accent block.
- Hover item: subtle surface. Focus: focus ring.
- Navigation labels are 14px/500, sentence case.
- Icons are 16px, 1.75px stroke, `currentColor`.
- Counts (for example active bans) appear as a small neutral count pill on the right. Do not use red badges for counts unless the count is an error state.
- The org switcher shows the current organisation name, role, and proxy state. It opens a menu with a search field when there are more than five organisations, plus “Create organisation”.
- Breadcrumbs are 13px, secondary text, separated by `/`; the last item is current and not a link.

### 8.11 Tabs

- Underline style, not pills.
- Tab label: 14px/500, 12px horizontal padding, 10px vertical padding.
- Active: primary text, 2px accent bottom border.
- Inactive: secondary text; hover primary text and subtle surface.
- Focus: focus ring inset or around the label.
- Keyboard: Left/Right arrows, Home/End. `role="tablist"`, `role="tab"`, `aria-selected`, `aria-controls`.
- Use tabs for views of the same object (Live/History, Routes/Rate limits/Detection). Do not use tabs for sequential steps.
- On mobile, tabs scroll horizontally with a fade edge; do not wrap to multiple lines.

### 8.12 Tables

**Purpose:** compare structured data, especially logs, members, bans, and routes.

**Specification**

| Property | Value |
|---|---|
| Header | 12px/500, secondary text, subtle bottom border, sticky at top of scroll region |
| Cell | 13px/400, 12px vertical padding, 16px horizontal padding |
| Row height | 44px standard, 36px compact for logs |
| Divider | 1px `--px-color-border-subtle` |
| Hover | Subtle surface |
| Selected | Accent-subtle background, accent left indicator |
| Numeric column | Right-aligned, tabular numerals |
| Technical column | Monospace 12–13px |
| Empty | Empty state inside the table body, not a blank panel |

**MUST**

- Use a real `<table>` with `<caption>`, `<thead>`, `<tbody>`, and `scope="col"` on headers.
- Use `aria-sort="ascending|descending|none"` on sortable headers.
- Keep the header visible when the table scrolls.
- Provide a row action menu with an accessible name that includes the row identity.
- Never make the entire row a link if it contains other interactive controls. Use a dedicated link in the first cell or a row action.
- Truncate long paths with a middle ellipsis and a copy action. Do not wrap a path across three lines.
- Use skeleton rows for loading, not a full-table spinner.

**Responsive**

- **≥768px:** full table with horizontal scroll if needed. Freeze the first column for wide admin tables.
- **<768px:** convert rows to stacked definition cards. Each card shows the primary value first, then labelled metadata, then actions. Do not simply shrink the table.
- **Logs exception:** a dense log table may horizontally scroll on mobile if the alternative loses the time sequence. Provide a “Details” action for each row.

**Example**

```vue
<template>
  <table class="px-table">
    <caption class="px-table__caption">
      Recent requests
    </caption>
    <thead>
      <tr>
        <th scope="col">Time</th>
        <th scope="col">Method</th>
        <th scope="col">Path</th>
        <th scope="col" class="px-table__numeric">Status</th>
        <th scope="col" class="px-table__numeric">Latency</th>
        <th scope="col">Action</th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="entry in entries" :key="entry.id">
        <td>{{ formatTime(entry.timestamp) }}</td>
        <td class="px-table__mono">{{ entry.method }}</td>
        <td class="px-table__mono px-table__path">{{ entry.path }}</td>
        <td class="px-table__numeric">{{ entry.status }}</td>
        <td class="px-table__numeric">{{ formatLatency(entry.latency_ms) }}</td>
        <td><PxBadge :tone="actionTone(entry.action)">{{ entry.action }}</PxBadge></td>
      </tr>
    </tbody>
  </table>
</template>
```

### 8.13 Badges and status indicators

- Height 22px, radius 4px, padding 0 6px, 12px/500.
- Variants: neutral, accent, success, warning, danger, info.
- Optional 6px leading dot. The dot is mandatory for proxy, config, and live states.
- Text is always present. Colour alone is never sufficient.
- Use a count pill (`--px-radius-full`) only for numeric counts, never for status.

**Status vocabulary**

| State | Tone | Text |
|---|---|---|
| `ONLINE` | Success | Online |
| `STARTING` | Warning | Starting |
| `CREATING` | Neutral/warning | Creating |
| `OFFLINE` | Danger | Offline |
| `DELETING` | Neutral | Deleting |
| Config `applied` | Success | Applied |
| Config `pending` | Warning | Pending |
| Config `rejected` | Danger | Rejected |
| Live connected | Accent | Live |
| Live reconnecting | Warning | Reconnecting |

**Example**

```vue
<template>
  <span class="px-badge px-badge--success">
    <span class="px-badge__dot" aria-hidden="true" />
    Online
  </span>
</template>
```

### 8.14 Tooltips

- Use for clarification, keyboard shortcuts, and icon-only button names.
- Surface: inverse, text inverse, 12px/400, 6px radius, 6px padding, max-width 280px.
- Show on hover and focus. Hide on Escape.
- Delay: 400ms open, 0ms close.
- Never put essential information or an action inside a tooltip.
- Do not use a tooltip on a disabled button; wrap it in a focusable container or use inline help.
- Use `PxTooltip` (Reka `TooltipRoot`).
- Touch devices: do not rely on hover. Provide visible text or an info popover.

### 8.15 Toast notifications

- Position: bottom-right desktop, top-centre mobile.
- Maximum three visible; older toasts stack behind or collapse.
- Width: 320–400px. Padding 12–16px. Radius 8px. Shadow `--px-shadow-lg`.
- A 3px left border carries the tone: success, warning, danger, info.
- Title 13px/600, description 13px/400 secondary.
- Success and info auto-dismiss after 5 seconds. Errors persist until dismissed.
- `role="status"` for success/info; `role="alert"` for errors.
- Pause the timer on hover/focus.
- Include a single action at most: “View”, “Retry”, “Undo”.
- Do not use a toast for field-level validation, destructive confirmation, or anything the user must read before continuing.
- Use `PxToast` (Reka `ToastRoot`).

### 8.16 Empty states

- Container: minimum 240px high, centred, max-width 400px.
- Icon: 24px Lucide line icon, tertiary, `aria-hidden="true"`.
- Title: 15px/600.
- Description: 13px/400 secondary, one or two sentences.
- Action: one primary or secondary button.
- No illustrations, no large decorative art.

**Examples**

> **No routes yet**  
> Add a route to send traffic to your first upstream.  
> [Add route]

> **No traffic in this range**  
> Your proxy is online, but no requests matched these filters. Try a wider time range.

> **No bans**  
> Nothing is banned right now. Automatic bans will appear here.

### 8.17 Loading states

- **Page or panel:** skeleton shapes that match the final layout. Use `--px-color-skeleton` with a static opacity pulse (1 → 0.6 → 1) over 1.6s, disabled under reduced motion.
- **Button or inline action:** 14–16px spinner that replaces the leading icon; label unchanged; `aria-busy="true"`.
- **Table:** 5–8 skeleton rows, same column widths.
- **Chart:** skeleton panel with axis placeholders; never a spinner over an empty chart.
- **Do not** use a full-page spinner, a fake progress bar, or a “Loading…” screen for more than 300ms without structure.

### 8.18 Error states

- **Field error:** inline below the field, danger text, 12px/500, icon optional.
- **Form summary:** at the top of long forms, lists fields with links to each, `role="alert"`.
- **Page error:** replaces the content, keeps the page header, explains what failed and offers “Try again”.
- **Global error:** full-page only for authentication or a fatal shell failure.
- **Network/offline:** a persistent inline banner above the content, not a toast. Say whether data is stale and when it last updated.
- **Permission error:** explain the role required, not just “Forbidden”.
- **Not found:** “This page doesn't exist or you don't have access to it.” Provide a link back to Overview. This wording also protects org privacy.

### 8.19 Pagination and load more

- Cursor-based data uses “Load more” at the bottom of the table, not numbered pages.
- Show the number of loaded rows and whether more exist.
- Keep the scroll position when loading more.
- A “Load more” button shows a spinner and is disabled while loading.
- For audit and logs, show a result count when the API provides it; otherwise say “Showing the latest 50 requests” rather than inventing a total.

### 8.20 Code and copyable values

- Inline code: JetBrains Mono 12–13px, subtle surface, 1px border, 4px radius, 2px 4px padding.
- Code block: surface, subtle border, 8px radius, 16px padding, horizontal scroll, copy button top-right.
- Copy button: icon-only, 28px, accessible label “Copy”, tooltip. After copying, swap to a check icon for 1.5s and announce “Copied” with a polite live region.
- Never truncate a copyable value without providing the full value via the copy action or a tooltip.

---

## 9. Application patterns

These patterns describe the product surfaces implied by the API and security documents. They are design specifications, not implementations. Where an endpoint is planned rather than implemented, the screen must still be buildable later without changing the design language.

### 9.1 Authentication

**Surfaces:** sign in, create account, invitation acceptance, session-expired state.

**Layout**

- Centred card on the warm canvas. Max width `400px`.
- Wordmark placeholder `proxyma` above the title, 20px high, 32px bottom margin.
- No split-screen marketing panel. No globe art behind the form. The artwork belongs on the homepage.
- One primary action. Secondary links below the form.
- Page title: 32px/600. Description: 15px/400 secondary.
- Card padding: 32px desktop, 24px mobile. On mobile the card may lose its border and sit directly on the canvas.

**Sign in**

- Fields: email, password.
- Primary button: “Sign in”.
- Links: “Create an account”. Only show “Forgot password?” if a reset flow exists; do not ship a dead link.
- After a `401 INVALID_CREDENTIALS`, show a single inline form error: “Email or password is incorrect.” Do not reveal which field is wrong.
- After `403 ACCOUNT_SUSPENDED`, replace the form with a persistent message: “This account is suspended. Contact the operator if you think this is a mistake.” Do not leave the user in a retry loop.
- After `429 RATE_LIMITED`, show “Too many attempts. Try again in {seconds} seconds.” and disable submit until the retry window ends.
- Preserve the email when login fails. Clear the password.

**Create account**

- Fields: display name, email, password.
- Password requirements are shown before submission, not only after failure. Minimum 10 characters; the backend remains the authority.
- Success returns the user but does **not** sign them in. Show a confirmation state: “Account created. Sign in to continue.” with a primary “Sign in” action. Do not pretend registration logged them in.
- `409 EMAIL_TAKEN`: “An account with this email already exists. Sign in instead.” Link to sign in.
- `422 WEAK_PASSWORD`: explain the requirement that failed, next to the password field.
- `422 VALIDATION_FAILED`: map field errors to inputs and keep the form values.

**Invitation acceptance**

- Show the organisation name and the invited email address before the form.
- If the signed-in user's email does not match, show `403 INVITATION_EMAIL_MISMATCH` as a blocking message with a sign-out action.
- `410 INVITATION_EXPIRED`: explain that the link expired and ask an owner or co-owner for a new invitation.
- `409 ALREADY_MEMBER`: offer a link to the organisation instead of an error dead end.

**Session expiry**

- Refresh happens before the access token expires, with a single-flight refresh.
- If refresh fails with `401 INVALID_REFRESH_TOKEN`, clear local auth state and show the sign-in screen with “Your session has ended. Sign in again.”
- If a normal request returns `403 ACCOUNT_SUSPENDED`, clear local auth state and show the suspension message.
- Preserve the attempted in-app location and return there after sign-in when safe. Never preserve a destructive action.

### 9.2 Organisation onboarding

**Flow**

1. The user signs in without an organisation.
2. Show a short creation form: organisation name, optional slug preview, and a plain explanation that a dedicated proxy container will be created.
3. After creation, the org begins as `STARTING`.
4. Show a persistent status panel: “Starting your proxy. This usually takes a few seconds. You can leave this page; setup continues.”
5. When the state becomes `ONLINE`, show the proxy URL and a primary “Open Overview” action.
6. If the state stays `OFFLINE`, explain that the proxy did not start and offer “Try again” or a link to support.

**MUST**

- Do not fake a progress percentage. The backend exposes a state, not a percentage.
- Do not block the user on the onboarding screen; they may navigate to other org pages while the proxy starts.
- Show the proxy URL with a copy action once available.
- `ORG_ALREADY_OWNED` if the account already owns an organisation: “You already own an organisation. Open it, or ask the operator for help.” Do not offer a second creation path.

### 9.3 Application shell and page header

Every dashboard page uses the shell from [section 7.2](#72-application-shell).

**Page header**

```text
Routes                                             [Validate] [Save changes]
Ordered rules that decide where each request goes.
```

- Title: 32px/600, one per page.
- Description: one sentence, 15px secondary, max 720px.
- Actions: primary last, right-aligned desktop, full-width stacked mobile.
- Metadata line: optional, 12px tertiary, includes last updated and timezone when relevant.
- Breadcrumbs only when the page is nested (for example Routes → Edit route).

**MUST**

- Keep the page header stable during loading and errors.
- Move focus to the page `<h1>` after a client-side route change, without scrolling the page.
- Do not draw a visible focus ring on that heading; it is a focus target for assistive technology, not an interactive control.
- Use a skip link as the first focusable element: “Skip to main content”.
- Do not use a top-level “Dashboard” title. Use the specific page name.

### 9.4 Overview

**Purpose:** answer “is my proxy healthy, and what is happening?” in under five seconds.

**Order**

1. **Proxy status band** — state badge, proxy URL with copy action, running vs latest config version, last heartbeat. If offline, explain that traffic is not being forwarded and that reconnection is automatic.
2. **Metric cards** — requests in the selected range, allowed rate, blocked + malicious count, p95 latency. Four cards maximum on desktop; two columns tablet; one column mobile.
3. **Traffic chart** — stacked bars from `/logs/stats` for the selected range. Link to Traffic → History.
4. **Latency chart** — p50/p95/p99, subject to the API caveat in [section 10.4](#104-latency-chart).
5. **Recent activity** — the latest 10 log entries, compact. Link to Traffic → Live.
6. **Attention items** — only when relevant: rejected config, offline proxy, approaching ban limit, expiring invitation. Do not render an empty “all good” card.

**MUST**

- The status band is not a metric card. It is a full-width band with a clear state and next action.
- Use the local timezone label near every time range.
- Show “No traffic yet” honestly when the range is empty.
- Do not show a latency card if the API cannot supply latency aggregates; show the metric as unavailable rather than inventing a number.

### 9.5 Routes and proxy configuration

**Surfaces:** Routes list, route editor, rate limits, detection, auto-block, trusted IPs, client IP, config history.

**Section navigation**

Use tabs or a secondary sidebar inside the Routes area:

- Routes
- Rate limits
- Detection
- Auto-block
- Trusted IPs
- Client IP
- History

**Routes list**

- Ordered list, not an alphabetical table. Order is behaviour: the first matching route wins.
- Columns: order, name, path prefix, methods, upstream, route rate limit, actions.
- Reorder with accessible “Move up” and “Move down” buttons. Drag-and-drop is an enhancement only, never the sole method.
- Show a short inline explanation: “Requests use the first matching route. Put specific routes before general ones.”
- Row actions: Edit, Duplicate, Delete.
- Empty state: “No routes yet. Add a route to send traffic to your first upstream.” → **Add route**.

**Route editor**

- Fields: name, path prefix, methods, upstream URL, timeout, strip prefix, route rate limit override.
- Explain `strip_prefix` in context, with the mapping example:
  “With `/api/users` and strip prefix on, `/api/users/42` reaches your upstream as `/42`.”
- Warn next to `localhost`, `127.0.0.1`, and `::1`: inside the container these mean the container itself. Proxyma rewrites them to `host.docker.internal`; the hint must say so.
- Validate on blur and before save. Do not validate on every keystroke.
- Server validation errors map to the exact field. Keep the user's values.

**Rate limits**

- Org-wide enabled switch, requests, per (second/minute/hour).
- Explain that limits are per client IP and per route, and that route overrides replace the org-wide limit rather than stacking.
- Counters reset when the proxy restarts. State this in help text; do not present limits as a billing quota.

**Detection**

- Master switch, action (`block` or `log`), inspected parts, max body bytes, built-in pattern toggles, custom sequences.
- Use plain labels for built-ins: SQL injection, path traversal, XSS, command injection, null bytes.
- Custom sequences are literal matches, not regex. Say so next to the field.
- Warn that detection is basic and not a WAF. Do not oversell it.

**Auto-block**

- Master switch and rules. Each rule reads as a sentence: “Ban an IP for 15 minutes after 10 rate-limit violations in 60 seconds.”
- Use a duration input with a unit select; `null` means permanent, labelled “Until removed manually”.
- Confirm when a rule is permanent and the threshold is low.

**Trusted IPs**

- List of IPs/CIDRs with add/remove.
- Explain that trusted clients skip rate limiting and detection and are never auto-banned, but a manual ban still applies.

**Client IP**

- Source: connection or `x_forwarded_for`.
- Warn that `x_forwarded_for` is only safe behind another trusted proxy; otherwise clients can spoof it.

**Save and apply**

- Use a sticky save bar when the form is dirty.
- On save, `PUT /config` with `If-Match`.
- `202 Accepted`: show “Saved. Waiting for your proxy to apply v8.” with a pending badge. Do not say “Applied”.
- `config_status: applied`: “v8 is live.” Show a success confirmation and keep the version visible.
- `config_status: rejected`: “v8 was rejected. Your proxy is still running v7.” Show the full rejection errors and link to the failing fields.
- `412 Precondition Failed`: show a conflict dialog. Explain that someone saved a newer version. Offer “Review changes” and “Reload latest”. Do not silently overwrite.
- `422`: inline field errors plus a summary. Keep the draft.
- `validate` action: run `POST /config/validate` without saving and show the result in a non-blocking panel.

**History**

- Table of versions: version, created at (local time), created by, status.
- Status uses the config badge vocabulary: Applied, Pending, Rejected.
- Selecting a version opens a read-only detail. Restoring a version requires confirmation and a new save.

### 9.6 Traffic

**Tabs:** Live, History.

**History**

- Filter bar: time range, action, status, method, path prefix, IP, route.
- Range options: 1 hour, 24 hours, 7 days, 30 days. Show the exact `from`–`to` range and timezone.
- Table columns: time, method, path, status, action, latency, client IP, route, config version.
- Sort newest first. Paging is cursor-based via “Load more”.
- `action` is the primary badge: Allowed (neutral), Rate limited (warning), Blocked (danger), Malicious (danger, stronger emphasis).
- HTTP status is neutral for 2xx, warning for 4xx, danger for 5xx. Do not colour the whole row.
- Paths use monospace and middle truncation with a copy action.
- Filters are reflected in the URL so a view can be shared.
- Mobile: keep the time sequence; use stacked rows or a horizontally scrollable dense table. Provide a Details action.

**Log detail drawer**

- Opens from a row without losing the table position.
- Shows every field as a definition list: timestamp with timezone, method, path, status, action, latency, client IP, upstream, route, config version, rule, request ID if present.
- All values are plain text. Never use `v-html`.
- Long values wrap inside the drawer; do not expand the page width.
- Close with Escape, backdrop click, or the close button; restore focus to the row action.

**Live**

- Explicit connection state: **Live**, **Paused**, **Reconnecting**, **Disconnected**.
- A pulsing dot is allowed only while live, and must stop under reduced motion.
- “Pause” control. While paused, show “N new events” as a button that resumes and scrolls to the newest event.
- Auto-scroll only when the user is already at the top of the list. If they have scrolled down, never yank them back.
- New rows highlight for 1.2s with `--px-color-accent-subtle`, then settle.
- Cap the DOM at 500 rows; drop the oldest. Offer “Open history” for older entries.
- After reconnect, show “Backfilling…” until the gap is filled, then “Live”. De-duplicate by log `id`.
- Do not announce each new row to screen readers. Announce connection changes with a polite live region.
- If the stream is unavailable, show a persistent inline banner: “Live updates are unavailable. History still works.” Do not silently poll.

### 9.7 Bans

- Tabs or filter: Active, All.
- Table columns: IP, source (automatic/manual), reason, created at, expires at, status, actions.
- Expiry: show relative time under 24 hours, absolute local time with timezone otherwise. “Permanent” is explicit.
- Manual ban dialog: IP, reason, duration (1 hour, 24 hours, 7 days, 30 days, permanent). Explain that bans are delivered asynchronously.
- `409 ALREADY_BANNED`: “That IP is already banned.” Link to the existing ban.
- `409 BAN_LIMIT_REACHED`: “This organisation has reached the 10,000 active ban limit. Remove a ban before adding another.”
- Unban: confirmation dialog, then “Ban removed. Your proxy will apply it shortly.” The ban remains in history.
- Trusted IP note: a manual ban still applies to a trusted IP.
- Empty state: “No bans. Nothing is banned right now. Automatic bans will appear here.”

### 9.8 Team

- Member table: name, email, role, joined at, actions.
- Roles: Owner, Co-owner, Member. Show a short role description in the role menu.
- Owner row has no remove action. Co-owner rows are only editable by the owner.
- Changing your own role is disabled with an explanation, not hidden silently.
- Invite flow: email, role (Member or Co-owner), optional personal message if supported. The API returns a one-time invite link; show it once with a copy action and a warning that it cannot be shown again.
- Invitation list: email, role, invited by, created at, expires at, revoke action.
- Errors:
  - `OWNER_PROTECTED`: “The owner can't be changed or removed.”
  - `CO_OWNER_PROTECTED`: “Only the owner can change or remove a co-owner.”
  - `SELF_ROLE_CHANGE`: “You can't change your own role.”
  - `ALREADY_MEMBER`: “This person is already a member.”
  - `INVITATION_EXPIRED`: “This invitation has expired. Send a new one.”
  - `INVITATION_EMAIL_MISMATCH`: “This invitation was sent to a different email address.”
- Removing a member requires confirmation that names the person and explains they lose access immediately.

### 9.9 Audit

- Read-only. No edit, delete, or export unless a separate export is approved.
- Table: time, actor, action, target, summary, source IP.
- Actor can be a user or `system`. System entries use a neutral badge and the rule that caused them.
- Action is shown as a human-readable label derived from the `<area>.<verb>` code, with the raw code available in the detail drawer.
- Summary is plain text.
- Filters: time range, actor, action. Reflected in the URL.
- Local timezone label. Precise timestamps on hover.
- Empty state: “No audit events in this range.”

### 9.10 Settings

**Placement:** Settings is the top-level destination. Account is at `/settings/account`, General at `/settings/general`, and Security at `/settings/security`. `/settings` redirects to Account. Legacy `/app` and `/app/password` redirect to the corresponding sections. The Settings sidebar item stays active for every section.

**Sections**

- Account: profile, email, account type, and the platform-administration entry. Profile: display name, save. Show the email as static text in the form; never as a disabled or read-only input. Only the display name is editable while `PATCH /users/me` accepts no other field.
- General: appearance and language.
  - Appearance: System / Light / Dark.
  - Language: only when more than one locale is registered. The control also lives in the public footer and auth card so users can override browser detection before signing in.
- Security: password change. Current password, new password, confirm. A successful change invalidates every session and forces a new sign-in; explain this before saving.
- Organisation: add a section when organisation surfaces exist. Name, proxy URL, timezone display preference if added, danger zone for deletion.

**MUST**

- Password fields use `current-password` and `new-password` autocomplete.
- The password change warning says: “Changing your password signs you out of every device, including this one. Sign in again with your new password.” Explain this before saving; do not add a second confirmation dialog.
- After a successful password change, clear the local session and return to sign-in with a “Password changed” notice and the email prefilled.
- Deleting an organisation requires typing its name and explains the consequences. The owner is the only role that can delete.
- The danger zone is visually separated by a 1px danger border and 32px top margin, not by a red background.

### 9.11 Platform administration

Admin is a separate platform context, not an organisation role.

**MUST**

- Show a persistent “Platform administration” marker in the top bar or page header. Show it once; do not repeat it in the sidebar header.
- Use the same tokens and components, but a distinct context label. Do not build a second design system.
- Keep the admin navigation in its own labelled section at the bottom of the sidebar body, above the footer. The persistent Settings link is the way back to account views; do not add a separate “Back to dashboard” link.
- Admin views are metadata-only: no config contents, no logs, no ban IPs, no org audit. Do not design panels that imply access the API does not grant.
- All actions must be available on mobile, including risky ones. On small screens, risky actions open a full-screen step-up sheet.
- Risky actions (suspend, force logout) require the admin's own password and a reason (maximum 500 characters). Use a step-up dialog, not a browser confirm.
- Never offer impersonation. It is not in the API and must not be designed as a future convenience.
- `ADMIN_CANNOT_TARGET_ADMIN`: “Platform admins can't suspend or force-logout other platform admins.”
- `CANNOT_SUSPEND_SELF`: “You can't suspend your own account.”
- A disabled admin panel returns `404`; show the normal not-found page, not an “admin disabled” hint.

**Admin org detail**

- Header: org name, state, owner, created at.
- Proxy panel: last heartbeat, running version, latest version, config status, active ban count.
- Members list: name and role only.
- Actions: suspend the owner's account or force the owner to sign out. Each opens step-up.
- Suspending an account does not stop the organisation's proxy; say so in the confirmation so the operator is not misled.

**Admin audit**

- Platform-wide, newest first, filters by time and action.
- Read-only. Include actor, action, target, summary, source IP.

### 9.12 Public homepage

**Purpose:** explain the product, establish the brand, and route people to account creation or sign-in. It is a product surface, not a generic marketing template.

**Status:** design pattern only. Do not implement it as part of this guide.

**Structure**

1. **Header** — text wordmark `proxyma` left; “Sign in” and “Create an account” right. No mega-menu.
2. **Hero** — two columns on desktop:
   - Left: eyebrow “Managed reverse proxy”, H1 “Always near, always here.”, supporting line “A reverse proxy you don't have to babysit.”, a short paragraph, primary “Create an account”, secondary “Sign in”.
   - Right: globe artwork, drawn directly on the canvas. In dark mode its ink is inverted to a warm light tone; there is no plate, card, or background behind it.
3. **Value props** — three items, no cards:
   - “Your own proxy” — an isolated container and URL for each organisation.
   - “Traffic you can see” — live requests, latency, and bans without a terminal.
   - “Changes without a restart” — routes and limits apply as a new config version.
4. **A short “How it works”** — three steps: create an account, create an organisation, point traffic at your proxy. Use numbered text, not illustrations.
5. **Closing action** — one line and one primary CTA. No testimonial wall, no fake logos, no newsletter form.
6. **Footer** — wordmark, “Documentation” link if it exists, “Sign in”, copyright. No social icons unless accounts exist.

**Visual rules**

- Canvas `--px-color-canvas`, ink type, generous whitespace.
- H1 uses the display size at ≥1024px and h1 size below.
- The globe is the only large visual. It must not become a background behind text.
- In dark mode, invert the globe's ink to a warm light tone and keep it on the canvas. No plate, card, or background.
- No gradient overlays, no floating cards, no animated hero.
- The homepage may use a 12-column grid with a 48% / 52% split for copy and art.
- On mobile, art moves below the hero copy and above the value props, or is hidden below 480px.

**Copy rules**

- No “revolutionary”, “seamless”, “next-generation”, “supercharge”, “unlock”, or “empower”.
- No invented customer counts, uptime claims, or performance benchmarks.
- Explain the product in concrete terms: a proxy, a container, routes, rate limits, logs.
- The tagline is used once, as the H1. Do not repeat it in every section.

### 9.13 Permissions and role-aware UI

- Roles: `owner`, `co_owner`, `member`, plus the platform-level `is_platform_admin`.
- Hide or disable actions the user cannot perform, but never rely on the UI for security. The server enforces every request.
- When an action is disabled because of role, explain why in text or a tooltip: “Only the owner can delete the organisation.”
- Do not show a disabled control without an explanation; prefer hiding it when the user has no path to the permission.
- Admin navigation appears only when `is_platform_admin` is true. Treat it as a hint; the server rechecks every admin request.

### 9.14 Error and empty-state mapping

- Every API error code maps to a message key in `errors.*`. Components never display a raw code.
- Use `application/problem+json` `title` and `detail` as diagnostic fallbacks only; the UI copy comes from the catalogue.
- Network failure: “We couldn't reach Proxyma. Check your connection and try again.”
- Timeout: “The request took too long. Try again.”
- `500 INTERNAL_ERROR`: “Something went wrong on our side. Try again. If it keeps happening, contact the operator.”
- `404 NOT_FOUND` on an org route: “This page doesn't exist or you don't have access to it.”
- `403 FORBIDDEN` with a known role requirement: explain the role.
- `412` on config: conflict dialog, never a generic error.
- Unknown code: “Something went wrong. Try again.” Log the code to the client console in development only.
- Always preserve user input and offer a next step.

---

## 10. Data visualisation

### 10.1 Principles

- A chart exists to answer a question, not to fill a panel. Name the question in the panel title or description.
- Show real data only. Never invent a series, a trend, or a latency percentile.
- Prefer position and length over area and angle. No pie charts, donuts, gauges, or 3D.
- Start value axes at zero. Do not truncate a latency or traffic axis to make a change look dramatic.
- Use colour with a second channel: labels, line style, pattern, or direct annotation.
- Keep one chart per question. Do not stack unrelated metrics into a dual-axis chart.
- Provide a text summary and a data-table fallback for every chart.

**Approved library:** `@unovis/vue` with `@unovis/ts`. Use `VisXYContainer`, `VisStackedBar`, `VisLine`, `VisAxis`, `VisCrosshair`, and `VisTooltip`. Lazy-load chart components so the initial shell does not pay for them. Do not add a second chart library.

**Chart heights:** 240px desktop / 200px mobile for traffic; 220px desktop / 180px mobile for latency. A chart may grow if it is the page's primary content, but not beyond 360px without a user control.

### 10.2 Chart tokens

| Token | Light | Dark | Use |
|---|---|---|---|
| `--px-chart-allowed` | `#0E6E75` | `#5CC8C0` | Allowed requests |
| `--px-chart-rate-limited` | `#B45309` | `#F0B45A` | Rate-limited requests |
| `--px-chart-blocked` | `#C2410C` | `#FF9A62` | Blocked requests |
| `--px-chart-malicious` | `#B42318` | `#FF7A7A` | Malicious requests |
| `--px-chart-total` | `#555B61` | `#A8ADA9` | Total line or label |
| `--px-chart-grid` | `#E4E1DA` | `#272B2D` | Horizontal gridlines |
| `--px-chart-axis` | `#6B7178` | `#858B87` | Axis labels |
| `--px-chart-latency-p50` | `#17191C` | `#F2F1ED` | p50 line |
| `--px-chart-latency-p95` | `#0E6E75` | `#5CC8C0` | p95 line |
| `--px-chart-latency-p99` | `#6B7178` | `#858B87` | p99 line |
| `--px-chart-crosshair` | `#8A8F88` | `#6B7170` | Hover crosshair |

**MUST**

- Use these tokens directly. Do not recolour a series dynamically based on its value.
- Keep the same series order in the chart, legend, tooltip, and data table.
- Do not use semantic success/warning/danger tokens for chart series unless the mapping is exact. Chart tokens are separate so the palette can be tuned without changing UI status colours.

### 10.3 Traffic chart

**Question:** “How much traffic did my proxy handle, and what happened to it?”

**Type:** stacked vertical bars, one bar per time bucket.

**Series, bottom to top:** allowed, rate limited, blocked, malicious. If all four are zero in a bucket, show an empty slot, not a zero-height artefact.

**Specification**

- X axis: time buckets, labelled in local time with a timezone label nearby.
- Y axis: request count, starts at zero, 4–5 ticks, compact formatting (`0`, `250`, `500`, `1k`).
- Gridlines: horizontal only, 1px `--px-chart-grid`, behind the bars.
- Bars: 2px gap between series, no border, no radius on individual stacks. The top of the stack may have a 2px radius but only if it does not distort the total.
- Total: show the total in the tooltip and optionally a neutral line. Do not add a second y-axis.
- Legend: always visible, series label + swatch. Clicking a legend item MAY toggle a series; if it does, the total must not silently change meaning.
- Tooltip: bucket time, total, and the four counts. Use the same order as the legend. Keep it inside the chart bounds.
- Empty range: replace the plot with an empty state, keep the axes hidden, and offer a wider range.
- Loading: skeleton bars/axes, not a spinner over a blank rectangle.
- Error: inline panel error with “Try again”, keep the range control.

**Do not**

- Use a pie or donut for the action breakdown.
- Use gradient fills or rounded, glossy bars.
- Animate bars on load.
- Hide the legend and rely on colour alone.

### 10.4 Latency chart

**Question:** “How slow are requests, including the tail?”

**Type:** three lines over the same time buckets: p50, p95, p99. Y axis in milliseconds, starts at zero.

**Series styles**

| Series | Line | Colour |
|---|---|---|
| p50 | Solid 2px | `--px-chart-latency-p50` |
| p95 | Dashed 2px (`6 4`) | `--px-chart-latency-p95` |
| p99 | Dotted 2px (`2 4`) | `--px-chart-latency-p99` |

Line style is mandatory. Colour alone is not sufficient, and the p99 line is intentionally quieter.

**Specification**

- Tooltip: bucket time plus all three percentiles, with units.
- Y axis label: “Latency (ms)”.
- Do not plot an average latency line. Averages hide tail pain.
- Do not show a latency series for an empty bucket as zero. Break the line or omit the point.
- If the API cannot supply percentiles, do not render this chart. See the caveat below.

**API caveat — required extension**

`GET /orgs/{org_id}/logs` returns `latency_ms` per entry, but `GET /orgs/{org_id}/logs/stats` currently returns only counts:

```json
{ "t": "2026-10-18T14:30:00Z", "total": 412, "allowed": 398, "rate_limited": 9, "blocked": 3, "malicious": 2 }
```

A truthful p50/p95/p99 **time series** needs latency aggregates per bucket. The backend plans to add this. The required shape is:

```json
{
  "t": "2026-10-18T14:30:00Z",
  "total": 412,
  "allowed": 398,
  "rate_limited": 9,
  "blocked": 3,
  "malicious": 2,
  "latency_ms": { "p50": 18, "p95": 64, "p99": 210 }
}
```

Until that field exists, the only honest fallback is a clearly labelled **“Latency sample”** based on the currently loaded log page. It must say “Based on the latest N requests, not a full time series” and must not be drawn as a time-series chart. Prefer omitting the chart and showing p95 as “Not available yet” on the metric card.

**MUST**

- Do not compute percentiles from a partial page and present them as global.
- Do not label a sampled value as p95 for the selected range.
- When the extension lands, update this section and the API contract together.

### 10.5 Metric cards

Metric cards summarise, they do not decorate.

**Anatomy**

```text
Label
1,284
+12.4% vs previous 24 hours
[optional sparkline]
```

- Label: 13px/500 secondary.
- Value: 28px/600, tabular, primary. Use `--px-font-size-metric`.
- Unit: 13px/500 secondary, aligned to the baseline, with a small gap.
- Comparison: 12px, tertiary, only when a comparable previous period exists. Include the comparison window in words.
- Sparkline: optional, 32px high, no axes, same series colour, no animation.
- Card: surface, 1px subtle border, 8px radius, 20px padding, no shadow.

**Rules**

- Use no more than four metric cards in a row.
- Do not show a percentage change when the previous period has no data.
- Do not colour a metric red unless it is a genuine error state. A high p95 is not automatically an error.
- Use a dash (`—`) with “Not available” for missing data, not `0`.
- Link a metric card to the filtered Traffic view when that helps the user act.

**Overview set**

| Metric | Value | Comparison |
|---|---|---|
| Requests | total count | previous equal-length range |
| Allowed | count + percentage | previous range |
| Blocked / malicious | combined count, with a breakdown in the tooltip | previous range |
| p95 latency | ms | previous range, only when latency data exists |

### 10.6 Status indicators

Status is never colour alone. Use a dot + label, or an icon + label.

| State | Dot | Label | Explanation |
|---|---|---|---|
| Proxy `ONLINE` | Success | Online | Traffic is being forwarded. |
| Proxy `STARTING` | Warning | Starting | The container is starting. |
| Proxy `CREATING` | Neutral | Creating | The organisation is being set up. |
| Proxy `OFFLINE` | Danger | Offline | Traffic is not being forwarded. Reconnection is automatic. |
| Proxy `DELETING` | Neutral | Deleting | Cleanup is in progress. |
| Config `applied` | Success | Applied | The proxy confirmed this version. |
| Config `pending` | Warning | Pending | Saved, waiting for the proxy. |
| Config `rejected` | Danger | Rejected | The proxy refused it and is running the previous version. |
| Live | Accent | Live | The stream is connected. |
| Reconnecting | Warning | Reconnecting | The stream dropped and is retrying. |

**Heartbeat:** show absolute local time and a relative phrase: “Last heartbeat 12 seconds ago (14:35:12 WET)”. If there has never been a heartbeat, say “No heartbeat yet”.

**MUST**

- Use the status vocabulary exactly as written in the message catalogue.
- Include an explanation for any non-healthy state in the same panel.
- Do not use a spinning indicator for a state that is waiting on the server. Use a static pending badge.

### 10.7 Logs and activity tables

Logs are the densest surface in the product. Optimise for scanning, not for showing every field.

**Default columns**

| Column | Format | Notes |
|---|---|---|
| Time | `14:35:12.482` | Local time; timezone label in the table header or page metadata |
| Method | Mono, 3–7 chars | GET, POST, etc. |
| Path | Mono, middle-truncated | Copy action; full value in the drawer |
| Status | Numeric, tabular | Neutral 2xx, warning 4xx, danger 5xx |
| Action | Badge | Allowed, Rate limited, Blocked, Malicious |
| Latency | `18 ms` | Tabular; `—` if unavailable |
| Client IP | Mono | Copy action |
| Route | Text | `—` when the default route handled it |
| Config | `v7` | Mono |

**Rules**

- Plain text only. Never use `v-html` for a log field.
- Middle-truncate long paths, query strings, and user agents. Never let a hostile string change the layout.
- Cap field length visually and provide the full value in the detail drawer.
- Do not render ANSI codes, HTML, or control characters. Strip or escape them at the boundary.
- Use `white-space: pre-wrap` only in the detail drawer, and always inside a bounded container.
- Keep the timestamp precise. Do not round to the minute in the table.
- Newest first in History. Live prepends newest at the top.

**Action colours**

| Action | Badge tone |
|---|---|
| `allowed` | Neutral: subtle surface, secondary text. Allowed is the normal case; teal stays reserved for the live signal. |
| `rate_limited` | Warning |
| `blocked` | Danger or orange |
| `malicious` | Danger |

Use the action badge as the primary signal. Do not colour the entire row.

### 10.8 Live activity

- The live view is a stream, not a chart. It must feel immediate but stay readable.
- Header: status badge, pause/resume, clear, and a “N new events” pill when paused.
- Row insert: 1.2s accent-subtle highlight, no slide or bounce.
- Auto-scroll only if the user is at the top. If they scroll away, show a “Jump to latest” button.
- Cap the buffer at 500 rows; drop the oldest and say so in the footer.
- Connection loss: show “Reconnecting…” in the header and a persistent inline banner. Do not replace the table with an error.
- After reconnect: “Backfilling…” until the gap is filled. De-duplicate by `id`. If backfill fails, say “Some events may be missing” and keep the stream.
- Screen readers: announce connection state changes, not individual rows. Provide a “Pause live updates” control for users who need stability.

### 10.9 Time ranges and timezone

**Range presets**

| UI label | API range | Suggested interval |
|---|---|---|
| 1 hour | `from = now - 1h` | `1m` |
| 24 hours | `from = now - 24h` | `5m` |
| 7 days | `from = now - 7d` | `1h` |
| 30 days | `from = now - 30d` | `1d` |

The backend limits the number of buckets. If a range would exceed the limit, use the next larger interval and say so in the chart footer.

**Timezone**

- Convert every API UTC timestamp to the browser's local timezone with `Intl.DateTimeFormat`.
- Display a persistent label near the range control or table header: **“Times in Europe/Lisbon (WET)”**. The zone and abbreviation come from `Intl.DateTimeFormat().resolvedOptions().timeZone` and `timeZoneName: 'short'`.
- Use 24-hour time by default (`hour12: false`) for logs and charts. The locale may override this only if the user chooses a locale whose convention differs, and the label must still be present.
- Relative time under 24 hours: “12 seconds ago”, “4 minutes ago”, “3 hours ago”. Absolute time on hover or in the detail drawer.
- Absolute format: `2 Nov 2026, 14:35:12 WET`. Include the zone abbreviation.
- For a range, show both endpoints in local time with the zone label: `2 Nov 2026 13:00 – 14:00 WET`.
- DST changes must be handled by `Intl`, not by fixed offsets.

### 10.10 Chart accessibility

**MUST**

- Provide a visible legend and a text summary: “412 requests: 398 allowed, 9 rate limited, 3 blocked, 2 malicious.”
- Provide a data-table fallback, either visible in a collapsible “View as table” region or available to assistive technology.
- Give the chart container `role="img"` and an `aria-label` that includes the question and the summary. Do not put hundreds of focusable points in the tab order.
- If the chart is interactive, make the crosshair and tooltip keyboard-accessible, or provide the data table as the keyboard path.
- Do not rely on hover alone for any value.
- Use patterns or dash styles in addition to colour for line series.
- Respect `prefers-reduced-motion`; charts must not animate on load or on range change.

---

## 11. Interaction design

### 11.1 Hover

- Change background, border, or text colour. Use `--px-duration-fast` and `--px-ease-out`.
- Do not scale, lift, rotate, or glow. Do not move layout.
- Hover is an enhancement. Every hover-only affordance must also be available on focus and touch.
- On touch devices, do not require hover to reveal an action. Keep row actions visible or in an always-visible menu.

### 11.2 Focus

- Use `:focus-visible` for keyboard focus. Do not remove the browser outline without a replacement.
- Focus ring: 2px accent, 2px offset, plus a canvas-coloured separator so it works on coloured surfaces.
- Focus must be clearly visible on primary buttons, inverse surfaces, selected rows, tabs, and chart controls.
- When a dialog opens, move focus inside. When it closes, restore focus to the trigger.
- After a route change, move focus to the page `<h1>` and update the document title.

### 11.3 Motion

- Duration: 120ms for hover/focus, 180ms for menus and tooltips, 240ms for dialogs and drawers.
- Easing: `cubic-bezier(0.2, 0, 0, 1)`.
- Animate opacity and transform only. Do not animate width, height, or layout position.
- No page transitions, no loading choreography, no animated counters, no chart draw-in.
- Under `prefers-reduced-motion: reduce`, set durations to `0ms` and keep state changes instant.

### 11.4 Loading feedback

- Button action: spinner in place of the leading icon, label unchanged, `aria-busy="true"`, disabled.
- Panel load: skeleton matching the final layout.
- Table load: skeleton rows.
- Background refresh: keep the old data visible, show a subtle “Updating…” label, and do not blank the panel.
- Long-running config apply: show the pending state and allow navigation. Do not block the page with a modal.

### 11.5 Success and error feedback

- Inline confirmation for a change in context: “v8 is live.” next to the config status.
- Toast for a background action whose result is not visible on screen: “Invitation revoked.”
- Error inline when it belongs to a field; page-level when it belongs to the whole request.
- Say what happened, why, and what to do next. Do not say “Success!” or “Oops!”.
- Do not auto-dismiss an error. Do not stack more than three toasts.

### 11.6 Confirmation

Use a confirmation dialog when the action is destructive, irreversible, or affects other people.

- Title names the object and the verb: “Remove Bo from Acme?”
- Body explains the consequence and who is affected.
- Primary button repeats the verb.
- Cancel is the default focus for destructive dialogs; the destructive button is not focused by default.
- Typed confirmation for organisation deletion and other irreversible actions.
- Admin risky actions use a step-up sheet with the admin's password and a reason. The reason is required and appears in the audit trail.
- Do not use `window.confirm`.

### 11.7 Keyboard

- Logical tab order follows visual order.
- Skip link is the first focusable element.
- Escape closes dialogs, drawers, dropdowns, and tooltips.
- Enter submits forms; Space activates buttons and checkboxes.
- Arrow keys move within tabs, menus, radio groups, and selects.
- Home/End move to the first/last item in a menu or tab list.
- Do not implement custom shortcuts in the MVP beyond browser and standard control behaviour. If shortcuts are added later, they must be documented and configurable.
- Every action available by mouse must be available by keyboard.

### 11.8 Scrolling and sticky elements

- Sticky page header? No. The top bar is sticky; the page header scrolls.
- Sticky table headers inside a bounded table region, not the whole page.
- Sticky save bar only when the form is dirty; it must not cover the last field. Add bottom padding equal to the bar height.
- Modals lock body scroll. Drawers lock body scroll on mobile.
- Preserve scroll position when opening and closing a detail drawer.

### 11.9 Destructive and async actions

- Disable the trigger while the request is in flight.
- Do not close the dialog until the request resolves.
- On success, close the dialog and show the result inline.
- On failure, keep the dialog open and show the error inside it.
- For async operations (`202 Accepted`), say “Saved. Waiting for your proxy…” and show the pending state. Do not claim completion.

---

## 12. Responsive behaviour

### 12.1 Breakpoints

| Name | Width | Primary context |
|---|---:|---|
| Small mobile | `<480px` | Phone |
| Mobile | `480–767px` | Large phone |
| Tablet | `768–1023px` | Tablet, small laptop |
| Desktop | `1024–1279px` | Standard laptop |
| Wide | `1280–1535px` | Desktop |
| Ultra-wide | `≥1536px` | Large display |

### 12.2 Desktop (≥1024px)

- Persistent 232px sidebar.
- 12-column content grid, 24px gutter.
- Page padding 32px.
- Page actions right-aligned in the header.
- Metric cards in a row of four.
- Tables show all default columns.
- Charts side by side only if each remains at least 480px wide. Otherwise stack them.
- Forms use a 640px column, with section navigation to the left.

### 12.3 Tablet (768–1023px)

- 64px icon rail. Labels hidden; tooltips on hover and focus.
- Page padding 24px.
- Metric cards in two columns.
- Page actions may wrap below the title.
- Tables scroll horizontally; freeze the first column for wide tables.
- Charts stack vertically.
- Forms remain a single 640px column; section navigation becomes horizontal tabs above the form.

### 12.4 Mobile (<768px)

- Top bar with menu button, wordmark, and a compact status indicator.
- Navigation becomes a full-height drawer, `min(320px, 88vw)`, with a backdrop.
- Page padding 16px.
- One column everywhere.
- Page title 24px, description 14px.
- Primary actions full width; secondary actions become an overflow menu if there are more than one.
- Metric cards one per row, or two compact cards if the values remain legible.
- Tables become stacked definition cards, except dense log tables which may scroll horizontally with a Details action.
- Filters open in a bottom sheet or full-screen sheet. Show the number of active filters on the trigger.
- Sticky save bar becomes full-width with stacked actions.
- Dialogs become full-screen sheets below 480px.
- Touch targets at least 44×44px.
- Inputs use 16px text to prevent iOS zoom.
- Charts 200px high, axes simplified to 4 labels, legend below the plot.

### 12.5 Small mobile (<480px)

- Type scale drops one step: display 36px, h1 24px, h2 20px, body 15px.
- Hide the globe artwork or place it below the hero copy at 280–320px.
- Tabs scroll horizontally with a fade edge.
- Hide non-essential table columns; keep time, action, path, and status in logs.
- Admin risky actions open a full-screen step-up sheet. All admin actions remain available.
- Do not use multi-column forms.

### 12.6 Responsive rules

**MUST**

- Reflow and reprioritise; do not simply shrink the desktop layout.
- Never require horizontal page scrolling at 320px. Individual data tables may scroll inside their own container.
- Keep the primary action reachable without opening a menu.
- Keep status, time, and the next action visible on every screen.
- Test at 320px, 375px, 768px, 1024px, 1280px, and 1440px.
- Test with 200% browser zoom at 1280px.

### 12.7 Mobile admin

All admin actions are available on mobile, including suspend and force logout. The layout changes, the capability does not.

- Admin navigation is in the drawer, under a “Platform administration” section.
- Org detail becomes a single column: identity, proxy, members, actions.
- Risky actions live in a sticky action bar or overflow menu and open a full-screen step-up sheet.
- The reason field is a textarea with a visible label; the password field uses `current-password`.
- Destructive confirmation still requires the typed resource name.
- Do not hide admin actions behind a “desktop only” message.

---

## 13. Accessibility

### 13.1 Standard

Target **WCAG 2.2 AA**. Accessibility is a release requirement, not a later audit.

### 13.2 Contrast

- Normal text: at least 4.5:1.
- Large text (≥24px regular or ≥18.66px bold): at least 3:1.
- Icons, control boundaries, focus indicators, and meaningful graphics: at least 3:1.
- Disabled controls are exempt from contrast requirements but must remain identifiable as disabled by more than colour.
- Use the measured palette in [section 5.5](#55-contrast). Do not introduce a new text colour without measuring it.

### 13.3 Semantics

- Use one `<main>` per page, with `<header>`, `<nav>`, and `<footer>` landmarks as appropriate.
- One `<h1>` per page. Do not skip heading levels.
- Use `<button>` for actions and `<a>` for navigation. Do not put a button inside a link.
- Use real `<table>`, `<form>`, `<label>`, `<select>`, and `<dialog>` semantics.
- Use `<ul>`/`<ol>` for lists, including navigation and audit events where the order matters.
- Decorative icons get `aria-hidden="true"`. Icon-only buttons get `aria-label`.
- The document `<html lang>` must be set to the active locale (`en-GB` by default, `pt-PT` when selected).
- Set a unique, descriptive `<title>` per route.

### 13.4 Forms

- Every input has a visible label connected with `for`/`id`.
- Required fields are marked in text and with `required`; do not rely on an asterisk alone.
- Help text and errors are connected with `aria-describedby`.
- Invalid fields set `aria-invalid="true"`.
- Error summaries use `role="alert"` and link to each field.
- Group related controls with `<fieldset>` and `<legend>`.
- Do not disable the submit button as the only way to prevent errors; validate and explain.
- Preserve input after errors.
- Announce asynchronous validation results politely, not on every keystroke.

### 13.5 Focus and keyboard

- Every interactive element is reachable by keyboard in a logical order.
- Focus is always visible.
- Dialogs trap focus and restore it on close.
- Menus, tabs, selects, and radio groups support arrow keys.
- Escape closes overlays.
- A skip link is the first focusable element.
- Route changes move focus to the new `<h1>`.
- Do not use positive `tabindex`.

### 13.6 Screen readers and live regions

- Toasts: `role="status"` for success/info, `role="alert"` for errors.
- Connection state: a polite live region announces “Live”, “Reconnecting”, and “Disconnected”.
- Config status: announce “v8 is live” or “v8 was rejected” politely.
- Do not announce every log row. Live data is visual by default; offer a pause control.
- Loading buttons use `aria-busy="true"` and keep their label.
- Icon-only controls have accessible names that describe the action and target: “Remove Bo from Acme”, not “Remove”.

### 13.7 Motion and sensory

- Respect `prefers-reduced-motion: reduce`: disable pulses, transitions, and chart animation.
- Do not flash more than three times per second.
- Do not rely on sound.
- Do not use colour alone for status, errors, chart series, or selected state.
- Provide text alternatives for charts and status indicators.

### 13.8 Zoom and reflow

- The layout must work at 200% zoom without loss of content or function.
- At 320px CSS width, content reflows to one column and the page does not scroll horizontally.
- Text must remain readable when the user increases the browser's default font size.
- Do not set `user-scalable=no` or `maximum-scale=1`.

### 13.9 Touch

- Minimum target 44×44px for interactive controls.
- At least 8px between adjacent targets.
- Do not rely on hover for essential actions.
- Swipe gestures are enhancements only; always provide a button alternative.

### 13.10 Testing

**MUST**

- Run `@axe-core/playwright` on every primary route in light and dark mode.
- Manually test keyboard-only navigation for sign-in, route editing, traffic filters, bans, team, and admin step-up.
- Test with a screen reader for the live stream, config status, and destructive dialogs.
- Test at 320px, 768px, and 1440px.
- Test with reduced motion enabled.
- Test at 200% zoom.

**Automated checks do not replace manual testing.** Axe catches a minority of real accessibility problems.

---

## 14. Brand voice

### 14.1 Principles

Proxyma sounds like a competent colleague who is calm under pressure.

- **Clear** — say the thing in the fewest words that stay accurate.
- **Concise** — one idea per sentence, one action per button.
- **Friendly** — direct, never chummy or performative.
- **Technically accurate** — use the real term when it matters: proxy, route, upstream, config version, ban.
- **Free from unnecessary jargon** — explain a term the first time it appears in a flow.
- **Free from marketing language inside the product** — the tagline belongs on the homepage and auth surfaces, not on the dashboard.

### 14.2 British English

Default locale: `en-GB`; Portuguese copy uses European Portuguese (`pt-PT`).

| Use | Not |
|---|---|
| organisation | organization |
| behaviour | behavior |
| colour | color |
| centre | center |
| authorise | authorize |
| licence (noun) | license (noun) |
| practise (verb) | practice (verb) |
| cancelled | canceled |
| modelling | modeling |
| catalogue | catalog |
| customise | customize |
| analyse | analyze |

Keep API field names and code identifiers in their original form (`organization` if the backend uses it). The UI copy is British English; the wire format is not translated.

### 14.3 Grammar and tone

- Sentence case for headings, labels, buttons, menu items, and table headers.
- Active voice: “Your proxy rejected v8”, not “v8 was rejected by your proxy”.
- Second person: “your proxy”, “your organisation”, “you”.
- Present tense for state: “Waiting for your proxy”, “Your proxy is offline”.
- Contractions are allowed: “can't”, “you'll”, “it's”. Avoid “cannot” unless the sentence needs emphasis.
- No exclamation marks.
- No “Oops”, “Whoops”, “Uh oh”, “Success!”, “Done!”, or “Ta-da”.
- No “simply”, “just”, “obviously”, “easy”, or “quick” when describing a task that might fail.
- Do not blame the user. Say what happened and what to do.
- Do not use “we” for the product when “Proxyma” or the specific component is clearer.
- Do not personify the proxy excessively. It can “wait”, “reconnect”, and “reject”, but it does not “think” or “want”.

### 14.4 Tone by context

| Context | Tone | Example |
|---|---|---|
| Onboarding | Encouraging, concrete | “Starting your proxy. This usually takes a few seconds.” |
| Normal operation | Neutral, informative | “4 routes · 1,284 requests in the last hour” |
| Pending | Honest, patient | “Saved. Waiting for your proxy to apply v8.” |
| Success | Brief, specific | “v8 is live.” |
| Warning | Direct, actionable | “Your proxy is offline. Traffic is not being forwarded right now.” |
| Error | Calm, specific, next step | “We couldn't save the route. Check the upstream URL and try again.” |
| Destructive | Serious, explicit | “This stops the proxy for everyone in this organisation and can't be undone.” |
| Admin | Procedural, neutral | “Suspend this account? The user will be signed out and unable to sign in.” |

### 14.5 Buttons and actions

Use verb-first labels that name the outcome.

| Use | Not |
|---|---|
| Save changes | Submit |
| Create organisation | Create |
| Add route | Add |
| Ban IP | Confirm |
| Remove member | Delete |
| Unban IP | Remove |
| Sign in | Log in |
| Sign out | Log out |
| Try again | Retry |
| View details | More |

One action per button. Do not use “Yes” / “No” in a dialog; repeat the verb.

### 14.6 Status vocabulary

These words are fixed. Do not invent synonyms in different screens.

| Concept | Words |
|---|---|
| Proxy | Online, Starting, Creating, Offline, Deleting |
| Config | Applied, Pending, Rejected |
| Live stream | Live, Paused, Reconnecting, Disconnected |
| Ban | Active, Expired, Removed, Permanent |
| Role | Owner, Co-owner, Member |
| Request action | Allowed, Rate limited, Blocked, Malicious |

Lowercase in sentences; capitalise only as a badge or label.

### 14.7 Errors

Every error answers three questions:

1. **What happened?**
2. **Why?** (when the cause is known)
3. **What next?**

**Pattern**

```text
[What happened]. [Why, if known]. [Next step].
```

**Examples**

- “We couldn't save the route. The upstream URL must start with `http://` or `https://`.”
- “Your proxy is offline. Traffic is not being forwarded right now. It will reconnect automatically.”
- “v8 was rejected. Your proxy is still running v7. Fix the highlighted fields and save again.”
- “This invitation has expired. Ask an owner or co-owner for a new one.”
- “You don't have permission to change roles. Only the owner can change a co-owner.”

**Do not**

- Show a raw error code as the main message.
- Show a stack trace, request ID, or backend exception in the interface. A request ID may appear in a details disclosure for support.
- Say “Something went wrong” without a next step.
- Use a toast for a field error.

The full API code-to-copy map is in [appendix 17.3](#173-api-error-to-copy-map).

### 14.8 Numbers and units

- Thousands separators: `1,284`, `10,000`.
- Compact counts only where space is tight: `12.4k`, with the full value in a tooltip or detail view.
- Percentages: one decimal place when it adds meaning (`97.2%`), otherwise whole (`98%`).
- Latency: `18 ms`, `1.2 s` above 1,000ms.
- Rates: `100/min`, `5/sec`, `1,000/hour`.
- Config versions: `v7`.
- Bytes: `8 KB`, `1.5 MB` (decimal units for UI, matching typical proxy limits).
- Durations: `15 minutes`, `24 hours`, `30 days`.
- Ranges: `13:00–14:00 WET`, with a space after the en dash if the surrounding typography allows.
- Negative or missing values: use `—` with an accessible label, not `0` or `N/A` alone.

### 14.9 Dates and times

- Display in the browser's local timezone with a persistent label: **“Times in Europe/Lisbon (WET)”**.
- Logs and charts use 24-hour time by default.
- Relative under 24 hours: “12 seconds ago”, “4 minutes ago”, “3 hours ago”.
- Absolute: `2 Nov 2026, 14:35:12 WET`.
- Hover or detail shows the absolute timestamp.
- Never show a bare UTC timestamp in the interface. If UTC is needed for support, show it in a labelled secondary line, not as the primary value.
- Derive the label from the browser's current timezone at render time with `Intl`, not from a fixed string. It should reflect a system timezone change on the next load.

### 14.10 Empty states and onboarding copy

Empty states are instructions, not apologies.

**Use**

- “No routes yet. Add a route to send traffic to your first upstream.”
- “No traffic in this range. Your proxy is online, but no requests matched these filters.”
- “No bans. Nothing is banned right now. Automatic bans will appear here.”
- “No audit events in this range.”

**Avoid**

- “Nothing to see here!”
- “You haven't done anything yet.”
- “Get started by exploring the dashboard.”
- “Oops, this is empty.”

### 14.11 Homepage copy

Approved:

- H1: **“Always near, always here.”**
- Supporting line: **“A reverse proxy you don't have to babysit.”**
- Primary CTA: **“Create an account”**
- Secondary CTA: **“Sign in”**

Portuguese (`pt-PT`) equivalents:

- H1: **“Sempre perto, sempre presente.”**
- Supporting line: **“Um proxy reverso que não precisa de supervisão constante.”**
- Primary CTA: **“Criar uma conta”**
- Secondary CTA: **“Iniciar sessão”**

The homepage may add concrete value propositions, but it must not invent customers, benchmarks, or claims. Keep the tone confident and plain.

### 14.12 Portuguese (pt-PT) terminology

Use the same terms across the product so users do not have to relearn them per screen.

| Concept | Use | Not |
|---|---|---|
| account | conta | conta de utilizador |
| audit | auditoria | audit |
| ban | banimento | ban |
| config version | versão da configuração | versão de config |
| display name | nome a apresentar | nome de exibição |
| email | email | e-mail |
| heartbeat | sinal de atividade | heartbeat |
| member | membro | membro de equipa |
| organisation | organização | organizaçao (sem acento) |
| owner | proprietário | dono |
| co-owner | coproprietário | co-dono |
| password | palavra-passe | password |
| platform administrator | administrador da plataforma | admin |
| proxy | proxy | servidor intermédio |
| rate limit | limite de taxa | rate limit |
| route | rota | caminho |
| session | sessão | sessão de utilizador |
| sign in | iniciar sessão | fazer login |
| sign out | terminar sessão | fazer logout |
| suspend | suspender | desativar |
| traffic | tráfego | tráfego de rede |
| user | utilizador | usuário |

Keep API field names, status enums, and log content in their original form. Translate their labels, not the wire format.

---

## 15. Do's and don'ts

### 15.1 Layout

| Do | Don't |
|---|---|
| Group with spacing and 1px rules | Wrap every section in a floating card |
| Keep one primary action per view | Put three filled buttons side by side |
| Use a stable page header | Collapse the header while loading |
| Let tables and charts use the full content width | Constrain data to a narrow marketing column |
| Use a 640px form column | Stretch inputs across a 1440px screen |
| Keep the shell stable across routes | Rebuild navigation per page |

### 15.2 Colour

| Do | Don't |
|---|---|
| Use ink for primary buttons | Use teal for every CTA |
| Reserve teal for links, focus, selected nav, live | Use teal as a decorative background wash |
| Pair status colour with text or a dot | Rely on red/green alone |
| Use semantic tokens | Inline `#0E6E75` in a component |
| Invert the globe's ink to warm light on the dark canvas | Put the globe on a white or paper plate in dark mode |
| Keep danger for destructive actions | Use danger red for a high but healthy metric |

### 15.3 Typography

| Do | Don't |
|---|---|
| Use Inter for UI and JetBrains Mono for technical values | Use monospace for body copy |
| Keep headings at 600 | Use 700–900 for emphasis |
| Use sentence case | Use Title Case Everywhere |
| Use tabular numerals in tables | Let numbers jitter between rows |
| Keep 15px body text | Drop body text to 11px to fit more |
| Allow 30–40% expansion for translations | Fix button widths to English strings |

### 15.4 Components

| Do | Don't |
|---|---|
| Use native semantics first | Build a div-based select |
| Use Reka UI for complex behaviour | Reimplement focus traps by hand |
| Show a visible label | Use placeholder as a label |
| Keep the button label during loading | Replace it with a spinner only |
| Use one dialog for a focused task | Nest dialogs |
| Use underline tabs | Use pill tabs for navigation |
| Use a 44px touch target | Use a 20px icon as the only target |

### 15.5 Data visualisation

| Do | Don't |
|---|---|
| Use stacked bars for traffic composition | Use a pie or donut |
| Start the y-axis at zero | Truncate the axis to dramatise a change |
| Use line style plus colour for latency | Rely on colour alone |
| Show a legend and a data-table fallback | Hide the legend and hope |
| Label sampled data as a sample | Present a partial page as a full time series |
| Show empty ranges honestly | Draw a flat line at zero for missing data |

### 15.6 Copy

| Do | Don't |
|---|---|
| “v8 is live.” | “Success!” |
| “Your proxy is offline. Traffic is not being forwarded.” | “Something went wrong.” |
| “Save changes” | “Submit” |
| “This can't be undone.” | “Are you sure?” |
| “Times in Europe/Lisbon (WET)” | A bare timestamp with no zone |
| “Organisation” | “Organization” in UI copy |

### 15.7 Accessibility

| Do | Don't |
|---|---|
| Use `:focus-visible` with a 2px ring | Set `outline: none` |
| Keep one `<h1>` per page | Use a div styled as a heading |
| Announce connection changes politely | Announce every live log row |
| Respect reduced motion | Animate charts on load |
| Test at 200% zoom | Lock the viewport scale |
| Use `aria-label` on icon-only buttons | Leave an icon button unnamed |

### 15.8 Implementation

| Do | Don't |
|---|---|
| Keep tokens in `tokens.css` | Scatter hex values across SFCs |
| Use scoped SFC styles | Add a global utility framework |
| Import Lucide icons by name | `import * as icons` |
| Lazy-load charts | Put Unovis in the initial shell |
| Keep strings in the catalogue | Hardcode English in templates |
| Replace the scaffold before shipping | Ship the Vue logo or green accent |

---

## 16. Implementation guidelines

### 16.1 CSS architecture

The frontend currently has a scaffold `base.css` and `main.css`. Replace them incrementally with the following structure. These files are **proposed**, not existing.

```text
src/
├── assets/
│   ├── tokens.css      # primitives + semantic tokens, light and dark
│   ├── base.css        # reset, root defaults, focus, reduced motion
│   └── main.css        # app shell, layout primitives, global element styles
├── components/
│   ├── ui/             # PxButton, PxInput, PxDialog, ...
│   ├── layout/         # AppShell, Sidebar, TopBar, PageHeader
│   └── charts/         # TrafficChart, LatencyChart, Sparkline
├── composables/
│   ├── useFormatters.ts
│   ├── useLocale.ts
│   └── useTheme.ts
├── i18n/
│   ├── index.ts
│   ├── locale.ts       # detection, storage, document language
│   └── locales/
│       ├── en-GB.ts
│       └── pt-PT.ts
├── lib/
│   └── api.ts          # typed fetch wrapper, problem+json handling
└── views/
```

**Import order in `main.ts`**

1. Fontsource CSS.
2. `tokens.css`.
3. `base.css`.
4. `main.css`.
5. App bootstrap.

Do not use `@import` chains inside CSS for tokens; import each file once from `main.ts` so the order is explicit.

### 16.2 Base CSS

```css
*,
*::before,
*::after {
  box-sizing: border-box;
}

html {
  min-height: 100%;
  background: var(--px-color-canvas);
  color-scheme: light;
  font-family: var(--px-font-sans);
  text-rendering: optimizeLegibility;
  -webkit-text-size-adjust: 100%;
}

:root[data-theme='dark'] {
  color-scheme: dark;
}

body {
  min-height: 100%;
  margin: 0;
  background: var(--px-color-canvas);
  color: var(--px-color-text-primary);
  font-size: var(--px-font-size-body);
  line-height: var(--px-line-body);
}

::selection {
  background: var(--px-color-selection);
}

:focus-visible {
  outline: none;
  box-shadow: var(--px-focus-ring);
}

@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
    scroll-behavior: auto !important;
  }
}
```

**Rules**

- Do not set a global `outline: none`.
- Do not set `overflow: hidden` on `body` globally; only while a modal or drawer is open.
- Use `min-height: 100dvh` for full-screen auth layouts, with `100vh` as a fallback.
- Keep the reset small. Do not import a large reset library.

### 16.3 Theme initialization

The theme preference is three-state: **System**, **Light**, **Dark**. Store it in `localStorage` under `proxyma.theme`. Resolve it to `light` or `dark` and apply it as `data-theme` on `<html>` before first paint.

**Pre-paint script:** `public/theme-init.js`, loaded as a classic script in `<head>` before the stylesheet.

```js
(() => {
  const stored = localStorage.getItem('proxyma.theme')
  const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
  const theme = stored === 'light' || stored === 'dark' ? stored : prefersDark ? 'dark' : 'light'

  document.documentElement.dataset.theme = theme
  document.documentElement.style.setProperty('color-scheme', theme)
})()
```

```html
<script src="/theme-init.js"></script>
```

**Why an external file:** an inline script would require a CSP hash or `unsafe-inline`. A same-origin classic script can be allowed with `script-src 'self'`, which is stricter.

**Runtime behaviour**

- `useTheme.ts` owns the preference (`system`, `light`, `dark`) and the resolved theme.
- Setting the preference writes to `localStorage` and updates `data-theme` and `colorScheme`.
- When the preference is `system`, listen to `matchMedia('(prefers-color-scheme: dark)')` and update the resolved theme when it changes.
- The control is a three-way segmented control or a menu with three radio items. Use labels, not icons alone.
- Do not store the resolved value as the preference; store `system` so it keeps following the OS.
- If storage is unavailable, fall back to `system` in memory.

### 16.4 Internationalisation

**Setup**

```ts
// src/i18n/index.ts
import { createI18n } from 'vue-i18n'
import enGB from './locales/en-GB'
import ptPT from './locales/pt-PT'
import { resolveInitialLocale } from './locale'

export const i18n = createI18n({
  legacy: false,
  locale: resolveInitialLocale(),
  fallbackLocale: 'en-GB',
  messages: {
    'en-GB': enGB,
    'pt-PT': ptPT,
  },
})
```

**Rules**

- Shipped locales: `en-GB` (default) and `pt-PT`. Add new catalogues to the map and to `SUPPORTED_LOCALES`; the language switcher appears when more than one is registered.
- Resolve the first visit in this order: valid stored choice, the first supported entry in `navigator.languages`, then `en-GB`.
- Persist explicit choices in `localStorage` under `proxyma.locale`. Do not persist an auto-detected locale, so a later browser-language change still applies.
- Exact matches win, then the base language: `en-US` uses `en-GB`; any Portuguese variant uses `pt-PT` until a `pt-BR` catalogue exists.
- The switcher lives in the public footer, auth card, and Settings → General. The homepage hero, header, and sidebar footer stay free of language controls.
- Every UI string is a key. No literal English in templates, except user data and technical values.
- Use interpolation: `t('traffic.range.summary', { count, range })`.
- Use pluralisation: `t('bans.count', count, { count })`, not `count + ' bans'`.
- Use `<i18n-t>` for sentences with embedded links; never concatenate translated fragments.
- Error codes map to `errors.<CODE>`. Unknown codes fall back to `errors.UNKNOWN`.
- On locale change, set `document.documentElement.lang`, re-apply the route title, and update `Intl` formatters.
- Format dates and numbers with `Intl`, using the active locale and the browser timezone.
- Allow 30–40% expansion. Test the longest catalogue string in buttons, tabs, and table headers.
- Do not translate API field names, status enum values, or log content. Translate their labels.

**Formatter composable**

```ts
// src/composables/useFormatters.ts (proposed)
export function useFormatters(locale: string) {
  const dateTime = new Intl.DateTimeFormat(locale, {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
    timeZoneName: 'short',
  })

  const relative = new Intl.RelativeTimeFormat(locale, { numeric: 'auto' })
  const number = new Intl.NumberFormat(locale)

  return {
    formatDateTime: (value: string | Date) => dateTime.format(new Date(value)),
    formatRelative: (value: number, unit: Intl.RelativeTimeFormatUnit) =>
      relative.format(value, unit),
    formatNumber: (value: number) => number.format(value),
  }
}
```

### 16.5 Icons

Use `@lucide/vue` with explicit named imports.

```ts
import { Activity, Route, ShieldBan, ScrollText } from '@lucide/vue'
```

**Rules**

- Sizes: 16px inline, 18px sidebar, 20px page actions, 24px empty states.
- Stroke: 1.75 at 20px, 2 at 16px. Do not mix icon styles.
- Decorative icons: `aria-hidden="true"`. Icon-only buttons: `aria-label` plus a tooltip.
- Do not import the whole icon set or build a string-to-icon map with `import * as icons`.
- Do not use emoji as interface icons.
- Approved set: `LayoutDashboard`, `Route`, `Activity`, `ShieldBan`, `ScrollText`, `Users`, `Settings`, `ShieldCheck`, `Plus`, `Search`, `Filter`, `RefreshCw`, `Copy`, `Check`, `X`, `ChevronDown`, `MoreHorizontal`, `Pencil`, `Trash2`, `Play`, `Pause`, `ArrowUp`, `ArrowDown`, `ExternalLink`, `LogOut`, `KeyRound`, `Eye`, `EyeOff`, `AlertTriangle`, `CheckCircle2`, `Clock`, `CircleDot`, `Wifi`, `WifiOff`, `Server`, `Gauge`, `Ban`, `Unlock`, `UserPlus`, `Link`, `Clipboard`, `Terminal`, `Code2`, `Globe`, `Lock`, `CircleHelp`, `Info`.
- Add a new icon only when the concept is not covered. Keep the list curated.

### 16.6 Charts

**Lazy-load** chart components so the initial shell does not include Unovis.

```vue
<script setup lang="ts">
import { defineAsyncComponent } from 'vue'

const TrafficChart = defineAsyncComponent(
  () => import('@/components/charts/TrafficChart.vue'),
)
</script>

<template>
  <Suspense>
    <TrafficChart :buckets="buckets" :range="range" />
    <template #fallback>
      <div class="px-chart-skeleton" aria-hidden="true" />
    </template>
  </Suspense>
</template>
```

**Rules**

- Render a chart only when it has data or is in an explicit loading state.
- Use Unovis `VisXYContainer`, `VisStackedBar`, `VisLine`, `VisAxis`, `VisCrosshair`, and `VisTooltip`.
- Keep chart configuration in the chart component, not in the page view.
- Provide `aria-label`, a visible summary, and a data-table fallback.
- Do not animate on mount or range change.
- Recompute on container resize with the library's responsive behaviour; do not hardcode pixel widths.
- Pause polling when the tab is hidden; resume and refresh when it becomes visible.
- Do not poll `/logs/stats` faster than every 30 seconds. Live rows come from SSE, not from the stats endpoint.

### 16.7 Vue component example

```vue
<script setup lang="ts">
import { computed } from 'vue'
import { Copy } from '@lucide/vue'
import { PxBadge, PxIconButton } from '@/components/ui'

const props = defineProps<{
  proxyUrl: string
  state: 'ONLINE' | 'STARTING' | 'CREATING' | 'OFFLINE' | 'DELETING'
}>()

const emit = defineEmits<{
  copy: [value: string]
}>()

const stateTone = computed(() => {
  if (props.state === 'ONLINE') return 'success'
  if (props.state === 'STARTING') return 'warning'
  if (props.state === 'OFFLINE') return 'danger'
  return 'neutral'
})
</script>

<template>
  <section class="proxy-status" aria-labelledby="proxy-status-title">
    <div class="proxy-status__identity">
      <h2 id="proxy-status-title" class="proxy-status__title">Proxy</h2>
      <PxBadge :tone="stateTone">
        {{ props.state }}
      </PxBadge>
    </div>

    <div class="proxy-status__url">
      <code>{{ proxyUrl }}</code>
      <PxIconButton label="Copy proxy URL" @click="emit('copy', proxyUrl)">
        <Copy :size="16" aria-hidden="true" />
      </PxIconButton>
    </div>
  </section>
</template>

<style scoped>
.proxy-status {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--px-space-4);
  padding: var(--px-space-5);
  border: var(--px-border-width) solid var(--px-color-border-subtle);
  border-radius: var(--px-radius-card);
  background: var(--px-color-surface);
}

.proxy-status__identity {
  display: flex;
  align-items: center;
  gap: var(--px-space-2);
}

.proxy-status__title {
  margin: 0;
  font-size: var(--px-font-size-h3);
  font-weight: 600;
}

.proxy-status__url {
  display: flex;
  align-items: center;
  gap: var(--px-space-2);
  min-width: 0;
  font-family: var(--px-font-mono);
  font-size: var(--px-font-size-mono);
}

.proxy-status__url code {
  overflow-wrap: anywhere;
}
</style>
```

**Note:** the badge text should come from the catalogue in real code. The example keeps it literal only to show the component structure.

### 16.8 Styling rules

- Use `<style scoped>` for view and component styles. Global styles belong in `base.css` or `main.css`.
- Use BEM-like class names: `.px-card`, `.px-card__title`, `.px-card--compact`.
- Never style by element alone in a component unless the element is the component root.
- Use `:deep()` sparingly, and only to style third-party internals that cannot be reached through props.
- Avoid `!important`. The only acceptable use is the reduced-motion reset in `base.css`.
- Use logical properties (`margin-inline`, `padding-block`) where directionality matters.
- Use `gap` instead of margins for flex and grid children.
- Do not use inline styles except for dynamic values passed as CSS custom properties (for example `:style="{ '--value': percentage }"`).
- Keep selectors short. Avoid nesting deeper than three levels.
- Do not use CSS modules, CSS-in-JS, or a preprocessor.

### 16.9 Review checklist

**Visual**

- [ ] The page has one clear primary action.
- [ ] Sections are grouped with spacing and 1px rules, not card soup.
- [ ] Type hierarchy uses the scale; no arbitrary sizes.
- [ ] Numbers use tabular numerals.
- [ ] Technical values use JetBrains Mono.
- [ ] Icons are Lucide, correct size and stroke.
- [ ] Light and dark screenshots both look intentional.
- [ ] The globe appears only on the homepage, with its ink inverted for dark mode and no plate behind it.

**Behaviour**

- [ ] All states are implemented: default, hover, active, focus, disabled, loading, error, empty.
- [ ] Async actions show pending and never claim completion early.
- [ ] Destructive actions require confirmation and explain consequences.
- [ ] Errors preserve input and offer a next step.
- [ ] Live views show connection state and handle reconnect/backfill.

**Accessibility**

- [ ] Keyboard-only path works.
- [ ] Focus is visible and restored after overlays.
- [ ] One `<h1>`; landmarks and table semantics are correct.
- [ ] Forms have labels, help text, and `aria-describedby`.
- [ ] Status is not colour-only.
- [ ] Reduced motion is respected.
- [ ] Axe passes on the route in both themes.

**Responsive**

- [ ] Works at 320px, 768px, 1024px, 1440px.
- [ ] Mobile actions are not hidden.
- [ ] Tables reflow or scroll inside their container.
- [ ] Admin actions remain available on mobile.

**Code**

- [ ] No raw hex values outside `tokens.css`.
- [ ] No hardcoded UI strings.
- [ ] No new dependency.
- [ ] No `v-html` for logs or user data.
- [ ] `bun run lint`, `bun run type-check`, `bun run build` pass.
- [ ] Playwright and axe tests pass.

### 16.10 Migration from the scaffold

Do the work in this order:

1. Replace `index.html`: `lang="en-GB"`, real title, empty favicon, pre-paint theme script.
2. Add `tokens.css`, rewrite `base.css`, trim `main.css`.
3. Import fonts and tokens in `main.ts`; add `vue-i18n`.
4. Delete the Vue welcome components, logo, icons, and scaffold routes.
5. Build the UI primitives: button, input, field, badge, card, dialog, dropdown, table, toast, tooltip.
6. Build the app shell and navigation.
7. Build authentication and organisation onboarding.
8. Build Overview, Routes, Traffic, Bans, Team, Audit, Settings.
9. Build Admin.
10. Build the homepage last, using the approved copy and the globe asset.
11. Replace the Playwright test that asserts the scaffold “You did it!” heading.
12. Add axe checks for the primary routes.

Do not keep the Vue green accent, Vue logo, Vue favicon, or the scaffold `#app` width/grid.

### 16.11 Performance and bundle budget

These are **SHOULD** targets, not hard limits:

| Asset | Budget |
|---|---:|
| Initial JS (shell + auth) | ≤ 200 KB gzip |
| Initial CSS | ≤ 30 KB gzip |
| Fonts (both families, Latin, wght only) | ≤ 120 KB total |
| Chart chunk (lazy) | ≤ 150 KB gzip |
| Hero artwork | ≤ 150 KB, WebP preferred |

**Rules**

- Import only the font weight axis and the subsets you need.
- Import Lucide icons by name.
- Lazy-load Unovis and any admin-only code.
- Do not ship a chart library on the auth or homepage routes unless the chart is rendered.
- Do not preload both font families on the homepage if only one is used above the fold.
- Use `font-display: swap` (Fontsource default) so text is readable immediately.
- Measure with `bun run build` and the browser network panel before and after adding a dependency.

### 16.12 Testing

```bash
bun run lint
bun run type-check
bun run build
bun run test:unit
bun run test:e2e
```

**MUST**

- Replace the scaffold e2e assertion before shipping the homepage.
- Add axe checks for sign-in, Overview, Traffic, Routes, Bans, Team, and Admin.
- Test the live stream with the backend unavailable.
- Test config pending, applied, rejected, and 412 conflict states.
- Test admin step-up on mobile.
- Test 320px, 768px, 1024px, and 1440px.
- Test both themes and reduced motion.

### 16.13 How future AI agents should apply this guide

1. **Read first, code second.** Start with sections 2–7. Do not invent a visual direction from a prompt.
2. **Find the pattern.** Match the screen to section 9 and the component to section 8.
3. **Reuse before creating.** If a `Px*` component exists, use it. If it almost fits, extend it with a documented variant rather than forking it in a view.
4. **Use tokens.** If a value is missing, add a semantic token to section 5 and `tokens.css` as part of the change. Never inline a one-off colour, radius, or spacing value.
5. **Use the catalogue.** Add keys to every locale catalogue, use interpolation and plurals, and do not concatenate sentences.
6. **Model every state.** Default, hover, active, focus, disabled, loading, error, empty, and dark mode.
7. **Be honest about data.** Do not invent a metric, a percentile, a time series, a customer, or a completion state. If the API does not provide it, show “Not available yet” or omit the surface.
8. **Respect the security rules.** Logs are plain text. Never use `v-html`. Never derive permissions from the JWT. Never design impersonation.
9. **Check the budget.** No unapproved dependency, no full icon import, no chart in the initial shell.
10. **Run the checklist.** Section 1.3 and section 16.9 are the definition of done.

If the guide is silent or contradicts the API, stop and add the question to [section 17.4](#174-open-questions-and-decision-log). Do not guess in code.

---

## 17. Appendices

### 17.1 Token quick reference

**Most-used semantic tokens**

```css
/* Surfaces */
--px-color-canvas
--px-color-surface
--px-color-surface-subtle
--px-color-surface-inverse

/* Text */
--px-color-text-primary
--px-color-text-secondary
--px-color-text-tertiary
--px-color-text-disabled
--px-color-text-inverse

/* Borders */
--px-color-border-subtle
--px-color-border-strong

/* Accent */
--px-color-accent
--px-color-accent-hover
--px-color-accent-subtle
--px-color-accent-contrast

/* State */
--px-color-success-fg
--px-color-warning-fg
--px-color-danger-fg
--px-color-info-fg
--px-color-focus

/* Spacing */
--px-space-1   /* 4px */
--px-space-2   /* 8px */
--px-space-3   /* 12px */
--px-space-4   /* 16px */
--px-space-5   /* 20px */
--px-space-6   /* 24px */
--px-space-8   /* 32px */

/* Shape */
--px-radius-control
--px-radius-card
--px-radius-dialog

/* Type */
--px-font-sans
--px-font-mono
--px-font-size-body
--px-font-size-small
--px-font-size-caption
--px-font-size-mono
--px-font-size-h1
--px-font-size-h2
--px-font-size-h3

/* Motion */
--px-duration-fast
--px-duration-normal
--px-duration-slow
--px-ease-out
```

**Rule:** if a component needs a value that is not in this list, use the nearest semantic token or propose a new one. Do not reach for a primitive.

### 17.2 Component state matrix

| Component | Default | Hover | Active | Focus | Disabled | Loading | Error | Empty |
|---|---|---|---|---|---|---|---|---|
| Button | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | — | — |
| Icon button | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | — | — |
| Input | ✓ | — | — | ✓ | ✓ | — | ✓ | — |
| Select | ✓ | ✓ | ✓ | ✓ | ✓ | — | ✓ | ✓ |
| Checkbox | ✓ | ✓ | ✓ | ✓ | ✓ | — | ✓ | — |
| Radio | ✓ | ✓ | ✓ | ✓ | ✓ | — | ✓ | — |
| Switch | ✓ | ✓ | ✓ | ✓ | ✓ | — | ✓ | — |
| Card | ✓ | ✓* | — | ✓* | — | ✓ | ✓ | ✓ |
| Dialog | ✓ | — | — | ✓ | — | ✓ | ✓ | — |
| Dropdown | ✓ | ✓ | ✓ | ✓ | ✓ | — | — | ✓ |
| Tabs | ✓ | ✓ | ✓ | ✓ | ✓ | — | — | — |
| Table | ✓ | ✓ | ✓ | ✓ | — | ✓ | ✓ | ✓ |
| Badge | ✓ | — | — | — | — | — | — | — |
| Tooltip | ✓ | — | — | ✓ | — | — | — | — |
| Toast | ✓ | ✓** | — | ✓** | — | — | ✓ | — |
| Empty state | ✓ | — | — | — | — | — | — | — |
| Skeleton | ✓ | — | — | — | — | — | — | — |

\* Only when the card is clickable.  
\*\* Pauses the dismiss timer; does not change colour.

### 17.3 API error to copy map

The UI maps `problem+json.code` to a message key. It never displays the raw code as the main message.

| Code | UI message | Placement |
|---|---|---|
| `INVALID_CREDENTIALS` | Email or password is incorrect. | Inline form error |
| `INVALID_REFRESH_TOKEN` | Your session has ended. Sign in again. | Full auth screen |
| `ACCOUNT_SUSPENDED` | This account is suspended. Contact the operator if you think this is a mistake. | Blocking message |
| `REAUTH_FAILED` | That password is incorrect. | Step-up password field |
| `CORS_REJECTED` | This request came from an origin Proxyma doesn't allow. | Generic error |
| `EMAIL_TAKEN` | An account with this email already exists. Sign in instead. | Email field + link |
| `VALIDATION_FAILED` | Check the highlighted fields and try again. | Form summary |
| `WEAK_PASSWORD` | Choose a stronger password. Use at least 10 characters and avoid common passwords. | Password field |
| `MALFORMED_REQUEST` | We couldn't read that request. Refresh the page and try again. | Generic error |
| `RATE_LIMITED` | Too many attempts. Try again in {seconds} seconds. | Inline form or banner |
| `UNAUTHORIZED` | Sign in to continue. | Full auth screen |
| `FORBIDDEN` | You don't have permission to do that. | Inline or page error |
| `NOT_FOUND` | This page doesn't exist or you don't have access to it. | Page error |
| `INVALID_CURSOR` | This list couldn't load more results. Refresh and try again. | Table footer |
| `INVALID_PARAMETER` | One of the filters is invalid. Check the values and try again. | Filter bar |
| `CONFLICT` | This changed while you were editing. Reload and try again. | Inline or dialog |
| `REFRESH_IN_PROGRESS` | No user-facing message; the client retries the refresh once. | — |
| `INTERNAL_ERROR` | Something went wrong on our side. Try again. If it keeps happening, contact the operator. | Page or panel error |
| `OWNER_PROTECTED` | The owner can't be changed or removed. | Role menu |
| `CO_OWNER_PROTECTED` | Only the owner can change or remove a co-owner. | Role menu |
| `SELF_ROLE_CHANGE` | You can't change your own role. | Role menu |
| `OWNER_CANNOT_LEAVE` | The owner can't leave the organisation. Delete the organisation instead. | Leave action |
| `ORG_ALREADY_OWNED` | You already own an organisation. Open it, or ask the operator for help. | Onboarding |
| `ALREADY_MEMBER` | This person is already a member. | Invite form |
| `INVITATION_EXPIRED` | This invitation has expired. Ask an owner or co-owner for a new one. | Invitation screen |
| `INVITATION_EMAIL_MISMATCH` | This invitation was sent to a different email address. | Invitation screen |
| `ALREADY_BANNED` | That IP is already banned. | Ban form, with link |
| `BAN_LIMIT_REACHED` | This organisation has reached the 10,000 active ban limit. Remove a ban before adding another. | Ban form |
| `ADMIN_CANNOT_TARGET_ADMIN` | Platform admins can't suspend or force-logout other platform admins. | Admin step-up |
| `CANNOT_SUSPEND_SELF` | You can't suspend your own account. | Admin step-up |
| HTTP `412` on config | Someone saved a newer version while you were editing. Review the changes or reload the latest. | Conflict dialog |
| HTTP `428` on config | Your editor is out of date. Reload the page and try again. | Page error |
| Unknown code | Something went wrong. Try again. | Generic error |

**Rules**

- Use the `Retry-After` header for `RATE_LIMITED`; do not count down from a guessed value.
- Use `problem+json.detail` only as a development diagnostic, not as the primary user message.
- If a field-level error includes a JSON Pointer or field name, map it to the control and focus the first invalid field.
- Do not show the same error in a toast and inline. Pick the closest context.

### 17.4 Open questions and decision log

**Open questions**

| # | Question | Current default | Owner |
|---|---|---|---|
| 1 | When will the official logo and favicon be available? | Text wordmark; favicon intentionally empty | Brand |
| 2 | What is the final latency-aggregate shape and release? | Design against `latency_ms: { p50, p95, p99 }`; do not render a time series until it exists | Backend |
| 3 | Is ownership transfer in scope? | No transfer UI; `OWNER_PROTECTED` explains the limit | Product |
| 4 | Is the homepage part of this SPA or a separate marketing site? | Design as a public route in this repo; decide before implementation | Product |
| 5 | Which locales follow `en-GB`? | `en-GB` and `pt-PT` ship; all Portuguese variants use `pt-PT` until a `pt-BR` catalogue exists | Product |
| 6 | Is email change supported? | Treat email as read-only until an endpoint exists | Backend |
| 7 | Is audit export in scope? | Read-only audit; no export control | Product |
| 8 | Should admin gain org-level controls beyond account suspension and force logout? | Account-only; do not invent org suspension | Product |
| 9 | Should the timezone be user-selectable as well as local? | Browser-local with a visible label | Product |

**Decision log**

| Date | Decision | Status |
|---|---|---|
| 2026-11-02 | Visual direction “Quiet Signal”: warm paper, ink, 1px rules, petrol-teal signal | Approved |
| 2026-11-02 | Primary buttons use ink, not teal; teal is reserved for interaction and live signal | Approved |
| 2026-11-02 | Reka UI for headless behaviour, custom Proxyma CSS, no Tailwind or pre-styled kit | Approved |
| 2026-11-02 | Lucide icons via `@lucide/vue`; Inter and JetBrains Mono self-hosted via Fontsource | Approved |
| 2026-11-02 | Unovis for charts; lazy-loaded | Approved |
| 2026-11-02 | Three-state theme: System / Light / Dark, persisted, pre-paint `data-theme` | Approved |
| 2026-11-02 | Default locale `en-GB`; translation-ready catalogue and error-key mapping | Approved |
| 2026-11-02 | Navigation: Overview, Routes, Traffic, Bans, Team, Audit, Settings; Admin separate | Approved |
| 2026-11-02 | Full data visualisation including traffic and latency charts | Approved |
| 2026-11-02 | All admin actions available on mobile, with full-screen step-up | Approved |
| 2026-11-02 | Timestamps in local timezone with a persistent timezone label | Approved |
| 2026-11-02 | Text wordmark placeholder; favicon empty until the official logo | Approved |
| 2026-11-02 | Homepage copy: “Always near, always here.” / “A reverse proxy you don't have to babysit.” | Approved |
| 2026-11-02 | Globe art is homepage-only, oxblood ink in light mode, inverted to warm light in dark mode with no plate | Approved |
| 2026-11-02 | Latency time series requires a `/logs/stats` aggregate extension | Blocked on backend |
| 2026-11-06 | Add `pt-PT`; detect browser language with a stored override; put the switcher in the public footer, auth card, and app sidebar | Approved |
| 2026-11-06 | Replace the top-level Account item with Settings; sections Account, General, and Security; keep preferences out of the sidebar footer | Approved |

### 17.5 Changelog

| Version | Date | Change |
|---|---|---|
| 1.0 | 2 November 2026 | Initial design system: brand, tokens, typography, layout, components, application patterns, data visualisation, interaction, responsive, accessibility, voice, implementation, appendices. |
| 1.1 | 2 November 2026 | Homepage: dark-mode globe inverts its ink to a warm light tone on the canvas; the paper plate is removed. |
| 1.2 | 6 November 2026 | Add the `pt-PT` catalogue, locale detection and persistence, and the language switcher in the public footer, auth card, and app sidebar. |
| 1.3 | 6 November 2026 | Replace the Account destination with Settings; move preferences into Settings → General and place Settings directly above Sign out in the sidebar footer. |
| 1.4 | 10 November 2026 | Align Sign out with the navigation item footprint in the sidebar footer (36px height, 14px label, 16px icon, 12px gutter); correct the documented navigation gutter from 8px to 12px. |
