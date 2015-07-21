function redrawGrid(){
    var ctx = $("#canvasGrid")[0].getContext("2d");

    function calcEdge(pst,ui){
        var xr = ui.tile()*ui.game.dmn.xr;
        var yr = ui.tile()*ui.game.dmn.yr;
        var xgLeft = Math.max(0, div(pst.x, ui.tile()) + 1);
        var xgRight = Math.max(0, div(ui.dmn.xr - pst.x - xr, ui.tile()) + 1);
        var ygUp = Math.max(0, div(pst.y, ui.tile()) + 1);
        var ygDown = Math.max(0, div(ui.dmn.yr - pst.y - yr, ui.tile()) + 1);
        return {
            drawTopLeft(){
                for(var x=-xgLeft;x<ui.game.dmn.xr+xgRight;x++){
                    for(var y=-ygUp;y<ui.game.dmn.yr+ygDown;y++){
                        if(x<0 || y<0)
                            drawDabOnGrid(ctx,ui.game.edge,{x,y},ui.tileset,ui.tile());
                    }
                }
            },
            drawBotRight(){
                for (var x = -xgLeft; x < ui.game.dmn.xr + xgRight; x++) {
                    for (var y = -ygUp; y < ui.game.dmn.yr + ygDown; y++) {
                        if (y >= 0 && x >= ui.game.dmn.xr || x >= 0 && y >= ui.game.dmn.yr)
                            drawDabOnGrid(ctx, ui.game.edge,{x, y},ui.tileset,ui.tile());
                    }
                }
            }
        }
    }

    return function(ui){
        if(ui.game==null || ui.tileset==null) return;
        ctx.save();
        var pst = ui.pstGrid();
        ctx.translate(pst.x, pst.y);

        var edge = calcEdge(pst,ui);
        edge.drawTopLeft();
        drawDrawing(ctx,ui.game.grid,ui.tileset,ui.tile());
        drawDrawing(ctx,ui.game.traces,ui.tileset,ui.tile());
        edge.drawBotRight();

        ctx.restore();
    };
}

function redrawAkter(){
    var ctx = $("#canvasAkts")[0].getContext("2d");

    return function(ui){
        ctx.clearRect(0, 0, ui.dmn.xr, ui.dmn.yr);
        if (ui.focus==null || ui.tileset==null) return;
        ctx.save();
        var pst = ui.pstGrid();
        ctx.translate(pst.x, pst.y);
        drawDrawing(ctx,ui.akts,ui.tileset,ui.tile());
        drawDabOnGrid(ctx,ui.game.focus,ui.focus.pg,ui.tileset,ui.tile());
        ctx.restore();
    }
}

function redrawOpter(){
    var ctx = $("#canvasOpter")[0].getContext("2d");
    var texture = "rgb(0,90,60)";
    var img = new Image();
    img.onload = () => texture = ctx.createPattern(img, "repeat");
    img.src = "texture/opter.png";
    return function(ui){
        ctx.clearRect(0, 0, ui.dmn.xr, ui.dmn.yr);
        if(ui.opts==null || ui.tilesetOpter==null) return;
        ctx.save();
        var dmnOpter = ui.dmnOpter();
        var tile = ui.qdmnTileOpter();
        var xr = dmnOpter.xr*tile;
        var yr = dmnOpter.yr*tile;
        var pst = ui.pstOpter();
        ctx.translate(pst.x, pst.y);
        ctx.fillStyle = texture;
        ctx.fillRect(0, 0, xr, yr);
        ctx.font = tile*0.3 + "px Arial";
        for (var i = 0; i < ui.opts.length; i++) {
            ctx.save();
            var dabs = ui.opts[i];
            var xTile = i % dmnOpter.xr * tile;
            var yTile = div(i, dmnOpter.xr) * tile;
            ctx.translate(xTile, yTile);
            for (var dab of dabs) {
                drawDab(ctx, dab, ui.tilesetOpter, tile);
            }
            ctx.restore();
        }
        ctx.restore();
    }
}

function drawDrawing(ctx,drawing,tileset,tile) {
    for (var drawOnGrid of drawing) {
        drawDabOnGrid(ctx,drawOnGrid.dab, drawOnGrid, tileset,tile);
    }
}

