/**
 * Ontology Model - Formalna ontologia dla Visual Semantic Model
 * 
 * Wzorowane na podejściu Palantir Foundry, które używa ontologii do:
 * - Formalnego modelowania relacji między konceptami
 * - Inferencji (wnioskowania) o relacjach
 * - Querying (zapytań) o strukturę danych
 * - Walidacji na poziomie ontologii
 * 
 * Korzyści:
 * - Backend: Lepsze walidacja, inferencja relacji, querying
 * - Frontend: Lepsze renderowanie, walidacja, sugestie
 * - LLM: Lepsze zrozumienie kontekstu, możliwość querying ontologii
 */

// ============================================================================
// Core Ontology Types
// ============================================================================

/**
 * Entity - Podstawowa jednostka w ontologii (węzeł, zmienna, połączenie, etc.)
 */
export interface OntologyEntity {
    id: string;
    type: EntityType;
    properties: Map<string, PropertyValue>;
    relationships: Map<string, Relationship[]>;
    metadata: EntityMetadata;
}

export type EntityType = 
    | 'Plugin' 
    | 'Node' 
    | 'Variable' 
    | 'Connection' 
    | 'Execution' 
    | 'Log' 
    | 'Result'
    | 'Container'
    | 'Leaf';

export type PropertyValue = string | number | boolean | null | PropertyValue[] | { [key: string]: PropertyValue };

export interface EntityMetadata {
    createdAt: number;
    updatedAt: number;
    version: string;
    source: 'user' | 'system' | 'inferred';
}

/**
 * Relationship - Relacja między encjami
 */
export interface Relationship {
    id: string;
    type: RelationshipType;
    from: string; // Entity ID
    to: string; // Entity ID
    properties: Map<string, PropertyValue>;
    metadata: RelationshipMetadata;
}

export type RelationshipType =
    | 'PARENT_OF'           // Węzeł jest rodzicem innego węzła
    | 'CHILD_OF'            // Węzeł jest dzieckiem innego węzła
    | 'SEQUENTIAL_TO'       // Węzeł wykonuje się przed innym
    | 'CONDITIONAL_TO'      // Węzeł wykonuje się warunkowo przed innym
    | 'ERROR_TO'            // Węzeł wykonuje się w przypadku błędu
    | 'DEFINES_VARIABLE'    // Węzeł definiuje zmienną
    | 'USES_VARIABLE'       // Węzeł używa zmiennej
    | 'EXECUTES_AS'         // Węzeł wykonuje się jako część execution
    | 'GENERATES_LOG'       // Węzeł generuje log
    | 'PRODUCES_RESULT'     // Węzeł produkuje wynik
    | 'CONNECTED_TO'        // Węzeł jest połączony z innym (visual connection)
    | 'REQUIRES_PARENT'     // Węzeł wymaga rodzica określonego typu
    | 'ALLOWS_CHILD'        // Węzeł pozwala na dzieci określonego typu
    | 'CONTAINS'            // Kontener zawiera węzeł
    | 'DEPENDS_ON'          // Węzeł zależy od innego (data dependency)
    | 'VALIDATES'           // Węzeł waliduje dane innego
    | 'TRANSFORMS'          // Węzeł transformuje dane innego
    | 'FILTERS'             // Węzeł filtruje dane innego
    | 'AGGREGATES';         // Węzeł agreguje dane innych

export interface RelationshipMetadata {
    createdAt: number;
    confidence: number; // 0.0 - 1.0, dla inferred relationships
    source: 'explicit' | 'inferred' | 'validated';
}

// ============================================================================
// Ontology Schema - Definicje typów i relacji
// ============================================================================

/**
 * EntitySchema - Schema definiująca strukturę encji
 */
export interface EntitySchema {
    type: EntityType;
    properties: Map<string, PropertySchema>;
    allowedRelationships: Map<RelationshipType, RelationshipConstraint[]>;
    constraints: Constraint[];
}

export interface PropertySchema {
    name: string;
    type: 'string' | 'number' | 'boolean' | 'array' | 'object' | 'enum';
    required: boolean;
    defaultValue?: PropertyValue;
    enumValues?: string[];
    description?: string;
}

export interface RelationshipConstraint {
    relationshipType: RelationshipType;
    targetType: EntityType | EntityType[];
    cardinality: Cardinality;
    description?: string;
}

export interface Cardinality {
    min: number; // 0 = optional, 1+ = required
    max: number; // -1 = unlimited, 1+ = specific limit
}

export interface Constraint {
    type: 'required' | 'unique' | 'range' | 'pattern' | 'custom';
    property?: string;
    value?: PropertyValue;
    message?: string;
    validator?: (entity: OntologyEntity) => boolean;
}

