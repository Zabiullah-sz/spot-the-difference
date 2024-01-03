// user return by a nestjs controller
export interface User {
    username: string;
    userId: string;
    muted?: boolean;
}
export interface UserRanking {
    username: string;
    userId: string;
    points: number;
}
