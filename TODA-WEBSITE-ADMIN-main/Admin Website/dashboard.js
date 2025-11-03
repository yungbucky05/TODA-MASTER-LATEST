// Import Firebase SDKs
import { initializeApp } from "https://www.gstatic.com/firebasejs/11.0.0/firebase-app.js";
import { getDatabase, ref, onValue, update, query, orderByChild, get } from "https://www.gstatic.com/firebasejs/11.0.0/firebase-database.js";

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
    
    // Set logout flag for login page notification
    sessionStorage.setItem('justLoggedOut', 'true');
    
    // Redirect to login page
    window.location.href = 'login.html';
  }
});

// ===== NOTIFICATION SYSTEM =====
let notifications = [];

// Toggle notification dropdown
window.toggleNotifications = function() {
  const dropdown = document.getElementById('notificationDropdown');
  dropdown.classList.toggle('active');
}

// Close notification dropdown when clicking outside
document.addEventListener('click', function(event) {
  const notificationContainer = document.querySelector('.notification-container');
  const dropdown = document.getElementById('notificationDropdown');
  
  if (notificationContainer && !notificationContainer.contains(event.target)) {
    dropdown?.classList.remove('active');
  }
});

// Mark all notifications as read
window.markAllAsRead = async function() {
  try {
    const notificationsRef = ref(db, 'notifications');
    const updates = {};
    
    notifications.forEach(notif => {
      if (!notif.isRead) {
        updates[`notifications/${notif.id}/isRead`] = true;
      }
    });
    
    if (Object.keys(updates).length > 0) {
      await update(ref(db), updates);
      showMessage('All notifications marked as read', 'success');
    }
  } catch (error) {
    showMessage('Error marking notifications as read: ' + error.message, 'error');
  }
}

// Mark single notification as read
window.markAsRead = async function(notificationId) {
  try {
    const notifRef = ref(db, `notifications/${notificationId}`);
    await update(notifRef, { isRead: true });
  } catch (error) {
    console.error('Error marking notification as read:', error);
  }
}

// Delete notification
window.deleteNotification = async function(notificationId) {
  try {
    const notifRef = ref(db, `notifications/${notificationId}`);
    await update(notifRef, { isRead: true, deleted: true });
    showMessage('Notification deleted', 'success');
  } catch (error) {
    showMessage('Error deleting notification: ' + error.message, 'error');
  }
}

// Clear all notifications
window.clearAllNotifications = async function() {
  const confirmResult = await showConfirm(
    'Are you sure you want to clear all notifications?\n\nThis will mark all notifications as deleted.',
    '🗑️ Clear All Notifications',
    'Clear All'
  );
  
  if (!confirmResult) {
    return;
  }

  try {
    const updates = {};
    
    notifications.forEach(notif => {
      if (!notif.deleted) {
        updates[`notifications/${notif.id}/deleted`] = true;
        updates[`notifications/${notif.id}/isRead`] = true;
      }
    });
    
    if (Object.keys(updates).length > 0) {
      await update(ref(db), updates);
      showMessage('All notifications cleared', 'success');
    }
  } catch (error) {
    showMessage('Error clearing notifications: ' + error.message, 'error');
  }
}

// Clear single notification (alias for deleteNotification for consistency)
window.clearNotification = async function(notificationId, event) {
  if (event) {
    event.stopPropagation(); // Prevent triggering notification action
  }
  await deleteNotification(notificationId);
}

// Handle notification action (e.g., navigate to driver management)
window.handleNotificationAction = function(notification) {
  // Mark as read
  markAsRead(notification.id);
  
  // Navigate based on notification type
  switch(notification.type) {
    case 'RFID_MISSING':
      window.location.href = 'DriverManagement/DriverManagement.html';
      break;
    case 'DRIVER_VERIFICATION':
      window.location.href = 'DriverManagement/DriverManagement.html';
      break;
    case 'DISCOUNT_APPLICATION':
      window.location.href = 'DiscountApplications/DiscountApplications.html';
      break;
    case 'DISCOUNT_RESUBMISSION':
      window.location.href = 'DiscountApplications/DiscountApplications.html';
      break;
    default:
      // Close dropdown
      document.getElementById('notificationDropdown').classList.remove('active');
  }
}

// Format timestamp
function formatNotificationTime(timestamp) {
  const now = Date.now();
  const diff = now - timestamp;
  
  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(diff / 86400000);
  
  if (minutes < 1) return 'Just now';
  if (minutes < 60) return `${minutes}m ago`;
  if (hours < 24) return `${hours}h ago`;
  if (days < 7) return `${days}d ago`;
  
  const date = new Date(timestamp);
  return date.toLocaleDateString();
}

