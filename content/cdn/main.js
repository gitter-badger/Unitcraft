var ws = null;
var isLocal = location.hostname == "localhost";

$(function () {
    if (!isOkCanvasAndWs()) return;

    var second = Kefir.interval(1000, null);
    var server = initServer();
    var dmnCanvas = initDmnCanvas();
    var keyboard = initKeyboard();
    var click = initClick();
    var status = initStatus(server);
    var memo = initMemo(server);
    var key = initKey(keyboard);
    var keyTest = initKeyTest(keyboard);
    var cmdScale = initCmdScale(keyboard);
    var tileset = Kefir.pool();
    var panelset = Kefir.pool();

    createResize(dmnCanvas);
    createLogin(server);
    createPing(server, keyboard);
    createRefresh();

    var streams = [
        [memo, onMemo],
        [status, onStatus],
        [dmnCanvas, onDmnCanvas],
        [click, onClick],
        [key, onKey],
        [keyTest, onKeyTest],
        [cmdScale, onCmdScale],
        [tileset, onTileset],
        [panelset, onPanelset],
        [second, onSecond]
    ];

    var streamUi = Kefir.merge(R.map(([stream,fn]) => stream.map(R.curry(fn)), streams));
    createUI(tileset, panelset, streamUi);
});

function onDmnCanvas(dmn, ui) {
    var qdmnPanelOld = ui.dmn != null ? ui.qdmnPanel() : null;
    ui.dmn = dmn;
    if (qdmnPanelOld != ui.qdmnPanel()) {
        var panelset = ui.storePanelset(ui.qdmnPanel());
        if (panelset) ui.panelset = panelset;
        ui.fireGrid();
        ui.fireAkter();
    } else {
        ui.fireGrid();
        ui.fireAkter();
    }
    ui.fireOpter();
    ui.fireToolbar();
}

function onCmdScale(cmdScale, ui) {
    var scale = cmdScale != null ? clamp(ui.scale + cmdScale, 0, listQdmnTile.length - 1) : ui.scaleBest();
    updateScale(scale, ui);
}

function onKey(key, ui) {
    if (key === "Enter") {
        endTurn(ui);
    } else if (key === "w") {
        if (ui.game.stage === "turn") ui.fireAkt("w");
    } else if (key === "q") {
        ui.fireCmd("t");
    }
}

var keyTestToCmd = {"x": "r", "c": "c", "v": "d"};

function onKeyTest([key,pst], ui) {
    if (ui.game.opterTest == null) return;
    var pg = ui.pgFromPst(pst);
    if (key === "z") {
        if (ui.opts != null) {
            ui.opts = null;
            ui.fireOpter();
        } else if (pg) {
            ui.openOpter(ui.game.opterTest, num => {
                ui.numOpterTestLast = num;
                return "z" + strPg(pg) + " " + num;
            });
            ui.fireOpter();
        }
    } else if (ui.opts == null) {
        var akt = key === "a" ?
        "z" + strPg(pg) + " " + (ui.numOpterTestLast == null ? 0 : ui.numOpterTestLast) :
        keyTestToCmd[key] + strPg(pg);
        ui.fireAkt(akt);
    }
}

function onClick(click, ui) {
    if (ui.opts != null) {
        onClickOpter(click, ui);
    } else {
        var num = ui.numFromPstOnToolbar(click);
        if (num != null) {
            onClickToolbar(num, ui);
        } else {
            function clickGrid() {
                var pg = ui.pgFromPst(click);
                if (pg != null) {
                    onClickGrid(pg, ui);
                } else {
                    ui.clearFocus();
                    ui.fireAkter();
                }
            }

            if (ui.game.stage === "bonus" || ui.game.stage === "join") {
                var bonus = ui.bonusFromBonusBar(click);
                if (bonus != null) {
                    onClickBonusbar(bonus, ui);
                } else clickGrid();
            } else clickGrid();
        }
    }
}

function onClickBonusbar(bonus, ui) {
    if (bonus >= 1 && bonus <= 10) ui.fireAkt("s" + (bonus - 1));
    if (bonus == 12 || bonus == 13) ui.fireAkt("j" + (bonus - 12));
}

function endTurn(ui) {
    if (ui.game.stage === "turn") ui.fireAkt("e");
}

function onClickToolbar(num, ui) {
    if (num == 0) {
        endTurn(ui);
    } else if (num == 1) {
        if (ui.status == "online") ws.send("p1 1");
        else if (ui.status == "queue" || ui.status == "macth" || ui.status == "invite") ws.send("d");
    } else if (num == 2) {
        // открыть чат
    }else if (num == 9) {
        ws.send("y");
    }
}

