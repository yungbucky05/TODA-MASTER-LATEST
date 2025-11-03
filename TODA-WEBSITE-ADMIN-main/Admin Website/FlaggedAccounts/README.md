# 🚩 Flagged Accounts Module - User Guide

## Overview

The Flagged Accounts module is an automated system that monitors and manages problematic driver and customer accounts based on behavioral patterns and performance metrics.

---

## Features

### 🔍 Auto-Detection System
- **Automated Flag Detection**: Runs analysis on all accounts to identify issues
- **Real-time Monitoring**: Continuously tracks account behavior
- **Smart Algorithms**: Detects patterns and anomalies automatically

### 📊 Statistics Dashboard
- **Critical Flags**: High-priority issues requiring immediate attention
- **High Priority**: Urgent matters that need quick resolution
- **Monitored Accounts**: Accounts under observation
- **Total Flagged**: Overall count of flagged accounts

### 🎯 Flag Management
- **View Details**: Complete information about each flag
- **Resolve**: Mark issues as resolved
- **Escalate**: Increase severity of flags
- **Dismiss**: Mark as false positive
- **Export Reports**: Download CSV reports

---

## Auto-Detection Flags

### Driver Flags

#### 1. 💰 Low Contributions
**Severity:** High (75 points)

**Detection Logic:**
- Compares driver's weekly contributions to TODA average
- Flags if contributions are less than 50% of average
- Only checks active drivers with contribution history

**Example:**
```
Average Weekly Contribution: ₱1,500
Driver's Contribution: ₱600 (40%)
Result: FLAGGED ⚠️
```

---

#### 2. 💤 Inactive Account
**Severity:** Medium (50 points)

**Detection Logic:**
- Checks last login or activity timestamp
- Flags if no activity for 7+ days
- Only applies to active driver accounts

**Example:**
```
Last Active: October 15, 2025
Current Date: November 1, 2025
Days Inactive: 17 days
Result: FLAGGED ⚠️
```

---

#### 3. 🚫 High Cancellation Rate
**Severity:** High (75 points)

**Detection Logic:**
- Analyzes driver's booking cancellations
- Flags if cancellation rate exceeds 15%
- Requires minimum 10 bookings for analysis

**Example:**
```
Total Bookings: 50
Cancelled by Driver: 10
Cancellation Rate: 20%
Result: FLAGGED ⚠️
```

---

### Customer Flags

#### 4. 👻 No-Show Pattern
**Severity:** Critical (100 points)

**Detection Logic:**
- Tracks customer booking confirmations vs actual pickups
- Flags if no-show rate exceeds 20%
- Requires minimum 5 bookings for analysis

**Example:**
```
Total Bookings: 10
No-Shows: 3
No-Show Rate: 30%
Result: FLAGGED 🚨
```

---

## Using the Module

### Accessing Flagged Accounts

1. **From Sidebar:**
   - Click on "🚩 Flagged Accounts" in the navigation menu
   - Badge shows count of critical flags

2. **From Dashboard:**
   - Click "Flagged Accounts" quick action card
   - Shows "Monitor problematic accounts"

---

### Filtering Accounts

#### Account Type Filter
- **All Accounts**: Show both drivers and customers
- **Drivers Only**: Show only driver flags
- **Customers Only**: Show only customer flags

#### Severity Filter
- **All Severities**: Show all flag types
- **Critical**: Emergency issues (100 points)
- **High**: Urgent issues (75 points)
- **Medium**: Notable concerns (50 points)
- **Low**: Minor issues (25 points)

#### Status Filter
- **Active Flags**: Currently open issues
- **All Statuses**: Include resolved and expired
- **Resolved**: Successfully handled flags
- **Expired**: Automatically closed flags

#### Search
- Search by account name
- Search by phone number
- Search by account ID

---

### Viewing Flag Details

1. Click **"View Details"** button on any flagged account
2. Modal displays:
   - **Flag Information**: Type, severity, points, status
   - **Account Information**: Name, phone, email, total score
   - **Flag Details**: Specific metrics that triggered flag
   - **Action Timeline**: History of admin actions
   - **Notes**: Admin comments and observations

