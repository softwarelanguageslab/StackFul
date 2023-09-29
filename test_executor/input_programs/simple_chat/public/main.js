(function () {
  "CLIENT ENTERED";
  let socket = io();
  let input = document.getElementById("m_input");
  let button = document.getElementById("m_button");
  function shouldCensor(word) {
    if (word === "swear word") {
      return true;
    } else if (word.startsWith("swear")) {
      return true;
    } else {
      return false;
    }
  }
  button.addEventListener("click", function(e) {
    "DEBUGGING CLIENT on click";
    const m = input.value;
    if (! shouldCensor(m)) {
      console.log(m);
      "DEBUGGING CLIENT emit chat_message";
      socket.emit('chat_message', m);
    }
    return false;
  });
  socket.on('chat_message', function(msg){
    "DEBUGGING CLIENT on chat_message";
    let messages = document.getElementById("messages");
    messages.append(msg + "\n");
    window.scrollTo(0, document.body.scrollHeight);
  });
})();