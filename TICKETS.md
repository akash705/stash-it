# STASHED — Implementation Tickets

> Organized by phase. Each epic has sub-tickets. Estimate scale: S (< 1 day), M (1–3 days), L (3–5 days), XL (5+ days).
> Dependencies are noted where they exist. Work within an epic can often be parallelized.

---

## Phase 1 — MVP (Weeks 1–6)

---

### EPIC 1: Project Scaffolding & Build Setup

> Set up the Android project, dependencies, and CI before any feature work.

| ID | Ticket | Description | Size | Depends On |
|---|---|---|---|---|
| 1.1 | Create Android project | Initialize Kotlin + Jetpack Compose project with Gradle KTS, set min SDK 26, target SDK 35, configure package name and signing | S | — |
| 1.2 | Add core dependencies | Add Room, ONNX Runtime Android, Compose Material 3, Navigation Compose, Coroutines, Hilt (DI) to build.gradle.kts | S | 1.1 |
| 1.3 | Cross-compile sqlite-vec for Android | Use Android NDK to compile sqlite-vec C source for arm64-v8a and armeabi-v7a, place .so files in jniLibs | M | 1.1 |
| 1.4 | Set up Room database with sqlite-vec extension | Create AppDatabase, configure Room to load the vec0 extension at connection open, verify extension loads on a real device | M | 1.2, 1.3 |
| 1.5 | Bundle MiniLM model and tokenizer | Export all-MiniLM-L6-v2 to ONNX format with INT8 quantization, bundle model file (~23 MB) and tokenizer.json (~800 KB) in app assets | M | 1.1 |
| 1.6 | Set up CI pipeline | Configure GitHub Actions (or similar) for build, lint, and unit test on every push | S | 1.1 |

---

### EPIC 2: Intelligence Layer — Natural Language Parser

> The regex-based parser that extracts item + location from plain text.

| ID | Ticket | Description | Size | Depends On |
|---|---|---|---|---|
| 2.1 | Implement NL parser | Build the priority-ordered regex parser supporting 3 patterns: verb+item+prep+location, item+is+prep+location, item+prep+location. Handle recognized verbs and prepositions per the plan. Strip filler words (my, the, a). | M | 1.1 |
| 2.2 | Parser fallback handling | When no pattern matches, store entire input as item with empty location. Ensure downstream embedding still works on raw text. | S | 2.1 |
| 2.3 | Unit tests for parser | Cover all 3 pattern types, edge cases (no preposition, multiple prepositions, very long input, single word input, punctuation, mixed case) with at minimum 20 test cases | M | 2.1 |

---

### EPIC 3: Intelligence Layer — Tokenizer

> WordPiece tokenizer that converts text to token IDs for MiniLM.

| ID | Ticket | Description | Size | Depends On |
|---|---|---|---|---|
| 3.1 | Load and parse tokenizer vocabulary | Read tokenizer.json from assets at app startup, build vocab lookup map (token string → ID) | S | 1.5 |
| 3.2 | Implement WordPiece tokenization | Lowercase input, split on whitespace/punctuation, subword-split unknown words using ## prefix convention, add [CLS]/[SEP] tokens, pad/truncate to 128, generate attention mask | M | 3.1 |
| 3.3 | Unit tests for tokenizer | Verify token IDs match expected output from the Python reference tokenizer for at least 10 diverse inputs. Test edge cases: empty string, max-length input, Unicode characters, numbers | M | 3.2 |

---

### EPIC 4: Intelligence Layer — Embedding (MiniLM + ONNX)

> On-device inference that converts tokenized text into 384-dim vectors.

| ID | Ticket | Description | Size | Depends On |
|---|---|---|---|---|
| 4.1 | Initialize ONNX Runtime session | Load INT8 MiniLM model from assets into a singleton OrtSession on first use. Handle OOM gracefully. Measure cold-start time on target devices. | M | 1.2, 1.5 |
| 4.2 | Implement embedding inference | Accept tokenized input (input_ids, attention_mask, token_type_ids), run ONNX session, extract per-token embeddings from output tensor | M | 4.1, 3.2 |
| 4.3 | Implement mean pooling + L2 normalization | Average per-token embeddings weighted by attention mask, L2-normalize the result to a unit vector. Output: FloatArray of size 384. | S | 4.2 |
| 4.4 | Build the Embedder API | Create a single-method API: `suspend fun embed(text: String): FloatArray` that chains tokenizer → ONNX inference → pooling → normalization. Run inference on Dispatchers.Default. | S | 4.3 |
| 4.5 | Benchmark embedding performance | Measure end-to-end embed() latency on 3 device tiers (low-end, mid-range, flagship). Target: < 150ms on mid-range. Log results. | S | 4.4 |
| 4.6 | Verify embedding correctness | Compare Kotlin embedder output against Python sentence-transformers output for 10 identical inputs. Cosine similarity between Kotlin and Python vectors should be > 0.99 for each. | M | 4.4 |

