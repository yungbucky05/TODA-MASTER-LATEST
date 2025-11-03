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

let allBookings = [];
let filteredBookings = [];
let allDrivers = [];

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

// Load all bookings from Firebase
function loadBookings() {
  const bookingsRef = ref(db, 'bookings');
  onValue(bookingsRef, (snapshot) => {
    if (snapshot.exists()) {
      const bookings = snapshot.val();
      allBookings = [];

      Object.keys(bookings).forEach(bookingId => {
        allBookings.push({
          id: bookingId,
          ...bookings[bookingId]
        });
      });

      // Sort by timestamp (newest first)
      allBookings.sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0));

      applyFilters();
    } else {
      document.getElementById('bookingsList').innerHTML = '<div class="empty-state"><div class="empty-state-icon">📋</div><p>No bookings found</p></div>';
    }
  }, (error) => {
    showMessage('Error loading bookings: ' + error.message, 'error');
    document.getElementById('bookingsList').innerHTML = '<div class="empty-state"><div class="empty-state-icon">❌</div><p>Error loading bookings</p></div>';
  });

  // Load drivers for filter dropdown
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
    option.value = driver.driverName;
    option.textContent = `${driver.driverName} (${driver.driverId})`;
    driverFilter.appendChild(option);
  });
  
  // Restore previous selection
  if (currentValue) {
    driverFilter.value = currentValue;
  }
}

// Apply filters
function applyFilters() {
  const searchQuery = document.getElementById('searchInput').value.toLowerCase();
  const statusFilter = document.getElementById('statusFilter').value;
  const tripTypeFilter = document.getElementById('tripTypeFilter').value;
  const driverFilter = document.getElementById('driverFilter').value;
  const dateFilter = document.getElementById('dateFilter').value;

  filteredBookings = allBookings.filter(booking => {
    // Search filter
    const matchesSearch = !searchQuery ||
      booking.id?.toLowerCase().includes(searchQuery) ||
      booking.customerName?.toLowerCase().includes(searchQuery) ||
      booking.driverName?.toLowerCase().includes(searchQuery) ||
      booking.pickupLocation?.toLowerCase().includes(searchQuery) ||
      booking.destination?.toLowerCase().includes(searchQuery);

    // Status filter
    const matchesStatus = statusFilter === 'all' || booking.status === statusFilter;
    
    // Booking app filter (replacing trip type filter)
    const bookingApp = booking.bookingApp || 'unknown';
    const matchesTripType = tripTypeFilter === 'all' || bookingApp === tripTypeFilter;

    // Driver filter
    const matchesDriver = !driverFilter || booking.driverName === driverFilter;

    // Date filter
    let matchesDate = true;
    if (dateFilter) {
      const bookingDate = new Date(booking.timestamp || 0);
      const filterDate = new Date(dateFilter);
      matchesDate = bookingDate.toDateString() === filterDate.toDateString();
    }

    return matchesSearch && matchesStatus && matchesTripType && matchesDriver && matchesDate;
  });

  updateActiveFilters();
  displayBookings();
}

// Update active filters display
function updateActiveFilters() {
  const container = document.getElementById('activeFilters');
  const filters = [];

  const statusFilter = document.getElementById('statusFilter').value;
  const tripTypeFilter = document.getElementById('tripTypeFilter').value;
  const dateFilter = document.getElementById('dateFilter').value;

  if (statusFilter !== 'all') {
    filters.push(`<div class="filter-chip">Status: ${statusFilter} <button onclick="document.getElementById('statusFilter').value='all'; applyFilters();">×</button></div>`);
  }
  
  if (tripTypeFilter !== 'all') {
    const sourceLabels = {
      'passengerApp': 'Online Booking',
      'barkerApp': 'On Site',
      'unknown': 'Unknown Source'
    };
    const sourceLabel = sourceLabels[tripTypeFilter] || tripTypeFilter;
    filters.push(`<div class="filter-chip">Booking Source: ${sourceLabel} <button onclick="document.getElementById('tripTypeFilter').value='all'; applyFilters();">×</button></div>`);
  }

  if (dateFilter) {
    const date = new Date(dateFilter);
    filters.push(`<div class="filter-chip">Date: ${date.toLocaleDateString()} <button onclick="document.getElementById('dateFilter').value=''; applyFilters();">×</button></div>`);
  }

  container.innerHTML = filters.join('');
}

