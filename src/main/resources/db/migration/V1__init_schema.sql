CREATE TABLE IF NOT EXISTS niche (
    id           UUID PRIMARY KEY,
    keyword      VARCHAR(255),
    created_at   TIMESTAMP(6) WITH TIME ZONE,
    last_used_at TIMESTAMP(6) WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS image_style (
    id            UUID PRIMARY KEY,
    name          VARCHAR(255) NOT NULL UNIQUE,
    system_prompt VARCHAR(2000),
    active        BOOLEAN NOT NULL
);

CREATE TABLE IF NOT EXISTS image_prompt (
    id             UUID PRIMARY KEY,
    niche_id       UUID REFERENCES niche (id),
    style_id       UUID REFERENCES image_style (id),
    value          VARCHAR(1500),
    negative_value VARCHAR(500),
    title          VARCHAR(255),
    main_tag       VARCHAR(255),
    description    VARCHAR(1000),
    created_at     TIMESTAMP(6) WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS image_prompt_supporting_tags (
    image_prompt_id UUID NOT NULL REFERENCES image_prompt (id),
    supporting_tags VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS design (
    id         UUID PRIMARY KEY,
    prompt_id  UUID REFERENCES image_prompt (id),
    image_path VARCHAR(255),
    created_at TIMESTAMP(6) WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS processed_image (
    id            UUID PRIMARY KEY,
    design_id     UUID REFERENCES design (id),
    type          VARCHAR(32),
    status        VARCHAR(32),
    result_url    VARCHAR(255),
    error_message VARCHAR(255),
    created_at    TIMESTAMP(6) WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS design_processed_images (
    design_id           UUID NOT NULL REFERENCES design (id),
    processed_images_id UUID NOT NULL UNIQUE REFERENCES processed_image (id)
);

CREATE TABLE IF NOT EXISTS generation_task (
    id               UUID PRIMARY KEY,
    raw_trend        VARCHAR(255),
    filtering_needed BOOLEAN NOT NULL,
    status           VARCHAR(32),
    created_at       TIMESTAMP(6) WITH TIME ZONE,
    finished_at      TIMESTAMP(6) WITH TIME ZONE,
    error_message    TEXT
);

CREATE TABLE IF NOT EXISTS generation_task_style_ids (
    generation_task_id UUID NOT NULL REFERENCES generation_task (id),
    style_ids          UUID
);

CREATE TABLE IF NOT EXISTS generation_subtask (
    id                 UUID PRIMARY KEY,
    generation_task_id UUID,
    status             VARCHAR(32),
    niche_id           UUID,
    image_style_id     UUID,
    image_prompt_id    UUID,
    design_id          UUID,
    error_message      TEXT,
    created_at         TIMESTAMP(6) WITH TIME ZONE,
    finished_at        TIMESTAMP(6) WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_image_prompt_niche_id         ON image_prompt (niche_id);
CREATE INDEX IF NOT EXISTS idx_image_prompt_style_id         ON image_prompt (style_id);
CREATE INDEX IF NOT EXISTS idx_design_prompt_id              ON design (prompt_id);
CREATE INDEX IF NOT EXISTS idx_processed_image_design_id     ON processed_image (design_id);
CREATE INDEX IF NOT EXISTS idx_generation_subtask_task_id    ON generation_subtask (generation_task_id);