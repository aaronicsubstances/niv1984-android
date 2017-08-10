$(function(){
    /*var e = getQueryVariable('e');
    if (e) {
        $("#wrapper").load(e);
    }*/
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

function goToChapter(chapterUrl) {
    var hashIndex = chapterUrl.indexOf("#");
    var hash = chapterUrl.substring(hashIndex+1);
    console.log("got hash: " + hash);
    document.getElementById(hash).scrollIntoView();
}

function goToBook(bookUrl) {
    /*$("body").load(bookUrl + ' #wrapper', function(responseText, textStatus, jqXHR){
        biblei.jsDoneLoading(textStatus);
    });*/
    $.ajax(bookUrl, {
        dataType: "html",
        success: function(data, textStatus) {
            $('#wrapper').html(
                $("<div>").append($.parseHTML(data)).find('#wrapper')
            );
            biblei.jsDoneLoading(textStatus);
        },
        error: function() {
            location.href = bookUrl;
        }
    });
}