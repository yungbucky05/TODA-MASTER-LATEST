// Import Firebase SDKs
import { initializeApp } from "https://www.gstatic.com/firebasejs/11.0.0/firebase-app.js";
import { getDatabase, ref, onValue, set, push, update } from "https://www.gstatic.com/firebasejs/11.0.0/firebase-database.js";

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

let currentFareData = null;
let fareHistory = [];
let pendingUpdate = null;
let currentTab = 'special'; // Track current tab

// Pagination variables
let currentPage = 1;
let itemsPerPage = 10;
let totalPages = 1;

// Load current fare configuration
function loadCurrentFare() {
  const fareRef = ref(db, 'fareConfig/current');
  onValue(fareRef, (snapshot) => {
    if (snapshot.exists()) {
      currentFareData = snapshot.val();
      displayCurrentFare();
    } else {
      // Initialize with default values if not exists
      currentFareData = {
        special: {
          baseFare: 25.00,
          perKmRate: 5.00
        },
        regular: {
          baseFare: 8.00,
          perKmRate: 2.00
        },
        lastUpdated: Date.now(),
        updatedBy: 'System'
      };
      displayCurrentFare();
    }
  }, (error) => {
    showMessage('Error loading fare configuration', 'error');
  });
}

// Load fare change history
function loadFareHistory() {
  const historyRef = ref(db, 'fareConfig/history');
  onValue(historyRef, (snapshot) => {
    if (snapshot.exists()) {
      const history = snapshot.val();
      fareHistory = [];

      Object.keys(history).forEach(historyId => {
        fareHistory.push({
          id: historyId,
          ...history[historyId]
        });
      });

      // Sort by timestamp (newest first)
      fareHistory.sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0));

      displayFareHistory();
    } else {
      fareHistory = [];
      displayEmptyHistory();
    }
  }, (error) => {
    showMessage('Error loading fare history', 'error');
  });
}

// Display current fare
function displayCurrentFare() {
  if (!currentFareData) return;

  // Display special trip fares
  const specialFare = currentFareData.special || { baseFare: 25, perKmRate: 5 };
  document.getElementById('specialBaseFare').textContent = formatCurrency(specialFare.baseFare);
  document.getElementById('specialPerKm').textContent = formatCurrency(specialFare.perKmRate);

  // Display regular trip fares
  const regularFare = currentFareData.regular || { baseFare: 8, perKmRate: 2 };
  document.getElementById('regularBaseFare').textContent = formatCurrency(regularFare.baseFare);
  document.getElementById('regularPerKm').textContent = formatCurrency(regularFare.perKmRate);
  
  const lastUpdated = currentFareData.lastUpdated ? new Date(currentFareData.lastUpdated) : null;
  document.getElementById('lastUpdated').textContent = lastUpdated ? 
    formatRelativeTime(lastUpdated) : 'Never';

  // Populate form placeholders with current values
  document.getElementById('specialBaseFareInput').placeholder = `Current: ₱${specialFare.baseFare?.toFixed(2)}`;
  document.getElementById('specialPerKmInput').placeholder = `Current: ₱${specialFare.perKmRate?.toFixed(2)}`;
  document.getElementById('regularBaseFareInput').placeholder = `Current: ₱${regularFare.baseFare?.toFixed(2)}`;
  document.getElementById('regularPerKmInput').placeholder = `Current: ₱${regularFare.perKmRate?.toFixed(2)}`;

  // Update small helper text
  const specialSmalls = document.querySelectorAll('#specialForm small');
  specialSmalls[0].textContent = `Current: ₱${specialFare.baseFare?.toFixed(2)} for first kilometer`;
  specialSmalls[1].textContent = `Current: ₱${specialFare.perKmRate?.toFixed(2)} per additional kilometer`;

  const regularSmalls = document.querySelectorAll('#regularForm small');
  regularSmalls[0].textContent = `Current: ₱${regularFare.baseFare?.toFixed(2)} for first kilometer`;
  regularSmalls[1].textContent = `Current: ₱${regularFare.perKmRate?.toFixed(2)} per additional kilometer`;
}

