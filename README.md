Pour tester la Phase 1 (configuration initiale), vous pouvez effectuer quelques vérifications simples pour vous assurer que votre environnement Spring Boot est correctement configuré. Voici comment procéder :

## 1. Vérifier que l'application démarre correctement

La première étape est de s'assurer que l'application démarre sans erreur. Exécutez la commande :

```bash
mvn spring-boot:run
```

Une application qui démarre correctement devrait afficher quelque chose comme :
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.4.3)
```

Et se terminer par une ligne indiquant que l'application a démarré, comme :
```
Started AuthApiApplication in X.XXX seconds (process running for Y.YYY)
```

## 2. Vérifier l'accès à la console H2

Puisque vous avez configuré H2 dans votre application.yml, vous pouvez vérifier que la console H2 est accessible :

1. Démarrez l'application si ce n'est pas déjà fait
2. Ouvrez un navigateur et accédez à `http://localhost:8080/h2-console`
3. Dans le formulaire de connexion, assurez-vous que :
   - JDBC URL = `jdbc:h2:mem:authdb`
   - Username = `sa`
   - Password = `password`
4. Cliquez sur "Connect" et vérifiez que vous pouvez accéder à la console

## 3. Vérifier la configuration de sécurité de base

À ce stade, Spring Security est dans sa configuration par défaut et sécurise tous les endpoints. Vous pouvez le vérifier en essayant d'accéder à un endpoint quelconque, par exemple :

```bash
curl http://localhost:8080/api/any-path
```

Vous devriez obtenir une réponse 401 (Unauthorized) ou être redirigé vers une page de login, ce qui confirme que Spring Security est actif.

## 4. Créer un test simple

Vous pouvez créer un petit contrôleur de test temporaire pour vérifier que votre application répond :

Puis, modifiez temporairement votre configuration de sécurité pour permettre l'accès à certains endpoints de test :



Après avoir ajouté ces fichiers :

1. Redémarrez l'application
2. Testez l'endpoint public avec `curl http://localhost:8080/api/test/public`
   - Vous devriez obtenir "Contenu public"
3. Testez l'endpoint sécurisé avec `curl http://localhost:8080/api/test/secured`
   - Vous devriez recevoir une erreur d'authentification

## Conclusion

Si tous ces tests passent, cela signifie que :
- Votre application Spring Boot démarre correctement
- La base de données H2 est configurée et accessible
- Spring Security est correctement configuré
- Le serveur web est opérationnel et répond aux requêtes

Votre Phase 1 est alors validée, et vous pouvez passer à la Phase 2 pour implémenter les modèles de données.

Remarque : N'oubliez pas de supprimer ou de commenter le contrôleur de test et la configuration de sécurité temporaire une fois que vous êtes prêt à passer à la Phase 2, car vous les remplacerez par la configuration réelle de votre API d'authentification.