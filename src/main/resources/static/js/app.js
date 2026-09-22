const toastEl = document.getElementById("toast");

function showToast(message, isError = false) {
    if (!toastEl) {
        return;
    }
    toastEl.textContent = message;
    toastEl.classList.toggle("error", isError);
    toastEl.classList.remove("hidden");
    window.clearTimeout(showToast._timer);
    showToast._timer = window.setTimeout(() => toastEl.classList.add("hidden"), 2800);
}

async function api(path, options = {}) {
    const response = await fetch(path, {
        headers: { "Content-Type": "application/json", ...(options.headers || {}) },
        ...options
    });
    if (response.status === 204) {
        return null;
    }
    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
        throw new Error(data.message || "Request failed");
    }
    return data;
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;");
}

function formatDate(value) {
    if (!value) {
        return "—";
    }
    return new Date(value).toLocaleString();
}

async function copyText(text) {
    await navigator.clipboard.writeText(text);
    showToast("Copied to clipboard");
}

async function loadDashboard() {
    const cards = document.querySelectorAll("[data-stat]");
    if (!cards.length) {
        return;
    }
    try {
        const stats = await api("/api/stats/summary");
        cards.forEach((el) => {
            el.textContent = stats[el.dataset.stat] ?? 0;
        });
    } catch (error) {
        showToast(error.message, true);
    }
}

function renderResult(payload) {
    const box = document.getElementById("result");
    if (!box) {
        return;
    }
    box.classList.remove("hidden");
    box.innerHTML = `
        <h2>${escapeHtml(payload.title || "Link ready")}</h2>
        <div class="result-url">
            <a href="${escapeHtml(payload.shortUrl)}" target="_blank" rel="noopener">${escapeHtml(payload.shortUrl)}</a>
            <button type="button" class="btn btn-ghost" data-copy="${escapeHtml(payload.shortUrl)}">Copy</button>
            <a class="btn btn-ghost" href="/stats/${encodeURIComponent(payload.shortCode)}">Analytics</a>
        </div>
        <p class="result-meta">Redirects to ${escapeHtml(payload.originalUrl)}</p>
    `;
}

function initShortenForm() {
    const form = document.getElementById("shorten-form");
    if (!form) {
        return;
    }
    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        const button = document.getElementById("shorten-btn");
        const expiresAt = document.getElementById("expiresAt").value;
        const body = {
            originalUrl: document.getElementById("originalUrl").value.trim(),
            title: document.getElementById("title").value.trim(),
            customAlias: document.getElementById("customAlias").value.trim(),
            expiresAt: expiresAt ? (expiresAt.length === 16 ? `${expiresAt}:00` : expiresAt) : null
        };
        button.disabled = true;
        button.textContent = "Shortening…";
        try {
            const created = await api("/api/urls", {
                method: "POST",
                body: JSON.stringify(body)
            });
            renderResult(created);
            form.reset();
            loadDashboard();
            showToast("Short link created");
        } catch (error) {
            showToast(error.message, true);
        } finally {
            button.disabled = false;
            button.textContent = "Shorten URL";
        }
    });

    document.addEventListener("click", (event) => {
        const button = event.target.closest("[data-copy]");
        if (button) {
            copyText(button.dataset.copy);
        }
    });
}

document.addEventListener("DOMContentLoaded", () => {
    loadDashboard();
    initShortenForm();
});
