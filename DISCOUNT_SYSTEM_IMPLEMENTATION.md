# Discount System Implementation

## Overview
This document describes the implementation of the discount system for PWD (Persons with Disability), Senior Citizens, and Students in the TODA application.

## Features Implemented

### 1. Discount Types
Three discount categories are now supported:
- **PWD (Person with Disability)**: 20% discount
- **Senior Citizen**: 20% discount
- **Student**: 10% discount

### 2. User Interface Changes

#### Customer Dashboard Screen
- Added a new "Apply for Discount" button positioned above the Recent Bookings section
- Button changes to "Manage Discount" when a discount is already applied
- Shows discount type in the button label for easy identification
- Clicking the button opens a category selection dialog

#### Discount Category Selection Dialog
- Clean, modern dialog showing three discount categories
- Each category displayed as a clickable card with:
  - Category icon (Accessible, Elderly, School)
  - Category name
  - Discount percentage
  - Right arrow indicator
- Tapping a category navigates to the application form

#### Discount Application Screen (New Separate Page)
- **Full-screen form** for entering discount details
- Top app bar with back navigation
- Header card showing selected discount category and percentage
- Required form fields:
  - Full Name
  - ID Number (specific to discount type)
  - Date of Birth
  - Complete Address
  - Contact Number
- Information card showing required documents for verification
- Submit button (disabled until required fields are filled)
- Terms and conditions text
- Success dialog upon submission

#### Customer Information Card
- Displays active discount information with visual indicators
- Shows discount type, percentage, and ID number
- Color-coded status:
  - **Green (Primary Container)**: Verified discount
  - **Yellow (Secondary Container)**: Pending verification
- Icons indicate verification status:
  - ✓ Checkmark: Verified
  - ⏱ Clock: Pending verification

### 3. Data Model Changes

#### Models.kt
Added to `UserProfile` data class:
```kotlin
// Discount eligibility
val discountType: DiscountType? = null,
val discountIdNumber: String = "",
val discountVerified: Boolean = false,
val discountExpiryDate: Long? = null
```

Added new enum:
```kotlin
enum class DiscountType(val displayName: String, val discountPercent: Double) {
    PWD("Person with Disability", 20.0),
    SENIOR_CITIZEN("Senior Citizen", 20.0),
    STUDENT("Student", 10.0)
}
```

#### FirebaseModels.kt
Added to `FirebaseUserProfile` data class:
```kotlin
// Discount eligibility
val discountType: String? = null, // PWD, SENIOR_CITIZEN, STUDENT
val discountIdNumber: String = "",
val discountVerified: Boolean = false,
val discountExpiryDate: Long? = null
```

### 4. ViewModel Changes

#### CustomerDashboardViewModel.kt
Added new function:
```kotlin
fun applyForDiscount(discountType: DiscountType, idNumber: String)
```

This function:
- Validates user profile and userId
- Creates updated profile with discount information
- Sets `discountVerified` to false (requires operator verification)
- Updates the profile in Firebase
- Shows success message to user

### 5. Repository Changes

#### TODARepository.kt
Updated two methods to handle discount fields:

**`updateUserProfile`**:
- Now includes discount fields when saving to Firebase
- Converts DiscountType enum to String for storage

**`getUserProfile`**:
- Retrieves discount fields from Firebase
- Converts String back to DiscountType enum

## User Flow

### Applying for Discount (Two-Step Process)

**Step 1: Category Selection**
1. Customer opens dashboard
2. Clicks "Apply for Discount" button
3. Dialog appears showing three discount category cards:
   - Person with Disability (PWD) - 20% discount
   - Senior Citizen - 20% discount
   - Student - 10% discount
4. Customer taps on their applicable category

**Step 2: Application Form**
5. User is navigated to a full-screen application form
6. Form shows the selected category at the top with icon
7. Customer fills in required information:
   - Full Name
   - ID Number (e.g., "PWD ID Number" for PWD category)
   - Date of Birth
   - Complete Address
   - Contact Number
8. Form displays required documents information
9. Customer reviews terms and conditions
10. Customer clicks "Submit Application" button
11. Success dialog appears confirming submission
12. System saves discount information with `discountVerified = false`
13. User is returned to dashboard
14. Discount card appears in Customer Information section with "Pending Verification" status

### Updating Discount
1. Customer clicks "Manage Discount" button (shows current discount in label)
2. Category selection dialog appears
3. Customer can select a different category or re-select current one
4. Application form appears with empty fields
5. Customer fills in updated information
6. Clicking "Submit Application" saves changes
7. Verification status may reset based on operator requirements

## Navigation Changes

The discount system now requires navigation support in your app's navigation graph:

