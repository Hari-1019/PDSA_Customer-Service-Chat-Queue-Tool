const socket = new SockJS('/customer-service-chat-queue/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, () => {
    console.log('Connected to WebSocket');

    // Subscribe to messages
    stompClient.subscribe('/topic/messages', (message) => {
        const msg = JSON.parse(message.body);
        const messagesDiv = document.getElementById('messages');
        const newMessage = document.createElement('div');
        newMessage.textContent = `${msg.sender}: ${msg.messageText}`;
        messagesDiv.appendChild(newMessage);
    });
});

// Send a message
document.getElementById('sendMessage').addEventListener('click', () => {
    const messageInput = document.getElementById('messageInput');
    const message = messageInput.value;
    stompClient.send('/app/chat.send', {}, JSON.stringify({ messageText: message }));
    messageInput.value = '';
});