---

### Managing Flags

#### Resolve Flag ✅
**When to use:**
- Issue has been addressed and resolved
- Driver/customer has corrected behavior
- Metrics are back to acceptable levels

**Effect:**
- Flag status changes to "resolved"
- Points are subtracted from account score
- Account status may improve (suspended → restricted → monitored → good)

**Process:**
1. Click "Resolve" button
2. Confirm action
3. Flag marked as resolved
4. Score automatically updated

---

#### Escalate Flag ⚠️
**When to use:**
- Issue is worsening
- Driver/customer not responding to warnings
- Severity needs to be increased

**Effect:**
- Severity increases (medium → high → critical)
- Additional 25 points added
- Account status may worsen

**Process:**
1. Click "Escalate" button
2. Confirm escalation
3. Severity and points increased
4. Account status updated

---

#### Dismiss Flag 🗑️
**When to use:**
- Flag is a false positive
- System error in detection
- Special circumstances justify dismissal

**Effect:**
- Flag status changes to "dismissed"
- No point changes
- Flag removed from active list

**Process:**
1. Click "Dismiss" button
2. Confirm dismissal
3. Flag marked as dismissed

---

### Running Auto-Detection

#### Manual Detection Process

1. Click **"Run Auto-Detection"** button
2. Progress modal shows detection stages:
   - **Step 1 (10%)**: Analyzing driver contributions
   - **Step 2 (30%)**: Checking for inactive accounts
   - **Step 3 (50%)**: Analyzing customer booking patterns
   - **Step 4 (70%)**: Checking cancellation rates
   - **Step 5 (100%)**: Detection complete

3. Results displayed:
   - Green messages: No new flags found
   - Red messages: New flags detected
   - Shows account name and flag type

4. Click **"Close"** to return to main view

#### Detection Results Example
```
✅ No new flags detected. All accounts are in good standing.

OR

🚩 Juan Dela Cruz - Low Contributions (high)
🚩 Maria Santos - No-Show Pattern (critical)
🚩 Pedro Reyes - Inactive Account (medium)
```

---

### Exporting Reports

1. Apply desired filters
2. Click **"📥 Export Report"** button
3. CSV file downloads with columns:
   - Account Name
   - Account Type
   - Phone
   - Flag Type
   - Severity
   - Points
   - Status
   - Flagged Date

4. Filename format: `flagged_accounts_YYYY-MM-DD.csv`

---

## Flag Scoring System

### Point Values
- **Critical**: 100 points
- **High**: 75 points
- **Medium**: 50 points
- **Low**: 25 points

### Account Status Levels

| Total Score | Status | Description | Color |
|-------------|--------|-------------|-------|
| 0-50 | Good Standing ✅ | No issues or minor concerns | Green |
| 51-150 | Monitored 👀 | Under observation | Yellow |
| 151-300 | Restricted ⚠️ | Limited privileges | Orange |
| 301+ | Suspended 🚫 | Account suspended | Red |

### Point Decay (Future Feature)
- Good behavior: -10 points per week
- Excellent behavior: -20 points per week
- Points expire after 180 days

---

## Understanding the Table

### Account Column
- **Avatar**: Initials of account holder
- **Name**: Full name of driver/customer
- **Phone**: Contact number

### Type Column
- **Driver**: Blue badge
- **Customer**: Pink badge

### Flag Type Column
- **Icon**: Visual indicator of flag type
- **Name**: Description of issue

### Severity Column
- **Critical**: Red badge
- **High**: Orange badge
- **Medium**: Yellow badge
- **Low**: Blue badge

### Score Column
- **Number**: Points assigned to flag
- **Color**: Matches severity

### Status Column
- **Active**: Red badge - Currently open
- **Resolved**: Green badge - Successfully handled
- **Expired**: Gray badge - Auto-closed

### Flagged Date Column
- Date when flag was created

---

## Best Practices

### For Administrators

1. **Regular Monitoring**
   - Check flagged accounts daily
   - Prioritize critical and high severity flags
   - Run auto-detection weekly

