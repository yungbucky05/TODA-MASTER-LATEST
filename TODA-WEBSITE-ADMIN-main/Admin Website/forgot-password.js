// Import Firebase SDKs
import { initializeApp } from "https://www.gstatic.com/firebasejs/11.0.0/firebase-app.js";
import { getAuth, sendPasswordResetEmail } from "https://www.gstatic.com/firebasejs/11.0.0/firebase-auth.js";

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
const auth = getAuth(app);

document.addEventListener('DOMContentLoaded', function() {
    const forgotPasswordForm = document.getElementById('forgotPasswordForm');
    const resetBtn = document.getElementById('resetBtn');
    const btnLoader = document.getElementById('btnLoader');
    const messageDisplay = document.getElementById('messageDisplay');

    if (!forgotPasswordForm || !resetBtn || !btnLoader || !messageDisplay) {
        console.error('Required elements not found:', {
            form: !!forgotPasswordForm,
            button: !!resetBtn,
            loader: !!btnLoader,
            message: !!messageDisplay
        });
        return;
    }

    forgotPasswordForm.addEventListener('submit', async function(e) {
        e.preventDefault();
        console.log('Form submitted');
        
        // Show loading state
        resetBtn.disabled = true;
        btnLoader.style.display = 'inline-block';
        messageDisplay.textContent = '';
        messageDisplay.style.display = 'none';

        const emailInput = document.getElementById('email');
        if (!emailInput) {
            console.error('Email input not found');
            return;
        }

        const email = emailInput.value;
        console.log('Attempting password reset for:', email);

        // Validate email format
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(email)) {
            messageDisplay.textContent = 'Please enter a valid email address.';
            messageDisplay.style.backgroundColor = '#fee2e2';
            messageDisplay.style.color = '#dc2626';
            messageDisplay.style.borderLeftColor = '#dc2626';
            messageDisplay.style.display = 'block';
            resetBtn.disabled = false;
            btnLoader.style.display = 'none';
            return;
        }

        try {
            // Send password reset email
            // Note: Firebase Auth will only send the email if the account exists
            // For security, it won't reveal if the account doesn't exist
            await sendPasswordResetEmail(auth, email);
            console.log('Password reset email request processed');
            
            // Show success message
            // Note: We show this message regardless of whether the email exists (security best practice)
            messageDisplay.textContent = 'If an account exists with this email, a password reset link has been sent. Please check your inbox and spam folder.';
            messageDisplay.style.backgroundColor = '#dcfce7';  // Light green background
            messageDisplay.style.color = '#16a34a';  // Dark green text
            messageDisplay.style.borderLeftColor = '#16a34a';  // Dark green border
            messageDisplay.style.display = 'block';
            
            // Clear the form
            forgotPasswordForm.reset();
        } catch (error) {
            console.error('Password reset error:', error);
            messageDisplay.textContent = error.message || 'Failed to send password reset email. Please try again.';
            messageDisplay.style.backgroundColor = '#f44336';
            messageDisplay.style.display = 'block';
        } finally {
            // Reset button state
            resetBtn.disabled = false;
            btnLoader.style.display = 'none';
        }
    });
});