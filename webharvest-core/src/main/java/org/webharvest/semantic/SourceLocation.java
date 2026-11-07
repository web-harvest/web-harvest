package org.webharvest.semantic;

/** One-based XML source position. Unknown coordinates are represented by -1. */
public final class SourceLocation {
    private final int line;
    private final int column;

    public SourceLocation(int line, int column) {
        this.line = line;
        this.column = column;
    }

    public int getLine() { return line; }
    public int getColumn() { return column; }

    public static SourceLocation unknown() {
        return new SourceLocation(-1, -1);
    }
}
