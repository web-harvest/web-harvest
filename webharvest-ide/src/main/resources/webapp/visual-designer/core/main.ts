/**
 * WebHarvest Visual Designer - Main Application Entry Point
 * 
 * This is the main entry point for the visual designer application.
 * It initializes the canvas, plugin palette, property panel, and other components.
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 */

import { VisualDesigner } from './VisualDesigner';

// Application initialization
document.addEventListener('DOMContentLoaded', () => {
  console.log('🌺 WebHarvest Visual Designer - Starting...');
  
  // Get the main app container
  const appContainer = document.getElementById('app');
  
  if (!appContainer) {
    console.error('❌ Failed to find app container element');
    return;
  }
  
  // Initialize the visual designer
  try {
    const designer = new VisualDesigner(appContainer);
    
    // Make designer globally accessible for debugging
    (window as any).designer = designer;
    
    console.log('✅ Visual Designer initialized successfully');
  } catch (error) {
    console.error('❌ Failed to initialize Visual Designer:', error);
    
    // Show error message to user
    appContainer.innerHTML = `
      <div style="
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        height: 100vh;
        color: #E53935;
        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
      ">
        <h1>⚠️ Initialization Error</h1>
        <p>Failed to start Visual Designer. Check console for details.</p>
        <pre style="
          background: #f5f5f5;
          padding: 16px;
          border-radius: 8px;
          margin-top: 16px;
          max-width: 600px;
          overflow: auto;
        ">${error}</pre>
      </div>
    `;
  }
});

// Expose version info
console.log(`
╔══════════════════════════════════════════════════════════╗
║                                                          ║
║   🌺 WebHarvest Visual Designer v2.2.0                  ║
║                                                          ║
║   Low-Code Configuration Interface                      ║
║   Node-RED/NiFi-Style Flow Editor                      ║
║                                                          ║
║   © 2025 WebHarvest Project                            ║
║   BSD 3-Clause License                                  ║
║                                                          ║
╚══════════════════════════════════════════════════════════╝
`);


