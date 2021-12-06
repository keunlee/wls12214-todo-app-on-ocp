DROP TABLE IF EXISTS public.todo;

CREATE TABLE IF NOT EXISTS public.todo
(
    id BIGSERIAL,
    completed boolean,
    ordering SERIAL,
    title character varying(255) COLLATE pg_catalog."default",
    CONSTRAINT todo_pkey PRIMARY KEY (id)
    )

    TABLESPACE pg_default;

ALTER TABLE IF EXISTS public.todo
    OWNER to postgres;