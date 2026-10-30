-- Portfolios
INSERT INTO portfolio (id, name, base_currency) VALUES (1, 'UK Growth', 'GBP');
INSERT INTO portfolio (id, name, base_currency) VALUES (2, 'US Tech', 'USD');
INSERT INTO portfolio (id, name, base_currency) VALUES (3, 'Euro Income', 'EUR');

-- Prices (in the instrument's trading currency)
INSERT INTO price (ticker, price, currency) VALUES ('AAPL', 189.84, 'USD');
INSERT INTO price (ticker, price, currency) VALUES ('MSFT', 415.26, 'USD');
INSERT INTO price (ticker, price, currency) VALUES ('NVDA', 1180.05, 'USD');
INSERT INTO price (ticker, price, currency) VALUES ('HSBA', 7.0325, 'GBP');
INSERT INTO price (ticker, price, currency) VALUES ('SHEL', 28.615, 'GBP');
INSERT INTO price (ticker, price, currency) VALUES ('SAP', 182.14, 'EUR');
INSERT INTO price (ticker, price, currency) VALUES ('ASML', 862.30, 'EUR');

-- FX rates: 1 unit of base currency in USD
INSERT INTO fx_rate (base_currency, rate) VALUES ('GBP', 1.2715);
INSERT INTO fx_rate (base_currency, rate) VALUES ('EUR', 1.0845);

-- Holdings
INSERT INTO holding (portfolio_id, ticker, quantity) VALUES (1, 'HSBA', 12000);
INSERT INTO holding (portfolio_id, ticker, quantity) VALUES (1, 'SHEL', 1500);
INSERT INTO holding (portfolio_id, ticker, quantity) VALUES (1, 'AAPL', 250);
INSERT INTO holding (portfolio_id, ticker, quantity) VALUES (2, 'AAPL', 1000);
INSERT INTO holding (portfolio_id, ticker, quantity) VALUES (2, 'MSFT', 400);
INSERT INTO holding (portfolio_id, ticker, quantity) VALUES (2, 'NVDA', 75);
INSERT INTO holding (portfolio_id, ticker, quantity) VALUES (3, 'SAP', 300);
INSERT INTO holding (portfolio_id, ticker, quantity) VALUES (3, 'ASML', 40);
INSERT INTO holding (portfolio_id, ticker, quantity) VALUES (3, 'HSBA', 5000);