2. **Investigation**
   - Review flag details before taking action
   - Check account history in other modules
   - Contact driver/customer if needed

3. **Documentation**
   - Add notes to flags
   - Record reason for resolution/dismissal
   - Keep audit trail

4. **Fair Treatment**
   - Investigate all flags thoroughly
   - Give drivers/customers opportunity to respond
   - Consider special circumstances

5. **Pattern Recognition**
   - Look for trends in flagged accounts
   - Identify systemic issues
   - Adjust thresholds if needed

### For Action Planning

**Critical Flags (100 points):**
- Address immediately
- Contact account holder within 24 hours
- May require account suspension

**High Flags (75 points):**
- Address within 48 hours
- Send warning notification
- Monitor closely for improvement

**Medium Flags (50 points):**
- Address within 1 week
- Send informational notification
- Set review date

**Low Flags (25 points):**
- Address within 2 weeks
- Optional notification
- May auto-expire

---

## Troubleshooting

### Issue: No flags showing
**Solutions:**
- Run auto-detection to create flags
- Check status filter (set to "Active Flags")
- Verify database connection

### Issue: Auto-detection not finding issues
**Possible reasons:**
- All accounts in good standing
- Insufficient data for analysis
- Detection thresholds too high

### Issue: Can't resolve flag
**Solutions:**
- Check if flag is already resolved
- Verify admin permissions
- Refresh page and try again

### Issue: Wrong account flagged
**Action:**
- Review flag details carefully
- Dismiss flag if false positive
- Check detection algorithm

---

## Integration with Other Modules

### Driver Management
- View full driver profile
- Check verification status
- See RFID history

### Contributions History
- Analyze contribution patterns
- Compare to averages
- Export contribution reports

### Booking History
- Check booking patterns
- Verify cancellations
- See no-show history

### Queue Management
- Monitor queue behavior
- Check contribution payments
- Analyze queue times

### Discount Applications
- Verify discount eligibility
- Check for abuse patterns
- Review application history

---

## Database Structure

### Flag Object Example
```javascript
{
  "flagId": "flag-abc123",
  "type": "LOW_CONTRIBUTIONS",
  "severity": "high",
  "points": 75,
  "timestamp": 1730534400000,
  "status": "active",
  "details": {
    "averageContribution": "1500.00",
    "driverContribution": "600.00",
    "percentage": "40",
    "period": "weekly"
  },
  "actions": [
    {
      "action": "FLAG_CREATED",
      "timestamp": 1730534400000,
      "adminId": "AUTO_DETECTION"
    }
  ],
  "notes": "Automatically detected by system on 11/1/2025"
}
```

### Account Flag Score
```javascript
{
  "flagScore": 75,
  "flagStatus": "monitored",
  "flags": {
    "flag-abc123": { /* flag object */ }
  }
}
```

---

## Future Enhancements

### Planned Features
- [ ] Automated notifications to flagged accounts
- [ ] Appeal system for drivers/customers
- [ ] Custom flag types
- [ ] Configurable thresholds
- [ ] Flag analytics dashboard
- [ ] Bulk flag actions
- [ ] Email notifications to admins
- [ ] SMS alerts for critical flags
- [ ] Point decay system
- [ ] Historical flag trends

---

## Support

### Getting Help
- Review this documentation
- Check console for errors
- Contact system administrator
- Review FLAGGED_ACCOUNTS_SYSTEM.md for technical details

### Reporting Issues
- Describe the problem clearly
- Include steps to reproduce
- Provide screenshots if possible
- Note any error messages

---

## Summary

✅ **Auto-Detection**: Automatically finds problematic accounts  
✅ **Smart Filtering**: Find exactly what you're looking for  
✅ **Detailed Views**: Complete flag information  
✅ **Action Tools**: Resolve, escalate, or dismiss flags  
✅ **Export Reports**: Download data for analysis  
✅ **Real-time Updates**: Live sync with Firebase  

The Flagged Accounts module helps maintain a healthy, accountable community of drivers and customers! 🎉
