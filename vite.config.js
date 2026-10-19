import { resolve } from 'node:path';
import { defineConfig } from 'vite';

export default defineConfig({
    root: 'src',
    envDir: '..',
    envPrefix: 'MINDFORA_',
    base: './',
    build: {
        outDir: '../dist',
        emptyOutDir: true,
        rollupOptions: {
            input: {
                index: resolve(import.meta.dirname, 'src/index.html'),
                offline: resolve(import.meta.dirname, 'src/offline.html'),
            },
        },
    },
});
