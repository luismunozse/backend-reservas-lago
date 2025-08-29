create unique index if not exists ux_reservations_date_dni
on reservations (visit_date, dni);