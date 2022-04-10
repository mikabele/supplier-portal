DROP TABLE IF EXISTS "group";
CREATE TABLE "group"
(
    id UUID DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL,
    PRIMARY KEY (id)
);

DROP TABLE IF EXISTS group_to_user;
CREATE TABLE group_to_user
(
  user_id UUID NOT NULL,
  group_id UUID NOT NULL,
  PRIMARY KEY (user_id,group_id),
  CONSTRAINT user_fk FOREIGN KEY (user_id) REFERENCES "user"(id),
  CONSTRAINT group_fk FOREIGN KEY (group_id) REFERENCES "group"(id)
);

DROP TABLE IF EXISTS group_to_product;
CREATE TABLE group_to_product
(
    product_id UUID NOT NULL,
    group_id UUID NOT NULL,
    PRIMARY KEY (product_id,group_id),
    CONSTRAINT product_fk FOREIGN KEY (product_id) REFERENCES product(id),
    CONSTRAINT group_fk FOREIGN KEY (group_id) REFERENCES "group"(id)
);