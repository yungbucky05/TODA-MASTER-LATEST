# 🚩 Flagged Accounts - Quick Reference Card

## 🎯 Quick Actions

| Action | Button | Result |
|--------|--------|--------|
| **Run Detection** | 🔍 Run Auto-Detection | Scans all accounts for issues |
| **View Details** | View Details | Opens flag information modal |
| **Resolve** | Resolve | Marks flag as fixed, removes points |
| **Escalate** | Escalate | Increases severity, adds 25 points |
| **Dismiss** | Dismiss | Marks as false positive |
| **Export** | 📥 Export Report | Downloads CSV file |

---

## 📊 Severity Levels

| Level | Points | Color | When to Use |
|-------|--------|-------|-------------|
| 🚨 **Critical** | 100 | Red | Immediate action required |
| ⚠️ **High** | 75 | Orange | Urgent attention needed |
| 👀 **Medium** | 50 | Yellow | Monitor closely |
| ℹ️ **Low** | 25 | Blue | Minor issue |

---

## 🎭 Account Status

| Score | Status | Action Required |
|-------|--------|-----------------|
| 0-50 | ✅ Good | None |
| 51-150 | 👀 Monitored | Watch behavior |
| 151-300 | ⚠️ Restricted | Limit privileges |
| 301+ | 🚫 Suspended | Account frozen |

---

## 🚗 Driver Flags

| Flag | Icon | Trigger | Points |
|------|------|---------|--------|
| Low Contributions | 💰 | <50% of average | 75 |
| Inactive Account | 💤 | No activity 7+ days | 50 |
| High Cancellations | 🚫 | >15% cancel rate | 75 |
| Complaints | 😡 | Multiple reports | 100 |
| RFID Issues | 🏷️ | Repeated problems | 50 |

---

## 👥 Customer Flags

| Flag | Icon | Trigger | Points |
|------|------|---------|--------|
| No-Show Pattern | 👻 | >20% no-show rate | 100 |
| Non-Payment | 💸 | Reported by driver | 100 |
| Wrong PIN | 📍 | Incorrect locations | 50 |
| Abusive Behavior | 🤬 | Driver reports abuse | 100 |
| Excessive Cancels | ❌ | >25% cancel rate | 75 |

---

## 🔍 Filters

**Account Type:**
- All Accounts
- Drivers Only
- Customers Only

**Severity:**
- All / Critical / High / Medium / Low

**Status:**
- Active / All / Resolved / Expired

**Search:**
- Name / Phone / ID

---

## ⚡ Keyboard Shortcuts

| Key | Action |
|-----|--------|
| `Ctrl + F` | Focus search |
| `Esc` | Close modal |
| `Enter` | Confirm action |

---

## 📋 Decision Tree

```
New Flag Detected
    ↓
Is it valid?
    ├─ Yes → Is it critical?
    │         ├─ Yes → Act immediately
    │         └─ No → Schedule review
    └─ No → Dismiss flag

Review Complete
    ↓
Is issue resolved?
    ├─ Yes → Resolve flag
    └─ No → Is it getting worse?
              ├─ Yes → Escalate
              └─ No → Monitor
```

---

## 🔔 Response Times

| Severity | Response Time | Action |
|----------|---------------|--------|
| Critical | 24 hours | Contact + Action |
| High | 48 hours | Warning sent |
| Medium | 1 week | Info sent |
| Low | 2 weeks | Optional notice |

---

## 📞 What to Do

### For Critical Flags:
1. ✅ Review details immediately
2. ✅ Contact account holder
3. ✅ Document conversation
4. ✅ Take appropriate action
5. ✅ Follow up in 24 hours

### For High Flags:
1. ✅ Review within 48 hours
2. ✅ Send warning notification
3. ✅ Set 7-day monitoring period
4. ✅ Re-evaluate after period

### For Medium/Low Flags:
1. ✅ Review when convenient
2. ✅ Send info (optional)
3. ✅ Monitor for patterns
4. ✅ May auto-resolve

---

## 🛠️ Troubleshooting

| Problem | Solution |
|---------|----------|
| No flags showing | Run auto-detection |
| Can't resolve flag | Refresh page, try again |
| Wrong account flagged | Review details, dismiss if false |
| Badge not updating | Check Firebase connection |

---

## 📊 Example Scenarios

### Scenario 1: Low Contributions
```
Driver: Juan Dela Cruz
Average: ₱1,500/week
Driver's: ₱600/week (40%)
Action: Contact driver, understand situation
Options: 
  - Personal issue → Give time, monitor
  - Vehicle problem → Assist if possible
  - Not trying → Warning, escalate if continues
```

### Scenario 2: No-Show Customer
```
Customer: Maria Santos
Bookings: 10
No-Shows: 3 (30%)
Action: Contact customer immediately
Options:
  - Valid reasons → Note in system, monitor
  - Forgot → Remind of impact, warn
  - Habitual → Require prepayment
```

### Scenario 3: Inactive Driver
```
Driver: Pedro Reyes
Last Active: 17 days ago
Action: Contact to check status
Options:
  - On leave → Note in system, resolve when back
  - Quit → Deactivate account
  - Forgot → Remind, resolve when active
```

---

## ✅ Daily Checklist

- [ ] Check critical flags count
- [ ] Review new flags from yesterday
- [ ] Follow up on escalated cases
- [ ] Run weekly auto-detection (Mondays)
- [ ] Export monthly report (1st of month)

---

## 📈 Success Metrics

Track these monthly:
- Total flags created
- Flags resolved vs escalated
- Average resolution time
- Repeat offenders
- False positive rate

---

## 🎓 Remember

✅ **Fair**: Investigate before acting  
✅ **Fast**: Address critical flags quickly  
✅ **Firm**: Enforce rules consistently  
✅ **Flexible**: Consider special circumstances  
✅ **Follow-up**: Monitor resolved cases  

---

**Last Updated:** November 1, 2025  
**Version:** 1.0  
**Module:** Flagged Accounts
