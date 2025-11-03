import { initializeApp } from "https://www.gstatic.com/firebasejs/11.0.0/firebase-app.js";
import { getDatabase, ref, onValue, push, set, update, get } from "https://www.gstatic.com/firebasejs/11.0.0/firebase-database.js";

const firebaseConfig = {
  apiKey: "AIzaSyA9ADVig4CiO2Y3ELl3unzXajdzxCgRxHI",
  authDomain: "toda-contribution-system.firebaseapp.com",
  databaseURL: "https://toda-contribution-system-default-rtdb.asia-southeast1.firebasedatabase.app",
  projectId: "toda-contribution-system",
  storageBucket: "toda-contribution-system.firebasestorage.app",
  messagingSenderId: "536068566619",
  appId: "1:536068566619:web:ff7cc576e59b76ae58997e"
};

const app = initializeApp(firebaseConfig);
const db = getDatabase(app);

const formEl = document.getElementById("accountForm");
const fullNameInput = document.getElementById("fullName");
const phoneInput = document.getElementById("phoneNumber");
const emailInput = document.getElementById("email");
const roleSelect = document.getElementById("accountRole");
const passwordInput = document.getElementById("password");
const confirmPasswordInput = document.getElementById("confirmPassword");
const sendCredentialsCheckbox = document.getElementById("sendCredentialsCheckbox");
const createAccountBtn = document.getElementById("createAccountBtn");
const formFeedback = document.getElementById("formFeedback");
const generatePasswordBtn = document.getElementById("generatePasswordBtn");
const accountsListEl = document.getElementById("accountsList");
const searchInput = document.getElementById("searchInput");
const roleFilter = document.getElementById("roleFilter");
const statusFilter = document.getElementById("statusFilter");
const messageContainer = document.getElementById("messageContainer");
const confirmModal = document.getElementById("confirmModal");
const confirmTitle = document.getElementById("confirmTitle");
const confirmMessage = document.getElementById("confirmMessage");
const confirmButton = document.getElementById("confirmButton");
const credentialsModal = document.getElementById("credentialsModal");
const credentialName = document.getElementById("credentialName");
const credentialPhone = document.getElementById("credentialPhone");
const credentialRole = document.getElementById("credentialRole");
const credentialPassword = document.getElementById("credentialPassword");
const copyCredentialsBtn = document.getElementById("copyCredentialsBtn");
const totalAccountsEl = document.getElementById("totalAccounts");
const adminCountEl = document.getElementById("adminCount");
const barkerCountEl = document.getElementById("barkerCount");
const activeCountEl = document.getElementById("activeCount");

let allUserRecords = {};
let managedAccounts = [];
let filteredAccounts = [];
let confirmResolver = null;
let latestCredentials = null;

const ROLE_LABELS = {
  ADMIN: "Administrator",
  BARKER: "Barker"
};

init();

function init() {
  attachEventListeners();
  subscribeToAccounts();
}

function attachEventListeners() {
  formEl?.addEventListener("submit", handleCreateAccount);
  formEl?.addEventListener("reset", () => setFormFeedback());
  generatePasswordBtn?.addEventListener("click", handleGeneratePassword);
  copyCredentialsBtn?.addEventListener("click", copyCredentialsToClipboard);

  document.querySelectorAll(".link-btn[data-toggle]").forEach(btn => {
    btn.addEventListener("click", () => togglePasswordVisibility(btn.dataset.toggle));
  });

  searchInput?.addEventListener("input", applyFilters);
  roleFilter?.addEventListener("change", applyFilters);
  statusFilter?.addEventListener("change", applyFilters);

  accountsListEl?.addEventListener("click", handleAccountListClick);

  document.querySelectorAll('[data-dismiss="modal"]').forEach(btn => {
    btn.addEventListener("click", closeConfirmModal);
  });

  document.querySelectorAll('[data-dismiss="credentials"]').forEach(btn => {
    btn.addEventListener("click", closeCredentialsModal);
  });

  confirmButton?.addEventListener("click", () => {
    if (confirmResolver) {
      const resolver = confirmResolver;
      confirmResolver = null;
      confirmModal.classList.remove("active");
      resolver(true);
    } else {
      closeConfirmModal();
    }
  });

  [confirmModal, credentialsModal].forEach(modal => {
    modal?.addEventListener("click", event => {
      if (event.target === modal) {
        if (modal === confirmModal) {
          closeConfirmModal();
        } else {
          closeCredentialsModal();
        }
      }
    });
  });
}

