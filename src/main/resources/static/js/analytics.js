function renderBreakdown(targetId, map) {
    const entries = Object.entries(map || {});
    const list = document.getElementById(targetId);
    if (!entries.length) {
        list.innerHTML = `<li class="muted">No data yet</li>`;
        return;
    }
    list.innerHTML = entries
        .sort((a, b) => b[1] - a[1])
        .map(([label, count]) => `<li>${escapeHtml(label)} <span>${count}</span></li>`)
        .join("");
}

function drawChart(series) {
    const ctx = document.getElementById("clicks-chart");
    new Chart(ctx, {
        type: "line",
        data: {
            labels: series.map((item) => item.date.slice(5)),
            datasets: [{
                label: "Clicks",
                data: series.map((item) => item.clicks),
                borderColor: "#ff6b2c",
                backgroundColor: "rgba(255, 107, 44, 0.15)",
                fill: true,
                tension: 0.35,
                pointRadius: 3
            }]
        },
        options: {
            plugins: { legend: { display: false } },
            scales: {
                y: { beginAtZero: true, ticks: { precision: 0 } }
            }
        }
    });
}

document.addEventListener("DOMContentLoaded", async () => {
    const code = document.querySelector("main").dataset.code;
    try {
        const data = await api(`/api/urls/${encodeURIComponent(code)}/analytics`);
        document.getElementById("analytics-title").textContent = data.title || data.shortCode;
        document.getElementById("analytics-sub").textContent = `${data.shortUrl}  →  ${data.originalUrl}`;
        document.getElementById("total-clicks").textContent = data.totalClicks;
        document.getElementById("link-status").textContent = data.expired ? "Expired" : "Active";
        document.getElementById("created-at").textContent = formatDate(data.createdAt);
        document.getElementById("expires-at").textContent = data.expiresAt ? formatDate(data.expiresAt) : "Never";
        document.getElementById("open-short").href = data.shortUrl;
        document.getElementById("copy-short").addEventListener("click", () => copyText(data.shortUrl));

        const qrUrl = `/api/urls/${encodeURIComponent(code)}/qr`;
        document.getElementById("qr-image").src = qrUrl;
        document.getElementById("qr-download").href = qrUrl;

        drawChart(data.clicksByDay || []);
        renderBreakdown("devices", data.clicksByDevice);
        renderBreakdown("browsers", data.clicksByBrowser);
        renderBreakdown("referrers", data.clicksByReferrer);

        const recent = document.getElementById("recent-clicks");
        if (!data.recentClicks.length) {
            recent.innerHTML = `<tr><td colspan="5" class="muted">No clicks recorded yet.</td></tr>`;
        } else {
            recent.innerHTML = data.recentClicks.map((click) => `
                <tr>
                    <td>${formatDate(click.clickedAt)}</td>
                    <td>${escapeHtml(click.deviceType)}</td>
                    <td>${escapeHtml(click.browser)}</td>
                    <td class="dest-cell">${escapeHtml(click.referrer)}</td>
                    <td>${escapeHtml(click.ipAddress)}</td>
                </tr>
            `).join("");
        }
    } catch (error) {
        showToast(error.message, true);
        document.getElementById("analytics-sub").textContent = error.message;
    }
});
