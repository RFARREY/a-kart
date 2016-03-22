var socket = io();
var target = document.getElementById('target');
var gamecontrol = document.getElementById('gamecontrol');
var playerList = document.getElementById('playerList');
var speedSelector = document.querySelector('#overall-speed');
var speedSelectorOutput = document.querySelector('#overall-speed-output');
var formAdd = document.querySelector("#form-add");
var speed = 75;


// Sends the event to the server, both for "all" and for single drone
var setSpeed = function( id, val ) {
	socket.emit( "setSpeed", { "id": id, "value": val } );
	console.log( "setSpeed", id, val);
}

// Sync overall speed with single drone speed (resets speed customization for single drone)
var syncSpeed = function( val ) {
	var ranges = document.querySelectorAll('.single-speed');
	for (var i = 0, l = ranges.length; i < l; i++) {
		ranges[i].value = val;
	}
}

// Add listener on overall speed change
speedSelector.addEventListener('change', 
	function() {
		speed = speedSelector.value;
		speedSelectorOutput.innerHTML = speed;
		setSpeed( null, speed );
		syncSpeed( speed );
	},
	false
);

// Dispatch the event that the maximum speed has changed
speedSelector.value = speed;
var event = document.createEvent('HTMLEvents');
event.initEvent('change', true, false);
speedSelector.dispatchEvent(event);


// Prevent form submitting when manually adding new clients
formAdd.addEventListener(
	'submit',
	function(e) {
		e.preventDefault();
		var event = document.createEvent('HTMLEvents');
		event.initEvent('click', true, false);
		document.querySelector('#register').dispatchEvent(event);
		target.value = "";
	}
)



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
    
    data.map( function (e) {
        var div = document.createElement('div');
        
        
        div.innerHTML = '<div class="playerItem flexHolder">' +
        	'<input type="range" min="0" max="100" step="5" class="single-speed" /> ' +
        	'<button class="pure-button" data-action="boom" ><i class="fa fa-bomb"></i></button> ' +
			'<span class="playerName">' + e + '</span></div>';
        
        var buttons = div.querySelectorAll('button');
        
        for (var i=0, l=buttons.length; i<l; i++) {
	        buttons[i].addEventListener(
		        'click',
		        function(evt) {
			        evt.preventDefault();
			        var action = this.getAttribute("data-action") || false;
			        if (action) {
				        socket.emit( action, { "id": e } );
				        console.log( action, e );
			        }
		        },
		        false
	        );
        }
        
        var range = div.querySelectorAll('input[type="range"]');
        
        for (var i=0, l=range.length; i<l; i++) {
	        range[i].addEventListener(
		        'change',
		        function(evt) {
			        evt.preventDefault();
			        var val = this.value || false;
			        if (val) {
				        setSpeed( e, val);
			        }
		        },
		        false
	        );
        }
        
        div.className = "playerRow " + e.toLowerCase();
        playerList.appendChild(div);
    });
    syncSpeed( speed );
})