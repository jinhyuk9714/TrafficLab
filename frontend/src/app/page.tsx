"use client";

import { useEffect, useMemo, useState } from "react";
import {
  Activity,
  ClipboardCopy,
  Database,
  Loader2,
  Lock,
  Play,
  RotateCcw,
  ServerCrash,
  ShieldCheck,
  UnlockKeyhole,
  Zap
} from "lucide-react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis
} from "recharts";
import {
  API_BASE_URL,
  Experiment,
  ExperimentPayload,
  ProgressMessage,
  Reservation,
  RunEvent,
  RunSummary,
  Scenario,
  StrategyType,
  api,
  parseProgress
} from "@/lib/api";

const strategyLabels: Record<StrategyType, string> = {
  UNSAFE: "Unsafe",
  OPTIMISTIC_LOCK: "Optimistic Lock",
  PESSIMISTIC_LOCK: "Pessimistic Lock",
  REDIS_LOCK: "Redis Lock"
};

const presets: Array<{ name: string; description: string; payload: ExperimentPayload }> = [
  {
    name: "Safe small demo",
    description: "작은 요청량으로 흐름을 빠르게 확인합니다.",
    payload: {
      name: "Safe small demo - Optimistic",
      scenarioType: "CONCERT_BOOKING",
      strategyType: "OPTIMISTIC_LOCK",
      concurrentUsers: 20,
      totalRequests: 120,
      targetSeatCount: 20,
      hotspotMode: false,
      artificialDelayMs: 10
    }
  },
  {
    name: "Hot seat race",
    description: "Unsafe 전략의 중복 예약을 드러냅니다.",
    payload: {
      name: "Hot seat race - Unsafe",
      scenarioType: "CONCERT_BOOKING",
      strategyType: "UNSAFE",
      concurrentUsers: 60,
      totalRequests: 120,
      targetSeatCount: 1,
      hotspotMode: true,
      artificialDelayMs: 40
    }
  },
  {
    name: "Redis lock comparison",
    description: "락 획득 실패와 좌석 무결성을 확인합니다.",
    payload: {
      name: "Hot seat rush - Redis Lock",
      scenarioType: "CONCERT_BOOKING",
      strategyType: "REDIS_LOCK",
      concurrentUsers: 60,
      totalRequests: 120,
      targetSeatCount: 1,
      hotspotMode: true,
      artificialDelayMs: 30
    }
  },
  {
    name: "Pessimistic lock comparison",
    description: "DB row lock으로 직렬화된 임계 구역을 봅니다.",
    payload: {
      name: "Hot seat rush - Pessimistic",
      scenarioType: "CONCERT_BOOKING",
      strategyType: "PESSIMISTIC_LOCK",
      concurrentUsers: 50,
      totalRequests: 120,
      targetSeatCount: 1,
      hotspotMode: true,
      artificialDelayMs: 20
    }
  }
];

const scenarioCards = [
  { title: "Concert Booking", status: "enabled", detail: "동시 좌석 예약" },
  { title: "Coupon Rush", status: "coming soon", detail: "한정 쿠폰 발급" },
  { title: "Rental Reservation", status: "coming soon", detail: "예약 선점 경쟁" },
  { title: "Inventory Rush", status: "coming soon", detail: "재고 소진 이벤트" }
];

const COLD_START_NOTICE_MS = 4000;

type ComparisonRun = {
  strategy: StrategyType;
  run: RunSummary;
};

