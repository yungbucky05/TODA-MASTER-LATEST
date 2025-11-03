// Import Firebase SDKs
import { initializeApp } from "https://www.gstatic.com/firebasejs/11.0.0/firebase-app.js";
import { getDatabase, ref, onValue, push, set } from "https://www.gstatic.com/firebasejs/11.0.0/firebase-database.js";

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

let allContributions = [];
let filteredContributions = [];
let allDrivers = [];
let selectedDriver = null;

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

// Load all contributions and drivers
function loadData() {
  // Load contributions
  const contributionsRef = ref(db, 'contributions');
  onValue(contributionsRef, (snapshot) => {
    if (snapshot.exists()) {
      const contributions = snapshot.val();
      allContributions = [];

      Object.keys(contributions).forEach(contributionId => {
        allContributions.push({
          id: contributionId,
          ...contributions[contributionId]
        });
      });

      // Sort by timestamp (newest first)
      allContributions.sort((a, b) => {
        const timestampA = parseInt(a.timestamp) || 0;
        const timestampB = parseInt(b.timestamp) || 0;
        return timestampB - timestampA;
      });

      updateSummary();
      applyFilters();
    } else {
      document.getElementById('contributionsList').innerHTML = '<div class="empty-state"><div class="empty-state-icon">💰</div><p>No contributions found</p></div>';
    }
  }, (error) => {
    showMessage('Error loading contributions: ' + error.message, 'error');
  });

  // Load drivers for search suggestions
  const driversRef = ref(db, 'drivers');
  onValue(driversRef, (snapshot) => {
    if (snapshot.exists()) {
      const drivers = snapshot.val();
      allDrivers = [];

      Object.keys(drivers).forEach(driverId => {
        const driver = drivers[driverId];
        // Use driver data from drivers collection
        const driverName = driver.driverName || driver.name || `${driver.firstName || ''} ${driver.lastName || ''}`.trim() || 'Unknown';
        const rfid = driver.rfidNumber || driver.rfidUID || driverId; // Backward compatibility
        
        allDrivers.push({
          id: driverId,
          driverName: driverName,
          driverId: rfid,
          todaNumber: driver.todaNumber || 'N/A',
          phoneNumber: driver.phoneNumber || 'N/A'
        });
      });

      // Populate driver dropdown
      populateDriverDropdown();
    }
  });
}

// Populate driver dropdown
function populateDriverDropdown() {
  const driverFilter = document.getElementById('driverFilter');
  const currentValue = driverFilter.value;
  
  driverFilter.innerHTML = '<option value="">All Drivers</option>';
  
  // Sort drivers alphabetically
  const sortedDrivers = [...allDrivers].sort((a, b) => 
    a.driverName.localeCompare(b.driverName)
  );
  
  sortedDrivers.forEach(driver => {
    const option = document.createElement('option');
    option.value = driver.driverId;
    option.textContent = `${driver.driverName} (${driver.driverId})`;
    driverFilter.appendChild(option);
  });
  
  // Restore previous selection
  if (currentValue) {
    driverFilter.value = currentValue;
  }
}

// Update summary totals
function updateSummary() {
  const now = new Date();
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
  const weekStart = new Date(now.getFullYear(), now.getMonth(), now.getDate() - now.getDay()).getTime();
  const monthStart = new Date(now.getFullYear(), now.getMonth(), 1).getTime();

  let todayTotal = 0;
  let weekTotal = 0;
  let monthTotal = 0;

  const contributionsToUse = selectedDriver ? filteredContributions : allContributions;

  contributionsToUse.forEach(contribution => {
    const amount = parseFloat(contribution.amount) || 0;
    const timestamp = parseInt(contribution.timestamp) * 1000 || 0; // Convert seconds to milliseconds

    if (timestamp >= todayStart) todayTotal += amount;
    if (timestamp >= weekStart) weekTotal += amount;
    if (timestamp >= monthStart) monthTotal += amount;
  });

  document.getElementById('todayTotal').textContent = formatAmount(todayTotal);
  document.getElementById('weekTotal').textContent = formatAmount(weekTotal);
  document.getElementById('monthTotal').textContent = formatAmount(monthTotal);
}

// Format amount
function formatAmount(amount) {
  return '₱' + amount.toFixed(2);
}

