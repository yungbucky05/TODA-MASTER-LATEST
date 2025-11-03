# Mobile App Integration - Flagged Accounts System

This document provides the complete implementation guide for integrating the flagged accounts system into your Driver and Customer mobile apps.

---

## 📱 Overview

The flagged accounts system automatically monitors user behavior and applies restrictions based on violations. Users can be in one of four states:

- **Good Standing** (0-50 pts) ✅ - Full access
- **Monitored** (51-150 pts) 👀 - Warning displayed
- **Restricted** (151-300 pts) ⚠️ - Limited features
- **Suspended** (301+ pts) 🚫 - Blocked access

### ✅ Admin Panel Update (November 4, 2025)

The admin panel now **properly recalculates flag scores** when resolving, dismissing, or escalating flags. It sums up all active flags instead of using simple arithmetic, ensuring accuracy and consistency with the mobile app's calculation method.

---

## 🗄️ Database Structure

### **1. Driver Account Fields**

Path: `drivers/{driverId}`

```javascript
{
  // Existing fields
  "driverId": "string",
  "driverName": "string",
  "phoneNumber": "string",
  "email": "string",
  "isActive": true,
  
  // Flag system fields
  "flagScore": 0,           // Total points from active flags
  "flagStatus": "good"      // "good" | "monitored" | "restricted" | "suspended"
}
```

### **2. Customer Account Fields**

Path: `users/{userId}`

```javascript
{
  // Existing fields
  "userId": "string",
  "userType": "PASSENGER",
  "name": "string",
  "phoneNumber": "string",
  "email": "string",
  
  // Flag system fields
  "flagScore": 0,
  "flagStatus": "good"
}
```

### **3. Driver Flags Collection**

Path: `driverFlags/{driverId}/{flagId}`

```javascript
{
  "flagId": "auto-generated",
  "type": "string",         // Flag type identifier
  "severity": "string",     // "low" | "medium" | "high" | "critical"
  "points": 75,            // Points for this flag
  "timestamp": 1699123456000,
  "status": "active",      // "active" | "resolved" | "dismissed"
  
  "details": {
    // Flag-specific details
    "reason": "Low contribution percentage",
    "percentage": "40%"
  },
  
  "notes": "Optional admin notes"
}
```

### **4. Customer Flags Collection**

Path: `userFlags/{userId}/{flagId}`

```javascript
{
  "flagId": "auto-generated",
  "type": "string",
  "severity": "string",
  "points": 100,
  "timestamp": 1699123456000,
  "status": "active",
  
  "details": {
    "reason": "Multiple no-shows detected",
    "totalBookings": 10,
    "noShowCount": 3,
    "noShowRate": "30.0%"
  },
  
  "notes": ""
}
```

---

## 🚗 Driver App Implementation

### **1. Fetch Driver Flag Status**

```javascript
import { ref, onValue } from 'firebase/database';

// Listen to driver account status (real-time)
function monitorDriverFlagStatus(driverId) {
  const driverRef = ref(database, `drivers/${driverId}`);
  
  onValue(driverRef, (snapshot) => {
    const driver = snapshot.val();
    
    if (driver) {
      // Note: flagScore is now automatically recalculated by admin panel
      // when flags are resolved, dismissed, or escalated
      const flagScore = driver.flagScore || 0;
      const flagStatus = driver.flagStatus || 'good';
      
      // Update UI based on status
      updateDriverUI(flagStatus, flagScore);
      
      // Apply restrictions
      applyDriverRestrictions(flagStatus);
    }
  });
}

// Listen to active flags (real-time)
function monitorDriverFlags(driverId) {
  const flagsRef = ref(database, `driverFlags/${driverId}`);
  
  onValue(flagsRef, (snapshot) => {
    const flags = snapshot.val() || {};
    const activeFlags = Object.values(flags).filter(f => f.status === 'active');
    
    // Calculate total score from active flags (for verification/display)
    // This matches the admin panel's recalculation method
    const totalScore = activeFlags.reduce((sum, flag) => sum + (flag.points || 0), 0);
    
    // Optional: Verify against stored flagScore
    // (Should match, as admin panel now recalculates correctly)
    
    // Display flags in UI
    displayDriverFlags(activeFlags, totalScore);
  });
}
```

