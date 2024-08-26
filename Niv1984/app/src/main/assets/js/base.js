$(function(){
    var slashIndex = location.href.lastIndexOf('/');
    var bnum = parseInt(location.href.substring(slashIndex+1, slashIndex+3));
    var sortedBookmarks = []
    var throttleFxn = function() {
        if (!sortedBookmarks.length) {
            //NB: BOOKMARKS is a global Map injected into html files
            sortedBookmarks = identifyAndSortInternalBookmarks(BOOKMARKS);
        }
        // Get container scroll position
        var fromTop = $(window).scrollTop();

        var lastCnumSeen = 0
        for (var i = 0; i < sortedBookmarks.length; i++) {
            var bookmark = sortedBookmarks[i]
            var bookmarkEl = $('#'+bookmark);
            var isChapterDiv = false;
            if (bookmark.startsWith("chapter-")) {
                lastCnumSeen = parseInt(bookmark.substring("chapter-".length));
                isChapterDiv = true;
            }
            var offsetTop = bookmarkEl.offset().top;
            if (offsetTop - (isChapterDiv ? 10 : 0) >= fromTop) {
                saveInternalBookmark(bnum, bookmark, lastCnumSeen);
                return;
            }
            if (!isChapterDiv) {
                if (offsetTop + bookmarkEl.height()- > fromTop) {
                    saveInternalBookmark(bnum, bookmark, lastCnumSeen);
                    return;
                }
            }
        }
    };
    $(window).scroll(throttle(1000, throttleFxn));
});

function identifyAndSortInternalBookmarks(mapOfBookmarks) {
    var sortedBookmarks = []
    mapOfBookmarks.forEach((list, key) => {
        for (value of list) {
            var sepIdx = value.indexOf('-')
            var bmSuffix = value.substring(0, sepIdx);
            var bmDesc = value.substring(sepIdx+1);
            if (/^\d+$/.test(bmDesc)) {
                sortedBookmarks.push(`chapter-${bmDesc}`);
            }
            sortedBookmarks.push(`${key}-bookmark-${bmSuffix}`)
        }
    });
    sortedBookmarks.sort((a, b) => $('#'+a).offset().top - $('#'+b).offset().top);
    return sortedBookmarks
}

function saveInternalBookmark(bnum, bookmark, cnum) {
    if (window.biblei) {
        biblei.javaSaveInternalBookmark(bnum, bookmark, cnum);
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