// Import Firebase SDKs
import { initializeApp } from "https://www.gstatic.com/firebasejs/11.0.0/firebase-app.js";
import { getDatabase, ref, onValue, remove } from "https://www.gstatic.com/firebasejs/11.0.0/firebase-database.js";

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

let queueList = [];
let selectedQueueEntry = null;

// Pagination variables
let currentPage = 1;
let itemsPerPage = 25;
let totalPages = 1;

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

// Load queue from Firebase
function loadQueue() {
  console.log('[QueueManagement] Starting to load queue data...');
  // FIXED: Changed from 'driverQueue' to 'queue' to match actual database structure
  const queueRef = ref(db, 'queue');
  onValue(queueRef, (snapshot) => {
    console.log('[QueueManagement] Received snapshot:', snapshot.exists());
    if (snapshot.exists()) {
      const queue = snapshot.val();
      console.log('[QueueManagement] Queue data:', queue);
      queueList = [];

      Object.keys(queue).forEach(queueId => {
        const entry = queue[queueId];
        console.log('[QueueManagement] Processing queue entry:', queueId, entry);
        queueList.push({
          id: queueId,
          driverName: entry.driverName || 'Unknown Driver',
          driverId: entry.driverRFID || entry.driverId || 'N/A', // Use driverRFID from actual DB
          vehicleType: entry.vehicleType || 'N/A',
          plateNumber: entry.plateNumber || 'N/A',
          todaNumber: entry.todaNumber || 'N/A',
          contributionPaid: entry.contributionPaid || false,
          status: entry.status || 'waiting',
          timestamp: entry.queueTime || entry.timestamp || Date.now(), // Use queueTime from actual DB
          queueTime: entry.queueTime || '',
          ...entry
        });
      });

      console.log('[QueueManagement] Processed queue list:', queueList);

      // Sort by timestamp (oldest first - FIFO)
      queueList.sort((a, b) => {
        const timeA = parseInt(a.timestamp) || 0;
        const timeB = parseInt(b.timestamp) || 0;
        return timeA - timeB;
      });

      updateQueueCount();
      displayQueue();
    } else {
      console.log('[QueueManagement] No queue data found');
      queueList = [];
      updateQueueCount();
      displayEmptyState();
    }
  }, (error) => {
    console.error('[QueueManagement] Error loading queue:', error);
    showMessage('Error loading queue: ' + error.message, 'error');
    document.getElementById('queueList').innerHTML = '<div class="empty-state"><div class="empty-state-icon">❌</div><h3>Error Loading Queue</h3><p>Failed to load queue data</p></div>';
  });
}

// Update queue count
function updateQueueCount() {
  document.getElementById('queueCount').textContent = queueList.length;
}

// Display queue
function displayQueue() {
  const queueListElement = document.getElementById('queueList');

  if (queueList.length === 0) {
    displayEmptyState();
    document.getElementById('paginationControls').style.display = 'none';
    return;
  }

  // Calculate pagination
  totalPages = Math.ceil(queueList.length / itemsPerPage);
  if (currentPage > totalPages) currentPage = totalPages;
  if (currentPage < 1) currentPage = 1;

  const startIndex = (currentPage - 1) * itemsPerPage;
  const endIndex = startIndex + itemsPerPage;
  const paginatedQueue = queueList.slice(startIndex, endIndex);

  queueListElement.innerHTML = paginatedQueue.map((entry, index) => {
    // Handle both numeric timestamp and formatted string timestamp
    let timestamp;
    if (typeof entry.timestamp === 'string' && entry.timestamp.includes('-')) {
      // Already formatted timestamp like "2025-11-01 09:29:47"
      timestamp = new Date(entry.timestamp.replace(' ', 'T'));
    } else {
      timestamp = new Date(parseInt(entry.timestamp) || 0);
    }
    
    const waitingTime = getWaitingTime(entry.timestamp);
    const actualIndex = startIndex + index + 1;
    
    // Show contribution status badge
    const contributionBadge = entry.contributionPaid 
      ? '<span class="contribution-paid">✓ Paid</span>' 
      : '<span class="contribution-unpaid">⚠ Not Paid</span>';

    return `
      <div class="queue-item">
        <div class="queue-position">${actualIndex}</div>
        <div class="queue-driver-info">
          <div class="queue-driver-name">
            ${entry.driverName || 'Unknown Driver'}
            ${contributionBadge}
          </div>
          <div class="queue-driver-details">
            RFID: ${entry.driverId || 'N/A'} •
            TODA: ${entry.todaNumber || 'N/A'} •
            Vehicle: ${entry.vehicleType || 'N/A'} •
            Plate: ${entry.plateNumber || 'N/A'}
          </div>
          <div class="queue-timestamp">
            Joined: ${timestamp.toLocaleString()} • Waiting: ${waitingTime} • Status: ${entry.status || 'waiting'}
          </div>
        </div>
        <div class="queue-actions">
          <button class="btn-remove" onclick="showRemoveModal('${entry.id}')">
            🗑️ Remove
          </button>
        </div>
      </div>
    `;
  }).join('');
  
  updatePaginationControls();
}