### **2. Display Flag Status UI**

```javascript
function updateDriverUI(status, score) {
  const statusConfig = {
    good: {
      color: '#4caf50',
      icon: '✅',
      title: 'Good Standing',
      message: 'Keep up the great work!',
      showBanner: false
    },
    monitored: {
      color: '#ff9800',
      icon: '👀',
      title: 'Account Monitored',
      message: 'Please improve your performance to avoid restrictions.',
      showBanner: true,
      bannerType: 'warning'
    },
    restricted: {
      color: '#f44336',
      icon: '⚠️',
      title: 'Account Restricted',
      message: 'Your account has limited access. Contact support or resolve active flags.',
      showBanner: true,
      bannerType: 'error'
    },
    suspended: {
      color: '#d32f2f',
      icon: '🚫',
      title: 'Account Suspended',
      message: 'Your account is suspended. You cannot accept bookings. Contact admin immediately.',
      showBanner: true,
      bannerType: 'critical',
      blockApp: true
    }
  };
  
  const config = statusConfig[status];
  
  // Show banner if needed
  if (config.showBanner) {
    showBanner({
      icon: config.icon,
      title: config.title,
      message: config.message,
      color: config.color,
      type: config.bannerType
    });
  }
  
  // Block app if suspended
  if (config.blockApp) {
    blockDriverApp();
  }
  
  // Update status badge in profile
  updateStatusBadge(config.icon, config.title, config.color, score);
}
```

### **3. Display Active Flags**

```javascript
function displayDriverFlags(activeFlags, totalScore) {
  const flagMessages = {
    'LOW_CONTRIBUTIONS': '💰 Low Contributions - Please increase your weekly contributions',
    'INACTIVE_ACCOUNT': '😴 Inactive Account - Log in regularly to maintain good standing',
    'HIGH_CANCELLATION_RATE': '🚫 High Cancellation Rate - Avoid cancelling accepted bookings',
    'CUSTOMER_COMPLAINTS': '😠 Customer Complaints - Improve service quality',
    'RFID_ISSUES': '🔑 RFID Issues - Ensure proper RFID card usage'
  };
  
  if (activeFlags.length === 0) {
    // Show "no flags" message
    return;
  }
  
  // Calculate status based on total score
  const status = totalScore > 300 ? 'suspended' 
    : totalScore > 150 ? 'restricted' 
    : totalScore > 50 ? 'monitored' 
    : 'good';
  
  // Create flag list UI
  const flagsHTML = activeFlags.map(flag => `
    <div class="flag-item ${flag.severity}">
      <div class="flag-header">
        <span class="flag-icon">${flagMessages[flag.type]?.split(' ')[0] || '⚠️'}</span>
        <span class="flag-title">${flag.type.replace(/_/g, ' ')}</span>
        <span class="flag-points">+${flag.points} pts</span>
      </div>
      <p class="flag-message">${flagMessages[flag.type] || 'Please improve this area'}</p>
      ${flag.details ? `<p class="flag-details">${JSON.stringify(flag.details)}</p>` : ''}
      ${flag.status === 'resolved' ? `<span class="flag-resolved">✅ Resolved</span>` : ''}
    </div>
  `).join('');
  
  // Display total score and status
  const headerHTML = `
    <div class="flags-header">
      <h3>Active Flags (${activeFlags.length})</h3>
      <div class="score-badge ${status}">
        Total: ${totalScore} points
      </div>
    </div>
  `;
  
  // Display in profile or dedicated flags screen
  document.getElementById('active-flags-list').innerHTML = headerHTML + flagsHTML;
}
```

### **4. Apply Restrictions**

