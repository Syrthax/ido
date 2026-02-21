fn main() {
    // Load .env file for local development.
    // In GitHub Actions, secrets are already set as real environment variables.
    let env_path = std::path::Path::new(env!("CARGO_MANIFEST_DIR")).join("../.env");
    if let Ok(content) = std::fs::read_to_string(&env_path) {
        for line in content.lines() {
            let line = line.trim();
            if line.is_empty() || line.starts_with('#') {
                continue;
            }
            if let Some((key, value)) = line.split_once('=') {
                let key = key.trim();
                let value = value.trim().trim_matches('"').trim_matches('\'');
                // Only inject if not already set in the environment
                if std::env::var(key).is_err() {
                    println!("cargo:rustc-env={}={}", key, value);
                }
            }
        }
    }
    tauri_build::build()
}
