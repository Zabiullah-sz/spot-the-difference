import { User } from '@common/interfaces/user';

export default class UserSessionService {
    private activeUsers: { clientId: string; user: User }[] = [];

    isUsernameAvailable(clientId: string, user: User): boolean {
        if (user.userId === undefined || user.username.trim() === '') return false;

        const isAvailable = !this.activeUsers.some((activeUser) => activeUser.user.userId === user.userId);
        if (isAvailable) {
            this.activeUsers.push({ clientId, user });
        }
        return isAvailable;
    }
    updateUsername(clientId: string, user: User): boolean {
        // find the user in the activeUsers array
        const index = this.activeUsers.findIndex((activeUser) => activeUser.user.userId === user.userId);
        if (index > -1) {
            this.activeUsers[index].user = user;
        }
        return index > -1;
    }

    removePlayer(clientId: string): void {
        const index = this.activeUsers.findIndex((activeUser) => activeUser.clientId === clientId);
        if (index > -1) {
            this.activeUsers.splice(index, 1);
        }
    }
    getClientIdbyUsername(username: string): string {
        return this.activeUsers.find((activeUser) => activeUser.user.username === username)?.clientId;
    }
    getClientIdbyUserId(userId: string): string {
        return this.activeUsers.find((activeUser) => activeUser.user.userId === userId)?.clientId;
    }

    getUser(clientId: string): User {
        return this.activeUsers.find((activeUser) => activeUser.clientId === clientId)?.user;
    }
    getAllActiveUsers(): User[] {
        return this.activeUsers.map((activeUser) => activeUser.user);
    }
}
