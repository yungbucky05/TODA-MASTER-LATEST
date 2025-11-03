# 🚩 Flagged Accounts System - Complete Specification

## Overview
A comprehensive automated system to flag and monitor problematic driver and customer accounts based on behavioral patterns and performance metrics.

---

## 🚗 DRIVER FLAG CONDITIONS

### 1. **Low Contributions (CRITICAL)** 🔴
**Your Condition:** Contributions lower than average by 50%

**Implementation:**
- Calculate average daily/weekly contributions across all active drivers
- Flag if driver's contributions < (average × 0.5)
- **Severity:** High
- **Auto-action:** Warning notification + monitoring period (7 days)
- **Escalation:** Temporary suspension if continues for 14 days

**Additional Metrics:**
- Compare against TODA-specific averages (not just global)
- Consider seasonal variations (holidays, weather)
- Account for vehicle downtime (maintenance periods)

---

### 2. **Inactive Account (WARNING)** 🟡
**Your Condition:** Driver has been inactive for a week

**Implementation:**
- Track last login timestamp
- Track last booking/trip timestamp
- Flag if no activity for 7+ days
- **Severity:** Medium
- **Auto-action:** SMS/notification asking for status update
- **Escalation:** Account review after 30 days

**Additional Checks:**
- No queue entries in 7 days
- No contributions in 7 days
- App not opened in 7 days

---

### 3. **Contribution Pattern Irregularities** 🟠
**New Condition:** Suspicious contribution patterns

**Triggers:**
- Sudden drop in contributions (>70% decrease week-over-week)
- Erratic contribution amounts (high variance)
- Multiple missed contribution deadlines
- Contributions only on specific days (avoiding monitoring)
- **Severity:** Medium-High
- **Auto-action:** Admin review + audit

---

### 4. **Excessive Queue Time** 🟡
**New Condition:** Staying in queue unusually long

**Triggers:**
- Average queue wait time >2x normal average
- Frequently joins queue but rarely accepts bookings
- Queue manipulation (join/leave repeatedly)
- **Severity:** Medium
- **Auto-action:** Queue priority reduction

---

### 5. **High Cancellation Rate** 🔴
**New Condition:** Frequently cancels accepted trips

**Triggers:**
- Cancellation rate >15% of accepted bookings
- Multiple consecutive cancellations (3+ in a row)
- Last-minute cancellations (<5 min before pickup)
- **Severity:** High
- **Auto-action:** Temporary queue suspension (24 hours)
- **Escalation:** Permanent suspension if >30% cancellation rate

---

### 6. **RFID Issues** 🟡
**New Condition:** Repeated RFID problems

**Triggers:**
- Reported RFID missing >2 times in 6 months
- Attempting to use another driver's RFID
- RFID mismatch during verification
- Damaged RFID card (negligence)
- **Severity:** Medium-High
- **Auto-action:** RFID replacement fee + warning
- **Investigation:** Check for RFID sharing/fraud

---

### 7. **Document/License Expiration** 🟠
**New Condition:** Expired credentials

**Triggers:**
- Driver's license expired
- Vehicle registration expired
- Insurance expired
- Franchise/TODA permit expired
- **Severity:** Critical
- **Auto-action:** Immediate suspension until renewed

---

### 8. **Customer Complaint Score** 🔴
**New Condition:** Multiple customer complaints

**Triggers:**
- Avg rating <3.0 stars (out of 5)
- >5 complaints in 30 days
- Complaints about: rudeness, dangerous driving, overcharging
- **Severity:** High
- **Auto-action:** Mandatory training + monitoring
- **Escalation:** Suspension if score doesn't improve in 30 days

---

### 9. **GPS/Route Violations** 🟠
**New Condition:** Route manipulation or GPS issues

**Triggers:**
- Frequently turns off GPS during trips
- Deviates significantly from optimal route (>30% longer)
- Multiple GPS spoofing attempts
- Operates outside designated TODA area
- **Severity:** High
- **Auto-action:** GPS audit + route explanation required

---

### 10. **Financial Irregularities** 🔴
**New Condition:** Payment and contribution anomalies

**Triggers:**
- Multiple failed payment attempts
- Outstanding debt >₱5,000
- Contribution payment always late (>3 days overdue)
- Disputing valid charges repeatedly
- **Severity:** High
- **Auto-action:** Payment plan required + credit hold

---

### 11. **Vehicle Safety Issues** 🔴
**New Condition:** Safety and maintenance concerns

