proj4.defs("EPSG:4326","+proj=longlat +datum=WGS84 +x_0=37.0 +y_0=37  +no_defs");
/* proj4.defs(
	"EPSG:27700",
	"+proj=tmerc +lat_0=49 +lon_0=-2 +k=0.9996012717 " +
	  "+x_0=400000 +y_0=-100000 +ellps=airy " +
	  "+towgs84=446.448,-125.157,542.06,0.15,0.247,0.842,-20.489 " +
	  "+units=m +no_defs"
); */
//proj4.defs("EPSG:3857","+proj=merc +a=6378137 +b=6378137 +lat_ts=0.0 +lon_0=0.0 +x_0=-7.0 +y_0=-7 +k=1.0 +units=m +nadgrids=@null +wktext  +no_defs");


ol.proj.proj4.register(proj4);
// let proj4326 = ol.proj.get('EPSG:4326');
// proj4326.setExtent([0, 0, 700000, 1300000]);



//이미지레이어범위
//let projExtent = [14157005.425914, 4498212.914733]; //3857
//let projExtent = [14157005.425914, 4498212.914733, 14157071.006190, 4498255.665878]; //3857
// let projExtent = [2.22, 0.65, 127.175741, 37.423578]; //27700
//let projExtent = [127.175111, 37.423295, 127.175741, 37.423578]; //4326
let projExtent = [127.17467, 37.423025, 127.17493, 37.423105]; //4326




//맵전역객체
let map;




/**
 * 레이어 선언부분
 */
//로드 레이어
let streetSource = new ol.source.Vector();
let streetLayer;

//마커포지션 레이어
let posiSource = new ol.source.Vector();
let posiLayer;

//비콘삐뽀 레이어
let source3 = new ol.source.Vector();
//let vectorLayer3 = new ol.layer.Vector({
//	source: source3,
////	opacity: 0.5,
//	style: new ol.style.Style({
//        fill: new ol.style.Fill({
//            color: 'rgba(255,255,255,0.2)'
//        }),
//        image: new ol.style.Circle({
//            radius: 17,
//            fill: new ol.style.Fill({
//                color: "#3399ff",
//            }),
//            stroke: new ol.style.Stroke({
//                color: '#0000ff',
//                width: 1
//            }),
//        }),
//    }),
//});

let vectorLayer3 = new ol.layer.Vector({
	source: source3,
	opacity: 0.5,
	style: function (feature) {
		var geometry = feature.getGeometry();
		console.log("radius - " + feature.getProperties().radius);
		return new ol.style.Style({
			fill: new ol.style.Fill({
				color: 'rgba(0,0,255,0.1)'
			}),
			image: new ol.style.Circle({
				radius: 10,
				fill: new ol.style.Fill({
					color: "#3399ff",
				}),
			}),
            stroke: new ol.style.Stroke({
                color: '#0000ff',
                width: 1
            }),

            text: new ol.style.Text({
                font: '8px Verdana',
                scale: 2,
                text: feature.getProperties().radius === undefined ? "" :  feature.getProperties().radius + " m",
                fill: new ol.style.Fill({ color: '#000' }),
                //stroke: new ol.style.Stroke({ color: 'yellow', width: 3 })
            }),
        });

	}
});



//배경지도 타일맵 레이어
let rasterLayer = new ol.layer.Tile({
  source: new ol.source.OSM(),
});

//사무실이미지 레이어
let imgSource = new ol.source.ImageStatic({
	url:
	  'dr_off.png',
	crossOrigin: 'anonymous',
	projection: rotateProjection("EPSG:4326", Math.PI / 180*(-4), projExtent),
	imageExtent: projExtent,
	imageSmoothing: true,
});
let imageLayer;

//라인Path 레이어
let linePathSource = new ol.source.Vector();
let linePathLayer = new ol.layer.Vector({
	source: linePathSource,
	style: new ol.style.Style({
		fill: new ol.style.Fill({
			color: 'rgba(255,255,255,0.2)'
		}),
		stroke: new ol.style.Stroke({
			color: '#ff99ff',
			width: 10
		}),
		image: new ol.style.Circle({
			radius: 7,
			fill: new ol.style.Fill({
				color: "#ffcc33",
			})
		}),
	}),
});

//라인마커 레이어
let lineMarkerSource = new ol.source.Vector();
let lineMarkerLayer = new ol.layer.Vector({
	source: lineMarkerSource,
	 style: function(feature){
	 	let styles = [];

	 	feature.getGeometry().forEachSegment(function(start, end){
	 		styles.push(new ol.style.Style({
	 			geometry: new ol.geom.LineString([start, end]),
	 			fill: new ol.style.Fill({
	 				color: 'rgba(0,255,0,0.5)'
	 			}),
	 			stroke: new ol.style.Stroke({
	 				color: '#00ff00',
	 				width: 10
	 			}),
	 		}));
	 	});
	 	return styles;
	 },
});









/**
 * 전역 변수/객체 선언
 */
let _json;
let parser = new ol.format.WMTSCapabilities();
let GeoJSON = new ol.format.GeoJSON();

//이전포지션
let	pre_marker;

//편집객체
let draw;
let snap;
let modify;








/**
 * Function 선언
 */


