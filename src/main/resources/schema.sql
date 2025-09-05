-- 勤怠管理システムのデータベーススキーマ

-- データベースの作成（管理者権限で実行）
-- CREATE DATABASE kintai;

-- updated_atを自動更新するトリガー関数
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 部署テーブル
CREATE TABLE IF NOT EXISTS departments (
    department_id VARCHAR(50) PRIMARY KEY,
    department_name VARCHAR(100) NOT NULL,
    description TEXT,
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ユーザーテーブル
CREATE TABLE IF NOT EXISTS users (
    username VARCHAR(255) PRIMARY KEY,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    department_id VARCHAR(50),
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (department_id) REFERENCES departments(department_id)
);

-- 勤怠記録テーブル
CREATE TABLE IF NOT EXISTS attendance (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    check_in_time TIMESTAMP WITH TIME ZONE,
    check_out_time TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(username) ON DELETE CASCADE
);

-- 休暇申請テーブル
CREATE TABLE IF NOT EXISTS leave_requests (
    id SERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'rejected')),
    approved_by VARCHAR(255),
    approval_date TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE,
    FOREIGN KEY (approved_by) REFERENCES users(username) ON DELETE SET NULL
);

-- パスキー認証器テーブル
CREATE TABLE IF NOT EXISTS authenticators (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    credential_id BYTEA NOT NULL UNIQUE,
    attested_credential_data BYTEA NOT NULL,
    sign_count BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(username) ON DELETE CASCADE
);

-- インデックスの作成
CREATE INDEX IF NOT EXISTS idx_attendance_user_id ON attendance(user_id);
CREATE INDEX IF NOT EXISTS idx_attendance_check_in_time ON attendance(check_in_time);
CREATE INDEX IF NOT EXISTS idx_attendance_user_id_check_in ON attendance(user_id, check_in_time);
CREATE INDEX IF NOT EXISTS idx_users_department_id ON users(department_id);
CREATE INDEX IF NOT EXISTS idx_leave_requests_username ON leave_requests(username);
CREATE INDEX IF NOT EXISTS idx_leave_requests_status ON leave_requests(status);
CREATE INDEX IF NOT EXISTS idx_leave_requests_dates ON leave_requests(start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_authenticators_user_id ON authenticators(user_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_authenticators_credential_id ON authenticators(credential_id);

-- トリガーの作成
DROP TRIGGER IF EXISTS update_departments_updated_at ON departments;
CREATE TRIGGER update_departments_updated_at 
    BEFORE UPDATE ON departments 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_users_updated_at ON users;
CREATE TRIGGER update_users_updated_at 
    BEFORE UPDATE ON users 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_attendance_updated_at ON attendance;
CREATE TRIGGER update_attendance_updated_at 
    BEFORE UPDATE ON attendance 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_leave_requests_updated_at ON leave_requests;
CREATE TRIGGER update_leave_requests_updated_at 
    BEFORE UPDATE ON leave_requests 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_authenticators_updated_at ON authenticators;
CREATE TRIGGER update_authenticators_updated_at
    BEFORE UPDATE ON authenticators
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- サンプルデータの挿入

-- 部署データ
INSERT INTO departments (department_id, department_name, description, enabled) VALUES 
    ('HR', '人事部', '人事・労務管理を担当する部署', true),
    ('IT', 'IT部', 'システム開発・IT運用を担当する部署', true),
    ('SALES', '営業部', '営業・販売を担当する部署', true),
    ('FINANCE', '経理部', '経理・財務を担当する部署', true),
    ('OLD_DEPT', '旧部署', '廃止された部署（無効）', false)
ON CONFLICT (department_id) DO NOTHING;

-- ユーザーデータ
INSERT INTO users (username, password, role, department_id, enabled) VALUES 
    ('employee1', '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8', 'employee', 'IT', true),  -- password
    ('admin1', '713bfda78870bf9d1b261f565286f85e97ee614efe5f0faf7c34e7ca4f65baca', 'admin', 'HR', true),     -- adminpass
    ('employee2', '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8', 'employee', 'SALES', false)  -- password
ON CONFLICT (username) DO UPDATE SET
    department_id = EXCLUDED.department_id,
    enabled = EXCLUDED.enabled;
