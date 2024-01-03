import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { UserDocument } from '@app/model/database-schema/user.schema';
import { UserRanking } from '@common/interfaces/user';
import { FriendRequest } from '@common/interfaces/friend-request';
import { ChatDocument } from '@app/model/database-schema/chat.schema';
import * as bcrypt from 'bcrypt';
const CHAT_PRINCIPAL_ID = '6565fecd98b4ea695ffe3197';

@Injectable()
export class UsersService {
    constructor(
        @InjectModel(UserDocument.name) public userModel: Model<UserDocument>,
        @InjectModel(ChatDocument.name) private chatModel: Model<ChatDocument>,
    ) {}

    async insertUser(userName: string, password: string, email: string) {
        const username = userName.toLowerCase();
        // Check if username already exists in the database
        const newUser = new this.userModel({
            username,
            password,
            email,
        });
        await newUser.save();
        return newUser;
    }
    async getUserbyId(userId: string): Promise<UserDocument> {
        const user = await this.userModel.findById(userId);
        return user;
    }
    async getUserByUsername(username: string) {
        const user = this.userModel.findOne({ username });
        return user;
    }
    async getUsernameByEmail(email: string): Promise<UserDocument> {
        // Check if email already exists in the database
        // eslint-disable-next-line no-useless-escape
        const user = this.userModel.findOne({ email });
        return user;
    }
    async generatePasswordResetToken(email: string): Promise<string> {
        const token = Math.random().toString(36).slice(-8);
        const result = await this.userModel.findOne({ email });
        if (!result) {
            throw new NotFoundException('User not found');
        }
        result.token = token;
        await result.save();
        return token;
    }
    async changePasswordWithToken(token: string, newPassword: string): Promise<boolean> {
        try {
            const saltOrRounds = 10;
            const hashedPassword = await bcrypt.hash(newPassword, saltOrRounds);
            const user = await this.userModel.findOneAndUpdate({ token }, { password: hashedPassword });
            if (!user) {
                throw new NotFoundException('User not found');
            }
            return true;
        } catch (error) {
            console.error('Error changing password with token:', error);
            return false;
        }
    }

    async addUserToChatPrincipal(userId: string): Promise<ChatDocument> {
        return await this.chatModel.findByIdAndUpdate(CHAT_PRINCIPAL_ID, { $push: { userIds: userId } }, { new: true });
    }
    async changeUsername(userId: string, newUsername: string): Promise<boolean> {
        try {
            const username = newUsername.toLowerCase();

            // Check if new username already exists in the database
            const existingUser = await this.userModel.findOne({ username });
            if (existingUser) {
                throw new Error('Username is already taken');
            }

            // Update the username
            const result = await this.userModel.findByIdAndUpdate(userId, { username }, { new: true });
            return Boolean(result);
        } catch (error) {
            console.error('Error changing username:', error.message);
            return false;
        }
    }

    async changePassword(userId: string, hashedPassword: string): Promise<boolean> {
        try {
            const result = await this.userModel.findByIdAndUpdate(userId, { password: hashedPassword });
            if (result) {
                return true;
            } else {
                return false;
            }
        } catch (error) {
            console.error('Error changing password:', error);
            return false;
        }
    }
    async addPoints(userId: string, points: number): Promise<boolean> {
        try {
            const result = await this.userModel.findByIdAndUpdate(userId, { $inc: { points } });
            if (result) {
                return true;
            } else {
                return false;
            }
        } catch (error) {
            console.error('Error adding points:', error);
            return false;
        }
    }
    async createFriendRequest(friendRequest: FriendRequest): Promise<boolean> {
        try {
            const receiver = await this.userModel.findById(friendRequest.to.userId);
            if (!receiver) {
                throw new NotFoundException('Receiver not found');
            }
            if (!receiver.friendRequests.some((req) => req.from.userId === friendRequest.from.userId)) {
                receiver.friendRequests.push(friendRequest);
                await receiver.save();
                return true;
            }
            return false;
        } catch (error) {
            console.error('Error creating friend request:', error);
            return false;
        }
    }

    async completeFriendRequest(friendRequest: FriendRequest): Promise<boolean> {
        try {
            if (friendRequest.status === 'accepted') {
                const sender = await this.userModel.findById(friendRequest.from.userId);
                const receiver = await this.userModel.findById(friendRequest.to.userId);
                if (!sender || !receiver) {
                    throw new NotFoundException('Sender or receiver not found');
                }
                if (!sender.friends.some((friend) => friend.userId === friendRequest.to.userId)) {
                    sender.friends.push(friendRequest.to);
                    await sender.save();
                }
                if (!receiver.friends.some((friend) => friend.userId === friendRequest.from.userId)) {
                    receiver.friends.push(friendRequest.from);
                    await receiver.save();
                }
                receiver.friendRequests = receiver.friendRequests.filter((req) => req.from.userId !== friendRequest.from.userId);
                await receiver.save();
                return true;
            } else if (friendRequest.status === 'declined') {
                const receiver = await this.userModel.findById(friendRequest.to.userId);
                if (!receiver) {
                    throw new NotFoundException('Receiver not found');
                }
                receiver.friendRequests = receiver.friendRequests.filter((req) => req.from.userId !== friendRequest.from.userId);
                await receiver.save();
                return true;
            }
        } catch (error) {
            console.error('Error completing friend request:', error);
            return false;
        }
    }
    async getRanking(): Promise<UserRanking[]> {
        try {
            const users = await this.userModel.find({}, { username: 1, points: 1, _id: 1 });
            const userRanking: UserRanking[] = [];
            for (const user of users) {
                userRanking.push({
                    username: user.username,
                    // eslint-disable-next-line no-underscore-dangle
                    userId: user._id.toString(),
                    points: user.points,
                });
            }
            return userRanking.sort((a, b) => b.points - a.points);
        } catch (error) {
            return [];
        }
    }
    async getFriendRequests(userId: string): Promise<FriendRequest[]> {
        try {
            const user = await this.userModel.findById(userId);
            if (!user) {
                throw new NotFoundException('User not found');
            }
            return user.friendRequests;
        } catch (error) {
            console.error('Error getting friend requests:', error);
            return [];
        }
    }
}
