# Payment Modes Database Structure Guide

## Overview

This document explains the complete Firebase Realtime Database structure for the **Payment Modes Integration** in the TODA system. The system supports two payment modes:

1. **Pay Every Trip** - Hardware-integrated mode (upfront payment)
2. **Pay Later** - Flexible mode (accumulate and pay later)

---

## Quick Reference

| Database Node | Purpose | Key Fields |
|--------------|---------|-----------|
| `drivers/` | Store driver payment preferences & balance | `paymentMode`, `balance`, `canGoOnline` |
| `bookings/` | Track payment mode used per trip | `driverPaymentMode`, `contributionPaid` |
| `contributions/` | Record all payment transactions | `type`, `paidUpfront`, `balanceAfter` |
| `payment_history/` | Driver-specific payment history (optional) | `runningBalance`, `type` |

---

## 1. DRIVERS NODE

**Path:** `drivers/{driverId}` or `drivers/{rfidUID}`

### Complete Structure

```json
{
  "rfidUID": "A1B2C3D4",
  "name": "Juan Dela Cruz",
  "licenseNumber": "A01-23-456789",
  "tricycleId": "tricycle001",
  "todaNumber": "TODA001-001",
  "isRegistered": true,
  "totalContributions": 150.00,
  "isActive": true,
  
  // ===== PAYMENT MODE FIELDS =====
  "paymentMode": "pay_later",
  "balance": 15.00,
  "lastPaymentDate": 1730678400000,
  "canGoOnline": true,
  "lastBalanceAccrualDate": 1730678400000
}
```

### Field Descriptions

| Field | Type | Values | Description |
|-------|------|--------|-------------|
| `paymentMode` | String | `"pay_every_trip"` or `"pay_later"` | Driver's selected payment mode |
| `balance` | Double | 0.00 to 9999.99 | Current unpaid balance in pesos |
| `lastPaymentDate` | Long | Timestamp | Last time driver paid contribution or balance |
| `canGoOnline` | Boolean | true/false | Whether driver can go online (computed field) |
| `lastBalanceAccrualDate` | Long | Timestamp | Last date when balance was increased |

### Payment Mode Behavior

#### Pay Every Trip Mode
```json
{
  "paymentMode": "pay_every_trip",
  "balance": 0.00,           // Always stays at 0
  "canGoOnline": true        // Always true (if paid at hardware)
}
```
- Driver pays ₱5 at hardware terminal **before** joining queue
- Balance never accumulates (always ₱0.00)
- No online restrictions

#### Pay Later Mode
```json
{
  "paymentMode": "pay_later",
  "balance": 15.00,          // Accumulates ₱5 per trip
  "canGoOnline": true,       // Becomes false next day if balance > 0
  "lastBalanceAccrualDate": 1730678400000
}
```
- Balance increases by ₱5 after each completed trip
- Can work same day with unpaid balance
- **Cannot go online next day** if balance > 0
- Must settle balance to continue working

### Logic for `canGoOnline`

```javascript
// Pseudo-code for computing canGoOnline
if (paymentMode === "pay_every_trip") {
    canGoOnline = true; // Always allowed
} else if (paymentMode === "pay_later") {
    if (balance === 0) {
        canGoOnline = true;
    } else {
        // Check if it's a new day since last balance accrual
        const isNewDay = isDateDifferent(currentDate, lastBalanceAccrualDate);
        canGoOnline = !isNewDay; // Can work same day, not next day
    }
}
```

---

## 2. BOOKINGS NODE

**Path:** `bookings/{bookingId}`

### Complete Structure

```json
{
  "bookingId": "booking_123",
  "customerId": "user456",
  "assignedDriverId": "driver_1759325281790_2222",
  "driverName": "Juan Dela Cruz",
  "status": "COMPLETED",
  "pickupLocation": "Barangay 177",
  "destination": "SM North",
  "fare": 50.00,
  "timestamp": 1730678400000,
  "completedAt": 1730679400000,
  
  // ===== PAYMENT MODE TRACKING FIELDS =====
  "driverPaymentMode": "pay_later",
  "contributionPaid": false,
  "contributionAmount": 5.00,
  "contributionRecordedAt": 1730679400000
}
```

### Field Descriptions

