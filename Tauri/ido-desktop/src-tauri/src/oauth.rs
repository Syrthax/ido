
use std::io::{Read, Write};
use std::net::TcpListener;
use std::collections::HashMap;
use serde::{Deserialize, Serialize};
use reqwest::Client;
use base64::{engine::general_purpose::URL_SAFE_NO_PAD, Engine};
use sha2::{Digest, Sha256};
use rand::Rng;
use tauri_plugin_opener::OpenerExt;

const GOOGLE_AUTH_URL: &str = "https://accounts.google.com/o/oauth2/v2/auth";
const GOOGLE_TOKEN_URL: &str = "https://oauth2.googleapis.com/token";
const GOOGLE_USERINFO_URL: &str = "https://www.googleapis.com/oauth2/v3/userinfo";

// Credentials injected at compile time from environment variables.
// Set GOOGLE_DESKTOP_CLIENT_ID and GOOGLE_DESKTOP_CLIENT_SECRET in your environment or .env file.
// In GitHub Actions, add them as repository secrets.
const CLIENT_ID: &str = match option_env!("GOOGLE_DESKTOP_CLIENT_ID") {
    Some(v) => v,
    None => "",
};
const CLIENT_SECRET: &str = match option_env!("GOOGLE_DESKTOP_CLIENT_SECRET") {
    Some(v) => v,
    None => "",
};

// Required scopes
const GOOGLE_SCOPE: &str = "openid email profile https://www.googleapis.com/auth/drive.file";

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
    expires_in: Option<u64>,
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
    let challenge = URL_SAFE_NO_PAD.encode(hasher.finalize());
    (verifier, challenge)
}

fn parse_query(query: &str) -> HashMap<String, String> {
    query
        .split('&')
        .filter_map(|pair| {
            let mut parts = pair.splitn(2, '=');
            let k = parts.next()?.to_string();
            let v = urlencoding::decode(parts.next().unwrap_or(""))
                .unwrap_or_default()
                .to_string();
            Some((k, v))
        })
        .collect()
}

fn bind_random_port() -> Result<(TcpListener, u16), String> {
    let listener = TcpListener::bind("127.0.0.1:0")
        .map_err(|e| format!("Failed to bind OAuth listener: {}", e))?;
    let port = listener
        .local_addr()
        .map_err(|e| format!("Failed to read local addr: {}", e))?
        .port();
    Ok((listener, port))
}

