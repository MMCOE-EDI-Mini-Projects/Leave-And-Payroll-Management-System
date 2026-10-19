# LPMS Setup and Deployment Guide

This guide provides step-by-step instructions for team members to set up, build, and run the **Leave and Payroll Management System (LPMS)** on their local machines.

## 1. Prerequisites
Before you begin, ensure you have the following installed on your machine:
- **Java Development Kit (JDK) 17**: Ensure `JAVA_HOME` is set correctly.
- **MySQL Server (8.x)**: Running locally on port `3306`.
- **Apache Tomcat (10.x or 9.x)**: Make sure it supports Jakarta Servlet API 6.0.
- **Apache Maven**: For building the project.
- **IDE** (Optional but recommended): IntelliJ IDEA, Eclipse, or VS Code.

---

## 2. Database Setup

The system requires a MySQL database to function. The complete database structure is provided in the schema file.

1. Connect to MySQL as `root` (or an admin user).
2. Run `database/schema.sql` (creates database `lpms`, 16 tables; safe to re-run).
3. Run `database/seed.sql` (roles, departments, leave types, salary components, default admin `admin` / `Admin@123` - change after first login).

### 2.1 Database Credentials
No code change needed. Set environment variables `LPMS_DB_URL`, `LPMS_DB_USER`, `LPMS_DB_PASSWORD`
(or JVM props `-Dlpms.db.url/user/password`), or edit `src/main/resources/db.properties`.
Check connectivity at `http://localhost:8080/lpms/health`.

---

## 3. Building the Application

Since this is a Maven project, it must be compiled and packaged into a `.war` (Web Application Archive) file before deployment.

### Using Command Line
1. Open a terminal or command prompt in the root directory of the project (where the `pom.xml` is located).
2. Run the following Maven command:
   ```bash
   mvn clean install
   ```
3. If the build is successful, you will see a `BUILD SUCCESS` message. A new directory named `target/` will be created containing the `lpms.war` file.

### Using an IDE
- If you are using IntelliJ or Eclipse, you can import the project as a "Maven Project" and use the IDE's built-in Maven tool window to run the `clean` and `install` lifecycles.

---

## 4. Deploying to Apache Tomcat

### Option A: Manual Deployment
1. Navigate to the `target/` folder in your project directory.
2. Copy the `lpms.war` file.
3. Go to your Apache Tomcat installation directory and open the `webapps/` folder.
4. Paste `lpms.war` into the `webapps/` folder.
5. Start Tomcat by running `bin/startup.bat` (Windows) or `bin/startup.sh` (Mac/Linux).
6. Tomcat will automatically extract the `.war` file and deploy the application.

### Option B: Deploying via IDE (e.g., Eclipse / IntelliJ)
1. Add a Local Tomcat Server Run Configuration in your IDE.
2. Add the `lpms:war exploded` artifact to the deployment list.
3. Set the application context path to `/lpms`.
4. Click **Run** or **Debug** to start the server directly from the IDE.

---

## 5. Accessing the Application

Once Tomcat is running and the application is deployed:
1. Open your web browser.
2. Navigate to: `http://localhost:8080/lpms` 
   *(Note: If you configured your Tomcat on a different port, change `8080` accordingly).*
3. You should see the LPMS Login Screen.

---

## 6. Seed Data (First Time Login)
Because the database is fresh, there are no users to log in with. You will need to manually insert at least one `HR_ADMIN` user directly into your database to get started.

Run the following SQL in your MySQL client to create an initial HR Admin user:

```sql
-- 1. Create a dummy department
INSERT INTO department (department_code, department_name) VALUES ('HR', 'Human Resources');

-- 2. Create a dummy designation
INSERT INTO designation (designation_code, designation_name) VALUES ('HR_MGR', 'HR Manager');

-- 3. Create the Admin user
-- The password hash below corresponds to the password: 'admin'
INSERT INTO employee (
    employee_code, first_name, last_name, gender, email, date_of_joining, employment_type, 
    department_id, designation_id, username, password_hash, role
) VALUES (
    'EMP001', 'System', 'Admin', 'OTHER', 'admin@lpms.com', CURRENT_DATE, 'FULL_TIME', 
    1, 1, 'admin', '$2a$12$R.P90qKzQ2jC/HkYjXk0X.xI4p5c.3Gk.X9u.P.X.X.X.X.X.X.X.', 'HR_ADMIN'
);
```

You can now log in to the application using:
- **Username:** admin
- **Password:** admin

Once logged in, the Admin can use the system to add other employees and managers.

---

## Authentication and Roles (Stage 2)

- **No self-registration.** HR_ADMIN creates accounts (Employees page) and sets a temporary password; users change it under *My Account*.
- Passwords: BCrypt (cost 12), min 8 chars with a letter and a digit. Login locks a username+IP for 15 min after 5 failures.
- Sessions: new session id on login, HttpOnly + SameSite=Strict cookie, 30 min idle timeout, invalidated on logout. Role/active status/password are re-checked from the database on every API request, so deactivation, role change and password reset take effect immediately.
- All `POST/PUT` API calls require the header `X-Requested-With: LPMS` (CSRF defence; the bundled UI sends it).
- Pages: `login.html` is public; `index.html` and `js/app.js` redirect to login without a session. Permissions are enforced server-side (`AuthenticationFilter` + servlet checks); the UI only hides controls.
- Over HTTPS, add `<secure>true</secure>` to the cookie config in `web.xml`.
- Default seed admin `admin` / `Admin@123` - change it on first login.