| Field | Type | Values | Description |
|-------|------|--------|-------------|
| `driverPaymentMode` | String | `"pay_every_trip"` or `"pay_later"` | Payment mode driver was using during this trip |
| `contributionPaid` | Boolean | true/false | Whether ₱5 was paid upfront (true) or added to balance (false) |
| `contributionAmount` | Double | 5.00 | Contribution amount for this trip |
| `contributionRecordedAt` | Long | Timestamp | When the contribution was recorded in the system |

### Examples

#### Pay Every Trip Booking
```json
{
  "bookingId": "booking_001",
  "driverPaymentMode": "pay_every_trip",
  "contributionPaid": true,        // Paid at hardware before trip
  "contributionAmount": 5.00
}
```

#### Pay Later Booking
```json
{
  "bookingId": "booking_002",
  "driverPaymentMode": "pay_later",
  "contributionPaid": false,       // Added to balance after trip
  "contributionAmount": 5.00
}
```

### Why Track This?

✅ **Audit Trail:** Know exactly which trips were paid upfront vs. added to balance  
✅ **Reporting:** Generate reports on payment mode usage  
✅ **Dispute Resolution:** Clear record of payment status per trip  
✅ **Analytics:** Track which payment mode is more popular

---

## 3. CONTRIBUTIONS NODE

**Path:** `contributions/{contributionId}`

This is your **main payment transaction ledger**. Every payment event is recorded here.

### Complete Structure

```json
{
  "contributionId": "1730678400000",
  "driverRFID": "A1B2C3D4",
  "driverName": "Juan Dela Cruz",
  "todaNumber": "TODA001-001",
  "amount": 5.00,
  "timestamp": 1730678400000,
  "date": "2024-11-03",
  
  // ===== PAYMENT MODE TRACKING FIELDS =====
  "type": "TRIP_CONTRIBUTION",
  "paymentMode": "pay_later",
  "bookingId": "booking_123",
  "paidUpfront": false,
  "balanceAfter": 15.00,
  "paymentMethod": "cash",
  "isBalanceSettlement": false
}
```

### Field Descriptions

| Field | Type | Values | Description |
|-------|------|--------|-------------|
| `type` | String | See below | Type of payment transaction |
| `paymentMode` | String | `"pay_every_trip"` or `"pay_later"` | Payment mode active during transaction |
| `bookingId` | String | Booking ID or null | Associated booking (null for balance settlements) |
| `paidUpfront` | Boolean | true/false | Was payment made before trip (hardware) or after (balance) |
| `balanceAfter` | Double | 0.00+ | Driver's balance after this transaction |
| `paymentMethod` | String | `"cash"`, `"hardware_coin"`, `"gcash"` | How payment was made |
| `isBalanceSettlement` | Boolean | true/false | Is this settling accumulated balance? |

### Transaction Types

| Type Value | Description | When Created | Example |
|------------|-------------|--------------|---------|
| `HARDWARE_PAYMENT` | Upfront payment at hardware terminal | Driver taps RFID + inserts ₱5 coin | Driver pays ₱5 before joining queue |
| `TRIP_CONTRIBUTION` | ₱5 added to balance after trip | Trip status changes to "COMPLETED" | Pay Later driver finishes trip |
| `BALANCE_PAYMENT` | Settling accumulated balance | Driver manually pays balance | Driver pays ₱15 accumulated balance |

### Examples

#### Example 1: Hardware Payment (Pay Every Trip)
```json
{
  "contributionId": "1730678400000",
  "driverRFID": "A1B2C3D4",
  "driverName": "Juan Dela Cruz",
  "amount": 5.00,
  "timestamp": 1730678400000,
  "date": "2024-11-03",
  
  "type": "HARDWARE_PAYMENT",
  "paymentMode": "pay_every_trip",
  "bookingId": null,              // No booking yet
  "paidUpfront": true,            // Paid BEFORE trip
  "balanceAfter": 0.00,           // Balance stays at 0
  "paymentMethod": "hardware_coin",
  "isBalanceSettlement": false
}
```

**Flow:**
1. Driver taps RFID at terminal
2. Inserts ₱5 coin
3. This record created
4. Driver added to queue
5. Balance remains ₱0.00

