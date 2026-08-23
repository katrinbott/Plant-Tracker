let plants = [];
let imagesByPlantId = {};

const WEATHER_CODES = {
    0: 'Clear sky', 1: 'Mainly clear', 2: 'Partly cloudy', 3: 'Overcast',
    45: 'Fog', 48: 'Icy fog',
    51: 'Light drizzle', 53: 'Drizzle', 55: 'Heavy drizzle',
    61: 'Light rain', 63: 'Rain', 65: 'Heavy rain',
    71: 'Light snow', 73: 'Snow', 75: 'Heavy snow',
    80: 'Light showers', 81: 'Showers', 82: 'Heavy showers',
    95: 'Thunderstorm',
};

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
        if (plants.length === 0) {
            list.innerHTML = '<p class="empty">No plants yet.</p>';
            return;
        }
        const imagesByPlant = await Promise.all(
            plants.map(p => apiFetch(`/plants/${p.id}/images`))
        );
        plants.forEach((p, i) => { imagesByPlantId[p.id] = imagesByPlant[i]; });
        list.innerHTML = plants.map((p, i) => {
            const images = imagesByPlant[i];
            const thumbnails = images.map(img => `
                <div style="position:relative; flex-shrink:0;">
                    <img src="/plants/${p.id}/images/${img.id}"
                         onclick="openLightbox(${p.id}, ${img.id})"
                         style="width:64px; height:64px; object-fit:cover; border-radius:6px; cursor:pointer;">
                    <button onclick="deletePlantImage(${p.id}, ${img.id})" title="Delete photo"
                        style="position:absolute; top:-6px; right:-6px; background:#c0392b; color:white;
                               border:none; border-radius:50%; width:18px; height:18px; font-size:10px;
                               cursor:pointer; line-height:18px; padding:0;">✕</button>
                </div>
            `).join('');
            return `
                <div class="card" style="flex-direction:column; align-items:stretch; gap:0.75rem;">
                    <div style="display:flex; justify-content:space-between; align-items:flex-start;">
                        <div class="card-info">
                            <strong>${p.name}</strong>
                            <span>${[p.species, p.location].filter(Boolean).join(' · ')}</span>
                        </div>
                        <div style="display:flex; gap:0.5rem; align-items:center;">
                            <label class="qr-btn" title="Upload photo" style="cursor:pointer;">
                                📷<input type="file" accept="image/*" style="display:none;"
                                    onchange="uploadPlantImage(${p.id}, this)">
                            </label>
                            <a href="/plants/${p.id}/qr" target="_blank" class="qr-btn" title="QR Code">QR</a>
                            <button class="delete-btn" onclick="deletePlant(${p.id})" title="Delete plant">✕</button>
                        </div>
                    </div>
                    ${images.length > 0 ? `
                        <div style="display:flex; gap:0.5rem; flex-wrap:wrap;">
                            ${thumbnails}
                        </div>` : ''}
                    <div id="upload-note-${p.id}" style="display:none;">
                        <input type="text" id="upload-note-input-${p.id}" placeholder="Optional note (e.g. chopped back due to pests)"
                               style="width:100%; margin-bottom:0.5rem; padding:0.4rem; border:1px solid #ccc; border-radius:4px; font-size:0.9rem;">
                        <div style="display:flex; gap:0.5rem;">
                            <button onclick="submitUpload(${p.id})"
                                style="padding:0.4rem 1rem; background:#2d6a2d; color:white; border:none; border-radius:4px; cursor:pointer; font-size:0.9rem;">Upload</button>
                            <button onclick="cancelUpload(${p.id})"
                                style="padding:0.4rem 1rem; background:#eee; color:#333; border:none; border-radius:4px; cursor:pointer; font-size:0.9rem;">Cancel</button>
                        </div>
                    </div>
                </div>
            `;
        }).join('');
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

const pendingUploads = {};

function uploadPlantImage(id, input) {
    const file = input.files[0];
    if (!file) return;
    pendingUploads[id] = file;
    const noteSection = document.getElementById(`upload-note-${id}`);
    if (noteSection) noteSection.style.display = 'block';
}

async function submitUpload(id) {
    const file = pendingUploads[id];
    if (!file) return;
    const noteInput = document.getElementById(`upload-note-input-${id}`);
    const note = noteInput ? noteInput.value.trim() : '';
    const formData = new FormData();
    formData.append('file', file);
    if (note) formData.append('note', note);
    await fetch(`/plants/${id}/images`, { method: 'POST', body: formData });
    delete pendingUploads[id];
    await loadPlants();
}

