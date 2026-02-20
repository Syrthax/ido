/* ===================================================
   GOOGLE DRIVE API HANDLER
   Handles OAuth authentication and file operations
   =================================================== */

// OAuth Configuration is loaded from config.js (not committed to git)
// See config.example.js for template

// State management
let accessToken = null;
let fileId = null;
let userInfo = null;

/* ===================================================
   PKCE HELPER FUNCTIONS
   =================================================== */

// Generate random string for PKCE
function generateRandomString(length) {
    const possible = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~';
    let text = '';
    for (let i = 0; i < length; i++) {
        text += possible.charAt(Math.floor(Math.random() * possible.length));
    }
    return text;
}

// Generate code verifier for PKCE
function generateCodeVerifier() {
    return generateRandomString(128);
}

// Generate code challenge from verifier
async function generateCodeChallenge(codeVerifier) {
    const encoder = new TextEncoder();
    const data = encoder.encode(codeVerifier);
    const digest = await crypto.subtle.digest('SHA-256', data);
    return base64UrlEncode(digest);
}

// Base64 URL encode
function base64UrlEncode(arrayBuffer) {
    const bytes = new Uint8Array(arrayBuffer);
    let binary = '';
    for (let i = 0; i < bytes.length; i++) {
        binary += String.fromCharCode(bytes[i]);
    }
    return btoa(binary)
        .replace(/\+/g, '-')
        .replace(/\//g, '_')
        .replace(/=/g, '');
}

/* ===================================================
   OAUTH FLOW
   =================================================== */

// Initiate OAuth login
async function initiateLogin() {
    // Generate PKCE values
    const codeVerifier = generateCodeVerifier();
    const codeChallenge = await generateCodeChallenge(codeVerifier);
    const state = generateRandomString(32);

    // Store verifier and state in session storage
    sessionStorage.setItem('code_verifier', codeVerifier);
    sessionStorage.setItem('oauth_state', state);

    // Build authorization URL
    const authUrl = new URL('https://accounts.google.com/o/oauth2/v2/auth');
    authUrl.searchParams.set('client_id', CONFIG.clientId);
    authUrl.searchParams.set('redirect_uri', CONFIG.redirectUri);
    authUrl.searchParams.set('response_type', 'code');
    authUrl.searchParams.set('scope', CONFIG.scope);
    authUrl.searchParams.set('state', state);
    authUrl.searchParams.set('code_challenge', codeChallenge);
    authUrl.searchParams.set('code_challenge_method', 'S256');
    authUrl.searchParams.set('access_type', 'offline');
    authUrl.searchParams.set('prompt', 'consent');
    authUrl.searchParams.set('include_granted_scopes', 'false');
    
    console.log('OAuth Scopes being requested:', CONFIG.scope);
    console.log('Full auth URL:', authUrl.toString());

    // Redirect to Google
    window.location.href = authUrl.toString();
}

// Handle OAuth callback
async function handleCallback() {
    const urlParams = new URLSearchParams(window.location.search);
    const code = urlParams.get('code');
    const state = urlParams.get('state');
    const error = urlParams.get('error');

    // Check for errors
    if (error) {
        console.error('OAuth error:', error);
        alert('Login failed: ' + error);
        window.location.href = CONFIG.redirectUri;
        return false;
    }

    // Check if we have a code
    if (!code) {
        return false;
    }

    // Verify state
    const savedState = sessionStorage.getItem('oauth_state');
    if (state !== savedState) {
        console.error('State mismatch');
        alert('Security error: state mismatch');
        return false;
    }

    // Exchange code for token
    const codeVerifier = sessionStorage.getItem('code_verifier');
    try {
        const tokenResponse = await fetch('https://oauth2.googleapis.com/token', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            body: new URLSearchParams({
                client_id: CONFIG.clientId,
                client_secret: CONFIG.clientSecret,
                code: code,
                code_verifier: codeVerifier,
                grant_type: 'authorization_code',
                redirect_uri: CONFIG.redirectUri
            })
        });

        const tokenData = await tokenResponse.json();

        if (tokenData.error) {
            throw new Error(tokenData.error_description || tokenData.error);
        }

        // Store access token
        accessToken = tokenData.access_token;
        
        // Store refresh token if available
        if (tokenData.refresh_token) {
            localStorage.setItem('refresh_token', tokenData.refresh_token);
        }
        
        // Store token with expiry
        const expiryTime = Date.now() + (tokenData.expires_in * 1000);
        localStorage.setItem('access_token', accessToken);
        localStorage.setItem('token_expiry', expiryTime.toString());

        // Clean up session storage
        sessionStorage.removeItem('code_verifier');
        sessionStorage.removeItem('oauth_state');

        // Clean up URL
        window.history.replaceState({}, document.title, CONFIG.redirectUri);

        return true;
    } catch (error) {
        console.error('Token exchange failed:', error);
        alert('Login failed: ' + error.message);
        return false;
    }
}

