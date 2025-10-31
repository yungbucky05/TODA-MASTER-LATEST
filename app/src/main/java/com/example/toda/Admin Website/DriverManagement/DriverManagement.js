// Import Firebase SDKs
import { initializeApp } from "https://www.gstatic.com/firebasejs/11.0.0/firebase-app.js";
import { getDatabase, ref, onValue, update, push, set } from "https://www.gstatic.com/firebasejs/11.0.0/firebase-database.js";
import { getStorage, ref as storageRef, getDownloadURL } from "https://www.gstatic.com/firebasejs/11.0.0/firebase-storage.js";

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
const storage = getStorage(app);

let allDrivers = [];
let filteredDrivers = [];
let selectedDriver = null;
let currentStep = 1; // Track current onboarding step

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

// Custom prompt dialog
let promptCallback = null;

function showPrompt(message, title = 'Enter Information', defaultValue = '') {
  return new Promise((resolve) => {
    const modal = document.getElementById('promptModal');
    const titleEl = document.getElementById('promptTitle');
    const messageEl = document.getElementById('promptMessage');
    const inputEl = document.getElementById('promptInput');
    
    titleEl.textContent = title;
    messageEl.textContent = message;
    inputEl.value = defaultValue;
    
    promptCallback = (result) => {
      modal.classList.remove('active');
      promptCallback = null;
      resolve(result);
    };
    
    modal.classList.add('active');
    setTimeout(() => inputEl.focus(), 100);
  });
}

window.handlePromptSubmit = function() {
  const value = document.getElementById('promptInput').value;
  if (promptCallback) promptCallback(value);
}

window.closePromptModal = function() {
  if (promptCallback) promptCallback(null);
}

// Load all drivers from Firebase
function loadDrivers() {
  const driversRef = ref(db, 'drivers');
  onValue(driversRef, (snapshot) => {
    if (snapshot.exists()) {
      const drivers = snapshot.val();
      allDrivers = [];

      Object.keys(drivers).forEach(driverId => {
        const driver = drivers[driverId];
        allDrivers.push({
          id: driverId,
          ...driver
        });
      });

      updateStats();
      applyFilters();
    } else {
      document.getElementById('driversList').innerHTML = '<div class="empty-state"><div class="empty-state-icon">👥</div><p>No drivers registered yet</p></div>';
    }
  }, (error) => {
    showMessage('Error loading drivers: ' + error.message, 'error');
    document.getElementById('driversList').innerHTML = '<div class="empty-state"><div class="empty-state-icon">❌</div><p>Error loading drivers</p></div>';
  });
}

// Update statistics
function updateStats() {
  const totalDrivers = allDrivers.length;
  const pendingVerification = allDrivers.filter(d => !d.verificationStatus || d.verificationStatus === 'pending').length;
  const needRfid = allDrivers.filter(d => (!d.rfidNumber && !d.rfidUID) || d.rfidNumber === '' || d.rfidUID === '' || d.needsRfidAssignment).length;
  const activeDrivers = allDrivers.filter(d => (d.rfidNumber || d.rfidUID) && d.isActive).length;

  document.getElementById('totalDrivers').textContent = totalDrivers;
  document.getElementById('pendingVerification').textContent = pendingVerification;
  document.getElementById('needRfid').textContent = needRfid;
  document.getElementById('activeDrivers').textContent = activeDrivers;
}

// Apply filters
function applyFilters() {
  const searchQuery = document.getElementById('searchInput').value.toLowerCase();
  const statusFilter = document.getElementById('statusFilter').value;

  filteredDrivers = allDrivers.filter(driver => {
    // Search filter
    const driverName = driver.driverName || `${driver.firstName || ''} ${driver.lastName || ''}`.trim();
    const matchesSearch = !searchQuery ||
      driverName?.toLowerCase().includes(searchQuery) ||
      driver.firstName?.toLowerCase().includes(searchQuery) ||
      driver.lastName?.toLowerCase().includes(searchQuery) ||
      driver.phoneNumber?.toLowerCase().includes(searchQuery) ||
      driver.rfidNumber?.toLowerCase().includes(searchQuery) ||
      driver.rfidUID?.toLowerCase().includes(searchQuery) ||
      driver.plateNumber?.toLowerCase().includes(searchQuery) ||
      driver.licenseNumber?.toLowerCase().includes(searchQuery) ||
      driver.todaNumber?.toLowerCase().includes(searchQuery);

    // Status filter
    let matchesStatus = true;
    if (statusFilter === 'pending') {
      matchesStatus = !driver.verificationStatus || driver.verificationStatus === 'pending';
    } else if (statusFilter === 'verified') {
      matchesStatus = driver.verificationStatus === 'verified';
    } else if (statusFilter === 'needRfid') {
      matchesStatus = driver.verificationStatus === 'verified' && (!driver.rfidNumber || driver.rfidNumber === '');
    } else if (statusFilter === 'active') {
      matchesStatus = driver.verificationStatus === 'verified' && driver.rfidNumber && driver.rfidNumber !== '';
    }

    return matchesSearch && matchesStatus;
  });

  displayDrivers();
}

