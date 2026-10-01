-- ============================================================
-- EMPLOYEES
-- ============================================================

INSERT INTO employees (
    email,
    username,
    password,
    first_name,
    last_name,
    role
) VALUES
('admintest@gmail.com', 'admin_test', 'CTRLALTDELETE', 'Admin', 'Test', 'ADMIN'),
('admin2@example.com', 'admin_agarcia', 'Maple9!Harbor', 'Ana', 'Garcia', 'ADMIN'),
('admin3@example.com', 'admin_mchen', 'Bronze_Kite42', 'Michael', 'Chen', 'ADMIN'),
('admin4@example.com', 'admin_pkumar', 'Quartz88-Wind', 'Priya', 'Kumar', 'ADMIN'),
('admin5@example.com', 'admin_ljohnson', 'Cobalt!Trail15', 'Laura', 'Johnson', 'ADMIN'),
('admin6@example.com', 'admin_dwilliams', 'Ember-Fox2026', 'David', 'Williams', 'ADMIN'),
('admin7@example.com', 'admin_smartin', 'Willow63!Storm', 'Sophia', 'Martin', 'ADMIN'),
('admin8@example.com', 'admin_rlee', 'Granite_Owl91', 'Robert', 'Lee', 'ADMIN'),
('admin9@example.com', 'admin_ebrown', 'Sunset27-Reef', 'Emily', 'Brown', 'ADMIN'),
('admin10@example.com', 'admin_twilson', 'Ironclad!Path8', 'Thomas', 'Wilson', 'ADMIN'),
('reportertest@example.com', 'rep_test', 'CTRLALTDELETE', 'Rep', 'Test', 'REPORTER'),
('reporter2@example.com', 'rep_jtaylor', 'Copper!Vista56', 'James', 'Taylor', 'REPORTER'),
('reporter3@example.com', 'rep_nthomas', 'Nimbus-Creek19', 'Nina', 'Thomas', 'REPORTER'),
('reporter4@example.com', 'rep_omoore', 'Onyx82!Meadow', 'Oscar', 'Moore', 'REPORTER'),
('reporter5@example.com', 'rep_hjackson', 'Hazel_Summit47', 'Hannah', 'Jackson', 'REPORTER'),
('reporter6@example.com', 'rep_bwhite', 'Birch-Falcon63', 'Brian', 'White', 'REPORTER'),
('reporter7@example.com', 'rep_charris', 'Cedar!Ridge29', 'Chloe', 'Harris', 'REPORTER'),
('reporter8@example.com', 'rep_gmartinez', 'Garnet_Wave71', 'George', 'Martinez', 'REPORTER'),
('reporter9@example.com', 'rep_svance', 'Slate-Hollow05', 'Sarah', 'Vance', 'REPORTER'),
('reporter10@example.com', 'rep_rclark', 'Rustic!Beacon40', 'Ryan', 'Clark', 'REPORTER');


-- ============================================================
-- INSTRUMENTS
--
-- One authoritative insert.
--
-- STOCK  -> Alpaca Stocks API
-- CRYPTO -> Alpaca Crypto API
-- FOREX  -> configured for future Forex support
-- ============================================================

INSERT INTO instruments (
    ticker,
    previous_close,
    open,
    volume,
    avg_volume,
    asset_type
) VALUES

-- STOCKS
('MMM',   0, 0, 0, 0, 'STOCK'),
('AXP',   0, 0, 0, 0, 'STOCK'),
('AMGN',  0, 0, 0, 0, 'STOCK'),
('AMZN',  0, 0, 0, 0, 'STOCK'),
('AAPL',  0, 0, 0, 0, 'STOCK'),
('GOOGL', 0, 0, 0, 0, 'STOCK'),
('TSLA',  0, 0, 0, 0, 'STOCK'),
('BA',    0, 0, 0, 0, 'STOCK'),
('CAT',   0, 0, 0, 0, 'STOCK'),
('CVX',   0, 0, 0, 0, 'STOCK'),
('CSCO',  0, 0, 0, 0, 'STOCK'),
('KO',    0, 0, 0, 0, 'STOCK'),
('DIS',   0, 0, 0, 0, 'STOCK'),
('GS',    0, 0, 0, 0, 'STOCK'),
('HD',    0, 0, 0, 0, 'STOCK'),
('HON',   0, 0, 0, 0, 'STOCK'),
('IBM',   0, 0, 0, 0, 'STOCK'),
('JNJ',   0, 0, 0, 0, 'STOCK'),
('JPM',   0, 0, 0, 0, 'STOCK'),
('MCD',   0, 0, 0, 0, 'STOCK'),
('MRK',   0, 0, 0, 0, 'STOCK'),
('MSFT',  0, 0, 0, 0, 'STOCK'),
('NKE',   0, 0, 0, 0, 'STOCK'),
('NVDA',  0, 0, 0, 0, 'STOCK'),
('PG',    0, 0, 0, 0, 'STOCK'),
('CRM',   0, 0, 0, 0, 'STOCK'),
('SHW',   0, 0, 0, 0, 'STOCK'),
('TRV',   0, 0, 0, 0, 'STOCK'),
('UNH',   0, 0, 0, 0, 'STOCK'),
('VZ',    0, 0, 0, 0, 'STOCK'),
('V',     0, 0, 0, 0, 'STOCK'),
('WMT',   0, 0, 0, 0, 'STOCK'),

