// Deterministic jsdom platform shim. Tests that need resize behavior should
// invoke the observer callback explicitly; the default observer is inert.
if (typeof globalThis.ResizeObserver === 'undefined') {
    class TestResizeObserver {
        constructor(callback) {
            this.callback = callback;
        }
        observe() {}
        unobserve() {}
        disconnect() {}
    }

    Object.defineProperty(globalThis, 'ResizeObserver', {
        configurable: true,
        writable: true,
        value: TestResizeObserver
    });
}
