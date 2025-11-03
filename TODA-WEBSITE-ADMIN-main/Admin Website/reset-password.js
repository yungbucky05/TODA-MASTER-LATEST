// Import Firebase SDKs
import { initializeApp } from "https://www.gstatic.com/firebasejs/11.0.0/firebase-app.js";
import { getAuth, confirmPasswordReset, verifyPasswordResetCode } from "https://www.gstatic.com/firebasejs/11.0.0/firebase-auth.js";

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
    const resetPasswordForm = document.getElementById('resetPasswordForm');
    const newPasswordInput = document.getElementById('newPassword');
    const confirmPasswordInput = document.getElementById('confirmPassword');
    const togglePasswordBtn = document.getElementById('togglePassword');
    const toggleConfirmPasswordBtn = document.getElementById('toggleConfirmPassword');
    const resetBtn = document.getElementById('resetBtn');
    const btnLoader = document.getElementById('btnLoader');
    const messageDisplay = document.getElementById('messageDisplay');

    // Get the action code from the URL
    const urlParams = new URLSearchParams(window.location.search);
    const actionCode = urlParams.get('oobCode');

    if (!actionCode) {
        messageDisplay.textContent = 'Invalid password reset link. Please request a new one.';
        messageDisplay.style.backgroundColor = '#f44336';
        messageDisplay.style.display = 'block';
        resetPasswordForm.style.display = 'none';
        return;
    }

    // Verify the password reset code
    verifyPasswordResetCode(auth, actionCode).catch(error => {
        messageDisplay.textContent = 'Invalid or expired password reset link. Please request a new one.';
        messageDisplay.style.backgroundColor = '#f44336';
        messageDisplay.style.display = 'block';
        resetPasswordForm.style.display = 'none';
    });

    // Toggle password visibility
    togglePasswordBtn.addEventListener('click', () => {
        const type = newPasswordInput.getAttribute('type') === 'password' ? 'text' : 'password';
        newPasswordInput.setAttribute('type', type);
    });

    toggleConfirmPasswordBtn.addEventListener('click', () => {
        const type = confirmPasswordInput.getAttribute('type') === 'password' ? 'text' : 'password';
        confirmPasswordInput.setAttribute('type', type);
    });

    resetPasswordForm.addEventListener('submit', async function(e) {
        e.preventDefault();
        
        // Show loading state
        resetBtn.disabled = true;
        btnLoader.style.display = 'inline-block';
        messageDisplay.textContent = '';
        messageDisplay.style.display = 'none';

        const newPassword = newPasswordInput.value;
        const confirmPassword = confirmPasswordInput.value;

        // Validate passwords
        if (newPassword !== confirmPassword) {
            messageDisplay.textContent = 'Passwords do not match.';
            messageDisplay.style.backgroundColor = '#f44336';
            messageDisplay.style.display = 'block';
            resetBtn.disabled = false;
            btnLoader.style.display = 'none';
            return;
        }

        try {
            // Confirm the password reset
            await confirmPasswordReset(auth, actionCode, newPassword);
            
            // Show success message
            messageDisplay.textContent = 'Password has been reset successfully! Redirecting to login...';
            messageDisplay.style.backgroundColor = '#4CAF50';
            messageDisplay.style.display = 'block';
            
            // Redirect to login page after 3 seconds
            setTimeout(() => {
                window.location.href = 'login.html';
            }, 3000);
        } catch (error) {
            console.error('Password reset error:', error);
            messageDisplay.textContent = error.message || 'Failed to reset password. Please try again.';
            messageDisplay.style.backgroundColor = '#f44336';
            messageDisplay.style.display = 'block';
            resetBtn.disabled = false;
            btnLoader.style.display = 'none';
        }
    });
});