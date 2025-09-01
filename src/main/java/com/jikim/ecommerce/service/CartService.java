package com.jikim.ecommerce.service;

import com.jikim.ecommerce.entity.Cart;
import com.jikim.ecommerce.entity.CartItem;
import com.jikim.ecommerce.entity.Product;
import com.jikim.ecommerce.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {
    private final CartRepository cartRepository;
    private final ProductService productService;
    
    public Cart getOrCreateCart(String sessionId) {
        return cartRepository.findBySessionId(sessionId)
                .orElseGet(() -> {
                    Cart cart = Cart.builder()
                            .sessionId(sessionId)
                            .build();
                    return cartRepository.save(cart);
                });
    }
    
    public Cart addToCart(String sessionId, Long productId, Integer quantity) {
        Cart cart = getOrCreateCart(sessionId);
        Product product = productService.getProductById(productId);
        
        // Check if product already in cart
        CartItem existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .orElse(null);
        
        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
        } else {
            CartItem cartItem = CartItem.builder()
                    .product(product)
                    .quantity(quantity)
                    .build();
            cart.addItem(cartItem);
        }
        
        return cartRepository.save(cart);
    }
    
    public Cart updateCartItem(String sessionId, Long productId, Integer quantity) {
        Cart cart = getOrCreateCart(sessionId);
        
        cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .ifPresent(item -> {
                    if (quantity <= 0) {
                        cart.removeItem(item);
                    } else {
                        item.setQuantity(quantity);
                    }
                });
        
        return cartRepository.save(cart);
    }
    
    public void clearCart(String sessionId) {
        cartRepository.findBySessionId(sessionId)
                .ifPresent(cart -> {
                    cart.getItems().clear();
                    cartRepository.save(cart);
                });
    }
}
