# TrafficLab Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a Dockerized full-stack concurrency experiment platform for concert seat booking.

**Architecture:** Spring Boot owns experiment execution, persistence, locking strategies, SSE, and Markdown export. Next.js owns the Korean dashboard, presets, live stream, result charts, reservation table, and export view.

**Tech Stack:** Java 21, Spring Boot 3, JPA, PostgreSQL, Redis, JUnit 5, Next.js, TypeScript, Tailwind CSS, Recharts, Docker Compose.

---

### Task 1: Backend Test Skeleton

**Files:**
- Create: `backend/build.gradle.kts`
- Create: `backend/settings.gradle.kts`
- Create: `backend/src/test/java/com/trafficlab/metrics/MetricCalculatorTest.java`
- Create: `backend/src/test/java/com/trafficlab/export/MarkdownExportServiceTest.java`
- Create: `backend/src/test/java/com/trafficlab/reservation/ReservationStrategyConcurrencyTest.java`

**Steps:**
1. Write tests for metric calculation, export content, unsafe duplicates, pessimistic locking, and Redis-style locking.
2. Run `./gradlew test`.
3. Confirm red because production classes are missing.

### Task 2: Backend Domain and Persistence

**Files:**
- Create domain enums and JPA entities under `backend/src/main/java/com/trafficlab/domain`.
- Create repositories under `backend/src/main/java/com/trafficlab/repository`.
- Create test configuration under `backend/src/test/resources/application-test.yml`.

**Steps:**
1. Implement entities without unique constraints on successful reservations.
2. Add `@Version` to `Seat`.
3. Add pessimistic lock and unsafe update repository methods.
4. Run backend tests and continue to strategy implementation failures.

### Task 3: Reservation Strategies

**Files:**
- Create strategy contracts and implementations under `backend/src/main/java/com/trafficlab/reservation`.

**Steps:**
1. Implement unsafe read-delay-update without optimistic protection.
2. Implement optimistic lock using JPA version conflict.
3. Implement pessimistic write lock.
4. Implement Redis lock through a `DistributedLockClient` abstraction.
5. Run strategy tests and fix root causes.

### Task 4: Experiment Runner and API

**Files:**
- Create services under `backend/src/main/java/com/trafficlab/experiment`.
- Create controllers under `backend/src/main/java/com/trafficlab/api`.

**Steps:**
1. Add experiment CRUD endpoints.
2. Add async run creation, seat seeding, live progress events, result computation, and final status persistence.
3. Add SSE endpoint, reservation table endpoint, Markdown export endpoint, and lab reset endpoint.
4. Run backend tests.

### Task 5: Frontend Dashboard

**Files:**
- Create Next.js app under `frontend`.

**Steps:**
1. Build one operational dashboard screen with scenario gallery, builder, live run, results, and export.
2. Use Korean UI labels, Tailwind CSS, Fetch API, and Recharts.
3. Handle loading, errors, SSE updates, and copy-to-clipboard.
4. Run `npm run build`.

### Task 6: Docker and Documentation

**Files:**
- Create: `docker-compose.yml`
- Create: `.env.example`
- Create: `infra/postgres/init.sql`
- Create: `README.md`
- Create: `AGENTS.md`

**Steps:**
1. Add PostgreSQL, Redis, backend, and frontend services.
2. Document local and Docker execution.
3. Document concurrency strategies, metrics, demo script, roadmap, and limitations.
4. Run final backend and frontend verification.
