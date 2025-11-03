# Discount Application Rejection - Mobile App Integration Guide

## 📋 Overview
The admin website now has a comprehensive rejection system for discount applications with specific reasons. This guide explains how to integrate the rejection feedback into your mobile app so users can see why their discount was rejected and reapply.

**✨ NEW: When users resubmit after rejection, the admin automatically receives a notification in the notification bell!**

---

## 🔥 Firebase Database Structure

### User Node Structure
When a discount application is rejected, the following fields are updated in the user's node:

```javascript
users/
  {userId}/
    // Application Data (preserved after rejection)
    discountType: "PWD" | "Senior" | "Student"
    discountIdNumber: "1234567890"
    discountIdImageUrl: "https://..."
    discountExpiryDate: "2026-12-31"
    
    // Status Flags
    discountVerified: false           // Always false for rejected
    discountRejected: true             // NEW: Indicates rejection
    
    // Rejection Details (NEW)
    discountRejectionReasons: [        // Array of rejection reasons
      "Invalid or expired ID",
      "ID photo is unclear or unreadable",
      "Additional custom reason..."
    ]
    discountRejectedAt: 1730419200000  // Timestamp of rejection
    discountRejectedBy: "Admin"        // Who rejected it
    
    // Backup of Rejected Data (NEW)
    discountRejectionData: {           // Copy of rejected application
      discountType: "PWD",
      discountIdNumber: "1234567890",
      discountIdImageUrl: "https://...",
      discountExpiryDate: "2026-12-31"
    }
```

### Possible Rejection Reasons
The admin can select from these predefined reasons or add custom ones:

1. "Invalid or expired ID"
2. "ID photo is unclear or unreadable"
3. "ID does not match applicant information"
4. "Incomplete information provided"
5. "Duplicate application"
6. "Does not qualify for selected discount type"
7. Custom text entered by admin

---

## 📱 Mobile App Implementation

### 1. **Check Discount Status**

When a user opens their profile or discount section, check for rejection:

```dart
// Dart/Flutter Example
Future<DiscountStatus> checkDiscountStatus(String userId) async {
  final userRef = FirebaseDatabase.instance.ref('users/$userId');
  final snapshot = await userRef.get();
  
  if (snapshot.exists) {
    final userData = snapshot.value as Map;
    
    // Check if discount is rejected
    if (userData['discountRejected'] == true) {
      return DiscountStatus(
        status: 'rejected',
        reasons: List<String>.from(userData['discountRejectionReasons'] ?? []),
        rejectedAt: userData['discountRejectedAt'],
        previousData: userData['discountRejectionData']
      );
    }
    
    // Check if discount is verified
    if (userData['discountVerified'] == true) {
      return DiscountStatus(
        status: 'approved',
        discountType: userData['discountType'],
        // ... other fields
      );
    }
    
    // Check if application is pending
    if (userData['discountType'] != null && 
        userData['discountVerified'] != true) {
      return DiscountStatus(
        status: 'pending',
        discountType: userData['discountType']
      );
    }
    
    // No discount application
    return DiscountStatus(status: 'none');
  }
  
  return DiscountStatus(status: 'none');
}
```

```javascript
// React Native / JavaScript Example
async function checkDiscountStatus(userId) {
  const userRef = ref(db, `users/${userId}`);
  const snapshot = await get(userRef);
  
  if (snapshot.exists()) {
    const userData = snapshot.val();
    
    // Check if discount is rejected
    if (userData.discountRejected === true) {
      return {
        status: 'rejected',
        reasons: userData.discountRejectionReasons || [],
        rejectedAt: userData.discountRejectedAt,
        previousData: userData.discountRejectionData
      };
    }
    
    // Check if discount is verified
    if (userData.discountVerified === true) {
      return {
        status: 'approved',
        discountType: userData.discountType,
        // ... other fields
      };
    }
    
    // Check if application is pending
    if (userData.discountType && !userData.discountVerified) {
      return {
        status: 'pending',
        discountType: userData.discountType
      };
    }
  }
  
  return { status: 'none' };
}
```

---

### 2. **Display Rejection Message**

Show a clear, user-friendly message with the rejection reasons:

