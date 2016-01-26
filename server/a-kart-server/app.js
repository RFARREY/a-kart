var express = require('express');
var http = require('http');
var socketio = require('socket.io')


var app = express();
var server = http.Server(app);
var io = socketio(server);

var port = process.env.PORT || 5000;
app.set('port', port);
app.use(express.static(__dirname + '/public'));
app.set('view engine', 'ejs');

app.get('/', function (req, res) {
    res.sendfile('index.html');
});

var GAME_IS_ON = false

var connecteds = {}
io.on('connection', function (socket) {
    console.log('a user connected');
    socket.emit('set game', GAME_IS_ON)
    var players = []
    for (var k in connecteds) players.push(k)
    socket.emit('players', players)

    socket.on('disconnect', function () {
        var id = this['id'];
        if (id) {
            console.log('disconnect ' + id);
            delete connecteds[id]
            delete this['id']
        }
    }.bind(socket));

    socket.on('set game on', function () {
        console.log('game on')
        if (!GAME_IS_ON) {
            GAME_IS_ON = true;
            socket.broadcast.emit('set game', GAME_IS_ON)
        }
    });

    socket.on('set game off', function () {
        console.log('game off')
        if (GAME_IS_ON) {
            GAME_IS_ON = false;
            socket.broadcast.emit('set game', GAME_IS_ON)
        }
    });

    socket.on('get game status', function () {
        console.log('get game status')
        socket.emit('set game', GAME_IS_ON)
        var players = []
        for (var k in connecteds) players.push(k)
        socket.emit('players', players)
    });

    socket.on('register', function (data) {
        console.log('register ' + JSON.stringify(data));
        connecteds[data] = this;
        this['id'] = data;



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
