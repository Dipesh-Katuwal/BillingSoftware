-- MySQL: expand the original CASH/ESEWA enum without changing stored values.
-- Safe to re-run. Apply once to existing databases; Hibernate ddl-auto=update
-- does not reliably expand an existing native enum's allowed values.
ALTER TABLE tbl_orders
    MODIFY COLUMN payment_method ENUM('CASH', 'ESEWA', 'KHALTI', 'STRIPE') NULL;
