function isOkCanvasAndWs() {
    var elem = document.createElement('canvas');
    var ok = !!(elem.getContext && elem.getContext('2d')) && ("WebSocket" in window);
    if (!ok) {
        $("body").html("<p>Your browser not support Canvas or Websockets. Try Firefox or Chrome.<p>");
    }
    return ok;
}

function getNumMission(n){
    if(arguments.length==1) localStorage["mission"] = n;
    var num = parseInt(localStorage["mission"]);
    if(isNaN(num)) num = 0;
    return num;
}

function clamp(val,min,max){
    if(val<min) return min;
    if(val>max) return max;
    return val;
}

function sign(x) { return x ? x < 0 ? -1 : 1 : 0; }

function div(a,b){ return ~~(a/b) }

function showFatal(msg){
    $("#spanFatal").html(msg);
    $.UIkit.modal("#modalFatal", {bgclose: false}).show();
}

function showErr(){
    $.UIkit.modal("#modalErr", {bgclose: true}).show();
}

function pstEvent(e){
    return {
        x: e.pageX,
        y: e.pageY
    };
    //return {
    //    x: e.offsetX!=undefined?e.offsetX:e.layerX,
    //    y: e.offsetY!=undefined?e.offsetY:e.layerY
    //}
}

function isPstInRect({x:xx,y:yy},{x,y},{xr,yr}){
    var xIn = xx - x;
    var yIn = yy - y;
    return xIn>=0 && xIn<xr && yIn>=0 && yIn<yr;
}

function scaleDmn({xr,yr},scale){
    return {xr:xr*scale,yr:yr*scale};
}

function drawDab(ctx,dab,tileset,qdmnTile){
    if(dab.tile!=null){
        if(dab.hint!=null) hintTile[dab.hint](ctx,qdmnTile);
        ctx.drawImage(
            tileset,dab.tile%50*tileset.step*2,div(dab.tile,50)*tileset.step*2,tileset.step*2,tileset.step*2,
            -qdmnTile/2,-qdmnTile/2,qdmnTile*2,qdmnTile*2
        );
    }else if(dab.text!=null){
        if(dab.hint!=null) hintText[dab.hint](ctx,qdmnTile);
        drawText(ctx,dab.text,0,0,qdmnTile*0.4);
    }
}

function drawText(ctx,txt,x,y,size){
    ctx.textBaseline = "top";
    ctx.font = size+"px Arial";
    ctx.save();
    ctx.fillStyle = "black";
    ctx.fillText(txt,x-1,y);
    ctx.fillText(txt,x+1,y);
    ctx.fillText(txt,x,y-1);
    ctx.fillText(txt,x,y+1);
    ctx.restore();
    ctx.fillText(txt,x,y);
}

function strPg({x,y}){
    return x+" "+y;
}

function loadImage(prefix,qdmn){
    return Kefir.stream(em =>{
        var key = prefix+qdmn;
        var img = new Image();
        img.step = qdmn;
        img.onload = () => {
            em.emit(img);
            em.end();
        };
        img.src = "png/"+key+".png"+(isLocal?"?t="+Date.now():"");
    });
}

function storeImages(pool,prefix) {
    var qdmnLast = [];
    var imgs = new Map();
    var plugLoadImg = qdmn => pool.plug(loadImage(prefix, qdmn).onValue(img => imgs.set(img.step, img)));
    if(isLocal) $(window).focus(()=>{
        imgs.clear();
        for(var q of qdmnLast) plugLoadImg(q);
    });
    return qdmn => {
        if(qdmnLast[qdmnLast.length-1]!==qdmn) qdmnLast.push(qdmn);
        if(qdmnLast.length>2) qdmnLast.shift();
        var img = imgs.get(qdmn);
        if (img) {
            return img;
        } else {
            plugLoadImg(qdmn);
            return null;
        }
    };
}