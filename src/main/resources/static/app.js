let plants = [];
let rooms = [];
let imagesByPlantId = {};

const WEATHER_EMOJIS = {
    0: '☀️', 1: '🌤️', 2: '⛅', 3: '☁️',
    45: '🌫️', 48: '🌫️',
    51: '🌦️', 53: '🌦️', 55: '🌧️',
    61: '🌧️', 63: '🌧️', 65: '🌧️',
    71: '🌨️', 73: '🌨️', 75: '❄️',
    80: '🌦️', 81: '🌧️', 82: '⛈️',
    95: '⛈️',
};

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
        renderRoomPlantChecklist();
        loadWateringEvents();
        loadLocation();
    }
    if (name === 'rooms') {
        loadRoomStats();
    }
    if (name === 'analytics') {
        populateAnalyticsDropdown();
        loadAnalytics();
    }
}

// ── Rooms ─────────────────────────────────────────────────────────────────────

const NEW_ROOM = '__new__';

function roomName(roomId) {
    return rooms.find(r => r.id === roomId)?.name ?? null;
}

async function loadRooms() {
    try {
        rooms = await apiFetch('/rooms');
    } catch (e) {
        rooms = [];
    }
    renderRoomOptions();
}

// Each room dropdown has its own inline "create a room" row, keyed by the select's id.
const ROOM_PICKERS = {
    'plant-room': { row: 'new-room-row', name: 'new-room-name', error: 'new-room-error' },
    'plant-room-input': { row: 'analytics-new-room-row', name: 'analytics-new-room-name', error: 'analytics-new-room-error' },
};

// Every plant belongs to a room, so there is no "no room" choice — only a prompt
// when none is picked yet, the real rooms, and the inline create option.
function renderRoomOptions() {
    const options = '<option value="">Select a room…</option>'
        + rooms.map(r => `<option value="${r.id}">${escapeHtml(r.name)}</option>`).join('')
        + `<option value="${NEW_ROOM}">+ New room…</option>`;

    Object.keys(ROOM_PICKERS).forEach(selectId => {
        const select = document.getElementById(selectId);
        if (!select) return;
        const previous = select.value;
        select.innerHTML = options;
        if (previous && previous !== NEW_ROOM) select.value = previous;
    });

    // The room-watering picker chooses an existing room to water, so it offers no
    // "create one" option.
    const wateringSelect = document.getElementById('room-watering-room');
    if (wateringSelect) {
        const previous = wateringSelect.value;
        wateringSelect.innerHTML = '<option value="">Select a room…</option>'
            + rooms.map(r => `<option value="${r.id}">${escapeHtml(r.name)}</option>`).join('');
        if (previous) wateringSelect.value = previous;
    }
}

function setRoomSelection(selectId, value) {
    const select = document.getElementById(selectId);
    select.value = value ?? '';
    // Remembered so cancelling "+ New room…" can put the previous choice back
    // instead of silently leaving the plant with no room.
    select.dataset.lastRoom = select.value;
}

function onRoomSelectChange(select) {
    const picker = ROOM_PICKERS[select.id];
    const creating = select.value === NEW_ROOM;
    if (!creating) select.dataset.lastRoom = select.value;
    document.getElementById(picker.row).style.display = creating ? 'flex' : 'none';
    document.getElementById(picker.error).textContent = '';
    if (creating) document.getElementById(picker.name).focus();
}

function hideNewRoomRow(selectId) {
    const picker = ROOM_PICKERS[selectId];
    document.getElementById(picker.row).style.display = 'none';
    document.getElementById(picker.name).value = '';
    document.getElementById(picker.error).textContent = '';
}

function cancelNewRoom(selectId) {
    const select = document.getElementById(selectId);
    select.value = select.dataset.lastRoom ?? '';
    hideNewRoomRow(selectId);
}

