import { appUrl, isReachable } from './connection.js';

const AUTO_RETRY_MS = 5000;

const button = document.getElementById('retry');
const spinner = document.getElementById('retry-spinner');
const label = document.getElementById('retry-label');
const status = document.getElementById('status');

let checking = false;

async function tryReconnect({ manual = false } = {}) {
    if (checking) return;
    checking = true;
    button.disabled = true;
    spinner.classList.remove('hidden');
    label.textContent = 'Menghubungkan...';

    if (await isReachable()) {
        status.textContent = 'Tersambung! Membuka Mindfora...';
        window.location.replace(appUrl);
        return;
    }

    checking = false;
    button.disabled = false;
    spinner.classList.add('hidden');
    label.textContent = 'Coba lagi';
    status.textContent = manual
        ? 'Masih belum tersambung. Periksa Wi-Fi atau data seluler kamu.'
        : 'Mencoba menghubungkan otomatis...';
}

button.addEventListener('click', () => tryReconnect({ manual: true }));
window.addEventListener('online', () => tryReconnect());
setInterval(() => navigator.onLine && tryReconnect(), AUTO_RETRY_MS);
