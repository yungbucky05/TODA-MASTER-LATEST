# Unified Firebase Realtime Database Structure for TODA System
# Integrating Hardware ESP32 Queueing System + Mobile App

## Updated Root Structure
```
toda-booking-system/
├── users/                          # User records (auth + profile fields)
├── drivers/                        # Hardware-scanned drivers (ESP32 compatible)
├── driverQueue/                    # Physical queue at terminal (ESP32)
├── availableDrivers/               # Real-time available drivers (mobile app)
├── contributions/                  # Driver payments (ESP32)
├── bookings/                       # Mobile app bookings
├── activeTrips/                    # Hardware-assigned trips (ESP32)
├── tricycles/                      # Fleet management
├── driverLocations/                # Real-time GPS tracking
├── chatMessages/                   # In-app messaging
├── notifications/                  # User notifications
├── emergencyAlerts/                # Safety features
├── phoneNumberIndex/               # Quick lookups
└── systemStatus/                   # Hardware status monitoring
```

Note: The legacy userProfiles/ node has been removed. All profile-related fields now live under users/{userId}.

## Unified Driver Management

### 1. ESP32 Hardware Integration
```json
drivers/
  $rfidUID/
    rfidUID: "A1B2C3D4"
    name: "Juan Dela Cruz"
    licenseNumber: "A01-23-456789"
    tricycleId: "tricycle001"
    todaNumber: "TODA001-001"
    isRegistered: true
    createdBy: "hardware" | "mobile"
    registrationDate: 1693123456789
    totalContributions: 150.00
    isActive: true
```

### 2. Physical Queue (ESP32)
```json
driverQueue/
  $timestamp/
    driverId: "A1B2C3D4"  // RFID UID
    driverName: "Juan Dela Cruz"
    queuePosition: 1
    timestamp: 1693123456789
    contributionAmount: 5.00
    status: "waiting" | "assigned" | "completed"
```

### 3. Available Drivers (Mobile App)
```json
availableDrivers/
  $driverId/
    driverId: "A1B2C3D4"
    tricycleId: "tricycle001"
    latitude: 14.748005
    longitude: 121.049900
    isOnline: true
    isInPhysicalQueue: true  // Links to hardware queue
    queuePosition: 1
    timestamp: 1693123456789
```

### 4. Unified Trip Records
```json
activeTrips/
  $tripId/
    tripId: "trip123"
    driverId: "A1B2C3D4"
    bookingId: "booking123"  // null if hardware-assigned
    passengerType: "regular" | "special"
    assignmentSource: "hardware" | "mobile"
    startTime: 1693123456789
    status: "active" | "completed"
    
    // Hardware assignment data
    buttonPressed: "regular" | "special"
    
    // Mobile booking data (if applicable)
    customerId: "user456"
    pickupLocation: "Barangay 177"
    destination: "SM North"
```

## Hardware-Mobile Integration Points

### 5. System Status Monitoring
```json
systemStatus/
  hardware/
    mainESP32/
      status: "online" | "offline"
      lastHeartbeat: 1693123456789
      wifiConnected: true
      rfidReaderStatus: "ready"
      coinSlotStatus: "enabled"
    buttonESP32/
      status: "online" | "offline"
      lastHeartbeat: 1693123456789
      queueLength: 3
  mobile/
    activeUsers: 15
    pendingBookings: 2
```

## Database Security Rules

```javascript
{
  "rules": {
    // Users can read/write their own data
    "users": {
      "$userId": {
        ".read": "$userId === auth.uid",
        ".write": "$userId === auth.uid"
      }
    },

    // Bookings are readable by customer, assigned driver, and operators
    "bookings": {
      "$bookingId": {
        ".read": "data.child('customerId').val() === auth.uid || data.child('assignedDriverId').val() === auth.uid || root.child('users/' + auth.uid + '/userType').val() === 'OPERATOR'",
        ".write": "data.child('customerId').val() === auth.uid || data.child('assignedDriverId').val() === auth.uid || root.child('users/' + auth.uid + '/userType').val() === 'OPERATOR'"
      }
    },

    // Driver locations readable by operators and other drivers
    "driverLocations": {
      ".read": "root.child('users/' + auth.uid + '/userType').val() === 'DRIVER' || root.child('users/' + auth.uid + '/userType').val() === 'OPERATOR'",
      "$driverId": {
        ".write": "$driverId === auth.uid"
      }
    },

    // Available drivers index readable by passengers and operators
    "availableDrivers": {
      ".read": "auth != null"
    },

    // Active bookings readable by operators
    "activeBookings": {
      ".read": "root.child('users/' + auth.uid + '/userType').val() === 'OPERATOR'"
    },

    // Emergency alerts writable by authenticated users
    "emergencyAlerts": {
      ".read": "root.child('users/' + auth.uid + '/userType').val() === 'OPERATOR' || root.child('users/' + auth.uid + '/userType').val() === 'TODA_ADMIN'",
      ".write": "auth != null"
    }
  }
}
```

## Query Optimization Strategies

### 1. Indexing
- Create indexes on frequently queried fields:
  - `bookings` ordered by `timestamp`, `status`, `customerId`, `assignedDriverId`
  - `driverLocations` ordered by `timestamp`, `isAvailable`
  - `chatMessages` ordered by `timestamp`

### 2. Data Denormalization
- Store frequently accessed data redundantly for faster reads
- Maintain user names in bookings to avoid additional lookups
- Keep driver availability status in multiple places for quick access

### 3. Efficient Queries
- Use index collections for complex queries
- Limit query results with `.limitToLast()` and `.limitToFirst()`
- Use `.orderByChild()` for sorted queries

### 4. Real-time Updates
- Use Firebase listeners only for critical real-time data
- Implement proper cleanup for listeners to prevent memory leaks
- Use single listeners for collections rather than multiple individual listeners

## Performance Considerations

1. Minimize Data Transfer: Only fetch required fields
2. Use Shallow Queries: Use `.shallow=true` for list operations
3. Implement Pagination: For large datasets like booking history
4. Cache Frequently Used Data: Store in local storage/SharedPreferences
5. Batch Operations: Use `updateChildren()` for multiple updates
6. Offline Support: Firebase automatically handles offline caching
