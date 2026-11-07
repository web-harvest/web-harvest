import { readFile } from 'node:fs/promises';
import { defineConfig, type Plugin } from 'vite';
import path from 'path';

const runtimeWebappDir = path.resolve(__dirname, '../src/main/resources/webapp');
const runtimeStaticDir = path.join(runtimeWebappDir, 'static');
const runtimeHtmlPath = path.join(runtimeWebappDir, 'index.html');

const staticContentTypes: Record<string, string> = {
    '.css': 'text/css; charset=utf-8',
    '.js': 'text/javascript; charset=utf-8',
    '.json': 'application/json; charset=utf-8',
    '.svg': 'image/svg+xml',
    '.png': 'image/png',
    '.jpg': 'image/jpeg',
    '.jpeg': 'image/jpeg',
    '.gif': 'image/gif',
    '.webp': 'image/webp',
    '.ico': 'image/x-icon',
    '.woff': 'font/woff',
    '.woff2': 'font/woff2',
    '.ttf': 'font/ttf',
    '.otf': 'font/otf',
};

function runtimeHtmlForDevelopment(): Plugin {
    return {
        name: 'webharvest-runtime-html',
        apply: 'serve',
        transformIndexHtml: {
            order: 'pre',
            handler() {
                const productionShell = require('node:fs').readFileSync(runtimeHtmlPath, 'utf-8');
                const withViteEntry = productionShell.replace(
                    /<script\\b(?=[^>]*\\bsrc=["']dist\\/assets\\/main\\.js(?:\\?[^"']*)?["'])[^>]*>\\s*<\\/script>/i,
                    '<script type="module" src="/src/main.ts"></script>',
                );
                const developmentShell = withViteEntry.replace(
                    /<link\\b(?=[^>]*\\bhref=["']dist\\/assets\\/main\\.css(?:\\?[^"']*)?["'])[^>]*\\/?\s*>/i,
                    '',
                );

                if (developmentShell === productionShell) {
                    throw new Error(
                        'Could not adapt the canonical WebHarvest runtime HTML: expected production main.js and main.css references were not found.',
                    );
                }

                return developmentShell;
            },
        },
        configureServer(server) {
            server.middlewares.use(async (request, response, next) => {
                if (!request.url || (request.method !== 'GET' && request.method !== 'HEAD')) {
                    next();
                    return;
                }

                let pathname: string;
                try {
                    pathname = decodeURIComponent(new URL(request.url, 'http://localhost').pathname);
                } catch {
                    response.statusCode = 400;
                    response.end('Bad request');
                    return;
                }

                if (!pathname.startsWith('/static/')) {
                    next();
                    return;
                }

                const relativePath = pathname.slice('/static/'.length);
                const filePath = path.resolve(runtimeStaticDir, relativePath);
                if (!filePath.startsWith(runtimeStaticDir + path.sep)) {
                    response.statusCode = 403;
                    response.end('Forbidden');
                    return;
                }

                try {
                    const file = await readFile(filePath);
                    response.statusCode = 200;
                    response.setHeader(
                        'Content-Type',
                        staticContentTypes[path.extname(filePath).toLowerCase()] || 'application/octet-stream',
                    );
                    response.setHeader('Cache-Control', 'no-cache');
                    response.end(request.method === 'HEAD' ? undefined : file);
                } catch (error) {
                    if ((error as NodeJS.ErrnoException).code === 'ENOENT') {
                        next();
                        return;
                    }
                    next(error as Error);
                }
            });
        },
    };
}

export default defineConfig({
    plugins: [runtimeHtmlForDevelopment()],
    // Serve the same static resource tree referenced by the production HTML.
    // The custom middleware preserves the existing /static/** URL contract.
    publicDir: false,
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
