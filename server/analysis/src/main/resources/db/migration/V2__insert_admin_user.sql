-- adminユーザーのパスワードは「admin」をBCryptでハッシュ化した例
INSERT INTO users (username, password_hash, role, created_at) VALUES (
  'admin',
  '$2a$10$E59kSgZES2fpXCmUyeNQVe3Z1Oslyxovbr/rcVGyKvTsorLVRBVze',
  'ADMIN',
  NOW()
);
