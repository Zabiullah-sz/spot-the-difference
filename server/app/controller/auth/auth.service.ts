import { Injectable, NotAcceptableException } from '@nestjs/common';
import { UsersService } from '@app/controller/users/users.service';
import * as bcrypt from 'bcrypt';

@Injectable()
export class AuthService {
    constructor(private readonly usersService: UsersService) {}
    async validateUser(username: string, password: string): Promise<unknown> {
        const user = await this.usersService.getUserByUsername(username);
        if (!user) {
            throw new NotAcceptableException('could not find the user');
        }
        const passwordValid = await bcrypt.compare(password, user.password);
        if (user && passwordValid) {
            return {
                userId: user.id,
                username: user.username,
            };
        }
        return null;
    }
}
