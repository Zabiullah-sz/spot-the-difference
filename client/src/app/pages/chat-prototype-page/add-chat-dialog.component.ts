import { Component } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';

@Component({
    selector: 'app-add-chat-dialog',
    templateUrl: './add-chat-dialog.component.html',
    styleUrls: ['./add-chat-dialog.component.scss'],
})
export class AddChatDialogComponent {
    username = '';
    users: string[] = [];
    name = '';
    // return users on dialog close
    constructor(public dialogRef: MatDialogRef<AddChatDialogComponent>) {}
    onNoClick(): void {
        this.dialogRef.close();
    }
    addTag() {
        if (this.username.trim()) {
            this.users.push(this.username);
            this.username = '';
        }
    }

    createChat() {
        // TODO: deactive button if no users or no name
        this.dialogRef.close({ users: this.users, name: this.name });
    }
    handleKeydown(event: KeyboardEvent) {
        if (event.key === 'Backspace' && !this.username) {
            this.removeLastTag();
        }
    }

    removeLastTag() {
        if (this.users.length) {
            this.users.pop();
        }
    }
}