// Update pagination controls
function updatePaginationControls() {
  const paginationControls = document.getElementById('paginationControls');
  
  if (queueList.length === 0) {
    paginationControls.style.display = 'none';
    return;
  }
  
  paginationControls.style.display = 'flex';

  const startItem = (currentPage - 1) * itemsPerPage + 1;
  const endItem = Math.min(currentPage * itemsPerPage, queueList.length);
  
  document.getElementById('paginationInfo').textContent = 
    `Showing ${startItem}-${endItem} of ${queueList.length}`;

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
    // Show all pages
    for (let i = 1; i <= totalPages; i++) {
      pages.push(i);
    }
  } else {
    // Show ellipsis for many pages
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
  displayQueue();
}

window.nextPage = function() {
  if (currentPage < totalPages) {
    currentPage++;
    displayQueue();
  }
}

window.previousPage = function() {
  if (currentPage > 1) {
    currentPage--;
    displayQueue();
  }
}

window.changeItemsPerPage = function() {
  itemsPerPage = parseInt(document.getElementById('itemsPerPage').value);
  currentPage = 1;
  displayQueue();
}

// Display empty state
function displayEmptyState() {
  document.getElementById('queueList').innerHTML = `
    <div class="empty-state">
      <div class="empty-state-icon">⏳</div>
      <h3>No drivers in queue</h3>
      <p>Drivers will appear here when they join the queue</p>
    </div>
  `;
}

// Calculate waiting time
function getWaitingTime(timestamp) {
  const now = Date.now();
  const diff = now - (timestamp || 0);

  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(minutes / 60);
  const days = Math.floor(hours / 24);

  if (days > 0) return `${days} day${days > 1 ? 's' : ''}`;
  if (hours > 0) return `${hours} hour${hours > 1 ? 's' : ''}`;
  if (minutes > 0) return `${minutes} minute${minutes > 1 ? 's' : ''}`;
  return 'Just now';
}

// Show remove confirmation modal
window.showRemoveModal = function(queueId) {
  selectedQueueEntry = queueList.find(q => q.id === queueId);
  if (!selectedQueueEntry) return;

  document.getElementById('removeDriverName').textContent = selectedQueueEntry.driverName || 'Unknown Driver';
  document.getElementById('removeModal').classList.add('active');
}

// Close remove modal
window.closeRemoveModal = function() {
  document.getElementById('removeModal').classList.remove('active');
  selectedQueueEntry = null;
}

// Confirm remove from queue
window.confirmRemove = async function() {
  if (!selectedQueueEntry) return;

  try {
    const queueRef = ref(db, `driverQueue/${selectedQueueEntry.id}`);
    await remove(queueRef);

    closeRemoveModal();
    // Queue will auto-update via onValue listener
  } catch (error) {
    showMessage('Failed to remove driver from queue: ' + error.message, 'error');
  }
}

// Close modals when clicking outside
window.addEventListener('click', (event) => {
  if (event.target.classList.contains('modal')) {
    event.target.classList.remove('active');
  }
});

// Initialize
document.addEventListener('DOMContentLoaded', loadQueue);

