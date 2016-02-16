var express = require('express');
var http = require('http');
var path = require('path');
var socketio = require('socket.io')


var app = express();
var server = http.Server(app);
var io = socketio(server);
console.log("dirname is ",__dirname);
var port = process.env.PORT || 5000;
app.set('port', port);
app.use(express.static(__dirname + '/public'));
app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, 'views'));

app.get('/', function (req, res) {
    res.render('dashboard', {
        title: 'A-Kart Dashboard'
    });
});

var GAME_IS_ON = false

var connecteds = {}
io.on('connection', function (socket) {
    console.log('a user connected');

    function broadcastGameStatus(socket) {
        io.emit('set game', GAME_IS_ON);
        var players = [];
        for (var k in connecteds) players.push(k);
        io.emit('players', players);
    }

    broadcastGameStatus(socket);

    socket.on('disconnect', function () {
        var id = this['id'];
        if (id) {
            console.log('disconnect ' + id);
            delete connecteds[id]
            delete this['id']
            broadcastGameStatus(socket);
        }
    }.bind(socket));

    socket.on('set game on', function () {
        console.log('game on')
        if (!GAME_IS_ON) {
            GAME_IS_ON = true;
            broadcastGameStatus(socket);
        }
    });

    socket.on('set game off', function () {
        console.log('game off')
        if (GAME_IS_ON) {
            GAME_IS_ON = false;
            broadcastGameStatus(socket);
        }
    });

    socket.on('get game status', function () {
        console.log('get game status')
        broadcastGameStatus(socket)
    });

    socket.on('register', function (data) {
        console.log('register ' + JSON.stringify(data));
        connecteds[data] = this;
        this['id'] = data;
        broadcastGameStatus(socket)
    }.bind(socket));

    socket.on('boom', function (data) {
        var targetId = data.id;
        console.log("targetId" + targetId)
        if (connecteds[targetId]) {
            connecteds[targetId].emit('hit');
            console.log('boom!!');
        } else {
            console.log('dont know that id '+targetId);
        }
    });
});

server.listen(port, function () {
    console.log('listening on *:' + port);
});