function cancelUpload(id) {
    delete pendingUploads[id];
    const noteSection = document.getElementById(`upload-note-${id}`);
    if (noteSection) noteSection.style.display = 'none';
}

async function deletePlantImage(plantId, imageId) {
    await apiFetch(`/plants/${plantId}/images/${imageId}`, { method: 'DELETE' });
    await loadPlants();
}

// ── Lightbox ──────────────────────────────────────────────────────────────────

let lightbox = { plantId: null, images: [], index: 0 };

function openLightbox(plantId, imageId) {
    const images = imagesByPlantId[plantId] ?? [];
    const index = images.findIndex(img => img.id === imageId);
    lightbox = { plantId, images, index: index >= 0 ? index : 0 };
    renderLightbox();
    document.getElementById('lightbox').style.display = 'flex';
    document.addEventListener('keydown', lightboxKeyHandler);
}

function closeLightbox() {
    document.getElementById('lightbox').style.display = 'none';
    document.removeEventListener('keydown', lightboxKeyHandler);
}

function lightboxKeyHandler(e) {
    if (e.key === 'ArrowLeft') lightboxPrev();
    else if (e.key === 'ArrowRight') lightboxNext();
    else if (e.key === 'Escape') closeLightbox();
}

function lightboxPrev() { if (lightbox.index > 0) { lightbox.index--; renderLightbox(); } }
function lightboxNext() { if (lightbox.index < lightbox.images.length - 1) { lightbox.index++; renderLightbox(); } }

function renderLightbox() {
    const img = lightbox.images[lightbox.index];
    document.getElementById('lightbox-img').src = `/plants/${lightbox.plantId}/images/${img.id}`;
    const counter = document.getElementById('lightbox-counter');
    counter.textContent = lightbox.images.length > 1 ? `${lightbox.index + 1} / ${lightbox.images.length}` : '';
    document.getElementById('lightbox-prev').style.visibility =
        lightbox.index > 0 ? 'visible' : 'hidden';
    document.getElementById('lightbox-next').style.visibility =
        lightbox.index < lightbox.images.length - 1 ? 'visible' : 'hidden';
}

// ── History slideshow ─────────────────────────────────────────────────────────

let slideshow = { plantId: null, images: [], index: 0 };

function renderSlideshow() {
    const container = document.getElementById('history-plant-image');
    if (slideshow.images.length === 0) { container.innerHTML = ''; return; }
    const img = slideshow.images[slideshow.index];
    container.innerHTML = `
        <div style="background:white; border-radius:8px; overflow:hidden;
                    box-shadow:0 1px 3px rgba(0,0,0,0.1); margin-bottom:1rem;">
            <div style="text-align:right; padding:0.4rem 0.75rem; font-size:0.8rem; color:#999; border-bottom:1px solid #f0f0f0;">
                ${new Date(img.createdAt).toLocaleString()}
            </div>
            <div style="position:relative;">
                <img src="/plants/${slideshow.plantId}/images/${img.id}"
                     style="width:100%; max-height:300px; object-fit:contain; display:block;">
                ${slideshow.images.length > 1 ? `
                    <button onclick="slideshowPrev()" style="position:absolute; left:0.5rem; top:50%;
                        transform:translateY(-50%); background:rgba(0,0,0,0.4); color:white; border:none;
                        border-radius:50%; width:2rem; height:2rem; font-size:1.2rem; cursor:pointer;
                        visibility:${slideshow.index > 0 ? 'visible' : 'hidden'};">‹</button>
                    <button onclick="slideshowNext()" style="position:absolute; right:0.5rem; top:50%;
                        transform:translateY(-50%); background:rgba(0,0,0,0.4); color:white; border:none;
                        border-radius:50%; width:2rem; height:2rem; font-size:1.2rem; cursor:pointer;
                        visibility:${slideshow.index < slideshow.images.length - 1 ? 'visible' : 'hidden'};">›</button>
                    <div style="position:absolute; bottom:0.5rem; right:0.75rem; color:white;
                                font-size:0.8rem; background:rgba(0,0,0,0.4); padding:0.1rem 0.4rem; border-radius:4px;">
                        ${slideshow.index + 1} / ${slideshow.images.length}
                    </div>
                ` : ''}
            </div>
            ${img.note ? `
            <div style="padding:0.5rem 0.75rem; border-top:1px solid #f0f0f0; font-size:0.9rem; color:#555;">
                ${img.note}
            </div>` : ''}
        </div>
    `;
}

function slideshowPrev() { if (slideshow.index > 0) { slideshow.index--; renderSlideshow(); } }
function slideshowNext() { if (slideshow.index < slideshow.images.length - 1) { slideshow.index++; renderSlideshow(); } }

