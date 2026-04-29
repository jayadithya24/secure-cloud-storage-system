const BASE_URL = "http://localhost:8080/api";
const SESSION_KEYS = {
    token: "token",
    email: "userEmail",
    userId: "userId"
};

const PASSWORD_RULES = [
    {
        id: "ruleLength",
        label: "8-32 characters",
        test: (value) => value.length >= 8 && value.length <= 32
    },
    {
        id: "ruleUppercase",
        label: "At least one uppercase letter",
        test: (value) => /[A-Z]/.test(value)
    },
    {
        id: "ruleLowercase",
        label: "At least one lowercase letter",
        test: (value) => /[a-z]/.test(value)
    },
    {
        id: "ruleNumber",
        label: "At least one number",
        test: (value) => /\d/.test(value)
    },
    {
        id: "ruleSymbol",
        label: "At least one special character",
        test: (value) => /[^A-Za-z0-9]/.test(value)
    }
];

function getStoredSession() {
    return {
        token: localStorage.getItem(SESSION_KEYS.token),
        email: localStorage.getItem(SESSION_KEYS.email),
        userId: localStorage.getItem(SESSION_KEYS.userId)
    };
}

function saveSession(user, token) {
    if (token) {
        localStorage.setItem(SESSION_KEYS.token, token);
    }

    if (user?.email) {
        localStorage.setItem(SESSION_KEYS.email, user.email);
    }

    if (user?.id) {
        localStorage.setItem(SESSION_KEYS.userId, user.id);
    }
}

function clearSession() {
    Object.values(SESSION_KEYS).forEach((key) => localStorage.removeItem(key));
}

function getSessionEmail() {
    return localStorage.getItem(SESSION_KEYS.email) || "";
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}

function showToast(title, message = "", type = "info") {
    let toast = document.getElementById("appToast");

    if (!toast) {
        toast = document.createElement("div");
        toast.id = "appToast";
        toast.className = "toast";
        toast.innerHTML = `
            <div class="toast-title"></div>
            <div class="toast-body"></div>
        `;
        document.body.appendChild(toast);
    }

    toast.className = `toast ${type}`;
    toast.querySelector(".toast-title").textContent = title;
    toast.querySelector(".toast-body").textContent = message;
    toast.classList.add("is-visible");

    window.clearTimeout(toast._hideTimer);
    toast._hideTimer = window.setTimeout(() => {
        toast.classList.remove("is-visible");
    }, 2800);
}

function setStatus(message, type = "success", elementId = "status") {
    const statusElement = document.getElementById(elementId);

    if (!statusElement) {
        return;
    }

    statusElement.textContent = message;
    statusElement.className = `status-message ${type}`;
}

function passwordChecks(value) {
    return PASSWORD_RULES.map((rule) => ({
        ...rule,
        valid: rule.test(value)
    }));
}

function validatePassword(value) {
    const checks = passwordChecks(value);
    const valid = checks.every((rule) => rule.valid);

    return {
        valid,
        checks,
        message: valid
            ? ""
            : "Password must be 8-32 characters and include uppercase, lowercase, number, and special character."
    };
}

function renderPasswordGuidance(passwordValue = "") {
    const checklist = document.getElementById("passwordChecklist");
    const strengthBar = document.getElementById("passwordStrengthBar");
    const strengthLabel = document.getElementById("passwordStrengthLabel");

    if (!checklist) {
        return;
    }

    const checks = passwordChecks(passwordValue);

    checklist.innerHTML = checks.map((rule) => `
        <li class="${rule.valid ? "is-valid" : ""}">${escapeHtml(rule.label)}</li>
    `).join("");

    if (strengthBar && strengthLabel) {
        const score = checks.filter((rule) => rule.valid).length;
        const strengthMap = [
            { label: "Very weak", width: "12%" },
            { label: "Weak", width: "28%" },
            { label: "Fair", width: "52%" },
            { label: "Strong", width: "76%" },
            { label: "Excellent", width: "100%" }
        ];
        const strength = strengthMap[Math.min(score, strengthMap.length - 1)];

        strengthBar.style.width = strength.width;
        strengthLabel.textContent = passwordValue ? strength.label : "Start typing to see password strength";
    }
}

function bindPasswordGuidance() {
    const passwordField = document.getElementById("password");

    if (!passwordField) {
        return;
    }

    renderPasswordGuidance(passwordField.value);
    passwordField.addEventListener("input", (event) => renderPasswordGuidance(event.target.value));
}