-- FOREX
('EUR-USD', 1.165200, 1.164800, 125430, 132500, 'FOREX'),
('GBP-USD', 1.348700, 1.347900,  98450, 105200, 'FOREX'),
('USD-JPY', 148.420000, 148.650000, 156780, 162300, 'FOREX'),
('AUD-USD', 0.657800, 0.658200, 76430, 82100, 'FOREX'),
('USD-CAD', 1.374500, 1.373800, 89320, 91400, 'FOREX'),

-- CRYPTO
('BTC-USD', 108450.250000, 108720.500000,  324500,  356000, 'CRYPTO'),
('ETH-USD',   4350.750000,   4382.200000,  512300,  548000, 'CRYPTO'),
('SOL-USD',    198.450000,    201.120000,  845600,  912000, 'CRYPTO'),
('XRP-USD',      2.840000,      2.910000, 1254300, 1345000, 'CRYPTO'),
('ADA-USD',      0.842000,      0.856000,  634200,  701000, 'CRYPTO')

ON CONFLICT (ticker) DO NOTHING;


-- ============================================================
-- DUMMY PRICE HISTORY
--
-- Uses the NEW Alpaca quote schema.
--
-- We store:
--   ask_price
--   ask_size
--   ask_exchange
--   bid_price
--   bid_size
--   bid_exchange
--   tape
--   quote_timestamp
--   recorded_at
-- ============================================================

INSERT INTO prices (
    ticker,
    ask_price,
    ask_size,
    ask_exchange,
    bid_price,
    bid_size,
    bid_exchange,
    tape,
    quote_timestamp,
    recorded_at
) VALUES

-- AAPL
('AAPL', 229.46, 100, 'V', 229.45, 200, 'V', 'C',
 '2026-08-31 14:00:00+00',
 '2026-08-31 14:00:00+00'),

('AAPL', 230.13, 150, 'V', 230.12, 250, 'V', 'C',
 '2026-08-31 15:00:00+00',
 '2026-08-31 15:00:00+00'),

('AAPL', 228.98, 125, 'V', 228.97, 300, 'V', 'C',
 '2026-08-31 16:00:00+00',
 '2026-08-31 16:00:00+00'),

-- MSFT
('MSFT', 511.26, 100, 'V', 511.25, 200, 'V', 'C',
 '2026-08-31 14:00:00+00',
 '2026-08-31 14:00:00+00'),

('MSFT', 513.85, 125, 'V', 513.84, 225, 'V', 'C',
 '2026-08-31 15:00:00+00',
 '2026-08-31 15:00:00+00'),

('MSFT', 512.44, 150, 'V', 512.43, 300, 'V', 'C',
 '2026-08-31 16:00:00+00',
 '2026-08-31 16:00:00+00'),

-- GOOGL
('GOOGL', 187.66, 100, 'V', 187.65, 200, 'V', 'C',
 '2026-08-31 14:00:00+00',
 '2026-08-31 14:00:00+00'),

('GOOGL', 189.22, 125, 'V', 189.21, 250, 'V', 'C',
 '2026-08-31 15:00:00+00',
 '2026-08-31 15:00:00+00'),

('GOOGL', 188.44, 150, 'V', 188.43, 300, 'V', 'C',
 '2026-08-31 16:00:00+00',
 '2026-08-31 16:00:00+00'),

-- AMZN
('AMZN', 231.79, 100, 'V', 231.78, 200, 'V', 'C',
 '2026-08-31 14:00:00+00',
 '2026-08-31 14:00:00+00'),

('AMZN', 233.46, 125, 'V', 233.45, 250, 'V', 'C',
 '2026-08-31 15:00:00+00',
 '2026-08-31 15:00:00+00'),

('AMZN', 232.20, 150, 'V', 232.19, 300, 'V', 'C',
 '2026-08-31 16:00:00+00',
 '2026-08-31 16:00:00+00'),

-- TSLA
('TSLA', 342.13, 100, 'V', 342.12, 200, 'V', 'C',
 '2026-08-31 14:00:00+00',
 '2026-08-31 14:00:00+00'),

('TSLA', 338.76, 125, 'V', 338.75, 250, 'V', 'C',
 '2026-08-31 15:00:00+00',
 '2026-08-31 15:00:00+00'),

