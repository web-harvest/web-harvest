import { describe, expect, it, beforeEach } from '@jest/globals';
import { getDependentsHintVisibility, dismissDependentsHint } from '../PalettePreferences';

class MemoryStorage implements Storage {
    private store = new Map<string, string>();

    get length(): number {
        return this.store.size;
    }

    clear(): void {
        this.store.clear();
    }

    getItem(key: string): string | null {
        return this.store.get(key) ?? null;
    }

    key(index: number): string | null {
        return Array.from(this.store.keys())[index] ?? null;
    }

    removeItem(key: string): void {
        this.store.delete(key);
    }

    setItem(key: string, value: string): void {
        this.store.set(key, value);
    }
}

describe('PalettePreferences', () => {
    let storage: MemoryStorage;

    beforeEach(() => {
        storage = new MemoryStorage();
    });

    it('shows dependents hint by default when no preference is stored', () => {
        const visible = getDependentsHintVisibility(storage);
        expect(visible).toBe(true);
    });

    it('hides dependents hint once dismissed', () => {
        dismissDependentsHint(storage);
        const visible = getDependentsHintVisibility(storage);
        expect(visible).toBe(false);
    });

    it('ignores malformed preference values', () => {
        storage.setItem('designer.palette.hideDependentsHint', 'maybe');
        const visible = getDependentsHintVisibility(storage);
        expect(visible).toBe(true);
    });
});

