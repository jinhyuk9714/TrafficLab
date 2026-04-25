import { readFileSync } from "node:fs";

const source = readFileSync(new URL("../src/app/page.tsx", import.meta.url), "utf8");

function fail(message) {
  console.error(message);
  process.exitCode = 1;
}

function presetBlock(name) {
  const start = source.indexOf(`name: "${name}"`);
  if (start === -1) {
    fail(`Missing preset: ${name}`);
    return "";
  }
  const nextPreset = source.indexOf("\n  {\n    name:", start + 1);
  return source.slice(start, nextPreset === -1 ? source.indexOf("\n];", start) : nextPreset);
}

function numericValue(block, key) {
  const match = block.match(new RegExp(`${key}:\\s*(\\d+)`));
  return match ? Number(match[1]) : Number.NaN;
}

for (const preset of ["Hot seat race", "Redis lock comparison", "Pessimistic lock comparison"]) {
  const block = presetBlock(preset);
  const totalRequests = numericValue(block, "totalRequests");
  if (!Number.isFinite(totalRequests) || totalRequests > 200) {
    fail(`${preset} should stay within 200 requests for the public Render demo, got ${totalRequests}`);
  }
}

if (!source.includes("COLD_START_NOTICE_MS")) {
  fail("The UI should expose a cold-start notice timing constant.");
}

if (!source.includes("서버를 깨우는 중")) {
  fail("The UI should explain Render cold start while the start request is pending.");
}

if (!source.includes("completedRequestsValue")) {
  fail("The live counters should not show totalRequests as completed before progress arrives.");
}
