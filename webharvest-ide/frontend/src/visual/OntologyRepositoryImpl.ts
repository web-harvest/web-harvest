/**
 * OntologyRepositoryImpl - Implementacja repozytorium ontologicznego
 */

import type {
    OntologyRepository,
    OntologyEntity,
    OntologyQuery,
    Relationship,
    EntityType,
    RelationshipType,
    PropertyValue
} from './OntologyModel';

export class OntologyRepositoryImpl implements OntologyRepository {
    private entities: Map<string, OntologyEntity> = new Map();
    private relationships: Map<string, Relationship> = new Map();
    private entitiesByType: Map<EntityType, Set<string>> = new Map();
    private relationshipsByEntity: Map<string, Set<string>> = new Map(); // entityId -> relationshipIds

    addEntity(entity: OntologyEntity): void {
        this.entities.set(entity.id, entity);
        
        if (!this.entitiesByType.has(entity.type)) {
            this.entitiesByType.set(entity.type, new Set());
        }
        this.entitiesByType.get(entity.type)!.add(entity.id);
    }

    removeEntity(entityId: string): void {
        const entity = this.entities.get(entityId);
        if (!entity) {
            return;
        }

        // Usuń wszystkie relacje związane z tą encją
        const relationshipIds = this.relationshipsByEntity.get(entityId);
        if (relationshipIds) {
            relationshipIds.forEach(relId => {
                this.relationships.delete(relId);
            });
            this.relationshipsByEntity.delete(entityId);
        }

        // Usuń encję
        this.entities.delete(entityId);
        this.entitiesByType.get(entity.type)?.delete(entityId);
    }

    getEntity(entityId: string): OntologyEntity | null {
        return this.entities.get(entityId) || null;
    }

    getEntitiesByType(type: EntityType): OntologyEntity[] {
        const entityIds = this.entitiesByType.get(type);
        if (!entityIds) {
            return [];
        }
        return Array.from(entityIds)
            .map(id => this.entities.get(id))
            .filter((e): e is OntologyEntity => e !== undefined);
    }

    query(query: OntologyQuery): OntologyEntity[] {
        let results: OntologyEntity[] = [];

        // Filtruj po typie
        const types = Array.isArray(query.from) ? query.from : [query.from];
        types.forEach(type => {
            results.push(...this.getEntitiesByType(type));
        });

        // Zastosuj warunki WHERE
        if (query.where && query.where.length > 0) {
            results = results.filter(entity => {
                return query.where!.every(condition => {
                    const value = entity.properties.get(condition.property);
                    return this.evaluateCondition(value, condition.operator, condition.value);
                });
            });
        }

        // Zastosuj relacje
        if (query.relationships && query.relationships.length > 0) {
            const relatedResults: OntologyEntity[] = [];
            query.relationships.forEach(relQuery => {
                results.forEach(entity => {
                    const related = this.getRelatedEntities(
                        entity.id,
                        relQuery.type,
                        relQuery.targetType as EntityType
                    );
                    if (relQuery.recursive) {
                        // TODO: Implementuj rekurencyjne przechodzenie
                    }
                    relatedResults.push(...related);
                });
            });
            results = relatedResults;
        }

        // Sortuj
        if (query.orderBy && query.orderBy.length > 0) {
            results.sort((a, b) => {
            for (const order of query.orderBy!) {
                const aValue = a.properties.get(order.property) ?? null;
                const bValue = b.properties.get(order.property) ?? null;
                const comparison = this.compareValues(aValue, bValue);
                    if (comparison !== 0) {
                        return order.direction === 'ASC' ? comparison : -comparison;
                    }
                }
                return 0;
            });
        }

        // Limit
        if (query.limit !== undefined) {
            results = results.slice(0, query.limit);
        }

        return results;
    }

