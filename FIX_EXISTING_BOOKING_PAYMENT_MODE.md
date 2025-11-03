/**
 * Script to add payment mode to existing bookings that don't have it
 * 
 * For booking: -Od6s1kOWuEkUnWC3AfR
 * Driver: aQAyEHQ6prhOqs7layjLwLvW1g43
 * 
 * Instructions:
 * 1. Go to Firebase Console -> Realtime Database
 * 2. Navigate to: drivers/aQAyEHQ6prhOqs7layjLwLvW1g43
 * 3. Copy the value of the "paymentMode" field
 * 4. Navigate to: bookings/-Od6s1kOWuEkUnWC3AfR
 * 5. Add a new field: "paymentMode" with the value from step 3
 * 6. Also add it to: bookingIndex/-Od6s1kOWuEkUnWC3AfR/paymentMode
 * 
 * Example:
 * If driver's paymentMode is "pay_later", then:
 * - bookings/-Od6s1kOWuEkUnWC3AfR/paymentMode: "pay_later"
 * - bookingIndex/-Od6s1kOWuEkUnWC3AfR/paymentMode: "pay_later"
 * 
 * After doing this, the payment mode indicator will appear in the driver app.
 */