// Raw fetch rather than apiFetch: a 409 means the room already exists, which is
// worth handling as "select that one" instead of surfacing an error.
async function createRoomInline(selectId) {
    const picker = ROOM_PICKERS[selectId];
    const nameInput = document.getElementById(picker.name);
    const errorEl = document.getElementById(picker.error);
    const name = nameInput.value.trim();
    errorEl.textContent = '';
    if (!name) {
        errorEl.textContent = 'Enter a room name.';
        return;
    }
    try {
        const res = await fetch('/rooms', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name })
        });
        if (res.status === 409) {
            await loadRooms();
            const existing = rooms.find(r => r.name.toLowerCase() === name.toLowerCase());
            if (existing) selectCreatedRoom(selectId, existing.id);
            errorEl.textContent = 'That room already exists, so it is now selected.';
            return;
        }
        if (!res.ok) {
            errorEl.textContent = 'Failed to create room.';
            return;
        }
        const created = await res.json();
        await loadRooms();
        selectCreatedRoom(selectId, created.id);
    } catch (e) {
        errorEl.textContent = 'Failed to create room.';
    }
}

function selectCreatedRoom(selectId, roomId) {
    const picker = ROOM_PICKERS[selectId];
    setRoomSelection(selectId, roomId);
    document.getElementById(picker.row).style.display = 'none';
    document.getElementById(picker.name).value = '';
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function describeRoomRecency(stats) {
    if (stats.plantCount === 0) return 'no plants yet';
    if (stats.daysSinceLastWatering === null) return 'never watered';
    if (stats.daysSinceLastWatering === 0) return 'watered today';
    if (stats.daysSinceLastWatering === 1) return 'watered yesterday';
    return `watered ${stats.daysSinceLastWatering} days ago`;
}

// The room cards list their plants from the cached /plants response rather than
// fetching per room, the same way the room-watering checklist does.
function roomPlantRows(roomId) {
    const roomPlants = plants.filter(p => p.roomId === roomId);
    if (roomPlants.length === 0) return '<p class="empty" style="margin:0;">No plants in this room yet.</p>';
    const elsewhere = rooms.filter(r => r.id !== roomId);
    return roomPlants.map(p => `
        <div style="display:flex; justify-content:space-between; align-items:center; gap:0.5rem; font-size:0.95rem; color:#333;">
            <span>${escapeHtml(p.name)}${p.location ? ` <span style="color:#888; font-size:0.85rem;">· ${escapeHtml(p.location)}</span>` : ''}</span>
            ${elsewhere.length === 0 ? '' : `
                <select onchange="movePlantToRoom(${p.id}, ${roomId}, this)"
                        style="width:auto; margin-top:0; padding:0.25rem; font-size:0.85rem;">
                    <option value="">Move to…</option>
                    ${elsewhere.map(r => `<option value="${r.id}">${escapeHtml(r.name)}</option>`).join('')}
                </select>`}
        </div>
    `).join('');
}

async function loadRoomStats() {
    const list = document.getElementById('rooms-list');
    try {
        const stats = await apiFetch('/rooms/stats');
        if (stats.length === 0) {
            list.innerHTML = '<p class="empty">No rooms yet.</p>';
            return;
        }
        list.innerHTML = stats.map(s => {
            const summary = [
                s.plantCount === 1 ? '1 plant' : `${s.plantCount} plants`,
                describeRoomRecency(s),
                s.averageDaysBetweenWaterings !== null
                    ? `avg every ${s.averageDaysBetweenWaterings.toFixed(1)} days` : null,
            ].filter(Boolean).join(' · ');
            const overdue = s.overduePlants.length === 0 ? '' : `
                <div style="font-size:0.85rem; color:#c0392b;">
                    Check: ${s.overduePlants.map(p =>
                        `${escapeHtml(p.name)} (${p.daysSinceLastWatering} days, usually every ${p.medianDaysBetweenWaterings.toFixed(1)})`
                    ).join(', ')}
                </div>`;
            return `
                <div class="card" style="flex-direction:column; align-items:stretch; gap:0.5rem;">
                    <div style="display:flex; justify-content:space-between; align-items:flex-start;">
                        <div class="card-info">
                            <strong>${escapeHtml(s.name)}</strong>
                            <span>${summary}</span>
                        </div>
                        <div style="display:flex; gap:0.5rem; align-items:center;">
                            <button type="button" class="qr-btn" onclick="toggleRoomRename(${s.roomId})">Rename</button>
                            <button class="delete-btn" onclick="deleteRoom(${s.roomId})" title="Delete room">✕</button>
                        </div>
                    </div>
                    ${overdue}
                    <div style="display:flex; flex-direction:column; gap:0.35rem;">
                        ${roomPlantRows(s.roomId)}
                    </div>
                    <div id="room-rename-${s.roomId}" style="display:none; gap:0.5rem;">
                        <input type="text" id="room-rename-input-${s.roomId}" value="${escapeHtml(s.name)}"
                               style="flex:1; margin-top:0; padding:0.4rem; font-size:0.9rem;"
                               onkeydown="if(event.key==='Enter'){event.preventDefault();saveRoomName(${s.roomId});}">
                        <button type="button" onclick="saveRoomName(${s.roomId})"
                            style="padding:0.4rem 1rem; background:#2d6a2d; color:white; border:none; border-radius:4px; cursor:pointer; font-size:0.9rem;">Save</button>
                    </div>
                    <div class="error" id="room-action-error-${s.roomId}"></div>
                </div>
            `;
        }).join('');
    } catch (e) {
        list.innerHTML = '<p class="error">Failed to load rooms.</p>';
    }
}

async function addRoom(event) {
    event.preventDefault();
    const error = document.getElementById('room-error');
    error.textContent = '';
    const name = document.getElementById('room-name').value.trim();
    const res = await fetch('/rooms', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name })
    });
    if (res.status === 409) {
        error.textContent = 'A room with that name already exists.';
        return;
    }
    if (!res.ok) {
        error.textContent = 'Failed to add room.';
        return;
    }
    event.target.reset();
    await loadRooms();
    await loadRoomStats();
}

