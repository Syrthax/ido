
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
