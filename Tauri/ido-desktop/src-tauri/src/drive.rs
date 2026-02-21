
use reqwest::Client;
use serde::Deserialize;

const DRIVE_UPLOAD_URL: &str = "https://www.googleapis.com/upload/drive/v3/files";
const DRIVE_FILES_URL: &str = "https://www.googleapis.com/drive/v3/files";
const IDO_FILE_NAME: &str = "ido-data.json";

#[derive(Debug, Deserialize)]
#[allow(dead_code)]
struct DriveFile {
    id: String,
    name: String,
}

#[derive(Debug, Deserialize)]
struct DriveFileList {
    files: Vec<DriveFile>,
}

macro_rules! dbg_log {
    ($($arg:tt)*) => {
        println!("[IDO DEBUG] {}", format!($($arg)*));
    };
}

macro_rules! err_log {
    ($($arg:tt)*) => {
        eprintln!("[IDO DEBUG] FAIL — {}", format!($($arg)*));
    };
}

/// Find ido-data.json in the user's Drive (drive.file scope — app-owned files only)
async fn find_ido_file(client: &Client, access_token: &str) -> Result<Option<String>, String> {
    let query = format!("name = '{}' and trashed = false", IDO_FILE_NAME);
    dbg_log!("Drive file search: query = {}", query);

    let resp = client
        .get(DRIVE_FILES_URL)
        .bearer_auth(access_token)
        .query(&[
            ("q", query.as_str()),
            ("fields", "files(id,name)"),
            ("spaces", "drive"),
        ])
        .send()
        .await
        .map_err(|e| format!("Drive list request failed: {}", e))?;

    let status = resp.status();
    dbg_log!("Drive file search: response status = {}", status);

    if !status.is_success() {
        let body = resp.text().await.unwrap_or_default();
        err_log!("Drive list error {}: {}", status, body);
        return Err(format!("Drive list error {}: {}", status, body));
    }

    let list: DriveFileList = resp
        .json()
        .await
        .map_err(|e| format!("Drive list parse error: {}", e))?;

    dbg_log!(
        "Drive file search: {} files returned",
        list.files.len()
    );
    for f in &list.files {
        dbg_log!("  - file: id={} name={}", f.id, f.name);
    }

    let found = list.files.into_iter().next().map(|f| f.id);
    dbg_log!(
        "ido-data.json found: {}",
        if found.is_some() { "YES" } else { "NO" }
    );
    Ok(found)
}

#[tauri::command]
pub async fn drive_download_data(access_token: String) -> Result<String, String> {
    dbg_log!("drive_download_data called");
    dbg_log!("Access token acquired: YES");
    dbg_log!("Access token length: {}", access_token.len());

    let client = Client::new();

    let file_id = match find_ido_file(&client, &access_token).await? {
        Some(id) => {
            dbg_log!("File ID used for download: {}", id);
            id
        }
        None => {
            dbg_log!("No ido-data.json on Drive — returning empty dataset");
            return Ok(serde_json::json!({
                "version": 1,
                "tasks": [],
                "categories": ["Finance","Product","Client","Personal","Work"],
                "updatedAt": chrono::Utc::now().to_rfc3339()
            })
            .to_string());
        }
    };

    dbg_log!("Drive download: sending GET request for file {}", file_id);
    let resp = client
        .get(format!("{}/{}", DRIVE_FILES_URL, file_id))
        .bearer_auth(&access_token)
        .query(&[("alt", "media")])
        .send()
        .await
        .map_err(|e| format!("Drive download request failed: {}", e))?;

    let status = resp.status();
    dbg_log!("Drive download: response status = {}", status);

    if !status.is_success() {
        let body = resp.text().await.unwrap_or_default();
        err_log!("Drive download error {}: {}", status, body);
        return Err(format!("Drive download error {}: {}", status, body));
    }

    let text = resp
        .text()
        .await
        .map_err(|e| format!("Drive read body error: {}", e))?;

    dbg_log!("File download: SUCCESS — content length: {} bytes", text.len());
    dbg_log!("File content (first 500 chars): {}", &text[..text.len().min(500)]);
    println!("[IDO DEBUG] Remote JSON content:\n{}", text);

    Ok(text)
}

#[tauri::command]
pub async fn drive_upload_data(access_token: String, data: String) -> Result<(), String> {
    dbg_log!("drive_upload_data called — payload length: {} bytes", data.len());
    let client = Client::new();
    let existing_id = find_ido_file(&client, &access_token).await?;

    if let Some(file_id) = existing_id {
        dbg_log!("Updating existing drive file: {}", file_id);
        let resp = client
            .patch(format!("{}/{}", DRIVE_UPLOAD_URL, file_id))
            .bearer_auth(&access_token)
            .query(&[("uploadType", "media")])
            .header("Content-Type", "application/json")
            .body(data)
            .send()
            .await
            .map_err(|e| format!("Drive update request failed: {}", e))?;

        let status = resp.status();
        dbg_log!("Drive update: response status = {}", status);
        if !status.is_success() {
            let body = resp.text().await.unwrap_or_default();
            err_log!("Drive update error {}: {}", status, body);
            return Err(format!("Drive update error {}: {}", status, body));
        }
        dbg_log!("Drive update: SUCCESS");
    } else {
        dbg_log!("Creating new ido-data.json on Drive");
        let boundary = "IDoBoundary1234567890";
        let metadata = serde_json::json!({ "name": IDO_FILE_NAME });
        let body = format!(
            "--{b}\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n{meta}\r\n--{b}\r\nContent-Type: application/json\r\n\r\n{data}\r\n--{b}--\r\n",
            b = boundary,
            meta = metadata,
            data = data
        );

        let resp = client
            .post(DRIVE_UPLOAD_URL)
            .bearer_auth(&access_token)
            .query(&[("uploadType", "multipart")])
            .header(
                "Content-Type",
                format!("multipart/related; boundary={}", boundary),
            )
            .body(body)
            .send()
            .await
            .map_err(|e| format!("Drive create request failed: {}", e))?;

        let status = resp.status();
        dbg_log!("Drive create: response status = {}", status);
        if !status.is_success() {
            let body = resp.text().await.unwrap_or_default();
            err_log!("Drive create error {}: {}", status, body);
            return Err(format!("Drive create error {}: {}", status, body));
        }
        dbg_log!("Drive create: SUCCESS");
    }

    println!("[IDO DEBUG] Drive sync ready");
    Ok(())
}
