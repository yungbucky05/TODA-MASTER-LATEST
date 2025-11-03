# Admin Notification System

## 📢 Overview
The admin website now has a comprehensive notification system that alerts administrators when important events occur that require their attention.

## 🔔 Notification Bell System
Located in the dashboard header (top-right corner):
- Bell icon with unread count badge
- Dropdown panel showing all notifications
- Real-time updates via Firebase listeners
- Browser notifications for high-priority alerts

## 📋 Notification Types

### 1. **RFID_MISSING** (High Priority)
**When:** Driver reports missing RFID card via mobile app
**Icon:** 🚨
**Priority:** High (triggers browser notification)
**Action:** Navigates to Driver Management
**Created by:** Driver mobile app when reporting missing RFID

**Notification Structure:**
```javascript
{
  type: 'RFID_MISSING',
  priority: 'high',
  title: 'RFID Missing Report',
  message: 'Driver Name reported RFID card as missing',
  driverId: 'driver_id',
  driverName: 'Driver Name',
  rfidNumber: 'RFID_UID',
  timestamp: Date.now(),
  isRead: false,
  deleted: false,
  actionRequired: true
}
```

### 2. **DRIVER_VERIFICATION** (Medium Priority)
**When:** 
- New driver registers (pending verification)
- Rejected driver resubmits documents

**Icon:** 👤
**Priority:** Medium
**Action:** Navigates to Driver Management
**Created by:** Admin website (DriverManagement.js)

**Notification Structure:**
```javascript
{
  type: 'DRIVER_VERIFICATION',
  priority: 'medium',
  title: 'New Driver Registration' | 'Driver Resubmitted Documents',
  message: 'Driver Name has registered and needs verification' | 'Driver Name has resubmitted documents after rejection',
  driverId: 'driver_id',
  driverName: 'Driver Name',
  timestamp: Date.now(),
  isRead: false,
  deleted: false,
  actionRequired: true
}
```

### 3. **DISCOUNT_APPLICATION** (Medium Priority)
**When:** Passenger applies for discount (Senior/PWD/Student)
**Icon:** 🎫
**Priority:** Medium
**Action:** Navigates to Discount Applications
**Created by:** Admin website (DiscountApplications.js)

**Notification Structure:**
```javascript
{
  type: 'DISCOUNT_APPLICATION',
  priority: 'medium',
  title: 'New Discount Application',
  message: 'Passenger Name has applied for PWD discount',
  userId: 'user_id',
  userName: 'Passenger Name',
  discountType: 'PWD' | 'Senior' | 'Student',
  timestamp: Date.now(),
  isRead: false,
  deleted: false,
  actionRequired: true
}
```

## 🎯 How It Works

### Dashboard (index.html + dashboard.js)
1. Listens to Firebase `notifications` node in real-time
2. Renders notifications in dropdown panel
3. Updates unread count badge
4. Shows browser notifications for high-priority alerts
5. Handles notification actions (mark as read, delete, navigate)

### DriverManagement.js
1. Tracks processed drivers using `processedDriverIds` Set
2. When loading drivers, checks for:
   - New pending drivers (verification status = 'pending')
   - Auto-reset drivers (rejected → pending after resubmission)
3. Creates notification if driver hasn't been notified before
4. Adds driver to processed list to prevent duplicates

### DiscountApplications.js
1. Tracks processed applications using `processedApplicationIds` Set
2. When loading applications, scans all users for:
   - Users with `discountType` field
   - Users without `discountVerified` flag
3. Creates notification if application hasn't been notified before
4. Adds application to processed list to prevent duplicates

## 🔧 Implementation Details

### Preventing Duplicate Notifications
Both modules use a `Set` to track which items have been notified:
```javascript
let processedDriverIds = new Set();      // DriverManagement
let processedApplicationIds = new Set(); // DiscountApplications
```

**Important:** These Sets are reset on page reload, so:
- First time admin opens page → creates notifications for existing items
- Subsequent page loads → no duplicate notifications
- New items after page load → creates notifications immediately

### Firebase Structure
All notifications stored in `/notifications` node:
```
/notifications
  /-NotificationID1
    type: "DRIVER_VERIFICATION"
    priority: "medium"
    title: "..."
    message: "..."
    timestamp: 1234567890
    isRead: false
    deleted: false
    actionRequired: true
  /-NotificationID2
    ...
```

## 📱 Browser Notifications

High-priority notifications trigger native browser notifications if:
1. User granted notification permission
2. Notification is unread
3. Notification was created within last 5 seconds

**Permission Request:**
Automatically requested on dashboard load via `requestNotificationPermission()`

## 🎨 User Interface

### Notification Badge
- Red circle with number
- Shows count of unread notifications
- Hidden when count is 0

### Notification Items
- **Unread:** Bold text with blue dot indicator
- **Read:** Grayed out text
- **High Priority:** Red border
- **Action Required:** Orange accent

### Actions
- Click notification → Navigate to relevant module & mark as read
- "Mark all as read" button → Marks all notifications as read
- Delete button (per notification) → Soft delete (sets `deleted: true`)

## 🚀 Future Enhancements

### Potential Additional Notifications:
1. **Queue Congestion** - When queue exceeds certain threshold
2. **Contribution Reminders** - Drivers with unpaid contributions
3. **System Alerts** - Database issues, errors, maintenance
4. **Booking Anomalies** - Cancelled bookings, disputes
5. **Revenue Milestones** - Daily/weekly/monthly targets reached

### Enhancement Ideas:
- Email notifications for critical alerts
- SMS notifications for high-priority items
- Notification history/archive
- Filter notifications by type
- Snooze/reminder functionality
- Batch operations (bulk mark as read, bulk delete)

## 📝 Files Modified

### Core Notification System
- ✅ `dashboard.js` - Notification listener, rendering, and actions
- ✅ `index.html` - Notification bell UI and dropdown

### Notification Creators
- ✅ `DriverManagement/DriverManagement.js` - Creates DRIVER_VERIFICATION notifications
- ✅ `DiscountApplications/DiscountApplications.js` - Creates DISCOUNT_APPLICATION notifications

### Mobile App (External)
- 📱 Driver app - Creates RFID_MISSING notifications

## 🔒 Security Considerations

1. **Read-only for Admin:** Admin website can only create notifications for events it detects
2. **No User Data Exposure:** Notifications only contain necessary info (names, IDs, types)
3. **Soft Deletes:** Deleted notifications not removed, just hidden (for audit trail)
4. **Timestamp Tracking:** All notifications have creation timestamp for chronological ordering

## 📊 Testing Checklist

- [x] New driver registration creates notification
- [x] Rejected driver resubmission creates notification  
- [x] New discount application creates notification
- [x] RFID missing report creates notification (from mobile app)
- [x] Notification bell shows correct unread count
- [x] Clicking notification navigates to correct page
- [x] Mark as read works correctly
- [x] Delete notification works correctly
- [x] Browser notification appears for high priority
- [x] No duplicate notifications on page refresh
- [x] Real-time updates when new notifications arrive