---

#### Example 2: Trip Contribution (Pay Later)
```json
{
  "contributionId": "1730679400000",
  "driverRFID": "A1B2C3D4",
  "driverName": "Juan Dela Cruz",
  "amount": 5.00,
  "timestamp": 1730679400000,
  "date": "2024-11-03",
  
  "type": "TRIP_CONTRIBUTION",
  "paymentMode": "pay_later",
  "bookingId": "booking_123",     // Associated with trip
  "paidUpfront": false,           // Added to balance
  "balanceAfter": 5.00,           // Balance increased
  "paymentMethod": null,          // Not paid yet
  "isBalanceSettlement": false
}
```

**Flow:**
1. Driver completes trip (booking_123)
2. App marks booking as "COMPLETED"
3. This record created
4. Driver's balance updated: ₱0 → ₱5
5. Driver can continue working (same day)

---

#### Example 3: Balance Settlement (Pay Later)
```json
{
  "contributionId": "1730780000000",
  "driverRFID": "A1B2C3D4",
  "driverName": "Juan Dela Cruz",
  "amount": 15.00,
  "timestamp": 1730780000000,
  "date": "2024-11-03",
  
  "type": "BALANCE_PAYMENT",
  "paymentMode": "pay_later",
  "bookingId": null,              // Not tied to specific trip
  "paidUpfront": false,
  "balanceAfter": 0.00,           // Balance cleared
  "paymentMethod": "cash",        // Or gcash, etc.
  "isBalanceSettlement": true     // This is a settlement
}
```

**Flow:**
1. Driver has accumulated ₱15 balance (3 trips)
2. Driver pays ₱15 cash to admin/operator
3. This record created
4. Driver's balance updated: ₱15 → ₱0
5. Driver can go online again next day

---

## 4. PAYMENT_HISTORY NODE (Optional)

**Path:** `payment_history/{driverId}/{entryId}`

This is an **optional denormalized node** for faster driver-specific queries.

### Complete Structure

```json
{
  "entryId": "ph_1730678400000",
  "driverId": "driver_1759325281790_2222",
  "amount": 5.00,
  "type": "TRIP_CONTRIBUTION",
  "bookingId": "booking_123",
  "timestamp": 1730678400000,
  "runningBalance": 15.00,
  "paymentMode": "pay_later",
  "description": "Trip contribution - Booking #123"
}
```

### Benefits

✅ **Faster Queries:** Get driver history without scanning all contributions  
✅ **Better Performance:** Organized by driver for quick lookups  
✅ **Easy Display:** Perfect for driver app "Payment History" screen  
✅ **Running Balance:** Shows balance progression over time

### Example Driver History

```json
payment_history/driver_1759325281790_2222/
  ph_1730678400000: {
    "amount": 5.00,
    "type": "TRIP_CONTRIBUTION",
    "runningBalance": 5.00,
    "description": "Trip #1 - Pay Later"
  }
  ph_1730679400000: {
    "amount": 5.00,
    "type": "TRIP_CONTRIBUTION",
    "runningBalance": 10.00,
    "description": "Trip #2 - Pay Later"
  }
  ph_1730680400000: {
    "amount": 5.00,
    "type": "TRIP_CONTRIBUTION",
    "runningBalance": 15.00,
    "description": "Trip #3 - Pay Later"
  }
  ph_1730780000000: {
    "amount": -15.00,           // Negative = payment made
    "type": "BALANCE_PAYMENT",
    "runningBalance": 0.00,
    "description": "Balance settlement - Cash payment"
  }
```

---

## Hardware Integration Flow

### ESP32 Current Implementation

Your ESP32 hardware (`esp.ino`) currently has this function:

```cpp
void recordContribution() {
    String timestamp = String(time(nullptr));
    String contributionPath = "/contributions/" + timestamp;

    FirebaseJson contributionJson;
    contributionJson.set("driverRFID", currentDriver.rfidUID);
    contributionJson.set("driverName", currentDriver.driverName);
    contributionJson.set("todaNumber", currentDriver.todaNumber);
    contributionJson.set("amount", 5);
    contributionJson.set("timestamp", timestamp);
    contributionJson.set("date", getCurrentDate());

    Firebase.RTDB.setJSON(&fbdo, contributionPath, &contributionJson);
}
```

