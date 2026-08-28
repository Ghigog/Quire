# Prior art

Surveyed 2026-08-28, before committing to the M0b build. The question was whether anyone
has already built Quire, and whether anything out there should change our design.

Short answer: **the attribution-and-casting half is well-trodden; the delivery mechanism
is not.** And the research literature has a result that should probably change our
attribution plan.

---

## 1. What exists

### Batch generators — desktop, offline, produce audio files

The busiest category. You feed in an EPUB, wait, and get a multi-voice audiobook out.

| Project | Approach |
| --- | --- |
| [VoxNovel](https://github.com/DrewThomasson/VoxNovel) | BookNLP for quotation attribution, Coqui TTS for voices |
| [audiobook-creator](https://github.com/prakharsr/audiobook-creator) | LLM character identification, Kokoro / Orpheus TTS, emotion tags |
| [audiobook-creator-v2](https://github.com/chriswritescode-dev/audiobook-creator-v2) | NER + clustering; **minor characters folded into the narrator to cut LLM cost** |
| [alexandria-audiobook](https://github.com/Finrandojin/alexandria-audiobook) | LLM script annotation, voice cloning, per-line style, M4B export |
| [ebook2audiobook](https://github.com/DrewThomasson/ebook2audiobook) | Many formats, 1158 languages, voice cloning |

These validate the *concept* thoroughly. None of them runs on a device, and none of them
plays into a reader you already use — they produce a file you then listen to elsewhere.

### Reader apps with character voices — the closest competitors

- **[@Voice Aloud Reader](https://play.google.com/store/apps/details?id=com.hyperionics.avar)**
  (Android) assigns different voices to narrator and characters, with a "dialog-detection
  wizard" that analyses quote styles and sets up *alternating* voices, plus named voice
  groups and per-character engine/rate/pitch. Attribution is quote-alternation heuristics,
  not character identification — it does not know *who* is speaking, only that the speaker
  changed.
- **[AudiFlo](https://audiflo.app/epub-tts-android)** (Android) parses EPUB and does
  "multi-character dialogue casting with 100% offline neural speech models". The closest
  thing to Quire's feature set that we found.

Both are **reader apps**: to use them you switch to their reader. That is precisely the
cost PRD v1.2 exists to avoid.

### Cloud

**[ElevenReader](https://elevenlabs.io/docs/eleven-creative/products/audiobooks)** has
"Multi cast" and Character Casting — it detects characters, proposes a voice for each, and
previews on real dialogue from the book. Excellent quality, cloud-dependent,
subscription. Our V2 cloud toggle would be competing with this directly; our V1 is not.

---

## 2. Where Quire is actually different

Not the multi-voice idea, and not on-device synthesis — AudiFlo claims that too.

**The interception architecture.** Registering as a system `TextToSpeechService` so that
multi-voice audio comes out of NeoReader, Moon+ or Librera — the reader the user already
has, with its own controls, its own library, its own highlighting — appears to be
unoccupied. Everyone else either makes you switch readers or makes you wait for a file.

Verified viable on the reference device: ADR-0004.

Two supporting differences: e-ink as a first-class target rather than an afterthought, and
character identification (who is speaking) rather than speaker-change detection (that
someone else is speaking), which is what @Voice does.

---

## 3. What we should take

### PDNC — the labelled data we are owed

The **[Project Dialogism Novel Corpus](https://arxiv.org/pdf/2204.05836)** is 35,978
manually annotated quotations across 22 full-length English novels, labelled with speaker,
addressee, quotation type, referring expression and mentions.

QUI-018's acceptance criteria ask for three hand-labelled chapters and we currently have
three hand-*written* passages, which the ticket honestly flags as a weak accuracy estimate.
PDNC is that gap closed several hundred times over, and it is the benchmark the whole field
reports against, so our numbers would be comparable to published ones.

**Check its licence before use**, and note that it is derived from public-domain novels but
the annotations have their own terms.

### An encoder model may beat the 1B SLM at attribution — and this is the big one

The architecture assumes a quantized 1B SLM does Tier 2 attribution, and
`device-profile.md` §2 works out that this lands in the *hours* on a Snapdragon 750G with
no i8mm. That is our worst constraint.

The literature says we may not need the SLM for this at all:

| Approach | PDNC accuracy | Speed |
| --- | --- | --- |
| [BookNLP](https://github.com/booknlp/booknlp) (BERT-based) | ~63% | fast |
| [Encoder with joint scoring](https://arxiv.org/abs/2608.02359) | **94.5%** | **20× faster than standard methods, >1000× faster than LLM approaches** |
| LLM approaches ([LLaMa3 study](https://aclanthology.org/2025.naacl-short.62/)) | strong | "computational cost limits use" |

An encoder in the BERT-base class is roughly 110M parameters — an order of magnitude below
a 1B SLM, and the kind of model ONNX Runtime Mobile already runs well. Being purpose-built
for quotation attribution, it is also *more* accurate than a general model at this one job.

If that transfers to our hardware, it collapses the throughput problem that shaped
`architecture.md` §5, and QUI-007's 30-minute indexing budget stops being tight.

It does not remove the SLM entirely: the **character manifest** (names, aliases, gender,
age band, personality traits, for casting) is a different task that an attribution encoder
does not do. But it would move the SLM off the hot path and onto a much smaller job.

Filed as **QUI-028**.

### Smaller things worth stealing

- audiobook-creator-v2 folds minor characters into the narrator to cut model cost. That is
  QUI-007's "discard candidates appearing fewer than 3 times" rule, independently arrived
  at, and a good sign it is the right instinct.
- BookNLP scores quotation spans against candidate mention spans in a ~50-word window.
  That is close to our Tier 2 five-line sliding window, and it is worth comparing the two
  framings when QUI-009 is built.

---

## 4. What this does not change

The interception architecture, the cursor matcher, the index, the casting design and the
e-ink work are all unaffected. This is a survey, not a redesign: the only live question it
raises is which model does attribution, and QUI-028 answers that with a measurement rather
than an argument.