fn wait_for_callback(listener: TcpListener) -> Result<String, String> {
    let (mut stream, _) = listener.accept().map_err(|e| e.to_string())?;
    let mut buf = [0u8; 8192];
    let n = stream.read(&mut buf).map_err(|e| e.to_string())?;
    let request = String::from_utf8_lossy(&buf[..n]);

    let html = r#"<!DOCTYPE html>
<html><head><meta charset="utf-8"><title>iDo Login Successful</title>
<style>
*{box-sizing:border-box;margin:0;padding:0}
body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;
     display:flex;align-items:center;justify-content:center;
     min-height:100vh;background:#0d1117;color:#e6edf3}
.card{padding:48px 56px;border-radius:20px;background:#161b22;
      border:1px solid #30363d;text-align:center;max-width:420px}
h1{font-size:28px;color:#2563eb;margin-bottom:12px}
p{color:#8b949e;font-size:15px;line-height:1.5}
</style></head>
<body><div class="card">
<h1>&#10003; Login Successful</h1>
<p>You're signed in.<br>You can close this tab and return to iDo.</p>
</div></body></html>"#;

    let response = format!(
        "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: {}\r\nConnection: close\r\n\r\n{}",
        html.len(),
        html
    );
    let _ = stream.write_all(response.as_bytes());

    let first_line = request.lines().next().unwrap_or("");
    let path = first_line.split_whitespace().nth(1).unwrap_or("");
    if let Some(q) = path.find('?') {
        Ok(path[q + 1..].to_string())
    } else {
        Err("No query string in OAuth callback".to_string())
    }
}

#[tauri::command]
pub async fn start_oauth_flow(app: tauri::AppHandle) -> Result<OAuthResult, String> {
    // Step 1 — Start local HTTP server on a dynamic port
    let (listener, port) = bind_random_port()?;
    println!("OAuth server started on port {}", port);

    let (verifier, challenge) = generate_pkce();
    let state: String = rand::thread_rng()
        .sample_iter(&rand::distributions::Alphanumeric)
        .take(24)
        .map(char::from)
        .collect();

    let redirect_uri = format!("http://127.0.0.1:{}", port);

    let auth_url = format!(
        "{}?client_id={}&redirect_uri={}&response_type=code&scope={}&state={}&code_challenge={}&code_challenge_method=S256&access_type=offline&prompt=consent",
        GOOGLE_AUTH_URL,
        urlencoding::encode(CLIENT_ID),
        urlencoding::encode(&redirect_uri),
        urlencoding::encode(GOOGLE_SCOPE),
        urlencoding::encode(&state),
        urlencoding::encode(&challenge),
    );

    // Step 2 — Open system browser
    println!("Opening browser for login...");
    app.opener()
        .open_url(&auth_url, None::<String>)
        .map_err(|e| format!("Failed to open browser: {}", e))?;

    // Step 3 — Capture authorization code
    let query_string = tokio::task::spawn_blocking(move || wait_for_callback(listener))
        .await
        .map_err(|e| format!("Spawn error: {}", e))?
        .map_err(|e| format!("Callback error: {}", e))?;

    let params = parse_query(&query_string);

    if let Some(err) = params.get("error") {
        return Err(format!("Google OAuth error: {}", err));
    }

    let code = params.get("code").ok_or("No authorization code in callback")?.clone();
    let returned_state = params.get("state").cloned().unwrap_or_default();
    if returned_state != state {
        return Err("OAuth state mismatch — possible CSRF attack.".to_string());
    }
    println!("Authorization code received");

    // Step 4 — Exchange code for tokens
    let client = Client::new();
    let token_params = [
        ("code", code.as_str()),
        ("client_id", CLIENT_ID),
        ("client_secret", CLIENT_SECRET),
        ("redirect_uri", &redirect_uri),
        ("grant_type", "authorization_code"),
        ("code_verifier", &verifier),
    ];

    let token_resp = client
        .post(GOOGLE_TOKEN_URL)
        .form(&token_params)
        .send()
        .await
        .map_err(|e| format!("Token request failed: {}", e))?;

    if !token_resp.status().is_success() {
        let err_body = token_resp.text().await.unwrap_or_default();
        eprintln!("Token exchange failed: {}", err_body);
        return Err(format!("Token exchange failed: {}", err_body));
    }

    let tokens: TokenResponse = token_resp
        .json()
        .await
        .map_err(|e| format!("Failed to parse token response: {}", e))?;
    println!("Token exchange success");

    // Step 5 — Fetch user info
    let userinfo_resp = client
        .get(GOOGLE_USERINFO_URL)
        .bearer_auth(&tokens.access_token)
        .send()
        .await
        .map_err(|e| format!("Userinfo request failed: {}", e))?;

    if !userinfo_resp.status().is_success() {
        let err_body = userinfo_resp.text().await.unwrap_or_default();
        return Err(format!("Userinfo failed: {}", err_body));
    }

    let userinfo: UserInfo = userinfo_resp
        .json()
        .await
        .map_err(|e| format!("Failed to parse userinfo: {}", e))?;
    println!("User info fetched: {}", userinfo.email.as_deref().unwrap_or("?"));

    if let Ok(entry) = keyring::Entry::new("app.ido.desktop", "oauth_tokens") {
        let _ = entry.set_password(&serde_json::json!({
            "access_token": &tokens.access_token,
            "refresh_token": &tokens.refresh_token,
        }).to_string());
    }

    Ok(OAuthResult {
        access_token: tokens.access_token,
        refresh_token: tokens.refresh_token.unwrap_or_default(),
        expires_in: tokens.expires_in.unwrap_or(3600),
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
