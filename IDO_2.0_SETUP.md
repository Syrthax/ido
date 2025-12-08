# iDo 2.0 - Quick Setup Guide

## 🚀 Deployment Steps

### 1. Update Google Cloud Console

#### Add Calendar API Scope
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Select your iDo project
3. Navigate to **APIs & Services** > **OAuth consent screen**
4. Click **Edit App**
5. In the **Scopes** section, add:
   ```
   https://www.googleapis.com/auth/calendar.readonly
   ```
6. Save changes

#### Verify All Scopes
Make sure these scopes are enabled:
- ✅ `https://www.googleapis.com/auth/drive.file`
- ✅ `https://www.googleapis.com/auth/userinfo.email`
- ✅ `https://www.googleapis.com/auth/userinfo.profile`
- ✅ `https://www.googleapis.com/auth/calendar.readonly` (NEW)

### 2. Update config.js

If you're using the local config.js file (not GitHub Actions):

```javascript
const CONFIG = {
    clientId: 'YOUR_CLIENT_ID',
    clientSecret: 'YOUR_CLIENT_SECRET',
    redirectUri: 'https://yourdomain.com/ido/web/index.html',
    scope: 'https://www.googleapis.com/auth/drive.file https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/calendar.readonly',
    fileName: 'ido-data.json'
};
```

### 3. Deploy Files

Upload these files to your web server:

**New Files:**
- ✅ `web/calendar.js`
- ✅ `IDO_2.0_GUIDE.md` (documentation)
- ✅ `IDO_2.0_IMPLEMENTATION_SUMMARY.md` (documentation)

**Modified Files:**
- ✅ `web/index.html`
- ✅ `web/style.css`
- ✅ `web/app.js`
- ✅ `web/config.example.js`

**Unchanged Files (but needed):**
- ✅ `web/drive.js`
- ✅ `web/taskSchema.js`
- ✅ `web/config.js` (your credentials)
- ✅ `assets/logo.png`

### 4. Test the Deployment

1. Open `https://yourdomain.com/ido/web/index.html`
2. Click "Sign in with Google"
3. You should see a new permission request for Calendar access
4. Grant all permissions
5. Verify:
   - ✅ Three-pane layout appears
   - ✅ Calendar events show in center area
   - ✅ Tasks split into planned/unplanned
   - ✅ Drag-and-drop works
   - ✅ Today section shows events/tasks

---

## 🔧 GitHub Actions Deployment

If you're using GitHub Actions (like in the original iDo):

### Update Secrets

No new secrets needed! The Calendar scope is added to `config.example.js`, which gets the client ID/secret injected during build.

### Workflow File

Your existing workflow should work. Just make sure:
1. The new files are committed to the repo
2. The workflow copies all files including `calendar.js`
3. The config injection step includes the updated scope

### Verification

After pushing to GitHub:
1. Wait for Actions to complete
2. Visit your deployed URL
3. Test as described above

---

## 👥 User Migration

### Existing Users

Existing users will need to **re-authenticate** to grant Calendar permission:

1. They'll be automatically logged out (token scope mismatch)
2. On login, they'll see the new Calendar permission request
3. After granting, all their existing tasks will load
4. No data loss - everything migrates automatically

### Communication

Recommended message to users:

> 🎉 **iDo 2.0 is here!**
> 
> We've added calendar integration! You'll need to log in again to grant Calendar access. Your tasks are safe and will load automatically.
> 
> New features:
> - See Google Calendar events alongside tasks
> - Drag-and-drop task scheduling
> - Weekly calendar view
> - Today section for daily planning
> 
> Check out the full guide: [link to IDO_2.0_GUIDE.md]

---

## 🐛 Troubleshooting

### Calendar Events Not Showing

**Problem**: Users see tasks but no calendar events

**Solutions**:
1. Verify Calendar API is enabled in Google Cloud Console:
   - APIs & Services > Library
   - Search "Google Calendar API"
   - Click Enable if not already enabled
2. Check browser console for API errors
3. Verify scope in config.js includes `calendar.readonly`
4. Have user log out and log back in

### Permission Error

**Problem**: "Access not configured" or "Insufficient Permission" error

