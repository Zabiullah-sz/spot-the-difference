/* eslint-disable no-underscore-dangle */
import FileSystemManager from '@app/class/diverse/file-system-manager/file-system-manager';
import { AuthenticatedGuard } from '@app/controller/auth/authenticated.guard';
import { LocalAuthGuard } from '@app/controller/auth/local.auth.guard';
import { MailService } from '@app/services/mail/mail.service';
import { FriendRequest } from '@common/interfaces/friend-request';
import { ChangeSettingResponse } from '@common/interfaces/http/change-setting';
import { LoginResponse } from '@common/interfaces/http/login';
import { UserRanking } from '@common/interfaces/user';
import { Body, Controller, Get, Param, Post, Request, Res, UploadedFile, UseGuards, UseInterceptors } from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import * as bcrypt from 'bcrypt';
import { Response } from 'express';
import { UsersService } from './users.service';
@Controller('users')
export class UsersController {
    constructor(private readonly usersService: UsersService, private readonly mailService: MailService) {}
    @Post('/signup')
    async addUser(@Body('password') userPassword: string, @Body('username') userName: string, @Body('email') email: string) {
        const saltOrRounds = 10;
        const hashedPassword = await bcrypt.hash(userPassword, saltOrRounds);
        const result = await this.usersService.insertUser(userName, hashedPassword, email);
        await this.usersService.addUserToChatPrincipal(result._id.toString());

        return {
            msg: 'User successfully registered',
            userId: result.id,
            username: result.username,
        };
    }
    @Post('/forgot-password/:email')
    async forgotPassword(@Param('email') email: string): Promise<unknown> {
        const user = await this.usersService.getUsernameByEmail(email);
        if (!user) {
            return { status: 'error', msg: 'No user found with this email' };
        }
        const token = await this.usersService.generatePasswordResetToken(user.email);
        const result = await this.mailService.sendMail({
            to: email,
            subject: 'Changement de mot de passe',
            username: user.username,
            temporaryPassword: token,
        });
        if (result !== undefined) {
            return { status: 'success', msg: 'Password reset email sent successfully' };
        } else {
            return { status: 'error', msg: 'Failed to send password reset email' };
        }
    }
    @Post('/update-password')
    async updatePassword(@Body('token') token: string, @Body('newPassword') newPassword: string): Promise<unknown> {
        console.log('token', token);
        console.log('newPassword', newPassword);
        const result = await this.usersService.changePasswordWithToken(token, newPassword);
        if (result) {
            return { status: 'success', msg: 'Password updated successfully' };
        } else {
            return { status: 'error', msg: 'Failed to update password' };
        }
    }

    // Post / Login
    @UseGuards(LocalAuthGuard)
    @Post('/login')
    login(@Request() req): LoginResponse {
        return {
            user: { userId: req.user.userId, username: req.user.username },
            msg: 'User logged in',
        };
    }
    // Get / validate
    @UseGuards(AuthenticatedGuard)
    @Get('/validate')
    validate(@Request() req): unknown {
        return req.user;
    }
    // Get / protected
    @UseGuards(AuthenticatedGuard)
    @Get('/protected')
    getHello(@Request() req): string {
        return req.user;
    }
    // Get / logout
    @Get('/logout')
    logout(@Request() req): unknown {
        return { msg: 'The user session has ended' };
    }
    // @UseGuards(AuthenticatedGuard)
    @Post('/upload-profile-image')
    @UseInterceptors(FileInterceptor('profileImage'))
    async uploadProfileImage(@Request() req, @UploadedFile() file): Promise<unknown> {
        if (!file || !file.buffer) {
            return { status: 'error', msg: 'No profile image provided' };
        }

        const userId = req.body.userId;
        console.log('userId', userId);
        const result = FileSystemManager.storeUserProfileImage(userId, file.buffer);
        if (result) {
            return { status: 'success', msg: 'Profile image uploaded successfully' };
        } else {
            return { status: 'error', msg: 'Failed to upload profile image' };
        }
    }

