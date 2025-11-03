# Database Restructuring - Quick Reference

## Summary
Moved `flags` and `rfidHistory` from nested driver/user objects to separate top-level collections for better performance and scalability.

## New Collections

### driverFlags/{driverId}/{flagId}
- Stores all flags for drivers
- Replaces `drivers/{driverId}/flags`

### userFlags/{userId}/{flagId}
- Stores all flags for passengers
- Replaces `users/{userId}/flags`

### rfidHistory/{driverId}
- Stores RFID reassignment history
- Already existed, just removed from driver objects

## What Stays in Driver/User Objects

```javascript
{
  // Driver object
  flagScore: 75,           // ✅ Keep - for quick filtering
  flagStatus: "monitored", // ✅ Keep - for quick status checks
  rfidReassignmentCount: 2 // ✅ Keep - for stats
}
```

## Files Modified

1. **FlaggedAccounts.js**
   - All flag operations use separate collections
   - Detection functions check separate collections
   - Flag creation/update/resolve operations

2. **DriverManagement.js**
   - RFID history reads from separate collection
   - RFID reassignment no longer writes history to driver object

## Migration Steps

1. **Backup your database** (export from Firebase Console)

2. **Open migration tool:**
   ```
   Admin Website/migrate-database.html
   ```

3. **Run migration:**
   - Check backup confirmation
   - Select migration options
   - Click "Start Migration"
   - Wait for completion

4. **Verify results:**
   - Check that flags work in FlaggedAccounts page
   - Check that RFID history displays in reassignment modal
   - Verify no nested data in new records

## Testing Checklist

After migration:
- [ ] Can view flagged accounts
- [ ] Can resolve flags
- [ ] Can escalate flags
- [ ] Auto-detection creates flags correctly
- [ ] RFID history displays in modal
- [ ] RFID reassignment works
- [ ] New driver records have no nested data

## Rollback (if needed)

If something goes wrong:
1. Restore database from backup
2. Revert code changes from git
3. Contact developer for assistance

## Support

For issues or questions, refer to:
- `DATABASE_RESTRUCTURING.md` - Full documentation
- Migration log output from migration tool
- Firebase Console for manual verification
