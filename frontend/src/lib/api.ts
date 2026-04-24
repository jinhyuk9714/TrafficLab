export const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";

export type ScenarioType = "CONCERT_BOOKING";
export type StrategyType = "UNSAFE" | "OPTIMISTIC_LOCK" | "PESSIMISTIC_LOCK" | "REDIS_LOCK";
export type RunStatus = "PENDING" | "RUNNING" | "COMPLETED" | "FAILED" | "CANCELLED";

export interface Scenario {
  id: number;
  type: ScenarioType;
  name: string;
  description: string;
  enabled: boolean;
}

export interface ExperimentPayload {
  name: string;
  scenarioType: ScenarioType;
  strategyType: StrategyType;
  concurrentUsers: number;
  totalRequests: number;
  targetSeatCount: number;
  hotspotMode: boolean;
  artificialDelayMs: number;
}

export interface Experiment extends ExperimentPayload {
  id: number;
  createdAt: string;
}

export interface RunSummary {
  id: number;
  experimentId: number;
  status: RunStatus;
  startedAt: string | null;
  finishedAt: string | null;
  totalRequests: number;
  successCount: number;
  failureCount: number;
  duplicateReservationCount: number;
  invariantViolationCount: number;
  throughput: number;
  p50LatencyMs: number;
  p95LatencyMs: number;
  p99LatencyMs: number;
  elapsedMs: number;
  errorMessage: string | null;
}

export interface RunEvent {
  id: number;
  runId: number;
  type: string;
  message: string;
  createdAt: string;
}

export interface ProgressMessage {
  completedRequests: number;
  totalRequests: number;
  successCount: number;
  failureCount: number;
  throughput: number;
}

export interface Reservation {
  id: number;
  runId: number;
  seatId: number | null;
  seatNumber: number;
  userKey: string;
  strategyType: StrategyType;
  success: boolean;
  failureReason: string | null;
  latencyMs: number;
  createdAt: string;
}

export interface ReservationPage {
  items: Reservation[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(init?.headers ?? {})
    }
  });
  if (!response.ok) {
    const body = await response.text();
    throw new Error(body || `HTTP ${response.status}`);
  }
  return response.json() as Promise<T>;
}

export const api = {
  scenarios: () => request<Scenario[]>("/api/scenarios"),
  experiments: () => request<Experiment[]>("/api/experiments"),
  createExperiment: (payload: ExperimentPayload) =>
    request<Experiment>("/api/experiments", {
      method: "POST",
      body: JSON.stringify(payload)
    }),
  startRun: (experimentId: number) =>
    request<{ runId: number; status: RunStatus }>(`/api/experiments/${experimentId}/runs`, { method: "POST" }),
  experimentRuns: (experimentId: number) => request<RunSummary[]>(`/api/experiments/${experimentId}/runs`),
  run: (runId: number) => request<RunSummary>(`/api/runs/${runId}`),
  reservations: (runId: number, size = 100) => request<ReservationPage>(`/api/runs/${runId}/reservations?size=${size}`),
  exportMarkdown: async (runId: number) => {
    const response = await fetch(`${API_BASE_URL}/api/runs/${runId}/export`);
    if (!response.ok) {
      throw new Error(await response.text());
    }
    return response.text();
  },
  reset: () => request<{ status: string }>("/api/lab/reset", { method: "POST" })
};

export function parseProgress(message: string): ProgressMessage | null {
  try {
    const parsed = JSON.parse(message) as ProgressMessage;
    if (typeof parsed.completedRequests === "number") {
      return parsed;
    }
  } catch {
    return null;
  }
  return null;
}
