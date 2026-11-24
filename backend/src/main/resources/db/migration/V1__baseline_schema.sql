CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE users (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email         VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name    VARCHAR(80)  NOT NULL,
    last_name     VARCHAR(80)  NOT NULL,
    phone         VARCHAR(30),
    city          VARCHAR(80)  NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('OWNER', 'PROVIDER', 'ADMIN')),
    CONSTRAINT ck_users_email_lowercase CHECK (email = lower(email))
);

CREATE TABLE pets (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    owner_id   BIGINT        NOT NULL,
    name       VARCHAR(60)   NOT NULL,
    species    VARCHAR(20)   NOT NULL,
    breed      VARCHAR(80),
    birth_date DATE,
    weight_kg  NUMERIC(5, 2),
    care_notes VARCHAR(1000),
    archived   BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT fk_pets_owner_id FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT ck_pets_species CHECK (species IN ('DOG', 'CAT', 'BIRD', 'RABBIT', 'REPTILE', 'OTHER')),
    CONSTRAINT ck_pets_weight CHECK (weight_kg IS NULL OR weight_kg > 0)
);
CREATE INDEX ix_pets_owner_active ON pets (owner_id) WHERE archived = FALSE;

CREATE TABLE service_categories (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code        VARCHAR(30)  NOT NULL,
    name        VARCHAR(60)  NOT NULL,
    description VARCHAR(300) NOT NULL,
    sort_order  INTEGER      NOT NULL,
    CONSTRAINT uq_service_categories_code UNIQUE (code),
    CONSTRAINT ck_service_categories_code CHECK (code IN ('DAYCARE', 'GROOMING', 'VETERINARY', 'WALKING', 'TRAINING'))
);

INSERT INTO service_categories (code, name, description, sort_order) VALUES
    ('DAYCARE',    'Daycare',    'Supervised care and play while you are at work or away for the day.', 1),
    ('GROOMING',   'Grooming',   'Bathing, brushing, trimming, nail clipping and coat care.',            2),
    ('VETERINARY', 'Veterinary', 'Check-ups, vaccinations and advice from licensed veterinarians.',     3),
    ('WALKING',    'Walking',    'Regular walks and outdoor exercise, alone or in small groups.',        4),
    ('TRAINING',   'Training',   'Obedience, behaviour and socialisation sessions with a trainer.',      5);

CREATE TABLE provider_profiles (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id             BIGINT        NOT NULL,
    display_name        VARCHAR(120)  NOT NULL,
    bio                 VARCHAR(2000),
    city                VARCHAR(80)   NOT NULL,
    address             VARCHAR(200),
    years_experience    INTEGER       NOT NULL DEFAULT 0,
    verification_status VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    verification_note   VARCHAR(500),
    submitted_at        TIMESTAMPTZ,
    verified_at         TIMESTAMPTZ,
    rating_average      NUMERIC(3, 2) NOT NULL DEFAULT 0,
    review_count        INTEGER       NOT NULL DEFAULT 0,
    version             BIGINT        NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT fk_provider_profiles_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uq_provider_profiles_user_id UNIQUE (user_id),
    CONSTRAINT ck_provider_profiles_status CHECK (verification_status IN ('DRAFT', 'PENDING', 'VERIFIED', 'REJECTED')),
    CONSTRAINT ck_provider_profiles_experience CHECK (years_experience BETWEEN 0 AND 60),
    CONSTRAINT ck_provider_profiles_rating CHECK (rating_average BETWEEN 0 AND 5 AND review_count >= 0)
);
CREATE INDEX ix_provider_profiles_status_city ON provider_profiles (verification_status, lower(city));

CREATE TABLE service_offerings (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    provider_id      BIGINT         NOT NULL,
    category_id      BIGINT         NOT NULL,
    title            VARCHAR(120)   NOT NULL,
    description      VARCHAR(1000),
    price            NUMERIC(10, 2) NOT NULL,
    duration_minutes INTEGER        NOT NULL,
    active           BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT fk_service_offerings_provider_id FOREIGN KEY (provider_id) REFERENCES provider_profiles (id),
    CONSTRAINT fk_service_offerings_category_id FOREIGN KEY (category_id) REFERENCES service_categories (id),
    CONSTRAINT ck_service_offerings_price CHECK (price > 0),
    CONSTRAINT ck_service_offerings_duration CHECK (duration_minutes BETWEEN 15 AND 480 AND duration_minutes % 15 = 0)
);
CREATE INDEX ix_service_offerings_provider ON service_offerings (provider_id);
CREATE INDEX ix_service_offerings_category_active ON service_offerings (category_id, price) WHERE active;

CREATE TABLE availability_rules (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    provider_id BIGINT      NOT NULL,
    day_of_week INTEGER     NOT NULL,
    start_time  TIME        NOT NULL,
    end_time    TIME        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_availability_rules_provider_id FOREIGN KEY (provider_id) REFERENCES provider_profiles (id) ON DELETE CASCADE,
    CONSTRAINT ck_availability_rules_day CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT ck_availability_rules_window CHECK (end_time > start_time)
);
CREATE INDEX ix_availability_rules_provider_day ON availability_rules (provider_id, day_of_week);

