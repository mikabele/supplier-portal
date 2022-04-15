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
  attachment VARCHAR(256) NOT NULL UNIQUE ,
  product_id UUID NOT NULL,
  PRIMARY KEY(id),
  CONSTRAINT product_fk FOREIGN KEY (product_id) REFERENCES product(id)
);