package com.foodapp.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    public record ItemRequest(@NotNull Long menuItemId, @Min(1) int quantity) {}

    public record CreateOrderRequest(@NotBlank String customerName,
                                     @NotNull Long restaurantId,
                                     @NotEmpty List<@Valid ItemRequest> items) {}

    private final OrderRepository orders;
    private final RestaurantClient restaurants;

    OrderController(OrderRepository orders, RestaurantClient restaurants) {
        this.orders = orders;
        this.restaurants = restaurants;
    }

    // ---- Commands ----

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderEntity createOrder(@Valid @RequestBody CreateOrderRequest req) {
        OrderEntity order = new OrderEntity();
        order.setCustomerName(req.customerName());
        order.setRestaurantId(req.restaurantId());

        BigDecimal total = BigDecimal.ZERO;
        for (ItemRequest item : req.items()) {
            RestaurantClient.MenuItemDto menu = restaurants.findMenuItem(item.menuItemId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                            "Menu item " + item.menuItemId() + " does not exist"));
            if (!menu.restaurantId().equals(req.restaurantId())) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Menu item " + menu.id() + " does not belong to restaurant " + req.restaurantId());
            }
            if (!menu.available()) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Menu item " + menu.name() + " is not available");
            }
            OrderLine line = new OrderLine();
            line.setMenuItemId(menu.id());
            line.setName(menu.name());
            line.setUnitPrice(menu.price());
            line.setQuantity(item.quantity());
            order.getLines().add(line);
            total = total.add(menu.price().multiply(BigDecimal.valueOf(item.quantity())));
        }
        order.setTotal(total);
        return orders.save(order);
    }

    @PutMapping("/{id}/status")
    public OrderEntity updateStatus(@PathVariable Long id, @RequestParam OrderEntity.Status status) {
        OrderEntity order = find(id);
        if (order.getStatus() == OrderEntity.Status.CANCELLED || order.getStatus() == OrderEntity.Status.DELIVERED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order already " + order.getStatus());
        }
        order.setStatus(status);
        return orders.save(order);
    }

    @DeleteMapping("/{id}")
    public OrderEntity cancelOrder(@PathVariable Long id) {
        return updateStatus(id, OrderEntity.Status.CANCELLED);
    }

    // ---- Queries ----

    @GetMapping("/{id}")
    public OrderEntity getOrder(@PathVariable Long id) {
        return find(id);
    }

    @GetMapping
    public List<OrderEntity> listOrders(@RequestParam(required = false) String customerName) {
        return customerName == null ? orders.findAll() : orders.findByCustomerName(customerName);
    }

    private OrderEntity find(Long id) {
        return orders.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }
}