```javascript
function applyDriverRestrictions(status) {
  switch (status) {
    case 'good':
      // No restrictions
      enableAllFeatures();
      break;
      
    case 'monitored':
      // Show warnings but allow all actions
      enableAllFeatures();
      showPerformanceWarnings(true);
      break;
      
    case 'restricted':
      // Limit bookings per day
      setDailyBookingLimit(5);
      requireAdditionalVerification(true);
      showPerformanceWarnings(true);
      break;
      
    case 'suspended':
      // Block all booking activities
      disableBookingFeatures();
      showContactSupportScreen();
      break;
  }
}
```

---

## 👤 Customer App Implementation

### **1. Fetch Customer Flag Status**

```javascript
import { ref, onValue } from 'firebase/database';

// Listen to customer account status (real-time)
function monitorCustomerFlagStatus(userId) {
  const userRef = ref(database, `users/${userId}`);
  
  onValue(userRef, (snapshot) => {
    const user = snapshot.val();
    
    if (user && user.userType === 'PASSENGER') {
      // Note: flagScore is now automatically recalculated by admin panel
      // when flags are resolved, dismissed, or escalated
      const flagScore = user.flagScore || 0;
      const flagStatus = user.flagStatus || 'good';
      
      // Update UI
      updateCustomerUI(flagStatus, flagScore);
      
      // Apply restrictions
      applyCustomerRestrictions(flagStatus);
    }
  });
}

// Listen to active flags (real-time)
function monitorCustomerFlags(userId) {
  const flagsRef = ref(database, `userFlags/${userId}`);
  
  onValue(flagsRef, (snapshot) => {
    const flags = snapshot.val() || {};
    const activeFlags = Object.values(flags).filter(f => f.status === 'active');
    
    // Calculate total score from active flags (for verification/display)
    // This matches the admin panel's recalculation method
    const totalScore = activeFlags.reduce((sum, flag) => sum + (flag.points || 0), 0);
    
    // Display flags
    displayCustomerFlags(activeFlags, totalScore);
  });
}
```

### **2. Display Flag Status UI**

```javascript
function updateCustomerUI(status, score) {
  const statusConfig = {
    good: {
      color: '#4caf50',
      icon: '✅',
      title: 'Good Standing',
      message: 'Thank you for being a great passenger!',
      showBanner: false
    },
    monitored: {
      color: '#ff9800',
      icon: '👀',
      title: 'Account Monitored',
      message: 'Please improve your booking behavior to avoid restrictions.',
      showBanner: true
    },
    restricted: {
      color: '#f44336',
      icon: '⚠️',
      title: 'Account Restricted',
      message: 'Limited bookings allowed. Resolve issues to restore full access.',
      showBanner: true,
      requirePrepayment: true
    },
    suspended: {
      color: '#d32f2f',
      icon: '🚫',
      title: 'Account Suspended',
      message: 'You cannot create bookings. Contact support immediately.',
      showBanner: true,
      blockBooking: true
    }
  };
  
  const config = statusConfig[status];
  
  if (config.showBanner) {
    showBanner({
      icon: config.icon,
      title: config.title,
      message: config.message,
      color: config.color
    });
  }
  
  if (config.blockBooking) {
    blockBookingFeatures();
  }
  
  updateStatusBadge(config.icon, config.title, config.color, score);
}
```

### **3. Display Active Flags**

