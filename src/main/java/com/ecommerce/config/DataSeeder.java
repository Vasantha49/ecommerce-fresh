package com.ecommerce.config;

import com.ecommerce.entity.Product;
import com.ecommerce.entity.User;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataSeeder {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner seedData() {
        return args -> {
            if (productRepository.count() > 0) return;
            log.info("Seeding initial data...");

            if (!userRepository.existsByEmail("admin@ecommerce.de")) {
                userRepository.save(User.builder()
                        .firstName("Admin").lastName("User")
                        .email("admin@ecommerce.de")
                        .password(passwordEncoder.encode("Admin1234!"))
                        .phone("+49 30 12345678")
                        .roles(Set.of("USER", "ADMIN")).active(true).build());
            }
            if (!userRepository.existsByEmail("user@ecommerce.de")) {
                userRepository.save(User.builder()
                        .firstName("Max").lastName("Mustermann")
                        .email("user@ecommerce.de")
                        .password(passwordEncoder.encode("User1234!"))
                        .phone("+49 89 98765432")
                        .roles(Set.of("USER")).active(true).build());
            }

            productRepository.saveAll(List.of(
                Product.builder().name("Laptop Pro 15").sku("LPT-001")
                    .description("High-performance laptop, 16GB RAM, 512GB SSD")
                    .price(new BigDecimal("1299.99")).category("Electronics")
                    .stockQuantity(50).brand("TechBrand").taxRate(new BigDecimal("19"))
                    .weight(new BigDecimal("1.8")).active(true).build(),
                Product.builder().name("Wireless Headphones").sku("WHP-001")
                    .description("Noise-cancelling Bluetooth, 30h battery")
                    .price(new BigDecimal("249.99")).category("Electronics")
                    .stockQuantity(120).brand("SoundPro").taxRate(new BigDecimal("19"))
                    .weight(new BigDecimal("0.3")).active(true).build(),
                Product.builder().name("Mechanical Keyboard").sku("KBD-001")
                    .description("Cherry MX Blue switches, full-size")
                    .price(new BigDecimal("129.99")).category("Peripherals")
                    .stockQuantity(75).brand("TypeMaster").taxRate(new BigDecimal("19"))
                    .weight(new BigDecimal("1.1")).active(true).build(),
                Product.builder().name("USB-C Hub 7-in-1").sku("HUB-001")
                    .description("HDMI, USB 3.0, SD card reader")
                    .price(new BigDecimal("49.99")).category("Peripherals")
                    .stockQuantity(200).brand("ConnectPro").taxRate(new BigDecimal("19"))
                    .weight(new BigDecimal("0.15")).active(true).build(),
                Product.builder().name("Standing Desk Mat").sku("MAT-001")
                    .description("Anti-fatigue ergonomic mat")
                    .price(new BigDecimal("79.99")).category("Office")
                    .stockQuantity(60).brand("ErgoComfort").taxRate(new BigDecimal("19"))
                    .weight(new BigDecimal("1.5")).active(true).build(),
                Product.builder().name("4K Webcam").sku("CAM-001")
                    .description("4K UHD, built-in mic, autofocus")
                    .price(new BigDecimal("179.99")).category("Electronics")
                    .stockQuantity(8).brand("VisionTech").taxRate(new BigDecimal("19"))
                    .weight(new BigDecimal("0.2")).active(true).build(),
                Product.builder().name("Office Chair Pro").sku("CHR-001")
                    .description("Ergonomic, lumbar support, adjustable armrests")
                    .price(new BigDecimal("399.99")).category("Office")
                    .stockQuantity(25).brand("ErgoComfort").taxRate(new BigDecimal("19"))
                    .weight(new BigDecimal("15.0")).active(true).build(),
                Product.builder().name("Portable SSD 1TB").sku("SSD-001")
                    .description("USB 3.2, 1050MB/s read speed")
                    .price(new BigDecimal("109.99")).category("Storage")
                    .stockQuantity(3).brand("SpeedStore").taxRate(new BigDecimal("19"))
                    .weight(new BigDecimal("0.05")).active(true).build()
            ));
            log.info("Seeded {} products and 2 users", productRepository.count());
        };
    }
}