export default function Home() {
  const [scenarios, setScenarios] = useState<Scenario[]>([]);
  const [experiments, setExperiments] = useState<Experiment[]>([]);
  const [form, setForm] = useState<ExperimentPayload>(presets[1].payload);
  const [activeExperiment, setActiveExperiment] = useState<Experiment | null>(null);
  const [activeRunId, setActiveRunId] = useState<number | null>(null);
  const [run, setRun] = useState<RunSummary | null>(null);
  const [progress, setProgress] = useState<ProgressMessage | null>(null);
  const [events, setEvents] = useState<RunEvent[]>([]);
  const [reservations, setReservations] = useState<Reservation[]>([]);
  const [markdown, setMarkdown] = useState("");
  const [comparisonRuns, setComparisonRuns] = useState<ComparisonRun[]>([]);
  const [loading, setLoading] = useState(false);
  const [coldStartNotice, setColdStartNotice] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    void bootstrap();
  }, []);

  useEffect(() => {
    if (!activeRunId) {
      return;
    }

    const source = new EventSource(`${API_BASE_URL}/api/runs/${activeRunId}/events`);
    const handleEvent = (rawEvent: MessageEvent<string>) => {
      const event = JSON.parse(rawEvent.data) as RunEvent;
      setEvents((current) => [event, ...current].slice(0, 80));
      const parsedProgress = parseProgress(event.message);
      if (parsedProgress) {
        setProgress(parsedProgress);
      }
      if (event.type === "RUN_COMPLETED" || event.type === "RUN_FAILED") {
        void refreshRunArtifacts(activeRunId, activeExperiment?.strategyType);
      }
    };

    ["RUN_STARTED", "SEATS_SEEDED", "PROGRESS", "RUN_COMPLETED", "RUN_FAILED"].forEach((eventName) => {
      source.addEventListener(eventName, handleEvent as EventListener);
    });
    source.onerror = () => {
      setError("SSE 연결이 끊겼습니다. 결과 조회는 계속 시도합니다.");
    };

    const poll = window.setInterval(() => {
      void refreshRunArtifacts(activeRunId, activeExperiment?.strategyType, true);
    }, 1800);

    return () => {
      window.clearInterval(poll);
      source.close();
    };
  }, [activeRunId, activeExperiment?.strategyType]);

  const progressPercent = useMemo(() => {
    if (progress && progress.totalRequests > 0) {
      return Math.min(100, Math.round((progress.completedRequests / progress.totalRequests) * 100));
    }
    if ((run?.status === "COMPLETED" || run?.status === "FAILED") && run.totalRequests > 0) {
      return 100;
    }
    return 0;
  }, [progress, run]);

  const completedRequestsValue = useMemo(() => {
    if (progress) {
      return progress.completedRequests;
    }
    if (run?.status === "COMPLETED" || run?.status === "FAILED") {
      return run.totalRequests;
    }
    return 0;
  }, [progress, run]);

  const successCountValue = progress?.successCount ?? run?.successCount ?? 0;
  const failureCountValue = progress?.failureCount ?? run?.failureCount ?? 0;

  const startButtonLabel = loading
    ? coldStartNotice
      ? "서버를 깨우는 중"
      : "시작 중"
    : "실행";

  const successFailureData = useMemo(() => [
    { name: "성공", value: successCountValue, color: "#0f9f84" },
    { name: "실패", value: failureCountValue, color: "#e85d48" }
  ], [failureCountValue, successCountValue]);

  const latencyData = useMemo(() => [
    { name: "p50", latency: run?.p50LatencyMs ?? 0 },
    { name: "p95", latency: run?.p95LatencyMs ?? 0 },
    { name: "p99", latency: run?.p99LatencyMs ?? 0 }
  ], [run]);

  const comparisonData = comparisonRuns.map((item) => ({
    strategy: item.strategy.replace("_", " "),
    duplicates: item.run.duplicateReservationCount,
    throughput: Number(item.run.throughput.toFixed(1)),
    p95LatencyMs: item.run.p95LatencyMs
  }));

  async function bootstrap() {
    try {
      setError(null);
      const [scenarioList, experimentList] = await Promise.all([api.scenarios(), api.experiments()]);
      setScenarios(scenarioList);
      const recentExperiments = experimentList.slice().reverse().slice(0, 8);
      setExperiments(recentExperiments);
      await loadComparisonRuns(recentExperiments);
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : "초기 데이터를 불러오지 못했습니다.");
    }
  }

  async function loadComparisonRuns(experimentList: Experiment[]) {
    const runGroups = await Promise.all(
      experimentList.map(async (experiment) => {
        const runs = await api.experimentRuns(experiment.id);
        return runs
          .filter((item) => item.status === "COMPLETED")
          .map((item) => ({ strategy: experiment.strategyType, run: item }));
      })
    );
    setComparisonRuns(
      runGroups
        .flat()
        .sort((left, right) => right.run.id - left.run.id)
        .slice(0, 6)
    );
  }

  async function startExperiment() {
    let noticeTimer: number | null = null;
    try {
      setLoading(true);
      setColdStartNotice(null);
      noticeTimer = window.setTimeout(() => {
        setColdStartNotice("Render Free 서버를 깨우는 중입니다. 첫 실행은 1-2분 정도 걸릴 수 있습니다.");
      }, COLD_START_NOTICE_MS);
      setError(null);
      setRun(null);
      setProgress(null);
      setEvents([]);
      setReservations([]);
      setMarkdown("");

      const experiment = await api.createExperiment(form);
      const start = await api.startRun(experiment.id);
      setActiveExperiment(experiment);
      setActiveRunId(start.runId);
      setExperiments((current) => [experiment, ...current.filter((item) => item.id !== experiment.id)].slice(0, 8));
      setRun({
        id: start.runId,
        experimentId: experiment.id,
        status: start.status,
        startedAt: null,
        finishedAt: null,
        totalRequests: experiment.totalRequests,
        successCount: 0,
        failureCount: 0,
        duplicateReservationCount: 0,
        invariantViolationCount: 0,
        throughput: 0,
        p50LatencyMs: 0,
        p95LatencyMs: 0,
        p99LatencyMs: 0,
        elapsedMs: 0,
        errorMessage: null
      });
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : "실험을 시작하지 못했습니다.");
    } finally {
      if (noticeTimer) {
        window.clearTimeout(noticeTimer);
      }
      setColdStartNotice(null);
      setLoading(false);
    }
  }

  async function refreshRunArtifacts(runId: number, strategy?: StrategyType, quiet = false) {
    try {
      const summary = await api.run(runId);
      setRun(summary);
      if (summary.status === "COMPLETED" || summary.status === "FAILED") {
        const [reservationPage, exported] = await Promise.all([
          api.reservations(runId, 120),
          api.exportMarkdown(runId)
        ]);
        setReservations(reservationPage.items);
        setMarkdown(exported);
        if (strategy && summary.status === "COMPLETED") {
          setComparisonRuns((current) => {
            const next = current.filter((item) => item.run.id !== summary.id);
            return [{ strategy, run: summary }, ...next].slice(0, 6);
          });
        }
      }
    } catch (exception) {
      if (!quiet) {
        setError(exception instanceof Error ? exception.message : "실행 결과를 불러오지 못했습니다.");
      }
    }
  }

  async function resetLab() {
    try {
      setLoading(true);
      setColdStartNotice(null);
      await api.reset();
      setActiveExperiment(null);
      setActiveRunId(null);
      setRun(null);
      setProgress(null);
      setEvents([]);
      setReservations([]);
      setMarkdown("");
      setComparisonRuns([]);
      await bootstrap();
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : "랩 데이터를 초기화하지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }

  function applyPreset(payload: ExperimentPayload) {
    setForm(payload);
  }

  function setNumber<K extends keyof ExperimentPayload>(key: K, value: string) {
    setForm((current) => ({ ...current, [key]: Number(value) }));
  }

  return (
    <main className="min-h-screen text-ink">
      <div className="mx-auto flex w-full max-w-[1500px] gap-0 px-4 py-4 lg:px-6">
        <aside className="hidden w-72 shrink-0 border-r border-line bg-[#f7f7f4]/90 pr-5 lg:block">
          <div className="sticky top-4">
            <div className="mb-8">
              <div className="flex items-center gap-2 text-sm font-semibold text-mint">
                <Activity size={18} /> TrafficLab
              </div>
              <h1 className="mt-3 text-4xl font-semibold leading-tight">TrafficLab</h1>
              <p className="mt-3 text-sm leading-6 text-neutral-600">
                동시성 제어 전략을 실시간으로 비교하는 부하 실험 플랫폼
              </p>
            </div>

            <nav className="space-y-2 text-sm">
              {["시나리오", "실험 설정", "라이브 실행", "결과 대시보드", "케이스 스터디"].map((item) => (
                <a key={item} href={`#${item}`} className="block border-l-2 border-transparent px-3 py-2 text-neutral-600 transition hover:border-mint hover:text-ink">
                  {item}
                </a>
              ))}
            </nav>

            <div className="mt-8 border-t border-line pt-5 text-xs leading-5 text-neutral-500">
              API: {API_BASE_URL}
            </div>
          </div>
        </aside>

        <section className="min-w-0 flex-1 space-y-5 pl-0 lg:pl-6">
          <header className="flex flex-col justify-between gap-4 border-b border-line pb-4 sm:flex-row sm:items-center">
            <div>
              <div className="text-sm font-semibold text-mint lg:hidden">TrafficLab</div>
              <h2 className="mt-1 text-2xl font-semibold">콘서트 좌석 예약 부하 실험</h2>
              <p className="mt-1 text-sm text-neutral-600">실제 요청 결과로 중복 예약, 처리량, 지연 시간을 측정합니다.</p>
            </div>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={resetLab}
                className="inline-flex h-10 items-center gap-2 rounded-md border border-line bg-white px-3 text-sm font-medium transition hover:border-ink"
              >
                <RotateCcw size={16} /> 초기화
              </button>
              <button
                type="button"
                onClick={startExperiment}
                disabled={loading}
                className="inline-flex h-10 min-w-[92px] items-center justify-center gap-2 rounded-md bg-ink px-4 text-sm font-semibold text-white transition hover:bg-neutral-700 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {loading ? <Loader2 className="animate-spin" size={16} /> : <Play size={16} />}
                {startButtonLabel}
              </button>
            </div>
          </header>

          {coldStartNotice && (
            <div className="flex items-center gap-3 rounded-md border border-amber/50 bg-white px-4 py-3 text-sm text-ink shadow-panel">
              <Loader2 className="animate-spin text-amber" size={18} />
              <span>{coldStartNotice}</span>
            </div>
          )}

          {error && (
            <div className="flex items-center gap-3 rounded-md border border-coral/40 bg-white px-4 py-3 text-sm text-coral shadow-panel">
              <ServerCrash size={18} />
              <span>{error}</span>
            </div>
          )}

          <section id="시나리오" className="grid gap-3 md:grid-cols-4">
            {scenarioCards.map((scenario) => (
              <div key={scenario.title} className="rounded-md border border-line bg-white p-4 shadow-panel">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <div className="text-sm font-semibold">{scenario.title}</div>
                    <div className="mt-1 text-xs text-neutral-500">{scenario.detail}</div>
                  </div>
                  <span className={`rounded-full px-2 py-1 text-[11px] font-semibold ${scenario.status === "enabled" ? "bg-mint/10 text-mint" : "bg-neutral-100 text-neutral-500"}`}>
                    {scenario.status === "enabled" ? "활성" : "준비중"}
                  </span>
                </div>
              </div>
            ))}
          </section>

          <section className="grid gap-5 xl:grid-cols-[420px_minmax(0,1fr)]">
            <div id="실험 설정" className="rounded-md border border-line bg-white p-5 shadow-panel">
              <div className="mb-4 flex items-center justify-between">
                <div>
                  <h3 className="text-lg font-semibold">Experiment Builder</h3>
                  <p className="mt-1 text-sm text-neutral-500">전략과 부하 조건을 선택합니다.</p>
                </div>
                <Database className="text-mint" size={22} />
              </div>

              <div className="grid gap-3">
                <label className="grid gap-1 text-sm font-medium">
                  실험 이름
                  <input
                    value={form.name}
                    onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))}
                    className="h-10 rounded-md border border-line px-3 text-sm outline-none transition focus:border-mint"
                  />
                </label>

                <label className="grid gap-1 text-sm font-medium">
                  전략
                  <select
                    value={form.strategyType}
                    onChange={(event) => setForm((current) => ({ ...current, strategyType: event.target.value as StrategyType }))}
                    className="h-10 rounded-md border border-line px-3 text-sm outline-none transition focus:border-mint"
                  >
                    {Object.keys(strategyLabels).map((strategy) => (
                      <option key={strategy} value={strategy}>
                        {strategyLabels[strategy as StrategyType]}
                      </option>
                    ))}
                  </select>
                </label>

                <div className="grid grid-cols-2 gap-3">
                  <NumberField label="동시 사용자" value={form.concurrentUsers} onChange={(value) => setNumber("concurrentUsers", value)} />
                  <NumberField label="총 요청 수" value={form.totalRequests} onChange={(value) => setNumber("totalRequests", value)} />
                  <NumberField label="좌석 수" value={form.targetSeatCount} onChange={(value) => setNumber("targetSeatCount", value)} />
                  <NumberField label="지연(ms)" value={form.artificialDelayMs} onChange={(value) => setNumber("artificialDelayMs", value)} />
                </div>

                <label className="flex items-center justify-between rounded-md border border-line px-3 py-3 text-sm font-medium">
                  Hotspot mode
                  <input
                    type="checkbox"
                    checked={form.hotspotMode}
                    onChange={(event) => setForm((current) => ({ ...current, hotspotMode: event.target.checked }))}
                    className="h-5 w-5 accent-mint"
                  />
                </label>
              </div>

              <div className="mt-5 grid gap-2">
                {presets.map((preset) => (
                  <button
                    key={preset.name}
                    type="button"
                    onClick={() => applyPreset(preset.payload)}
                    className="rounded-md border border-line p-3 text-left transition hover:border-mint hover:bg-mint/5"
                  >
                    <div className="text-sm font-semibold">{preset.name}</div>
                    <div className="mt-1 text-xs text-neutral-500">{preset.description}</div>
                  </button>
                ))}
              </div>
            </div>

            <div className="space-y-5">
              <div id="라이브 실행" className="rounded-md border border-line bg-white p-5 shadow-panel">
                <div className="mb-5 flex flex-col justify-between gap-3 sm:flex-row sm:items-start">
                  <div>
                    <h3 className="text-lg font-semibold">Live Run</h3>
                    <p className="mt-1 text-sm text-neutral-500">SSE 이벤트로 실행 상태를 추적합니다.</p>
                  </div>
                  <StatusBadge status={run?.status ?? "PENDING"} />
                </div>

                <SeatPressureVisual targetSeatCount={form.targetSeatCount} hotspotMode={form.hotspotMode} running={run?.status === "RUNNING"} />

                <div className="mt-5">
                  <div className="mb-2 flex items-center justify-between text-sm">
                    <span>진행률</span>
                    <span className="font-semibold">{progressPercent}%</span>
                  </div>
                  <div className="h-3 overflow-hidden rounded-full bg-neutral-100">
                    <div className="h-full rounded-full bg-mint transition-all duration-500" style={{ width: `${progressPercent}%` }} />
                  </div>
                </div>

                <div className="mt-5 grid gap-3 sm:grid-cols-4">
                  <MiniMetric label="완료 요청" value={completedRequestsValue} />
                  <MiniMetric label="성공" value={successCountValue} tone="mint" />
                  <MiniMetric label="실패" value={failureCountValue} tone="coral" />
                  <MiniMetric label="현재 TPS" value={formatNumber(progress?.throughput ?? run?.throughput ?? 0)} tone="amber" />
                </div>

                <div className="mt-5 max-h-52 overflow-auto rounded-md border border-line bg-[#fbfbf9] p-3">
                  {events.length === 0 ? (
                    <div className="text-sm text-neutral-500">아직 이벤트가 없습니다.</div>
                  ) : (
                    <div className="space-y-2">
                      {events.map((event) => (
                        <div key={event.id} className="grid gap-1 border-b border-line/70 pb-2 last:border-b-0">
                          <div className="flex items-center justify-between gap-3 text-xs">
                            <span className="font-semibold text-mint">{event.type}</span>
                            <span className="text-neutral-400">{new Date(event.createdAt).toLocaleTimeString("ko-KR")}</span>
                          </div>
                          <div className="break-words text-xs text-neutral-600">{event.message}</div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>

              <div id="결과 대시보드" className="rounded-md border border-line bg-white p-5 shadow-panel">
                <div className="mb-5 flex items-center justify-between">
                  <div>
                    <h3 className="text-lg font-semibold">Result Dashboard</h3>
                    <p className="mt-1 text-sm text-neutral-500">저장된 예약 시도에서 계산한 최종 지표입니다.</p>
                  </div>
                  {run?.duplicateReservationCount ? <UnlockKeyhole className="text-coral" /> : <ShieldCheck className="text-mint" />}
                </div>

                <div className="grid gap-3 md:grid-cols-3 xl:grid-cols-6">
                  <MetricCard label="총 요청" value={run?.totalRequests ?? 0} />
                  <MetricCard label="성공" value={run?.successCount ?? 0} tone="mint" />
                  <MetricCard label="실패" value={run?.failureCount ?? 0} tone="coral" />
                  <MetricCard label="중복 예약" value={run?.duplicateReservationCount ?? 0} tone="amber" />
                  <MetricCard label="p95 지연" value={`${run?.p95LatencyMs ?? 0} ms`} />
                  <MetricCard label="처리량" value={`${formatNumber(run?.throughput ?? 0)} /s`} />
                </div>

                <div className="mt-5 grid gap-4 xl:grid-cols-3">
                  <ChartPanel title="성공 / 실패">
                    <ResponsiveContainer width="100%" height={220}>
                      <PieChart>
                        <Pie data={successFailureData} dataKey="value" nameKey="name" innerRadius={55} outerRadius={82}>
                          {successFailureData.map((entry) => (
                            <Cell key={entry.name} fill={entry.color} />
                          ))}
                        </Pie>
                        <Tooltip />
                      </PieChart>
                    </ResponsiveContainer>
                  </ChartPanel>
                  <ChartPanel title="지연 시간 백분위">
                    <ResponsiveContainer width="100%" height={220}>
                      <BarChart data={latencyData}>
                        <CartesianGrid stroke="#dedbd2" vertical={false} />
                        <XAxis dataKey="name" />
                        <YAxis />
                        <Tooltip />
                        <Bar dataKey="latency" fill="#0f9f84" radius={[4, 4, 0, 0]} />
                      </BarChart>
                    </ResponsiveContainer>
                  </ChartPanel>
                  <ChartPanel title="전략 비교">
                    {comparisonData.length === 0 ? (
                      <div className="flex h-[220px] items-center justify-center text-sm text-neutral-500">완료된 실행이 쌓이면 새로고침 후에도 비교 차트가 표시됩니다.</div>
                    ) : (
                      <ResponsiveContainer width="100%" height={220}>
                        <BarChart data={comparisonData}>
                          <CartesianGrid stroke="#dedbd2" vertical={false} />
                          <XAxis dataKey="strategy" />
                          <YAxis />
                          <Tooltip />
                          <Bar dataKey="duplicates" fill="#d9952f" radius={[4, 4, 0, 0]} />
                          <Bar dataKey="throughput" fill="#171717" radius={[4, 4, 0, 0]} />
                          <Bar dataKey="p95LatencyMs" fill="#0f9f84" radius={[4, 4, 0, 0]} />
                        </BarChart>
                      </ResponsiveContainer>
                    )}
                  </ChartPanel>
                </div>

                <div className="mt-5 overflow-hidden rounded-md border border-line">
                  <div className="border-b border-line bg-[#fbfbf9] px-4 py-3">
                    <div className="text-sm font-semibold">Strategy Comparison History</div>
                    <div className="mt-1 text-xs text-neutral-500">최근 완료 실행 기준 · 중복 예약 / 처리량 / p95 지연</div>
                  </div>
                  <div className="max-h-56 overflow-auto">
                    <table className="w-full min-w-[680px] border-collapse text-sm">
                      <thead className="sticky top-0 bg-white text-left text-xs text-neutral-500">
                        <tr>
                          <th className="border-b border-line px-4 py-3">Run</th>
                          <th className="border-b border-line px-4 py-3">전략</th>
                          <th className="border-b border-line px-4 py-3">중복 예약</th>
                          <th className="border-b border-line px-4 py-3">처리량</th>
                          <th className="border-b border-line px-4 py-3">p95</th>
                          <th className="border-b border-line px-4 py-3">결과</th>
                        </tr>
                      </thead>
                      <tbody>
                        {comparisonRuns.length === 0 ? (
                          <tr>
                            <td colSpan={6} className="px-4 py-6 text-center text-neutral-500">
                              완료된 실행 결과가 없습니다.
                            </td>
                          </tr>
                        ) : (
                          comparisonRuns.map((item) => (
                            <tr key={item.run.id} className="border-b border-line/70 last:border-b-0">
                              <td className="px-4 py-3 font-semibold">#{item.run.id}</td>
                              <td className="px-4 py-3">{item.strategy}</td>
                              <td className="px-4 py-3">{item.run.duplicateReservationCount}</td>
                              <td className="px-4 py-3">{formatNumber(item.run.throughput)} /s</td>
                              <td className="px-4 py-3">{item.run.p95LatencyMs} ms</td>
                              <td className="px-4 py-3">
                                <span className={`rounded-full px-2 py-1 text-xs font-semibold ${item.run.duplicateReservationCount > 0 ? "bg-coral/10 text-coral" : "bg-mint/10 text-mint"}`}>
                                  {item.run.duplicateReservationCount > 0 ? "위반 감지" : "무결성 유지"}
                                </span>
                              </td>
                            </tr>
                          ))
                        )}
                      </tbody>
                    </table>
                  </div>
                </div>

                <div className="mt-5 overflow-hidden rounded-md border border-line">
                  <div className="flex items-center justify-between border-b border-line bg-[#fbfbf9] px-4 py-3">
                    <div className="text-sm font-semibold">Reservation Attempts</div>
                    <div className="text-xs text-neutral-500">{reservations.length} rows</div>
                  </div>
                  <div className="max-h-80 overflow-auto">
                    <table className="w-full min-w-[760px] border-collapse text-sm">
                      <thead className="sticky top-0 bg-white text-left text-xs text-neutral-500">
                        <tr>
                          <th className="border-b border-line px-4 py-3">좌석</th>
                          <th className="border-b border-line px-4 py-3">사용자</th>
                          <th className="border-b border-line px-4 py-3">전략</th>
                          <th className="border-b border-line px-4 py-3">결과</th>
                          <th className="border-b border-line px-4 py-3">실패 사유</th>
                          <th className="border-b border-line px-4 py-3">지연</th>
                        </tr>
                      </thead>
                      <tbody>
                        {reservations.length === 0 ? (
                          <tr>
                            <td colSpan={6} className="px-4 py-8 text-center text-neutral-500">
                              실행 완료 후 예약 시도 내역이 표시됩니다.
                            </td>
                          </tr>
                        ) : (
                          reservations.map((reservation) => (
                            <tr key={reservation.id} className="border-b border-line/70 last:border-b-0">
                              <td className="px-4 py-3 font-semibold">{reservation.seatNumber}</td>
                              <td className="px-4 py-3 text-neutral-600">{reservation.userKey}</td>
                              <td className="px-4 py-3">{reservation.strategyType}</td>
                              <td className="px-4 py-3">
                                <span className={`rounded-full px-2 py-1 text-xs font-semibold ${reservation.success ? "bg-mint/10 text-mint" : "bg-coral/10 text-coral"}`}>
                                  {reservation.success ? "성공" : "실패"}
                                </span>
                              </td>
                              <td className="px-4 py-3 text-neutral-600">{reservation.failureReason ?? "-"}</td>
                              <td className="px-4 py-3">{reservation.latencyMs} ms</td>
                            </tr>
                          ))
                        )}
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
            </div>
          </section>

          <section id="케이스 스터디" className="rounded-md border border-line bg-white p-5 shadow-panel">
            <div className="mb-4 flex flex-col justify-between gap-3 sm:flex-row sm:items-center">
              <div>
                <h3 className="text-lg font-semibold">Case Study Export</h3>
                <p className="mt-1 text-sm text-neutral-500">포트폴리오에 붙여 넣기 좋은 Markdown 결과입니다.</p>
              </div>
              <button
                type="button"
                onClick={() => void navigator.clipboard.writeText(markdown)}
                disabled={!markdown}
                className="inline-flex h-10 items-center gap-2 rounded-md border border-line px-3 text-sm font-medium transition hover:border-ink disabled:cursor-not-allowed disabled:opacity-50"
              >
                <ClipboardCopy size={16} /> 복사
              </button>
            </div>
            <pre className="max-h-[460px] overflow-auto rounded-md bg-ink p-4 text-xs leading-6 text-white">
              {markdown || "실험이 완료되면 Markdown 케이스 스터디가 표시됩니다."}
            </pre>
          </section>

          <section className="rounded-md border border-line bg-white p-5 shadow-panel">
            <h3 className="text-lg font-semibold">최근 생성 실험</h3>
            <div className="mt-3 grid gap-2">
              {experiments.length === 0 ? (
                <div className="text-sm text-neutral-500">아직 생성된 실험이 없습니다.</div>
              ) : (
                experiments.slice(0, 5).map((experiment) => (
                  <div key={experiment.id} className="flex flex-col justify-between gap-2 border-b border-line py-3 last:border-b-0 sm:flex-row sm:items-center">
                    <div>
                      <div className="text-sm font-semibold">{experiment.name}</div>
                      <div className="mt-1 text-xs text-neutral-500">
                        {experiment.strategyType} · {experiment.concurrentUsers} users · {experiment.totalRequests} requests
                      </div>
                    </div>
                    <Lock size={16} className="text-neutral-400" />
                  </div>
                ))
              )}
            </div>
          </section>
        </section>
      </div>
    </main>
  );
}

function NumberField({ label, value, onChange }: { label: string; value: number; onChange: (value: string) => void }) {
  return (
    <label className="grid gap-1 text-sm font-medium">
      {label}
      <input
        type="number"
        min={0}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="h-10 min-w-0 rounded-md border border-line px-3 text-sm outline-none transition focus:border-mint"
      />
    </label>
  );
}

function StatusBadge({ status }: { status: RunSummary["status"] }) {
  const color = status === "COMPLETED" ? "bg-mint/10 text-mint" : status === "FAILED" ? "bg-coral/10 text-coral" : status === "RUNNING" ? "bg-amber/10 text-amber" : "bg-neutral-100 text-neutral-500";
  return (
    <span className={`inline-flex h-8 items-center gap-2 rounded-full px-3 text-xs font-semibold ${color}`}>
      {status === "RUNNING" && <Zap size={14} />}
      {status}
    </span>
  );
}

function MiniMetric({ label, value, tone }: { label: string; value: string | number; tone?: "mint" | "coral" | "amber" }) {
  const color = tone === "mint" ? "text-mint" : tone === "coral" ? "text-coral" : tone === "amber" ? "text-amber" : "text-ink";
  return (
    <div className="rounded-md border border-line bg-[#fbfbf9] p-3">
      <div className="text-xs text-neutral-500">{label}</div>
      <div className={`mt-1 text-xl font-semibold ${color}`}>{value}</div>
    </div>
  );
}

function MetricCard({ label, value, tone }: { label: string; value: string | number; tone?: "mint" | "coral" | "amber" }) {
  const color = tone === "mint" ? "text-mint" : tone === "coral" ? "text-coral" : tone === "amber" ? "text-amber" : "text-ink";
  return (
    <div className="rounded-md border border-line p-4">
      <div className="text-xs text-neutral-500">{label}</div>
      <div className={`mt-2 text-2xl font-semibold ${color}`}>{value}</div>
    </div>
  );
}

function ChartPanel({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="rounded-md border border-line p-4">
      <div className="mb-3 text-sm font-semibold">{title}</div>
      {children}
    </div>
  );
}

function SeatPressureVisual({ targetSeatCount, hotspotMode, running }: { targetSeatCount: number; hotspotMode: boolean; running: boolean }) {
  const seats = Array.from({ length: Math.min(36, Math.max(1, targetSeatCount)) }, (_, index) => index + 1);
  return (
    <div className="rounded-md border border-line bg-[#fbfbf9] p-4">
      <div className="mb-3 flex items-center justify-between text-sm">
        <span className="font-semibold">Seat Pressure Map</span>
        <span className="text-xs text-neutral-500">{hotspotMode ? "hotspot 집중" : "균등 분산"}</span>
      </div>
      <div className="grid grid-cols-9 gap-2 sm:grid-cols-12">
        {seats.map((seat) => {
          const hot = hotspotMode && seat <= Math.min(3, targetSeatCount);
          return (
            <div
              key={seat}
              className={`flex aspect-square items-center justify-center rounded-md border text-[11px] font-semibold transition ${
                hot ? "border-coral bg-coral/10 text-coral" : "border-line bg-white text-neutral-500"
              } ${running && hot ? "seat-pulse" : ""}`}
              title={`seat-${seat}`}
            >
              {seat}
            </div>
          );
        })}
      </div>
    </div>
  );
}

function formatNumber(value: number) {
  return new Intl.NumberFormat("ko-KR", {
    maximumFractionDigits: 1
  }).format(value);
}
