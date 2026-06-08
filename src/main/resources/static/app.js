let plants = [];

async function apiFetch(path, options = {}) {
    const res = await fetch(path, {
        headers: { 'Content-Type': 'application/json' },
        ...options
    });
    if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
    if (res.status === 204) return null;
    return res.json();
}

function showTab(name) {
    document.querySelectorAll('.tab').forEach(t => {
        t.classList.toggle('active', t.dataset.tab === name);
    });
    document.querySelectorAll('.section').forEach(s => {
        s.classList.toggle('active', s.id === name);
    });
    if (name === 'watering') {
        populatePlantDropdown();
        loadWateringEvents();
    }
    if (name === 'history') {
        populateHistoryDropdown();
        loadHistory();
    }
}

// ── Plants ────────────────────────────────────────────────────────────────────

async function loadPlants() {
    const list = document.getElementById('plant-list');
    try {
        plants = await apiFetch('/plants');
        list.innerHTML = plants.length === 0
            ? '<p class="empty">No plants yet.</p>'
            : plants.map(p => `
                <div class="card">
                    <div class="card-info">
                        <strong>${p.name}</strong>
                        <span>${[p.species, p.location].filter(Boolean).join(' · ')}</span>
                    </div>
                    <button class="delete-btn" onclick="deletePlant(${p.id})" title="Delete">✕</button>
                </div>
            `).join('');
    } catch (e) {
        list.innerHTML = '<p class="error">Failed to load plants.</p>';
    }
}

async function addPlant(event) {
    event.preventDefault();
    const error = document.getElementById('plant-error');
    error.textContent = '';
    const body = {
        name: document.getElementById('plant-name').value,
        species: document.getElementById('plant-species').value || null,
        location: document.getElementById('plant-location').value || null,
    };
    try {
        await apiFetch('/plants', { method: 'POST', body: JSON.stringify(body) });
        event.target.reset();
        await loadPlants();
    } catch (e) {
        error.textContent = 'Failed to add plant.';
    }
}

async function deletePlant(id) {
    try {
        await apiFetch(`/plants/${id}`, { method: 'DELETE' });
        await loadPlants();
    } catch (e) {
        alert('Failed to delete plant.');
    }
}

// ── Watering ──────────────────────────────────────────────────────────────────

async function loadWateringEvents() {
    const list = document.getElementById('watering-list');
    try {
        const events = await apiFetch('/watering');
        list.innerHTML = events.length === 0
            ? '<p class="empty">No watering events yet.</p>'
            : events.map(e => {
                const plant = plants.find(p => p.id === e.plantId);
                const date = new Date(e.wateredAt).toLocaleString();
                const details = [e.amountMl ? `${e.amountMl} ml` : null, e.note].filter(Boolean).join(' · ');
                return `
                    <div class="card">
                        <div class="card-info">
                            <strong>${plant ? plant.name : 'Unknown plant'}</strong>
                            <span>${date}${details ? ' · ' + details : ''}</span>
                        </div>
                        <button class="delete-btn" onclick="deleteWateringEvent(${e.id})" title="Delete">✕</button>
                    </div>
                `;
            }).join('');
    } catch (e) {
        list.innerHTML = '<p class="error">Failed to load watering events.</p>';
    }
}

async function recordWatering(event) {
    event.preventDefault();
    const error = document.getElementById('watering-error');
    error.textContent = '';
    const amountVal = document.getElementById('watering-amount').value;
    const body = {
        plantId: parseInt(document.getElementById('watering-plant').value),
        amountMl: amountVal ? parseInt(amountVal) : null,
        note: document.getElementById('watering-note').value || null,
    };
    try {
        await apiFetch('/watering', { method: 'POST', body: JSON.stringify(body) });
        event.target.reset();
        populatePlantDropdown();
        await loadWateringEvents();
    } catch (e) {
        error.textContent = 'Failed to record watering.';
    }
}

async function deleteWateringEvent(id) {
    try {
        await apiFetch(`/watering/${id}`, { method: 'DELETE' });
        await loadWateringEvents();
    } catch (e) {
        alert('Failed to delete watering event.');
    }
}

