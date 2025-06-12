-- サービス情報テーブル作成
CREATE TABLE IF NOT EXISTS service_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    analysis_type VARCHAR(255) NOT NULL,
    analysis_name VARCHAR(255) NOT NULL,
    data_process_info_json TEXT NOT NULL
);
