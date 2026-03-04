// Base URL of Spring Boot backend
const BASE_URL = "http://localhost:8080/api";

// ================= LOGIN =================

const loginForm = document.getElementById("loginForm");

if (loginForm) {
    loginForm.addEventListener("submit", async function (e) {

        e.preventDefault();

        const email = document.getElementById("email").value;
        const password = document.getElementById("password").value;

        try {

            const response = await fetch(`${BASE_URL}/login`, {

                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    email: email,
                    password: password
                })

            });

            if (response.ok) {

                const data = await response.json();

                // Save token
                localStorage.setItem("token", data.token);

                alert("Login successful ✅");

                window.location.href = "dashboard.html";

            } else {

                alert("Invalid credentials ❌");

            }

        } catch (error) {

            console.error(error);
            alert("Server error");

        }

    });
}


// ================= REGISTER =================

const registerForm = document.getElementById("registerForm");

if (registerForm) {

    registerForm.addEventListener("submit", async function (e) {

        e.preventDefault();

        const fullname = document.getElementById("fullname").value;
        const email = document.getElementById("email").value;
        const password = document.getElementById("password").value;

        try {

            const response = await fetch(`${BASE_URL}/register`, {

                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    name: fullname,
                    email: email,
                    password: password
                })

            });

            if (response.ok) {

                alert("Registration successful ✅");

                window.location.href = "login.html";

            } else {

                alert("Registration failed");

            }

        } catch (error) {

            console.error(error);

        }

    });

}


// ================= LOAD FILES (DASHBOARD) =================

async function loadFiles() {

    const token = localStorage.getItem("token");

    try {

        const response = await fetch(`${BASE_URL}/files`, {

            headers: {
                "Authorization": "Bearer " + token
            }

        });

        const files = await response.json();

        const table = document.querySelector("tbody");

        table.innerHTML = "";

        files.forEach(file => {

            const row = `
                <tr>
                    <td>${file.fileName}</td>
                    <td>${file.owner}</td>
                    <td>${file.date}</td>
                    <td>
                        <button onclick="downloadFile('${file.id}')">Download</button>
                        <button onclick="sharePage('${file.id}')">Share</button>
                    </td>
                </tr>
            `;

            table.innerHTML += row;

        });

    } catch (error) {

        console.error(error);

    }

}


// ================= UPLOAD =================

async function uploadFile(encryptedData) {

    const token = localStorage.getItem("token");

    const formData = new FormData();

    formData.append("file", new Blob([encryptedData]));

    try {

        const response = await fetch(`${BASE_URL}/upload`, {

            method: "POST",

            headers: {
                "Authorization": "Bearer " + token
            },

            body: formData

        });

        if (response.ok) {

            alert("File uploaded successfully ✅");

            window.location.href = "dashboard.html";

        }

    } catch (error) {

        console.error(error);

    }

}


// ================= SHARE =================

async function shareFile() {

    const email = document.getElementById("shareEmail").value;
    const permission = document.getElementById("permission").value;

    const token = localStorage.getItem("token");

    try {

        const response = await fetch(`${BASE_URL}/share`, {

            method: "POST",

            headers: {

                "Content-Type": "application/json",
                "Authorization": "Bearer " + token

            },

            body: JSON.stringify({

                email: email,
                permission: permission

            })

        });

        if (response.ok) {

            alert("File shared successfully ✅");

        }

    } catch (error) {

        console.error(error);

    }

}


// ================= DOWNLOAD =================

async function downloadFile(fileId) {

    const token = localStorage.getItem("token");

    window.open(`${BASE_URL}/download/${fileId}?token=${token}`);

}


// ================= LOGOUT =================

function logout() {

    localStorage.removeItem("token");

    window.location.href = "login.html";

}


// ================= AUTO LOAD =================

if (window.location.pathname.includes("dashboard.html")) {

    loadFiles();

}