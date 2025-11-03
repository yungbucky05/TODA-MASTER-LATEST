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

let allLogs = [];
let filteredLogs = [];

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

// Load all audit logs from Firebase
function loadAuditLogs() {
  const logsRef = ref(db, 'auditLogs');
  onValue(logsRef, (snapshot) => {
    if (snapshot.exists()) {
      const logs = snapshot.val();
      allLogs = [];

      Object.keys(logs).forEach(logId => {
        allLogs.push({
          id: logId,
          ...logs[logId]
        });
      });

      // Sort by timestamp (newest first)
      allLogs.sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0));

      updateSummary();
      applyFilters();
    } else {
      document.getElementById('auditLogsList').innerHTML = '<div class="empty-state"><div class="empty-state-icon">📋</div><p>No audit logs found</p></div>';
    }
  }, (error) => {
    showMessage('Error loading audit logs: ' + error.message, 'error');
    document.getElementById('auditLogsList').innerHTML = '<div class="empty-state"><div class="empty-state-icon">❌</div><p>Error loading logs</p></div>';
  });
}

// Update summary statistics
function updateSummary() {
  const now = new Date();
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
  const weekStart = new Date(now.getFullYear(), now.getMonth(), now.getDate() - now.getDay()).getTime();

  let todayCount = 0;
  let weekCount = 0;
  const uniqueAdmins = new Set();

  allLogs.forEach(log => {
    const timestamp = log.timestamp || 0;
    
    if (timestamp >= todayStart) todayCount++;
    if (timestamp >= weekStart) weekCount++;
    
    if (log.adminName) uniqueAdmins.add(log.adminName);
  });

  document.getElementById('todayLogs').textContent = todayCount;
  document.getElementById('weekLogs').textContent = weekCount;
  document.getElementById('totalLogs').textContent = allLogs.length;
  document.getElementById('activeAdmins').textContent = uniqueAdmins.size;
}

// Apply filters
function applyFilters() {
  const searchQuery = document.getElementById('searchInput').value.toLowerCase();
  const moduleFilter = document.getElementById('moduleFilter').value;
  const actionFilter = document.getElementById('actionFilter').value;
  const dateFilter = document.getElementById('dateFilter').value;

  filteredLogs = allLogs.filter(log => {
    // Search filter
    const matchesSearch = !searchQuery ||
      log.module?.toLowerCase().includes(searchQuery) ||
      log.action?.toLowerCase().includes(searchQuery) ||
      log.adminName?.toLowerCase().includes(searchQuery) ||
      log.description?.toLowerCase().includes(searchQuery) ||
      log.targetName?.toLowerCase().includes(searchQuery);

    // Module filter
    const matchesModule = moduleFilter === 'all' || log.module === moduleFilter;

    // Action filter
    const matchesAction = actionFilter === 'all' || log.action === actionFilter;

    // Date filter
    let matchesDate = true;
    if (dateFilter) {
      const logDate = new Date(log.timestamp || 0);
      const filterDate = new Date(dateFilter);
      matchesDate = logDate.toDateString() === filterDate.toDateString();
    }

    return matchesSearch && matchesModule && matchesAction && matchesDate;
  });

  updateActiveFilters();
  displayLogs();
}

// Update active filters display
function updateActiveFilters() {
  const container = document.getElementById('activeFilters');
  const filters = [];

  const moduleFilter = document.getElementById('moduleFilter').value;
  const actionFilter = document.getElementById('actionFilter').value;
  const dateFilter = document.getElementById('dateFilter').value;

  if (moduleFilter !== 'all') {
    const moduleName = document.querySelector(`#moduleFilter option[value="${moduleFilter}"]`).textContent;
    filters.push(`<div class="filter-chip">Module: ${moduleName} <button onclick="document.getElementById('moduleFilter').value='all'; applyFilters();">×</button></div>`);
  }

  if (actionFilter !== 'all') {
    filters.push(`<div class="filter-chip">Action: ${actionFilter} <button onclick="document.getElementById('actionFilter').value='all'; applyFilters();">×</button></div>`);
  }

  if (dateFilter) {
    const date = new Date(dateFilter);
    filters.push(`<div class="filter-chip">Date: ${date.toLocaleDateString()} <button onclick="document.getElementById('dateFilter').value=''; applyFilters();">×</button></div>`);
  }

  container.innerHTML = filters.join('');
  container.style.display = filters.length > 0 ? 'flex' : 'none';
}