// Display drivers
function displayDrivers() {
  const driversList = document.getElementById('driversList');

  if (filteredDrivers.length === 0) {
    driversList.innerHTML = '<div class="empty-state"><div class="empty-state-icon">🔍</div><p>No drivers found matching your criteria</p></div>';
    document.getElementById('paginationControls').style.display = 'none';
    return;
  }

  // Calculate pagination
  totalPages = Math.ceil(filteredDrivers.length / itemsPerPage);
  if (currentPage > totalPages) currentPage = totalPages;
  if (currentPage < 1) currentPage = 1;

  const startIndex = (currentPage - 1) * itemsPerPage;
  const endIndex = startIndex + itemsPerPage;
  const paginatedDrivers = filteredDrivers.slice(startIndex, endIndex);

  driversList.innerHTML = paginatedDrivers.map(driver => {
    const verificationStatus = driver.verificationStatus || 'pending';
    const hasRfid = (driver.rfidNumber && driver.rfidNumber !== '') || (driver.rfidUID && driver.rfidUID !== '');
    
    let statusClass, statusText;
    if (verificationStatus === 'pending') {
      statusClass = 'status-pending';
      statusText = '⏳ Pending Verification';
    } else if (verificationStatus === 'verified' && !hasRfid) {
      statusClass = 'status-need-rfid';
      statusText = '⚠️ Need RFID';
    } else if (verificationStatus === 'verified' && hasRfid) {
      statusClass = 'status-active';
      statusText = '✅ Active';
    } else if (verificationStatus === 'rejected') {
      statusClass = 'status-rejected';
      statusText = '❌ Rejected';
    } else if (hasRfid && driver.isActive) {
      statusClass = 'status-active';
      statusText = '✅ Active';
    } else if (!hasRfid || driver.needsRfidAssignment) {
      statusClass = 'status-need-rfid';
      statusText = '⚠️ Need RFID';
    }

    const fullName = driver.driverName || `${driver.firstName || ''} ${driver.middleName || ''} ${driver.lastName || ''}`.trim() || 'Unknown';
    const rfidDisplay = driver.rfidNumber || driver.rfidUID || 'Not assigned';
    
    // Show reassign RFID button for active drivers with RFID
    const reassignButton = (hasRfid && verificationStatus === 'verified') 
      ? `<button class="btn-reassign-rfid" onclick="event.stopPropagation(); showReassignRfidModal('${driver.id}')">🔄 Reassign RFID</button>`
      : '';

    return `
      <div class="driver-card" onclick="showDriverDetails('${driver.id}')">
        <div class="driver-header">
          <div>
            <div class="driver-name">${fullName}</div>
            <div class="driver-email">${driver.phoneNumber || 'No phone'}</div>
          </div>
          <span class="driver-status ${statusClass}">${statusText}</span>
        </div>
        <div class="driver-info">
          <div class="info-item">
            <span class="info-label">Phone:</span> ${driver.phoneNumber || 'N/A'}
          </div>
          <div class="info-item">
            <span class="info-label">RFID:</span> ${rfidDisplay}
          </div>
          <div class="info-item">
            <span class="info-label">License Number:</span> ${driver.licenseNumber || 'N/A'}
          </div>
          <div class="info-item">
            <span class="info-label">TODA Number:</span> ${driver.todaNumber || 'Not assigned'}
          </div>
        </div>
        ${reassignButton}
      </div>
    `;
  }).join('');
  
  updatePaginationControls();
}

// Update pagination controls
function updatePaginationControls() {
  const paginationControls = document.getElementById('paginationControls');
  
  if (filteredDrivers.length === 0) {
    paginationControls.style.display = 'none';
    return;
  }
  
  paginationControls.style.display = 'flex';

  const startItem = (currentPage - 1) * itemsPerPage + 1;
  const endItem = Math.min(currentPage * itemsPerPage, filteredDrivers.length);
  
  document.getElementById('paginationInfo').textContent = 
    `Showing ${startItem}-${endItem} of ${filteredDrivers.length}`;

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
  displayDrivers();
}

window.nextPage = function() {
  if (currentPage < totalPages) {
    currentPage++;
    displayDrivers();
  }
}

window.previousPage = function() {
  if (currentPage > 1) {
    currentPage--;
    displayDrivers();
  }
}

window.changeItemsPerPage = function() {
  itemsPerPage = parseInt(document.getElementById('itemsPerPage').value);
  currentPage = 1;
  displayDrivers();
}

