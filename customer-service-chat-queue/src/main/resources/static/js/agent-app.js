(function(){
    const aid = localStorage.getItem('agent_id') || '1';
    const assign = document.getElementById('assignment');

    ChatWS.init(() => {
        ChatWS.sub(`/topic/agent.${aid}`, a => {
            localStorage.setItem('last_session', a.sessionId);
            assign.textContent = `Assigned: ${a.customerDisplayName} (${a.customerType}) — "${a.customerMessage}" [Session ${a.sessionId}]`;
        });
    });

    document.getElementById('goOnline').onclick = () => {
        ChatWS.send('/app/agent.available', { agentId: Number(aid), available: true });
    };
    document.getElementById('goOffline').onclick = () => {
        ChatWS.send('/app/agent.available', { agentId: Number(aid), available: false });
    };
})();
