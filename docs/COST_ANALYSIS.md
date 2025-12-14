# Cost Analysis

## AWS Cost Estimates

Assuming 1M requests per month, moderate traffic.

### EC2 Instances
- Application: t3.medium (2 vCPU, 4GB RAM) - $32/month
- Prometheus/Grafana: t3.micro (1 vCPU, 1GB RAM) - $8/month

### Managed Services
- ElastiCache (Redis): cache.t3.micro - $15/month
- MSK (Kafka): 2 brokers, kafka.t3.small - $100/month
- ELB (Load Balancer): $20/month

### Storage
- EBS for logs: 20GB gp3 - $2/month

### Total Monthly Cost: ~$177

### Breakdown
- Compute: $40
- Managed Services: $135
- Storage: $2

### Scaling Costs
- For 10M requests: Double EC2 instances, add more brokers - ~$400/month
- For 100M requests: Use larger instances, more brokers - ~$1000/month

### Optimizations
- Use Spot instances for non-critical workloads.
- Reserve instances for long-term savings.
- Monitor and auto-scale to reduce costs.
