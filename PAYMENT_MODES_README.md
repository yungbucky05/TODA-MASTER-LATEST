# Payment Modes Documentation

## Overview

The TODA system supports two payment modes for drivers to manage their P5 per-trip contribution to the organization. Each mode offers different flexibility and payment schedules to accommodate various driver preferences.

---

## Payment Modes

### 1. **Pay Every Trip** (Hardware-Integrated Mode)
Drivers pay P5 **BEFORE** going online and joining the queue.

#### How It Works:
- **Payment Timing**: Driver pays P5 contribution **BEFORE joining queue** (upfront payment)
- **Hardware Flow**: **Tap RFID → Pay P5 → Auto-join queue → Go online → Can receive bookings**
- **App Flow**: Manual join queue (payment verification required)
- **Balance Tracking**: Balance remains at **₱0.00** at all times (no accumulation)
- **Online Status**: Driver can go online **after paying P5** (payment enables queue access)
- **Best For**: Drivers who use the hardware system; ensures payment before service

#### Process Flow:
```
1. Driver taps RFID card at terminal OR manually joins queue in app
2. System charges P5 (hardware accepts coin/payment)
3. Payment confirmed → Driver added to queue
4. Driver goes online → Can receive bookings
5. Driver completes trip
6. Trip contribution recorded in history (P5 already paid)
7. Balance stays at ₱0.00
8. Driver removed from queue after trip completion
9. Repeat: Tap RFID → Pay P5 → Join queue again
```

#### Key Points:
✅ **Upfront Payment**: P5 paid BEFORE joining queue (not after trip)  
✅ **Hardware Integration**: Works seamlessly with RFID terminal system  
✅ **No Debt**: Payment is prepaid, no balance accumulation  
✅ **Queue Access Control**: Payment enables queue entry  
✅ **Auto Queue Join**: Hardware automatically adds driver to queue after payment  

#### Hardware Terminal Flow:
```
┌─────────────────────────────────────────────┐
│  Driver taps RFID card at terminal          │
│  ↓                                           │
│  System verifies driver & payment mode      │
│  ↓                                           │
│  Terminal prompts: "Insert P5 coin"         │
│  ↓                                           │
│  Driver inserts P5 coin                     │
│  ↓                                           │
│  Payment confirmed ✓                        │
│  ↓                                           │
│  Driver automatically added to queue        │
│  ↓                                           │
│  Driver status: ONLINE - Can receive trips  │
└─────────────────────────────────────────────┘
```

#### Benefits:
✅ Ensures payment before service  
✅ No debt accumulation  
✅ Integrates with existing hardware  
✅ Automatic queue management  
✅ Simple for drivers using terminal  
✅ Clear payment record per trip  

---

### 2. **Pay Later** (Flexible Mode)
Drivers accumulate P5 per trip and pay at the end of the day or when convenient.

#### How It Works:
- **Payment Timing**: Driver pays accumulated balance at **end of day or when convenient**
- **Queue Join**: Can join queue **without upfront payment**
- **Balance Tracking**: **P5 is added to balance AFTER each completed trip**
- **Online Status**: 
  - ✅ Can go online **same day** even with outstanding balance
  - ❌ **Cannot go online next day** until balance is settled
- **Best For**: Drivers who want flexibility and prefer batch payments

#### Process Flow:
```
1. Driver joins queue (no upfront payment needed)
2. Driver goes online → Can receive bookings
3. Driver completes trip
4. Status changes to "COMPLETED"
5. System adds P5 to driver's balance (e.g., ₱0 → ₱5)
6. Balance updates in real-time
7. Driver removed from queue after trip completion
8. Driver continues working (same day) - can join queue again
9. At end of day or next day:
   - If balance > 0 and it's a new day → Driver CANNOT go online
   - Driver must settle balance to continue
```

#### Balance Update Example:
```
Trip 1 Complete: ₱0.00 → ₱5.00
Trip 2 Complete: ₱5.00 → ₱10.00
Trip 3 Complete: ₱10.00 → ₱15.00
Payment Made:    ₱15.00 → ₱0.00 ✅ Can go online next day
```

#### Benefits:
✅ No upfront payment needed  
✅ Flexible payment schedule  
✅ Work full day before paying  
✅ Batch payment convenience  
✅ Clear running balance tracker  
✅ Good for app-only drivers  

---

## Key Differences

