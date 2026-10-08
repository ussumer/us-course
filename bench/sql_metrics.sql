-- Run after the MQ/outbox has drained. Replace ? with the coupon id.
SELECT id, total_num, issue_num, (issue_num <= total_num) AS inventory_ok
FROM us_promotion.coupon WHERE id = ?;

SELECT request_id, COUNT(*) AS receive_rows
FROM us_promotion.coupon_receive_record
GROUP BY request_id HAVING COUNT(*) > 1;

-- The user_coupon table has no request_id, so validate totals for the isolated
-- test coupon. A user_limit greater than one is valid; only an excess over
-- accepted events indicates duplicate issuance.
SELECT r.coupon_id,
       COUNT(*) AS accepted_events,
       (SELECT COUNT(*) FROM us_promotion.user_coupon u
        WHERE u.coupon_id = r.coupon_id) AS user_coupon_rows
FROM us_promotion.coupon_receive_record r
WHERE r.coupon_id = ?
GROUP BY r.coupon_id;

-- Learning write counters for a run window. Capture before/after values.
SELECT table_name, table_rows
FROM information_schema.tables
WHERE table_schema = 'us_learning'
  AND table_name IN ('learning_record', 'learning_lesson');
