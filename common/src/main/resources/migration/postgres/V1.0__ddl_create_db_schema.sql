DROP TABLE IF EXISTS category CASCADE;
CREATE TABLE category
(
    id SERIAL,
    name VARCHAR(32) NOT NULL,
    PRIMARY KEY(id)
);

DROP TABLE IF EXISTS supplier CASCADE;
CREATE TABLE supplier
(
    id SERIAL,
    name VARCHAR(64) NOT NULL,
    address VARCHAR(64) NOT NULL,
    PRIMARY KEY(id)
);

DROP TYPE IF EXISTS PRODUCT_STATUS CASCADE;
CREATE TYPE PRODUCT_STATUS AS ENUM('in_processing','available','not_available');

DROP TABLE IF EXISTS product CASCADE;
CREATE TABLE product
(
    id UUID DEFAULT gen_random_uuid(),
    name VARCHAR(64) NOT NULL,
    category_id INT NOT NULL,
    supplier_id INT NOT NULL,
    price FLOAT NOT NULL,
    description VARCHAR(256),
    publication_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status PRODUCT_STATUS DEFAULT('available'),
    PRIMARY KEY(id),
    CONSTRAINT category_fk FOREIGN KEY (category_id) REFERENCES category(id),
    CONSTRAINT supplier_fk FOREIGN KEY (supplier_id) REFERENCES supplier(id),
    CONSTRAINT unique_name UNIQUE (name,supplier_id)
);

DROP TABLE IF EXISTS attachment CASCADE;
CREATE TABLE attachment
(
  id UUID DEFAULT gen_random_uuid(),
  attachment VARCHAR(256) NOT NULL ,
  product_id UUID NOT NULL,
  PRIMARY KEY(id),
  CONSTRAINT product_fk FOREIGN KEY (product_id) REFERENCES product(id),
  CONSTRAINT unique_attachment UNIQUE (attachment,product_id)
);


DROP TYPE IF EXISTS USER_ROLE CASCADE ;
CREATE TYPE USER_ROLE AS ENUM ('manager','client','courier');

DROP TABLE IF EXISTS "user" CASCADE;
CREATE TABLE "user"
(
    id UUID DEFAULT gen_random_uuid(),
    name VARCHAR(20) NOT NULL,
    surname VARCHAR(20) NOT NULL,
    age INT NOT NULL,
    login VARCHAR(50) NOT NULL UNIQUE ,
    password VARCHAR(50) NOT NULL,
    role USER_ROLE NOT NULL ,
    phone VARCHAR(15),
    email VARCHAR(50),
    PRIMARY KEY (id)
);

DROP TABLE IF EXISTS category_subscription CASCADE;
CREATE TABLE category_subscription
(
    category_id INT NOT NULL,
    user_id UUID NOT NULL,
    start_date DATE DEFAULT CURRENT_DATE,
    PRIMARY KEY (category_id,user_id),
    CONSTRAINT category_fk FOREIGN KEY (category_id) REFERENCES category(id),
    CONSTRAINT user_fk FOREIGN KEY (user_id) REFERENCES "user"(id)
);

DROP TABLE IF EXISTS supplier_subscription CASCADE;
CREATE TABLE supplier_subscription
(
    supplier_id INT NOT NULL,
    user_id UUID NOT NULL,
    start_date DATE DEFAULT CURRENT_DATE,
    PRIMARY KEY (supplier_id,user_id),
    CONSTRAINT supplier_fk FOREIGN KEY (supplier_id) REFERENCES supplier(id),
    CONSTRAINT user_fk FOREIGN KEY (user_id) REFERENCES "user"(id)
);

DROP TYPE IF EXISTS ORDER_STATUS CASCADE;
CREATE TYPE ORDER_STATUS AS ENUM('ordered','cancelled','assigned','delivered');

DROP TABLE IF EXISTS "order" CASCADE;
CREATE TABLE "order"
(
    id UUID DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL ,
    total FLOAT DEFAULT 0,
    status ORDER_STATUS DEFAULT 'ordered',
    ordered_start_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    address VARCHAR(256) NOT NULL,
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

DROP TABLE IF EXISTS delivery CASCADE ;
CREATE TABLE IF NOT EXISTS delivery
(
    id UUID DEFAULT gen_random_uuid(),
    courier_id UUID NOT NULL,
    order_id UUID NOT NULL,
    delivery_start_date DATE DEFAULT CURRENT_DATE,
    delivery_finish_date DATE,
    PRIMARY KEY (id),
    CONSTRAINT courier_fk FOREIGN KEY (courier_id) REFERENCES "user"(id),
    CONSTRAINT order_fk FOREIGN KEY (order_id) REFERENCES "order"(id)
    );

DROP TABLE IF EXISTS "group" CASCADE ;
CREATE TABLE "group"
(
    id UUID DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE ,
    PRIMARY KEY (id)
);

DROP TABLE IF EXISTS group_to_user CASCADE ;
CREATE TABLE group_to_user
(
    user_id UUID NOT NULL,
    group_id UUID NOT NULL,
    PRIMARY KEY (user_id,group_id),
    CONSTRAINT user_fk FOREIGN KEY (user_id) REFERENCES "user"(id),
    CONSTRAINT group_fk FOREIGN KEY (group_id) REFERENCES "group"(id)
);

DROP TABLE IF EXISTS group_to_product CASCADE ;
CREATE TABLE group_to_product
(
    product_id UUID NOT NULL,
    group_id UUID NOT NULL,
    PRIMARY KEY (product_id,group_id),
    CONSTRAINT product_fk FOREIGN KEY (product_id) REFERENCES product(id),
    CONSTRAINT group_fk FOREIGN KEY (group_id) REFERENCES "group"(id)
);

DROP TABLE IF EXISTS last_notification CASCADE;
CREATE TABLE last_notification
(
    last_date TIMESTAMP
);