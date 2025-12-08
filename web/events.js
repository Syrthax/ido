/* ===================================================
   iDo 2.0 - EVENT MANAGEMENT
   =================================================== */

let currentEditingEventId = null;

/* ===================================================
   MODAL FUNCTIONS
   =================================================== */

/**
 * Open event modal for creating new event
 */
function openEventModal(date = null) {
    const modal = document.getElementById('event-modal');
    const title = document.getElementById('modal-title');
    const deleteBtn = document.getElementById('delete-event-btn');
    const form = document.getElementById('event-form');
    
    // Reset form
    form.reset();
    currentEditingEventId = null;
    
    // Set title
    title.textContent = 'Add Event';
    deleteBtn.classList.add('hidden');
    
    // Set default date if provided
    if (date) {
        document.getElementById('event-start-date').value = date;
        document.getElementById('event-end-date').value = date;
    } else {
        // Default to today
        const today = new Date().toISOString().split('T')[0];
        document.getElementById('event-start-date').value = today;
        document.getElementById('event-end-date').value = today;
    }
    
    // Show modal
    modal.classList.remove('hidden');
}

/**
 * Open event modal for editing existing event
 */
function openEditEventModal(event) {
    const modal = document.getElementById('event-modal');
    const title = document.getElementById('modal-title');
    const deleteBtn = document.getElementById('delete-event-btn');
    
    // Set editing mode
    currentEditingEventId = event.id;
    title.textContent = 'Edit Event';
    deleteBtn.classList.remove('hidden');
    
    // Populate form
    document.getElementById('event-title').value = event.summary || '';
    document.getElementById('event-description').value = event.description || '';
    
    // Handle dates and times
    const isAllDay = !event.start.dateTime;
    document.getElementById('event-all-day').checked = isAllDay;
    
    if (isAllDay) {
        // All-day event
        const startDate = new Date(event.start.date);
        document.getElementById('event-start-date').value = event.start.date;
        
        if (event.end && event.end.date) {
            // Google Calendar end date for all-day events is exclusive (next day)
            const endDate = new Date(event.end.date);
            endDate.setDate(endDate.getDate() - 1);
            document.getElementById('event-end-date').value = endDate.toISOString().split('T')[0];
        }
        
        toggleTimeInputs(true);
    } else {
        // Timed event
        const startDateTime = new Date(event.start.dateTime);
        const endDateTime = event.end ? new Date(event.end.dateTime) : null;
        
        document.getElementById('event-start-date').value = startDateTime.toISOString().split('T')[0];
        document.getElementById('event-start-time').value = startDateTime.toTimeString().slice(0, 5);
        
        if (endDateTime) {
            document.getElementById('event-end-date').value = endDateTime.toISOString().split('T')[0];
            document.getElementById('event-end-time').value = endDateTime.toTimeString().slice(0, 5);
        }
        
        toggleTimeInputs(false);
    }
    
    // Handle recurrence
    if (event.recurrence && event.recurrence.length > 0) {
        document.getElementById('event-recurring').checked = true;
        document.getElementById('recurring-options').classList.remove('hidden');
        
        // Parse recurrence rule (simplified)
        const rrule = event.recurrence[0];
        if (rrule.includes('FREQ=DAILY')) {
            document.getElementById('event-recurrence-type').value = 'DAILY';
        } else if (rrule.includes('FREQ=WEEKLY')) {
            document.getElementById('event-recurrence-type').value = 'WEEKLY';
        } else if (rrule.includes('FREQ=MONTHLY')) {
            document.getElementById('event-recurrence-type').value = 'MONTHLY';
        } else if (rrule.includes('FREQ=YEARLY')) {
            document.getElementById('event-recurrence-type').value = 'YEARLY';
        }
        
        // Extract count if present
        const countMatch = rrule.match(/COUNT=(\d+)/);
        if (countMatch) {
            document.getElementById('event-recurrence-count').value = countMatch[1];
        }
    }
    
    // Show modal
    modal.classList.remove('hidden');
}

/**
 * Close event modal
 */
function closeEventModal() {
    const modal = document.getElementById('event-modal');
    modal.classList.add('hidden');
    currentEditingEventId = null;
}

/**
 * Toggle time inputs based on all-day checkbox
 */
function toggleTimeInputs(disabled) {
    const startTime = document.getElementById('event-start-time');
    const endTime = document.getElementById('event-end-time');
    
    startTime.disabled = disabled;
    endTime.disabled = disabled;
    
    if (disabled) {
        startTime.value = '';
        endTime.value = '';
    }
}

/* ===================================================
   EVENT CRUD OPERATIONS
   =================================================== */

