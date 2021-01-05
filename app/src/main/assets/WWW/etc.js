/// Constant
const res_meter = 2; //path 격자단위 m






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
 * 격자상 위치결정
 * @param {xy [x,y]} 비콘위경도
 * @param {_xy [_x,_y]} 이전 격자상 위치
 */
let gfn_nextPos = function(xy, _xy){

	//이전위치없으면 격자상에 그냥 결정
	if(_xy == null){

	}

	// 레이어의 모든 좌표들
	let layerCoords = []; //[[1,2],[1,3],[1,4]...]
	

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

