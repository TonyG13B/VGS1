<content><![CDATA[
from locust import HttpUser, task, between
from couchbase.cluster import Cluster
# ... Connect to Capella
class KVUser(HttpUser):
    wait_time = between(1, 5)
    @task
    def test_embedded(self):
        # KV upsert/get, measure time, log to Sheets if criteria met
]]></content>
