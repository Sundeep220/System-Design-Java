Awesome! Here’s an **advanced-level Proxy Pattern assignment** for you to practice and apply everything you’ve learned.

---

## 💼 Advanced Proxy Pattern Problem: **API Gateway Proxy with Caching & Logging**

---

### 🧩 Problem Statement:

You're building an internal **API Gateway** for a microservices-based application. The gateway provides access to a **Real Weather Service** that fetches current weather data by calling an expensive external API (simulated).

To avoid unnecessary API hits and add logging, you must implement a **Proxy** that:

1. Logs every API access.
2. Caches the weather data for each city — if a request for the same city is made again within the same session, the proxy should return the cached result instead of calling the real service.

---

## 🔧 Requirements:

### ✅ Interface:

```java
public interface WeatherService {
    String getWeather(String city);
}
```

---

### ✅ RealWeatherService (Simulates External API Call):

```java
public class RealWeatherService implements WeatherService {
    @Override
    public String getWeather(String city) {
        System.out.println("Fetching weather data from external API for " + city + "...");
        // Simulated delay
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "Sunny 30°C in " + city;
    }
}
```

---

### ✅ ProxyWeatherService (Your Task):

* Should implement `WeatherService`.
* Should internally use a `Map<String, String>` for caching.
* Should log every request.
* Should delegate to `RealWeatherService` only if the data is not cached.

---

## 🧪 Client Example:

```java
public class Main {
    public static void main(String[] args) {
        WeatherService service = new ProxyWeatherService();

        System.out.println(service.getWeather("Delhi"));   // Should fetch from real service
        System.out.println(service.getWeather("Mumbai"));  // Should fetch from real service
        System.out.println(service.getWeather("Delhi"));   // Should return cached result
    }
}
```

---

### 🧾 Expected Output (Sample):

```
[LOG] Request received for city: Delhi
Fetching weather data from external API for Delhi...
Sunny 30°C in Delhi

[LOG] Request received for city: Mumbai
Fetching weather data from external API for Mumbai...
Sunny 30°C in Mumbai

[LOG] Request received for city: Delhi
[LOG] Returning cached weather data for Delhi
Sunny 30°C in Delhi
```

---

## 🔨 Task for You:

Implement the `ProxyWeatherService` class that:

* Caches weather results.
* Logs every access.
* Delegates to the real service only when needed.

---