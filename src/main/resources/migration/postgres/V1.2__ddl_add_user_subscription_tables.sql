
DROP TYPE IF EXISTS USER_ROLE CASCADE ;
CREATE TYPE USER_ROLE AS ENUM ('manager','client','courier');

DROP TABLE IF EXISTS "user" CASCADE;
CREATE TABLE "user"
(
    id UUID DEFAULT gen_random_uuid(),
    name VARCHAR(20) NOT NULL,
    surname VARCHAR(20) NOT NULL,
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