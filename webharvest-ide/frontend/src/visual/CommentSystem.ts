/**
 * CommentSystem - Implementation
 * Node comment system with markdown support
 */

export interface Comment {
    id: string;
    nodeId: string;
    text: string;
    author?: string;
    timestamp: number;
}

export class CommentSystem {
    private comments: Map<string, Comment[]> = new Map();

    addComment(nodeId: string, text: string, author?: string): Comment {
        const comment: Comment = {
            id: `comment-${Date.now()}`,
            nodeId,
            text,
            author,
            timestamp: Date.now()
        };

        if (!this.comments.has(nodeId)) {
            this.comments.set(nodeId, []);
        }

        this.comments.get(nodeId)!.push(comment);
        return comment;
    }

    getComments(nodeId: string): Comment[] {
        return this.comments.get(nodeId) || [];
    }

    deleteComment(commentId: string): boolean {
        for (const [nodeId, comments] of this.comments.entries()) {
            const index = comments.findIndex(c => c.id === commentId);
            if (index !== -1) {
                comments.splice(index, 1);
                return true;
            }
        }
        return false;
    }

    hasComments(nodeId: string): boolean {
        return (this.comments.get(nodeId)?.length || 0) > 0;
    }

    clear(): void {
        this.comments.clear();
    }
}


