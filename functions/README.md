# TODA Cloud Function: resetPassword

This project adds a callable Cloud Function named `resetPassword` (region: `asia-southeast1`) used by the Android app to reset a user's password after verifying ownership via SMS (Phone Auth OTP).

What it does:
- Verifies the caller is authenticated (via temporary Phone Auth in the app)
- Maps the local PH phone format (09XXXXXXXXX) to the app's email scheme: `09XXXXXXXXX@toda.local`
- Updates the Firebase Auth password securely using the Admin SDK

Prereqs:
- Node.js 18+
- Firebase CLI
- Access to the Firebase project `toda-contribution-system`

Windows (cmd.exe) deployment steps:

1) Install Firebase CLI (if needed)
```
npm install -g firebase-tools
```

2) Log in to your Google account
```
firebase login
```

3) From the repo root, install function deps
```
cd functions
npm install
cd ..
```

4) Ensure the project alias is set to `toda-contribution-system` (already in .firebaserc). If needed:
```
firebase use toda-contribution-system
```

5) Deploy only this function
```
firebase deploy --only functions:resetPassword
```

Notes:
- The Android app will first call the default region, then fall back to `asia-southeast1`, so deploying in `asia-southeast1` is sufficient.
- If you see permission/billing errors, enable billing for the Firebase project or check IAM roles for the deployer. Callable functions typically run fine on the Spark plan, but Admin SDK usage requires the function runtime (provided by Firebase) to be enabled in your project.
- After deploy, test the Forgot Password flow in the app. If the OTP verification succeeds, the password should update and you should be able to sign in with the new password.

