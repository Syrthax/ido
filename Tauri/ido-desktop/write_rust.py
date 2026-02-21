#!/usr/bin/env python3
"""Write all Rust source files for iDo Tauri backend."""
import os

base = "/Users/sarthakghosh/projects/ido/Tauri/ido-desktop/src-tauri/src"
os.makedirs(base, exist_ok=True)

# ---- lib.rs ----
lib_rs = r"""
use tauri::Manager;

mod oauth;
mod drive;
mod storage;
mod notifications;

pub use oauth::*;
pub use drive::*;
pub use storage::*;
pub use notifications::*;

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_opener::init())
        .plugin(tauri_plugin_notification::init())
        .plugin(tauri_plugin_store::Builder::new().build())
        .plugin(tauri_plugin_shell::init())
        .invoke_handler(tauri::generate_handler![
            start_oauth_flow,
            clear_auth_tokens,
            drive_download_data,
            drive_upload_data,
            send_test_notification,
            schedule_task_notification,
        ])
        .setup(|app| {
            #[cfg(debug_assertions)]
            {
                let window = app.get_webview_window("main").unwrap();
                window.open_devtools();
            }
            Ok(())
        })
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
"""

with open(f"{base}/lib.rs", "w") as f:
    f.write(lib_rs)
print("Written lib.rs")

