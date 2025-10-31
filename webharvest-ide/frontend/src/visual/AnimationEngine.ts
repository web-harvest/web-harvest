/**
 * AnimationEngine - Implementation
 * Centralized animation management for node operations
 */

export type AnimationType = 'fadeIn' | 'fadeOut' | 'scaleUp' | 'scaleDown' | 'slideIn' | 'slideOut';

export class AnimationEngine {
    animate(element: HTMLElement, type: AnimationType, duration: number = 300): Promise<void> {
        return new Promise((resolve) => {
            element.style.transition = `all ${duration}ms ease`;

            switch (type) {
                case 'fadeIn':
                    element.style.opacity = '0';
                    setTimeout(() => { element.style.opacity = '1'; }, 10);
                    break;
                case 'fadeOut':
                    element.style.opacity = '1';
                    setTimeout(() => { element.style.opacity = '0'; }, 10);
                    break;
                case 'scaleUp':
                    element.style.transform = 'scale(0.8)';
                    setTimeout(() => { element.style.transform = 'scale(1)'; }, 10);
                    break;
                case 'scaleDown':
                    element.style.transform = 'scale(1)';
                    setTimeout(() => { element.style.transform = 'scale(0.8)'; }, 10);
                    break;
            }

            setTimeout(() => {
                element.style.transition = '';
                resolve();
            }, duration);
        });
    }

    animateNodeAdd(element: HTMLElement): Promise<void> {
        return this.animate(element, 'fadeIn', 300);
    }

    animateNodeRemove(element: HTMLElement): Promise<void> {
        return this.animate(element, 'fadeOut', 200);
    }
}


