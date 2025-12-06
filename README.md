

# 🚀 iDo — A Minimal, Privacy-First To-Do App (Synced with Google Drive)

iDo is a lightweight task manager that keeps your data fully in your control.
No servers, no databases, no tracking — all your tasks are stored securely in your own Google Drive using OAuth 2.0.

✨ Perfect for students, builders, and anyone who wants a frictionless, cross-device to-do list.



# 🌟 Features

🔐 Login with Google (OAuth 2.0)
	•	Secure authentication flow
	•	Uses Google Drive API to store your tasks
	•	No backend server needed — everything happens on your device

☁️ Real-Time Sync via Google Drive
	•	Tasks are saved inside a dedicated folder in your Drive
	•	Automatically synced across browser sessions

🧹 Minimal, Distraction-Free UI
	•	Clean layout focused on productivity
	•	Add, delete, and toggle tasks instantly
	•	Mobile-responsive design

🔒 Your Data. Your Drive.
	•	iDo stores zero data outside Google Drive
	•	No analytics, no cookies, no logging
	•	Perfect for privacy-conscious users



# 🖼️ Live Demo

👉 Use the web app here:
https://syrthax.github.io/ido/web/



# 🧰 Tech Stack

Component	Technology
Frontend	HTML, CSS, JavaScript
Auth	Google OAuth 2.0
Storage	Google Drive API
Hosting	GitHub Pages




# 🛠️ How It Works (Simple Overview)
	1.	User signs in with Google.
	2.	OAuth returns a token authorized for Drive access.
	3.	iDo checks for a folder named "iDo" in Google Drive.
	•    If not found, it creates one.
	4.	Tasks are stored in a JSON file: iDo/tasks.json, this is how the json file looks
	5.	Adding/deleting tasks immediately updates the Drive file.

This architecture means iDo requires no backend server, making it extremely fast, safe, and free to operate.
<img width="1710" height="982" alt="image" src="https://github.com/user-attachments/assets/5c21034e-16aa-4564-a948-4744eaa59503" />

# 🧑‍💻 Run Locally (Development Setup)
	1.	Clone the repo:

git clone https://github.com/Syrthax/ido
cd ido

	2.	Create your own Google OAuth credentials and configure:

web/config.js

	3.	Use Live Server or open /web/index.html directly.


# 📁 Project Structure

/ (Landing page)
├── index.html
├── styles.css
├── script.js

/web (Actual app)
├── index.html
├── drive.js
├── config.example.js
├── config.js (local only, contains secrets)



# 📜 License

This project is open-source under the MIT License.
You are free to modify, distribute, and use it in your own projects.


# 🤝 Contributing

Pull requests are welcome!
If you have ideas for improvements—like reminders, labels, widgets—drop an issue or submit PRs.


# ✨ Author

Sarthak
Portfolio: [Portfolio](https://sarthakg.tech/)
GitHub: https://github.com/Syrthax
