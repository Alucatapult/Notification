-- Create extensions
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- Create schema
CREATE SCHEMA IF NOT EXISTS notification;

-- Create additional user with limited privileges
CREATE USER app_user WITH ENCRYPTED PASSWORD 'app_password';
GRANT CONNECT ON DATABASE notification_db TO app_user;
GRANT USAGE ON SCHEMA notification TO app_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA notification TO app_user;
GRANT USAGE ON ALL SEQUENCES IN SCHEMA notification TO app_user;

-- Alter default privileges for future tables
ALTER DEFAULT PRIVILEGES IN SCHEMA notification
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO app_user;

ALTER DEFAULT PRIVILEGES IN SCHEMA notification
GRANT USAGE ON SEQUENCES TO app_user;

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_notifications_recipient ON notifications(recipient);
CREATE INDEX IF NOT EXISTS idx_notifications_status ON notifications(status);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at);

-- Create audit logging table
CREATE TABLE IF NOT EXISTS notification_audit_log (
    id SERIAL PRIMARY KEY,
    event_id VARCHAR(50) NOT NULL,
    username VARCHAR(255) NOT NULL,
    action VARCHAR(50) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id VARCHAR(50) NOT NULL,
    details JSONB,
    timestamp TIMESTAMP NOT NULL
);

-- Create index on audit table
CREATE INDEX IF NOT EXISTS idx_audit_log_timestamp ON notification_audit_log(timestamp);
CREATE INDEX IF NOT EXISTS idx_audit_log_username ON notification_audit_log(username);
CREATE INDEX IF NOT EXISTS idx_audit_log_action ON notification_audit_log(action);

-- Add database parameters for better performance
ALTER SYSTEM SET shared_buffers = '256MB';
ALTER SYSTEM SET work_mem = '16MB';
ALTER SYSTEM SET maintenance_work_mem = '128MB';
ALTER SYSTEM SET random_page_cost = 1.1;
ALTER SYSTEM SET effective_cache_size = '1GB';
ALTER SYSTEM SET default_statistics_target = 100;
ALTER SYSTEM SET autovacuum = on;

-- Grant limited privileges to prometheus monitoring user
CREATE USER prometheus WITH PASSWORD 'prometheus_password';
GRANT pg_monitor TO prometheus;

-- Setup database level permissions
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON DATABASE notification_db FROM PUBLIC; 