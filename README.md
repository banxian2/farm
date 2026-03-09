# Seeker Farm

Seeker Farm is a hybrid Android game that combines isometric farming simulation with Solana blockchain integration. The game runs in a WebView wrapper, bridging native Android features (like Solana wallet signing) with a JavaScript-based game engine.

## 🌾 Game Features

### Core Gameplay
- **Farming Cycle**: Plant seeds, water crops, and harvest produce.
- **Tools**:
  - `Seed`: Select from inventory to plant.
  - `Watering Can`: Accelerate crop growth (3 stages: Seed -> Sprout -> Ripe).
  - `Sickle`: Harvest ripe crops.
- **Economy**:
  - **Shop**: Buy seeds using in-game coins.
  - **Sell**: Sell harvested crops for profit. Prices are calculated dynamically based on growth time.
  - **Tasks**: Complete daily and achievement tasks (e.g., "Harvest 10 times", "Visit a friend") to earn rewards.

### Social Features
- **Friend System**: Add friends, view their profiles, and visit their farms.
- **Visitor Mode**: Explore friends' farms in a "View Only" mode (interaction restricted).
- **Chat**: Send text messages and gift items (seeds) to friends.
- **Invites**: Invite new users and claim "Grand Rewards" for reaching milestones.

### Blockchain Integration
- **Solana Wallet**: Login via Phantom or other Solana wallets.
- **Signature Auth**: Uses wallet signatures for secure API authentication (Shop purchases, etc.).

## 🏗 Project Structure

The project is an Android application with a WebView core.

### Native (Kotlin)
Located in `app/src/main/java/com/farm/seeker/`
- **`MainActivity.kt`**: The entry point, handles navigation and WebView setup.
- **`jsbridge/WebAppInterface.kt`**: The bridge between JavaScript and Android. Exposes methods like `buySeed`, `harvestCrop`, `getUserToken` to the WebView.
- **`solana/SolanaManager.kt`**: Manages wallet connection and signing.
- **`friend/`**: Handles friend lists, chat, and visiting logic.
- **`game/`**: Managers for Farm, Shop, and Inventory logic (native side).

### Web (JavaScript/HTML)
Located in `app/src/main/assets/`
- **`game.html`**: The main game container and UI overlays.
- **`game.js`**: The core game engine. Handles:
  - Isometric rendering loop.
  - Tile interaction (Click/Touch).
  - UI updates (Inventory, Shop, Toast messages).
  - API calls to the backend (or native bridge).

## 🛠 Tech Stack

- **Android**: Kotlin, XML Layouts, WebView.
- **Frontend**: HTML5, CSS3, Vanilla JavaScript (Canvas API).
- **Blockchain**: Solana Web3.js (injected via WebView or handled natively).
- **Build System**: Gradle.

## 🚀 Getting Started

1. **Prerequisites**:
   - Android Studio Koala or newer.
   - JDK 17+.
2. **Build**:
   - Open the project in Android Studio.
   - Sync Gradle files.
   - Run `AssembleDebug` or deploy to an emulator/device.
3. **Run**:
   - Ensure you have a Solana wallet installed (e.g., Phantom) on the device/emulator for full login functionality.

## 📝 License

Proprietary - Seeker Farm Team.