**Triggers:**
- Failed vehicle inspection
- Multiple reports of poor vehicle condition
- Accident rate >2 per 6 months
- No proof of vehicle maintenance
- **Severity:** Critical
- **Auto-action:** Immediate suspension until inspection passed

---

### 12. **Peak Hours Avoidance** 🟡
**New Condition:** Strategic inactivity during busy periods

**Triggers:**
- Consistently offline during peak hours
- Only active during slow hours (avoiding traffic/demand)
- Pattern of selective availability
- **Severity:** Medium
- **Auto-action:** Queue priority adjustment

---

## 👥 CUSTOMER FLAG CONDITIONS

### 1. **No-Show Pattern (CRITICAL)** 🔴
**Your Condition:** Constantly not showing up for bookings

**Implementation:**
- Track booking confirmations vs actual rides
- Flag if no-show rate >20%
- Consider time of day patterns
- **Severity:** High
- **Auto-action:** Require prepayment for next bookings
- **Escalation:** Account suspension after 5 no-shows in 30 days

**Thresholds:**
- 1st offense: Warning
- 2nd offense: ₱50 no-show fee
- 3rd+ offense: Account review + possible suspension

---

### 2. **Non-Payment Reports (CRITICAL)** 🔴
**Your Condition:** Reported for not paying

**Implementation:**
- Allow drivers to report non-payment
- Require proof (screenshot/timestamp)
- Cross-verify with GPS/booking data
- **Severity:** Critical
- **Auto-action:** Account freeze until payment resolved
- **Collections:** Add to debt collection queue

**Process:**
- Driver reports non-payment
- Customer has 24 hours to respond
- If confirmed: immediate payment required
- Multiple reports: account banned

---

### 3. **Wrong PIN/Address (WARNING)** 🟡
**Your Condition:** Providing wrong location pins

**Implementation:**
- Track pickup location accuracy
- Flag if driver can't find customer >3 times
- Check if PIN matches actual waiting location
- **Severity:** Medium
- **Auto-action:** Tutorial on how to set correct PIN
- **Penalty:** Reduced booking priority

**Indicators:**
- Distance between PIN and actual location >500m
- Driver repeatedly calls for directions
- Multiple cancellations due to wrong address

---

### 4. **Abusive Behavior (CRITICAL)** 🔴
**Your Condition:** Being mean to drivers

**Implementation:**
- Driver can report abusive customer
- Categories: verbal abuse, threats, harassment, discrimination
- Require incident description
- **Severity:** Critical
- **Auto-action:** Immediate investigation
- **Escalation:** Permanent ban for verified abuse

**Types of Abuse:**
- Verbal abuse/cursing
- Threats of violence
- Sexual harassment
- Discrimination (race, gender, age)
- Physical aggression

---

### 5. **Excessive Cancellations** 🟠
**New Condition:** Frequently cancels bookings

**Triggers:**
- Cancellation rate >25%
- Cancels after driver already en route
- Pattern of booking then canceling immediately
- **Severity:** Medium-High
- **Auto-action:** Cancellation fee (₱20-50)
- **Escalation:** Booking restrictions (max 3 per day)

---

### 6. **Fare Disputes** 🟡
**New Condition:** Constantly arguing about fare

**Triggers:**
- >3 fare disputes in 30 days
- Refuses to pay correct fare amount
- Claims "driver said different price"
- Attempts to negotiate after trip
- **Severity:** Medium
- **Auto-action:** In-app fare display reminder
- **Warning:** Suspension if pattern continues

---

### 7. **Booking Spam** 🟠
**New Condition:** Creating multiple bookings rapidly

**Triggers:**
- >5 bookings created in 10 minutes
- Books then cancels repeatedly
- Creates duplicate bookings for same route
- Books all available drivers then cancels most
- **Severity:** Medium-High
- **Auto-action:** Temporary booking cooldown (15 min)
- **Investigation:** Check for malicious intent

---

### 8. **Fraudulent Activities** 🔴
**New Condition:** Attempting scams or fraud

**Triggers:**
- Multiple accounts with same phone number
- Credit card chargebacks
- Fake discount document attempts
- Identity verification failures
- Sharing account credentials
- **Severity:** Critical
- **Auto-action:** Immediate permanent ban
- **Legal:** Report to authorities if needed

---

### 9. **Poor Rating Behavior** 🟡
**New Condition:** Consistently gives unfair low ratings

**Triggers:**
- Gives <3 stars to >50% of drivers
- Pattern of 1-star ratings with no comment
- Ratings significantly lower than other passengers
- Uses ratings as retaliation tool
- **Severity:** Medium
- **Auto-action:** Rating weight reduced
- **Warning:** Notification about fair rating practices

