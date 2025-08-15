#!/usr/bin/env python3
import http.server
import socketserver
import json
import time
import random
import sys

class MockVGSServer(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == '/health':
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            response = {'status': 'UP', 'pattern': 'Mock VGS Server'}
            self.wfile.write(json.dumps(response).encode())
        else:
            self.send_response(404)
            self.end_headers()
    
    def do_POST(self):
        if self.path == '/api/atomic/atomic-transaction':
            # Read the request body
            content_length = int(self.headers['Content-Length'])
            post_data = self.rfile.read(content_length)
            request_data = json.loads(post_data.decode('utf-8'))
            
            # Simulate processing time (1-5ms)
            time.sleep(random.uniform(0.001, 0.005))
            
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            
            response = {
                'success': True,
                'status': 'Atomic transaction processed successfully',
                'transaction_id': 'TXN_' + str(random.randint(10000, 99999)),
                'round_id': request_data.get('roundId', 'UNKNOWN'),
                'cas_value': random.randint(1000, 9999),
                'execution_time_ms': random.randint(1, 15),
                'atomic_operation': True,
                'pattern': 'Mock VGS Server',
                'timestamp': int(time.time() * 1000),
                'response_time_ms': random.randint(1, 15),
                'meets_20ms_target': True,
                'conflict_resolved': False,
                'retry_count': 0
            }
            self.wfile.write(json.dumps(response).encode())
        else:
            self.send_response(404)
            self.end_headers()
    
    def log_message(self, format, *args):
        pass

if __name__ == "__main__":
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 5100
    print(f"Starting Mock VGS Server on port {port}")
    
    with socketserver.TCPServer(("", port), MockVGSServer) as httpd:
        print(f"Mock VGS Server running on port {port}")
        httpd.serve_forever()