// Show driver details modal
window.showDriverDetails = async function(driverId) {
  selectedDriver = allDrivers.find(d => d.id === driverId);
  if (!selectedDriver) return;

  const verificationStatus = selectedDriver.verificationStatus || 'pending';
  const hasRfid = (selectedDriver.rfidNumber && selectedDriver.rfidNumber !== '') || (selectedDriver.rfidUID && selectedDriver.rfidUID !== '');
  const fullName = selectedDriver.driverName || `${selectedDriver.firstName || ''} ${selectedDriver.middleName || ''} ${selectedDriver.lastName || ''}`.trim() || 'Unknown';
  const hasTodaNumber = selectedDriver.todaNumber && selectedDriver.todaNumber !== '';

  // Determine current step based on driver status
  if (verificationStatus === 'pending' || !verificationStatus) {
    currentStep = 1; // Needs verification
  } else if (verificationStatus === 'verified' && !hasTodaNumber) {
    currentStep = 2; // Needs TODA
  } else if (verificationStatus === 'verified' && hasTodaNumber && !hasRfid) {
    currentStep = 3; // Needs RFID
  } else if (verificationStatus === 'rejected') {
    currentStep = 0; // Rejected - view only
  } else {
    currentStep = 0; // Completed - view only
  }

  // Load license photo if available
  let licensePhotoHTML = '<div class="document-viewer no-image">No license photo uploaded</div>';
  if (selectedDriver.licensePhotoURL) {
    try {
      licensePhotoHTML = `
        <div class="document-viewer">
          <img src="${selectedDriver.licensePhotoURL}" alt="Driver License" />
        </div>
      `;
    } catch (error) {
      licensePhotoHTML = '<div class="document-viewer no-image">Error loading license photo</div>';
    }
  }

  // Get verification badge
  let verificationBadge = '';
  if (verificationStatus === 'pending' || !verificationStatus) {
    verificationBadge = '<span class="verification-badge pending">⏳ Pending Verification</span>';
  } else if (verificationStatus === 'verified') {
    verificationBadge = '<span class="verification-badge verified">✅ Verified</span>';
  } else if (verificationStatus === 'rejected') {
    verificationBadge = '<span class="verification-badge rejected">❌ Rejected</span>';
  }

  // Store driver details for Step 1
  selectedDriver.driverDetailsHTML = `
    <div class="document-section">
      <h3>📄 Driver's License</h3>
      ${licensePhotoHTML}
    </div>

    <div class="document-section">
      <h3>👤 Personal Information ${verificationBadge}</h3>
      <div class="info-grid">
        <div class="info-item">
          <div class="info-label">Full Name</div>
          <div class="info-value">${fullName}</div>
        </div>
        <div class="info-item">
          <div class="info-label">Phone Number</div>
          <div class="info-value">${selectedDriver.phoneNumber || 'N/A'}</div>
        </div>
        <div class="info-item">
          <div class="info-label">Address</div>
          <div class="info-value">${selectedDriver.address || 'N/A'}</div>
        </div>
        <div class="info-item">
          <div class="info-label">License Number</div>
          <div class="info-value">${selectedDriver.licenseNumber || 'N/A'}</div>
        </div>
        <div class="info-item">
          <div class="info-label">TODA Number</div>
          <div class="info-value">${selectedDriver.todaNumber || '⚠️ Not assigned'}</div>
        </div>
        <div class="info-item">
          <div class="info-label">RFID Number</div>
          <div class="info-value">${selectedDriver.rfidNumber || selectedDriver.rfidUID || '⚠️ Not assigned'}</div>
        </div>
      </div>
    </div>
  `;

  // Show modal first
  document.getElementById('driverModal').classList.add('active');
  
  // Then show appropriate step UI (using requestAnimationFrame to ensure DOM is ready)
  requestAnimationFrame(() => {
    requestAnimationFrame(() => {
      showStepUI();
    });
  });
}

// Show step UI based on current step
function showStepUI() {
  const progressSteps = document.getElementById('progressSteps');
  const verificationStep = document.getElementById('verificationStep');
  const todaStep = document.getElementById('todaStep');
  const rfidStep = document.getElementById('rfidStep');
  
  const rejectBtn = document.getElementById('rejectBtn');
  const approveBtn = document.getElementById('approveBtn');
  const assignTodaBtn = document.getElementById('assignTodaBtn');
  const assignRfidBtn = document.getElementById('assignRfidBtn');
  const viewOnlyBtn = document.getElementById('viewOnlyBtn');

  // Check if elements exist
  if (!progressSteps || !verificationStep || !todaStep || !rfidStep || 
      !rejectBtn || !approveBtn || !assignTodaBtn || !assignRfidBtn || !viewOnlyBtn) {
    showMessage('Required modal elements not found. Please refresh the page.', 'error');
    return;
  }

  // Hide all steps and buttons
  progressSteps.style.display = 'none';
  verificationStep.style.display = 'none';
  todaStep.style.display = 'none';
  rfidStep.style.display = 'none';
  rejectBtn.style.display = 'none';
  approveBtn.style.display = 'none';
  assignTodaBtn.style.display = 'none';
  assignRfidBtn.style.display = 'none';
  viewOnlyBtn.style.display = 'none';

  // Get modal body to inject step-specific content
  const modalBody = document.getElementById('modalBody');
  
  // Remove any previously injected driver details (clean slate for each step)
  const existingDetailsContainer = document.getElementById('stepDriverDetails');
  if (existingDetailsContainer) {
    existingDetailsContainer.remove();
  }
  
  // Also clean up from inside verificationStep if it exists there
  const verificationStepCheck = document.getElementById('verificationStep');
  if (verificationStepCheck) {
    const detailsInStep = verificationStepCheck.querySelector('#stepDriverDetails');
    if (detailsInStep) {
      detailsInStep.remove();
    }
  }

  // Update step indicators
  const step1 = document.getElementById('step1');
  const step2 = document.getElementById('step2');
  const step3 = document.getElementById('step3');
  
  if (step1) step1.classList.remove('active', 'completed');
  if (step2) step2.classList.remove('active', 'completed');
  if (step3) step3.classList.remove('active', 'completed');

  if (currentStep === 0) {
    // View only mode (rejected or completed)
    document.getElementById('modalTitle').textContent = 'Driver Details';
    
    // Show driver details for view-only
    if (selectedDriver && selectedDriver.driverDetailsHTML) {
      const detailsContainer = document.createElement('div');
      detailsContainer.id = 'stepDriverDetails';
      detailsContainer.innerHTML = selectedDriver.driverDetailsHTML;
      modalBody.insertBefore(detailsContainer, modalBody.firstChild);
    }
    
    viewOnlyBtn.style.display = 'inline-block';
  } else {
    // Show progress steps
    progressSteps.style.display = 'flex';
    document.getElementById('modalTitle').textContent = 'Driver Onboarding Process';
    
    // Update step indicators
    if (currentStep >= 1) {
      if (currentStep === 1) {
        if (step1) step1.classList.add('active');
      } else {
        if (step1) step1.classList.add('completed');
      }
    }
    if (currentStep >= 2) {
      if (currentStep === 2) {
        if (step2) step2.classList.add('active');
      } else if (currentStep > 2) {
        if (step2) step2.classList.add('completed');
      }
    }
    if (currentStep >= 3) {
      if (step3) step3.classList.add('active');
    }

    // Show appropriate step content
    if (currentStep === 1) {
      // Step 1: Show driver details INSIDE verification step container
      verificationStep.style.display = 'block';
      
      if (selectedDriver && selectedDriver.driverDetailsHTML) {
        // Insert driver details after the header, before the form
        const detailsContainer = document.createElement('div');
        detailsContainer.id = 'stepDriverDetails';
        detailsContainer.innerHTML = selectedDriver.driverDetailsHTML;
        
        // Insert after header, before form content
        const formContent = document.getElementById('verificationFormContent');
        if (formContent) {
          verificationStep.insertBefore(detailsContainer, formContent);
        }
      }
      
      rejectBtn.style.display = 'inline-block';
      approveBtn.style.display = 'inline-block';
      
      // Reset all checkboxes
      const allCheckboxes = document.querySelectorAll('input[name="verificationStatus"]');
      allCheckboxes.forEach(cb => {
        cb.checked = false;
      });
      
      // Hide custom notes section
      const customNotesSection = document.getElementById('customNotesSection');
      if (customNotesSection) {
        customNotesSection.style.display = 'none';
      }
      
      // Clear custom notes
      const customNotes = document.getElementById('customNotes');
      if (customNotes) {
        customNotes.value = '';
      }
      
    } else if (currentStep === 2) {
      // Step 2: Show ONLY TODA assignment form
      todaStep.style.display = 'block';
      assignTodaBtn.style.display = 'inline-block';
      const todaNumberInput = document.getElementById('todaNumberInput');
      if (todaNumberInput) {
        todaNumberInput.value = '';
        setTimeout(() => todaNumberInput.focus(), 100);
      }
      
    } else if (currentStep === 3) {
      // Step 3: Show ONLY RFID scanner
      rfidStep.style.display = 'block';
      assignRfidBtn.style.display = 'inline-block';
      const rfidNumberInput = document.getElementById('rfidNumberInput');
      if (rfidNumberInput) {
        rfidNumberInput.value = '';
        setTimeout(() => rfidNumberInput.focus(), 100);
      }
    }
  }
}

