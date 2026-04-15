# Stashed — Android Setup Guide

## Prerequisites

- **Android Studio Hedgehog (2023.1.1) or later**
- **JDK 17** (bundled with Android Studio)
- **Android SDK** with API 35 and NDK installed (via SDK Manager)

---

## 1. Open the project

Open Android Studio → **Open** → select the `android/` folder.

Let Gradle sync complete. It will download all dependencies automatically.

---

## 2. Bundle the MiniLM model

The ONNX model must be placed in `app/src/main/assets/` before building.

```bash
# From Python (run once on your machine):
pip install sentence-transformers optimum onnxruntime

python3 - <<'EOF'
from sentence_transformers import SentenceTransformer
from optimum.exporters.onnx import main_export

# Export to ONNX + INT8 quantization
main_export(
    model_name_or_path="sentence-transformers/all-MiniLM-L6-v2",
    output="./minilm-export",
    task="feature-extraction",
    int8=True,
)
EOF

cp minilm-export/model_quantized.onnx \
   android/app/src/main/assets/minilm-l6-v2-int8.onnx
```

The tokenizer.json is downloaded automatically by the Python export and should
also be placed in assets:

```bash
cp minilm-export/tokenizer.json \
   android/app/src/main/assets/tokenizer.json
```

---

## 3. Compile sqlite-vec for Android

sqlite-vec provides the vector similarity search engine.

```bash
# Install Android NDK via SDK Manager first, then:
git clone https://github.com/asg017/sqlite-vec.git
cd sqlite-vec

# Set NDK path (adjust to your installation)
export ANDROID_NDK_HOME=$HOME/Library/Android/sdk/ndk/26.1.10909125

# Build for both architectures
make loadable ANDROID_ABI=arm64-v8a
make loadable ANDROID_ABI=armeabi-v7a

# Copy output .so files
cp dist/arm64-v8a/vec0.so \
   ../android/app/src/main/jniLibs/arm64-v8a/libvec0.so

cp dist/armeabi-v7a/vec0.so \
   ../android/app/src/main/jniLibs/armeabi-v7a/libvec0.so
```

**Note:** Without sqlite-vec, the app runs in FTS5-only (keyword search) mode.
It will still work — just without semantic search. You can skip this step while
getting the app running for the first time.

---

## 4. Run

Select a device or emulator (API 26+) and click **Run**.

For best performance, test on a physical device. The ONNX inference is
CPU-bound and runs ~3–4× faster on real hardware than on the emulator.

---

## 5. Running tests

```bash
# Unit tests (NL parser, emoji mapper — no device needed)
./gradlew test

# Instrumented tests (database, search — needs a connected device)
./gradlew connectedAndroidTest
```

---

## Project structure

```
app/src/main/kotlin/com/stashed/app/
├── intelligence/
│   ├── NLParser.kt           # Regex-based item+location extractor
│   ├── EmojiMapper.kt        # Keyword → emoji lookup
│   ├── WordPieceTokenizer.kt # Text → token IDs for MiniLM
│   └── MiniLMEmbedder.kt     # ONNX inference + mean pooling + L2 norm
├── data/
│   ├── local/
│   │   ├── MemoryEntity.kt   # Room entity (structured fields + embedding BLOB)
│   │   ├── MemoryDao.kt      # Room DAO (insert, search, update, delete)
│   │   └── AppDatabase.kt    # Room database + sqlite-vec virtual tables
│   └── repository/
│       └── MemoryRepository.kt  # Orchestrates save + search pipelines
├── di/
│   └── AppModule.kt          # Hilt DI: database, tokenizer, embedder
├── ui/
│   ├── save/                 # Save screen + ViewModel
│   ├── search/               # Search screen + ViewModel
│   ├── list/                 # Memory list screen + ViewModel
│   ├── components/           # MemoryCard, SwipeToDeleteContainer
│   ├── navigation/           # Bottom nav + NavHost
│   └── theme/                # Material 3 colors + typography
├── MainActivity.kt
└── StashedApp.kt             # Hilt application class
```

---

## What's implemented (Phase 1 MVP)

- [x] Natural language parser (regex, 3 patterns, all prepositions)
- [x] Emoji auto-assignment (~35 categories)
- [x] WordPiece tokenizer (bundled vocab, 128-token max, attention mask)
- [x] MiniLM embedder (ONNX Runtime, mean pooling, L2 normalization)
- [x] Room database with sqlite-vec + FTS5 virtual tables
- [x] Full save pipeline: parse → emoji → embed → store (one transaction)
- [x] Full search pipeline: embed query → cosine similarity → top 5 results
- [x] FTS5 keyword fallback (used when MiniLM model not yet available)
- [x] Memory list with relative timestamps
- [x] Swipe-to-delete with undo snackbar
- [x] Tap-to-edit with re-embedding on save
- [x] Bottom navigation (Save / Search / All)
- [x] Material 3 theming with dynamic color (Android 12+)
- [x] Unit tests for NL parser (20 cases)

## What's next (Phase 2)

- [ ] Home screen widget (Glance API)
- [ ] Voice input (SpeechRecognizer)
- [ ] Location history table + UI
- [ ] Search result match explanation
- [ ] Premium paywall (Google Play Billing)
- [ ] Play Feature Delivery for deferred model download