function onClickOpter(click, ui) {
    var num = ui.numFromPstOnOpter(click);
    if (num != null) {
        if (num < ui.opts.length && ui.opts[num].isOn) {
            ui.fireAkt(ui.aktSelect(num));
            ui.opts = null;
            ui.fireOpter();
        }
    }else {
        ui.opts = null;
        ui.fireOpter();
    }
}

function onClickGrid(pg, ui) {
    var focus = ui.focus;
    if (focus) {
        var sloy = ui.game.spots[strPg(focus.pg)][focus.idx];
        var akt = findAkt(pg, sloy.akts);
        if (akt != null && sloy.isOn) {
            if (akt.opter) {
                ui.openOpter(akt.opter, num => "b" + strPg(focus.pg) + " " + focus.idx + " " + strPg(pg) + " " + num);
                ui.fireOpter();
            } else {
                ui.fireAkt("a" + strPg(focus.pg) + " " + focus.idx + " " + strPg(pg));
            }
        } else if (R.eqDeep(focus.pg, pg)) {
            ui.incrIdxFocus();
        } else if (ui.game.spots[strPg(pg)]) {
            ui.updateFocus(pg);
        } else {
            ui.clearFocus();
        }
        ui.fireAkter();
    } else {
        if (ui.game.spots[strPg(pg)]) {
            ui.updateFocus(pg);
            ui.fireAkter();
        }
    }
}

function findAkt(pg, akts) {
    for (var akt of akts) {
        if (pg.x == akt.x && pg.y == akt.y) {
            return akt;
        }
    }
    return null;
}

function onMemo(memo, ui) {
    ui.memo = memo;
    var dmnGameOld = ui.game != null ? ui.game.dmn : null;
    ui.game = R.last(memo);
    ui.instant = Date.now();
    if(ui.game.focus!=null && ui.game.spots[strPg(ui.game.focus)]!=null) ui.updateFocus(ui.game.focus); else ui.clearFocus();
    if (dmnGameOld == null || !R.eqDeep(dmnGameOld, ui.game.dmn)) {
        updateScale(ui.scaleBest(), ui);
    }
    ui.fireGrid();
    ui.fireAkter();
    ui.fireToolbar();
}

function onTileset(tileset, ui) {
    if (ui.tileset == null || ui.tile() == tileset.step) {
        ui.tileset = tileset;
        ui.fireGrid();
        ui.fireAkter();
    }
    if (ui.tilesetOpter == null || (ui.opts != null && ui.qdmnTileOpter() == tileset.step)) {
        ui.tilesetOpter = tileset;
        ui.fireOpter();
    }
}

function onPanelset(panelset, ui) {
    if (ui.qdmnPanel == panelset.step || ui.panelset == null) {
        ui.panelset = panelset;
        ui.fireToolbar();
    }
}

function onSecond(_, ui) {
    if (ui.game == null) return;
    ui.fireClock();
    if (ui.game.stage != "win" && ui.game.stage != "winEnemy" && ui.game.clockIsOn[1] && ui.intervalElapsed() >= ui.game.clock[1]) {
        ui.fireCmd("r");
    }
}

function initKeyTest(keyboard) {
    var mouse = initMouse();
    return Kefir.combine([keyboard.key("z", "x", "c", "v", "a")], [mouse]);
}

function initKey(keyboard) {
    return Kefir.merge([keyboard.key("Enter", "q", "w")]);
}

function initCmdScale(keyboard) {
    return Kefir.merge([keyboard.key("-").map(() => -1), keyboard.key("=").map(() => 1), keyboard.key("0").map(() => null)]);
}

function updateScale(scale, ui) {
    if (ui.scale != scale) {
        ui.scale = scale;
        var tileset = ui.storeTileset(ui.tile());
        if (tileset) ui.tileset = tileset;
        ui.fireGrid();
        ui.fireAkter();
        ui.fireToolbar();
    }
}

function onStatus(status, ui) {
    ui.status = status;
    ui.fireToolbar();
}