// Close driver modal
window.closeDriverModal = function() {
  document.getElementById('driverModal').classList.remove('active');
  selectedDriver = null;
  currentStep = 1;
}

// Handle approval (Step 1)
window.handleApprove = async function() {
  if (!selectedDriver) return;

  // Get all checked checkboxes
  const checkedBoxes = document.querySelectorAll('input[name="verificationStatus"]:checked');
  const selectedValues = Array.from(checkedBoxes).map(cb => cb.value);

  if (selectedValues.length === 0) {
    showMessage('Please select at least one option (Approve or specify rejection reasons)', 'warning');
    return;
  }

  // Check if "Approve" is selected
  const isApproved = selectedValues.includes('approve');
  
  // If approve is selected along with rejections, warn the user
  if (isApproved && selectedValues.length > 1) {
    const confirmResult = await showConfirm(
      'You selected "Approve" along with rejection reasons. Approve will take priority and rejection reasons will be ignored. Continue?',
      '⚠️ Mixed Selection Warning',
      'Continue with Approval'
    );
    if (!confirmResult) {
      return;
    }
  }

  // If only rejections are selected (no approve), call handleReject instead
  if (!isApproved) {
    handleReject();
    return;
  }

  try {
    const fullName = selectedDriver.driverName || `${selectedDriver.firstName || ''} ${selectedDriver.middleName || ''} ${selectedDriver.lastName || ''}`.trim() || 'Unknown';
    
    // Update driver's verification status
    const driverRef = ref(db, `drivers/${selectedDriver.id}`);
    await update(driverRef, {
      verificationStatus: 'verified',
      verifiedAt: new Date().toISOString(),
      verifiedBy: 'Admin',
      verificationNotes: 'Documents approved - All documents are clear and verified'
    });

    // Update local driver object
    selectedDriver.verificationStatus = 'verified';

    // Log audit action
    await logAuditAction('Driver Management', 'Verify Documents', fullName, selectedDriver.id, {
      verificationStatus: 'verified',
      notes: 'Documents approved'
    });

    showMessage('Documents verified successfully! Proceeding to TODA assignment...', 'success');
    
    // Move to step 2
    currentStep = 2;
    await showDriverDetails(selectedDriver.id);
  } catch (error) {
    showMessage('Failed to verify driver: ' + error.message, 'error');
  }
}

