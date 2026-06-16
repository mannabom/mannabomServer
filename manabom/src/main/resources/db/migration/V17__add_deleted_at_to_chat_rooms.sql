ALTER TABLE public.chat_rooms
ADD COLUMN IF NOT EXISTS deleted_at timestamptz(6) NULL;