| Aspect | Pay Every Trip | Pay Later |
|--------|---------------|-----------|
| **Payment Time** | BEFORE joining queue | AFTER trip completion |
| **Upfront Cost** | P5 per queue join | None |
| **Balance** | Always ₱0.00 | Accumulates daily |
| **Hardware Integration** | ✅ Full support | ⚠️ Manual tracking |
| **Queue Entry** | After P5 payment | Immediate |
| **Next Day Restriction** | None (if paid) | Must settle balance |
| **Best For** | Hardware users | App-only users |

---

## Setting Payment Mode

### First-Time Setup
When a driver first logs in, they are prompted to select their preferred payment mode:

1. **Payment Mode Selection Screen** appears
2. Driver chooses:
   - **Pay Every Trip** - Hardware system: Pay P5 upfront to join queue
   - **Pay Later** - Accumulate and pay at end of day
3. Selection is saved to driver profile
4. For Pay Every Trip: Driver instructed to use hardware terminal
5. For Pay Later: Driver can immediately join queue from app

### Changing Payment Mode
Drivers can change their payment mode anytime from the **Contributions** tab:

1. Navigate to **Contributions** tab
2. Tap **Change Payment Mode** button
3. Select new mode
4. Confirm change
5. New mode takes effect immediately

---

## Driver Workflow by Mode

### Pay Every Trip Workflow (Hardware System)

```
┌─────────────────────────────────────────────────┐
│                                                  │
│  START                                           │
│    ↓                                            │
│  Tap RFID at Terminal                           │
│    ↓                                            │
│  Pay P5 (Coin Insertion)                        │
│    ↓                                            │
│  Auto-Join Queue                                │
│    ↓                                            │
│  Status: ONLINE                                 │
│    ↓                                            │
│  Receive Booking                                │
│    ↓                                            │
│  Accept Booking (Status: ACCEPTED)              │
│    ↓                                            │
│  Complete Trip (Status: COMPLETED)              │
│    ↓                                            │
│  P5 Contribution Recorded in History            │
│  (Already Paid - No Balance Change)             │
│    ↓                                            │
│  Remove from Queue                              │
│    ↓                                            │
│  Back to START (Tap RFID again for next trip)   │
│                                                  │
└─────────────────────────────────────────────────┘
```

**Key Points:**
- ✅ P5 paid BEFORE joining queue
- ✅ Hardware terminal handles payment
- ✅ Automatic queue addition after payment
- ✅ Balance always ₱0.00 (prepaid model)
- ✅ Driver removed from queue AFTER trip completion
- ✅ Must tap RFID again for next queue entry

---

### Pay Later Workflow

```
┌─────────────────────────────────────────────────┐
│                                                  │
│  START                                           │
│    ↓                                            │
│  Join Queue (App) - No Payment Required         │
│    ↓                                            │
│  Status: ONLINE                                 │
│    ↓                                            │
│  Receive Booking                                │
│    ↓                                            │
│  Accept Booking (Status: ACCEPTED)              │
│    ↓                                            │
│  Complete Trip (Status: COMPLETED)              │
│    ↓                                            │
│  Balance += P5 (e.g., ₱5.00 → ₱10.00)          │
│    ↓                                            │
│  Balance Updates in Real-Time                   │
│    ↓                                            │
│  Remove from Queue                              │
│    ↓                                            │
│  Can Rejoin Queue? ─────┐                       │
│                         ↓                       │
│              ┌─── Same Day? ───┐               │
│              │                  │               │
│             YES                NO              │
│              │                  │               │
│        ✅ Can Rejoin      Balance > 0?          │
│              │                  │               │
│              │         ┌────────┴────────┐     │
│              │        YES               NO      │
│              │         │                 │      │
│              │   ❌ Must Pay         ✅ Can     │
│              │    Balance            Rejoin    │
│              │         │                 │      │
│              │         ↓                 ↓      │
│              └──── Pay Balance ─────────┘      │
│                         │                       │
│                         ↓                       │
│                  Back to START                  │
│                                                  │
└─────────────────────────────────────────────────┘
```

**Key Points:**
- ✅ No upfront payment
- ✅ P5 added to balance AFTER each trip
- ✅ Real-time balance updates in UI
- ✅ Can continue working same day with balance
- ✅ Must settle before next day
- ✅ Driver removed from queue AFTER trip completion

---

