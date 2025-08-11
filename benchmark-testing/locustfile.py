import time
import random
from locust import HttpUser, task, between, events
from locust.exception import StopUser


class VGSUser(HttpUser):
    wait_time = between(0.1, 0.5)  # Faster for stress testing
    connection_timeout = 10.0
    network_timeout = 10.0

    def on_start(self):
        """Initialize user session"""
        self.round_id = f"test-round-{random.randint(1000, 99999)}"
        self.transaction_count = 0
        self.player_id = f"player-{random.randint(1, 10000)}"
        
        # Pre-warm connection
        try:
            self.client.get("/actuator/health", timeout=5)
        except Exception:
            pass  # Ignore pre-warm failures

    @task(3)  # Higher weight for transactions
    def post_transaction(self):
        """Simulate transaction processing"""
        try:
            payload = {
                "roundId": self.round_id,
                "transactionId": f"tx-{int(time.time() * 1000)}",
                "type": random.choice(["BET", "WIN", "LOSS", "BONUS"]),
                "amount": round(random.uniform(1.0, 100.0), 2),
                "playerId": f"player-{random.randint(1, 1000)}",
            }

            with self.client.post(
                "/api/atomic/atomic-transaction",
                json=payload,
                catch_response=True,
            ) as response:
                if response.status_code == 200:
                    result = response.json()
                    if result.get("success"):
                        response.success()
                        self.transaction_count += 1
                    else:
                        response.failure(
                            f"Transaction failed: {result.get('error', 'Unknown error')}"
                        )
                else:
                    response.failure(f"HTTP {response.status_code}")

        except Exception as e:
            self.environment.events.request_failure.fire(
                request_type="POST",
                name="/api/atomic/atomic-transaction",
                response_time=0,
                exception=e,
            )

    @task(1)  # Lower weight for reads
    def get_round(self):
        """Simulate round retrieval"""
        try:
            with self.client.get(
                f"/api/atomic/game-round/{self.round_id}",
                catch_response=True,
            ) as response:
                if response.status_code == 200:
                    response.success()
                elif response.status_code == 404:
                    # Round not found is acceptable for new rounds
                    response.success()
                else:
                    response.failure(f"HTTP {response.status_code}")

        except Exception as e:
            self.environment.events.request_failure.fire(
                request_type="GET",
                name="/api/atomic/game-round",
                response_time=0,
                exception=e,
            )

    @task(1)
    def health_check(self):
        """Periodic health checks"""
        try:
            with self.client.get("/actuator/health", catch_response=True) as response:
                if response.status_code == 200:
                    response.success()
                else:
                    response.failure(f"Health check failed: HTTP {response.status_code}")
        except Exception as e:
            self.environment.events.request_failure.fire(
                request_type="GET",
                name="/actuator/health",
                response_time=0,
                exception=e,
            )


# Custom event handlers for better monitoring
@events.request.add_listener
def my_request_handler(
    request_type,
    name,
    response_time,
    response_length,
    response,
    context,
    exception,
    start_time,
    url,
    **kwargs,
):
    if exception:
        print(f"Request failed: {name} - {exception}")
    elif response and response.status_code >= 400:
        print(f"Request error: {name} - HTTP {response.status_code}")

# Run with: locust --host=http://<vgs-ip>:5100 --users=500 --run-time=5m --headless --csv=locust_results