// Handle rejection (Step 1)
window.handleReject = async function() {
  if (!selectedDriver) return;

  // Get all checked checkboxes
  const checkedBoxes = document.querySelectorAll('input[name="verificationStatus"]:checked');
  const selectedValues = Array.from(checkedBoxes).map(cb => cb.value);

  // Remove 'approve' from the list if somehow included
  const rejectionReasons = selectedValues.filter(v => v !== 'approve');

  if (rejectionReasons.length === 0) {
    showMessage('Please select at least one reason for rejection', 'warning');
    return;
  }

  // Build rejection messages
  const messages = [];
  const reasonTypes = [];

  rejectionReasons.forEach(reason => {
    switch (reason) {
      case 'reupload_photo':
        messages.push('📸 Please reupload your license photo. The current photo is blurry, unclear, or incomplete.');
        reasonTypes.push('Reupload Photo');
        break;
      case 'invalid_license':
        messages.push('❌ Your driver\'s license is expired or not valid. Please renew your license and resubmit.');
        reasonTypes.push('Invalid License');
        break;
      case 'incomplete_info':
        messages.push('📝 Your registration is missing required personal information. Please complete all required fields.');
        reasonTypes.push('Incomplete Information');
        break;
      case 'document_mismatch':
        messages.push('⚠️ The information provided does not match your license details. Please review and correct your information.');
        reasonTypes.push('Document Mismatch');
        break;
      case 'other':
        const customNotes = document.getElementById('customNotes').value.trim();
        if (!customNotes) {
          showMessage('Please enter additional notes for "Other Reason"', 'warning');
          document.getElementById('customNotes').focus();
          return;
        }
        messages.push('📋 ' + customNotes);
        reasonTypes.push('Other');
        break;
    }
  });

  // Combine all messages
  const combinedMessage = messages.join('\n\n');
  const combinedTypes = reasonTypes.join(', ');

  const confirmResult = await showConfirm(
    `Are you sure you want to reject this driver's application?\n\nIssues found (${reasonTypes.length}):\n${reasonTypes.map((r, i) => `${i + 1}. ${r}`).join('\n')}\n\nThe driver will receive:\n\n"${combinedMessage}"`,
    '⚠️ Reject Driver Application',
    'Confirm Rejection'
  );
  
  if (!confirmResult) {
    return;
  }

  try {
    const fullName = selectedDriver.driverName || `${selectedDriver.firstName || ''} ${selectedDriver.middleName || ''} ${selectedDriver.lastName || ''}`.trim() || 'Unknown';
    
    // Update driver's verification status
    const driverRef = ref(db, `drivers/${selectedDriver.id}`);
    await update(driverRef, {
      verificationStatus: 'rejected',
      rejectedAt: new Date().toISOString(),
      rejectedBy: 'Admin',
      rejectionReason: combinedMessage,
      rejectionTypes: rejectionReasons,
      rejectionCount: rejectionReasons.length
    });

    // Log audit action
    await logAuditAction('Driver Management', 'Reject Documents', fullName, selectedDriver.id, {
      verificationStatus: 'rejected',
      reason: combinedMessage,
      rejectionTypes: combinedTypes,
      issuesCount: rejectionReasons.length
    });

    showMessage(`Driver application rejected with ${rejectionReasons.length} issue(s). The driver has been notified.`, 'success');
    closeDriverModal();
  } catch (error) {
    showMessage('Failed to reject driver: ' + error.message, 'error');
  }
}

// Handle TODA assignment (Step 2)
window.handleTodaAssignment = async function() {
  if (!selectedDriver) return;

  let todaNumber = document.getElementById('todaNumberInput').value.trim().toUpperCase();

  if (!todaNumber) {
    showMessage('Please enter a TODA number', 'warning');
    return;
  }

  // Auto-format to TODA-XXX if user just enters numbers
  if (/^\d+$/.test(todaNumber)) {
    todaNumber = `TODA-${todaNumber.padStart(3, '0')}`;
  } else if (!/^TODA-\d+$/.test(todaNumber)) {
    showMessage('Please enter a valid TODA number (e.g., TODA-001 or just 001)', 'warning');
    return;
  }

  // Check if TODA number is already assigned
  const existingDriver = allDrivers.find(d => d.todaNumber === todaNumber && d.id !== selectedDriver.id);
  if (existingDriver) {
    const existingName = existingDriver.driverName || `${existingDriver.firstName || ''} ${existingDriver.lastName || ''}`.trim() || 'another driver';
    showMessage(`TODA Number ${todaNumber} is already assigned to ${existingName}`, 'error');
    return;
  }

  try {
    const fullName = selectedDriver.driverName || `${selectedDriver.firstName || ''} ${selectedDriver.middleName || ''} ${selectedDriver.lastName || ''}`.trim() || 'Unknown';
    
    // Update driver's TODA number in Firebase
    const driverRef = ref(db, `drivers/${selectedDriver.id}`);
    await update(driverRef, {
      todaNumber: todaNumber,
      todaAssignedAt: new Date().toISOString(),
      todaAssignedBy: 'Admin'
    });

    // Update local driver object
    selectedDriver.todaNumber = todaNumber;

    // Log audit action
    await logAuditAction('Driver Management', 'Assign TODA Number', fullName, selectedDriver.id, {
      todaNumber: todaNumber
    });

    showMessage(`TODA Number ${todaNumber} assigned successfully! Proceeding to RFID assignment...`, 'success');
    
    // Move to step 3
    currentStep = 3;
    await showDriverDetails(selectedDriver.id);
  } catch (error) {
    showMessage('Failed to assign TODA number: ' + error.message, 'error');
  }
}

// Handle RFID assignment (Step 3)
window.handleRfidAssignment = async function() {
  if (!selectedDriver) return;

  const rfidNumber = document.getElementById('rfidNumberInput').value.trim().toUpperCase();

  if (!rfidNumber) {
    showMessage('Please scan an RFID card', 'warning');
    return;
  }

  // Check if RFID is already assigned
  const existingDriver = allDrivers.find(d => {
    const existingRfid = d.rfidNumber || d.rfidUID;
    return existingRfid === rfidNumber && d.id !== selectedDriver.id;
  });
  if (existingDriver) {
    const existingName = existingDriver.driverName || `${existingDriver.firstName || ''} ${existingDriver.lastName || ''}`.trim() || 'another driver';
    showMessage(`RFID ${rfidNumber} is already assigned to ${existingName}`, 'error');
    return;
  }

  try {
    const fullName = selectedDriver.driverName || `${selectedDriver.firstName || ''} ${selectedDriver.middleName || ''} ${selectedDriver.lastName || ''}`.trim() || 'Unknown';
    
    // Update driver's RFID in Firebase
    const driverRef = ref(db, `drivers/${selectedDriver.id}`);
    await update(driverRef, {
      rfidNumber: rfidNumber,
      rfidUID: rfidNumber,
      hasRfidAssigned: true,
      needsRfidAssignment: false,
      isActive: true,
      rfidAssignedAt: new Date().toISOString(),
      rfidAssignedBy: 'Admin'
    });

    // Log audit action
    await logAuditAction('Driver Management', 'Assign RFID', fullName, selectedDriver.id, {
      rfidNumber: rfidNumber
    });

    showMessage(`🎉 Driver registration completed successfully!\n\nDriver: ${fullName}\nTODA: ${selectedDriver.todaNumber}\nRFID: ${rfidNumber}\n\nThe driver is now active in the system.`, 'success');
    closeDriverModal();
  } catch (error) {
    showMessage('Failed to assign RFID: ' + error.message, 'error');
  }
}

