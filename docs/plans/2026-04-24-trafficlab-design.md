# TrafficLab MVP Design

## Product Shape

TrafficLab is a full-stack concurrency experiment lab. The MVP focuses on one complete scenario: concert seat booking under concurrent request pressure. Users configure a strategy, run a real load experiment, observe live progress through Server-Sent Events, compare actual metrics, and export a portfolio-ready case study.

## Recommended Architecture

The app uses a Spring Boot backend as the experiment engine and a Next.js dashboard as the operator surface. PostgreSQL stores experiments, runs, seats, reservations, and event logs. Redis is used only by the `REDIS_LOCK` strategy through `SET NX` plus token-checked release.

Alternatives considered:

- Pure in-memory lab: faster to build but too weak for a backend portfolio.
- Queue-first architecture with Kafka: realistic at larger scale but too broad for the MVP.
- Current approach: database-backed experiment engine with Redis locking, SSE progress, and measurable results. This best matches the requested scope.

## Backend Components

- Domain entities: `Scenario`, `Experiment`, `ExperimentRun`, `Seat`, `Reservation`, `RunEvent`.
- Strategy interface: `ReservationStrategy.reserve(ReservationCommand)`.
- Strategies: `UNSAFE`, `OPTIMISTIC_LOCK`, `PESSIMISTIC_LOCK`, `REDIS_LOCK`.
- Runner: creates run seats, executes concurrent attempts asynchronously, records every result, emits progress events, computes final metrics.
- Exporter: renders measured run data into Markdown.

## Frontend Components

- Scenario gallery with Korean labels and coming-soon scenarios.
- Experiment builder with presets and strategy controls.
- Live run panel with SSE progress, counters, status badge, and event log.
- Result dashboard with metric cards, Recharts visualizations, reservation table, and Markdown export.

## Testing

Backend tests cover unsafe duplicates, pessimistic lock safety, Redis-style lock safety with a test lock client fallback, percentile and invariant metrics, and Markdown export content. Frontend verification focuses on TypeScript build and lint where configured.

## Constraints

No global unique constraint prevents duplicate successful reservations, because the lab must demonstrate unsafe invariant violations. Metrics must be computed from recorded reservation attempts, never generated.
