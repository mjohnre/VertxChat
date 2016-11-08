'use strict';

(function(w) {
	

	w.UTIL = {
		each: function(array, callback) {
			for(var i=0; i<array.length; i++) {
				callback.apply(array[i], []);
			}
		},
		checkStatus: function(response) {
		  if (response.status >= 200 && response.status < 300) {
		    return response;
		  } else {
		    var error = new Error(response.statusText)
		    error.response = response;
		    throw error
		  }
		},
		parseJSON: function(response) {
		  return response.json();
		}
	};


})(window);