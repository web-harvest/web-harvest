import { defineConfig } from 'vite';
import path from 'path';

export default defineConfig({
    resolve: {
        alias: {
            '@': path.resolve(__dirname, './src'),
            '@core': path.resolve(__dirname, './src/core'),
            '@visual': path.resolve(__dirname, './src/visual'),
            '@panels': path.resolve(__dirname, './src/panels'),
            '@types': path.resolve(__dirname, './src/types'),
            '@utils': path.resolve(__dirname, './src/utils'),
        },
    },
    build: {
        outDir: '../src/main/resources/webapp/dist',
        emptyOutDir: true,
        sourcemap: true,
        rollupOptions: {
            input: {
                main: path.resolve(__dirname, 'index.html'),
            },
            output: {
                entryFileNames: 'assets/[name].js',
                chunkFileNames: 'assets/[name].js',
                assetFileNames: 'assets/[name].[ext]',
            },
        },
    },
    server: {
        port: 3000,
        strictPort: true,
        proxy: {
            '/api': {
                target: 'http://127.0.0.1:8080',
                changeOrigin: true,
            },
            '/ws': {
                target: 'ws://127.0.0.1:8080',
                ws: true,
                changeOrigin: true,
            },
        },
    },
});

