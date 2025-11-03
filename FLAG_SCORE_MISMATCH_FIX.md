# Flag Score Data Mismatch - Issue & Solution

## Problem Identified

**Symptom:** Driver app shows Flag Score: 500 pts, but admin panel shows 125 pts with 2 active flags

**Root Cause:** Data inconsistency in Firebase database
- `drivers/driver_1759325281790_2222/flagScore` = **500** (stale data)
- Active flags in `driverFlags/driver_1759325281790_2222` = **2 flags totaling 125 pts**

## Analysis

The discrepancy occurs when:
1. Admin panel resolves flags in `driverFlags` collection
2. Admin panel **fails to update** the `flagScore` field in `drivers` collection
3. Driver app reads the stale `flagScore` value from `drivers` collection

## Current Implementation

Our driver app correctly reads from:
```kotlin
drivers/{driverId}/flagScore  // Returns 500 (stale)
drivers/{driverId}/flagStatus  // Returns "monitored"
```

The admin panel should be updating both:
- Resolving flags in `driverFlags/{driverId}/{flagId}`
- Recalculating and updating `flagScore` in `drivers/{driverId}`

## Solutions

### Option 1: Fix Admin Panel (Recommended)
The admin panel should update `drivers/{driverId}/flagScore` whenever flags are resolved:

```javascript
// When resolving a flag
async function resolveFlag(driverId, flagId) {
  // 1. Update flag status
  await update(ref(db, `driverFlags/${driverId}/${flagId}`), {
    status: 'resolved',
    resolvedBy: 'Admin',
    resolvedDate: Date.now()
  });
  
  // 2. Recalculate total score from active flags
  const flagsSnapshot = await get(ref(db, `driverFlags/${driverId}`));
  let totalScore = 0;
  flagsSnapshot.forEach(child => {
    const flag = child.val();
    if (flag.status === 'active') {
      totalScore += flag.points;
    }
  });
  
  // 3. Update driver's flagScore
  const newStatus = totalScore > 300 ? 'suspended' 
    : totalScore > 150 ? 'restricted' 
    : totalScore > 50 ? 'monitored' 
    : 'good';
    
  await update(ref(db, `drivers/${driverId}`), {
    flagScore: totalScore,
    flagStatus: newStatus
  });
}
```

### Option 2: Calculate Score in Driver App (Workaround)
Modify the driver app to calculate score from active flags instead of trusting the stored value:

```kotlin
// In FirebaseRealtimeDatabaseService.kt
suspend fun getDriverFlagData(driverId: String): Result<DriverFlagData> {
    return try {
        // Get active flags
        val flagsSnapshot = database.child("driverFlags").child(driverId).get().await()
        
        // Calculate actual score from active flags
        var calculatedScore = 0
        flagsSnapshot.children.forEach { flagSnapshot ->
            val status = flagSnapshot.child("status").getValue(String::class.java)
            val points = flagSnapshot.child("points").getValue(Int::class.java) ?: 0
            if (status == "active") {
                calculatedScore += points
            }
        }
        
        // Determine status based on calculated score
        val calculatedStatus = when {
            calculatedScore > 300 -> "suspended"
            calculatedScore > 150 -> "restricted"
            calculatedScore > 50 -> "monitored"
            else -> "good"
        }
        
        Result.success(DriverFlagData(
            flagScore = calculatedScore,  // Use calculated score
            flagStatus = calculatedStatus  // Use calculated status
        ))
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### Option 3: Hybrid Approach (Best)
Use calculated score but also log discrepancies for admin to fix:

```kotlin
suspend fun getDriverFlagData(driverId: String): Result<DriverFlagData> {
    return try {
        // Read stored values
        val driverSnapshot = hardwareDriversRef.child(driverId).get().await()
        val storedScore = driverSnapshot.child("flagScore").getValue(Int::class.java) ?: 0
        val storedStatus = driverSnapshot.child("flagStatus").getValue(String::class.java) ?: "good"
        
        // Calculate actual score from active flags
        val flagsSnapshot = database.child("driverFlags").child(driverId).get().await()
        var calculatedScore = 0
        flagsSnapshot.children.forEach { flagSnapshot ->
            val status = flagSnapshot.child("status").getValue(String::class.java)
            val points = flagSnapshot.child("points").getValue(Int::class.java) ?: 0
            if (status == "active") {
                calculatedScore += points
            }
        }
        
        // Log discrepancy
        if (storedScore != calculatedScore) {
            println("⚠️ FLAG SCORE MISMATCH for driver $driverId")
            println("   Stored in drivers collection: $storedScore")
            println("   Calculated from active flags: $calculatedScore")
            println("   Using calculated value for accuracy")
        }
        
        // Determine status based on calculated score
        val calculatedStatus = when {
            calculatedScore > 300 -> "suspended"
            calculatedScore > 150 -> "restricted"
            calculatedScore > 50 -> "monitored"
            else -> "good"
        }
        
        Result.success(DriverFlagData(
            flagScore = calculatedScore,  // Always use calculated
            flagStatus = calculatedStatus
        ))
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

## Recommendation

**Implement Option 3 (Hybrid)** for now to ensure data accuracy, then **fix the admin panel** to prevent future discrepancies.

### Benefits:
✅ Driver app shows accurate scores immediately
✅ Logs help identify data inconsistencies  
✅ Works even if admin panel isn't fixed yet
✅ Self-healing - always calculates from source of truth

### Implementation Priority:
1. **Immediate**: Implement hybrid approach in driver app
2. **Soon**: Fix admin panel to update flagScore when resolving flags
3. **Future**: Add data validation/sync job to fix existing inconsistencies

## Testing

After implementing the fix:
1. Login as Lucas Abad (driver_1759325281790_2222)
2. Verify Flag Score shows: **125 pts** ✅
3. Verify Status shows: **Monitored** ✅
4. Verify 2 active flags are listed ✅

---

**Created:** November 4, 2025
**Status:** Issue Identified, Solution Proposed