// Switch between tabs
window.switchTab = function(tab) {
  currentTab = tab;
  
  // Update tab buttons
  document.querySelectorAll('.tab-btn').forEach(btn => {
    btn.classList.toggle('active', btn.dataset.tab === tab);
  });
  
  // Update tab content
  document.querySelectorAll('.tab-content').forEach(content => {
    content.classList.remove('active');
  });
  document.getElementById(tab + 'Form').classList.add('active');
}

// Display fare history
function displayFareHistory() {
  const historyContainer = document.getElementById('fareHistory');

  if (fareHistory.length === 0) {
    displayEmptyHistory();
    document.getElementById('paginationControls').style.display = 'none';
    return;
  }

  // Calculate pagination
  totalPages = Math.ceil(fareHistory.length / itemsPerPage);
  if (currentPage > totalPages) currentPage = totalPages;
  if (currentPage < 1) currentPage = 1;

  const startIndex = (currentPage - 1) * itemsPerPage;
  const endIndex = startIndex + itemsPerPage;
  const paginatedHistory = fareHistory.slice(startIndex, endIndex);

  historyContainer.innerHTML = paginatedHistory.map(entry => {
    const date = new Date(entry.timestamp);
    const tripType = entry.tripType || 'special'; // Default to special for old entries
    const tripTypeName = tripType === 'special' ? 'Special Trip' : 'Regular Trip';
    
    return `
      <div class="history-card">
        <div class="history-header">
          <div>
            <div class="history-date">${date.toLocaleDateString()}</div>
            <div class="history-time">${date.toLocaleTimeString()}</div>
          </div>
          <div>
            <span class="trip-type-badge ${tripType}">${tripTypeName}</span>
            <div class="history-badge">Updated</div>
          </div>
        </div>
        <div class="history-changes">
          <div class="change-item">
            <div class="change-label">Base Fare</div>
            <div class="change-value">
              ${entry.oldBaseFare ? `<span class="change-old">₱${entry.oldBaseFare.toFixed(2)}</span> <span class="change-arrow">→</span>` : ''}
              ${formatCurrency(entry.newBaseFare)}
            </div>
          </div>
          <div class="change-item">
            <div class="change-label">Per KM Rate</div>
            <div class="change-value">
              ${entry.oldPerKmRate ? `<span class="change-old">₱${entry.oldPerKmRate.toFixed(2)}</span> <span class="change-arrow">→</span>` : ''}
              ${formatCurrency(entry.newPerKmRate)}
            </div>
          </div>
        </div>
        ${entry.reason ? `
          <div class="history-reason">
            <strong>Reason:</strong>
            <p>${entry.reason}</p>
          </div>
        ` : ''}
      </div>
    `;
  }).join('');

  updatePaginationControls();
}

// Display empty history
function displayEmptyHistory() {
  document.getElementById('fareHistory').innerHTML = `
    <div class="empty-state">
      <div class="empty-state-icon">📋</div>
      <h3>No history yet</h3>
      <p>Fare changes will appear here</p>
    </div>
  `;
}

// Update pagination controls
function updatePaginationControls() {
  const paginationControls = document.getElementById('paginationControls');
  
  if (fareHistory.length === 0) {
    paginationControls.style.display = 'none';
    return;
  }
  
  paginationControls.style.display = 'flex';

  const startItem = (currentPage - 1) * itemsPerPage + 1;
  const endItem = Math.min(currentPage * itemsPerPage, fareHistory.length);
  
  document.getElementById('paginationInfo').textContent = 
    `Showing ${startItem}-${endItem} of ${fareHistory.length}`;

  // Update button states
  document.getElementById('firstPageBtn').disabled = currentPage === 1;
  document.getElementById('prevPageBtn').disabled = currentPage === 1;
  document.getElementById('nextPageBtn').disabled = currentPage === totalPages;
  document.getElementById('lastPageBtn').disabled = currentPage === totalPages;

  // Generate page numbers
  renderPageNumbers();
}

