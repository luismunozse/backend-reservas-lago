-- Eliminar campo alergias ya que fue removido del proceso de reserva
ALTER TABLE reservations DROP COLUMN IF EXISTS allergies;
