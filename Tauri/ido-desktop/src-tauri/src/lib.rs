
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
