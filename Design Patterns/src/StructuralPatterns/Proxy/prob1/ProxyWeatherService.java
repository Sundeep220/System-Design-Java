package StructuralPatterns.Proxy.prob1;

import java.util.HashMap;
import java.util.Map;

public class ProxyWeatherService implements WeatherService {
    private final Map<String, String> cache = new HashMap<>();
    private RealWeatherService realService;

   @Override
    public String getWeather(String city) {
       System.out.println("[LOG] Request received for city: " + city);
       if(realService == null) {
           realService = new RealWeatherService();  // lazy initialization
       }
        if (cache.containsKey(city)) {  // cache hit
            System.out.println("[LOG] Returning cached weather data for " + city);
            return cache.get(city);
        } else {
            String weather = realService.getWeather(city);
            cache.put(city, weather);
            System.out.println("[LOG] Request received for city: " + city+" caching result");
            return weather;
        }
    }
}
