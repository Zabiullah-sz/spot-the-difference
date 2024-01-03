import { Component, HostListener } from '@angular/core';
import { Router } from '@angular/router';

@Component({
    selector: 'app-tutorial',
    templateUrl: './tutorial.component.html',
    styleUrls: ['./tutorial.component.scss'],
})
export class TutorialComponent {
    images: string[] = [
        'assets/tutorial/image1.png',
        'assets/tutorial/image2.png',
        'assets/tutorial/image3.png',
        'assets/tutorial/image4.png',
        'assets/tutorial/image5.png',
        'assets/tutorial/image6.png',
        'assets/tutorial/image7.png',
        'assets/tutorial/image8.png',
        'assets/tutorial/image9.png',
        'assets/tutorial/image10.png',
        'assets/tutorial/image11.png',
        'assets/tutorial/image12.png',
        'assets/tutorial/image13.png',
        'assets/tutorial/image14.png',
        'assets/tutorial/image15.png',
    ];

    activeSlide: number = 0;
    showPopup: boolean = false;

    constructor(private router: Router) {}
    @HostListener('document:keydown', ['$event'])
    handleKeyboardEvent(event: KeyboardEvent): void {
        if (event.key === 'ArrowLeft') {
            this.prevSlide();
        } else if (event.key === 'ArrowRight') {
            this.nextSlide();
        }
    }
    prevSlide() {
        this.activeSlide = (this.activeSlide - 1 + this.images.length) % this.images.length;
    }

    nextSlide() {
        this.activeSlide = (this.activeSlide + 1) % this.images.length;
    }

    returnToHomePage() {
        this.router.navigate(['/home']);
    }
}
