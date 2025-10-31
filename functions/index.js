// Cloud Functions entrypoint for TODA app
const functions = require('firebase-functions');
const admin = require('firebase-admin');

// Initialize Admin SDK
try {
  admin.initializeApp();
} catch (e) {
  // App may already be initialized in emulator/multiple loads
}

// Helper: normalize PH local (09XXXXXXXXX) to E.164 +63 format
function toE164(local) {
  if (!local) return null;
  const clean = String(local).replace(/\D/g, '');
  if (clean.length === 11 && clean.startsWith('09')) {
    return `+63${clean.substring(1)}`;
  }
  if (clean.startsWith('63') && clean.length === 12) {
    return `+${clean}`;
  }
  if (String(local).startsWith('+')) return String(local);
  return null;
}

exports.resetPassword = functions
  .region('asia-southeast1')
  .https.onCall(async (data, context) => {
    // Require an authenticated caller (the app signs in with PhoneAuth OTP temporarily)
    if (!context.auth) {
      throw new functions.https.HttpsError(
        'unauthenticated',
        'You must be signed in with a verified phone number to reset your password.'
      );
    }

    const newPassword = (data?.newPassword || '').toString();
    const localPhone = (data?.phoneNumber || '').toString().replace(/\D/g, '');
    const providedE164 = data?.phoneNumberE164 ? String(data.phoneNumberE164) : null;

    if (!newPassword || newPassword.length < 6) {
      throw new functions.https.HttpsError('invalid-argument', 'Password must be at least 6 characters.');
    }
    if (!localPhone || localPhone.length !== 11) {
      throw new functions.https.HttpsError('invalid-argument', 'Invalid phone number format.');
    }

    // Validate that the caller's verified phone matches the target phone (defense-in-depth)
    const callerPhone = context.auth.token?.phone_number || null;
    const targetE164 = providedE164 || toE164(localPhone);
    if (callerPhone && targetE164 && callerPhone !== targetE164) {
      throw new functions.https.HttpsError('permission-denied', 'Phone number mismatch.');
    }

    // Our app uses email = `${phoneNumber}@toda.local` for Firebase Auth accounts
    const derivedEmail = `${localPhone}@toda.local`;

    try {
      // Preferred path: update the email/password account directly
      const userRecord = await admin.auth().getUserByEmail(derivedEmail);
      await admin.auth().updateUser(userRecord.uid, { password: newPassword });
      return { ok: true, uid: userRecord.uid, method: 'byEmail' };
    } catch (err) {
      // If not found by email, we attempt a cautious fallback: if a phone-auth account
      // exists and has an email linked, update that. Otherwise, report not-found.
      if (err?.code === 'auth/user-not-found') {
        try {
          if (targetE164) {
            const phoneUser = await admin.auth().getUserByPhoneNumber(targetE164);
            if (phoneUser?.email) {
              await admin.auth().updateUser(phoneUser.uid, { password: newPassword });
              return { ok: true, uid: phoneUser.uid, method: 'byPhoneLinkedEmail' };
            }
          }
        } catch (_) {
          // ignore and fall through to not-found
        }
        throw new functions.https.HttpsError('not-found', 'Account not found for this phone number.');
      }
      // Unexpected error
      throw new functions.https.HttpsError('internal', err?.message || 'Failed to reset password');
    }
  });

