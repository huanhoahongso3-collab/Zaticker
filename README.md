# 📦 Zaticker — Sticker Experiments for Zalo

Zaticker is a vibe-driven coding project focused on how Zalo handles sticker share by Intent in Android.

It is inspired by how Zamoji - a VNG app can send sticker to Zalo
Released under **GPLv2** to keep it open.

---

## Project Goals

- Allow users to test custom sticker packs with Zalo
- Log successes and failures during sticker sending

---

## Status Overview

> **STATUS:** Functional. Usable for experimentation. 

Working features include:

- Share intents (`ACTION_SEND` / `ACTION_SEND_MULTIPLE`)
- Sticker press and preview interactions


---

## Technical Overview

### Android Share Layer

Zaticker integrates with the Android share pipeline to pass sticker assets to Zalo.

Mechanisms involved:

- `Intent.ACTION_SEND`
- `Intent.ACTION_SEND_MULTIPLE`
- MIME types used:
  - `image/webp`
  - `image/png`
- Temporary URI sharing via ContentProvider

Asset formats used during testing include:

- JPG
- PNG
- WEBP
---

## License
Zaticker Copyright (c) 2026
Licensed under the GNU General Public License v2.0 (GPL-2.0)
