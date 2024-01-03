import { User } from './user';

export interface Chat {
    id?: string;
    users: User[];
    _id?: string;
    messages: ChatMessage[];
    unread?: boolean;
    name?: string;
    private: boolean;
    mutedUsers?: string[];
}
export interface ChatMessage {
    chatId?: string;
    sender: string;
    message: string;
    timestamp: Date;
    type: string;
}
