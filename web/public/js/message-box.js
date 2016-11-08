'use strict';


(function (g) {

    var eb = new EventBus("http://192.168.1.114:9091/eventbus");
    var handlers = [];
    var outboundAddress = 'server-to-client.' + getQueryVariable('username');
    var outboundAddress_listUsers = 'server-to-client-list-users';
    console.log("outbount: " + outboundAddress);
    eb.onopen = function () {

	// forward to 
	eb.registerHandler(outboundAddress, function (error, resp) {
	    UTIL.each(handlers, function () {
		console.log("recieved");
		this.call(null, resp.body)
	    });
	});

	// forward to 
	eb.registerHandler(outboundAddress_listUsers, function (error, resp) {
	    UTIL.each(handlers, function () {
		console.log("recieved listUsers");
		this.call(null, resp.body)
	    });
	});

	var data = {
	    origin: getQueryVariable('username')
	};

	window.Message.sendMessageListUsers(data);
    }

    eb.onclose = function () {
	console.log("Web Socket closing...");
    }


    g.Message = {
	registerHandler: function (callback) {
	    handlers.push(callback);
	},
	sendMessage: function (data) {
	    console.log("sending...");
	    eb.send('client-to-server', data);
	},
	sendMessageListUsers: function (data) {
	    console.log("sending listUsers...");
	    eb.send('client-to-server-list-users', data);
	}
    };

})(window);
