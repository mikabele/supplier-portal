DROP TYPE IF EXISTS ORDER_STATUS CASCADE;
CREATE TYPE ORDER_STATUS AS ENUM('ordered','canceled');

DROP TABLE IF EXISTS "order" CASCADE;
CREATE TABLE "order"
(
    id UUID DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL ,
    total FLOAT DEFAULT 0,
    status ORDER_STATUS DEFAULT 'ordered',
    ordered_start_date DATE DEFAULT CURRENT_DATE,
    PRIMARY KEY (id),
    CONSTRAINT user_fk FOREIGN KEY (user_id) REFERENCES "user"(id)
);

DROP TABLE IF EXISTS order_to_product CASCADE ;
CREATE TABLE order_to_product
(
    product_id UUID NOT NULL,
    order_id UUID NOT NULL,
    count INT NOT NULL,
    PRIMARY KEY (product_id,order_id),
    CONSTRAINT product_fk FOREIGN KEY (product_id) REFERENCES product(id),
    CONSTRAINT order_fk FOREIGN KEY (order_id) REFERENCES "order"(id)
);

