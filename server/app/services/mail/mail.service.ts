import { MailerService } from '@nestjs-modules/mailer';
import { Injectable } from '@nestjs/common';

@Injectable()
export class MailService {
    constructor(private readonly mailerService: MailerService) {}

    async sendMail(data: { to: string; subject: string; username: string; temporaryPassword: string }) {
        const { to, subject, username, temporaryPassword } = data;
        const htmlContent = `
        <p>Bonjour ${username},</p>
        <p>Pour changer votre mot de passe, veuillez utiliser le token suivant : <strong>${temporaryPassword}</strong></p>
        <p>Si vous n'avez pas demandé de réinitialisation de mot de passe, veuillez ignorer cet email.</p> `;
        const result = await this.mailerService.sendMail({
            to,
            subject,
            html: htmlContent, // Directly using HTML content here
        });
        return result;
    }
}
