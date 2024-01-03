import { FriendRequest } from '@common/interfaces/friend-request';
import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document } from 'mongoose';
import * as mongoose from 'mongoose';
import { User as UserInterface } from '@common/interfaces/user';
@Schema({ timestamps: true })
// TODO: add preference for theme and language, make this a class in common
export class UserDocument extends Document {
    @Prop({ required: true, unique: true })
    username: string;

    @Prop({ required: true })
    password: string;

    @Prop({ required: true, unique: true })
    email: string;

    @Prop([{ type: Object, default: [] }])
    friendRequests: FriendRequest[];

    @Prop([{ type: Object, default: [] }])
    friends: UserInterface[];

    @Prop({ required: false, default: 'light' }) // Assuming 'light' and 'dark' themes
    themePreference: string;

    @Prop({ required: false, default: 'en' }) // Assuming English as default language
    languagePreference: string;

    @Prop({ required: false, default: 0 })
    points: number;

    @Prop({ required: false, default: false })
    token: string;
}

export const userSchema = SchemaFactory.createForClass(UserDocument);
// Change this
export interface User extends mongoose.Document {
    _id: string;
    username: string;
    password: string;
    friendRequests: FriendRequest[];
    friends: UserInterface[];
    themePreference: string;
    languagePreference: string;
    points: number;
    token: string;
}
