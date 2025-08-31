// File: customer-service-chat-queue/src/main/resources/static/js/admin-app.js

const API_BASE_URL = '/api/admin';
const authToken = localStorage.getItem('authToken');

// Redirect to login if not authenticated
if (!authToken) {
    window.location.href = 'login.html';
}

// Set admin name in the dashboard
function setAdminName() {
    const userInfo = localStorage.getItem('userInfo');
    if (userInfo) {
        const user = JSON.parse(userInfo);
        document.getElementById('adminName').textContent = user.name || user.email || 'Admin';
    }
}

function setStatus(msg) {
    document.getElementById('status').textContent = msg;
}

async function fetchLists() {
    setStatus('Loading...');
    try {
        const [customersRes, agentsRes] = await Promise.all([
            fetch(`${API_BASE_URL}/queue/customers`, { headers: { 'Authorization': 'Bearer ' + authToken } }),
            fetch(`${API_BASE_URL}/queue/agents`, { headers: { 'Authorization': 'Bearer ' + authToken } })
        ]);
        if (!customersRes.ok || !agentsRes.ok) {
            throw new Error('Failed to fetch lists');
        }
        const customers = await customersRes.json();
        const agents = await agentsRes.json();
        renderLists(customers, agents);
        renderAssignSection(customers, agents);
        setStatus('');
    } catch (err) {
        setStatus(err.message || 'Error loading lists');
    }
}

function renderLists(customers, agents) {
    const customerList = document.getElementById('customerList');
    const agentList = document.getElementById('agentList');
    customerList.innerHTML = '';
    agentList.innerHTML = '';
    customers.forEach(c => {
        const li = document.createElement('li');
        li.textContent = `${c.name || 'Customer'} (ID: ${c.id})`;
        customerList.appendChild(li);
    });
    agents.forEach(a => {
        const li = document.createElement('li');
        li.textContent = `${a.name || 'Agent'} (ID: ${a.id})`;
        agentList.appendChild(li);
    });
}

function renderAssignSection(customers, agents) {
    const section = document.getElementById('assignSection');
    const selectCustomer = document.getElementById('selectCustomer');
    const selectAgent = document.getElementById('selectAgent');
    selectCustomer.innerHTML = '';
    selectAgent.innerHTML = '';
    if (customers.length && agents.length) {
        customers.forEach(c => {
            const opt = document.createElement('option');
            opt.value = c.id;
            opt.textContent = `${c.name || 'Customer'} (ID: ${c.id})`;
            selectCustomer.appendChild(opt);
        });
        agents.forEach(a => {
            const opt = document.createElement('option');
            opt.value = a.id;
            opt.textContent = `${a.name || 'Agent'} (ID: ${a.id})`;
            selectAgent.appendChild(opt);
        });
        section.style.display = '';
    } else {
        section.style.display = 'none';
    }
}

async function assignChat() {
    const customerId = document.getElementById('selectCustomer').value;
    const agentId = document.getElementById('selectAgent').value;
    setStatus('Assigning...');
    try {
        const res = await fetch(`${API_BASE_URL}/assign`, {
            method: 'POST',
            headers: {
                'Authorization': 'Bearer ' + authToken,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ customerId, agentId })
        });
        if (res.ok) {
            setStatus('Assigned successfully!');
            fetchLists();
        } else {
            const data = await res.json();
            setStatus(data.message || 'Assignment failed.');
        }
    } catch (err) {
        setStatus('Assignment failed.');
    }
}

// Logout functionality
function logout() {
    localStorage.removeItem('authToken');
    localStorage.removeItem('userInfo');
    window.location.href = 'login.html';
}

document.getElementById('assignBtn').onclick = assignChat;
document.getElementById('logoutBtn').onclick = logout;
window.onload = function() {
    setAdminName();
    fetchLists();
};