# ---- oauth.rs ----
oauth_rs = r"""
use std::collections::HashMap;
use std::io::{Read, Write};
use std::net::TcpListener;
use std::sync::{Arc, Mutex};
use std::time::Duration;
use serde::{Deserialize, Serialize};
use reqwest::Client;
use base64::{engine::general_purpose::URL_SAFE_NO_PAD, Engine};
use sha2::{Digest, Sha256};
use rand::Rng;
use url::Url;
use tauri_plugin_shell::ShellExt;

const GOOGLE_AUTH_URL: &str = "https://accounts.google.com/o/oauth2/v2/auth";
const GOOGLE_TOKEN_URL: &str = "https://oauth2.googleapis.com/token";
const GOOGLE_USERINFO_URL: &str = "https://www.googleapis.com/oauth2/v3/userinfo";
const GOOGLE_SCOPE: &str = "https://www.googleapis.com/auth/drive.appdata openid email profile";
const REDIRECT_PORT: u16 = 47832;

// IMPORTANT: Replace these with your actual Google OAuth credentials
// from https://console.cloud.google.com/
const CLIENT_ID: &str = "YOUR_GOOGLE_CLIENT_ID.apps.googleusercontent.com";
const CLIENT_SECRET: &str = "YOUR_GOOGLE_CLIENT_SECRET";

#[derive(Debug, Serialize, Deserialize)]
pub struct OAuthResult {
    pub access_token: String,
    pub refresh_token: String,
    pub expires_in: u64,
    pub user_id: String,
    pub user_name: String,
    pub user_email: String,
    pub avatar_url: String,
}

#[derive(Debug, Deserialize)]
struct TokenResponse {
    access_token: String,
    refresh_token: Option<String>,
    expires_in: u64,
}

#[derive(Debug, Deserialize)]
struct UserInfo {
    sub: String,
    name: Option<String>,
    email: Option<String>,
    picture: Option<String>,
}

fn generate_pkce() -> (String, String) {
    let verifier: String = rand::thread_rng()
        .sample_iter(&rand::distributions::Alphanumeric)
        .take(64)
        .map(char::from)
        .collect();
    let mut hasher = Sha256::new();
    hasher.update(verifier.as_bytes());
    let hash = hasher.finalize();
    let challenge = URL_SAFE_NO_PAD.encode(hash);
    (verifier, challenge)
}

fn parse_query_params(query: &str) -> HashMap<String, String> {
    query
        .split('&')
        .filter_map(|pair| {
            let mut parts = pair.splitn(2, '=');
            let key = parts.next()?.to_string();
            let val = parts.next().unwrap_or("").to_string();
            Some((key, val))
        })
        .collect()
}

fn wait_for_callback() -> Result<String, String> {
    let listener = TcpListener::bind(format!("127.0.0.1:{}", REDIRECT_PORT))
        .map_err(|e| format!("Failed to bind port {}: {}", REDIRECT_PORT, e))?;
    listener
        .set_nonblocking(false)
        .map_err(|e| e.to_string())?;

    let (mut stream, _) = listener.accept().map_err(|e| e.to_string())?;
    let mut request = String::new();
    let mut buf = [0u8; 4096];
    let n = stream.read(&mut buf).map_err(|e| e.to_string())?;
    request.push_str(&String::from_utf8_lossy(&buf[..n]));

    // Send HTML response
    let html = r#"<!DOCTYPE html>
<html>
<head><title>iDo – Login successful</title>
<style>body{font-family:-apple-system,sans-serif;display:flex;align-items:center;justify-content:center;height:100vh;margin:0;background:#0d1117;color:#e6edf3;}
.card{text-align:center;padding:40px;border-radius:16px;background:#161b22;border:1px solid #30363d;}
h1{color:#2563eb;}p{color:#8b949e;margin-top:8px;}</style>
</head>
<body><div class="card">
<h1>Login Successful!</h1>
<p>You can close this window and return to iDo.</p>
</div></body>
</html>"#;
    let response = format!(
        "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\nContent-Length: {}\r\nConnection: close\r\n\r\n{}",
        html.len(),
        html
    );
    let _ = stream.write_all(response.as_bytes());

    // Extract the path/query from the request line
    let first_line = request.lines().next().unwrap_or("");
    let path = first_line.split_whitespace().nth(1).unwrap_or("");
    if let Some(query_start) = path.find('?') {
        Ok(path[query_start + 1..].to_string())
    } else {
        Err("No query params in callback".to_string())
    }
}

#[tauri::command]
pub async fn start_oauth_flow(app: tauri::AppHandle) -> Result<OAuthResult, String> {
    let (verifier, challenge) = generate_pkce();
    let state: String = rand::thread_rng()
        .sample_iter(&rand::distributions::Alphanumeric)
        .take(16)
        .map(char::from)
        .collect();

    let redirect_uri = format!("http://127.0.0.1:{}/callback", REDIRECT_PORT);

    let auth_url = format!(
        "{}?client_id={}&redirect_uri={}&response_type=code&scope={}&state={}&code_challenge={}&code_challenge_method=S256&access_type=offline&prompt=consent",
        GOOGLE_AUTH_URL,
        urlencoding::encode(CLIENT_ID),
        urlencoding::encode(&redirect_uri),
        urlencoding::encode(GOOGLE_SCOPE),
        urlencoding::encode(&state),
        urlencoding::encode(&challenge),
    );

    // Open browser
    app.shell().open(&auth_url, None)
        .map_err(|e| format!("Failed to open browser: {}", e))?;

    // Wait for callback in blocking task
    let verifier_clone = verifier.clone();
    let redirect_uri_clone = redirect_uri.clone();

    let query_string = tokio::task::spawn_blocking(wait_for_callback)
        .await
        .map_err(|e| format!("Task error: {}", e))?
        .map_err(|e| format!("Callback error: {}", e))?;

    let params = parse_query_params(&query_string);
    let code = params.get("code").ok_or("No auth code in callback")?.clone();
    let returned_state = params.get("state").cloned().unwrap_or_default();
    if returned_state != state {
        return Err("State mismatch - possible CSRF attack".to_string());
    }

    // Exchange code for tokens
    let client = Client::new();
    let token_params = [
        ("code", code.as_str()),
        ("client_id", CLIENT_ID),
        ("client_secret", CLIENT_SECRET),
        ("redirect_uri", &redirect_uri_clone),
        ("grant_type", "authorization_code"),
        ("code_verifier", &verifier_clone),
    ];

    let token_resp = client
        .post(GOOGLE_TOKEN_URL)
        .form(&token_params)
        .send()
        .await
        .map_err(|e| format!("Token request failed: {}", e))?;

    if !token_resp.status().is_success() {
        let err_text = token_resp.text().await.unwrap_or_default();
        return Err(format!("Token exchange failed: {}", err_text));
    }

    let tokens: TokenResponse = token_resp
        .json()
        .await
        .map_err(|e| format!("Failed to parse token response: {}", e))?;

    // Get user info
    let userinfo_resp = client
        .get(GOOGLE_USERINFO_URL)
        .bearer_auth(&tokens.access_token)
        .send()
        .await
        .map_err(|e| format!("Userinfo request failed: {}", e))?;

    let userinfo: UserInfo = userinfo_resp
        .json()
        .await
        .map_err(|e| format!("Failed to parse userinfo: {}", e))?;

    // Store tokens securely
    let entry = keyring::Entry::new("app.ido.desktop", "oauth_tokens")
        .map_err(|e| e.to_string())?;
    let token_json = serde_json::json!({
        "access_token": tokens.access_token,
        "refresh_token": tokens.refresh_token,
        "expires_in": tokens.expires_in,
    });
    let _ = entry.set_password(&token_json.to_string());

    Ok(OAuthResult {
        access_token: tokens.access_token,
        refresh_token: tokens.refresh_token.unwrap_or_default(),
        expires_in: tokens.expires_in,
        user_id: userinfo.sub,
        user_name: userinfo.name.unwrap_or_else(|| "User".to_string()),
        user_email: userinfo.email.unwrap_or_default(),
        avatar_url: userinfo.picture.unwrap_or_default(),
    })
}

#[tauri::command]
pub async fn clear_auth_tokens() -> Result<(), String> {
    if let Ok(entry) = keyring::Entry::new("app.ido.desktop", "oauth_tokens") {
        let _ = entry.delete_credential();
    }
    Ok(())
}
"""

