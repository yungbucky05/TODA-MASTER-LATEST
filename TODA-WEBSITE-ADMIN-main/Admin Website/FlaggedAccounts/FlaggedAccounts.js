// Import Firebase SDKs
import { initializeApp } from "https://www.gstatic.com/firebasejs/11.0.0/firebase-app.js";
import { getDatabase, ref, onValue, update, push, set, get } from "https://www.gstatic.com/firebasejs/11.0.0/firebase-database.js";

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

// Global variables
let allFlaggedAccounts = [];
let filteredAccounts = [];
let currentPage = 1;
let itemsPerPage = 25;
let selectedFlag = null;
let confirmCallback = null;

// Flag type configurations
const FLAG_TYPES = {
  // Driver Flags
  LOW_CONTRIBUTIONS: {
    category: 'driver',
    severity: 'high',
    points: 75,
    name: 'Low Contributions',
    icon: '💰',
    color: '#f97316'
  },
  INACTIVE_ACCOUNT: {
    category: 'driver',
    severity: 'medium',
    points: 50,
    name: 'Inactive Account',
    icon: '💤',
    color: '#eab308'
  },
  HIGH_CANCELLATION_RATE: {
    category: 'driver',
    severity: 'high',
    points: 75,
    name: 'High Cancellation Rate',
    icon: '🚫',
    color: '#f97316'
  },
  CONTRIBUTION_IRREGULARITIES: {
    category: 'driver',
    severity: 'high',
    points: 75,
    name: 'Contribution Irregularities',
    icon: '📉',
    color: '#f97316'
  },
  CUSTOMER_COMPLAINTS: {
    category: 'driver',
    severity: 'critical',
    points: 100,
    name: 'Customer Complaints',
    icon: '😡',
    color: '#ef4444'
  },
  RFID_ISSUES: {
    category: 'driver',
    severity: 'medium',
    points: 50,
    name: 'RFID Issues',
    icon: '🏷️',
    color: '#eab308'
  },
  
  // Customer Flags
  NO_SHOW: {
    category: 'customer',
    severity: 'critical',
    points: 100,
    name: 'No-Show Pattern',
    icon: '👻',
    color: '#ef4444'
  },
  NON_PAYMENT: {
    category: 'customer',
    severity: 'critical',
    points: 100,
    name: 'Non-Payment',
    icon: '💸',
    color: '#ef4444'
  },
  WRONG_PIN: {
    category: 'customer',
    severity: 'medium',
    points: 50,
    name: 'Wrong Location PIN',
    icon: '📍',
    color: '#eab308'
  },
  ABUSIVE_BEHAVIOR: {
    category: 'customer',
    severity: 'critical',
    points: 100,
    name: 'Abusive Behavior',
    icon: '🤬',
    color: '#ef4444'
  },
  EXCESSIVE_CANCELLATIONS: {
    category: 'customer',
    severity: 'high',
    points: 75,
    name: 'Excessive Cancellations',
    icon: '❌',
    color: '#f97316'
  },
  DISCOUNT_ABUSE: {
    category: 'customer',
    severity: 'high',
    points: 75,
    name: 'Discount Abuse',
    icon: '🎫',
    color: '#f97316'
  }
};

// Show notification message
function showMessage(text, type = 'success') {
  const container = document.getElementById('messageContainer');
  const message = document.createElement('div');
  message.className = `message ${type}`;
  
  const icons = {
    success: '✅',
    error: '❌',
    warning: '⚠️',
    info: 'ℹ️'
  };
  
  message.innerHTML = `
    <span class="message-icon">${icons[type] || icons.info}</span>
    <span class="message-text">${text}</span>
  `;
  
  container.appendChild(message);
  
  setTimeout(() => {
    message.style.animation = 'slideIn 0.3s ease reverse';
    setTimeout(() => message.remove(), 300);
  }, 4000);
}

// Confirmation dialog
function showConfirm(message, title = '⚠️ Confirm Action', confirmText = 'Confirm') {
  return new Promise((resolve) => {
    document.getElementById('confirmTitle').textContent = title;
    document.getElementById('confirmMessage').textContent = message;
    document.getElementById('confirmBtn').textContent = confirmText;
    document.getElementById('confirmModal').classList.add('active');
    
    confirmCallback = (result) => {
      document.getElementById('confirmModal').classList.remove('active');
      confirmCallback = null;
      resolve(result);
    };
  });
}

