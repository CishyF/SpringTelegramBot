package com.project.SpringBot.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.SpringBot.exceptions.NotFoundJsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Slf4j
@Service
public class GeographicalWeatherService {

    @Value("${weatherservice.api}")
    private String api;

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    public GeographicalWeatherService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    private JsonNode getPostsPlainJSON(String city) {
        String url = "https://api.openweathermap.org/data/2.5/weather?appid=" + api + "&q=" + city + "&units=metric&lang=ru";
        log.info("url: " + url);
        try {
            return mapper.readTree(this.restTemplate.getForObject(url, String.class));
        } catch (IOException e) {
            log.error("Error occurred: " + e.getMessage());
            throw new NotFoundJsonNode();
        }

    }

    public String getCoordinates(String city) {
        JsonNode coord = getPostsPlainJSON(city).get("coord");
        double latitude = Double.parseDouble(coord.get("lat").toString());
        double longitude = Double.parseDouble(coord.get("lon").toString());
        if (latitude <= 0 && longitude <= 0) {
            return "Город располагается на координатах: " + (latitude * -1) + " градусов западной долготы и " +
                    (longitude * -1) + " градусов южной широты";
        } else if (latitude > 0 && longitude <= 0) {
            return "Город располагается на координатах: " + latitude + " градусов западной долготы и " +
                    (longitude * -1) + " градусов северной широты";
        } else if (latitude <= 0 && longitude > 0) {
            return "Город располагается на координатах: " + (latitude * -1) + " градусов восточной долготы и " +
                    longitude + " градусов южной широты";
        } else {
            return "Город располагается на координатах: " + latitude + " градусов восточной долготы и " +
                    longitude + " градусов северной широты";
        }
    }

    public String getWeather(String city) {
        JsonNode node = getPostsPlainJSON(city);
        JsonNode weather = node.get("weather");
        JsonNode temperature = node.get("main");
        JsonNode wind = node.get("wind");
        String weatherMessage;
        weatherMessage = "На улице сейчас " + weather.findValues("description").get(0).asText() +
                         ", температура за окном " + temperature.get("temp") + "°C, ощущается как " +
                         temperature.get("feels_like") + "°C. " + "Максимальная температура на сегодня " +
                         temperature.get("temp_max") + "°C, минимальная — " + temperature.get("temp_min") +
                         "°C. Скорость ветра — " + wind.get("speed") + " м/с. Хорошего дня!";
        return weatherMessage;
    }
}
