package com.shop.config;

import com.shop.entity.Category;
import com.shop.entity.Product;
import com.shop.entity.Role;
import com.shop.entity.User;
import com.shop.repository.CategoryRepository;
import com.shop.repository.ProductRepository;
import com.shop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private static final Map<String, String> PRODUCT_IMAGES = new LinkedHashMap<>();
    private static final Map<String, String> CATEGORY_IMAGES = new LinkedHashMap<>();

    static {
        // Categories
        CATEGORY_IMAGES.put(
                "Electronics",
                "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=640&h=640&q=80"
        );
        CATEGORY_IMAGES.put(
                "Fashion",
                "https://images.unsplash.com/photo-1483985988355-763728e1935b?auto=format&fit=crop&w=640&h=640&q=80"
        );
        CATEGORY_IMAGES.put(
                "Home & Living",
                "https://images.unsplash.com/photo-1616486338812-3dadae4b4ace?auto=format&fit=crop&w=640&h=640&q=80"
        );
        CATEGORY_IMAGES.put(
                "Sports & Outdoors",
                "https://images.unsplash.com/photo-1517649763962-0c623066027e?auto=format&fit=crop&w=640&h=640&q=80"
        );
        CATEGORY_IMAGES.put(
                "Beauty & Care",
                "https://images.unsplash.com/photo-1596462502278-27bfdc403348?auto=format&fit=crop&w=640&h=640&q=80"
        );
        CATEGORY_IMAGES.put(
                "Books",
                "https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=640&h=640&q=80"
        );
        CATEGORY_IMAGES.put(
                "Kitchen & Dining",
                "https://images.unsplash.com/photo-1556910103-1c02745aae4d?auto=format&fit=crop&w=640&h=640&q=80"
        );

        // Electronics
        PRODUCT_IMAGES.put(
                "Wireless Headphones",
                "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=800&h=800&q=80"
        );
        PRODUCT_IMAGES.put(
                "Smart Watch",
                "https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=800&h=800&q=80"
        );
        PRODUCT_IMAGES.put(
                "Bluetooth Speaker",
                "https://images.unsplash.com/photo-1608043152269-423dbba4e7e1?auto=format&fit=crop&w=800&h=800&q=80"
        );
        PRODUCT_IMAGES.put(
                "USB-C Hub",
                "https://images.unsplash.com/photo-1625948515291-69613efd103f?auto=format&fit=crop&w=800&h=800&q=80"
        );

        // Fashion
        PRODUCT_IMAGES.put(
                "Classic Cotton T-Shirt",
                "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=800&h=800&q=80"
        );
        PRODUCT_IMAGES.put(
                "Leather Crossbody Bag",
                "https://images.unsplash.com/photo-1548036328-c9fa89d128fa?auto=format&fit=crop&w=800&h=800&q=80"
        );
        PRODUCT_IMAGES.put(
                "Slim Fit Jeans",
                "https://images.unsplash.com/photo-1542272454315-4c01d7abdf4a?auto=format&fit=crop&w=800&h=800&q=80"
        );
        PRODUCT_IMAGES.put(
                "Canvas Sneakers",
                "https://images.unsplash.com/photo-1549298916-b41d501d3772?auto=format&fit=crop&w=800&h=800&q=80"
        );

        // Home & Living
        PRODUCT_IMAGES.put(
                "Ceramic Pour-Over Mug",
                "https://images.unsplash.com/photo-1514228742587-6b1558fcca3d?auto=format&fit=crop&w=800&h=800&q=80"
        );
        PRODUCT_IMAGES.put(
                "Linen Throw Pillow",
                "https://images.unsplash.com/photo-1584100936595-c0654b55a2e2?auto=format&fit=crop&w=800&h=800&q=80"
        );
        PRODUCT_IMAGES.put(
                "Desk Lamp",
                "https://images.unsplash.com/photo-1507473885765-e6ed057f782c?auto=format&fit=crop&w=800&h=800&q=80"
        );
        PRODUCT_IMAGES.put(
                "Scented Candle Set",
                "https://images.unsplash.com/photo-1603006905004-abe15a4c0b7a?auto=format&fit=crop&w=800&h=800&q=80"
        );

        // Sports & Outdoors
        PRODUCT_IMAGES.put(
                "Yoga Mat",
                "https://images.unsplash.com/photo-1601925260368-ae2f83cf8b7f?auto=format&fit=crop&w=800&h=800&q=80"
        );
        PRODUCT_IMAGES.put(
                "Stainless Water Bottle",
                "https://images.unsplash.com/photo-1602143407151-7111542de6e8?auto=format&fit=crop&w=800&h=800&q=80"
        );
        PRODUCT_IMAGES.put(
                "Resistance Bands Set",
                "https://images.unsplash.com/photo-1598289431512-b97b0917affc?auto=format&fit=crop&w=800&h=800&q=80"
        );

        // Beauty & Care
        PRODUCT_IMAGES.put(
                "Hydrating Face Cream",
                "https://images.unsplash.com/photo-1556228578-0d85b1a4d571?auto=format&fit=crop&w=800&h=800&q=80"
        );
        PRODUCT_IMAGES.put(
                "Essential Oil Diffuser",
                "https://images.unsplash.com/photo-1608571423902-eed4a5ad8108?auto=format&fit=crop&w=800&h=800&q=80"
        );
        PRODUCT_IMAGES.put(
                "Bamboo Hairbrush",
                "https://images.unsplash.com/photo-1522338242992-e1a549cf75ca?auto=format&fit=crop&w=800&h=800&q=80"
        );

        // Books
        PRODUCT_IMAGES.put(
                "Modern Design Hardcover",
                "https://images.unsplash.com/photo-1544947950-fa07a98d237f?auto=format&fit=crop&w=800&h=800&q=80"
        );
        PRODUCT_IMAGES.put(
                "Cookbook Essentials",
                "https://images.unsplash.com/photo-1589998059171-988d887df646?auto=format&fit=crop&w=800&h=800&q=80"
        );

        // Kitchen & Dining
        PRODUCT_IMAGES.put(
                "Non-Stick Frying Pan",
                "https://images.unsplash.com/photo-1556911220-bff31c875dbb?auto=format&fit=crop&w=800&h=800&q=80"
        );
        PRODUCT_IMAGES.put(
                "Glass Meal Prep Set",
                "https://images.unsplash.com/photo-1585515320310-259814833e62?auto=format&fit=crop&w=800&h=800&q=80"
        );
        PRODUCT_IMAGES.put(
                "Wooden Cutting Board",
                "https://images.unsplash.com/photo-1604908177453-746695fb6d66?auto=format&fit=crop&w=800&h=800&q=80"
        );
    }

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedUsers();
        seedCatalog();
        backfillImages();
    }

    private void seedUsers() {
        if (!userRepository.existsByEmail("admin@shop.com")) {
            userRepository.save(User.builder()
                    .name("Shop Admin")
                    .email("admin@shop.com")
                    .password(passwordEncoder.encode("Admin@123"))
                    .role(Role.ADMIN)
                    .phone("94770000001")
                    .build());
            log.info("Seeded admin user: admin@shop.com");
        }

        if (!userRepository.existsByEmail("customer@shop.com")) {
            userRepository.save(User.builder()
                    .name("Test Customer")
                    .email("customer@shop.com")
                    .password(passwordEncoder.encode("Customer@123"))
                    .role(Role.CUSTOMER)
                    .phone("94770000002")
                    .build());
            log.info("Seeded customer user: customer@shop.com");
        }
    }

    private void seedCatalog() {
        Category electronics = ensureCategory("Electronics");
        Category fashion = ensureCategory("Fashion");
        Category home = ensureCategory("Home & Living");
        Category sports = ensureCategory("Sports & Outdoors");
        Category beauty = ensureCategory("Beauty & Care");
        Category books = ensureCategory("Books");
        Category kitchen = ensureCategory("Kitchen & Dining");

        // Electronics
        ensureProduct(electronics, "Wireless Headphones",
                "Comfortable over-ear Bluetooth headphones with 30-hour battery life.", "89.99", 40);
        ensureProduct(electronics, "Smart Watch",
                "Fitness tracking, heart-rate monitor, and smartphone notifications.", "149.00", 25);
        ensureProduct(electronics, "Bluetooth Speaker",
                "Portable waterproof speaker with rich bass and 12-hour playtime.", "59.99", 35);
        ensureProduct(electronics, "USB-C Hub",
                "7-in-1 hub with HDMI, USB-A, SD card reader, and power delivery.", "39.50", 50);

        // Fashion
        ensureProduct(fashion, "Classic Cotton T-Shirt",
                "Soft everyday tee. Available in multiple sizes.", "19.99", 100);
        ensureProduct(fashion, "Leather Crossbody Bag",
                "Compact everyday bag with adjustable strap.", "59.50", 30);
        ensureProduct(fashion, "Slim Fit Jeans",
                "Stretch denim jeans with a modern slim cut.", "49.99", 60);
        ensureProduct(fashion, "Canvas Sneakers",
                "Lightweight everyday sneakers with cushioned insole.", "44.00", 55);

        // Home & Living
        ensureProduct(home, "Ceramic Pour-Over Mug",
                "350ml handmade ceramic mug for coffee or tea.", "24.00", 60);
        ensureProduct(home, "Linen Throw Pillow",
                "Soft linen cushion cover, 45x45cm.", "29.99", 45);
        ensureProduct(home, "Desk Lamp",
                "Adjustable LED desk lamp with warm and cool light modes.", "34.99", 40);
        ensureProduct(home, "Scented Candle Set",
                "Set of three soy candles with fresh seasonal scents.", "27.50", 70);

        // Sports & Outdoors
        ensureProduct(sports, "Yoga Mat",
                "Non-slip 6mm yoga mat with carrying strap.", "32.00", 48);
        ensureProduct(sports, "Stainless Water Bottle",
                "Insulated 750ml bottle keeps drinks cold for 24 hours.", "22.99", 80);
        ensureProduct(sports, "Resistance Bands Set",
                "Five-level resistance bands for home workouts.", "18.50", 90);

        // Beauty & Care
        ensureProduct(beauty, "Hydrating Face Cream",
                "Lightweight daily moisturizer for all skin types.", "28.00", 65);
        ensureProduct(beauty, "Essential Oil Diffuser",
                "Ultrasonic diffuser with soft ambient light.", "36.99", 40);
        ensureProduct(beauty, "Bamboo Hairbrush",
                "Gentle paddle brush with natural bamboo handle.", "14.99", 75);

        // Books
        ensureProduct(books, "Modern Design Hardcover",
                "Illustrated guide to contemporary interior design.", "31.00", 40);
        ensureProduct(books, "Cookbook Essentials",
                "100 everyday recipes for home cooks.", "26.50", 55);

        // Kitchen & Dining
        ensureProduct(kitchen, "Non-Stick Frying Pan",
                "28cm ceramic non-stick pan, oven-safe handle.", "42.00", 35);
        ensureProduct(kitchen, "Glass Meal Prep Set",
                "Set of 5 glass containers with locking lids.", "29.99", 50);
        ensureProduct(kitchen, "Wooden Cutting Board",
                "Thick acacia wood board with juice groove.", "24.50", 45);

        log.info("Catalog ready: {} categories, {} products",
                categoryRepository.count(), productRepository.count());
    }

    private Category ensureCategory(String name) {
        return categoryRepository.findByNameIgnoreCase(name)
                .map(existing -> {
                    if ((existing.getImageUrl() == null || existing.getImageUrl().isBlank())
                            && CATEGORY_IMAGES.containsKey(name)) {
                        existing.setImageUrl(CATEGORY_IMAGES.get(name));
                        return categoryRepository.save(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .name(name)
                        .imageUrl(CATEGORY_IMAGES.get(name))
                        .build()));
    }

    private void ensureProduct(
            Category category,
            String name,
            String description,
            String price,
            int stock
    ) {
        if (productRepository.existsByNameIgnoreCase(name)) {
            return;
        }
        productRepository.save(Product.builder()
                .name(name)
                .description(description)
                .price(new BigDecimal(price))
                .stockQuantity(stock)
                .imageUrl(PRODUCT_IMAGES.get(name))
                .category(category)
                .build());
        log.info("Seeded product: {}", name);
    }

    private void backfillImages() {
        int productsUpdated = 0;
        for (Product product : productRepository.findAll()) {
            String mapped = PRODUCT_IMAGES.get(product.getName());
            if (mapped == null) {
                continue;
            }
            if (product.getImageUrl() == null || product.getImageUrl().isBlank()
                    || product.getImageUrl().contains("0c0c0c0c")) {
                product.setImageUrl(mapped);
                productRepository.save(product);
                productsUpdated++;
            }
        }

        int categoriesUpdated = 0;
        for (Category category : categoryRepository.findAll()) {
            String mapped = CATEGORY_IMAGES.get(category.getName());
            if (mapped == null) {
                continue;
            }
            if (category.getImageUrl() == null || category.getImageUrl().isBlank()) {
                category.setImageUrl(mapped);
                categoryRepository.save(category);
                categoriesUpdated++;
            }
        }

        if (productsUpdated > 0 || categoriesUpdated > 0) {
            log.info("Backfilled images for {} products and {} categories", productsUpdated, categoriesUpdated);
        }
    }
}