---

### EPIC 5: Storage Layer — Room + sqlite-vec

> Database schema, DAOs, and vector search queries.

| ID | Ticket | Description | Size | Depends On |
|---|---|---|---|---|
| 5.1 | Define Room entities and DAOs | Create MemoryEntity (id, rawText, item, location, emoji, createdAt, updatedAt, embedding as ByteArray). Create MemoryDao with insert, getAll (Flow), getById, update, delete. | M | 1.4 |
| 5.2 | Create sqlite-vec virtual table | On database creation, execute raw SQL to create the memories_vec virtual table with float[384] embedding column | S | 5.1 |
| 5.3 | Implement save transaction | Single Room transaction that inserts into both memories table and memories_vec virtual table. Convert FloatArray to BLOB for storage. | M | 5.2 |
| 5.4 | Implement vector similarity search | Build a RawQuery that embeds the query text, runs vec_distance_cosine against memories_vec, joins back to memories, returns top 5 results ordered by similarity | M | 5.2, 4.4 |
| 5.5 | Implement delete and update | Delete removes from both tables in a transaction. Update re-embeds the modified text and updates both tables. | M | 5.3 |
| 5.6 | Add FTS5 fallback search | Create an FTS5 virtual table on raw_text, item, and location columns. Implement keyword search path used before the MiniLM model is available. | M | 5.1 |
| 5.7 | Database tests | Instrumented tests verifying save, search (both vector and FTS5), update, delete, and transaction rollback behavior | M | 5.4, 5.5, 5.6 |

---

### EPIC 6: Emoji Assignment

> Automatic emoji selection based on item keywords.

| ID | Ticket | Description | Size | Depends On |
|---|---|---|---|---|
| 6.1 | Build emoji lookup map | Create a keyword-to-emoji map covering common item categories: keys (🔑), passport (🛂), glasses (👓), charger (🔌), bag (🎒), medicine (💊), documents (📄), tools (🔧), etc. ~50 mappings. | S | — |
| 6.2 | Implement emoji assignment logic | Match parsed item name against the keyword map using substring matching. Fall back to a default emoji (📦) when no match is found. | S | 6.1, 2.1 |
| 6.3 | Allow manual emoji override | User can tap the assigned emoji to pick a different one from a small grid. Store the user's choice. | S | 6.2 |

---

### EPIC 7: UI — Save Flow

> The primary screen where users type a memory and confirm.

| ID | Ticket | Description | Size | Depends On |
|---|---|---|---|---|
| 7.1 | Build main save screen | Single screen with a large text input field auto-focused on launch. Compose Material 3 styling. Keyboard visible immediately. | M | 1.2 |
| 7.2 | Smart preview card | After user types and pauses (300ms debounce), show a preview card below the input: parsed emoji, item name, and location. Animate card appearance. | M | 7.1, 2.1, 6.2 |
| 7.3 | Confirm and save | "Stash it" button below the preview card. On tap: run the full save pipeline (parse → embed → store), show a brief success animation, clear the input field. | M | 7.2, 5.3, 4.4 |
| 7.4 | Error and edge-case handling | Handle: empty input, input with no parseable location (show warning but allow save), embedding failure (save with FTS5 only, retry embedding later) | S | 7.3 |

---

### EPIC 8: UI — Search Flow

> The screen where users search for memories.