// Display logs
function displayLogs() {
  const container = document.getElementById('auditLogsList');

  if (filteredLogs.length === 0) {
    container.innerHTML = '<div class="empty-state"><div class="empty-state-icon">🔍</div><p>No logs match your filters</p></div>';
    document.getElementById('paginationControls').style.display = 'none';
    return;
  }

  // Calculate pagination
  totalPages = Math.ceil(filteredLogs.length / itemsPerPage);
  if (currentPage > totalPages) currentPage = totalPages;
  if (currentPage < 1) currentPage = 1;

  const startIndex = (currentPage - 1) * itemsPerPage;
  const endIndex = startIndex + itemsPerPage;
  const paginatedLogs = filteredLogs.slice(startIndex, endIndex);

  container.innerHTML = paginatedLogs.map(log => {
    const date = new Date(log.timestamp || 0);
    const moduleName = getModuleName(log.module);
    
    return `
      <div class="log-card" onclick="showLogDetails('${log.id}')">
        <div class="log-header">
          <div class="log-title">
            <span class="action-badge ${log.action}">${log.action || 'unknown'}</span>
            <span class="log-module">${moduleName}</span>
          </div>
          <div class="log-timestamp">
            <div class="log-date">${date.toLocaleDateString()}</div>
            <div class="log-time">${date.toLocaleTimeString()}</div>
          </div>
        </div>
        
        <div class="log-details">
          ${log.targetName ? `
            <div class="log-detail-item">
              <div class="log-detail-label">Target</div>
              <div class="log-detail-value">${log.targetName}</div>
            </div>
          ` : ''}
          ${log.targetId ? `
            <div class="log-detail-item">
              <div class="log-detail-label">ID</div>
              <div class="log-detail-value">${log.targetId}</div>
            </div>
          ` : ''}
        </div>
        
        <div class="log-description">${log.description || 'No description available'}</div>
        
        <div class="log-footer">
          <div class="log-admin">
            <div class="admin-icon">👤</div>
            <span>${log.adminName || 'Unknown Admin'}</span>
          </div>
          ${log.ipAddress ? `<span style="font-size: 12px; color: #6c757d;">IP: ${log.ipAddress}</span>` : ''}
        </div>
      </div>
    `;
  }).join('');

  updatePaginationControls();
}

// Get module display name
function getModuleName(module) {
  const names = {
    'driver_management': 'Driver Management',
    'queue_management': 'Queue Management',
    'discount_applications': 'Discount Applications',
    'contributions_export': 'Contributions Export',
    'bookings_export': 'Bookings Export',
    'fare_matrix': 'Fare Matrix'
  };
  return names[module] || module;
}

// Show log details in modal
window.showLogDetails = function(logId) {
  const log = allLogs.find(l => l.id === logId);
  if (!log) return;

  const date = new Date(log.timestamp || 0);
  const moduleName = getModuleName(log.module);

  document.getElementById('modalBody').innerHTML = `
    <div class="detail-section">
      <h3>General Information</h3>
      <div class="detail-grid">
        <div class="detail-item">
          <div class="detail-label">Module</div>
          <div class="detail-value">${moduleName}</div>
        </div>
        <div class="detail-item">
          <div class="detail-label">Action</div>
          <div class="detail-value"><span class="action-badge ${log.action}">${log.action}</span></div>
        </div>
        <div class="detail-item">
          <div class="detail-label">Date & Time</div>
          <div class="detail-value">${date.toLocaleString()}</div>
        </div>
        <div class="detail-item">
          <div class="detail-label">Admin</div>
          <div class="detail-value">${log.adminName || 'Unknown'}</div>
        </div>
        ${log.ipAddress ? `
          <div class="detail-item">
            <div class="detail-label">IP Address</div>
            <div class="detail-value">${log.ipAddress}</div>
          </div>
        ` : ''}
      </div>
    </div>

    ${log.targetName || log.targetId ? `
      <div class="detail-section">
        <h3>Target Information</h3>
        <div class="detail-grid">
          ${log.targetName ? `
            <div class="detail-item">
              <div class="detail-label">Name</div>
              <div class="detail-value">${log.targetName}</div>
            </div>
          ` : ''}
          ${log.targetId ? `
            <div class="detail-item">
              <div class="detail-label">ID</div>
              <div class="detail-value">${log.targetId}</div>
            </div>
          ` : ''}
        </div>
      </div>
    ` : ''}

    ${log.description ? `
      <div class="detail-section">
        <h3>Description</h3>
        <p style="color: #495057; line-height: 1.6;">${log.description}</p>
      </div>
    ` : ''}

    ${log.changes ? `
      <div class="detail-section">
        <h3>Changes Made</h3>
        <div class="changes-grid">
          ${Object.keys(log.changes).map(field => `
            <div class="change-row">
              <div class="change-field">${field}</div>
              <div class="change-old">${log.changes[field].old || 'N/A'}</div>
              <div class="change-new">${log.changes[field].new || 'N/A'}</div>
            </div>
          `).join('')}
        </div>
      </div>
    ` : ''}

    ${log.metadata ? `
      <div class="detail-section">
        <h3>Additional Information</h3>
        <pre style="background: #f8f9fa; padding: 12px; border-radius: 8px; overflow-x: auto; font-size: 12px;">${JSON.stringify(log.metadata, null, 2)}</pre>
      </div>
    ` : ''}
  `;

  document.getElementById('logModal').classList.add('active');
}

