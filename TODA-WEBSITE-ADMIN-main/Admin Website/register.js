// Import Firebase SDKs
import { initializeApp } from "https://www.gstatic.com/firebasejs/11.0.0/firebase-app.js";
import { getDatabase, ref, set, get, query, orderByChild, equalTo } from "https://www.gstatic.com/firebasejs/11.0.0/firebase-database.js";
import { getAuth, createUserWithEmailAndPassword, updateProfile } from "https://www.gstatic.com/firebasejs/11.0.0/firebase-auth.js";

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

// Password hashing utility using Web Crypto API
async function hashPassword(password) {
  // Generate a random salt
  const salt = crypto.getRandomValues(new Uint8Array(16));
  const saltHex = Array.from(salt).map(b => b.toString(16).padStart(2, '0')).join('');
  
  // Hash password with salt using SHA-256
  const encoder = new TextEncoder();
  const passwordData = encoder.encode(password);
  const saltData = encoder.encode(saltHex);
  
  const combinedData = new Uint8Array(passwordData.length + saltData.length);
  combinedData.set(passwordData);
  combinedData.set(saltData, passwordData.length);
  
  const hashBuffer = await crypto.subtle.digest('SHA-256', combinedData);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  const hashHex = hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
  
  // Return salt:hash format for storage
  return `${saltHex}:${hashHex}`;
}

