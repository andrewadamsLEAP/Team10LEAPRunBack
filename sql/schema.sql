CREATE TABLE employees (
	employee_id SERIAL PRIMARY KEY,
	email VARCHAR(255) UNIQUE NOT NULL,
	username VARCHAR(255) UNIQUE NOT NULL,
	password VARCHAR(255) NOT NULL,
	first_name VARCHAR(255) NOT NULL,
	last_name VARCHAR(255) NOT NULL,
	role VARCHAR(50) NOT NULL
);

CREATE TABLE instruments (
    ticker VARCHAR(100) PRIMARY KEY,
    previous_close float8 NOT NULL,
    open float8 NOT NULL,
    volume integer NOT NULL,
    avg_volume float8 NOT NULL,
    asset_type VARCHAR(50) NOT NULL,
    CONSTRAINT chk_asset_type
	CHECK (asset_type IN ('STOCK', 'FOREX', 'CRYPTO'));
);

CREATE TABLE clients(
client_id BIGSERIAL PRIMARY KEY,
email VARCHAR(255) NOT NULL UNIQUE,
username VARCHAR(100) NOT NULL UNIQUE,
password VARCHAR(255) NOT NULL,
first_name VARCHAR(100) NOT NULL,
last_name VARCHAR(100) NOT NULL,
cash_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00
);


CREATE TABLE transactions(
transaction_id BIGSERIAL PRIMARY KEY,
client_id BIGINT NOT NULL REFERENCES clients(client_id),
withdrawal NUMERIC(15,2) NOT NULL DEFAULT 0.00 CHECK (withdrawal >= 0),
deposit NUMERIC(15,2) NOT NULL DEFAULT 0.00 CHECK (deposit >= 0),
created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE orders (
order_id BIGSERIAL PRIMARY KEY,
ticker VARCHAR(100) NOT NULL,
client_id BIGINT NOT NULL,
order_type VARCHAR(10) NOT NULL,
order_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    CONSTRAINT chk_order_status CHECK (order_status IN ('pending', 'fullfilled', 'canceled', 'CANCELED', 'PENDING', 'FULFILLED')),
price NUMERIC(18,2) NOT NULL,
quantity INTEGER NOT NULL,
order_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
FOREIGN KEY (ticker)
	REFERENCES instruments(ticker),
FOREIGN KEY (client_id)
	REFERENCES clients(client_id)
);

CREATE TABLE holdings (
    hold_id BIGSERIAL NOT NULL PRIMARY KEY,
    client_id BIGINT NOT NULL,
    ticker VARCHAR(100) NOT NULL,
    quantity INTEGER NOT NULL,
    upadted_at DATE NOT NULL,

    CONSTRAINT fk_client
        FOREIGN KEY (client_id)
        REFERENCES clients (client_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_ticker
        FOREIGN KEY (ticker)
        REFERENCES instruments (ticker)
        ON DELETE CASCADE,

    CONSTRAINT chk_quantity_positive
        CHECK (quantity > 0),

    CONSTRAINT uq_client_ticker
        UNIQUE (client_id, ticker)
);

CREATE TABLE prices (
    price_id BIGSERIAL PRIMARY KEY,
    ticker VARCHAR(100) NOT NULL,
    price NUMERIC(18,2) NOT NULL,
    previous_close NUMERIC(18,2),
    open NUMERIC(18,2),
    high NUMERIC(18,2),
    low NUMERIC(18,2),
    volume BIGINT,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_prices_ticker
        FOREIGN KEY (ticker)
        REFERENCES instruments(ticker)
        ON DELETE CASCADE,

    CONSTRAINT chk_price_positive
        CHECK (price >= 0),

    CONSTRAINT uq_ticker_recorded_at
        UNIQUE (ticker, recorded_at)
);