```javascript
function displayCustomerFlags(activeFlags, totalScore) {
  const flagMessages = {
    'NO_SHOW': '👻 No-Shows Detected - Please show up for your bookings',
    'NON_PAYMENT': '💸 Unpaid Booking - Please settle your outstanding payment',
    'WRONG_PIN': '📍 Location Errors - Ensure correct pickup location',
    'ABUSIVE_BEHAVIOR': '😠 Behavior Warning - Treat drivers with respect',
    'EXCESSIVE_CANCELLATIONS': '🚫 Too Many Cancellations - Avoid cancelling confirmed bookings'
  };
  
  if (activeFlags.length === 0) {
    return;
  }
  
  // Calculate status based on total score
  const status = totalScore > 300 ? 'suspended' 
    : totalScore > 150 ? 'restricted' 
    : totalScore > 50 ? 'monitored' 
    : 'good';
  
  const flagsHTML = activeFlags.map(flag => `
    <div class="customer-flag-item ${flag.severity}">
      <div class="flag-header">
        <span class="flag-icon">${flagMessages[flag.type]?.split(' ')[0] || '⚠️'}</span>
        <span class="flag-title">${flag.type.replace(/_/g, ' ')}</span>
        <span class="flag-points">+${flag.points} pts</span>
      </div>
      <p class="flag-message">${flagMessages[flag.type]}</p>
      ${flag.details ? `
        <div class="flag-details">
          ${flag.details.noShowRate ? `<p>No-show rate: ${flag.details.noShowRate}</p>` : ''}
          ${flag.details.cancellationRate ? `<p>Cancellation rate: ${flag.details.cancellationRate}</p>` : ''}
        </div>
      ` : ''}
      <button onclick="contactSupport('${flag.flagId}')">Resolve This Issue</button>
    </div>
  `).join('');
  
  // Display total score and status
  const headerHTML = `
    <div class="flags-header">
      <h3>Active Flags (${activeFlags.length})</h3>
      <div class="score-badge ${status}">
        Total: ${totalScore} points
      </div>
    </div>
  `;
  
  document.getElementById('customer-flags-list').innerHTML = headerHTML + flagsHTML;
}
```

### **4. Apply Restrictions**

```javascript
function applyCustomerRestrictions(status) {
  switch (status) {
    case 'good':
      // No restrictions
      setDailyBookingLimit(null);
      requirePrepayment(false);
      break;
      
    case 'monitored':
      // Show warning before booking
      showBookingWarning(true);
      break;
      
    case 'restricted':
      // Limit bookings and require prepayment
      setDailyBookingLimit(2);
      requirePrepayment(true);
      showBookingWarning(true);
      break;
      
    case 'suspended':
      // Block all booking features
      disableBookingButton();
      showContactSupportScreen();
      break;
  }
}

function requirePrepayment(required) {
  // Force online payment before booking confirmation
  if (required) {
    disableCashPaymentOption();
    showPrepaymentNotice();
  } else {
    enableAllPaymentOptions();
  }
}
```

---

## 🎨 UI Components (React Native / Flutter)

### **Status Banner Component**

```javascript
function FlagStatusBanner({ status, score, onContactSupport }) {
  const configs = {
    monitored: {
      bgColor: '#FFF3CD',
      borderColor: '#FFC107',
      icon: '👀',
      title: 'Account Monitored',
      message: 'Improve behavior to avoid restrictions'
    },
    restricted: {
      bgColor: '#F8D7DA',
      borderColor: '#F44336',
      icon: '⚠️',
      title: 'Account Restricted',
      message: 'Limited access. Resolve issues now.'
    },
    suspended: {
      bgColor: '#FFCDD2',
      borderColor: '#D32F2F',
      icon: '🚫',
      title: 'Account Suspended',
      message: 'Contact support immediately'
    }
  };
  
  if (status === 'good') return null;
  
  const config = configs[status];
  
  return (
    <View style={{
      backgroundColor: config.bgColor,
      borderLeftWidth: 4,
      borderLeftColor: config.borderColor,
      padding: 16,
      margin: 16,
      borderRadius: 8
    }}>
      <Text style={{ fontSize: 18, fontWeight: 'bold' }}>
        {config.icon} {config.title}
      </Text>
      <Text style={{ marginTop: 8 }}>
        {config.message}
      </Text>
      <Text style={{ marginTop: 4, fontWeight: 'bold' }}>
        Flag Score: {score} points
      </Text>
      <Button 
        title="Contact Support" 
        onPress={onContactSupport}
        style={{ marginTop: 12 }}
      />
    </View>
  );
}
```

