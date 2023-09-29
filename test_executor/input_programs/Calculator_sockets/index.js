let express = require('express');
let app = express();
let http = require('http').Server(app);
const io = require('socket.io')(http);
app.use(express.static(__dirname + '/public'));
io.on('connection', function (socket) {
    console.log("DEBUGGING SERVER connection");
    socket.on('compute', function (op, left, right) {
        console.log('ERROR: compute');
        "DEBUGGING SERVER compute";
        let result;
        if (op === -1) {
            "DEBUGGING SERVER -1";
            result = left + right;
        } else if (op === -2) {
            "DEBUGGING SERVER -2";
            result = left - right;
        } else if (op === -3) {
            "DEBUGGING SERVER -3";
            result = left * right;
        } else if (op === -4) {
            "DEBUGGING SERVER -4";
            if (right === 0) {
                "DEBUGGING SERVER -4 division-by-zero";
                throw new Error("Dividing by zero");
            } else {
                "DEBUGGING SERVER -4 regular division";
                result = left / right;
            }
        } else {
        "DEBUGGING SERVER other operator";
        "ERROR: Unknown operator"; // LP
        }
        socket.emit('computation_result', result);
    });
});
http.listen(3000, function () {
    console.log('Listening on localhost:3000');
    'FINISHED_SETUP';
});