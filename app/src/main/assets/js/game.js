// Override alert with Android Toast
const originalAlert = window.alert;
window.alert = function(message) {
    const msgString = String(message);
    if (window.seeker && window.seeker.showToast) {
        window.seeker.showToast(msgString);
    } else {
        // Fallback for browser debugging
        console.log("Alert:", msgString);
        originalAlert(msgString);
    }
};

// Shop Configuration
let shopConfig = [
  
];

function getRandomCrop() {
    return shopConfig[Math.floor(Math.random() * shopConfig.length)];
}

// Game State
let currentTool = null;
const GRID_ROWS = 4;
const GRID_COLS = 3;

let coins = 0;
let gems = 0;
let level = 1;
let isFriendFarm = false;
let isLoggedIn = false;
let myTiles = [];
let originalOwnerInfo = null;

let isLandLoaded = false;
let isShopLoaded = false;
let isInventoryLoaded = false;

function updateLoadingOverlay() {
    const el = document.getElementById('loading-overlay');
    if (!el) return;
    const shouldShow = !(isLandLoaded && isShopLoaded && isInventoryLoaded);
    el.style.display = shouldShow ? 'flex' : 'none';
}

function resetLoadingState() {
    isLandLoaded = false;
    isShopLoaded = false;
    isInventoryLoaded = false;
    updateLoadingOverlay();
}

function getLandUnlockLevel(index) {
    return 1 + index * 5;
}

let tiles = Array(GRID_ROWS * GRID_COLS).fill(null).map((_, i) => {
    let requiredLevel = getLandUnlockLevel(i);
    let unlocked = level >= requiredLevel;
    let state = 'empty';
    let crop = null;
    
    if (unlocked) {
        state = 'planted';
        crop = getRandomCrop();
    }

    return { 
        x: i % GRID_COLS,
        y: Math.floor(i / GRID_COLS),
        state: state, 
        crop: crop, 
        watered: false,
        unlocked: unlocked
    };
});

function setLoginState(state) {
    isLoggedIn = state;
    updateUIForLoginState();
}

// User Info
let userInfo = {
    nickname: 'Seeker',
    avatarIndex: 0,
    userId: null,
    level: 1,
    billboard: ""
};

function getAvatarUrl(index) {
    let url;
    // Use appassets URL for WebViewAssetLoader compatibility
    // Maps to /res/drawable/ via WebViewAssetLoader.ResourcesPathHandler
    const baseUrl = "https://appassets.androidplatform.net/res/drawable";
    
    if (index >= 1 && index <= 9) {
        url = `${baseUrl}/avatar_m_${index}.png`;
    } else if (index >= 11 && index <= 19) {
        url = `${baseUrl}/avatar_f_${index - 10}.png`;
    } else {
        url = `${baseUrl}/avatar_m_1.png`;
    }
    console.log(`GameJS: getAvatarUrl index=${index} -> url=${url}`);
    return url;
}

function setUserInfo(nickname, avatarIndex, userId, newLevel, billboard) {
    console.log(`GameJS: setUserInfo nickname=${nickname}, avatarIndex=${avatarIndex}`);
    userInfo.nickname = nickname;
    userInfo.avatarIndex = avatarIndex;
    userInfo.userId = userId;
    userInfo.level = newLevel || 1;
    userInfo.billboard = billboard || "";
    level = userInfo.level; // Sync global level
    
    // Update Land Unlock Status based on new level
    updateTilesUnlockState();

    if (isLoggedIn) {
        updateUIForLoginState();
        if (userInfo.billboard) {
            setBillboard(userInfo.billboard);
        }
    }
}

function updateTilesUnlockState() {
    tiles.forEach((tile, index) => {
        const requiredLevel = getLandUnlockLevel(index);
        tile.unlocked = level >= requiredLevel;
    });
}

function setUserResources(newCoins, newGems) {
    coins = newCoins;
    gems = newGems;
    if (isLoggedIn) {
        updateResourceUI();
    }
}

function setBillboard(content) {
    if (!content) return;
    
    // Update userInfo if different (e.g. called from UI save)
    if (userInfo.billboard !== content) {
        userInfo.billboard = content;
    }

    const html = content.replace(/\n/g, '<br>');
    const billboardText = document.getElementById('billboard-text');
    if (billboardText) {
        billboardText.innerHTML = html;
    }
}

function setLandData(landData) {
    if (!landData || landData.length === 0) return;
    
    // landData is expected to be an array of tile objects
    // We need to map it to our tiles array
    
    // Reset tiles first? Or just overwrite?
    // Let's assume landData covers the whole grid or updates specific tiles
    // Format of landData items: { index: 0, state: 1, crop: {...}, ... }
    
    // For now, let's assume landData is just a simple array of states or objects
    // If it's the full tiles array:
    if (Array.isArray(landData)) {
        landData.forEach((item, index) => {
            if (index < tiles.length && item) {
                // Merge or replace
                // Ensure we keep necessary methods/properties if tiles are objects with methods
                // Our tiles are simple objects created in map()
                
                // If item has state, crop, etc.
                if (item.state !== undefined) tiles[index].state = item.state;
                
                // Handle crop: if it's a string ID, find the object in shopConfig
                if (item.crop) {
                    if (typeof item.crop === 'string') {
                        tiles[index].crop = shopConfig.find(c => c.id === item.crop) || null;
                    } else {
                        tiles[index].crop = item.crop;
                    }
                } else {
                    tiles[index].crop = null;
                }
                
                if (item.watered !== undefined) tiles[index].watered = item.watered;
                // unlocked is now strictly controlled by level via setUserInfo -> updateTilesUnlockState
                // if (item.unlocked !== undefined) tiles[index].unlocked = item.unlocked;
            }
        });
        // No need to call drawFarm() as renderLoop runs continuously
    }
}

function fetchLandList(retry = 0) {
    if (window.seeker && window.seeker.fetchLandList) {
        try {
            const jsonStr = window.seeker.fetchLandList();
            console.log("GameJS: fetchLandList jsonStr=" + jsonStr);
            const response = JSON.parse(jsonStr);
            if (response.code === 0 && response.data) {
                // Update local tiles based on server data
                const serverPlots = response.data;
                
                // Clear existing state first? Or just update?
                // Better to iterate over all tiles and reset if not in server response?
                // Server returns list of plots. Plot index corresponds to our tiles index.
                
                serverPlots.forEach(plot => {
                    const index = plot.plot_index;
                    if (index >= 0 && index < tiles.length) {
                        const tile = tiles[index];
                        
                        // Status: 0=Empty, 1=Growing, 2=Ripe (implied by is_ready?), 3=Withered?
                        // API doc says: status 1: Growing. is_ready: boolean.
                        
                        if (plot.status === 0) {
                            tile.state = 'empty';
                            tile.crop = null;
                            tile.watered = false;
                        } else if (plot.status === 1) {
                            tile.state = plot.is_ready ? 'ripe' : 'planted';
                            
                            // Resolve crop from seed_id
                            if (plot.seed_id) {
                                tile.crop = shopConfig.find(c => c.id === plot.seed_id) || {
                                    id: plot.seed_id,
                                    name: "Unknown",
                                    icon: "🌱",
                                    harvestTime: "??",
                                    yield: 1,
                                    price: 0
                                };
                            }
                            
                            // Check if watered today? API doesn't explicitly return is_watered boolean in list, 
                            // but has watered_at. We might need logic here.
                            // For now, let's assume if it was watered recently it's watered.
                            // Or maybe the API should return `is_watered`. 
                            // Looking at doc: watered_at is timestamp.
                            // Let's assume frontend just displays state.
                            // If we want to show wet soil, we need to know if it needs water.
                            // API doc doesn't show `needs_water` or `is_watered`.
                            // Let's assume watered_at implies it.
                            // Simple logic: if watered_at is present and recent? 
                            // Or maybe we just don't show wet state from API yet unless we add logic.
                            // Let's set watered = false default, or check if watered_at > last_reset.
                            
                            // For this implementation, let's assume the visual state 'planted' implies growing.
                            // We can use a client-side check if we had `server_time`.
                            
                            // Update: Doc says `watered_at`. 
                            // Let's just store it.
                             tile.watered = !!plot.watered_at; // Simple check if not null
                        }
                    }
                });
                isLandLoaded = true;
                updateLoadingOverlay();
            } else {
                if (retry < 2) {
                    setTimeout(() => fetchLandList(retry + 1), 800);
                } else {
                    isLandLoaded = true;
                    updateLoadingOverlay();
                }
            }
        } catch (e) {
            console.error("Error fetching land list:", e);
            if (retry < 2) {
                setTimeout(() => fetchLandList(retry + 1), 800);
            } else {
                isLandLoaded = true;
                updateLoadingOverlay();
            }
        }
    }
}

