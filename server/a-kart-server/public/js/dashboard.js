var socket = io();
var target = document.getElementById('target');
var gamecontrol = document.getElementById('gamecontrol');
var playerList = document.getElementById('playerList');

gamecontrol.addEventListener('change', function (evt) {
    socket.emit('set game ' + (evt.srcElement.checked ? 'on' : 'off'));
});

document.getElementById('register').addEventListener('click', function () {
    if (target.value.length >= 0) {
        socket.emit('register', target.value);
    }
});

socket.on('set game', function (data) {
    console.log('somebody set game ', data)
    gamecontrol.checked = data
});

socket.on('players', function (data) {
    console.log('players', data)
    while (playerList.lastChild)  playerList.removeChild(playerList.lastChild);
    data.map(function (e) {
        var li = document.createElement('tr');
        li.innerHTML = '<td><span class="playerItem">' + e + '</span></td>' +
            '<td><button class="pure-button pure-button-primary" type="button">BOOM</button></td>';
        li.getElementsByTagName('button')[0].addEventListener('click', function () {
            if (target.value.length >= 0) {
                socket.emit('boom', {'id' :e });
                console.log('boomed ' + e)
            }
        });
        playerList.appendChild(li);
    })
})