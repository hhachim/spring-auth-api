# Documentation de la fonctionnalité de réinitialisation de mot de passe

Cette documentation détaille le processus complet de réinitialisation de mot de passe implémenté dans l'API d'authentification JWT.

## Vue d'ensemble du processus

Le processus de réinitialisation de mot de passe se déroule en plusieurs étapes :

1. L'utilisateur demande une réinitialisation de mot de passe en fournissant son adresse email
2. Le système génère un token unique et l'envoie par email à l'utilisateur
3. L'utilisateur clique sur le lien dans l'email et est dirigé vers un formulaire de réinitialisation
4. L'utilisateur entre son nouveau mot de passe
5. Le système valide le token, met à jour le mot de passe et confirme à l'utilisateur

## Architecture

L'implémentation prend en charge deux modes de fonctionnement :

- **Mode API**: Pour une utilisation avec un frontend séparé (React, Angular, Vue)
- **Mode Intégré**: Pour une utilisation sans frontend séparé (pages servies directement par Spring Boot)

Le mode est déterminé automatiquement selon la configuration `app.frontend-url` dans `application.yml`.

## Endpoints et URLs

### Endpoints API REST

| Méthode | URL | Description | Paramètres |
|---------|-----|-------------|------------|
| POST | `/api/auth/forgot-password` | Demande de réinitialisation | `{ "email": "user@example.com" }` |
| POST | `/api/auth/reset-password` | Réinitialisation du mot de passe | `{ "token": "uuid-token", "newPassword": "nouveauMdp" }` |

### URLs des pages web (mode intégré)

| URL | Description |
|-----|-------------|
| `/forgot-password` | Page pour demander une réinitialisation |
| `/reset-password?token=uuid-token` | Page pour réinitialiser le mot de passe |
| `/reset-password-success` | Page de confirmation de réinitialisation |
| `/login` | Page de connexion |

## Flux détaillé du processus

### 1. Demande de réinitialisation

**Mode API**:
```http
POST /api/auth/forgot-password
Content-Type: application/json

{
  "email": "user@example.com"
}
```

**Réponse**:
```json
{
  "message": "Un e-mail de réinitialisation de mot de passe a été envoyé à l'adresse fournie."
}
```

**Mode Intégré**:
- L'utilisateur visite l'URL `/forgot-password`
- Il entre son email dans le formulaire
- La soumission du formulaire appelle l'API backend

### 2. Génération et envoi du token

Le système :
1. Vérifie si l'email existe dans la base de données
2. Génère un token unique (UUID)
3. Stocke le token dans la base de données avec une date d'expiration (24h)
4. Envoie un email contenant un lien avec le token

L'email contient un lien formaté comme suit :
- En mode API : `http://frontend-url/reset-password?token=uuid-token`
- En mode Intégré : `http://api-url/reset-password?token=uuid-token`

### 3. Accès à la page de réinitialisation

Lorsque l'utilisateur clique sur le lien dans l'email :

**Mode API**:
- Le frontend intercepte l'URL et affiche sa propre page de formulaire
- Aucune validation de token à ce stade

**Mode Intégré**:
- Le contrôleur `PasswordResetController` intercepte la requête
- Il valide préalablement si le token existe et n'est pas expiré
- Si le token est valide, il affiche le formulaire de réinitialisation
- Sinon, il redirige vers la page d'erreur

### 4. Soumission du nouveau mot de passe

**Mode API**:
```http
POST /api/auth/reset-password
Content-Type: application/json

{
  "token": "uuid-token",
  "newPassword": "nouveauMotDePasse"
}
```

**Réponse**:
```json
{
  "message": "Mot de passe réinitialisé avec succès!"
}
```

**Mode Intégré**:
- L'utilisateur soumet le formulaire HTML avec son nouveau mot de passe
- Le système effectue une vérification côté client pour s'assurer que les deux champs correspondent
- Le contrôleur traite la requête et appelle le service de réinitialisation