with open(f"{base}/oauth.rs", "w") as f:
    f.write(oauth_rs)
print("Written oauth.rs")

# ---- drive.rs ----
drive_rs = r"""
use reqwest::Client;
use serde::{Deserialize, Serialize};

const DRIVE_UPLOAD_URL: &str = "https://www.googleapis.com/upload/drive/v3/files";
const DRIVE_FILES_URL: &str = "https://www.googleapis.com/drive/v3/files";
const IDO_FILE_NAME: &str = "ido-data.json";
const APP_DATA_SPACE: &str = "appDataFolder";

#[derive(Debug, Deserialize)]
struct DriveFile {
    id: String,
    name: String,
}

#[derive(Debug, Deserialize)]
struct DriveFileList {
    files: Vec<DriveFile>,
}

async fn find_ido_file(client: &Client, access_token: &str) -> Result<Option<String>, String> {
    let resp = client
        .get(DRIVE_FILES_URL)
        .bearer_auth(access_token)
        .query(&[
            ("spaces", APP_DATA_SPACE),
            ("fields", "files(id,name)"),
            ("q", &format!("name = '{}'", IDO_FILE_NAME)),
        ])
        .send()
        .await
        .map_err(|e| e.to_string())?;

    if !resp.status().is_success() {
        return Err(format!("Drive list error: {}", resp.status()));
    }

    let list: DriveFileList = resp.json().await.map_err(|e| e.to_string())?;
    Ok(list.files.into_iter().next().map(|f| f.id))
}

#[tauri::command]
pub async fn drive_download_data(access_token: String) -> Result<String, String> {
    let client = Client::new();
    let file_id = match find_ido_file(&client, &access_token).await? {
        Some(id) => id,
        None => {
            // Return empty ido data structure
            return Ok(serde_json::json!({
                "version": 1,
                "tasks": [],
                "categories": ["Finance","Product","Client","Personal","Work"],
                "updatedAt": chrono::Utc::now().to_rfc3339()
            }).to_string());
        }
    };

    let resp = client
        .get(format!("{}/{}", DRIVE_FILES_URL, file_id))
        .bearer_auth(&access_token)
        .query(&[("alt", "media")])
        .send()
        .await
        .map_err(|e| e.to_string())?;

    if !resp.status().is_success() {
        return Err(format!("Drive download error: {}", resp.status()));
    }

    resp.text().await.map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn drive_upload_data(access_token: String, data: String) -> Result<(), String> {
    let client = Client::new();
    let existing_id = find_ido_file(&client, &access_token).await?;

    if let Some(file_id) = existing_id {
        // Update existing file
        let resp = client
            .patch(format!("{}/{}", DRIVE_UPLOAD_URL, file_id))
            .bearer_auth(&access_token)
            .query(&[("uploadType", "media")])
            .header("Content-Type", "application/json")
            .body(data)
            .send()
            .await
            .map_err(|e| e.to_string())?;

        if !resp.status().is_success() {
            return Err(format!("Drive update error: {}", resp.status()));
        }
    } else {
        // Create new file with metadata
        let metadata = serde_json::json!({
            "name": IDO_FILE_NAME,
            "parents": [APP_DATA_SPACE]
        });

        let boundary = "===============boundary===============";
        let body = format!(
            "--{boundary}\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n{metadata}\r\n--{boundary}\r\nContent-Type: application/json\r\n\r\n{data}\r\n--{boundary}--\r\n",
            boundary = boundary,
            metadata = metadata,
            data = data
        );

        let resp = client
            .post(DRIVE_UPLOAD_URL)
            .bearer_auth(&access_token)
            .query(&[("uploadType", "multipart")])
            .header("Content-Type", format!("multipart/related; boundary={}", boundary))
            .body(body)
            .send()
            .await
            .map_err(|e| e.to_string())?;

        if !resp.status().is_success() {
            let txt = resp.text().await.unwrap_or_default();
            return Err(format!("Drive create error: {}", txt));
        }
    }

    Ok(())
}
"""

