(function(){
    ChatWS.init(() => {
        ChatWS.sub('/topic/admin/queue', d => {
            document.getElementById('vip').textContent = d.vipSize ?? d.vipWaiting ?? 0;
            document.getElementById('normal').textContent = d.normalSize ?? d.normalWaiting ?? 0;

            const tb = document.getElementById('agents');
            tb.innerHTML = '';
            (d.agents || []).forEach(a => {
                const tr = document.createElement('tr');
                tr.innerHTML = `<td>${a.id}</td><td>${a.agentName || a.name || ''}</td><td>${a.status}</td><td>${a.chatCount}</td>`;
                tb.appendChild(tr);
            });
        });
    });
})();