function subscribeToAccounts() {
  const usersRef = ref(db, "users");
  onValue(usersRef, snapshot => {
    allUserRecords = snapshot.val() || {};
    managedAccounts = Object.entries(allUserRecords)
      .map(([id, payload]) => transformAccount(id, payload))
      .filter(Boolean);
    updateStats(managedAccounts);
    applyFilters();
  }, error => {
    showMessage(`Failed to load accounts: ${error.message}`, "error");
    accountsListEl.innerHTML = '<div class="empty-state">Unable to load accounts right now.</div>';
  });
}

function transformAccount(id, payload) {
  if (!payload) return null;
  const role = (payload.userType || payload.role || "").toUpperCase();
  if (role !== "ADMIN" && role !== "BARKER") return null;

  const name = (payload.name || payload.fullName || "").trim() || "Unnamed User";
  const phone = String(payload.phoneNumber || payload.phone || "");
  const normalizedPhone = normalizePhone(phone);
  const email = (payload.email || "").trim();
  const firebaseEmail = (payload.firebaseAuthEmail || "").trim();
  const createdAt = payload.registrationDate || payload.createdAt || null;
  const lastUpdated = payload.updatedAt || payload.statusChangedAt || null;
  const isActive = payload.isActive !== undefined ? Boolean(payload.isActive) : true;

  return {
    id,
    name,
    email,
    phone,
    normalizedPhone,
    firebaseEmail,
    role,
    isActive,
    createdAt,
    lastUpdated,
    raw: payload
  };
}

function applyFilters() {
  const term = (searchInput?.value || "").trim().toLowerCase();
  const roleValue = roleFilter?.value || "all";
  const statusValue = statusFilter?.value || "all";

  filteredAccounts = managedAccounts.filter(account => {
    const matchesRole = roleValue === "all" || account.role === roleValue;
    const matchesStatus = statusValue === "all" || (statusValue === "active" ? account.isActive : !account.isActive);

    const matchesSearch = !term || [
      account.name.toLowerCase(),
      account.phone.toLowerCase(),
      account.email.toLowerCase(),
      account.firebaseEmail.toLowerCase(),
      account.id.toLowerCase()
    ].some(value => value.includes(term));

    return matchesRole && matchesStatus && matchesSearch;
  });

  filteredAccounts.sort((a, b) => {
    const aDate = a.createdAt || 0;
    const bDate = b.createdAt || 0;
    if (aDate === bDate) return a.name.localeCompare(b.name);
    return bDate - aDate;
  });

  renderAccountList();
}

function renderAccountList() {
  if (!accountsListEl) return;
  if (filteredAccounts.length === 0) {
    accountsListEl.innerHTML = '<div class="empty-state">No accounts match your filters.</div>';
    return;
  }

  const html = filteredAccounts.map(account => {
    const initials = extractInitials(account.name);
    const statusClass = account.isActive ? "active" : "inactive";
    const statusLabel = account.isActive ? "Active" : "Inactive";
    const roleClass = account.role === "ADMIN" ? "admin" : "barker";
    const formattedPhone = formatPhone(account.phone);
    const createdLabel = account.createdAt ? formatDate(account.createdAt) : "Not recorded";
    const updatedLabel = account.lastUpdated ? formatDate(account.lastUpdated) : "Never";

    return `
      <div class="account-card">
        <div class="account-header">
          <div class="account-avatar">${escapeHtml(initials)}</div>
          <div class="account-title">
            <h3>${escapeHtml(account.name)}</h3>
            <span class="role-badge ${roleClass}">${escapeHtml(ROLE_LABELS[account.role] || account.role)}</span>
          </div>
          <span class="status-pill ${statusClass}">${statusLabel}</span>
        </div>
        <div class="account-details">
          <div>
            <div class="detail-label">Phone</div>
            <div class="detail-value">${escapeHtml(formattedPhone || "None")}</div>
          </div>
          <div>
            <div class="detail-label">Email</div>
            <div class="detail-value">${escapeHtml(account.email || account.firebaseEmail || "None")}</div>
          </div>
          <div>
            <div class="detail-label">User ID</div>
            <div class="detail-value">${escapeHtml(account.id)}</div>
          </div>
          <div>
            <div class="detail-label">Created</div>
            <div class="detail-value">${escapeHtml(createdLabel)}</div>
          </div>
          <div>
            <div class="detail-label">Last Updated</div>
            <div class="detail-value">${escapeHtml(updatedLabel)}</div>
          </div>
        </div>
        <div class="account-actions">
          <button class="action-btn primary" data-action="toggle" data-user-id="${escapeHtml(account.id)}">${account.isActive ? "Deactivate" : "Activate"}</button>
          <button class="action-btn" data-action="copy-phone" data-user-id="${escapeHtml(account.id)}">Copy Phone</button>
          <button class="action-btn" data-action="copy-id" data-user-id="${escapeHtml(account.id)}">Copy ID</button>
        </div>
      </div>
    `;
  }).join("");

  accountsListEl.innerHTML = html;
}

