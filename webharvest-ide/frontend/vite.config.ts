import { readFileSync } from 'node:fs';
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
                const productionShell = readFileSync(runtimeHtmlPath, 'utf-8');
                const scriptMarker = '<script type="module" crossorigin src="dist/assets/main.js';
                const scriptStart = productionShell.indexOf(scriptMarker);
                const scriptEnd = productionShell.indexOf('</script>', scriptStart);

                if (scriptStart < 0 || scriptEnd < 0) {
                    throw new Error(
                        'Could not adapt the canonical WebHarvest runtime HTML: production main.js reference was not found.',
                    );
                }

                const withViteEntry =
                    productionShell.slice(0, scriptStart) +
                    '<script type="module" src="/src/main.ts"></script>' +
                    productionShell.slice(scriptEnd + '</script>'.length);
                const stylesheetMarker = '<link rel="stylesheet" crossorigin href="dist/assets/main.css';
                const stylesheetStart = withViteEntry.indexOf(stylesheetMarker);
                const stylesheetEnd = withViteEntry.indexOf('>', stylesheetStart);

                if (stylesheetStart < 0 || stylesheetEnd < 0) {
                    throw new Error(
                        'Could not adapt the canonical WebHarvest runtime HTML: production main.css reference was not found.',
                    );
                }

                return (
                    withViteEntry.slice(0, stylesheetStart) +
                    withViteEntry.slice(stylesheetEnd + 1)
                );
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