---

### 10. **Payment Method Issues** 🟠
**New Condition:** Problematic payment patterns

**Triggers:**
- Multiple failed payment methods
- Expired cards not updated
- Insufficient funds repeatedly
- Chargebacks/disputes after successful trips
- **Severity:** Medium-High
- **Auto-action:** Require cash-only until resolved
- **Escalation:** Account freeze if not fixed in 7 days

---

### 11. **Safety Violations** 🔴
**New Condition:** Endangering driver or public safety

**Triggers:**
- Intoxication during pickup
- Bringing prohibited items (weapons, illegal substances)
- Requesting illegal activities
- Endangering driver (grabbing wheel, fighting)
- Not wearing seatbelt repeatedly
- **Severity:** Critical
- **Auto-action:** Immediate permanent ban
- **Legal:** Police report if necessary

---

### 12. **Group Booking Violations** 🟡
**New Condition:** Exceeding passenger limits

**Triggers:**
- Shows up with more passengers than booked
- Refuses to split fare with ride-pool passengers
- Brings excessive luggage without notification
- Attempts to add passengers after booking confirmed
- **Severity:** Medium
- **Auto-action:** Additional passenger fee
- **Warning:** Future bookings may be restricted

---

### 13. **Late Arrivals** 🟡
**New Condition:** Customer always late to pickup

**Triggers:**
- Late to pickup >50% of the time
- Makes driver wait >5 minutes regularly
- No response when driver arrives
- Pattern of "just 5 more minutes"
- **Severity:** Medium
- **Auto-action:** Waiting time charges apply
- **Escalation:** Automatic cancellation after 5 min wait

---

### 14. **Discount Abuse** 🟠
**New Condition:** Misusing discount programs

**Triggers:**
- Expired discount ID still being used
- Borrowed/fake discount documents
- Claims discount after trip completed
- Transfers discount to non-eligible person
- **Severity:** High
- **Auto-action:** Discount revoked + investigation
- **Penalty:** Account suspension + possible legal action

---

### 15. **Ride-Pool Misconduct** 🟡
**New Condition:** Issues with shared rides

**Triggers:**
- Aggressive toward other passengers
- Refuses to share ride after booking pool option
- Excessive noise/disturbance
- Inappropriate behavior in shared space
- **Severity:** Medium
- **Auto-action:** Pool privilege revoked
- **Restriction:** Solo rides only for 30 days

---

## 📊 FLAG SEVERITY LEVELS

### 🔴 CRITICAL (Score: 100)
- Immediate action required
- Account suspension
- Manual admin review
- Examples: Non-payment, abuse, safety violations, fraud

### 🟠 HIGH (Score: 75)
- Urgent attention needed
- Warning issued
- Monitoring activated
- Examples: High cancellation rate, contribution issues, multiple complaints

### 🟡 MEDIUM (Score: 50)
- Notable concern
- Educational notification
- Tracked for patterns
- Examples: Late arrivals, wrong pins, minor disputes

### 🟢 LOW (Score: 25)
- Minor issue
- Automated reminder
- No immediate action
- Examples: First-time offense, correctable mistakes

---

## 🎯 FLAG SCORING SYSTEM

### Point Accumulation
Each violation adds points to account:
- Critical: +100 points
- High: +75 points
- Medium: +50 points
- Low: +25 points

### Point Decay
Points decrease over time if behavior improves:
- Good behavior: -10 points per week
- Excellent behavior: -20 points per week
- Points expire after 180 days

### Thresholds
- **0-50 points:** Good standing ✅
- **51-150 points:** Monitored 👀
- **151-300 points:** Restricted ⚠️
- **301+ points:** Suspended 🚫

---

## 🔄 AUTOMATED WORKFLOWS

### Driver Workflow
```
1. System detects condition → 
2. Calculate severity → 
3. Add flag to driver record → 
4. Send notification to driver → 
5. Notify admin → 
6. Apply automatic action → 
7. Set review date → 
8. Monitor for improvement
```

### Customer Workflow
```
1. Incident reported or detected → 
2. Verify with data/proof → 
3. Calculate severity → 
4. Add flag to customer record → 
5. Send notification to customer → 
6. Apply restrictions if needed → 
7. Set resolution deadline → 
8. Track compliance
```

---

## 🗄️ DATABASE STRUCTURE