// SKR Token Integration
var skrBalance = "0";
const SKR_ADDRESS = "SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3";

function fetchUserData() {
    if (window.seeker) {
        resetLoadingState();
        // Fetch Login State
        if (window.seeker.isLoggedIn) {
            const loggedIn = window.seeker.isLoggedIn();
            setLoginState(loggedIn);
            
            if (!loggedIn) {
                console.log("GameJS: Not logged in, blocking data fetch");
                isLandLoaded = true;
                isShopLoaded = true;
                isInventoryLoaded = true;
                updateLoadingOverlay();
                return;
            }
        }

        // Fetch User Info
        if (window.seeker.getUserInfoJson) {
            try {
                const jsonStr = window.seeker.getUserInfoJson();
                console.log("GameJS: fetchUserData jsonStr=" + jsonStr);
                const data = JSON.parse(jsonStr);
                setUserInfo(data.nickname, data.avatarIndex, data.userId, data.level, data.billboard);
            } catch (e) {
                console.error("Error fetching user info:", e);
            }
        }

        // Fetch Resources
        if (window.seeker.getUserResourcesJson) {
            try {
                const jsonStr = window.seeker.getUserResourcesJson();
                console.log("GameJS: fetchUserResources jsonStr=" + jsonStr);
                const data = JSON.parse(jsonStr);
                setUserResources(data.coins, data.gems);

                // Fetch SKR Balance
                if (window.seeker.getTokenBalance) {
                     const result = window.seeker.getTokenBalance(SKR_ADDRESS);
                     console.log("GameJS: SKR Balance Result: " + result);
                     if (result && !result.startsWith("Error")) {
                         // Display only integer part
                         skrBalance = Math.floor(parseFloat(result)).toString();
                         updateResourceUI();
                     }
                }
            } catch (e) {
                console.error("Error fetching resources:", e);
            }
        }
        
        // Fetch Land Data
        if (window.seeker.getLandDataJson) {
             try {
                const jsonStr = window.seeker.getLandDataJson();
                const data = JSON.parse(jsonStr);
                setLandData(data);
             } catch (e) {
                 console.error("Error fetching land data:", e);
             }
        }

        // Fetch Real Land List from API
        fetchLandList(0);

        // Fetch Shop Config
        fetchShopConfig(0);

        // Fetch Inventory
        fetchInventory(0);
    }
}

function fetchShopConfig(retry = 0) {
    if (window.seeker && window.seeker.fetchShopSeeds) {
        try {
            const jsonStr = window.seeker.fetchShopSeeds();
            console.log("GameJS: fetchShopSeeds jsonStr=" + jsonStr);
            const response = JSON.parse(jsonStr);
            if (response.code === 0 && response.data && Array.isArray(response.data)) {
                shopConfig = response.data.map(item => {
                    let type = "short";
                    // Simple heuristic for type if needed for sorting/coloring
                    if (item.harvest_hours >= 48) type = "desert";
                    else if (item.harvest_hours >= 24) type = "tree";
                    else if (item.harvest_hours >= 12) type = "long";
                    else if (item.harvest_hours >= 6) type = "mid";

                    return {
                        id: item.seed_id,
                        name: item.name,
                        icon: item.icon,
                        price: item.price,
                        harvestTime: item.harvest_time_raw || (item.harvest_hours + "h"),
                        yield: item.yield,
                        harvestHours: item.harvest_hours,
                        type: type
                    };
                });
                console.log("GameJS: shopConfig updated from API with " + shopConfig.length + " items");
                // Refresh shop UI if open or available
                if (typeof renderShop === 'function') {
                    renderShop();
                }
                isShopLoaded = true;
                updateLoadingOverlay();
            } else {
                 console.warn("GameJS: fetchShopSeeds returned error or empty data", response);
                 if (retry < 2) {
                    setTimeout(() => fetchShopConfig(retry + 1), 800);
                 } else {
                    isShopLoaded = true;
                    updateLoadingOverlay();
                 }
            }
        } catch (e) {
            console.error("Error fetching shop config:", e);
            if (retry < 2) {
                setTimeout(() => fetchShopConfig(retry + 1), 800);
            } else {
                isShopLoaded = true;
                updateLoadingOverlay();
            }
        }
    }
}

function handleLoginClick() {
    if (window.seeker && window.seeker.login) {
        const result = window.seeker.login();
        console.log("Login Result: " + result);
        if (result && !result.startsWith("Error")) {
             fetchUserData();
        }
    } else {
        alert("Login feature not available");
    }
}

function updateUIForLoginState() {
    const topLeft = document.getElementById('top-left-panel');
    const bottomBar = document.getElementById('bottom-bar');
    const topRight = document.getElementById('top-right-panel');
    const billboard = document.getElementById('billboard');

    if (!isLoggedIn) {
        // Save original info if not saved yet
        if (topLeft && !originalOwnerInfo && !isFriendFarm) {
            originalOwnerInfo = topLeft.innerHTML;
        }

        // Show Connect Wallet Button (Hide Personal Info)
        if (topLeft) {
            topLeft.style.display = ''; // Ensure visible
            // Remove container styling to show only the button
            topLeft.style.background = 'none';
            topLeft.style.padding = '0';
            topLeft.style.boxShadow = 'none';
            
            topLeft.innerHTML = `
                <div class="connect-wallet-btn" onclick="handleLoginClick()" style="
                    background: #8D6E63; 
                    color: white; 
                    padding: 8px 12px; 
                    border-radius: 12px; 
                    font-weight: bold; 
                    cursor: pointer;
                    border: 2px solid #5D4037;
                    box-shadow: 0 4px 0 #5D4037;
                    font-family: monospace;
                    display: flex;
                    align-items: center;
                    gap: 5px;
                ">
                    <span>🔗</span> Connect Wallet
                </div>
            `;
        }

        // Disable interactions
        if (bottomBar) {
            bottomBar.style.pointerEvents = 'none';
            bottomBar.style.filter = 'grayscale(100%)';
            bottomBar.style.opacity = '0.7';
        }
        if (topRight) {
            topRight.style.pointerEvents = 'none';
            topRight.style.filter = 'grayscale(100%)';
            topRight.style.opacity = '0.7';
        }
        if (billboard) {
            billboard.style.pointerEvents = 'none';
        }
    } else {
        // Restore UI
        if (topLeft) {
            // Restore container styling if it was modified
            if (!isFriendFarm) {
                topLeft.style.background = 'rgba(255, 255, 255, 0.8)';
                topLeft.style.padding = '5px 15px 5px 5px';
                topLeft.style.boxShadow = '0 2px 4px rgba(0,0,0,0.2)';
            }
            
            // If originalOwnerInfo exists, restore it first (structure)
            if (originalOwnerInfo && !isFriendFarm) {
                 // Check if we currently have the button (meaning we were in logged out state)
                 if (topLeft.innerHTML.includes('Connect Wallet')) {
                     topLeft.innerHTML = originalOwnerInfo;
                 }
            }
            
            // ALWAYS update the info (nickname, avatar) regardless of restore
            // Update User Info
            if (userInfo.nickname) {
                const levelDisplay = document.getElementById('level-display');
                if (levelDisplay) {
                    levelDisplay.innerText = "LV." + userInfo.level + " " + userInfo.nickname;
                }
            }
            
            // Update Avatar
            const avatarDiv = document.querySelector('.avatar');
            if (avatarDiv) {
                const url = getAvatarUrl(userInfo.avatarIndex);
                avatarDiv.style.backgroundImage = `url('${url}')`;
            }
            
            // Restore Billboard
            if (userInfo.billboard) {
                setBillboard(userInfo.billboard);
            }
            
            updateResourceUI();
        }

        // Enable interactions
        if (bottomBar) {
            bottomBar.style.pointerEvents = 'auto';
            bottomBar.style.filter = 'none';
            bottomBar.style.opacity = '1';
        }

        if (topRight) {
            topRight.style.pointerEvents = 'auto';
            topRight.style.filter = 'none';
            topRight.style.opacity = '1';
        }
        if (billboard) {
            billboard.style.pointerEvents = 'auto';
        }
    }
}