function createUI(tileset, panelset, streamUi) {
    var emitter = createEmitter();
    var grid = redrawGrid();
    var akter = redrawAkter();
    var opter = redrawOpter();
    var toolbar = redrawToolbar();
    emitter.stream.onValue(([redraw,ui]) => redraw(ui));
    var ui = {
        status: "online",
        pgFromPst({x,y}) {
            var xx = x - ui.pstGrid().x;
            var yy = y - ui.pstGrid().y;
            return (xx >= 0 && xx < this.tile() * this.game.dmn.xr && yy >= 0 && yy < this.tile() * this.game.dmn.yr) ? {
                x: div(xx, this.tile()),
                y: div(yy, this.tile())
            } : null;
        },
        pstGrid() {
            var xr = this.dmn.xr;
            var yr = this.dmn.yr;
            var xrTb = ui.qdmnPanel();
            var xrGrid = this.tile() * ui.game.dmn.xr;
            var yrGrid = this.tile() * ui.game.dmn.yr;
            var xSm = 0;
            if (xrTb * 2 + xrGrid <= xr) {
                xSm = 0;
            } else if (xrTb + xrGrid <= xr) {
                xSm = 2 * xrTb - xr + xrGrid;
            } else {
                xSm = xrTb;
            }
            return {x: div(xr - xSm - xrGrid, 2), y: div(yr - yrGrid, 2)};
        },
        pstToolbar() {
            var pstGrid = ui.pstGrid();
            return {x: Math.max(0, pstGrid.x - ui.qdmnPanel()), y: Math.max(0, pstGrid.y)};
        },
        pstBonusbar() {
            var pstGrid = ui.pstGrid();
            var xrGrid = this.tile() * ui.game.dmn.xr;
            return {x: pstGrid.x + xrGrid, y: pstGrid.y};
        },
        updateFocus(pg) {
            this.focus = {pg, idx: 0};
            this.akts = ui.game.spots[strPg(this.focus.pg)][this.focus.idx].akts;
        },
        incrIdxFocus() {
            this.focus.idx = (this.focus.idx + 1) % ui.game.spots[strPg(this.focus.pg)].length;
            this.akts = ui.game.spots[strPg(this.focus.pg)][this.focus.idx].akts;
        },
        clearFocus() {
            this.focus = null;
            this.akts = null;
        },
        openOpter(opts, aktSelect) {
            this.opts = opts;
            this.aktSelect = aktSelect;
            var tilesetOpter = ui.storeTileset(this.qdmnTileOpter());
            if (tilesetOpter) this.tilesetOpter = tilesetOpter;
        },
        qdmnTileOpter(){
            var dmn = this.dmnOpter();
            var qdmnXExact = (ui.dmn.xr - ui.qdmnPanel()) / dmn.xr;
            var qdmnX = R.minBy(qdmn => qdmnXExact - qdmn, R.filter(qdmn => qdmn <= qdmnXExact, listQdmnTile)) || listQdmnTile[0];
            var qdmnYExact = ui.dmn.yr / dmn.yr;
            var qdmnY = R.minBy(qdmn => qdmnYExact - qdmn, R.filter(qdmn => qdmn <= qdmnYExact, listQdmnTile)) || listQdmnTile[0];
            return Math.min(qdmnX, qdmnY);
        },
        dmnOpter() {
            var xr = 5;
            return {xr, yr: div(ui.opts.length, xr) + sign(ui.opts.length % xr)};
        },
        numFromPstOnOpter(pst) {
            return isPstInRect(pst, this.pstOpter(), scaleDmn(this.dmnOpter(), this.qdmnTileOpter())) ?
            div(pst.x - this.pstOpter().x, this.qdmnTileOpter()) + div(pst.y - this.pstOpter().y, this.qdmnTileOpter()) * this.dmnOpter().xr : null;
        },
        numFromPstOnToolbar(pst){
            var pstT = ui.pstToolbar();
            var qp = ui.qdmnPanel();
            if (isPstInRect(pst, pstT, scaleDmn({xr: 1, yr: 4}, qp)))
                return div(pst.y - this.pstToolbar().y, qp);
            else if (ui.status == "match" && isPstInRect(pst, {x: pstT.x + qp, y: pstT.y + qp}, {xr: qp,yr: qp}))
                return 9
        },
        bonusFromBonusBar(pst){
            if (ui.game.stage === "bonus") return isPstInRect(pst, this.pstBonusbar(), scaleDmn({xr: 1, yr: 12},
                this.qdmnPanel() / 2)) ? div(pst.y - this.pstBonusbar().y, this.qdmnPanel() / 2) : null;
            else return isPstInRect(pst, this.pstBonusbar(), scaleDmn({xr: 1, yr: 2},
                this.qdmnPanel())) ? div(pst.y - this.pstBonusbar().y, this.qdmnPanel()) + 12 : null;
        },
        isAcceptFromPstOnToolbar(pst){
            var qdmn = this.qdmnPanel();
            var pstTb = this.pstToolbar();
            return isPstInRect(pst, {xr: pstTb.x + qdmn, yr: pstTb.y + qdmn}, {xr: qdmn, yr: qdmn});
        },
        pstOpter() {
            var dmn = this.dmnOpter();
            var xr = dmn.xr * this.qdmnTileOpter();
            var yr = dmn.yr * this.qdmnTileOpter();
            return {x: div(this.dmn.xr - xr, 2), y: div(this.dmn.yr - yr, 2)}
        },
        qdmnPanel() {
            return R.minBy(qdmn => Math.abs(ui.dmn.yr / qdmn - 6.5), listQdmnPanel);
        },
        tile(){
            return listQdmnTile[ui.scale];
        },
        scaleBest() {
            var qdmnXExact = (ui.dmn.xr - ui.qdmnPanel()) / ui.game.dmn.xr;
            var qdmnX = R.minBy(qdmn => qdmnXExact - qdmn, R.filter(qdmn => qdmn <= qdmnXExact, listQdmnTile)) || listQdmnTile[0];
            var qdmnYExact = ui.dmn.yr / ui.game.dmn.yr;
            var qdmnY = R.minBy(qdmn => qdmnYExact - qdmn, R.filter(qdmn => qdmn <= qdmnYExact, listQdmnTile)) || listQdmnTile[0];
            return R.indexOf(Math.min(qdmnX, qdmnY), listQdmnTile);
        },
        intervalElapsed(){
            return Date.now() - this.instant;
        },
        storeTileset: storeImages(tileset, "tile"),
        storePanelset: storeImages(panelset, "panel"),
        lock() {
            console.log("lock");
        },
        fireGrid() {
            emitter.emit([grid, this]);
        },
        fireAkter() {
            emitter.emit([akter, this]);
        },
        fireOpter() {
            emitter.emit([opter, this]);
        },
        fireToolbar() {
            emitter.emit([toolbar.redraw, this]);
        },
        fireClock() {
            emitter.emit([toolbar.redrawClock, this]);
        },
        fireAkt(akt) {
            this.lock();
            ws.send("a" + ui.game.version + "#" + akt);
            ui.clearFocus();
        },
        fireCmd(cmd) {
            this.lock();
            ws.send(cmd);
        }
    };
    streamUi.scan((ui, fn) => {
        fn(ui);
        return ui;
    }, ui).onValue(()=> {
    });
}

