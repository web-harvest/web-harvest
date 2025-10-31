/**
 * PortSystem - TDD Tests
 * Tests for input/output port rendering and management
 */

import { describe, it, expect, beforeEach } from '@jest/globals';
import { PortSystem, Port, PortType } from '../PortSystem';
import type { VisualNode } from '../../types';

describe('PortSystem', () => {
    let portSystem: PortSystem;
    let mockNode: VisualNode;
    let mockElement: HTMLElement;

    beforeEach(() => {
        portSystem = new PortSystem();

        mockNode = {
            id: 'node-1',
            type: 'http',
            attributes: { url: 'http://example.com' },
            body: null,
            children: []
        };

        mockElement = document.createElement('div');
        mockElement.className = 'visual-node';
        mockElement.dataset.nodeId = 'node-1';
        mockElement.style.width = '250px';
        mockElement.style.height = '100px';
    });

    describe('addPorts', () => {
        it('should add input and output ports to action node', () => {
            portSystem.addPorts(mockElement, mockNode);

            const inputPort = mockElement.querySelector('.port-input');
            const outputPort = mockElement.querySelector('.port-output');

            expect(inputPort).not.toBeNull();
            expect(outputPort).not.toBeNull();
        });

        it('should position input port at top center', () => {
            portSystem.addPorts(mockElement, mockNode);

            const inputPort = mockElement.querySelector('.port-input') as HTMLElement;
            expect(inputPort.style.top).toBe('0px');
            expect(inputPort.style.left).toBe('50%');
        });

        it('should position output port at bottom center', () => {
            portSystem.addPorts(mockElement, mockNode);

            const outputPort = mockElement.querySelector('.port-output') as HTMLElement;
            expect(outputPort.style.bottom).toBe('0px');
            expect(outputPort.style.left).toBe('50%');
        });

        it('should add multiple output ports for if/case nodes', () => {
            mockNode.type = 'if';

            portSystem.addPorts(mockElement, mockNode);

            const outputPorts = mockElement.querySelectorAll('.port-output');
            expect(outputPorts.length).toBeGreaterThan(1); // true, false ports
        });

        it('should add error port for try nodes', () => {
            mockNode.type = 'try';

            portSystem.addPorts(mockElement, mockNode);

            const errorPort = mockElement.querySelector('.port-error');
            expect(errorPort).not.toBeNull();
        });

        it('should set port data attributes', () => {
            portSystem.addPorts(mockElement, mockNode);

            const inputPort = mockElement.querySelector('.port-input') as HTMLElement;
            expect(inputPort.dataset.nodeId).toBe('node-1');
            expect(inputPort.dataset.portType).toBe('input');
        });
    });

    describe('getPortPosition', () => {
        it('should calculate input port position', () => {
            portSystem.addPorts(mockElement, mockNode);

            const position = portSystem.getPortPosition(mockElement, 'input');
            expect(position).not.toBeNull();
            expect(position?.x).toBe(125); // 50% of 250px
            expect(position?.y).toBe(0);
        });

        it('should calculate output port position', () => {
            portSystem.addPorts(mockElement, mockNode);

            const position = portSystem.getPortPosition(mockElement, 'output');
            expect(position).not.toBeNull();
            expect(position?.y).toBe(100); // height
        });
    });

    describe('highlightCompatiblePorts', () => {
        it('should highlight valid target ports', () => {
            const sourceElement = document.createElement('div');
            const targetElement = document.createElement('div');

            portSystem.addPorts(sourceElement, mockNode);
            portSystem.addPorts(targetElement, { ...mockNode, id: 'node-2' });

            portSystem.highlightCompatiblePorts(sourceElement, 'output');

            const targetPort = targetElement.querySelector('.port-input') as HTMLElement;
            expect(targetPort.classList.contains('port-highlighted')).toBe(true);
        });

        it('should not highlight incompatible ports', () => {
            const sourceElement = document.createElement('div');
            const targetElement = document.createElement('div');

            portSystem.addPorts(sourceElement, mockNode);
            portSystem.addPorts(targetElement, { ...mockNode, id: 'node-2' });

            portSystem.highlightCompatiblePorts(sourceElement, 'output');

            const sourcePort = sourceElement.querySelector('.port-output') as HTMLElement;
            expect(sourcePort.classList.contains('port-highlighted')).toBe(false);
        });
    });

    describe('clearHighlights', () => {
        it('should remove all port highlights', () => {
            const element = document.createElement('div');
            portSystem.addPorts(element, mockNode);

            portSystem.highlightCompatiblePorts(element, 'output');
            portSystem.clearHighlights();

            const ports = element.querySelectorAll('.port-highlighted');
            expect(ports.length).toBe(0);
        });
    });

    describe('getPortsForNode', () => {
        it('should return all ports for a node', () => {
            portSystem.addPorts(mockElement, mockNode);

            const ports = portSystem.getPortsForNode(mockElement);
            expect(ports.length).toBe(2); // input + output
        });

        it('should return port metadata', () => {
            portSystem.addPorts(mockElement, mockNode);

            const ports = portSystem.getPortsForNode(mockElement);
            const inputPort = ports.find(p => p.type === 'input');

            expect(inputPort).toBeDefined();
            expect(inputPort?.nodeId).toBe('node-1');
            expect(inputPort?.position).toBeDefined();
        });
    });

    describe('port compatibility', () => {
        it('should allow output to input connection', () => {
            const compatible = portSystem.arePortsCompatible('output', 'input');
            expect(compatible).toBe(true);
        });

        it('should not allow input to input connection', () => {
            const compatible = portSystem.arePortsCompatible('input', 'input');
            expect(compatible).toBe(false);
        });

        it('should not allow output to output connection', () => {
            const compatible = portSystem.arePortsCompatible('output', 'output');
            expect(compatible).toBe(false);
        });

        it('should allow conditional output to input', () => {
            const compatible = portSystem.arePortsCompatible('output-true', 'input');
            expect(compatible).toBe(true);
        });

        it('should allow error output to error input', () => {
            const compatible = portSystem.arePortsCompatible('output-error', 'input-error');
            expect(compatible).toBe(true);
        });
    });

    describe('port types', () => {
        it('should create sequential port for action nodes', () => {
            const ports = portSystem.getPortsForNodeType('http');

            expect(ports.input.type).toBe('sequential');
            expect(ports.output.type).toBe('sequential');
        });

        it('should create conditional ports for if nodes', () => {
            const ports = portSystem.getPortsForNodeType('if');

            expect(ports.outputs.length).toBeGreaterThan(1);
            expect(ports.outputs.some(p => p.label === 'true')).toBe(true);
            expect(ports.outputs.some(p => p.label === 'false')).toBe(true);
        });

        it('should create error port for try nodes', () => {
            const ports = portSystem.getPortsForNodeType('try');

            expect(ports.outputs.some(p => p.type === 'error')).toBe(true);
        });
    });

    describe('hover effects', () => {
        it('should show hover state on port', () => {
            portSystem.addPorts(mockElement, mockNode);

            const port = mockElement.querySelector('.port-input') as HTMLElement;
            port.dispatchEvent(new MouseEvent('mouseenter'));

            expect(port.classList.contains('port-hover')).toBe(true);
        });

        it('should clear hover state on mouse leave', () => {
            portSystem.addPorts(mockElement, mockNode);

            const port = mockElement.querySelector('.port-input') as HTMLElement;
            port.dispatchEvent(new MouseEvent('mouseenter'));
            port.dispatchEvent(new MouseEvent('mouseleave'));

            expect(port.classList.contains('port-hover')).toBe(false);
        });
    });
});


