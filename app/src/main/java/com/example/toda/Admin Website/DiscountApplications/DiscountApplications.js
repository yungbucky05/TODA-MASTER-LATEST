// Import Firebase SDKs
import { initializeApp } from "https://www.gstatic.com/firebasejs/11.0.0/firebase-app.js";
import { getDatabase, ref, onValue, update } from "https://www.gstatic.com/firebasejs/11.0.0/firebase-database.js";

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

let pendingApplications = [];
let selectedApplication = null;

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

// Load pending discount applications
function loadApplications() {
  const usersRef = ref(db, 'users');
  onValue(usersRef, (snapshot) => {
    if (snapshot.exists()) {
      const users = snapshot.val();
      pendingApplications = [];

      Object.keys(users).forEach(userId => {
        const user = users[userId];
        if (user.discountApplication && user.discountApplication.status === 'pending') {
          pendingApplications.push({
            userId: userId,
            userName: user.fullName || 'Unknown',
            userEmail: user.email || 'N/A',
            userPhone: user.phoneNumber || 'N/A',
            ...user.discountApplication
          });
        }
      });

      // Sort by timestamp (newest first)
      pendingApplications.sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0));

      updatePendingCount();
      displayApplications();
    } else {
      pendingApplications = [];
      updatePendingCount();
      displayEmptyState();
    }
  }, (error) => {
    showMessage('Error loading applications: ' + error.message, 'error');
  });
}

// Update pending count
function updatePendingCount() {
  document.getElementById('pendingCount').textContent = pendingApplications.length;
}

// Display applications
function displayApplications() {
  const applicationsList = document.getElementById('applicationsList');

  if (pendingApplications.length === 0) {
    displayEmptyState();
    document.getElementById('paginationControls').style.display = 'none';
    return;
  }

  // Calculate pagination
  totalPages = Math.ceil(pendingApplications.length / itemsPerPage);
  if (currentPage > totalPages) currentPage = totalPages;
  if (currentPage < 1) currentPage = 1;

  const startIndex = (currentPage - 1) * itemsPerPage;
  const endIndex = startIndex + itemsPerPage;
  const paginatedApplications = pendingApplications.slice(startIndex, endIndex);

  applicationsList.innerHTML = paginatedApplications.map(app => {
    const date = new Date(app.timestamp || 0);
    const discountClass = `discount-${app.discountType?.toLowerCase() || 'senior'}`;

    return `
      <div class="application-card" onclick="showApplicationDetails('${app.userId}')">
        <div class="application-header">
          <div>
            <div class="applicant-name">${app.userName}</div>
            <div class="application-date">Applied: ${date.toLocaleString()}</div>
          </div>
          <span class="discount-badge ${discountClass}">${app.discountType || 'Senior'}</span>
        </div>
        <div class="application-info">
          <div class="info-item">
            <span class="info-label">Email:</span> ${app.userEmail}
          </div>
          <div class="info-item">
            <span class="info-label">Phone:</span> ${app.userPhone}
          </div>
          <div class="info-item">
            <span class="info-label">ID Number:</span> ${app.idNumber || 'N/A'}
          </div>
        </div>
      </div>
    `;
  }).join('');
  
  updatePaginationControls();
}

// Update pagination controls
function updatePaginationControls() {
  const paginationControls = document.getElementById('paginationControls');
  
  if (pendingApplications.length === 0) {
    paginationControls.style.display = 'none';
    return;
  }
  
  paginationControls.style.display = 'flex';

  const startItem = (currentPage - 1) * itemsPerPage + 1;
  const endItem = Math.min(currentPage * itemsPerPage, pendingApplications.length);
  
  document.getElementById('paginationInfo').textContent = 
    `Showing ${startItem}-${endItem} of ${pendingApplications.length}`;

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
  displayApplications();
}

window.nextPage = function() {
  if (currentPage < totalPages) {
    currentPage++;
    displayApplications();
  }
}

window.previousPage = function() {
  if (currentPage > 1) {
    currentPage--;
    displayApplications();
  }
}

window.changeItemsPerPage = function() {
  itemsPerPage = parseInt(document.getElementById('itemsPerPage').value);
  currentPage = 1;
  displayApplications();
}

// Display empty state
function displayEmptyState() {
  document.getElementById('applicationsList').innerHTML = `
    <div class="empty-state">
      <div class="empty-state-icon">✅</div>
      <h3>No pending applications</h3>
      <p>All discount applications have been reviewed</p>
    </div>
  `;
}

