---
name: Arete Performance OS
colors:
  surface: '#0f131c'
  surface-dim: '#0f131c'
  surface-bright: '#353942'
  surface-container-lowest: '#0a0e16'
  surface-container-low: '#181c24'
  surface-container: '#1c2028'
  surface-container-high: '#262a33'
  surface-container-highest: '#31353e'
  on-surface: '#dfe2ee'
  on-surface-variant: '#c3c6d7'
  inverse-surface: '#dfe2ee'
  inverse-on-surface: '#2c3039'
  outline: '#8d90a0'
  outline-variant: '#434655'
  surface-tint: '#b4c5ff'
  primary: '#b4c5ff'
  on-primary: '#002a78'
  primary-container: '#2563eb'
  on-primary-container: '#eeefff'
  inverse-primary: '#0053db'
  secondary: '#4edea3'
  on-secondary: '#003824'
  secondary-container: '#00a572'
  on-secondary-container: '#00311f'
  tertiary: '#ffb95f'
  on-tertiary: '#472a00'
  tertiary-container: '#996100'
  on-tertiary-container: '#ffeedd'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#dbe1ff'
  primary-fixed-dim: '#b4c5ff'
  on-primary-fixed: '#00174b'
  on-primary-fixed-variant: '#003ea8'
  secondary-fixed: '#6ffbbe'
  secondary-fixed-dim: '#4edea3'
  on-secondary-fixed: '#002113'
  on-secondary-fixed-variant: '#005236'
  tertiary-fixed: '#ffddb8'
  tertiary-fixed-dim: '#ffb95f'
  on-tertiary-fixed: '#2a1700'
  on-tertiary-fixed-variant: '#653e00'
  background: '#0f131c'
  on-background: '#dfe2ee'
  surface-variant: '#31353e'
typography:
  headline-xl:
    fontFamily: Space Grotesk
    fontSize: 36px
    fontWeight: '700'
    lineHeight: 40px
  headline-xl-mobile:
    fontFamily: Space Grotesk
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 32px
  headline-lg:
    fontFamily: Space Grotesk
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 28px
  headline-sm:
    fontFamily: Space Grotesk
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 22px
  metric-display:
    fontFamily: Space Grotesk
    fontSize: 30px
    fontWeight: '700'
    lineHeight: 32px
  metric-table:
    fontFamily: Hanken Grotesk
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 20px
  body-lg:
    fontFamily: Hanken Grotesk
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Hanken Grotesk
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-md:
    fontFamily: Hanken Grotesk
    fontSize: 13px
    fontWeight: '500'
    lineHeight: 16px
  label-sm:
    fontFamily: Hanken Grotesk
    fontSize: 11px
    fontWeight: '600'
    lineHeight: 14px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 0.75rem
  gutter-mobile: 0.5rem
  margin: 1rem
  margin-mobile: 1rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 0.75rem
  space-lg: 1rem
  space-xl: 1.5rem
---

## Brand & Style

This design system channels the precision, density, and clarity of modern European financial applications (such as Revolut, Monzo, and Linear) into high-performance strength training and biomechanics tracking.

The core target audience consists of dedicated lifters, strength athletes, and data-driven practitioners who value immediate feedback, tactile feedback during rest periods, and uncompromised legibility under heavy physical exertion. The UI rejects noisy gamification, skeuomorphic gym cliches, and hyper-saturated gradients in favor of an editorial, analytical tone: deep contrast, razor-sharp metric hierarchies, crisp status indicators, and clean hairline dividers.

The aesthetic blends **Modern Grotesque Editorial** with **Fintech Functionalism**:
- High-contrast monochromatic foundations allowing the vivid electric blue accent to direct motor action.
- Dense, tabular data presentation for sets, repetitions, and RIR (Reps in Reserve) optimized for rapid single-thumb inputs.
- Micro-visualizations (sparklines, segmented streak trackers, rest-interval dials) integrated directly into metric surfaces.
- Spanish language interface utilizing compact, active phrasing (`Iniciar`, `Añadir serie`, `Completado`).

## Colors

The palette operates under a strict hierarchy designed for low visual fatigue in gym lighting and instant recognition during live sets.

- **Primary (`#2563EB` / `#3B82F6`):** The catalytic electric blue. Reserved exclusively for primary actions (`Comenzar entrenamiento`, `Siguiente ejercicio`), active set checkmarks, primary active tabs, and current progress metrics.
- **Secondary (`#10B981` / `#059669`):** Performance green. Denotes positive delta progression, personal records (`PR`), completed adherence dots, and affirmative delta pill badges (`+2,5 kg`, `↑ 12%`).
- **Tertiary (`#F59E0B`):** Streak/warmth accent. Used selectively for active streaks (`Racha actual`), consistency badges, and critical warning thresholds.
- **Neutrals & Surfaces:**
  - **Dark Foundations (Default):** Deep slate canvas (`#080C14`), elevated card surface (`#111827`), secondary interactive surface (`#1F2937`), subtle hairline border (`rgba(255, 255, 255, 0.08)`).
  - **Light Foundations (Supported):** Pure canvas (`#F8FAFC`), crisp white surface cards (`#FFFFFF`), light input wells (`#F1F5F9`), subtle border lines (`#E2E8F0`).
- **Text & Data Contrast:** Primary metrics use pure white (`#FFFFFF`) in dark mode or pure pitch slate (`#0F172A`) in light mode; secondary metric labels and subheaders adhere strictly to muted slate tones (`#94A3B8` / `#64748B`) to keep visual weight on the recorded numbers.

