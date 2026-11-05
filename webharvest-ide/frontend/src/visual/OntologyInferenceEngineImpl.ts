/**
 * OntologyInferenceEngineImpl - Implementacja silnika inferencji ontologicznej
 */

import type {
    OntologyInferenceEngine,
    OntologyEntity,
    EntitySchema,
    Relationship,
    RelationshipType,
    ValidationResult,
    RelationshipConstraint
} from './OntologyModel';

export class OntologyInferenceEngineImpl implements OntologyInferenceEngine {
    inferRelationships(entity: OntologyEntity, schema: EntitySchema): Relationship[] {
        const inferred: Relationship[] = [];

        // Inferuj relacje na podstawie schematu
        schema.allowedRelationships.forEach((constraints, relationshipType) => {
            constraints.forEach(constraint => {
                // TODO: Implementuj inferencję na podstawie właściwości encji
                // Na przykład: jeśli węzeł ma parentId, inferuj relację CHILD_OF
                if (entity.properties.has('parentId')) {
                    const parentId = entity.properties.get('parentId');
                    if (parentId && typeof parentId === 'string') {
                        inferred.push({
                            id: `inferred-${entity.id}-${relationshipType}-${parentId}`,
                            type: relationshipType,
                            from: entity.id,
                            to: parentId as string,
                            properties: new Map(),
                            metadata: {
                                createdAt: Date.now(),
                                confidence: 0.8,
                                source: 'inferred'
                            }
                        });
                    }
                }
            });
        });

        return inferred;
    }

    validateRelationship(
        from: OntologyEntity,
        to: OntologyEntity,
        relationshipType: RelationshipType,
        schema: EntitySchema
    ): ValidationResult {
        const errors: string[] = [];
        const warnings: string[] = [];
        const inferredRelationships: Relationship[] = [];

        // Sprawdź czy relacja jest dozwolona w schemacie
        const constraints = schema.allowedRelationships.get(relationshipType);
        if (!constraints || constraints.length === 0) {
            errors.push(`Relationship type '${relationshipType}' is not allowed for entity type '${from.type}'`);
            return { valid: false, errors, warnings, inferredRelationships };
        }

        // Sprawdź constraints
        let validConstraint = false;
        for (const constraint of constraints) {
            const targetTypes = Array.isArray(constraint.targetType) 
                ? constraint.targetType 
                : [constraint.targetType];
            
            if (targetTypes.includes(to.type)) {
                validConstraint = true;
                
                // Sprawdź cardinality
                const existingRelationships = this.countRelationships(from, relationshipType, constraint);
                if (existingRelationships < constraint.cardinality.min) {
                    warnings.push(`Minimum cardinality not met: expected at least ${constraint.cardinality.min}, found ${existingRelationships}`);
                }
                if (constraint.cardinality.max !== -1 && existingRelationships >= constraint.cardinality.max) {
                    errors.push(`Maximum cardinality exceeded: expected at most ${constraint.cardinality.max}, found ${existingRelationships}`);
                }
                
                break;
            }
        }

        if (!validConstraint) {
            errors.push(`Relationship '${relationshipType}' from '${from.type}' to '${to.type}' is not allowed by schema`);
        }

        // Inferuj dodatkowe relacje jeśli to możliwe
        if (relationshipType === 'PARENT_OF') {
            // Jeśli A jest rodzicem B, to B jest dzieckiem A
            inferredRelationships.push({
                id: `inferred-${to.id}-CHILD_OF-${from.id}`,
                type: 'CHILD_OF',
                from: to.id,
                to: from.id,
                properties: new Map(),
                metadata: {
                    createdAt: Date.now(),
                    confidence: 1.0,
                    source: 'inferred'
                }
            });
        }

        return {
            valid: errors.length === 0,
            errors,
            warnings,
            inferredRelationships
        };
    }

    findPossibleRelationships(
        from: OntologyEntity,
        to: OntologyEntity,
        schemas: Map<string, EntitySchema>
    ): RelationshipType[] {
        const possible: RelationshipType[] = [];
        const fromSchema = schemas.get(from.type);
        
        if (!fromSchema) {
            return possible;
        }

        fromSchema.allowedRelationships.forEach((constraints, relationshipType) => {
            for (const constraint of constraints) {
                const targetTypes = Array.isArray(constraint.targetType)
                    ? constraint.targetType
                    : [constraint.targetType];
                
                if (targetTypes.includes(to.type)) {
                    possible.push(relationshipType);
                    break;
                }
            }
        });

        return possible;
    }

    validateEntity(entity: OntologyEntity, schema: EntitySchema): ValidationResult[] {
        const results: ValidationResult[] = [];

        // Sprawdź wymagane właściwości
        schema.properties.forEach((propSchema, propName) => {
            if (propSchema.required && !entity.properties.has(propName)) {
                results.push({
                    valid: false,
                    errors: [`Required property '${propName}' is missing`],
                    warnings: []
                });
            }
        });

        // Sprawdź constraints
        schema.constraints.forEach(constraint => {
            const result = this.validateConstraint(entity, constraint);
            if (!result.valid) {
                results.push(result);
            }
        });

        return results;
    }

    findCompatibleEntities(
        entity: OntologyEntity,
        relationshipType: RelationshipType,
        schemas: Map<string, EntitySchema>
    ): OntologyEntity[] {
        // TODO: Implementuj wyszukiwanie kompatybilnych encji
        // Na razie zwracamy pustą tablicę
        return [];
    }

    private countRelationships(
        entity: OntologyEntity,
        relationshipType: RelationshipType,
        constraint: RelationshipConstraint
    ): number {
        // TODO: Implementuj liczenie istniejących relacji
        // Na razie zwracamy 0
        return 0;
    }

    private validateConstraint(
        entity: OntologyEntity,
        constraint: any
    ): ValidationResult {
        const errors: string[] = [];
        const warnings: string[] = [];

        switch (constraint.type) {
            case 'required':
                if (constraint.property && !entity.properties.has(constraint.property)) {
                    errors.push(constraint.message || `Required property '${constraint.property}' is missing`);
                }
                break;
            case 'unique':
                // TODO: Implementuj walidację unikalności
                break;
            case 'range':
                if (constraint.property) {
                    const value = entity.properties.get(constraint.property);
                    if (typeof value === 'number') {
                        if (constraint.min !== undefined && value < constraint.min) {
                            errors.push(constraint.message || `Value ${value} is below minimum ${constraint.min}`);
                        }
                        if (constraint.max !== undefined && value > constraint.max) {
                            errors.push(constraint.message || `Value ${value} is above maximum ${constraint.max}`);
                        }
                    }
                }
                break;
            case 'pattern':
                if (constraint.property && constraint.pattern) {
                    const value = entity.properties.get(constraint.property);
                    if (typeof value === 'string') {
                        const pattern = new RegExp(constraint.pattern);
                        if (!pattern.test(value)) {
                            errors.push(constraint.message || `Value '${value}' does not match pattern '${constraint.pattern}'`);
                        }
                    }
                }
                break;
            case 'custom':
                if (constraint.validator && !constraint.validator(entity)) {
                    errors.push(constraint.message || 'Custom validation failed');
                }
                break;
        }

        return {
            valid: errors.length === 0,
            errors,
            warnings
        };
    }
}









