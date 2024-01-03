import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({
    providedIn: 'root',
})
export class ModalService {
    private modalStateSource = new BehaviorSubject<boolean>(false);

    // Observable stream
    // eslint-disable-next-line @typescript-eslint/member-ordering
    modalState$ = this.modalStateSource.asObservable();

    // Method to call when the component is initialized
    setModalInitState(): void {
        this.modalStateSource.next(true);
    }

    // Method to call when the component is destroyed
    setModalDestroyState(): void {
        this.modalStateSource.next(false);
    }
}
