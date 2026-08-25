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
--   INSUFFICIENT_STOCK - order more than 2 of PIZZA_03, or more than 1 of PIZZA_08
--   ITEM_NOT_FOUND     - order any item code not listed here

INSERT INTO restaurant_inventory (item_code, stock_count)
SELECT 'PIZZA_01', 50 WHERE NOT EXISTS (SELECT 1 FROM restaurant_inventory WHERE item_code = 'PIZZA_01');
INSERT INTO restaurant_inventory (item_code, stock_count)
SELECT 'PIZZA_02', 40 WHERE NOT EXISTS (SELECT 1 FROM restaurant_inventory WHERE item_code = 'PIZZA_02');
INSERT INTO restaurant_inventory (item_code, stock_count)
SELECT 'PIZZA_03', 2 WHERE NOT EXISTS (SELECT 1 FROM restaurant_inventory WHERE item_code = 'PIZZA_03');
INSERT INTO restaurant_inventory (item_code, stock_count)
SELECT 'PIZZA_04', 30 WHERE NOT EXISTS (SELECT 1 FROM restaurant_inventory WHERE item_code = 'PIZZA_04');
INSERT INTO restaurant_inventory (item_code, stock_count)
SELECT 'PIZZA_05', 25 WHERE NOT EXISTS (SELECT 1 FROM restaurant_inventory WHERE item_code = 'PIZZA_05');
INSERT INTO restaurant_inventory (item_code, stock_count)
SELECT 'PIZZA_06', 35 WHERE NOT EXISTS (SELECT 1 FROM restaurant_inventory WHERE item_code = 'PIZZA_06');
INSERT INTO restaurant_inventory (item_code, stock_count)
SELECT 'PIZZA_07', 20 WHERE NOT EXISTS (SELECT 1 FROM restaurant_inventory WHERE item_code = 'PIZZA_07');
INSERT INTO restaurant_inventory (item_code, stock_count)
SELECT 'PIZZA_08', 1 WHERE NOT EXISTS (SELECT 1 FROM restaurant_inventory WHERE item_code = 'PIZZA_08');
INSERT INTO restaurant_inventory (item_code, stock_count)
SELECT 'PIZZA_09', 45 WHERE NOT EXISTS (SELECT 1 FROM restaurant_inventory WHERE item_code = 'PIZZA_09');
INSERT INTO restaurant_inventory (item_code, stock_count)
SELECT 'PIZZA_10', 15 WHERE NOT EXISTS (SELECT 1 FROM restaurant_inventory WHERE item_code = 'PIZZA_10');