async function handleCreateAccount(event) {
  event.preventDefault();
  setFormFeedback();

  const fullName = fullNameInput.value.trim();
  const phone = phoneInput.value.trim();
  const email = emailInput.value.trim();
  const role = roleSelect.value;
  const password = passwordInput.value;
  const confirmPassword = confirmPasswordInput.value;

  if (!fullName) {
    return setFormFeedback("error", "Please enter the full name.");
  }

  const normalizedPhone = normalizePhone(phone);
  if (!normalizedPhone) {
    return setFormFeedback("error", "Please enter a valid phone number (10-15 digits).");
  }

  if (password.length < 6) {
    return setFormFeedback("error", "Password must be at least 6 characters long.");
  }

  if (password !== confirmPassword) {
    return setFormFeedback("error", "Passwords do not match. Please double-check.");
  }

  if (email && !isValidEmail(email)) {
    return setFormFeedback("error", "Please enter a valid email address.");
  }

  const phoneInUse = await isPhoneNumberInUse(normalizedPhone);
  if (phoneInUse) {
    return setFormFeedback("error", "Phone number is already in use. Each account must use a unique number.");
  }

  setCreateButtonState(true);

  try {
    const hashedPassword = await hashPassword(password);
    const now = Date.now();
    const usersRef = ref(db, "users");
    const newUserRef = push(usersRef);
    const userId = newUserRef.key;
    const pseudoEmail = email || `phone_${normalizedPhone}@toda.local`;

    const accountData = {
      id: userId,
      name: fullName,
      phoneNumber: normalizedPhone,
      email: email || null,
      firebaseAuthEmail: pseudoEmail,
      userType: role,
      role,
      isActive: true,
      isVerified: true,
      membershipStatus: "ACTIVE",
      registrationDate: now,
      createdAt: now,
      createdBy: "Role Management",
      passwordHash: hashedPassword,
      lastActiveTime: now,
      updatedAt: now
    };

    await set(newUserRef, accountData);

    const mappingUpdates = [
      set(ref(db, `auth_phone_mapping/${userId}`), {
        uid: userId,
        phoneNumber: normalizedPhone,
        email: pseudoEmail,
        name: fullName,
        role,
        createdAt: now
      }),
      set(ref(db, `phone_auth_mapping/${normalizedPhone}`), {
        uid: userId,
        firebaseAuthEmail: pseudoEmail,
        name: fullName,
        role,
        mappedAt: now
      })
    ];

    await Promise.all(mappingUpdates);

    await logAuditAction(
      "Role Management",
      "Create Account",
      fullName,
      userId,
      {
        role,
        phoneNumber: normalizedPhone,
        email: email || null
      }
    );

    showMessage("Account created successfully.", "success");
    latestCredentials = {
      name: fullName,
      phone: normalizedPhone,
      role: ROLE_LABELS[role] || role,
      password
    };

    if (sendCredentialsCheckbox.checked && latestCredentials) {
      openCredentialsModal(latestCredentials);
    }

    formEl.reset();
    roleSelect.value = role;
    setFormFeedback("success", "New account saved. The list will refresh shortly.");
  } catch (error) {
    console.error("Create account error", error);
    setFormFeedback("error", error.message || "Failed to create account. Please try again.");
  } finally {
    setCreateButtonState(false);
  }
}

async function isPhoneNumberInUse(normalizedPhone) {
  if (!normalizedPhone) return false;
  const usersRef = ref(db, "users");
  try {
    const snapshot = await get(usersRef);
    if (!snapshot.exists()) return false;
    const users = snapshot.val();
    return Object.values(users).some(user => {
      const phone = String(user.phoneNumber || user.phone || "");
      return normalizePhone(phone) === normalizedPhone;
    });
  } catch (error) {
    console.error("Phone number check failed", error);
    return false;
  }
}