('TSLA', 345.44, 150, 'V', 345.43, 300, 'V', 'C',
 '2026-08-31 16:00:00+00',
 '2026-08-31 16:00:00+00'),

-- NVDA
('NVDA', 181.35, 100, 'V', 181.34, 200, 'V', 'C',
 '2026-08-31 14:00:00+00',
 '2026-08-31 14:00:00+00'),

('NVDA', 184.68, 125, 'V', 184.67, 250, 'V', 'C',
 '2026-08-31 15:00:00+00',
 '2026-08-31 15:00:00+00'),

('NVDA', 183.93, 150, 'V', 183.92, 300, 'V', 'C',
 '2026-08-31 16:00:00+00',
 '2026-08-31 16:00:00+00');


-- ============================================================
-- CLIENTS
-- ============================================================

INSERT INTO clients (
    email,
    username,
    password,
    first_name,
    last_name,
    cash_amount
) VALUES
('client@gmail.com', 'leap', 'password', 'nathan', 'kevin', 150.00),
('aoife.murphy@gmail.com', 'aoife_m', 'hash_aoife', 'Aoife', 'Murphy', 120.00),
('sean.ryan@gmail.com', 'sean_r', 'hash_sean', 'Seán', 'Ryan', 75.50),
('niamh.byrne@gmail.com', 'niamh_b', 'hash_niamh', 'Niamh', 'Byrne', 300.00),
('cian.walsh@gmail.com', 'cian_w', 'hash_cian', 'Cian', 'Walsh', 0.00),
('orla.kelly@gmail.com', 'orla_k', 'hash_orla', 'Orla', 'Kelly', 9999.99),
('patrick.dunne@gmail.com', 'patrick_d', 'hash_patrick', 'Patrick', 'Dunne', 55.25),
('sinead.oconnor@gmail.com', 'sinead_o', 'hash_sinead', 'Sinéad', 'Connor', 480.10),
('liam.brennan@gmail.com', 'liam_b', 'hash_liam', 'Liam', 'Brennan', 12.00),
('emma.carroll@gmail.com', 'emma_c', 'hash_emma', 'Emma', 'Carroll', 760.00),
('jack.higgins@gmail.com', 'jack_h', 'hash_jack', 'Jack', 'Higgins', 5.75);


-- ============================================================
-- TRANSACTIONS
-- ============================================================

INSERT INTO transactions (
    client_id,
    withdrawal,
    deposit,
    created_at
) VALUES
(1, 0.00, 150.00, '2026-08-31 10:00:00+00'),
(2, 0.00, 120.00, '2026-08-31 10:15:00+00'),
(3, 0.00, 75.50, '2026-08-31 10:30:00+00'),
(4, 0.00, 300.00, '2026-08-31 10:45:00+00'),
(5, 0.00, 500.00, '2026-08-31 11:00:00+00'),
(6, 0.00, 9999.99, '2026-08-31 11:15:00+00'),
(7, 0.00, 55.25, '2026-08-31 11:30:00+00'),
(8, 0.00, 480.10, '2026-08-31 11:45:00+00'),
(9, 0.00, 12.00, '2026-08-31 12:00:00+00'),
(10, 0.00, 760.00, '2026-08-31 12:15:00+00');


-- ============================================================
-- ORDERS
-- ============================================================

INSERT INTO orders (
    ticker,
    client_id,
    order_type,
    order_status,
    price,
    quantity
) VALUES
('AAPL',    1,  'BUY',  'FULFILLED', 225.50, 10),
('MSFT',    2,  'BUY',  'PENDING',   510.75, 5),
('TSLA',    1,  'SELL', 'FULFILLED', 225.50, 10),

('BTC-USD', 3,  'BUY',  'PENDING',   225.50, 10),
('ETH-USD', 4,  'BUY',  'CANCELLED', 225.50, 10),

('AAPL',    2,  'SELL', 'PENDING',   225.50, 10),
('MSFT',    3,  'BUY',  'FULFILLED', 225.50, 10),
('TSLA',    4,  'BUY',  'CANCELLED', 225.50, 10),

('BTC-USD', 1,  'SELL', 'FULFILLED', 225.50, 10),
('ETH-USD', 2,  'BUY',  'PENDING',   225.50, 10),

('NVDA',    5,  'BUY',  'FULFILLED', 183.50, 8),
('GOOGL',   6,  'BUY',  'FULFILLED', 188.00, 12),

('EUR-USD', 7,  'BUY',  'PENDING',   1.165, 100),
('GBP-USD', 8,  'BUY',  'FULFILLED', 1.348, 50),
('USD-JPY', 9,  'BUY',  'PENDING',   148.50, 25),

('AMZN', 10, 'BUY',  'FULFILLED', 231.50, 7);
