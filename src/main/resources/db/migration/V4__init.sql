create table if not exists reservations (
  id uuid primary key,
  visit_date date not null,
  first_name varchar(100) not null,
  last_name varchar(100) not null,
  dni varchar(50) not null,
  phone varchar(50) not null,
  email varchar(255) not null,
  circuit VARCHAR(20) NOT NULL CHECK (circuit IN ('A','B','C','D')),
  visitor_type varchar(40) not null default 'INDIVIDUAL',
  institution_name varchar(255),
  institution_students int,
  adults_18_plus int not null,
  children_2_to_17 int not null,
  babies_less_than_2 int not null,
  reduced_mobility int not null,
  allergies int not null default 0,
  comment text,
  origin_location text,
  how_heard varchar(20) not null,
  accepted_policies boolean not null,
  status varchar(20) not null,
  created_at timestamp not null default now(),
  updated_at timestamp not null default now()
);

create table if not exists availability_rules (
  id bigserial primary key,
  day date not null unique,
  capacity int not null
);
