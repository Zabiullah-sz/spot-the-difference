/* eslint-disable no-dupe-class-members */
/* eslint-disable no-underscore-dangle */
import { Injectable, OnDestroy } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { ChatPrototypeComponent } from '@app/pages/chat-prototype-page/chat-prototype-page.component';
import { SocketClientService } from '@app/services/divers/socket-client.service';
import { UserService } from '@app/services/user/user.service';
import { WindowService } from '@app/services/window.service';
import { Chat, ChatMessage } from '@common/interfaces/chat';
import { User } from '@common/interfaces/user';
import * as Events from '@common/socket-event-constants';
import { BehaviorSubject, Observable, Subscription, take } from 'rxjs';

@Injectable({
    providedIn: 'root',
})
export class ChatService implements OnDestroy {
    private chatsSubject = new BehaviorSubject<Chat[]>([]);
    private activeChatSubject = new BehaviorSubject<Chat | null>(null);
    private inSeperateWindowSubject = new BehaviorSubject<boolean>(false);
    chats$: Observable<Chat[]> = this.chatsSubject.asObservable();
    activeChat$: Observable<Chat | null> = this.activeChatSubject.asObservable();
    inSeperateWindow$: Observable<boolean> = this.inSeperateWindowSubject.asObservable();

    private mutedUsers: string[] = [];

    private currentUser: User | null = null;
    private subscriptions: Subscription[] = [];
    private winRef: Window | undefined = undefined;

    constructor(
        private socketService: SocketClientService,
        private userService: UserService,
        private snackBar: MatSnackBar,
        private windowService: WindowService,
        private router: Router,
    ) {}

    init(): void {
        //this.setupSocketListeners();
        this.subscriptions.push(
            this.userService.currentUser$.subscribe((user) => {
                if (user) {
                    this.currentUser = user;
                    this.refreshSocketListeners();
                }
            }),
        );
    }
    setupSocketListeners(): void {
        // Setup all your socket listeners here
        this.socketService.on(Events.FromServer.LOAD_CHAT, (chat) => {
            this.updateChatList(chat);
        });
        this.socketService.on(Events.FromServer.NOTIFICATION, (notification) => {
            this.openSnackBar(notification.msg);
        });
        this.socketService.on(Events.FromServer.NEW_MESSAGE, (chatMessage) => {
            console.log('new message', chatMessage);
            this.handleNewMessage(chatMessage);
        });
    }

    refreshSocketListeners(): void {
        // Unsubscribe or disconnect previous socket connections
        this.socketService.off(Events.FromServer.LOAD_CHAT);
        this.socketService.off(Events.FromServer.NOTIFICATION);
        this.socketService.off(Events.FromServer.NEW_MESSAGE);
        //clear the chat list
        this.chatsSubject.next([]);
        
        // Resubscribe to the events
        this.setupSocketListeners();

        // Send a message to load chat for the new user
        this.socketService.send(Events.ToServer.LOAD_CHAT);
    }

    sendMessage(newMessage: string): void {
        if (newMessage.trim() && this.currentUser) {
            const activeChat = this.activeChatSubject.value;
            if (activeChat) {
                this.socketService.send(Events.ToServer.SEND_MESSAGE, {
                    chatId: activeChat._id,
                    message: newMessage,
                    sender: this.currentUser.userId,
                    type: 'text',
                });
            }
        }
    }

    toggleMute(isMuted: boolean): void {
        const activeChat = this.activeChatSubject.value;
        if (!this.currentUser || !activeChat) return;
        this.socketService.send(Events.ToServer.TOGGLE_MUTE, {
            isMuted,
            chatId: activeChat._id,
            userId: this.currentUser.userId,
        });
    }