### Enhanced Hardware Implementation

To support payment modes, enhance the function:

```cpp
void recordContribution() {
    String timestamp = String(time(nullptr));
    String contributionPath = "/contributions/" + timestamp;

    FirebaseJson contributionJson;
    contributionJson.set("driverRFID", currentDriver.rfidUID);
    contributionJson.set("driverName", currentDriver.driverName);
    contributionJson.set("todaNumber", currentDriver.todaNumber);
    contributionJson.set("amount", 5);
    contributionJson.set("timestamp", timestamp);
    contributionJson.set("date", getCurrentDate());
    
    // NEW FIELDS FOR PAYMENT MODE TRACKING
    contributionJson.set("type", "HARDWARE_PAYMENT");
    contributionJson.set("paymentMode", "pay_every_trip");
    contributionJson.set("paidUpfront", true);
    contributionJson.set("balanceAfter", 0.00);
    contributionJson.set("paymentMethod", "hardware_coin");
    contributionJson.set("isBalanceSettlement", false);
    contributionJson.set("bookingId", "");

    Firebase.RTDB.setJSON(&fbdo, contributionPath, &contributionJson);
}
```

---

## Complete Workflow Examples

### Scenario 1: Hardware Driver (Pay Every Trip)

**Initial State:**
```json
drivers/A1B2C3D4: {
  "paymentMode": "pay_every_trip",
  "balance": 0.00,
  "canGoOnline": true
}
```

**Step 1:** Driver taps RFID at terminal
```
ESP32 checks payment mode → "pay_every_trip"
Terminal prompts: "Insert ₱5 coin"
```

**Step 2:** Driver inserts ₱5 coin
```json
contributions/1730678400000: {
  "type": "HARDWARE_PAYMENT",
  "amount": 5.00,
  "paidUpfront": true,
  "balanceAfter": 0.00
}
```

**Step 3:** Driver added to queue
```json
driverQueue/1730678400000: {
  "driverId": "A1B2C3D4",
  "status": "waiting"
}
```

**Step 4:** Driver completes trip
```json
bookings/booking_123: {
  "status": "COMPLETED",
  "driverPaymentMode": "pay_every_trip",
  "contributionPaid": true
}

drivers/A1B2C3D4: {
  "balance": 0.00  // Stays at 0
}
```

---

### Scenario 2: App Driver (Pay Later - Same Day)

**Initial State:**
```json
drivers/driver_001: {
  "paymentMode": "pay_later",
  "balance": 0.00,
  "canGoOnline": true,
  "lastBalanceAccrualDate": 0
}
```

**Step 1:** Driver joins queue via app (8:00 AM)
```
No upfront payment required
Driver status: "waiting"
```

**Step 2:** Driver completes Trip #1 (9:00 AM)
```json
bookings/booking_001: {
  "status": "COMPLETED",
  "driverPaymentMode": "pay_later",
  "contributionPaid": false
}

contributions/1730678400000: {
  "type": "TRIP_CONTRIBUTION",
  "amount": 5.00,
  "paidUpfront": false,
  "balanceAfter": 5.00,
  "bookingId": "booking_001"
}

drivers/driver_001: {
  "balance": 5.00,
  "lastBalanceAccrualDate": 1730678400000,
  "canGoOnline": true  // Still same day
}
```

**Step 3:** Driver completes Trip #2 (11:00 AM)
```json
drivers/driver_001: {
  "balance": 10.00,
  "canGoOnline": true  // Still same day
}
```

**Step 4:** Driver completes Trip #3 (2:00 PM)
```json
drivers/driver_001: {
  "balance": 15.00,
  "canGoOnline": true  // Still same day
}
```

**Step 5:** Driver finishes work (5:00 PM)
```
Total trips today: 3
Total balance: ₱15.00
Can still work today: YES
```

---

### Scenario 3: Pay Later - Next Day Block

**Current State (End of Nov 3):**
```json
drivers/driver_001: {
  "balance": 15.00,
  "lastBalanceAccrualDate": 1730678400000,  // Nov 3, 2024
  "canGoOnline": true
}
```

**Next Day (Nov 4, 8:00 AM):**

