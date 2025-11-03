# Quick Setup Guide: Pay Later Mode

## Step 1: Update Firebase Database

Add these fields to existing drivers in Firebase:

```javascript
// For each driver in the database
{
  "paymentMode": "pay_every_trip",  // Default value
  "balance": 0.0,
  "lastPaymentDate": 0,
  "canGoOnline": true
}
```

## Step 2: Initialize Payment History Node

Create the `payment_history` node in Firebase Realtime Database:

```javascript
{
  "payment_history": {}
}
```

## Step 3: Test the Implementation

### Test 1: Verify Payment Service Integration
1. Complete a trip as a driver
2. Check the driver's record in Firebase
3. For "pay_later" mode: Balance should increase by 5.00
4. Check `payment_history` node for new entry

### Test 2: Test Balance Warning
1. Set a driver's balance to 10.00 manually
2. Login as that driver
3. Dashboard should show payment warning card
4. Balance card should display ₱10.00 in red

### Test 3: Test Online Restriction (Next Day)
1. Set driver to pay_later mode with balance > 0
2. Set `lastPaymentDate` to yesterday's timestamp
3. Try to go online (tap RFID at terminal)
4. Driver should be blocked (or warning shown)

## Step 4: Enable Payment Processing

### Option A: Manual Payment (Current Implementation)
Admins can manually update driver balance in Firebase when payment is received.

### Option B: Add Payment Button (Future Enhancement)
Drivers can pay through the app using the PaymentModeScreen.

## Database Setup Commands

### For New Drivers
When registering a new driver, initialize with:
```json
{
  "id": "driver_xxx",
  "name": "John Doe",
  "paymentMode": "pay_every_trip",
  "balance": 0.0,
  "lastPaymentDate": 0,
  "canGoOnline": true,
  ...other fields...
}
```

### For Existing Drivers (Migration)
Run this script in Firebase Console:

```javascript
// Get all drivers
firebase.database().ref('drivers').once('value', (snapshot) => {
  const updates = {};
  
  snapshot.forEach((child) => {
    const driverId = child.key;
    updates[`drivers/${driverId}/paymentMode`] = 'pay_every_trip';
    updates[`drivers/${driverId}/balance`] = 0.0;
    updates[`drivers/${driverId}/lastPaymentDate`] = 0;
    updates[`drivers/${driverId}/canGoOnline`] = true;
  });
  
  return firebase.database().ref().update(updates);
});
```

## Verification Checklist

- [x] DriverPaymentService.kt created
- [x] Driver model updated with payment fields
- [x] EnhancedBookingViewModel updated with payment service
- [x] DriverInterface displays balance and warnings
- [x] PaymentModeScreen created
- [x] Trip completion records contribution
- [x] Documentation created

## What Happens When Driver Completes a Trip

### Pay Every Trip Mode:
```
Trip Completed → Status = COMPLETED
  ↓
EnhancedBookingViewModel.updateBookingStatusOnly()
  ↓
paymentService.recordTripContribution()
  ↓
Record in payment_history (no balance change)
  ↓
Driver can continue working
```

### Pay Later Mode:
```
Trip Completed → Status = COMPLETED
  ↓
EnhancedBookingViewModel.updateBookingStatusOnly()
  ↓
paymentService.recordTripContribution()
  ↓
Add ₱5 to driver's balance
  ↓
Update lastPaymentDate
  ↓
Record in payment_history
  ↓
Driver can continue working (same day)
  ↓
Next day: Check balance before going online
  ↓
If balance > 0: BLOCK (must pay first)
```

## Admin Actions

### View Driver Balances
Navigate to Firebase Console → Realtime Database → drivers
Look for drivers with `balance > 0`

### Clear Driver Balance (After Payment Received)
```javascript
firebase.database().ref('drivers/DRIVER_ID').update({
  balance: 0,
  lastPaymentDate: Date.now(),
  canGoOnline: true
});
```

### View Payment History
Navigate to Firebase Console → Realtime Database → payment_history
Filter by `driverId` to see driver's contribution history

## Troubleshooting

### Issue: Balance not updating after trip
**Solution**: 
- Check that DriverPaymentService is properly injected in EnhancedBookingViewModel
- Verify Firebase write permissions
- Check logs for payment service errors

### Issue: Driver blocked from going online incorrectly
**Solution**:
- Verify `canGoOnline` field is true
- Check if balance is actually > 0
- Ensure lastPaymentDate is correct
- Manually update canGoOnline if needed

### Issue: Payment mode not changing
**Solution**:
- Verify valid values: "pay_later" or "pay_every_trip"
- Check Firebase security rules allow writing to paymentMode field
- Clear app cache and restart

## Next Steps

1. **Test with real drivers** in a controlled environment
2. **Monitor payment_history** to ensure contributions are being recorded
3. **Add payment gateway** for online payments (future)
4. **Create admin dashboard** to view all driver balances
5. **Add notifications** to remind drivers to pay before end of day

## Support

For issues or questions, check:
- PAY_LATER_MODE_IMPLEMENTATION.md (detailed documentation)
- Firebase Console logs
- Android Studio Logcat for debug messages

