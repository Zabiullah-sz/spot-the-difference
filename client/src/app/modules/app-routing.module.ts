import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ConfigurationComponent } from '@app/components/configuration/configuration.component';
import { ChatboxComponent } from '@app/components/game-play/chatbox/chatbox.component';
// import { ChatPrototypeComponent } from '@app/pages/chat-prototype-page/chat-prototype-page.component';
import { TutorialComponent } from '@app/components/tutorial/tutorial.component';
import { LoginComponent } from '@app/pages/auth-page/login/login.component';
import { SignupComponent } from '@app/pages/auth-page/signup/signup.component';
import { ChatPrototypeComponent } from '@app/pages/chat-prototype-page/chat-prototype-page.component';
import { ClassicSelectionPageComponent } from '@app/pages/classic-selection-page/classic-selection-page.component';
import { ConfigPageComponent } from '@app/pages/config-page/config-page.component';
import { GameCreationPageComponent } from '@app/pages/game-creation-page/game-creation-page.component';
import { GamePageComponent } from '@app/pages/game-page/game-page.component';
import { MainPageComponent } from '@app/pages/main-page/main-page.component';
import { AuthGuard } from '@app/services/user/auth.guard';
// import { SignupComponent } from '@app/pages/auth-page/signup/signup.component';
// import { SignupComponent } from '@app/pages/auth-page/signup/signup.component';
import { ObserverGamePageComponent } from '@app/pages/observer-game-page/observer-game-page.component';
import { ObserverComponent } from '@app/pages/observer/observer.component';
import { StatComponent } from '@app/components/stat/stat.component';

const routes: Routes = [
    { path: '', redirectTo: '/home', pathMatch: 'full' },
    { path: 'home', component: MainPageComponent, canActivate: [AuthGuard] },
    { path: 'classic', component: ClassicSelectionPageComponent, canActivate: [AuthGuard] },
    { path: 'chat', component: ChatboxComponent, canActivate: [AuthGuard] },
    { path: 'config', component: ConfigPageComponent, canActivate: [AuthGuard] },
    { path: 'game', component: GamePageComponent, canActivate: [AuthGuard] },
    { path: 'creation', component: GameCreationPageComponent, canActivate: [AuthGuard] },
    { path: 'configuration', component: ConfigurationComponent, canActivate: [AuthGuard] },
    { path: 'login', component: LoginComponent },
    { path: 'signup', component: SignupComponent },
    { path: 'chat-prototype', component: ChatPrototypeComponent, canActivate: [AuthGuard] },
    { path: 'tutorial', component: TutorialComponent },
    { path: 'chat-main', component: ChatPrototypeComponent, canActivate: [AuthGuard] },
    { path: 'observer', component: ObserverComponent, canActivate: [AuthGuard] },
    { path: 'observer-game', component: ObserverGamePageComponent, canActivate: [AuthGuard] },
    { path: 'stat', component: StatComponent, canActivate: [AuthGuard] },
];

@NgModule({
    imports: [RouterModule.forRoot(routes, { useHash: true })],
    exports: [RouterModule],
})
export class AppRoutingModule {}
