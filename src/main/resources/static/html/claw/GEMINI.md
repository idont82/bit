# 서울 인형뽑기 성지 가이드 (Seoul Claw Machine Sanctuary Guide)

## Project Overview
This project is a web-based guide to popular claw machine locations in Seoul, featuring an interactive 3D claw machine simulation. It serves as both a directory for enthusiasts and a casual gaming platform.

### Main Technologies
- **Frontend:** HTML5, CSS3 (Vanilla), JavaScript (ES6+)
- **3D Graphics:** [Three.js](https://threejs.org/) (used in the claw machine game)
- **Data:** Static JSON files (`data/*.json`)
- **Integration:** Coupang Partners ad integration

### Architecture
- **Single Page Application (SPA) Style:** Uses a hash-based router (`location.hash`) in `index.html` to toggle between a list view of Seoul areas and detailed information for each area.
- **Data-Driven:** All area and spot information is stored in `data/`, making it easy to update or add new locations without modifying the core HTML/JS.
- **Standalone Game:** The 3D claw machine (`claw-machine.html`) is a standalone experience linked from the main guide.

---

## Directory Structure
- `index.html`: The main entry point and guide interface.
- `claw-machine.html`: The 3D claw machine game page.
- `claw-machine.js`: Core logic for the 3D game, including physics, rendering, and character themes.
- `claw-machine.css`: Styling specifically for the claw machine game.
- `data/`:
    - `areas.json`: Master list of Seoul areas (Hongdae, Gangnam, etc.).
    - `{area_id}.json`: Detailed spot information, maps, and route steps for a specific area.

---

## Building and Running
This is a static web project and does not require a complex build process.

### Commands
- **Running:** Open `index.html` in any modern web browser.
- **Development Server:** It is recommended to use a local web server (e.g., VS Code's "Live Server" or `npx serve`) to handle `fetch` requests for JSON data properly (avoiding CORS issues from `file://` protocol).

---

## Development Conventions

### Data Management
- When adding a new area:
    1. Add the area metadata to `data/areas.json`.
    2. Create a corresponding `{id}.json` file in `data/` with the detailed spot list and routes.
- The UI automatically handles "coming soon" states for areas with `spotCount: 0`.

### 3D Game (`claw-machine.js`)
- The game uses **Three.js** for rendering and a custom physics implementation for the pendulum movement and claw mechanics.
- **State Machine:** Uses a `State` object to manage transitions between `IDLE`, `MOVING`, `DROPPING`, `GRABBING`, `RISING`, `RETURNING`, and `RESULT`.
- **Character Themes:** Character models (Shin-chan, Cinnamoroll, Dino, etc.) are procedurally generated using Three.js primitives to minimize external asset dependencies.
- **Persistence:** Player nicknames and collected plushies are saved in `localStorage`.

### Styling
- **Global Styles:** Defined within `<style>` tags in `index.html`. Uses a "dark mode" aesthetic with neon/cyberpunk accents.
- **Claw Machine CSS:** Located in `claw-machine.css`, focusing on the arcade-like UI overlay.

---

## Key Files for Investigation
- `index.html`: Main UI and routing logic.
- `claw-machine.js`: Complex 3D game logic and procedural modeling.
- `data/areas.json`: Central data registry.
