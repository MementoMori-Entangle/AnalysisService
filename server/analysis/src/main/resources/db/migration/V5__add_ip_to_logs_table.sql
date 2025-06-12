-- ログテーブルにIPアドレスカラム追加
ALTER TABLE logs ADD COLUMN ip_address VARCHAR(64) NOT NULL DEFAULT '' AFTER action;
