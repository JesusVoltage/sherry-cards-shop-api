-- El propietario administra la tienda y es quien puede entrar mientras está cerrada al público.
UPDATE usuarios
SET rol_id = (SELECT id FROM roles WHERE code = 'ADMIN')
WHERE username = 'jesus';