    @UseGuards(AuthenticatedGuard)
    @Get('/get-profile-image')
    getProfileImage(@Request() req, @Res() res: Response): void {
        const imageBuffer = FileSystemManager.getUserProfileImage(req.user.id);
        console.log('imageBuffer', imageBuffer);
        if (imageBuffer) {
            res.setHeader('Content-Type', 'image/jpeg'); // Adjust the MIME type if it's not a JPEG.
            res.send(imageBuffer);
        } else {
            res.status(404).json({ status: 'error', msg: 'Profile image not found' });
        }
    }

    @Get('/get-public-profile-image/:userId')
    getPublicProfileImage(@Param('userId') userId: string, @Res() res: Response): void {
        const imageBuffer = FileSystemManager.getUserProfileImage(userId);
        console.log('imageBuffer', imageBuffer);
        if (imageBuffer) {
            res.setHeader('Content-Type', 'image/jpeg'); // Adjust the MIME type if it's not a JPEG.
            res.send(imageBuffer);
        } else {
            res.status(404).json({ status: 'error', msg: 'Profile image not found' });
        }
    }
    // @UseGuards(AuthenticatedGuard)
    @Post('/change-username')
    async changeUsername(@Body('userId') userId: string, @Body('newUsername') newUsername: string): Promise<ChangeSettingResponse> {
        if (!newUsername) {
            console.log('No new username provided');
            return { status: 'error', msg: 'No new username provided' };
        }
        const result = await this.usersService.changeUsername(userId, newUsername);
        if (result) {
            return { status: 'success', msg: 'Username changed successfully' };
            console.log('Username changed successfully');
        } else {
            return { status: 'error', msg: 'Failed to change username' };
            console.log('Failed to change username');
        }
    }

    // @UseGuards(AuthenticatedGuard)
    @Post('/change-password')
    async changePassword(@Body('userId') userId: string, @Body('newPassword') newPassword: string): Promise<unknown> {
        if (!newPassword) {
            return { status: 'error', msg: 'No new password provided' };
        }
        const saltOrRounds = 10;
        const hashedPassword = await bcrypt.hash(newPassword, saltOrRounds);
        // const userId = req.user.userId; // Assuming user id is attached to the request object
        const result = await this.usersService.changePassword(userId, hashedPassword);

        if (result) {
            return { status: 'success', msg: 'Password changed successfully' };
        } else {
            return { status: 'error', msg: 'Failed to change password' };
        }
    }
    // @UseGuards(AuthenticatedGuard)
    @Get('/ranking')
    async getRanking(): Promise<UserRanking[]> {
        const result = await this.usersService.getRanking();
        if (result) {
            return result;
        } else {
            return [];
        }
    }
    // TODO: MOVE THIS ELSE WHERE
    @Get('voice-message/:messageId')
    async getVoiceMessage(@Param('messageId') messageId: string, @Res() response: Response): Promise<void> {
        const audioBuffer = FileSystemManager.getAudioBuffer(messageId);

        if (audioBuffer) {
            // Set the appropriate content type for .ogg audio files
            response.type('audio/ogg');

            // You can either send the buffer directly
            response.send(audioBuffer);

            // Or you can stream the buffer if it's stored as a file
            // const readStream = createReadStream(audioBuffer.path);
            // response.set('Content-Disposition', `attachment; filename="${messageId}.ogg"`);
            // readStream.pipe(response);
        } else {
            // If the buffer is not found, send an error response
            response.status(404).json({ status: 'error', msg: 'Failed to get voice message' });
        }
    }
    @Get('friend-requests/:userId')
    async getFriendRequests(@Param('userId') userId: string): Promise<FriendRequest[]> {
        const result = await this.usersService.getFriendRequests(userId);
        if (result) {
            return result;
        } else {
            return [];
        }
    }
}