| ID | Ticket | Description | Size | Depends On |
|---|---|---|---|---|
| 8.1 | Build search screen | Text input field at top, auto-focused. Results appear below as a list as the user types. Integrate with navigation (tab bar or swipe). | M | 1.2 |
| 8.2 | Live search with debounce | On text change (500ms debounce), run the search pipeline (embed query → vector search). Show results as memory cards with emoji, item, location, and relative time ("3 days ago"). | M | 8.1, 5.4 |
| 8.3 | Search mode toggle | If MiniLM is not yet available, automatically use FTS5 search. Show a subtle banner: "Smart search downloading — using basic search for now." | S | 8.2, 5.6 |
| 8.4 | Empty states | No results: "Nothing stashed yet" (if DB empty) or "No matches found" (if query doesn't match). First-time search: show example queries as placeholder hints. | S | 8.2 |

---

### EPIC 9: UI — Memory List

> Scrollable list of all saved memories.

| ID | Ticket | Description | Size | Depends On |
|---|---|---|---|---|
| 9.1 | Build memory list screen | LazyColumn of memory cards, ordered by most recently added. Each card shows emoji, item, location, and relative timestamp. Pull-to-refresh. | M | 1.2, 5.1 |
| 9.2 | Swipe-to-delete | Swipe left on a card reveals a delete action. Confirm with a brief undo snackbar (3 seconds to undo). | M | 9.1, 5.5 |
| 9.3 | Tap-to-edit | Tap a card to open an edit bottom sheet. Editable fields: item name, location. On save, re-embed and update both tables. | M | 9.1, 5.5 |
| 9.4 | Memory count indicator | Show total count at the top of the list. In free tier, show "X / 50 memories used" with a progress bar. | S | 9.1 |

---

### EPIC 10: Navigation & App Shell

> Tab navigation, theme, and overall app structure.

| ID | Ticket | Description | Size | Depends On |
|---|---|---|---|---|
| 10.1 | Set up navigation | Bottom navigation bar with 3 tabs: Save (default), Search, All Memories. Use Compose Navigation. | M | 7.1, 8.1, 9.1 |
| 10.2 | App theme and typography | Define color scheme (light + dark mode), typography scale, and shared component styles. Material 3 dynamic color support. | M | 1.2 |
| 10.3 | App icon and splash screen | Design and set app launcher icon. Configure Android 12+ splash screen API. | S | 1.1 |

---

### EPIC 11: Onboarding

> First-run experience.

| ID | Ticket | Description | Size | Depends On |
|---|---|---|---|---|
| 11.1 | Onboarding screens | 3-screen horizontal pager: (1) "Type where you put things" (2) "Search in your own words" (3) "Everything stays on your phone". Skip button + Get Started button. | M | 1.2 |
| 11.2 | Persist onboarding state | Use DataStore to track whether onboarding has been completed. Show only on first launch. | S | 11.1 |
| 11.3 | Model download during onboarding | If using Play Feature Delivery: trigger model download in background during onboarding. Show a non-blocking progress indicator. If model is bundled in APK, skip this. | M | 11.1, 1.5 |

---

### EPIC 12: Play Feature Delivery Setup

> Optional: deferred download of the MiniLM model to reduce initial install size.

| ID | Ticket | Description | Size | Depends On |
|---|---|---|---|---|
| 12.1 | Configure Play Feature Delivery module | Create a dynamic feature module containing the MiniLM ONNX model. Configure on-demand delivery in the app's build.gradle. | L | 1.5 |
| 12.2 | Implement download manager | Request model download on first launch. Track download state (pending, downloading, installed, failed). Retry on failure. Respect metered connection preferences. | M | 12.1 |
| 12.3 | Handle model availability transitions | When model becomes available: load OrtSession, batch-embed any memories saved with FTS5-only, switch search from FTS5 to vector. Show "Smart search is now active" notification. | M | 12.2, 4.1, 5.6 |
| 12.4 | Test on slow/metered networks | Verify graceful behavior when download is interrupted, phone goes offline, or user is on metered data. | S | 12.2 |

---

## Phase 2 — Stickiness (Weeks 7–12)

---

### EPIC 13: Home Screen Widget

| ID | Ticket | Description | Size | Depends On |
|---|---|---|---|---|
| 13.1 | Build 4×1 widget layout | Glance (Jetpack Compose for widgets) based widget with a single text input field and a "Stash" button | M | Phase 1 complete |
| 13.2 | Widget → app save pipeline | On widget submit: run NL parser → embed → save in background via WorkManager. Show brief toast confirmation. No app launch required. | M | 13.1 |
| 13.3 | Build 2×1 compact variant | Smaller widget with input field only, confirm on keyboard "Done" action | S | 13.1 |
| 13.4 | Widget configuration | Allow user to pick widget size during placement. Respect dark/light theme. | S | 13.1 |

---

### EPIC 14: Voice Input

| ID | Ticket | Description | Size | Depends On |
|---|---|---|---|---|
| 14.1 | Integrate SpeechRecognizer | Implement speech-to-text using createOnDeviceSpeechRecognizer (API 33+) with fallback to createSpeechRecognizer for older devices. Handle microphone permission. | M | Phase 1 complete |
| 14.2 | Voice button on save screen | Add a microphone FAB to the save screen. Hold to speak, release to process. Show real-time transcription in the text field. | M | 14.1 |
| 14.3 | Voice button on search screen | Same mic button on search screen. Transcribed text feeds into the search pipeline. | S | 14.1, 14.2 |
| 14.4 | Offline voice model check | Detect whether on-device speech model is available. If not, prompt user to download it from Android settings. Show graceful fallback message. | S | 14.1 |

---

### EPIC 15: Location History

| ID | Ticket | Description | Size | Depends On |
|---|---|---|---|---|
| 15.1 | Add location_history table | New Room entity: LocationHistoryEntity (memoryId, previousLocation, changedAt). Foreign key to memories. | S | Phase 1 complete |
| 15.2 | Archive on location update | When a memory's location is edited, insert the old location into location_history before updating. | S | 15.1 |
| 15.3 | Show location history in UI | On memory detail/edit screen, show "Previously:" section listing the last 5 locations with timestamps. | M | 15.2 |

---

### EPIC 16: Search Improvements

| ID | Ticket | Description | Size | Depends On |
|---|---|---|---|---|
| 16.1 | Search result highlighting | Show a brief explanation under each result: which words/concepts matched and the similarity score as a visual bar | M | Phase 1 complete |
| 16.2 | Improved emoji auto-assignment | Expand the keyword map to ~150 items. Add fuzzy matching (substring + common synonyms). | S | 6.1 |

---

### EPIC 17: Premium Paywall

| ID | Ticket | Description | Size | Depends On |
|---|---|---|---|---|
| 17.1 | Integrate Google Play Billing | Add Play Billing library. Implement purchase flow for monthly, annual, and lifetime plans. Handle purchase verification. | L | Phase 1 complete |
| 17.2 | Build paywall screen | Show free vs premium comparison, pricing, and purchase buttons. Trigger when user hits 50-memory limit or taps a premium feature. | M | 17.1 |
| 17.3 | Gate premium features | Enforce limits: 50 memories (free), voice input (premium), widget (premium), location history (premium). Check subscription state from Play Billing. | M | 17.1 |
| 17.4 | Free trial flow | Offer 7-day free trial of premium on first paywall encounter. Handle trial expiry gracefully. | S | 17.1, 17.2 |
| 17.5 | Restore purchases | Handle reinstalls and device switches. Query Play Billing for existing subscriptions/lifetime purchases. | S | 17.1 |

---

## Phase 3 — Growth (Months 4–6)

---

### EPIC 18: Departure Reminders

| ID | Ticket | Description | Size | Depends On |
|---|---|---|---|---|
| 18.1 | Implement geofence setup | Use Android Geofencing API to register home/work locations. User configures named locations in settings. Request location permissions (foreground + background). | L | Phase 2 complete |
| 18.2 | Departure detection | Listen for geofence exit transitions via BroadcastReceiver. Trigger reminder logic when user leaves a registered zone. | M | 18.1 |
| 18.3 | Smart reminder notification | On departure, query memories tagged as high-importance or recently moved. Show a notification: "Leaving home — passport is in the wardrobe, keys are on the hook." | M | 18.2 |
| 18.4 | Reminder settings | User can enable/disable departure reminders, choose which locations trigger them, and mark specific items as "remind me" items. | M | 18.3 |

---

### EPIC 19: Backup & Restore

| ID | Ticket | Description | Size | Depends On |
|---|---|---|---|---|
| 19.1 | Export to local file | Export all memories as an encrypted JSON file to device storage (Downloads folder). Include metadata, exclude raw embeddings (re-generate on import). | M | Phase 2 complete |
| 19.2 | Import from local file | Read an exported JSON file, parse memories, re-embed each via the save pipeline, insert into database. Handle duplicates by ID. | M | 19.1 |
| 19.3 | Scheduled auto-backup | Optional weekly auto-backup to a user-configured local directory via WorkManager. | S | 19.1 |

---

### EPIC 20: Household Sharing

| ID | Ticket | Description | Size | Depends On |
|---|---|---|---|---|
| 20.1 | Design sharing protocol | Define the local sharing format: compressed JSON memory packets transferred via QR code or WiFi Direct. No cloud involved. | M | Phase 2 complete |
| 20.2 | Export shareable memory packet | Generate a QR code or file containing selected memories (or all) for transfer to another device running Stashed. | M | 20.1 |
| 20.3 | Import shared memories | Receive a memory packet, merge into local database with source attribution (who shared it). Handle conflicts (same item, different location). | L | 20.1 |
| 20.4 | Shared memory indicator | Show a small badge on memories that were shared from another person, with their name and the share date. | S | 20.3 |

---

### EPIC 21: Accessibility & Elderly Mode

| ID | Ticket | Description | Size | Depends On |
|---|---|---|---|---|
| 21.1 | High-contrast theme | Create an alternative theme with larger fonts (1.5× base), high-contrast colors, and simplified layout. Toggle in settings. | M | Phase 2 complete |
| 21.2 | TalkBack and accessibility audit | Ensure all screens are fully navigable with TalkBack. Add content descriptions to all interactive elements. Fix any issues. | M | 21.1 |

---

### EPIC 22: iOS Version

| ID | Ticket | Description | Size | Depends On |
|---|---|---|---|---|
| 22.1 | Evaluate Compose Multiplatform vs Swift UI | Assess maturity of Compose Multiplatform for iOS. Decide whether to share UI code or build native SwiftUI with shared Kotlin logic (KMP). | M | Phase 2 complete |
| 22.2 | Set up KMP shared module | Extract intelligence layer (parser, tokenizer, embedder API) and storage layer (database schema, queries) into a Kotlin Multiplatform shared module. | L | 22.1 |
| 22.3 | ONNX Runtime iOS integration | Integrate ONNX Runtime for iOS (via CocoaPods/SPM). Bundle same INT8 MiniLM model. Verify embedding output matches Android. | L | 22.2 |
| 22.4 | sqlite-vec iOS integration | Compile sqlite-vec for iOS architectures (arm64). Load into iOS SQLite. Verify vector search works identically. | M | 22.2 |
| 22.5 | Build iOS UI | Implement all MVP screens in SwiftUI (or Compose Multiplatform). Match Android feature parity for save, search, memory list, edit/delete. | XL | 22.3, 22.4 |
| 22.6 | iOS-specific features | Home screen widget (WidgetKit), Siri Shortcuts integration, iOS speech recognition. | L | 22.5 |

---

## Summary

| Phase | Epics | Tickets | Estimated Effort |
|---|---|---|---|
| Phase 1 — MVP | 12 epics | 48 tickets | ~6 weeks |
| Phase 2 — Stickiness | 5 epics | 18 tickets | ~5 weeks |
| Phase 3 — Growth | 5 epics | 16 tickets | ~8 weeks |
| **Total** | **22 epics** | **82 tickets** | |

### Recommended Build Order (Phase 1)

```
Week 1:  EPIC 1 (scaffolding) → EPIC 2 (parser) → EPIC 3 (tokenizer)
Week 2:  EPIC 4 (embedder) → EPIC 5 (storage) — can partially parallelize
Week 3:  EPIC 6 (emoji) → EPIC 7 (save UI)
Week 4:  EPIC 8 (search UI) → EPIC 9 (memory list)
Week 5:  EPIC 10 (navigation) → EPIC 11 (onboarding)
Week 6:  EPIC 12 (play feature delivery) → integration testing → internal release
```

### Critical Path

The longest dependency chain in Phase 1 is:

```
1.1 → 1.5 → 3.1 → 3.2 → 4.1 → 4.4 → 5.4 → 8.2
Project setup → model bundle → tokenizer → embedder → vector search → search UI
```

This chain must be complete before the core value proposition (semantic search) works end to end. All other work (parser, emoji, UI shells, onboarding) can proceed in parallel.
