//proj4.defs("EPSG:3857","+proj=merc +a=6378137 +b=6378137 +lat_ts=0.0 +lon_0=0.0 +x_0=-7.0 +y_0=-7 +k=1.0 +units=m +nadgrids=@null +wktext  +no_defs");
proj4.defs("EPSG:4326","+proj=longlat +datum=WGS84 +x_0=37.0 +y_0=37  +no_defs");
/* proj4.defs(
	"EPSG:27700",
	"+proj=tmerc +lat_0=49 +lon_0=-2 +k=0.9996012717 " +
	  "+x_0=400000 +y_0=-100000 +ellps=airy " +
	  "+towgs84=446.448,-125.157,542.06,0.15,0.247,0.842,-20.489 " +
	  "+units=m +no_defs"
); */
ol.proj.proj4.register(proj4);
// var proj4326 = ol.proj.get('EPSG:4326');
// proj4326.setExtent([0, 0, 700000, 1300000]);



//이미지레이어범위
//let projExtent = [14157005.425914, 4498212.914733]; //3857
//let projExtent = [14157005.425914, 4498212.914733, 14157071.006190, 4498255.665878]; //3857
// let projExtent = [2.22, 0.65, 127.175741, 37.423578]; //27700
//let projExtent = [127.175111, 37.423295, 127.175741, 37.423578]; //4326
let projExtent = [127.17467, 37.423025, 127.17493, 37.423105]; //4326



//좌표계회전 함수
function rotateProjection(projection, angle, extent) {
	function rotateCoordinate(coordinate, angle, anchor) {
	  var coord = ol.coordinate.rotate(
		[coordinate[0] - anchor[0], coordinate[1] - anchor[1]],
		angle
	  );
	  return [coord[0] + anchor[0], coord[1] + anchor[1]];
	}
  
	function rotateTransform(coordinate) {
	  return rotateCoordinate(coordinate, angle, ol.extent.getCenter(extent));
	}
  
	function normalTransform(coordinate) {
	  return rotateCoordinate(coordinate, -angle, ol.extent.getCenter(extent));
	}
  
	var normalProjection = ol.proj.get(projection);
  
	var rotatedProjection = new ol.proj.Projection({
	  code:
		normalProjection.getCode() +
		":" +
		angle.toString() +
		":" +
		extent.toString(),
	  units: normalProjection.getUnits(),
	  extent: extent
	});
	ol.proj.addProjection(rotatedProjection);
  
	ol.proj.addCoordinateTransforms(
	  "EPSG:4326",
	  rotatedProjection,
	  function(coordinate) {
		return rotateTransform(ol.proj.transform(coordinate, "EPSG:4326", projection));
	  },
	  function(coordinate) {
		return ol.proj.transform(normalTransform(coordinate), projection, "EPSG:4326");
	  }
	);
  
	ol.proj.addCoordinateTransforms(
	  "EPSG:3857",
	  rotatedProjection,
	  function(coordinate) {
		return rotateTransform(ol.proj.transform(coordinate, "EPSG:3857", projection));
	  },
	  function(coordinate) {
		return ol.proj.transform(normalTransform(coordinate), projection, "EPSG:3857");
	  }
	);
  
	// also set up transforms with any projections defined using proj4
	if (typeof proj4 !== "undefined") {
	  var projCodes = Object.keys(proj4.defs);
	  projCodes.forEach(function(code) {
		var proj4Projection = ol.proj.get(code);
		if (!ol.proj.getTransform(proj4Projection, rotatedProjection)) {
		  ol.proj.addCoordinateTransforms(
			proj4Projection,
			rotatedProjection,
			function(coordinate) {
			  return rotateTransform(
				ol.proj.transform(coordinate, proj4Projection, projection)
			  );
			},
			function(coordinate) {
			  return ol.proj.transform(
				normalTransform(coordinate),
				projection,
				proj4Projection
			  );
			}
		  );
		}
	  });
	}
  
	return rotatedProjection;
  }



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
	projection: rotateProjection("EPSG:4326", Math.PI / 180*(-4), projExtent),
	imageExtent: projExtent, 
	imageSmoothing: true,
});

var imageLayer;
let parser = new ol.format.WMTSCapabilities();


//이전포지션
var pre_vectorLayer2;
var	pre_marker;









var initMap = function(){


	//브이월드 WMTS 타일맵
	fetch("http://localhost:8088/WMTSCapabilities.xml").then(function (response) {
			return response.text();
	}).then(function (text) {
		var result = parser.read(text);
		
		var options = ol.source.WMTS.optionsFromCapabilities(result, {
			layer: 'Base',
			matrixSet: 'EPSG:3857',
		});


		//맵초기화
		map = new ol.Map({
			layers: [
 				new ol.layer.Tile({
					opacity: 1,
					source: new ol.source.WMTS(options)
				}) 
 			],
			target: document.getElementById('map'),
			view: new ol.View({
			center: ol.proj.fromLonLat([127.1748, 37.4230]), //디알씨티에스
			zoom: 22,
			}),
		})
	
		//스케일바
		map.addControl(new ol.control.ScaleLine());



		//로드레이어 데이터
		$.getJSON("http://localhost:8088/roads-seoul.geojson", function(json){
			_json = json;
	
			//로드레이어
			fn_layer_Load();

			//마커레이어
			fn_layer_Pos();
	
		});

		//마커이동 
		var cnt = 0;
		setInterval(function(){
			move(cnt++);
		}, 1000);
	

		//이미지레이어
		fn_layer_Img();


		moveMaker([127.1747217,37.4229652]);

		// moveMaker([127.17474, 37.42303]); //좌하 
		// moveMaker([127.17473, 37.42309]); //좌상 
		// moveMaker([127.17492, 37.42311]); //우상
		// moveMaker([127.17492, 37.42304]); //우하
		
	});



}


//포인트 순차적으로 표시 - 이동처럼 보이게
var  moveMaker = function(cord){


	// convert the generated point to a OpenLayers feature
	var marker = new ol.Feature({
		geometry: new ol.geom.Point(cord),
	  });
	marker.getGeometry().transform('EPSG:4326', 'EPSG:3857');

	//source.clear();
	// try{
	// 	source2.removeFeature(pre_marker);
	// }catch(e){}

	source.addFeature(marker);
	//pre_marker = marker;
	//alert(1);

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
	  opacity: 0.5,
	  rotation: 3.14,
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

 
	initMap();




});