// Clear all filters
window.clearFilters = function() {
  document.getElementById('searchInput').value = '';
  document.getElementById('statusFilter').value = 'all';
  document.getElementById('tripTypeFilter').value = 'all';
  document.getElementById('dateFilter').value = '';
  applyFilters();
}

// Display bookings
function displayBookings() {
  const bookingsList = document.getElementById('bookingsList');

  if (filteredBookings.length === 0) {
    bookingsList.innerHTML = '<div class="empty-state"><div class="empty-state-icon">🔍</div><p>No bookings found matching your criteria</p></div>';
    document.getElementById('paginationControls').style.display = 'none';
    return;
  }

  // Calculate pagination
  totalPages = Math.ceil(filteredBookings.length / itemsPerPage);
  if (currentPage > totalPages) currentPage = totalPages;
  if (currentPage < 1) currentPage = 1;

  const startIndex = (currentPage - 1) * itemsPerPage;
  const endIndex = startIndex + itemsPerPage;
  const paginatedBookings = filteredBookings.slice(startIndex, endIndex);

  bookingsList.innerHTML = paginatedBookings.map(booking => {
    const status = booking.status || 'active';
    const statusClass = `status-${status}`;
    const cardClass = status;
    const date = new Date(booking.timestamp || 0);
    const bookingApp = booking.bookingApp || 'unknown';
    
    // Create booking app badge based on the bookingApp field
    let bookingAppBadge = '';
    if (bookingApp === 'passengerApp') {
      bookingAppBadge = '<span class="trip-type-badge passenger-app">📱 Online Booking</span>';
    } else if (bookingApp === 'barkerApp') {
      bookingAppBadge = '<span class="trip-type-badge barker-app">🚗 On Site</span>';
    } else {
      bookingAppBadge = '<span class="trip-type-badge unknown">❓ Unknown Source</span>';
    }

    return `
      <div class="booking-card ${cardClass}" onclick="showBookingDetails('${booking.id}')">
        <div class="booking-header">
          <div>
            <div class="booking-id">Booking #${booking.id.substring(0, 8)}</div>
            <div class="booking-date">${date.toLocaleString()}</div>
          </div>
          <div style="display: flex; gap: 8px; align-items: center;">
            ${bookingAppBadge}
            <span class="booking-status ${statusClass}">${status}</span>
          </div>
        </div>
        <div class="booking-info">
          <div class="info-item">
            <span class="info-label">Passenger:</span> ${booking.customerName || 'N/A'}
          </div>
          <div class="info-item">
            <span class="info-label">Driver:</span> ${booking.driverName || 'N/A'}
          </div>
          <div class="info-item">
            <span class="info-label">Fare:</span> ₱${booking.actualFare || booking.estimatedFare || '0.00'}
          </div>
          <div class="info-item">
            <span class="info-label">Distance:</span> ${booking.distance ? booking.distance.toFixed(2) : 'N/A'} km
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
  
  if (filteredBookings.length === 0) {
    paginationControls.style.display = 'none';
    return;
  }
  
  paginationControls.style.display = 'flex';

  const startItem = (currentPage - 1) * itemsPerPage + 1;
  const endItem = Math.min(currentPage * itemsPerPage, filteredBookings.length);
  
  document.getElementById('paginationInfo').textContent = 
    `Showing ${startItem}-${endItem} of ${filteredBookings.length}`;

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
  displayBookings();
}

window.nextPage = function() {
  if (currentPage < totalPages) {
    currentPage++;
    displayBookings();
  }
}

window.previousPage = function() {
  if (currentPage > 1) {
    currentPage--;
    displayBookings();
  }
}

window.changeItemsPerPage = function() {
  itemsPerPage = parseInt(document.getElementById('itemsPerPage').value);
  currentPage = 1;
  displayBookings();
}

// Show booking details modal
window.showBookingDetails = function(bookingId) {
  const booking = allBookings.find(b => b.id === bookingId);
  if (!booking) return;

  const date = new Date(booking.timestamp || 0);
  const status = booking.status || 'active';
  const tripType = booking.tripType || 'regular';
  const isRegular = tripType === 'regular';
  const bookingApp = booking.bookingApp || 'unknown';
  
  // Get booking app badge
  let bookingAppBadge = '';
  if (bookingApp === 'passengerApp') {
    bookingAppBadge = '<span class="trip-type-badge passenger-app">📱 Online Booking</span>';
  } else if (bookingApp === 'barkerApp') {
    bookingAppBadge = '<span class="trip-type-badge barker-app">🚗 On Site</span>';
  } else {
    bookingAppBadge = '<span class="trip-type-badge unknown">❓ Unknown Source</span>';
  }

  // Build linked passengers section for regular trips
  let linkedPassengersSection = '';
  if (isRegular && booking.poolId) {
    // Find other passengers in the same pool
    const linkedBookings = allBookings.filter(b => 
      b.poolId === booking.poolId && 
      b.id !== booking.id &&
      b.tripType === 'regular'
    );
    
    if (linkedBookings.length > 0) {
      const passengersList = linkedBookings.map((linkedBooking, index) => `
        <div class="passenger-item">
          <div class="passenger-number">${index + 2}</div>
          <div class="passenger-details">
            <div class="passenger-name">${linkedBooking.customerName || 'N/A'}</div>
            <div class="passenger-phone">${linkedBooking.phoneNumber || 'N/A'}</div>
            <div class="passenger-route">
              <small>📍 ${linkedBooking.pickupLocation || 'N/A'} → 🎯 ${linkedBooking.destination || 'N/A'}</small>
            </div>
          </div>
        </div>
      `).join('');
      
      linkedPassengersSection = `
        <div class="detail-section">
          <h3>🚗 Ride Pool Passengers (${linkedBookings.length + 1}/4)</h3>
          <p class="pool-info">For safety and accountability in case of theft or accidents, all passengers in this regular trip are recorded below:</p>
          <div class="passengers-pool">
            <div class="passenger-item current">
              <div class="passenger-number">1</div>
              <div class="passenger-details">
                <div class="passenger-name">${booking.customerName || 'N/A'} <span class="badge-current">Current</span></div>
                <div class="passenger-phone">${booking.phoneNumber || 'N/A'}</div>
                <div class="passenger-route">
                  <small>📍 ${booking.pickupLocation || 'N/A'} → 🎯 ${booking.destination || 'N/A'}</small>
                </div>
              </div>
            </div>
            ${passengersList}
          </div>
          <div class="pool-note">
            <strong>Pool ID:</strong> ${booking.poolId}<br>
            <small>This information is crucial for investigating incidents during the ride.</small>
          </div>
        </div>
      `;
    }
  }

  document.getElementById('modalBody').innerHTML = `
    <div class="detail-section">
      <h3>Booking Information ${bookingAppBadge}</h3>
      <div class="detail-grid">
        <div class="detail-item">
          <div class="detail-label">Booking ID</div>
          <div class="detail-value">${booking.id}</div>
        </div>
        <div class="detail-item">
          <div class="detail-label">Status</div>
          <div class="detail-value">${status.toUpperCase()}</div>
        </div>
        <div class="detail-item">
          <div class="detail-label">Booked Via</div>
          <div class="detail-value">${bookingApp === 'passengerApp' ? 'Online Booking' : bookingApp === 'barkerApp' ? 'On Site' : 'Unknown'}</div>
        </div>
        <div class="detail-item">
          <div class="detail-label">Date & Time</div>
          <div class="detail-value">${date.toLocaleString()}</div>
        </div>
        <div class="detail-item">
          <div class="detail-label">Fare</div>
          <div class="detail-value">₱${booking.actualFare || booking.estimatedFare || '0.00'}</div>
        </div>
      </div>
    </div>

    <div class="detail-section">
      <h3>Passenger Details</h3>
      <div class="detail-grid">
        <div class="detail-item">
          <div class="detail-label">Name</div>
          <div class="detail-value">${booking.customerName || 'N/A'}</div>
        </div>
        <div class="detail-item">
          <div class="detail-label">Phone</div>
          <div class="detail-value">${booking.phoneNumber || 'N/A'}</div>
        </div>
      </div>
    </div>

    ${linkedPassengersSection}

    <div class="detail-section">
      <h3>Driver Details</h3>
      <div class="detail-grid">
        <div class="detail-item">
          <div class="detail-label">Name</div>
          <div class="detail-value">${booking.driverName || 'N/A'}</div>
        </div>
        <div class="detail-item">
          <div class="detail-label">RFID</div>
          <div class="detail-value">${booking.driverRFID || 'N/A'}</div>
        </div>
        <div class="detail-item">
          <div class="detail-label">TODA Number</div>
          <div class="detail-value">${booking.todaNumber || 'N/A'}</div>
        </div>
      </div>
    </div>

    <div class="detail-section">
      <h3>Route Information</h3>
      <div class="route-map">
        <div class="route-point">
          <div class="route-icon">📍</div>
          <div class="route-text">
            <div class="route-label">Pickup Location</div>
            <div class="route-address">${booking.pickupLocation || 'N/A'}</div>
          </div>
        </div>
        <div class="route-point">
          <div class="route-icon">🎯</div>
          <div class="route-text">
            <div class="route-label">Dropoff Location</div>
            <div class="route-address">${booking.destination || 'N/A'}</div>
          </div>
        </div>
        <div class="detail-item" style="margin-top: 12px;">
          <div class="detail-label">Distance</div>
          <div class="detail-value">${booking.distance ? booking.distance.toFixed(2) : 'N/A'} km</div>
        </div>
        <div class="detail-item">
          <div class="detail-label">Payment Method</div>
          <div class="detail-value">${booking.paymentMethod || 'N/A'}</div>
        </div>
      </div>
    </div>

    ${booking.rating ? `
      <div class="detail-section">
        <h3>Rating & Feedback</h3>
        <div class="detail-grid">
          <div class="detail-item">
            <div class="detail-label">Rating</div>
            <div class="detail-value">${'⭐'.repeat(booking.rating)} (${booking.rating}/5)</div>
          </div>
          ${booking.feedback ? `
            <div class="detail-item">
              <div class="detail-label">Feedback</div>
              <div class="detail-value">${booking.feedback}</div>
            </div>
          ` : ''}
        </div>
      </div>
    ` : ''}
  `;

  document.getElementById('bookingModal').classList.add('active');
}

// Close booking modal
window.closeBookingModal = function() {
  document.getElementById('bookingModal').classList.remove('active');
}

// Event listeners
document.getElementById('searchInput').addEventListener('input', applyFilters);
document.getElementById('statusFilter').addEventListener('change', applyFilters);
document.getElementById('tripTypeFilter').addEventListener('change', applyFilters);
document.getElementById('driverFilter').addEventListener('change', applyFilters);
document.getElementById('dateFilter').addEventListener('change', applyFilters);

// Export bookings to CSV
window.exportBookings = function() {
  const driverFilter = document.getElementById('driverFilter').value;
  
  // Get bookings to export
  let bookingsToExport = filteredBookings.length > 0 ? filteredBookings : allBookings;

  if (bookingsToExport.length === 0) {
    showMessage('No bookings to export', 'warning');
    return;
  }

  // Calculate summaries
  const now = new Date();
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
  const weekStart = new Date(now.getFullYear(), now.getMonth(), now.getDate() - now.getDay()).getTime();
  const monthStart = new Date(now.getFullYear(), now.getMonth(), 1).getTime();

  let todayCount = 0;
  let weekCount = 0;
  let monthCount = 0;
  let todayRevenue = 0;
  let weekRevenue = 0;
  let monthRevenue = 0;

  bookingsToExport.forEach(booking => {
    const timestamp = booking.timestamp || 0;
    const fare = parseFloat(booking.fare || 0);

    if (timestamp >= todayStart) {
      todayCount++;
      todayRevenue += fare;
    }
    if (timestamp >= weekStart) {
      weekCount++;
      weekRevenue += fare;
    }
    if (timestamp >= monthStart) {
      monthCount++;
      monthRevenue += fare;
    }
  });

  // Create CSV content
  let csvContent = '';
  
  // Header with driver info if selected
  if (driverFilter) {
    csvContent += `Bookings Report - ${driverFilter}\n`;
  } else {
    csvContent += `Bookings Report - All Drivers\n`;
  }
  
  csvContent += `Generated: ${new Date().toLocaleString()}\n\n`;
  
  // Summary section
  csvContent += `SUMMARY\n`;
  csvContent += `Today,${todayCount} bookings,₱${todayRevenue.toFixed(2)}\n`;
  csvContent += `This Week,${weekCount} bookings,₱${weekRevenue.toFixed(2)}\n`;
  csvContent += `This Month,${monthCount} bookings,₱${monthRevenue.toFixed(2)}\n`;
  csvContent += `Total Bookings,${bookingsToExport.length}\n\n`;
  
  // Data headers
  csvContent += `Date,Time,Booking ID,Customer,Driver,Pickup,Destination,Fare,Distance,Status,Trip Type\n`;
  
  // Data rows
  bookingsToExport.forEach(booking => {
    const date = new Date(booking.timestamp || 0);
    const dateStr = date.toLocaleDateString();
    const timeStr = date.toLocaleTimeString();
    const bookingId = booking.id || 'N/A';
    const customer = (booking.customerName || 'Unknown').replace(/,/g, ';');
    const driver = (booking.driverName || 'Unassigned').replace(/,/g, ';');
    const pickup = (booking.pickupLocation || 'N/A').replace(/,/g, ';');
    const destination = (booking.destination || 'N/A').replace(/,/g, ';');
    const fare = parseFloat(booking.fare || 0).toFixed(2);
    const distance = booking.distance || 'N/A';
    const status = booking.status || 'Unknown';
    const tripType = booking.tripType || 'regular';
    
    csvContent += `${dateStr},${timeStr},${bookingId},${customer},${driver},${pickup},${destination},₱${fare},${distance},${status},${tripType}\n`;
  });

  // Create download link
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  const link = document.createElement('a');
  const url = URL.createObjectURL(blob);
  
  link.setAttribute('href', url);
  const filename = driverFilter 
    ? `bookings_${driverFilter.replace(/\s+/g, '_')}_${new Date().toISOString().split('T')[0]}.csv`
    : `bookings_all_${new Date().toISOString().split('T')[0]}.csv`;
  link.setAttribute('download', filename);
  link.style.visibility = 'hidden';
  
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  
  // Log to audit trail
  logAuditAction('bookings_export', 'export', {
    targetName: driverFilter || 'All Drivers',
    targetId: driverFilter || 'all',
    description: `Exported ${bookingsToExport.length} booking records. Today: ${todayCount} (₱${todayRevenue.toFixed(2)}), Week: ${weekCount} (₱${weekRevenue.toFixed(2)}), Month: ${monthCount} (₱${monthRevenue.toFixed(2)})`,
    metadata: {
      recordCount: bookingsToExport.length,
      todayCount: todayCount,
      todayRevenue: todayRevenue.toFixed(2),
      weekCount: weekCount,
      weekRevenue: weekRevenue.toFixed(2),
      monthCount: monthCount,
      monthRevenue: monthRevenue.toFixed(2)
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

// Close modals when clicking outside
window.addEventListener('click', (event) => {
  if (event.target.classList.contains('modal')) {
    event.target.classList.remove('active');
  }
});

// Initialize
document.addEventListener('DOMContentLoaded', loadBookings);