function populatePlantDropdown() {
    const select = document.getElementById('watering-plant');
    select.innerHTML = plants.length === 0
        ? '<option value="">No plants available</option>'
        : plants.map(p => `<option value="${p.id}">${p.name}</option>`).join('');
}

// ── History ───────────────────────────────────────────────────────────────────

let historyChart = null;

function populateHistoryDropdown() {
    const select = document.getElementById('history-plant');
    select.innerHTML = plants.length === 0
        ? '<option value="">No plants available</option>'
        : plants.map(p => `<option value="${p.id}">${p.name}</option>`).join('');
}

async function loadHistory() {
    const list = document.getElementById('history-list');
    const chartContainer = document.getElementById('history-chart-container');
    const statsContainer = document.getElementById('history-stats');
    const plantId = document.getElementById('history-plant').value;
    if (!plantId) return;
    try {
        const [events, analytics] = await Promise.all([
            apiFetch(`/watering/plant/${plantId}`),
            apiFetch(`/analytics/plant/${plantId}`)
        ]);

        if (events.length === 0) {
            chartContainer.style.display = 'none';
            statsContainer.style.display = 'none';
            list.innerHTML = '<p class="empty">No watering events recorded yet.</p>';
            return;
        }

        renderStats(analytics, statsContainer);
        chartContainer.style.display = 'block';
        renderHistoryChart(events);

        list.innerHTML = events.map(e => {
            const date = new Date(e.wateredAt).toLocaleString();
            const details = [e.amountMl ? `${e.amountMl} ml` : null, e.note].filter(Boolean).join(' · ');
            return `
                <div class="card">
                    <div class="card-info">
                        <strong>${date}</strong>
                        <span>${details || '—'}</span>
                    </div>
                </div>
            `;
        }).join('');
    } catch (e) {
        list.innerHTML = '<p class="error">Failed to load history.</p>';
    }
}

function renderStats(analytics, container) {
    const stats = [
        { label: 'Total waterings', value: analytics.totalWaterings },
        { label: 'Avg. days between waterings', value: analytics.averageDaysBetweenWaterings != null ? analytics.averageDaysBetweenWaterings.toFixed(1) : '—' },
        { label: 'Days since last watering', value: analytics.daysSinceLastWatering ?? '—' },
    ];
    container.style.display = 'flex';
    container.innerHTML = stats.map(s => `
        <div style="flex:1; background:white; padding:1rem; border-radius:8px; box-shadow:0 1px 3px rgba(0,0,0,0.1); text-align:center;">
            <div style="font-size:1.5rem; font-weight:bold; color:#2d6a2d;">${s.value}</div>
            <div style="font-size:0.8rem; color:#666; margin-top:0.25rem;">${s.label}</div>
        </div>
    `).join('');
}

function renderHistoryChart(events) {
    // Events arrive newest-first; reverse for chronological order on the chart
    const sorted = [...events].reverse();

    if (historyChart) {
        historyChart.destroy();
    }

    const ctx = document.getElementById('history-chart').getContext('2d');
    historyChart = new Chart(ctx, {
        type: 'line',
        data: {
            datasets: [{
                label: 'Amount (ml)',
                data: sorted.map(e => ({ x: e.wateredAt, y: e.amountMl ?? 0 })),
                borderColor: '#2d6a2d',
                backgroundColor: '#2d6a2d',
                pointRadius: 5,
                pointHoverRadius: 7,
                tension: 0,
            }]
        },
        options: {
            responsive: true,
            plugins: {
                legend: { display: false },
                tooltip: {
                    callbacks: {
                        footer: (items) => {
                            const e = sorted[items[0].dataIndex];
                            return e.note ? `Note: ${e.note}` : '';
                        }
                    }
                }
            },
            scales: {
                x: {
                    type: 'time',
                    time: { unit: 'day', displayFormats: { day: 'MMM d' } },
                    title: { display: true, text: 'Date' }
                },
                y: {
                    beginAtZero: true,
                    title: { display: true, text: 'Amount (ml)' }
                }
            }
        }
    });
}

// ── Init ──────────────────────────────────────────────────────────────────────

loadPlants();
