// Google Drive API Configuration
// NOTE: This is a legacy file. The main app is in /web/ folder.
// Credentials are injected via GitHub Actions for production.
const CLIENT_ID = ""; // Set via environment or use /web/ version
const API_KEY = ""; // Set via environment or use /web/ version
const SCOPES = "https://www.googleapis.com/auth/drive.file";
const DISCOVERY_DOCS = ["https://www.googleapis.com/discovery/v1/apis/drive/v3/rest"];

// State
let tasks = [];
let isGoogleSignedIn = false;
let isGoogleDriveConnected = false;
let accessToken = null;
let driveFileId = null;

// DOM Elements
const taskInput = document.getElementById('taskInput');
const addTaskBtn = document.getElementById('addTaskBtn');
const tasksList = document.getElementById('tasksList');
const completedList = document.getElementById('completedList');
const completedToggle = document.getElementById('completedToggle');
const completedChevron = document.getElementById('completedChevron');
const completedCount = document.getElementById('completedCount');
const googleSignInBtn = document.getElementById('googleSignInBtn');
const signInStatus = document.getElementById('signInStatus');
const driveConnectBtn = document.getElementById('driveConnectBtn');
const driveStatus = document.getElementById('driveStatus');
const cookieModal = document.getElementById('cookieModal');
const closeCookieModal = document.getElementById('closeCookieModal');

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    lucide.createIcons();
    loadTasks();
    checkGoogleSignIn();
    setupEventListeners();
});

// Event Listeners
function setupEventListeners() {
    addTaskBtn.addEventListener('click', handleAddTask);
    taskInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') handleAddTask();
    });
    completedToggle.addEventListener('click', toggleCompletedSection);
    googleSignInBtn.addEventListener('click', handleGoogleSignIn);
    driveConnectBtn.addEventListener('click', handleGoogleDriveConnect);
    closeCookieModal.addEventListener('click', hideCookieModal);
    cookieModal.addEventListener('click', (e) => {
        if (e.target === cookieModal) hideCookieModal();
    });
}

// Cookie Modal Functions
function showCookieModal() {
    cookieModal.classList.add('show');
    lucide.createIcons();
}

function hideCookieModal() {
    cookieModal.classList.remove('show');
}

// Add Task
function handleAddTask() {
    const text = taskInput.value.trim();
    if (!text) return;

    addTask(text);
    taskInput.value = '';
    taskInput.focus();
}

function addTask(text) {
    const task = {
        id: Date.now(),
        text: text,
        completed: false,
        createdAt: new Date().toISOString()
    };

    tasks.push(task);
    saveTasks();
    renderTasks();
}

// Delete Task
function deleteTask(id) {
    tasks = tasks.filter(task => task.id !== id);
    saveTasks();
    renderTasks();
}

// Edit Task
function editTask(id, newText) {
    const task = tasks.find(t => t.id === id);
    if (task) {
        task.text = newText.trim();
        saveTasks();
        renderTasks();
    }
}

// Toggle Complete
function toggleComplete(id) {
    const task = tasks.find(t => t.id === id);
    if (task) {
        task.completed = !task.completed;
        saveTasks();
        renderTasks();
    }
}

// Render Tasks
function renderTasks() {
    const activeTasks = tasks.filter(t => !t.completed);
    const completedTasks = tasks.filter(t => t.completed);

    // Render active tasks
    if (activeTasks.length === 0) {
        tasksList.innerHTML = `
            <div class="empty-state">
                <i data-lucide="check-circle-2"></i>
                <p>No tasks yet. Add one to get started!</p>
            </div>
        `;
    } else {
        tasksList.innerHTML = activeTasks.map(task => createTaskElement(task)).join('');
    }

    // Render completed tasks
    if (completedTasks.length === 0) {
        completedList.innerHTML = `
            <div class="empty-state">
                <p>No completed tasks</p>
            </div>
        `;
    } else {
        completedList.innerHTML = completedTasks.map(task => createTaskElement(task)).join('');
    }

    // Update completed count
    completedCount.textContent = completedTasks.length;

    // Reinitialize icons
    lucide.createIcons();
    
    // Add event listeners to task elements
    attachTaskEventListeners();
}

// Create Task Element
function createTaskElement(task) {
    return `
        <div class="task-item" data-id="${task.id}">
            <div class="task-checkbox ${task.completed ? 'checked' : ''}" onclick="toggleComplete(${task.id})">
                <i data-lucide="check"></i>
            </div>
            <div 
                class="task-text ${task.completed ? 'completed' : ''}" 
                contenteditable="true"
                data-id="${task.id}"
            >${task.text}</div>
            <div class="task-actions">
                <button class="task-btn delete" onclick="deleteTask(${task.id})">
                    <i data-lucide="trash-2"></i>
                </button>
            </div>
        </div>
    `;
}

