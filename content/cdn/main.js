var ws = null;
var isLocal = location.hostname == "localhost";

$(function () {
    if (!isOkCanvasAndWs()) return;

    var surr = initSurr();
    var second = Kefir.interval(1000, null);
    var server = initServer();
    var dmnCanvas = initDmnCanvas();
    var keyboard = initKeyboard();
    var click = initClick();
    var status = initStatus(server);
    var memo = initMemo(server);
    var key = initKey(keyboard);
    var keyTest = initKeyTest(keyboard);
    var tileset = Kefir.pool();
    var panelset = Kefir.pool();
    var stat = initStat(server);

    createResize(dmnCanvas);
    createLogin(server);
    createPing(server, keyboard);
    createReconnect();

    var streams = [
        [memo, onMemo],
        [status, onStatus],
        [dmnCanvas, onDmnCanvas],
        [click, R.curry(onClick)(surr.modal)],
        [key, onKey],
        [keyTest, onKeyTest],
        [tileset, onTileset],
        [panelset, onPanelset],
        [second, onSecond],
        [surr.stream, surr.onSurr],
        [stat.stream, stat.onEvent]
    ];

    var streamUi = Kefir.merge(R.map(([stream,fn]) => stream.map(R.curry(fn)), streams));
    createUI(tileset, panelset, streamUi);
});

function onDmnCanvas(dmn, ui) {
    var qdmnPanelOld = ui.dmn != null ? ui.qdmnPanel() : null;
    var qdmnTileOld = ui.dmn != null ? ui.qdmnTile() : null;
    ui.dmn = dmn;
    if (qdmnPanelOld != ui.qdmnPanel()) {
        var panelset = ui.storePanelset(ui.qdmnPanel());
        if (panelset) ui.panelset = panelset;
    }
    updateTileset(qdmnTileOld,ui);
    ui.fireGrid();
    ui.fireAkter();
    ui.fireOpter();
    ui.fireToolbar();
}

function onKey(key, ui) {
    if (key === "Enter") {
        endTurn(ui);
    } else if (key === "KeyW") {
        if (ui.game.stage === "turn") ui.fireAkt("w");
    } else if (key === "KeyQ" && ui.status == "online") {
        ui.fireCmd("t");
    } else if (key === "KeyT") {
        ws.send("e");
        ui.stat = null;
        ui.fireToolbar();
    }
}

var keyTestToCmd = {"KeyX": "r", "KeyC": "c", "KeyV": "d"};

function onKeyTest([key,pst], ui) {
    if (ui.game.opterTest == null) return;
    var pg = ui.pgFromPst(pst);
    if (key === "KeyZ") {
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
        var akt = key === "KeyA" ?
        "z" + strPg(pg) + " " + (ui.numOpterTestLast == null ? 0 : ui.numOpterTestLast) :
        keyTestToCmd[key] + strPg(pg);
        ui.fireAkt(akt, pg);
        ui.fireAkter();
    }
}

function onClick(modalSurr, click, ui) {
    if (ui.pgLock) return;
    if (ui.opts != null) {
        onClickOpter(click, ui);
    } else {
        var num = ui.numFromPstOnToolbar(click);
        if (num != null) {
            onClickToolbar(modalSurr, num, ui);
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

            if (ui.game.stage === "bonus") {
                var bonus = ui.bonusFromBonusBar(click);
                if (bonus != null) {
                    onClickBonusbar(bonus, ui);
                } else clickGrid();
            } else clickGrid();
        }
    }
}

function onClickBonusbar(bonus, ui) {
    if (bonus == 0) {
        ui.decrPageBonusBar();
        ui.fireToolbar();
    }
    if (bonus == 11) {
        ui.incrPageBonusBar();
        ui.fireToolbar();
    }
    if (bonus >= 1 && bonus <= 10) ui.fireAkt("s" + ((bonus - 1) + 10 * ui.pageBonusBar));
}

function endTurn(ui) {
    if (ui.game.stage === "turn") ui.fireAkt("e");
}

function onClickToolbar(modalSurr, num, ui) {
    if (num == 0) {
        endTurn(ui);
    } else if (num == 1) {
        if (ui.status == "online") ws.send("p1 1");
        else if (ui.status == "queue" || ui.status == "match" || ui.status == "invite" || ui.status == "wait") ws.send("d");
        else if (ui.status == "game") modalSurr.show();
    } else if (num == 2) {
        // открыть чат
    } else if (num == 9) {
        if (ui.status == "match") {
            ui.fireCmd("y");
            audoiMatch.pause();
        }
    }
}

