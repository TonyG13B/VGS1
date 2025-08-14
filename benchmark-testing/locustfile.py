import time
import random
import logging
from datetime import datetime
from locust import HttpUser, task, between, events


class VGSUser(HttpUser):
    wait_time = between(0.1, 0.5)  # Faster for stress testing
    connection_timeout = 10.0
    network_timeout = 10.0

    def on_start(self):
        """Initialize user session"""
        self.round_id = f"test-round-{random.randint(1000, 99999)}"
        self.transaction_count = 0
        self.player_id = f"player-{random.randint(1, 10000)}"
        self.start_time = time.time()

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

    # Added methods to test specific services
    def test_embedded_document(self, host):
        """Test the Embedded Document service"""
        try:
            with self.client.get(f"{host}/health", catch_response=True) as response:
                if response.status_code == 200:
                    response.success()
                else:
                    response.failure(f"Embedded Document health check failed: HTTP {response.status_code}")
        except Exception as e:
            self.environment.events.request_failure.fire(
                request_type="GET",
                name=f"{host}/health",
                response_time=0,
                exception=e,
            )

    def test_transaction_index(self, host):
        """Test the Transaction Index service"""
        try:
            with self.client.get(f"{host}/health", catch_response=True) as response:
                if response.status_code == 200:
                    response.success()
                else:
                    response.failure(f"Transaction Index health check failed: HTTP {response.status_code}")
        except Exception as e:
            self.environment.events.request_failure.fire(
                request_type="GET",
                name=f"{host}/health",
                response_time=0,
                exception=e,
            )

    # Method to simulate load on both services
    def simulate_service_load(self):
        # Test both services alternately for better load distribution
        if random.choice([True, False]):
            # Test Embedded Document service
            self.test_embedded_document("http://0.0.0.0:5100")
        else:
            # Test Transaction Index service
            self.test_transaction_index("http://0.0.0.0:5300")


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
        logging.error(f"Request failed: {name} - {exception}")
    elif response and response.status_code >= 400:
        logging.warning(f"Request error: {name} - {response.status_code}")

# Run with: locust --host=http://<vgs-ip>:5100 --users=500 --run-time=5m --headless --csv=locust_results