// Registration functionality
document.addEventListener('DOMContentLoaded', function() {
  const registerForm = document.getElementById('registerForm');
  const fullNameInput = document.getElementById('fullName');
  const phoneNumberInput = document.getElementById('phoneNumber');
  const emailInput = document.getElementById('email');
  const passwordInput = document.getElementById('password');
  const confirmPasswordInput = document.getElementById('confirmPassword');
  const positionInput = document.getElementById('position');
  const togglePasswordBtn = document.getElementById('togglePassword');
  const toggleConfirmPasswordBtn = document.getElementById('toggleConfirmPassword');
  const registerBtn = document.getElementById('registerBtn');
  const errorMessage = document.getElementById('errorMessage');

  // Toggle password visibility
  togglePasswordBtn.addEventListener('click', function() {
    const type = passwordInput.getAttribute('type') === 'password' ? 'text' : 'password';
    passwordInput.setAttribute('type', type);
    togglePasswordBtn.textContent = type === 'password' ? '👁️' : '🙈';
  });

  toggleConfirmPasswordBtn.addEventListener('click', function() {
    const type = confirmPasswordInput.getAttribute('type') === 'password' ? 'text' : 'password';
    confirmPasswordInput.setAttribute('type', type);
    toggleConfirmPasswordBtn.textContent = type === 'password' ? '👁️' : '🙈';
  });

  // Handle form submission
  registerForm.addEventListener('submit', async function(e) {
    e.preventDefault();
    
    const fullName = fullNameInput.value.trim();
    const phoneNumber = phoneNumberInput.value.trim();
    const email = emailInput.value.trim();
    const password = passwordInput.value;
    const confirmPassword = confirmPasswordInput.value;
    const position = positionInput.value.trim();

    // Validate inputs
    if (!fullName) {
      showError('Please enter your full name');
      return;
    }

    if (!phoneNumber) {
      showError('Please enter your phone number');
      return;
    }

    // Validate phone number format
    const cleanPhone = phoneNumber.replace(/\D/g, '');
    if (cleanPhone.length < 10 || cleanPhone.length > 15) {
      showError('Please enter a valid phone number (10-15 digits)');
      return;
    }

    if (!password || password.length < 6) {
      showError('Password must be at least 6 characters long');
      return;
    }

    if (password !== confirmPassword) {
      showError('Passwords do not match');
      return;
    }

    // Show loading state
    setLoadingState(true);

    try {
      await registerUser(fullName, phoneNumber, email, password, position);
      
      // Success - show message and redirect to login
      showSuccess('Account created successfully! Redirecting to login...');
      
      setTimeout(() => {
        window.location.href = 'login.html';
      }, 2000);
      
    } catch (error) {
      showError(error.message || 'Registration failed. Please try again.');
      setLoadingState(false);
    }
  });

  // Registration function
  async function registerUser(fullName, phoneNumber, email, password, position) {
    try {
      // Normalize phone number (remove formatting for comparison)
      const normalizePhone = (phone) => phone.replace(/\D/g, '');
      const normalizedPhone = normalizePhone(phoneNumber);
      
      console.log('🔍 Checking if phone number already exists...');
      
      // Check if phone number already exists in users
      const usersRef = ref(db, 'users');
      const usersSnapshot = await get(usersRef);
      
      if (usersSnapshot.exists()) {
        const users = usersSnapshot.val();
        for (const userId in users) {
          const user = users[userId];
          const userPhone = user.phoneNumber || user.phone || user.mobile || user.mobileNumber;
          
          if (userPhone) {
            const normalizedUserPhone = normalizePhone(String(userPhone));
            if (normalizedUserPhone === normalizedPhone) {
              throw new Error('Phone number already registered. Please use a different phone number or try logging in.');
            }
          }
        }
      }

      // Always create a Firebase Authentication user
      // If no email provided, use phone number as pseudo-email for Firebase Auth
      let firebaseAuthUserId = null;
      let authEmail = email;
      
      // If no email, create a pseudo-email using phone number
      if (!authEmail) {
        // Format: phone_09660058600@toda.local
        authEmail = `phone_${normalizedPhone}@toda.local`;
      }
      
      try {
        // Create user in Firebase Authentication
        const userCredential = await createUserWithEmailAndPassword(auth, authEmail, password);
        firebaseAuthUserId = userCredential.user.uid;
        console.log('✅ User created in Firebase Authentication');
        
        // Update the user's display name to include phone number and actual name
        await updateProfile(userCredential.user, {
          displayName: fullName,
          photoURL: null // Can add photo URL later if needed
        });
        
        // NOTE: Phone numbers cannot be added directly to Firebase Authentication via client SDK
        // Firebase Auth phone numbers require:
        //   1. Phone authentication provider (signInWithPhoneNumber) - requires SMS verification
        //   2. Firebase Admin SDK (server-side) - requires backend
        // 
        // Instead, we store phone numbers in Realtime Database with mappings:
        //   - /auth_phone_mapping/{uid} - Maps Firebase Auth UID to phone number
        //   - /phone_auth_mapping/{phoneNumber} - Maps phone number to Firebase Auth UID
        //   - /users/{uid} - Main user data including phone number
        // 
        // This allows easy lookup between Firebase Auth users and phone numbers
        console.log('✅ User profile updated with name:', fullName);
        console.log('📱 Phone number will be stored in Realtime Database mappings (Firebase Auth limitation)');
      } catch (authError) {
        if (authError.code === 'auth/email-already-in-use') {
          if (email) {
            throw new Error('Email address is already registered. Please use a different email or try logging in.');
          } else {
            throw new Error('Phone number is already registered in Firebase Authentication. Please try logging in.');
          }
        } else if (authError.code === 'auth/invalid-email') {
          throw new Error('Invalid email address. Please enter a valid email.');
        } else if (authError.code === 'auth/weak-password') {
          throw new Error('Password is too weak. Please use a stronger password.');
        } else {
          console.error('❌ Firebase Auth creation failed:', authError);
          throw new Error(`Authentication failed: ${authError.message || 'Please try again.'}`);
        }
      }

      // Use Firebase Auth user ID as the primary user ID
      const userId = firebaseAuthUserId;
      
      // Hash password before storing
      const hashedPassword = await hashPassword(password);
      
      // Create user object
      const userData = {
        id: userId,
        name: fullName,
        phoneNumber: normalizedPhone,
        userType: 'ADMIN',
        isActive: true,
        isVerified: true,
        membershipStatus: 'ACTIVE',
        registrationDate: Date.now(),
        lastActiveTime: Date.now(),
        position: position || '',
        passwordHash: hashedPassword, // Store hashed password securely (for backup/fallback)
        firebaseAuthEmail: authEmail, // Store the Firebase Auth email (phone-based if no email provided)
        // Include actual email if provided (different from Firebase Auth email if phone-based)
        ...(email && { email: email })
      };

      // Save to Firebase Realtime Database
      const userRef = ref(db, `users/${userId}`);
      await set(userRef, userData);
      
      // Also create a mapping: Firebase Auth UID -> Phone Number
      // This makes it easy to find phone numbers from Firebase Auth users
      const authPhoneMappingRef = ref(db, `auth_phone_mapping/${userId}`);
      await set(authPhoneMappingRef, {
        uid: userId,
        phoneNumber: normalizedPhone,
        email: authEmail,
        name: fullName,
        createdAt: Date.now()
      });
      
      // Also create reverse mapping: Phone Number -> Firebase Auth UID
      // This allows quick lookup by phone number
      const phoneAuthMappingRef = ref(db, `phone_auth_mapping/${normalizedPhone}`);
      await set(phoneAuthMappingRef, {
        uid: userId,
        firebaseAuthEmail: authEmail,
        name: fullName,
        mappedAt: Date.now()
      });
      
      console.log('✅ User registered successfully in database');
      console.log('✅ Phone number mapped to Firebase Auth UID');
      // Don't log password hash for security
      const { passwordHash, ...safeUserData } = userData;
      console.log('📝 User registered:', safeUserData);
      console.log('📱 Phone number stored in mappings for Firebase Auth user:', normalizedPhone);

      return {
        userId: userId,
        userData: userData
      };

    } catch (error) {
      console.error('Registration error:', error);
      
      // Handle specific Firebase errors
      if (error.code === 'PERMISSION_DENIED' || error.message.includes('Permission denied')) {
        throw new Error('Database access denied. Please check Firebase Realtime Database security rules.');
      } else if (error.code === 'UNAVAILABLE' || error.code === 'NETWORK_ERROR') {
        throw new Error('Network error. Please check your internet connection and try again.');
      } else if (error.message) {
        throw error; // Re-throw custom error messages
      } else {
        throw new Error('Registration failed. Please try again later.');
      }
    }
  }

  // Show error message
  function showError(message) {
    errorMessage.style.background = '#fee2e2';
    errorMessage.style.color = '#dc2626';
    errorMessage.style.borderLeftColor = '#dc2626';
    errorMessage.textContent = message;
    errorMessage.classList.add('show');
    
    // Auto-hide after 5 seconds
    setTimeout(() => {
      errorMessage.classList.remove('show');
    }, 5000);
  }

  // Show success message
  function showSuccess(message) {
    errorMessage.style.background = '#d1fae5';
    errorMessage.style.color = '#065f46';
    errorMessage.style.borderLeftColor = '#10b981';
    errorMessage.textContent = message;
    errorMessage.classList.add('show');
  }

  // Set loading state
  function setLoadingState(loading) {
    if (loading) {
      registerBtn.classList.add('loading');
      registerBtn.disabled = true;
      fullNameInput.disabled = true;
      phoneNumberInput.disabled = true;
      emailInput.disabled = true;
      passwordInput.disabled = true;
      confirmPasswordInput.disabled = true;
      positionInput.disabled = true;
    } else {
      registerBtn.classList.remove('loading');
      registerBtn.disabled = false;
      fullNameInput.disabled = false;
      phoneNumberInput.disabled = false;
      emailInput.disabled = false;
      passwordInput.disabled = false;
      confirmPasswordInput.disabled = false;
      positionInput.disabled = false;
    }
  }

  // Real-time password matching validation
  confirmPasswordInput.addEventListener('input', function() {
    if (confirmPasswordInput.value && passwordInput.value) {
      if (confirmPasswordInput.value !== passwordInput.value) {
        confirmPasswordInput.setCustomValidity('Passwords do not match');
      } else {
        confirmPasswordInput.setCustomValidity('');
      }
    }
  });

  passwordInput.addEventListener('input', function() {
    if (confirmPasswordInput.value) {
      if (confirmPasswordInput.value !== passwordInput.value) {
        confirmPasswordInput.setCustomValidity('Passwords do not match');
      } else {
        confirmPasswordInput.setCustomValidity('');
      }
    }
  });

  // Add input animations
  [fullNameInput, phoneNumberInput, emailInput, passwordInput, confirmPasswordInput, positionInput].forEach(input => {
    input.addEventListener('focus', function() {
      this.parentElement.style.transform = 'scale(1.02)';
    });

    input.addEventListener('blur', function() {
      this.parentElement.style.transform = 'scale(1)';
    });
  });
});