function onClickOpter(click, ui) {
    var num = ui.numFromPstOnOpter(click);
    if (num != null) {
        if (num < ui.opts.length && ui.opts[num].isOn) {
            ui.fireAkt(ui.aktSelect(num));
            ui.opts = null;
            ui.fireOpter();
            ui.fireAkter();
        }
    } else {
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
                ui.fireAkt("a" + strPg(focus.pg) + " " + focus.idx + " " + strPg(pg), pg);
            }
        } else if (R.equals(focus.pg, pg)) {
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

var audioYourTurn = new Audio("turn.mp3");

function onMemo(memo, ui) {
    ui.memo = memo;
    var dmnGameOld = ui.game != null ? ui.game.dmn : null;
    var qdmnTileOld = ui.game != null ? ui.qdmnTile(): null;
    ui.game = R.last(memo);
    ui.instant = Date.now();

    // поменять фокус
    if (ui.game.stage == "turn") {
        if (ui.game.focus != null && ui.game.spots[strPg(ui.game.focus)] != null) ui.updateFocus(ui.game.focus); else ui.clearFocus();
    } else {
        if (ui.focus != null && ui.game.spots[strPg(ui.focus.pg)] == null) ui.clearFocus();
    }

    // нужно ли изменить tileset
    if (dmnGameOld == null || !R.equals(dmnGameOld, ui.game.dmn)) {
        updateTileset(qdmnTileOld, ui);
    }

    // звук пора ходить
    if (!ui.game.isVsRobot && ui.memo.length > 1) {
        var cur = ui.game.stage;
        var prev = ui.memo[ui.memo.length - 2].stage;
        if (cur === "turn" && prev !== "turn") audioYourTurn.play()
    }

    // сбросить страницу в bonusBar
    if (ui.game.stage == "bonus") ui.pageBonusBar = 0;

    // сбросить счетчик спешки
    ui.dtAktHurry = new Date();

    ui.pgLock = null;
    ui.fireGrid();
    ui.fireAkter();
    ui.fireToolbar();
}

function onTileset(tileset, ui) {
    if (ui.tileset == null || ui.qdmnTile() == tileset.step) {
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
    if (ui.panelset == null || ui.qdmnPanel() == panelset.step) {
        ui.panelset = panelset;
        ui.fireToolbar();
    }
}

var audioHurry = new Audio("hurry.mp3");

function onSecond(_, ui) {
    if (ui.game == null || ui.game.clock == null) return;
    if (ui.game.stage == "turn" && (new Date() - ui.dtAktHurry) >= 30 * 1000) {
        ui.dtAktHurry = new Date();
        audioHurry.play();
        ui.dtHurry = new Date();
        ui.fireToolbar();
    }
    if (ui.dtHurry != null && new Date() - ui.dtHurry >= 3 * 1000) {
        ui.dtHurry = null;
        ui.fireToolbar();
    }
    ui.fireClock();
    if (ui.game.clockIsOn[1] && ui.intervalElapsed() >= ui.game.clock[1]) ui.fireCmd("o");
}

function initKeyTest(keyboard) {
    var mouse = initMouse();
    return Kefir.combine([keyboard.key("KeyZ", "KeyX", "KeyC", "KeyV", "KeyA")], [mouse]);
}

function initKey(keyboard) {
    return Kefir.merge([keyboard.key("Enter", "KeyQ", "KeyW", "KeyT")]);
}

function updateTileset(qdmnTileOld, ui) {
    if(ui.game==null) return;
    if (qdmnTileOld != ui.qdmnTile()) {
        var tileset = ui.storeTileset(ui.qdmnTile());
        if (tileset) ui.tileset = tileset;
    }
}

var audoiMatch = new Audio("match.mp3");

function onStatus(status, ui) {
    ui.status = status;
    if (ui.status === "match") audoiMatch.play();
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
            return (xx >= 0 && xx < this.qdmnTile() * this.game.dmn.xr && yy >= 0 && yy < this.qdmnTile() * this.game.dmn.yr) ? {
                x: div(xx, this.qdmnTile()),
                y: div(yy, this.qdmnTile())
            } : null;
        },
        pstGrid() {
            var xr = this.dmn.xr;
            var yr = this.dmn.yr;
            var qp = ui.qdmnPanel();
            var xrTbBb = qp + (ui.game.stage == "bonus" ? qp / 2 : 0);
            var xrGrid = this.qdmnTile() * ui.game.dmn.xr;
            var yrGrid = this.qdmnTile() * ui.game.dmn.yr;
            var diff = Math.max(xr - (xrGrid + xrTbBb), 0);
            return {x: xrTbBb + div(diff, 2), y: div(yr - yrGrid, 2)};
        },
        pstToolbar() {
            var pstGrid = ui.pstGrid();
            var qp = ui.qdmnPanel();
            var xrTbBb = qp + (ui.game.stage == "bonus" ? qp / 2 : 0);
            return {x: Math.max(0, pstGrid.x - xrTbBb), y: 0};
        },
        pstBonusbar() {
            var pstTb = ui.pstToolbar();
            var qp = ui.qdmnPanel();
            return {x: pstTb.x + qp, y: 0};
        },
        updateFocus(pg) {
            this.focus = {pg, idx: 0};
        },
        incrIdxFocus() {
            this.focus.idx = (this.focus.idx + 1) % ui.game.spots[strPg(this.focus.pg)].length;
        },
        clearFocus() {
            ui.focus = null;
        },
        incrPageBonusBar(){
            ui.pageBonusBar = Math.min(ui.pageBonusBar + 1, 2);
        },
        decrPageBonusBar(){
            ui.pageBonusBar = Math.max(ui.pageBonusBar - 1, 0);
        },
        akts(){
            return ui.game.spots[strPg(this.focus.pg)][this.focus.idx].akts
        },
        sizeSloys(){
            return ui.game.spots[strPg(this.focus.pg)].length
        },
        openOpter(opts, aktSelect) {
            this.opts = opts;
            this.aktSelect = aktSelect;
            var tilesetOpter = ui.storeTileset(this.qdmnTileOpter());
            if (tilesetOpter) this.tilesetOpter = tilesetOpter;
        },
        qdmnTileOpter(){
            var dmn = this.dmnOpter();
            var xBest = findBest(qdmn => ui.dmn.xr - qdmn * dmn.xr, listQdmnTile);
            var yBest = findBest(qdmn => ui.dmn.yr - qdmn * dmn.yr, listQdmnTile);
            return Math.min(xBest, yBest);
        },
        dmnOpter() {
            var xr = R.max(5, Math.ceil(Math.sqrt(ui.opts.length)));
            return {xr, yr: div(ui.opts.length, xr) + sign(ui.opts.length % xr)};
        },
        numFromPstOnOpter(pst) {
            return isPstInRect(pst, this.pstOpter(), scaleDmn(this.dmnOpter(), this.qdmnTileOpter())) ?
            div(pst.x - this.pstOpter().x, this.qdmnTileOpter()) + div(pst.y - this.pstOpter().y, this.qdmnTileOpter()) * this.dmnOpter().xr : null;
        },
        numFromPstOnToolbar(pst){
            var pstT = ui.pstToolbar();
            var qp = ui.qdmnPanel();
            if (isPstInRect(pst, pstT, scaleDmn({xr: 1, yr: 2}, qp)))
                return div(pst.y - this.pstToolbar().y, qp);
            else if (ui.status == "match" && isPstInRect(pst, {x: pstT.x + qp, y: pstT.y + qp}, {xr: qp, yr: qp}))
                return 9
        },
        bonusFromBonusBar(pst){
            return isPstInRect(pst, this.pstBonusbar(), scaleDmn({xr: 1, yr: 12},
                this.qdmnPanel() / 2)) ? div(pst.y - this.pstBonusbar().y, this.qdmnPanel() / 2) : null;
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
            return findBest(qdmn => ui.dmn.yr - qdmn * 6, listQdmnPanel);
        },
        xrTbBb() {
            var qp = ui.qdmnPanel();
            return qp + (ui.game.stage == "bonus" ? qp / 2 : 0);
        },
        qdmnTile(){
            var xr = ui.dmn.xr - ui.xrTbBb();
            var xBest = findBest(qdmn => xr - qdmn * ui.game.dmn.xr, listQdmnTile);
            var yBest = findBest(qdmn => ui.dmn.yr - qdmn * ui.game.dmn.yr, listQdmnTile);
            var scale = R.indexOf(R.min(xBest, yBest), listQdmnTile);
            return listQdmnTile[scale];
        },
        intervalElapsed(){
            return Date.now() - this.instant;
        },
        storeTileset: storeImages(tileset, "tile"),
        storePanelset: storeImages(panelset, "panel"),
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
        fireAkt(akt, pgLock = {}) {
            if (ui.pgLock != null) return;
            ws.send("a" + ui.game.version + "#" + akt);
            ui.clearFocus();
            ui.dtAktLast = new Date();
            ui.pgLock = pgLock;
        },
        fireCmd(cmd) {
            if (ui.pgLock != null) return;
            ws.send(cmd);
            ui.pgLock = {};
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

    return Kefir.fromEvents(w, "resize", makeBounds).toProperty(makeBounds);
}

function initKeyboard() {
    var keys = Kefir.merge([Kefir.fromEvents(document, 'keydown', R.prop("code")), Kefir.fromEvents(document, 'keyup', R.always(null))])
        .skipDuplicates().filter(key => key != null);
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
        ws.onclose = () => showReconnect();
    });
    if (isLocal) messages.onValue(msg => console.log(msg.length <= 50 ? msg : msg.substring(0, 50) + "..."));
    return {
        msg(tp) {
            return messages.filter(R.pipe(R.head, R.equals(tp))).map(R.tail);
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
    Kefir.combine([server.msg("q").map(Date.now)], [keyboard.key("KeyP").onValue(() => ws.send("q")).map(Date.now)], R.subtract).onValue(v =>
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

function createReconnect() {
    $("#btnRefresh").click(() => location.reload());
}

function initSurr() {
    var modal = $.UIkit.modal("#modalSurr", {bgclose: true});
    return {
        stream: Kefir.fromEvents($("#btnSurr"), "click"),
        modal,
        onSurr(e, ui){
            ui.fireAkt("u");
            modal.hide();
        }
    };
}

function initStat(server) {
    return {
        stream: server.msg("a").map(JSON.parse),
        onEvent(stat, ui){
            ui.stat = stat;
            ui.fireToolbar();
        }
    }
}