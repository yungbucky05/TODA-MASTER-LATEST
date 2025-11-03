# 🎫 Discount Application Rejection - Quick Reference

## ✅ What Was Added to Admin Website

### 1. **Rejection Modal with Reasons**
- Multiple checkbox options for common rejection reasons
- Custom text field for additional notes
- Professional UI matching DriverManagement style

### 2. **Predefined Rejection Reasons**
1. Invalid or expired ID
2. ID photo is unclear or unreadable
3. ID does not match applicant information
4. Incomplete information provided
5. Duplicate application
6. Does not qualify for selected discount type
+ Custom text field for specific notes

### 3. **Updated Database Structure**
When admin rejects a discount, these fields are saved:
```javascript
{
  discountRejected: true,
  discountRejectionReasons: ["reason1", "reason2", ...],
  discountRejectedAt: timestamp,
  discountRejectedBy: "Admin",
  discountRejectionData: { /* backup of rejected data */ }
}
```

### 4. **Smart Filtering**
- Rejected applications no longer appear in pending list
- Only shows truly pending applications (not verified AND not rejected)

---

## 📱 Mobile App Integration - Essential Steps

### Step 1: Check Status
```javascript
// Read from Firebase: users/{userId}
if (user.discountRejected === true) {
  // Show rejection UI with reasons
  const reasons = user.discountRejectionReasons; // Array of strings
}
```

### Step 2: Display Rejection
Show user:
- ❌ Rejection status
- List of rejection reasons
- Rejected date
- "Apply Again" button

### Step 3: Allow Reapplication
When user clicks "Apply Again":
```javascript
// Clear these fields:
await update(userRef, {
  discountRejected: null,
  discountRejectionReasons: null,
  discountRejectedAt: null,
  discountRejectedBy: null,
  discountRejectionData: null,
  
  // Clear old application
  discountType: null,
  discountIdNumber: null,
  discountIdImageUrl: null,
  discountExpiryDate: null,
});
```

### Step 4: Submit New Application
User fills form again and submits normally.

---

## 🔥 Firebase Database Example

### Before Rejection:
```json
"users": {
  "user123": {
    "name": "Juan Dela Cruz",
    "discountType": "PWD",
    "discountIdNumber": "1234567890",
    "discountIdImageUrl": "https://...",
    "discountVerified": false
  }
}
```

### After Rejection:
```json
"users": {
  "user123": {
    "name": "Juan Dela Cruz",
    "discountType": "PWD",
    "discountIdNumber": "1234567890",
    "discountIdImageUrl": "https://...",
    "discountVerified": false,
    "discountRejected": true,
    "discountRejectionReasons": [
      "Invalid or expired ID",
      "ID photo is unclear or unreadable"
    ],
    "discountRejectedAt": 1730419200000,
    "discountRejectedBy": "Admin",
    "discountRejectionData": {
      "discountType": "PWD",
      "discountIdNumber": "1234567890",
      "discountIdImageUrl": "https://...",
      "discountExpiryDate": "2026-12-31"
    }
  }
}
```

---

## 🎨 Mobile App UI Mockup

```
┌──────────────────────────────────────┐
│  Profile > Discount Status           │
├──────────────────────────────────────┤
│                                      │
│  ┌────────────────────────────────┐ │
│  │ ❌ Application Rejected        │ │
│  │                                │ │
│  │ Your PWD discount application  │ │
│  │ was rejected:                  │ │
│  │                                │ │
│  │ • Invalid or expired ID        │ │
│  │ • ID photo is unclear          │ │
│  │                                │ │
│  │ Rejected: Nov 1, 2024          │ │
│  │                                │ │
│  │ [View Details] [Apply Again]   │ │
│  └────────────────────────────────┘ │
│                                      │
└──────────────────────────────────────┘
```

---

## ⚡ Quick Mobile App Code Snippets

### Flutter
```dart
// Check status
if (userData['discountRejected'] == true) {
  final reasons = List<String>.from(
    userData['discountRejectionReasons'] ?? []
  );
  showRejectionUI(reasons);
}

// Reapply
await FirebaseDatabase.instance
  .ref('users/$userId')
  .update({
    'discountRejected': null,
    'discountRejectionReasons': null,
    // ... clear other fields
  });
```

### React Native
```javascript
// Check status
if (userData.discountRejected === true) {
  const reasons = userData.discountRejectionReasons || [];
  showRejectionUI(reasons);
}

// Reapply
await update(ref(db, `users/${userId}`), {
  discountRejected: null,
  discountRejectionReasons: null,
  // ... clear other fields
});
```

---

## ✅ Testing Checklist

### Admin Side (Already Working):
- ✅ Reject button opens modal
- ✅ Can select multiple reasons
- ✅ Can add custom text
- ✅ Saves to Firebase correctly
- ✅ Rejected apps don't show in pending

### Mobile App Side (To Implement):
- [ ] Detect rejected status
- [ ] Display rejection reasons
- [ ] Show "Apply Again" button
- [ ] Clear rejection data on reapply
- [ ] Submit new application
- [ ] Show different status badges (Pending/Approved/Rejected)

---

## 📚 Full Documentation

See `DISCOUNT_REJECTION_INTEGRATION.md` for:
- Detailed code examples
- Complete Firebase structure
- UI/UX best practices
- Push notification setup
- Error handling
- Common issues & solutions

---

## 🚀 Next Steps

1. **Read** `DISCOUNT_REJECTION_INTEGRATION.md`
2. **Implement** status checking in mobile app
3. **Design** rejection UI
4. **Add** reapplication flow
5. **Test** end-to-end workflow
6. **Deploy** to production

---

## 💡 Key Points to Remember

1. **Rejected ≠ Deleted** - Application data is preserved for reference
2. **Clear Flags on Reapply** - Set rejection flags to null/false
3. **Array of Reasons** - Can have multiple rejection reasons
4. **User-Friendly Messages** - Be clear and helpful
5. **Allow Resubmission** - Users should always be able to try again

---

## 📞 Questions?

Contact: Development Team
Documentation: See markdown files in Admin Website folder
