-- image_division_info テーブル作成マイグレーション
CREATE TABLE image_division_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(64) NOT NULL UNIQUE,
    division_num INT NOT NULL,
    embed_meta_text LONGTEXT,
    embed_meta_base64 LONGTEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