#### UI Design Suggestion:

```
┌─────────────────────────────────────┐
│  ❌ Discount Application Rejected   │
├─────────────────────────────────────┤
│                                     │
│  Your PWD discount application was  │
│  rejected for the following         │
│  reason(s):                         │
│                                     │
│  • Invalid or expired ID            │
│  • ID photo is unclear or          │
│    unreadable                       │
│                                     │
│  Rejected on: Nov 1, 2024          │
│                                     │
│  [View Details] [Apply Again]      │
└─────────────────────────────────────┘
```

#### Flutter Example:

```dart
Widget buildRejectionCard(DiscountStatus status) {
  return Card(
    color: Colors.red.shade50,
    child: Padding(
      padding: EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(Icons.cancel, color: Colors.red, size: 32),
              SizedBox(width: 12),
              Text(
                'Discount Application Rejected',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  color: Colors.red.shade900
                ),
              ),
            ],
          ),
          SizedBox(height: 16),
          Text(
            'Your ${status.previousData['discountType']} discount application was rejected for the following reason(s):',
            style: TextStyle(fontSize: 14, color: Colors.grey.shade700),
          ),
          SizedBox(height: 12),
          ...status.reasons.map((reason) => Padding(
            padding: EdgeInsets.only(bottom: 8),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('• ', style: TextStyle(fontSize: 16)),
                Expanded(
                  child: Text(
                    reason,
                    style: TextStyle(fontSize: 14, color: Colors.red.shade800),
                  ),
                ),
              ],
            ),
          )).toList(),
          SizedBox(height: 12),
          Text(
            'Rejected on: ${formatDate(status.rejectedAt)}',
            style: TextStyle(fontSize: 12, color: Colors.grey),
          ),
          SizedBox(height: 16),
          Row(
            children: [
              Expanded(
                child: OutlinedButton(
                  onPressed: () => showRejectionDetails(status),
                  child: Text('View Details'),
                ),
              ),
              SizedBox(width: 12),
              Expanded(
                child: ElevatedButton(
                  onPressed: () => reapplyForDiscount(),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.blue,
                  ),
                  child: Text('Apply Again'),
                ),
              ),
            ],
          ),
        ],
      ),
    ),
  );
}
```

---

### 3. **Allow Users to Reapply**

When a user clicks "Apply Again", clear the rejection flags and allow them to submit a new application:

**⚠️ IMPORTANT: Keep `discountRejectionData` field when clearing rejection! This allows the admin system to detect it's a resubmission and send appropriate notification.**

```dart
// Dart/Flutter Example
Future<void> reapplyForDiscount(String userId) async {
  final userRef = FirebaseDatabase.instance.ref('users/$userId');
  
  // Clear rejection flags and old application data
  // NOTE: We keep discountRejectionData so admin can detect this is a resubmission
  await userRef.update({
    'discountRejected': null,           // Remove rejection flag
    'discountRejectionReasons': null,   // Clear rejection reasons
    'discountRejectedAt': null,         // Clear rejection timestamp
    'discountRejectedBy': null,         // Clear who rejected
    // KEEP 'discountRejectionData' - don't delete it!
    
    // Clear old application data
    'discountType': null,
    'discountIdNumber': null,
    'discountIdImageUrl': null,
    'discountExpiryDate': null,
    'discountVerified': false,
    'discountApplicationDate': null,
  });
  
  // Navigate to discount application form
  Navigator.push(
    context,
    MaterialPageRoute(builder: (context) => DiscountApplicationPage()),
  );
}
```

```javascript
// React Native / JavaScript Example
async function reapplyForDiscount(userId) {
  const userRef = ref(db, `users/${userId}`);
  
  // Clear rejection flags and old application data
  // NOTE: We keep discountRejectionData so admin can detect this is a resubmission
  await update(userRef, {
    discountRejected: null,
    discountRejectionReasons: null,
    discountRejectedAt: null,
    discountRejectedBy: null,
    // KEEP discountRejectionData - don't delete it!
    
    // Clear old application data
    discountType: null,
    discountIdNumber: null,
    discountIdImageUrl: null,
    discountExpiryDate: null,
    discountVerified: false,
    discountApplicationDate: null,
  });
  
  // Navigate to discount application form
  navigation.navigate('DiscountApplication');
}
```

