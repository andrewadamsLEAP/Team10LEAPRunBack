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
    previous_close float8 NOT NULL DEFAULT 0,
    open float8 NOT NULL DEFAULT 0,
    volume integer NOT NULL DEFAULT 0,
    avg_volume float8 NOT NULL DEFAULT 0,
    asset_type VARCHAR(50) NOT NULL,

    CONSTRAINT chk_asset_type
        CHECK (asset_type IN ('STOCK', 'FOREX', 'CRYPTO'))
);

CREATE TABLE clients (
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
ttype VARCHAR(10) CONSTRAINT checker CHECK(ttype IN ('withdrawal' , 'deposit')),
amount NUMERIC(15,2) CONSTRAINT notzero CHECK(amount > 0),
created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE orders (
    order_id BIGSERIAL PRIMARY KEY,
    ticker VARCHAR(100) NOT NULL,
    client_id BIGINT NOT NULL,
    order_type VARCHAR(10) NOT NULL,
    order_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    CONSTRAINT chk_order_status
        CHECK (
            order_status IN (
                'CANCELLED',
                'PENDING',
                'FULFILLED'
            )
        ),

    price NUMERIC(18,2) NOT NULL,
    quantity INTEGER NOT NULL,
    order_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (ticker)
        REFERENCES instruments(ticker),

    FOREIGN KEY (client_id)
        REFERENCES clients(client_id)
);

CREATE TABLE holdings (
    client_id BIGINT NOT NULL ,
    ticker VARCHAR(100) NOT NULL,
    quantity INTEGER NOT NULL,

    PRIMARY KEY (client_id, ticker)

    CONSTRAINT fk_client
        FOREIGN KEY (client_id)
        REFERENCES clients(client_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_ticker
        FOREIGN KEY (ticker)
        REFERENCES instruments(ticker)
        ON DELETE CASCADE,

    CONSTRAINT chk_quantity_positive
        CHECK (quantity > 0),

    CONSTRAINT uq_client_ticker
        UNIQUE (client_id, ticker)
);

CREATE TABLE prices (
    price_id BIGSERIAL PRIMARY KEY,

    ticker VARCHAR(100) NOT NULL,

    -- Alpaca quote data
    ask_price NUMERIC(18,4),
    ask_size NUMERIC(18,4),
    ask_exchange VARCHAR(10),

    bid_price NUMERIC(18,4),
    bid_size NUMERIC(18,4),
    bid_exchange VARCHAR(10),

    tape VARCHAR(10),

    -- Exact timestamp supplied by Alpaca
    quote_timestamp TIMESTAMPTZ,

    -- Timestamp when our application recorded the quote
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_prices_ticker
        FOREIGN KEY (ticker)
        REFERENCES instruments(ticker)
        ON DELETE CASCADE,

    CONSTRAINT uq_ticker_recorded_at
        UNIQUE (ticker, recorded_at)
);

CREATE INDEX idx_prices_ticker_recorded_at
    ON prices (ticker, recorded_at DESC);
