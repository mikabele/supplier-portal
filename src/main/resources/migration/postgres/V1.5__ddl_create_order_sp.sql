-- TODO - сделать все в одной транзакции
-- TODO - не делать это в функции
-- TODO - поменьше обращений к базе, подумать как уменьшать суммарное время работы с базой. Имеет смысл разбивать запросы на несколько и какие-то проверки на сторону сервиса

CREATE OR REPLACE FUNCTION create_order(user_id UUID, product_ids UUID[],counts INT[]) RETURNS UUID
AS
$BODY$
DECLARE order_id_val UUID;
    DECLARE total_val FLOAT;
BEGIN
    INSERT INTO "order"("user_id") VALUES (user_id) RETURNING id INTO order_id_val;
    INSERT INTO order_to_product(order_id,product_id,count)
    SELECT order_id_val,unnest(product_ids),unnest(counts);
    total_val := (SELECT SUM(otp.count*p.price) FROM order_to_product AS otp INNER JOIN product AS p ON otp.product_id=p.id WHERE otp.order_id = order_id_val);
    UPDATE "order" SET "total" = total_val WHERE id = order_id_val;
    RETURN order_id_val;
END;
$BODY$
    LANGUAGE plpgsql;