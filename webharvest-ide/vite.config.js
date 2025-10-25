import { defineConfig } from 'vite';
import { resolve } from 'path';

export default defineConfig({
    root: 'src/main/resources/webapp/visual-designer',
    base: '/visual-designer/',

    build: {
        outDir: '../../../../../target/classes/webapp/visual-designer',
        emptyOutDir: true,
        rollupOptions: {
            input: {
                main: resolve(__dirname, 'src/main/resources/webapp/visual-designer/index.html')
            }
        },
        sourcemap: true,
        minify: 'esbuild',
        target: 'es2020'
    },

    server: {
        port: 3000,
        proxy: {
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true
            }
        }
    },

    resolve: {
        alias: {
            '@': resolve(__dirname, 'src/main/resources/webapp/visual-designer')
        }
    },

    css: {
        devSourcemap: true
    }
});

