package com.foodapp.order;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Synchronous REST client for restaurant-service.
 * The base URL comes from config so it resolves to the Kubernetes Service DNS name in the cluster.
 */
@Component
public class RestaurantClient {

    public record MenuItemDto(Long id, Long restaurantId, String name, BigDecimal price, boolean available) {}

    private final RestClient http;

    public RestaurantClient(@Value("${restaurant.service.url:http://localhost:8081}") String baseUrl) {
        org.springframework.http.client.SimpleClientHttpRequestFactory f =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        f.setConnectTimeout(2000);
        f.setReadTimeout(3000);
        this.http = RestClient.builder().baseUrl(baseUrl).requestFactory(f).build();
    }

    public Optional<MenuItemDto> findMenuItem(Long id) {
        try {
            return Optional.ofNullable(http.get().uri("/api/menu-items/{id}", id)
                    .retrieve().body(MenuItemDto.class));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "restaurant-service unavailable");
        }
    }
}
