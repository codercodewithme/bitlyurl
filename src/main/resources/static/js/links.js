let allLinks = [];

function renderLinks(links) {
    const body = document.getElementById("links-body");
    const count = document.getElementById("link-count");
    count.textContent = `${links.length} link${links.length === 1 ? "" : "s"}`;
    if (!links.length) {
        body.innerHTML = `<tr><td colspan="5" class="muted">No links yet. Create one from the home page.</td></tr>`;
        return;
    }
    body.innerHTML = links.map((link) => `
        <tr>
            <td>
                <a class="short-cell" href="${escapeHtml(link.shortUrl)}" target="_blank" rel="noopener">${escapeHtml(link.shortUrl)}</a>
                <div>${link.customAlias ? '<span class="badge">custom</span>' : ""} ${escapeHtml(link.title || "")}</div>
            </td>
            <td class="dest-cell">${escapeHtml(link.originalUrl)}</td>
            <td>${link.clickCount}</td>
            <td>${formatDate(link.createdAt)}</td>
            <td>
                <div class="row-actions">
                    <button class="btn btn-ghost" data-copy="${escapeHtml(link.shortUrl)}">Copy</button>
                    <a class="btn btn-ghost" href="/stats/${encodeURIComponent(link.shortCode)}">Stats</a>
                    <button class="btn btn-danger" data-delete="${escapeHtml(link.shortCode)}">Delete</button>
                </div>
            </td>
        </tr>
    `).join("");
}

async function loadLinks() {
    allLinks = await api("/api/urls");
    renderLinks(allLinks);
}

document.addEventListener("DOMContentLoaded", async () => {
    try {
        await loadLinks();
    } catch (error) {
        showToast(error.message, true);
    }

    document.getElementById("search").addEventListener("input", (event) => {
        const query = event.target.value.toLowerCase();
        renderLinks(allLinks.filter((link) =>
            [link.title, link.shortCode, link.originalUrl, link.shortUrl]
                .some((value) => (value || "").toLowerCase().includes(query))
        ));
    });

    document.getElementById("links-body").addEventListener("click", async (event) => {
        const copyBtn = event.target.closest("[data-copy]");
        if (copyBtn) {
            copyText(copyBtn.dataset.copy);
            return;
        }
        const deleteBtn = event.target.closest("[data-delete]");
        if (!deleteBtn) {
            return;
        }
        const code = deleteBtn.dataset.delete;
        if (!confirm(`Delete short link /${code}?`)) {
            return;
        }
        try {
            await api(`/api/urls/${encodeURIComponent(code)}`, { method: "DELETE" });
            showToast("Link deleted");
            await loadLinks();
        } catch (error) {
            showToast(error.message, true);
        }
    });
});
