package com.example.bowchat.product.repository;

import com.example.bowchat.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface productRepository extends JpaRepository<Product,Long> {
}
