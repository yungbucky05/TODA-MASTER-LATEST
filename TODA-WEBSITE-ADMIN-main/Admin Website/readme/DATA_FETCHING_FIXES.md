# Data Fetching Fixes - Queue & Discount Applications

## 🔍 Issues Found & Fixed

### Issue 1: QueueManagement - Wrong Database Path ❌

**Problem:**
- Code was looking for `driverQueue` node
- Actual database has `queue` node
- Field names didn't match actual database structure

**What Was Wrong:**
```javascript
// OLD CODE - WRONG PATH
const queueRef = ref(db, 'driverQueue'); // ❌ This node doesn't exist!
```

**Actual Database Structure:**
```json
"queue": {
  "1761960587": {
    "contributionPaid": true,
    "driverName": "Lucas Abad",
    "driverRFID": "2A8B5505",          // Note: driverRFID, not driverId
    "queueTime": "1761960587",         // Note: queueTime field
    "status": "waiting",
    "timestamp": "2025-11-01 09:29:47",
    "todaNumber": "395"
  }
}
```

**What Was Fixed:**
1. ✅ Changed path from `driverQueue` to `queue`
2. ✅ Added mapping for `driverRFID` → `driverId`
3. ✅ Handle both `queueTime` and `timestamp` fields
4. ✅ Display `contributionPaid` status with badge
5. ✅ Show `todaNumber` in details
6. ✅ Handle formatted timestamp strings (e.g., "2025-11-01 09:29:47")

**New Code:**
```javascript
// FIXED CODE - CORRECT PATH & FIELD MAPPING
const queueRef = ref(db, 'queue'); // ✅ Correct path

Object.keys(queue).forEach(queueId => {
  const entry = queue[queueId];
  queueList.push({
    id: queueId,
    driverName: entry.driverName || 'Unknown Driver',
    driverId: entry.driverRFID || entry.driverId || 'N/A', // Map driverRFID
    todaNumber: entry.todaNumber || 'N/A',
    contributionPaid: entry.contributionPaid || false,
    status: entry.status || 'waiting',
    timestamp: entry.queueTime || entry.timestamp || Date.now(),
    ...entry
  });
});
```

**Visual Updates:**
- Added contribution status badges:
  - ✓ Paid (green badge)
  - ⚠ Not Paid (orange badge)
- Shows TODA number in driver details
- Displays queue status

---

### Issue 2: DiscountApplications - Completely Wrong Approach ❌

**Problem:**
- Code was looking for `discountApplication` field inside `users` node
- This field doesn't exist in ANY user object in the database
- There's a separate `pendingApplications` node that contains user IDs
- Applications would NEVER load because the field doesn't exist

**What Was Wrong:**
```javascript
// OLD CODE - LOOKING IN WRONG PLACE
const usersRef = ref(db, 'users');
Object.keys(users).forEach(userId => {
  const user = users[userId];
  if (user.discountApplication && user.discountApplication.status === 'pending') {
    // ❌ This condition NEVER matches because discountApplication doesn't exist!
    pendingApplications.push({...});
  }
});
```

**Actual Database Structure:**
```json
"pendingApplications": {
  "-OYiYCc2Xv9npmSEQ4I7": true,
  "-OYiZ9_W-NPY2X_q2q9c": true,
  "-OYi_WmNGS3E-MliKPzc": true
}

"users": {
  "-OYiYCc2Xv9npmSEQ4I7": {
    "fullName": "John Doe",
    "email": "john@example.com",
    "phoneNumber": "09123456789",
    // Note: NO discountApplication field here!
    // Discount data might be in user object directly or in a nested field
  }
}
```

**What Was Fixed:**
1. ✅ Read from `pendingApplications` node first
2. ✅ Get list of pending user IDs
3. ✅ Fetch user details from `users` node
4. ✅ Extract discount application data (handles multiple field locations)
5. ✅ Remove from `pendingApplications` when approved/rejected

