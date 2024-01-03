import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { UserService } from '@app/services/user/user.service';
import { Observable, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { LoginResponse } from '@common/interfaces/http/login';
@Component({
    selector: 'app-login',
    templateUrl: './login.component.html',
    styleUrls: ['./login.component.scss'],
})
export class LoginComponent implements OnDestroy, OnInit {
    username: string = '';
    password: string = '';
    errorMessage: string;

    // constructor(private userService: UserService) {}
    loginResponse$: Observable<LoginResponse>;

    private ngUnsubscribe = new Subject<void>();

    constructor(private userService: UserService, private router: Router) {}
    ngOnInit(): void {
        this.userService.currentUser$.subscribe((user) => {
            if (user) this.router.navigate(['/home']);
        });
    }

    onSubmit() {
        this.userService.canJoinClassic = false
        this.loginResponse$ = this.userService.login(this.username, this.password);
        this.loginResponse$.pipe(takeUntil(this.ngUnsubscribe)).subscribe(
            (response) => {
                if (response) {
                    //console.log('login response', response);
                    //this.router.navigate(['/home']);
                }
            },
            (error) => {
                console.log('login error', error);
                this.errorMessage = 'Invalid username or password.';
            },
        );
    }

    ngOnDestroy() {
        this.ngUnsubscribe.next();
        this.ngUnsubscribe.complete();
    }
    goToSignup() {
        this.router.navigate(['/signup']);
    }
}