function drawDabOnGrid(ctx,dab,pg,tileset,tile){
    ctx.save();
    ctx.translate(pg.x*tile,pg.y*tile);
    drawDab(ctx,dab,tileset,tile);
    ctx.restore();
}

function redrawToolbar(){
    var ctx = $("#canvasToolbar")[0].getContext("2d");

    function drawPanel(panel,ui){
        ctx.drawImage(
            ui.panelset,
            imgPanels[panel]*ui.panelset.step,0,ui.panelset.step, ui.panelset.step,
            0,0,ui.panelset.step,ui.panelset.step
        );
    }

    function drawStage(ui){
        ctx.save();
        drawPanel(ui.game.stage,ui);

        var p = prmDrawStage(ui);
        drawClock(ui,p);
        ctx.fillStyle = "lightgreen";
        drawText(ctx,vpoint(ui,0),p.qdmn-xrText(vpoint(ui,0))-p.pd,p.pd,p.sizeFont);
        ctx.fillStyle = "pink";
        drawText(ctx,vpoint(ui,1),p.pd+xrText("00")-xrText(vpoint(ui,1)),p.yrRow+p.pd,p.sizeFont);
        ctx.fillStyle = "white";
        drawText(ctx,ui.game.bet,p.qdmn-xrText(ui.game.bet)-p.pd,p.yrRow*3+p.pd,p.sizeFont);
        ctx.restore();
    }

    function drawClock(ui,p){
        var xrClear = p.qdmn*0.7;
        var yrRowImg = ui.panelset.step*(p.yrRow/p.qdmn);
        var xrClearImg = ui.panelset.step*(xrClear/p.qdmn);
        ctx.drawImage(
            ui.panelset,
            imgPanels[ui.game.stage]*ui.panelset.step,0,xrClearImg, yrRowImg,
            0,0,xrClear,p.yrRow
        );
        ctx.drawImage(
            ui.panelset,
            imgPanels[ui.game.stage]*ui.panelset.step+ui.panelset.step-xrClearImg,yrRowImg,xrClearImg, yrRowImg,
            p.qdmn-xrClear,p.yrRow,xrClear,p.yrRow
        );
        ctx.fillStyle = "lightgreen";
        drawText(ctx,clock(ui,0),p.pd,p.pd,p.sizeFont);
        ctx.fillStyle = "pink";
        drawText(ctx,clock(ui,1),p.qdmn-xrText(clock(ui,1))-p.pd,p.yrRow+p.pd,p.sizeFont);
    }

    function prmDrawStage(ui){
        var qdmn = ui.qdmnPanel();
        return {
            qdmn,
            pd : qdmn/30,
            sizeFont : qdmn/5,
            yrRow : qdmn/4
        }
    }

    function clock(ui,num){
        var ms = Math.max(ui.game.clock[num]-ui.intervalElapsed(),0);
        var min = div(ms,60000);
        var sec = div((ms-min*60000),1000);
        return padClock(min)+":"+padClock(sec);
    }

    function padClock(v){
        return ((""+v).length==1?"0":"")+v;
    }

    function vpoint(ui,num){
         return ui.game.vpoint[num];
    }

    function xrText(txt) {
        return ctx.measureText(""+txt).width;
    }

    function drawStatus(ui){
        ctx.save();
        ctx.translate(0, ui.qdmnPanel());
        drawPanel(ui.status,ui);
        ctx.restore();
    }

    return {
        redraw(ui){
            ctx.clearRect(0, 0, ui.dmn.xr, ui.dmn.yr);
            if (ui.game==null || ui.panelset==null) return;
            ctx.save();
            var pst = ui.pstToolbar();
            ctx.translate(pst.x, pst.y);
            drawStage(ui);
            drawStatus(ui);
            ctx.restore();
        },
        redrawClock(ui){
            if (ui.game==null || ui.panelset==null) return;
            ctx.save();
            var pst = ui.pstToolbar();
            ctx.translate(pst.x, pst.y);
            drawClock(ui,prmDrawStage(ui));
            ctx.restore();
        }
    };
}