// Canvas Configuration
let canvas, ctx;
let TILE_WIDTH = 120;
let TILE_HEIGHT = 60; // Isometric aspect ratio (2:1 usually good)
let ORIGIN_X, ORIGIN_Y;
let hoveredTile = null;

// Assets (Simple colors/emojis for now, can be images later)
const COLORS = {
    // Soil colors - richer browns with more warmth
    SOIL: '#C8B7A6', // Warm light brown
    SOIL_SIDE: '#9C8B7A', // Medium brown for sides
    SOIL_WET: '#8B6B4D', // Darker, richer brown for wet soil
    SOIL_WET_SIDE: '#6B4B2D', // Even darker for wet sides
    SOIL_HIGHLIGHT: '#E8D7C6', // Light highlight for hover
    SOIL_SHADOW: '#7D6B5A', // Shadow color for depth

    // Grass colors - more vibrant greens
    GRASS: '#7EC850', // Bright grass green
    GRASS_SIDE: '#5CA830', // Darker green for grass sides
    GRASS_HIGHLIGHT: '#9FE870', // Light green highlight

    // Border colors
    BORDER: '#5D4037', // Dark brown border
    LOCKED_BORDER: '#2E7D32', // Green border for locked tiles
    LOCKED_SIDE: '#1B5E20', // Darker green for locked sides

    // Additional colors for effects
    SHADOW_COLOR: 'rgba(0, 0, 0, 0.3)', // Shadow effect
    LOCK_GLOW: '#FFD54F', // Gold color for lock glow
    TEXT_SHADOW: 'rgba(0, 0, 0, 0.7)', // Text shadow
    CROP_BG: 'rgba(255, 255, 255, 0.2)', // Crop icon background
    WATER_COLOR: 'rgba(64, 164, 223, 0.3)', // Water color for wet soil
    WATER_HIGHLIGHT: 'rgba(128, 212, 255, 0.4)', // Water highlight
};

// Update initial UI
window.onload = function() {
    updateResourceUI();
    initCanvas();
    
    // Fetch initial data from Native
    setTimeout(() => fetchUserData(), 0);
    
    // Initial Render
    requestAnimationFrame(renderLoop);
};

function initCanvas() {
    canvas = document.getElementById('farm-canvas');
    ctx = canvas.getContext('2d');
    
    resizeCanvas();
    window.addEventListener('resize', resizeCanvas);
    
    // Input Handling
    canvas.addEventListener('mousemove', handleMouseMove);
    canvas.addEventListener('click', handleCanvasClick);
    canvas.addEventListener('mouseleave', () => { hoveredTile = null; });
}

function resizeCanvas() {
    const dpr = window.devicePixelRatio || 1;
    // Set internal size to match physical pixels
    canvas.width = window.innerWidth * dpr;
    canvas.height = window.innerHeight * dpr;
    
    // Set CSS size to match logical pixels
    canvas.style.width = `${window.innerWidth}px`;
    canvas.style.height = `${window.innerHeight}px`;
    
    // Scale all drawing operations
    ctx.scale(dpr, dpr);
    
    // Center the grid (Isometric centering)
    // Right align the grid (Rightmost corner touches screen edge)
    ORIGIN_X = window.innerWidth - (GRID_COLS * TILE_WIDTH / 2);
    
    // Vertically center (Moved down by adding 100px)
    ORIGIN_Y = window.innerHeight / 2 - (GRID_ROWS + GRID_COLS) * TILE_HEIGHT / 4 + 100;
}

// Coordinate Conversion
function isoToScreen(x, y) {
    return {
        x: ORIGIN_X + (x - y) * (TILE_WIDTH / 2),
        y: ORIGIN_Y + (x + y) * (TILE_HEIGHT / 2)
    };
}

function screenToIso(screenX, screenY) {
    const adjX = screenX - ORIGIN_X;
    const adjY = screenY - ORIGIN_Y;
    
    const halfW = TILE_WIDTH / 2;
    const halfH = TILE_HEIGHT / 2;
    
    const x = Math.floor((adjX / halfW + adjY / halfH) / 2);
    const y = Math.floor((adjY / halfH - adjX / halfW) / 2);
    
    return { x, y };
}

// Render Loop
function renderLoop() {
    ctx.clearRect(0, 0, window.innerWidth, window.innerHeight);
    
    // Draw Farm Tiles (Painter's Algorithm: Back to Front)
    // For isometric: (0,0) is top. (0,1) and (1,0) are next.
    // Standard loops x:0->Max, y:0->Max work for top-down iso rendering
    // because higher x+y means "lower" on screen.
    for (let x = 0; x < GRID_COLS; x++) {
        for (let y = 0; y < GRID_ROWS; y++) {
            drawTile(x, y);
        }
    }
    
    requestAnimationFrame(renderLoop);
}

