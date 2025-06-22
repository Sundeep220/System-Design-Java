package StructuralPatterns.Proxy.prob1;

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