// Render page number buttons
function renderPageNumbers() {
  const pageNumbers = document.getElementById('pageNumbers');
  const maxVisiblePages = 5;
  let pages = [];

  if (totalPages <= maxVisiblePages) {
    for (let i = 1; i <= totalPages; i++) {
      pages.push(i);
    }
  } else {
    if (currentPage <= 3) {
      pages = [1, 2, 3, 4, '...', totalPages];
    } else if (currentPage >= totalPages - 2) {
      pages = [1, '...', totalPages - 3, totalPages - 2, totalPages - 1, totalPages];
    } else {
      pages = [1, '...', currentPage - 1, currentPage, currentPage + 1, '...', totalPages];
    }
  }

  pageNumbers.innerHTML = pages.map(page => {
    if (page === '...') {
      return '<button class="page-btn ellipsis">...</button>';
    }
    const activeClass = page === currentPage ? 'active' : '';
    return `<button class="page-btn ${activeClass}" onclick="goToPage(${page})">${page}</button>`;
  }).join('');
}

// Pagination functions
window.goToPage = function(page) {
  if (page === 'last') {
    currentPage = totalPages;
  } else {
    currentPage = parseInt(page);
  }
  displayFareHistory();
}

window.nextPage = function() {
  if (currentPage < totalPages) {
    currentPage++;
    displayFareHistory();
  }
}

window.previousPage = function() {
  if (currentPage > 1) {
    currentPage--;
    displayFareHistory();
  }
}

window.changeItemsPerPage = function() {
  itemsPerPage = parseInt(document.getElementById('itemsPerPage').value);
  currentPage = 1;
  displayFareHistory();
}

// Handle form submission
document.getElementById('fareUpdateForm').addEventListener('submit', function(e) {
  e.preventDefault();
  
  // Get values based on current tab
  let baseFare, perKmRate;
  if (currentTab === 'special') {
    baseFare = parseFloat(document.getElementById('specialBaseFareInput').value);
    perKmRate = parseFloat(document.getElementById('specialPerKmInput').value);
  } else {
    baseFare = parseFloat(document.getElementById('regularBaseFareInput').value);
    perKmRate = parseFloat(document.getElementById('regularPerKmInput').value);
  }

  const reason = document.getElementById('reason').value.trim();

  // Validation
  if (!baseFare || isNaN(baseFare) || baseFare <= 0) {
    showMessage(`Please enter a valid base fare for ${currentTab === 'special' ? 'Special' : 'Regular'} trip`, 'error');
    if (currentTab === 'special') {
      document.getElementById('specialBaseFareInput').focus();
    } else {
      document.getElementById('regularBaseFareInput').focus();
    }
    return;
  }

  if (!perKmRate || isNaN(perKmRate) || perKmRate <= 0) {
    showMessage(`Please enter a valid per kilometer rate for ${currentTab === 'special' ? 'Special' : 'Regular'} trip`, 'error');
    if (currentTab === 'special') {
      document.getElementById('specialPerKmInput').focus();
    } else {
      document.getElementById('regularPerKmInput').focus();
    }
    return;
  }

  if (!reason) {
    showMessage('Please provide a reason for the fare change', 'error');
    document.getElementById('reason').focus();
    return;
  }

  // Store pending update and show confirmation modal
  pendingUpdate = {
    tripType: currentTab,
    baseFare: baseFare,
    perKmRate: perKmRate,
    reason: reason
  };

  showConfirmModal();
});