function setCreateButtonState(isLoading) {
  if (!createAccountBtn) return;
  createAccountBtn.disabled = isLoading;
  createAccountBtn.classList.toggle("loading", isLoading);
  createAccountBtn.textContent = isLoading ? "Creating..." : "Create Account";
}

function setFormFeedback(type, message) {
  if (!formFeedback) return;
  if (!type || !message) {
    formFeedback.className = "form-feedback";
    formFeedback.textContent = "";
    formFeedback.removeAttribute("style");
    return;
  }

  formFeedback.className = `form-feedback show ${type}`;
  formFeedback.textContent = message;

  if (type === "error") {
    formFeedback.style.background = "#fef2f2";
    formFeedback.style.color = "#b91c1c";
    formFeedback.style.border = "1px solid #fecaca";
  } else if (type === "info") {
    formFeedback.style.background = "#eff6ff";
    formFeedback.style.color = "#1d4ed8";
    formFeedback.style.border = "1px solid #bfdbfe";
  } else {
    formFeedback.style.background = "#ecfdf5";
    formFeedback.style.color = "#047857";
    formFeedback.style.border = "1px solid #a7f3d0";
  }
}

function handleGeneratePassword() {
  const password = generatePassword();
  passwordInput.value = password;
  confirmPasswordInput.value = password;
  sendCredentialsCheckbox.checked = true;
  setFormFeedback("info", "Secure password generated. Share it with the new user.");
}

function generatePassword(length = 12) {
  const alphabet = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789@#$%";
  let password = "";
  const randomValues = crypto.getRandomValues(new Uint32Array(length));
  for (let i = 0; i < length; i++) {
    password += alphabet[randomValues[i] % alphabet.length];
  }
  return password;
}

function togglePasswordVisibility(targetId) {
  const input = document.getElementById(targetId);
  if (!input) return;
  const isPassword = input.type === "password";
  input.type = isPassword ? "text" : "password";
  const toggleBtn = document.querySelector(`.link-btn[data-toggle="${targetId}"]`);
  if (toggleBtn) {
    toggleBtn.textContent = isPassword ? "Hide" : "Show";
  }
}

function handleAccountListClick(event) {
  const button = event.target.closest("button[data-action]");
  if (!button) return;

  const userId = button.dataset.userId;
  const action = button.dataset.action;
  if (!userId || !action) return;

  if (action === "toggle") {
    toggleAccountStatus(userId);
  } else if (action === "copy-phone") {
    copyAccountField(userId, "phone", "Phone number copied to clipboard.");
  } else if (action === "copy-id") {
    copyAccountField(userId, "id", "Account ID copied to clipboard.");
  }
}

async function toggleAccountStatus(userId) {
  const account = managedAccounts.find(item => item.id === userId);
  if (!account) {
    showMessage("Account not found. Please refresh.", "error");
    return;
  }

  const newStatus = !account.isActive;
  const confirmed = await showConfirm({
    title: newStatus ? "Activate Account" : "Deactivate Account",
    message: newStatus
      ? `Activate ${account.name}? They will regain access immediately.`
      : `Deactivate ${account.name}? They will lose access until reactivated.`
  });

  if (!confirmed) return;

  try {
    const updates = {
      isActive: newStatus,
      status: newStatus ? "ACTIVE" : "INACTIVE",
      statusChangedAt: Date.now(),
      updatedAt: Date.now(),
      statusChangedBy: "Role Management"
    };

    await update(ref(db, `users/${userId}`), updates);

    await logAuditAction(
      "Role Management",
      newStatus ? "Activate Account" : "Deactivate Account",
      account.name,
      userId,
      { isActive: newStatus }
    );

    showMessage(`Account ${newStatus ? "activated" : "deactivated"}.`, "success");
  } catch (error) {
    showMessage(error.message || "Failed to update account status.", "error");
  }
}

function copyAccountField(userId, field, successMessage) {
  const account = managedAccounts.find(item => item.id === userId);
  if (!account) return;

  const value = field === "phone" ? formatPhone(account.phone) : account.id;
  navigator.clipboard.writeText(value).then(() => {
    showMessage(successMessage, "info");
  }).catch(() => {
    showMessage("Clipboard unavailable on this device.", "error");
  });
}

function setStatsText(el, value) {
  if (el) el.textContent = value;
}

function updateStats(accounts) {
  const totals = {
    total: accounts.length,
    admin: accounts.filter(a => a.role === "ADMIN").length,
    barker: accounts.filter(a => a.role === "BARKER").length,
    active: accounts.filter(a => a.isActive).length
  };

  setStatsText(totalAccountsEl, totals.total);
  setStatsText(adminCountEl, totals.admin);
  setStatsText(barkerCountEl, totals.barker);
  setStatsText(activeCountEl, totals.active);
}

