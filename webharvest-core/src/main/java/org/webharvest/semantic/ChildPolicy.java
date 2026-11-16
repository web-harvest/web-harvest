package org.webharvest.semantic;

/** Structural policy for plugin child elements. */
public enum ChildPolicy {
    /** A closed child allow-list is declared; hard checks require qualified identities. */
    ALLOW_LIST,
    /** Any plugin child is accepted by an explicit authoritative declaration. */
    ANY_CHILD,
    /** No plugin children are accepted; requires an explicit closed-policy producer. */
    FORBID_CHILDREN,
    /** Available metadata cannot establish a safe child policy. */
    UNKNOWN
}