### 5. Validation et mise à jour

Dans les deux modes, le système :
1. Vérifie si le token existe dans la base de données
2. Vérifie si le token n'a pas expiré
3. Récupère l'utilisateur associé au token
4. Encode le nouveau mot de passe
5. Met à jour le mot de passe de l'utilisateur
6. Supprime le token (pour éviter sa réutilisation)
7. Envoie un email de confirmation à l'utilisateur

### 6. Confirmation

**Mode API**: L'API renvoie une réponse de succès que le frontend peut utiliser pour rediriger l'utilisateur vers la page de connexion.

**Mode Intégré**: L'utilisateur est redirigé vers la page `/reset-password-success` qui affiche un message de confirmation et un lien vers la page de connexion.

## Configuration

### Configuration principale (application.yml)

```yaml
# Configuration pour l'application
app:
  # URL de base du frontend (pour les liens dans les emails)
  frontend-url: http://localhost:3000  # Utiliser l'URL du backend pour le mode intégré
  # Expéditeur des emails
  email-from: noreply@votredomaine.com
  # Nom de l'expéditeur
  email-from-name: "Votre Application"

# Configuration SMTP pour l'envoi d'emails
spring:
  mail:
    host: smtp.example.com
    port: 587
    username: votre-email@example.com
    password: votre-mot-de-passe
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

## Modèle de données

Dans l'entité `User`, les champs suivants ont été ajoutés :

```java
@Column(name = "reset_password_token")
private String resetPasswordToken;

@Column(name = "reset_password_token_expiry")
private Date resetPasswordTokenExpiry;
```

## Sécurité

Le processus inclut plusieurs mesures de sécurité :
1. Les tokens expirent après 24 heures
2. Les tokens sont uniques (UUID)
3. Les tokens sont à usage unique (supprimés après utilisation)
4. La réponse API ne révèle pas si un email existe ou non dans le système
5. Les mots de passe sont hashés avant stockage
6. Les pages de réinitialisation sont protégées contre les CSRF

## Exemples d'utilisation

### Exemple 1: Réinitialisation via API (pour frontend)

```javascript
// 1. Demande de réinitialisation
async function requestPasswordReset(email) {
  const response = await fetch('/api/auth/forgot-password', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email })
  });
  return response.json();
}

// 2. Réinitialisation avec token et nouveau mot de passe
async function resetPassword(token, newPassword) {
  const response = await fetch('/api/auth/reset-password', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ token, newPassword })
  });
  return response.json();
}
```

### Exemple 2: Réinitialisation via interface intégrée

1. Accédez à `/forgot-password`
2. Entrez votre email et soumettez le formulaire
3. Ouvrez l'email et cliquez sur le lien fourni
4. Entrez votre nouveau mot de passe dans le formulaire
5. Après confirmation, vous serez redirigé vers la page de succès

## Dépannage

| Problème | Cause possible | Solution |
|----------|----------------|----------|
| Email non reçu | Configuration SMTP incorrecte | Vérifier les paramètres SMTP dans application.yml |
| Email non reçu | Email bloqué par spam | Vérifier dossier spam/indésirables |
| "Token invalide" | Token expiré | Demander une nouvelle réinitialisation |
| "Token invalide" | Token déjà utilisé | Demander une nouvelle réinitialisation |
| Erreur 401 | URL non autorisée | Vérifier la configuration de sécurité |

## Personnalisation

### Templates d'emails

Les templates d'emails sont situés dans :
- `src/main/resources/templates/email/reset-password-email.html` (demande)
- `src/main/resources/templates/email/reset-password-confirmation.html` (confirmation)

### Pages web

Les templates des pages web sont situés dans :
- `src/main/resources/templates/web/forgot-password.html`
- `src/main/resources/templates/web/reset-password-form.html`
- `src/main/resources/templates/web/reset-password-success.html`
- `src/main/resources/templates/web/error.html`
- `src/main/resources/templates/web/login.html`