function showMessage(text, type = "success") {
  if (!messageContainer) return;
  const id = `msg-${Date.now()}`;
  const icons = { success: "✓", error: "✕", info: "ℹ" };
  const message = document.createElement("div");
  message.id = id;
  message.className = `message ${type}`;
  message.innerHTML = `<span>${icons[type] || "ℹ"}</span><span>${escapeHtml(text)}</span>`;
  messageContainer.appendChild(message);
  setTimeout(() => {
    const el = document.getElementById(id);
    if (el) {
      el.style.opacity = "0";
      setTimeout(() => el.remove(), 300);
    }
  }, 4500);
}

function showConfirm({ title, message }) {
  return new Promise(resolve => {
    confirmResolver = resolve;
    confirmTitle.textContent = title;
    confirmMessage.textContent = message;
    confirmModal.classList.add("active");
  });
}

function closeConfirmModal() {
  confirmModal.classList.remove("active");
  if (confirmResolver) {
    confirmResolver(false);
    confirmResolver = null;
  }
}

function openCredentialsModal(credentials) {
  credentialName.textContent = credentials.name;
  credentialPhone.textContent = formatPhone(credentials.phone);
  credentialRole.textContent = credentials.role;
  credentialPassword.textContent = credentials.password;
  credentialsModal.classList.add("active");
}

function closeCredentialsModal() {
  credentialsModal.classList.remove("active");
}

function copyCredentialsToClipboard() {
  if (!latestCredentials) return;
  const text = `Name: ${latestCredentials.name}\nRole: ${latestCredentials.role}\nPhone: ${formatPhone(latestCredentials.phone)}\nPassword: ${latestCredentials.password}`;
  navigator.clipboard.writeText(text).then(() => {
    showMessage("Credentials copied to clipboard.", "info");
  }).catch(() => {
    showMessage("Clipboard unavailable on this device.", "error");
  });
}

async function logAuditAction(module, action, targetName, targetId, changes = {}, metadata = {}) {
  try {
    const auditRef = ref(db, "auditLogs");
    const logRef = push(auditRef);
    await set(logRef, {
      timestamp: new Date().toISOString(),
      module,
      action,
      adminName: "Role Management",
      targetName,
      targetId,
      description: `${action} for ${targetName}`,
      changes,
      metadata
    });
  } catch (error) {
    console.warn("Audit logging skipped", error);
  }
}

function normalizePhone(value) {
  if (!value) return "";
  const digits = String(value).replace(/\D/g, "");
  if (digits.length < 10 || digits.length > 15) return "";
  return digits;
}

function formatPhone(value) {
  const digits = normalizePhone(value);
  if (!digits) return "";
  if (digits.startsWith("63") && digits.length === 12) {
    return `+${digits.slice(0, 2)} ${digits.slice(2, 5)} ${digits.slice(5, 8)} ${digits.slice(8)}`;
  }
  if (digits.length === 11) {
    return `${digits.slice(0, 4)} ${digits.slice(4, 7)} ${digits.slice(7)}`;
  }
  return digits;
}

function extractInitials(name) {
  const parts = name.trim().split(/\s+/).slice(0, 2);
  const initials = parts.map(part => part.charAt(0).toUpperCase()).join("");
  return initials || "RM";
}

function formatDate(value) {
  try {
    const date = typeof value === "number" ? new Date(value) : new Date(value);
    if (Number.isNaN(date.getTime())) return "Invalid date";
    return date.toLocaleString();
  } catch (error) {
    return "Invalid date";
  }
}

async function hashPassword(password) {
  const salt = crypto.getRandomValues(new Uint8Array(16));
  const saltHex = Array.from(salt).map(b => b.toString(16).padStart(2, "0")).join("");
  const encoder = new TextEncoder();
  const passwordData = encoder.encode(password);
  const saltData = encoder.encode(saltHex);
  const combined = new Uint8Array(passwordData.length + saltData.length);
  combined.set(passwordData);
  combined.set(saltData, passwordData.length);
  const hashBuffer = await crypto.subtle.digest("SHA-256", combined);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  const hashHex = hashArray.map(b => b.toString(16).padStart(2, "0")).join("");
  return `${saltHex}:${hashHex}`;
}

function escapeHtml(value) {
  if (value === null || value === undefined) return "";
  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

function isValidEmail(email) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}
