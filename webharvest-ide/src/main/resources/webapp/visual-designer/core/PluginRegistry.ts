/**
 * Plugin Registry
 * 
 * Manages plugin metadata and provides search/filter capabilities
 */

export interface PluginMetadata {
  id: string;
  name: string;
  elementName: string;
  description: string;
  category: string;
  icon: string;
  version: string;
  author: string;
  tags: string[];
  helpUrl: string;
  schema: PluginConfigurationSchema;
}

export interface PluginConfigurationSchema {
  attributes: AttributeDefinition[];
  allowsBody: boolean;
  bodyType?: 'text' | 'template' | 'xml';
  allowedSubElements: string[];
  requiredSubElements?: string[];
  requiredAttributes?: string[];
  optionalAttributes?: string[];

  // Context requirements (v2.2.0)
  canAppearAtRoot?: boolean;
  requiredParents?: string[];
  isContextSpecific?: boolean;
}

export interface AttributeDefinition {
  name: string;
  type: 'string' | 'number' | 'boolean' | 'enum';
  required: boolean;
  defaultValue?: any;
  description: string;
  options?: string[];  // For enum type
  min?: number;        // For number type
  max?: number;
  pattern?: string;    // For string validation
}

export class PluginRegistry {
  private plugins: Map<string, PluginMetadata> = new Map();
  private categories: Map<string, PluginMetadata[]> = new Map();

  constructor(pluginsData: PluginMetadata[]) {
    this.registerPlugins(pluginsData);
  }

  /**
   * Register plugins from metadata array
   */
  private registerPlugins(pluginsData: PluginMetadata[]): void {
    for (const plugin of pluginsData) {
      // Index by element name
      this.plugins.set(plugin.elementName, plugin);

      // Group by category
      if (!this.categories.has(plugin.category)) {
        this.categories.set(plugin.category, []);
      }
      this.categories.get(plugin.category)!.push(plugin);
    }

    console.log(`📦 Registered ${this.plugins.size} plugins in ${this.categories.size} categories`);
  }

  /**
   * Get plugin by element name
   */
  public getPlugin(elementName: string): PluginMetadata | undefined {
    return this.plugins.get(elementName);
  }

  /**
   * Get all plugins
   */
  public getAllPlugins(): PluginMetadata[] {
    return Array.from(this.plugins.values());
  }

  /**
   * Get all category names
   */
  public getCategories(): string[] {
    return Array.from(this.categories.keys()).sort();
  }

  /**
   * Get plugins by category
   */
  public getPluginsByCategory(category: string): PluginMetadata[] {
    return this.categories.get(category) || [];
  }

  /**
   * Search plugins by query
   * Searches in: name, description, tags, category
   */
  public search(query: string): PluginMetadata[] {
    const lowerQuery = query.toLowerCase().trim();

    if (!lowerQuery) {
      return this.getAllPlugins();
    }

    return this.getAllPlugins().filter(plugin =>
      plugin.name.toLowerCase().includes(lowerQuery) ||
      plugin.description.toLowerCase().includes(lowerQuery) ||
      plugin.elementName.toLowerCase().includes(lowerQuery) ||
      plugin.tags.some(tag => tag.toLowerCase().includes(lowerQuery)) ||
      plugin.category.toLowerCase().includes(lowerQuery)
    );
  }

  /**
   * Get plugin count
   */
  public getPluginCount(): number {
    return this.plugins.size;
  }

  /**
   * Get category count
   */
  public getCategoryCount(): number {
    return this.categories.size;
  }
}


