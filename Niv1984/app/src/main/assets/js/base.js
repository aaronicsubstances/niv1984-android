$(function() {
    var slashIndex = location.href.lastIndexOf('/');
    var bnum = parseInt(location.href.substring(slashIndex+1, slashIndex+3));
    var initialBookmark = undefined;
    var hashIndex = location.href.lastIndexOf('#')
    if (hashIndex >= 0) {
        initialBookmark = location.href.substring(hashIndex+1);
    }

    //NB: BOOKMARKS is a global Map injected into html files
    var sortedBookmarks = identifyAndSortInternalBookmarks(BOOKMARKS);
    var lastBookmark = sortedBookmarks[sortedBookmarks.length-1];

    var throttleFxn = function() {
        fireOnPageScrollEvent();

        // Get container scroll position
        var fromTop = $(window).scrollTop();

        var lastCnumSeen = 0
        for (var i = 0; i < sortedBookmarks.length; i++) {
            var bookmark = sortedBookmarks[i]
            var bookmarkEl = $('#'+bookmark);
            if (!bookmarkEl.offset() || !bookmarkEl.height()) {
                continue;
            }
            var isChapterDiv = false;
            if (bookmark.startsWith("chapter-")) {
                lastCnumSeen = parseInt(bookmark.substring("chapter-".length));
                isChapterDiv = true;
            }
            var offsetTop = bookmarkEl.offset().top;
            if (offsetTop >= fromTop) {
                // this check doesn't only save us unnecessary updates,
                // but also helps deal with undesirable calls after
                // initial scrolling is initiated by check() function.
                if (bookmark != lastBookmark) {
                    saveInternalBookmark(bnum, bookmark, lastCnumSeen);
                    lastBookmark = bookmark;
                }
                return;
            }
            if (!isChapterDiv) {
                if (offsetTop + bookmarkEl.height() / 2 > fromTop) {
                    // this check doesn't only save us unnecessary updates,
                    // but also helps deal with undesirable calls after
                    // initial scrolling is initiated by check() function.
                    if (bookmark != lastBookmark) {
                        saveInternalBookmark(bnum, bookmark, lastCnumSeen);
                        lastBookmark = bookmark;
                    }
                    return;
                }
            }
        }
    };
    var previousTop = 0;
    var isPageLoadingInProgress = function() {
        var elem = document.getElementById(lastBookmark)
        if (!elem) {
            return true
        }
        var currentTop = elem.getBoundingClientRect().top
        if (currentTop !== previousTop) {
            previousTop = currentTop
            return true
        }
        return false
    };
    var check = function() {
        if (isPageLoadingInProgress()) {
            setTimeout(check, 500);
            return;
        }

        // navigate to initial bookmark or scroll to start of document
        if (initialBookmark) {
            var elem = document.getElementById(initialBookmark)
            if (elem) {
                elem.scrollIntoView()
            }
            else {
                // do nothing
            }
        }
        else {
            window.scrollTo(0, 0);
        }
        lastBookmark = initialBookmark;
        $(window).scroll(throttle(1000, throttleFxn));
        fireOnPageLoadCompleted();
    };

    // don't install scroll listener until page loading completes.
    check();
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

function fireOnPageLoadCompleted() {
    if (window.biblei) {
        biblei.javaOnPageLoadCompleted()
    }
}

function fireOnPageScrollEvent() {
    if (window.biblei) {
        biblei.javaOnPageScrollEvent()
    }
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