Driver tries to go online via app:

```javascript
// App checks
const currentDate = 1730764800000;  // Nov 4
const lastAccrualDate = 1730678400000;  // Nov 3
const balance = 15.00;

const isNewDay = !isSameDay(currentDate, lastAccrualDate);  // true
const canGoOnline = !(isNewDay && balance > 0);  // false

// Update driver
drivers/driver_001: {
  "balance": 15.00,
  "canGoOnline": false  // BLOCKED
}
```

**App displays:**
```
⚠️ You cannot go online
Balance due: ₱15.00
Please settle your balance to continue working.
```

**Step: Driver Settles Balance**
```json
contributions/1730764900000: {
  "type": "BALANCE_PAYMENT",
  "amount": 15.00,
  "isBalanceSettlement": true,
  "balanceAfter": 0.00,
  "paymentMethod": "cash"
}

drivers/driver_001: {
  "balance": 0.00,
  "lastPaymentDate": 1730764900000,
  "canGoOnline": true  // NOW CAN WORK
}
```

---

## Database Queries Reference

### Query 1: Get Driver's Current Balance
```javascript
const driverRef = firebase.database().ref(`drivers/${driverId}`);
driverRef.once('value', (snapshot) => {
  const balance = snapshot.val().balance || 0;
  const canGoOnline = snapshot.val().canGoOnline;
});
```

### Query 2: Get Driver's Payment History
```javascript
const historyRef = firebase.database()
  .ref('contributions')
  .orderByChild('driverRFID')
  .equalTo(driverId);

historyRef.once('value', (snapshot) => {
  snapshot.forEach((child) => {
    const contribution = child.val();
    // Display contribution
  });
});
```

### Query 3: Get All Pay Later Drivers with Balance
```javascript
const driversRef = firebase.database()
  .ref('drivers')
  .orderByChild('paymentMode')
  .equalTo('pay_later');

driversRef.once('value', (snapshot) => {
  snapshot.forEach((child) => {
    const driver = child.val();
    if (driver.balance > 0) {
      // Driver has unpaid balance
    }
  });
});
```

### Query 4: Calculate Total Unpaid Balance (Admin)
```javascript
const driversRef = firebase.database().ref('drivers');

let totalUnpaid = 0;
driversRef.once('value', (snapshot) => {
  snapshot.forEach((child) => {
    const driver = child.val();
    totalUnpaid += driver.balance || 0;
  });
  console.log(`Total unpaid balance: ₱${totalUnpaid}`);
});
```

### Query 5: Get Today's Trip Contributions
```javascript
const todayStart = new Date().setHours(0, 0, 0, 0);
const contributionsRef = firebase.database()
  .ref('contributions')
  .orderByChild('timestamp')
  .startAt(todayStart);

contributionsRef.once('value', (snapshot) => {
  snapshot.forEach((child) => {
    const contribution = child.val();
    if (contribution.type === 'TRIP_CONTRIBUTION') {
      // Process trip contribution
    }
  });
});
```

---

## Security Rules

Add these Firebase security rules to protect payment data:

```json
{
  "rules": {
    "drivers": {
      "$driverId": {
        ".read": "$driverId === auth.uid || root.child('users/' + auth.uid + '/userType').val() === 'OPERATOR'",
        ".write": "$driverId === auth.uid || root.child('users/' + auth.uid + '/userType').val() === 'OPERATOR'",
        
        "balance": {
          ".validate": "newData.isNumber() && newData.val() >= 0"
        },
        "paymentMode": {
          ".validate": "newData.val() === 'pay_every_trip' || newData.val() === 'pay_later'"
        }
      }
    },
    
    "contributions": {
      ".read": "root.child('users/' + auth.uid + '/userType').val() === 'OPERATOR' || root.child('users/' + auth.uid + '/userType').val() === 'DRIVER'",
      "$contributionId": {
        ".write": "auth != null",
        "amount": {
          ".validate": "newData.isNumber() && newData.val() > 0"
        },
        "type": {
          ".validate": "newData.val() === 'HARDWARE_PAYMENT' || newData.val() === 'TRIP_CONTRIBUTION' || newData.val() === 'BALANCE_PAYMENT'"
        }
      }
    },
    
    "bookings": {
      "$bookingId": {
        ".read": "data.child('customerId').val() === auth.uid || data.child('assignedDriverId').val() === auth.uid || root.child('users/' + auth.uid + '/userType').val() === 'OPERATOR'",
        ".write": "data.child('customerId').val() === auth.uid || data.child('assignedDriverId').val() === auth.uid || root.child('users/' + auth.uid + '/userType').val() === 'OPERATOR'"
      }
    },
    
    "payment_history": {
      "$driverId": {
        ".read": "$driverId === auth.uid || root.child('users/' + auth.uid + '/userType').val() === 'OPERATOR'",
        ".write": "root.child('users/' + auth.uid + '/userType').val() === 'OPERATOR' || $driverId === auth.uid"
      }
    }
  }
}
```