// Apply filters
function applyFilters() {
  const searchQuery = document.getElementById('searchInput').value.toLowerCase();
  const dateFilter = document.getElementById('dateFilter').value;
  const paymentTypeFilter = document.getElementById('paymentTypeFilter').value;

  filteredContributions = allContributions.filter(contribution => {
    // Search filter
    const matchesSearch = !searchQuery ||
      contribution.driverName?.toLowerCase().includes(searchQuery) ||
      contribution.driverId?.toLowerCase().includes(searchQuery);

    // Date filter
    let matchesDate = true;
    if (dateFilter) {
      const contributionTimestamp = parseInt(contribution.timestamp) * 1000 || 0;
      const contributionDate = new Date(contributionTimestamp);
      const filterDate = new Date(dateFilter);
      matchesDate = contributionDate.toDateString() === filterDate.toDateString();
    }

    // Payment type filter
    let matchesPaymentType = true;
    if (paymentTypeFilter !== 'all') {
      if (paymentTypeFilter === 'pay_later') {
        // paid = false means pay_later (unpaid)
        matchesPaymentType = contribution.paid === false;
      } else if (paymentTypeFilter === 'paid') {
        // no paid field means pay_every_trip (already paid/settled)
        matchesPaymentType = contribution.paid === undefined || contribution.paid === null;
      }
    }

    return matchesSearch && matchesDate && matchesPaymentType;
  });

  // If a driver is selected, update their stats
  if (selectedDriver) {
    updateDriverStats();
  } else {
    updateSummary();
  }

  updateActiveFilters();
  displayContributions();
}

// Update driver-specific stats
function updateDriverStats() {
  const driverContributions = filteredContributions.filter(c =>
    c.driverName === selectedDriver || c.driverId === selectedDriver
  );

  const now = new Date();
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
  const weekStart = new Date(now.getFullYear(), now.getMonth(), now.getDate() - now.getDay()).getTime();
  const monthStart = new Date(now.getFullYear(), now.getMonth(), 1).getTime();

  let todayTotal = 0;
  let weekTotal = 0;
  let monthTotal = 0;
  let totalContributions = 0;
  let totalAmount = 0;

  driverContributions.forEach(contribution => {
    const amount = parseFloat(contribution.amount) || 0;
    const timestamp = parseInt(contribution.timestamp) * 1000 || 0; // Convert seconds to milliseconds

    totalAmount += amount;
    totalContributions++;

    if (timestamp >= todayStart) todayTotal += amount;
    if (timestamp >= weekStart) weekTotal += amount;
    if (timestamp >= monthStart) monthTotal += amount;
  });

  const driverInfo = allDrivers.find(d => d.driverName === selectedDriver || d.driverId === selectedDriver);

  document.getElementById('driverStatsCard').innerHTML = `
    <div class="driver-stats-header">
      <div class="driver-info">
        <h3>${driverInfo?.driverName || selectedDriver}</h3>
        <p>RFID: ${driverInfo?.driverId || 'N/A'}</p>
        <p>${totalContributions} contributions • Total: ${formatAmount(totalAmount)}</p>
      </div>
      <button class="download-btn" onclick="downloadDriverReport()">
        📥 Download Report
      </button>
    </div>
    <div class="driver-stats-grid">
      <div class="stat-item">
        <div class="stat-label">Today</div>
        <div class="stat-value">${formatAmount(todayTotal)}</div>
      </div>
      <div class="stat-item">
        <div class="stat-label">This Week</div>
        <div class="stat-value">${formatAmount(weekTotal)}</div>
      </div>
      <div class="stat-item">
        <div class="stat-label">This Month</div>
        <div class="stat-value">${formatAmount(monthTotal)}</div>
      </div>
    </div>
  `;

  document.getElementById('driverStatsCard').style.display = 'block';
}

// Download driver report
window.downloadDriverReport = function() {
  const driverContributions = filteredContributions.filter(c =>
    c.driverName === selectedDriver || c.driverId === selectedDriver
  );

  let csv = 'Date,Driver Name,RFID,Amount,Booking ID\n';
  driverContributions.forEach(contribution => {
    const date = new Date(contribution.timestamp || 0).toLocaleString();
    csv += `"${date}","${contribution.driverName || 'N/A'}","${contribution.driverId || 'N/A'}","${contribution.amount || 0}","${contribution.bookingId || 'N/A'}"\n`;
  });

  const blob = new Blob([csv], { type: 'text/csv' });
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `${selectedDriver}_contributions_${new Date().toISOString().split('T')[0]}.csv`;
  a.click();
  window.URL.revokeObjectURL(url);
}

// Update active filters display
function updateActiveFilters() {
  const container = document.getElementById('activeFilters');
  const filters = [];

  const dateFilter = document.getElementById('dateFilter').value;

  if (selectedDriver) {
    filters.push(`<div class="filter-chip">Driver: ${selectedDriver} <button onclick="clearDriverFilter()">×</button></div>`);
  }

  if (dateFilter) {
    const date = new Date(dateFilter);
    filters.push(`<div class="filter-chip">Date: ${date.toLocaleDateString()} <button onclick="document.getElementById('dateFilter').value=''; applyFilters();">×</button></div>`);
  }

  container.innerHTML = filters.join('');
}

