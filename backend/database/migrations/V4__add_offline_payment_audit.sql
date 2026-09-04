ALTER TABLE payment_history
    ADD COLUMN bank_name VARCHAR(100) NULL AFTER transaction_reference,
    ADD COLUMN cheque_date DATE NULL AFTER bank_name,
    ADD COLUMN recorded_by_user_id INT NULL AFTER remarks,
    ADD COLUMN recorded_by_name VARCHAR(100) NULL AFTER recorded_by_user_id;

CREATE INDEX idx_payment_recorded_by ON payment_history (recorded_by_user_id);