// Old verify function (kept for compatibility)
window.verifyDriver = async function() {
  if (!selectedDriver) return;

  const confirmResult = await showConfirm(
    'Are you sure you want to verify this driver\'s documents?',
    '✓ Verify Driver Documents',
    'Confirm Verification'
  );
  
  if (!confirmResult) {
    return;
  }

  try {
    const fullName = selectedDriver.driverName || `${selectedDriver.firstName || ''} ${selectedDriver.middleName || ''} ${selectedDriver.lastName || ''}`.trim() || 'Unknown';
    
    // Update driver's verification status
    const driverRef = ref(db, `drivers/${selectedDriver.id}`);
    await update(driverRef, {
      verificationStatus: 'verified',
      verifiedAt: new Date().toISOString(),
      verifiedBy: 'Admin' // You can replace with actual admin name
    });

    // Log audit action
    await logAuditAction('Driver Management', 'Verify Documents', fullName, selectedDriver.id, {
      verificationStatus: 'verified'
    });

    showMessage('Driver documents verified successfully!', 'success');
    closeDriverModal();
  } catch (error) {
    showMessage('Failed to verify driver: ' + error.message, 'error');
  }
}

// Reject driver documents
window.rejectDriver = async function() {
  if (!selectedDriver) return;

  const reason = await showPrompt('Enter reason for rejection (optional):', '⚠️ Reject Driver Documents', '');
  if (reason === null) return; // User cancelled

  try {
    const fullName = selectedDriver.driverName || `${selectedDriver.firstName || ''} ${selectedDriver.middleName || ''} ${selectedDriver.lastName || ''}`.trim() || 'Unknown';
    
    // Update driver's verification status
    const driverRef = ref(db, `drivers/${selectedDriver.id}`);
    await update(driverRef, {
      verificationStatus: 'rejected',
      rejectedAt: new Date().toISOString(),
      rejectedBy: 'Admin', // You can replace with actual admin name
      rejectionReason: reason || 'No reason provided'
    });

    // Log audit action
    await logAuditAction('Driver Management', 'Reject Documents', fullName, selectedDriver.id, {
      verificationStatus: 'rejected',
      reason: reason || 'No reason provided'
    });

    showMessage('Driver documents rejected.', 'success');
    closeDriverModal();
  } catch (error) {
    showMessage('Failed to reject driver: ' + error.message, 'error');
  }
}

// Show RFID assignment modal
window.showRfidAssignment = function() {
  if (!selectedDriver) return;

  const fullName = selectedDriver.driverName || `${selectedDriver.firstName || ''} ${selectedDriver.middleName || ''} ${selectedDriver.lastName || ''}`.trim() || 'Unknown';
  
  document.getElementById('rfidDriverName').textContent = fullName;
  document.getElementById('rfidInput').value = '';
  document.getElementById('rfidModal').classList.add('active');
  
  // Auto-focus the input for scanner
  setTimeout(() => {
    document.getElementById('rfidInput').focus();
  }, 100);
}

// Close RFID modal
window.closeRfidModal = function() {
  document.getElementById('rfidModal').classList.remove('active');
}

// Assign RFID to driver
window.assignRfid = async function() {
  const rfidNumber = document.getElementById('rfidInput').value.trim().toUpperCase();

  if (!rfidNumber) {
    showMessage('Please scan an RFID card', 'warning');
    return;
  }

  if (!selectedDriver) {
    showMessage('No driver selected', 'error');
    return;
  }

  // Check if RFID is already assigned
  const existingDriver = allDrivers.find(d => {
    const existingRfid = d.rfidNumber || d.rfidUID;
    return existingRfid === rfidNumber && d.id !== selectedDriver.id;
  });
  if (existingDriver) {
    const existingName = existingDriver.driverName || `${existingDriver.firstName || ''} ${existingDriver.lastName || ''}`.trim() || 'another driver';
    showMessage(`RFID ${rfidNumber} is already assigned to ${existingName}`, 'error');
    return;
  }

  try {
    const fullName = selectedDriver.driverName || `${selectedDriver.firstName || ''} ${selectedDriver.middleName || ''} ${selectedDriver.lastName || ''}`.trim() || 'Unknown';
    
    // Update driver's RFID in Firebase
    const driverRef = ref(db, `drivers/${selectedDriver.id}`);
    await update(driverRef, {
      rfidNumber: rfidNumber,
      rfidUID: rfidNumber,
      hasRfidAssigned: true,
      needsRfidAssignment: false,
      rfidAssignedAt: new Date().toISOString(),
      rfidAssignedBy: 'Admin' // You can replace with actual admin name
    });

    // Log audit action
    await logAuditAction('Driver Management', 'Assign RFID', fullName, selectedDriver.id, {
      rfidNumber: rfidNumber
    });

    showMessage('RFID assigned successfully!', 'success');
    closeRfidModal();
    closeDriverModal();
  } catch (error) {
    showMessage('Failed to assign RFID: ' + error.message, 'error');
  }
}

