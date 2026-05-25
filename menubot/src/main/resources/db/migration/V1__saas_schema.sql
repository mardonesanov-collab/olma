-- Multi-Tenant Menu, Order & Table Management — PostgreSQL schema

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    tg_id           BIGINT NOT NULL UNIQUE,
    username        VARCHAR(255),
    first_name      VARCHAR(255),
    last_name       VARCHAR(255),
    role            VARCHAR(32) NOT NULL DEFAULT 'CLIENT',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_tg_id ON users (tg_id);
CREATE INDEX idx_users_role ON users (role);

CREATE TABLE restaurants (
    id                  BIGSERIAL PRIMARY KEY,
    owner_id            BIGINT NOT NULL REFERENCES users(id),
    name                VARCHAR(255) NOT NULL,
    description         TEXT,
    logo_url            VARCHAR(512),
    banner_url          VARCHAR(512),
    status              VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    service_fee_percent NUMERIC(5, 2) NOT NULL DEFAULT 10.00,
    qr_code_url         VARCHAR(512),
    unique_slug         VARCHAR(64) UNIQUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_restaurants_owner ON restaurants (owner_id);
CREATE INDEX idx_restaurants_status ON restaurants (status);
CREATE INDEX idx_restaurants_slug ON restaurants (unique_slug);

CREATE TABLE categories (
    id              BIGSERIAL PRIMARY KEY,
    restaurant_id   BIGINT NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    sort_order      INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_categories_restaurant ON categories (restaurant_id);

CREATE TABLE products (
    id              BIGSERIAL PRIMARY KEY,
    category_id     BIGINT NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    restaurant_id   BIGINT NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    price           BIGINT NOT NULL DEFAULT 0,
    image_url       VARCHAR(512),
    is_available    BOOLEAN NOT NULL DEFAULT TRUE,
    search_vector   TSVECTOR,
    sort_order      INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_products_category ON products (category_id);
CREATE INDEX idx_products_restaurant ON products (restaurant_id);
CREATE INDEX idx_products_available ON products (restaurant_id, is_available);
CREATE INDEX idx_products_search ON products USING GIN (search_vector);

CREATE TABLE orders (
    id                      BIGSERIAL PRIMARY KEY,
    restaurant_id           BIGINT NOT NULL REFERENCES restaurants(id),
    client_id               BIGINT NOT NULL REFERENCES users(id),
    table_number            VARCHAR(32),
    total_products_price    BIGINT NOT NULL DEFAULT 0,
    service_fee_price       BIGINT NOT NULL DEFAULT 0,
    final_price             BIGINT NOT NULL DEFAULT 0,
    status                  VARCHAR(32) NOT NULL DEFAULT 'NEW',
    payment_status          VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_restaurant ON orders (restaurant_id);
CREATE INDEX idx_orders_client ON orders (client_id);
CREATE INDEX idx_orders_status ON orders (restaurant_id, status);
CREATE INDEX idx_orders_created ON orders (restaurant_id, created_at);

CREATE TABLE order_items (
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id  BIGINT NOT NULL REFERENCES products(id),
    quantity    INT NOT NULL DEFAULT 1,
    price       BIGINT NOT NULL
);

CREATE INDEX idx_order_items_order ON order_items (order_id);

CREATE TABLE reviews (
    id              BIGSERIAL PRIMARY KEY,
    restaurant_id   BIGINT NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
    client_id       BIGINT NOT NULL REFERENCES users(id),
    order_id        BIGINT REFERENCES orders(id),
    rating          SMALLINT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment         TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_reviews_restaurant ON reviews (restaurant_id);

-- Full-text search trigger for products
CREATE OR REPLACE FUNCTION products_search_vector_update() RETURNS trigger AS $$
BEGIN
    NEW.search_vector := to_tsvector('simple',
        coalesce(NEW.name, '') || ' ' || coalesce(NEW.description, ''));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER products_search_vector_trigger
    BEFORE INSERT OR UPDATE OF name, description ON products
    FOR EACH ROW EXECUTE FUNCTION products_search_vector_update();
