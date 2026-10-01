-- V6 buscaba un username que no coincide con la cuenta real del propietario; el email es inequívoco.
UPDATE usuarios
SET rol_id = (SELECT id FROM roles WHERE code = 'ADMIN')
WHERE email = 'jesusreyesreal@gmail.com';
