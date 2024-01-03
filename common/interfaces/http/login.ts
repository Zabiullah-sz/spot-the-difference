import { User } from '../user';
export interface LoginResponse {
    user: User;
    msg: string;
}