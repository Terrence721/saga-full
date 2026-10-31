-- Remove legacy item codes left over from the old PIZZA_0x naming so a
-- persistent database (Postgres / docker-compose volume) converges on the new
-- item codes instead of keeping stale rows that the frontend would otherwise
-- list and order against.
DELETE FROM menu_items WHERE item_code LIKE 'PIZZA_%' OR item_code LIKE 'DRINK_%' OR item_code LIKE 'SIDE_%';
DELETE FROM restaurant_inventory WHERE item_code LIKE 'PIZZA_%' OR item_code LIKE 'DRINK_%' OR item_code LIKE 'SIDE_%';

-- Converge any persisted rows on the new category convention (category == item code).
UPDATE menu_items SET category = item_code;

-- Seed data so restaurant-service is actually exercisable in a real run (bootRun,
-- docker-compose, manual end-to-end testing) - without this, InventoryItem rows never
-- get created anywhere outside of tests, and every real order fails ITEM_NOT_FOUND
-- unconditionally. INSERT...WHERE NOT EXISTS (not ON CONFLICT) is portable across both
-- H2 configurations this repo runs against - the explicit MODE=PostgreSQL H2 URL used
-- for a full app boot, and @DataJpaTest's own auto-configured plain H2, which doesn't
-- carry that mode and rejects ON CONFLICT syntax - as well as real Postgres, so this
-- also stays safe to re-run across restarts against a persistent database.
-- Ten items, two deliberately low-stock, to exercise all three InventoryStatus
-- outcomes for manual testing:
--   ALLOCATED          - order any item below with plenty of stock
--   INSUFFICIENT_STOCK - order more than 2 of HEAT_WAVE, or more than 1 of TRUFFLE
--   ITEM_NOT_FOUND     - order any item code not listed here

INSERT INTO restaurant_inventory (item_code, stock_count)
SELECT 'MARGHERITA', 50 WHERE NOT EXISTS (SELECT 1 FROM restaurant_inventory WHERE item_code = 'MARGHERITA');
INSERT INTO restaurant_inventory (item_code, stock_count)
SELECT 'PEPPERONI', 40 WHERE NOT EXISTS (SELECT 1 FROM restaurant_inventory WHERE item_code = 'PEPPERONI');
INSERT INTO restaurant_inventory (item_code, stock_count)
SELECT 'HEAT_WAVE', 2 WHERE NOT EXISTS (SELECT 1 FROM restaurant_inventory WHERE item_code = 'HEAT_WAVE');
INSERT INTO restaurant_inventory (item_code, stock_count)
SELECT 'QUATTRO_FORMAGGI', 30 WHERE NOT EXISTS (SELECT 1 FROM restaurant_inventory WHERE item_code = 'QUATTRO_FORMAGGI');
INSERT INTO restaurant_inventory (item_code, stock_count)
SELECT 'VEGGIE', 25 WHERE NOT EXISTS (SELECT 1 FROM restaurant_inventory WHERE item_code = 'VEGGIE');
INSERT INTO restaurant_inventory (item_code, stock_count)
SELECT 'CAPRICCIOSA', 35 WHERE NOT EXISTS (SELECT 1 FROM restaurant_inventory WHERE item_code = 'CAPRICCIOSA');
INSERT INTO restaurant_inventory (item_code, stock_count)
SELECT 'DIAVOLA', 20 WHERE NOT EXISTS (SELECT 1 FROM restaurant_inventory WHERE item_code = 'DIAVOLA');
INSERT INTO restaurant_inventory (item_code, stock_count)
SELECT 'TRUFFLE', 1 WHERE NOT EXISTS (SELECT 1 FROM restaurant_inventory WHERE item_code = 'TRUFFLE');
INSERT INTO restaurant_inventory (item_code, stock_count)
SELECT 'HAWAIIAN', 45 WHERE NOT EXISTS (SELECT 1 FROM restaurant_inventory WHERE item_code = 'HAWAIIAN');
INSERT INTO restaurant_inventory (item_code, stock_count)
SELECT 'SPINACH_RICOTTA', 15 WHERE NOT EXISTS (SELECT 1 FROM restaurant_inventory WHERE item_code = 'SPINACH_RICOTTA');
INSERT INTO restaurant_inventory (item_code, stock_count) SELECT 'COLA', 100 WHERE NOT EXISTS (SELECT 1 FROM restaurant_inventory WHERE item_code = 'COLA');
INSERT INTO restaurant_inventory (item_code, stock_count) SELECT 'LEMONADE', 100 WHERE NOT EXISTS (SELECT 1 FROM restaurant_inventory WHERE item_code = 'LEMONADE');
INSERT INTO restaurant_inventory (item_code, stock_count) SELECT 'GARLIC_BREAD', 60 WHERE NOT EXISTS (SELECT 1 FROM restaurant_inventory WHERE item_code = 'GARLIC_BREAD');

