<script>
    /* Tiny helper to share a single STOMP client per page */
    window.ChatWS = (function () {
    let stomp = null;
    let connected = false;
    const queue = [];

    function init(onConnect) {
    if (stomp) { if (connected && onConnect) onConnect(); return stomp; }
    const s = new SockJS('/ws');
    stomp = Stomp.over(s);
    stomp.debug = null; // quiet
    stomp.connect({}, () => {
    connected = true;
    if (onConnect) onConnect();
    while (queue.length) queue.shift()();
});
    return stomp;
}

    function ensure(cb) {
    if (connected) cb(); else queue.push(cb);
}

    function sub(topic, handler) {
    ensure(() => stomp.subscribe(topic, frame => handler(JSON.parse(frame.body))));
}

    function send(dest, payload) {
    ensure(() => stomp.send(dest, {}, JSON.stringify(payload)));
}

    return { init, sub, send };
})();
</script>
