'CLIENT ENTERED';
let socket = io();
let client_size = 2;
let cv = document.getElementById('c');
let c = cv.getContext('2d');
let grid = new Array(client_size);
for (let i = 0; i < client_size; i++) {
    grid[i] = new Array(client_size);
}
let draw = function () {
};
function getMousePos(canvas, evt) {
    let x = evt.clientX;
    let y = evt.clientY;
    if (evt.clientX >= client_size) {
        x = client_size - 1;
    }
    if (evt.clientY >= client_size) {
        y = client_size - 1;
    }
    return {
        x: x,
        y: y
    };
}
function invert(value) {
    if (value === 0) {
        return 1;
    } else {
        return 0;
    }
}
draw();
let pos = {
    x: 0,
    y: 0
};
let mouse_down = false;
let fill = false;
let generation = 0;
document.getElementsByTagName('canvas')[0].addEventListener('mousedown', function (e) {
    pos = getMousePos(cv, e);
    mouse_down = true;
    if (grid[pos.x][pos.y] === 1) {
        fill = false;
    } else {
        fill = true;
    }
    socket.emit('toggle', pos.x, pos.y);
    draw();
});
document.getElementsByTagName('canvas')[0].addEventListener('mouseup', function (e) {
    mouse_down = false;
});
document.getElementsByTagName('canvas')[0].addEventListener('mousemove', function (e) {
    if (!mouse_down) {
        return;
    }
    pos = getMousePos(cv, e);
    if (fill) {
        if (grid[pos.x][pos.y] === 0) {
            socket.emit('toggle', pos.x, pos.y);
            draw();
        }
    } else if (!fill) {
        if (grid[pos.x][pos.y] === 1) {
            socket.emit('toggle', pos.x, pos.y);
            draw();
        }
    }
    
});
document.getElementById('evolve').addEventListener('click', function (e) {
    "DEBUGGING CLIENT evolve";
    socket.emit('update', false);
});
document.getElementById('reset').addEventListener('click', function (e) {
    socket.emit('reset');
});
document.getElementById('auto').addEventListener('click', function (e) {
});
socket.on('toggle', function (coordX, coordY) {
    draw();
});
socket.on('update', function (board) {
    "DEBUGGING CLIENT SOCKET update";
    grid = board;
    generation++;
    document.getElementById('generation').innerHTML = generation;
    "DEBUGGING CLIENT SOCKET update before draw";
    draw();
    "DEBUGGING CLIENT SOCKET update after draw";
});
socket.on('reset', function () {
    generation = 0;
});
socket.on('auto', function (is_auto) {
    if (is_auto) {
        document.getElementById('auto').style.backgroundColor = '#000000';
    } else {
        document.getElementById('auto').style.backgroundColor = '#333333';
    }
});