**💡 Why Keep `discountRejectionData`?**
- Admin system checks this field to detect resubmissions
- When found, admin gets a special "Resubmitted After Rejection" notification
- Helps admin prioritize reviewing resubmissions
- Gets automatically cleared when admin approves or rejects the new application
```

---

### 4. **Submit New Application**

When the user submits a new application, set the data as usual:

```dart
// Dart/Flutter Example
Future<void> submitDiscountApplication({
  required String userId,
  required String discountType,
  required String idNumber,
  required String imageUrl,
  required String expiryDate,
}) async {
  final userRef = FirebaseDatabase.instance.ref('users/$userId');
  
  await userRef.update({
    'discountType': discountType,           // "PWD", "Senior", or "Student"
    'discountIdNumber': idNumber,           // ID card number
    'discountIdImageUrl': imageUrl,         // Uploaded image URL
    'discountExpiryDate': expiryDate,       // Expiry date if applicable
    'discountApplicationDate': DateTime.now().millisecondsSinceEpoch,
    'discountVerified': false,              // Pending verification
    'discountRejected': false,              // Not rejected (new application)
  });
  
  // ✅ Admin will automatically receive notification:
  // - "New Discount Application" if first-time applicant
  // - "Discount Resubmitted After Rejection" if reapplying after rejection
  // Notification appears in admin dashboard's notification bell!
  
  // Show success message
  showDialog(
    context: context,
    builder: (context) => AlertDialog(
      title: Text('Application Submitted'),
      content: Text('Your discount application has been submitted for review. You will be notified once it is reviewed.'),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: Text('OK'),
        ),
      ],
    ),
  );
}
```

---

## 🔔 Automatic Admin Notifications

### How It Works

The admin website automatically creates notifications when:

1. **New Application** (First-time applicant)
   - Type: `DISCOUNT_APPLICATION`
   - Icon: 🎫
   - Message: "User Name has applied for PWD discount"

2. **Resubmission After Rejection** (User reapplies after being rejected)
   - Type: `DISCOUNT_RESUBMISSION`
   - Icon: 🔄
   - Message: "User Name has resubmitted PWD discount application after rejection"

### Detection Logic

The admin system detects resubmissions by checking if `discountRejectionData` exists:

```javascript
// In admin website:
if (user.discountType && !user.discountVerified && !user.discountRejected) {
  // Check if previously rejected
  const wasRejected = user.discountRejectionData?.discountType != null;
  
  if (wasRejected) {
    // Create "Resubmission" notification 🔄
  } else {
    // Create "New Application" notification 🎫
  }
}
```

**This is why it's important to keep `discountRejectionData` when users reapply!**

---

### 5. **Real-time Status Listener**

Listen for real-time updates to notify users when their application is approved or rejected:

```dart
// Dart/Flutter Example
StreamSubscription? discountStatusListener;

void startListeningToDiscountStatus(String userId) {
  final userRef = FirebaseDatabase.instance.ref('users/$userId');
  
  discountStatusListener = userRef.onValue.listen((event) {
    if (event.snapshot.exists) {
      final userData = event.snapshot.value as Map;
      
      // Check if just approved
      if (userData['discountVerified'] == true) {
        showNotification(
          title: '✅ Discount Approved!',
          body: 'Your ${userData['discountType']} discount has been approved.',
        );
      }
      
      // Check if just rejected
      if (userData['discountRejected'] == true) {
        final reasons = List<String>.from(userData['discountRejectionReasons'] ?? []);
        showNotification(
          title: '❌ Discount Rejected',
          body: 'Your discount application was rejected. Tap to see reasons.',
          data: {'reasons': reasons},
        );
      }
    }
  });
}

@override
void dispose() {
  discountStatusListener?.cancel();
  super.dispose();
}
```

---

## 🎯 User Flow Diagram

```
User submits discount application
         ↓
Admin reviews in admin website
         ↓
    ┌────┴────┐
    ↓         ↓