function drawTile(x, y) {
    const index = y * GRID_COLS + x;
    const tile = tiles[index];
    const isUnlocked = isLoggedIn ? tile.unlocked : false;
    const pos = isoToScreen(x, y); // This is Top Vertex

    const isHovered = hoveredTile && hoveredTile.x === x && hoveredTile.y === y;
    const THICKNESS = 15;

    // Determine colors
    let topColor, sideColor, strokeColor;

    if (isUnlocked) {
        if (tile.watered) {
            topColor = COLORS.SOIL_WET;
            sideColor = COLORS.SOIL_WET_SIDE;
        } else {
            topColor = COLORS.SOIL;
            sideColor = COLORS.SOIL_SIDE;
        }

        if (isHovered) topColor = COLORS.SOIL_HIGHLIGHT;
        strokeColor = COLORS.BORDER;
    } else {
        topColor = COLORS.GRASS;
        sideColor = COLORS.GRASS_SIDE;
        strokeColor = COLORS.LOCKED_BORDER;
    }

    const halfW = TILE_WIDTH / 2;
    const halfH = TILE_HEIGHT / 2;

    // Vertices relative to pos (Top Vertex)
    // Top: (0, 0)
    // Right: (halfW, halfH)
    // Bottom: (0, TILE_HEIGHT)
    // Left: (-halfW, halfH)

    // Draw shadow for depth effect
    ctx.save();
    ctx.globalAlpha = 0.25;
    ctx.fillStyle = COLORS.SHADOW_COLOR;
    const shadowOffset = 4;

    // Shadow shape (same as top face but offset)
    ctx.beginPath();
    ctx.moveTo(pos.x + shadowOffset, pos.y + shadowOffset); // Top
    ctx.lineTo(pos.x + halfW + shadowOffset, pos.y + halfH + shadowOffset); // Right
    ctx.lineTo(pos.x + shadowOffset, pos.y + TILE_HEIGHT + shadowOffset); // Bottom
    ctx.lineTo(pos.x - halfW + shadowOffset, pos.y + halfH + shadowOffset); // Left
    ctx.closePath();
    ctx.fill();
    ctx.restore();

    // Draw Thickness (Sides) first - only Bottom-Right and Bottom-Left faces visible
    ctx.lineWidth = 1;
    ctx.strokeStyle = strokeColor;

    ctx.fillStyle = sideColor;

    // Right Side Face
    ctx.beginPath();
    ctx.moveTo(pos.x + halfW, pos.y + halfH); // Right
    ctx.lineTo(pos.x + halfW, pos.y + halfH + THICKNESS); // Right Lower
    ctx.lineTo(pos.x, pos.y + TILE_HEIGHT + THICKNESS); // Bottom Lower
    ctx.lineTo(pos.x, pos.y + TILE_HEIGHT); // Bottom
    ctx.closePath();
    ctx.fill();
    ctx.stroke();

    // Left Side Face
    ctx.beginPath();
    ctx.moveTo(pos.x, pos.y + TILE_HEIGHT); // Bottom
    ctx.lineTo(pos.x, pos.y + TILE_HEIGHT + THICKNESS); // Bottom Lower
    ctx.lineTo(pos.x - halfW, pos.y + halfH + THICKNESS); // Left Lower
    ctx.lineTo(pos.x - halfW, pos.y + halfH); // Left
    ctx.closePath();
    ctx.fill();
    ctx.stroke();

    // Draw Top Face (Diamond) with gradient for lighting effect
    ctx.beginPath();
    ctx.moveTo(pos.x, pos.y); // Top
    ctx.lineTo(pos.x + halfW, pos.y + halfH); // Right
    ctx.lineTo(pos.x, pos.y + TILE_HEIGHT); // Bottom
    ctx.lineTo(pos.x - halfW, pos.y + halfH); // Left
    ctx.closePath();

    // Create radial gradient for lighting effect
    const gradient = ctx.createRadialGradient(
        pos.x, pos.y + halfH, 0,
        pos.x, pos.y + halfH, TILE_WIDTH * 0.8
    );
    gradient.addColorStop(0, topColor);
    gradient.addColorStop(1, isUnlocked ? COLORS.SOIL_SHADOW : COLORS.GRASS_SIDE);

    ctx.fillStyle = gradient;
    ctx.fill();

    // Re-apply stroke here to ensure border is visible on top of fill
    ctx.stroke();

    // Add improved soil texture for unlocked tiles
    if (isUnlocked) {
        ctx.fillStyle = COLORS.SOIL_SIDE; // Use darker color for texture
        // Deterministic pseudo-random seed based on position
        let seed = (x * 12345 + y * 67890 + index);

        // Draw 4-6 small clumps with varied shapes
        const numClumps = 4 + (seed % 3);

        for (let i = 0; i < numClumps; i++) {
            seed = (seed * 9301 + 49297) % 233280;
            // Random position within the tile
            const offsetX = ((seed % 100) / 100 - 0.5) * (TILE_WIDTH * 0.6);

            seed = (seed * 9301 + 49297) % 233280;
            const offsetY = ((seed % 100) / 100 - 0.5) * (TILE_HEIGHT * 0.6);

            const clumpX = pos.x + offsetX;
            const clumpY = pos.y + TILE_HEIGHT / 2 + offsetY;

            // Draw clump (small ellipse with random rotation)
            ctx.save();
            ctx.translate(clumpX, clumpY);
            const rotation = (seed % 360) * Math.PI / 180;
            ctx.rotate(rotation);

            // Vary size slightly
            const sizeX = 3 + (seed % 4);
            const sizeY = 2 + (seed % 3);

            ctx.beginPath();
            ctx.ellipse(0, 0, sizeX, sizeY, 0, 0, Math.PI * 2);
            ctx.fill();
            ctx.restore();
        }

        // Add tiny specks for more texture
        ctx.fillStyle = COLORS.SOIL_HIGHLIGHT;
        seed = (x * 54321 + y * 98765 + index);
        const numSpecks = 8 + (seed % 5);

        for (let i = 0; i < numSpecks; i++) {
            seed = (seed * 6543 + 12345) % 99999;
            const offsetX = ((seed % 100) / 100 - 0.5) * (TILE_WIDTH * 0.8);
            seed = (seed * 6543 + 12345) % 99999;
            const offsetY = ((seed % 100) / 100 - 0.5) * (TILE_HEIGHT * 0.8);

            const speckX = pos.x + offsetX;
            const speckY = pos.y + TILE_HEIGHT / 2 + offsetY;

            ctx.beginPath();
            ctx.arc(speckX, speckY, 1, 0, Math.PI * 2);
            ctx.fill();
        }
    }

    // Draw Content (Crops, Signs)
    // Position center for content
    // Center of diamond is (pos.x, pos.y + halfH)
    const centerX = pos.x;
    const centerY = pos.y + halfH;

    if (!isUnlocked) {
        // Draw Lock with glow effect
        ctx.save();
        // Glow behind lock
        ctx.shadowColor = COLORS.LOCK_GLOW;
        ctx.shadowBlur = 8;
        drawEmoji(centerX, centerY - 10, '🔒', 30);
        ctx.restore();

        const reqLevel = getLandUnlockLevel(index);
        // Draw level text with shadow
        ctx.save();
        ctx.shadowColor = COLORS.TEXT_SHADOW;
        ctx.shadowBlur = 3;
        drawText(centerX, centerY + 20, `Lvl ${reqLevel}`, 12, '#fff');
        ctx.restore();
    } else {
        // Draw Crop if planted
        if (tile.crop) {
            // Show sprout for planted, actual icon for ripe
            const icon = tile.state === 'ripe' ? tile.crop.icon : '🌱';
            drawEmoji(centerX, centerY, icon, 32);
        } else if (tile.state === 'planted') {
            drawEmoji(centerX, centerY, '🌱', 32);
        }
    }
}


function drawEmoji(x, y, emoji, size) {
    // Draw shadow for better visibility
    ctx.save();
    ctx.font = `${size}px serif`;
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillStyle = 'rgba(0, 0, 0, 0.3)';
    ctx.fillText(emoji, x + 1, y + 1);
    ctx.restore();

    // Draw main emoji
    ctx.font = `${size}px serif`;
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(emoji, x, y);
}

function drawText(x, y, text, size, color) {
    // Draw text shadow for better readability
    ctx.save();
    ctx.font = `bold ${size}px Arial`;
    ctx.fillStyle = 'rgba(0, 0, 0, 0.5)';
    ctx.textAlign = 'center';
    ctx.fillText(text, x + 1, y + 1);
    ctx.restore();

    // Draw main text
    ctx.font = `bold ${size}px Arial`;
    ctx.fillStyle = color;
    ctx.textAlign = 'center';
    ctx.fillText(text, x, y);
}

// Interaction
function handleMouseMove(e) {
    const rect = canvas.getBoundingClientRect();
    const mouseX = e.clientX - rect.left;
    const mouseY = e.clientY - rect.top;
    
    const gridPos = screenToIso(mouseX, mouseY);
    
    if (gridPos.x >= 0 && gridPos.x < GRID_COLS && gridPos.y >= 0 && gridPos.y < GRID_ROWS) {
        hoveredTile = gridPos;
        if (!currentTool && !isFriendFarm) canvas.style.cursor = 'pointer';
    } else {
        hoveredTile = null;
        if (!currentTool) canvas.style.cursor = 'default';
    }
}

function handleCanvasClick(e) {
    if (!hoveredTile) return;
    
    const index = hoveredTile.y * GRID_COLS + hoveredTile.x;
    handleTileAction(index);
}