---

## 🔔 Real-time Notifications

### **Listen for Flag Changes**

```javascript
function setupFlagNotifications(userId, userType) {
  const flagsPath = userType === 'driver' 
    ? `driverFlags/${userId}` 
    : `userFlags/${userId}`;
  
  const flagsRef = ref(database, flagsPath);
  
  // Listen for new flags
  onChildAdded(flagsRef, (snapshot) => {
    const flag = snapshot.val();
    
    if (flag.status === 'active') {
      showNotification({
        title: '⚠️ New Flag Added',
        message: `You've received a ${flag.type} flag (+${flag.points} pts)`,
        action: 'VIEW_FLAGS'
      });
    }
  });
  
  // Listen for resolved flags
  onChildChanged(flagsRef, (snapshot) => {
    const flag = snapshot.val();
    
    if (flag.status === 'resolved') {
      showNotification({
        title: '✅ Flag Resolved',
        message: `Your ${flag.type} flag has been resolved!`,
        action: 'VIEW_ACCOUNT'
      });
    }
  });
}
```

---

## 📊 Flag Types Reference

### **Driver Flags**
| Type | Points | Severity | Description |
|------|--------|----------|-------------|
| `LOW_CONTRIBUTIONS` | 75 | High | Below 50% of average weekly contributions |
| `INACTIVE_ACCOUNT` | 50 | Medium | No login for 7+ days |
| `HIGH_CANCELLATION_RATE` | 75 | High | Cancellation rate > 15% |
| `CUSTOMER_COMPLAINTS` | 100 | Critical | Multiple customer complaints |
| `RFID_ISSUES` | 50 | Medium | RFID card usage problems |

### **Customer Flags**
| Type | Points | Severity | Description |
|------|--------|----------|-------------|
| `NO_SHOW` | 100 | Critical | No-show rate > 20% |
| `NON_PAYMENT` | 100 | Critical | Unpaid bookings detected |
| `WRONG_PIN` | 50 | Medium | Wrong location PIN > 30% |
| `ABUSIVE_BEHAVIOR` | 100 | Critical | Reported abusive behavior |
| `EXCESSIVE_CANCELLATIONS` | 75 | High | Cancellation rate > 25% |

---

## 🚀 Implementation Checklist

### **Backend Setup**
- [ ] Ensure `drivers` collection has `flagScore` and `flagStatus` fields
- [ ] Ensure `users` collection has `flagScore` and `flagStatus` fields (for PASSENGER type)
- [ ] Auto-detection system is running on admin panel

### **Driver App**
- [ ] Implement real-time listener for `drivers/{driverId}`
- [ ] Implement real-time listener for `driverFlags/{driverId}`
- [ ] Display flag status banner
- [ ] Show active flags in profile/account section
- [ ] Apply booking restrictions based on status
- [ ] Block app access if suspended
- [ ] Add contact support button

### **Customer App**
- [ ] Implement real-time listener for `users/{userId}`
- [ ] Implement real-time listener for `userFlags/{userId}`
- [ ] Display flag status banner
- [ ] Show active flags in account section
- [ ] Apply booking restrictions (daily limit, prepayment)
- [ ] Block booking features if suspended
- [ ] Add contact support functionality

### **Testing**
- [ ] Test good standing (no flags)
- [ ] Test monitored status (warnings only)
- [ ] Test restricted status (limited features)
- [ ] Test suspended status (blocked access)
- [ ] Test real-time updates when admin resolves flags
- [ ] Test notifications for new/resolved flags

---

## 🆘 Support Integration

### **Contact Support Feature**

```javascript
function contactSupportAboutFlag(flagId, flagType) {
  // Navigate to support screen with pre-filled message
  const message = `I need help with my ${flagType.replace(/_/g, ' ')} flag (ID: ${flagId})`;
  
  openSupportChat({
    subject: 'Flag Resolution Request',
    message: message,
    flagId: flagId
  });
}
```

---

## 📱 Sample UI Flow

### **Driver App Flow**
1. Driver opens app
2. System checks `drivers/{driverId}` for `flagStatus`
3. If flagged:
   - Show banner at top of dashboard
   - Display flags in profile section
   - Apply restrictions to booking features
4. If suspended:
   - Block access to main app
   - Show full-screen "Contact Support" message

### **Customer App Flow**
1. Customer opens app
2. System checks `users/{userId}` for `flagStatus`
3. If flagged:
   - Show banner at top of home screen
   - Display flags in account section
   - Apply booking limits
4. If restricted:
   - Require prepayment for bookings
5. If suspended:
   - Disable "Book Now" button
   - Show "Contact Support" screen

---

## 🔒 Security Rules

Add these Firebase Realtime Database rules:

```json
{
  "rules": {
    "drivers": {
      "$driverId": {
        ".read": "$driverId === auth.uid",
        ".write": "false"
      }
    },
    "users": {
      "$userId": {
        ".read": "$userId === auth.uid",
        ".write": "false"
      }
    },
    "driverFlags": {
      "$driverId": {
        ".read": "$driverId === auth.uid",
        ".write": "false"
      }
    },
    "userFlags": {
      "$userId": {
        ".read": "$userId === auth.uid",
        ".write": "false"
      }
    }
  }
}
```

---

## � Admin Panel Synchronization

### **Flag Score Calculation Method**

Both the mobile app and admin panel now use **identical calculation methods**:

```javascript
// Mobile App (Driver/Customer)
const activeFlags = allFlags.filter(f => f.status === 'active');
const totalScore = activeFlags.reduce((sum, flag) => sum + (flag.points || 0), 0);

