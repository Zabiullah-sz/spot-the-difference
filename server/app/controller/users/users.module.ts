import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { UsersController } from './users.controller';
import { userSchema, UserDocument } from '@app/model/database-schema/user.schema';
import { UsersService } from './users.service';
import UserSessionService from './user-session.service';
import { ChatDocument, chatSchema } from '@app/model/database-schema/chat.schema';
import { MailModule } from '@app/class/mail/mail.module';
@Module({
    imports: [
        MongooseModule.forFeature([{ name: UserDocument.name, schema: userSchema }]),
        MongooseModule.forFeature([{ name: ChatDocument.name, schema: chatSchema }]),
        MailModule
    ],
    controllers: [UsersController],
    providers: [UsersService, UserSessionService],
    exports: [UsersService, UserSessionService],
})
export class UsersModule {}
