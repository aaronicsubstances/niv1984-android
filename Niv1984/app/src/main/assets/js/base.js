DEFAULT_VERSION = "cpdv"

$(function() {
    var bcode = parseBookCodeFromUrl();
    var firstBookTextEl = $(".booktext")[0];
    var additionalVersion = getQueryVariable("add");
    if (!additionalVersion) {
        caterForScrolling(bcode, DEFAULT_VERSION, firstBookTextEl, window,
            fireOnPageLoadCompleted);
        return;
    }
    if (additionalVersion === "gnt1992" || additionalVersion === "bbe1965") {
        if (bcode === "ESG") {
            bcode = "EST";
        }
        else if (bcode === "DAG") {
             bcode = "DAN";
        }
    }
    var urlToLoad = `/${additionalVersion}/${bcode}.html`;
    $('<div id="add" class="booktext"></div>').insertAfter(".booktext");
    var secondBookTextEl = document.getElementById("add");
    $("#add").load(urlToLoad + ' .booktext', function(responseTxt, statusTxt, xhr) {
        var doneCbCallCnt = 0;
        var doneCb = undefined;
        if (statusTxt === "success") {
            $('#add .booktext').removeClass('booktext');
            doneCb = function() {
                doneCbCallCnt++;
                if (doneCbCallCnt > 1) {
                    fireOnPageLoadCompleted();
                }
            };
        }
        else {
            var errMsg = `${xhr.status} ${xhr.statusText}`;
            if (errMsg === "0 error") {
                errMsg = "NOT AVAILABLE";
            }
            $("#add").html(`<h2>${errMsg}</h2>`);
        }
        $("#wrapper").css({ display: "flex" });
        $(".booktext").css({
            height: "100vh",
            overflowY: "scroll",
            width: "50%",
        });
        caterForScrolling(bcode, DEFAULT_VERSION, firstBookTextEl, firstBookTextEl,
            doneCb ? doneCb : fireOnPageLoadCompleted);
        if (doneCb) {
            caterForScrolling(bcode, additionalVersion, secondBookTextEl, secondBookTextEl,
                doneCb);
        }
     });
});

