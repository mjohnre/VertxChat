'use strict';

(function (g) {

    Message.registerHandler(function (resp) {
	if (resp.type == 'message') {
	    var content = '[' + resp.timestamp + '] ' + resp.origin + ': ' + resp.message;
	    console.log('received Message: ' + content);
	    $('#messages').prepend('<li class="list-group-item">' + content + '</li>');

	} else if (resp.type == 'update_users') {
	    console.log('received Users: ' + resp.message);
	    populateActiveUsers(resp.message);
	}
    });


    $('#btn-send').click(function () {
	var items = [];
	$('#users option:selected').each(function () {
	    items.push($(this).val());
	});
	if ($('#input-message').val().length > 0 && items.length > 0) {

	    var datetime = Date.now();
	    var timestamp = new Date().toLocaleString();

	    var data = {
		origin: getQueryVariable('username'),
		dest: items,
		message: $('#input-message').val(),
		timestamp: timestamp
	    };
	    console.log("selected users " + items);
	    //send
	    Message.sendMessage(data);

	    var content = '[' + data.timestamp + '] ' + data.origin + ': ' + data.message;

	    $('#messages').prepend('<li class="list-group-item">' + content + '</li>');
	}
    });


})(window);