Approve   Reject (with reasons)
    ↓         ↓
    ↓    discountRejected = true
    ↓    discountRejectionReasons = [...]
    ↓         ↓
    ↓    User sees rejection in app
    ↓         ↓
    ↓    User clicks "Apply Again"
    ↓         ↓
    ↓    Clear rejection flags
    ↓         ↓
    ↓    Submit new application
    ↓         ↓
    └────→ Admin reviews again ←────┘
```

---

## 📊 Database Fields Summary

| Field | Type | Description | When Set |
|-------|------|-------------|----------|
| `discountType` | String | Type of discount (PWD/Senior/Student) | User submits |
| `discountIdNumber` | String | ID card number | User submits |
| `discountIdImageUrl` | String | Uploaded ID image URL | User submits |
| `discountExpiryDate` | String | ID expiry date | User submits |
| `discountApplicationDate` | Number | Timestamp of submission | User submits |
| `discountVerified` | Boolean | Approved status | Admin approves |
| `discountRejected` | Boolean | Rejected status | Admin rejects |
| `discountRejectionReasons` | Array | List of rejection reasons | Admin rejects |
| `discountRejectedAt` | Number | Rejection timestamp | Admin rejects |
| `discountRejectedBy` | String | Who rejected (usually "Admin") | Admin rejects |
| `discountRejectionData` | Object | Backup of rejected application | Admin rejects |

---

## ✅ Testing Checklist

### Admin Side:
- [ ] Can reject discount application
- [ ] Can select multiple rejection reasons
- [ ] Can add custom rejection reason
- [ ] Rejection saves all required fields to Firebase
- [ ] Rejected applications don't appear in pending list

### Mobile App Side:
- [ ] Can detect rejected status
- [ ] Shows rejection reasons clearly
- [ ] "Apply Again" button clears rejection flags
- [ ] Can submit new application after rejection
- [ ] Real-time listener detects rejection
- [ ] Push notification sent on rejection
- [ ] Push notification sent on approval

---

## 🔔 Push Notification Setup (Optional)

You can send push notifications to users when their discount is rejected:

```javascript
// Cloud Function Example (Firebase Functions)
exports.onDiscountRejected = functions.database
  .ref('/users/{userId}')
  .onUpdate(async (change, context) => {
    const before = change.before.val();
    const after = change.after.val();
    
    // Check if discount was just rejected
    if (!before.discountRejected && after.discountRejected) {
      const userId = context.params.userId;
      const reasons = after.discountRejectionReasons || [];
      
      // Get user's FCM token
      const userSnapshot = await admin.database()
        .ref(`users/${userId}/fcmToken`)
        .once('value');
      const fcmToken = userSnapshot.val();
      
      if (fcmToken) {
        // Send push notification
        await admin.messaging().send({
          token: fcmToken,
          notification: {
            title: '❌ Discount Application Rejected',
            body: `Your ${after.discountType} discount was rejected. Tap to see reasons.`,
          },
          data: {
            type: 'discount_rejected',
            reasons: JSON.stringify(reasons),
            userId: userId,
          },
        });
      }
    }
  });
```

---

## 🎨 UI/UX Best Practices

1. **Clear Communication**: Use friendly language to explain rejection
2. **Actionable Feedback**: Show specific reasons so users know what to fix
3. **Easy Reapplication**: Make "Apply Again" button prominent
4. **Help Resources**: Provide examples of valid IDs or requirements
5. **Status Badges**: Use color-coded badges (Red=Rejected, Yellow=Pending, Green=Approved)
6. **Timeline**: Show application history (submitted → rejected → resubmitted)

---

## 🐛 Common Issues & Solutions

### Issue 1: User sees old rejection after reapplying
**Solution**: Make sure to set `discountRejected: false` or `null` when submitting new application

### Issue 2: Multiple rejection reasons not showing
**Solution**: Check that `discountRejectionReasons` is an array, not a string

### Issue 3: Reapplication creates duplicate
**Solution**: Clear ALL old discount fields before allowing new submission

---

## 📞 Support

If you need help integrating this feature into your mobile app, please contact the development team or refer to the Firebase documentation for your specific platform (Flutter, React Native, Swift, Kotlin).