function caterForScrolling(bcode, version, booktextEl, scrollEl, doneCb) {
    var encodedBookmarks = JSON.parse($("span.bookmarks", $(booktextEl)).text());
    var sortedBookmarks = identifyAndSortInternalBookmarks(encodedBookmarks);
    if (version === DEFAULT_VERSION) {
        $.get('/footnote-status', { bcode: bcode }, function(data) {
            var ignoredFootnotes = data.hrefs.split(",");
            modifyFootnotes(bcode, booktextEl, sortedBookmarks,
                ignoredFootnotes, data.editEnabled);
        }).fail(function(jqXHR, textStatus, errorThrown) {
            console.log("Failed to load footnote states: errorThrown: ", errorThrown,
                "; textStatus: ", textStatus);
        });
    }

    var lastBookmark = sortedBookmarks[sortedBookmarks.length-1];

    var throttleFxn = function() {
        fireOnPageScrollEvent();

        var lastCnumSeen = undefined;
        for (var i = 0; i < sortedBookmarks.length; i++) {
            var bookmark = sortedBookmarks[i];
            var bookmarkEl = $('#'+bookmark);
            if (!bookmarkEl.height()) {
                continue;
            }
            var isChapterDiv = false;
            if (bookmark.startsWith("chapter-")) {
                lastCnumSeen = bookmark.substring("chapter-".length);
                isChapterDiv = true;
            }
            var offsetTop = betterOffset(false, bookmarkEl,
                scrollEl !== window ? $(scrollEl) : undefined);
            if (offsetTop >= 0) {
                // this check doesn't only save us unnecessary updates,
                // but also helps deal with undesirable calls after
                // initial scrolling is initiated by check() function.
                if (bookmark !== lastBookmark) {
                    saveInternalBookmark(bcode, version, bookmark, lastCnumSeen);
                    lastBookmark = bookmark;
                }
                return;
            }
            if (!isChapterDiv) {
                if (offsetTop + bookmarkEl.height() / 2 > 0) {
                    // this check doesn't only save us unnecessary updates,
                    // but also helps deal with undesirable calls after
                    // initial scrolling is initiated by check() function.
                    if (bookmark !== lastBookmark) {
                        saveInternalBookmark(bcode, version, bookmark, lastCnumSeen);
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
        var initialBookmark = fetchInternalBookmark(bcode, version)
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
            scrollEl.scrollTo(0, 0);
        }
        lastBookmark = initialBookmark;
        $(scrollEl).scroll(throttle(1000, throttleFxn));
        doneCb();
    };

    // don't install scroll listener until page loading completes.
    check();
}

function modifyFootnotes(bcode, booktextEl, sortedBookmarks,
        ignoredFootnotes, editEnabled) {
    var regex = new RegExp(`${DEFAULT_VERSION}-bookmark-` + '\\d+' + "-n");
    for (bookmark of sortedBookmarks) {
        if (!regex.test(bookmark)) {
            continue;
        }
        $("#" + bookmark + " > div").each(function() {
            var divEl = this;
            var firstAnchor = $("a", divEl)[0];
            var anchorHrefShort = firstAnchor.href.substring(
                firstAnchor.href.lastIndexOf("#"));

            // set initial value
            if (ignoredFootnotes.includes(anchorHrefShort)) {
                $(anchorHrefShort).parent().addClass("ignored-footnote");
                $(divEl).addClass("ignored-footnote");
            }

            // insert toggle button if mode suggests so
            if (!editEnabled) {
                return;
            }

            $("<p><button type='button' class='add-remove'>Ignore/Keep</button></p>").appendTo(divEl);
            $(".add-remove", divEl).click(function() {
                var that = this;
                $.ajax({
                    url: "/footnote-status/toggle",
                    method: "POST",
                    headers: {
                        "X-bcode": bcode,
                        "X-href": anchorHrefShort,
                        "X-current": $(divEl).hasClass("ignored-footnote")
                    },
                    beforeSend: function() {
                        $(that).attr('disabled', 'disabled');
                    },
                    complete: function() {
                        $(that).removeAttr('disabled');
                    },
                    success: function(data) {
                        if (data.ignore) {
                            $(anchorHrefShort).parent().addClass("ignored-footnote");
                            $(divEl).addClass("ignored-footnote");
                        }
                        else {
                            $(anchorHrefShort).parent().removeClass("ignored-footnote");
                            $(divEl).removeClass("ignored-footnote");
                        }
                    },
                    error: function(jqXHR, textStatus, errorThrown) {
                        console.log("Failed to toggle footnote status: errorThrown: ", errorThrown,
                            "; textStatus: ", textStatus);
                    }
                });
            });
        });
    }
}

function identifyAndSortInternalBookmarks(list) {
    var sortedBookmarks = []
    for (item of list) {
        var posInfo = $('#'+item).offset()
        if (posInfo) {
            sortedBookmarks.push([item, posInfo.top])
        }
    }
    sortedBookmarks.sort((a, b) => a[1] - b[1]);
    return sortedBookmarks.map(function(x) { return x[0]; });
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

function fetchInternalBookmark(bcode, version) {
    if (window.biblei) {
        return biblei.javaGetInternalBookmark(bcode, version);
    }
}

function saveInternalBookmark(bcode, version, bookmark, cnum) {
    if (window.biblei) {
        biblei.javaSaveInternalBookmark(bcode, version, bookmark, cnum);
    }
}

function parseBookCodeFromUrl() {
    var slashIndex = location.href.lastIndexOf('/');
    var bcode = location.href.substring(slashIndex+1, slashIndex+4);
    return bcode;
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

/* Copied from https://stackoverflow.com/a/46677056 */
/*
function: betterOffset
hint: Allows you to calculate dynamic and static offset whether they are in a div container with overscroll or not.

            name:           type:               option:         notes:
@param      s (static)      boolean             required        default: true | set false for dynamic
@param      e (element)     string or object    required
@param      v (viewer)      string or object    optional        If you leave this out, it will use $(window) by default. What I am calling the 'viewer' is the thing that scrolls (i.e. The element with "overflow-y:scroll;" style.).

@return                  numeric
*/
function betterOffset(s, e, v) {
    // Set Defaults
    s = (typeof s === 'boolean') ? s : true;
    e = (typeof e === 'object') ? e : $(e);
    if (v !== undefined) {
        v = (typeof v === 'object') ? v : $(v);
    }
    else {
        v = false;
    }

    // Set Variables
    var w = $(window);              // window object
    var wp = w.scrollTop();         // window position
    var eo = e.offset().top;        // element offset
    if (v) {
        var vo = v.offset().top;    // viewer offset
        var vp = v.scrollTop();     // viewer position
    }

    // Calculate
    if (s) {
        return (v) ? (eo - vo) + vp : eo;
    }
    else {
        return (v) ? eo - vo : eo - wp;
    }
}
