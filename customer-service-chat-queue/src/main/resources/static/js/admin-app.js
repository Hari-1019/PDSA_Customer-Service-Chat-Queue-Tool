// Admin Dashboard JavaScript
const API_BASE = 'http://localhost:8080/customer-service-chat-queue/api';

document.addEventListener('DOMContentLoaded', () => {
    const token = localStorage.getItem('token');
    const role = localStorage.getItem('role');

    if (!token || role !== 'admin') {
        alert('Access denied. Admins only.');
        window.location.href = '../customer/login.html';
        return;
    }

    fetchDashboardData(token);
    // Poll every 2 seconds for real-time updates
    setInterval(() => fetchDashboardData(token), 2000);
});

function fetchDashboardData(token) {
    fetch(`${API_BASE}/admin/dashboard`, {
        headers: { 'Authorization': `Bearer ${token}` },
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Failed to fetch dashboard data');
            }
            return response.json();
        })
        .then(data => {
            console.log('Dashboard data received:', data);
            document.getElementById('vipCount').textContent = data.vipQueue || 0;
            document.getElementById('normalCount').textContent = data.normalQueue || 0;
            document.getElementById('waitingChats').textContent = data.waitingChats || 0;
            document.getElementById('activeChats').textContent = data.inChat || 0;
            document.getElementById('closedChats').textContent = data.closed || 0;

            const agentList = document.getElementById('agentList');
            agentList.innerHTML = '';
            if (data.agents && data.agents.length > 0) {
                data.agents.forEach(agent => {
                    const li = document.createElement('li');
                    li.innerHTML = `
                    <strong>${agent.name}</strong> -
                    Status: ${agent.status} -
                    Current Chat: ${agent.currentChat || 'None'} -
                    Today's Chats: ${agent.totalChatsToday}
                `;
                    agentList.appendChild(li);
                });
            } else {
                const li = document.createElement('li');
                li.textContent = 'No agents available';
                agentList.appendChild(li);
            }
        })
        .catch(error => {
            console.error('Error fetching dashboard data:', error);
        });
}

function logout() {
    localStorage.clear();
    window.location.href = '../customer/login.html';
}