function toggleRoomRename(roomId) {
    const row = document.getElementById(`room-rename-${roomId}`);
    const open = row.style.display === 'none';
    row.style.display = open ? 'flex' : 'none';
    document.getElementById(`room-action-error-${roomId}`).textContent = '';
    if (open) document.getElementById(`room-rename-input-${roomId}`).focus();
}

async function saveRoomName(roomId) {
    const error = document.getElementById(`room-action-error-${roomId}`);
    error.textContent = '';
    const name = document.getElementById(`room-rename-input-${roomId}`).value.trim();
    if (!name) {
        error.textContent = 'Enter a room name.';
        return;
    }
    const res = await fetch(`/rooms/${roomId}`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name })
    });
    if (res.status === 409) {
        error.textContent = 'Another room already has that name.';
        return;
    }
    if (!res.ok) {
        error.textContent = 'Failed to rename room.';
        return;
    }
    await loadRooms();
    await loadPlants();
    await loadRoomStats();
}

async function deleteRoom(roomId) {
    const error = document.getElementById(`room-action-error-${roomId}`);
    error.textContent = '';
    const res = await fetch(`/rooms/${roomId}`, { method: 'DELETE' });
    // A plant always belongs to a room, so a room still holding plants cannot go.
    if (res.status === 409) {
        error.textContent = 'This room still has plants. Move them to another room first.';
        return;
    }
    if (!res.ok) {
        error.textContent = 'Failed to delete room.';
        return;
    }
    await loadRooms();
    await loadRoomStats();
}