// ============================================================================
// Ontology Query Language
// ============================================================================

/**
 * OntologyQuery - Zapytanie do ontologii
 */
export interface OntologyQuery {
    select: QuerySelect;
    from: EntityType | EntityType[];
    where?: QueryCondition[];
    relationships?: QueryRelationship[];
    orderBy?: QueryOrderBy[];
    limit?: number;
}

export interface QuerySelect {
    properties?: string[]; // null = wszystkie
    relationships?: RelationshipType[];
    count?: boolean;
}

export interface QueryCondition {
    property: string;
    operator: '=' | '!=' | '>' | '<' | '>=' | '<=' | 'IN' | 'NOT_IN' | 'LIKE' | 'EXISTS';
    value: PropertyValue;
}

export interface QueryRelationship {
    type: RelationshipType;
    direction: 'from' | 'to' | 'both';
    targetType?: EntityType;
    recursive?: boolean; // Czy rekurencyjnie przechodzić relacje
}

export interface QueryOrderBy {
    property: string;
    direction: 'ASC' | 'DESC';
}

// ============================================================================
// Ontology Inference Engine
// ============================================================================

/**
 * OntologyInferenceEngine - Silnik inferencji dla ontologii
 */
export interface OntologyInferenceEngine {
    /**
     * Inferuj relacje na podstawie istniejących encji i schematów
     */
    inferRelationships(entity: OntologyEntity, schema: EntitySchema): Relationship[];

    /**
     * Sprawdź czy relacja jest dozwolona
     */
    validateRelationship(
        from: OntologyEntity,
        to: OntologyEntity,
        relationshipType: RelationshipType,
        schema: EntitySchema
    ): ValidationResult;

    /**
     * Znajdź wszystkie możliwe relacje między dwoma encjami
     */
    findPossibleRelationships(
        from: OntologyEntity,
        to: OntologyEntity,
        schemas: Map<EntityType, EntitySchema>
    ): RelationshipType[];

    /**
     * Sprawdź czy encja spełnia wszystkie constraints
     */
    validateEntity(entity: OntologyEntity, schema: EntitySchema): ValidationResult[];

    /**
     * Znajdź wszystkie encje które mogą być połączone z daną encją
     */
    findCompatibleEntities(
        entity: OntologyEntity,
        relationshipType: RelationshipType,
        schemas: Map<EntityType, EntitySchema>
    ): OntologyEntity[];
}

export interface ValidationResult {
    valid: boolean;
    errors: string[];
    warnings: string[];
    inferredRelationships?: Relationship[];
}

// ============================================================================
// Ontology Repository - Przechowywanie i querying
// ============================================================================

/**
 * OntologyRepository - Repozytorium encji ontologicznych
 */
export interface OntologyRepository {
    /**
     * Dodaj encję do repozytorium
     */
    addEntity(entity: OntologyEntity): void;

    /**
     * Usuń encję z repozytorium
     */
    removeEntity(entityId: string): void;

    /**
     * Pobierz encję po ID
     */
    getEntity(entityId: string): OntologyEntity | null;

    /**
     * Pobierz wszystkie encje określonego typu
     */
    getEntitiesByType(type: EntityType): OntologyEntity[];

    /**
     * Wykonaj zapytanie do ontologii
     */
    query(query: OntologyQuery): OntologyEntity[];

    /**
     * Znajdź wszystkie relacje określonego typu
     */
    getRelationships(
        entityId: string,
        relationshipType?: RelationshipType,
        direction?: 'from' | 'to' | 'both'
    ): Relationship[];

    /**
     * Dodaj relację
     */
    addRelationship(relationship: Relationship): void;

    /**
     * Usuń relację
     */
    removeRelationship(relationshipId: string): void;

    /**
     * Pobierz wszystkie encje powiązane z daną encją
     */
    getRelatedEntities(
        entityId: string,
        relationshipType?: RelationshipType,
        targetType?: EntityType
    ): OntologyEntity[];

    /**
     * Sprawdź czy istnieje relacja między dwoma encjami
     */
    hasRelationship(
        fromId: string,
        toId: string,
        relationshipType: RelationshipType
    ): boolean;
}

// ============================================================================
// Plugin Ontology Schema - Specyficzne dla WebHarvest
// ============================================================================

/**
 * PluginOntologySchema - Schema ontologiczna dla pluginów
 */
export interface PluginOntologySchema {
    pluginType: string;
    category: string;
    
    // Hierarchia
    canAppearAtRoot: boolean;
    requiredParentTypes: string[];
    allowedChildTypes: string[];
    acceptsAnyChild: boolean;
    
