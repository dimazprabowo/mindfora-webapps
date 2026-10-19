import type { CapacitorConfig } from '@capacitor/cli';
import { existsSync } from 'node:fs';
import { version } from './package.json';

const mode = process.env.MINDFORA_ENV ?? 'production';
const envFile = `.env.${mode}`;

if (!existsSync(envFile)) {
    throw new Error(`[mindfora] ${envFile} tidak ditemukan. Salin dari .env.example.`);
}
process.loadEnvFile(envFile);

const url = process.env.MINDFORA_URL;
if (!url) {
    throw new Error(`[mindfora] MINDFORA_URL wajib diisi di ${envFile}.`);
}

const isDev = mode === 'development';
const allowNavigation = (process.env.MINDFORA_ALLOW_NAVIGATION ?? '')
    .split(',')
    .map((host) => host.trim())
    .filter(Boolean);

const config: CapacitorConfig = {
    appId: 'id.mindfora.app',
    appName: 'Mindfora',
    webDir: 'dist',
    appendUserAgent: `MindforaApp/${version}`,
    backgroundColor: '#059669',
    server: {
        url,
        cleartext: isDev,
        allowNavigation: [new URL(url).host, ...allowNavigation],
        errorPath: 'offline.html',
    },
    android: {
        allowMixedContent: isDev,
        webContentsDebuggingEnabled: isDev,
    },
};

export default config;
