package StructuralPatterns.Proxy.prob1;

public class Main {
    public static void main(String[] args) {
        WeatherService service = new ProxyWeatherService();

        System.out.println(service.getWeather("Delhi"));   // Should fetch from real service
        System.out.println(service.getWeather("Mumbai"));  // Should fetch from real service
        System.out.println(service.getWeather("Delhi"));   // Should return cached result
    }
}
