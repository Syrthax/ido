# iDo 2.0 - Upgrade Guide

## 🎉 Welcome to iDo 2.0!

Your simple task manager has been upgraded with a powerful calendar integration and enhanced UI. This guide will help you understand all the new features.

---

## ✨ What's New in iDo 2.0

### 1. **Three-Pane Layout**

The new interface is organized into three main sections:

#### **Left Sidebar**
- **Mini Month Calendar**: Quick month view (1-31) with today highlighted
- **Today Section**: Shows today's Google Calendar events and tasks
- **Filters**: Toggle between Personal and Work views (All/Personal/Work)

#### **Center Area**
- **Weekly Calendar View**: Monday-Saturday weekly grid
- **Google Calendar Events**: Your calendar events displayed with time
- **Planned Tasks**: Tasks with due dates shown on their scheduled day
- **Drag-and-Drop**: Drop tasks from the right sidebar into any day

#### **Right Sidebar**
- **Profile Info**: Your Google account avatar, name, and email
- **Tasks Panel**: 
  - **Planned**: Tasks with assigned dates
  - **Unplanned**: Tasks without dates
- **Add Task**: Quick task creation with date/time picker

### 2. **Google Calendar Integration**

iDo 2.0 now reads your Google Calendar events and displays them alongside your tasks:

- **Read-only access**: Events are displayed but not modified
- **Weekly view**: See all your calendar events for the week
- **Today view**: Quick glance at today's events in the left sidebar
- **Visual distinction**: Calendar events have a different style from tasks

### 3. **Drag-and-Drop Scheduling**

The most powerful new feature - schedule tasks by dragging them:

- **From Unplanned to Calendar**: Drag an unplanned task to any day to schedule it
- **Between Days**: Move planned tasks to different days by dragging
- **Auto-saves**: Changes are automatically synced to Google Drive
- **Visual Feedback**: Columns highlight when you drag over them

### 4. **Enhanced Task Management**

Tasks are now organized into two categories:

- **Planned Tasks**: 
  - Have a due date assigned
  - Appear on the calendar
  - Show in the "Planned" section of right sidebar
  - Can be dragged between different days

- **Unplanned Tasks**: 
  - No due date
  - Show in the "Unplanned" section
  - Can be dragged to the calendar to schedule them
  - Perfect for your backlog or ideas

### 5. **Week Navigation**

Easily navigate through your weeks:

- **Previous/Next Week**: Arrow buttons to move between weeks
- **Today Button**: Quickly jump back to the current week
- **Week Range Display**: Shows the date range (e.g., "1 - 6")
- **Month/Year Header**: Current month and year displayed prominently

---

## 🚀 Getting Started

### First Time Setup

1. **Login with Google**
   - Click "Sign in with Google"
   - Grant permissions for:
     - Google Drive (to store tasks)
     - Google Calendar (to read events)
     - Profile info (name, email, avatar)

2. **Explore the Interface**
   - Check your calendar events in the center
   - See today's schedule in the left sidebar
   - Find your existing tasks in the right sidebar

### Creating Tasks

#### Option 1: Using the Add Button
1. Click the "+" button in the right sidebar header
2. Type your task
3. Optionally add a due date/time
4. Click "Add"

#### Option 2: Quick Add
- If task input is already visible, just type and press Enter

### Scheduling Tasks

#### Method 1: Assign Date When Creating
1. Click the calendar icon when creating a task
2. Choose "Natural" or "Date & Time" tab
3. Set your preferred date/time
4. Task will appear in "Planned" section

#### Method 2: Drag and Drop
1. Find an unplanned task in the right sidebar
2. Click and drag it to a day column in the calendar
3. Drop it - the task is now scheduled for that day
4. Default time is 9:00 AM (uses existing time if rescheduling)

### Rescheduling Tasks

1. Find the task on the calendar or in "Planned" section
2. Drag it to a different day
3. Drop - the due date is updated automatically

### Viewing Your Week

- **Navigate**: Use arrow buttons or click a date in the mini calendar
- **Today**: Click "Today" button to return to current week
- **Scroll**: Scroll down to see all events and tasks for each day

---

## 📋 UI Elements Explained

### Calendar Day Columns

Each day shows:
- **Day Name**: Mon, Tue, Wed, etc.
- **Day Number**: Date of the month
- **Blue Background**: Indicates today
- **Events**: Google Calendar events (blue background, time shown)
- **Tasks**: Your iDo tasks (white background, checkbox)

### Task Items

- **Checkbox**: Mark task as complete/incomplete
- **Star Icon**: Pin important tasks to the top
- **Drag Handle**: Six dots - grab here to drag
- **Delete Button**: Appears on hover
- **Due Date**: Shows when task is due (if scheduled)

### Mini Calendar (Left Sidebar)

- **Current Month**: Shows all days 1-31
- **Today**: Highlighted in blue
- **Clickable**: Click any date to jump to that week

### Today Section (Left Sidebar)

- **Events**: Shows all calendar events for today
- **Tasks**: Shows all tasks due today
- **Empty State**: "No events or tasks today" when nothing scheduled

---

## 🎨 Visual Indicators

### Task States

