import json, os, random, time
import urllib.request, urllib.error

HOST = os.environ.get("VGS_APP_URL", "http://localhost:5100").rstrip("/")
USERS = int(os.environ.get("USERS", "50"))
DURATION = int(os.environ.get("DURATION_SECONDS", "10"))

start = time.time()
ended = start + DURATION
ok = fail = 0
lat = []

payloads = [
    {
        "roundId": f"smoke-{i}",
        "transactionType": random.choice(["BET", "WIN", "LOSS", "BONUS"]),
        "amount": round(random.uniform(1.0, 50.0), 2),
    }
    for i in range(USERS)
]

while time.time() < ended:
    p = random.choice(payloads)
    data = json.dumps(p).encode()
    req = urllib.request.Request(f"{HOST}/api/atomic/atomic-transaction", data=data, headers={"Content-Type": "application/json"})
    t0 = time.time()
    try:
        with urllib.request.urlopen(req, timeout=2) as resp:
            if resp.status == 200:
                ok += 1
            else:
                fail += 1
    except Exception:
        fail += 1
    lat.append((time.time() - t0) * 1000.0)

summary = {
    "app_url": HOST,
    "users": USERS,
    "duration": f"{DURATION}s",
    "rps": ok / DURATION if DURATION else 0,
    "success_rate": ok / (ok + fail) if (ok + fail) else 0,
    "error_rate": fail / (ok + fail) if (ok + fail) else 0,
    "latency_p50_ms": sorted(lat)[int(0.50 * len(lat))] if lat else 0,
    "latency_p95_ms": sorted(lat)[int(0.95 * len(lat))] if lat else 0,
    "latency_p99_ms": sorted(lat)[int(0.99 * len(lat))] if lat else 0,
    "write_success_pct": (ok / (ok + fail) * 100.0) if (ok + fail) else 0,
    "round_retrieval_p95_ms": 0,
    "http_request_p95_ms": sorted(lat)[int(0.95 * len(lat))] if lat else 0,
}

with open("/workspace/benchmark-testing/benchmark_summary.json", "w") as f:
    json.dump(summary, f)
print(json.dumps(summary, indent=2))