// Attach Task Event Listeners
function attachTaskEventListeners() {
    document.querySelectorAll('.task-text[contenteditable]').forEach(element => {
        element.addEventListener('blur', (e) => {
            const id = parseInt(e.target.dataset.id);
            const newText = e.target.textContent;
            if (newText.trim()) {
                editTask(id, newText);
            } else {
                renderTasks(); // Restore original text if empty
            }
        });

        element.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                e.target.blur();
            }
        });
    });
}

// Toggle Completed Section
function toggleCompletedSection() {
    const isCollapsed = completedList.classList.toggle('collapsed');
    completedToggle.classList.toggle('expanded', !isCollapsed);
}

// Save Tasks
function saveTasks() {
    localStorage.setItem('ido_tasks', JSON.stringify(tasks));
    
    if (isGoogleDriveConnected) {
        syncToGoogleDrive(tasks);
    }
}

// Load Tasks
function loadTasks() {
    const stored = localStorage.getItem('ido_tasks');
    if (stored) {
        tasks = JSON.parse(stored);
        renderTasks();
    }
    
    // Try loading from Google Drive if connected
    if (isGoogleDriveConnected) {
        loadFromGoogleDrive();
    }
}

// Google Sign-In
function checkGoogleSignIn() {
    accessToken = localStorage.getItem('google_access_token');
    const userEmail = localStorage.getItem('google_user_email');
    driveFileId = localStorage.getItem('google_drive_file_id');
    const driveEnabled = localStorage.getItem('google_drive_enabled');
    
    if (accessToken && userEmail) {
        isGoogleSignedIn = true;
        updateGoogleSignInUI(true, userEmail);
        
        if (driveEnabled === 'true') {
            isGoogleDriveConnected = true;
            updateDriveConnectionUI(true);
            loadFromGoogleDrive();
        }
    }
}

function handleGoogleSignIn() {
    if (isGoogleSignedIn) {
        signOutGoogle();
    } else {
        signInGoogle();
    }
}

function signInGoogle() {
    // Load Google API client
    gapi.load('client:auth2', () => {
        gapi.client.init({
            apiKey: API_KEY,
            clientId: CLIENT_ID,
            discoveryDocs: DISCOVERY_DOCS,
            scope: SCOPES
        }).then(() => {
            console.log('Google API initialized');
            // Sign in
            return gapi.auth2.getAuthInstance().signIn();
        }).then(() => {
            // Get access token and user info
            const authInstance = gapi.auth2.getAuthInstance();
            const user = authInstance.currentUser.get();
            const authResponse = user.getAuthResponse(true);
            const profile = user.getBasicProfile();
            
            accessToken = authResponse.access_token;
            const userEmail = profile.getEmail();
            
            localStorage.setItem('google_access_token', accessToken);
            localStorage.setItem('google_user_email', userEmail);
            
            isGoogleSignedIn = true;
            updateGoogleSignInUI(true, userEmail);
            
            console.log('Signed in to Google successfully');
        }).catch((error) => {
            console.error('Google sign-in error:', error);
            console.error('Error details:', JSON.stringify(error, null, 2));
            
            if (error.error === 'popup_closed_by_user') {
                // User cancelled, do nothing
                return;
            } else if (error.error === 'idpiframe_initialization_failed' || 
                       (error.details && error.details.includes('Cookies'))) {
                // Cookie issue - show modal
                showCookieModal();
            } else if (error.error === 'access_denied') {
                // User denied access
                alert('Sign-in was cancelled or denied.');
            } else if (error.error === 'popup_blocked_by_browser') {
                alert('Pop-up was blocked. Please allow pop-ups for this site and try again.');
            } else {
                // Generic error - might be OAuth configuration issue
                let errorMsg = 'Failed to sign in with Google.\n\n';
                if (error.details) {
                    errorMsg += `Details: ${error.details}\n\n`;
                }
                errorMsg += 'Please ensure:\n';
                errorMsg += '1. Cookies are enabled\n';
                errorMsg += '2. Pop-ups are allowed\n';
                errorMsg += '3. You\'re accessing via http://127.0.0.1:3000\n';
                errorMsg += '4. The OAuth client is configured correctly in Google Cloud Console';
                alert(errorMsg);
            }
        });
    });
}

function signOutGoogle() {
    // Sign out and clear all data
    if (typeof gapi !== 'undefined' && gapi.auth2) {
        gapi.auth2.getAuthInstance().signOut();
    }
    
    localStorage.removeItem('google_access_token');
    localStorage.removeItem('google_user_email');
    localStorage.removeItem('google_drive_file_id');
    localStorage.removeItem('google_drive_enabled');
    
    accessToken = null;
    driveFileId = null;
    isGoogleSignedIn = false;
    isGoogleDriveConnected = false;
    
    updateGoogleSignInUI(false);
    updateDriveConnectionUI(false);
}