// Show TODA assignment modal
window.showTodaAssignment = function() {
  if (!selectedDriver) return;

  const fullName = selectedDriver.driverName || `${selectedDriver.firstName || ''} ${selectedDriver.middleName || ''} ${selectedDriver.lastName || ''}`.trim() || 'Unknown';
  
  document.getElementById('todaDriverName').textContent = fullName;
  document.getElementById('todaInput').value = '';
  document.getElementById('todaModal').classList.add('active');
  
  // Auto-focus the input
  setTimeout(() => {
    document.getElementById('todaInput').focus();
  }, 100);
}

// Close TODA modal
window.closeTodaModal = function() {
  document.getElementById('todaModal').classList.remove('active');
}

// Assign TODA number to driver
window.assignToda = async function() {
  const todaNumber = document.getElementById('todaInput').value.trim();

  if (!todaNumber) {
    showMessage('Please enter a TODA number', 'warning');
    return;
  }

  if (!selectedDriver) {
    showMessage('No driver selected', 'error');
    return;
  }

  // Check if TODA number is already assigned
  const existingDriver = allDrivers.find(d => d.todaNumber === todaNumber && d.id !== selectedDriver.id);
  if (existingDriver) {
    const existingName = existingDriver.driverName || `${existingDriver.firstName || ''} ${existingDriver.lastName || ''}`.trim() || 'another driver';
    showMessage(`TODA Number ${todaNumber} is already assigned to ${existingName}`, 'error');
    return;
  }

  try {
    const fullName = selectedDriver.driverName || `${selectedDriver.firstName || ''} ${selectedDriver.middleName || ''} ${selectedDriver.lastName || ''}`.trim() || 'Unknown';
    
    // Update driver's TODA number in Firebase
    const driverRef = ref(db, `drivers/${selectedDriver.id}`);
    await update(driverRef, {
      todaNumber: todaNumber,
      todaAssignedAt: new Date().toISOString(),
      todaAssignedBy: 'Admin' // You can replace with actual admin name
    });

    // Log audit action
    await logAuditAction('Driver Management', 'Assign TODA Number', fullName, selectedDriver.id, {
      todaNumber: todaNumber
    });

    showMessage('TODA Number assigned successfully!', 'success');
    closeTodaModal();
    closeDriverModal();
  } catch (error) {
    showMessage('Failed to assign TODA number: ' + error.message, 'error');
  }
}

// Log audit action
async function logAuditAction(module, action, targetName, targetId, changes, metadata = {}) {
  try {
    const auditRef = ref(db, 'auditLogs');
    const newLogRef = push(auditRef);
    
    await set(newLogRef, {
      timestamp: new Date().toISOString(),
      module: module,
      action: action,
      adminName: 'Admin', // You can replace with actual admin name
      ipAddress: 'Unknown', // You can add IP detection if needed
      targetName: targetName,
      targetId: targetId,
      description: `${action} for ${targetName}`,
      changes: changes,
      metadata: metadata
    });
  } catch (error) {
    // Silently fail - audit logging is not critical
  }
}

// Event listeners
document.getElementById('searchInput').addEventListener('input', applyFilters);
document.getElementById('statusFilter').addEventListener('change', applyFilters);

// Close modals when clicking outside
window.addEventListener('click', (event) => {
  if (event.target.classList.contains('modal')) {
    event.target.classList.remove('active');
  }
});

// RFID Reassignment Functions
let reassignDriverData = null;