// Check if user is already logged in
function checkExistingAuth() {
    const token = localStorage.getItem('access_token');
    const expiry = localStorage.getItem('token_expiry');

    if (token && expiry) {
        if (Date.now() < parseInt(expiry)) {
            accessToken = token;
            return true;
        } else {
            // Token expired, try to refresh
            return refreshAccessToken();
        }
    }
    return false;
}

// Refresh access token
async function refreshAccessToken() {
    const refreshToken = localStorage.getItem('refresh_token');
    if (!refreshToken) return false;

    try {
        const response = await fetch('https://oauth2.googleapis.com/token', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            body: new URLSearchParams({
                client_id: CONFIG.clientId,
                client_secret: CONFIG.clientSecret,
                grant_type: 'refresh_token',
                refresh_token: refreshToken
            })
        });

        const data = await response.json();

        if (data.error) {
            throw new Error(data.error);
        }

        accessToken = data.access_token;
        const expiryTime = Date.now() + (data.expires_in * 1000);
        localStorage.setItem('access_token', accessToken);
        localStorage.setItem('token_expiry', expiryTime.toString());

        return true;
    } catch (error) {
        console.error('Token refresh failed:', error);
        logout();
        return false;
    }
}

// Logout
function logout() {
    accessToken = null;
    fileId = null;
    userInfo = null;
    localStorage.removeItem('access_token');
    localStorage.removeItem('token_expiry');
    localStorage.removeItem('refresh_token');
    localStorage.removeItem('file_id');
    localStorage.removeItem('user_info');
    window.location.href = CONFIG.redirectUri;
}

// Get user info from Google
async function getUserInfo() {
    if (!accessToken) {
        throw new Error('No access token available');
    }

    try {
        const response = await fetch('https://www.googleapis.com/oauth2/v2/userinfo', {
            headers: {
                'Authorization': `Bearer ${accessToken}`
            }
        });

        if (!response.ok) {
            throw new Error('Failed to fetch user info');
        }

        const data = await response.json();
        userInfo = {
            name: data.name,
            email: data.email,
            picture: data.picture
        };
        
        // Store in localStorage
        localStorage.setItem('user_info', JSON.stringify(userInfo));
        
        return userInfo;
    } catch (error) {
        console.error('Error fetching user info:', error);
        throw error;
    }
}

/* ===================================================
   GOOGLE DRIVE FILE OPERATIONS
   =================================================== */

// Find or create the ido-data.json file
async function findOrCreateFile() {
    try {
        // First, try to find existing file, ordered by modified time (most recent first)
        const searchResponse = await fetch(
            `https://www.googleapis.com/drive/v3/files?q=name='${CONFIG.fileName}' and trashed=false&orderBy=modifiedTime desc&fields=files(id,name,modifiedTime,size)`,
            {
                headers: {
                    'Authorization': `Bearer ${accessToken}`
                }
            }
        );

        const searchData = await searchResponse.json();

        if (searchData.files && searchData.files.length > 0) {
            // If multiple files exist, delete duplicates and keep the one with most recent data
            if (searchData.files.length > 1) {
                console.warn(`Found ${searchData.files.length} files with name '${CONFIG.fileName}'. Cleaning up duplicates...`);
                await cleanupDuplicateFiles(searchData.files);
            }
            
            // Use the most recently modified file
            fileId = searchData.files[0].id;
            localStorage.setItem('file_id', fileId);
            return fileId;
        } else {
            // File doesn't exist, create it
            return await createFile();
        }
    } catch (error) {
        console.error('Error finding file:', error);
        throw error;
    }
}

