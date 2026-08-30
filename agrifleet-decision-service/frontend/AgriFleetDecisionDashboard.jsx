import { useState, useCallback } from "react";

/**
 * AgriFleet - Task 4: Intelligent Decision Module dashboard.
 *
 * Talks to the Spring Boot decision-service endpoints:
 *   POST /api/decision/rank/{bookingId}
 *   POST /api/decision/risk/{bookingId}
 *
 * Drop this into your team's React app and pass a real bookingId prop
 * (from whatever booking-selection screen the group builds). Update
 * API_BASE_URL to match wherever the service is actually deployed.
 */

const API_BASE_URL = "http://localhost:8084/api/decision";

const CRITERIA = [
  { key: "costWeight", label: "Rental Cost", hint: "Lower hourly rate ranked higher" },
  { key: "distanceWeight", label: "Proximity", hint: "Closer machinery ranked higher" },
  { key: "horsepowerWeight", label: "Horsepower", hint: "More powerful machinery ranked higher" },
  { key: "ratingWeight", label: "Operator Rating", hint: "Higher-rated machinery ranked higher" },
];

const RISK_STYLES = {
  LOW_RISK: { bg: "#E9F2E4", fg: "#3F6B2E", border: "#A9C99A", label: "Low risk" },
  MODERATE_RISK: { bg: "#FBF0DD", fg: "#8A5A16", border: "#E3C081", label: "Moderate risk" },
  CRITICAL_DELAY: { bg: "#FBE7E4", fg: "#9A3324", border: "#E3A99A", label: "Critical delay risk" },
};

function normalizedPercent(weights, key) {
  const sum = Object.values(weights).reduce((a, b) => a + b, 0);
  if (sum <= 0) return 25;
  return Math.round((weights[key] / sum) * 100);
}