INSERT INTO menu_items (item_code, name, description, category, price, available)
SELECT 'MARGHERITA','Margherita','Tomato, mozzarella, basil','MARGHERITA',8.50,true WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE item_code='MARGHERITA');
INSERT INTO menu_items (item_code, name, description, category, price, available)
SELECT 'PEPPERONI','Pepperoni','Tomato, mozzarella, pepperoni','PEPPERONI',10.00,true WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE item_code='PEPPERONI');
INSERT INTO menu_items (item_code, name, description, category, price, available)
SELECT 'HEAT_WAVE','Heat Wave','Spicy salami, jalapeño, chili oil','HEAT_WAVE',11.50,true WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE item_code='HEAT_WAVE');
INSERT INTO menu_items (item_code, name, description, category, price, available)
SELECT 'QUATTRO_FORMAGGI','Quattro Formaggi','Mozzarella, gorgonzola, fontina, parmesan','QUATTRO_FORMAGGI',12.00,true WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE item_code='QUATTRO_FORMAGGI');
INSERT INTO menu_items (item_code, name, description, category, price, available)
SELECT 'VEGGIE','Veggie','Artichoke, mushroom, bell pepper, olive','VEGGIE',11.00,true WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE item_code='VEGGIE');
INSERT INTO menu_items (item_code, name, description, category, price, available)
SELECT 'CAPRICCIOSA','Capricciosa','Ham, artichoke, mushroom, olive, egg','CAPRICCIOSA',13.00,true WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE item_code='CAPRICCIOSA');
INSERT INTO menu_items (item_code, name, description, category, price, available)
SELECT 'DIAVOLA','Diavola','Spicy nduja, mozzarella, chili','DIAVOLA',12.50,true WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE item_code='DIAVOLA');
INSERT INTO menu_items (item_code, name, description, category, price, available)
SELECT 'TRUFFLE','Truffle','Mushroom, truffle oil, parmesan (low stock)','TRUFFLE',14.00,true WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE item_code='TRUFFLE');
INSERT INTO menu_items (item_code, name, description, category, price, available)
SELECT 'HAWAIIAN','Hawaiian','Ham, pineapple, mozzarella','HAWAIIAN',10.50,true WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE item_code='HAWAIIAN');
INSERT INTO menu_items (item_code, name, description, category, price, available)
SELECT 'SPINACH_RICOTTA','Spinach Ricotta','Ricotta, spinach, garlic, parmesan','SPINACH_RICOTTA',11.50,true WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE item_code='SPINACH_RICOTTA');
INSERT INTO menu_items (item_code, name, description, category, price, available)
SELECT 'COLA','Cola','330 ml','COLA',2.50,true WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE item_code='COLA');
INSERT INTO menu_items (item_code, name, description, category, price, available)
SELECT 'LEMONADE','Lemonade','330 ml','LEMONADE',2.50,true WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE item_code='LEMONADE');
INSERT INTO menu_items (item_code, name, description, category, price, available)
SELECT 'GARLIC_BREAD','Garlic Bread','4 pieces','GARLIC_BREAD',4.00,true WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE item_code='GARLIC_BREAD');
