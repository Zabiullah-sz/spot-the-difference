const express = require('express');
const http = require('http');
const socketIo = require('socket.io');

const app = express();
const server = http.createServer(app);
const io = socketIo(server);

const users = {};

io.on('connection', (socket) => {
    socket.on('checkUsername', (username) => {
        if (users[username]) {
            socket.emit('usernameTaken', true);
        } else {
            socket.emit('usernameTaken', false);
            socket.username = username;
            users[username] = socket.id;
        }
    });

    socket.on('chatMessage', (message) => {
        io.emit('message', { username: socket.username, message });
    });

    socket.on('disconnect', () => {
        delete users[socket.username];
    });
});

server.listen(3000, () => {
    console.log('Server is running on port 3000');
});
