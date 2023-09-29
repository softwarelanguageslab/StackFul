let express = require('express');
let app = express();
let http = require('http').Server(app);

app.use(express.static(__dirname + '/public'));

http.listen(3000, function(){
  console.log('Listening on localhost:3000');
  "FINISHED_SETUP";
});