//지도초기화
let initMap = function(){

	//브이월드 WMTS 타일맵
	gfn_loadFile("WMTSCapabilities.xml", function(text){
		let result = parser.read(text);

		let options = ol.source.WMTS.optionsFromCapabilities(result, {
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
			center: ol.proj.fromLonLat([127.1748, 37.42305]), //디알씨티에스
			zoom: 21,
			}),
		})

		//스케일바
		map.addControl(new ol.control.ScaleLine());



		//로드레이어
		fn_layer_Load();

		//마커레이어
		fn_layer_Pos();



		//이미지레이어
		fn_layer_Img();



		// movec([127.17474, 37.42303]); //좌하
		// moveMarker([127.17473, 37.42309]); //좌상
		// moveMarker([127.17492, 37.42311]); //우상
		// moveMarker([127.17492, 37.42304]); //우하
	});




}


// 포인트 표시
let pointMarker = function(cord){
    console.log("pointMarker .. " + cord);
    if(gfn_isNull(cord))    return;

	// convert the generated point to a OpenLayers feature
	let marker = new ol.Feature({
		geometry: new ol.geom.Point(cord),
	  });
	//marker.getGeometry().transform('EPSG:4326', 'EPSG:3857');

	try{
	 	posiSource.removeFeature(pre_marker);
	}catch(e){}
	try{
        posiSource.addFeature(marker);
        pre_marker = marker;
	}catch(e){}
}

// 포인트선분으로 표시
let lineMarker = function(cord){
    console.log("lineMarker .. " + cord);
    if(gfn_isNull(cord))    return;

	// convert the generated point to a OpenLayers feature
	let marker = new ol.Feature({
		geometry: new ol.geom.LineString([gfn_isNull(cur_xy)? cord : cur_xy, cord]),
	  });

	try{
        lineMarkerSource.addFeature(marker);
	}catch(e){}

}




// 격자포인트 표시
let pointLatis = function(cord){
	// convert the generated point to a OpenLayers feature
	let marker = new ol.Feature({
		geometry: new ol.geom.Point(cord),
	  });
	//marker.getGeometry().transform('EPSG:4326', 'EPSG:3857');

	streetSource.addFeature(marker);
	try{
        map.addLayer(streetLayer);
	}catch(e){}
}








//로드레이어
let fn_layer_Load = function(){

	gfn_loadFile("roads-seoul.geojson", function(text){
		_json = JSON.parse(text);

		streetLayer = new ol.layer.Vector({
		  source: streetSource,
		});

		let features = GeoJSON.readFeatures(_json);
		let street = features[0];

		street.getGeometry().transform('EPSG:4326', 'EPSG:3857');
		streetSource.addFeature(street);

        streetLayer.setZIndex(3);
		map.addLayer(streetLayer);

	});

	// $.getJSON("http://localhost:8088/roads-seoul.geojson", function(json){
	// });


}

let fn_layer_Pos = function(){
	//포지션레이어
	posiLayer = new ol.layer.Vector({
	  source: posiSource,
       style: new ol.style.Style({
         fill: new ol.style.Fill({
             color: '#ff33cc'
         }),
         stroke: new ol.style.Stroke({
             color: '#66ff66',
             width: 3
         }),
         image: new ol.style.Circle({
             radius: 7,
             fill: new ol.style.Fill({
                 color: "#ff0000",
             })
         }),
	    }),
	});

    posiLayer.setZIndex(4);
    map.addLayer(posiLayer);
}

let fn_layer_Img = function(){
	//이미지레이어
	imageLayer = new ol.layer.Image({		
	  source: imgSource,
	  opacity: 0.5,
	  rotation: 3.14,
	});

	map.addLayer(imageLayer);
}

let fn_layer_Bcn = function(){
    //비콘삐뽀

	vectorLayer3 = new ol.layer.Vector({
	  source: source3,
	});
    map.addLayer(vectorLayer3);

    source3.on('addfeature', function (e) {
      flash(e.feature);
    });
}











$(document).ready(function() {
	
	//지도초기화
	initMap();

	//격자불러오기
	gfn_loadFile("dr_path_4.geojson", function(text){
		let drawVector_Ary = JSON.parse(text);;

		latisAry = [];
		$.each(drawVector_Ary, function(idx, val){
			let ft = GeoJSON.readFeatures(val);
			$.each(ft[0].getGeometry().getCoordinates(), function(idx, val){
				latisAry.push(val);
			});
		});
	});

    //지도회전
    rot_Ang(90);
	
	
	/**
	 * 이벤트설정
	 */



	//라인편집해제
	$("#btn3").click(function(){

		try{
			map.removeLayer(streetLayer);
			map.removeLayer(linePathLayer);
		}catch(e){}
	});




	//저장된 Path 로딩
	$("#btn4").click(function(){

        gfn_loadFile("dr_path_4.geojson", function(text){
            let drawVector_Ary = JSON.parse(text);;

            //라인스트링 표시
            $.each(drawVector_Ary, function(idx, val){
                let ft = GeoJSON.readFeatures(val);
                //ft[0].getGeometry().transform('EPSG:4326','EPSG:3857');//좌표계로 변환
                linePathSource.addFeatures(ft);
            });

            try{
                linePathLayer.setZIndex(2);
                map.addLayer(linePathLayer);
            }catch(e){}

            //resolved 포인트확인
            $.each(drawVector_Ary, function(idx, val){
                let ft = GeoJSON.readFeatures(val);
                $.each(ft[0].getGeometry().getCoordinates(), function(idx, val){
                    pointLatis(val);
                });
            });

            try{
                lineMarkerLayer.setZIndex(2);
                map.addLayer(lineMarkerLayer);
            }catch(e){}
        });

	});



	//Left
	$("#btn1").click(function(){
		rot_Ang(-30);
	});
	//Right
	$("#btn2").click(function(){
		rot_Ang(30);
	});


});
