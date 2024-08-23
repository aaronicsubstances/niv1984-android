$(function(){
    var slashIndex = location.href.lastIndexOf('/');
    var bnum = parseInt(location.href.substring(slashIndex+1, slashIndex+3));

    var throttleFxn = function() {
        // Get container scroll position
        var fromTop = $(this).scrollTop();

        var lastPositiveCheckpoint = 0;
        var lastCnumSeen = 0
        for (var i = 0; i < checkpoints.length; i++) {
            var checkpoint = checkpoints[i]
            if (checkpoint < 0) {
                lastCnumSeen = -checkpoint;
                var fragId = "chapter" + checkpoint;
                var offsetTop = $('#'+fragId).offset().top;
                if (offsetTop >= fromTop) {
                    saveCheckpoint(bnum, lastCnumSeen, checkpoint);
                    return;
                }
            }
            else {
                for (var j = lastPositiveCheckpoint + 1; j <= checkpoint; j++) {
                    var fragId = "verse-" + j;
                    var offsetTop = $('#'+fragId).offset().top;
                    if (offsetTop >= fromTop) {
                        saveCheckpoint(bnum, lastCnumSeen, j);
                        return;
                    }
                    if (offsetTop + $('#'+fragId).height() >= fromTop) {
                        saveCheckpoint(bnum, lastCnumSeen, j);
                        return;
                    }
                }
                lastPositiveCheckpoint = checkpoint
            }
        }
    };
    $(window).scroll(throttle(1000, throttleFxn));
});

function saveCheckpoint(bnum, cnum, checkpoint) {
    if (window.biblei) {
        biblei.javaCacheCurrentChapter(bnum, cnum, checkpoint);
    }
}

function getQueryVariable(variable) {
	if (!window.location.search) {
		return null;
	}
    var query = window.location.search.substring(1);
    var vars = query.split('&');
    for (var i = 0; i < vars.length; i++) {
        var pair = vars[i].split('=');
        if (decodeURIComponent(pair[0]) == variable) {
            return decodeURIComponent(pair[1]);
        }
    }
}

function throttle(delay, callback) {
    // After wrapper has stopped being called, this timeout ensures that
    // `callback` is executed at the proper times in `throttle` and `end`
    // debounce modes.
    var timeout_id,

      // Keep track of the last time `callback` was executed.
      last_exec = 0;

    // The `wrapper` function encapsulates all of the throttling / debouncing
    // functionality and when executed will limit the rate at which `callback`
    // is executed.
    function wrapper() {
      var that = this,
        elapsed = new Date() - last_exec,
        args = arguments;

      // Execute `callback` and update the `last_exec` timestamp.
      function exec() {
        last_exec = new Date();
        callback.apply( that, args );
      };

      if (!timeout_id ) {
        // Since `wrapper` is being called for the first time and
        // `debounce_mode` is true (at_begin), execute `callback`.
        exec();
      }

      // Clear any existing timeout.
      if (timeout_id) {
        clearTimeout( timeout_id );
        timeout_id = undefined;
      }

      if (elapsed > delay ) {
        // In throttle mode, if `delay` time has been exceeded, execute
        // `callback`.
        exec();

      } else {
        // In trailing throttle mode, since `delay` time has not been
        // exceeded, schedule `callback` to execute `delay` ms after most
        // recent execution.
        timeout_id = setTimeout( exec, delay - elapsed );
      }
    };

    // Return the wrapper function.
    return wrapper;
}