### Driver Flags
```javascript
{
  "drivers": {
    "driver-id-123": {
      // ... existing fields ...
      "flagScore": 75,
      "flagStatus": "monitored", // good | monitored | restricted | suspended
      "flags": {
        "flag-id-1": {
          "flagId": "flag-id-1",
          "type": "LOW_CONTRIBUTIONS",
          "severity": "high",
          "points": 75,
          "timestamp": 1730534400000,
          "status": "active", // active | resolved | expired
          "details": {
            "averageContribution": 1500,
            "driverContribution": 600,
            "percentage": 40,
            "period": "weekly"
          },
          "actions": [
            {
              "action": "WARNING_SENT",
              "timestamp": 1730534400000,
              "adminId": "admin-123"
            }
          ],
          "reviewDate": 1731139200000, // 7 days later
          "resolvedDate": null,
          "notes": "Driver notified via SMS and in-app notification"
        }
      },
      "flagHistory": [
        // Archive of resolved flags
      ]
    }
  }
}
```

### Customer Flags
```javascript
{
  "users": {
    "customer-id-456": {
      // ... existing fields ...
      "flagScore": 150,
      "flagStatus": "restricted",
      "flags": {
        "flag-id-2": {
          "flagId": "flag-id-2",
          "type": "NO_SHOW",
          "severity": "critical",
          "points": 100,
          "timestamp": 1730534400000,
          "status": "active",
          "details": {
            "noShowCount": 3,
            "totalBookings": 10,
            "noShowRate": 30,
            "lastNoShowDate": 1730448000000
          },
          "reportedBy": {
            "driverId": "driver-id-789",
            "driverName": "Juan Dela Cruz",
            "bookingId": "booking-id-999"
          },
          "actions": [
            {
              "action": "PREPAYMENT_REQUIRED",
              "timestamp": 1730534400000,
              "expiryDate": 1733126400000 // 30 days
            }
          ],
          "resolution": {
            "status": "pending",
            "customerResponse": null,
            "adminDecision": null
          }
        }
      }
    }
  }
}
```

### Flag Types Reference
```javascript
{
  "flagTypes": {
    // Driver Flags
    "LOW_CONTRIBUTIONS": {
      "category": "driver",
      "severity": "high",
      "points": 75,
      "autoActions": ["WARNING", "MONITORING"],
      "escalationDays": 14
    },
    "INACTIVE_ACCOUNT": {
      "category": "driver",
      "severity": "medium",
      "points": 50,
      "autoActions": ["NOTIFICATION"],
      "escalationDays": 30
    },
    // ... more driver flags ...
    
    // Customer Flags
    "NO_SHOW": {
      "category": "customer",
      "severity": "critical",
      "points": 100,
      "autoActions": ["PREPAYMENT_REQUIRED"],
      "escalationThreshold": 5
    },
    "NON_PAYMENT": {
      "category": "customer",
      "severity": "critical",
      "points": 100,
      "autoActions": ["ACCOUNT_FREEZE"],
      "requiresProof": true
    }
    // ... more customer flags ...
  }
}
```

---

## 📱 UI COMPONENTS NEEDED

### 1. Flagged Accounts Module (New Page)
**Location:** Admin Website → Flagged Accounts

**Features:**
- List of all flagged drivers and customers
- Filter by: severity, type, status, date
- Search by name, ID, phone
- Bulk actions (resolve, escalate, dismiss)
- Export flagged accounts report

### 2. Flag Details Modal
- Complete flag information
- Timeline of events
- Evidence/proof attached
- Admin actions history
- Resolution options
- Add notes/comments

### 3. Driver/Customer Profile Flag Section
- Flag badges on profile
- Flag score display
- Active flags list
- Flag history
- Quick actions (resolve, escalate)

### 4. Dashboard Widgets
- Total flagged accounts count
- Critical flags requiring attention
- Flags by type chart
- Trending issues graph

### 5. Notification System Integration
- Admin notifications for new critical flags
- Driver/customer notifications when flagged
- Escalation reminders
- Resolution confirmations

---

## 🔔 NOTIFICATION TEMPLATES

### For Drivers

**Low Contributions Warning:**
```
⚠️ CONTRIBUTION ALERT
Your recent contributions (₱600) are below the weekly average (₱1,500).
Please ensure regular contributions to maintain good standing.
Review period: 7 days
```

**Inactive Account Notice:**
```
👋 WE MISS YOU!
Your account has been inactive for 7 days.
Please log in or contact admin if you need assistance.
Account will be reviewed in 23 days.
```

**High Cancellation Rate:**
```
🚫 CANCELLATION WARNING
You have cancelled 15% of your accepted bookings.
High cancellation rates affect customer experience.
Next cancellation may result in queue suspension.
```

### For Customers

