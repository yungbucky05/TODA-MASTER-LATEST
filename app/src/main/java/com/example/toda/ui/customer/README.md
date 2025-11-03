# 🚖 Fare Matrix Module - User Guide

## Overview

The Fare Matrix module manages the fare calculation system for both Special and Regular trips. It provides a centralized interface to update base fares and per-kilometer rates, with complete change history tracking.

---

## Table of Contents
1. [How Fare Matrix Works](#how-fare-matrix-works)
2. [Database Structure](#database-structure)
3. [Admin Panel Features](#admin-panel-features)
4. [Mobile App Integration](#mobile-app-integration)
5. [Fare Calculation Examples](#fare-calculation-examples)
6. [Best Practices](#best-practices)

---

## How Fare Matrix Works

### Trip Types

The system supports two types of trips:

#### 1. 🚖 Special Trip
- **Purpose**: Premium service, direct routes, exclusive booking
- **Default Rates**:
  - Base Fare (First KM): ₱25.00
  - Per KM Rate: ₱5.00
- **Use Case**: Customer requests dedicated service from point A to B

#### 2. 🚕 Regular Trip
- **Purpose**: Standard service, shared routes, queue-based
- **Default Rates**:
  - Base Fare (First KM): ₱8.00
  - Per KM Rate: ₱2.00
- **Use Case**: Customer joins the queue for standard service

### Fare Calculation Formula

```
Total Fare = Base Fare + (Distance - 1) × Per KM Rate
```

**Where:**
- `Base Fare`: Covers the first kilometer
- `Distance`: Total distance in kilometers
- `Per KM Rate`: Rate for each additional kilometer

**Important:** The first kilometer is included in the base fare, so we subtract 1 from the total distance.

---

## Database Structure

### Firebase Realtime Database Schema

```
firebaseRoot/
├── fareConfig/
│   ├── current/
│   │   ├── special/
│   │   │   ├── baseFare: 25
│   │   │   └── perKmRate: 5
│   │   ├── regular/
│   │   │   ├── baseFare: 8
│   │   │   └── perKmRate: 2
│   │   ├── lastUpdated: 1762196167803
│   │   └── updatedBy: "Admin"
│   └── history/
│       └── {historyId}/
│           ├── timestamp: 1762196167803
│           ├── tripType: "special"
│           ├── oldBaseFare: 26
│           ├── oldPerKmRate: 4
│           ├── newBaseFare: 25
│           ├── newPerKmRate: 5
│           ├── reason: "Price adjustment"
│           └── updatedBy: "Admin"
└── fareMatrix/
    ├── special/
    │   ├── baseFare: 25
    │   ├── perKmRate: 5
    │   ├── lastUpdated: 1762196167803
    │   └── updatedBy: "Admin"
    └── regular/
        ├── baseFare: 8
        ├── perKmRate: 2
        ├── lastUpdated: 1762196167803
        └── updatedBy: "Admin"
```

### Key Notes:
- **fareConfig/current**: Primary source for fare rates (used by admin panel)
- **fareMatrix**: Duplicate node created for mobile app compatibility
- **Both nodes are synchronized** when admin updates fares
- Mobile app can listen to either `fareConfig/current` or `fareMatrix`

---

## Admin Panel Features

### Dashboard View

The main dashboard displays:

1. **Special Trip Card (Blue)**
   - Current base fare
   - Current per-kilometer rate
   - Visual format: `₱25.00 + ₱5.00/km`

2. **Regular Trip Card (Green)**
   - Current base fare
   - Current per-kilometer rate
   - Visual format: `₱8.00 + ₱2.00/km`

3. **Last Updated Card (Orange)**
   - Timestamp of last fare change
   - Relative time display (e.g., "2 hours ago")

### Updating Fare Rates

#### Step-by-Step Process:

1. **Select Trip Type**
   - Click "🚖 Special Trip" or "🚕 Regular Trip" tab
   - Form switches to show relevant inputs

2. **Enter New Rates**
   - **Base Fare**: First kilometer charge (must be > 0)
   - **Per KM Rate**: Additional kilometer charge (must be > 0)
   - Form shows current values as placeholders

3. **Provide Reason**
   - Required field for audit trail
   - Examples: "Fuel price increase", "TODA board decision", "Seasonal adjustment"

4. **Submit Changes**
   - Click "Update Rates" button
   - Confirmation modal appears

5. **Confirm Update**
   - Review changes in modal:
     - Trip type badge (Special/Regular)
     - New base fare
     - New per KM rate
     - Reason for change
   - Click "Confirm Update" to apply
   - Or click "Cancel" to abort

### Fare Change History

The system maintains a complete audit trail of all fare changes:

- **Date & Time**: When the change was made
- **Trip Type**: Special or Regular (color-coded badge)
- **Old → New Values**: Before and after comparison
- **Reason**: Admin's justification
- **Updated By**: Who made the change

#### History Features:
- **Pagination**: 10 entries per page (configurable)
- **Sorting**: Newest changes first
- **Navigation**: First, Previous, Next, Last page buttons
- **Page Numbers**: Quick jump to specific page
- **Items Per Page**: Dropdown to change display count (5, 10, 25, 50, 100)

---

## Mobile App Integration

### Setup Instructions

#### 1. Install Firebase SDK

**For React Native:**
```bash
npm install @react-native-firebase/app @react-native-firebase/database
```

**For Flutter:**
```yaml
# pubspec.yaml
dependencies:
  firebase_core: ^latest_version
  firebase_database: ^latest_version
```

#### 2. Initialize Firebase

**React Native:**
```javascript
// firebaseConfig.js
import database from '@react-native-firebase/database';

export const fareMatrixRef = database().ref('fareConfig/current');
// Alternative: database().ref('fareMatrix')
```

**Flutter:**
```dart
// firebase_service.dart
import 'package:firebase_database/firebase_database.dart';

final fareMatrixRef = FirebaseDatabase.instance.ref('fareConfig/current');
// Alternative: FirebaseDatabase.instance.ref('fareMatrix')
```

### Real-Time Listener Implementation

#### React Native Example

```javascript
import React, { useState, useEffect } from 'react';
import { View, Text, StyleSheet } from 'react-native';
import database from '@react-native-firebase/database';

const FareEstimationCard = ({ distance, tripType = 'regular' }) => {
  const [fareData, setFareData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Reference to fare configuration
    const fareRef = database().ref('fareConfig/current');

    // Set up real-time listener
    const onValueChange = fareRef.on('value', snapshot => {
      if (snapshot.exists()) {
        const data = snapshot.val();
        setFareData(data);
        setLoading(false);
      }
    });

    // Cleanup listener on unmount
    return () => fareRef.off('value', onValueChange);
  }, []);

  // Calculate estimated fare
  const calculateFare = () => {
    if (!fareData || !fareData[tripType]) {
      return 0;
    }

    const { baseFare, perKmRate } = fareData[tripType];
    
    // Formula: baseFare + (distance - 1) * perKmRate
    // First kilometer is included in base fare
    const additionalKm = Math.max(0, distance - 1);
    const totalFare = baseFare + (additionalKm * perKmRate);
    
    return totalFare.toFixed(2);
  };

  if (loading) {
    return (
      <View style={styles.card}>
        <Text>Loading fare information...</Text>
      </View>
    );
  }

  const estimatedFare = calculateFare();
  const { baseFare, perKmRate } = fareData[tripType] || {};

  return (
    <View style={styles.card}>
      <View style={styles.header}>
        <Text style={styles.title}>
          {tripType === 'special' ? '🚖 Special Trip' : '🚕 Regular Trip'}
        </Text>
        <Text style={styles.badge}>
          {tripType === 'special' ? 'Premium' : 'Standard'}
        </Text>
      </View>

      <View style={styles.fareBreakdown}>
        <View style={styles.breakdownRow}>
          <Text style={styles.label}>Base Fare (1st km):</Text>
          <Text style={styles.value}>₱{baseFare?.toFixed(2)}</Text>
        </View>
        <View style={styles.breakdownRow}>
          <Text style={styles.label}>Per KM Rate:</Text>
          <Text style={styles.value}>₱{perKmRate?.toFixed(2)}/km</Text>
        </View>
        <View style={styles.breakdownRow}>
          <Text style={styles.label}>Distance:</Text>
          <Text style={styles.value}>{distance.toFixed(2)} km</Text>
        </View>
      </View>

      <View style={styles.totalSection}>
        <Text style={styles.totalLabel}>Estimated Fare</Text>
        <Text style={styles.totalValue}>₱{estimatedFare}</Text>
      </View>

      <Text style={styles.disclaimer}>
        * Final fare may vary based on actual route and conditions
      </Text>
    </View>
  );
};

const styles = StyleSheet.create({
  card: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    margin: 16,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  title: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333',
  },
  badge: {
    backgroundColor: '#4CAF50',
    color: '#fff',
    paddingHorizontal: 12,
    paddingVertical: 4,
    borderRadius: 12,
    fontSize: 12,
    fontWeight: '600',
  },
  fareBreakdown: {
    borderTopWidth: 1,
    borderBottomWidth: 1,
    borderColor: '#e0e0e0',
    paddingVertical: 12,
    marginBottom: 12,
  },
  breakdownRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 8,
  },
  label: {
    fontSize: 14,
    color: '#666',
  },
  value: {
    fontSize: 14,
    fontWeight: '600',
    color: '#333',
  },
  totalSection: {
    backgroundColor: '#f5f5f5',
    borderRadius: 8,
    padding: 12,
    marginBottom: 12,
  },
  totalLabel: {
    fontSize: 12,
    color: '#666',
    marginBottom: 4,
  },
  totalValue: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#2196F3',
  },
  disclaimer: {
    fontSize: 11,
    color: '#999',
    fontStyle: 'italic',
    textAlign: 'center',
  },
});

export default FareEstimationCard;
```

#### Flutter Example

```dart
import 'package:flutter/material.dart';
import 'package:firebase_database/firebase_database.dart';

class FareEstimationCard extends StatefulWidget {
  final double distance;
  final String tripType; // 'special' or 'regular'

  const FareEstimationCard({
    Key? key,
    required this.distance,
    this.tripType = 'regular',
  }) : super(key: key);

  @override
  _FareEstimationCardState createState() => _FareEstimationCardState();
}

class _FareEstimationCardState extends State<FareEstimationCard> {
  final DatabaseReference _fareRef = FirebaseDatabase.instance.ref('fareConfig/current');
  
  Map<String, dynamic>? fareData;
  bool isLoading = true;

  @override
  void initState() {
    super.initState();
    _setupFareListener();
  }

  void _setupFareListener() {
    _fareRef.onValue.listen((DatabaseEvent event) {
      if (event.snapshot.exists) {
        setState(() {
          fareData = Map<String, dynamic>.from(event.snapshot.value as Map);
          isLoading = false;
        });
      }
    });
  }

  double _calculateFare() {
    if (fareData == null || fareData![widget.tripType] == null) {
      return 0.0;
    }

    final tripData = Map<String, dynamic>.from(fareData![widget.tripType]);
    final baseFare = (tripData['baseFare'] ?? 0).toDouble();
    final perKmRate = (tripData['perKmRate'] ?? 0).toDouble();

    // Formula: baseFare + (distance - 1) * perKmRate
    final additionalKm = (widget.distance - 1).clamp(0.0, double.infinity);
    final totalFare = baseFare + (additionalKm * perKmRate);

    return totalFare;
  }

  @override
  Widget build(BuildContext context) {
    if (isLoading) {
      return Card(
        margin: EdgeInsets.all(16),
        child: Padding(
          padding: EdgeInsets.all(16),
          child: Center(child: CircularProgressIndicator()),
        ),
      );
    }

    final tripData = fareData?[widget.tripType] ?? {};
    final baseFare = (tripData['baseFare'] ?? 0).toDouble();
    final perKmRate = (tripData['perKmRate'] ?? 0).toDouble();
    final estimatedFare = _calculateFare();

    final isSpecial = widget.tripType == 'special';

    return Card(
      margin: EdgeInsets.all(16),
      elevation: 3,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Padding(
        padding: EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Header
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  isSpecial ? '🚖 Special Trip' : '🚕 Regular Trip',
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                    color: Colors.black87,
                  ),
                ),
                Container(
                  padding: EdgeInsets.symmetric(horizontal: 12, vertical: 4),
                  decoration: BoxDecoration(
                    color: isSpecial ? Colors.blue : Colors.green,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Text(
                    isSpecial ? 'Premium' : 'Standard',
                    style: TextStyle(
                      color: Colors.white,
                      fontSize: 12,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
              ],
            ),
            SizedBox(height: 16),

            // Fare Breakdown
            Container(
              decoration: BoxDecoration(
                border: Border(
                  top: BorderSide(color: Colors.grey[300]!),
                  bottom: BorderSide(color: Colors.grey[300]!),
                ),
              ),
              padding: EdgeInsets.symmetric(vertical: 12),
              margin: EdgeInsets.only(bottom: 12),
              child: Column(
                children: [
                  _buildBreakdownRow('Base Fare (1st km):', '₱${baseFare.toStringAsFixed(2)}'),
                  SizedBox(height: 8),
                  _buildBreakdownRow('Per KM Rate:', '₱${perKmRate.toStringAsFixed(2)}/km'),
                  SizedBox(height: 8),
                  _buildBreakdownRow('Distance:', '${widget.distance.toStringAsFixed(2)} km'),
                ],
              ),
            ),

            // Total Fare
            Container(
              padding: EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: Colors.grey[100],
                borderRadius: BorderRadius.circular(8),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Estimated Fare',
                    style: TextStyle(
                      fontSize: 12,
                      color: Colors.grey[600],
                    ),
                  ),
                  SizedBox(height: 4),
                  Text(
                    '₱${estimatedFare.toStringAsFixed(2)}',
                    style: TextStyle(
                      fontSize: 24,
                      fontWeight: FontWeight.bold,
                      color: Colors.blue,
                    ),
                  ),
                ],
              ),
            ),
            SizedBox(height: 12),

            // Disclaimer
            Text(
              '* Final fare may vary based on actual route and conditions',
              style: TextStyle(
                fontSize: 11,
                color: Colors.grey[600],
                fontStyle: FontStyle.italic,
              ),
              textAlign: TextAlign.center,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildBreakdownRow(String label, String value) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(
          label,
          style: TextStyle(fontSize: 14, color: Colors.grey[700]),
        ),
        Text(
          value,
          style: TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w600,
            color: Colors.black87,
          ),
        ),
      ],
    );
  }

  @override
  void dispose() {
    super.dispose();
  }
}
```

### Usage in Mobile App

**React Native:**
```javascript
import FareEstimationCard from './components/FareEstimationCard';

// In your screen component
<FareEstimationCard 
  distance={5.5} 
  tripType="special" 
/>
```

**Flutter:**
```dart
// In your widget tree
FareEstimationCard(
  distance: 5.5,
  tripType: 'special',
)
```

### Real-Time Updates

When admin updates fare rates in the admin panel:

1. **Admin Panel** → Updates `fareConfig/current/{tripType}`
2. **Admin Panel** → Also updates `fareMatrix/{tripType}` (for compatibility)
3. **Firebase** → Broadcasts change to all connected clients
4. **Mobile App** → Listener receives update automatically
5. **Mobile App** → UI re-renders with new rates
6. **User** → Sees updated fare estimate in real-time

**No app restart or manual refresh required!** ✨

---

## Fare Calculation Examples

### Example 1: Short Special Trip

**Scenario:**
- Trip Type: Special (🚖)
- Distance: 2.5 km
- Base Fare: ₱25.00
- Per KM Rate: ₱5.00

**Calculation:**
```
Total Fare = 25 + (2.5 - 1) × 5
           = 25 + (1.5 × 5)
           = 25 + 7.50
           = ₱32.50
```

### Example 2: Long Regular Trip

**Scenario:**
- Trip Type: Regular (🚕)
- Distance: 10 km
- Base Fare: ₱8.00
- Per KM Rate: ₱2.00

**Calculation:**
```
Total Fare = 8 + (10 - 1) × 2
           = 8 + (9 × 2)
           = 8 + 18
           = ₱26.00
```

### Example 3: Minimum Fare (Less than 1 km)

**Scenario:**
- Trip Type: Special (🚖)
- Distance: 0.5 km
- Base Fare: ₱25.00
- Per KM Rate: ₱5.00

**Calculation:**
```
Total Fare = 25 + (0.5 - 1) × 5
           = 25 + (0 × 5)    ← Clamped to 0
           = 25 + 0
           = ₱25.00
```

**Note:** Distance less than 1 km still charges the full base fare.

### Example 4: Comparing Trip Types

**Same Distance (5 km):**

**Special Trip:**
```
₱25 + (5 - 1) × ₱5 = ₱25 + ₱20 = ₱45.00
```

**Regular Trip:**
```
₱8 + (5 - 1) × ₱2 = ₱8 + ₱8 = ₱16.00
```

**Difference:** Special costs ₱29.00 more for premium service

---

## Best Practices

### For Administrators

#### 1. **Regular Review**
- Monitor fuel prices and operating costs
- Review fare rates monthly or quarterly
- Compare with other TODA associations

#### 2. **Transparent Communication**
- Announce fare changes in advance
- Notify drivers through the system
- Post notices for customers
- Provide clear reasons for changes

#### 3. **Data-Driven Decisions**
- Analyze trip data and driver earnings
- Consider driver feedback
- Balance affordability for customers vs. fair driver compensation

#### 4. **Documentation**
- Always provide detailed reasons for fare changes
- Keep records of TODA board meetings
- Document external factors (fuel prices, inflation)

#### 5. **Testing**
- Test fare calculations before announcing
- Verify mobile app displays correct rates
- Check that history is properly recorded

### For Mobile App Developers

#### 1. **Always Use Real-Time Listeners**
❌ **Don't** fetch once on app start:
```javascript
// BAD - Will show outdated fares
const snapshot = await database().ref('fareConfig/current').once('value');
```

✅ **Do** use real-time listeners:
```javascript
// GOOD - Always shows current fares
database().ref('fareConfig/current').on('value', snapshot => {
  // Updates automatically when admin changes rates
});
```

#### 2. **Handle Loading States**
- Show loading indicator while fetching
- Display error message if fetch fails
- Provide fallback values if needed

#### 3. **Cache for Offline**
```javascript
// React Native with AsyncStorage
import AsyncStorage from '@react-native-async-storage/async-storage';

const CACHE_KEY = '@fare_rates';

// Cache fare data
const cacheFareData = async (data) => {
  try {
    await AsyncStorage.setItem(CACHE_KEY, JSON.stringify(data));
  } catch (error) {
    console.error('Failed to cache fare data:', error);
  }
};

// Load from cache if offline
const loadCachedFareData = async () => {
  try {
    const cached = await AsyncStorage.getItem(CACHE_KEY);
    return cached ? JSON.parse(cached) : null;
  } catch (error) {
    console.error('Failed to load cached fare data:', error);
    return null;
  }
};
```

#### 4. **Input Validation**
```javascript
const calculateFare = (distance, tripType, fareData) => {
  // Validate inputs
  if (!fareData || !fareData[tripType]) {
    console.warn('Fare data not available');
    return 0;
  }
  
  if (distance <= 0 || isNaN(distance)) {
    console.warn('Invalid distance');
    return 0;
  }
  
  const { baseFare, perKmRate } = fareData[tripType];
  
  if (!baseFare || !perKmRate) {
    console.warn('Incomplete fare data');
    return 0;
  }
  
  // Safe calculation
  const additionalKm = Math.max(0, distance - 1);
  return baseFare + (additionalKm * perKmRate);
};
```

#### 5. **Display Formatting**
```javascript
// Format currency consistently
const formatCurrency = (amount) => {
  return `₱${parseFloat(amount).toFixed(2)}`;
};

// Format distance
const formatDistance = (km) => {
  return `${parseFloat(km).toFixed(2)} km`;
};
```

---

## Troubleshooting

### Issue: Fare not updating in mobile app

**Possible Causes:**
1. Not using real-time listener (using `.once()` instead of `.on()`)
2. Listener not properly attached
3. Network connectivity issues
4. Firebase rules blocking read access

**Solutions:**
- Check Firebase Realtime Database rules
- Verify listener is active (check console logs)
- Test with a fresh app restart
- Check device internet connection

### Issue: Calculation showing wrong amount

**Possible Causes:**
1. Not accounting for first kilometer in base fare
2. Distance not converted to kilometers
3. Rounding errors

**Solutions:**
```javascript
// Correct formula
const totalFare = baseFare + Math.max(0, distance - 1) * perKmRate;

// Round to 2 decimal places
const roundedFare = Math.round(totalFare * 100) / 100;
```

### Issue: Old rates showing after update

**Solution:**
```javascript
// Force refresh listener
fareRef.off(); // Remove old listener
fareRef.on('value', snapshot => {
  // Attach new listener
});
```

---

## API Reference

### Database Paths

```javascript
// Get current fare configuration
'fareConfig/current'
'fareConfig/current/special'
'fareConfig/current/regular'

// Get fare history
'fareConfig/history'
'fareConfig/history/{historyId}'

// Alternative path (mobile compatibility)
'fareMatrix/special'
'fareMatrix/regular'
```

### Data Objects

**Current Fare Object:**
```javascript
{
  special: {
    baseFare: 25,
    perKmRate: 5
  },
  regular: {
    baseFare: 8,
    perKmRate: 2
  },
  lastUpdated: 1762196167803,
  updatedBy: "Admin"
}
```

**History Entry Object:**
```javascript
{
  timestamp: 1762196167803,
  tripType: "special",
  oldBaseFare: 26,
  oldPerKmRate: 4,
  newBaseFare: 25,
  newPerKmRate: 5,
  reason: "Price adjustment",
  updatedBy: "Admin"
}
```

---

## Summary

✅ **Two Trip Types**: Special (premium) and Regular (standard)  
✅ **Simple Formula**: Base fare + additional kilometers × per km rate  
✅ **Real-Time Sync**: Changes instantly reflect in mobile apps  
✅ **Complete History**: Full audit trail of all changes  
✅ **Easy Integration**: Copy-paste code examples for React Native & Flutter  
✅ **Dual Compatibility**: Works with both `fareConfig` and `fareMatrix` nodes  

The Fare Matrix system ensures consistent, transparent, and fair pricing across your TODA network! 🎉

---

## Support

### Getting Help
- Review this documentation
- Check Firebase console for data
- Test with sample calculations
- Contact system administrator

### Related Documentation
- **Driver Management**: How drivers earn from trips
- **Booking History**: See actual fares charged
- **Queue Management**: Regular trip queue system
- **Discount Applications**: How discounts affect fares

---

*Last Updated: November 4, 2025*
