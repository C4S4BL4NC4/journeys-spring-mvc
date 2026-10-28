create table tours
(
    id               varchar(36)    not null,
    name             varchar(50)    not null,
    slug             varchar(60)    not null,
    average_rating   double precision,
    rating_quantity  integer        not null default 0,
    price            numeric(10, 2) not null,
    discount         numeric(10, 2),
    created_at       timestamp(6),
    last_modified_at timestamp(6),
    version          integer,
    primary key (id),
    constraint uk_tour_slug unique (slug),
    constraint ck_tour_average_rating check (average_rating between 1.0 and 5.0),
    constraint ck_tour_rating_quantity check (rating_quantity >= 0),
    constraint ck_tour_price check (price > 0),
    constraint ck_tour_discount check (discount >= 0 and discount < price)
);

create table users
(
    id               varchar(36)  not null,
    name             varchar(50)  not null,
    email            varchar(254) not null,
    photo            varchar(255),
    role             varchar(20)  not null default 'USER',
    password         varchar(100) not null,
    created_at       timestamp(6),
    last_modified_at timestamp(6),
    version          integer,
    primary key (id),
    constraint uk_users_email unique (email),
    constraint ck_users_role check (role in ('USER', 'GUIDE', 'LEAD_GUIDE', 'ADMIN'))
);
