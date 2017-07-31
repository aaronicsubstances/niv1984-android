$(function(){
    loadChapter(1);
});

function loadChapter(chapter) {
    var e = getQueryVariable('e');
    var i = e.lastIndexOf('/');
    e = e.substring(0, i+1);
    i = chapter + '.html';
    while (i.length < 8) {
        i = '0' + i;
    }
    e = e + i;
    $.ajax({ url: e, type: 'GET', dataType: 'html',
        success: function(data){
            $('#wrapper').append(data);
            loadChapter(chapter+1);
        }
    });
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