function updateGoogleSignInUI(signedIn, email = '') {
    if (signedIn) {
        googleSignInBtn.classList.add('signed-in');
        const displayEmail = email.length > 25 ? email.substring(0, 22) + '...' : email;
        signInStatus.innerHTML = `<i data-lucide="check-circle"></i> ${displayEmail}`;
        driveConnectBtn.style.display = 'flex';
    } else {
        googleSignInBtn.classList.remove('signed-in');
        signInStatus.textContent = 'Sign in with Google';
        driveConnectBtn.style.display = 'none';
    }
    lucide.createIcons();
}

function handleGoogleDriveConnect() {
    if (!isGoogleSignedIn) {
        alert('Please sign in with Google first');
        return;
    }
    
    if (isGoogleDriveConnected) {
        disconnectGoogleDrive();
    } else {
        connectGoogleDrive();
    }
}

function connectGoogleDrive() {
    isGoogleDriveConnected = true;
    localStorage.setItem('google_drive_enabled', 'true');
    updateDriveConnectionUI(true);
    
    // Sync current tasks
    syncToGoogleDrive(tasks);
    console.log('Google Drive sync enabled');
}

function disconnectGoogleDrive() {
    localStorage.removeItem('google_drive_file_id');
    localStorage.setItem('google_drive_enabled', 'false');
    driveFileId = null;
    isGoogleDriveConnected = false;
    updateDriveConnectionUI(false);
    console.log('Google Drive sync disabled');
}

function updateDriveConnectionUI(connected) {
    if (connected) {
        driveConnectBtn.classList.add('connected');
        driveStatus.innerHTML = 'Drive Sync Enabled ✓';
    } else {
        driveConnectBtn.classList.remove('connected');
        driveStatus.innerHTML = '<i data-lucide="cloud"></i> Enable Drive Sync';
    }
    lucide.createIcons();
}

// Sync to Google Drive
async function syncToGoogleDrive(tasksData) {
    if (!isGoogleDriveConnected || !accessToken) {
        console.log('Google Drive not connected, skipping sync');
        return;
    }

    try {
        const fileContent = JSON.stringify(tasksData, null, 2);
        const blob = new Blob([fileContent], { type: 'application/json' });
        
        if (driveFileId) {
            // Update existing file
            await updateDriveFile(driveFileId, blob);
            console.log('Tasks synced to Google Drive (updated)');
        } else {
            // Create new file
            const newFileId = await createDriveFile(blob);
            driveFileId = newFileId;
            localStorage.setItem('google_drive_file_id', newFileId);
            console.log('Tasks synced to Google Drive (created)');
        }
    } catch (error) {
        console.error('Error syncing to Google Drive:', error);
        // If token expired, sign out
        if (error.status === 401) {
            signOutGoogle();
        }
    }
}

async function createDriveFile(blob) {
    const metadata = {
        name: 'ido_tasks.json',
        mimeType: 'application/json'
    };

    const form = new FormData();
    form.append('metadata', new Blob([JSON.stringify(metadata)], { type: 'application/json' }));
    form.append('file', blob);

    const response = await fetch('https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart', {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${accessToken}`
        },
        body: form
    });

    if (!response.ok) {
        throw await response.json();
    }

    const result = await response.json();
    return result.id;
}

async function updateDriveFile(fileId, blob) {
    const response = await fetch(`https://www.googleapis.com/upload/drive/v3/files/${fileId}?uploadType=media`, {
        method: 'PATCH',
        headers: {
            'Authorization': `Bearer ${accessToken}`,
            'Content-Type': 'application/json'
        },
        body: blob
    });

    if (!response.ok) {
        throw await response.json();
    }

    return await response.json();
}

// Load from Google Drive
async function loadFromGoogleDrive() {
    if (!isGoogleDriveConnected || !accessToken) {
        console.log('Google Drive not connected, skipping load');
        return;
    }

    try {
        // First, find the file
        if (!driveFileId) {
            driveFileId = await findDriveFile();
            if (driveFileId) {
                localStorage.setItem('google_drive_file_id', driveFileId);
            } else {
                console.log('No tasks file found in Google Drive');
                return;
            }
        }

        // Download the file
        const response = await fetch(`https://www.googleapis.com/drive/v3/files/${driveFileId}?alt=media`, {
            headers: {
                'Authorization': `Bearer ${accessToken}`
            }
        });

        if (!response.ok) {
            throw await response.json();
        }

        const tasksData = await response.json();
        tasks = tasksData;
        localStorage.setItem('ido_tasks', JSON.stringify(tasks));
        renderTasks();
        console.log('Tasks loaded from Google Drive');
    } catch (error) {
        console.error('Error loading from Google Drive:', error);
        // If token expired, sign out
        if (error.status === 401) {
            signOutGoogle();
        }
    }
}

async function findDriveFile() {
    const response = await fetch(
        `https://www.googleapis.com/drive/v3/files?q=name='ido_tasks.json'&spaces=drive`,
        {
            headers: {
                'Authorization': `Bearer ${accessToken}`
            }
        }
    );

    if (!response.ok) {
        throw await response.json();
    }

    const result = await response.json();
    return result.files && result.files.length > 0 ? result.files[0].id : null;
}