function createEmitter() {
    var em;
    return {
        stream: Kefir.stream(e => em = e),
        emit(value) {
            em.emit(value);
        }
    }
}

function initDmnCanvas() {
    var w = $(window);

    function makeBounds() {
        return {
            xr: w.width(),
            yr: w.height()
        }
    }

    return Kefir.fromEvents(w, "resize", makeBounds).skipDuplicates(R.eqDeep).toProperty(makeBounds);
}

function initKeyboard() {
    var keys = Kefir.merge([Kefir.fromEvents(document, 'keydown', R.prop("key")), Kefir.fromEvents(document, 'keyup', R.always(null))])
        .skipDuplicates().filter(R.not(R.eq(null)));
    return {
        key(...ks) {
            return keys.filter(key => R.contains(key, ks));
        }
    }
}

function initServer() {
    var messages = Kefir.stream(em => {
        ws = new WebSocket(urlWs);
        ws.onmessage = e => em.emit(e.data);
        ws.onerror = em.error;
        ws.onclose = () => showFatal("Cant connect to server");
    });
    if (isLocal) messages.onValue(msg => console.log(msg.length <= 50 ? msg : msg.substring(0, 50) + "..."));
    return {
        msg(tp) {
            return messages.filter(R.compose(R.eq(tp), R.nthChar(0))).map(R.substringFrom(1));
        }
    }
}

function createResize(dmn) {
    var canvases = R.map(id => $(id)[0], ["#canvasGrid", "#canvasAkts", "#canvasToolbar", "#canvasOpter"]);
    dmn.onValue(b =>
        R.forEach(c => {
            c.width = b.xr;
            c.height = b.yr;
        }, canvases)
    );
}

function createPing(server, keyboard) {
    Kefir.combine([server.msg("q").map(Date.now)], [keyboard.key("p").onValue(() => ws.send("q")).map(Date.now)], R.subtract).onValue(v =>
        console.log(v + "ms")
    );
}

function initMemo(server) {
    var sizeMemo = 10;
    return server.msg("g").map(JSON.parse).onValue(game => {
        if (game.err) showErr()
    }).slidingWindow(sizeMemo);
}

function initStatus(server) {
    return server.msg("s");
}

function createLogin(server) {
    var modalLogin = $.UIkit.modal("#modalLogin", {bgclose: false});
    modalLogin.show();
    server.msg("u").take(1).onValue(user => modalLogin.hide());

    var spanKey = $("#spanKey");
    var btnReg = $("#btnReg");
    btnReg.click(() => ws.send("n"));
    server.msg("n").onValue(msg => {
        spanKey.html(msg);
        btnReg.hide();
    });

    $("#btnLogin").click(() => ws.send("l" + $("#txtKey").val().trim() + " " + getNumMission()));
}

function initClick() {
    return Kefir.fromEvents($("#canvasOpter"), "mousedown", pstEvent);
}

function initMouse() {
    return Kefir.fromEvents($("#canvasOpter"), "mousemove", pstEvent);
}

function createRefresh() {
    $("#btnRefresh").click(() => location.reload());
}