function bindPasswordToggle() {
    document.querySelectorAll("[data-password-toggle]").forEach((button) => {
        button.addEventListener("click", () => {
            const targetId = button.getAttribute("data-password-toggle");
            const target = document.getElementById(targetId);

            if (!target) {
                return;
            }

            const isPassword = target.type === "password";
            target.type = isPassword ? "text" : "password";
            button.textContent = isPassword ? "Hide" : "Show";
        });
    });
}

function updateSessionBanner() {
    const email = getSessionEmail();
    const label = document.getElementById("sessionEmailLabel");

    if (label && email) {
        label.textContent = email;
    }
}

async function readResponseBody(response) {
    const contentType = response.headers.get("content-type") || "";

    if (contentType.includes("application/json")) {
        return await response.json();
    }

    return await response.text();
}

async function loginUser(event) {
    event.preventDefault();

    const email = document.getElementById("email")?.value.trim();
    const password = document.getElementById("password")?.value;

    if (!email || !password) {
        showToast("Missing fields", "Email and password are required.", "error");
        return;
    }

    try {
        const response = await fetch(`${BASE_URL}/login`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ email, password })
        });

        const data = await readResponseBody(response);

        if (!response.ok) {
            const message = typeof data === "string" ? data : data?.message || "Invalid credentials";
            showToast("Login failed", message, "error");
            setStatus(message, "error");
            return;
        }

        const user = data?.user || data;
        const token = data?.token || "";

        saveSession(user, token);
        showToast("Welcome back", `Signed in as ${user?.email || email}`, "success");
        setStatus("Login successful. Redirecting to dashboard...", "success");
        window.location.href = "dashboard.html";
    } catch (error) {
        console.error(error);
        showToast("Server error", "Unable to log in right now.", "error");
        setStatus("Server error. Please try again.", "error");
    }
}

async function registerUser(event) {
    event.preventDefault();

    const email = document.getElementById("email")?.value.trim();
    const password = document.getElementById("password")?.value;
    const confirmPassword = document.getElementById("confirmPassword")?.value;
    const passwordState = validatePassword(password || "");

    if (!email || !password || !confirmPassword) {
        showToast("Missing fields", "Please complete every field.", "error");
        return;
    }

    if (password !== confirmPassword) {
        showToast("Passwords do not match", "Confirm password must match the password.", "error");
        setStatus("Password and confirm password do not match.", "error");
        return;
    }

    if (!passwordState.valid) {
        showToast("Password too weak", passwordState.message, "error");
        setStatus(passwordState.message, "error");
        renderPasswordGuidance(password || "");
        return;
    }

    try {
        const response = await fetch(`${BASE_URL}/register`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                email,
                password
            })
        });

        const data = await readResponseBody(response);

        if (!response.ok) {
            const message = typeof data === "string" ? data : data?.message || "Registration failed";
            showToast("Registration failed", message, "error");
            setStatus(message, "error");
            return;
        }

        const user = data?.user || data;
        showToast("Registration successful", `Account created for ${user?.email || email}.`, "success");
        setStatus("Account created successfully. Redirecting to login...", "success");
        event.target.reset();
        renderPasswordGuidance("");
        window.location.href = "login.html";
    } catch (error) {
        console.error(error);
        showToast("Server error", "Unable to register right now.", "error");
        setStatus("Server error. Please try again.", "error");
    }
}

function renderFileTable(files, tableBody, options = {}) {
    const { showPath = true, allowDelete = true, showId = true } = options;

    if (!tableBody) {
        return;
    }

    if (!files || files.length === 0) {
        tableBody.innerHTML = `
            <tr>
                <td colspan="${showPath ? (showId ? 5 : 4) : (showId ? 4 : 3)}">
                    <div class="empty-state">
                        <strong>No files found yet.</strong>
                        <div style="margin-top:8px;">Upload your first file to see it appear here.</div>
                    </div>
                </td>
            </tr>
        `;
        return;
    }

    tableBody.innerHTML = files.map((file) => {
        const safeName = escapeHtml(file.fileName);
        const safeOwner = escapeHtml(file.ownerEmail || file.owner || "You");
        const safePath = escapeHtml(file.filePath || "-");
        const buttons = [
            `<button class="action-btn" onclick="downloadFile('${file.id}')">Download</button>`,
            `<button class="action-btn" onclick="window.location.href='share.html'">Share</button>`
        ];

        if (allowDelete) {
            buttons.push(`<button class="action-btn delete" onclick="deleteFile('${file.id}')">Delete</button>`);
        }

        return `
            <tr>
                ${showId ? `<td>${escapeHtml(file.id)}</td>` : ""}
                <td>${safeName}</td>
                <td>${safeOwner}</td>
                ${showPath ? `<td class="muted">${safePath}</td>` : ""}
                <td>
                    <div class="button-row">${buttons.join("")}</div>
                </td>
            </tr>
        `;
    }).join("");
}

