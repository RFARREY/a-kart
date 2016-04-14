var express = require('express');
var http = require('http');
var path = require('path');
var socketio = require('socket.io')

var speed = 0;

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
var globalSpeed = 100

var connecteds = {}

io.on('connection', function (socket) {
    console.log('a user connected');

    function broadcastGameStatus(socket) {
        io.emit('set game', GAME_IS_ON);
        var players = [];
        for (var k in connecteds) players.push(k);
        io.emit('players', players);
		io.emit('speed', globalSpeed);
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
        connecteds[data]._custom_speed = globalSpeed;
        connecteds[data]._custom_timeout = null;
        this['id'] = data;
        broadcastGameStatus(socket);
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

    socket.on('setSpeed', function (data) {
        console.log( data );
        if (connecteds[data.id]) {
	        console.log( "Change " + data.id + " maximun speed to " + data.value);
            connecteds[data.id].emit('speed', data.value );
            // speed = data.value;
            connecteds[data.id]._custom_speed = data.value;
        } else {
            console.log( "Change all maximun speed to " + data.value);
            for (var key in connecteds) {
				if (connecteds.hasOwnProperty(key)) {
					console.log( "Send message to " + key);
					clearTimeout( connecteds[key]._custom_timeout );
					connecteds[key].emit( 'speed', data.value );
					connecteds[key]._custom_speed = data.value;
					globalSpeed = data.value
				}
			}
        }
    });
    
    var setMalus = function(id) {
		    var newSpeed = (connecteds[id]._custom_speed - 10);
		    connecteds[id]._custom_speed = (newSpeed > 10) ? newSpeed : 10;
		    connecteds[id].emit( "speed", connecteds[id]._custom_speed );
		    //clearTimeout( connecteds[id]._custom_timeout );
		    connecteds[id]._custom_timeout = setTimeout(
			    function() {
				    //clearTimeout( connecteds[id]._custom_timeout );
				    console.log("Timeout malus", id);
				    var newSpeed = (connecteds[id]._custom_speed + 10);
				    connecteds[id]._custom_speed = (newSpeed <= 100) ? newSpeed : 100;
				    connecteds[id].emit( "speed", connecteds[id]._custom_speed );
			    },
			    5000
		    );
	    
    }

    function getRandomInt(max) {
        return parseInt(""+ Math.random() * max);
    }
    
    var onBonus = function(data) {
	    console.log( data );
	    if ( parseInt(data.marker) % 3 == 0) {
		    //bonus
		    console.log( "Assigning a bonus to the player that hit the cube, which means, assign a malus to all the other players " , data);
		    for (var key in connecteds) {
				if (connecteds.hasOwnProperty(key)) {
					if (key != data.player) {
						setMalus(key);
					}
				}
			}
	    } else if ( parseInt(data.marker) % 3 == 1) {
            //malus
            console.log( "Assigning a malus to the player that hit the cube " , data);
            setMalus(data.player);
        } else if ( parseInt(data.marker) % 3 == 2) {
            //shot random
            var keys = Object.keys(connecteds);
            var randomIdx = Math.floor(Math.random()*keys.length);
            var key = keys[randomIdx];
            console.log("hitting", key, keys, randomIdx)
            connecteds[key].emit('hit');
        }
    }
    socket.on('s-bonus', function(data) {
	    console.log( 'bonus', data );
	    onBonus( { "marker":1, "player": data.id } );
    });
    socket.on('s-malus', function(data) {
	    console.log( 's-malus', data );
	    onBonus( { "marker":0, "player": data.id } );
    })
    socket.on('bonus', onBonus);
});

server.listen(port, function () {
    console.log('listening on *:' + port);
});
