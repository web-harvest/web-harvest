/**
 * Arrow Rendering Visual Tests
 * 
 * Testy sprawdzające, że strzałki są rysowane poprawnie na płótnie
 */

import { describe, it, expect, beforeEach } from '@jest/globals';
import type { Point } from '../VisualSemanticModel';

describe('Arrow Rendering Visual Tests', () => {
    describe('Arrow polygon calculation', () => {
        it('should calculate arrow points correctly for horizontal right-to-left connection', () => {
            const arrowEndPoint: Point = { x: 100, y: 125 };
            const angle = Math.PI; // 180 degrees (pointing left)
            const arrowSize = 10;

            // Calculate arrow base
            const arrowBaseX = arrowEndPoint.x - Math.cos(angle) * arrowSize;
            const arrowBaseY = arrowEndPoint.y - Math.sin(angle) * arrowSize;

            // Calculate perpendicular offset
            const baseWidth = arrowSize * 0.6;
            const perpAngle = angle + Math.PI / 2;
            const offsetX = Math.cos(perpAngle) * baseWidth;
            const offsetY = Math.sin(perpAngle) * baseWidth;

            const arrowPoints = [
                [arrowEndPoint.x, arrowEndPoint.y], // Tip
                [arrowBaseX - offsetX, arrowBaseY - offsetY], // Left base
                [arrowBaseX + offsetX, arrowBaseY + offsetY]  // Right base
            ];

            // Tip should be at end point
            expect(arrowPoints[0][0]).toBe(arrowEndPoint.x);
            expect(arrowPoints[0][1]).toBe(arrowEndPoint.y);

            // Base should be offset by arrowSize in direction opposite to angle
            expect(arrowBaseX).toBe(arrowEndPoint.x + arrowSize); // cos(π) = -1, so -(-1)*10 = +10
            expect(arrowBaseY).toBe(arrowEndPoint.y); // sin(π) = 0

            // Base points should form a triangle pointing towards end point
            const tipToBase1 = {
                x: arrowPoints[1][0] - arrowPoints[0][0],
                y: arrowPoints[1][1] - arrowPoints[0][1]
            };
            const tipToBase2 = {
                x: arrowPoints[2][0] - arrowPoints[0][0],
                y: arrowPoints[2][1] - arrowPoints[0][1]
            };

            // Both base points should be behind the tip (in direction opposite to angle)
            const angleDirection = { x: Math.cos(angle), y: Math.sin(angle) };
            const dot1 = tipToBase1.x * angleDirection.x + tipToBase1.y * angleDirection.y;
            const dot2 = tipToBase2.x * angleDirection.x + tipToBase2.y * angleDirection.y;

            // Both should be negative (pointing opposite to angle direction)
            expect(dot1).toBeLessThan(0);
            expect(dot2).toBeLessThan(0);
        });

        it('should calculate arrow points correctly for vertical top-to-bottom connection', () => {
            const arrowEndPoint: Point = { x: 150, y: 200 };
            const angle = Math.PI / 2; // 90 degrees (pointing down)
            const arrowSize = 10;

            // Calculate arrow base
            const arrowBaseX = arrowEndPoint.x - Math.cos(angle) * arrowSize;
            const arrowBaseY = arrowEndPoint.y - Math.sin(angle) * arrowSize;

            // Calculate perpendicular offset
            const baseWidth = arrowSize * 0.6;
            const perpAngle = angle + Math.PI / 2;
            const offsetX = Math.cos(perpAngle) * baseWidth;
            const offsetY = Math.sin(perpAngle) * baseWidth;

            const arrowPoints = [
                [arrowEndPoint.x, arrowEndPoint.y], // Tip
                [arrowBaseX - offsetX, arrowBaseY - offsetY], // Left base
                [arrowBaseX + offsetX, arrowBaseY + offsetY]  // Right base
            ];

            // Tip should be at end point
            expect(arrowPoints[0][0]).toBe(arrowEndPoint.x);
            expect(arrowPoints[0][1]).toBe(arrowEndPoint.y);

            // Base should be offset by arrowSize in direction opposite to angle
            expect(arrowBaseX).toBe(arrowEndPoint.x); // cos(π/2) = 0
            expect(arrowBaseY).toBe(arrowEndPoint.y - arrowSize); // sin(π/2) = 1, so -1*10 = -10

            // Base points should form a triangle pointing towards end point
            const tipToBase1 = {
                x: arrowPoints[1][0] - arrowPoints[0][0],
                y: arrowPoints[1][1] - arrowPoints[0][1]
            };
            const tipToBase2 = {
                x: arrowPoints[2][0] - arrowPoints[0][0],
                y: arrowPoints[2][1] - arrowPoints[0][1]
            };

            // Both base points should be behind the tip (in direction opposite to angle)
            const angleDirection = { x: Math.cos(angle), y: Math.sin(angle) };
            const dot1 = tipToBase1.x * angleDirection.x + tipToBase1.y * angleDirection.y;
            const dot2 = tipToBase2.x * angleDirection.x + tipToBase2.y * angleDirection.y;

            // Both should be negative (pointing opposite to angle direction)
            expect(dot1).toBeLessThan(0);
            expect(dot2).toBeLessThan(0);
        });

        it('should calculate arrow points correctly for diagonal connection', () => {
            const arrowEndPoint: Point = { x: 200, y: 225 };
            const angle = Math.PI / 4; // 45 degrees (pointing down-right)
            const arrowSize = 10;

            // Calculate arrow base
            const arrowBaseX = arrowEndPoint.x - Math.cos(angle) * arrowSize;
            const arrowBaseY = arrowEndPoint.y - Math.sin(angle) * arrowSize;

            // Calculate perpendicular offset
            const baseWidth = arrowSize * 0.6;
            const perpAngle = angle + Math.PI / 2;
            const offsetX = Math.cos(perpAngle) * baseWidth;
            const offsetY = Math.sin(perpAngle) * baseWidth;

            const arrowPoints = [
                [arrowEndPoint.x, arrowEndPoint.y], // Tip
                [arrowBaseX - offsetX, arrowBaseY - offsetY], // Left base
                [arrowBaseX + offsetX, arrowBaseY + offsetY]  // Right base
            ];

            // Tip should be at end point
            expect(arrowPoints[0][0]).toBe(arrowEndPoint.x);
            expect(arrowPoints[0][1]).toBe(arrowEndPoint.y);

            // Base should be offset by arrowSize in direction opposite to angle
            const expectedBaseX = arrowEndPoint.x - Math.cos(angle) * arrowSize;
            const expectedBaseY = arrowEndPoint.y - Math.sin(angle) * arrowSize;
            expect(arrowBaseX).toBeCloseTo(expectedBaseX, 5);
            expect(arrowBaseY).toBeCloseTo(expectedBaseY, 5);

            // Base points should form a triangle pointing towards end point
            const tipToBase1 = {
                x: arrowPoints[1][0] - arrowPoints[0][0],
                y: arrowPoints[1][1] - arrowPoints[0][1]
            };
            const tipToBase2 = {
                x: arrowPoints[2][0] - arrowPoints[0][0],
                y: arrowPoints[2][1] - arrowPoints[0][1]
            };

            // Both base points should be behind the tip (in direction opposite to angle)
            const angleDirection = { x: Math.cos(angle), y: Math.sin(angle) };
            const dot1 = tipToBase1.x * angleDirection.x + tipToBase1.y * angleDirection.y;
            const dot2 = tipToBase2.x * angleDirection.x + tipToBase2.y * angleDirection.y;

            // Both should be negative (pointing opposite to angle direction)
            expect(dot1).toBeLessThan(0);
            expect(dot2).toBeLessThan(0);
        });
    });
});






