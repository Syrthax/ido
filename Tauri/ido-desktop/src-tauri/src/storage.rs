
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
