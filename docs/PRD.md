# Product Requirement Document — Quire

> Status: v1 baseline, captured 2026-08-27. Changes to this document are made by ticket
> and reviewed like code.

To give the ideal experience — instant playback, zero battery drain, and offline
reliability on e-ink hardware — the recommended tech stack combines a **1B Small
Language Model (SLM)** with an **ONNX-based TTS engine**. This setup requires under
**1.2 GB of RAM**, leaves a tiny storage footprint, and processes text in milliseconds
on mid-range Android e-ink hardware.

---

## 1. Product overview

- **Product name:** Quire
- **Tagline:** The tactile, multi-voice e-reader.
- **Core value proposition:** Transforms standard EPUB books into multi-character audio
  experiences by using local AI to analyse dialogue, extract character traits, and
  dynamically assign distinct voices — all while highlighting synced text on e-ink and
  standard displays.

---

## 2. Platform & hardware strategy

- **Target OS:** Android only. iOS is **out of scope** (decided 2026-08-27); the PRD's
  original cross-platform ambition is dropped rather than deferred.
- **Reference device:** Onyx Boox Note Air5 C — see [`device-profile.md`](device-profile.md).
  All SLAs are measured there. Standard (non e-ink) Android is supported but not tuned for.
- **Core framework:** Open-source **Readium Engine** (EPUB rendering, DOM handling,
  pagination, bookmarking).
- **Display optimisation:**
  - Low-refresh monochrome UI mode (pure black/white `#000000` / `#FFFFFF` assets).
  - Reduced DOM animations to eliminate e-ink ghosting.
  - Native support for hardware page-turn and volume keys.

---

## 3. System architecture & AI pipeline

```
[EPUB File] ──► [Readium Parser] ──► [Hybrid Attribution Engine]
                                                │
                                                ▼
[Audio Playback] ◄── [Ring Buffer] ◄── [ONNX TTS Engine] ◄── [Voice Allocation]
```

### 3.1 Dialogue attribution & character roster

1. **Upfront book scan (manifest generation):**
   - On EPUB import, a quantized 1B SLM (e.g. *Llama 3.2 1B* / *Qwen 2.5 1.5B* running
     via `llama.cpp` / `ExecuTorch`) scans the text to extract character names,
     estimated gender, age, and personality traits.
   - Outputs a structured `characters.json` manifest.
2. **Hybrid line attribution:**
   - **Tier 1 (heuristics):** regex maps explicit quotes (*"I know," said Sarah*).
   - **Tier 2 (SLM context):** for unattributed dialogue, the 1B SLM analyses a 5-line
     sliding context window.
   - **Tier 3 (confidence fallback):** if SLM confidence is `< 0.65`, the system makes an
     educated guess from active speakers in the scene. If confidence remains `< 0.40`,
     it defaults to the **Narrator** voice.

### 3.2 Audio synthesis & buffering (Option A)

- **Engine:** **Kokoro-TTS** (82M parameter ONNX model) or **Piper C++**.
- **Generation strategy:** rolling dynamic ring buffer.
  - Pre-synthesises audio for the current paragraph plus **3 paragraphs ahead**.
  - Audio chunks are stored as temporary `.wav` cache and discarded after playback, to
    keep disk and memory footprints minimal.
- **Latency SLA:** time to first sound (TTFS) `< 800 ms` on pressing Play.

---

## 4. Functional requirements

### 4.1 Reading & text rendering
- EPUB parsing, font face/size adjustment, line height, margins, and
  dark/light/e-ink high-contrast modes.
- Progress tracking, chapter navigation, and offline position syncing.

### 4.2 Multi-voice playback
- **Automatic voice casting:** maps character profiles from `characters.json` to
  built-in local TTS voice variants without requiring user setup.
- **Sentence-level highlighting:** emits word/sentence boundary timestamps during
  synthesis to highlight the active reading line in real time.
- **Playback controls:** play/pause, ±10 s skip, playback speed slider (0.8×–2.0×), and
  a "Narrator Only" toggle.

### 4.3 Character & voice drawer (optional user override)
- A minimalist slide-out panel displaying detected book characters and their assigned
  voices.
- Allows users to manually swap a character's voice pitch/tone if auto-assignment isn't
  to their liking.

---

## 5. Non-functional requirements

| Metric | Target SLA |
| --- | --- |
| Peak RAM usage | ≤ 1.2 GB (SLM + TTS + e-reader UI combined) |
| App footprint | ≤ 450 MB (includes base quantized models) |
| Real-time factor (RTF) | ≤ 0.15 (synthesises 10 s of audio in < 1.5 s on mid-tier ARM SoC) |
| Battery impact | < 8% drain per hour of continuous multi-voice playback |

---

## 6. Future scope (version 2)

- **Real-time performance tags:** injecting emotion markers (`whispering`, `excited`,
  `weary`) into the TTS stream per line.
- **Cloud voice synthesis:** optional toggle for ElevenLabs or OpenAI Voice APIs for
  premium cloud fidelity.
- **Voice cloning:** allowing users to record short voice clips to serve as custom
  narrators or characters.
