/**
 * 전역변수
 */

/// Constant
const res_meter = 2; //path 격자단위 m
const mod_foctor = 1.3; //비콘거리 보정 factor
const maxRange = 30.0; //약한신호 무시 factor - 기준비콘들간의 사정거리 스케일

//비콘디바이스정보
//const refBconAry = [
//	{minor:10, xy: 	[14157026.19, 4498245.31]	},//양전무
//	{minor:7, xy: 	[14157047.34, 4498248.11]	},//김영선
//	{minor:9, xy: 	[14157047.34, 4498238.30]	},//회의실
//	{minor:1604, xy:[14157027.30, 4498236.90]	},//김연희
//];
//const refBconAry = [
//	{minor:10, xy: [127.17473, 37.42309]},//양전무
//	{minor:7, xy: [127.17492, 37.42311]},//김영선
//	{minor:9, xy: [127.17492, 37.42304]},//회의실
//	{minor:1604, xy: [127.17474, 37.42303]},//김연희
//];
const refBconAry = [
	{minor:10, xy: 	[14157028.415604059, 4498241.388697902] },//양전무
	{minor:7, xy: 	[14157041.702509366, 4498242.769640307]	},//김영선
	{minor:9, xy: 	[14157042.038414275, 4498237.245870685]	},//회의실
	{minor:1604, xy:[14157028.938122805, 4498235.976896583]	},//김연희
];
const refBconAry_4326 = [
	{minor:10, xy: 	[127.17475003451499, 37.42306205490493] },//좌상
	{minor:7, xy: 	[127.17486939281615, 37.423071906756235]},//우상
	{minor:9, xy: 	[127.17487241030128, 37.423032499343236]},//우하
	{minor:1604, xy:[127.17475472838073, 37.42302344628594]	},//좌하
];
// const refBconAry_4326 = refBconAry;
// $.each(refBconAry_4326, function(idx, val){
// 	val.xy =  ol.proj.toLonLat(val.xy);
// });



//이미지레이어범위
//let projExtent = [14157005.425914, 4498212.914733, 14157071.006190, 4498255.665878]; //3857
//let projExtent = [127.175111, 37.423295, 127.175741, 37.423578]; //4326
// let projExtent = [127.17467, 37.423025, 127.17493, 37.423105]; //4326 - 3층

let projExtent = ol.proj.toLonLat([14157019.701777756, 4498235.228361304]); //4326 - 4층
projExtent = projExtent.concat(ol.proj.toLonLat([14157041.572919639, 4498247.582197418]));




//레이어의 모든 좌표들
let latisAry = []; //[[1,2],[1,3],[1,4]...]


let cur_xy;//현재좌표








