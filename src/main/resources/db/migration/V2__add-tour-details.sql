-- Existing rows get placeholder values so the required columns can be made not null
alter table tours add column duration integer not null default 1;
alter table tours add column max_group_size integer not null default 1;
alter table tours add column difficulty varchar(20) not null default 'MEDIUM';
alter table tours add column summary varchar(255) not null default '';
alter table tours add column description varchar(2000);
alter table tours add column image_cover varchar(255) not null default '';
alter table tours add column secret_tour boolean not null default false;
alter table tours add column start_location_longitude double precision;
alter table tours add column start_location_latitude double precision;
alter table tours add column start_location_address varchar(255);
alter table tours add column start_location_description varchar(255);
alter table tours add column start_location_day_number integer;

alter table tours alter column duration drop default;
alter table tours alter column max_group_size drop default;
alter table tours alter column difficulty drop default;
alter table tours alter column summary drop default;
alter table tours alter column image_cover drop default;

alter table tours add constraint ck_tour_duration check (duration > 0);
alter table tours add constraint ck_tour_max_group_size check (max_group_size > 0);
alter table tours add constraint ck_tour_difficulty check (difficulty in ('EASY', 'MEDIUM', 'DIFFICULT'));
alter table tours add constraint ck_tour_start_location check (
    (start_location_longitude is null and start_location_latitude is null)
    or (start_location_longitude between -180 and 180 and start_location_latitude between -90 and 90)
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
