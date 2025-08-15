import time
import random
import logging
from datetime import datetime
from locust import HttpUser, task, between, events

class VGSUser(HttpUser):
    wait_time = between(0.1, 0.5)  # Faster for stress testing
    connection_timeout = 30.0  # Increased from 10.0
    network_timeout = 30.0     # Increased from 10.0

    def on_start(self):
        """Initialize user session"""
        self.round_id = f"test-round-{random.randint(1000, 99999)}"
        self.transaction_count = 0
        self.player_id = f"player-{random.randint(1, 10000)}"
        self.start_time = time.time()

        # Pre-warm connection with retry logic
        max_retries = 3
        for attempt in range(max_retries):
            try:
                response = self.client.get("/actuator/health", timeout=10)
                if response.status_code == 200:
                    break
            except Exception as e:
                if attempt == max_retries - 1:
                    self.environment.events.request_failure.fire(
                        request_type="GET",
                        name="/actuator/health",
                        response_time=0,
                        exception=e,
                    )
                time.sleep(1)  # Wait before retry

    @task(3)  # Higher weight for transactions
    def post_transaction(self):
        """Simulate transaction processing with improved error handling"""
        try:
            payload = {
                "roundId": self.round_id,
                "transactionId": f"tx-{int(time.time() * 1000)}",
                "type": random.choice(["BET", "WIN", "LOSS", "BONUS"]),
                "amount": round(random.uniform(1.0, 100.0), 2),
                "playerId": self.player_id,  # Use consistent player ID
            }

            # Add retry logic for failed requests
            max_retries = 3
            for attempt in range(max_retries):
                try:
                    with self.client.post(
                        "/api/atomic/atomic-transaction",
                        json=payload,
                        catch_response=True,
                        timeout=15  # Increased timeout
                    ) as response:
                        if response.status_code == 200:
                            result = response.json()
                            if result.get("success"):
                                response.success()
                                self.transaction_count += 1
                                break  # Success, exit retry loop
                            else:
                                error_msg = result.get('error', 'Unknown error')
                                if attempt == max_retries - 1:  # Last attempt
                                    response.failure(f"Transaction failed: {error_msg}")
                                else:
                                    time.sleep(0.1 * (2 ** attempt))  # Exponential backoff
                        elif response.status_code == 404:
                            # Round not found, create it first
                            self.create_round_if_needed()
                            if attempt == max_retries - 1:
                                response.failure(f"HTTP {response.status_code} - Round not found")
                        else:
                            if attempt == max_retries - 1:
                                response.failure(f"HTTP {response.status_code}")
                            else:
                                time.sleep(0.1 * (2 ** attempt))
                except Exception as e:
                    if attempt == max_retries - 1:
                        self.environment.events.request_failure.fire(
                            request_type="POST",
                            name="/api/atomic/atomic-transaction",
                            response_time=0,
                            exception=e,
                        )
                    else:
                        time.sleep(0.1 * (2 ** attempt))

        except Exception as e:
            self.environment.events.request_failure.fire(
                request_type="POST",
                name="/api/atomic/atomic-transaction",
                response_time=0,
                exception=e,
            )

    def create_round_if_needed(self):
        """Create a game round if it doesn't exist"""
        try:
            round_payload = {
                "roundId": self.round_id,
                "playerId": self.player_id,
                "initialBalance": 1000.0
            }
            
            response = self.client.post(
                "/api/game/create-round",
                json=round_payload,
                timeout=10
            )
            
            if response.status_code == 200:
                return True
        except Exception:
            pass
        return False

    @task(1)  # Lower weight for reads
    def get_round(self):
        """Simulate round retrieval with improved error handling"""
        try:
            with self.client.get(
                f"/api/atomic/game-round/{self.round_id}",
                catch_response=True,
                timeout=10
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
        """Periodic health checks with retry logic"""
        max_retries = 2
        for attempt in range(max_retries):
            try:
                with self.client.get("/actuator/health", catch_response=True, timeout=5) as response:
                    if response.status_code == 200:
                        response.success()
                        break
                    else:
                        if attempt == max_retries - 1:
                            response.failure(f"Health check failed: HTTP {response.status_code}")
                        else:
                            time.sleep(0.5)
            except Exception as e:
                if attempt == max_retries - 1:
                    self.environment.events.request_failure.fire(
                        request_type="GET",
                        name="/actuator/health",
                        response_time=0,
                        exception=e,
                    )
                else:
                    time.sleep(0.5)

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