    sendVoiceMessage(audioBlob: Blob) {
        if (this.currentUser) {
            const activeChat = this.activeChatSubject.value;
            if (activeChat) {
                const reader = new FileReader();
                reader.readAsArrayBuffer(audioBlob);
                reader.onloadend = () => {
                    const audioBuffer = reader.result as ArrayBuffer;
                    this.socketService.send(Events.ToServer.SEND_VOICE_MESSAGE, {
                        chatId: activeChat._id,
                        message: audioBuffer,
                        sender: this.currentUser?.userId,
                    });
                };
            }
        }
    }

    createChat(usernames: string[], name: string): void {
        if (this.currentUser) {
            usernames.push(this.currentUser.username);
            this.socketService.send(Events.ToServer.CREATE_CHAT, { usernames, name });
            this.socketService.send(Events.ToServer.LOAD_CHAT);
        }
    }

    deleteChat(chat: Chat): void {
        this.socketService.send(Events.ToServer.REMOVE_CHAT, { chatId: chat._id });
        const currentChats = this.chatsSubject.value;
        const updatedChats = currentChats.filter((c) => c._id !== chat._id);
        this.chatsSubject.next(updatedChats);
    }

    getAllChats(name: string) {
        this.socketService.send(Events.ToServer.GET_ALL_CHATS, { name });
    }

    receiveAllChats(): Observable<Chat[]> {
        return new Observable((observer) => {
            this.socketService.on(Events.FromServer.ALL_CHATS, (data) => {
                observer.next(data);
            });
        });
    }

    setActiveChat(chat: Chat | null): void {
        if (chat) chat.unread = false;
        this.activeChatSubject.next(chat);
    }

    joinChat(chat: Chat): void {
        this.socketService.send(Events.ToServer.JOIN_CHAT, { chatId: chat._id });
    }

    detachWindow(): void {
        this.router.navigate(['/home']);
        this.winRef = this.windowService.openComponentInNewWindow(ChatPrototypeComponent);
        this.inSeperateWindowSubject.next(true);
    }

    attachWindow(): void {
        this.winRef?.close();
        this.router.navigate(['/chat-prototype']);
        this.inSeperateWindowSubject.next(false);
    }

    isUserMuted(userId: string): boolean {
        return this.mutedUsers.includes(userId);
    }

    unmuteUser(chat: Chat, userIdToUnmute: string): void {
        if (this.currentUser && chat) {
            this.socketService.send(Events.ToServer.UNMUTE_USER, {
                chatId: chat._id,
                userIdToUnmute,
            });

            // Retire l'utilisateur de la liste des utilisateurs muets
            this.mutedUsers = this.mutedUsers.filter((userId) => userId !== userIdToUnmute);
        }
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach((sub) => sub.unsubscribe());
    }

    private sortMessagesByTimestamp(messages: ChatMessage[]): ChatMessage[] {
        return messages.sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());
    }

    private openSnackBar(message: string): void {
        const audio = new Audio('../../assets/notif.mp3');
        audio.play();
        this.snackBar.open(message, 'Close', {
            duration: 3000,
            horizontalPosition: 'center',
            verticalPosition: 'top',
        });
    }

    private updateChatList(chat: Chat): void {
        const currentChats = this.chatsSubject.value;
        if (!currentChats.some((c) => c._id === chat._id)) {
            chat.messages = this.sortMessagesByTimestamp(chat.messages);
            const updatedChats = [...currentChats, chat];
            this.chatsSubject.next(updatedChats);
        }
    }

    private handleNewMessage(chatMessage: ChatMessage): void {
        this.chats$.pipe(take(1)).subscribe((chats) => {
            const chat = chats.find((c) => c._id === chatMessage.chatId);
            if (chat) {
                chat.messages.push(chatMessage);
                chat.messages = this.sortMessagesByTimestamp(chat.messages);
                this.handleUnreadMessage(chat);
            } else {
                this.socketService.send(Events.ToServer.LOAD_CHAT);
            }
        });
    }

    private handleUnreadMessage(chat: Chat): void {
        const activeChat = this.activeChatSubject.value;
        if (!activeChat || chat._id !== activeChat._id) {
            this.openSnackBar('New message!');
            chat.unread = true;
        }
    }
}
