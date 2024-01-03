import { User } from './user';
export interface FriendRequest { 
    from: User;
    to: User;
    status: string;
}