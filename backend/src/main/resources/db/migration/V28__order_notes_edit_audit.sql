-- V8: Add edit-audit columns to order_notes
--
-- original_note : preserved on first edit (immutable after that); NULL = never edited
-- edited_at     : timestamp of the most recent edit; NULL = never edited
--
-- Design rationale: we keep original_note frozen at the first edit so the audit
-- trail always answers "what was the note originally?" regardless of how many
-- times it was subsequently updated.

ALTER TABLE order_notes
    ADD COLUMN original_note TEXT         NULL COMMENT 'Preserved original text on first edit; NULL if never edited',
    ADD COLUMN edited_at     DATETIME(6)  NULL COMMENT 'Timestamp of most recent edit; NULL if never edited';