window.handleConfirm = function() {
  if (confirmCallback) confirmCallback(true);
}

window.closeConfirmModal = function() {
  if (confirmCallback) confirmCallback(false);
}

// Load all flagged accounts
function loadFlaggedAccounts() {
  allFlaggedAccounts = [];
  
  // Load drivers with flags from separate driverFlags collection
  const driversRef = ref(db, 'drivers');
  const driverFlagsRef = ref(db, 'driverFlags');
  
  // First get all drivers data
  get(driversRef).then((driversSnapshot) => {
    const drivers = driversSnapshot.val() || {};
    
    // Then get all driver flags
    get(driverFlagsRef).then((flagsSnapshot) => {
      if (flagsSnapshot.exists()) {
        const driverFlags = flagsSnapshot.val();
        Object.keys(driverFlags).forEach(driverId => {
          const driver = drivers[driverId];
          if (!driver) return; // Skip if driver doesn't exist
          
          const flags = driverFlags[driverId];
          if (flags && Object.keys(flags).length > 0) {
            Object.keys(flags).forEach(flagId => {
              const flag = flags[flagId];
              if (flag.status === 'active' || flag.status === 'resolved') {
                allFlaggedAccounts.push({
                  accountId: driverId,
                  accountType: 'driver',
                  accountName: driver.driverName || driver.name || 'Unknown Driver',
                  accountPhone: driver.phoneNumber || 'N/A',
                  accountEmail: driver.email || 'N/A',
                  flagScore: driver.flagScore || 0,
                  flagStatus: driver.flagStatus || 'monitored',
                  ...flag,
                  flagId: flagId
                });
              }
            });
          }
        });
      }
      loadCustomerFlags();
    });
  });
}

function loadCustomerFlags() {
  // Load customers with flags from separate userFlags collection
  const usersRef = ref(db, 'users');
  const userFlagsRef = ref(db, 'userFlags');
  
  // First get all users data
  get(usersRef).then((usersSnapshot) => {
    const users = usersSnapshot.val() || {};
    
    // Then get all user flags
    get(userFlagsRef).then((flagsSnapshot) => {
      if (flagsSnapshot.exists()) {
        const userFlags = flagsSnapshot.val();
        Object.keys(userFlags).forEach(userId => {
          const user = users[userId];
          if (!user || user.userType !== 'PASSENGER') return; // Skip if not a passenger
          
          const flags = userFlags[userId];
          if (flags && Object.keys(flags).length > 0) {
            Object.keys(flags).forEach(flagId => {
              const flag = flags[flagId];
              if (flag.status === 'active' || flag.status === 'resolved') {
                allFlaggedAccounts.push({
                  accountId: userId,
                  accountType: 'customer',
                  accountName: user.name || 'Unknown Customer',
                  accountPhone: user.phoneNumber || 'N/A',
                  accountEmail: user.email || 'N/A',
                  flagScore: user.flagScore || 0,
                  flagStatus: user.flagStatus || 'monitored',
                  ...flag,
                  flagId: flagId
                });
              }
            });
          }
        });
      }
      updateStats();
      applyFilters();
    });
  });
}

// Update statistics
function updateStats() {
  const critical = allFlaggedAccounts.filter(f => f.severity === 'critical' && f.status === 'active').length;
  const high = allFlaggedAccounts.filter(f => f.severity === 'high' && f.status === 'active').length;
  const medium = allFlaggedAccounts.filter(f => f.severity === 'medium' && f.status === 'active').length;
  const total = allFlaggedAccounts.filter(f => f.status === 'active').length;
  
  document.getElementById('criticalCount').textContent = critical;
  document.getElementById('highCount').textContent = high;
  document.getElementById('mediumCount').textContent = medium;
  document.getElementById('totalFlagged').textContent = total;
}

