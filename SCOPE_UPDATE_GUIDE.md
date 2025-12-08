# Scope Update Guide

## If you see "Insufficient Authentication Scopes" error:

This means your current login session doesn't have the new Calendar event creation permissions.

### Solution:

1. **Logout** from the app (click Logout button in top right)
2. **Sign in again** with Google
3. You'll see a new permission screen asking for Calendar access
4. **Accept** the new permissions

This is a one-time requirement after the Calendar event management feature was added.

### What's happening?

The app now requests these permissions:
- ✅ Google Drive (to save tasks) - *you already had this*
- ✅ View profile info - *you already had this*
- ✅ **Calendar events (read/write)** - *NEW - this is what you need to re-authorize*

Once you sign in again, you'll be able to:
- Create calendar events
- Edit calendar events  
- Delete calendar events
- Create recurring events

---

**Note**: Your tasks are safely stored in Google Drive and won't be lost when you logout.