// Logic adapted from old handleTileClick
function handleTileAction(index) {
    const tile = tiles[index];
    
    // Calculate screen position for floating text
    const screenPos = isoToScreen(tile.x, tile.y);
    // Center of tile
    const cx = screenPos.x;
    const cy = screenPos.y + TILE_HEIGHT / 2;
    
    if (!isLoggedIn) {
        showFloatingText(cx, cy, "Please Login! 🔒");
        return;
    }
    
    if (isFriendFarm) {
        return;
    }

    if (!tile.unlocked) {
        showFloatingText(cx, cy, "Locked 🔒");
        return;
    }

    console.log(`Clicked tile ${index} with tool ${currentTool}`);
    
    if (currentTool === 'hoe') {
        if (tile.state === 'planted' || tile.state === 'ripe') {
            pendingHoeTileIndex = index;
            toggleModal('hoe-confirm-modal');
        } else {
            showFloatingText(cx, cy, "Empty 🧹");
        }
    } else if (!currentTool && tile.state === 'ripe') { // Harvest when clicked without tool
        if (window.seeker && window.seeker.harvestCrop) {
            try {
                const resStr = window.seeker.harvestCrop(index);
                const res = JSON.parse(resStr);
                if (res.code === 0) {
                    showFloatingText(cx, cy, "Harvested! 🌾");
                    fetchLandList();
                    fetchUserData(); // Update inventory
                } else {
                    showFloatingText(cx, cy, res.msg || "Error!");
                }
            } catch (e) {
                console.error("Harvest error:", e);
                showFloatingText(cx, cy, "Error!");
            }
        }
    } else if (currentTool === 'water') {
        if (!tile.watered) {
            if (window.seeker && window.seeker.waterPlant) {
                try {
                    const resStr = window.seeker.waterPlant(index);
                    const res = JSON.parse(resStr);
                    if (res.code === 0) {
                        tile.watered = true;
                        showFloatingText(cx, cy, "Watered 💧");
                        fetchLandList();
                    } else {
                        showFloatingText(cx, cy, res.msg || "Error!");
                    }
                } catch (e) {
                    console.error("Water error:", e);
                }
            }
        }
    } else if (currentTool === 'seed') {
        if (tile.state !== 'planted' && tile.state !== 'ripe') {
            if (selectedSeedItem && selectedSeedItem.count > 0) {
                if (window.seeker && window.seeker.plantSeed) {
                    try {
                        const resStr = window.seeker.plantSeed(index, selectedSeedItem.id);
                        const res = JSON.parse(resStr);
                        if (res.code === 0) {
                            tile.state = 'planted';
                            tile.crop = selectedSeedItem;
                            selectedSeedItem.count--;
                            showFloatingText(cx, cy, `Planted ${selectedSeedItem.name} 🌱`);
                            
                            if (selectedSeedItem.count === 0) {
                                selectedSeedItem = null;
                            }
                            fetchLandList();
                        } else {
                            showFloatingText(cx, cy, res.msg || "Error!");
                        }
                    } catch (e) {
                        console.error("Plant error:", e);
                    }
                }
            } else {
                 showFloatingText(cx, cy, "Select a seed! 🌱");
                 toggleModal('seed-modal');
                 renderSeedSelection();
            }
        }
    } else {
        // Default click (info?)
    }
}



function showFloatingText(x, y, text) {
    const float = document.createElement('div');
    float.textContent = text;
    float.style.position = 'absolute';
    float.style.color = 'gold';
    float.style.fontWeight = 'bold';
    float.style.fontSize = '24px';
    float.style.textShadow = '1px 1px 2px black';
    float.style.pointerEvents = 'none';
    float.style.left = `${x}px`;
    float.style.top = `${y}px`;
    float.style.transform = 'translate(-50%, -50%)';
    float.style.transition = 'all 1s ease-out';
    float.style.zIndex = '1000';
    
    document.body.appendChild(float);
    
    // Animation
    requestAnimationFrame(() => {
        float.style.top = `${y - 50}px`;
        float.style.opacity = '0';
    });
    
    setTimeout(() => {
        float.remove();
    }, 1000);
}

function updateResourceUI() {
    document.getElementById('coin-display').textContent = coins;
    document.getElementById('gem-display').textContent = skrBalance;
    const levelEl = document.getElementById('level-display');
    if (levelEl) {
        if (isLoggedIn && userInfo.nickname) {
            levelEl.textContent = `LV.${level} ${userInfo.nickname}`;
        } else {
            levelEl.textContent = `LV.${level} Farmer`;
        }
    }
}

// --- KEEPING EXISTING INVENTORY/SHOP/MODAL LOGIC BELOW ---

// Mock Inventory Data
let inventory = [];

function fetchInventory(retry = 0) {
    if (!isLoggedIn) return;
    if (window.seeker && window.seeker.fetchInventory) {
        try {
            const jsonStr = window.seeker.fetchInventory();
            console.log("GameJS: fetchInventory jsonStr=" + jsonStr);
            const response = JSON.parse(jsonStr);
            if (response.code === 0 && response.data && Array.isArray(response.data)) {
                inventory = response.data.map(item => ({
                    id: item.item_id,
                    name: item.item_name,
                    type: item.item_type || 'material',
                    count: item.count,
                    icon: item.icon || '📦',
                    price: item.price || 0
                }));
                console.log("GameJS: Inventory updated with " + inventory.length + " items");
                
                // Refresh UIs dependent on inventory
                if (typeof renderShop === 'function') renderShop();
                if (typeof renderSeedSelection === 'function') renderSeedSelection();
                if (typeof renderWarehouse === 'function') renderWarehouse(); // Assuming warehouse uses inventory
                isInventoryLoaded = true;
                updateLoadingOverlay();
            } else {
                console.warn("GameJS: fetchInventory failed or empty", response);
                if (retry < 2) {
                    setTimeout(() => fetchInventory(retry + 1), 800);
                } else {
                    isInventoryLoaded = true;
                    updateLoadingOverlay();
                }
            }
        } catch (e) {
            console.error("Error fetching inventory:", e);
            if (retry < 2) {
                setTimeout(() => fetchInventory(retry + 1), 800);
            } else {
                isInventoryLoaded = true;
                updateLoadingOverlay();
            }
        }
    }
}

// Current Sell/Buy State
let currentSellItem = null;
let sellQuantity = 1;
let currentBuyItem = null;
let buyQuantity = 1;

let selectedSeedItem = null;
let pendingHoeTileIndex = -1;

function renderSeedSelection() {
    const grid = document.getElementById('seed-grid');
    if (!grid) return;
    grid.innerHTML = '';

    const seeds = inventory.filter(item => {
        const idStr = String(item.id).toLowerCase();
        const nameStr = String(item.name).toLowerCase();
        // Check type first (from API), then fallback to ID/Name convention
        return (item.type && item.type.toLowerCase() === 'seed') || 
               idStr.includes('seed') || 
               nameStr.includes('seed');
    });

    for (let i = 0; i < 16; i++) {
        const item = seeds[i];
        const slot = document.createElement('div');
        slot.className = 'item-slot';
        
        if (item) {
             slot.innerHTML = `
                <div class="item-icon" style="font-size: 32px;">${item.icon}</div>
                <div class="item-count-badge">${item.count}</div>
            `;
            slot.onclick = () => selectSeed(item);
        }

        grid.appendChild(slot);
    }
}

function selectSeed(item) {
    selectedSeedItem = item;
    toggleModal('seed-modal');
    console.log(`Selected seed: ${item.name}`);
    showFloatingText(window.innerWidth/2, window.innerHeight/2, `Selected ${item.name} 🌱`);
}

function confirmHoe() {
    if (pendingHoeTileIndex === -1) return;
    
    if (window.seeker && window.seeker.shovelPlot) {
        try {
            const resStr = window.seeker.shovelPlot(pendingHoeTileIndex);
            const res = JSON.parse(resStr);
            if (res.code === 0) {
                 const tile = tiles[pendingHoeTileIndex];
                 // Calculate position for text
                 const screenPos = isoToScreen(tile.x, tile.y);
                 showFloatingText(screenPos.x, screenPos.y + TILE_HEIGHT/2, "Cleared 🧹");
                 fetchLandList();
            } else {
                 const tile = tiles[pendingHoeTileIndex];
                 const screenPos = isoToScreen(tile.x, tile.y);
                 showFloatingText(screenPos.x, screenPos.y + TILE_HEIGHT/2, res.msg || "Error!");
            }
        } catch(e) {
            console.error("Shovel error:", e);
        }
    }
    
    pendingHoeTileIndex = -1;
    toggleModal('hoe-confirm-modal');
}

// Shop Configuration moved to top


