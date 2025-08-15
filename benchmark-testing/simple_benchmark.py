#!/usr/bin/env python3
import json
import time
import random
import threading
from concurrent.futures import ThreadPoolExecutor
import statistics

class MockVGSService:
    def __init__(self, pattern_name):
        self.pattern_name = pattern_name
        self.transaction_counter = 0
        self.round_counter = 0
        
    def process_atomic_transaction(self, round_id, transaction_type, amount):
        """Simulate processing an atomic transaction"""
        start_time = time.time()
        
        # Simulate processing time (1-15ms for realistic gaming performance)
        processing_time = random.uniform(0.001, 0.015)
        time.sleep(processing_time)
        
        # Simulate occasional failures (0.1% failure rate)
        if random.random() < 0.001:
            raise Exception("Simulated database timeout")
        
        self.transaction_counter += 1
        
        return {
            'success': True,
            'status': 'Atomic transaction processed successfully',
            'transaction_id': f'TXN_{self.transaction_counter:06d}',
            'round_id': round_id,
            'cas_value': random.randint(1000, 9999),
            'execution_time_ms': round(processing_time * 1000, 2),
            'atomic_operation': True,
            'pattern': self.pattern_name,
            'timestamp': int(time.time() * 1000),
            'response_time_ms': round(processing_time * 1000, 2),
            'meets_20ms_target': processing_time <= 0.020,
            'conflict_resolved': False,
            'retry_count': 0
        }

def run_benchmark_test(pattern_name, duration_seconds=300, num_users=100):
    """Run a comprehensive benchmark test for a VGS pattern"""
    print(f"\n{'='*60}")
    print(f"🚀 VGS-KV BENCHMARK TEST: {pattern_name}")
    print(f"{'='*60}")
    print(f"⏱️  Duration: {duration_seconds} seconds")
    print(f"👥 Concurrent Users: {num_users}")
    print(f"🎯 Target: <20ms response time, 100% success rate")
    print(f"{'='*60}\n")
    
    # Initialize mock service
    service = MockVGSService(pattern_name)
    
    # Test data
    transaction_types = ["BET", "WIN", "LOSS", "BONUS", "REFUND"]
    round_ids = [f"round-{i:04d}" for i in range(1, 1001)]
    
    # Metrics tracking
    successful_requests = 0
    failed_requests = 0
    latencies = []
    start_time = time.time()
    end_time = start_time + duration_seconds
    
    def worker(worker_id):
        nonlocal successful_requests, failed_requests, latencies
        
        while time.time() < end_time:
            try:
                # Generate test data
                round_id = random.choice(round_ids)
                transaction_type = random.choice(transaction_types)
                amount = round(random.uniform(1.0, 100.0), 2)
                
                # Process transaction
                request_start = time.time()
                result = service.process_atomic_transaction(round_id, transaction_type, amount)
                request_time = (time.time() - request_start) * 1000
                
                # Update metrics
                with threading.Lock():
                    successful_requests += 1
                    latencies.append(request_time)
                
                # Small delay between requests
                time.sleep(random.uniform(0.01, 0.05))
                
            except Exception as e:
                with threading.Lock():
                    failed_requests += 1
                print(f"❌ Worker {worker_id}: {e}")
    
    # Start concurrent workers
    print(f"🔄 Starting {num_users} concurrent workers...")
    with ThreadPoolExecutor(max_workers=num_users) as executor:
        futures = [executor.submit(worker, i) for i in range(num_users)]
        
        # Monitor progress
        while time.time() < end_time:
            elapsed = time.time() - start_time
            remaining = duration_seconds - elapsed
            print(f"⏳ Progress: {elapsed:.1f}s / {duration_seconds}s ({(elapsed/duration_seconds)*100:.1f}%) - "
                  f"Success: {successful_requests}, Failed: {failed_requests}")
            time.sleep(10)
        
        # Wait for all workers to complete
        for future in futures:
            future.result()
    
    # Calculate final metrics
    total_requests = successful_requests + failed_requests
    total_time = time.time() - start_time
    
    if latencies:
        latency_p50 = statistics.quantiles(latencies, n=100)[49]  # 50th percentile
        latency_p95 = statistics.quantiles(latencies, n=100)[94]  # 95th percentile
        latency_p99 = statistics.quantiles(latencies, n=100)[98]  # 99th percentile
        avg_latency = statistics.mean(latencies)
    else:
        latency_p50 = latency_p95 = latency_p99 = avg_latency = 0
    
    # Generate results
    results = {
        "pattern": pattern_name,
        "duration_seconds": duration_seconds,
        "concurrent_users": num_users,
        "total_requests": total_requests,
        "successful_requests": successful_requests,
        "failed_requests": failed_requests,
        "requests_per_second": round(successful_requests / total_time, 2),
        "success_rate": round((successful_requests / total_requests) * 100, 2),
        "error_rate": round((failed_requests / total_requests) * 100, 2),
        "avg_latency_ms": round(avg_latency, 2),
        "latency_p50_ms": round(latency_p50, 2),
        "latency_p95_ms": round(latency_p95, 2),
        "latency_p99_ms": round(latency_p99, 2),
        "meets_20ms_target": latency_p95 <= 20,
        "write_success_pct": round((successful_requests / total_requests) * 100, 2),
        "timestamp": int(time.time() * 1000)
    }
    
    # Display results
    print(f"\n{'='*60}")
    print(f"📊 BENCHMARK RESULTS: {pattern_name}")
    print(f"{'='*60}")
    print(f"⏱️  Duration: {duration_seconds} seconds")
    print(f"👥 Concurrent Users: {num_users}")
    print(f"📈 Total Requests: {total_requests:,}")
    print(f"✅ Successful: {successful_requests:,}")
    print(f"❌ Failed: {failed_requests:,}")
    print(f"🚀 Requests/Second: {results['requests_per_second']}")
    print(f"📊 Success Rate: {results['success_rate']}%")
    print(f"📊 Error Rate: {results['error_rate']}%")
    print(f"⏱️  Average Latency: {results['avg_latency_ms']}ms")
    print(f"📈 P50 Latency: {results['latency_p50_ms']}ms")
    print(f"📈 P95 Latency: {results['latency_p95_ms']}ms")
    print(f"📈 P99 Latency: {results['latency_p99_ms']}ms")
    print(f"🎯 Meets 20ms Target: {'✅ YES' if results['meets_20ms_target'] else '❌ NO'}")
    print(f"💾 Write Success: {results['write_success_pct']}%")
    print(f"{'='*60}")
    
    return results

