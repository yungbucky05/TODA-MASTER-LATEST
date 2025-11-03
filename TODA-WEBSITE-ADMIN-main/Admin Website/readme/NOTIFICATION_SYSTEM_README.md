# Admin Website Notification System

## Overview
Your admin website now has a comprehensive real-time notification system that listens to Firebase and displays notifications with visual indicators and browser notifications.

## Features

### 🔔 Real-time Notifications
- **Firebase Integration**: Listens to `notifications` node in Firebase Realtime Database
- **Auto-update**: Notifications appear instantly when added to Firebase
- **Unread Count**: Visual badge showing number of unread notifications
- **Browser Notifications**: High-priority alerts trigger browser notifications

### 📋 Notification Types Supported
1. **RFID_MISSING** (High Priority)
   - Icon: 🚨
   - Triggered when a driver reports RFID as missing
   - Includes driver info and old RFID UID
   - Navigates to Driver Management when clicked

2. **DRIVER_VERIFICATION** (Normal Priority)
   - Icon: 👤
   - New driver verification requests
   - Navigates to Driver Management when clicked

3. **DISCOUNT_APPLICATION** (Normal Priority)
   - Icon: 🎫
   - New discount applications
   - Navigates to Discount Applications when clicked

### 🎨 Visual Features
- **Unread Indicator**: Blue dot for unread notifications
- **Priority Colors**: 
  - High priority: Red left border
  - Normal: Blue left border
- **Action Required**: Warning icon (⚠️) for notifications requiring action
- **Timestamp**: Shows relative time (e.g., "5m ago", "2h ago")
- **Hover Effects**: Interactive hover states

### 🔧 Functionality
- **Mark as Read**: Click any notification to mark it as read
- **Mark All as Read**: Button to mark all notifications as read at once
- **Auto-delete**: Notifications marked as "deleted" are automatically hidden
- **Dropdown Panel**: Clean dropdown with scrollable list
- **Click Outside to Close**: Dropdown closes when clicking outside

## Expected Notification Structure

```json
{
  "id": "notification_id",
  "type": "RFID_MISSING",
  "title": "RFID Reported Missing",
  "message": "Driver Name (ID: driver_123) reported RFID ABC123 as missing. RFID has been auto-unlinked.",
  "driverId": "driver_123",
  "driverName": "Driver Name",
  "oldRfidUID": "ABC123",
  "timestamp": 1730409600000,
  "isRead": false,
  "priority": "high",
  "actionRequired": true
}
```

## Field Descriptions

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Unique notification ID (auto-generated) |
| `type` | string | Yes | Notification type (RFID_MISSING, DRIVER_VERIFICATION, etc.) |
| `title` | string | Yes | Notification title |
| `message` | string | Yes | Detailed notification message |
| `timestamp` | number | Yes | Unix timestamp in milliseconds |
| `isRead` | boolean | Yes | Whether notification has been read |
| `priority` | string | No | "high" or "normal" (default: normal) |
| `actionRequired` | boolean | No | Shows warning icon if true |
| `driverId` | string | No | Related driver ID (for driver-related notifications) |
| `driverName` | string | No | Related driver name |
| `oldRfidUID` | string | No | RFID UID (for RFID-related notifications) |

## How to Test

### Method 1: Use Test Page
1. Open `test-notification.html` in your browser
2. Click any button to create test notifications
3. Open the dashboard (`index.html`) to see notifications appear

### Method 2: Add Manually to Firebase
1. Go to Firebase Console
2. Navigate to Realtime Database
3. Add a new entry under `/notifications` with the structure above

### Method 3: From Your Mobile App
Your mobile app can push notifications using:
```javascript
const notification = {
  type: "RFID_MISSING",
  title: "RFID Reported Missing",
  message: `${driverName} (ID: ${driverId}) reported RFID ${rfidUID} as missing.`,
  driverId: driverId,
  driverName: driverName,
  oldRfidUID: rfidUID,
  timestamp: Date.now(),
  isRead: false,
  priority: "high",
  actionRequired: true
};

const newNotifRef = push(ref(db, 'notifications'));
await set(newNotifRef, { id: newNotifRef.key, ...notification });
```

## Browser Notification Permission

The system automatically requests browser notification permission on first load. High-priority notifications will trigger:
- Browser/system notifications (if permission granted)
- In-app notification badge
- Visual indicators in the dropdown

## Files Modified

1. **index.html** - Added notification bell and dropdown HTML
2. **dashboard.js** - Added notification listener and functionality
3. **styles.css** - Added notification system styling

## Files Created

1. **test-notification.html** - Test page for creating notifications
2. **NOTIFICATION_SYSTEM_README.md** - This documentation

## Usage in Other Pages

To add the notification system to other admin pages:

1. Copy the notification HTML structure from `index.html`:
```html
<div class="notification-container">
  <!-- Notification bell and dropdown -->
</div>
```

2. Copy the notification JavaScript functions from `dashboard.js`

3. Ensure `styles.css` is included

4. Initialize the listener:
```javascript
listenToNotifications();
```

## Customization

### Add New Notification Types
1. Update the icon mapping in `renderNotifications()`:
```javascript
if (notif.type === 'YOUR_TYPE') icon = '🔥';
```

2. Add navigation logic in `handleNotificationAction()`:
```javascript
case 'YOUR_TYPE':
  window.location.href = 'YourPage/YourPage.html';
  break;
```

### Change Priority Colors
Edit `styles.css`:
```css
.notification-item.high-priority {
  border-left-color: #your-color;
}
```

### Adjust Notification Retention
Add auto-delete for old notifications in `listenToNotifications()`:
```javascript
const oldNotifications = notifications.filter(n => 
  Date.now() - n.timestamp > 7 * 24 * 60 * 60 * 1000 // 7 days
);
// Delete old notifications
```

## Troubleshooting

### Notifications not appearing?
- Check Firebase console - ensure notifications exist in `/notifications`
- Check browser console for errors
- Verify Firebase configuration is correct

### Notification count not updating?
- Ensure `isRead` field is boolean, not string
- Check that Firebase listener is active

### Browser notifications not showing?
- Check notification permission in browser settings
- Ensure `priority` is set to "high"
- Verify notification was created within last 5 seconds

## Support

For issues or questions:
1. Check browser console for errors
2. Verify Firebase connection
3. Test with `test-notification.html`
4. Check notification structure matches expected format
