package uz.app.strong_junior_test_task.controller;

import uz.app.strong_junior_test_task.dto.CreateOrderRequest;
import uz.app.strong_junior_test_task.dto.OrderResponse;
import uz.app.strong_junior_test_task.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@Tag(name = "Order API", description = "Buyurtmalarni boshqarish uchun endpointlar")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public OrderResponse createOrder(@RequestBody @Valid CreateOrderRequest request) {
        return orderService.createOrder(request);
    }

    @GetMapping
    public List<OrderResponse> getAllOrders() {
        return orderService.getAllOrders();
    }

    @GetMapping("/{id}")
    public OrderResponse getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id);
    }

    @GetMapping("/customer/{email}")
    public List<OrderResponse> getOrdersByCustomer(@PathVariable String email) {
        return orderService.getOrdersByCustomerEmail(email);
    }

    @PutMapping("/{id}/status")
    public OrderResponse changeStatus(@PathVariable Long id,
                                      @RequestParam String status) {
        return orderService.changeOrderStatus(id, status);
    }

    @DeleteMapping("/{id}")
    public void cancelOrder(@PathVariable Long id) {
        orderService.cancelOrder(id);
    }
}
