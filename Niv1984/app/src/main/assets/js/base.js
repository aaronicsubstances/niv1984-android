$(function(){
    var lastCnum = -1;
    //var mode = getQueryVariable('mode');
    $(window).scroll(function(){
        // Get container scroll position
        var fromTop = $(this).scrollTop();

        // Get the current chapter: last hidden chapter.
        var targetCnum = 0;
        for (var cnum = 1; cnum <= chapterCounts.length; cnum++) {
            var fragId = createChapFragId(cnum);
            var offsetTop = $('#'+fragId).offset().top;
            if (offsetTop <= fromTop) {
                targetCnum = cnum;
            }
            else {
                break;
            }
        }
        if (window.biblei) {
            if (targetCnum != lastCnum) {
                biblei.javaCacheCurrentChapter(targetCnum);
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

function createChapFragId(cnum) {
    return "chapter-" + cnum;
}