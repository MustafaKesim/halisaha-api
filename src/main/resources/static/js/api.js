const TOKEN_KEY = 'halisaha_token';
const USER_KEY = 'halisaha_user';

function saveSession(token, user) {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
}

function getToken() {
    return localStorage.getItem(TOKEN_KEY);
}

function getCurrentUser() {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
}

function clearSession() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
}

function logout() {
    clearSession();
    window.location.href = '/login.html';
}

function hasValidSession() {
    const token = getToken();
    if (!token) return false;
    try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        if (!payload.exp) return true;
        return Date.now() < payload.exp * 1000;
    } catch (e) {
        return false;
    }
}

function requireAuth() {
    if (!hasValidSession()) {
        clearSession();
        window.location.href = '/login.html';
    }
}

async function apiFetch(path, options = {}) {
    const headers = Object.assign({ 'Content-Type': 'application/json' }, options.headers || {});
    const token = getToken();
    if (token) {
        headers['Authorization'] = 'Bearer ' + token;
    }

    const response = await fetch(path, Object.assign({}, options, { headers }));

    const text = await response.text();
    let body = null;
    if (text) {
        try {
            body = JSON.parse(text);
        } catch (e) {
            body = null;
        }
    }

    if (!response.ok) {
        // Onceden bir token vardi ama artik gecersiz/suresi dolmussa (login denemesinde token hic yoktu,
        // o durumu burada YAKALAMIYORUZ - asil hata mesaji asagida normal sekilde gosterilir).
        if ((response.status === 401 || response.status === 403) && token && !hasValidSession()) {
            clearSession();
            window.location.href = '/login.html';
            return new Promise(() => {});
        }

        const message = (body && body.message) ? body.message : ('Bir hata oluştu (' + response.status + ')');
        throw new Error(message);
    }

    return body;
}

const api = {
    get: (path) => apiFetch(path, { method: 'GET' }),
    post: (path, data) => apiFetch(path, { method: 'POST', body: JSON.stringify(data) }),
};

function renderNavbar(activePage) {
    const user = getCurrentUser();
    const links = [
        { href: '/venues.html', label: 'Sahalar', key: 'venues' },
        { href: '/reservations.html', label: 'Maçlarım', key: 'reservations' },
        { href: '/open-matches.html', label: 'Eksik Oyuncu', key: 'open-matches' },
        { href: '/open-challenges.html', label: 'Rakip Bul', key: 'open-challenges' },
        { href: '/teams.html', label: 'Takımlarım', key: 'teams' },
    ];

    const linksHtml = links.map(link =>
        `<a href="${link.href}" class="${link.key === activePage ? 'active' : ''}">${link.label}</a>`
    ).join('');

    const nameLabel = user && user.fullName ? user.fullName : '';

    return `
    <nav class="navbar">
        <div class="navbar-inner">
            <a href="/venues.html" class="brand"><span class="ball">⚽</span> Yeşil Vadi Halı Saha</a>
            <div class="nav-links">${linksHtml}</div>
            <div class="nav-user">
                <span>${nameLabel}</span>
                <button class="btn btn-outline btn-sm" onclick="logout()">Çıkış</button>
            </div>
        </div>
    </nav>`;
}

function escapeHtml(value) {
    const div = document.createElement('div');
    div.textContent = value == null ? '' : String(value);
    return div.innerHTML;
}

function formatTime(t) {
    return t ? t.substring(0, 5) : '';
}

function formatDate(d) {
    if (!d) return '';
    const parts = d.split('-');
    return parts.length === 3 ? `${parts[2]}.${parts[1]}.${parts[0]}` : d;
}

function showAlert(message, type) {
    let modal = document.getElementById('shared-alert-modal');
    if (!modal) {
        modal = document.createElement('div');
        modal.id = 'shared-alert-modal';
        modal.style.cssText = 'display:none; position:fixed; inset:0; background:rgba(11,61,30,0.45); align-items:center; justify-content:center; z-index:300;';
        modal.innerHTML = `
            <div class="card" style="max-width:360px; width:90%; text-align:center;">
                <div id="shared-alert-icon" style="font-size:32px; margin-bottom:10px;"></div>
                <p id="shared-alert-message" style="margin-bottom:18px;"></p>
                <button class="btn btn-primary btn-block" id="shared-alert-ok">Tamam</button>
            </div>`;
        document.body.appendChild(modal);
        document.getElementById('shared-alert-ok').addEventListener('click', () => {
            modal.style.display = 'none';
        });
    }

    document.getElementById('shared-alert-icon').textContent = type === 'success' ? '✅' : '⚠️';
    document.getElementById('shared-alert-message').textContent = message;
    modal.style.display = 'flex';
}
