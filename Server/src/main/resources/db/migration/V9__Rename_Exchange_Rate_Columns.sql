-- V9: Rename columns in exchange_rates to match Java Entity
ALTER TABLE exchange_rates RENAME COLUMN base_currency TO from_currency;
ALTER TABLE exchange_rates RENAME COLUMN target_currency TO to_currency;