**New Code:**
```javascript
// FIXED CODE - TWO-STEP PROCESS
// Step 1: Get pending application IDs
const pendingAppsRef = ref(db, 'pendingApplications');

onValue(pendingAppsRef, async (pendingSnapshot) => {
  if (pendingSnapshot.exists()) {
    const pendingIds = pendingSnapshot.val();
    const pendingUserIds = Object.keys(pendingIds).filter(id => pendingIds[id] === true);
    
    // Step 2: Fetch user details for each pending ID
    const usersRef = ref(db, 'users');
    onValue(usersRef, (usersSnapshot) => {
      const users = usersSnapshot.val();
      
      pendingUserIds.forEach(userId => {
        const user = users[userId];
        if (user) {
          const discountData = user.discountApplication || {};
          
          pendingApplications.push({
            userId: userId,
            userName: user.fullName || user.name || 'Unknown',
            userEmail: user.email || 'N/A',
            userPhone: user.phoneNumber || user.phone || 'N/A',
            discountType: discountData.discountType || user.discountType || 'N/A',
            timestamp: discountData.timestamp || Date.now(),
            documentURL: discountData.documentURL || user.discountDocumentURL || '',
            ...discountData
          });
        }
      });
    });
  }
});
```

**Approval/Rejection Updates:**
```javascript
// ALSO FIXED: Remove from pendingApplications after approval/rejection
const pendingAppRef = ref(db, `pendingApplications/${selectedApplication.userId}`);
await update(pendingAppRef, null); // Delete the entry
```

---

## 📊 Summary of Changes

### QueueManagement.js
| Change | Before | After |
|--------|--------|-------|
| **Database Path** | `driverQueue` ❌ | `queue` ✅ |
| **RFID Field** | `driverId` only | `driverRFID` or `driverId` ✅ |
| **Timestamp** | `timestamp` only | `queueTime` or `timestamp` ✅ |
| **Contribution Status** | Not shown | Badge shown ✅ |
| **TODA Number** | Not shown | Displayed ✅ |

### DiscountApplications.js
| Change | Before | After |
|--------|--------|-------|
| **Data Source** | `users` node only ❌ | `pendingApplications` + `users` ✅ |
| **Detection Method** | Check user.discountApplication ❌ | Check pendingApplications list ✅ |
| **Field Extraction** | Fixed field path | Multiple field paths ✅ |
| **After Approval** | Update user only | Update user + remove from pending ✅ |

---

## 🧪 Testing

### Test Queue Management:
1. Open `QueueManagement/QueueManagement.html`
2. Should see queue entry: "Lucas Abad"
3. Should show:
   - ✓ Paid badge (green)
   - TODA: 395
   - RFID: 2A8B5505
   - Correct timestamp

### Test Discount Applications:
1. Open `DiscountApplications/DiscountApplications.html`
2. Should see 3 pending applications (from pendingApplications node)
3. Approve or reject an application
4. Should be removed from pending list
5. Check Firebase - should be removed from `pendingApplications` node

---

## ⚠️ Important Notes

### For Mobile App Developers:

**Queue Data Structure:**
```javascript
// When adding to queue, use this structure:
{
  driverName: "Driver Name",
  driverRFID: "RFID_UID",           // Use driverRFID, not driverId
  todaNumber: "123",
  contributionPaid: true/false,
  status: "waiting",
  queueTime: Date.now().toString(), // Can be timestamp
  timestamp: "2025-11-01 09:29:47"  // Or formatted string
}
```

**Discount Application Process:**
```javascript
// Step 1: Add to pendingApplications
await set(ref(db, `pendingApplications/${userId}`), true);

// Step 2: Add discount data to user object
await update(ref(db, `users/${userId}`), {
  discountApplication: {
    discountType: "Senior",
    timestamp: Date.now(),
    documentURL: "url_to_document",
    idNumber: "1234567890",
    status: "pending"
  }
});
```

---

## 🎯 Why These Fixes Matter

1. **Queue Management** - Page was completely non-functional, showing empty state even with data
2. **Discount Applications** - Page was completely non-functional, never finding any applications
3. **Data Consistency** - Now matches actual database structure
4. **Better UX** - Shows contribution status and more details
5. **Proper Cleanup** - Removes applications from pending list after processing

---

## 📝 Files Modified

1. ✅ `QueueManagement/QueueManagement.js`
   - Changed database path
   - Added field mapping
   - Updated display logic

2. ✅ `QueueManagement/QueueManagement.css`
   - Added contribution badge styles

3. ✅ `DiscountApplications/DiscountApplications.js`
   - Complete rewrite of loadApplications()
   - Updated approve/reject functions
   - Added proper cleanup

All changes are backward compatible and handle missing fields gracefully with fallbacks.
