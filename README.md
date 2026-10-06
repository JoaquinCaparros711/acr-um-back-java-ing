# Dental Management System - Backend 🦷

A RESTful API built with **Java** and **Spring Boot** for comprehensive management of patients, appointments, and dental clinical records. Developed as an academic project for the **Programming II** course at **Universidad de Mendoza (UM)**.

---

## 🎯 The Problem

In traditional dental clinics, clinical and administrative information is often fragmented or recorded on physical paper. This leads to:
* **Physical media dependency:** Slow and cumbersome access to paper-based patient records.
* **Information fragmentation:** Disconnect between appointment scheduling, patient personal data, and medical history.
* **Lack of traceability:** Inability to quickly track and review a patient's historical medical evolutions.
* **Risk of data loss:** Vulnerability of paper records to physical damage, loss, or deterioration.

---

## 🚀 Key Features & Architecture

1. **Authentication & JWT Security:**
   - Stateless authentication powered by JSON Web Tokens (JWT) and Spring Security.
   - Passwords securely hashed with `BCryptPasswordEncoder`.
   - Token-based stateless session management.

2. **Per-Dentist Data Isolation (User-Level Multi-Tenancy):**
   - Each authenticated dentist accesses and manages only their own registered patients, appointments, and clinical records.
   - User identity context is automatically extracted from validated JWT claims on every request.

3. **Patient Management:**
   - Complete CRUD operations for patient records.
   - Real-time searching by full name or DNI.
   - Regex validation on name formats and database compound unique constraint `(dni, user_id)`.

4. **Appointment Management:**
   - Schedule, update, cancel, and complete appointments.
   - Overlap conflict detection for time slots (`AppointmentConflictException`).
   - Temporal consistency validation ensuring `startTime` precedes `endTime`.
   - Optional filtering by date (`LocalDate`) and `patientId`.
   - Appointment status lifecycle: `SCHEDULED`, `COMPLETED`, `CANCELLED`.

5. **Clinical History & Evolutions:**
   - Record medical evolution entries (`ClinicalRecord`) tied to a patient and optionally linked to a specific appointment.
   - Automatic clinical record generation upon completing an appointment with evolution notes.
   - Fetch complete patient clinical history ordered chronologically (newest first).

6. **Global Exception Handling:**
   - Centralized exception handler producing standardized JSON error responses with precise HTTP status codes (400 Bad Request, 401 Unauthorized, 403 Forbidden, 404 Not Found, 409 Conflict).

---

## 🛠️ Tech Stack

* **Language:** Java 17
* **Framework:** Spring Boot 4.0.5 (Spring MVC, Spring Data JPA, Spring Security, Validation)
* **Database:** MySQL
* **Security:** Spring Security + JWT (`io.jsonwebtoken:jjwt-api 0.12.6`)
* **Utilities:** Lombok, Jackson (Datatype JSR310)
* **Build Tool:** Maven

---

## 📡 API Endpoints (`/api/v1`)

All protected endpoints require the HTTP header `Authorization: Bearer <jwt_token>`.

### 🔑 Authentication (`/api/v1/auth`)

| Method | Endpoint | Description | Request Body |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/register` | Register a new dentist | `{ "fullName", "email", "password" }` |
| `POST` | `/api/v1/auth/login` | Authenticate and obtain JWT | `{ "email", "password" }` |

---

### 👤 Patients (`/api/v1/patients`)

| Method | Endpoint | Description | Parameters / Body |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/patients` | List all patients for authenticated dentist | `?search={query}` (optional: filter by name or DNI) |
| `GET` | `/api/v1/patients/{id}` | Get patient details by ID | - |
| `POST` | `/api/v1/patients` | Register a new patient | `{ "fullName", "dni", "phone", "birthDate", "notes" }` |
| `PUT` | `/api/v1/patients/{id}` | Update patient details | `{ "fullName", "dni", "phone", "birthDate", "notes" }` |
| `DELETE` | `/api/v1/patients/{id}` | Remove a patient | - |

---

### 📅 Appointments (`/api/v1/appointments`)

| Method | Endpoint | Description | Parameters / Body |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/appointments` | List dentist appointments | `?date=YYYY-MM-DD` & `?patientId={id}` (optional) |
| `GET` | `/api/v1/appointments/{id}` | Get appointment details by ID | - |
| `POST` | `/api/v1/appointments` | Schedule a new appointment | `{ "patientId", "startTime", "endTime", "reason" }` |
| `PUT` | `/api/v1/appointments/{id}` | Update appointment details / time | `{ "patientId", "startTime", "endTime", "reason" }` |
| `PATCH` | `/api/v1/appointments/{id}/cancel` | Cancel an appointment | - |
| `PATCH` | `/api/v1/appointments/{id}/complete` | Mark complete & record clinical notes | Optional body: `{ "notes": "Clinical notes..." }` |
| `DELETE` | `/api/v1/appointments/{id}` | Cancel / delete an appointment | - |

---

### 📑 Clinical Records (`/api/v1/patients`)

| Method | Endpoint | Description | Request Body |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/patients/{patientId}/clinical-history` | Get complete clinical history for a patient (newest first) | - |
| `POST` | `/api/v1/patients/{patientId}/clinical-records` | Add a manual clinical record entry | `{ "notes": "Treatment details..." }` |

---

## ⚙️ Local Setup & Installation

### Prerequisites
* **Java 17** or higher installed.
* **MySQL** Server running locally on port `3306`.
* **Maven** (or use the included `./mvnw` wrapper).

### Configuration Steps

1. **Clone the repository:**
   ```bash
   git clone <REPOSITORY_URL>
   cd dental-management-backend
   ```

2. **Configure Database Credentials:**
   Copy the provided local development properties template:
   ```bash
   cp src/main/resources/application.properties.example src/main/resources/application-dev.properties
   ```
   Edit `application-dev.properties` to set your local MySQL URL, username, and password:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/dental_management?createDatabaseIfNotExist=true
   spring.datasource.username=root
   spring.datasource.password=your_password
   ```

3. **Run the application:**
   Using the Maven wrapper:
   ```bash
   ./mvnw spring-boot:run
   ```
   Or using installed Maven:
   ```bash
   mvn spring-boot:run
   ```

The API will start running at `http://localhost:8080`.