**Solutions**:
1. Confirm Calendar API is enabled
2. Check OAuth consent screen has Calendar scope
3. Verify scope string in config.js is correct
4. Clear browser cache and cookies
5. Try incognito/private browsing

### Layout Broken

**Problem**: Three-pane layout not displaying correctly

**Solutions**:
1. Hard refresh (Ctrl+Shift+R or Cmd+Shift+R)
2. Clear browser cache
3. Verify style.css uploaded correctly
4. Check browser console for CSS errors
5. Test in a different browser

### Drag-and-Drop Not Working

**Problem**: Can't drag tasks to calendar

**Solutions**:
1. Verify calendar.js loaded (check browser console)
2. Check if JavaScript errors present
3. Ensure tasks have the drag handle visible
4. Try dragging from the six-dot icon specifically
5. Refresh the page

---

## 📱 Mobile Considerations

### Current Mobile Support

The responsive design works but with limitations:
- Sidebars hidden on tablets
- Calendar shows 3 days on phones
- Drag-and-drop may be difficult on touchscreens

### Recommendations

For mobile users:
1. Use in landscape mode when possible
2. Pinch-to-zoom works
3. Touch-and-hold to drag (may require practice)
4. Consider using desktop version for best experience

---

## 🔒 Security Notes

### API Key Safety

**Important**: Never commit `config.js` to public repositories!

**Safe files to commit:**
- ✅ `config.example.js` (template only)
- ✅ All other files

**Never commit:**
- ❌ `config.js` (contains actual credentials)

### OAuth Security

- Using PKCE (Proof Key for Code Exchange) - ✅ Secure
- Redirect URI must match exactly in Google Console
- Scopes are clearly disclosed to users
- Tokens stored in localStorage (consider security implications)

---

## 📊 Monitoring

### What to Watch

After deployment, monitor:

1. **Error Rates**
   - Check browser console logs
   - Monitor API request failures
   - Track authentication errors

2. **API Usage**
   - Google Calendar API calls
   - Google Drive API calls
   - Stay within quota limits

3. **User Feedback**
   - New permission concerns
   - UI/UX issues
   - Feature requests

### Google Cloud Console Monitoring

1. Go to APIs & Services > Dashboard
2. Check Calendar API usage
3. Monitor quota consumption
4. Set up alerts if needed

---

## ✅ Launch Checklist

Before going live:

### Pre-Launch
- [ ] Calendar API enabled in Google Cloud
- [ ] OAuth consent screen updated with Calendar scope
- [ ] config.js has correct credentials and scopes
- [ ] All files uploaded to server
- [ ] Test login with your account
- [ ] Verify calendar events appear
- [ ] Test drag-and-drop
- [ ] Check mobile responsiveness
- [ ] Review browser console for errors

### Post-Launch
- [ ] Notify existing users about re-authentication
- [ ] Share IDO_2.0_GUIDE.md link
- [ ] Monitor for issues in first 24 hours
- [ ] Collect user feedback
- [ ] Document any issues/solutions

---

## 🎯 Success Metrics

Track these to measure success:

1. **Re-authentication Rate**: % of users who grant new permission
2. **Feature Adoption**: % using drag-and-drop scheduling
3. **Error Rate**: API failures, sync issues
4. **User Retention**: Do users continue using iDo 2.0?
5. **Feedback**: Qualitative user comments

---

## 📞 Support

### Developer Support

For technical issues:
- Check browser console first
- Review this guide
- Check IDO_2.0_IMPLEMENTATION_SUMMARY.md
- Test in incognito mode
- Contact: [@i._._.sarthak](https://instagram.com/i._._.sarthak)

### User Support

Direct users to:
- IDO_2.0_GUIDE.md for features/usage
- This setup guide for technical issues
- Your Instagram for direct support

---

## 🎉 You're Ready!

Everything is set up for iDo 2.0 deployment. The upgrade maintains backward compatibility while adding powerful new features. Good luck! 🚀

**Remember:**
1. Update Google Cloud Console scopes
2. Deploy all files
3. Test thoroughly
4. Communicate with users
5. Monitor for issues

**Happy deploying!** ✨