// ── Watering ──────────────────────────────────────────────────────────────────

async function loadWateringEvents() {
    const list = document.getElementById('watering-list');
    try {
        const events = await apiFetch('/watering');
        events.sort((a, b) => new Date(b.wateredAt) - new Date(a.wateredAt)); // newest first
        list.innerHTML = events.length === 0
            ? '<p class="empty">No watering events yet.</p>'
            : events.map(e => {
                const plant = plants.find(p => p.id === e.plantId);
                const date = new Date(e.wateredAt).toLocaleString();
                const weather = e.minTemperatureC != null
                    ? `${e.minTemperatureC}°C / ${e.maxTemperatureC}°C  · ${WEATHER_CODES[e.weatherCode] ?? 'Unknown'}`
                    : null;
                const details = [e.amountMl ? `${e.amountMl} ml` : null, e.note, weather].filter(Boolean).join(' · ');
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
let temperatureChart = null;

function populateHistoryDropdown() {
    const select = document.getElementById('history-plant');
    select.innerHTML = plants.length === 0
        ? '<option value="">No plants available</option>'
        : plants.map(p => `<option value="${p.id}">${p.name}</option>`).join('');
}

async function loadHistory() {
    const list = document.getElementById('history-list');
    const chartContainer = document.getElementById('history-chart-container');
    const tempChartContainer = document.getElementById('temperature-chart-container');
    const statsContainer = document.getElementById('history-stats');
    const plantId = document.getElementById('history-plant').value;
    if (!plantId) return;

    const plant = plants.find(p => p.id == plantId);
    const imageContainer = document.getElementById('history-plant-image');
    const images = plant ? await apiFetch(`/plants/${plant.id}/images`) : [];
    slideshow = { plantId: plant?.id ?? null, images, index: 0 };
    renderSlideshow();
    try {
        const [events, analytics] = await Promise.all([
            apiFetch(`/watering/plant/${plantId}`),
            apiFetch(`/analytics/plant/${plantId}`)
        ]);

        if (events.length === 0) {
            chartContainer.style.display = 'none';
            tempChartContainer.style.display = 'none';
            statsContainer.style.display = 'none';
            list.innerHTML = '<p class="empty">No watering events recorded yet.</p>';
            return;
        }

        renderStats(analytics, statsContainer);
        chartContainer.style.display = 'block';
        renderHistoryChart(events);

        const hasTemperatureData = events.some(e => e.temperatureC != null);
//        tempChartContainer.style.display = hasTemperatureData ? 'block' : 'none';
        tempChartContainer.style.display = 'block';
        if (hasTemperatureData) renderTemperatureChart(events);

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

function renderTemperatureChart(events) {
    const sorted = [...events].reverse();

    if (temperatureChart) {
        temperatureChart.destroy();
    }

    const ctx = document.getElementById('temperature-chart').getContext('2d');
    temperatureChart = new Chart(ctx, {
        type: 'line',
        data: {
            datasets: [
                {
                    label: 'Min °C',
                    data: sorted.map(e => ({ x: e.wateredAt, y: e.minTemperatureC ?? null })),
                    borderColor: '#2980b9',
                    backgroundColor: '#2980b9',
                    pointRadius: 3,
                    pointHoverRadius: 7,
                    tension: 0,
                    borderDash: [7,7],
                    borderWidth: 1,
                },
                {
                    label: 'Max °C',
                    data: sorted.map(e => ({ x: e.wateredAt, y: e.maxTemperatureC ?? null })),
                    borderColor: '#c0392b',
                    backgroundColor: '#c0392b',
                    pointRadius: 3,
                    pointHoverRadius: 7,
                    tension: 0,
                    borderDash: [7,7],
                    borderWidth: 1,
                },
                {
                    label: 'Current °C',
                    data: sorted.map(e => ({ x: e.wateredAt, y: e.temperatureC ?? null })),
                    borderColor: '#2d6a2d',
                    backgroundColor: '#2d6a2d',
                    pointRadius: 5,
                    pointHoverRadius: 7,
                    tension: 0,
                },
            ]
        },
        options: {
            responsive: true,
            plugins: {
                legend: { display: true },
            },
            scales: {
                x: {
                    type: 'time',
                    time: { unit: 'day', displayFormats: { day: 'MMM d' } },
                    title: { display: true, text: 'Date' }
                },
                y: {
                    title: { display: true, text: 'Temperature (°C)' }
                }
            }
        }
    });
}

// ── Init ──────────────────────────────────────────────────────────────────────

loadPlants();
