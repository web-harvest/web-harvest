export default {
    preset: 'ts-jest',
    testEnvironment: 'jsdom',
    roots: ['<rootDir>/src'],
    testMatch: ['**/__tests__/**/*.test.ts', '**/*.test.ts'],
    // D3 v7 and its closure ship ESM-only entry points ("type":"module").
    // Transform that closure to CJS so Jest can load the real modules without
    // global D3 mocks or --experimental-vm-modules. Allow-list is the verified
    // ESM-only closure of d3@7.x: d3, d3-*, internmap, delaunator, robust-predicates.
    transform: {
        '^.+\\.tsx?$': [
            'ts-jest',
            {
                tsconfig: {
                    module: 'commonjs',
                    esModuleInterop: true,
                    isolatedModules: true,
                },
            },
        ],
        '^.+\\.m?js$': [
            'ts-jest',
            {
                tsconfig: {
                    allowJs: true,
                    module: 'commonjs',
                    target: 'es2020',
                    esModuleInterop: true,
                    isolatedModules: true,
                },
            },
        ],
    },
    transformIgnorePatterns: [
        '/node_modules/(?!(d3|d3-[^/]+|internmap|delaunator|robust-predicates)/)',
    ],
    moduleNameMapper: {
        '^@/(.*)$': '<rootDir>/src/$1',
        '^@core/(.*)$': '<rootDir>/src/core/$1',
        '^@visual/(.*)$': '<rootDir>/src/visual/$1',
        '^@panels/(.*)$': '<rootDir>/src/panels/$1',
        '^@types/(.*)$': '<rootDir>/src/types/$1',
        '^@utils/(.*)$': '<rootDir>/src/utils/$1',
        // Resolve to compiled package output. Package sources use Node16 .js
        // import specifiers that Jest cannot map onto .ts files.
        '^@webharvest/metadata-client$':
            '<rootDir>/../../packages/visual-shared/metadata-client/dist/index.js',
        '^@webharvest/xml-roundtrip$':
            '<rootDir>/../../packages/visual-shared/xml-roundtrip/dist/index.js',
    },
    collectCoverageFrom: [
        'src/**/*.ts',
        '!src/**/*.d.ts',
        '!src/main.ts',
    ],
    coverageThreshold: {
        global: {
            branches: 80,
            functions: 80,
            lines: 80,
            statements: 80,
        },
    },
};
