
var map;
var _json;

var source = new ol.source.Vector();
var vectorLayer;
var source2 = new ol.source.Vector();
var vectorLayer2;
var source3 = new ol.source.Vector({
    wrapX: false,
});
var vectorLayer3;


var rasterLayer = new ol.layer.Tile({
  source: new ol.source.OSM(),
});

var source_img = new ol.source.ImageStatic({
	url:
	  'http://localhost:8088/dr_off.png',
	crossOrigin: 'anonymous',
	projection: 'EPSG:3857',
	imageExtent: [14157005.425914, 4498212.914733, 14157071.006190, 4498255.665878],
	imageSmoothing: true,
});
var imageLayer;


//타일맵
let eMapUrl = "http://192.168.0.204:8180/eMap/";
var _resolutions = [
	1954.5973888747778,
	977.2986945,
	488.64934725,
	244.324673625,
	122.1623368125,
	61.08116840625,
	30.540584203125,
	15.2702921015625,
	7.63514605078125,
	3.817573025390625,
	1.9087865126953125,
	0.9543932563476563,
	0.47719662817382813,
	0.23859831408691407
];
var _levels = ['L06', 'L07', 'L08', 'L09', 'L10', 'L11', 'L12', 'L13', 'L14', 'L15', 'L16', 'L17', 'L18', 'L19' ];

var source_tile = new ol.source.XYZ({
	projection : "EPSG:5179",
	tileUrlFunction : function(tileCoord) {
			var bounds = this.tileGrid.getTileCoordExtent(tileCoord);
			var origin = this.tileGrid.getOrigin();
			var resolution = this.tileGrid.getResolution(tileCoord[0]);
			var tileSize = this.tileGrid.getTileSize();
			var x = Math.round((bounds[0] - origin[0]) / (resolution * tileSize));
			var y = Math.round((bounds[1] - origin[1]) / (resolution * tileSize));
			y = y + Math.pow(2, tileCoord[0] + 6); // Correction factor
			var mapUri = eMapUrl + "{z}/{x}/{y}.png";

			var _uri = mapUri.replace('{z}', _levels[tileCoord[0]].toString())
											.replace('{x}',x.toString())
											.replace('{y}',y.toString());
			return _uri;
	},
	tileGrid: new ol.tilegrid.TileGrid({
			tileSize : 256,
			resolutions : _resolutions,
			origin : [ -200000, 4000000 ]
	}),
	wrapX : false
});

var emap_layer = new ol.layer.Tile({
	title : 'eMap',
	name: 'eMap',
	source : source_tile,
	layerName : "baroE_korea",
	layerCategory : 'background'
});



//이전포지션
var pre_vectorLayer2;
var	pre_marker;






var initMap = function(){


	//맵초기화
	map = new ol.Map({
	  layers: [emap_layer],
	  target: document.getElementById('map'),
	  view: new ol.View({
		center: ol.proj.fromLonLat([126.980366, 37.52654]),
		zoom: 15,
	  }),
	})

    //스케일바
	map.addControl(new ol.control.ScaleLine());

}


//포인트 순차적으로 표시 - 이동처럼 보이게
var  moveMaker = function(json){

	var format = new ol.format.GeoJSON();
	var features = format.readFeatures(json);
	var street = features[0];

	// convert to a turf.js feature
	var turfLine = format.writeFeatureObject(street);

	var distance = 0.2;
	// get the line length in kilometers
	var length = turf.lineDistance(turfLine, 'kilometers');
	for (var i = 1; i <= length / distance; i++) {
		var turfPoint = turf.along(turfLine, i * distance, 'kilometers');

		// convert the generated point to a OpenLayers feature
		var marker = format.readFeature(turfPoint);
		marker.getGeometry().transform('EPSG:4326', 'EPSG:3857');

		//source.clear();
		try{
			source.removeFeature(pre_marker);
		}catch(e){}

		source.addFeature(marker);
		pre_marker = marker;
		//alert(1);

	}
}


//포인트 순차적으로 표시 - 이동처럼 보이게
var  move = function(i){
    try{
	var format = new ol.format.GeoJSON();
	var features = format.readFeatures(_json);
	var street = features[0];

	// convert to a turf.js feature
	var turfLine = format.writeFeatureObject(street);

	var distance = 0.01;
	// get the line length in kilometers
	var length = turf.lineDistance(turfLine, 'kilometers');

    var p = i % (length / distance); // 범위넘어서면 처음부터

	var turfPoint = turf.along(turfLine, p * distance, 'kilometers');

	// convert the generated point to a OpenLayers feature
	var marker = format.readFeature(turfPoint);
	marker.getGeometry().transform('EPSG:4326', 'EPSG:3857');



	// try{
		// map.removeLayer(pre_vectorLayer2);
	// }catch(e){}
	//console.log("i - " + i);
	//포인트벡터초기화 맵에추가
	source2.clear();
	source2.addFeature(marker);
	// map.addLayer(vectorLayer2);
	// pre_vectorLayer2 = vectorLayer2;
    }catch(e){}
}