// Apply filters
window.applyFilters = function() {
  const accountType = document.getElementById('accountTypeFilter').value;
  const severity = document.getElementById('severityFilter').value;
  const status = document.getElementById('statusFilter').value;
  const search = document.getElementById('searchInput').value.toLowerCase();
  
  filteredAccounts = allFlaggedAccounts.filter(account => {
    const matchesAccountType = accountType === 'all' || account.accountType === accountType;
    const matchesSeverity = severity === 'all' || account.severity === severity;
    const matchesStatus = status === 'all' || account.status === status;
    const matchesSearch = !search || 
      account.accountName.toLowerCase().includes(search) ||
      account.accountPhone.includes(search) ||
      account.accountId.toLowerCase().includes(search);
    
    return matchesAccountType && matchesSeverity && matchesStatus && matchesSearch;
  });
  
  // Sort by severity and timestamp
  filteredAccounts.sort((a, b) => {
    const severityOrder = { critical: 0, high: 1, medium: 2, low: 3 };
    if (severityOrder[a.severity] !== severityOrder[b.severity]) {
      return severityOrder[a.severity] - severityOrder[b.severity];
    }
    return b.timestamp - a.timestamp;
  });
  
  currentPage = 1;
  displayAccounts();
}

// Display accounts
function displayAccounts() {
  const tbody = document.getElementById('flaggedTableBody');
  const start = (currentPage - 1) * itemsPerPage;
  const end = start + itemsPerPage;
  const pageAccounts = filteredAccounts.slice(start, end);
  
  if (pageAccounts.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="8" class="empty-state">
          <div class="empty-state-icon">🔍</div>
          <p>No flagged accounts found</p>
        </td>
      </tr>
    `;
  } else {
    tbody.innerHTML = pageAccounts.map(account => {
      const flagType = FLAG_TYPES[account.type] || {};
      const initials = account.accountName.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();
      
      return `
        <tr>
          <td>
            <div class="account-info">
              <div class="account-avatar">${initials}</div>
              <div class="account-details">
                <h4>${account.accountName}</h4>
                <p>${account.accountPhone}</p>
              </div>
            </div>
          </td>
          <td>
            <span class="badge ${account.accountType}">${account.accountType}</span>
          </td>
          <td>
            <div style="display: flex; align-items: center; gap: 8px;">
              <span style="font-size: 20px;">${flagType.icon || '🚩'}</span>
              <span>${flagType.name || account.type}</span>
            </div>
          </td>
          <td>
            <span class="badge ${account.severity}">${account.severity}</span>
          </td>
          <td>
            <span class="flag-score ${account.severity}">${account.points || 0}</span>
          </td>
          <td>
            <span class="badge ${account.status}">${account.status}</span>
          </td>
          <td>${new Date(account.timestamp).toLocaleDateString()}</td>
          <td>
            <button class="action-btn" onclick="viewFlagDetails('${account.accountId}', '${account.flagId}')">
              View Details
            </button>
          </td>
        </tr>
      `;
    }).join('');
  }
  
  // Update table count
  document.getElementById('tableCount').textContent = `${filteredAccounts.length} account${filteredAccounts.length !== 1 ? 's' : ''}`;
  
  // Update pagination
  const totalPages = Math.ceil(filteredAccounts.length / itemsPerPage);
  document.getElementById('pageInfo').textContent = `Page ${currentPage} of ${totalPages || 1}`;
  
  const prevBtn = document.querySelectorAll('.page-btn')[0];
  const nextBtn = document.querySelectorAll('.page-btn')[1];
  
  prevBtn.disabled = currentPage === 1;
  nextBtn.disabled = currentPage >= totalPages;
}

// Change page
window.changePage = function(direction) {
  const totalPages = Math.ceil(filteredAccounts.length / itemsPerPage);
  const newPage = currentPage + direction;
  
  if (newPage >= 1 && newPage <= totalPages) {
    currentPage = newPage;
    displayAccounts();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
}

// View flag details
window.viewFlagDetails = async function(accountId, flagId) {
  const account = filteredAccounts.find(f => f.accountId === accountId && f.flagId === flagId);
  if (!account) return;
  
  selectedFlag = account;
  
  // Get full account data
  const accountRef = ref(db, `${account.accountType === 'driver' ? 'drivers' : 'users'}/${accountId}`);
  const snapshot = await get(accountRef);
  const fullAccount = snapshot.val();
  
  const flagType = FLAG_TYPES[account.type] || {};
  
  let detailsHTML = `
    <div class="flag-detail-section">
      <h3>${flagType.icon || '🚩'} ${flagType.name || account.type}</h3>
      <div class="detail-grid">
        <div class="detail-item">
          <div class="detail-label">Account Type</div>
          <div class="detail-value">${account.accountType}</div>
        </div>
        <div class="detail-item">
          <div class="detail-label">Severity</div>
          <div class="detail-value">
            <span class="badge ${account.severity}">${account.severity}</span>
          </div>
        </div>
        <div class="detail-item">
          <div class="detail-label">Points</div>
          <div class="detail-value">${account.points || 0}</div>
        </div>
        <div class="detail-item">
          <div class="detail-label">Status</div>
          <div class="detail-value">
            <span class="badge ${account.status}">${account.status}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="flag-detail-section">
      <h3>👤 Account Information</h3>
      <div class="detail-grid">
        <div class="detail-item">
          <div class="detail-label">Name</div>
          <div class="detail-value">${account.accountName}</div>
        </div>
        <div class="detail-item">
          <div class="detail-label">Phone</div>
          <div class="detail-value">${account.accountPhone}</div>
        </div>
        <div class="detail-item">
          <div class="detail-label">Email</div>
          <div class="detail-value">${account.accountEmail}</div>
        </div>
        <div class="detail-item">
          <div class="detail-label">Total Flag Score</div>
          <div class="detail-value">
            <span class="flag-score ${getSeverityByScore(account.flagScore)}">${account.flagScore || 0}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="flag-detail-section">
      <h3>📋 Flag Details</h3>
      <div class="detail-grid">
        ${Object.keys(account.details || {}).map(key => `
          <div class="detail-item">
            <div class="detail-label">${formatKey(key)}</div>
            <div class="detail-value">${formatValue(account.details[key])}</div>
          </div>
        `).join('')}
      </div>
    </div>
  `;
  
  // Add timeline if actions exist
  if (account.actions && account.actions.length > 0) {
    detailsHTML += `
      <div class="flag-detail-section">
        <h3>📅 Action Timeline</h3>
        <div class="timeline">
          ${account.actions.map(action => `
            <div class="timeline-item">
              <div class="timeline-date">${new Date(action.timestamp).toLocaleString()}</div>
              <div class="timeline-content"><strong>${action.action}</strong></div>
            </div>
          `).join('')}
        </div>
      </div>
    `;
  }
  
  // Add notes if available
  if (account.notes) {
    detailsHTML += `
      <div class="flag-detail-section">
        <h3>📝 Notes</h3>
        <div class="detail-item">
          <div class="detail-value">${account.notes}</div>
        </div>
      </div>
    `;
  }
  
  document.getElementById('flagDetailsContent').innerHTML = detailsHTML;
  document.getElementById('flagDetailsModal').classList.add('active');
}

// Close flag details
window.closeFlagDetails = function() {
  document.getElementById('flagDetailsModal').classList.remove('active');
  selectedFlag = null;
}

// Resolve flag
window.resolveFlag = async function() {
  if (!selectedFlag) return;
  
  const confirmed = await showConfirm(
    `Are you sure you want to resolve this flag for ${selectedFlag.accountName}?`,
    '✅ Resolve Flag',
    'Resolve'
  );
  
  if (!confirmed) return;
  
  try {
    const accountPath = selectedFlag.accountType === 'driver' ? 'drivers' : 'users';
    const flagCollectionPath = selectedFlag.accountType === 'driver' ? 'driverFlags' : 'userFlags';
    const flagRef = ref(db, `${flagCollectionPath}/${selectedFlag.accountId}/${selectedFlag.flagId}`);
    
    await update(flagRef, {
      status: 'resolved',
      resolvedDate: Date.now(),
      resolvedBy: 'Admin'
    });
    
    // Update flag score
    const accountRef = ref(db, `${accountPath}/${selectedFlag.accountId}`);
    const snapshot = await get(accountRef);
    const account = snapshot.val();
    const newScore = Math.max(0, (account.flagScore || 0) - (selectedFlag.points || 0));
    
    await update(accountRef, {
      flagScore: newScore,
      flagStatus: newScore > 300 ? 'suspended' : newScore > 150 ? 'restricted' : newScore > 50 ? 'monitored' : 'good'
    });
    
    showMessage('Flag resolved successfully', 'success');
    closeFlagDetails();
  } catch (error) {
    showMessage('Error resolving flag: ' + error.message, 'error');
  }
}

// Escalate flag
window.escalateFlag = async function() {
  if (!selectedFlag) return;
  
  const confirmed = await showConfirm(
    `Are you sure you want to escalate this flag for ${selectedFlag.accountName}? This will increase the severity.`,
    '⚠️ Escalate Flag',
    'Escalate'
  );
  
  if (!confirmed) return;
  
  try {
    const accountPath = selectedFlag.accountType === 'driver' ? 'drivers' : 'users';
    const flagCollectionPath = selectedFlag.accountType === 'driver' ? 'driverFlags' : 'userFlags';
    const flagRef = ref(db, `${flagCollectionPath}/${selectedFlag.accountId}/${selectedFlag.flagId}`);
    
    const newSeverity = selectedFlag.severity === 'medium' ? 'high' : 
                       selectedFlag.severity === 'high' ? 'critical' : 'critical';
    const additionalPoints = 25;
    
    await update(flagRef, {
      severity: newSeverity,
      points: (selectedFlag.points || 0) + additionalPoints
    });
    
    // Update flag score
    const accountRef = ref(db, `${accountPath}/${selectedFlag.accountId}`);
    const snapshot = await get(accountRef);
    const account = snapshot.val();
    const newScore = (account.flagScore || 0) + additionalPoints;
    
    await update(accountRef, {
      flagScore: newScore,
      flagStatus: newScore > 300 ? 'suspended' : newScore > 150 ? 'restricted' : newScore > 50 ? 'monitored' : 'good'
    });
    
    showMessage('Flag escalated successfully', 'success');
    closeFlagDetails();
  } catch (error) {
    showMessage('Error escalating flag: ' + error.message, 'error');
  }
}

// Dismiss flag
window.dismissFlag = async function() {
  if (!selectedFlag) return;
  
  const confirmed = await showConfirm(
    `Are you sure you want to dismiss this flag for ${selectedFlag.accountName}? This will mark it as a false positive.`,
    '🗑️ Dismiss Flag',
    'Dismiss'
  );
  
  if (!confirmed) return;
  
  try {
    const accountPath = selectedFlag.accountType === 'driver' ? 'drivers' : 'users';
    const flagRef = ref(db, `${accountPath}/${selectedFlag.accountId}/flags/${selectedFlag.flagId}`);
    
    await update(flagRef, {
      status: 'dismissed',
      dismissedDate: Date.now(),
      dismissedBy: 'Admin'
    });
    
    showMessage('Flag dismissed successfully', 'success');
    closeFlagDetails();
  } catch (error) {
    showMessage('Error dismissing flag: ' + error.message, 'error');
  }
}

// Export report
window.exportReport = function() {
  const data = filteredAccounts;
  
  let csv = 'Account Name,Account Type,Phone,Flag Type,Severity,Points,Status,Flagged Date\n';
  
  data.forEach(account => {
    const flagType = FLAG_TYPES[account.type] || {};
    const name = (account.accountName || 'Unknown').replace(/,/g, ';');
    const phone = account.accountPhone || 'N/A';
    const type = (flagType.name || account.type).replace(/,/g, ';');
    const severity = account.severity || 'unknown';
    const points = account.points || 0;
    const status = account.status || 'unknown';
    const date = new Date(account.timestamp).toLocaleDateString();
    
    csv += `${name},${account.accountType},${phone},${type},${severity},${points},${status},${date}\n`;
  });
  
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const link = document.createElement('a');
  const url = URL.createObjectURL(blob);
  
  link.setAttribute('href', url);
  link.setAttribute('download', `flagged_accounts_${new Date().toISOString().split('T')[0]}.csv`);
  link.style.visibility = 'hidden';
  
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  
  showMessage('Report exported successfully', 'success');
}

// Run auto-detection
window.runAutoDetection = async function() {
  document.getElementById('detectionModal').classList.add('active');
  document.getElementById('closeDetectionBtn').disabled = true;
  document.getElementById('progressFill').style.width = '0%';
  document.getElementById('progressText').textContent = 'Starting detection...';
  document.getElementById('detectionResults').innerHTML = '';
  
  const results = [];
  
  try {
    // Step 1: Check driver contributions (20%)
    updateProgress(10, 'Analyzing driver contributions...');
    const contributionFlags = await detectLowContributions();
    results.push(...contributionFlags);
    
    // Step 2: Check inactive accounts (40%)
    updateProgress(30, 'Checking for inactive accounts...');
    const inactiveFlags = await detectInactiveAccounts();
    results.push(...inactiveFlags);
    
    // Step 3: Check customer no-shows (60%)
    updateProgress(50, 'Analyzing customer booking patterns...');
    const noShowFlags = await detectNoShows();
    results.push(...noShowFlags);
    
    // Step 4: Check cancellations (80%)
    updateProgress(70, 'Checking cancellation rates...');
    const cancellationFlags = await detectHighCancellations();
    results.push(...cancellationFlags);
    
    // Step 5: Finalize (100%)
    updateProgress(100, 'Detection complete!');
    
    // Display results
    displayDetectionResults(results);
    
    document.getElementById('closeDetectionBtn').disabled = false;
    showMessage(`Detection complete! Found ${results.length} new flags.`, 'success');
    
  } catch (error) {
    showMessage('Error during auto-detection: ' + error.message, 'error');
    document.getElementById('closeDetectionBtn').disabled = false;
  }
}

// Update progress
function updateProgress(percent, text) {
  document.getElementById('progressFill').style.width = percent + '%';
  document.getElementById('progressText').textContent = text;
}

// Display detection results
function displayDetectionResults(results) {
  const container = document.getElementById('detectionResults');
  
  if (results.length === 0) {
    container.innerHTML = '<div class="detection-item no-flag">✅ No new flags detected. All accounts are in good standing.</div>';
    return;
  }
  
  container.innerHTML = results.map(result => `
    <div class="detection-item new-flag">
      🚩 ${result.accountName} - ${result.flagType} (${result.severity})
    </div>
  `).join('');
}

// Close detection modal
window.closeDetectionModal = function() {
  document.getElementById('detectionModal').classList.remove('active');
}

// Detection Functions

// Detect low contributions
async function detectLowContributions() {
  const flags = [];
  const driversRef = ref(db, 'drivers');
  const contributionsRef = ref(db, 'contributions');
  const driverFlagsRef = ref(db, 'driverFlags');
  
  const driversSnapshot = await get(driversRef);
  const contributionsSnapshot = await get(contributionsRef);
  const driverFlagsSnapshot = await get(driverFlagsRef);
  
  if (!driversSnapshot.exists() || !contributionsSnapshot.exists()) return flags;
  
  const drivers = driversSnapshot.val();
  const contributions = contributionsSnapshot.val();
  const existingFlags = driverFlagsSnapshot.exists() ? driverFlagsSnapshot.val() : {};
  
  // Calculate average weekly contribution
  const oneWeekAgo = Date.now() - (7 * 24 * 60 * 60 * 1000);
  const weeklyContributions = {};
  let totalWeeklyContribution = 0;
  let activeDriverCount = 0;
  
  Object.keys(contributions).forEach(contribId => {
    const contrib = contributions[contribId];
    const timestamp = parseInt(contrib.timestamp) * 1000;
    
    if (timestamp > oneWeekAgo) {
      const driverId = contrib.driverId;
      if (!weeklyContributions[driverId]) {
        weeklyContributions[driverId] = 0;
        activeDriverCount++;
      }
      weeklyContributions[driverId] += parseFloat(contrib.amount) || 0;
      totalWeeklyContribution += parseFloat(contrib.amount) || 0;
    }
  });
  
  const averageWeeklyContribution = activeDriverCount > 0 ? totalWeeklyContribution / activeDriverCount : 0;
  const threshold = averageWeeklyContribution * 0.5; // 50% of average
  
  // Check each driver
  Object.keys(drivers).forEach(driverId => {
    const driver = drivers[driverId];
    const driverContribution = weeklyContributions[driverId] || 0;
    
    if (driver.isActive && driverContribution < threshold && averageWeeklyContribution > 0) {
      // Check if flag already exists in separate collection
      const driverFlags = existingFlags[driverId] || {};
      const hasLowContributionFlag = Object.values(driverFlags).some(f => f.type === 'LOW_CONTRIBUTIONS' && f.status === 'active');
      
      if (!hasLowContributionFlag) {
        flags.push(createFlag(driverId, driver, 'LOW_CONTRIBUTIONS', {
          averageContribution: averageWeeklyContribution.toFixed(2),
          driverContribution: driverContribution.toFixed(2),
          percentage: ((driverContribution / averageWeeklyContribution) * 100).toFixed(0),
          period: 'weekly'
        }));
      }
    }
  });
  
  return flags;
}

// Detect inactive accounts
async function detectInactiveAccounts() {
  const flags = [];
  const driversRef = ref(db, 'drivers');
  const driverFlagsRef = ref(db, 'driverFlags');
  
  const driversSnapshot = await get(driversRef);
  const driverFlagsSnapshot = await get(driverFlagsRef);
  
  if (!driversSnapshot.exists()) return flags;
  
  const drivers = driversSnapshot.val();
  const existingFlags = driverFlagsSnapshot.exists() ? driverFlagsSnapshot.val() : {};
  const sevenDaysAgo = Date.now() - (7 * 24 * 60 * 60 * 1000);
  
  Object.keys(drivers).forEach(driverId => {
    const driver = drivers[driverId];
    const lastActive = driver.lastLoginTimestamp || driver.createdAt || 0;
    
    if (lastActive < sevenDaysAgo && driver.isActive) {
      // Check if flag already exists in separate collection
      const driverFlags = existingFlags[driverId] || {};
      const hasInactiveFlag = Object.values(driverFlags).some(f => f.type === 'INACTIVE_ACCOUNT' && f.status === 'active');
      
      if (!hasInactiveFlag) {
        const daysSinceActive = Math.floor((Date.now() - lastActive) / (24 * 60 * 60 * 1000));
        flags.push(createFlag(driverId, driver, 'INACTIVE_ACCOUNT', {
          lastActiveDate: new Date(lastActive).toLocaleDateString(),
          daysSinceActive: daysSinceActive
        }));
      }
    }
  });
  
  return flags;
}

// Detect no-shows
async function detectNoShows() {
  const flags = [];
  const usersRef = ref(db, 'users');
  const bookingsRef = ref(db, 'bookings');
  const userFlagsRef = ref(db, 'userFlags');
  
  const usersSnapshot = await get(usersRef);
  const bookingsSnapshot = await get(bookingsRef);
  const userFlagsSnapshot = await get(userFlagsRef);
  
  if (!usersSnapshot.exists() || !bookingsSnapshot.exists()) return flags;
  
  const users = usersSnapshot.val();
  const bookings = bookingsSnapshot.val();
  const existingFlags = userFlagsSnapshot.exists() ? userFlagsSnapshot.val() : {};
  
  // Count no-shows per customer
  const customerBookings = {};
  
  Object.keys(bookings).forEach(bookingId => {
    const booking = bookings[bookingId];
    const customerId = booking.customerId;
    
    if (!customerBookings[customerId]) {
      customerBookings[customerId] = { total: 0, noShows: 0 };
    }
    
    customerBookings[customerId].total++;
    
    if (booking.status === 'no-show' || booking.customerNoShow === true) {
      customerBookings[customerId].noShows++;
    }
  });
  
  // Check each customer
  Object.keys(users).forEach(userId => {
    const user = users[userId];
    if (user.role === 'customer') {
      const bookingData = customerBookings[userId];
      
      if (bookingData && bookingData.total >= 5) {
        const noShowRate = (bookingData.noShows / bookingData.total) * 100;
        
        if (noShowRate > 20) {
          // Check if flag already exists in separate collection
          const userFlags = existingFlags[userId] || {};
          const hasNoShowFlag = Object.values(userFlags).some(f => f.type === 'NO_SHOW' && f.status === 'active');
          
          if (!hasNoShowFlag) {
            flags.push(createFlag(userId, user, 'NO_SHOW', {
              totalBookings: bookingData.total,
              noShowCount: bookingData.noShows,
              noShowRate: noShowRate.toFixed(1) + '%'
            }));
          }
        }
      }
    }
  });
  
  return flags;
}

// Detect high cancellations
async function detectHighCancellations() {
  const flags = [];
  const driversRef = ref(db, 'drivers');
  const bookingsRef = ref(db, 'bookings');
  const driverFlagsRef = ref(db, 'driverFlags');
  
  const driversSnapshot = await get(driversRef);
  const bookingsSnapshot = await get(bookingsRef);
  const driverFlagsSnapshot = await get(driverFlagsRef);
  
  if (!driversSnapshot.exists() || !bookingsSnapshot.exists()) return flags;
  
  const drivers = driversSnapshot.val();
  const bookings = bookingsSnapshot.val();
  const existingFlags = driverFlagsSnapshot.exists() ? driverFlagsSnapshot.val() : {};
  
  // Count cancellations per driver
  const driverBookings = {};
  
  Object.keys(bookings).forEach(bookingId => {
    const booking = bookings[bookingId];
    const driverId = booking.driverId;
    
    if (driverId) {
      if (!driverBookings[driverId]) {
        driverBookings[driverId] = { total: 0, cancelled: 0 };
      }
      
      driverBookings[driverId].total++;
      
      if (booking.status === 'cancelled' && booking.cancelledBy === 'driver') {
        driverBookings[driverId].cancelled++;
      }
    }
  });
  
  // Check each driver
  Object.keys(drivers).forEach(driverId => {
    const driver = drivers[driverId];
    const bookingData = driverBookings[driverId];
    
    if (bookingData && bookingData.total >= 10) {
      const cancellationRate = (bookingData.cancelled / bookingData.total) * 100;
      
      if (cancellationRate > 15) {
        // Check if flag already exists in separate collection
        const driverFlags = existingFlags[driverId] || {};
        const hasCancellationFlag = Object.values(driverFlags).some(f => f.type === 'HIGH_CANCELLATION_RATE' && f.status === 'active');
        
        if (!hasCancellationFlag) {
          flags.push(createFlag(driverId, driver, 'HIGH_CANCELLATION_RATE', {
            totalBookings: bookingData.total,
            cancelledCount: bookingData.cancelled,
            cancellationRate: cancellationRate.toFixed(1) + '%'
          }));
        }
      }
    }
  });
  
  return flags;
}

// Create flag
function createFlag(accountId, accountData, flagType, details) {
  const flagConfig = FLAG_TYPES[flagType];
  const flagId = push(ref(db, 'temp')).key;
  
  const flag = {
    flagId: flagId,
    type: flagType,
    severity: flagConfig.severity,
    points: flagConfig.points,
    timestamp: Date.now(),
    status: 'active',
    details: details,
    actions: [
      {
        action: 'FLAG_CREATED',
        timestamp: Date.now(),
        adminId: 'AUTO_DETECTION'
      }
    ],
    notes: `Automatically detected by system on ${new Date().toLocaleDateString()}`
  };
  
  // Save to separate flag collection
  const accountPath = flagConfig.category === 'driver' ? 'drivers' : 'users';
  const flagCollectionPath = flagConfig.category === 'driver' ? 'driverFlags' : 'userFlags';
  const flagRef = ref(db, `${flagCollectionPath}/${accountId}/${flagId}`);
  set(flagRef, flag);
  
  // Update account flag score
  const currentScore = accountData.flagScore || 0;
  const newScore = currentScore + flagConfig.points;
  const accountRef = ref(db, `${accountPath}/${accountId}`);
  
  update(accountRef, {
    flagScore: newScore,
    flagStatus: newScore > 300 ? 'suspended' : newScore > 150 ? 'restricted' : newScore > 50 ? 'monitored' : 'good'
  });
  
  return {
    accountName: accountData.fullName || accountData.driverName || accountData.name || 'Unknown',
    flagType: flagConfig.name,
    severity: flagConfig.severity
  };
}

// Helper functions
function getSeverityByScore(score) {
  if (score >= 100) return 'critical';
  if (score >= 75) return 'high';
  if (score >= 50) return 'medium';
  return 'low';
}

function formatKey(key) {
  return key.replace(/([A-Z])/g, ' $1').replace(/^./, str => str.toUpperCase());
}

function formatValue(value) {
  if (typeof value === 'number') {
    return value.toLocaleString();
  }
  if (typeof value === 'boolean') {
    return value ? 'Yes' : 'No';
  }
  return value;
}

// Initialize
loadFlaggedAccounts();
