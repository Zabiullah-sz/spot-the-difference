import { Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { UserService } from '@app/services/user/user.service';
import { Observable, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { LoginResponse } from '@common/interfaces/http/login';
@Component({
    selector: 'app-signup',
    templateUrl: './signup.component.html',
    styleUrls: ['./signup.component.scss'],
})
export class SignupComponent implements OnDestroy, OnInit {
    @ViewChild('fileInput') fileInput: ElementRef;
    username: string = '';
    password: string = '';
    email: string = '';
    errorMessage: string;
    imageHasBeenSelected: boolean = false;
    file: File | undefined;
    // constructor(private userService: UserService) {}
    loginResponse$: Observable<LoginResponse>;
    selectedImage: string;

    private ngUnsubscribe = new Subject<void>();

    constructor(private userService: UserService, private router: Router) {}
    ngOnInit(): void {
        this.userService.currentUser$.subscribe((user) => {
            if (user) this.router.navigate(['/home']);
        });
    }

    onSubmit() {
        if (!this.username || !this.password || !this.email) {
            this.errorMessage = 'Username, password and email are required.';
            return;
        }
        if (!this.imageHasBeenSelected) {
            this.errorMessage = 'Please select an image.';
            return;
        }

        this.userService.signup(this.username, this.password, this.email).subscribe(
            (response) => {
                this.loginResponse$ = this.userService.login(this.username, this.password);
                this.loginResponse$.pipe(takeUntil(this.ngUnsubscribe)).subscribe(
                    (response) => {
                        console.log('login response', response);
                        if (response) {
                            this.uploadImage(this.file as File, response.user.userId);
                            this.router.navigate(['/home']);
                        }
                    },
                    (error) => {
                        console.log('login error', error);
                        this.errorMessage = 'Invalid username or password.';
                    },
                );
            },
            (error) => {
                this.errorMessage = 'Username or email already exists.';
            },
        );

    }
    usePredefinedImage(imagePath: string) {
        // Convert image path to File and call the upload function
        console.log('imagePath', imagePath);
        this.selectedImage = imagePath;
        fetch(imagePath)
            .then(async (response) => response.blob())
            .then((blob) => {
                this.file = new File([blob], 'predefined.jpg', { type: 'image/jpeg' });
                this.imageHasBeenSelected = true;
            });
    }
    removeSelectedImage() {
        this.imageHasBeenSelected = false;
        this.selectedImage = '';
        this.file = undefined;
    }
    goToLogin() {
        this.router.navigate(['/login']);
    }

    onFileSelected(event: Event) {
        const input = event.target as HTMLInputElement;
        if (input.files && input.files.length > 0) {
            const file: File = input.files[0];
            if (file) {
                this.file = file;
                this.imageHasBeenSelected = true;
            }
            if (file) {
                const reader = new FileReader();
                reader.onload = (e: any) => {
                    this.selectedImage = e.target.result; // This will be a data URL
                    this.imageHasBeenSelected = true;
                };
                reader.readAsDataURL(file);
            }
        }
    }

    onDrop(event: DragEvent) {
        event.preventDefault();
        const file: File | undefined = event.dataTransfer?.files[0];
        if (file) {
            this.imageHasBeenSelected = true;
            this.file = file;
        }
    }

    preventDefault(event: DragEvent) {
        event.preventDefault();
    }

    uploadImage(file: File, userId: string) {
        this.userService.uploadProfileImage(file, userId).subscribe(
            (response) => {
                // Handle response here
            },
            (error) => {
                // Handle error here
            },
        );
    }

    ngOnDestroy() {
        this.ngUnsubscribe.next();
        this.ngUnsubscribe.complete();
    }
}