function renderShop() {
    const list = document.getElementById('shop-list');
    if (!list) return;
    list.innerHTML = '';

    shopConfig.forEach(item => {
        // Check inventory for owned count
        const invItem = inventory.find(i => i.id === item.id);
        const ownedCount = invItem ? invItem.count : 0;

        const card = document.createElement('div');
        card.className = 'shop-card';
        card.innerHTML = `
            <div class="ribbon">Sale</div>
            <div class="card-left">
                <div class="card-img-box">${item.icon}</div>
                <div class="card-owned">Owned: ${ownedCount}</div>
            </div>
            <div class="card-right">
                <div class="card-title">${item.name}</div>
                <div class="card-info-box">
                    <div class="info-row">Time: ${item.harvestTime.split('(')[0].trim()}</div>
                    <div class="info-row">Yield: ${item.yield}</div>
                    <div class="info-row price">Price: <span class="coin-icon">💰</span>${item.price}</div>
                </div>
                <button class="card-buy-btn" onclick="buyItem('${item.id}')">Buy</button>
            </div>
        `;
        list.appendChild(card);
    });
}

function buyItem(itemId) {
    const itemConfig = shopConfig.find(i => i.id === itemId);
    if (!itemConfig) return;

    currentBuyItem = itemConfig;
    buyQuantity = 1;

    // Check currently owned
    const invItem = inventory.find(i => i.id === itemConfig.id);
    const ownedCount = invItem ? invItem.count : 0;

    // Update Modal UI
    document.getElementById('buy-item-icon').textContent = itemConfig.icon;
    document.getElementById('buy-item-owned').textContent = ownedCount;
    document.getElementById('buy-item-name').textContent = itemConfig.name;
    document.getElementById('buy-unit-price').textContent = itemConfig.price;
    
    // Update Title
    const titleEl = document.getElementById('buy-modal-title');
    if (titleEl) titleEl.textContent = `Buy ${itemConfig.name}`;

    updateBuyUI();

    // Show modal
    toggleModal('buy-modal');
}

function updateBuyUI() {
    if (!currentBuyItem) return;
    document.getElementById('buy-qty').textContent = buyQuantity;
    document.getElementById('buy-total-price').textContent = buyQuantity * currentBuyItem.price;
}

function adjustBuyQty(delta) {
    if (!currentBuyItem) return;
    
    let newQty = buyQuantity + delta;
    if (newQty < 1) newQty = 1;
    
    // Optional: Check max affordable
    // const maxAffordable = Math.floor(coins / currentBuyItem.price);
    // if (newQty > maxAffordable && maxAffordable > 0) newQty = maxAffordable; 
    
    buyQuantity = newQty;
    updateBuyUI();
}

function setBuyMax() {
    if (!currentBuyItem) return;
    const maxAffordable = Math.floor(coins / currentBuyItem.price);
    buyQuantity = maxAffordable > 0 ? maxAffordable : 1;
    updateBuyUI();
}

function confirmBuy() {
    if (!currentBuyItem) return;

    const totalCost = currentBuyItem.price * buyQuantity;

    if (coins < totalCost) {
        alert('Not enough coins!');
        return;
    }

    if (window.seeker && window.seeker.buySeed) {
        // Native call
        console.log(`Buying ${buyQuantity} ${currentBuyItem.id}`);
        try {
            const responseStr = window.seeker.buySeed(currentBuyItem.id, buyQuantity);
            console.log("Buy response:", responseStr);
            const response = JSON.parse(responseStr);
            
            if (response.code === 0) {
                // Success
                const data = response.data;
                // Update coins
                if (data.coins !== undefined) {
                    coins = data.coins;
                } else {
                    coins -= totalCost; // Fallback
                }
                
                // Update inventory
                if (data.item) {
                    // Update or add to local inventory
                    // API item_id should match shop item id
                    const existingItem = inventory.find(i => i.id === data.item.item_id); 
                    if (existingItem) {
                        existingItem.count = data.item.count;
                    } else {
                         inventory.push({
                            id: data.item.item_id,
                            name: data.item.item_name,
                            type: data.item.item_type || 'seed',
                            count: data.item.count,
                            icon: data.item.icon,
                            price: Math.floor(currentBuyItem.price / 5) // Estimate resell
                        });
                    }
                } else {
                     // Fallback local update
                     let invItem = inventory.find(i => i.id === currentBuyItem.id);
                     if (invItem) {
                         invItem.count += buyQuantity;
                     } else {
                         inventory.push({
                             id: currentBuyItem.id,
                             name: currentBuyItem.name,
                             type: 'seed',
                             count: buyQuantity,
                             icon: currentBuyItem.icon,
                             price: Math.floor(currentBuyItem.price / 5)
                         });
                     }
                }

                updateResourceUI();
                renderShop(); // Refresh shop UI (owned counts)
                toggleModal('buy-modal');
                alert(`Successfully bought ${buyQuantity} ${currentBuyItem.name}!`);
                
                // Refresh Inventory List if needed (to be safe)
                fetchInventory();
            } else {
                alert(response.msg || "Purchase failed");
            }
        } catch (e) {
            console.error("Buy error:", e);
            alert("Error processing purchase");
        }
    } else {
        // Fallback to local logic (mock)
        coins -= totalCost;
        
        // Add to inventory
        let invItem = inventory.find(i => i.id === currentBuyItem.id);
        if (invItem) {
            invItem.count += buyQuantity;
        } else {
            inventory.push({
                id: currentBuyItem.id,
                name: currentBuyItem.name,
                type: 'seed', // Default type for seeds
                count: buyQuantity,
                icon: currentBuyItem.icon,
                price: Math.floor(currentBuyItem.price / 5) // Resell price
            });
        }

        updateResourceUI();
        renderShop(); // Refresh shop UI (owned counts)
        
        console.log(`Bought ${buyQuantity} ${currentBuyItem.name} for ${totalCost} coins`);
        
        // Close modal
        toggleModal('buy-modal');
    }
}

function returnToShop() {
    toggleModal('shop-modal');
}

// Billboard Editing
function openBillboardEdit() {
    const billboardContent = document.getElementById('billboard-text').innerHTML;
    // Replace <br> with newlines for editing
    const text = billboardContent.replace(/<br\s*\/?>/gi, '\n').trim();
    document.getElementById('billboard-input').value = text;
    toggleModal('billboard-modal');
}

function saveBillboardText() {
    const input = document.getElementById('billboard-input').value;
    
    // Check length limit
    if (input.length > 60) {
        alert('Content cannot exceed 60 characters');
        return;
    }

    // Check line limit
    const lines = input.split('\n');
    if (lines.length > 4) {
        alert('Content cannot exceed 4 lines');
        return;
    }

    // Replace newlines with <br> for display
    const html = input.replace(/\n/g, '<br>');
    document.getElementById('billboard-text').innerHTML = html;
    
    // Call Native API to save
    if (window.seeker && window.seeker.saveBillboard) {
        window.seeker.saveBillboard(input);
    }
    
    toggleModal('billboard-modal');
}

// UI Interaction
function updateOverlayState() {
    const anyModalActive = document.querySelector('.modal.active');
    const overlay = document.getElementById('modal-overlay');
    if (overlay) {
        if (anyModalActive) {
            overlay.classList.add('active');
        } else {
            overlay.classList.remove('active');
        }
    }

    if (window.seeker && window.seeker.setSwipeRefreshEnabled) {
        window.seeker.setSwipeRefreshEnabled(!anyModalActive);
    }
}

// Task System
function fetchTasks() {
    if (!isLoggedIn) return;
    const list = document.querySelector('#tasks-modal .modal-content');
    if (list) {
        list.innerHTML = '<div style="text-align: center; padding: 20px; color: #5D4037;">Loading tasks...</div>';
    }

    if (window.seeker && window.seeker.fetchTasks) {
        console.log("GameJS: calling native fetchTasks");
        const jsonString = window.seeker.fetchTasks();
        onTasksFetched(jsonString);
    } else {
        const mockTasks = [
            { task_id: 1, title: 'Mock Task 1', description: 'Water 5 plants', current_value: 2, target_value: 5, is_completed: 0, is_claimed: 0 },
            { task_id: 2, title: 'Mock Task 2', description: 'Harvest 10 crops', current_value: 10, target_value: 10, is_completed: 1, is_claimed: 0 },
            { task_id: 3, title: 'Mock Task 3', description: 'Visit 3 friends', current_value: 3, target_value: 3, is_completed: 1, is_claimed: 1 }
        ];
        setTimeout(() => {
            onTasksFetched(JSON.stringify({ code: 200, data: mockTasks }));
        }, 500);
    }
}

