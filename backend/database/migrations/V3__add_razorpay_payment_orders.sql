-- Run this once in smart_society_connect_db before enabling Razorpay checkout.
CREATE TABLE IF NOT EXISTS payment_gateway_orders (
    gateway_order_record_id CHAR(36) NOT NULL,
    billing_id CHAR(36) NOT NULL,
    razorpay_order_id VARCHAR(100) NOT NULL,
    razorpay_payment_id VARCHAR(100) NULL,
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (gateway_order_record_id),
    CONSTRAINT uk_gateway_razorpay_order UNIQUE (razorpay_order_id),
    CONSTRAINT uk_gateway_razorpay_payment UNIQUE (razorpay_payment_id),
    CONSTRAINT fk_gateway_order_billing
        FOREIGN KEY (billing_id) REFERENCES billing (billing_id),
    INDEX idx_gateway_order_billing (billing_id),
    INDEX idx_gateway_order_status (status)
) ENGINE=InnoDB;
