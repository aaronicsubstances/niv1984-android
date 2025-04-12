DEFAULT_VERSION = "cpdv"

$(function() {
    const bcode = parseBookCodeFromUrl();
    const firstBookTextEl = $(".booktext");
    let addQuery = getQueryVariable("add");
    const additionalVersions = [];
    if (addQuery.indexOf(",") === -1) {
        additionalVersions.push(addQuery);
    }
    else {
        const excludedVersions = getQueryVariable("allExcl").split(",").map(function(v) {
            return v.toLowerCase();
        });
        for (const candidate of addQuery.split(",")) {
            if (!excludedVersions.some(function(v) { return candidate.startsWith(v); })) {
                additionalVersions.push(candidate);
            }
        }
    }
    if (!additionalVersions.length) {
        caterForScrolling(bcode, DEFAULT_VERSION, firstBookTextEl[0], window,
            fireOnPageLoadCompleted);
        return;
    }
    let doneCbCallCnt = 0;
    const doneCb = function() {
        doneCbCallCnt++;
        if (doneCbCallCnt === additionalVersions.length) {
            $("#wrapper").css({ display: "flex", gap: "0.5rem" });
            $(".booktext").css({
                height: "100vh",
                overflowY: "scroll",
                flex: "1",
            });
            caterForScrolling(bcode, DEFAULT_VERSION, firstBookTextEl, firstBookTextEl[0],
                fireOnPageLoadCompleted);
        }
    };
    for (const additionalVersion of additionalVersions) {
        if (additionalVersion === "gnt1992" || additionalVersion === "bbe1965") {
            if (bcode === "ESG") {
                bcode = "EST";
            }
            else if (bcode === "DAG") {
                 bcode = "DAN";
            }
        }
        const urlToLoad = `/${additionalVersion}/${bcode}.html`;
        const nextBookTextEl = $('<div class="booktext"></div>');
        nextBookTextEl.insertAfter($(".booktext").last());
        nextBookTextEl.load(urlToLoad + ' .booktext', function(responseTxt, statusTxt, xhr) {
            if (statusTxt === "success") {
                $('.booktext', nextBookTextEl).removeClass('booktext');
                caterForScrolling(bcode, additionalVersion, nextBookTextEl, nextBookTextEl[0],
                    doneCb);
            }
            else {
                var errMsg = `${xhr.status} ${xhr.statusText}`;
                if (errMsg === "0 error") {
                    errMsg = "NOT AVAILABLE";
                }
                nextBookTextEl.html(`<h2>${errMsg}</h2>`);
                doneCb()
            }
         });
     }
});

function caterForScrolling(bcode, version, booktextEl, scrollEl, doneCb) {
    var encodedBookmarks = JSON.parse($("span.bookmarks", $(booktextEl)).text());
    var sortedBookmarks = identifyAndSortInternalBookmarks(encodedBookmarks);
    if ([DEFAULT_VERSION, "thomson1808", "tcent2022"].includes(version)) {
        $.get('/comments', { bcode, bversion: version }, function(data) {
            var initialComments = new Map();
            for (const item of data.items) {
                initialComments.set(item[0], item[1]);
            }
            insertComments(version, bcode, booktextEl, sortedBookmarks,
                initialComments, data.editEnabled);
        }).fail(function(jqXHR, textStatus, errorThrown) {
            console.log("Failed to load comments: errorThrown: ", errorThrown,
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

function insertComments(version, bcode, booktextEl, sortedBookmarks,
            initialComments, editEnabled) {
    const verseRegex = new RegExp(`${version}-bookmark-` + '\\d+-(\\d+)-(\\d+)');
    const footnotesRegex = new RegExp(`${version}-bookmark-` + '(\\d+)-n');
    for (bookmark of sortedBookmarks) {
        let m = verseRegex.exec(bookmark);
        if (m === null) {
            m = footnotesRegex.exec(bookmark);
        }
        if (m === null) {
            continue;
        }
        let cnum = m[1], vnum = 'n';
        if (m.length > 2) {
            vnum = m[2];
        }
        const commentId = `${cnum}:${vnum}`; // use of hyphen caused it to be interpreted as date in CSV export
        if (!initialComments.has(commentId)) {
            // add demo data
            //initialComments.set(commentId, "n/a");
        }

        const readonlyEl = $(`<div class="comment noEdit"></div>`);
        if (initialComments.get(commentId)) {
            readonlyEl.text(initialComments.get(commentId));
        }
        else {
            readonlyEl.addClass("hidden");
        }
        readonlyEl.appendTo("#" + bookmark);
        if (!editEnabled) {
            continue;
        }
        const newEl = $(`<div class="comment edit">
                <textarea class="hidden" style="width: 90%;" rows="5"></textarea>
                <button class="add" type="button">Add/Edit</button>
                <button class="cancel hidden" type="button">Cancel</button>
                <button class="update hidden" type="button">Update</button>
            </div>`);
        newEl.appendTo("#" + bookmark);
        $("button.add", newEl).click(function() {
            readonlyEl.addClass("hidden");
            $("button.add", newEl).addClass("hidden");
            $("button.cancel", newEl).removeClass("hidden");
            $("button.update", newEl).removeClass("hidden");

            $("textarea", newEl).val(readonlyEl.text());
            $("textarea", newEl).removeClass("hidden");
            $("textarea", newEl).focus();
        });
        $("button.cancel", newEl).click(function() {
            $("button.add", newEl).removeClass("hidden");
            $("button.cancel", newEl).addClass("hidden");
            $("button.update", newEl).addClass("hidden");
            $("textarea", newEl).val(readonlyEl.text());
            $("textarea", newEl).addClass("hidden");
            if (readonlyEl.text()) {
                readonlyEl.removeClass("hidden");
            }
        });
        $("button.update", newEl).click(function() {
            var newComment = $("textarea", newEl).val().trim();
            $.ajax({
                url: "/comments/update",
                method: "POST",
                headers: {
                    "X-version": version,
                    "X-bcode": bcode,
                    "X-id": commentId,
                    "X-val": newComment
                },
                beforeSend: function() {
                    $("button.update").attr('disabled', 'disabled');
                    $("button.cancel").attr('disabled', 'disabled');
                },
                complete: function() {
                    $("button.update").removeAttr('disabled');
                    $("button.cancel").removeAttr('disabled');
                },
                success: function() {
                    $("button.add", newEl).removeClass("hidden");
                    $("button.cancel", newEl).addClass("hidden");
                    $("button.update", newEl).addClass("hidden");
                    $("textarea", newEl).addClass("hidden");
                    readonlyEl.text(newComment);
                    if (newComment) {
                        readonlyEl.removeClass("hidden");
                    }
                },
                error: function(jqXHR, textStatus, errorThrown) {
                    console.log("Failed to update comment: errorThrown: ", errorThrown,
                        "; textStatus: ", textStatus);
                }
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
