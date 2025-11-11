const PREFERENCE_KEY = 'designer.palette.hideDependentsHint';

type StorageLike = Pick<Storage, 'getItem' | 'setItem'>;

const resolveStorage = (storage?: StorageLike): StorageLike | null => {
    if (storage) {
        return storage;
    }

    if (typeof window !== 'undefined' && window.localStorage) {
        return window.localStorage;
    }

    return null;
};

export function getDependentsHintVisibility(storage?: StorageLike): boolean {
    const store = resolveStorage(storage);
    if (!store) {
        return true;
    }

    const value = store.getItem(PREFERENCE_KEY);
    if (value === null) {
        return true;
    }

    if (value === 'hidden') {
        return false;
    }

    // Malformed value, reset to visible
    store.setItem(PREFERENCE_KEY, 'visible');
    return true;
}

export function dismissDependentsHint(storage?: StorageLike): void {
    const store = resolveStorage(storage);
    if (!store) {
        return;
    }
    store.setItem(PREFERENCE_KEY, 'hidden');
}

