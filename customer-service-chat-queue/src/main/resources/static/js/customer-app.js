(function(){
    const nameEl = document.getElementById('name');
    const typeEl = document.getElementById('type');
    const msgEl  = document.getElementById('message');
    const stat   = document.getElementById('stat');

    const tempId = crypto.randomUUID();
    sessionStorage.setItem('temp_id', tempId);

    document.getElementById('go').onclick = () => {
        const name = nameEl.value || 'Guest';
        const type = typeEl.value || 'NORMAL';
        const message = msgEl.value || 'Hello';
        sessionStorage.setItem('cx_name', name);
        sessionStorage.setItem('cx_type', type);
        sessionStorage.setItem('cx_msg', message);

        ChatWS.init(() => {
            stat.textContent = 'Enqueued. Waiting for agent…';
            ChatWS.sub(`/topic/cx.${tempId}`, () => {
                // Assigned; jump to chat window
                location.href = 'chat.html';
            });
            ChatWS.send('/app/cx.enter', { tempId, displayName: name, message, customerType: type });
        });
    };
})();
