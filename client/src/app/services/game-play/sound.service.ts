/* eslint-disable max-params */
import { Injectable } from '@angular/core';
import { Speed } from '@common/enums/game-play/speed';

@Injectable({
    providedIn: 'root',
})
export class SoundService {
    private playSpeed: Speed;
    private successSounds: HTMLAudioElement[] = [];
    private errorSounds: HTMLAudioElement[] = [];
    private selectedSuccessSoundIndex: number = 0;
    private selectedErrorSoundIndex: number = 0;

    constructor() {
        for (let i = 0; i < 2; i++) {
            this.successSounds[i] = new Audio();
            this.successSounds[i].volume = 0.2;
            this.successSounds[i].src = `./assets/success${i === 0 ? '' : i}.mp3`;

            this.errorSounds[i] = new Audio();
            this.errorSounds[i].volume = 0.2;
            this.errorSounds[i].src = `./assets/error${i === 0 ? '' : i}.wav`;
        }

        this.speed = Speed.NORMAL;
    }

    set speed(speed: Speed) {
        this.playSpeed = speed;
        this.successSounds.forEach((sound) => (sound.playbackRate = speed));
        this.errorSounds.forEach((sound) => (sound.playbackRate = speed));
    }

    setSelectedSuccessSound(index: number) {
        if (index >= 0 && index < this.successSounds.length) {
            this.selectedSuccessSoundIndex = index;
        }
    }

    setSelectedErrorSound(index: number) {
        if (index >= 0 && index < this.errorSounds.length) {
            this.selectedErrorSoundIndex = index;
        }
    }

    playSuccess(): void {
        this.playSound(this.successSounds[this.selectedSuccessSoundIndex]);
    }

    playError(): void {
        this.playSound(this.errorSounds[this.selectedErrorSoundIndex]);
    }

    pause() {
        this.successSounds.forEach((sound) => sound.pause());
        this.errorSounds.forEach((sound) => sound.pause());
    }

    resume() {
        this.successSounds.forEach((sound) => {
            if (this.isPlaying(sound)) sound.play();
        });
        this.errorSounds.forEach((sound) => {
            if (this.isPlaying(sound)) sound.play();
        });
    }

    private isPlaying(sound: HTMLAudioElement): boolean {
        return !sound.ended && sound.currentTime !== 0;
    }

    private playSound(sound: HTMLAudioElement): void {
        sound.load();
        sound.playbackRate = this.playSpeed;
        sound.oncanplaythrough = () => {
            sound.play();
            sound.oncanplaythrough = null; // Remove the event listener after playing
        };
    }
}