    // Semantyka wykonania
    executionSemantics: {
        isContainer: boolean;
        isLeaf: boolean;
        executionOrder: 'sequential' | 'parallel' | 'conditional';
        canPause: boolean;
        canResume: boolean;
    };
    
    // Semantyka danych
    dataSemantics: {
        inputTypes: string[]; // STRING, XML, BINARY, etc.
        outputTypes: string[];
        transformsData: boolean;
        filtersData: boolean;
        aggregatesData: boolean;
    };
    
    // Semantyka zmiennych
    variableSemantics: {
        definesVariables: string[]; // Nazwy zmiennych które definiuje
        usesVariables: string[]; // Nazwy zmiennych których używa
        variableScope: 'root' | 'parent' | 'local';
    };
    
    // Constraints
    constraints: {
        minOccurs: number;
        maxOccurs: number; // -1 = unlimited
        requiredAttributes: string[];
        attributeConstraints: Map<string, AttributeConstraint>;
    };
}

export interface AttributeConstraint {
    type: 'required' | 'enum' | 'pattern' | 'range' | 'custom';
    value?: PropertyValue;
    pattern?: string;
    min?: number;
    max?: number;
    validator?: (value: PropertyValue) => boolean;
    message?: string;
}

// ============================================================================
// Visual Ontology Schema - Specyficzne dla warstwy wizualnej
// ============================================================================

/**
 * VisualOntologySchema - Schema ontologiczna dla warstwy wizualnej
 */
export interface VisualOntologySchema {
    // Pozycja i wymiary
    position: {
        canMove: boolean;
        canResize: boolean;
        defaultSize: { width: number; height: number };
        minSize: { width: number; height: number };
        maxSize: { width: number; height: number };
    };
    
    // Connection points
    connectionPoints: {
        inputs: ConnectionPointSchema[];
        outputs: ConnectionPointSchema[];
    };
    
    // Layout
    layout: {
        preferredLayout: 'vertical' | 'horizontal' | 'mindmap' | 'swimlane';
        canNest: boolean;
        nestingDepth: number; // -1 = unlimited
    };
    
    // Visual state
    visualState: {
        canSelect: boolean;
        canHover: boolean;
        canExpand: boolean;
        canCollapse: boolean;
    };
}

export interface ConnectionPointSchema {
    id: string;
    name: string;
    side: 'left' | 'right' | 'top' | 'bottom';
    type: 'input' | 'output';
    portType: string; // STRING, XML, BINARY, etc.
    required: boolean;
    multiple: boolean; // Czy może mieć wiele połączeń
    compatibleTypes: string[]; // Typy portów z którymi może się połączyć
}

// ============================================================================
// Runtime Ontology Schema - Specyficzne dla wykonania
// ============================================================================

/**
 * RuntimeOntologySchema - Schema ontologiczna dla runtime
 */
export interface RuntimeOntologySchema {
    // Execution state
    executionState: {
        canStart: boolean;
        canStop: boolean;
        canPause: boolean;
        canResume: boolean;
        executionTime: number | null; // ms
        status: 'idle' | 'running' | 'paused' | 'completed' | 'failed' | 'stopped';
    };
    
    // Logging
    logging: {
        canLog: boolean;
        logLevels: ('DEBUG' | 'INFO' | 'WARN' | 'ERROR')[];
        logFormat: string;
    };
    
    // Results
    results: {
        producesResults: boolean;
        resultTypes: string[];
        resultFormat: 'string' | 'xml' | 'json' | 'binary';
    };
    
    // Variables
    variables: {
        definesVariables: string[];
        usesVariables: string[];
        variableScope: 'root' | 'parent' | 'local';
    };
}

// ============================================================================
// Unified Ontology Model
// ============================================================================

/**
 * UnifiedOntologyModel - Zunifikowany model ontologiczny łączący wszystkie aspekty
 */
export interface UnifiedOntologyModel {
    // Repozytoria
    pluginRepository: OntologyRepository;
    nodeRepository: OntologyRepository;
    variableRepository: OntologyRepository;
    connectionRepository: OntologyRepository;
    executionRepository: OntologyRepository;
    
    // Schematy
    pluginSchemas: Map<string, PluginOntologySchema>;
    visualSchemas: Map<string, VisualOntologySchema>;
    runtimeSchemas: Map<string, RuntimeOntologySchema>;
    
    // Silniki
    inferenceEngine: OntologyInferenceEngine;
    
    // Query interface
    query(query: OntologyQuery): OntologyEntity[];
    
    // Validation interface
    validate(entity: OntologyEntity, schemaType: 'plugin' | 'visual' | 'runtime'): ValidationResult[];
    
    // Inference interface
    inferRelationships(entity: OntologyEntity): Relationship[];
}









