$('#btn-signin').on('click', function () {
    var username = $('#input-username').val();
    if ($('#input-username').val().length > 0) {
	
	var jsonData = {
	    "username": username
	};
	
	$.ajax({
	    url: 'http://'+host+'/signin',
	    type: 'post',
	    contentType: "application/json; charset=utf-8",
	    dataType: 'json',
	    traditional: true,
	    data: JSON.stringify(jsonData),
	    success: function (resp) {
		if (resp.success) {
		    window.location = "/chatbox?username="+username;
		} else {
		    alert(""+resp.message);
		}
	    },
	    error: function () {
		alert("Something went wrong on the server. Please notify the admin.");
	    }
	});
    } else {
	alert("Please enter username");
    }
});

$(document).ready(function(){    
    //Check if the current URL contains '#'
//    if(document.URL.indexOf("#")==-1){
//        // Set the URL to whatever it was plus "#".
//        url = document.URL+"#";
//        location = "#";
//
//        //Reload the page
//        location.reload(true);
//    }
});