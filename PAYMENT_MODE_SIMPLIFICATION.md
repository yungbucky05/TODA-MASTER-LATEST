# Payment Mode Simplification - Hardware-First Approach

## Summary of Changes

The payment mode system has been simplified to focus **only** on what the app handles, while the hardware system manages all online/offline logic.

## What the App Handles NOW:

### 1. **Balance Display** ✅
- Shows current driver balance in real-time
- Updates automatically when trips are completed
- Syncs across Dashboard and Contributions screens

### 2. **Payment Mode Switching** ✅
- Allows drivers to switch between:
  - **Pay Every Trip**: P5 paid upfront via hardware before each trip
  - **Pay Later**: P5 accumulates after each trip completion
- Changes sync instantly across all screens

### 3. **Payment History** ✅
- Records all trip contributions (P5 per trip)
- Tracks payment history for reporting
- Distinguishes between:
  - `TRIP_CONTRIBUTION`: Pay Later mode (P5 added to balance)
  - `TRIP_CONTRIBUTION_PREPAID`: Pay Every Trip (P5 already paid via hardware)

## What the Hardware Handles:

### 1. **Online/Offline Status** (Hardware Only)
- RFID tap at terminal → Driver goes online
- Remove RFID or leave queue → Driver goes offline
- App displays status but CANNOT control it

### 2. **Payment Collection** (Hardware Only)
- **Pay Every Trip**: Coin/payment accepted at terminal before RFID tap
- **Pay Later**: No upfront payment required, balance tracked in app

### 3. **Queue Management** (Hardware Only)
- Driver joins queue via hardware terminal
- Driver position managed by hardware
- App only displays queue status

## App Implementation:

### Balance Tracking:
```
- Pay Every Trip: Balance always stays at 0 (payment is upfront)
- Pay Later: Balance = ₱5 × number of completed trips
```

### Trip Completion Flow:
```
1. Driver completes trip → Status changes to "COMPLETED"
2. App checks payment mode:
   - Pay Every Trip: Record in history only (no balance change)
   - Pay Later: Add ₱5 to balance + record in history
3. Real-time observers update UI instantly
```

### What Was Removed:
- ❌ `canGoOnline()` logic - Hardware manages this
- ❌ App-based payment verification - Hardware handles this
- ❌ Balance-based online restrictions - Hardware controls access
- ❌ Day-based payment validation - Simplified to hardware management

### What Remains:
- ✅ Balance display and tracking
- ✅ Payment mode selection
- ✅ Payment history recording
- ✅ Real-time sync across screens
- ✅ Trip contribution calculation (₱5 per trip)

## Data Flow:

### When Driver Switches Payment Mode:
```
Contributions Screen → updatePaymentMode() → Firebase
                                            ↓
Firebase Real-time Observer → Dashboard (updates instantly)
                            → Contributions (stays in sync)
```

### When Driver Completes Trip:
```
Complete Button → updateBookingStatus("COMPLETED") → Firebase
                                                    ↓
                  recordTripContribution() checks payment mode:
                    - Pay Later: balance += 5.0
                    - Pay Every Trip: balance stays 0
                                                    ↓
                  Real-time observer → UI updates instantly
```

## Firebase Structure:

```json
{
  "drivers": {
    "driverId": {
      "balance": 0.0,              // Current outstanding balance
      "paymentMode": "pay_later",  // or "pay_every_trip"
      "lastPaymentDate": 1234567890 // Last payment timestamp
    }
  },
  "payment_history": {
    "historyId": {
      "driverId": "xxx",
      "bookingId": "yyy",
      "amount": 5.0,
      "type": "TRIP_CONTRIBUTION", // or "TRIP_CONTRIBUTION_PREPAID"
      "timestamp": 1234567890
    }
  }
}
```

## Key Points:

1. **Hardware is the source of truth** for online/offline status
2. **App only displays and tracks** payment information
3. **No app-based restrictions** on going online (hardware manages this)
4. **Balance is informational only** - doesn't prevent operations
5. **Payment history** is for record-keeping and reporting

This approach ensures clean separation between hardware (operational control) and app (information display and tracking).

