# STASHED
## Your Offline Memory for Lost Things

> **App Planning & Technical Design Document**
> Prepared by Akash · April 2026 · Version 1.0

---

## Table of Contents

1. [The Problem](#1-the-problem)
2. [Product Vision](#2-product-vision)
3. [Target Audience](#3-target-audience)
4. [Features](#4-features)
5. [User Flows](#5-user-flows)
6. [Technical Architecture](#6-technical-architecture)
7. [App Size Breakdown](#7-app-size-breakdown)
8. [Monetization](#8-monetization)
9. [Development Roadmap](#9-development-roadmap)
10. [Open Questions](#10-open-questions)

---

## 1. The Problem

Everyone has experienced the frustration of forgetting where they put something. You set down your passport before a trip, lock your spare key somewhere safe, tuck away an important document — and later spend 20 minutes searching. The problem is not memory failure; it is that there is no frictionless way to capture a location at the exact moment you put something down.

Existing apps solve this badly. They force you to navigate menus, pick a category, select a room, and then type an item name. By the time you have done all that, it is faster to just try to remember. What is missing is an app that works the way your brain does: one plain sentence in, one plain answer out.

> *"toilet key in office drawer" → saved. Later: "where are my keys?" → answered. That is the entire experience.*

### Why Existing Apps Fail

| Existing App Approach | Why It Fails |
|---|---|
| Structured menus (pick room → pick item) | Too slow. 5 taps to save one thing. |
| Bluetooth trackers (Tile, AirTag) | Only works for tagged physical objects. |
| Notes apps (Google Keep, Apple Notes) | Keyword search only, no semantic understanding. |
| Photo-based inventory apps | Requires photos, too much effort for small items. |
| Cloud-based AI assistants | Privacy risk, needs internet, subscription costs. |

---

## 2. Product Vision

Stashed is a fully offline Android app that acts as an external memory for where you put things. You describe something in plain English, and the app stores it intelligently. When you need to find it, you ask in plain English and the app retrieves it — even if your wording is completely different from what you typed originally.

The entire experience is private by design. No data ever leaves your phone. No account needed. No internet connection required. Not on first launch, not ever. This is the foundational promise of Stashed.

### Core Principles

- **Offline-first.** Every feature works without internet.
- **Zero friction.** Saving a memory must take under 5 seconds.
- **Natural language in and out.** No menus, no categories, no forms.
- **Private by default.** No accounts, no cloud, no tracking.
- **Smart retrieval.** Finds the right memory even with different words.

### App Name Candidates

| Name | Meaning | Verdict |
|---|---|---|
| **Stashed** | Things you have hidden away safely | ⭐ Top pick — warm, memorable, clear |
| Loci | Method of Loci memory technique | Smart and clever but niche |
| Tucked | Something put away carefully | Warm but less distinct |
| Whereabouts | Exactly what the app does | Long but very descriptive |
| Recalled | The act of remembering | Clean, professional feel |
| Misplaced | Ironic, self-aware | Risky but memorable |

---

## 3. Target Audience

### Primary — People With ADHD

Adults with ADHD are the single best-served audience for this app. They frequently experience object displacement as a daily, debilitating issue. They are highly vocal in communities (Reddit, TikTok, YouTube) and will actively share tools that work. They have demonstrated willingness to pay for apps that genuinely help.

> *"I always said I wish I could search my brain for where I put stuff — and here it is."* — Competitor app review

### Secondary — Forgetful Adults (General)

The broader population of adults who regularly misplace things. This is a near-universal experience but a less urgent pain point compared to ADHD users. Best reached via search ads and word of mouth from the primary audience.

### Tertiary — Families and Households

Shared household items — chargers, tools, documents, spare keys — are a constant source of friction between family members. A shared household memory accessible by everyone on the same device could be a compelling feature for this group.

### Quaternary — Elderly Users

Elderly individuals and their caregivers represent a growing, underserved market. Families would pay for an app that reduces "where did I put my medicine?" stress. The UI must be especially simple and high-contrast. This is a Phase 2 consideration.

---

## 4. Features

### Core Features (MVP)

#### 1. Natural Language Save

The user opens the app or widget and types a single sentence. The app parses it into an item and location, assigns an emoji, and stores it.

Examples:
- `"passport in the top shelf of the wardrobe"`
- `"toilet key in office desk drawer"`
- `"laptop charger behind the TV unit"`
- `"gym bag in the car boot"`

The input field is the first thing visible on launch. No categories, no dropdowns. Just type and confirm.

#### 2. Semantic Search

The user types or speaks a question in natural language. The app uses on-device semantic search to find the most relevant memory, even if the words do not match exactly.

- `"where are my keys?"` → finds *"toilet key in office drawer"*
- `"where is my travel document?"` → finds *"passport in wardrobe"*
- `"gym stuff?"` → finds *"gym bag in car boot"*

#### 3. Memory List

A scrollable list of all saved memories, ordered by most recently added. Each card shows the emoji, item name, location, and how long ago it was saved.

#### 4. Delete & Edit

Swipe left on a card to delete. Tap to open and edit either the item name or location. When a location is updated, the previous location is kept in history.

---

### Phase 2 Features (Post-MVP)

#### Home Screen Widget

A 2×1 or 4×1 Android widget with a single text field for instant saving without opening the app. This is the most important feature for reducing friction below 3 seconds. Without a widget, many users will simply not build the habit.

#### Voice Input

Hold a button and speak. Uses Android's built-in on-device speech recognition (`SpeechRecognizer` API — works offline on Android 13+) to convert speech to text, then processes it as a normal save. No third-party voice service required.

#### Location History

When an item's location is updated, the previous location is stored with a timestamp. This handles the very common case of "I moved it but forgot I moved it." The user can see the last 3 places an item was.

#### Departure Reminder

Using the phone's GPS and Android's Geofencing API (offline-capable), the app can remind the user of important items when they leave a known location. For example: *"Leaving home — your passport is in the wardrobe, not your bag."* This is opt-in and uses no external services.

---

## 5. User Flows

### Flow 1: Saving a Memory

1. User puts something in a location (e.g., passport in wardrobe).
2. Opens app or taps the home screen widget.
3. Types: `"passport in the top shelf of wardrobe"`.
4. App shows a smart preview: emoji 🛂, item *"passport"*, location *"top shelf of wardrobe"*.
5. User confirms with one tap. Saved in under 4 seconds.

### Flow 2: Finding a Memory

1. User can't find their passport.
2. Opens app. Search bar is auto-focused.
3. Types: `"where is my travel document?"`.
4. App returns: *"🛂 Passport — top shelf of wardrobe (saved 3 days ago)"*.
5. User finds their passport.

### Flow 3: Updating a Location

1. User moves their passport from the wardrobe to their travel bag.
2. Opens the passport memory card.
3. Taps Edit → updates location to *"travel bag"*.
4. Old location (*"top shelf of wardrobe"*) is archived in history.

---

## 6. Technical Architecture

> **Core constraint: Nothing ever leaves the device. No API calls, no telemetry, no cloud sync. Every component described below runs entirely on-device.**

### Architecture Overview

The app has three main technical layers: the UI layer, the intelligence layer, and the storage layer. All three run locally on the Android device.

| Layer | Component | Technology |
|---|---|---|
| UI | App interface and navigation | Kotlin + Jetpack Compose |
| Intelligence | Natural language parsing | On-device regex + rule engine |
| Intelligence | Tokenization | WordPiece tokenizer (bundled vocab) |
| Intelligence | Text to vector (embedding) | MiniLM-L6-v2 via ONNX Runtime |
| Intelligence | Voice input | Android SpeechRecognizer API |
| Storage | Vector similarity search | sqlite-vec extension (native .so) |
| Storage | Structured data (items, history) | SQLite via Room |
| Storage | App preferences | Android DataStore (local) |

### Framework Decision: Kotlin + Jetpack Compose (Not React Native)

The constraints answer this decisively:

- **App size:** React Native adds ~15–20 MB of JavaScript bridge overhead against a 48 MB total target.
- **Native integration:** ONNX Runtime and sqlite-vec both require native (C/JNI) bindings. In React Native, every native module needs a bridge wrapper — added complexity for the two most critical components.
- **Widget support:** Android home screen widgets are first-class in Kotlin/Compose but second-class in React Native, requiring a separate native module.
- **Performance:** The embedding pipeline (tokenize → infer → store) runs on tight latency budgets (~100ms inference). Crossing the JS–native bridge adds unpredictable overhead.
- **iOS in Phase 3:** The MiniLM model, tokenizer, sqlite-vec schema, and NL parser logic can be shared across platforms. Only the UI layer needs to be native per platform — and Compose Multiplatform is maturing as an option.

---

### Full Save Pipeline (End to End)

When the user types or speaks a memory, the following happens in sequence:

```
User input (text or speech)
      ↓
1. Speech-to-Text (if voice)
   Android SpeechRecognizer → raw text string
      ↓
2. Natural Language Parser
   Regex rule engine → { item, location }
      ↓
3. Emoji Assignment
   Keyword-to-emoji lookup map → best-fit emoji
      ↓
4. Tokenization
   WordPiece tokenizer → array of token IDs + attention mask
      ↓
5. Embedding
   ONNX Runtime runs MiniLM → 384-dim float vector
   Mean pooling across tokens + L2 normalization
      ↓
6. Storage
   Room inserts structured fields (item, location, emoji, timestamps)
   sqlite-vec inserts the embedding vector into the virtual table
      ↓
Done. Total time: < 500ms on a mid-range phone.
```

### Full Search Pipeline (End to End)

```
User types or speaks a query ("where are my travel documents?")
      ↓
1. Speech-to-Text (if voice)
      ↓
2. Tokenization + Embedding
   Same pipeline as save: WordPiece → MiniLM → 384-dim vector
      ↓
3. Vector Similarity Search
   sqlite-vec cosine distance query against all stored embeddings
   Returns top 5 nearest matches ranked by similarity score
      ↓
4. Result Display
   Each result shows emoji, item, location, time since saved, similarity score
      ↓
Done. Total time: < 300ms on a mid-range phone.
```

---

### Component 1: Speech-to-Text

Uses Android's built-in on-device speech recognition via the `SpeechRecognizer` API.

- **Android 13+ (API 33+):** Supports fully offline recognition via `createOnDeviceSpeechRecognizer()`. No internet needed. This is the target path.
- **Android 12 and below:** Falls back to `createSpeechRecognizer()`, which requires an internet connection for the initial speech model download. After the model is cached, it works offline on most devices.
- **No third-party libraries needed.** This is a system API — zero additional app size.

The speech recognizer returns a raw text string (e.g., *"passport in the top shelf of wardrobe"*) which feeds directly into the NL parser.

---

### Component 2: Natural Language Parser

Before generating an embedding, the app parses the raw input text to extract a structured item name and location. This is done with a rule-based pattern engine — no AI model needed for this step.

The parser uses a priority-ordered list of regex patterns, evaluated top to bottom (first match wins):

| Priority | Pattern | Example |
|---|---|---|
| 1 | `[verb] [item] [preposition] [location]` | *"put passport in wardrobe"* |
| 2 | `[item] is/are [preposition] [location]` | *"passport is in the wardrobe"* |
| 3 | `[item] [preposition] [location]` | *"passport in wardrobe"* (simplest) |

Recognized verbs: `put`, `placed`, `left`, `stored`, `kept`, `tossed`.

Recognized prepositions: `in`, `inside`, `at`, `on`, `under`, `above`, `behind`, `near`, `beside`, `next to`, `on top of`, `beneath`.

The parser also strips filler words (`my`, `the`, `a`) from the item field.

```
Input:  "toilet key in the office desk drawer"
Parsed: { item: "toilet key", location: "office desk drawer" }

Input:  "put my passport inside the wardrobe top shelf"
Parsed: { item: "passport", location: "wardrobe top shelf" }

Input:  "glasses are on the bedside table"
Parsed: { item: "glasses", location: "bedside table" }
```

**Fallback:** If no pattern matches, the entire input is stored as the item with an empty location. The embedding is still generated from the raw text, so semantic search will still work.

---

### Component 3: Tokenization (WordPiece)

MiniLM expects tokenized input, not raw text. Tokenization converts a sentence into an array of integer token IDs that the model understands.

**How it works:**

1. The app bundles a `tokenizer.json` vocabulary file (~800 KB) from the MiniLM model package.
2. Input text is lowercased and split on whitespace and punctuation.
3. Each word is looked up in the vocabulary. Unknown words are split into known subword pieces (e.g., *"passport"* → *["pass", "##port"]*).
4. Special tokens `[CLS]` (start) and `[SEP]` (end) are added.
5. The sequence is padded or truncated to a fixed length of 128 tokens.
6. An attention mask is generated: `1` for real tokens, `0` for padding.

**Output:** Two arrays — `input_ids` and `attention_mask` — both of length 128. These feed directly into the ONNX model.

**Implementation:** The WordPiece algorithm is straightforward (~200 lines of Kotlin). No external tokenization library is needed.

---

### Component 4: On-Device Embedding (MiniLM + ONNX)

After tokenization, the token arrays are fed into the MiniLM model running via ONNX Runtime to produce a semantic embedding vector.

#### What is MiniLM?

`all-MiniLM-L6-v2` is a lightweight AI model created by Microsoft, trained on 1 billion sentence pairs. It converts any text into a 384-dimensional vector. Sentences with similar meaning produce vectors that are mathematically close to each other.

- Full precision (FP32): ~90 MB
- INT8 quantized: ~23 MB ✅ (what we use)

#### What is ONNX?

ONNX (Open Neural Network Exchange) is a universal format for AI models. MiniLM is originally a PyTorch model. Exporting it to ONNX format allows it to run on Android via the **ONNX Runtime Mobile** library (~12 MB), which uses the phone's CPU or GPU to execute the model locally.

#### The Embedding Pipeline

```
Tokenized input (input_ids + attention_mask)
      ↓
ONNX Runtime loads the INT8 quantized MiniLM model (~23 MB)
      ↓
Model runs inference on-device (~100ms on modern phone)
      ↓
Raw output: per-token embeddings (128 tokens × 384 dimensions)
      ↓
Mean pooling: average all non-padding token vectors into one
(weighted by attention mask — padding tokens are excluded)
      ↓
L2 normalization: scale the vector to unit length
(required for cosine similarity to work correctly)
      ↓
Final output: single 384-dimensional unit vector (1,536 bytes as float32)
```

> **Critical detail:** MiniLM outputs one vector per token, not one vector per sentence. The mean pooling and L2 normalization steps are essential — without them, cosine similarity scores will be meaningless. This is a common implementation mistake.

#### INT8 Quantization

The MiniLM model is quantized from 32-bit floats (FP32) to 8-bit integers (INT8), reducing size from 90 MB to 23 MB with less than 1% accuracy loss.

| | FP32 Full Precision | INT8 Quantized |
|---|---|---|
| Model size | 90 MB | **23 MB ✅** |
| Accuracy loss | — | < 1% |
| Inference speed | ~180ms | ~100ms (faster) |
| Memory usage | High | Low |

#### Model Loading Strategy

The ONNX model file is loaded into an `OrtSession` once when the app starts (or when the intelligence layer is first needed). The session is kept in memory as a singleton for the lifetime of the app process. Creating a new session per query would add ~500ms of overhead — unacceptable for a <300ms search target.

#### Dependency

- **ONNX Runtime Android** (`com.microsoft.onnxruntime:onnxruntime-android`) — ~12 MB. Provides the inference engine. Supports CPU execution on all Android devices and optional GPU/NNAPI acceleration on supported hardware.

---

### Component 5: Vector Search (sqlite-vec)

`sqlite-vec` is an open-source SQLite extension that adds a vector column type and nearest-neighbour search to a standard SQLite database. Since SQLite is already built into Android, adding sqlite-vec costs only ~2 MB.

#### Android Integration

sqlite-vec is a C library. To use it on Android:

1. **Cross-compile** the sqlite-vec source for Android CPU architectures (`arm64-v8a`, `armeabi-v7a`) using the Android NDK. The sqlite-vec repository includes build tooling for this.
2. **Bundle** the compiled `.so` files in `app/src/main/jniLibs/<abi>/libvec0.so`.
3. **Load at runtime** via `System.loadLibrary("vec0")` and register the extension with Room's underlying SQLite connection.

Room (Android's SQLite ORM) manages the structured `memories` table. The sqlite-vec virtual table (`memories_vec`) is created alongside it and linked by row ID.

#### Database Schema

```sql
TABLE memories (
  id           TEXT PRIMARY KEY,
  raw_text     TEXT,          -- original input
  item         TEXT,          -- parsed item name
  location     TEXT,          -- parsed location
  emoji        TEXT,          -- auto-assigned
  created_at   INTEGER,       -- unix timestamp
  updated_at   INTEGER,
  embedding    BLOB           -- 384-dim float32 vector (1,536 bytes)
);

VIRTUAL TABLE memories_vec USING vec0(
  embedding float[384]
);
```

Both tables are written to in a single Room transaction when saving a memory. The embedding BLOB in the `memories` table is the source of truth; the `memories_vec` virtual table is the search index.

#### How Search Works

```
User searches: "where are my travel documents?"
      ↓
Query is tokenized → embedded via MiniLM → 384-dim query vector
      ↓
sqlite-vec runs cosine distance against all stored vectors
Returns top 5 nearest matches ranked by similarity
      ↓
Room joins the vector results back to the memories table
for structured fields (item, location, emoji, timestamps)
      ↓
Results displayed to user
"passport in top shelf of wardrobe" scores 0.91 similarity
```

The entire search — embed the query, run vector search, return results — completes in **under 300ms** on a mid-range Android phone.

#### Scaling

sqlite-vec uses brute-force (exact) nearest-neighbour search. For this app's expected data size (tens to low thousands of memories per user), brute-force is fast enough — there is no need for approximate nearest-neighbour (ANN) indexing. At 1,000 memories, the vector scan takes <10ms.

#### Why Semantic Search Beats Keyword Search

| Query typed | Keyword search finds | Semantic search finds |
|---|---|---|
| "where are my keys?" | Nothing (word "keys" not in saved text) | ✅ "toilet key in office drawer" |
| "travel document" | Nothing | ✅ "passport in wardrobe" |
| "specs" | Nothing | ✅ "glasses on bedside table" |
| "charging cable" | Nothing | ✅ "phone charger in bedroom drawer" |

#### Why Semantic Search Beats Keyword Search

Keyword search requires exact word matches. Semantic search compares meaning. This is the single most important technical differentiator of Stashed — it is what makes the "natural language in, natural language out" promise real.

#### Search Fallback (Before Model Download)

If the MiniLM model has not yet been downloaded (Play Feature Delivery flow), the app falls back to SQLite full-text search (FTS5) on the `raw_text`, `item`, and `location` columns. FTS5 supports prefix matching and basic ranking — good enough for exact and partial word matches. Once the model downloads, all existing memories are batch-embedded in the background and semantic search activates silently.

---

## 7. App Size Breakdown

Since the app runs entirely offline, all AI components must be bundled with or downloaded by the app.

### Installed Size by Component

| Component | What It Does | Size |
|---|---|---|
| MiniLM INT8 ONNX model | Converts text to vectors | ~23 MB |
| ONNX Runtime Mobile | Executes MiniLM on-device | ~12 MB |
| sqlite-vec | Vector similarity search in SQLite | ~2 MB |
| Tokenizer files | Splits text into tokens for MiniLM | ~1 MB |
| App code + UI | React Native framework + all screens | ~8 MB |
| Assets (icons, fonts) | Visual assets | ~2 MB |
| **TOTAL INSTALLED** | | **~48 MB** |

### Play Store Download Size

| Distribution Method | Download Size |
|---|---|
| Full APK (all architectures) | ~80 MB |
| Android App Bundle (AAB) — ARM64 only | ~30 MB ✅ Recommended |
| AAB + Play Feature Delivery (model post-install) | ~5 MB initial + 23 MB background |

### Recommended Approach: Play Feature Delivery

The best user experience is to ship a tiny 5 MB base app that installs instantly, then silently download the MiniLM model in the background on first launch.

```
Step 1: User installs Stashed from Play Store  →  5 MB download
Step 2: App launches → basic keyword search works immediately
Step 3: Background download of MiniLM model  →  23 MB (WiFi/data)
Step 4: "Smart search is now active" notification shown
Step 5: Full semantic search works forever, even offline
```

---

## 8. Monetization

The app is free to download with a genuinely useful free tier. Monetization comes from a premium upgrade — no ads, as ads would undermine the privacy-first positioning.

### Free vs Premium

| Free Tier | Premium Tier |
|---|---|
| Up to 50 saved memories | Unlimited saved memories |
| Basic keyword search | Full semantic (AI) search |
| Manual text input only | Voice input |
| Single device | Export / backup to local file |
| — | Location history (last 5 per item) |
| — | Home screen widget |
| — | Departure reminders (GPS geofence) |

### Pricing

| Plan | Price |
|---|---|
| Monthly | ₹199 / month |
| Annual (best value) | ₹1,499 / year (₹125/mo — 37% off) |
| Lifetime | ₹2,999 one-time |

> **Key insight:** Lifetime pricing converts well for utility apps. Users who are genuinely frustrated by the problem are willing to pay once to solve it permanently. Target the ADHD community first — they evangelize tools that work.

---

## 9. Development Roadmap

### Phase 1 — MVP (Weeks 1–6)

**Goal:** A working app that can save and retrieve memories using semantic search, entirely offline.

- Natural language text input and parser
- MiniLM + ONNX Runtime integration
- sqlite-vec vector storage and search
- Memory list with emoji cards
- Delete and edit memories
- Basic onboarding (3 screens explaining the app)
- Play Feature Delivery setup for model download

**Target:** Internal Play Store testing release.

### Phase 2 — Stickiness (Weeks 7–12)

**Goal:** Features that build daily habit and increase retention.

- Home screen widget (2×1 quick-save)
- Android on-device voice input
- Location history (last 5 per item)
- Emoji auto-assignment improvements
- Search result highlighting (show why a match was found)
- Premium paywall with free trial

**Target:** Public Play Store launch.

### Phase 3 — Growth (Months 4–6)

**Goal:** Features that expand the user base and drive word of mouth.

- Departure reminders using Android Geofencing API
- Local backup and restore (to device storage, no cloud)
- Household sharing via local Bluetooth/WiFi export
- High-contrast accessibility mode for elderly users
- iOS version (same React Native codebase)

---

## 10. Open Questions

| Question | Options to Consider |
|---|---|
| Mobile framework choice | React Native (faster, one codebase) vs Kotlin (better Android integration, better widget support) |
| Semantic search fallback | If model not yet downloaded, use keyword search or BM25 ranking? |
| Household sharing mechanism | Local JSON export/import, or local network (same WiFi) peer sharing? |
| Onboarding model download | Download on first launch automatically, or ask user permission first? |
| Widget input method | Quick text field only, or also voice from widget? |
| App name final decision | Stashed vs Loci vs Whereabouts — needs user testing |

---

*Stashed — Offline. Private. Effortless.*
*Document prepared April 2026 · Akash · Confidential*
