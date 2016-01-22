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

app.get('/', function (req, res) {
    res.sendfile('index.html');
});

var connecteds = {}
io.on('connection', function (socket) {
    console.log('a user connected');

    socket.on('disconnect', function () {
        var id = this['id'];
        if (id) {
            console.log('disconnect ' + id);
            delete connecteds[id]
            delete this['id']
        }
    }.bind(socket));

    socket.on('reset', function () {
        console.log('reset')
        socket.emit('reset')
    });
    socket.on('start', function () {
        console.log('start')
        socket.emit('start')
    });

    socket.on('register', function (data) {
        console.log('register ' + JSON.stringify(data));
        connecteds[data.id] = this;
        this['id'] = data.id;
    }.bind(socket));

    socket.on('boom', function (data) {
        var targetId = data.id;
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
