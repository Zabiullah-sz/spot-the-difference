import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';
import { MatSnackBar, MatSnackBarConfig } from '@angular/material/snack-bar';
import { CustomSnackbarComponent } from '@app/components/game-play/custom-snackbar/custom-snackbar.component';

@Injectable({
    providedIn: 'root',
})
export class SnackbarQueueService {
    cheaterName: string = '';
    private snackbarDismissedSource = new Subject<{ dismissType: 'action' | 'dismiss'; cheaterName: string }>();
    // eslint-disable-next-line @typescript-eslint/member-ordering
    snackbarDismissed$ = this.snackbarDismissedSource.asObservable();
    private snackbarQueue: { config: MatSnackBarConfig; cheaterName: string }[] = [];
    private isSnackbarActive = false;

    constructor(private snackBar: MatSnackBar) {}

    openCustomSnackbar(data: unknown, cheaterName: string) {
        const config: MatSnackBarConfig = {
            data,
            duration: 15000, // Set your own duration
        };

        if (!this.isSnackbarActive) {
            this.cheaterName = cheaterName;
            this.showSnackbar(config, cheaterName);
        } else {
            if (this.cheaterName !== cheaterName) this.snackbarQueue.push({ config, cheaterName });
        }
    }
    private showSnackbar(config: MatSnackBarConfig, cheaterName: string) {
        this.isSnackbarActive = true;
        const snackBarRef = this.snackBar.openFromComponent(CustomSnackbarComponent, config);

        snackBarRef.afterDismissed().subscribe((info) => {
            this.isSnackbarActive = false;
            if (!info.dismissedByAction) {
                this.snackbarDismissedSource.next({ dismissType: 'dismiss', cheaterName });
            } else {
                this.snackbarDismissedSource.next({ dismissType: 'action', cheaterName });
            }
            // Check if the shifted element is not undefined before calling showSnackbar
            const nextItem = this.snackbarQueue.shift();
            if (nextItem !== undefined) {
                this.showSnackbar(nextItem.config, nextItem.cheaterName);
            }
        });
    }
}