async function loadFiles() {
    const email = getSessionEmail();
    const tableBody = document.getElementById("fileTableBody") || document.getElementById("uploadedFilesBody") || document.querySelector("tbody");
    const fileCountLabel = document.getElementById("fileCount");
    const sharedCountLabel = document.getElementById("sharedCount");

    if (!email) {
        window.location.href = "login.html";
        return;
    }

    try {
        const response = await fetch(`${BASE_URL}/files?email=${encodeURIComponent(email)}`);
        const files = await response.json();

        renderFileTable(files, tableBody, { showPath: true, allowDelete: true, showId: true });

        if (fileCountLabel) {
            fileCountLabel.textContent = files.length;
        }

        if (sharedCountLabel && sharedCountLabel.dataset.role === "shared") {
            sharedCountLabel.textContent = files.length;
        }
    } catch (error) {
        console.error(error);
        if (tableBody) {
            tableBody.innerHTML = `
                <tr>
                    <td colspan="4">
                        <div class="empty-state">Failed to load files. Please refresh the page.</div>
                    </td>
                </tr>
            `;
        }
        showToast("Loading error", "Failed to load files from the server.", "error");
    }
}

async function loadSharedFiles() {
    const email = getSessionEmail();
    const tableBody = document.getElementById("accessTable");

    if (!email || !tableBody) {
        return;
    }

    try {
        const response = await fetch(`${BASE_URL}/shared-files?email=${encodeURIComponent(email)}`);
        const sharedFiles = await response.json();

        if (!sharedFiles.length) {
            tableBody.innerHTML = `
                <tr>
                    <td colspan="3">
                        <div class="empty-state">No shared files found for this account.</div>
                    </td>
                </tr>
            `;
            return;
        }

        tableBody.innerHTML = sharedFiles.map((file) => `
            <tr>
                <td>${escapeHtml(file.fileName)}</td>
                <td>${escapeHtml(file.ownerEmail)}</td>
                <td>
                    <div class="button-row">
                        <button class="action-btn" onclick="openSharedFileLink('${encodeURIComponent(file.shareLink || "")}')">Open link</button>
                        <button class="action-btn" onclick="copySharedFileLink('${encodeURIComponent(file.shareLink || "")}')">Copy link</button>
                    </div>
                </td>
            </tr>
        `).join("");
    } catch (error) {
        console.error(error);
        tableBody.innerHTML = `
            <tr>
                <td colspan="3">
                    <div class="empty-state">Unable to load shared files.</div>
                </td>
            </tr>
        `;
    }
}

function formatAlertTime(isoValue) {
    if (!isoValue) {
        return "Unknown time";
    }

    const parsed = new Date(isoValue);
    if (Number.isNaN(parsed.getTime())) {
        return "Unknown time";
    }

    return parsed.toLocaleString();
}

async function loadOwnerAlerts() {
    const email = getSessionEmail();
    const alertsContainer = document.getElementById("ownerAlertsList");
    const alertCountLabel = document.getElementById("alertCount");

    if (!email || !alertsContainer) {
        return;
    }

    try {
        const response = await fetch(`${BASE_URL}/alerts?email=${encodeURIComponent(email)}`);
        const payload = await readResponseBody(response);

        if (!response.ok) {
            throw new Error(typeof payload === "string" ? payload : payload?.message || "Unable to load alerts");
        }


        const alerts = Array.isArray(payload) ? payload : [];
        const suspiciousAlerts = alerts.filter((alert) =>
            alert?.action === "RECIPIENT_SUSPICIOUS" ||
            alert?.action === "RECIPIENT_EXCEEDED_DOWNLOAD_LIMIT" ||
            alert?.action === "RECIPIENT_EXCEEDED_TIME_LIMIT"
        );

        if (alertCountLabel) {
            alertCountLabel.textContent = String(suspiciousAlerts.length);
        }

        if (!suspiciousAlerts.length) {
            alertsContainer.innerHTML = `
                <div class="empty-state">No suspicious recipient activity has been detected yet.</div>
            `;
            return;
        }

        alertsContainer.innerHTML = suspiciousAlerts.map((alert) => {
            const details = escapeHtml(alert.details || "Suspicious activity detected.");
            const occurredAt = escapeHtml(formatAlertTime(alert.createdAt));
            let title = "Recipient flagged as suspicious";
            if (alert.action === "RECIPIENT_EXCEEDED_DOWNLOAD_LIMIT") {
                title = "Recipient exceeded download limit";
            } else if (alert.action === "RECIPIENT_EXCEEDED_TIME_LIMIT") {
                title = "Recipient exceeded time limit";
            }
            return `
                <article class="alert-item warning">
                    <div class="alert-item-head">
                        <strong>${title}</strong>
                        <span>${occurredAt}</span>
                    </div>
                    <p>${details}</p>
                </article>
            `;
        }).join("");
    } catch (error) {
        console.error(error);
        if (alertCountLabel) {
            alertCountLabel.textContent = "0";
        }

        alertsContainer.innerHTML = `
            <div class="empty-state">Unable to load security alerts right now.</div>
        `;
    }
}

