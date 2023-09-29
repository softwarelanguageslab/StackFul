let express = require('express');
let request = require('request');
let app = express();
let http = require('http').Server(app);
let cookieParser = require('cookie-parser');
let bodyParser = require('body-parser');
const io = require('socket.io')(http);
let port = 3000;
let server_size = 2;
app.use(express.static(__dirname + '/public'));
function makeID(length) {
    let text = '';
    let possible = 'abcdefghijklmnopqrstuvwxyz0123456789';
    for (let i = 0; i < length; i++) {
        text += possible.charAt(Math.floor(Math.random() * possible.length));
    }
    return text;
}
let gridSum = function (x, y, grid) {
    let s = 0;
    for (let a = -1; a < 2; a++) {
        "DEBUGGING SERVER first loop start";
        for (let b = -1; b < 2; b++) {
            "DEBUGGING SERVER second loop start";
            if (a === 0) {
                "DEBUGGING SERVER gridSum a === 0";
                if (b === 0) {
                    "DEBUGGING SERVER continue";
                } else {
                    if (x + a < 0) {
                        "ERROR: INJECTED SERVER ERROR 1";
                    } else {
                        "DEBUGGING SERVER gridSum else-branch";
                        s += grid[x + a][y + b];
                    }
                }
            } else {
                if (x + a < 0) {
                    "ERROR: INJECTED SERVER ERROR 1";
                } else {
                    "DEBUGGING SERVER gridSum else-branch";
                    s += grid[x + a][y + b];
                }
            }
        }
    }
    return s;
};
let newGrid = function () {
    let grid = new Array(server_size);
    for (let i = 0; i < server_size; i++) {
        grid[i] = new Array(server_size);
        for (let j = 0; j < server_size; j++) {
        }
    }
    return grid;
};
let grid = newGrid();
let repeater = null;
let is_auto = false;
function update(socket) {
    let ngrid = new Array(server_size);
    for (let i = 0; i < server_size; i++) {
        ngrid[i] = new Array(server_size);
    }
    for (let x = 0; x < server_size; x++) {
        for (let y = 0; y < server_size; y++) {
            s = 0;
            if (grid[x][y] === 1) {
                "ERROR: INJECTED SERVER ERROR 2";
                {
                    if (s < 2) {
                        "DEBUGGING SERVER update < 3";
                    } else {
                        "ERROR: INJECTED SERVER ERROR 3";
                    }
                }
            } else {
                "DEBUGGING SERVER update else-branch";
                if (s === 3) {
                    "DEBUGGING SERVER update equals 3";
                } else {
                    "DEBUGGING SERVER update does not equal 3";
                }
            }
        }
    }
    grid = ngrid;
    socket.emit('update', grid);
}
io.on('connection', function (socket) {
    socket.on('update', function (data) {
        'DEBUGGING SERVER UPDATE';
        update(socket);
    });
    socket.on('auto', function (data) {
        'DEBUGGING SERVER AUTO';
        if (is_auto) {
            is_auto = false;
        } else {
            update(socket);
            is_auto = true;
        }
    });
    socket.on('reset', function (data) {
        'DEBUGGING SERVER RESET';
        grid = newGrid();
        socket.emit('reset', undefined);
        socket.emit('update', grid);
    });
    function invert(value) {
        if (value === 0) {
            return 1;
        } else {
            return 0;
        }
    }
    socket.on('toggle', function (coordX, coordY) {
        'DEBUGGING SERVER TOGGLE 1';
        if (coordX >= server_size) {
            "ERROR: INJECTED SERVER ERROR 4";
            {
                return;
            }
        }
        if (coordY >= server_size) {
            return;
        }
    });
});
http.listen(3000, function () {
    console.log('Listening on localhost:3000');
    'FINISHED_SETUP';
});