# SmartCity 360

## Intelligent Citizen Grievance and Municipal Service Management System

SmartCity 360 is a full-stack municipal service management platform designed to help citizens report civic issues, track complaints, and receive updates while enabling officers and administrators to manage complaints, priorities, assignments, and analytics.

The system provides three main roles:

- Citizen
- Officer
- Admin

---

## Features

### Citizen Portal

- Citizen registration and login
- Citizen dashboard
- Report a civic problem
- Live complaint classification preview
- Complaint tracking
- Complaint status updates
- Feedback for resolved complaints
- Photo and location support
- In-app notifications

### Officer Portal

- Priority-based complaint queue
- Department-based complaint filtering
- Complaint assignment
- Complaint status management
- Mark complaints as resolved

### Admin Portal

- Analytics dashboard
- Complaint statistics
- Category-wise analysis
- Department workload
- Monthly filed vs resolved trends
- Status breakdown
- Officer management

---

## Technology Stack

### Backend

- Java 17
- Spring Boot 3
- Spring Security
- JWT Authentication
- Spring Data JPA
- MySQL
- Maven

### Frontend

- React 18
- Vite
- React Router
- Axios
- Chart.js
- react-chartjs-2

### Classification

The current complaint classification uses a rule-based keyword classification service.

The classification logic is implemented in:

`ClassificationService.java`

The service is designed so that a Python NLP/ML service can be integrated later without changing the main controller and DTO structure.

---

## Project Structure

```text
smartcity360/
│
├── backend/
│   ├── pom.xml
│   └── src/
│
├── frontend/
│   ├── package.json
│   ├── src/
│   └── public/
│
├── .gitignore
└── README.md
```

## Email Notifications

The backend sends notification emails through Gmail SMTP. Configure `MAIL_USERNAME` with the Gmail account, `MAIL_PASSWORD` with that account's Google app password, and `MAIL_FROM` with the sender address (use the same Gmail address as `MAIL_USERNAME`). The backend retries temporary SMTP send failures up to three times. Complaint notifications go to the email address stored on each recipient's account, including the admin email configured in the database. In-app notifications are stored separately and do not depend on email delivery. SMTP acceptance does not guarantee inbox placement; check Spam and verify SPF, DKIM, and DMARC if sending from a custom domain.