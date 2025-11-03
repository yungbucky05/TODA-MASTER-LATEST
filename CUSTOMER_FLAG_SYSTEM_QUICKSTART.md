# Customer Flag System - Quick Reference

## 🚀 Quick Start

The customer flag system is **automatically active** in the passenger app. No configuration needed!

---

## 📊 Status Levels

| Status | Points | Icon | What It Means |
|--------|--------|------|---------------|
| **Good** | 0-50 | ✅ | Full access, no issues |
| **Monitored** | 51-150 | 👀 | Warning shown, full access |
| **Restricted** | 151-300 | ⚠️ | Limited features (future) |
| **Suspended** | 301+ | 🚫 | **Cannot book rides** |

---

## 🚩 Flag Types

| Flag | Points | Severity | What Triggers It |
|------|--------|----------|------------------|
| NO_SHOW | 100 | Critical | Didn't show up for booking |
| NON_PAYMENT | 100 | Critical | Unpaid ride |
| WRONG_PIN | 50 | Medium | Wrong pickup location |
| ABUSIVE_BEHAVIOR | 100 | Critical | Reported by driver |
| EXCESSIVE_CANCELLATIONS | 75 | High | Too many cancellations |

---

## 🎨 UI Components

### Where Customers See Flags

1. **Booking Screen** - Banner at top (if flagged)
2. **Flags Tab** - Complete flag summary and active issues
3. **Tab Badge** - Red badge showing number of active flags

### What Customers See

**Good Standing:**
```
Flags Tab:
🎉 No Active Issues!
You're a great passenger! Keep it up! 🌟
```

**Monitored (51-150 pts):**
```
Booking Screen:
👀 Account Under Monitoring
We've noticed some issues...
Flag Score: 75 pts • 1 active issue

Flags Tab:
[Full details of issues]
[How to improve]
```

**Suspended (301+ pts):**
```
Booking Screen:
[Tries to book]
→ Snackbar: "🚫 Your account is suspended"
→ Auto-redirected to Flags tab

Flags Tab:
🚫 Account Suspended
[Contact Support button]
```

---

## 💻 Key Code Locations

### UI Components
```
app/src/main/java/com/example/toda/ui/customer/CustomerFlagComponents.kt
```
- CustomerFlagStatusBanner
- CustomerFlagStatusBadge
- CustomerActiveFlagsList
- CustomerFlagItem
- CustomerBlockedAccessScreen

### Integration
```
app/src/main/java/com/example/toda/ui/customer/CustomerInterface.kt
```
- Real-time flag monitoring (lines ~120-145)
- Flags tab (view toggle buttons)
- Booking restriction check
- Status banner in booking view

### Data Layer
```
app/src/main/java/com/example/toda/service/FirebaseRealtimeDatabaseService.kt
```
- getUserFlagData() - Get flag score and status
- observeUserFlagData() - Real-time flag data updates
- getUserActiveFlags() - Get list of active flags
- observeUserFlags() - Real-time active flags updates

---

## 🔧 Quick Modifications

### Change Flag Thresholds
```kotlin
// In FirebaseRealtimeDatabaseService.kt - getUserFlagData()
val calculatedStatus = when {
    calculatedScore > 300 -> "suspended"  // ← Change these
    calculatedScore > 150 -> "restricted"
    calculatedScore > 50 -> "monitored"
    else -> "good"
}
```

### Add New Flag Type
```kotlin
// In CustomerFlagComponents.kt - getCustomerFlagTypeInfo()
"NEW_TYPE" -> CustomerFlagTypeInfo(
    icon = "🔴",
    title = "Title Here",
    explanation = "What happened...",
    actionItems = listOf(
        "How to fix 1",
        "How to fix 2"
    )
)
```

### Change Colors
```kotlin
// In CustomerFlagComponents.kt
FlagStatus.GOOD -> Color(0xFF4CAF50)      // Green
FlagStatus.MONITORED -> Color(0xFFFFA000) // Orange
FlagStatus.RESTRICTED -> Color(0xFFFF6F00) // Deep Orange
FlagStatus.SUSPENDED -> Color(0xFFD32F2F)  // Red
```

---

## 🧪 Testing Guide

### Test Scenarios

**1. Test Good Standing**
- Create customer with no flags
- Expected: No banner, "No Active Issues!" in Flags tab

**2. Test Monitored Status**
- Add flag worth 75 points (e.g., EXCESSIVE_CANCELLATIONS)
- Expected: Yellow banner appears, can still book

**3. Test Suspended Status**
- Add flags totaling 301+ points
- Expected: Cannot book, blocked access screen in Flags tab

