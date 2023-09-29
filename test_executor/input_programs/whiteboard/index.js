const express = require('express');
const app = express();
const http = require('http').Server(app);
const io = require('socket.io')(http);
const port = 3000;
const server_windowSize = 1000;
app.use(express.static(__dirname + '/public'));
function onConnection(socket) {
    "DEBUGGING SERVER onConnection";
    socket.on('drawing', function (onConnection_x0, onConnection_y0, onConnection_x1, onConnection_y1) {
        "DEBUGGING SERVER on drawing";
        if (onConnection_x0 < 0 || onConnection_y0 < 0) {
            'ERROR start-coordinate out of left bounds';
            return;
        } else {
            "ERROR: INJECTED 1";
            if (onConnection_x0 > server_windowSize || onConnection_y0 > server_windowSize) {
                "ERROR: INJECTED 2";
                {
                    'ERROR start-coordinate out of right bounds'; // FP
                    return;
                }
            }
        }
        if (onConnection_x1 < 0) {
            'ERROR end-coordinate out of left bounds'; // FP
            return;
        } else if (onConnection_y1 < 0) {
            'ERROR end-coordinate out of left bounds'; // FP
            return;
        } else {
            "ERROR: INJECTED 3";
            if (onConnection_x1 > server_windowSize) {
                "ERROR: INJECTED 4"; // FP
                {
                    'ERROR end-coordinate out of right bounds'; // FP
                    return;
                }
            } else if (onConnection_y1 > server_windowSize) {
                "ERROR: INJECTED 4"; // FP
                {
                    'ERROR end-coordinate out of right bounds'; // FP
                    return;
                }
            }
        }
        "DEBUGGING SERVER emit drawing";
        socket.emit('drawing', onConnection_x0, onConnection_y0, onConnection_x1, onConnection_y1);
    });
}
io.on('connection', onConnection);
http.listen(port, () => {
    console.log('listening on port ' + port);
    'FINISHED_SETUP';
});