## Typography

The typographical framework balances mechanical legibility with technical elegance:

- **Space Grotesk** governs all numbers, metrics, large titles, and PR notifications. Its tabular figures and subtle geometric quirks provide an analytical, instrument-panel feel reminiscent of financial terminals.
- **Hanken Grotesk** drives the interface copy, table column headers, labels, and micro-copy. It delivers crisp clarity at 11px to 14px sizes without visual distortion on high-density OLED Android panels.
- Numbers within tables and metrics must always enforce tabular proportional figures (`tnum`) to eliminate jitter during counter animations, stopwatch progression, or live rep entry.

## Layout & Spacing

The layout philosophy prioritizes compact visual rhythm and ergonomic thumb-reach on mobile devices:

- **Grid & Margins:** Built on a standardized 4px/8px modular base. Edge margins are locked at `16px` (`margin-mobile`), ensuring maximal horizontal room for multi-column set tracking tables (`Serie`, `Kg`, `Reps`, `RIR`, `Status`).
- **Gutter Rhythm:** Internal gutters are kept compact (`8px` to `12px`) between sibling metric modules to facilitate dense, dashboard-style card pairings (e.g., side-by-side 1RM and Total Volume cards).
- **Thumb Zone Structuring:** Primary operational triggers—including the rest timer widget, set completion triggers, and `Añadir serie` actions—are concentrated in the bottom 45% of the viewport.
- **Vertical Hierarchy:** Sections are partitioned by generous `24px` spacing intervals or subtle 1px dividers rather than nested, heavy containers.

## Elevation & Depth

This system avoids heavy drop shadows, fuzzy blurs, and skeuomorphic layered cards:

- **Tonal Layering:** Visual elevation is created through tonal background progression. Canvas begins at `#080C14`, cards step up to `#111827`, and interactive input cells or secondary controls sit at `#1F2937`.
- **Low-Contrast Outlines:** Card boundaries and interactive elements use thin, precise borders (`1px solid rgba(255, 255, 255, 0.08)` in dark mode; `1px solid #E2E8F0` in light mode).
- **Glow Accents:** Diffused lighting is strictly confined to active primary actions (e.g., an electric blue glow `0 8px 24px rgba(37, 99, 235, 0.25)` under the primary CTA button or live rest stopwatch).
- **Surface Translucency:** Top application bars and bottom navigation bars utilize frosted structural overlays (`backdrop-filter: blur(16px)` with `rgba(8, 12, 20, 0.85)`) to preserve continuous content flow while scrolling.

## Shapes

The shape system adopts modern architectural rounded corners that balance friendly utility with precision engineering:

- **Base Radii (`roundedness: 2`):** Primary cards, list cells, and interactive table rows use `12px` to `16px` (`rounded-lg`).
- **Pill Badges:** Delta tags (`+2,5 kg`), segmented tab indicators, workout category chips, and week streak day indicators use fully rounded caps (`9999px` / pill format).
- **Data Input Wells:** Individual tabular input cells for weight (`Kg`) and repetitions (`Reps`) use a slightly tightened radius (`8px` to `10px`) to maximize typing target areas while fitting seamlessly within dense rows.
- **Checkmark Action Nodes:** Circular geometry (`28px x 28px` to `32px x 32px`) provides high tactile contrast against rectangular table rows.

## Components

### Action Buttons
- **Primary CTA (`Comenzar entrenamiento`, `Siguiente ejercicio`):** Full-width or prominent high-contrast buttons in electric blue (`#2563EB`) with white typography (`font-weight: 600`), 48px to 52px height for rapid palm/thumb contact, rounded corners (`12px` to `16px`), accompanied by concise vector iconography (e.g., play symbol or right chevron).
- **Secondary Action (`+ Añadir serie`):** Transparent surface with subtle tinted border and primary blue text or soft filled surface (`rgba(37, 99, 235, 0.1)`), providing a quiet yet identifiable add-state.

### Sets & Reps Table (Core Workout Row)
- Consists of clean horizontal bands with minimal line separators:
  - **Index (`Serie`):** Small, muted numerical indicator (`1`, `2`, `3`).
  - **Inputs (`Kg`, `Reps`, `RIR`):** Contained rounded input boxes with centered tabular numerals, subtle background (`#1F2937`), and instantaneous numeric keypad invocation.
  - **Completion Button:** Circular checkbox transitioning from an open muted border ring to solid electric blue with a sharp white checkmark upon completion.

### Status Pills & Sparkline Metric Cards
- **Metric Cards:** Compact rectangular tiles with an upper eyebrow label, large Space Grotesk headline number (`102,5 kg`), followed by an embedded positive trend pill badge (`+12%`, `#10B981` text over `rgba(16, 185, 129, 0.12)` background).
- **Sparklines:** Monoline SVG curves in electric blue without bulky gradients, carrying terminal node dots indicating the latest session data point.

### Rest Interval Timer
- Integrated dynamic bar or floating pill displaying remaining rest seconds (`02:00`), featuring a circular play/pause button and quick increment chips (`+30s`).

### Navigation & Segmented Switches
- Segmented sub-header filter (`Resumen`, `Fuerza`, `Volumen`, `Medidas`) enclosed within an integrated pill-shaped rail with sliding contrast pill backgrounds.
- Bottom navigation bar: 4 items max (`Inicio`, `Entrenamiento`, `Progreso`, `Más`), paired with clean, monoline line-art icons and active color switching to electric blue.
