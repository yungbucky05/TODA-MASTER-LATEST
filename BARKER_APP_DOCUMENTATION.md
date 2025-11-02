# TODA Barker Terminal App - Documentation

## Overview
The **Barker App** is a specialized build variant of the TODA system designed for use at the physical TODA terminal. It allows barkers (terminal staff) to create bookings on behalf of walk-in customers who approach the terminal in person.

## Features

### Current Implementation (Barebones)
1. **Direct Launch**: Automatically bypasses user type selection and goes straight to barker login
2. **Simple Booking Form**: 
   - Customer name input
   - Phone number input (11-digit validation)
   - Pickup location selection via map
   - Destination selection via map
   - Real-time fare calculation
3. **Map Integration**: Interactive map for location selection with reverse geocoding
4. **Automatic Assignment**: Bookings are automatically assigned to the next driver in queue
5. **Walk-in Tracking**: Bookings are flagged as `isWalkIn: true` for reporting purposes
6. **Verification Code**: Generates a 4-digit code for each booking

### Authentication
- Uses admin-level authentication (same as admin app)
- Barker staff must have admin credentials to access the terminal

## Build Configuration

### Build Variant
The barker app is configured as a product flavor in `build.gradle.kts`:

```kotlin
create("barker") {
    dimension = "role"
    applicationIdSuffix = ".barker"
    versionNameSuffix = "-barker"
    resValue("string", "app_name", "TODA Barker Terminal")
}
```

### Building the Barker App

#### Debug Build
```bash
gradlew assembleBarkerDebug
```

#### Release Build
```bash
gradlew assembleBarkerRelease
```

The APK will be generated at:
`app/build/outputs/apk/barker/debug/app-barker-debug.apk`
or
`app/build/outputs/apk/barker/release/app-barker-release.apk`

## File Structure

### New Files Created
1. **BarkerInterface.kt** 
   - Location: `app/src/main/java/com/example/toda/ui/barker/BarkerInterface.kt`
   - Main UI for the barker terminal

2. **BarkerLauncherActivity.kt**
   - Location: `app/src/barker/java/com/example/toda/BarkerLauncherActivity.kt`
   - Launcher that automatically starts barker flow

3. **AndroidManifest.xml (barker variant)**
   - Location: `app/src/barker/AndroidManifest.xml`
   - Overrides launcher activity for barker variant

### Modified Files
1. **build.gradle.kts** - Added barker product flavor
2. **MainActivity.kt** - Added barker_login and barker_interface screens
3. **Models.kt** - Added `isWalkIn: Boolean` field to Booking data class

## User Flow

```
App Launch (Barker Variant)
    ↓
BarkerLauncherActivity
    ↓
MainActivity (with initial_screen = "barker_login")
    ↓
Admin Login Screen (Barker authenticates with admin credentials)
    ↓
Barker Interface
    ↓
    ├─ Enter Customer Name
    ├─ Enter Customer Phone (11 digits)
    ├─ Select Pickup Location (tap map)
    ├─ Select Destination (tap map)
    ├─ View Estimated Fare
    └─ Submit Booking
        ↓
    Booking Created → Auto-assigned to next driver in queue
        ↓
    Success message with verification code
        ↓
    Form clears for next customer
```

## Data Model

### Walk-in Booking Structure
```kotlin
Booking(
    id = "booking_${timestamp}",
    customerId = "walkin_${timestamp}",  // Special ID format for walk-ins
    customerName = "Customer Name",
    phoneNumber = "09123456789",
    pickupLocation = "Geocoded address",
    destination = "Geocoded address",
    pickupGeoPoint = GeoPoint(lat, lng),
    dropoffGeoPoint = GeoPoint(lat, lng),
    estimatedFare = 150.0,
    verificationCode = "1234",
    isWalkIn = true  // NEW FIELD - marks as walk-in booking
)
```

## Future Enhancements (Polish Ideas)

### High Priority
1. **Receipt Printing**: Integration with thermal printer for physical receipts
2. **Booking History**: View recent bookings made at the terminal
3. **Driver Queue Display**: Show current driver queue on screen
4. **Quick Location Presets**: Commonly used pickup/dropoff locations
5. **Customer Phone Verification**: SMS verification for walk-in customers

### Medium Priority
6. **Multi-language Support**: Tagalog/English toggle
7. **Larger UI Elements**: Optimized for tablet/kiosk displays
8. **Barker Statistics**: Track bookings per barker/shift
9. **Cash Handling**: Record payment collected at terminal
10. **Customer Search**: Look up returning customers by phone number

### Low Priority
11. **Custom TODA Branding**: Logo and color scheme per TODA
12. **Shift Management**: Clock in/out for barkers
13. **Offline Mode**: Cache and sync when connection restored
14. **Audio Feedback**: Confirmation sounds for actions
15. **QR Code Generation**: Customer can scan to track booking

## Security Considerations

1. **Admin-Level Access**: Barkers use admin credentials - secure these carefully
2. **Terminal Lock**: Implement auto-lock after inactivity
3. **Audit Trail**: All bookings logged with barker ID and timestamp
4. **Customer Data**: Walk-in phone numbers should be verified/validated

## Testing Checklist

- [ ] Can launch barker app directly to login screen
- [ ] Can authenticate with admin credentials
- [ ] Can enter customer information
- [ ] Can select pickup location on map
- [ ] Can select destination on map
- [ ] Fare calculation works correctly
- [ ] Phone number validation (11 digits)
- [ ] Booking submission succeeds
- [ ] Walk-in flag is set correctly
- [ ] Verification code is generated
- [ ] Form clears after successful submission
- [ ] Logout functionality works
- [ ] Booking appears in admin panel with walk-in indicator

## Installation on Terminal Device

1. Enable "Install from Unknown Sources" on Android device
2. Transfer APK to device via USB or network
3. Install APK
4. Launch "TODA Barker Terminal" app
5. Login with admin credentials
6. Begin processing walk-in customers

## Support & Troubleshooting

### Common Issues

**Issue**: Can't login to barker terminal
- **Solution**: Verify admin credentials are correct, check internet connection

**Issue**: Map not loading
- **Solution**: Check GPS/location permissions, verify internet connection

**Issue**: Fare not calculating
- **Solution**: Ensure both pickup AND destination are selected

**Issue**: Booking submission fails
- **Solution**: Check Firebase connection, verify all required fields are filled

## Development Notes

- Based on customer booking flow from `CustomerInterface.kt`
- Uses same Firebase backend as main app
- Shares authentication system with admin app
- Booking auto-match logic handled by existing backend
- No special queue priority for walk-in bookings (future enhancement)

## Version History

### v1.0 (Current - Barebones)
- Initial implementation
- Basic booking form
- Map-based location selection
- Fare calculation
- Walk-in flag tracking
- Admin authentication

---

**Last Updated**: November 2, 2025
**Status**: Barebones Implementation Complete ✅
**Ready for Testing**: Yes
**Ready for Production**: After testing and polish

