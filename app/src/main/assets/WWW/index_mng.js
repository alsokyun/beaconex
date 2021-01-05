/**
 * Path 편집 관리화면
 */

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
let source1 = new ol.source.Vector();
let vectorLayer;

//마커포지션 레이어
let source2 = new ol.source.Vector();
let vectorLayer2;

//삐뽀 레이어
let source3 = new ol.source.Vector({
    wrapX: false,
});
let vectorLayer3;

//배경지도 타일맵 레이어
let rasterLayer = new ol.layer.Tile({
  source: new ol.source.OSM(),
});

//사무실이미지 레이어
let source_img = new ol.source.ImageStatic({
	url:
	  'dr_off.png',
	crossOrigin: 'anonymous',
	projection: rotateProjection("EPSG:4326", Math.PI / 180*(-4), projExtent),
	imageExtent: projExtent, 
	imageSmoothing: true,
});
let imageLayer;

//라인편집 레이어
let drawVectorSource = new ol.source.Vector();
let drawVectorLayer = new ol.layer.Vector({
	source: drawVectorSource,
	style: new ol.style.Style({
		fill: new ol.style.Fill({
			color: 'rgba(255,255,255,0.2)'		
		}),
		stroke: new ol.style.Stroke({
			color: '#ff99ff',
			width: 10
		}),
		// image: new ol.style.Circle({
		// 	radius: 7,
		// 	fill: new ol.style.Fill({
		// 		color: "#ffcc33",
		// 	})
		// }),
	}),	
});









/**
 * 전역 변수/객체 선언
 */
let _json;
let parser = new ol.format.WMTSCapabilities();
let GeoJSON = new ol.format.GeoJSON();

//이전포지션
let pre_vectorLayer2;
var	pre_marker;

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
			center: ol.proj.fromLonLat([127.1748, 37.4230]), //디알씨티에스
			zoom: 22,
			}),
		})
	
		//스케일바
		map.addControl(new ol.control.ScaleLine());



		//로드레이어
		fn_layer_Load();

		//마커레이어
		fn_layer_Pos();

		//마커이동 
		let cnt = 0;
		setInterval(function(){
			move(cnt++);
		}, 1000);
	

		//이미지레이어
		fn_layer_Img();


		// pointMaker([127.17487910037713,37.42310490165161]);
		// pointMaker([127.17487106339897,37.42309890571249	]);
		// pointMaker([127.17488319176584,37.4230935143468  ]);
		// pointMaker([127.17481258966491,37.42306528757972 ]);
		// pointMaker([127.1748108776806,37.423063707964474 ]);
		// pointMaker([127.17486723854375,37.42309845412717 ]);
		// pointMaker([127.17487542284822,37.42309697143114 ]);
		// pointMaker([127.17487553259048,37.423097089788634]);
		// pointMaker([127.17487375480032,37.423097952174125]);
		// pointMaker([127.1748646811337,37.42309552760887  ]);
		// pointMaker([127.17486219937012,37.423097824455766]);
		// pointMaker([127.17486386209274,37.42309944709116 ]);

		// pointMaker([127.17474, 37.42303]); //좌하 
		// pointMaker([127.17473, 37.42309]); //좌상 
		// pointMaker([127.17492, 37.42311]); //우상
		// pointMaker([127.17492, 37.42304]); //우하
	});




}


//포인트 순차적으로 표시 - 이동처럼 보이게
let  pointMaker = function(cord){


	// convert the generated point to a OpenLayers feature
	let marker = new ol.Feature({
		geometry: new ol.geom.Point(cord),
	  });
	marker.getGeometry().transform('EPSG:4326', 'EPSG:3857');

	//source.clear();
	// try{
	// 	source2.removeFeature(pre_marker);
	// }catch(e){}

	source1.addFeature(marker);
	//pre_marker = marker;
	//alert(1);

}