// Close log modal
window.closeLogModal = function() {
  document.getElementById('logModal').classList.remove('active');
}

// Clear all filters
window.clearFilters = function() {
  document.getElementById('searchInput').value = '';
  document.getElementById('moduleFilter').value = 'all';
  document.getElementById('actionFilter').value = 'all';
  document.getElementById('dateFilter').value = '';
  applyFilters();
}

// Update pagination controls
function updatePaginationControls() {
  const paginationControls = document.getElementById('paginationControls');
  
  if (filteredLogs.length === 0) {
    paginationControls.style.display = 'none';
    return;
  }
  
  paginationControls.style.display = 'flex';

  const startItem = (currentPage - 1) * itemsPerPage + 1;
  const endItem = Math.min(currentPage * itemsPerPage, filteredLogs.length);
  
  document.getElementById('paginationInfo').textContent = 
    `Showing ${startItem}-${endItem} of ${filteredLogs.length}`;

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
  displayLogs();
}

window.nextPage = function() {
  if (currentPage < totalPages) {
    currentPage++;
    displayLogs();
  }
}

window.previousPage = function() {
  if (currentPage > 1) {
    currentPage--;
    displayLogs();
  }
}

window.changeItemsPerPage = function() {
  itemsPerPage = parseInt(document.getElementById('itemsPerPage').value);
  currentPage = 1;
  displayLogs();
}

// Export logs to CSV
window.exportLogs = function() {
  let logsToExport = filteredLogs.length > 0 ? filteredLogs : allLogs;

  if (logsToExport.length === 0) {
    showMessage('No logs to export', 'warning');
    return;
  }

  // Create CSV content
  let csvContent = 'Audit Logs Export\n';
  csvContent += `Generated: ${new Date().toLocaleString()}\n`;
  csvContent += `Total Logs: ${logsToExport.length}\n\n`;
  
  // Data headers
  csvContent += `Date,Time,Module,Action,Admin,Target,Description,IP Address\n`;
  
  // Data rows
  logsToExport.forEach(log => {
    const date = new Date(log.timestamp || 0);
    const dateStr = date.toLocaleDateString();
    const timeStr = date.toLocaleTimeString();
    const module = getModuleName(log.module);
    const action = log.action || 'N/A';
    const admin = (log.adminName || 'Unknown').replace(/,/g, ';');
    const target = (log.targetName || 'N/A').replace(/,/g, ';');
    const description = (log.description || '').replace(/,/g, ';').replace(/\n/g, ' ');
    const ip = log.ipAddress || 'N/A';
    
    csvContent += `${dateStr},${timeStr},${module},${action},${admin},${target},"${description}",${ip}\n`;
  });

  // Create download link
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  const link = document.createElement('a');
  const url = URL.createObjectURL(blob);
  
  link.setAttribute('href', url);
  link.setAttribute('download', `audit_logs_${new Date().toISOString().split('T')[0]}.csv`);
  link.style.visibility = 'hidden';
  
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
}

// Event listeners
document.getElementById('searchInput').addEventListener('input', applyFilters);
document.getElementById('moduleFilter').addEventListener('change', applyFilters);
document.getElementById('actionFilter').addEventListener('change', applyFilters);
document.getElementById('dateFilter').addEventListener('change', applyFilters);

// Close modal when clicking outside
window.addEventListener('click', (event) => {
  if (event.target.classList.contains('modal')) {
    event.target.classList.remove('active');
  }
});

// Initialize
document.addEventListener('DOMContentLoaded', loadAuditLogs);

// Export function to create audit log (to be used by other modules)
export function createAuditLog(logData) {
  const logsRef = ref(db, 'auditLogs');
  const newLogRef = push(logsRef);
  
  const auditLog = {
    timestamp: Date.now(),
    adminName: 'Admin', // This should come from auth system
    ipAddress: 'N/A', // Can be obtained from backend
    ...logData
  };
  
  return set(newLogRef, auditLog);
}