with open(f"{base}/drive.rs", "w") as f:
    f.write(drive_rs)
print("Written drive.rs")

# ---- storage.rs ----
storage_rs = r"""
// Secure token storage helpers (supplemental to keyring in oauth.rs)

#[tauri::command]
pub async fn get_stored_tokens() -> Result<Option<serde_json::Value>, String> {
    match keyring::Entry::new("app.ido.desktop", "oauth_tokens") {
        Ok(entry) => match entry.get_password() {
            Ok(json) => {
                let v: serde_json::Value = serde_json::from_str(&json).map_err(|e| e.to_string())?;
                Ok(Some(v))
            }
            Err(_) => Ok(None),
        },
        Err(e) => Err(e.to_string()),
    }
}
"""

with open(f"{base}/storage.rs", "w") as f:
    f.write(storage_rs)
print("Written storage.rs")

# ---- notifications.rs ----
notifications_rs = r"""
use tauri_plugin_notification::NotificationExt;

#[tauri::command]
pub async fn send_test_notification(app: tauri::AppHandle) -> Result<(), String> {
    app.notification()
        .builder()
        .title("iDo")
        .body("Test notification from iDo! Notifications are working.")
        .show()
        .map_err(|e| e.to_string())?;
    Ok(())
}

#[tauri::command]
pub async fn schedule_task_notification(
    app: tauri::AppHandle,
    task_title: String,
    task_time: String,
) -> Result<(), String> {
    app.notification()
        .builder()
        .title("Task Reminder")
        .body(format!("{} is scheduled for {}", task_title, task_time))
        .show()
        .map_err(|e| e.to_string())?;
    Ok(())
}
"""

with open(f"{base}/notifications.rs", "w") as f:
    f.write(notifications_rs)
print("Written notifications.rs")

print("ALL RUST FILES WRITTEN")
