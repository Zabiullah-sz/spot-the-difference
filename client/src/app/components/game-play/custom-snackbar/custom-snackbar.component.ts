import { Component, Inject } from '@angular/core';
import { MAT_SNACK_BAR_DATA, MatSnackBarRef } from '@angular/material/snack-bar';

@Component({
    selector: 'app-custom-snackbar',
    templateUrl: './custom-snackbar.component.html',
    styleUrls: ['./custom-snackbar.component.scss'],
})
export class CustomSnackbarComponent {
    constructor(@Inject(MAT_SNACK_BAR_DATA) public data: any, private snackBarRef: MatSnackBarRef<CustomSnackbarComponent>) {}

    onVote(vote: 'yes' | 'no') {
        // Implement your vote handling logic here
        this.data.onVote(vote);
        this.snackBarRef.dismissWithAction();
    }
}
