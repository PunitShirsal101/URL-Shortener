# URL Shortener

A production-ready URL shortener built with Spring Boot, Redis, Kafka, Prometheus, and Grafana.

## Architecture

```
[Web Client] --> [Spring Boot API] --> [Redis] (Persistence)
                     |
                     --> [Kafka] (Analytics)
                     |
                     --> [Prometheus] (Metrics)
                     |
                     --> [Grafana] (Dashboards)
```

- **Frontend**: Web client for shortening URLs
- **API**: Spring Boot REST API for shortening and redirecting
- **Persistence**: Redis for storing URL mappings
- **Analytics**: Kafka for processing click events
- **Metrics**: Prometheus for collecting metrics
- **Visualization**: Grafana for dashboards

## Features

- Shorten URLs with custom codes
- Redirect to original URLs
- Real-time analytics via WebSocket
- Kafka-based event processing
- Prometheus metrics
- Grafana dashboards
- Load testing with JMeter

## Running

```bash
docker-compose up
```

## API

See [API.md](docs/API.md) for detailed API documentation.

## Scaling

See [SCALING.md](docs/SCALING.md) for scaling strategies.

## Cost Analysis

See [COST_ANALYSIS.md](docs/COST_ANALYSIS.md) for AWS cost estimates.

# Load Testing

Use JMeter to simulate load.

## Test Plan

- Thread Group: 100 users, ramp-up 10s, loop forever
- HTTP Request: POST /api/shorten with JSON body {"originalUrl": "http://example.com"}
- HTTP Request: GET /{shortCode} from previous response

## Running

1. Install JMeter
2. Open jmeter-test.jmx
3. Run test
4. View results in listeners