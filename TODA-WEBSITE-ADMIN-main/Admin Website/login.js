// Import Firebase SDKs
import { initializeApp } from "https://www.gstatic.com/firebasejs/11.0.0/firebase-app.js";
import { getDatabase, ref, query, orderByChild, equalTo, get } from "https://www.gstatic.com/firebasejs/11.0.0/firebase-database.js";
import { getAuth, signInWithEmailAndPassword, signInWithPhoneNumber, RecaptchaVerifier } from "https://www.gstatic.com/firebasejs/11.0.0/firebase-auth.js";

// Firebase config
const firebaseConfig = {
  apiKey: "AIzaSyA9ADVig4CiO2Y3ELl3unzXajdzxCgRxHI",
  authDomain: "toda-contribution-system.firebaseapp.com",
  databaseURL: "https://toda-contribution-system-default-rtdb.asia-southeast1.firebasedatabase.app",
  projectId: "toda-contribution-system",
  storageBucket: "toda-contribution-system.firebasestorage.app",
  messagingSenderId: "536068566619",
  appId: "1:536068566619:web:ff7cc576e59b76ae58997e"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const db = getDatabase(app);
const auth = getAuth(app);

// Password hashing utility (same as register.js)
async function hashPassword(password, salt) {
  const encoder = new TextEncoder();
  const passwordData = encoder.encode(password);
  const saltData = encoder.encode(salt);
  
  const combinedData = new Uint8Array(passwordData.length + saltData.length);
  combinedData.set(passwordData);
  combinedData.set(saltData, passwordData.length);
  
  const hashBuffer = await crypto.subtle.digest('SHA-256', combinedData);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  const hashHex = hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
  
  return hashHex;
}

// Verify password against stored hash
async function verifyPassword(password, storedHash) {
  try {
    // Check if stored hash is in format "salt:hash"
    if (storedHash.includes(':')) {
      const [salt, hash] = storedHash.split(':');
      const computedHash = await hashPassword(password, salt);
      return computedHash === hash;
    } else {
      // Backward compatibility: if it's a plain password (old format), compare directly
      // This allows existing users to still login, but they should update their password
      return storedHash === password;
    }
  } catch (error) {
    console.error('Password verification error:', error);
    return false;
  }
}

// Test Firebase connection (non-blocking, informational only)
async function testFirebaseConnection() {
  try {
    console.log('🔌 Testing Firebase connection...');
    // Firebase is initialized, connection will be tested during actual login
    // Just verify the database instance is available
    if (db) {
      console.log('✅ Firebase database instance ready');
      return true;
    }
    return false;
  } catch (error) {
    console.error('❌ Firebase initialization error:', error);
    return false;
  }
}

// Login functionality
document.addEventListener('DOMContentLoaded', async function() {
  const loginForm = document.getElementById('loginForm');
  const phoneNumberInput = document.getElementById('phoneNumber');
  const passwordInput = document.getElementById('password');
  const togglePasswordBtn = document.getElementById('togglePassword');
  const loginBtn = document.getElementById('loginBtn');
  const errorMessage = document.getElementById('errorMessage');
  const rememberMeCheckbox = document.getElementById('rememberMe');
  let messageHideTimeout = null;

  // Check if user just logged out and show notification
  if (sessionStorage.getItem('justLoggedOut') === 'true') {
    sessionStorage.removeItem('justLoggedOut');
    showSuccess('You have been logged out successfully');
  }

  // Test Firebase connection on page load (informational only)
  testFirebaseConnection().then(isConnected => {
    if (isConnected) {
      console.log('✅ Firebase ready');
    } else {
      console.warn('⚠️ Firebase initialization check failed, but login will still attempt connection');
    }
  }).catch(err => {
    console.error('Connection test error:', err);
    // Don't block login - let it try anyway
  });

  // Toggle password visibility
  togglePasswordBtn.addEventListener('click', function() {
    const type = passwordInput.getAttribute('type') === 'password' ? 'text' : 'password';
    passwordInput.setAttribute('type', type);
    togglePasswordBtn.textContent = type === 'password' ? '👁️' : '🙈';
  });

  // Check if user is already logged in
  checkAuthStatus();

  // Handle form submission
  loginForm.addEventListener('submit', async function(e) {
    e.preventDefault();
    
    const phoneNumber = phoneNumberInput.value.trim();
    const password = passwordInput.value;
    const rememberMe = rememberMeCheckbox.checked;

    // Validate inputs
    if (!phoneNumber) {
      showError('Please enter your phone number');
      return;
    }
    
    // Password is now required
    if (!password || password.trim() === '') {
      showError('Please enter your password');
      return;
    }

    // Validate phone number format
    const cleanPhone = phoneNumber.replace(/\D/g, ''); // Remove non-digits for validation
    
    if (cleanPhone.length < 10 || cleanPhone.length > 15) {
      showError('Please enter a valid phone number (10-15 digits)');
      return;
    }

    // Show loading state
    setLoadingState(true);

    try {
      // Simulate API call delay (replace with actual authentication)
      await authenticateUser(phoneNumber, password, rememberMe);
      
      // Success - redirect to dashboard
      showSuccess('Login successful! Redirecting...');
      
      setTimeout(() => {
        window.location.href = 'index.html';
      }, 1000);
      
    } catch (error) {
      showError(error.message || 'Invalid phone number. Please check your credentials and ensure you are an admin user.');
      setLoadingState(false);
    }
  });

  // Authentication function - connects to Firebase Realtime Database and Firebase Authentication
  // Verifies admin status in Realtime Database and password in Firebase Authentication
  async function authenticateUser(phoneNumber, password, rememberMe) {
    try {
      // Normalize phone number (remove formatting for comparison)
      const normalizePhone = (phone) => phone.replace(/\D/g, '');
      const normalizedInput = normalizePhone(phoneNumber);
      
      console.log('🔍 Searching for admin with phone:', normalizedInput);
      
      // Query Firebase for admin users
      // Based on your database structure: /users/{userId} with userType: "ADMIN"
      // Priority: 'users' first (your actual structure), then fallback paths
      const possiblePaths = ['users', 'admins', 'credentials'];
      let userFound = null;
      let adminUsersFound = [];
      
      for (const path of possiblePaths) {
        try {
          const usersRef = ref(db, path);
          console.log(`📂 Checking path: /${path}`);
          
          // First, try direct lookup (works without index)
          let allUsersSnapshot;
          try {
            allUsersSnapshot = await get(usersRef);
          } catch (getError) {
            console.error(`❌ Error reading ${path}:`, getError);
            // Check if it's a permission error
            if (getError.code === 'PERMISSION_DENIED' || getError.message.includes('Permission denied')) {
              throw new Error(`Permission denied reading /${path}. Please update Firebase Realtime Database rules to allow read access.`);
            }
            throw getError; // Re-throw other errors
          }
          
          if (allUsersSnapshot.exists()) {
            console.log(`✅ Path /${path} exists and is readable`);
            const allUsers = allUsersSnapshot.val();
            
            // Search through all users for admin with matching phone
            for (const userId in allUsers) {
              const user = allUsers[userId];
              
              // Check if user is admin (matching your structure)
              if (user.userType === 'ADMIN' || user.userType === 'admin') {
                // Phone number field name from your structure: phoneNumber
                const userPhone = user.phoneNumber || user.phone || user.mobile || user.mobileNumber;
                
                if (userPhone) {
                  const normalizedUserPhone = normalizePhone(String(userPhone)); // Ensure it's a string
                  
                  // Debug: log found admin users
                  adminUsersFound.push({
                    userId: userId,
                    phone: normalizedUserPhone,
                    name: user.name || 'Unknown'
                  });
                  
                  // Match phone number
                  if (normalizedUserPhone === normalizedInput) {
                    console.log('✅ Phone number matched! User:', userId);
                    console.log('✅ Admin user found - verifying password with Firebase Authentication');
                    
                    // Try to authenticate with Firebase Auth using email or phone-based email
                    let authSuccess = false;
                    // Try firebaseAuthEmail first (phone-based format if no email), then regular email
                    const authEmail = user.firebaseAuthEmail || user.email;
                    
                    if (authEmail) {
                      // Try Firebase Authentication with email/password
                      try {
                        await signInWithEmailAndPassword(auth, authEmail, password);
                        authSuccess = true;
                        console.log('✅ Password verified via Firebase Authentication');
                      } catch (authError) {
                        console.log('⚠️ Firebase Auth authentication failed, trying alternative methods...', authError.code);
                        // Handle specific Firebase Auth errors
                        if (authError.code === 'auth/user-not-found') {
                          console.log('⚠️ User not found in Firebase Authentication');
                        } else if (authError.code === 'auth/wrong-password' || authError.code === 'auth/invalid-credential') {
                          throw new Error('Invalid password. Please check your credentials.');
                        } else if (authError.code === 'auth/invalid-email') {
                          console.log('⚠️ Invalid email format, trying database password check...');
                        }
                      }
                    }
                    
                    // If no email or email auth didn't work, verify password against stored hash
                    if (!authSuccess) {
                      // Check for hashed password or plain password (for backward compatibility)
                      const storedPasswordHash = user.passwordHash || user.password || user.pin || user.passcode;
                      
                      if (storedPasswordHash) {
                        const isValid = await verifyPassword(password, storedPasswordHash);
                        if (isValid) {
                          authSuccess = true;
                          console.log('✅ Password verified from database');
                          
                          // If old plain password format, log a warning
                          if (!storedPasswordHash.includes(':')) {
                            console.warn('⚠️ User is using old password format. Consider updating password for better security.');
                          }
                        } else {
                          throw new Error('Invalid password. Please check your credentials.');
                        }
                      } else {
                        throw new Error('Invalid password. Please check your credentials.');
                      }
                    }
                    
                    if (authSuccess) {
                      userFound = {
                        ...user,
                        userId: userId,
                        id: userId, // Include id field from your structure
                        phoneNumber: userPhone
                      };
                      break;
                    }
                  }
                }
              }
            }
            
            // Log summary of admin users found
            if (adminUsersFound.length > 0) {
              console.log(`📊 Found ${adminUsersFound.length} admin user(s):`, adminUsersFound);
            }
          }
          
          // If found, break out of loop
          if (userFound) break;
          
          // Try indexed query as fallback (requires Firebase index)
          try {
            const adminQuery = query(usersRef, orderByChild('userType'), equalTo('ADMIN'));
            const snapshot = await get(adminQuery);
            
            if (snapshot.exists() && !userFound) {
              const users = snapshot.val();
              
              // Search through admin users to find matching phone number
              for (const userId in users) {
                const user = users[userId];
                const userPhone = user.phoneNumber || user.phone || user.mobile || user.mobileNumber;
                
                if (userPhone) {
                  const normalizedUserPhone = normalizePhone(String(userPhone)); // Ensure it's a string
                  
                  // Match phone number and verify password
                  if (normalizedUserPhone === normalizedInput) {
                    console.log('✅ Phone number matched! User:', userId);
                    console.log('✅ Admin user found - verifying password with Firebase Authentication');
                    
                    // Try to authenticate with Firebase Auth using email (if available)
                    let authSuccess = false;
                    const userEmail = user.email;
                    
                    if (userEmail) {
                      try {
                        await signInWithEmailAndPassword(auth, userEmail, password);
                        authSuccess = true;
                        console.log('✅ Password verified via email authentication');
                      } catch (authError) {
                        console.log('⚠️ Email authentication failed, trying alternative methods...', authError.code);
                        if (authError.code === 'auth/wrong-password' || authError.code === 'auth/invalid-credential') {
                          throw new Error('Invalid password. Please check your credentials.');
                        }
                      }
                    }
                    
                    if (!authSuccess) {
                      // Check for hashed password or plain password (for backward compatibility)
                      const storedPasswordHash = user.passwordHash || user.password || user.pin || user.passcode;
                      
                      if (storedPasswordHash) {
                        const isValid = await verifyPassword(password, storedPasswordHash);
                        if (isValid) {
                          authSuccess = true;
                          console.log('✅ Password verified from database');
                          
                          // If old plain password format, log a warning
                          if (!storedPasswordHash.includes(':')) {
                            console.warn('⚠️ User is using old password format. Consider updating password for better security.');
                          }
                        } else {
                          throw new Error('Invalid password. Please check your credentials.');
                        }
                      } else {
                        throw new Error('Invalid password. Please check your credentials.');
                      }
                    }
                    
                    if (authSuccess) {
                      userFound = {
                        ...user,
                        userId: userId,
                        id: user.id || userId,
                        phoneNumber: userPhone
                      };
                      break;
                    }
                  }
                }
              }
            }
            
            if (userFound) break;
          } catch (queryError) {
            // If it's a password error, throw it immediately
            if (queryError.message && (queryError.message.includes('Invalid password') || queryError.message.includes('wrong-password') || queryError.message.includes('invalid-credential'))) {
              throw queryError;
            }
            // Index might not be set up, continue with other methods
            console.log(`Indexed query failed for ${path}, using direct lookup (this is fine)`);
          }
          
        } catch (error) {
          // Path doesn't exist or other error, try next path
          console.error(`❌ Path /${path} error:`, error.code || error.message);
          
          // If it's a permission error, throw it immediately (don't try other paths)
          if (error.code === 'PERMISSION_DENIED' || error.message.includes('Permission denied')) {
            throw new Error(`Permission denied accessing /${path}. Please check Firebase Realtime Database security rules. You need read access to the /users path.`);
          }
          
          // If it's a password error, throw it immediately (don't try other paths)
          if (error.message && (error.message.includes('Invalid password') || error.message.includes('wrong-password') || error.message.includes('invalid-credential'))) {
            throw error;
          }
          
          // For other errors, continue to next path
          continue;
        }
      }
      
      if (!userFound) {
        console.log('⚠️ Admin user not found in standard paths, trying root search...');
        
        // Provide helpful error message
        if (adminUsersFound.length > 0) {
          const matchingPhone = adminUsersFound.find(u => u.phone === normalizedInput);
          if (matchingPhone) {
            // If we found the phone but no user, it means password verification failed
            // This could happen if password verification threw an error that was caught elsewhere
            throw new Error('Invalid password. Please check your credentials.');
          } else {
            throw new Error(`Invalid phone number. Found ${adminUsersFound.length} admin user(s) in database. Please check your phone number.`);
          }
        }
        
        // Try searching all nodes (in case structure is flat)
        try {
          const rootRef = ref(db);
          const rootSnapshot = await get(rootRef);
          
          if (rootSnapshot.exists()) {
            const searchNode = (obj, depth = 0) => {
              if (depth > 3) return null; // Limit recursion depth
              
              for (const key in obj) {
                const value = obj[key];
                
                if (typeof value === 'object' && value !== null) {
                  // Check if this object has userType = ADMIN
                  if ((value.userType === 'ADMIN' || value.userType === 'admin')) {
                    const userPhone = value.phoneNumber || value.phone || value.mobile || value.mobileNumber;
                    
                    if (userPhone) {
                      const normalizedUserPhone = normalizePhone(String(userPhone)); // Ensure it's a string
                      
                      // Match phone number and verify password
                      if (normalizedUserPhone === normalizedInput) {
                        // Verify password - this is a helper function that will be called after finding the user
                        // We'll handle password verification outside this recursive function
                        return {
                          ...value,
                          userId: key,
                          id: value.id || key,
                          phoneNumber: userPhone
                        };
                      }
                    }
                  }
                  
                  // Recursively search nested objects
                  const found = searchNode(value, depth + 1);
                  if (found) return found;
                }
              }
              
              return null;
            };
            
            const foundUser = searchNode(rootSnapshot.val());
            if (foundUser) {
              // Verify password for found user
              let authSuccess = false;
              // Try firebaseAuthEmail first (phone-based format if no email), then regular email
              const authEmail = foundUser.firebaseAuthEmail || foundUser.email;
              
              if (authEmail) {
                try {
                  await signInWithEmailAndPassword(auth, authEmail, password);
                  authSuccess = true;
                  console.log('✅ Password verified via Firebase Authentication');
                } catch (authError) {
                  console.log('⚠️ Firebase Auth authentication failed, trying alternative methods...', authError.code);
                  if (authError.code === 'auth/wrong-password' || authError.code === 'auth/invalid-credential') {
                    throw new Error('Invalid password. Please check your credentials.');
                  }
                }
              }
              
              if (!authSuccess) {
                // Check for hashed password or plain password (for backward compatibility)
                const storedPasswordHash = foundUser.passwordHash || foundUser.password || foundUser.pin || foundUser.passcode;
                
                if (storedPasswordHash) {
                  const isValid = await verifyPassword(password, storedPasswordHash);
                  if (isValid) {
                    authSuccess = true;
                    console.log('✅ Password verified from database');
                    
                    // If old plain password format, log a warning
                    if (!storedPasswordHash.includes(':')) {
                      console.warn('⚠️ User is using old password format. Consider updating password for better security.');
                    }
                  } else {
                    throw new Error('Invalid password. Please check your credentials.');
                  }
                } else {
                  throw new Error('Invalid password. Please check your credentials.');
                }
              }
              
              if (authSuccess) {
                userFound = foundUser;
              }
            }
          }
        } catch (error) {
          console.error('Error searching root:', error);
          // If it's a password error, re-throw it
          if (error.message && error.message.includes('password')) {
            throw error;
          }
        }
      }
      
      if (userFound) {
        console.log('✅ Authentication successful!');
        // Store session with complete user data (matching your database structure)
        const authData = {
          userId: userFound.userId || userFound.id,
          phoneNumber: phoneNumber,
          userData: {
            id: userFound.id || userFound.userId,
            name: userFound.name || 'Admin',
            email: userFound.email || '',
            employeeId: userFound.employeeId || '',
            position: userFound.position || '',
            userType: userFound.userType || 'ADMIN',
            isActive: userFound.isActive !== undefined ? userFound.isActive : true,
            isVerified: userFound.isVerified !== undefined ? userFound.isVerified : false,
            membershipStatus: userFound.membershipStatus || '',
            registrationDate: userFound.registrationDate || null
          },
          loggedIn: true,
          timestamp: Date.now()
        };

        if (rememberMe) {
          localStorage.setItem('toda_auth', JSON.stringify(authData));
        } else {
          sessionStorage.setItem('toda_auth', JSON.stringify(authData));
        }
        
        return authData;
      } else {
        // More specific error messages
        if (adminUsersFound.length === 0) {
          throw new Error('No admin users found in database. Please contact administrator.');
        } else {
          const phones = adminUsersFound.map(u => u.phone).join(', ');
          throw new Error(`Invalid phone number. Found ${adminUsersFound.length} admin user(s) with different phone numbers. Please check your phone number.`);
        }
      }
    } catch (error) {
      console.error('Authentication error:', error);
      console.error('Error code:', error.code);
      console.error('Error message:', error.message);
      
      // Handle specific Firebase errors
      if (error.code) {
        if (error.code === 'PERMISSION_DENIED' || error.message.includes('Permission denied')) {
          throw new Error('Database access denied. Please check Firebase Realtime Database security rules. The /users path must have read access enabled.');
        } else if (error.code === 'UNAVAILABLE' || error.code === 'NETWORK_ERROR' || error.message.includes('Failed to fetch') || error.message.includes('network')) {
          throw new Error('Network error. Please check your internet connection and try again. If the problem persists, check Firebase Realtime Database rules.');
        } else if (error.code === 'DATABASE_ERROR') {
          throw new Error('Database error. Please check Firebase configuration and ensure the database URL is correct.');
        }
      }
      
      // Handle custom error messages
      if (error.message.includes('Invalid phone number') || error.message.includes('not found') || error.message.includes('authentication failed')) {
        throw error;
      } else if (error.message.includes('Permission denied') || error.message.includes('access denied')) {
        throw new Error('Database access denied. Please check Firebase Realtime Database security rules.');
      } else {
        // Show more helpful error message
        const errorDetails = error.message || 'Unknown error';
        throw new Error(`Connection error: ${errorDetails}. Please check Firebase configuration and database rules.`);
      }
    }
  }

  // Check authentication status
  function checkAuthStatus() {
    const authData = localStorage.getItem('toda_auth') || sessionStorage.getItem('toda_auth');
    
    if (authData) {
      try {
        const storedAuth = JSON.parse(authData);
        // Check if session is still valid (24 hours)
        const sessionDuration = 24 * 60 * 60 * 1000; // 24 hours
        if (Date.now() - storedAuth.timestamp < sessionDuration) {
          // User is already logged in, redirect to dashboard
          window.location.href = 'index.html';
        } else {
          // Session expired, clear it
          localStorage.removeItem('toda_auth');
          sessionStorage.removeItem('toda_auth');
        }
      } catch (e) {
        // Invalid auth data, clear it
        localStorage.removeItem('toda_auth');
        sessionStorage.removeItem('toda_auth');
      }
    }
  }

  // Show error message
  function showError(message) {
    if (messageHideTimeout) {
      clearTimeout(messageHideTimeout);
      messageHideTimeout = null;
    }
    errorMessage.style.background = '#fef2f2';
    errorMessage.style.color = '#b91c1c';
    errorMessage.style.borderColor = '#fecaca';
    errorMessage.style.boxShadow = '0 18px 35px rgba(248, 113, 113, 0.25)';
    errorMessage.textContent = message;
    errorMessage.classList.add('show');
    
    // Auto-hide after 5 seconds
    messageHideTimeout = setTimeout(() => {
      errorMessage.classList.remove('show');
      messageHideTimeout = null;
    }, 5000);
  }

  // Show success message
  function showSuccess(message) {
    if (messageHideTimeout) {
      clearTimeout(messageHideTimeout);
      messageHideTimeout = null;
    }
    errorMessage.style.background = '#ecfdf5';
    errorMessage.style.color = '#065f46';
    errorMessage.style.borderColor = '#a7f3d0';
    errorMessage.style.boxShadow = '0 18px 35px rgba(52, 211, 153, 0.25)';
    errorMessage.textContent = message;
    errorMessage.classList.add('show');
  }

  // Set loading state
  function setLoadingState(loading) {
    if (loading) {
      loginBtn.classList.add('loading');
      loginBtn.disabled = true;
      phoneNumberInput.disabled = true;
      passwordInput.disabled = true;
    } else {
      loginBtn.classList.remove('loading');
      loginBtn.disabled = false;
      phoneNumberInput.disabled = false;
      passwordInput.disabled = false;
    }
  }

  // Optional: Phone number formatting (can be enabled if needed)
  // phoneNumberInput.addEventListener('input', function(e) {
  //   // Add formatting logic here if needed
  // });

  // Add input animations
  [phoneNumberInput, passwordInput].forEach(input => {
    input.addEventListener('focus', function() {
      this.parentElement.style.transform = 'scale(1.02)';
    });

    input.addEventListener('blur', function() {
      this.parentElement.style.transform = 'scale(1)';
    });

    // Add shake animation on error
    input.addEventListener('invalid', function() {
      this.style.animation = 'shake 0.5s';
      setTimeout(() => {
        this.style.animation = '';
      }, 500);
    });
  });
});

// Add shake animation
const style = document.createElement('style');
style.textContent = `
  @keyframes shake {
    0%, 100% { transform: translateX(0); }
    25% { transform: translateX(-10px); }
    75% { transform: translateX(10px); }
  }
`;
document.head.appendChild(style);

// Forgot password link is now handled by the href in the HTML