function openSharedFileLink(encodedLink) {
    const link = decodeURIComponent(encodedLink || "");
    if (!link) {
        showToast("Missing link", "No share link is available for this file.", "error");
        return;
    }

    window.open(link, "_blank", "noopener,noreferrer");
}

async function copySharedFileLink(encodedLink) {
    const link = decodeURIComponent(encodedLink || "");
    if (!link) {
        showToast("Missing link", "No share link is available for this file.", "error");
        return;
    }

    try {
        await navigator.clipboard.writeText(link);
        showToast("Link copied", "Shared file link copied to clipboard.", "success");
    } catch (error) {
        console.error(error);
        showToast("Copy failed", "Could not copy shared file link.", "error");
    }
}

async function uploadSelectedFile() {
    const fileInput = document.getElementById("fileInput");
    const file = fileInput?.files?.[0];

    if (!file) {
        showToast("Choose a file", "Select a file before uploading.", "error");
        setStatus("Select a file before uploading.", "error");
        return;
    }

    await uploadFile(file);
}

async function uploadFile(file) {
    const email = getSessionEmail();

    if (!email) {
        window.location.href = "login.html";
        return;
    }

    const formData = new FormData();
    formData.append("file", file);
    formData.append("email", email);

    try {
        setStatus("Uploading file...", "success");
        const response = await fetch(`${BASE_URL}/upload`, {
            method: "POST",
            body: formData
        });

        if (!response.ok) {
            const message = await readResponseBody(response);
            const errorMessage = typeof message === "string" ? message : message?.message || "Upload failed";
            throw new Error(errorMessage);
        }

        showToast("Upload complete", `${file.name} has been uploaded successfully.`, "success");
        setStatus("File uploaded successfully. Redirecting to dashboard...", "success");
        window.location.href = "dashboard.html";
    } catch (error) {
        console.error(error);
        showToast("Upload failed", error.message || "Unable to upload file.", "error");
        setStatus(error.message || "Unable to upload file.", "error");
    }
}

