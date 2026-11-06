import { jest } from '@jest/globals';

// Jest's native ESM runtime does not inject the CommonJS-style global `jest`
// used by a number of existing tests. Expose the official ESM API explicitly.
Object.defineProperty(globalThis, 'jest', {
    configurable: true,
    value: jest,
});

// jsdom does not implement ResizeObserver. Tests do not simulate browser layout
// changes; this no-op observer keeps construction deterministic without inventing
// resize notifications or dimensions.
if (typeof globalThis.ResizeObserver === 'undefined') {
    class ResizeObserverStub {
        observe(): void {}
        unobserve(): void {}
        disconnect(): void {}
    }

    Object.defineProperty(globalThis, 'ResizeObserver', {
        configurable: true,
        value: ResizeObserverStub,
    });
}
