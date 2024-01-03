import UserSessionService from '@app/controller/users/user-session.service';
import GameAuthorityService from '@app/services/game-authority/game-authority.service';
import MongoDBService from '@app/services/mongodb/mongodb.service';

import * as Events from '@common/socket-event-constants';
// import { Logger } from '@nestjs/common';
import { SubscribeMessage, WebSocketGateway, WebSocketServer } from '@nestjs/websockets';
import { Server, Socket } from 'socket.io';
import { GATEWAY_PORT } from './game.gateway.constants';
import OutputFilterGateway from './output-filters.gateway';

import { UsersService } from '@app/controller/users/users.service';
import { Chat } from '@common/interfaces/chat';
import { FriendRequest } from '@common/interfaces/friend-request';
import { User } from '@common/interfaces/user';

@WebSocketGateway(GATEWAY_PORT)
export default class FriendGateway {
    @WebSocketServer() server: Server;

    constructor(private mongoDBService: MongoDBService, private userSessionService: UserSessionService, private userService: UsersService) {
        GameAuthorityService.mongoDBService = this.mongoDBService;
        GameAuthorityService.userSessionService = this.userSessionService;
        GameAuthorityService.userService = this.userService;
    }

    @SubscribeMessage(Events.ToServer.FRIEND_REQUEST)
    async handleFriendRequest(client: Socket, payload: { username: string }) {
        if (typeof payload === 'string') {
            payload = JSON.parse(payload);
        }
        const user = this.userSessionService.getUser(client.id);
        const userDoc = await this.userService.getUserbyId(user.userId);
        const from = { userId: userDoc._id.toString(), username: userDoc.username } as User;
        const friend = await this.userService.getUserByUsername(payload.username);
        if (!friend) {
            OutputFilterGateway.sendNotification.toClient(client, { msg: 'User does not exist' });
            return;
        } else {
            const to = { userId: friend._id.toString(), username: friend.username } as User;
            const friendRequestPending = { from, to, status: 'pending' } as FriendRequest;
            await this.userService.createFriendRequest(friendRequestPending);
            const toClientId = this.userSessionService.getClientIdbyUserId(to.userId);
            if (toClientId) {
                const toClient = OutputFilterGateway.server.sockets.sockets.get(toClientId);
                if (toClient) OutputFilterGateway.sendFriendRequest.toClient(toClient, friendRequestPending);
            }
        }
    }
    // receive response from client
    @SubscribeMessage(Events.ToServer.FRIEND_REQUEST_RESPONSE)
    async handleFriendRequestResponse(client: Socket, payload: { friendRequest: FriendRequest }) {
        if (typeof payload === 'string') {
            payload = JSON.parse(payload);
        }
        this.userService.completeFriendRequest(payload.friendRequest);
        const toClientId = this.userSessionService.getClientIdbyUserId(payload.friendRequest.to.userId);
        const toClient = OutputFilterGateway.server.sockets.sockets.get(toClientId);
        const fromClientId = this.userSessionService.getClientIdbyUserId(payload.friendRequest.from.userId);
        const fromClient = OutputFilterGateway.server.sockets.sockets.get(fromClientId);

        if (payload.friendRequest.status === 'accepted') {
            try {
                const users: string[] = [payload.friendRequest.from.userId, payload.friendRequest.to.userId];
                const chat = await this.mongoDBService.createPrivateChat(
                    users,
                    payload.friendRequest.from.username + ' - ' + payload.friendRequest.to.username,
                );
                // from chat get userIds and make User[] by calling getUserById to chat to Chat interface
                const chatUsersPromises = chat.userIds.map(async (userId) => {
                    const user = await this.userService.getUserbyId(userId);
                    return { userId, username: user.username } as User;
                });

                const chatUsers = await Promise.all(chatUsersPromises);

                const chatMsg = {
                    chatId: chat._id.toString(),
                    users: chatUsers,
                    messages: chat.messages,
                    private: chat.private,
                } as Chat;

                if (toClient) {
                    OutputFilterGateway.sendNotification.toClient(toClient, {
                        msg: 'Vous êtes maintenant ami avec ' + payload.friendRequest.from.username,
                    });
                    if (chat) OutputFilterGateway.sendLoadChat.toClient(toClient, chatMsg);
                }
                if (fromClient)
                    OutputFilterGateway.sendNotification.toClient(fromClient, {
                        msg: 'Vous êtes maintenant ami avec ' + payload.friendRequest.to.username,
                    });
            } catch (error) {
                console.log(error);
                if (fromClient)
                    OutputFilterGateway.sendNotification.toClient(fromClient, {
                        msg: 'Vous êtes dêjà ami avec ' + payload.friendRequest.to.username,
                    });
                if (toClient)
                    OutputFilterGateway.sendNotification.toClient(toClient, {
                        msg: 'Vous êtes dêjà ami avec  ' + payload.friendRequest.from.username,
                    });
                return false;
            }
        } else {
            if (fromClient)
                OutputFilterGateway.sendNotification.toClient(fromClient, {
                    msg: payload.friendRequest.to.username + " a rejeter votre demande d'ami",
                });
        }
    }

    @SubscribeMessage(Events.ToServer.RETRIEVE_ALL_ACTIVE_USERS)
    usernameAvailability(client: Socket) {
        OutputFilterGateway.sendAllActiveUsers.toClient(client, this.userSessionService.getAllActiveUsers());
    }
}