// Show confirmation modal
function showConfirmModal() {
  if (!pendingUpdate) return;

  // Update trip type badge
  const tripTypeBadge = document.querySelector('.trip-type-badge');
  tripTypeBadge.textContent = pendingUpdate.tripType === 'special' ? 'Special Trip' : 'Regular Trip';
  tripTypeBadge.className = `trip-type-badge ${pendingUpdate.tripType}`;

  document.getElementById('confirmBaseFare').textContent = formatCurrency(pendingUpdate.baseFare);
  document.getElementById('confirmPerKm').textContent = formatCurrency(pendingUpdate.perKmRate);
  document.getElementById('confirmReason').textContent = pendingUpdate.reason;

  document.getElementById('confirmModal').classList.add('active');
}

// Close confirmation modal
window.closeConfirmModal = function() {
  document.getElementById('confirmModal').classList.remove('active');
  pendingUpdate = null;
}

// Confirm and apply fare update
window.confirmUpdate = async function() {
  if (!pendingUpdate) return;

  try {
    const timestamp = Date.now();
    const tripType = pendingUpdate.tripType;
    
    // Get old values for history
    const oldFare = currentFareData[tripType] || { baseFare: 0, perKmRate: 0 };
    
    // Save to history
    const historyRef = ref(db, 'fareConfig/history');
    const newHistoryRef = push(historyRef);
    
    await set(newHistoryRef, {
      timestamp: timestamp,
      tripType: tripType,
      oldBaseFare: oldFare.baseFare,
      oldPerKmRate: oldFare.perKmRate,
      newBaseFare: pendingUpdate.baseFare,
      newPerKmRate: pendingUpdate.perKmRate,
      reason: pendingUpdate.reason,
      updatedBy: 'Admin'
    });

    // Update current fare config for specific trip type
    const currentRef = ref(db, `fareConfig/current/${tripType}`);
    await set(currentRef, {
      baseFare: pendingUpdate.baseFare,
      perKmRate: pendingUpdate.perKmRate
    });

    // Update lastUpdated timestamp and metadata
    const metaRef = ref(db, 'fareConfig/current');
    await update(metaRef, {
      lastUpdated: timestamp,
      updatedBy: 'Admin'
    });

    // ALSO CREATE/UPDATE fareMatrix node for mobile app compatibility
    const fareMatrixRef = ref(db, `fareMatrix/${tripType}`);
    await set(fareMatrixRef, {
      baseFare: pendingUpdate.baseFare,
      perKmRate: pendingUpdate.perKmRate,
      lastUpdated: timestamp,
      updatedBy: 'Admin'
    });

    showMessage(`${tripType === 'special' ? 'Special' : 'Regular'} trip fare rates updated successfully!`, 'success');
    closeConfirmModal();
    resetForm();
  } catch (error) {
    showMessage('Failed to update fare rates. Please try again.', 'error');
  }
}

// Reset form
window.resetForm = function() {
  document.getElementById('fareUpdateForm').reset();
  pendingUpdate = null;
}

// Show message
function showMessage(text, type = 'success') {
  const container = document.getElementById('messageContainer');
  const messageId = 'msg-' + Date.now();
  
  const icon = type === 'success' ? '✓' : '✕';
  const title = type === 'success' ? 'Success' : 'Error';
  
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

// Format currency
function formatCurrency(amount) {
  return '₱' + parseFloat(amount).toFixed(2);
}

// Format relative time
function formatRelativeTime(date) {
  const now = new Date();
  const diff = now - date;
  const seconds = Math.floor(diff / 1000);
  const minutes = Math.floor(seconds / 60);
  const hours = Math.floor(minutes / 60);
  const days = Math.floor(hours / 24);

  if (days > 0) return `${days} day${days > 1 ? 's' : ''} ago`;
  if (hours > 0) return `${hours} hour${hours > 1 ? 's' : ''} ago`;
  if (minutes > 0) return `${minutes} minute${minutes > 1 ? 's' : ''} ago`;
  return 'Just now';
}

// Initialize
document.addEventListener('DOMContentLoaded', () => {
  loadCurrentFare();
  loadFareHistory();
});
