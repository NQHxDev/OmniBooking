-- Drop the existing hard unique constraint
ALTER TABLE "public"."user_passkeys" DROP CONSTRAINT IF EXISTS "user_passkeys_credential_id_key";

-- Create a partial unique index that only applies to active (non-deleted) passkeys
CREATE UNIQUE INDEX "idx_user_passkeys_credential_id_active" ON "public"."user_passkeys" ("credential_id") 
WHERE "deleted_at" IS NULL;

-- Ensure all existing Hard Deletes were intentional, but from now on it will be Soft Delete via Hibernate
COMMENT ON COLUMN "public"."user_passkeys"."deleted_at" IS 'Timestamp of soft deletion';