CREATE TABLE bookings (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    owner_id    BIGINT         NOT NULL,
    pet_id      BIGINT         NOT NULL,
    provider_id BIGINT         NOT NULL,
    offering_id BIGINT         NOT NULL,
    start_at    TIMESTAMPTZ    NOT NULL,
    end_at      TIMESTAMPTZ    NOT NULL,
    status      VARCHAR(20)    NOT NULL,
    price       NUMERIC(10, 2) NOT NULL,
    owner_notes VARCHAR(500),
    reviewed    BOOLEAN        NOT NULL DEFAULT FALSE,
    version     BIGINT         NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT fk_bookings_owner_id FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT fk_bookings_pet_id FOREIGN KEY (pet_id) REFERENCES pets (id),
    CONSTRAINT fk_bookings_provider_id FOREIGN KEY (provider_id) REFERENCES provider_profiles (id),
    CONSTRAINT fk_bookings_offering_id FOREIGN KEY (offering_id) REFERENCES service_offerings (id),
    CONSTRAINT ck_bookings_status CHECK (status IN ('REQUESTED', 'CONFIRMED', 'DECLINED', 'CANCELLED', 'COMPLETED')),
    CONSTRAINT ck_bookings_time CHECK (end_at > start_at),
    CONSTRAINT ck_bookings_price CHECK (price > 0),
    CONSTRAINT ex_bookings_provider_no_overlap EXCLUDE USING gist (
        provider_id WITH =,
        tstzrange(start_at, end_at, '[)') WITH &&
    ) WHERE (status IN ('REQUESTED', 'CONFIRMED'))
);
CREATE INDEX ix_bookings_owner_start ON bookings (owner_id, start_at DESC);
CREATE INDEX ix_bookings_provider_start ON bookings (provider_id, start_at DESC);
CREATE INDEX ix_bookings_pet_start ON bookings (pet_id, start_at);
CREATE INDEX ix_bookings_requested_start ON bookings (start_at) WHERE status = 'REQUESTED';

CREATE TABLE booking_events (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    booking_id    BIGINT       NOT NULL,
    from_status   VARCHAR(20),
    to_status     VARCHAR(20)  NOT NULL,
    actor_user_id BIGINT,
    note          VARCHAR(500),
    occurred_at   TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_booking_events_booking_id FOREIGN KEY (booking_id) REFERENCES bookings (id) ON DELETE CASCADE,
    CONSTRAINT fk_booking_events_actor_user_id FOREIGN KEY (actor_user_id) REFERENCES users (id),
    CONSTRAINT ck_booking_events_from CHECK (from_status IS NULL OR from_status IN ('REQUESTED', 'CONFIRMED', 'DECLINED', 'CANCELLED', 'COMPLETED')),
    CONSTRAINT ck_booking_events_to CHECK (to_status IN ('REQUESTED', 'CONFIRMED', 'DECLINED', 'CANCELLED', 'COMPLETED'))
);
CREATE INDEX ix_booking_events_booking ON booking_events (booking_id, occurred_at);

CREATE TABLE reviews (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    booking_id  BIGINT        NOT NULL,
    provider_id BIGINT        NOT NULL,
    author_id   BIGINT        NOT NULL,
    rating      INTEGER       NOT NULL,
    comment     VARCHAR(1000),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT fk_reviews_booking_id FOREIGN KEY (booking_id) REFERENCES bookings (id),
    CONSTRAINT fk_reviews_provider_id FOREIGN KEY (provider_id) REFERENCES provider_profiles (id),
    CONSTRAINT fk_reviews_author_id FOREIGN KEY (author_id) REFERENCES users (id),
    CONSTRAINT uq_reviews_booking_id UNIQUE (booking_id),
    CONSTRAINT ck_reviews_rating CHECK (rating BETWEEN 1 AND 5)
);
CREATE INDEX ix_reviews_provider_created ON reviews (provider_id, created_at DESC);

CREATE TABLE notifications (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    type       VARCHAR(40)  NOT NULL,
    message    VARCHAR(300) NOT NULL,
    link       VARCHAR(200),
    read_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_notifications_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_notifications_type CHECK (type IN (
        'BOOKING_REQUESTED', 'BOOKING_CONFIRMED', 'BOOKING_DECLINED', 'BOOKING_CANCELLED', 'BOOKING_EXPIRED',
        'BOOKING_COMPLETED', 'PROVIDER_VERIFIED', 'PROVIDER_REJECTED', 'REVIEW_RECEIVED'))
);
CREATE INDEX ix_notifications_user_created ON notifications (user_id, created_at DESC);
CREATE INDEX ix_notifications_user_unread ON notifications (user_id) WHERE read_at IS NULL;
