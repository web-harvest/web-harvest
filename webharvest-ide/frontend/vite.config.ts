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
            '@webharvest/metadata-client': path.resolve(
                __dirname,
                '../../packages/visual-shared/metadata-client/src/index.ts'
            ),
            '@webharvest/xml-roundtrip': path.resolve(
                __dirname,
                '../../packages/visual-shared/xml-roundtrip/src/index.ts'
            ),
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
        proxy: {
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true,
            },
            '/ws': {
                target: 'ws://localhost:8080',
                ws: true,
            },
        },
    },
});

