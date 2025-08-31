const API_BASE_URL = '/api/agent';
const WS_BASE_URL = (location.protocol === 'https:' ? 'wss://' : 'ws://') + location.host + '/ws/agent';
let currentChatId = null;
let ws = null;
const authToken = localStorage.getItem('authToken');

function setStatus(msg) {
    document.getElementById('status').textContent = msg;
}

function showChatInfo(chat) {
    document.getElementById('chatInfo').textContent = `Chat with: ${chat.customerName || 'Customer'} (ID: ${chat.chatId})`;
}

function appendMessage(sender, message) {
    const messagesDiv = document.getElementById('messages');
    const div = document.createElement('div');
    div.className = 'msg ' + (sender === 'AGENT' ? 'agent' : 'customer');
    div.textContent = `[${sender}] ${message}`;
    messagesDiv.appendChild(div);
    messagesDiv.scrollTop = messagesDiv.scrollHeight;
}

function clearMessages() {
    document.getElementById('messages').innerHTML = '';
}

async function loadAssignedChat() {
    setStatus('Loading assigned chat...');
    const res = await fetch(`${API_BASE_URL}/assigned`, {
        headers: { 'Authorization': 'Bearer ' + authToken }
    });
    if (!res.ok) {
        setStatus('Failed to load assigned chat.');
        return;
    }
    const data = await res.json();
    if (data && data.chatId) {
        currentChatId = data.chatId;
        setStatus('Chat assigned: ' + currentChatId);
        document.getElementById('chatbox').style.display = '';
        showChatInfo(data);
        connectWebSocket();
    } else {
        setStatus('No assigned chat.');
        document.getElementById('chatbox').style.display = 'none';
    }
}

function connectWebSocket() {
    if (!currentChatId) return;
    ws = new WebSocket(WS_BASE_URL + `?chatId=${currentChatId}&token=${authToken}`);
    ws.onopen = () => {
        clearMessages();
        setStatus('Connected. Start chatting!');
    };
    ws.onmessage = (event) => {
        const msg = JSON.parse(event.data);
        appendMessage(msg.senderType, msg.message);
    };
    ws.onclose = () => {
        setStatus('Connection closed.');
    };
    ws.onerror = () => {
        setStatus('WebSocket error.');
    };
}

function sendMessage() {
    const input = document.getElementById('messageInput');
    const msg = input.value.trim();
    if (!msg || !ws || ws.readyState !== WebSocket.OPEN) return;
    ws.send(JSON.stringify({ message: msg, chatId: currentChatId }));
    appendMessage('AGENT', msg);
    input.value = '';
}

async function closeChat() {
    if (!currentChatId) return;
    await fetch(`${API_BASE_URL}/chat/${currentChatId}/close`, {
        method: 'POST',
        headers: { 'Authorization': 'Bearer ' + authToken }
    });
    setStatus('Chat closed.');
    if (ws) ws.close();
    document.getElementById('chatbox').style.display = 'none';
    currentChatId = null;
}

document.getElementById('sendMessage').onclick = sendMessage;
document.getElementById('closeChat').onclick = closeChat;
document.getElementById('messageInput').addEventListener('keydown', function(e) {
    if (e.key === 'Enter') sendMessage();
});

window.onload = loadAssignedChat;