-- Optional demo data. Run after the app has started once (so Hibernate has created the tables).
-- Passwords below are the BCrypt hash of "password123".

INSERT INTO users (name, email, password, role, department, ward, created_at) VALUES
('Asha Rao', 'asha@example.com', '$2a$10$gt2SvC3SJWKIcVZhg9PjX.exUTApQXdSLcpHLc4nc2dvdSrcuNJ6y', 'CITIZEN', NULL, NULL, NOW()),
('R. Naidu', 'naidu@example.com', '$2a$10$gt2SvC3SJWKIcVZhg9PjX.exUTApQXdSLcpHLc4nc2dvdSrcuNJ6y', 'OFFICER', 'Public Works', 'Ward 7', NOW()),
('M. Iyer', 'admin@example.com', '$2a$10$gt2SvC3SJWKIcVZhg9PjX.exUTApQXdSLcpHLc4nc2dvdSrcuNJ6y', 'ADMIN', NULL, NULL, NOW());
