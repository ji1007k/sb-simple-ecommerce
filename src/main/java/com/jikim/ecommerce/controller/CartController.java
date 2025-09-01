package com.jikim.ecommerce.controller;

import com.jikim.ecommerce.dto.AddToCartRequest;
import com.jikim.ecommerce.dto.CartResponse;
import com.jikim.ecommerce.entity.Cart;
import com.jikim.ecommerce.service.CartService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CartController {
    private final CartService cartService;
    
    @GetMapping
    public ResponseEntity<CartResponse> getCart(HttpSession session) {
        String sessionId = session.getId();
        Cart cart = cartService.getOrCreateCart(sessionId);
        return ResponseEntity.ok(CartResponse.from(cart));
    }
    
    @PostMapping("/add")
    public ResponseEntity<CartResponse> addToCart(
            @Valid @RequestBody AddToCartRequest request,
            HttpSession session) {
        String sessionId = session.getId();
        Cart cart = cartService.addToCart(sessionId, request.getProductId(), request.getQuantity());
        return ResponseEntity.ok(CartResponse.from(cart));
    }
    
    @PutMapping("/update/{productId}")
    public ResponseEntity<CartResponse> updateCartItem(
            @PathVariable Long productId,
            @RequestParam Integer quantity,
            HttpSession session) {
        String sessionId = session.getId();
        Cart cart = cartService.updateCartItem(sessionId, productId, quantity);
        return ResponseEntity.ok(CartResponse.from(cart));
    }
    
    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearCart(HttpSession session) {
        String sessionId = session.getId();
        cartService.clearCart(sessionId);
        return ResponseEntity.noContent().build();
    }
}
