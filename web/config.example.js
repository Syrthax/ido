// OAuth Configuration Template
// Copy this file to config.js and add your actual credentials
// Note: With GitHub Actions, secrets are automatically injected during deployment
const CONFIG = {
    clientId: 'YOUR_CLIENT_ID_HERE',
    clientSecret: 'YOUR_CLIENT_SECRET_HERE',
    redirectUri: 'https://syrthax.github.io/ido/web/index.html',
    scope: 'https://www.googleapis.com/auth/drive.file https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/userinfo.profile',
    fileName: 'ido-data.json'
};
