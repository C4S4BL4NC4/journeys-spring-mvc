create table users
(
    id                     varchar(36)  not null,
    name                   varchar(50)  not null,
    email                  varchar(254) not null,
    photo                  varchar(255) not null default 'default.jpg',
    role                   varchar(20)  not null default 'USER',
    password               varchar(100) not null,
    password_changed_at    timestamp(6),
    password_reset_token   varchar(64),
    password_reset_expires timestamp(6),
    active                 boolean      not null default true,
    created_at             timestamp(6),
    last_modified_at       timestamp(6),
    version                integer,
    primary key (id),
    constraint uk_users_email unique (email),
    constraint ck_users_role check (role in ('USER', 'GUIDE', 'LEAD_GUIDE', 'ADMIN'))
);

create index ix_users_password_reset_token on users (password_reset_token);

create table tours
(
    id                         varchar(36)    not null,
    name                       varchar(50)    not null,
    slug                       varchar(60)    not null,
    duration                   integer        not null,
    max_group_size             integer        not null,
    difficulty                 varchar(20)    not null,
    average_rating             double precision,
    rating_quantity            integer        not null default 0,
    price                      numeric(10, 2) not null,
    discount                   numeric(10, 2),
    summary                    varchar(255)   not null,
    description                varchar(2000),
    image_cover                varchar(255)   not null,
    secret_tour                boolean        not null default false,
    start_location_longitude   double precision,
    start_location_latitude    double precision,
    start_location_address     varchar(255),
    start_location_description varchar(255),
    start_location_day_number  integer,
    created_at                 timestamp(6),
    last_modified_at           timestamp(6),
    version                    integer,
    primary key (id),
    constraint uk_tour_slug unique (slug),
    constraint ck_tour_duration check (duration > 0),
    constraint ck_tour_max_group_size check (max_group_size > 0),
    constraint ck_tour_difficulty check (difficulty in ('EASY', 'MEDIUM', 'DIFFICULT')),
    constraint ck_tour_average_rating check (average_rating between 1.0 and 5.0),
    constraint ck_tour_rating_quantity check (rating_quantity >= 0),
    constraint ck_tour_price check (price > 0),
    constraint ck_tour_discount check (discount >= 0 and discount < price),
    constraint ck_tour_start_location check (
        (start_location_longitude is null and start_location_latitude is null)
        or (start_location_longitude between -180 and 180 and start_location_latitude between -90 and 90)
    )
);

create index ix_tours_price_average_rating on tours (price, average_rating);

create table tour_images
(
    tour_id     varchar(36)  not null,
    image_order integer      not null,
    image       varchar(255) not null,
    primary key (tour_id, image_order),
    constraint fk_tour_images_tour foreign key (tour_id) references tours (id) on delete cascade
);

create table tour_start_dates
(
    tour_id    varchar(36)  not null,
    start_date timestamp(6) not null,
    primary key (tour_id, start_date),
    constraint fk_tour_start_dates_tour foreign key (tour_id) references tours (id) on delete cascade
);

create table tour_locations
(
    tour_id        varchar(36)      not null,
    location_order integer          not null,
    longitude      double precision not null,
    latitude       double precision not null,
    address        varchar(255),
    description    varchar(255),
    day_number     integer,
    primary key (tour_id, location_order),
    constraint fk_tour_locations_tour foreign key (tour_id) references tours (id) on delete cascade,
    constraint ck_tour_locations_coordinates check (longitude between -180 and 180 and latitude between -90 and 90),
    constraint ck_tour_locations_day check (day_number > 0)
);

create table tour_guides
(
    tour_id varchar(36) not null,
    user_id varchar(36) not null,
    primary key (tour_id, user_id),
    constraint fk_tour_guides_tour foreign key (tour_id) references tours (id) on delete cascade,
    constraint fk_tour_guides_user foreign key (user_id) references users (id)
);

create index ix_tour_guides_user on tour_guides (user_id);