- **Normal**: White background, black text
- **Completed**: Strikethrough, faded
- **Overdue**: Red-tinted background
- **Pinned**: Star icon filled
- **Dragging**: Semi-transparent

### Calendar States

- **Today Column**: Light blue background
- **Drag Over**: Dashed blue outline when dragging task over it
- **Hover**: Slight highlight on hover

### Sync Status (Bottom Bar)

- **Synced ✓**: Green - all changes saved
- **Saving...**: Yellow - currently saving
- **Error**: Red - sync failed (check connection)

---

## 💡 Pro Tips

### Task Management

1. **Use Unplanned for Backlog**: Keep task ideas in "Unplanned" until you're ready to schedule them
2. **Pin Important Tasks**: Star tasks to keep them at the top of lists
3. **Natural Language**: Use phrases like "tomorrow at 3pm" or "next Monday 9am"
4. **Reminders**: Set reminders to get notifications before due time

### Calendar Usage

1. **Weekly Planning**: At the start of each week, drag unplanned tasks to appropriate days
2. **Daily Review**: Check the "Today" section each morning
3. **Quick Reschedule**: Drag tasks between days if priorities change
4. **Mini Calendar**: Click dates to quickly jump to any week

### Productivity Hacks

1. **Time Blocking**: Drag tasks to specific days to plan your week
2. **Theme Days**: Dedicate certain days to certain types of tasks
3. **Morning Routine**: Check "Today" section to see your day at a glance
4. **End of Day**: Mark completed tasks as done, reschedule any incomplete ones

---

## 🔧 Technical Details

### Data Storage

- **STG.json**: Your tasks are stored in Google Drive as before
- **Auto-sync**: Changes save automatically to the cloud
- **Local State**: Calendar events are fetched fresh each time (not stored locally)

### Permissions Required

- `drive.file`: Read/write your iDo task file
- `calendar.readonly`: Read your Google Calendar events
- `userinfo.email`: Display your email
- `userinfo.profile`: Display your name and avatar

### Browser Compatibility

- **Chrome/Edge**: Full support
- **Firefox**: Full support
- **Safari**: Full support
- **Mobile**: Limited support (responsive design for smaller screens)

### Performance

- **Calendar Events**: Fetched once per week view
- **Task Updates**: Instant with optimistic UI updates
- **Sync**: Debounced to prevent excessive saves

---

## 🐛 Troubleshooting

### Calendar Events Not Showing

1. Check you granted Calendar permission during login
2. Try logging out and logging back in
3. Ensure you have events in your Google Calendar
4. Check browser console for errors

### Tasks Not Syncing

1. Check sync status indicator (bottom bar)
2. Click manual sync button to force a sync
3. Check internet connection
4. Verify Google Drive access in your Google Account settings

### Drag and Drop Not Working

1. Make sure you're dragging from the drag handle (six dots)
2. Try refreshing the page
3. Check if JavaScript is enabled
4. Try a different browser

### Today Section Empty

- This is normal if you have no events or tasks scheduled for today
- Create or drag a task to today to see it appear

---

## 📱 Responsive Design

### Desktop (1200px+)
- Full three-pane layout
- All features available

### Tablet (768px - 1200px)
- Calendar takes full width
- Sidebars hidden (focus on calendar)

### Mobile (<768px)
- Stacked layout
- Calendar shows 3 days at a time
- Simplified interface

---

## 🔄 Migration Notes

### From iDo 1.0

All your existing tasks are automatically migrated:
- Tasks without dates → Unplanned section
- Tasks with dates → Planned section + Calendar
- All metadata preserved (priority, reminders, etc.)

### First Login After Upgrade

1. You'll be prompted to re-authenticate (new Calendar permission)
2. All existing tasks will load automatically
3. Calendar events will appear in the center pane

---

## 🎯 Best Practices

### Task Organization

✅ **DO:**
- Keep unplanned tasks for ideas and backlog
- Schedule tasks to specific days when ready to work on them
- Use priority (star) for truly important tasks
- Mark tasks complete as you finish them

❌ **DON'T:**
- Schedule every single task immediately
- Leave completed tasks unchecked
- Forget to review and reschedule overdue tasks

### Calendar Management

✅ **DO:**
- Review the week ahead on Mondays
- Use "Today" section for daily planning
- Drag tasks to reschedule when priorities change

❌ **DON'T:**
- Over-schedule your days
- Ignore calendar events when planning tasks
- Forget that calendar events are read-only

---

## 🆘 Support

### Need Help?

If you encounter issues:
1. Check the browser console for errors
2. Try logging out and back in
3. Clear browser cache and reload
4. Contact the developer on Instagram: [@i._._.sarthak](https://instagram.com/i._._.sarthak)

### Feature Requests

Have ideas for iDo 3.0? Share them!

---

## 📄 Privacy & Security

- **Your Data**: Stored only in your personal Google Drive
- **Calendar Access**: Read-only, never modified
- **No Tracking**: No analytics or data collection
- **Open Source**: Code is transparent and auditable

---

## 🎊 Enjoy iDo 2.0!

You now have a powerful, integrated task and calendar management system. Take some time to explore all the features and find a workflow that works best for you.

**Happy Planning! 📝✨**