**No-Show Warning:**
```
⚠️ BOOKING ALERT
You missed your last scheduled pickup.
Multiple no-shows may require prepayment for future bookings.
No-shows this month: 2/5
```

**Non-Payment Report:**
```
🚨 PAYMENT REQUIRED
A driver has reported non-payment for your recent trip.
Booking ID: #XYZ123
Amount due: ₱150
Please settle within 24 hours to avoid account suspension.
```

**Abusive Behavior Report:**
```
⚠️ CONDUCT VIOLATION
We received a report of inappropriate behavior during your trip.
This is being reviewed by our admin team.
Our community guidelines require respectful treatment of all drivers.
```

---

## 🛠️ IMPLEMENTATION CHECKLIST

### Phase 1: Database Setup (Week 1)
- [ ] Create flag types configuration
- [ ] Add flag fields to drivers collection
- [ ] Add flag fields to users collection
- [ ] Create flagHistory archive structure
- [ ] Set up indexes for queries

### Phase 2: Backend Logic (Week 2-3)
- [ ] Build flag detection functions
- [ ] Create auto-flagging triggers
- [ ] Implement scoring system
- [ ] Build notification system
- [ ] Create admin action handlers

### Phase 3: Admin UI (Week 4-5)
- [ ] Create Flagged Accounts page
- [ ] Build flag details modal
- [ ] Add flag indicators to profiles
- [ ] Create dashboard widgets
- [ ] Implement bulk actions

### Phase 4: Mobile App Integration (Week 6)
- [ ] Add flag notifications
- [ ] Show flag status in profile
- [ ] Implement restrictions (booking limits, etc.)
- [ ] Add appeal/response mechanism
- [ ] Update booking flow for flagged accounts

### Phase 5: Testing & Rollout (Week 7-8)
- [ ] Unit testing all flag conditions
- [ ] Integration testing
- [ ] Admin training
- [ ] Soft launch with monitoring
- [ ] Full deployment
- [ ] Documentation and SOPs

---

## 📈 MONITORING & ANALYTICS

### Key Metrics to Track
1. Total flagged accounts (drivers vs customers)
2. Flags by type distribution
3. Resolution rate and time
4. Re-offense rate
5. Appeal success rate
6. False positive rate
7. Impact on bookings/revenue
8. Customer satisfaction correlation

### Reports
- **Daily:** Critical flags requiring immediate action
- **Weekly:** New flags, resolved flags, trending issues
- **Monthly:** Comprehensive flag analytics, pattern analysis
- **Quarterly:** System effectiveness review, policy updates

---

## ⚖️ ADMIN CAPABILITIES

### Actions Admins Can Take
1. **Review Flag:** View details and evidence
2. **Resolve Flag:** Mark as resolved with notes
3. **Escalate Flag:** Increase severity
4. **Dismiss Flag:** Mark as false positive
5. **Add Note:** Comment on flag
6. **Override System:** Manual flag creation/removal
7. **Set Custom Actions:** Apply specific restrictions
8. **Appeal Review:** Handle customer/driver appeals
9. **Bulk Operations:** Process multiple flags
10. **Export Report:** Generate flag reports

---

## 🎓 TRAINING & EDUCATION

### For Drivers
- Welcome kit with behavior guidelines
- Monthly performance reports
- Flag prevention tips
- Appeal process explanation
- Success stories of improved drivers

### For Customers
- Booking etiquette guide
- No-show impact explanation
- How to set correct PINs
- Community guidelines
- Feedback mechanism

---

## 🚀 BENEFITS

### For TODA Administration
- Automated problematic account detection
- Data-driven decision making
- Reduced manual monitoring
- Improved service quality
- Better accountability
- Legal protection (documented evidence)

### For Good Drivers
- Fair competition
- Recognition of good performance
- Protection from problematic customers
- Transparent evaluation system

### For Good Customers
- Better service quality
- Reliable drivers
- Safer experience
- Fair pricing
- Accountable drivers

---

## 📋 NEXT STEPS

1. **Review & Approve:** Admin team reviews this specification
2. **Prioritize Flags:** Decide which flags to implement first
3. **Customize Thresholds:** Adjust numbers based on TODA needs
4. **Create UI Mockups:** Design the admin interface
5. **Build Database:** Set up Firebase structure
6. **Develop Backend:** Implement detection logic
7. **Build Frontend:** Create admin pages
8. **Test System:** Thorough testing with sample data
9. **Train Admins:** Educate team on using system
10. **Deploy:** Roll out in phases

---

**Would you like me to start implementing any specific part of this system?**
