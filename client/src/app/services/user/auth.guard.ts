import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree, Router } from '@angular/router';
import { Observable, of } from 'rxjs';
import { UserService } from '@app/services/user/user.service';
import { catchError, map } from 'rxjs/operators';

@Injectable({
    providedIn: 'root',
})
export class AuthGuard implements CanActivate {
    constructor(private userService: UserService, private router: Router) {}

    canActivate(next: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean | UrlTree> {
        return this.userService.currentUser$.pipe(
            map((user) => (user ? true : this.router.parseUrl('/login'))),
            catchError(() => {
                return of(this.router.parseUrl('/login')); // If there's any error, redirect to login
            }),
        );
    }
}
