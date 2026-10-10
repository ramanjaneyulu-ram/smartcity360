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

The backend sends notification emails through Resend. Configure `RESEND_API_KEY` and `RESEND_FROM` in the backend hosting environment. `RESEND_FROM` must be a sender address verified in Resend; the Resend onboarding test sender can only deliver to an address verified by that Resend account. Complaint emails are sent to the email address stored on each user with the `ADMIN` role, so verify the admin account's registered email address as well. In-app notifications do not depend on these email settings.