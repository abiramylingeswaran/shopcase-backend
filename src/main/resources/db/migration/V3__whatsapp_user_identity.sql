-- WhatsApp bot identity (Defect Tracker–style VERIFY link)
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS whatsapp_secret_code VARCHAR(32),
    ADD COLUMN IF NOT EXISTS whatsapp_id VARCHAR(128);

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_whatsapp_secret_code
    ON users (whatsapp_secret_code)
    WHERE whatsapp_secret_code IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_whatsapp_id
    ON users (whatsapp_id)
    WHERE whatsapp_id IS NOT NULL;
