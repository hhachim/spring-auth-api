# Inscription d'un utilisateur
curl -X POST http://localhost:8086/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@example.com","password":"password123","roles":["admin","user"]}'

# Connexion et récupération du token JWT
curl -X POST http://localhost:8086/api/auth/signin \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123"}'

# Accès à un endpoint protégé avec le token JWT
curl -X GET http://localhost:8086/api/users \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0dXNlciIsImlhdCI6MTc0MTEyNzc2OCwiZXhwIjoxNzQxMjE0MTY4fQ.qmJ_MSfdxq5BhlguAl9BR_DHevqFC6XZtcJWHpMd--4"

# Refresh
curl -X POST http://localhost:8086/api/auth/refresh \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0dXNlciIsImlhdCI6MTc0MTEyNzc2OCwiZXhwIjoxNzQxMjE0MTY4fQ.qmJ_MSfdxq5BhlguAl9BR_DHevqFC6XZtcJWHpMd--4"
{"token":"eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0dXNlciIsImlhdCI6MTc0MTEzMDQ0MCwiZXhwIjoxNzQxMjE2ODQwfQ.VTXBhGSa_TU6MV6jo5vXqo4ARpYfcAy25JJyN0VmbOc","id":4,"username":"testuser","email":"test@example.com","roles":["ROLE_ADMIN","ROLE_USER"],"type":"Bearer"}%

Points importants à noter :

Le AuthController a trois endpoints :

/api/auth/signin pour la connexion
/api/auth/signup pour l'inscription
/api/auth/refresh pour rafraîchir le token JWT


Le UserController a quatre endpoints :

/api/users pour lister tous les utilisateurs (admin uniquement)
/api/users/{id} pour obtenir un utilisateur par ID
/api/users/me pour obtenir l'utilisateur courant
/api/users/{id} (DELETE) pour supprimer un utilisateur (admin uniquement)


La sécurité est gérée par les annotations @PreAuthorize qui permettent de spécifier quels rôles peuvent accéder à chaque endpoint.
L'initialisateur de données (DataInitializer) s'exécute au démarrage et crée les rôles de base s'ils n'existent pas déjà.