var _sleep = function(t){
	var cnt = 0;
	while(true){
		//console.log("_sleep - " + cnt);
		if(cnt++ > t*1000)	break;
	}
}








var fn_layer_Load = function(){
	//로드레이어
	vectorLayer = new ol.layer.Vector({
	  source: source,
	});

	var format = new ol.format.GeoJSON();
	var features = format.readFeatures(_json);
	var street = features[0];

	street.getGeometry().transform('EPSG:4326', 'EPSG:3857');
	source.addFeature(street);

    map.addLayer(vectorLayer);
}

var fn_layer_Pos = function(){
	//포지션레이어
	vectorLayer2 = new ol.layer.Vector({
	  source: source2,
	});

    map.addLayer(vectorLayer2);
}

var fn_layer_Img = function(){
	//이미지레이어
	imageLayer = new ol.layer.Image({		
	  source: source_img,
	  opacity: 0.3,
	});

    map.addLayer(imageLayer);
}

var fn_layer_Bcn = function(){
    //비콘삐뽀

	vectorLayer3 = new ol.layer.Vector({
	  source: source3,
	});

    map.addLayer(vectorLayer3);

    source3.on('addfeature', function (e) {
      flash(e.feature);
    });
}


var rot_L = function(){
     var view = map.getView();
     view.animate({
        rotation: view.getRotation() + Math.PI / 2,
      });
}
var rot_R = function(){
     var view = map.getView();
     view.animate({
        rotation: view.getRotation() - Math.PI / 2,
      });
}
var rot_Ang = function(ang){
    var angle = 0.0;
    try{
        angle = parseFloat(ang);
    }catch(e){}
     var view = map.getView();
     view.animate({
        rotation: view.getRotation() + Math.PI / 180 * ang,
      });
}



function addRandomFeature() {
    var y = 37.52654;
    var x = 126.980366;

    var geom = new ol.geom.Point(ol.proj.fromLonLat([x, y]));
    var feature = new ol.Feature(geom);
    source3.addFeature(feature);
}

var duration = 3000;
function flash(feature) {
  var start = new Date().getTime();
  var listenerKey = rasterLayer.on('postrender', animate);

  function animate(event) {
    var vectorContext = ol.render.getVectorContext(event);
    var frameState = event.frameState;
    var flashGeom = feature.getGeometry().clone();
    var elapsed = frameState.time - start;
    var elapsedRatio = elapsed / duration;
    // radius will be 5 at start and 30 at end.
    var radius = ol.easing.easeOut(elapsedRatio) * 25 + 5;
    var opacity = ol.easing.easeOut(1 - elapsedRatio);

    var style = new ol.style.Style({
      image: new ol.style.Circle({
        radius: radius,
        stroke: new ol.style.Stroke({
          color: 'rgba(255, 0, 0, ' + opacity + ')',
          width: 0.25 + opacity,
        }),
      }),
    });

    vectorContext.setStyle(style);
    vectorContext.drawGeometry(flashGeom);
    if (elapsed > duration) {
      //unByKey(listenerKey);
      return;
    }
    // tell OpenLayers to continue postrender animation
    map.render();
  }
}





        //삐뽀레이어
		//fn_layer_Bcn();
		//




$(document).ready(function() {
	
	//데이터로딩
/* 	fetch('http://1.221.243.162:8088/file_path/data/geojson/roads-seoul.geojson')
	  .then(function (response) {
		return response.json();
	  })
	  .then(function (json) {
		_json = json;

		//배경지도
		initMap();

		//로드레이어
		fn_layer_Load();

		//마커레이어
		fn_layer_Pos();

        //삐뽀레이어
		//fn_layer_Bcn();
		//addRandomFeature();
	});
 */
	

	$.getJSON("http://localhost:8088/roads-seoul.geojson", function(json){
		_json = json;

		//배경지도
		initMap();

		//로드레이어
		fn_layer_Load();

		//마커레이어
		fn_layer_Pos();

		//이미지레이어
		fn_layer_Img();

	});

	var cnt = 0;
	setInterval(function(){
		move(cnt++);
	}, 1000);



});
