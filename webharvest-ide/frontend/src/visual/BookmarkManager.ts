/**
 * BookmarkManager - Implementation
 * Bookmark system for quick navigation to nodes
 */

export interface Bookmark {
    id: string;
    nodeId: string;
    label: string;
    timestamp: number;
}

export class BookmarkManager {
    private bookmarks: Map<string, Bookmark> = new Map();

    addBookmark(nodeId: string, label: string): Bookmark {
        const bookmark: Bookmark = {
            id: `bookmark-${Date.now()}`,
            nodeId,
            label,
            timestamp: Date.now()
        };
        this.bookmarks.set(bookmark.id, bookmark);
        return bookmark;
    }

    removeBookmark(bookmarkId: string): boolean {
        return this.bookmarks.delete(bookmarkId);
    }

    getBookmarks(): Bookmark[] {
        return Array.from(this.bookmarks.values());
    }

    getBookmark(bookmarkId: string): Bookmark | null {
        return this.bookmarks.get(bookmarkId) || null;
    }

    clear(): void {
        this.bookmarks.clear();
    }
}