export default function AgriFleetDecisionDashboard({ bookingId = 1 }) {
  const [weights, setWeights] = useState({
    costWeight: 0.35,
    distanceWeight: 0.25,
    horsepowerWeight: 0.2,
    ratingWeight: 0.2,
  });
  const [ranking, setRanking] = useState(null);
  const [risk, setRisk] = useState(null);
  const [loadingRank, setLoadingRank] = useState(false);
  const [loadingRisk, setLoadingRisk] = useState(false);
  const [error, setError] = useState(null);

  const updateWeight = (key, value) => {
    setWeights((prev) => ({ ...prev, [key]: Number(value) }));
  };

  const runRanking = useCallback(async () => {
    setLoadingRank(true);
    setError(null);
    try {
      const res = await fetch(`${API_BASE_URL}/rank/${bookingId}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(weights),
      });
      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new Error(body.error || `Request failed (${res.status})`);
      }
      setRanking(await res.json());
    } catch (e) {
      setError(e.message);
      setRanking(null);
    } finally {
      setLoadingRank(false);
    }
  }, [bookingId, weights]);

  const runRiskCheck = useCallback(async () => {
    setLoadingRisk(true);
    setError(null);
    try {
      const res = await fetch(`${API_BASE_URL}/risk/${bookingId}?rainProbability=0.4&breakdownHistory=0`, {
        method: "POST",
      });
      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new Error(body.error || `Request failed (${res.status})`);
      }
      setRisk(await res.json());
    } catch (e) {
      setError(e.message);
      setRisk(null);
    } finally {
      setLoadingRisk(false);
    }
  }, [bookingId]);

  return (
    <div style={styles.page}>
      <style>{`
        @keyframes af-rise { from { opacity: 0; transform: translateY(6px); } to { opacity: 1; transform: translateY(0); } }
        .af-row { animation: af-rise 0.25s ease-out both; }
        input[type="range"].af-slider { -webkit-appearance: none; height: 6px; border-radius: 999px; background: #DCD3BF; }
        input[type="range"].af-slider::-webkit-slider-thumb {
          -webkit-appearance: none; width: 16px; height: 16px; border-radius: 50%;
          background: #4C6B3E; cursor: pointer; border: 2px solid #F6F2E8;
          box-shadow: 0 0 0 1px #4C6B3E;
        }
      `}</style>

      <header style={styles.header}>
        <div style={styles.eyebrow}>TASK 4 · INTELLIGENT DECISION MODULE</div>
        <h1 style={styles.title}>Machinery match for booking #{bookingId}</h1>
        <p style={styles.subtitle}>
          Rank available machinery with TOPSIS using your own priorities, and check the
          decision-tree harvest delay forecast before you confirm.
        </p>
      </header>

      <section style={styles.panel}>
        <h2 style={styles.panelTitle}>Set your priorities</h2>
        <div style={styles.slidersGrid}>
          {CRITERIA.map((c) => (
            <div key={c.key} style={styles.sliderBlock}>
              <div style={styles.sliderLabelRow}>
                <span style={styles.sliderLabel}>{c.label}</span>
                <span style={styles.sliderPercent}>{normalizedPercent(weights, c.key)}%</span>
              </div>
              <input
                className="af-slider"
                type="range"
                min="0"
                max="1"
                step="0.05"
                value={weights[c.key]}
                onChange={(e) => updateWeight(c.key, e.target.value)}
                style={{ width: "100%" }}
              />
              <span style={styles.sliderHint}>{c.hint}</span>
            </div>
          ))}
        </div>

        <div style={styles.actionsRow}>
          <button style={styles.primaryButton} onClick={runRanking} disabled={loadingRank}>
            {loadingRank ? "Ranking…" : "Rank available machinery"}
          </button>
          <button style={styles.secondaryButton} onClick={runRiskCheck} disabled={loadingRisk}>
            {loadingRisk ? "Checking…" : "Check delay risk"}
          </button>
        </div>

        {error && <div style={styles.errorBox}>{error}</div>}
      </section>

      {risk && (
        <section style={styles.panel}>
          <h2 style={styles.panelTitle}>Harvest delay forecast</h2>
          <RiskBadge risk={risk} />
        </section>
      )}

      {ranking && (
        <section style={styles.panel}>
          <div style={styles.resultsHeaderRow}>
            <h2 style={styles.panelTitle}>Ranked machinery</h2>
            <span style={styles.timing}>computed in {ranking.executionTimeMs.toFixed(2)} ms</span>
          </div>
          <div style={styles.tableWrap}>
            <table style={styles.table}>
              <thead>
                <tr>
                  <th style={styles.th}>Rank</th>
                  <th style={styles.th}>Vehicle</th>
                  <th style={styles.th}>Type</th>
                  <th style={styles.th}>Rate/hr</th>
                  <th style={styles.th}>Distance</th>
                  <th style={styles.th}>HP</th>
                  <th style={styles.th}>Rating</th>
                  <th style={styles.th}>Closeness C</th>
                </tr>
              </thead>
              <tbody>
                {ranking.rankedCandidates.map((c, i) => (
                  <tr key={c.vehicleId} className="af-row" style={{ animationDelay: `${i * 40}ms` }}>
                    <td style={{ ...styles.td, ...styles.rankCell }}>
                      {c.finalRank === 1 ? <span style={styles.topBadge}>#1</span> : c.finalRank}
                    </td>
                    <td style={styles.td}>Vehicle {c.vehicleId}</td>
                    <td style={styles.td}>{c.vehicleType.replaceAll("_", " ")}</td>
                    <td style={styles.td}>${c.hourlyRate.toFixed(2)}</td>
                    <td style={styles.td}>{c.distanceKm.toFixed(2)} km</td>
                    <td style={styles.td}>{c.horsepower}</td>
                    <td style={styles.td}>{c.ratingScore.toFixed(2)} ★</td>
                    <td style={styles.td}>
                      <div style={styles.closenessBarTrack}>
                        <div style={{ ...styles.closenessBarFill, width: `${c.relativeClosenessC * 100}%` }} />
                      </div>
                      <span style={styles.closenessValue}>{c.relativeClosenessC.toFixed(3)}</span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}
    </div>
  );
}

function RiskBadge({ risk }) {
  const style = RISK_STYLES[risk.predictedRiskTier] || RISK_STYLES.MODERATE_RISK;
  return (
    <div style={{ ...styles.riskBadge, background: style.bg, color: style.fg, borderColor: style.border }}>
      <div style={styles.riskLabel}>{style.label}</div>
      <div style={styles.riskDetail}>
        Confidence {(risk.confidenceScore * 100).toFixed(0)}% · {risk.fieldAcres} acres ·{" "}
        {(risk.rainProbability * 100).toFixed(0)}% rain chance
      </div>
    </div>
  );
}

const styles = {
  page: {
    fontFamily: "'Iowan Old Style', 'Palatino Linotype', Georgia, serif",
    background: "#F6F2E8",
    color: "#2B2A25",
    padding: "32px 24px",
    maxWidth: 900,
    margin: "0 auto",
  },
  header: { marginBottom: 28 },
  eyebrow: {
    fontFamily: "'Courier New', monospace",
    fontSize: 12,
    letterSpacing: "0.12em",
    color: "#6B7A4F",
    marginBottom: 8,
  },
  title: { fontSize: 28, margin: "0 0 8px", fontWeight: 600, color: "#2B2A25" },
  subtitle: { fontSize: 15, lineHeight: 1.5, color: "#5C5A50", margin: 0, maxWidth: 560 },
  panel: {
    background: "#FFFDF7",
    border: "1px solid #E2DAC6",
    borderRadius: 10,
    padding: 24,
    marginBottom: 20,
  },
  panelTitle: { fontSize: 17, fontWeight: 600, margin: "0 0 16px", color: "#2B2A25" },
  slidersGrid: {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))",
    gap: 20,
  },
  sliderBlock: { display: "flex", flexDirection: "column", gap: 6 },
  sliderLabelRow: { display: "flex", justifyContent: "space-between", alignItems: "baseline" },
  sliderLabel: { fontSize: 14, fontWeight: 600 },
  sliderPercent: { fontFamily: "'Courier New', monospace", fontSize: 13, color: "#4C6B3E", fontWeight: 700 },
  sliderHint: { fontSize: 12, color: "#8A8776" },
  actionsRow: { display: "flex", gap: 12, marginTop: 24 },
  primaryButton: {
    background: "#4C6B3E",
    color: "#FFFDF7",
    border: "none",
    borderRadius: 6,
    padding: "10px 18px",
    fontSize: 14,
    fontWeight: 600,
    cursor: "pointer",
    fontFamily: "inherit",
  },
  secondaryButton: {
    background: "transparent",
    color: "#4C6B3E",
    border: "1px solid #4C6B3E",
    borderRadius: 6,
    padding: "10px 18px",
    fontSize: 14,
    fontWeight: 600,
    cursor: "pointer",
    fontFamily: "inherit",
  },
  errorBox: {
    marginTop: 16,
    background: "#FBE7E4",
    border: "1px solid #E3A99A",
    color: "#9A3324",
    borderRadius: 6,
    padding: "10px 14px",
    fontSize: 13,
  },
  riskBadge: { border: "1px solid", borderRadius: 8, padding: "14px 18px" },
  riskLabel: { fontWeight: 700, fontSize: 15, marginBottom: 4 },
  riskDetail: { fontSize: 13, opacity: 0.85 },
  resultsHeaderRow: { display: "flex", justifyContent: "space-between", alignItems: "baseline" },
  timing: { fontFamily: "'Courier New', monospace", fontSize: 12, color: "#8A8776" },
  tableWrap: { overflowX: "auto" },
  table: { width: "100%", borderCollapse: "collapse", fontSize: 13 },
  th: {
    textAlign: "left",
    padding: "8px 10px",
    borderBottom: "2px solid #E2DAC6",
    color: "#6B7A4F",
    fontSize: 11,
    letterSpacing: "0.06em",
    textTransform: "uppercase",
  },
  td: { padding: "10px 10px", borderBottom: "1px solid #EFEADB", verticalAlign: "middle" },
  rankCell: { fontWeight: 700, width: 48 },
  topBadge: {
    background: "#4C6B3E",
    color: "#FFFDF7",
    borderRadius: 999,
    padding: "2px 8px",
    fontSize: 12,
    fontWeight: 700,
  },
  closenessBarTrack: { width: 90, height: 6, background: "#EFEADB", borderRadius: 999, marginBottom: 4 },
  closenessBarFill: { height: "100%", background: "#4C6B3E", borderRadius: 999 },
  closenessValue: { fontFamily: "'Courier New', monospace", fontSize: 11, color: "#8A8776" },
};
