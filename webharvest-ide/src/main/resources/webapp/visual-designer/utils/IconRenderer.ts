/**
 * Icon Renderer Utility
 * Renders Font Awesome icons from API (fa-globe) or emoji fallback
 */

export function renderIcon(icon: string): string {
    // If icon starts with 'fa-', render as Font Awesome
    if (icon && icon.startsWith('fa-')) {
        return `<i class="fas ${icon}"></i>`;
    }
    
    // Otherwise return as-is (emoji or other)
    return icon || '';
}

export function renderPluginIcon(plugin: any): string {
    return renderIcon(plugin.icon);
}









