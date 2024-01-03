/* eslint-disable no-underscore-dangle */
import { Component, OnInit } from '@angular/core';
import { FriendService } from '@app/services/divers/friend.service';
import { UserService } from '@app/services/user/user.service';
import { FriendRequest } from '@common/interfaces/friend-request';
import { User } from '@common/interfaces/user';
import { environment } from 'src/environments/environment';
@Component({
    selector: 'app-configuration',
    templateUrl: './configuration.component.html',
    styleUrls: ['./configuration.component.scss'],
})
export class ConfigurationComponent implements OnInit {
    imageBaseUrl = environment.serverUrl + '/users/get-public-profile-image/';
    // password: string = 'password123';
    language: string = 'english';
    selectedImage: File;
    friendUsername: string = '';
    usernameChanged = false;
    passwordChanged = false;
    imageChanged = false;
    languageChangedFlag = false;
    pendingRequests: FriendRequest[] = [];
    // Updated mock data for activeUsers to include profile pictures
    activeUsers: User[] = [];
    private _username: string = '';
    private _password: string = '';
    private userId: string = '';
    constructor(private friendService: FriendService, private userService: UserService) {}
    get username(): string {
        return this._username;
    }

    get password(): string {
        return this._password;
    }
    set username(value: string) {
        if (this._username !== value) {
            this.usernameChanged = true;
        }
        this._username = value;
    }

    set password(value: string) {
        if (this._password !== value) {
            this.passwordChanged = true;
        }
        this._password = value;
    }

    ngOnInit() {
        this.friendService.allActiveUsers$.subscribe((users) => {
            this.activeUsers = users;
        });
        this.userService.currentUser$.subscribe((user) => {
            this._username = user?.username || '';
            this._password = 'giberish';
            this.usernameChanged = false;
            this.passwordChanged = false;
            this.userId = user?.userId || '';
        });
        this.userService.getFriendRequests().subscribe((requests) => {
            this.pendingRequests = requests;
        });
    }

    /* eslint-disable @typescript-eslint/no-explicit-any */
    profilePictureChanged(event: any): void {
        const file = event.target.files[0];
        if (file) {
            this.imageChanged = true;
            // Store the selected file to update later
            this.selectedImage = file;
            this.userService.uploadProfileImage(file, this.userId).subscribe((result) => {
                if (result) {
                    console.log('Username updated successfully');
                } else {
                    console.log('Username update failed');
                }
            });
        }
    }

    languageChanged(): void {
        this.languageChangedFlag = true;
    }
    updateUsername(): void {
        this.usernameChanged = false;
        this.userService.updateUsername(this._username).subscribe((result) => {
            if (result) {
                console.log('Username updated successfully');
            } else {
                console.log('Username update failed');
            }
        });
    }

    updatePassword(): void {
        this.passwordChanged = false;
        this.userService.updatePassword(this._password).subscribe((result) => {
            if (result) {
                console.log('Username updated successfully');
            } else {
                console.log('Username update failed');
            }
        });
    }

    updateProfilePicture(): void {
        this.imageChanged = false;
        // Implement your logic to upload and save the image here...
    }

    updateLanguage(): void {
        this.languageChangedFlag = false;
    }

    sendRequest(): void {
        this.friendService.sendFriendRequest(this.friendUsername);
        this.friendUsername = '';
    }
    sendRequestToUser(userName: string): void {
        this.friendService.sendFriendRequest(userName);
    }
    getFriendRequests(): void {}
    completeFriendRequest(friendRequest: FriendRequest, status: string): void {
        const friendRequestReponse =
            status === 'accepted'
                ? { status: 'accepted', from: friendRequest.to, to: friendRequest.from }
                : { status: 'declined', from: friendRequest.to, to: friendRequest.from };
        this.friendService.sendFriendRequestResponse(friendRequestReponse);
    }
}
