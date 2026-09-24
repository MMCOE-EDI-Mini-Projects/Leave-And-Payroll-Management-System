document.addEventListener('DOMContentLoaded', () => {
    // DOM Elements
    const loginScreen = document.getElementById('loginScreen');
    const appScreen = document.getElementById('appScreen');
    const loginForm = document.getElementById('loginForm');
    const loginError = document.getElementById('loginError');
    const logoutBtn = document.getElementById('logoutBtn');
    const userNameDisplay = document.getElementById('userNameDisplay');
    const pageTitle = document.getElementById('pageTitle');

    // Navigation Links
    const navDashboard = document.getElementById('nav-dashboard');
    const navLeave = document.getElementById('nav-leave');
    const navAttendance = document.getElementById('nav-attendance');
    const navPayroll = document.getElementById('nav-payroll');
    const navLinks = [navDashboard, navLeave, navAttendance, navPayroll];

    // Views
    const viewDashboard = document.getElementById('view-dashboard');
    const viewLeave = document.getElementById('view-leave');
    const viewAttendance = document.getElementById('view-attendance');
    const viewPayroll = document.getElementById('view-payroll');
    const views = [viewDashboard, viewLeave, viewAttendance, viewPayroll];

    // State
    let currentUser = null;

    // View Manager
    function switchView(viewId, title, activeNav) {
        views.forEach(v => v.classList.add('hidden'));
        document.getElementById(viewId).classList.remove('hidden');
        
        navLinks.forEach(n => { if(n) n.classList.remove('active'); });
        if(activeNav) activeNav.classList.add('active');
        
        pageTitle.textContent = title;
        
        if(viewId === 'view-leave' && (currentUser.role === 'MANAGER' || currentUser.role === 'HR_ADMIN')) {
            document.getElementById('managerLeaveSection').classList.remove('hidden');
            fetchPendingLeaves();
        }
    }

    navDashboard.addEventListener('click', (e) => { e.preventDefault(); switchView('view-dashboard', 'Dashboard', navDashboard); });
    navLeave.addEventListener('click', (e) => { e.preventDefault(); switchView('view-leave', 'Leave Management', navLeave); });
    navAttendance.addEventListener('click', (e) => { e.preventDefault(); switchView('view-attendance', 'Attendance', navAttendance); });
    if(navPayroll) navPayroll.addEventListener('click', (e) => { e.preventDefault(); switchView('view-payroll', 'Payroll Processing', navPayroll); });

    // --- Authentication ---
    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const username = document.getElementById('username').value;
        const password = document.getElementById('password').value;
        
        try {
            const response = await fetch('/lpms/api/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password })
            });
            
            const data = await response.json();
            
            if (response.ok) {
                currentUser = data;
                userNameDisplay.textContent = `Welcome, ${data.firstName} (${data.role})`;
                
                // RBAC UI adjustments
                if (data.role === 'HR_ADMIN') {
                    document.querySelectorAll('.admin-only').forEach(el => el.classList.remove('hidden'));
                }
                
                loginScreen.classList.add('hidden');
                appScreen.classList.remove('hidden');
                switchView('view-dashboard', 'Dashboard', navDashboard);
            } else {
                loginError.textContent = data.error || 'Login failed';
            }
        } catch (error) {
            loginError.textContent = 'Server connection error';
        }
    });

    logoutBtn.addEventListener('click', async () => {
        try {
            await fetch('/lpms/api/auth/logout', { method: 'POST' });
        } catch(e) {}
        currentUser = null;
        appScreen.classList.add('hidden');
        loginScreen.classList.remove('hidden');
        document.getElementById('username').value = '';
        document.getElementById('password').value = '';
        loginError.textContent = '';
        document.querySelectorAll('.admin-only').forEach(el => el.classList.add('hidden'));
        document.getElementById('managerLeaveSection').classList.add('hidden');
    });

    // --- Leave Management ---
    const leaveForm = document.getElementById('leaveForm');
    const leaveMsg = document.getElementById('leaveMsg');
    
    leaveForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const leaveTypeId = document.getElementById('leaveType').value;
        const startDate = document.getElementById('leaveStart').value;
        const endDate = document.getElementById('leaveEnd').value;
        const reason = document.getElementById('leaveReason').value;
        
        // Calculate total days (simple mock)
        const start = new Date(startDate);
        const end = new Date(endDate);
        const diffTime = Math.abs(end - start);
        const totalDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1;

        if (totalDays <= 0) {
            leaveMsg.className = 'text-danger';
            leaveMsg.textContent = 'End date must be after start date';
            return;
        }

        try {
            const response = await fetch('/lpms/api/leaves/apply', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ action: 'APPLY', leaveTypeId, startDate, endDate, totalDays, reason })
            });
            const data = await response.json();
            if(response.ok) {
                leaveMsg.className = 'text-success';
                leaveMsg.textContent = 'Application submitted successfully!';
                leaveForm.reset();
            } else {
                leaveMsg.className = 'text-danger';
                leaveMsg.textContent = data.error || 'Submission failed';
            }
        } catch (error) {
            leaveMsg.className = 'text-danger';
            leaveMsg.textContent = 'Connection error';
        }
    });

    // Manager View Pending Leaves
    async function fetchPendingLeaves() {
        const container = document.getElementById('pendingLeavesContainer');
        try {
            const response = await fetch('/lpms/api/leaves/pending');
            if (!response.ok) return;
            const leaves = await response.json();
            
            if (leaves.length === 0) {
                container.innerHTML = '<p>No pending leaves to approve.</p>';
                return;
            }
            
            let html = '<table style="width:100%; border-collapse: collapse; text-align: left;">';
            html += '<tr style="border-bottom: 1px solid #ccc;"><th>Emp ID</th><th>Start</th><th>End</th><th>Days</th><th>Action</th></tr>';
            leaves.forEach(l => {
                html += `<tr>
                    <td style="padding: 0.5rem;">${l.employeeId}</td>
                    <td style="padding: 0.5rem;">${l.startDate}</td>
                    <td style="padding: 0.5rem;">${l.endDate}</td>
                    <td style="padding: 0.5rem;">${l.totalDays}</td>
                    <td style="padding: 0.5rem;">
                        <button class="btn btn-primary" style="padding: 0.2rem 0.5rem; font-size: 0.8rem;" onclick="processLeave(${l.leaveApplicationId}, ${l.employeeId}, ${l.leaveTypeId}, ${l.totalDays}, '${l.startDate}', 'APPROVE')">Approve</button>
                    </td>
                </tr>`;
            });
            html += '</table>';
            container.innerHTML = html;
        } catch(e) {
            container.innerHTML = '<p class="text-danger">Failed to load pending leaves.</p>';
        }
    }

    // Expose function for inline onclick
    window.processLeave = async function(id, empId, typeId, days, start, action) {
        try {
            const response = await fetch('/lpms/api/leaves/approve', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ action: action, leaveApplicationId: id, applicantEmployeeId: empId, leaveTypeId: typeId, totalDays: days, startDate: start, comments: 'Approved by Manager' })
            });
            if(response.ok) {
                alert('Leave Approved!');
                fetchPendingLeaves();
            } else {
                alert('Failed to process leave');
            }
        } catch(e) {
            alert('Error processing leave');
        }
    };

    // --- Attendance ---
    const btnCheckIn = document.getElementById('btnCheckIn');
    const attendanceMsg = document.getElementById('attendanceMsg');

    btnCheckIn.addEventListener('click', async () => {
        const today = new Date().toISOString().split('T')[0];
        try {
            const response = await fetch('/lpms/api/attendance/mark', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ date: today, status: 'PRESENT', remarks: '' })
            });
            const data = await response.json();
            if(response.ok) {
                attendanceMsg.className = 'text-success';
                attendanceMsg.textContent = data.message;
            } else {
                attendanceMsg.className = 'text-danger';
                attendanceMsg.textContent = data.error || 'Failed to mark attendance';
            }
        } catch (error) {
            attendanceMsg.className = 'text-danger';
            attendanceMsg.textContent = 'Connection error';
        }
    });

    // --- Payroll ---
    const payrollForm = document.getElementById('payrollForm');
    const payrollMsg = document.getElementById('payrollMsg');
    
    if(payrollForm) {
        payrollForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const payrollYear = document.getElementById('payrollYear').value;
            const payrollMonth = document.getElementById('payrollMonth').value;
            
            payrollMsg.className = '';
            payrollMsg.textContent = 'Processing...';

            try {
                const response = await fetch('/lpms/api/payroll/run', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ payrollYear, payrollMonth })
                });
                const data = await response.json();
                if(response.ok) {
                    payrollMsg.className = 'text-success';
                    payrollMsg.textContent = data.message;
                } else {
                    payrollMsg.className = 'text-danger';
                    payrollMsg.textContent = data.error || 'Payroll processing failed';
                }
            } catch (error) {
                payrollMsg.className = 'text-danger';
                payrollMsg.textContent = 'Connection error';
            }
        });
    }
});