    getRelationships(
        entityId: string,
        relationshipType?: RelationshipType,
        direction: 'from' | 'to' | 'both' = 'both'
    ): Relationship[] {
        const relationshipIds = this.relationshipsByEntity.get(entityId);
        if (!relationshipIds) {
            return [];
        }

        return Array.from(relationshipIds)
            .map(id => this.relationships.get(id))
            .filter((rel): rel is Relationship => {
                if (!rel) return false;
                if (relationshipType && rel.type !== relationshipType) return false;
                if (direction === 'from' && rel.from !== entityId) return false;
                if (direction === 'to' && rel.to !== entityId) return false;
                return true;
            });
    }

    addRelationship(relationship: Relationship): void {
        this.relationships.set(relationship.id, relationship);
        
        // Indeksuj relacje po encjach
        [relationship.from, relationship.to].forEach(entityId => {
            if (!this.relationshipsByEntity.has(entityId)) {
                this.relationshipsByEntity.set(entityId, new Set());
            }
            this.relationshipsByEntity.get(entityId)!.add(relationship.id);
        });
    }

    removeRelationship(relationshipId: string): void {
        const relationship = this.relationships.get(relationshipId);
        if (!relationship) {
            return;
        }

        this.relationships.delete(relationshipId);
        [relationship.from, relationship.to].forEach(entityId => {
            this.relationshipsByEntity.get(entityId)?.delete(relationshipId);
        });
    }

    getRelatedEntities(
        entityId: string,
        relationshipType?: RelationshipType,
        targetType?: EntityType
    ): OntologyEntity[] {
        const relationships = this.getRelationships(entityId, relationshipType, 'both');
        const relatedIds = new Set<string>();

        relationships.forEach(rel => {
            if (rel.from === entityId) {
                relatedIds.add(rel.to);
            } else if (rel.to === entityId) {
                relatedIds.add(rel.from);
            }
        });

        return Array.from(relatedIds)
            .map(id => this.getEntity(id))
            .filter((e): e is OntologyEntity => {
                if (!e) return false;
                if (targetType && e.type !== targetType) return false;
                return true;
            });
    }

    hasRelationship(
        fromId: string,
        toId: string,
        relationshipType: RelationshipType
    ): boolean {
        const relationships = this.getRelationships(fromId, relationshipType, 'from');
        return relationships.some(rel => rel.to === toId);
    }

    private evaluateCondition(
        value: PropertyValue | undefined,
        operator: string,
        conditionValue: PropertyValue
    ): boolean {
        if (value === undefined) {
            return operator === 'EXISTS' && conditionValue === false;
        }

        switch (operator) {
            case '=':
                return this.compareValues(value, conditionValue) === 0;
            case '!=':
                return this.compareValues(value, conditionValue) !== 0;
            case '>':
                return this.compareValues(value, conditionValue) > 0;
            case '<':
                return this.compareValues(value, conditionValue) < 0;
            case '>=':
                return this.compareValues(value, conditionValue) >= 0;
            case '<=':
                return this.compareValues(value, conditionValue) <= 0;
            case 'IN':
                return Array.isArray(conditionValue) && conditionValue.includes(value);
            case 'NOT_IN':
                return Array.isArray(conditionValue) && !conditionValue.includes(value);
            case 'LIKE':
                if (typeof value === 'string' && typeof conditionValue === 'string') {
                    const pattern = new RegExp(conditionValue.replace(/%/g, '.*'));
                    return pattern.test(value);
                }
                return false;
            case 'EXISTS':
                return conditionValue === true;
            default:
                return false;
        }
    }

    private compareValues(a: PropertyValue, b: PropertyValue): number {
        if (a === b) return 0;
        if (a === null || a === undefined) return -1;
        if (b === null || b === undefined) return 1;
        
        if (typeof a === 'number' && typeof b === 'number') {
            return a - b;
        }
        
        if (typeof a === 'string' && typeof b === 'string') {
            return a.localeCompare(b);
        }
        
        return String(a).localeCompare(String(b));
    }
}