function onTasksFetched(jsonString) {
    console.log("GameJS: onTasksFetched", jsonString);
    try {
        let response;
        if (typeof jsonString === 'string') {
            response = JSON.parse(jsonString);
        } else {
            response = jsonString;
        }

        // Handle loose equality for code and check for data array
        if ((response.code == 0) && response.data && Array.isArray(response.data)) {
            renderTasks(response.data);
        } else {
            console.error("Invalid task response:", response);
            const list = document.querySelector('#tasks-modal .modal-content');
            if (list) {
                const msg = response.msg || "Failed to load tasks.";
                list.innerHTML = `<div style="text-align: center; padding: 20px; color: #5D4037;">${msg}</div>`;
            }
        }
    } catch (e) {
        console.error("Error parsing task data:", e);
        const list = document.querySelector('#tasks-modal .modal-content');
        if (list) list.innerHTML = `<div style="text-align: center; padding: 20px; color: #5D4037;">Error: ${e.message}</div>`;
    }
}

function renderTasks(tasks) {
    const list = document.querySelector('#tasks-modal .modal-content');
    if (!list) return;
    list.innerHTML = '';

    if (tasks.length === 0) {
        list.innerHTML = '<div style="text-align: center; padding: 20px; color: #5D4037;">No tasks available.</div>';
        return;
    }

    tasks.forEach(task => {
        // Fallback for ID: check task_id first, then id
        const taskId = task.task_id || task.id;
        if (!taskId) {
            console.error("Task missing ID:", task);
            return;
        }

        const item = document.createElement('div');
        item.className = 'task-item';
        
        let btnClass = 'buy-btn claim-btn';
        let btnText = 'Claim';
        let btnDisabled = false;
        let btnOnClick = `claimTask(${taskId})`;

        // Ensure numeric comparison
        const isClaimed = parseInt(task.is_claimed || 0) === 1;
        const isCompleted = parseInt(task.is_completed || 0) === 1;
        const currentVal = parseInt(task.current_value || 0);
        const targetVal = parseInt(task.target_value || 1);

        if (isClaimed) {
            btnClass += ' claimed';
            btnText = 'Claimed';
            btnDisabled = true;
            btnOnClick = '';
        } else if (!isCompleted) {
            btnClass += ' claimed'; // Reuse claimed style for disabled/progress state
            btnText = `${currentVal}/${targetVal}`;
            btnDisabled = true;
            btnOnClick = '';
        }
        
        item.innerHTML = `
            <div>
                <strong>${task.title || 'Unknown Task'}</strong><br>
                <small>${task.description || ''}</small>
            </div>
            <button class="${btnClass}" ${btnDisabled ? 'disabled' : ''} ${btnOnClick ? `onclick="${btnOnClick}"` : ''}>${btnText}</button>
        `;
        list.appendChild(item);
    });
}

function claimTask(taskId) {
    if (window.seeker && window.seeker.claimTask) {
        const jsonString = window.seeker.claimTask(taskId);
        onTaskClaimResult(jsonString);
    } else {
        console.log(`Mock Claim Task ${taskId}`);
        onTaskClaimResult(JSON.stringify({ code: 200, msg: "Mock Success" }));
    }
}

function onTaskClaimResult(jsonString) {
    console.log("GameJS: onTaskClaimResult", jsonString);
    try {
        const response = JSON.parse(jsonString);
        if (response.code === 0) {
            if (window.seeker && window.seeker.showToast) {
                window.seeker.showToast("Task Claimed!");
            }
            fetchTasks();
            fetchUserData();
        } else {
             if (window.seeker && window.seeker.showToast) {
                window.seeker.showToast("Claim Failed: " + (response.msg || "Unknown error"));
            }
        }
    } catch (e) {
        console.error("Error parsing claim result:", e);
    }
}

function toggleModal(modalId) {
    const modal = document.getElementById(modalId);
    if (!modal) return;
    
    // Close others if opening
    if (!modal.classList.contains('active')) {
        document.querySelectorAll('.modal').forEach(m => {
            m.classList.remove('active');
        });
    }

    modal.classList.toggle('active');
    updateOverlayState();

    const isActive = modal.classList.contains('active');

    // If opening warehouse, render initial tab
    if (modalId === 'warehouse-modal' && isActive) {
        switchTab('seed');
    }

    // If opening shop, render shop items
    if (modalId === 'shop-modal' && isActive) {
        renderShop();
    }

    // If opening tasks, fetch tasks
    if (modalId === 'tasks-modal' && isActive) {
        fetchTasks();
    }

    // If closing warehouse, deselect bag tool
    if (modalId === 'warehouse-modal' && !isActive) {
        if (currentTool === 'bag') {
            currentTool = null;
            document.querySelectorAll('.tool-slot').forEach(slot => {
                slot.classList.remove('selected');
            });
        }
    }
}

function selectTool(tool) {
    currentTool = tool;
    
    // Update UI
    document.querySelectorAll('.tool-slot').forEach(slot => {
        slot.classList.remove('selected');
    });
    
    // Find the clicked slot
    const slots = {
        'hoe': 0, 'water': 1, 'seed': 2, 'bag': 3
    };
    
    const index = slots[tool];
    if (index !== undefined) {
        document.querySelectorAll('.tool-slot')[index].classList.add('selected');
    }

    // Update cursor context on game container
    const container = document.getElementById('game-container');
    if (container) {
        container.className = ''; // Reset
        canvas.style.cursor = ''; // Reset inline cursor
        if (tool) {
            container.classList.add(`tool-${tool}`);
        }
    }
    
    console.log(`Tool selected: ${tool}`);

    // If bag selected, open warehouse
    if (tool === 'bag') {
        toggleModal('warehouse-modal');
    }
    
    // If seed selected, open seed selection
    if (tool === 'seed') {
        // Fetch inventory to ensure we have latest seeds
        fetchInventory();
        renderSeedSelection();
        toggleModal('seed-modal');
    }
}

// Warehouse Logic
function switchTab(type) {
    // Update active tab UI
    document.querySelectorAll('.warehouse-tabs .tab').forEach(tab => {
        tab.classList.remove('active');
        if (tab.getAttribute('onclick').includes(type)) {
            tab.classList.add('active');
        }
    });

    renderWarehouse(type);
}

function renderWarehouse(type) {
    const grid = document.getElementById('warehouse-grid');
    grid.innerHTML = ''; // Clear existing

    // Filter items
    const items = inventory.filter(item => item.type === type || (type === 'other' && !['seed', 'material', 'tool', 'chest'].includes(item.type)));

    // Create slots (always fill at least 16 slots for grid look)
    for (let i = 0; i < 16; i++) {
        const item = items[i];
        const slot = document.createElement('div');
        slot.className = 'item-slot';
        
        if (item) {
            slot.innerHTML = `
                <div class="item-icon" style="font-size: 32px;">${item.icon}</div>
                <div class="item-count-badge">${item.count}</div>
            `;
            slot.onclick = () => openSellModal(item);
        }

        grid.appendChild(slot);
    }
}

// Sell Logic
function openSellModal(item) {
    currentSellItem = item;
    sellQuantity = 1;

    // Update UI
    document.getElementById('sell-item-icon').textContent = item.icon;
    document.getElementById('sell-item-owned').textContent = item.count;
    document.getElementById('sell-item-name').textContent = item.name;
    
    // Calculate Dynamic Sell Price
    let sellPrice = item.price; 
    let rawSellPrice = item.price;

    // Try to find seed data in shopConfig
    const seedId = item.id + '_seed';
    const saplingId = item.id + '_sapling';
    const seed = shopConfig.find(s => s.id === seedId || s.id === saplingId);
    
    if (seed) {
        // Formula: P = (C * (1 + 0.12 * T)) / Y
        const C = seed.price;
        const T = seed.harvestHours || 0;
        const Y = seed.yield > 0 ? seed.yield : 1;
        rawSellPrice = (C * (1 + 0.12 * T)) / Y;
        sellPrice = parseFloat(rawSellPrice.toFixed(2));
    }
    
    currentSellItem.rawSellPrice = rawSellPrice;
    document.getElementById('sell-unit-price').textContent = sellPrice;
    
    updateSellUI();

    // Show modal
    document.getElementById('sell-modal').classList.add('active');
    updateOverlayState();
}