window.showReassignRfidModal = function(driverId) {
  const driver = allDrivers.find(d => d.id === driverId);
  if (!driver) return;

  reassignDriverData = driver;
  const fullName = driver.driverName || `${driver.firstName || ''} ${driver.middleName || ''} ${driver.lastName || ''}`.trim() || 'Unknown';
  const currentRfid = driver.rfidNumber || driver.rfidUID || 'Not assigned';

  // Display driver info
  document.getElementById('reassignDriverInfo').innerHTML = `
    <div class="info-item">
      <div class="info-label">Full Name</div>
      <div class="info-value">${fullName}</div>
    </div>
    <div class="info-item">
      <div class="info-label">Phone Number</div>
      <div class="info-value">${driver.phoneNumber || 'N/A'}</div>
    </div>
    <div class="info-item">
      <div class="info-label">TODA Number</div>
      <div class="info-value">${driver.todaNumber || 'N/A'}</div>
    </div>
  `;

  // Display current RFID
  document.getElementById('currentRfidDisplay').textContent = currentRfid;

  // Display RFID history if exists
  const rfidHistory = driver.rfidHistory || [];
  const historySection = document.getElementById('rfidHistorySection');
  const historyList = document.getElementById('rfidHistoryList');
  
  if (rfidHistory.length > 0) {
    historySection.style.display = 'block';
    historyList.innerHTML = rfidHistory.map((entry, index) => {
      const replacedDate = new Date(entry.replacedAt);
      const formattedDate = replacedDate.toLocaleDateString('en-US', { 
        month: 'short', 
        day: 'numeric', 
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
      
      return `
        <div class="rfid-history-item">
          <div>
            <div class="rfid-history-number">${entry.rfid}</div>
            <div class="rfid-history-date">Replaced on ${formattedDate}</div>
          </div>
          <div class="rfid-history-badge">Old RFID #${index + 1}</div>
        </div>
      `;
    }).join('');
  } else {
    historySection.style.display = 'none';
  }

  // Clear input and status
  document.getElementById('reassignRfidInput').value = '';
  document.getElementById('reassignRfidStatus').textContent = '';
  document.getElementById('reassignRfidStatus').className = 'rfid-status';

  // Show modal
  document.getElementById('reassignRfidModal').classList.add('active');
  
  // Focus on input after a short delay
  setTimeout(() => {
    document.getElementById('reassignRfidInput').focus();
  }, 100);
}

window.closeReassignRfidModal = function() {
  document.getElementById('reassignRfidModal').classList.remove('active');
  reassignDriverData = null;
}

window.confirmReassignRfid = async function() {
  if (!reassignDriverData) return;

  const newRfidNumber = document.getElementById('reassignRfidInput').value.trim().toUpperCase();
  const statusDiv = document.getElementById('reassignRfidStatus');

  if (!newRfidNumber) {
    statusDiv.textContent = '⚠️ Please scan an RFID card';
    statusDiv.className = 'rfid-status error';
    return;
  }

  const currentRfid = reassignDriverData.rfidNumber || reassignDriverData.rfidUID;

  if (newRfidNumber === currentRfid) {
    statusDiv.textContent = '⚠️ This is the same RFID card';
    statusDiv.className = 'rfid-status error';
    return;
  }

  // Check if new RFID is already assigned to another driver
  const existingDriver = allDrivers.find(d => {
    const existingRfid = d.rfidNumber || d.rfidUID;
    return existingRfid === newRfidNumber && d.id !== reassignDriverData.id;
  });

  if (existingDriver) {
    const existingName = existingDriver.driverName || `${existingDriver.firstName || ''} ${existingDriver.lastName || ''}`.trim() || 'another driver';
    statusDiv.textContent = `❌ RFID ${newRfidNumber} is already assigned to ${existingName}`;
    statusDiv.className = 'rfid-status error';
    return;
  }

  const fullName = reassignDriverData.driverName || `${reassignDriverData.firstName || ''} ${reassignDriverData.middleName || ''} ${reassignDriverData.lastName || ''}`.trim() || 'Unknown';

  const confirmResult = await showConfirm(
    `⚠️ Reassign RFID Card?\n\nDriver: ${fullName}\n\nOld RFID: ${currentRfid}\nNew RFID: ${newRfidNumber}\n\nThis action will replace the driver's current RFID card.`,
    '⚠️ Reassign RFID Card',
    '✓ Confirm Reassignment'
  );
  
  if (!confirmResult) {
    return;
  }

  try {
    statusDiv.textContent = '⏳ Updating RFID...';
    statusDiv.className = 'rfid-status';

    const timestamp = new Date().toISOString();
    
    // Create RFID history entry for tracking old RFID
    const rfidHistoryRef = ref(db, `rfidHistory/${reassignDriverData.id}`);
    const newHistoryRef = push(rfidHistoryRef);
    
    await set(newHistoryRef, {
      driverId: reassignDriverData.id,
      driverName: fullName,
      oldRfid: currentRfid,
      newRfid: newRfidNumber,
      reassignedAt: timestamp,
      reassignedBy: 'Admin',
      reason: 'RFID card lost/replaced',
      // Store driver info for reference
      phoneNumber: reassignDriverData.phoneNumber || '',
      todaNumber: reassignDriverData.todaNumber || ''
    });

    // Also create a reverse lookup for old RFID to new RFID mapping
    // This allows tracking contributions/bookings from old RFID
    const rfidMappingRef = ref(db, `rfidMapping/${currentRfid}`);
    await set(rfidMappingRef, {
      oldRfid: currentRfid,
      currentRfid: newRfidNumber,
      driverId: reassignDriverData.id,
      driverName: fullName,
      mappedAt: timestamp,
      mappedBy: 'Admin'
    });

    // Update driver's RFID in Firebase
    const driverRef = ref(db, `drivers/${reassignDriverData.id}`);
    
    // Get existing RFID history array or create new one
    const existingHistory = reassignDriverData.rfidHistory || [];
    existingHistory.push({
      rfid: currentRfid,
      replacedAt: timestamp,
      replacedBy: 'Admin'
    });
    
    await update(driverRef, {
      rfidNumber: newRfidNumber,
      rfidUID: newRfidNumber,
      rfidReassignedAt: timestamp,
      rfidReassignedBy: 'Admin',
      previousRfid: currentRfid,
      rfidHistory: existingHistory, // Array of all previous RFIDs
      rfidReassignmentCount: existingHistory.length
    });

    // Log audit action
    await logAuditAction('Driver Management', 'Reassign RFID', fullName, reassignDriverData.id, {
      oldRfid: currentRfid,
      newRfid: newRfidNumber,
      reason: 'RFID card lost/replaced',
      historyCount: existingHistory.length
    });

    statusDiv.textContent = '✅ RFID reassigned successfully!';
    statusDiv.className = 'rfid-status success';

    showMessage(`✅ RFID Reassigned Successfully!\n\nDriver: ${fullName}\nOld RFID: ${currentRfid}\nNew RFID: ${newRfidNumber}\n\nHistory has been preserved for tracking contributions and bookings.`, 'success');

    // Reload drivers to show updated info
    await loadDrivers();

    // Close modal after short delay
    setTimeout(() => {
      closeReassignRfidModal();
    }, 1000);

  } catch (error) {
    statusDiv.textContent = '❌ Failed to reassign RFID: ' + error.message;
    statusDiv.className = 'rfid-status error';
  }
}

// Initialize
document.addEventListener('DOMContentLoaded', () => {
  loadDrivers();
  
  // Add event listener for checkboxes to show/hide custom notes
  document.addEventListener('change', (e) => {
    if (e.target.name === 'verificationStatus') {
      const customNotesSection = document.getElementById('customNotesSection');
      const otherCheckbox = document.getElementById('statusOther');
      
      if (customNotesSection && otherCheckbox) {
        if (otherCheckbox.checked) {
          customNotesSection.style.display = 'block';
          document.getElementById('customNotes').focus();
        } else {
          customNotesSection.style.display = 'none';
          document.getElementById('customNotes').value = '';
        }
      }
    }
  });
});

