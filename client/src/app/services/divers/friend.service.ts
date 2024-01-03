import { Injectable, OnDestroy } from '@angular/core';
import { SocketClientService } from '@app/services/divers/socket-client.service';
import { UserService } from '@app/services/user/user.service';
import { User } from '@common/interfaces/user';
import * as Events from '@common/socket-event-constants';
import { BehaviorSubject, Observable, Subscription } from 'rxjs';

import { MatDialog } from '@angular/material/dialog';
import { FriendRequestDialogComponent } from '@app/components/friend-request-dialog/friend-request-dialog.component';
import { FriendRequest } from '@common/interfaces/friend-request';

@Injectable({
    providedIn: 'root',
})
export class FriendService implements OnDestroy {
    private allActiveUsers = new BehaviorSubject<User[]>([]);
    allActiveUsers$: Observable<User[]> = this.allActiveUsers.asObservable();

    private currentUser: User | null = null;
    private subscriptions: Subscription[] = [];

    constructor(private socketService: SocketClientService, private userService: UserService, private dialog: MatDialog) {}

    init(): void {
        this.socketService.on(Events.FromServer.ALL_ACTIVE_USERS, (allActiveUser) => {
            this.allActiveUsers.next(allActiveUser);
        });

        // User change listener
        this.subscriptions.push(
            this.userService.currentUser$.subscribe((user) => {
                if (user) this.currentUser = user;
                console.log('Current user: ', this.currentUser);
            }),
        );

        // New message listener

        this.socketService.on(Events.FromServer.FRIEND_REQUEST, (friendRequest) => {
            this.handleNewFriendRequest(friendRequest);
        });
    }

    ngOnDestroy(): void {
        // Unsubscribe all subscriptions to prevent memory leaks.
        this.subscriptions.forEach((sub) => sub.unsubscribe());
    }
    handleNewFriendRequest(friendRequest: FriendRequest): void {
        const audio = new Audio('../../assets/notif.mp3');
        audio.play();
        const dialogRef = this.dialog.open(FriendRequestDialogComponent, {
            width: '250px',
            data: { user: friendRequest.from },
        });

        dialogRef.afterClosed().subscribe((result) => {
            if (result) {
                if (result === 'accepted') {
                    friendRequest.status = 'accepted';
                }
                if (result === 'declined') {
                    friendRequest.status = 'declined';
                }

                this.socketService.send(Events.ToServer.FRIEND_REQUEST_RESPONSE, {
                    friendRequest,
                });
            }
        });
    }
    sendFriendRequest(username: string): void {
        this.socketService.send(Events.ToServer.FRIEND_REQUEST, { username });
    }
    sendFriendRequestResponse(friendRequest: FriendRequest): void {
        this.socketService.send(Events.ToServer.FRIEND_REQUEST_RESPONSE, { friendRequest });
    }
}
