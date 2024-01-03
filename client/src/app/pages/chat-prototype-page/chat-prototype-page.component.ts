/* eslint-disable @typescript-eslint/member-ordering */
import { AfterViewChecked, Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { ChatService } from '@app/services/divers/chat.service';
import { UserService } from '@app/services/user/user.service';
import { Chat } from '@common/interfaces/chat';
import { User } from '@common/interfaces/user';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { AddChatDialogComponent } from './add-chat-dialog.component';
import { environment } from 'src/environments/environment';

@Component({
    selector: 'app-chat-prototype-page',
    templateUrl: './chat-prototype-page.component.html',
    styleUrls: ['./chat-prototype-page.component.scss'],
})
export class ChatPrototypeComponent implements OnInit, AfterViewChecked {
    imageBaseUrl = environment.serverUrl + '/users/get-public-profile-image/';
    voiceMessageBaseUrl = environment.serverUrl + '/users/voice-message/';
    @ViewChild('msgContainer') private msgContainer: ElementRef;
    isRecording = false;
    chats: Chat[] = [];
    activeChat: Chat | null = null;
    newMessage = '';
    currentUser: User | null = null;
    allActiveUser: User[] = [];
    isDetached = false;
    allChats: Chat[] = [];
    searchControl = new FormControl();
    private mediaRecorder: MediaRecorder | null = null;
    private stream: MediaStream | null = null;
    private audioChunks: Blob[] = [];

    // Propriété pour la couleur de fond du chat
    chatBackgroundColor: string = 'red';

    constructor(private chatService: ChatService, private dialog: MatDialog, private userService: UserService, private router: Router) {}

    ngOnInit(): void {
        this.chatService.chats$.subscribe((chats: Chat[]) => {
            for (const chat of chats) {
                if (chat.private) {
                    // find the other user
                    const otherUser = chat.users.find((user) => user.userId !== this.currentUser?.userId);
                    if (otherUser) {
                        chat.name = otherUser.username;
                        chat.users = chat.users.filter((user) => user.userId === this.currentUser?.userId);
                        chat.users.unshift(otherUser);
                    }
                }
            }
            this.chats = chats;
        });
        this.chatService.activeChat$.subscribe((activeChat: Chat | null) => {
            this.activeChat = activeChat;
        });
        this.userService.currentUser$.subscribe((user) => {
            console.log('user changed')
            console.log("Current user : ", user)
            this.currentUser = user;
        });
        this.chatService.inSeperateWindow$.subscribe((state) => {
            this.isDetached = state;
        });
        this.chatService.getAllChats('');

        // Écouter les changements de la recherche
        this.searchControl.valueChanges
            .pipe(
                debounceTime(400), // Attendre 400 ms entre les frappes
                distinctUntilChanged(), // Ne pas faire d'appel API si le texte de recherche n'a pas changé depuis la dernière frappe
            )
            .subscribe((searchText) => {
                this.chatService.getAllChats(searchText);
            });

        // S'abonner pour recevoir tous les chats
        this.chatService.receiveAllChats().subscribe((data) => {
            this.allChats = data;
        });
        // S'abonner pour recevoir tous les utilisateurs actifs
        this.userService.getActiveUsers().subscribe((users) => {
            this.allActiveUser = users;
        });
    }

    // Méthode pour changer la couleur de fond du chat
    changeChatBackgroundColor() {
        // Mettre à jour la couleur de fond de la zone de chat avec la couleur sélectionnée
        const chatContainer = this.msgContainer.nativeElement as HTMLElement;
        chatContainer.style.backgroundColor = this.chatBackgroundColor;
    }

    detachWindow() {
        this.chatService.detachWindow();
    }

    close() {
        this.chatService.attachWindow();
    }

    goToHome() {
        this.router.navigate(['/home']);
    }

    joinChat(chat: Chat) {
        this.chatService.joinChat(chat);
    }

    ngAfterViewChecked(): void {
        this.scrollToBottom();
    }

    createChat(usernames: string[], name: string) {
        this.chatService.createChat(usernames, name);
    }

    deleteChat(chat: Chat) {
        if (this.activeChat?._id === chat._id) {
            this.chatService.setActiveChat(null);
        }
        this.chatService.deleteChat(chat);
    }

    selectChat(chat: Chat) {
        this.chatService.setActiveChat(chat);
    }

    openAddChatDialog() {
        const dialogRef = this.dialog.open(AddChatDialogComponent, {
            width: '300px',
        });
        dialogRef.afterClosed().subscribe((result) => {
            if (result) this.chatService.createChat(result.users, result.name);
        });
    }

    confirmChatBackgroundColor() {
        const chatContainer = this.msgContainer.nativeElement as HTMLElement;
        chatContainer.style.backgroundColor = this.chatBackgroundColor;
        console.log('Couleur de fond confirmée :', this.chatBackgroundColor);
    }

    sendMessage() {
        const message = this.newMessage;
        if (this.currentUser && !this.isUserMuted(this.currentUser)) {
            this.chatService.sendMessage(message); // Envoie le message au service
        } else {
            console.log("L'utilisateur est muté. Message non affiché.");
        }

        this.newMessage = '';
    }

    startRecording() {
        this.isRecording = true;
        navigator.mediaDevices
            .getUserMedia({ audio: true })
            .then((stream) => {
                this.mediaRecorder = new MediaRecorder(stream);
                this.stream = stream; // Store the stream
                this.mediaRecorder.start();

                this.audioChunks = []; // Clear previous data
                this.mediaRecorder.addEventListener('dataavailable', (event) => {
                    this.audioChunks.push(event.data);
                });

                this.mediaRecorder.addEventListener('stop', this.onRecordingStop);
            })
            .catch((error) => {
                console.error('Could not start recording: ', error);
                this.isRecording = false;
            });
    }

    onRecordingStop = () => {
        const audioBlob = new Blob(this.audioChunks);
        // const audioUrl = URL.createObjectURL(audioBlob);
        // const audio = new Audio(audioUrl);
        /// audio.play();
        this.chatService.sendVoiceMessage(audioBlob);
        // Release the stream
        this.stream?.getTracks().forEach((track) => track.stop());
    };

    stopRecording() {
        if (!this.isRecording || !this.mediaRecorder) return;

        this.mediaRecorder.stop(); // This will trigger the 'stop' event and call `onRecordingStop`
        this.isRecording = false;
        // Do not stop the stream here since it will be stopped in `onRecordingStop`
    }

    private scrollToBottom(): void {
        try {
            this.msgContainer.nativeElement.scrollTop = this.msgContainer.nativeElement.scrollHeight;
        } catch (err) {
            // Ignorer les erreurs silencieuses
        }
    }
    muteUser(user: User) {
        if (user) {
            user.muted = true;
        }
    }

    unmuteUser(user: User) {
        if (user) {
            user.muted = false;
        }
    }

    isUserMuted(user: User | null): boolean {
        return user?.muted || false;
    }
}
