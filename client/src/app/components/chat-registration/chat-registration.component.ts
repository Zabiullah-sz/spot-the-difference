import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
    selector: 'app-chat-registration',
    templateUrl: './chat-registration.component.html',
    styleUrls: ['./chat-registration.component.scss'],
})
export class ChatRegistrationComponent {
    username: string;
    constructor(private router: Router) {}

    checkUsernameAvailability() {
        const isUsernameAvailable = true;

        if (isUsernameAvailable) {
            this.router.navigate(['/chatPage']);
        }
    }
}