```kotlin
// Add this route to your navigation
composable(
    route = "discount_application/{category}",
    arguments = listOf(navArgument("category") { type = NavType.StringType })
) { backStackEntry ->
    val category = backStackEntry.arguments?.getString("category") ?: "PWD"
    DiscountApplicationScreen(
        userId = currentUserId,
        discountCategory = category,
        onNavigateBack = { navController.popBackStack() }
    )
}

// Update CustomerDashboardScreen call
CustomerDashboardScreen(
    userId = currentUserId,
    onBookRide = { navController.navigate("customer_booking") },
    onViewProfile = { navController.navigate("profile") },
    onLogout = { /* logout logic */ },
    onApplyDiscount = { category ->
        navController.navigate("discount_application/$category")
    }
)
```

## Verification Process (To Be Implemented)

The discount system includes a verification flag that operators can use to validate discounts. Here's what needs to be added next:

### Operator Interface (Future Enhancement)
1. **Pending Verifications List**: Operators should see a list of customers awaiting discount verification
2. **Verification Dialog**: Operators can view customer details and approve/reject discount
3. **ID Verification**: During first ride, operators can verify physical ID
4. **Expiry Date Setting**: For Senior Citizens and Students, operators can set expiry dates

### Recommended Verification Steps
1. Customer applies for discount in app
2. Operator sees notification of pending discount verification
3. During customer's first ride (or at office):
   - Operator requests to see physical ID
   - Operator verifies ID authenticity
   - Operator marks discount as verified in system
4. For future bookings, system automatically applies discount

## Integration with Booking System

### Fare Calculation (To Be Implemented)
When creating a booking, the fare calculation should check:
```kotlin
if (userProfile.discountVerified && userProfile.discountType != null) {
    val discountPercent = userProfile.discountType.discountPercent
    val discountedFare = baseFare * (1 - discountPercent / 100)
    estimatedFare = discountedFare
}
```

### Booking Display
Bookings should show:
- Original fare
- Discount applied (if any)
- Final fare amount

Example:
```
Base Fare: ₱50.00
Discount (PWD 20%): -₱10.00
Final Fare: ₱40.00
```

## Database Structure

### Firebase Realtime Database
Discount information is stored under:
```
users/
  {userId}/
    profile/
      discountType: "PWD" | "SENIOR_CITIZEN" | "STUDENT" | null
      discountIdNumber: "string"
      discountVerified: boolean
      discountExpiryDate: timestamp | null
```

## Security Considerations

1. **ID Verification**: Critical that operators physically verify IDs before marking as verified
2. **Expiry Dates**: Senior Citizen IDs and Student IDs should have expiry dates set
3. **Fraud Prevention**: System tracks who verified the discount (to be implemented)
4. **Audit Trail**: All discount changes should be logged (to be implemented)

## Testing Checklist

- [x] Customer can apply for PWD discount
- [x] Customer can apply for Senior Citizen discount
- [x] Customer can apply for Student discount
- [x] Discount shows as "Pending Verification" after application
- [x] Customer can update discount information
- [x] Discount information persists after app restart
- [ ] Operator can verify discount (not yet implemented)
- [ ] Verified discount shows with green checkmark
- [ ] Discount is applied to fare calculation (not yet implemented)
- [ ] Booking shows discount breakdown (not yet implemented)

## Next Steps

1. **Implement Operator Verification UI**
   - Add "Discount Verifications" screen for operators
   - Show list of pending verifications
   - Add verify/reject buttons

2. **Update Fare Calculation**
   - Modify booking creation to apply discounts
   - Show discount breakdown in fare display
   - Store original and discounted fare in booking

3. **Add Expiry Date Management**
   - Operators set expiry dates during verification
   - System checks expiry before applying discount
   - Notify customers when discount is expiring

4. **Add Audit Logging**
   - Log who verified each discount
   - Log when discount was applied to bookings
   - Track discount usage statistics

5. **Add Reporting**
   - Monthly discount usage reports
   - Discount beneficiary statistics
   - Financial impact analysis

## Legal Compliance

Ensure compliance with:
- **RA 7277** (Magna Carta for Persons with Disability) - 20% discount
- **RA 9994** (Expanded Senior Citizens Act) - 20% discount
- Local ordinances regarding student discounts

## Files Modified

1. `app/src/main/java/com/example/toda/data/Models.kt`
   - Added discount fields to UserProfile
   - Added DiscountType enum

2. `app/src/main/java/com/example/toda/data/FirebaseModels.kt`
   - Added discount fields to FirebaseUserProfile

3. `app/src/main/java/com/example/toda/ui/customer/CustomerDashboardScreen.kt`
   - Added discount button
   - Added discount application dialog
   - Added discount display in customer info card

4. `app/src/main/java/com/example/toda/viewmodel/CustomerDashboardViewModel.kt`
   - Added applyForDiscount() function

5. `app/src/main/java/com/example/toda/repository/TODARepository.kt`
   - Updated updateUserProfile() to handle discount fields
   - Updated getUserProfile() to handle discount fields

## Support and Maintenance

For questions or issues regarding the discount system, contact the development team.

---
**Document Version**: 1.0  
**Last Updated**: October 12, 2025  
**Author**: Development Team