//로드레이어
let fn_layer_Load = function(){

	gfn_loadFile("roads-seoul.geojson", function(text){
		_json = JSON.parse(text);

		vectorLayer = new ol.layer.Vector({
		  source: source1,
		});
	
		let features = GeoJSON.readFeatures(_json);
		let street = features[0];
	
		street.getGeometry().transform('EPSG:4326', 'EPSG:3857');
		source1.addFeature(street);
	
		vectorLayer.setZIndex(3);
		map.addLayer(vectorLayer);

	});

	// $.getJSON("http://localhost:8088/roads-seoul.geojson", function(json){
	// });


}

let fn_layer_Pos = function(){
	//포지션레이어
	vectorLayer2 = new ol.layer.Vector({
	  source: source2,
	});

    map.addLayer(vectorLayer2);
}

let fn_layer_Img = function(){
	//이미지레이어
	imageLayer = new ol.layer.Image({		
	  source: source_img,
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


	
	
	/**
	 * 이벤트설정
	 */

	//라인편집모드
	$("#btn1").click(function(){
		//라인벡터 추가
		try{
			map.addLayer(drawVectorLayer);
		}catch(e){}

		//그리기
		draw = new ol.interaction.Draw({
			source: drawVectorSource,
			type: "LineString"
		});	
		//수정편집
		modify = new ol.interaction.Modify({
			source: drawVectorSource,
		});		
		snap = new ol.interaction.Snap({
			source: drawVectorSource,
		});		
		map.addInteraction(modify);
		map.addInteraction(draw);
		map.addInteraction(snap);
	});



	//라인편집해제
	$("#btn3").click(function(){
		map.removeInteraction(draw);
		map.removeInteraction(snap);
		
		try{
			map.removeLayer(drawVectorLayer);
		}catch(e){}
	});


	//라인벡터레이어 저장
	$("#btn2").click(function(){
		debugger;
		let drawVector_Ary = [];	//원본 vector  layer
		let drawVector_Ary2 = [];	//퀀텀 vector  layer

		let features = drawVectorSource.getFeatures();

		$.each(features, function(idx, val){
			// 0.원본json
			drawVector_Ary.push(GeoJSON.writeFeatureObject(features[idx])); //feature -> json변환

			
			// 1.퀀텀
			//라인분해
			let aryCord = [];
			features[idx].getGeometry().forEachSegment(function(start, end){
				aryCord = aryCord.concat(gfn_resolvLine(start.concat(end), res_meter)); //m 단위로 분해 
			});

			let ff = new ol.Feature({
				geometry: new ol.geom.LineString(aryCord),
			});
			
			drawVector_Ary2.push(GeoJSON.writeFeatureObject(ff)); 
		});
		
		
		//0.원본 line string
		let drawVector_string = JSON.stringify(drawVector_Ary);
		localStorage.setItem("drawVector_string", drawVector_string);
		
		//1.실제 분해된 line string
		let drawVector_string2 = JSON.stringify(drawVector_Ary2);
		localStorage.setItem("drawVector_string2", drawVector_string2);



		try{
			map.removeLayer(drawVectorLayer);
		}catch(e){}

		alert("OK");
	});

	//저장된 Path 로딩
	$("#btn4").click(function(){
		let drawVector_string = localStorage.getItem("drawVector_string");
		let drawVector_Ary = JSON.parse(drawVector_string);
		let drawVector_string2 = localStorage.getItem("drawVector_string2");
		let drawVector_Ary2 = JSON.parse(drawVector_string2);
		
		debugger;
		if(drawVector_Ary == null)	return;

		$.each(drawVector_Ary, function(idx, val){
			let ft = GeoJSON.readFeatures(val);
			drawVectorSource.addFeatures(ft);
		});

		try{
			drawVectorLayer.setZIndex(2);
			map.addLayer(drawVectorLayer);
		}catch(e){}


		//resolved 포인트확인
		$.each(drawVector_Ary2, function(idx, val){
			let ft = GeoJSON.readFeatures(val);
			$.each(ft[0].getGeometry().transform('EPSG:3857','EPSG:4326').getCoordinates(), function(idx, val){
				pointMaker(val);
			});

		});

		
	});


});
