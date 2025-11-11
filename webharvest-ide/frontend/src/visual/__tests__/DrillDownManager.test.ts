/**
 * DrillDownManager - TDD tests
 */

import { describe, it, expect, beforeEach } from '@jest/globals';
import { DrillDownManager } from '../DrillDownManager';

describe('DrillDownManager', () => {
    let manager: DrillDownManager;

    beforeEach(() => {
        manager = new DrillDownManager();
    });

    it('starts at root with empty trail', () => {
        expect(manager.getCurrentId()).toBeNull();
        expect(manager.getTrail()).toEqual([]);
    });

    it('tracks entered containers in order', () => {
        manager.enter('loop-1');
        manager.enter('try-1');

        expect(manager.getCurrentId()).toBe('try-1');
        expect(manager.getTrail()).toEqual(['loop-1', 'try-1']);
    });

    it('supports exiting to previous level', () => {
        manager.enter('loop-1');
        manager.enter('if-1');

        const exited = manager.exit();
        expect(exited).toBe('if-1');
        expect(manager.getCurrentId()).toBe('loop-1');
        expect(manager.getTrail()).toEqual(['loop-1']);
    });

    it('ignores exit when no levels are present', () => {
        const exited = manager.exit();
        expect(exited).toBeNull();
        expect(manager.getTrail()).toEqual([]);
    });

    it('navigates directly to requested level', () => {
        manager.enter('loop-1');
        manager.enter('try-1');
        manager.enter('catch-1');

        manager.navigateToLevel(1);

        expect(manager.getTrail()).toEqual(['loop-1']);
        expect(manager.getCurrentId()).toBe('loop-1');
    });

    it('resets to root when navigating to level 0', () => {
        manager.enter('loop-1');
        manager.enter('try-1');

        manager.navigateToLevel(0);

        expect(manager.getTrail()).toEqual([]);
        expect(manager.getCurrentId()).toBeNull();
    });
});

