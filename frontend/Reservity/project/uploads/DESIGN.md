---
name: Academic Commons
colors:
  surface: '#fbf8fe'
  surface-dim: '#dcd9de'
  surface-bright: '#fbf8fe'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f6f2f8'
  surface-container: '#f0edf2'
  surface-container-high: '#eae7ed'
  surface-container-highest: '#e4e1e7'
  on-surface: '#1b1b1f'
  on-surface-variant: '#464651'
  inverse-surface: '#303034'
  inverse-on-surface: '#f3f0f5'
  outline: '#767683'
  outline-variant: '#c7c5d3'
  surface-tint: '#4f56ab'
  primary: '#4b52a7'
  on-primary: '#ffffff'
  primary-container: '#646bc2'
  on-primary-container: '#fcf9ff'
  inverse-primary: '#bec2ff'
  secondary: '#9f4200'
  on-secondary: '#ffffff'
  secondary-container: '#fd803a'
  on-secondary-container: '#642600'
  tertiary: '#505498'
  on-tertiary: '#ffffff'
  tertiary-container: '#696db2'
  on-tertiary-container: '#fcf9ff'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#e0e0ff'
  primary-fixed-dim: '#bec2ff'
  on-primary-fixed: '#020667'
  on-primary-fixed-variant: '#373d92'
  secondary-fixed: '#ffdbcb'
  secondary-fixed-dim: '#ffb692'
  on-secondary-fixed: '#341100'
  on-secondary-fixed-variant: '#793000'
  tertiary-fixed: '#e0e0ff'
  tertiary-fixed-dim: '#bfc2ff'
  on-tertiary-fixed: '#0e1055'
  on-tertiary-fixed-variant: '#3c4082'
  background: '#fbf8fe'
  on-background: '#1b1b1f'
  surface-variant: '#e4e1e7'
typography:
  headline-xl:
    fontFamily: Lexend
    fontSize: 40px
    fontWeight: '700'
    lineHeight: '1.2'
  headline-lg:
    fontFamily: Lexend
    fontSize: 32px
    fontWeight: '600'
    lineHeight: '1.2'
  headline-md:
    fontFamily: Lexend
    fontSize: 24px
    fontWeight: '600'
    lineHeight: '1.3'
  body-lg:
    fontFamily: Work Sans
    fontSize: 18px
    fontWeight: '400'
    lineHeight: '1.6'
  body-md:
    fontFamily: Work Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: '1.6'
  label-md:
    fontFamily: Work Sans
    fontSize: 14px
    fontWeight: '600'
    lineHeight: '1.4'
    letterSpacing: 0.02em
  caption:
    fontFamily: Work Sans
    fontSize: 12px
    fontWeight: '500'
    lineHeight: '1.4'
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base-unit: 8px
  container-max: 1280px
  gutter: 24px
  margin-mobile: 16px
  margin-desktop: 40px
---

## Brand & Style

This design system balances academic rigor with student-focused energy. The brand personality is **Intellectual yet Energetic**, designed to feel like a high-end digital campus rather than a dry administrative tool. 

The aesthetic follows a **Modern Corporate** foundation—utilizing structured grids and clear hierarchy—infused with **Soft Minimalism**. By prioritizing generous white space and purposeful color accents, the UI reduces cognitive load for students. The interaction model is approachable, using friendly corner radii and vibrant accents to break the "institutional" feel of traditional education platforms.

## Colors

The palette is anchored by **Digital Indigo (#646BC2)**, a sophisticated purple that communicates stability and intelligence. To differentiate from traditional academic software, all success states, secondary actions, and motivational badges utilize **Vibrant Orange (#FF823C)**. This specific orange replaces all green accents to provide a warm, friendly, and high-energy contrast that feels active rather than passive.

- **Primary Purple:** Used for navigation, primary buttons, and key brand moments.
- **Vibrant Orange:** Used for success feedback, completion badges, and "next step" calls to action.
- **Neutrals:** A range of cool grays ensure the purple doesn't feel overly heavy, maintaining a fresh and clean interface.

## Typography

The system uses **Lexend** for headlines. Its history in reading proficiency and accessibility makes it ideal for an educational context, while its geometric, open shapes feel modern and friendly to teens. 

For body text and labels, **Work Sans** provides a neutral, reliable foundation that ensures long-form study materials remain highly legible. Use tighter tracking on Lexend headlines for a punchier, modern look, while keeping Work Sans body text at default spacing for maximum clarity.

## Layout & Spacing

The layout utilizes a **Fixed Grid** model for desktop to ensure content readability and a **Fluid Grid** for mobile devices. 

- **Grid:** A 12-column grid is standard for desktop views.
- **Rhythm:** An 8px base unit governs all padding and margins. 
- **Content Density:** Elements should be spaced generously to prevent the UI from feeling cluttered. Large 32px or 40px margins between major sections help students focus on one task at a time.

## Elevation & Depth

This system uses **Tonal Layers** combined with **Ambient Shadows** to create a sense of organized hierarchy. 

- **Base Layer:** The main background is a very light lavender-tinted white (#FAFAFD).
- **Surface Layer:** White cards (#FFFFFF) sit on the base with a subtle 1px border (#E2E4F0) or a soft, diffused shadow.
- **Interaction Depth:** Elements like primary buttons and active cards use a subtle "lift" effect—increasing shadow spread on hover to signal interactivity. 
- **Overlays:** Modals and menus use a soft backdrop blur to maintain context while focusing the user's attention.

## Shapes

The shape language is **Rounded**, utilizing a 0.5rem (8px) standard radius. This softens the professional layout, making the platform feel approachable and less "industrial." 

- **Standard Elements:** 8px radius for buttons, input fields, and cards.
- **Large Elements:** 16px radius for containers or featured promotional banners.
- **Pills:** Full-round (pill) shapes are reserved exclusively for badges (e.g., status labels, tags) to distinguish them from actionable buttons.

## Components

- **Buttons:** Primary buttons are Solid Purple with White text. Secondary buttons use a Ghost style with a Purple outline. "Success" or "Action Complete" buttons transition to Solid Orange.
- **Chips & Badges:** Use the pill shape. For status tracking, "Completed" or "Verified" badges must use Orange backgrounds with white or dark-orange text, never green.
- **Input Fields:** Use a 1px border in a mid-tone neutral. On focus, the border thickens and changes to Brand Purple with a subtle purple glow.
- **Cards:** Utilize the "Rounded" radius (8px). Header areas within cards can use a subtle light-purple tint to separate navigation from content.
- **Progress Bars:** Use a Purple track with a Vibrant Orange progress fill to visualize achievement and movement.
- **Academic Specifics:** Task lists should feature Orange checkboxes when marked complete to reinforce the friendly, high-energy success state.