// Clear driver filter
window.clearDriverFilter = function() {
  selectedDriver = null;
  document.getElementById('searchInput').value = '';
  document.getElementById('driverStatsCard').style.display = 'none';
  applyFilters();
}

// Clear all filters
window.clearFilters = function() {
  selectedDriver = null;
  document.getElementById('searchInput').value = '';
  document.getElementById('dateFilter').value = '';
  document.getElementById('paymentTypeFilter').value = 'all';
  document.getElementById('driverStatsCard').style.display = 'none';
  applyFilters();
}

// Display contributions
function displayContributions() {
  const contributionsList = document.getElementById('contributionsList');

  if (filteredContributions.length === 0) {
    contributionsList.innerHTML = '<div class="empty-state"><div class="empty-state-icon">💰</div><p>No contributions found matching your criteria</p></div>';
    document.getElementById('paginationControls').style.display = 'none';
    return;
  }

  // Calculate pagination
  totalPages = Math.ceil(filteredContributions.length / itemsPerPage);
  if (currentPage > totalPages) currentPage = totalPages;
  if (currentPage < 1) currentPage = 1;

  const startIndex = (currentPage - 1) * itemsPerPage;
  const endIndex = startIndex + itemsPerPage;
  const paginatedContributions = filteredContributions.slice(startIndex, endIndex);

  contributionsList.innerHTML = paginatedContributions.map(contribution => {
    const timestamp = parseInt(contribution.timestamp) * 1000 || 0;
    const date = new Date(timestamp);

    // Determine payment type based on 'paid' field
    let paymentTypeBadge = '';
    if (contribution.paid === false) {
      // paid = false means pay_later (unpaid)
      paymentTypeBadge = '<span class="payment-type-badge pay-later">💳 Pay Later (Unpaid)</span>';
    } else if (contribution.paid === undefined || contribution.paid === null) {
      // no paid field means pay_every_trip (already paid/settled)
      paymentTypeBadge = '<span class="payment-type-badge paid">✅ Paid</span>';
    }

    return `
      <div class="contribution-card">
        <div class="contribution-header">
          <div>
            <div class="driver-name">${contribution.driverName || 'Unknown Driver'}</div>
            <div class="info-value" style="color: #666; font-size: 14px;">${date.toLocaleString()}</div>
          </div>
          <div style="display: flex; align-items: center; gap: 12px;">
            ${paymentTypeBadge}
            <div class="contribution-amount">${formatAmount(parseFloat(contribution.amount) || 0)}</div>
          </div>
        </div>
        <div class="contribution-info">
          <div class="info-item">
            <span class="info-label">RFID:</span>
            <span class="info-value">${contribution.driverRFID || 'N/A'}</span>
          </div>
          <div class="info-item">
            <span class="info-label">TODA Number:</span>
            <span class="info-value">${contribution.todaNumber || 'N/A'}</span>
          </div>
          <div class="info-item">
            <span class="info-label">Date:</span>
            <span class="info-value">${contribution.date || 'N/A'}</span>
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
  
  if (filteredContributions.length === 0) {
    paginationControls.style.display = 'none';
    return;
  }
  
  paginationControls.style.display = 'flex';

  const startItem = (currentPage - 1) * itemsPerPage + 1;
  const endItem = Math.min(currentPage * itemsPerPage, filteredContributions.length);
  
  document.getElementById('paginationInfo').textContent = 
    `Showing ${startItem}-${endItem} of ${filteredContributions.length}`;

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
  displayContributions();
}

window.nextPage = function() {
  if (currentPage < totalPages) {
    currentPage++;
    displayContributions();
  }
}

window.previousPage = function() {
  if (currentPage > 1) {
    currentPage--;
    displayContributions();
  }
}

window.changeItemsPerPage = function() {
  itemsPerPage = parseInt(document.getElementById('itemsPerPage').value);
  currentPage = 1;
  displayContributions();
}

// Event listeners
document.getElementById('searchInput').addEventListener('input', applyFilters);
document.getElementById('dateFilter').addEventListener('change', applyFilters);
document.getElementById('paymentTypeFilter').addEventListener('change', applyFilters);
document.getElementById('driverFilter').addEventListener('change', function() {
  const selectedDriverId = this.value;
  if (selectedDriverId) {
    const driver = allDrivers.find(d => d.driverId === selectedDriverId);
    selectedDriver = driver ? driver.driverId : null;
  } else {
    selectedDriver = null;
  }
  applyFilters();
});

// Export contributions to CSV
window.exportContributions = function() {
  const driverFilter = document.getElementById('driverFilter').value;
  const driverInfo = driverFilter ? allDrivers.find(d => d.driverId === driverFilter) : null;
  
  // Get contributions to export
  let contributionsToExport = driverFilter 
    ? filteredContributions.filter(c => c.driverId === driverFilter || c.driverName === driverInfo?.driverName)
    : filteredContributions.length > 0 ? filteredContributions : allContributions;

  if (contributionsToExport.length === 0) {
    showMessage('No contributions to export', 'warning');
    return;
  }

  // Calculate summaries
  const now = new Date();
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
  const weekStart = new Date(now.getFullYear(), now.getMonth(), now.getDate() - now.getDay()).getTime();
  const monthStart = new Date(now.getFullYear(), now.getMonth(), 1).getTime();

  let todayTotal = 0;
  let weekTotal = 0;
  let monthTotal = 0;

  contributionsToExport.forEach(contribution => {
    const amount = parseFloat(contribution.amount) || 0;
    const timestamp = parseInt(contribution.timestamp) * 1000 || 0;

    if (timestamp >= todayStart) todayTotal += amount;
    if (timestamp >= weekStart) weekTotal += amount;
    if (timestamp >= monthStart) monthTotal += amount;
  });

  // Create CSV content
  let csvContent = '';
  
  // Header with driver info if selected
  if (driverInfo) {
    csvContent += `Contributions Report - ${driverInfo.driverName}\n`;
    csvContent += `RFID: ${driverInfo.driverId}\n`;
  } else {
    csvContent += `Contributions Report - All Drivers\n`;
  }
  
  csvContent += `Generated: ${new Date().toLocaleString()}\n\n`;
  
  // Summary section
  csvContent += `SUMMARY\n`;
  csvContent += `Today,₱${todayTotal.toFixed(2)}\n`;
  csvContent += `This Week,₱${weekTotal.toFixed(2)}\n`;
  csvContent += `This Month,₱${monthTotal.toFixed(2)}\n`;
  csvContent += `Total Contributions,${contributionsToExport.length}\n\n`;
  
  // Data headers
  csvContent += `Date,Time,Driver Name,RFID,Amount,Payment Method,Reference\n`;
  
  // Data rows
  contributionsToExport.forEach(contribution => {
    const date = new Date(parseInt(contribution.timestamp) * 1000);
    const dateStr = date.toLocaleDateString();
    const timeStr = date.toLocaleTimeString();
    const driverName = (contribution.driverName || 'Unknown').replace(/,/g, ';');
    const driverId = contribution.driverId || 'N/A';
    const amount = parseFloat(contribution.amount || 0).toFixed(2);
    const paymentMethod = contribution.paymentMethod || 'Cash';
    const reference = contribution.referenceNumber || 'N/A';
    
    csvContent += `${dateStr},${timeStr},${driverName},${driverId},₱${amount},${paymentMethod},${reference}\n`;
  });

  // Create download link
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  const link = document.createElement('a');
  const url = URL.createObjectURL(blob);
  
  link.setAttribute('href', url);
  const filename = driverInfo 
    ? `contributions_${driverInfo.driverName.replace(/\s+/g, '_')}_${new Date().toISOString().split('T')[0]}.csv`
    : `contributions_all_${new Date().toISOString().split('T')[0]}.csv`;
  link.setAttribute('download', filename);
  link.style.visibility = 'hidden';
  
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  
  // Log to audit trail
  logAuditAction('contributions_export', 'export', {
    targetName: driverInfo ? driverInfo.driverName : 'All Drivers',
    targetId: driverInfo ? driverInfo.driverId : 'all',
    description: `Exported ${contributionsToExport.length} contribution records. Today: ₱${todayTotal.toFixed(2)}, Week: ₱${weekTotal.toFixed(2)}, Month: ₱${monthTotal.toFixed(2)}`,
    metadata: {
      recordCount: contributionsToExport.length,
      todayTotal: todayTotal.toFixed(2),
      weekTotal: weekTotal.toFixed(2),
      monthTotal: monthTotal.toFixed(2)
    }
  });
}

// Helper function to log audit actions
function logAuditAction(module, action, data) {
  const logsRef = ref(db, 'auditLogs');
  const newLogRef = push(logsRef);
  
  const auditLog = {
    timestamp: Date.now(),
    module: module,
    action: action,
    adminName: 'Admin', // Should come from auth system
    ipAddress: 'N/A',
    ...data
  };
  
  set(newLogRef, auditLog).catch(error => {
    // Silently fail - audit logging is not critical
  });
}

// Initialize
document.addEventListener('DOMContentLoaded', loadData);