/// 좌표계회전 함수
function rotateProjection(projection, angle, extent) {
	function rotateCoordinate(coordinate, angle, anchor) {
	  let coord = ol.coordinate.rotate(
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

	let normalProjection = ol.proj.get(projection);

	let rotatedProjection = new ol.proj.Projection({
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
	  let projCodes = Object.keys(proj4.defs);
	  projCodes.forEach(function(code) {
		let proj4Projection = ol.proj.get(code);
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



/// 맵 회전
let rot_L = function(){
    let view = map.getView();
    view.animate({
       rotation: view.getRotation() + Math.PI / 2,
     });
}
let rot_R = function(){
    let view = map.getView();
    view.animate({
       rotation: view.getRotation() - Math.PI / 2,
     });
}
let rot_Ang = function(ang){
   let angle = 0.0;
   try{
       angle = parseFloat(ang);
   }catch(e){}
    let view = map.getView();
    view.animate({
       rotation: view.getRotation() + Math.PI / 180 * ang,
     });
}





/// LineString 지정된 거리스케일로 분해
let gfn_resolvLine = function(seg, m){
	//segment = [x1,y1, x2,y2];
	let AB = Math.sqrt((seg[2]-seg[0])^2 + (seg[3]-seg[1])^2); //선분길이
	//let t = Math.atan((seg[3]-seg[1])/(seg[2]-seg[0])); //두점의 기울기각도
	let t = gfn_atan((seg[2]-seg[0]), (seg[3]-seg[1]));

	let dx = m * Math.cos(t); //x증가분(부호포함)
	let dy = m * Math.sin(t); //y증가분(부호포함)

	let x = seg[0];
	let y = seg[1];
	let extSeg = [[x, y]]; // resoved 좌표들 (m로 분해된)


	//기울기가 큰 축으로 분해한다...
	if(Math.abs(dx) > Math.abs(dy)){
		//x축으로 분해
		while(true){
			x += dx;
			y += dy;

			if(dx > 0){
				if(x > seg[2])	break; //증가방향
			}
			else{
				if(x < seg[2])	break; //감소방향
			}

			// extSeg = extSeg.concat([x,y]);
			extSeg.push([x,y]);
		}
	}
	else{
		//y축으로 분해
		while(true){
			x += dx;
			y += dy;

			if(dy > 0){
				if(y > seg[3])	break; //증가방향
			}
			else{
				if(y < seg[3])	break; //감소방향
			}

			// extSeg = extSeg.concat([x,y]);
			extSeg.push([x,y]);
		}
	}
	return extSeg;
}




/// 영역 좌표계로 변환 - feature  입/출력 파라미터
let gfn_transProj = function(_f){
	let f = new ol.Feature();
	f = _f;
	return f.getGeometry().transform('EPSG:3857','EPSG:4326');
}

/// Arc Tangent 함수
let gfn_atan = function(dx, dy){
	let v = Math.abs(Math.atan(dy/dx));

	//1사분면
	if(dx>0 && dy>0){
		return Math.atan(dy/dx);
	}
	//2사분면
	else if(dx<0 && dy>0){
		return Math.atan(dy/dx) + Math.PI;
	}
	//3사분면
	else if(dx<0 && dy<0){
		return Math.atan(dy/dx) - Math.PI;
	}
	//4사분면
	else if(dx>0 && dy<0){
		return Math.atan(dy/dx);
	}
}












/**
 * 격자상 위치결정 - Distance
 * @param {bcAry [{minor:1, dist: 4},{minor:2, dist: 33},{minor:3, dist: 44},{minor:4, dist: 11}]} 비콘거리set
 */
let gfn_calLatis = function(bcAry){

debugger;




	//비콘영역 스케일로 필터링하고, xy 좌표추가
	try {
		bcAry = gfn_realAry(bcAry, maxRange);
		bcAry.sort(function(a,b){
			return a.dist - b.dist; //가까운순서로 3개 선택
		});
	} catch (error) {}

	if(bcAry.length < 2)	return null; //2개 이하면 계산못함.. 원칙은 3개가 필요하지만 데이터없는경우 2개로도 계산해야할듯..

    //격자없으면 exit
    if(!latisAry)    return null;
	let _latisAry = latisAry;


	//기준비콘 동심원범위 (참고용)
	source3.clear();
	for(let i=0; i<3; i++){
	    //동심원
		let range = new ol.Feature({
			geometry: new ol.geom.Circle([bcAry[i].xy[0],bcAry[i].xy[1]], bcAry[i].dist * mod_foctor),
		});
		source3.addFeature(range);
		//비콘마커
		let marker = new ol.Feature({
			geometry: new ol.geom.Point(bcAry[i].xy),
		});
		marker.setProperties({"radius": bcAry[i].dist});
		source3.addFeature(marker);
	}
	try{
        vectorLayer3.setZIndex(5);
		map.addLayer(vectorLayer3);
	}catch(e){}



	//각 비콘기준점에서 격자 필터링제외
	//1번째
	_latisAry = latisAry;
	let latisAry0 = _latisAry.filter( p => gfn_dist(bcAry[0].xy, p) < bcAry[0].dist * mod_foctor );
	//2번째
	_latisAry = latisAry;
	let latisAry1 = _latisAry.filter( p => gfn_dist(bcAry[1].xy, p) < bcAry[1].dist * mod_foctor );
	//3번째
	_latisAry = latisAry;
	let latisAry2 = _latisAry.filter( p => gfn_dist(bcAry[2].xy, p) < bcAry[2].dist * mod_foctor );


	//각영역 교집합
	let ary012 = latisAry0.filter( p =>  gfn_hasAry(latisAry1, p)).filter( p =>  gfn_hasAry(latisAry2, p));
	let ary01;
	let ary12;
	let ary20;
	//3영역 교집합이 있으면 현재위치와 근접한 놈으로 결정
	if(ary012 != null && ary012.length > 0 ){
		cur_xy = gfn_getNear(ary012, cur_xy);
	}
	// 2영역 교집합이 없으면 차선을 결정
	else if((ary01 = latisAry0.filter( x =>  gfn_hasAry(latisAry1, x))).length > 0){
		cur_xy = gfn_getNear(ary01, cur_xy);
	}
	else if((ary12 = latisAry1.filter( x =>  gfn_hasAry(latisAry2, x))).length > 0){
		cur_xy = gfn_getNear(ary12, cur_xy);
	}
	else if((ary20 = latisAry2.filter( x =>  gfn_hasAry(latisAry0, x))).length > 0){
		cur_xy = gfn_getNear(ary20, cur_xy);
	}
	//교집합이 없으면 null
	else{
		return null;
	}

	return cur_xy;


	//연속적으로 비콘에서 격자 필터링
	// $.each(bcAry, function(idx, bc){
	// 	_latisAry.filter( latis => gfn_dist(bc.xy, latis) > bc.dist );

	// 	if(!_latisAry){
	// 		//격자없음..
	// 	}
	// 	else if(_latisAry.length < 2){
	// 		return false;//한개남으면 그냥 결정..
	// 	}
	// });





}


/**
 * 해당영역의 측정거리만 유효한 거리로 간주
 * @param {*} _bcAry 기준비콘
 * @param {*} _maxRange 비콘영역스케일 m
 */
let gfn_realAry = function(_bcAry, _maxRange){
	let __bcAry = [];
	$.each(_bcAry, function(idx, bc){
		if(bc.dist < _maxRange){
			//ref비콘 위치넣어주기
			$.each(refBconAry, function(idx, val){
				if(val.minor == bc.minor) {
					bc.xy = val.xy;
					return false;
				}
			});
			__bcAry.push(bc);
		}
	});
	return __bcAry;
}



/**
 * 두점사이의 거리 m
 * @param {*} xy - [x,y]
 * @param {*} xy2 - [x2,y2]
 */
let gfn_dist = function(xy, xy2){
	return Math.sqrt(Math.pow(xy[0]-xy2[0],2) + Math.pow(xy[1]-xy2[1],2));
}

/**
 * 배열에서 xy 좌표가 있는지 체크 .. 3857좌표에서 반올림
 * @param {} ary
 * @param {*} xy
 */
let gfn_hasAry = function(ary, xy){
	let ret = false;
	$.each(ary, function(idx, val){
		if(Math.round(val[0],2) == Math.round(xy[0],2) && Math.round(val[1],2) == Math.round(xy[1],2)){
			ret = true;
			return false;
		}
	});
	return ret;
}


/**
 * xy와 가장가까운 좌표선별
 * @param {*} ary
 * @param {*} xy
 */
let gfn_getNear = function(ary, xy){
    if(!xy) return ary[0]; //현재위치없으면 첫번째놈으로..


	$.each(ary, function(idx, p){
		p.dist = gfn_dist(p, xy);
	});

	ary.sort(function(a,b){
		return a.dist - b.dist;//오름차순
	});

	return ary[0];
}



























function addRandomFeature() {
   let y = 37.52654;
   let x = 126.980366;

   let geom = new ol.geom.Point(ol.proj.fromLonLat([x, y]));
   let feature = new ol.Feature(geom);
   source3.addFeature(feature);
}

let duration = 3000;
function flash(feature) {
 let start = new Date().getTime();
 let listenerKey = rasterLayer.on('postrender', animate);

 function animate(event) {
   let vectorContext = ol.render.getVectorContext(event);
   let frameState = event.frameState;
   let flashGeom = feature.getGeometry().clone();
   let elapsed = frameState.time - start;
   let elapsedRatio = elapsed / duration;
   // radius will be 5 at start and 30 at end.
   let radius = ol.easing.easeOut(elapsedRatio) * 25 + 5;
   let opacity = ol.easing.easeOut(1 - elapsedRatio);

   let style = new ol.style.Style({
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









//포인트 순차적으로 표시 - 이동처럼 보이게
let move = function(i){
    try{
	let format = new ol.format.GeoJSON();
	let features = format.readFeatures(_json);
	let street = features[0];

	// convert to a turf.js feature
	let turfLine = format.writeFeatureObject(street);

	let distance = 0.01;
	// get the line length in kilometers
	let length = turf.lineDistance(turfLine, 'kilometers');

    let p = i % (length / distance); // 범위넘어서면 처음부터

	let turfPoint = turf.along(turfLine, p * distance, 'kilometers');

	// convert the generated point to a OpenLayers feature
	let marker = format.readFeature(turfPoint);
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


let _sleep = function(t){
	let cnt = 0;
	while(true){
		//console.log("_sleep - " + cnt);
		if(cnt++ > t*1000)	break;
	}
}

