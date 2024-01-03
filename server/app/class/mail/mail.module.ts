import { MailerModule } from '@nestjs-modules/mailer';
import { HandlebarsAdapter } from '@nestjs-modules/mailer/dist/adapters/handlebars.adapter';
import { Module } from '@nestjs/common';
import { MailService } from '@app/services/mail/mail.service';
import { join } from 'path';

@Module({
    imports: [
        MailerModule.forRoot({
            transport: {
                host: 'smtp.mandrillapp.com',
                port: 587,
                secure: false, // Since we're using port 25, keep secure as false
                auth: {
                    user: "Polytechnique Montreal", // No authentication required
                    pass: "md-Nj6F_Pfv5Dv4Z2luaxSs-w",
                },
            },
            defaults: {
                from: '"No Reply" <test@polymtl.ca>', // Set the default 'from' address
            },
            template: {
                dir: join(__dirname, 'templates'),
                adapter: new HandlebarsAdapter(),
            },
        }),
    ],
    providers: [MailService],
    exports: [MailService],
})
export class MailModule {}
