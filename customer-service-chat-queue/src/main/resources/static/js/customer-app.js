// File: customer-service-chat-queue/src/main/resources/static/js/customer-login.js

document.getElementById('loginForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const formData = new FormData(e.target);
    const data = Object.fromEntries(formData.entries());

    try {
        const response = await fetch('http://localhost:8080/customer-service-chat-queue/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data),
        });

        if (response.ok) {
            const result = await response.json();
            localStorage.setItem('token', result.token);
            alert('Successfully logged in!');
            window.location.href = 'dashboard.html';
        } else {
            const error = await response.text();
            alert(`Error: ${error}`);
        }
    } catch (err) {
        console.error(err);
        alert('An error occurred. Please try again.');
    }
});