/**
 * Create a new calendar event
 */
async function createCalendarEvent(eventData) {
    const accessToken = localStorage.getItem('access_token');
    if (!accessToken) {
        throw new Error('No access token available');
    }

    try {
        const response = await fetch(
            'https://www.googleapis.com/calendar/v3/calendars/primary/events',
            {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${accessToken}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(eventData)
            }
        );

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error?.message || 'Failed to create event');
        }

        return await response.json();
    } catch (error) {
        console.error('Error creating event:', error);
        throw error;
    }
}

/**
 * Update an existing calendar event
 */
async function updateCalendarEvent(eventId, eventData) {
    const accessToken = localStorage.getItem('access_token');
    if (!accessToken) {
        throw new Error('No access token available');
    }

    try {
        const response = await fetch(
            `https://www.googleapis.com/calendar/v3/calendars/primary/events/${eventId}`,
            {
                method: 'PUT',
                headers: {
                    'Authorization': `Bearer ${accessToken}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(eventData)
            }
        );

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error?.message || 'Failed to update event');
        }

        return await response.json();
    } catch (error) {
        console.error('Error updating event:', error);
        throw error;
    }
}

/**
 * Delete a calendar event
 */
async function deleteCalendarEvent(eventId) {
    const accessToken = localStorage.getItem('access_token');
    if (!accessToken) {
        throw new Error('No access token available');
    }

    try {
        const response = await fetch(
            `https://www.googleapis.com/calendar/v3/calendars/primary/events/${eventId}`,
            {
                method: 'DELETE',
                headers: {
                    'Authorization': `Bearer ${accessToken}`
                }
            }
        );

        if (!response.ok && response.status !== 410) {
            // 410 Gone means already deleted, which is fine
            const errorData = await response.json();
            throw new Error(errorData.error?.message || 'Failed to delete event');
        }

        return true;
    } catch (error) {
        console.error('Error deleting event:', error);
        throw error;
    }
}

/* ===================================================
   FORM HANDLING
   =================================================== */

/**
 * Build event data from form
 */
function buildEventDataFromForm() {
    const title = document.getElementById('event-title').value.trim();
    const description = document.getElementById('event-description').value.trim();
    const startDate = document.getElementById('event-start-date').value;
    const startTime = document.getElementById('event-start-time').value;
    const endDate = document.getElementById('event-end-date').value;
    const endTime = document.getElementById('event-end-time').value;
    const isAllDay = document.getElementById('event-all-day').checked;
    const isRecurring = document.getElementById('event-recurring').checked;
    
    if (!title) {
        throw new Error('Event title is required');
    }
    
    if (!startDate) {
        throw new Error('Start date is required');
    }
    
    const eventData = {
        summary: title,
        description: description || undefined
    };
    
    // Handle dates/times
    if (isAllDay) {
        // All-day event
        eventData.start = {
            date: startDate,
            timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone
        };
        
        // End date for all-day events is exclusive (next day)
        const end = new Date(endDate || startDate);
        end.setDate(end.getDate() + 1);
        eventData.end = {
            date: end.toISOString().split('T')[0],
            timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone
        };
    } else {
        // Timed event
        const startDateTime = new Date(`${startDate}T${startTime || '09:00'}`);
        
        let endDateTime;
        if (endDate && endTime) {
            endDateTime = new Date(`${endDate}T${endTime}`);
        } else if (endDate) {
            endDateTime = new Date(`${endDate}T${startTime || '10:00'}`);
        } else {
            // Default to 1 hour after start
            endDateTime = new Date(startDateTime);
            endDateTime.setHours(endDateTime.getHours() + 1);
        }
        
        eventData.start = {
            dateTime: startDateTime.toISOString(),
            timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone
        };
        
        eventData.end = {
            dateTime: endDateTime.toISOString(),
            timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone
        };
    }
    
    // Handle recurrence
    if (isRecurring) {
        const recurrenceType = document.getElementById('event-recurrence-type').value;
        const recurrenceCount = document.getElementById('event-recurrence-count').value;
        
        let rrule = `RRULE:FREQ=${recurrenceType}`;
        
        if (recurrenceCount) {
            rrule += `;COUNT=${recurrenceCount}`;
        }
        
        eventData.recurrence = [rrule];
    }
    
    return eventData;
}

/**
 * Handle form submission
 */
async function handleEventFormSubmit(e) {
    e.preventDefault();
    
    const saveBtn = document.getElementById('save-event-btn');
    const originalText = saveBtn.textContent;
    
    try {
        saveBtn.textContent = 'Saving...';
        saveBtn.disabled = true;
        
        const eventData = buildEventDataFromForm();
        
        if (currentEditingEventId) {
            // Update existing event
            await updateCalendarEvent(currentEditingEventId, eventData);
        } else {
            // Create new event
            await createCalendarEvent(eventData);
        }
        
        // Close modal
        closeEventModal();
        
        // Refresh calendar
        if (window.renderCalendar) {
            await window.renderCalendar();
        }
        if (window.renderTodaySection) {
            window.renderTodaySection();
        }
        
        // Show success message
        alert(currentEditingEventId ? 'Event updated successfully!' : 'Event created successfully!');
        
    } catch (error) {
        console.error('Error saving event:', error);
        
        if (error.message.includes('insufficient authentication scopes')) {
            const shouldReauth = confirm(
                'Your current login doesn\'t have permission to create calendar events. ' +
                'This requires logging out and signing in again with the correct permissions. ' +
                'Would you like to logout now?'
            );
            
            if (shouldReauth) {
                // Clear all auth data
                localStorage.removeItem('access_token');
                localStorage.removeItem('refresh_token');
                localStorage.removeItem('id_token');
                localStorage.removeItem('user_info');
                localStorage.removeItem('file_id');
                
                // Reload to show login screen
                window.location.reload();
                return;
            }
        } else {
            alert('Failed to save event: ' + error.message);
        }
    } finally {
        saveBtn.textContent = originalText;
        saveBtn.disabled = false;
    }
}

/**
 * Handle event deletion
 */
async function handleEventDelete() {
    if (!currentEditingEventId) return;
    
    if (!confirm('Are you sure you want to delete this event?')) {
        return;
    }
    
    const deleteBtn = document.getElementById('delete-event-btn');
    const originalText = deleteBtn.textContent;
    
    try {
        deleteBtn.textContent = 'Deleting...';
        deleteBtn.disabled = true;
        
        await deleteCalendarEvent(currentEditingEventId);
        
        // Close modal
        closeEventModal();
        
        // Refresh calendar
        if (window.renderCalendar) {
            await window.renderCalendar();
        }
        if (window.renderTodaySection) {
            window.renderTodaySection();
        }
        
        // Show success message
        alert('Event deleted successfully!');
        
    } catch (error) {
        console.error('Error deleting event:', error);
        alert('Failed to delete event: ' + error.message);
    } finally {
        deleteBtn.textContent = originalText;
        deleteBtn.disabled = false;
    }
}

/* ===================================================
   INITIALIZATION
   =================================================== */

/**
 * Initialize event modal
 */
function initEventModal() {
    // Add Event button
    const addEventBtn = document.getElementById('add-event-btn');
    if (addEventBtn) {
        addEventBtn.addEventListener('click', () => openEventModal());
    }
    
    // Close modal button
    const closeBtn = document.getElementById('close-modal-btn');
    if (closeBtn) {
        closeBtn.addEventListener('click', closeEventModal);
    }
    
    // Cancel button
    const cancelBtn = document.getElementById('cancel-event-btn');
    if (cancelBtn) {
        cancelBtn.addEventListener('click', closeEventModal);
    }
    
    // Modal overlay click to close
    const modalOverlay = document.querySelector('.modal-overlay');
    if (modalOverlay) {
        modalOverlay.addEventListener('click', closeEventModal);
    }
    
    // Form submission
    const eventForm = document.getElementById('event-form');
    if (eventForm) {
        eventForm.addEventListener('submit', handleEventFormSubmit);
    }
    
    // Delete button
    const deleteBtn = document.getElementById('delete-event-btn');
    if (deleteBtn) {
        deleteBtn.addEventListener('click', handleEventDelete);
    }
    
    // All-day checkbox toggle
    const allDayCheckbox = document.getElementById('event-all-day');
    if (allDayCheckbox) {
        allDayCheckbox.addEventListener('change', (e) => {
            toggleTimeInputs(e.target.checked);
        });
    }
    
    // Recurring checkbox toggle
    const recurringCheckbox = document.getElementById('event-recurring');
    const recurringOptions = document.getElementById('recurring-options');
    if (recurringCheckbox && recurringOptions) {
        recurringCheckbox.addEventListener('change', (e) => {
            if (e.target.checked) {
                recurringOptions.classList.remove('hidden');
            } else {
                recurringOptions.classList.add('hidden');
            }
        });
    }
}

/* ===================================================
   EXPORT FUNCTIONS
   =================================================== */

window.openEventModal = openEventModal;
window.openEditEventModal = openEditEventModal;
window.closeEventModal = closeEventModal;
window.initEventModal = initEventModal;
