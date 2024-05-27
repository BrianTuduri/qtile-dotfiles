
<h2>🛠️ Steps to implements:</h2>

<p>1. Añadir dependencias: Agrega la dependencia de Spring Cloud Vault Config en tu archivo pom.xml:</p>

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-vault-config-consul</artifactId>
        <version>3.0.0-SNAPSHOT</version>
    </dependency>
</dependencies>
```

<p>2. Configurar Vault Agent en bootstrap.yml: Crea o edita el archivo bootstrap.yml en tu proyecto Spring y añade la siguiente configuración:</p>

```yaml
spring:
  cloud:
    vault:
      uri: http://127.0.0.1:8200
      token: @file:/path/to/token
      kv:
        enabled: true
        backend: secret
        profile-separator: "/"
```