async function movePlantToRoom(plantId, fromRoomId, select) {
    const error = document.getElementById(`room-action-error-${fromRoomId}`);
    error.textContent = '';
    if (!select.value) return;
    try {
        await apiFetch(`/plants/${plantId}/room`, {
            method: 'PUT',
            body: JSON.stringify({ roomId: parseInt(select.value) })
        });
    } catch (e) {
        error.textContent = 'Failed to move the plant.';
        select.value = '';
        return;
    }
    // Both rooms' counts and stats change, so the whole tab is reloaded.
    await loadPlants();
    await loadRoomStats();
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
                            <strong onclick="openPlantAnalytics(${p.id})" title="Show this plant's analytics"
                                    style="cursor:pointer;">${p.name}</strong>
                            <span>${[p.species, escapeHtml(roomName(p.roomId) ?? ''), p.location].filter(Boolean).join(' · ')}</span>
                        </div>
                        <div style="display:flex; gap:0.5rem; align-items:center;">
                            <label class="qr-btn camera-btn" title="Upload photo" style="cursor:pointer;">
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

// Returns null when nothing real is picked, which blocks the submit.
function selectedNewRoomId() {
    const value = document.getElementById('plant-room').value;
    return value && value !== NEW_ROOM ? parseInt(value) : null;
}

async function addPlant(event) {
    event.preventDefault();
    const error = document.getElementById('plant-error');
    error.textContent = '';
    const roomId = selectedNewRoomId();
    if (roomId === null) {
        error.textContent = 'Pick a room, or create one first.';
        return;
    }
    const body = {
        name: document.getElementById('plant-name').value,
        species: document.getElementById('plant-species').value || null,
        location: document.getElementById('plant-location').value || null,
        roomId,
    };
    try {
        await apiFetch('/plants', { method: 'POST', body: JSON.stringify(body) });
        event.target.reset();
        hideNewRoomRow('plant-room');
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

// ── Analytics slideshow ───────────────────────────────────────────────────────

let slideshow = { plantId: null, images: [], index: 0 };

function renderSlideshow() {
    const container = document.getElementById('analytics-plant-image');
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
                const details = [e.amountMl ? `${e.amountMl} ml` : null, e.fertilizer ? 'fertilized' : null, e.note, weather].filter(Boolean).join(' · ');
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

// ── Room watering ─────────────────────────────────────────────────────────────

function renderRoomPlantChecklist() {
    const roomId = document.getElementById('room-watering-room').value;
    const container = document.getElementById('room-watering-plants');
    document.getElementById('room-watering-error').textContent = '';
    if (!roomId) {
        container.innerHTML = '';
        updateRoomWateringButton();
        return;
    }
    const roomPlants = plants.filter(p => p.roomId == roomId);
    container.innerHTML = roomPlants.length === 0
        ? '<p class="empty">No plants in this room yet.</p>'
        : roomPlants.map(p => `
            <label style="display:flex; align-items:center; gap:0.5rem; margin-bottom:0.25rem; font-size:0.95rem; color:#333;">
                <input type="checkbox" class="room-watering-plant" value="${p.id}" checked
                       onchange="updateRoomWateringButton()"
                       style="display:inline-block; width:auto; margin-top:0;">
                ${escapeHtml(p.name)}${p.location ? ` <span style="color:#888; font-size:0.85rem;">· ${escapeHtml(p.location)}</span>` : ''}
            </label>
        `).join('');
    updateRoomWateringButton();
}

function checkedRoomPlantIds() {
    return [...document.querySelectorAll('.room-watering-plant:checked')]
        .map(box => parseInt(box.value));
}

function updateRoomWateringButton() {
    const count = checkedRoomPlantIds().length;
    const button = document.getElementById('room-watering-btn');
    button.textContent = count === 1 ? 'Water 1 plant' : `Water ${count} plants`;
    button.disabled = count === 0;
    button.style.opacity = count === 0 ? '0.5' : '1';
    button.style.cursor = count === 0 ? 'default' : 'pointer';
}

async function recordRoomWatering() {
    const error = document.getElementById('room-watering-error');
    error.textContent = '';
    const plantIds = checkedRoomPlantIds();
    if (plantIds.length === 0) {
        error.textContent = 'Tick at least one plant.';
        return;
    }
    try {
        await apiFetch('/watering/bulk', {
            method: 'POST',
            body: JSON.stringify({
                plantIds,
                note: document.getElementById('room-watering-note').value || null,
                fertilizer: document.getElementById('room-watering-fertilizer').checked,
            })
        });
        document.getElementById('room-watering-note').value = '';
        document.getElementById('room-watering-fertilizer').checked = false;
        renderRoomPlantChecklist();
        await loadWateringEvents();
    } catch (e) {
        error.textContent = 'Failed to record the watering round.';
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
        fertilizer: document.getElementById('watering-fertilizer').checked,
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

async function fetchWeatherEmoji(lat, lon) {
    try {
        const res = await fetch(`https://api.open-meteo.com/v1/forecast?latitude=${lat}&longitude=${lon}&current=weather_code&timezone=auto&forecast_days=1`);
        const data = await res.json();
        const code = data?.current?.weather_code;
        return WEATHER_EMOJIS[code] ?? '🌤️';
    } catch {
        return '🌤️';
    }
}

async function reverseGeocode(lat, lon) {
    try {
        const res = await fetch(`https://nominatim.openstreetmap.org/reverse?lat=${lat}&lon=${lon}&format=json`);
        if (!res.ok) return null;
        const data = await res.json();
        const a = data.address;
        return [a.city || a.town || a.village || a.county, a.country].filter(Boolean).join(', ');
    } catch {
        return null;
    }
}

async function loadLocation() {
    const data = await apiFetch('/settings/location');
    document.getElementById('location-lat').value = data.latitude;
    document.getElementById('location-lon').value = data.longitude;
    const [name, emoji] = await Promise.all([
        reverseGeocode(data.latitude, data.longitude),
        fetchWeatherEmoji(data.latitude, data.longitude),
    ]);
    document.getElementById('location-display').textContent =
        name ? `${name} (${data.latitude}, ${data.longitude})` : `${data.latitude}, ${data.longitude}`;
    document.getElementById('weather-icon').textContent = emoji;
    const btn = document.getElementById('location-edit-btn');
    if (data.confirmed) {
        btn.textContent = 'Edit';
        btn.onclick = toggleLocationEdit;
    } else {
        btn.textContent = 'Confirm location';
        btn.onclick = confirmLocation;
    }
}

async function confirmLocation() {
    const lat = parseFloat(document.getElementById('location-lat').value);
    const lon = parseFloat(document.getElementById('location-lon').value);
    await apiFetch('/settings/location', {
        method: 'POST',
        body: JSON.stringify({ latitude: lat, longitude: lon })
    });
    document.getElementById('location-edit-btn').textContent = 'Edit';
    document.getElementById('location-edit-btn').onclick = toggleLocationEdit;
}

let locationMap = null;
let locationMarker = null;

function toggleLocationEdit() {
    const form = document.getElementById('location-form');
    const btn = document.getElementById('location-edit-btn');
    const visible = form.style.display !== 'none';
    form.style.display = visible ? 'none' : 'block';
    btn.textContent = visible ? 'Edit' : 'Cancel';
    if (!visible) {
        const lat = parseFloat(document.getElementById('location-lat').value);
        const lon = parseFloat(document.getElementById('location-lon').value);
        initLocationMap(lat, lon);
    }
}

function initLocationMap(lat, lon) {
    if (locationMap) {
        locationMap.setView([lat, lon], 10);
        locationMarker.setLatLng([lat, lon]);
        locationMap.invalidateSize();
        return;
    }
    locationMap = L.map('location-map').setView([lat, lon], 10);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
    }).addTo(locationMap);
    locationMarker = L.marker([lat, lon], { draggable: true }).addTo(locationMap);
    locationMarker.on('dragend', () => {
        const pos = locationMarker.getLatLng();
        document.getElementById('location-lat').value = pos.lat.toFixed(5);
        document.getElementById('location-lon').value = pos.lng.toFixed(5);
    });
    locationMap.on('click', (e) => {
        locationMarker.setLatLng(e.latlng);
        document.getElementById('location-lat').value = e.latlng.lat.toFixed(5);
        document.getElementById('location-lon').value = e.latlng.lng.toFixed(5);
    });
}

async function searchLocation() {
    const query = document.getElementById('location-search').value.trim();
    const errorEl = document.getElementById('location-search-error');
    if (!query) return;
    errorEl.style.display = 'none';
    try {
        const res = await fetch(`https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(query)}&format=json&limit=1`);
        const results = await res.json();
        if (!results.length) { errorEl.style.display = 'block'; return; }
        const lat = parseFloat(results[0].lat);
        const lon = parseFloat(results[0].lon);
        document.getElementById('location-lat').value = lat.toFixed(5);
        document.getElementById('location-lon').value = lon.toFixed(5);
        if (locationMap && locationMarker) {
            locationMarker.setLatLng([lat, lon]);
            locationMap.setView([lat, lon], 10);
        }
    } catch {
        errorEl.style.display = 'block';
    }
}

async function saveLocation() {
    const lat = parseFloat(document.getElementById('location-lat').value);
    const lon = parseFloat(document.getElementById('location-lon').value);
    if (isNaN(lat) || isNaN(lon)) return;
    const data = await apiFetch('/settings/location', {
        method: 'POST',
        body: JSON.stringify({ latitude: lat, longitude: lon })
    });
    const name = await reverseGeocode(data.latitude, data.longitude);
    document.getElementById('location-display').textContent =
        name ? `${name} (${data.latitude}, ${data.longitude})` : `${data.latitude}, ${data.longitude}`;
    toggleLocationEdit();
}

function loadStored(key) {
    try { return localStorage.getItem(key); } catch { return null; }
}

function store(key, value) {
    try { localStorage.setItem(key, value); } catch { /* storage unavailable, keep in-memory only */ }
}

let selectedWateringPlantId = loadStored('wateringPlantId');
let selectedAnalyticsPlantId = loadStored('analyticsPlantId');

function selectWateringPlant(id) {
    selectedWateringPlantId = id;
    store('wateringPlantId', id);
}

// Sets the selection before switching tabs: showTab repopulates the dropdown, which picks
// up selectedAnalyticsPlantId itself and then loads. Calling selectAnalyticsPlant here
// instead would fetch the analytics twice.
function openPlantAnalytics(plantId) {
    selectedAnalyticsPlantId = plantId;
    store('analyticsPlantId', plantId);
    showTab('analytics');
}

function selectAnalyticsPlant(id) {
    selectedAnalyticsPlantId = id;
    store('analyticsPlantId', id);
    loadAnalytics();
}

function populatePlantDropdown() {
    const select = document.getElementById('watering-plant');
    select.innerHTML = plants.length === 0
        ? '<option value="">No plants available</option>'
        : plants.map(p => `<option value="${p.id}">${p.name}</option>`).join('');
    if (plants.some(p => p.id == selectedWateringPlantId)) select.value = selectedWateringPlantId;
}

// ── Analytics ─────────────────────────────────────────────────────────────────

let historyChart = null;
let temperatureChart = null;

function populateAnalyticsDropdown() {
    const select = document.getElementById('analytics-plant');
    select.innerHTML = plants.length === 0
        ? '<option value="">No plants available</option>'
        : plants.map(p => `<option value="${p.id}">${p.name}</option>`).join('');
    if (plants.some(p => p.id == selectedAnalyticsPlantId)) select.value = selectedAnalyticsPlantId;
}

async function loadAnalytics() {
    const list = document.getElementById('history-list');
    const chartContainer = document.getElementById('history-chart-container');
    const tempChartContainer = document.getElementById('temperature-chart-container');
    const statsContainer = document.getElementById('analytics-stats');
    const plantId = document.getElementById('analytics-plant').value;
    if (!plantId) return;

    const plant = plants.find(p => p.id == plantId);
    renderPlantLocation(plant);
    const imageContainer = document.getElementById('analytics-plant-image');
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
            document.getElementById('analytics-overdue').style.display = 'none';
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
            const details = [e.amountMl ? `${e.amountMl} ml` : null, e.fertilizer ? 'fertilized' : null, e.note].filter(Boolean).join(' · ');
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
        list.innerHTML = '<p class="error">Failed to load analytics.</p>';
    }
}

function renderPlantLocation(plant) {
    const card = document.getElementById('plant-location-card');
    if (!plant) { card.style.display = 'none'; return; }
    card.style.display = 'block';
    document.getElementById('plant-location-display').textContent =
        [roomName(plant.roomId), plant.location].filter(Boolean).join(' · ');
    document.getElementById('plant-location-form').style.display = 'none';
    document.getElementById('plant-location-edit-btn').textContent = 'Edit';
}

function togglePlantLocationEdit() {
    const form = document.getElementById('plant-location-form');
    const editing = form.style.display === 'none';
    form.style.display = editing ? 'block' : 'none';
    document.getElementById('plant-location-edit-btn').textContent = editing ? 'Cancel' : 'Edit';
    if (editing) {
        const plant = plants.find(p => p.id == document.getElementById('analytics-plant').value);
        const input = document.getElementById('plant-location-input');
        setRoomSelection('plant-room-input', plant?.roomId);
        document.getElementById('analytics-new-room-row').style.display = 'none';
        document.getElementById('analytics-new-room-error').textContent = '';
        input.value = plant?.location ?? '';
        input.focus();
    }
}

async function savePlantLocation() {
    const plantId = document.getElementById('analytics-plant').value;
    const location = document.getElementById('plant-location-input').value;
    const roomValue = document.getElementById('plant-room-input').value;
    if (!roomValue || roomValue === NEW_ROOM) {
        document.getElementById('analytics-new-room-error').textContent =
            roomValue === NEW_ROOM ? 'Create the new room first, or cancel.' : 'Pick a room.';
        return;
    }
    try {
        await apiFetch(`/plants/${plantId}`, {
            method: 'PATCH',
            body: JSON.stringify({ location })
        });
        const updated = await apiFetch(`/plants/${plantId}/room`, {
            method: 'PUT',
            body: JSON.stringify({ roomId: parseInt(roomValue) })
        });
        await loadPlants();
        renderPlantLocation(updated);
    } catch (e) {
        alert('Failed to update room or spot.');
    }
}

// Hedged on purpose: the median is a rhythm, not a rule, and a plant may genuinely not
// need water yet. A median exists whenever overdue is set, so it is safe to read here.
function renderOverdueNotice(analytics) {
    const notice = document.getElementById('analytics-overdue');
    notice.style.display = analytics.overdue ? 'block' : 'none';
    if (analytics.overdue) {
        notice.textContent = `Check if watering is necessary — last watered `
            + `${analytics.daysSinceLastWatering} days ago, usually every `
            + `${analytics.medianDaysBetweenWaterings.toFixed(1)}.`;
    }
}

function renderStats(analytics, container) {
    const stats = [
        { label: 'Total waterings', value: analytics.totalWaterings },
        // Marked on the days since, not the median: the median is the plant's normal
        // rhythm, and it is the time since watering that is out of range.
        { label: 'Days since last watering', value: analytics.daysSinceLastWatering ?? '—', overdue: analytics.overdue },
        { label: 'Median days between waterings', value: analytics.medianDaysBetweenWaterings != null ? analytics.medianDaysBetweenWaterings.toFixed(1) : '—' },
        { label: 'Avg. days between waterings', value: analytics.averageDaysBetweenWaterings != null ? analytics.averageDaysBetweenWaterings.toFixed(1) : '—' },
    ];
    renderOverdueNotice(analytics);
    container.style.display = 'flex';
    container.style.flexWrap = 'wrap';
    container.innerHTML = stats.map(s => `
        <div style="flex:1; background:white; padding:1rem; border-radius:8px; box-shadow:0 1px 3px rgba(0,0,0,0.1); text-align:center;">
            <div style="font-size:1.5rem; font-weight:bold; color:${s.overdue ? '#c0392b' : '#2d6a2d'};">${s.value}</div>
            <div style="font-size:0.8rem; color:#666; margin-top:0.25rem;">${s.label}</div>
        </div>
    `).join('');
}

// One chart, one y scale. Every watering sits in a gutter below zero, where the axis
// deliberately shows no value, and the amounts that were recorded sit above the zero line
// on the ml scale. A watering with an amount therefore appears twice, which is the point:
// a gutter mark with nothing above it is a watering whose volume was never recorded.
// Points, never a line, since joining amounts across waterings that have none would draw a
// slope between numbers that are not a series.
function renderHistoryChart(events) {
    const sorted = [...events].reverse();
    const amounts = sorted
        .filter(e => e.amountMl != null)
        .map(e => ({ x: e.wateredAt, y: e.amountMl, fertilizer: e.fertilizer, note: e.note }));

    // Sized from the data so the gutter stays a thin strip whatever the volumes are, and
    // has a sane height when no amount was ever recorded.
    const top = Math.max(0, ...amounts.map(p => p.y)) || 100;
    const gutter = top * 0.18;

    const waterings = sorted.map(e => ({
        x: e.wateredAt,
        y: -gutter / 2,
        fertilizer: e.fertilizer,
        note: e.note,
        amountMl: e.amountMl,
    }));

    if (historyChart) historyChart.destroy();
    historyChart = new Chart(document.getElementById('history-chart').getContext('2d'), {
        type: 'scatter',
        data: {
            datasets: [
                {
                    label: 'Amount recorded',
                    data: amounts,
                    borderColor: '#2d6a2d',
                    backgroundColor: '#2d6a2d',
                    pointRadius: 5,
                    pointHoverRadius: 7,
                    pointStyle: amounts.map(p => p.fertilizer ? 'triangle' : 'circle'),
                },
                {
                    // Hollow rather than a second colour: shape already carries fertilizer,
                    // and one hue keeps the two rows readable without relying on colour.
                    label: 'Watering',
                    data: waterings,
                    borderColor: '#2d6a2d',
                    backgroundColor: 'transparent',
                    borderWidth: 2,
                    pointRadius: 5,
                    pointHoverRadius: 7,
                    pointStyle: waterings.map(p => p.fertilizer ? 'triangle' : 'circle'),
                },
            ]
        },
        options: {
            responsive: true,
            plugins: {
                // Boxes rather than point styles: pointStyle is a per-point array here, which
                // the legend cannot draw. Filled versus hollow distinguishes the two rows.
                legend: { display: true, position: 'top' },
                subtitle: {
                    display: sorted.some(e => e.fertilizer),
                    position: 'bottom',
                    text: 'Triangle: watered with fertilizer',
                },
                tooltip: {
                    callbacks: {
                        // The gutter row has no value on the axis, so it must not read as one.
                        label: (item) => item.datasetIndex === 1
                            ? (item.raw.amountMl != null ? 'Watered' : 'Watered, no amount recorded')
                            : `${item.raw.y} ml`,
                        footer: (items) => items[0].raw.note ? `Note: ${items[0].raw.note}` : '',
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
                    min: -gutter,
                    suggestedMax: top,
                    title: { display: true, text: 'Amount (ml)' },
                    // The gutter is unlabelled, and zero is drawn as the line dividing the two.
                    ticks: { callback: (value) => value < 0 ? '' : value },
                    grid: {
                        color: (c) => c.tick.value === 0 ? '#9aa89a' : 'rgba(0,0,0,0.1)',
                        lineWidth: (c) => c.tick.value === 0 ? 2 : 1,
                    },
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

// Rooms first: the plant cards resolve room names from the cached list.
loadRooms().then(loadPlants);
