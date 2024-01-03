// UserService (user.service.ts)

import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { SocketClientService } from '@app/services/divers/socket-client.service';
import { FriendRequest } from '@common/interfaces/friend-request';
import { ChangeSettingResponse } from '@common/interfaces/http/change-setting';
import { LoginResponse } from '@common/interfaces/http/login';
import { UploadReponse } from '@common/interfaces/http/upload';
import { User, UserRanking } from '@common/interfaces/user';
import * as Events from '@common/socket-event-constants';
import { BehaviorSubject, Observable } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { environment } from 'src/environments/environment';

@Injectable({
    providedIn: 'root',
})
export class UserService {
    currentUserSubject: BehaviorSubject<User | null> = new BehaviorSubject<User | null>(null);
    currentUser$ = this.currentUserSubject.asObservable();
    canJoinClassic: boolean;
    private validatedUser = false;
    private baseUrl: string = environment.serverUrl + '/users';

    activeUsersSubject: BehaviorSubject<User[]> = new BehaviorSubject<User[]>([]);
    activeUsers$ = this.activeUsersSubject.asObservable();

    constructor(private http: HttpClient, private socketService: SocketClientService, private router: Router, private snackBar: MatSnackBar) {}

    init() {
        this.socketService.connect();
        this.socketService.on(Events.FromServer.VALIDATION_RESPONSE, (isValid: boolean) => {
            if (!isValid) {
                this.openSnackBar('Session already in use.');
                this.router.navigate(['/login']);
                this.clearSession();
            } else {
                this.validatedUser = true;
                this.router.navigate(['/home']);
            }
        });

        this.socketService.on(Events.FromServer.ALL_ACTIVE_USERS, (activeUsers: User[]) => {
            this.activeUsersSubject.next(activeUsers);
        });
    }

    signup(username: string, password: string, email: string): Observable<unknown> {
        return this.http.post(`${this.baseUrl}/signup`, { username, password, email }, { withCredentials: true });
    }

    openSnackBar(message: string) {
        this.snackBar.open(message, 'Close', {
            duration: 5000,
        });
    }

    login(username: string, password: string): Observable<LoginResponse> {
        console.log('hello');
        return this.http.post<LoginResponse>(`${this.baseUrl}/login`, { username, password }, { withCredentials: true }).pipe(
            tap((response) => {
                setTimeout(() => {
                    this.canJoinClassic = true;
                }, 500);
                this.socketService.send(Events.ToServer.USERNAME_VALIDATION, response.user);
                this.currentUserSubject.next(response.user);
            }),
        );
    }

    uploadProfileImage(file: File, userId: string): Observable<unknown> {
        const formData = new FormData();
        formData.append('profileImage', file);
        formData.append('userId', userId);

        return this.http
            .post<UploadReponse>(`${this.baseUrl}/upload-profile-image`, formData, {
                reportProgress: true,
                withCredentials: true,
            })
            .pipe(
                tap((response) => {
                    if (response.status === 'success') {
                        this.openSnackBar('Profile changé avec succès!');
                    } else if (response.status === 'error') {
                        this.openSnackBar('Échec du changement de profil!');
                    }
                }),
                catchError((error) => {
                    console.error('Error uploading the profile image: ', error);
                    throw error;
                }),
            );
    }

    updateUsername(username: string): Observable<ChangeSettingResponse> {
        const userId = this.currentUserSubject.value?.userId;
        const body = { userId, newUsername: username };
        return this.http.post<ChangeSettingResponse>(`${this.baseUrl}/change-username`, body, { withCredentials: true }).pipe(
            tap((response) => {
                if (response['status'] === 'error') {
                    this.openSnackBar("Échec du changement de nom d'utilisateur!");
                } else if (response['status'] === 'success') {
                    this.openSnackBar("Nom d'utilisateur changé avec succès!");
                    if (this.currentUserSubject.value) this.currentUserSubject.value.username = username;
                    this.socketService.send(Events.ToServer.UPDATE_USERNAME, { userId: this.currentUserSubject.value?.userId, username });
                }
            }),
        );
    }

    updatePassword(password: string): Observable<ChangeSettingResponse> {
        const userId = this.currentUserSubject.value?.userId;
        const body = { userId, newPassword: password };
        return this.http.post<ChangeSettingResponse>(`${this.baseUrl}/change-password`, body, { withCredentials: true }).pipe(
            tap((response) => {
                if (response['status'] === 'error') {
                    this.openSnackBar('Échec du changement de mot de passe!');
                } else if (response['status'] === 'success') {
                    this.openSnackBar('Mot de passe changé avec succès!');
                }
            }),
        );
    }

    clearSession() {
        this.currentUserSubject.next(null);
        this.validatedUser = false;
    }

    logout(): Observable<unknown> {
        return this.http.get(`${this.baseUrl}/logout`, {}).pipe(
            tap(() => {
                // update the user list
                this.activeUsersSubject.next([]);
                this.clearSession();
                this.router.navigate(['/login']);
                this.socketService.disconnect();
                this.socketService.connect();
            }),
        );
    }

    getRanking(): Observable<UserRanking[]> {
        return this.http.get<UserRanking[]>(`${this.baseUrl}/ranking`, { withCredentials: true });
    }

    hasValidated(): boolean {
        return this.validatedUser;
    }

    getActiveUsers(): Observable<User[]> {
        return this.activeUsersSubject.asObservable();
    }

    muteUser(userId: string): Observable<void> {
        return this.http.post<void>(`${this.baseUrl}/mute-user`, { userId }, { withCredentials: true });
    }

    unmuteUser(userId: string): Observable<void> {
        return this.http.post<void>(`${this.baseUrl}/unmute-user`, { userId }, { withCredentials: true });
    }
    getFriendRequests(): Observable<FriendRequest[]> {
        const paramUserID = this.currentUserSubject.value?.userId;
        return this.http.get<FriendRequest[]>(`${this.baseUrl}/friend-requests/${paramUserID}`, { withCredentials: true });
    }
}
