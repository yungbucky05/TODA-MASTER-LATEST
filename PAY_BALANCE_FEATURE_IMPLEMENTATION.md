# Pay Balance Feature Implementation

## Overview
This feature allows drivers to mark when they want to settle their outstanding balance through the Contributions History screen in the payment mode card. The payment will be handled by the hardware terminal.

## Database Structure

### New Field in `drivers` Table
```
drivers/
  {driverId}/
    pay_balance: boolean           // true = driver wants to pay, false = default
    pay_balance_timestamp: number  // timestamp when pay_balance was set
```

## Implementation Details

### 1. Firebase Service Layer
**File:** `FirebaseRealtimeDatabaseService.kt`

#### Methods Added:
- `observePayBalanceStatus(driverId: String): Flow<Boolean>`
  - Real-time observer for the `pay_balance` field
  - Returns a Flow that emits boolean values when the status changes
  
- `markPayBalance(driverId: String, wantsToPay: Boolean): Boolean`
  - Updates the `pay_balance` field in Firebase
  - Also records `pay_balance_timestamp` for tracking

### 2. Repository Layer
**File:** `TODARepository.kt`

#### Methods Added:
- `observePayBalanceStatus(driverId: String): Flow<Boolean>`
  - Wrapper for Firebase service method
  
- `markPayBalance(driverId: String, wantsToPay: Boolean): Result<Unit>`
  - Wrapper for Firebase service method with Result handling

### 3. ViewModel Layer
**File:** `EnhancedBookingViewModel.kt`

#### Methods Added:
- `observePayBalanceStatus(driverId: String): Flow<Boolean>`
  - Exposes the real-time observer to UI
  
- `markPayBalance(driverId: String, wantsToPay: Boolean): Result<Unit>`
  - Exposes the mark function to UI

### 4. UI Layer
**File:** `DriverContributionsScreen.kt`

#### Features:
1. **Real-time Status Observer**
   - Listens for changes to `pay_balance` status
   - Updates UI automatically when status changes

2. **Payment Mode and Balance Card**
   - Shows current payment mode (Pay Every Trip / Pay Later)
   - Displays current balance
   - Shows "I Want to Pay Balance" button when balance > 0 and pay_balance = false
   - Shows "Waiting for payment at terminal..." status when pay_balance = true

3. **UI States:**
   - **No Balance:** Card shows green background, no action button
   - **Has Balance + Not Marked:** Red background, shows "I Want to Pay Balance" button
   - **Has Balance + Marked:** Yellow background, shows waiting status with schedule icon

## User Flow

### Driver Side (App):
1. Driver navigates to Contributions screen
2. If driver has outstanding balance, sees the balance amount in red
3. When ready to pay, clicks "I Want to Pay Balance" button
4. Status changes to "Waiting for payment at terminal..."
5. Driver goes to the hardware terminal to settle payment

### Hardware Side (Terminal):
1. Terminal detects `pay_balance = true` for the driver
2. Allows driver to insert payment
3. After payment is processed:
   - Deducts amount from driver's balance
   - Sets `pay_balance = false` (payment complete)
   - Records the transaction

### App Update (Real-time):
1. When `pay_balance` is set to false by hardware
2. UI automatically updates to remove "waiting" status
3. Balance updates to reflect payment
4. Button becomes available again if balance still exists

## Key Features

✅ **Real-time Sync:** All changes are instantly reflected across app and hardware
✅ **No Manual Refresh:** Uses Firebase Flow observers for automatic updates
✅ **Clear Visual Feedback:** Different colors and icons for different states
✅ **Hardware Integration Ready:** Field is monitored by hardware system
✅ **Timestamp Tracking:** Records when driver marks for payment

## Testing Checklist

- [ ] Driver can see current balance
- [ ] "I Want to Pay" button appears when balance > 0
- [ ] Clicking button changes status to "Waiting for payment"
- [ ] Status updates in real-time when hardware processes payment
- [ ] Balance updates correctly after payment
- [ ] Multiple drivers can use feature simultaneously
- [ ] Status persists across app restarts

## Notes

- The actual payment processing is handled by the hardware terminal
- The `pay_balance` field is just a flag/signal for coordination
- Hardware should reset `pay_balance = false` after processing payment
- The app only allows drivers to mark their intention to pay
- All monetary transactions happen at the physical terminal

## Related Files

- `app/src/main/java/com/example/toda/service/FirebaseRealtimeDatabaseService.kt` (lines 1871-1928)
- `app/src/main/java/com/example/toda/repository/TODARepository.kt` (lines 1040-1055)
- `app/src/main/java/com/example/toda/viewmodel/EnhancedBookingViewModel.kt` (lines 477-484)
- `app/src/main/java/com/example/toda/ui/driver/DriverContributionsScreen.kt` (entire file)