// Show application details modal
window.showApplicationDetails = function(userId) {
  selectedApplication = pendingApplications.find(app => app.userId === userId);
  if (!selectedApplication) return;

  const date = new Date(selectedApplication.timestamp || 0);

  let documentsHtml = '';
  if (selectedApplication.documentUrl) {
    documentsHtml = `
      <div class="document-preview">
        <img src="${selectedApplication.documentUrl}"
             class="document-thumb"
             alt="ID Document"
             onclick="showImagePreview('${selectedApplication.documentUrl}')">
      </div>
    `;
  } else {
    documentsHtml = '<p style="color: #999;">No document uploaded</p>';
  }

  document.getElementById('modalBody').innerHTML = `
    <div class="detail-section">
      <h3>Applicant Information</h3>
      <div class="detail-grid">
        <div class="detail-item">
          <div class="detail-label">Full Name</div>
          <div class="detail-value">${selectedApplication.userName}</div>
        </div>
        <div class="detail-item">
          <div class="detail-label">Email</div>
          <div class="detail-value">${selectedApplication.userEmail}</div>
        </div>
        <div class="detail-item">
          <div class="detail-label">Phone</div>
          <div class="detail-value">${selectedApplication.userPhone}</div>
        </div>
        <div class="detail-item">
          <div class="detail-label">Application Date</div>
          <div class="detail-value">${date.toLocaleString()}</div>
        </div>
      </div>
    </div>

    <div class="detail-section">
      <h3>Discount Information</h3>
      <div class="detail-grid">
        <div class="detail-item">
          <div class="detail-label">Discount Type</div>
          <div class="detail-value">${selectedApplication.discountType || 'Senior Citizen'}</div>
        </div>
        <div class="detail-item">
          <div class="detail-label">ID Number</div>
          <div class="detail-value">${selectedApplication.idNumber || 'N/A'}</div>
        </div>
        <div class="detail-item">
          <div class="detail-label">Discount Rate</div>
          <div class="detail-value">${getDiscountRate(selectedApplication.discountType)}% off</div>
        </div>
      </div>
    </div>

    <div class="detail-section">
      <h3>Supporting Documents</h3>
      ${documentsHtml}
    </div>
  `;

  document.getElementById('applicationModal').classList.add('active');
}

// Get discount rate
function getDiscountRate(type) {
  switch(type?.toLowerCase()) {
    case 'senior': return 20;
    case 'pwd': return 20;
    case 'student': return 10;
    default: return 20;
  }
}

// Show image preview
window.showImagePreview = function(imageUrl) {
  document.getElementById('imagePreview').src = imageUrl;
  document.getElementById('imageModal').classList.add('active');
}

// Close modals
window.closeApplicationModal = function() {
  document.getElementById('applicationModal').classList.remove('active');
  selectedApplication = null;
}

window.closeImageModal = function() {
  document.getElementById('imageModal').classList.remove('active');
}

// Approve application
window.approveApplication = async function() {
  if (!selectedApplication) return;

  const confirmResult = await showConfirm(
    `Approve discount application for ${selectedApplication.userName}?`,
    '✓ Approve Discount Application',
    'Approve'
  );
  
  if (!confirmResult) {
    return;
  }

  try {
    const userRef = ref(db, `users/${selectedApplication.userId}`);
    await update(userRef, {
      'discountApplication/status': 'approved',
      'discountApplication/reviewedAt': Date.now(),
      'hasDiscount': true,
      'discountType': selectedApplication.discountType || 'Senior'
    });

    showMessage(`Application approved for ${selectedApplication.userName}`, 'success');
    closeApplicationModal();
  } catch (error) {
    showMessage('Failed to approve application: ' + error.message, 'error');
  }
}

// Reject application
window.rejectApplication = async function() {
  if (!selectedApplication) return;

  const confirmResult = await showConfirm(
    `Reject discount application for ${selectedApplication.userName}?`,
    '⚠️ Reject Discount Application',
    'Reject'
  );
  
  if (!confirmResult) {
    return;
  }

  try {
    const userRef = ref(db, `users/${selectedApplication.userId}`);
    await update(userRef, {
      'discountApplication/status': 'rejected',
      'discountApplication/reviewedAt': Date.now(),
      'hasDiscount': false
    });

    showMessage(`Application rejected for ${selectedApplication.userName}`, 'success');
    closeApplicationModal();
  } catch (error) {
    showMessage('Failed to reject application: ' + error.message, 'error');
  }
}

// Show message
function showMessage(text, type = 'success') {
  const messageContainer = document.getElementById('messageContainer');
  const icon = type === 'success' ? '✅' : '❌';

  const messageDiv = document.createElement('div');
  messageDiv.className = `message ${type}`;
  messageDiv.innerHTML = `
    <div class="message-icon">${icon}</div>
    <div class="message-text">${text}</div>
  `;

  messageContainer.appendChild(messageDiv);

  // Auto-remove after 5 seconds
  setTimeout(() => {
    messageDiv.remove();
  }, 5000);
}

// Close modals when clicking outside
window.addEventListener('click', (event) => {
  if (event.target.classList.contains('modal')) {
    event.target.classList.remove('active');
  }
});

// Initialize
document.addEventListener('DOMContentLoaded', loadApplications);

