import { Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { MongooseModule } from '@nestjs/mongoose';
import CardGateway from './gateways/card.gateway';
import GameGateway from './gateways/game.gateway';
import OutputFilterGateway from './gateways/output-filters.gateway';
import RecordGateway from './gateways/record.gateway';
import ChatGateway from './gateways/chat.gateway';
import FriendGateway from './gateways/friend.gateway';
import { CardDocument, cardSchema } from './model/database-schema/card.schema';
import { RecordDocument, recordSchema } from './model/database-schema/history.schema';
import { ChatDocument, chatSchema } from './model/database-schema/chat.schema';
import MongoDBService from './services/mongodb/mongodb.service';
import { UsersModule } from './controller/users/users.module';
import { AuthService } from './controller/auth/auth.service';
import { LocalStrategy } from './controller/auth/local.strategy';
import { SessionSerializer } from './controller/auth/session.serializer';
//import { MailerService } from '@nestjs-modules/mailer';
//import { MailService } from './services/mail/mail.service';
import { MailModule } from './class/mail/mail.module';
// test
@Module({
    imports: [
        ConfigModule.forRoot({ isGlobal: true }),
        MongooseModule.forRootAsync({
            imports: [ConfigModule],
            inject: [ConfigService],
            useFactory: async (config: ConfigService) => ({
                uri: config.get<string>('DATABASE_CONNECTION_STRING'), // Loaded from .env
            }),
        }),
        MongooseModule.forFeature([{ name: CardDocument.name, schema: cardSchema }]),
        MongooseModule.forFeature([{ name: RecordDocument.name, schema: recordSchema }]),
        MongooseModule.forFeature([{ name: ChatDocument.name, schema: chatSchema }]),
        UsersModule,
        MailModule,
    ],
    controllers: [],
    providers: [
        MongoDBService,
        GameGateway,
        CardGateway,
        RecordGateway,
        OutputFilterGateway,
        AuthService,
        LocalStrategy,
        SessionSerializer,
        ChatGateway,
        FriendGateway,
    ],
})
export class AppModule {}
