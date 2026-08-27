# System Architecture & PRD Specification: Quire

**Document version:** 1.2
**Target platform:** Android (optimised for Onyx Boox / e-ink OS)
**Core strategy:** system-level `TextToSpeechService` integration
**Reference device:** Onyx Boox Note Air5 C — see [`device-profile.md`](device-profile.md)

> **v1.2 supersedes v1.1.** v1.1 described a standalone Readium e-reader that also spoke.
> v1.2 inverts that: Quire is a system TTS engine that sits *beneath* the reader the user
> already has, and the standalone reader moves to V3.0. See §6 for what this changed.

---

## 1. Executive summary

**Quire** functions as an Android system-level `TextToSpeechService`. Instead of requiring
users to abandon their preferred reading software, Quire sits beneath native e-reader
applications (such as Onyx NeoReader, Moon+ Reader, or Librera).

When a user initiates the e-reader's native "Read Aloud" function, Quire intercepts the
text stream, matches sentences against a pre-indexed character dialogue map, and
dynamically routes each line to its corresponding AI character voice in real time.

---

## 2. System architecture & operational workflow

```
┌────────────────────────────────────────────────────────┐
│                   PHASE 1: PRE-INDEXING                │
│  [EPUB File] ──► [Quire Companion App]                 │
│                         │                              │
│                         ▼                              │
│             [1B SLM Parsing Engine]                    │
│                         │                              │
│           ┌─────────────┴─────────────┐                │
│           ▼                           ▼                │
│   [characters.json]         [dialogue_index.db]        │
└────────────────────────────────────────────────────────┘
                            │
┌───────────────────────────┼────────────────────────────┐
│                           ▼                            │
│                 PHASE 2: LIVE INTERCEPTION             │
│  [Onyx NeoReader] ──► Hits Native "Read Aloud"         │
│                               │                        │
│                               ▼                        │
│                  [Android TextToSpeech API]            │
│                               │                        │
│                               ▼                        │
│                  [Quire System TTS Service]            │
│                               │                        │
│                        (Hash Lookup)                   │
│                               │                        │
│            ┌──────────────────┴──────────────────┐     │
│            ▼                                     ▼     │
│     [Match Found]                        [No Match]    │
│  (Synthesize Speaker Voice)          (Default Narrator)│
│            │                                     │     │
│            └──────────────────┬──────────────────┘     │
│                               ▼                        │
│                     [ONNX Ring Buffer TTS]             │
│                               │                        │
│                               ▼                        │
│                        [Audio Output]                  │
└────────────────────────────────────────────────────────┘
```

### Phase 1: one-time indexing (Quire companion app)

1. **Import:** the user selects an EPUB in the Quire companion app.
2. **Character manifest:** a quantized local 1B SLM (e.g. *Llama 3.2 1B* via ExecuTorch)
   scans the book to generate a `characters.json` roster (names, age, gender, voice
   profiles).
3. **Dialogue hash table:** Quire parses the book's text, creating a lightweight SQLite
   index (`dialogue_index.db`) mapping sanitised text strings to character IDs:
   - `hash("I don't think that's wise") -> { speaker_id: "Julian", confidence: 0.95 }`

### Phase 2: live synthesis interception (native e-reader)

1. **Trigger:** the user opens their book in **Onyx NeoReader** and taps the built-in
   **Read Aloud** button.
2. **Interception:** NeoReader routes text chunks to Android's default system TTS engine.
   Quire receives the raw text string inside its extended `TextToSpeechService` class.
3. **Lookup & routing:** Quire hashes the incoming string and queries `dialogue_index.db`:
   - **Match (≥ 0.65 confidence):** routed to the assigned character voice.
   - **Low confidence (0.40–0.64):** routed to the most active speaker in the current
     scene context.
   - **No match / narrative text (< 0.40):** routed to the default Narrator voice.
4. **Audio output:** the assigned ONNX voice model (Kokoro-TTS / Piper) synthesises audio
   through a dynamic 3-paragraph rolling buffer and plays it back directly.

---

## 3. E-reader compatibility matrix

| Tier | Supported applications | Integration method | User experience |
| --- | --- | --- | --- |
| **Tier 1 (fully supported)** | **Onyx NeoReader**, Moon+ Reader, Librera Reader, Google Play Books, eReader Prestigio | Native Android `TextToSpeechService` API | **Seamless.** The native "Read Aloud" button triggers Quire multi-voice audio automatically. |
| **Tier 2 (walled gardens)** | Amazon Kindle app, Kobo Android app | Accessibility service / screen overlay *(post-V1)* | Requires background accessibility permissions to capture on-screen text. |
| **Tier 3 (standalone platforms)** | KOReader (e-ink Linux / Android) | Share intent or custom KOReader plugin hook | Works natively on KOReader Android builds via system TTS. |

---

## 4. Technical stack & hardware SLAs

- **OS level:** Android 10+ (system TTS service registration)
- **Local inference engine:** ExecuTorch / `llama.cpp` (quantized 1B SLM for parsing)
- **Local TTS engine:** ONNX Runtime Mobile running Kokoro-TTS (82M) or Piper C++

| Metric | Target SLA |
| --- | --- |
| Peak memory footprint | ≤ 1.2 GB RAM |
| Base app footprint | ≤ 450 MB (quantized SLM + base voice models) |
| Time to first sound (TTFS) | < 800 ms upon pressing Play |
| Real-time factor (RTF) † | ≤ 0.15 |
| Battery impact † | < 8% drain per hour of continuous playback |

† Carried over from v1.1. v1.2 does not restate these two; they are retained here because
nothing about the new architecture makes them less binding, and the ≈1.14 W device budget
in `device-profile.md` §4 still applies. **Confirm or drop them explicitly.**

---

## 5. Development roadmap

### Version 1.0 (MVP scope)
- Quire companion app for EPUB import, character manifest creation, and voice library
  assignment.
- System-level Android `TextToSpeechService` implementation.
- Hash-matching dialogue lookup engine with Narrator fallback.
- Rolling audio ring buffer for instantaneous playback start.

### Version 2.0 (enhanced performance)
- Real-time emotional performance tags (`whispering`, `shouting`, `weary`) passed to the
  TTS driver.
- Optional cloud synthesis toggle (ElevenLabs / OpenAI Voice API) for high-fidelity
  playback when connected.
- Manual voice-casting drawer to override character-voice mappings.

### Version 3.0 (standalone fallback)
- Dedicated built-in Readium e-reader inside Quire for zero-setup reading and guaranteed
  word-level text highlighting.
- Accessibility-scraping fallback mode for locked apps like Kindle.

---

## 6. What v1.2 changed from v1.1

| Area | v1.1 | v1.2 |
| --- | --- | --- |
| Product shape | Standalone Readium e-reader with built-in playback | System TTS engine under the user's existing reader |
| Reader UI | Core V1 scope | Moved to V3.0 |
| Playback transport | Owned by Quire | Owned by the host reader app |
| Text highlighting | Core V1 feature, driven by TTS boundary timestamps | Host reader's responsibility; Quire can feed it via `rangeStart` |
| Position tracking | Quire's own progress store | Host reader owns position; Quire has no locator |
| Attribution timing | Chapter-ahead during reading | Entirely at index time, in the companion app |
| Character drawer | V1 §4.3 | Moved to V2.0 |
| SLM and TTS co-residency | The central memory problem | Largely dissolved — they now run in different processes at different times |

The attribution and synthesis work (`characters.json`, the tier system, voice casting, the
ring buffer) survives intact. What changed is where it runs and what consumes it.
