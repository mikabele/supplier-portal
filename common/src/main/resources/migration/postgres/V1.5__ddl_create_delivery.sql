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