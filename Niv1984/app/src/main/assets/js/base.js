$(function(){
    var slashIndex = location.href.lastIndexOf('/');
    var bnum = parseInt(location.href.substring(slashIndex+1, slashIndex+3));

    var lastCnum = -1;
    $(window).scroll(function(){
        // Get container scroll position
        var fromTop = $(this).scrollTop();

        // Get the current chapter: last hidden chapter.
        var targetCnum = 0;
        for (var cnum = 1; cnum <= chapterCounts.length; cnum++) {
            var fragId = "chapter-" + cnum;
            var offsetTop = $('#'+fragId).offset().top;
            // just a little pixel off by 10 so chapter headings are rightly seen as
            // current.
            if (offsetTop-10 < fromTop) {
                targetCnum = cnum;
            }
            else {
                break;
            }
        }
        if (window.biblei) {
            if (targetCnum != lastCnum) {
                biblei.javaCacheCurrentChapter(bnum, targetCnum);
                lastCnum = targetCnum;
            }
        }
    });
});

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