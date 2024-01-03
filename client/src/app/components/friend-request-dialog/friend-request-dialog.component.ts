import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { User } from '@common/interfaces/user';
import { environment } from 'src/environments/environment';
@Component({
    selector: 'app-friend-request-dialog',
    templateUrl: './friend-request-dialog.component.html',
    styleUrls: ['./friend-request-dialog.component.scss'],
})
export class FriendRequestDialogComponent {
    imageBaseUrl = environment.serverUrl + '/users/get-public-profile-image/';
    constructor(public dialogRef: MatDialogRef<FriendRequestDialogComponent>, @Inject(MAT_DIALOG_DATA) public data: { user: User }) {}
    onNoClick(): void {
        this.dialogRef.close('ignore');
    }
    onAccept(): void {
        // Logic for accepting the friend request.
        this.dialogRef.close('accepted');
    }

    onDecline(): void {
        // Logic for declining the friend request.
        this.dialogRef.close('declined');
    }
}
