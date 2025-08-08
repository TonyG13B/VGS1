import os
import time
import json
from typing import Any, Dict

from prometheus_client import start_http_server, Gauge


SUMMARY_PATH = os.environ.get("BENCHMARK_SUMMARY_PATH", "benchmark_summary.json")
PORT = int(os.environ.get("BENCHMARK_METRICS_PORT", "8080"))


gauges = {
    "vgs_bench_rps": Gauge("vgs_bench_rps", "Requests per second"),
    "vgs_bench_success_rate": Gauge("vgs_bench_success_rate", "Success rate (0..1)"),
    "vgs_bench_error_rate": Gauge("vgs_bench_error_rate", "Error rate (0..1)"),
    "vgs_bench_latency_p50_ms": Gauge("vgs_bench_latency_p50_ms", "P50 latency (ms)"),
    "vgs_bench_latency_p95_ms": Gauge("vgs_bench_latency_p95_ms", "P95 latency (ms)"),
    "vgs_bench_latency_p99_ms": Gauge("vgs_bench_latency_p99_ms", "P99 latency (ms)"),
    "vgs_bench_write_success_pct": Gauge("vgs_bench_write_success_pct", "Write success percent"),
    "vgs_bench_round_retrieval_p95_ms": Gauge("vgs_bench_round_retrieval_p95_ms", "Round retrieval P95 (ms)"),
    "vgs_bench_http_request_p95_ms": Gauge("vgs_bench_http_request_p95_ms", "HTTP request P95 (ms)"),
}


def load_summary() -> Dict[str, Any]:
    try:
        with open(SUMMARY_PATH, "r", encoding="utf-8") as f:
            return json.load(f)
    except Exception:
        return {}


def update_metrics(data: Dict[str, Any]) -> None:
    mapping = {
        "vgs_bench_rps": "rps",
        "vgs_bench_success_rate": "success_rate",
        "vgs_bench_error_rate": "error_rate",
        "vgs_bench_latency_p50_ms": "latency_p50_ms",
        "vgs_bench_latency_p95_ms": "latency_p95_ms",
        "vgs_bench_latency_p99_ms": "latency_p99_ms",
        "vgs_bench_write_success_pct": "write_success_pct",
        "vgs_bench_round_retrieval_p95_ms": "round_retrieval_p95_ms",
        "vgs_bench_http_request_p95_ms": "http_request_p95_ms",
    }
    for metric, key in mapping.items():
        value = float(data.get(key, 0.0) or 0.0)
        gauges[metric].set(value)


def main():
    start_http_server(PORT)
    while True:
        data = load_summary()
        update_metrics(data)
        time.sleep(5)


if __name__ == "__main__":
    main()