// Render notifications
function renderNotifications() {
  const notificationList = document.getElementById('notificationList');
  const notificationCount = document.getElementById('notificationCount');
  
  // Filter out deleted notifications and sort by timestamp
  const activeNotifications = notifications
    .filter(n => !n.deleted)
    .sort((a, b) => b.timestamp - a.timestamp);
  
  // Update count (only unread notifications)
  const unreadCount = activeNotifications.filter(n => !n.isRead).length;
  notificationCount.textContent = unreadCount;
  notificationCount.style.display = unreadCount > 0 ? 'flex' : 'none';
  
  // Render notification list
  if (activeNotifications.length === 0) {
    notificationList.innerHTML = `
      <div class="no-notifications">
        <span class="no-notif-icon">🔔</span>
        <p>No notifications yet</p>
      </div>
    `;
    return;
  }
  
  notificationList.innerHTML = activeNotifications.map(notif => {
    const priorityClass = notif.priority === 'high' ? 'high-priority' : '';
    const readClass = notif.isRead ? 'read' : 'unread';
    const actionClass = notif.actionRequired ? 'action-required' : '';
    
    // Get icon based on notification type
    let icon = '🔔';
    if (notif.type === 'RFID_MISSING') icon = '🚨';
    else if (notif.type === 'DRIVER_VERIFICATION') icon = '👤';
    else if (notif.type === 'DISCOUNT_APPLICATION') icon = '🎫';
    else if (notif.type === 'DISCOUNT_RESUBMISSION') icon = '🔄';
    
    return `
      <div class="notification-item ${readClass} ${priorityClass} ${actionClass}" 
           onclick="handleNotificationAction(${JSON.stringify(notif).replace(/"/g, '&quot;')})">
        <div class="notification-icon">${icon}</div>
        <div class="notification-content">
          <div class="notification-title">${notif.title}</div>
          <div class="notification-message">${notif.message}</div>
          <div class="notification-time">${formatNotificationTime(notif.timestamp)}</div>
        </div>
        ${!notif.isRead ? '<div class="unread-indicator"></div>' : ''}
        <button class="clear-notification-btn" onclick="clearNotification('${notif.id}', event)" title="Clear notification">×</button>
      </div>
    `;
  }).join('');
}

// Listen to Firebase notifications
function listenToNotifications() {
  const notificationsRef = ref(db, 'notifications');
  
  onValue(notificationsRef, (snapshot) => {
    if (snapshot.exists()) {
      const data = snapshot.val();
      notifications = Object.keys(data).map(key => ({
        id: key,
        ...data[key]
      }));
      
      renderNotifications();
      
      // Show browser notification for new high-priority notifications
      const newHighPriority = notifications.find(n => 
        !n.isRead && 
        n.priority === 'high' && 
        (Date.now() - n.timestamp) < 5000 // Within last 5 seconds
      );
      
      if (newHighPriority && Notification.permission === 'granted') {
        new Notification(newHighPriority.title, {
          body: newHighPriority.message,
          icon: '🚨',
          tag: newHighPriority.id
        });
      }
    } else {
      notifications = [];
      renderNotifications();
    }
  }, (error) => {
    console.error('Error listening to notifications:', error);
  });
}

// Request notification permission
function requestNotificationPermission() {
  if ('Notification' in window && Notification.permission === 'default') {
    Notification.requestPermission();
  }
}

// ===== END NOTIFICATION SYSTEM =====

