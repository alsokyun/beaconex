
/// 로컬파일 로딩
let gfn_loadFile = function(file_path, callback){

	var rawFile = new XMLHttpRequest();
    rawFile.open("GET", file_path, false);
    rawFile.onreadystatechange = function ()
    {
        if(rawFile.readyState === 4)
        {
            if(rawFile.status === 200 || rawFile.status == 0)
            {
                var allText = rawFile.responseText;
				//alert(c);
				if(typeof callback === "function"){
					callback(allText);
				}
            }
        }
    }
    rawFile.send(null);	
}



function gfn_isNull(str) {
	if (str == null)
		return true;
	if (str == "NaN")
		return true;
	if (new String(str).valueOf().replace(/-/g,"").search("undef") > -1)
		return true;
	var chkStr = new String(str);
	if (chkStr.valueOf().replace(/-/g,"").search("undef") > -1)
		return true;
	if (chkStr == null)
		return true;
	if (chkStr.toString().length == 0)
		return true;
	return false;
}