// Clean up duplicate files, keeping only the most recently modified one with data
async function cleanupDuplicateFiles(files) {
    try {
        // Keep the first file (most recently modified), delete the rest
        const filesToKeep = files[0];
        const filesToDelete = files.slice(1);
        
        console.log(`Keeping file: ${filesToKeep.id} (modified: ${filesToKeep.modifiedTime})`);
        
        for (const file of filesToDelete) {
            console.log(`Deleting duplicate file: ${file.id} (modified: ${file.modifiedTime})`);
            await fetch(
                `https://www.googleapis.com/drive/v3/files/${file.id}`,
                {
                    method: 'DELETE',
                    headers: {
                        'Authorization': `Bearer ${accessToken}`
                    }
                }
            );
        }
        
        console.log('Duplicate files cleaned up successfully');
    } catch (error) {
        console.error('Error cleaning up duplicate files:', error);
        // Don't throw - continue with the most recent file even if cleanup fails
    }
}

// Create new file with default data
async function createFile() {
    // Double-check if file exists before creating (race condition protection)
    try {
        const searchResponse = await fetch(
            `https://www.googleapis.com/drive/v3/files?q=name='${CONFIG.fileName}' and trashed=false&orderBy=modifiedTime desc`,
            {
                headers: {
                    'Authorization': `Bearer ${accessToken}`
                }
            }
        );
        
        const searchData = await searchResponse.json();
        if (searchData.files && searchData.files.length > 0) {
            console.log('File found during create check, using existing file instead');
            fileId = searchData.files[0].id;
            localStorage.setItem('file_id', fileId);
            return fileId;
        }
    } catch (error) {
        console.warn('Pre-create check failed, proceeding with creation:', error);
    }

    const defaultData = {
        tasks: []
    };

    const metadata = {
        name: CONFIG.fileName,
        mimeType: 'application/json'
    };

    const formData = new FormData();
    formData.append('metadata', new Blob([JSON.stringify(metadata)], { type: 'application/json' }));
    formData.append('file', new Blob([JSON.stringify(defaultData, null, 2)], { type: 'application/json' }));

    try {
        const response = await fetch(
            'https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart',
            {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${accessToken}`
                },
                body: formData
            }
        );

        const data = await response.json();
        fileId = data.id;
        localStorage.setItem('file_id', fileId);
        console.log('Created new file:', fileId);
        return fileId;
    } catch (error) {
        console.error('Error creating file:', error);
        throw error;
    }
}

// Load tasks from Google Drive
async function loadTasks() {
    if (!fileId) {
        await findOrCreateFile();
    }

    try {
        const response = await fetch(
            `https://www.googleapis.com/drive/v3/files/${fileId}?alt=media`,
            {
                headers: {
                    'Authorization': `Bearer ${accessToken}`
                }
            }
        );

        if (!response.ok) {
            throw new Error('Failed to load tasks');
        }

        const data = await response.json();
        
        // Check if migration is needed
        if (window.TaskSchema && typeof window.TaskSchema.migrateTasks === 'function') {
            console.log('Checking if tasks need migration...');
            const migratedData = window.TaskSchema.migrateTasks(data);
            
            // If migration occurred, save migrated data back
            const originalTaskCount = data.tasks?.length || 0;
            const needsMigration = data.tasks?.some(task => 
                window.TaskSchema.isOldFormat(task)
            ) || false;
            
            if (needsMigration && originalTaskCount > 0) {
                console.log('Tasks migrated, saving updated schema to Drive...');
                // Save migrated tasks back to Drive
                await saveTasks(migratedData.tasks);
            }
            
            return migratedData.tasks || [];
        }
        
        return data.tasks || [];
    } catch (error) {
        console.error('Error loading tasks:', error);
        return [];
    }
}

// Save tasks to Google Drive
async function saveTasks(tasks) {
    if (!fileId) {
        await findOrCreateFile();
    }

    const data = { tasks };

    try {
        const response = await fetch(
            `https://www.googleapis.com/upload/drive/v3/files/${fileId}?uploadType=media`,
            {
                method: 'PATCH',
                headers: {
                    'Authorization': `Bearer ${accessToken}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(data)
            }
        );

        if (!response.ok) {
            throw new Error('Failed to save tasks');
        }

        return true;
    } catch (error) {
        console.error('Error saving tasks:', error);
        throw error;
    }
}

/* ===================================================
   EXPORT FUNCTIONS
   =================================================== */

// Make functions available globally
window.DriveAPI = {
    initiateLogin,
    handleCallback,
    checkExistingAuth,
    logout,
    loadTasks,
    saveTasks,
    getUserInfo
};