// Admin Panel (when resolving/dismissing/escalating flags)
async function recalculateFlagScore(accountId, accountType) {
  // 1. Fetch all flags
  const allFlags = await getAllFlags(accountId, accountType);
  
  // 2. Sum only active flags
  let totalScore = 0;
  allFlags.forEach(flag => {
    if (flag.status === 'active') {
      totalScore += (flag.points || 0);
    }
  });
  
  // 3. Update account
  await updateAccount(accountId, {
    flagScore: totalScore,
    flagStatus: calculateStatus(totalScore)
  });
}
```

### **Why This Matters**

✅ **Consistency**: Both systems calculate scores the same way  
✅ **Accuracy**: Always reflects the true sum of active flags  
✅ **Real-time**: Mobile app sees updates immediately when admin resolves flags  
✅ **Self-healing**: Any admin action recalculates the score from scratch

### **Status Calculation**

Both systems use these thresholds:
- `totalScore > 300` → `suspended` 🔴
- `totalScore > 150` → `restricted` 🟠
- `totalScore > 50` → `monitored` 🟡
- `else` → `good` 🟢

---

## 📊 Best Practices

### **Mobile App Development**

1. **Always calculate locally**: Don't rely solely on stored `flagScore`, recalculate from active flags for verification
2. **Listen to both**: Monitor both the account record and the flags collection for real-time updates
3. **Handle edge cases**: Account for missing or null values gracefully
4. **Cache intelligently**: Store flag data locally but refresh on each app launch
5. **Test all states**: Verify behavior in all four status levels

### **Testing Checklist**

- [ ] Verify score calculation matches admin panel
- [ ] Test real-time updates when admin resolves flags
- [ ] Confirm UI updates when flags change
- [ ] Test restrictions for each status level
- [ ] Verify notifications appear correctly
- [ ] Test edge cases (no flags, all flags resolved)

---

## �📞 Contact

For questions about the flagged accounts system, contact the admin team or refer to the admin panel documentation.

See also: `FLAG_SCORE_RECALCULATION.md` for technical details on the admin panel implementation.

---

**Last Updated:** November 4, 2025  
**Admin Panel Update:** Recalculation implemented ✅
