-- Eliminar el índice único anterior que no excluía canceladas
DROP INDEX IF EXISTS ux_reservations_date_dni;

-- Crear un partial index que permite duplicados si la reserva anterior está cancelada
CREATE UNIQUE INDEX ux_reservations_date_dni
ON reservations (visit_date, dni)
WHERE status <> 'CANCELLED';
