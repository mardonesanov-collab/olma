import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
// frontend/vite.config.ts
export default defineConfig({
    plugins: [react()],
    base: '/', // Fayllar ildizdan chaqirilishini ta'minlaydi
    build: {
        outDir: '../src/main/resources/static',
        emptyOutDir: true,
    }
});