function updateSellUI() {
    if (!currentSellItem) return;
    
    document.getElementById('sell-qty').textContent = sellQuantity;
    
    const price = currentSellItem.rawSellPrice !== undefined ? currentSellItem.rawSellPrice : currentSellItem.price;
    const total = Math.floor(price * sellQuantity);
    
    document.getElementById('sell-total-price').textContent = total;
}

function adjustSellQty(change) {
    if (!currentSellItem) return;

    let newQty = sellQuantity + change;
    if (newQty < 1) newQty = 1;
    if (newQty > currentSellItem.count) newQty = currentSellItem.count;

    sellQuantity = newQty;
    updateSellUI();
}

function setMaxSell() {
    if (!currentSellItem) return;
    sellQuantity = currentSellItem.count;
    updateSellUI();
}

function confirmSell() {
    if (!currentSellItem) return;

    // Logic to sell via Android Native
    if (window.seeker && window.seeker.sellCrop) {
        try {
            const resStr = window.seeker.sellCrop(currentSellItem.id, sellQuantity);
            console.log("GameJS: sellCrop result=" + resStr);
            const res = JSON.parse(resStr);
            
            if (res.code === 0) {
                // Success
                const totalIncome = res.data.total_price || (currentSellItem.price * sellQuantity);
                const newBalance = res.data.new_balance;
                
                // Update coins locally for immediate feedback
                if (newBalance !== undefined) {
                    coins = newBalance;
                } else {
                    coins += totalIncome;
                }
                updateResourceUI();
                
                // Update inventory locally
                currentSellItem.count -= sellQuantity;
                if (currentSellItem.count <= 0) {
                     const index = inventory.indexOf(currentSellItem);
                     if (index > -1) inventory.splice(index, 1);
                }

                // UI Feedback
                console.log(`Sold ${sellQuantity} ${currentSellItem.name} for ${totalIncome} coins`);
                if (window.seeker.showToast) window.seeker.showToast(`Sold for ${totalIncome} coins! 💰`);
                
                // Close modal
                document.getElementById('sell-modal').classList.remove('active');
                updateOverlayState();
                
                // Refresh warehouse UI
                const activeTab = document.querySelector('.warehouse-tabs .tab.active');
                let currentType = 'seed';
                if (activeTab) {
                    const match = activeTab.getAttribute('onclick').match(/'(\w+)'/);
                    if (match) currentType = match[1];
                }
                renderWarehouse(currentType);

                if (window.seeker && window.seeker.getUserResourcesJson) {
                    try {
                        const resourcesStr = window.seeker.getUserResourcesJson();
                        const resources = JSON.parse(resourcesStr);
                        setUserResources(resources.coins, resources.gems);
                    } catch (e) {
                        console.error("Error fetching resources after sell:", e);
                    }
                }

                fetchInventory();

            } else {
                // Error
                console.error("Sell error:", res.msg);
                if (window.seeker.showToast) window.seeker.showToast(res.msg || "Sell failed!");
            }
        } catch (e) {
            console.error("Sell exception:", e);
            if (window.seeker.showToast) window.seeker.showToast("Error processing sell!");
        }
    } else {
        // Fallback for browser testing (or old logic)
        console.warn("Native sellCrop not available");
        
        // Logic to sell
        const totalIncome = currentSellItem.price * sellQuantity;
        coins += totalIncome;
        updateResourceUI();
        
        // Update inventory
        currentSellItem.count -= sellQuantity;
        if (currentSellItem.count <= 0) {
            // Remove item from inventory if count is 0
            const index = inventory.indexOf(currentSellItem);
            if (index > -1) {
                inventory.splice(index, 1);
            }
        }

        // Update UI
        console.log(`Sold ${sellQuantity} ${currentSellItem.name} for ${totalIncome} coins`);
        
        // Close sell modal
        document.getElementById('sell-modal').classList.remove('active');
        updateOverlayState();
        
        // Refresh warehouse
        const activeTab = document.querySelector('.warehouse-tabs .tab.active');
        let currentType = 'seed';
        if (activeTab) {
            // Extract type from onclick attribute roughly
            const match = activeTab.getAttribute('onclick').match(/'(\w+)'/);
            if (match) currentType = match[1];
        }
        renderWarehouse(currentType);
    }
}

// Friend Visit Logic
function visitFriend(friendId, friendName = 'Friend', friendLevel = 10, friendAvatar = '') {
    console.log(`Visiting friend: ${friendId}`);
    if (isFriendFarm) return;

    // Close all modals
    document.querySelectorAll('.modal.active').forEach(modal => {
        modal.classList.remove('active');
    });
    updateOverlayState();
    
    // Save my state
    myTiles = JSON.parse(JSON.stringify(tiles));
    isFriendFarm = true;

    // Generate friend's farm (randomly)
    tiles = Array(GRID_ROWS * GRID_COLS).fill(null).map((_, i) => {
        const unlocked = i < 9; 
        let state = 'empty';
        let crop = null;
        if (unlocked) {
            const rand = Math.random();
            if (rand > 0.3) {
                state = 'planted';
                crop = getRandomCrop();
                if (Math.random() > 0.5) state = 'ripe';
            }
        }
        return { 
            x: i % GRID_COLS,
            y: Math.floor(i / GRID_COLS),
            state: state, 
            crop: crop, 
            watered: Math.random() > 0.5,
            unlocked: unlocked
        };
    });

    // Update Top Left Panel (Owner Info)
    const topLeftPanel = document.getElementById('top-left-panel');
    if (topLeftPanel) {
        if (!originalOwnerInfo) originalOwnerInfo = topLeftPanel.innerHTML;
        
        const avatarStyle = friendAvatar ? `background-image: url('${friendAvatar}'); background-size: cover;` : 'background-color: #FFAB91;';
        
        topLeftPanel.innerHTML = `
            <div class="avatar" style="${avatarStyle}"></div>
            <div class="stats">
                <div class="level-bar">LV.${friendLevel} ${friendName}</div>
            </div>
        `;
    }

    // Hide Owner UI elements
    const elementsToHide = ['top-right-panel', 'bottom-bar'];
    elementsToHide.forEach(id => {
        const el = document.getElementById(id);
        if (el) el.style.display = 'none';
    });

    // Show Back Button
    const backBtn = document.getElementById('back-home-btn');
    if (backBtn) backBtn.style.display = 'flex';
    
    showFloatingText(window.innerWidth / 2, window.innerHeight / 2, `Visited ${friendName}! 🏠`);
}

function enterOwnerMode() {
    console.log('Returning to home farm');
    if (!isFriendFarm) return;
    
    isFriendFarm = false;
    tiles = myTiles;
    
    // Notify Native Android
    if (window.seeker && window.seeker.onReturnHome) {
        window.seeker.onReturnHome();
    }
    
    // Restore Top Left Panel
    const topLeftPanel = document.getElementById('top-left-panel');
    if (topLeftPanel && originalOwnerInfo) {
        topLeftPanel.innerHTML = originalOwnerInfo;
        updateResourceUI(); // Restore dynamic values
    }

    // Restore Owner UI elements
    const elementsToRestore = ['top-right-panel', 'bottom-bar'];
    elementsToRestore.forEach(id => {
        const el = document.getElementById(id);
        if (el) el.style.display = '';
    });

    // Hide Back Button
    const backBtn = document.getElementById('back-home-btn');
    if (backBtn) backBtn.style.display = 'none';
    
    currentTool = null;
}
