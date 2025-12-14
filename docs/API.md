# API Documentation

## Shorten URL

**POST** `/api/shorten`

Request Body:
```json
{
  "originalUrl": "http://example.com",
  "customShortCode": "abc",
  "ttlSeconds": 3600
}
```

Response:
```json
{
  "shortUrl": "http://localhost:8080/abc",
  "originalUrl": "http://example.com",
  "shortCode": "abc"
}
```

## Bulk Shorten URLs

**POST** `/api/shorten/bulk`

Request Body:
```json
{
  "requests": [
    {
      "originalUrl": "http://example1.com",
      "customShortCode": "abc"
    },
    {
      "originalUrl": "http://example2.com"
    }
  ]
}
```

Response:
```json
{
  "responses": [
    {
      "shortUrl": "http://localhost:8080/abc",
      "originalUrl": "http://example1.com",
      "shortCode": "abc"
    },
    {
      "shortUrl": "http://localhost:8080/def123",
      "originalUrl": "http://example2.com",
      "shortCode": "def123"
    }
  ]
}
```

## Redirect

**GET** `/{shortCode}`

Redirects to the original URL.

## Get Click Count

**GET** `/api/clicks/{shortCode}`

Response:
```json
{
  "clickCount": 10
}
```

## WebSocket Analytics

Connect to `/ws` for real-time click updates.

Topic: `/topic/analytics`