// Check Firebase connection and load data
async function initializeDashboard() {
  try {
    // Request notification permission
    requestNotificationPermission();
    
    // Start listening to notifications
    listenToNotifications();
    
    // Load statistics with default timeframe
    loadStats('all');
    
    // Add timeframe selector event listener
    const timeframeSelect = document.getElementById('timeframeSelect');
    if (timeframeSelect) {
      timeframeSelect.addEventListener('change', (e) => {
        const selectedTimeframe = e.target.value;
        loadStats(selectedTimeframe);
        showMessage(`Statistics updated for ${e.target.selectedOptions[0].text}`, 'info');
      });
    }
    
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
function loadStats(timeframe = 'all') {
  const now = new Date();
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
  const startOfWeek = new Date(now.getTime() - (now.getDay() * 24 * 60 * 60 * 1000)).setHours(0, 0, 0, 0);
  const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1).getTime();

  // Helper function to check if timestamp is within timeframe
  function isWithinTimeframe(timestamp) {
    if (!timestamp || timeframe === 'all') return true;
    
    let time;
    
    // Handle different timestamp formats
    if (typeof timestamp === 'string') {
      // Check if it's a date string (YYYY-MM-DD) or timestamp string
      if (timestamp.includes('-') && timestamp.length === 10) {
        // Date format like "2025-08-26"
        time = new Date(timestamp).getTime();
      } else if (timestamp.includes('T') || timestamp.includes('Z')) {
        // ISO date string like "2025-10-30T13:27:14.161Z"
        time = new Date(timestamp).getTime();
      } else {
        // Timestamp string in seconds like "1756208451"
        time = parseInt(timestamp) * 1000; // Convert seconds to milliseconds
      }
    } else if (typeof timestamp === 'number') {
      // Check if it's in seconds or milliseconds
      // Timestamps in milliseconds are much larger (13 digits vs 10 digits)
      time = timestamp > 10000000000 ? timestamp : timestamp * 1000;
    } else {
      return true; // Can't parse, include it
    }
    
    switch(timeframe) {
      case 'today':
        return time >= startOfToday;
      case 'week':
        return time >= startOfWeek;
      case 'month':
        return time >= startOfMonth;
      default:
        return true;
    }
  }

  // Load drivers count (only verified drivers)
  const driversRef = ref(db, 'drivers');
  onValue(driversRef, (snapshot) => {
    let count = 0;
    if (snapshot.exists()) {
      const drivers = snapshot.val();
      Object.keys(drivers).forEach(key => {
        const driver = drivers[key];
        // Count verified drivers, filter by verification date for timeframe
        if (driver.verificationStatus === 'verified') {
          if (timeframe === 'all' || isWithinTimeframe(driver.verifiedAt || driver.registrationDate)) {
            count++;
          }
        }
      });
    }
    document.getElementById('totalDrivers').textContent = count;
  });

  // Load bookings count with timeframe filter
  const bookingsRef = ref(db, 'bookings');
  onValue(bookingsRef, (snapshot) => {
    let count = 0;
    if (snapshot.exists()) {
      const bookings = snapshot.val();
      Object.keys(bookings).forEach(key => {
        const booking = bookings[key];
        // Use timestamp field from bookings
        if (isWithinTimeframe(booking.timestamp)) {
          count++;
        }
      });
    }
    document.getElementById('totalBookings').textContent = count;
  });

  // Load contributions total with timeframe filter
  const contributionsRef = ref(db, 'contributions');
  onValue(contributionsRef, (snapshot) => {
    if (snapshot.exists()) {
      const contributions = snapshot.val();
      let total = 0;
      
      Object.keys(contributions).forEach(key => {
        const contribution = contributions[key];
        // Use timestamp field (which is in seconds as a string)
        if (isWithinTimeframe(contribution.timestamp)) {
          const amount = parseFloat(contribution.amount) || 0;
          total += amount;
        }
      });
      
      document.getElementById('totalContributions').textContent = `₱${total.toFixed(2)}`;
    } else {
      document.getElementById('totalContributions').textContent = '₱0.00';
    }
  });

  // Load queue count (always today's queue)
  const queueRef = ref(db, 'driverQueue');
  onValue(queueRef, (snapshot) => {
    const count = snapshot.exists() ? Object.keys(snapshot.val()).length : 0;
    document.getElementById('queueCount').textContent = count;
  });

  // Load flagged accounts count from separate collections
  const driverFlagsRef = ref(db, 'driverFlags');
  const userFlagsRef = ref(db, 'userFlags');
  
  get(driverFlagsRef).then((driverFlagsSnapshot) => {
    let flaggedCount = 0;
    
    if (driverFlagsSnapshot.exists()) {
      const driverFlags = driverFlagsSnapshot.val();
      Object.keys(driverFlags).forEach(driverId => {
        const flags = driverFlags[driverId];
        Object.keys(flags).forEach(flagId => {
          if (flags[flagId].status === 'active' && flags[flagId].severity === 'critical') {
            flaggedCount++;
          }
        });
      });
    }
    
    // Also count customer flags
    get(userFlagsRef).then((userFlagsSnapshot) => {
      if (userFlagsSnapshot.exists()) {
        const userFlags = userFlagsSnapshot.val();
        Object.keys(userFlags).forEach(userId => {
          const flags = userFlags[userId];
          Object.keys(flags).forEach(flagId => {
            if (flags[flagId].status === 'active' && flags[flagId].severity === 'critical') {
              flaggedCount++;
            }
          });
        });
      }
      
      const flaggedBadge = document.getElementById('flaggedBadge');
      if (flaggedBadge) {
        flaggedBadge.textContent = flaggedCount;
        flaggedBadge.style.display = flaggedCount > 0 ? 'flex' : 'none';
      }
    });
  });
}

const menuToggle = document.getElementById("menuToggle");
const sidebar = document.querySelector(".sidebar");

menuToggle.addEventListener("click", () => {
  sidebar.classList.toggle("active");
  document.body.classList.toggle("sidebar-open");
});

// Optional: close sidebar when clicking outside
document.body.addEventListener("click", (e) => {
  if (
    sidebar.classList.contains("active") &&
    !sidebar.contains(e.target) &&
    !menuToggle.contains(e.target)
  ) {
    sidebar.classList.remove("active");
    document.body.classList.remove("sidebar-open");
  }
});


// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', initializeDashboard);