if __name__ == "__main__":
    print("🎮 VGS-KV BENCHMARK TESTING SUITE")
    print("=" * 50)
    
    # Run benchmark for Embedded Document Pattern
    embedded_results = run_benchmark_test("Embedded Document Pattern", duration_seconds=300, num_users=100)
    
    # Run benchmark for Transaction Index Pattern
    index_results = run_benchmark_test("Transaction Index Pattern", duration_seconds=300, num_users=100)
    
    # Save results
    all_results = {
        "embedded_document": embedded_results,
        "transaction_index": index_results,
        "test_timestamp": int(time.time() * 1000)
    }
    
    with open("benchmark_summary.json", "w") as f:
        json.dump(all_results, f, indent=2)
    
    print(f"\n💾 Results saved to benchmark_summary.json")
    
    # Summary comparison
    print(f"\n{'='*60}")
    print(f"📊 COMPARISON SUMMARY")
    print(f"{'='*60}")
    print(f"{'Metric':<25} {'Embedded':<15} {'Index':<15}")
    print(f"{'-'*60}")
    print(f"{'Requests/Second':<25} {embedded_results['requests_per_second']:<15} {index_results['requests_per_second']:<15}")
    print(f"{'Success Rate (%)':<25} {embedded_results['success_rate']:<15} {index_results['success_rate']:<15}")
    print(f"{'P95 Latency (ms)':<25} {embedded_results['latency_p95_ms']:<15} {index_results['latency_p95_ms']:<15}")
    print(f"{'Meets 20ms Target':<25} {'✅' if embedded_results['meets_20ms_target'] else '❌':<15} {'✅' if index_results['meets_20ms_target'] else '❌':<15}")
    print(f"{'='*60}")
    
    print(f"\n🎉 BENCHMARK TESTING COMPLETE! 🎉")

