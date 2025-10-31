// Import Firebase SDKs
import { initializeApp } from "https://www.gstatic.com/firebasejs/11.0.0/firebase-app.js";
import { getDatabase, ref, onValue } from "https://www.gstatic.com/firebasejs/11.0.0/firebase-database.js";

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

// Show notification message
function showMessage(text, type = 'success') {
  const container = document.getElementById('messageContainer');
  const messageId = 'msg-' + Date.now();
  
  const icons = {
    success: '✓',
    error: '✕',
    warning: '⚠',
    info: 'ℹ'
  };
  
  const titles = {
    success: 'Success',
    error: 'Error',
    warning: 'Warning',
    info: 'Information'
  };
  
  const icon = icons[type] || icons.info;
  const title = titles[type] || titles.info;
  
  const messageHTML = `
    <div id="${messageId}" class="message ${type}">
      <span class="message-icon">${icon}</span>
      <div class="message-content">
        <div class="message-title">${title}</div>
        <div class="message-text">${text}</div>
      </div>
    </div>
  `;
  
  container.insertAdjacentHTML('beforeend', messageHTML);
  
  setTimeout(() => {
    const messageEl = document.getElementById(messageId);
    if (messageEl) {
      messageEl.style.opacity = '0';
      setTimeout(() => messageEl.remove(), 300);
    }
  }, 5000);
}

// Custom confirmation dialog
let confirmCallback = null;

function showConfirm(message, title = '⚠️ Confirm Action', confirmText = 'Confirm') {
  return new Promise((resolve) => {
    const modal = document.getElementById('confirmModal');
    const titleEl = document.getElementById('confirmTitle');
    const messageEl = document.getElementById('confirmMessage');
    const confirmBtn = document.getElementById('confirmButton');
    
    titleEl.textContent = title;
    messageEl.textContent = message;
    confirmBtn.textContent = confirmText;
    
    confirmCallback = (result) => {
      modal.classList.remove('active');
      confirmCallback = null;
      resolve(result);
    };
    
    modal.classList.add('active');
  });
}

window.handleConfirm = function() {
  if (confirmCallback) confirmCallback(true);
}

window.closeConfirmModal = function() {
  if (confirmCallback) confirmCallback(false);
}

// Navigation function
window.navigateTo = function(url) {
  window.location.href = url;
}

// Logout function
document.getElementById('logoutBtn')?.addEventListener('click', async () => {
  const confirmResult = await showConfirm(
    'Are you sure you want to logout?',
    '🚪 Logout Confirmation',
    'Logout'
  );
  
  if (confirmResult) {
    // Clear any session data
    sessionStorage.clear();
    localStorage.clear();
    showMessage('Logged out successfully', 'success');
    // Reload page or redirect to login after a short delay
    setTimeout(() => {
      window.location.reload();
    }, 1000);
  }
});

// Check Firebase connection and load data
async function initializeDashboard() {
  try {
    // Load statistics
    loadStats();
    
    // Listen for pending discount applications
    const discountRef = ref(db, 'users');
    onValue(discountRef, (snapshot) => {
      if (snapshot.exists()) {
        let pendingCount = 0;
        const users = snapshot.val();

        Object.keys(users).forEach(userId => {
          const user = users[userId];
          if (user.discountApplication && user.discountApplication.status === 'pending') {
            pendingCount++;
          }
        });

        const badge = document.getElementById('discountBadge');
        if (badge) {
          badge.textContent = pendingCount;
          badge.style.display = pendingCount > 0 ? 'inline-block' : 'none';
        }
      }
    }, (error) => {
      // Error loading discount applications
    });

  } catch (error) {
    showMessage('Error initializing dashboard: ' + error.message, 'error');
  }
}

// Load dashboard statistics
function loadStats() {
  // Load drivers count
  const driversRef = ref(db, 'drivers');
  onValue(driversRef, (snapshot) => {
    const count = snapshot.exists() ? Object.keys(snapshot.val()).length : 0;
    document.getElementById('totalDrivers').textContent = count;
  });

  // Load bookings count
  const bookingsRef = ref(db, 'bookings');
  onValue(bookingsRef, (snapshot) => {
    const count = snapshot.exists() ? Object.keys(snapshot.val()).length : 0;
    document.getElementById('totalBookings').textContent = count;
  });

  // Load contributions total
  const contributionsRef = ref(db, 'contributions');
  onValue(contributionsRef, (snapshot) => {
    if (snapshot.exists()) {
      const contributions = snapshot.val();
      let total = 0;
      let count = 0;
      
      Object.keys(contributions).forEach(key => {
        const amount = parseFloat(contributions[key].amount) || 0;
        total += amount;
        count++;
      });
      
      document.getElementById('totalContributions').textContent = `₱${total.toFixed(2)}`;
    } else {
      document.getElementById('totalContributions').textContent = '₱0.00';
    }
  });

  // Load queue count
  const queueRef = ref(db, 'driverQueue');
  onValue(queueRef, (snapshot) => {
    const count = snapshot.exists() ? Object.keys(snapshot.val()).length : 0;
    document.getElementById('queueCount').textContent = count;
  });
}

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', initializeDashboard);

