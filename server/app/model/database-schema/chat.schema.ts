import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document } from 'mongoose';
//import { User } from '@common/interfaces/user';
@Schema({ timestamps: true })
export class ChatDocument extends Document {
    @Prop([{ type: String }])
    userIds: string[];

    @Prop({ type: String, unique: true })
    name: string;

    @Prop({ type: Boolean, default: false })
    private: boolean;

    @Prop([{ type: Object }])
    messages: {
        sender: string;
        message: string;
        timestamp: Date;
        type: string;
    }[];
}

export const chatSchema = SchemaFactory.createForClass(ChatDocument);

export interface Chat extends Document {
    name: string;
    userIds: string[];
    private: boolean;
    messages: {
        sender: string;
        message: string;
        timestamp: Date;
    }[];
}