**4. Test Real-time Updates**
- Have admin resolve a flag while app is open
- Expected: Banner disappears, score updates immediately

### Test Checklist
- [ ] No flags → "No Active Issues!" message
- [ ] 51-150 pts → Warning banner visible
- [ ] 301+ pts → Booking blocked
- [ ] Flags tab badge shows correct count
- [ ] Suspended customer redirected to Flags tab
- [ ] Flag explanations display correctly
- [ ] Contact Support button visible when suspended

---

## 📱 User Flow

### Normal Customer Journey
```
1. Open App
   ↓
2. See "Book Ride" screen (no banner)
   ↓
3. Check Flags tab → "No Active Issues! 🎉"
   ↓
4. Book rides normally
```

### Flagged Customer Journey
```
1. Open App
   ↓
2. See yellow/orange warning banner
   ↓
3. Check Flags tab → See active issues + how to improve
   ↓
4. Can still book (if not suspended)
   ↓
5. Improve behavior → Flags resolved by admin
   ↓
6. Banner disappears → Back to good standing
```

### Suspended Customer Journey
```
1. Open App
   ↓
2. Try to book a ride
   ↓
3. Snackbar: "🚫 Your account is suspended"
   ↓
4. Auto-redirect to Flags tab
   ↓
5. See full-screen blocked access message
   ↓
6. Click "Contact Support"
   ↓
7. [Support flow - to be implemented]
```

---

## 🔄 How It Works

### Real-time Monitoring
```kotlin
// Runs automatically when CustomerInterface loads
LaunchedEffect(user.id) {
    // Monitor flag data (score + status)
    viewModel.observeUserFlagData(user.id).collect { flagData ->
        flagScore = flagData.flagScore
        flagStatus = FlagStatus.fromString(flagData.flagStatus)
    }
}

LaunchedEffect(user.id) {
    // Monitor active flags
    viewModel.observeUserFlags(user.id).collect { flags ->
        activeFlags = flags
    }
}
```

### Booking Restriction
```kotlin
// Runs when customer clicks "Submit Booking"
fun submitBooking() {
    if (flagStatus == FlagStatus.SUSPENDED) {
        // Show error message
        snackbarHostState.showSnackbar("🚫 Account suspended")
        // Navigate to Flags tab
        currentView = "flags"
        return // Stop booking submission
    }
    // ... continue with normal booking
}
```

### Score Calculation
```kotlin
// Hybrid approach - calculates from active flags
1. Fetch all flags from userFlags/{userId}
2. Filter only active flags (status == "active")
3. Sum up the points
4. Calculate status based on total
5. Return calculated values (not stored values)
```

---

## 🆘 Troubleshooting

| Problem | Solution |
|---------|----------|
| Score doesn't match admin panel | Hybrid calculation recalculates from active flags - check for resolved but unsynced flags |
| Suspended customer can still book | Check Firebase: `users/{userId}.flagStatus` should be "suspended" (lowercase) |
| Flags tab is empty | Verify `userFlags/{userId}` exists in Firebase. Check console logs. |
| Banner doesn't appear | Ensure `flagStatus != FlagStatus.GOOD` and check enum conversion |
| Badge doesn't update | Check real-time observer connection. Restart app to reestablish listeners. |

---

## 📚 Related Documentation

- **Full Implementation Guide:** `CUSTOMER_FLAG_HANDLER_IMPLEMENTATION.md`
- **Mobile App Integration:** `MOBILE_APP_INTEGRATION.md`
- **Driver Flag System:** `DRIVER_FLAG_HANDLER_IMPLEMENTATION.md`
- **Admin Panel:** Flag management features in admin dashboard

---

## ✅ Quick Checklist

### For Developers
- [ ] CustomerFlagComponents.kt exists and compiles
- [ ] CustomerInterface.kt shows Flags tab
- [ ] Real-time observers are active
- [ ] Booking restriction works for suspended customers
- [ ] UI is user-friendly and compassionate

### For Testers
- [ ] Test all 4 status levels (good, monitored, restricted, suspended)
- [ ] Test booking restriction for suspended customers
- [ ] Test real-time updates when admin resolves flags
- [ ] Test UI on different screen sizes
- [ ] Test with multiple flags of different types

### For Product Owners
- [ ] Flag types match business requirements
- [ ] Point values are appropriate
- [ ] Messaging is customer-friendly
- [ ] Restrictions are reasonable
- [ ] Support flow is accessible

---

**Last Updated:** November 4, 2025  
**Status:** ✅ Production Ready

For detailed information, see `CUSTOMER_FLAG_HANDLER_IMPLEMENTATION.md`