async function shareFile() {
    const fileId = document.getElementById("fileId")?.value.trim();
    const email = document.getElementById("shareEmail")?.value.trim();
    const permission = document.getElementById("permission")?.value;
    const maxDownloadsRaw = document.getElementById("maxDownloads")?.value.trim();
    const expiresAtRaw = document.getElementById("expiresAt")?.value;

    if (!fileId || !email) {
        showToast("Missing data", "File ID and recipient email are required.", "error");
        setStatus("File ID and recipient email are required.", "error");
        return;
    }

    let maxDownloads = "";
    if (maxDownloadsRaw) {
        const parsedMax = Number(maxDownloadsRaw);
        if (!Number.isInteger(parsedMax) || parsedMax < 1) {
            showToast("Invalid limit", "Max downloads must be a whole number starting from 1.", "error");
            setStatus("Max downloads must be a whole number starting from 1.", "error");
            return;
        }

        maxDownloads = String(parsedMax);
    }

    let expiresAtIso = "";
    if (expiresAtRaw) {
        const parsedDate = new Date(expiresAtRaw);
        if (Number.isNaN(parsedDate.getTime())) {
            showToast("Invalid time", "Please provide a valid expiry date/time.", "error");
            setStatus("Please provide a valid expiry date/time.", "error");
            return;
        }

        if (parsedDate.getTime() <= Date.now()) {
            showToast("Invalid time", "Expiry time must be in the future.", "error");
            setStatus("Expiry time must be in the future.", "error");
            return;
        }

        expiresAtIso = parsedDate.toISOString();
    }

    try {
        const params = new URLSearchParams();
        params.set("fileId", fileId);
        params.set("email", email);
        params.set("permission", permission);

        if (maxDownloads) {
            params.set("maxDownloads", maxDownloads);
        }

        if (expiresAtIso) {
            params.set("expiresAt", expiresAtIso);
        }

        const response = await fetch(`${BASE_URL}/share?${params.toString()}`, {
            method: "POST"
        });

        const message = await readResponseBody(response);

        if (!response.ok) {
            const errorMessage = typeof message === "string" ? message : message?.message || "Unable to share file";
            throw new Error(errorMessage);
        }

        const responseText = typeof message === "string" ? message : "File shared successfully";
        const linkMatch = responseText.match(/https?:\/\/\S+/);
        const shareLink = linkMatch ? linkMatch[0].replace(/[),.]$/, "") : "";
        const emailConfigured = /email sent to/i.test(responseText) || /email re-sent to/i.test(responseText);
        const emailFallback = /email delivery is not configured/i.test(responseText);
        const summaryText = responseText
            .replace(/^Share link:\s*/i, "")
            .replace(/\| Email sent to .*$/i, "")
            .replace(/\| Email re-sent to .*$/i, "")
            .replace(/\| Permission:\s*(VIEW|EDIT)$/i, "")
            .trim();

        if (emailFallback) {
            showToast("Share saved", "Email is not configured here, but the share link is ready to copy.", "success");
            setStatus(`${summaryText} (${permission.toLowerCase()} access selected)`, "success");
            return;
        }

        const statusMessage = summaryText || "File shared successfully";

        if (emailConfigured) {
            showToast("Shared", `${statusMessage} (${permission.toLowerCase()} access selected).`, "success");
            setStatus(`${statusMessage} (${permission.toLowerCase()} access selected)`, "success");
            return;
        }

        showToast("Shared", "File shared successfully. Copy the link below if needed.", "success");
        setStatus(`${statusMessage} (${permission.toLowerCase()} access selected)`, "success");
    } catch (error) {
        console.error(error);
        showToast("Share failed", error.message || "Unable to share file.", "error");
        setStatus(error.message || "Unable to share file.", "error");
    }
}

async function deleteFile(fileId) {
    const email = getSessionEmail();

    if (!email) {
        window.location.href = "login.html";
        return;
    }

    const confirmDelete = window.confirm("Delete this file permanently?");
    if (!confirmDelete) {
        return;
    }

    try {
        const response = await fetch(`${BASE_URL}/file/${fileId}`, {
            method: "DELETE"
        });

        const message = await readResponseBody(response);

        if (!response.ok) {
            throw new Error(typeof message === "string" ? message : message?.message || "Unable to delete file");
        }

        showToast("Deleted", typeof message === "string" ? message : "File deleted successfully.", "success");
        loadFiles();
    } catch (error) {
        console.error(error);
        showToast("Delete failed", error.message || "Unable to delete file.", "error");
    }
}

function downloadFile(fileId) {
    window.open(`${BASE_URL}/download/${fileId}`, "_blank", "noopener,noreferrer");
}

function revokeAccess(button) {
    const row = button?.closest("tr");

    if (row) {
        row.remove();
    }

    showToast("Access revoked", "The selected sharing entry has been removed.", "success");
}

function logout() {
    clearSession();
    showToast("Signed out", "You have been logged out.", "success");
    window.location.href = "login.html";
}

function goDashboard() {
    window.location.href = "dashboard.html";
}

function goUpload() {
    window.location.href = "upload.html";
}

function goShare() {
    window.location.href = "share.html";
}

function initAuthPages() {
    const loginForm = document.getElementById("loginForm");
    const registerForm = document.getElementById("registerForm");

    if (loginForm) {
        loginForm.addEventListener("submit", loginUser);
    }

    if (registerForm) {
        registerForm.addEventListener("submit", registerUser);
        bindPasswordGuidance();
    }

    bindPasswordToggle();
}

function initAppPage() {
    updateSessionBanner();

    if (document.getElementById("fileTableBody") || document.getElementById("uploadedFilesBody")) {
        loadFiles();
    }

    if (document.getElementById("accessTable")) {
        loadSharedFiles();
    }

    if (document.getElementById("ownerAlertsList")) {
        loadOwnerAlerts();
        window.setInterval(loadOwnerAlerts, 15000);
    }

    const uploadButton = document.getElementById("uploadButton");
    if (uploadButton) {
        uploadButton.addEventListener("click", uploadSelectedFile);
    }
}

function initLandingPage() {
    // Landing page only needs shared shell styling.
}

function initApp() {
    initAuthPages();
    initAppPage();
    initLandingPage();
}

if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", initApp);
} else {
    initApp();
}
