// Agent Dashboard JavaScript
const API_BASE = 'http://localhost:8080/customer-service-chat-queue/api';
let currentChatId = null;
let currentCustomerId = null;
let agentStatus = 'busy'; // Default to busy on login
let stompClient = null;

document.addEventListener('DOMContentLoaded', () => {
    const token = localStorage.getItem('token');
    const role = localStorage.getItem('role');

    if (!token || role !== 'agent') {
        alert('Access denied. Agents only.');
        window.location.href = '../customer/login.html';
        return;
    }

    // First, explicitly set agent status to busy on login via API
    setInitialBusyStatus(token);
});

function setInitialBusyStatus(token) {
    fetch(`${API_BASE}/agent/login`, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${token}`
        }
    })
    .then(response => response.json())
    .then(data => {
        agentStatus = 'busy';
        document.getElementById('agentStatus').textContent = 'busy';
        updateStatusButtons();

        // Now initialize the rest of the dashboard
        initializeAgentDashboard(token);
    })
    .catch(error => {
        console.error('Error setting initial busy status:', error);
        // Continue with initialization even if there was an error
        initializeAgentDashboard(token);
    });
}

function initializeAgentDashboard(token) {
    fetchUserInfo(token);
    setupEventListeners(token);
    startStatusChecker(token);
    connectToWebSocket(token);
}

function fetchUserInfo(token) {
    fetch(`${API_BASE}/auth/me`, {
        headers: { 'Authorization': `Bearer ${token}` },
    })
        .then(response => response.json())
        .then(user => {
            document.getElementById('welcomeMessage').textContent = `Welcome, ${user.name}`;
            // Force busy status regardless of what comes back from server
            document.getElementById('agentStatus').textContent = 'busy';
            agentStatus = 'busy';
            updateStatusButtons();
        })
        .catch(error => {
            console.error('Error fetching user info:', error);
            alert('Session expired. Please login again.');
            logout();
        });
}

function setupEventListeners(token) {
    // Status buttons
    document.getElementById('availableBtn').addEventListener('click', () => updateAgentStatus(token, 'available'));
    document.getElementById('busyBtn').addEventListener('click', () => updateAgentStatus(token, 'busy'));

    // Chat buttons
    document.getElementById('sendMessage').addEventListener('click', () => sendMessage(token));
    document.getElementById('endChat').addEventListener('click', () => endChat(token));

    // Send message on Enter key
    document.getElementById('messageInput').addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            sendMessage(token);
        }
    });

    // Get Next Customer button - still available as fallback
    const getNextCustomerBtn = document.getElementById('getNextCustomer');
    if (getNextCustomerBtn) {
        getNextCustomerBtn.addEventListener('click', () => getNextCustomer(token));
    }
}

function updateAgentStatus(token, status) {
    // Convert to lowercase to match enum values
    const statusLower = status.toLowerCase();

    fetch(`${API_BASE}/agent/status`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({ status: statusLower })
    })
        .then(response => {
            if (!response.ok) {
                return response.text().then(text => { throw new Error(text) });
            }
            return response.json();
        })
        .then(data => {
            agentStatus = data.status;
            document.getElementById('agentStatus').textContent = data.status;
            updateStatusButtons();

            // If a chat was assigned automatically
            if (data.chatAssigned && data.chatId) {
                startChat(token, data);
            } else {
                alert(data.message);
            }
        })
        .catch(error => {
            console.error('Error updating status:', error);
            alert('Failed to update status: ' + error.message);
        });
}

function startChat(token, chatData) {
    currentChatId = chatData.chatId;
    currentCustomerId = chatData.customerId;

    // Update UI
    document.getElementById('currentChatInfo').innerHTML = `
        <p><strong>Customer:</strong> ${chatData.customerName || 'Customer'}</p>
        <p><strong>Query:</strong> ${chatData.customerQuery || 'No initial query'}</p>
    `;

    document.getElementById('chatContainer').classList.remove('hidden');
    document.getElementById('waitingForChat').classList.add('hidden');

    // Subscribe to this specific chat via WebSocket
    subscribeToChatMessages(currentChatId, currentCustomerId);

    // Load chat history
    loadChatHistory(token);
}

function sendMessage(token) {
    const messageInput = document.getElementById('messageInput');
    const message = messageInput.value.trim();
    if (!message || !currentChatId) return;

    // Add message to UI immediately for better UX
    addMessageToChat('You', message, true);

    // Clear input field
    messageInput.value = '';

    // Send message via REST API
    fetch(`${API_BASE}/agent/send-message`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
            chatId: currentChatId,
            messageText: message
        })
    })
    .catch(error => {
        console.error('Error sending message:', error);
        // If there was an error, indicate it somehow in the UI
    });
}

function endChat(token) {
    if (!currentChatId) {
        alert('No active chat to end');
        return;
    }

    fetch(`${API_BASE}/agent/end-chat`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({ chatId: currentChatId })
    })
    .then(response => {
        if (!response.ok) {
            return response.text().then(text => { throw new Error(text) });
        }
        return response.json();
    })
    .then(data => {
        console.log('End chat response:', data);

        // If a new chat was automatically assigned
        if (data.newChatAssigned && data.chatId) {
            alert(data.message);
            startChat(token, data);
        } else {
            alert(data.message || 'Chat ended successfully');
            resetChatUI();
            currentChatId = null;
            currentCustomerId = null;
        }
    })
    .catch(error => {
        console.error('Error ending chat:', error);
        alert('Failed to end chat: ' + error.message);
    });
}

function getNextCustomer(token) {
    if (agentStatus !== 'available') {
        alert('You must be available to get the next customer');
        return;
    }

    fetch(`${API_BASE}/agent/next-customer`, {
        headers: { 'Authorization': `Bearer ${token}` }
    })
    .then(response => {
        if (!response.ok) {
            return response.text().then(text => { throw new Error(text) });
        }
        return response.json();
    })
    .then(data => {
        if (data.chatId) {
            startChat(token, data);
        } else {
            alert(data.message || 'No customers in queue');
        }
    })
    .catch(error => {
        console.error('Error getting next customer:', error);
        alert('Failed to get next customer: ' + error.message);
    });
}

function loadChatHistory(token) {
    fetch(`${API_BASE}/agent/chat-history/${currentChatId}`, {
        headers: { 'Authorization': `Bearer ${token}` },
    })
        .then(response => response.json())
        .then(messages => {
            // Clear any existing messages first
            document.getElementById('chatMessages').innerHTML = '';

            messages.forEach(msg => {
                const isAgent = msg.senderId !== currentCustomerId;
                const sender = isAgent ? 'You' : 'Customer';
                addMessageToChat(sender, msg.messageText, isAgent);
            });
        })
        .catch(error => {
            console.error('Error loading chat history:', error);
        });
}

function addMessageToChat(sender, message, isAgent = false) {
    const messagesDiv = document.getElementById('chatMessages');
    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${isAgent ? 'agent-message' : 'customer-message'}`;
    messageDiv.innerHTML = `<strong>${sender}:</strong> ${message}`;
    messagesDiv.appendChild(messageDiv);
    messagesDiv.scrollTop = messagesDiv.scrollHeight;
}

function resetChatUI() {
    document.getElementById('currentChatInfo').innerHTML = '<p>No active chat</p>';
    document.getElementById('chatMessages').innerHTML = '';
    document.getElementById('chatContainer').classList.add('hidden');
    document.getElementById('waitingForChat').classList.remove('hidden');
}

function updateStatusButtons() {
    const availableBtn = document.getElementById('availableBtn');
    const busyBtn = document.getElementById('busyBtn');

    availableBtn.classList.remove('active');
    busyBtn.classList.remove('active');

    if (agentStatus === 'available') {
        availableBtn.classList.add('active');
    } else if (agentStatus === 'busy') {
        busyBtn.classList.add('active');
    }
}

function startStatusChecker(token) {
    // Update queue status every 5 seconds
    setInterval(() => {
        updateQueueStatus(token);
    }, 5000);
    updateQueueStatus(token); // Initial update
}

function updateQueueStatus(token) {
    fetch(`${API_BASE}/admin/queue-status`, {
        headers: { 'Authorization': `Bearer ${token}` },
    })
        .then(response => response.json())
        .then(data => {
            document.getElementById('vipCount').textContent = data.vipCount || 0;
            document.getElementById('normalCount').textContent = data.normalCount || 0;
        })
        .catch(error => {
            console.error('Error updating queue status:', error);
        });
}

function connectToWebSocket(token) {
    const socket = new SockJS('/customer-service-chat-queue/ws');
    stompClient = Stomp.over(socket);

    stompClient.connect({}, function(frame) {
        console.log('Connected to WebSocket: ' + frame);

        // If there's already an active chat, subscribe to its messages
        if (currentChatId) {
            subscribeToChatMessages(currentChatId, currentCustomerId);
        }

        // Also subscribe to agent-specific channel for notifications
        const agentId = extractAgentIdFromToken(token);
        if (agentId) {
            stompClient.subscribe('/queue/agent/' + agentId, function(message) {
                const data = JSON.parse(message.body);
                handleAgentNotification(data, token);
            });
        }
    }, function(error) {
        console.error('Error connecting to WebSocket:', error);
        // Try reconnecting after 5 seconds
        setTimeout(() => connectToWebSocket(token), 5000);
    });
}

function subscribeToChatMessages(chatId, customerId) {
    if (!stompClient || !stompClient.connected) return;

    // Subscribe to messages from this specific chat
    stompClient.subscribe('/topic/chat/' + chatId, function(message) {
        const data = JSON.parse(message.body);
        // Only show messages from the customer (agent messages are shown immediately when sent)
        if (data.senderId === customerId) {
            addMessageToChat('Customer', data.messageText);
        }
    });
}

function handleAgentNotification(data, token) {
    console.log('Agent notification received:', data);

    switch(data.type) {
        case 'new_chat_assigned':
            if (data.chatId && agentStatus === 'available') {
                // Load the new chat
                fetch(`${API_BASE}/agent/chat-history/${data.chatId}`, {
                    headers: { 'Authorization': `Bearer ${token}` }
                })
                .then(response => response.json())
                .then(chatData => {
                    startChat(token, chatData);
                    alert('New customer assigned: ' + (chatData.customerName || 'Customer'));
                });
            }
            break;

        case 'chat_ended_by_customer':
            if (data.chatId === currentChatId) {
                alert('Customer has ended the chat.');
                resetChatUI();
                currentChatId = null;
                currentCustomerId = null;
            }
            break;
    }
}

function extractAgentIdFromToken(token) {
    // This is a placeholder - you should implement proper JWT token parsing
    // For security, it's better to get the agent ID from a secure API call
    // rather than trying to extract it from the token on the client side
    return null;
}

function logout() {
    if (stompClient && stompClient.connected) {
        stompClient.disconnect();
    }
    localStorage.clear();
    window.location.href = '../customer/login.html';
}
