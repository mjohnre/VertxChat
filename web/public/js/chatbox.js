
$('#btn-logout').on('click', function () {
    var username = "" + getQueryVariable("username");
    if (username.length > 0) {

	var jsonData = {
	    "username": username
	};

	$.ajax({
	    url: 'http://'+host+'/logout',
	    type: 'post',
	    contentType: "application/json; charset=utf-8",
	    dataType: 'json',
	    traditional: true,
	    data: JSON.stringify(jsonData),
	    success: function (resp) {
		if (resp.success) {
		    window.location = "/";
		} else {
		    alert("" + resp.message);
		}
	    },
	    error: function () {
		alert("Something went wrong on the server. Please try again.");
	    }
	});
    } else {
	alert("Something is wrong with your URL. You may just close this window.")
    }
});

function getActiveUsers() {
    $.ajax({
	url: 'http://'+host+'/active-users',
	type: 'post',
	contentType: "application/json; charset=utf-8",
	dataType: 'json',
	traditional: true,
	success: function (resp) {
	    if (resp.success) {
		populateActiveUsers(resp.message);
	    } else {
		alert("" + resp.message);
	    }
	},
	error: function () {
	    alert("Something went wrong on the server. Please try again.");
	}
    });
}

function populateActiveUsers(userArray) {
    var activeUsersHtml = "";
    for (var i = 0; i < userArray.length; i++) {
	if (getQueryVariable('username') != userArray[i]) {
	    activeUsersHtml += "<option>" + userArray[i] + "</option>";
	}
    }
    $('#users').html(activeUsersHtml);
}

function getQueryVariable(variable)
{
    var query = window.location.search.substring(1);
    var vars = query.split("?");
    for (var i = 0; i < vars.length; i++) {
	var pair = vars[i].split("=");
	if (pair[0] == variable) {
	    return decodeURIComponent(pair[1]);
	}
    }
    return(false);
}