---

## Testing Checklist

### ✅ Pay Every Trip Mode
- [ ] Driver taps RFID at hardware terminal
- [ ] Terminal prompts for ₱5 coin
- [ ] Contribution recorded with `type: "HARDWARE_PAYMENT"`
- [ ] Driver balance stays at ₱0.00
- [ ] Driver added to queue
- [ ] Driver can complete trip
- [ ] Booking marked with `contributionPaid: true`

### ✅ Pay Later Mode - Same Day
- [ ] Driver joins queue without payment
- [ ] Driver completes trip
- [ ] Contribution recorded with `type: "TRIP_CONTRIBUTION"`
- [ ] Driver balance increases by ₱5.00
- [ ] Driver can still go online (same day)
- [ ] Booking marked with `contributionPaid: false`

### ✅ Pay Later Mode - Next Day Block
- [ ] Driver has unpaid balance from previous day
- [ ] Driver tries to go online next day
- [ ] System blocks driver (`canGoOnline: false`)
- [ ] App shows balance warning
- [ ] Driver cannot join queue

### ✅ Balance Settlement
- [ ] Driver pays accumulated balance
- [ ] Contribution recorded with `type: "BALANCE_PAYMENT"`
- [ ] Driver balance resets to ₱0.00
- [ ] Driver can go online again
- [ ] Payment recorded with `isBalanceSettlement: true`

---

## FAQs

### Q: What happens if a driver switches from Pay Later to Pay Every Trip with unpaid balance?

**A:** The driver must settle their existing balance before switching modes. The system should:
1. Check current balance
2. If balance > 0, prevent mode switch and prompt payment
3. Once balance = 0, allow mode change

### Q: Can a driver work multiple days in Pay Later mode?

**A:** No. The system enforces daily settlement:
- **Day 1:** Work and accumulate balance ✅
- **Day 2:** Cannot go online until balance is paid ❌
- **After payment:** Can work again ✅

### Q: What if hardware is offline and driver needs to join queue?

**A:** Hardware drivers (Pay Every Trip) can:
1. Temporarily switch to Pay Later mode in app
2. Join queue via app
3. Their contribution will be added to balance
4. Must settle balance before switching back to Pay Every Trip

### Q: How do operators track total unpaid balance?

**A:** Query all drivers with `paymentMode: "pay_later"` and sum their `balance` fields. See "Query 4" above.

### Q: What happens to balance when trip is cancelled?

**A:** Contribution is only recorded when trip status = "COMPLETED". Cancelled trips do not affect balance.

---

## Summary

This database structure provides:

✅ **Flexible Payment Modes:** Support for both upfront and deferred payment  
✅ **Hardware Compatibility:** Works with existing ESP32 terminal  
✅ **Balance Tracking:** Real-time driver balance monitoring  
✅ **Audit Trail:** Complete history of all payment transactions  
✅ **Business Rules:** Automatic enforcement of payment policies  
✅ **Reporting:** Easy queries for analytics and admin dashboards

The key innovation is the `type` field in contributions that distinguishes between:
- **HARDWARE_PAYMENT:** Upfront payment at terminal
- **TRIP_CONTRIBUTION:** Balance accrual after trip
- **BALANCE_PAYMENT:** Settlement of accumulated balance

This maintains backward compatibility with your hardware while adding flexible payment options for app-based drivers.

---

**Document Version:** 1.0  
**Last Updated:** November 3, 2024  
**Author:** TODA System Development Team

