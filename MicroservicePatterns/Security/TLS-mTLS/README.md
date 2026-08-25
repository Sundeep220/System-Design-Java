# TLS & mTLS — Deep Dive with Full Flows and Spring Boot Implementation

TLS is the cryptographic layer under every HTTPS connection. mTLS extends it so both sides prove identity — critical for microservice zero-trust security. This doc covers the full handshake flows, Spring Boot implementation from scratch, and exactly how Istio wires mTLS automatically.

> **TLS = "I (client) know I'm talking to the real server." mTLS = "I know I'm talking to the real server AND the server knows it's talking to me specifically." No passwords — just cryptographic certificates.**

> **Read `TLS/README.md` first for practical certificate management (Let's Encrypt, errors, debugging). This doc covers the cryptographic internals and full implementation.**

---

# 1. SSL vs TLS — History and Naming

```text
SSL (Secure Sockets Layer) — developed by Netscape:
  SSL 1.0 (1994): never released publicly (serious security flaws)
  SSL 2.0 (1995): released but had major flaws, deprecated 2011
  SSL 3.0 (1996): widely used, but had POODLE attack, deprecated 2015

TLS (Transport Layer Security) — successor to SSL, maintained by IETF:
  TLS 1.0 (1999): successor to SSL 3.0, deprecated 2021
  TLS 1.1 (2006): deprecated 2021
  TLS 1.2 (2008): still in widespread use today
  TLS 1.3 (2018): current standard — faster, more secure, simpler
```

**Nobody uses SSL anymore.** When someone says "SSL certificate" or "SSL/TLS" today, they mean TLS. The term "SSL" persists colloquially but the protocol is TLS.

```text
"Do you have an SSL certificate?" → They mean a TLS certificate (X.509 certificate)
"Enable SSL on your server"       → They mean configure TLS
"SSL encryption"                  → They mean TLS encryption
```

---

# 2. What Problem TLS Solves

Without TLS (plain HTTP):

```text
Client ────────── GET /login (username=alice, password=secret123) ──────── Server
         ↑
      Network packet visible to:
        - Your ISP
        - Coffee shop WiFi router
        - Any network hop between you and the server
        - Man-in-the-middle attacker
```

With TLS (HTTPS):

```text
Client ────── [ENCRYPTED GIBBERISH] ──────── Server
  Only endpoints can decrypt — intermediate nodes see nothing useful
```

TLS provides:
1. **Confidentiality**: data is encrypted — intermediaries cannot read it
2. **Integrity**: data cannot be modified in transit without detection (MAC)
3. **Authentication**: client verifies it's talking to the real server (via certificate)

---

# 3. Cryptography Foundations (Brief)

You need to understand two types of cryptography to understand TLS:

## Symmetric Encryption

```text
One key for both encryption and decryption:

Alice: encrypt("Hello", key=K) → "x9kL2p..."
Bob:   decrypt("x9kL2p...", key=K) → "Hello"

Fast (hardware-accelerated on modern CPUs)
Problem: how do both sides get the same key without an attacker seeing it?
         You can't send the key over an insecure channel!

Examples: AES-128, AES-256 (TLS uses these for the actual data)
```

## Asymmetric Encryption (Public-Key Cryptography)

```text
Two mathematically related keys:
  Private key: kept SECRET by the owner, never shared
  Public key:  shared openly with anyone

Encrypt with public key → only private key can decrypt
Sign with private key → public key can verify the signature

Slower (complex math operations)
No shared secret problem — public key can be freely distributed

Examples: RSA (2048-4096 bit), ECDSA, Ed25519
```

## How TLS Uses Both

```text
TLS handshake: asymmetric cryptography
  → Securely establish a shared symmetric key

TLS data transfer: symmetric encryption using that shared key
  → Fast encryption of all actual data

"The handshake exchanges keys, data uses those keys"
```

---

# 4. X.509 Certificates — The Identity Document

A TLS certificate is a digital document that proves identity. It's like a passport for a server.

```text
X.509 Certificate contains:
  Subject:    CN=api.example.com (who this cert is for)
  Issuer:     CN=DigiCert TLS RSA SHA256 CA (who signed it)
  Valid From: 2024-01-01
  Valid Until:2025-01-01
  Public Key: RSA 2048-bit [the server's public key]
  SAN:        api.example.com, www.example.com (Subject Alternative Names)
  Signature:  [CA's digital signature of all the above]
```

The certificate is signed by a **Certificate Authority (CA)** — a trusted third party.

## Certificate Authority Chain of Trust

```mermaid
graph TD
    RootCA["🔐 Root CA<br/>(DigiCert Global Root)<br/>Self-signed<br/>Pre-installed in browsers/OS<br/>Kept OFFLINE"]
    IntCA["🏛️ Intermediate CA<br/>(DigiCert TLS RSA SHA256 CA)<br/>Signed by Root CA<br/>Online — does daily signing"]
    ServerCert["📄 Server Certificate<br/>(api.example.com)<br/>Signed by Intermediate CA<br/>Installed on your server<br/>Contains server's public key"]

    RootCA -->|"signs"| IntCA
    IntCA -->|"signs"| ServerCert
```

When your browser connects to `api.example.com`:

```text
Step 1: Server sends its cert chain:
        [api.example.com cert] + [Intermediate CA cert]
        (Root CA not sent — browser already has it)

Step 2: Browser validates bottom-up:
        api.example.com cert → signed by Intermediate CA?   ✓
        Intermediate CA cert → signed by DigiCert Root CA?  ✓
        DigiCert Root CA → in my trust store?               ✓

Step 3: Browser checks extra conditions:
        Hostname matches SAN in cert?   ✓
        Cert not expired?               ✓
        Cert not revoked? (OCSP)        ✓

Step 4: Chain of trust established → TLS handshake continues
```

## Certificate File Formats

```text
.pem   → Base64-encoded, human-readable, most common
         -----BEGIN CERTIFICATE-----
         MIIDXTCCAkWgAwIBAgIJALh...
         -----END CERTIFICATE-----

.crt / .cer → usually PEM format, sometimes DER binary
.key         → private key file (NEVER share!)
               -----BEGIN PRIVATE KEY-----
               MIIEvgIBADANBgkqhkiG9w...
               -----END PRIVATE KEY-----

.p12 / .pfx → binary format containing cert + private key (password-protected)
              Used by: Windows, Java (PKCS12 format)

.jks         → Java KeyStore (Java-specific binary format, legacy)
              Replaced by .p12 in modern Java
```

---

# 5. TLS 1.2 Full Handshake

**Total cost: 2 RTTs before first byte of data** (1 TCP + 1 TLS)

```mermaid
sequenceDiagram
    participant C as Client (Browser/Service)
    participant S as Server

    Note over C,S: TCP Handshake first (SYN/SYN-ACK/ACK) — 1 RTT

    C->>S: ClientHello<br/>TLS 1.2, cipher suites list,<br/>client_random (32 bytes), session_id
    S->>C: ServerHello<br/>chosen cipher suite,<br/>server_random (32 bytes)
    S->>C: Certificate<br/>server cert chain (cert + intermediates)
    S->>C: ServerKeyExchange<br/>ECDH params + server signature<br/>(only for DHE/ECDHE ciphers)
    S->>C: ServerHelloDone

    Note over C: Verify cert chain<br/>Check hostname matches SAN<br/>Check not expired<br/>Verify CA signature

    C->>S: ClientKeyExchange<br/>client's ECDH public value
    Note over C,S: Both compute same pre-master secret<br/>master_secret = PRF(pre_master, client_random, server_random)<br/>Derive 4 keys: client/server write key + MAC key
    C->>S: ChangeCipherSpec (switching to encrypted)
    C->>S: Finished (HMAC of all handshake messages, encrypted)
    S->>C: ChangeCipherSpec
    S->>C: Finished (HMAC of all handshake messages, encrypted)

    Note over C,S: ✅ Handshake complete — 2 RTTs used
    C->>S: GET /api/users HTTP/1.1 (AES-GCM encrypted)
    S->>C: HTTP/1.1 200 OK (AES-GCM encrypted)
```

## TLS 1.3 Full Handshake — 1 RTT

The key innovation: **client sends its ECDH key share in the very first message**, so both sides can compute session keys after the first round trip.

```mermaid
sequenceDiagram
    participant C as Client
    participant S as Server

    Note over C,S: TCP Handshake — 1 RTT

    C->>S: ClientHello<br/>TLS 1.3, cipher suites,<br/>client_random,<br/>key_share (ECDH public value) ⬅ KEY CHANGE

    S->>C: ServerHello<br/>server_random, chosen cipher,<br/>key_share (server's ECDH public value)

    Note over C,S: 🔑 Both sides derive session keys NOW<br/>No more round trips needed for key exchange!

    S->>C: EncryptedExtensions 🔒 already encrypted!
    S->>C: Certificate 🔒 already encrypted!
    S->>C: CertificateVerify 🔒 (proves server has private key)
    S->>C: Finished 🔒

    Note over C: Verify cert (encrypted so less metadata leaks)<br/>Derive final session keys

    C->>S: Finished 🔒

    Note over C,S: ✅ Handshake complete — 1 RTT used
    C->>S: GET /api/users (encrypted)
    S->>C: 200 OK (encrypted)
```

### 0-RTT Session Resumption (TLS 1.3 only)

If client previously connected to this server and has a **session ticket**:

```mermaid
sequenceDiagram
    participant C as Client
    participant S as Server

    Note over C: Has session ticket from previous connection

    C->>S: ClientHello + early_data<br/>(GET /api/users sent in FIRST packet!)<br/>session_ticket + new key_share
    S->>C: ServerHello + Finished
    C->>S: Finished

    Note over C,S: ✅ Data received before handshake completes!<br/>0 RTT cost for resumption<br/>⚠️ Risk: replay attacks (only use for idempotent GETs)
```

## TLS 1.2 vs TLS 1.3 — What Changed

| | TLS 1.2 | TLS 1.3 |
|---|---|---|
| Handshake RTTs | 2 RTTs | 1 RTT (0-RTT resumption) |
| Key exchange | RSA, DHE, ECDHE | ECDHE only (forward secrecy mandatory) |
| Ciphers | Many (including weak: RC4, CBC, 3DES) | 5 AEAD-only ciphers |
| Certificate encryption | No — cert visible during handshake | Yes — cert encrypted |
| Forward secrecy | Optional (RSA has none) | Mandatory |
| Removed | — | RSA key exchange, CBC, MD5, SHA-1, compression, renegotiation |

---

# 6. Forward Secrecy — Why It Matters

```mermaid
graph LR
    subgraph "WITHOUT Forward Secrecy (TLS 1.2 RSA key exchange)"
        A1["Client encrypts<br/>session key with<br/>server's public key"] --> B1["Server decrypts<br/>with private key"]
        B1 --> C1["Encrypted session data<br/>stored by attacker"]
        D1["Later: attacker steals<br/>server private key"] --> E1["Decrypts session key<br/>→ decrypts ALL past traffic ❌"]
    end

    subgraph "WITH Forward Secrecy (ECDHE — TLS 1.3 only option)"
        A2["Both sides generate<br/>EPHEMERAL key pairs<br/>per session"] --> B2["Session key derived<br/>from ephemeral values"]
        B2 --> C2["Ephemeral private keys<br/>DISCARDED after session"]
        D2["Later: attacker steals<br/>server private key"] --> E2["Ephemeral keys gone<br/>→ cannot decrypt past traffic ✅"]
    end
```

```text
ECDHE = Elliptic Curve Diffie-Hellman Ephemeral

"Ephemeral" = a new key pair is generated for EVERY TLS session.
              The keys are used, then thrown away.
              Even the server can't decrypt past sessions.
```

---

# 7. mTLS — Mutual TLS

## TLS vs mTLS — The Core Difference

```mermaid
graph LR
    subgraph "Standard TLS (one-way)"
        C1[Client] -->|"1. Hello"| S1[Server]
        S1 -->|"2. Here is my certificate<br/>proving I'm api.example.com"| C1
        C1 -->|"3. Verified ✅ — session key exchange"| S1
        C1 -->|"4. Anonymous request"| S1
        note1["Client = anonymous<br/>Server = identified"]
    end

    subgraph "mTLS (two-way)"
        C2[Client/Service A] -->|"1. Hello + here is MY certificate<br/>proving I'm order-service"| S2[Server/Service B]
        S2 -->|"2. Here is MY certificate<br/>proving I'm inventory-service"| C2
        C2 -->|"3. Both verified ✅"| S2
        note2["Both sides = identified<br/>No anonymous access"]
    end
```

## mTLS Full Handshake Flow

```mermaid
sequenceDiagram
    participant C as Client Service A<br/>(has its own cert)
    participant S as Server Service B<br/>(has its own cert)

    Note over C,S: TCP + TLS setup

    C->>S: ClientHello

    S->>C: ServerHello
    S->>C: Certificate (server's cert — Service B identity)
    S->>C: CertificateRequest ⬅ NEW: "I need your cert too"
    S->>C: ServerHelloDone

    Note over C: Verify Service B's certificate<br/>Is it signed by our internal CA?<br/>Is CN = inventory-service? ✓

    C->>S: Certificate (client's cert — Service A identity) ⬅ NEW
    C->>S: ClientKeyExchange (ECDH value)
    C->>S: CertificateVerify ⬅ NEW: signed proof client owns private key
    C->>S: ChangeCipherSpec
    C->>S: Finished

    Note over S: Verify Service A's certificate ⬅ NEW<br/>Is it signed by our internal CA?<br/>Is CN = order-service? ✓<br/>Is it in the allowed services list?

    S->>C: ChangeCipherSpec
    S->>C: Finished

    Note over C,S: ✅ Mutual authentication complete<br/>Both sides verified<br/>Encrypted channel established

    C->>S: POST /api/inventory/check (encrypted)
    S->>C: 200 OK (encrypted)
```

## Why mTLS for Microservices

```mermaid
graph TD
    subgraph "Problem: No authentication between services"
        A["Order Service"] -->|"POST /inventory/check"| B["Inventory Service"]
        BAD["🚨 Rogue/compromised container<br/>can call any service!<br/>No way to know if caller is legitimate"] -.->|"also calls"| B
    end

    subgraph "Solution: mTLS"
        C["Order Service<br/>cert: order-svc.company.internal"] -->|"mTLS: presents cert"| D["Inventory Service<br/>verifies cert is from internal CA<br/>+ CN = order-service"]
        ROGUE["🚨 Rogue container<br/>(no valid cert from internal CA)"] -->|"rejected! ❌<br/>no valid certificate"| D
    end
```

---

# 8. Implementing TLS in Spring Boot — Complete Guide

## Step 1: Generate Certificates (Local Dev)

```bash
# Option A: Self-signed cert (dev only — browser will warn)
keytool -genkeypair \
  -alias myapp \
  -keyalg RSA \
  -keysize 2048 \
  -storetype PKCS12 \
  -keystore src/main/resources/keystore.p12 \
  -validity 365 \
  -storepass changeit \
  -dname "CN=localhost, OU=Dev, O=MyCompany, L=Mumbai, ST=MH, C=IN" \
  -ext "SAN=DNS:localhost,IP:127.0.0.1"

# Option B: Use mkcert (trusted locally — no browser warning)
brew install mkcert
mkcert -install                     # installs local CA in browser trust store
mkcert -pkcs12 -p12-file keystore.p12 localhost 127.0.0.1
# password is "changeit" by default with mkcert
```

## Step 2: Configure Spring Boot Server

```yaml
# src/main/resources/application.yml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-type: PKCS12
    key-store-password: ${SSL_KEYSTORE_PASSWORD:changeit}
    key-alias: myapp
    protocol: TLS
    enabled-protocols:
      - TLSv1.3
      - TLSv1.2

spring:
  security:
    require-ssl: true
```

## Step 3: Redirect HTTP to HTTPS

```java
// config/HttpsRedirectConfig.java
@Configuration
public class HttpsRedirectConfig {

    @Bean
    public ServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {
            @Override
            protected void postProcessContext(Context context) {
                // Force HTTPS for all paths
                SecurityConstraint securityConstraint = new SecurityConstraint();
                securityConstraint.setUserConstraint("CONFIDENTIAL");
                SecurityCollection collection = new SecurityCollection();
                collection.addPattern("/*");
                securityConstraint.addCollection(collection);
                context.addConstraint(securityConstraint);
            }
        };
        // Add HTTP connector that redirects to HTTPS
        tomcat.addAdditionalTomcatConnectors(httpConnector());
        return tomcat;
    }

    private Connector httpConnector() {
        Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        connector.setScheme("http");
        connector.setPort(8080);        // HTTP port
        connector.setSecure(false);
        connector.setRedirectPort(8443); // → HTTPS port
        return connector;
    }
}
```

## Step 4: Make HTTPS Calls from Spring Boot (as a Client)

```java
// config/HttpsClientConfig.java
@Configuration
public class HttpsClientConfig {

    // Default — uses JVM cacerts (works for Let's Encrypt / public CAs)
    @Bean
    @Primary
    public RestTemplate defaultRestTemplate() {
        return new RestTemplate();
    }

    // Custom TrustStore — for internal CAs or self-signed certs
    @Bean("trustedRestTemplate")
    public RestTemplate trustedRestTemplate(
            @Value("${ssl.truststore.path}") String trustStorePath,
            @Value("${ssl.truststore.password}") String trustStorePassword
    ) throws Exception {

        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        try (InputStream is = new FileInputStream(trustStorePath)) {
            trustStore.load(is, trustStorePassword.toCharArray());
        }

        SSLContext sslContext = SSLContextBuilder.create()
                .loadTrustMaterial(trustStore, null)
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLContext(sslContext)
                .build();

        return new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
    }

    // WebClient version (reactive)
    @Bean("trustedWebClient")
    public WebClient trustedWebClient(
            @Value("${ssl.truststore.path}") String trustStorePath
    ) throws Exception {
        SslContext sslContext = SslContextBuilder.forClient()
                .trustManager(new File(trustStorePath))
                .build();

        HttpClient httpClient = HttpClient.create()
                .secure(t -> t.sslContext(sslContext));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
```

---

# 9. Implementing mTLS in Spring Boot — Complete Guide

mTLS means:
- **Server**: configured to REQUIRE a client certificate (`client-auth: need`)
- **Client**: configured with its own certificate to present during handshake

## Architecture for This Example

```mermaid
graph LR
    OS["Order Service<br/>(Spring Boot client)<br/>Has: order-service.p12<br/>(its certificate)"] -->|"mTLS connection<br/>presents client cert"| IS["Inventory Service<br/>(Spring Boot server)<br/>Requires: client cert<br/>Trust: internal-ca.p12"]

    CA["Internal CA<br/>(you created this)"] -.->|"issued cert to"| OS
    CA -.->|"issued cert to"| IS
    CA -.->|"trusted by"| IS
```

## Step 1: Create Your Own Internal CA

```bash
# Create internal CA (do this ONCE, store securely)
openssl genrsa -out ca.key 4096
openssl req -new -x509 -days 3650 -key ca.key -out ca.crt \
  -subj "/CN=My Internal CA/O=MyCompany/C=IN"

echo "Internal CA created: ca.crt (public) + ca.key (private, keep safe!)"
```

## Step 2: Issue Certificate to Inventory Service (the Server)

```bash
# Generate key and CSR for inventory-service
openssl genrsa -out inventory-service.key 2048
openssl req -new -key inventory-service.key -out inventory-service.csr \
  -subj "/CN=inventory-service/O=MyCompany/C=IN"

# CA signs it (SAN needed for hostname validation)
openssl x509 -req -days 365 \
  -in inventory-service.csr \
  -CA ca.crt -CAkey ca.key -CAcreateserial \
  -out inventory-service.crt \
  -extfile <(echo "subjectAltName=DNS:inventory-service,DNS:localhost,IP:127.0.0.1")

# Package into PKCS12 keystore
openssl pkcs12 -export \
  -in inventory-service.crt \
  -inkey inventory-service.key \
  -out inventory-keystore.p12 \
  -name inventory-service \
  -passout pass:changeit
```

## Step 3: Issue Certificate to Order Service (the Client)

```bash
openssl genrsa -out order-service.key 2048
openssl req -new -key order-service.key -out order-service.csr \
  -subj "/CN=order-service/O=MyCompany/C=IN"

openssl x509 -req -days 365 \
  -in order-service.csr \
  -CA ca.crt -CAkey ca.key -CAcreateserial \
  -out order-service.crt

openssl pkcs12 -export \
  -in order-service.crt \
  -inkey order-service.key \
  -out order-keystore.p12 \
  -name order-service \
  -passout pass:changeit
```

## Step 4: Create TrustStore with Internal CA

```bash
# Both services need to trust the internal CA
keytool -import -alias internal-ca \
  -file ca.crt \
  -keystore truststore.p12 \
  -storetype PKCS12 \
  -storepass changeit \
  -noprompt

# Now copy:
# inventory-service: needs inventory-keystore.p12 + truststore.p12
# order-service:     needs order-keystore.p12 + truststore.p12
```

## Step 5: Configure Inventory Service (Server — Requires Client Cert)

```yaml
# inventory-service/application.yml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:inventory-keystore.p12
    key-store-type: PKCS12
    key-store-password: ${SSL_KEY_STORE_PASSWORD:changeit}
    key-alias: inventory-service

    trust-store: classpath:truststore.p12
    trust-store-type: PKCS12
    trust-store-password: ${SSL_TRUST_STORE_PASSWORD:changeit}

    client-auth: need       # ← CRITICAL: REQUIRE client certificate
                            # need  = reject any connection without valid client cert
                            # want  = accept cert if provided, but don't require
                            # none  = standard TLS (no client cert)
```

```java
// (Optional) Extract client identity in controller
@RestController
public class InventoryController {

    @GetMapping("/api/inventory/check")
    public ResponseEntity<InventoryResponse> checkInventory(
            HttpServletRequest request) {         // or X509Certificate[] directly

        // Get the client certificate from request
        X509Certificate[] certs =
            (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");

        if (certs == null || certs.length == 0) {
            return ResponseEntity.status(403).build();
        }

        // Extract client service name from certificate CN
        String clientCN = certs[0].getSubjectX500Principal().getName();
        // clientCN = "CN=order-service,O=MyCompany,C=IN"
        String serviceName = extractCN(clientCN);   // → "order-service"

        log.info("Request from authenticated service: {}", serviceName);

        // Optionally check if this service is allowed to call this endpoint
        if (!allowedServices.contains(serviceName)) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(inventoryService.check());
    }

    private String extractCN(String dn) {
        // Parse "CN=order-service,O=..." → "order-service"
        return Arrays.stream(dn.split(","))
            .filter(part -> part.trim().startsWith("CN="))
            .map(part -> part.trim().substring(3))
            .findFirst().orElse("unknown");
    }
}
```

## Step 6: Configure Order Service (Client — Sends Its Certificate)

```java
// config/MtlsClientConfig.java in order-service
@Configuration
public class MtlsClientConfig {

    @Value("${ssl.key-store.path:classpath:order-keystore.p12}")
    private Resource keyStorePath;

    @Value("${ssl.key-store.password:changeit}")
    private String keyStorePassword;

    @Value("${ssl.trust-store.path:classpath:truststore.p12}")
    private Resource trustStorePath;

    @Value("${ssl.trust-store.password:changeit}")
    private String trustStorePassword;

    @Bean
    public RestTemplate mtlsRestTemplate() throws Exception {
        // Load OUR certificate (we present this to servers)
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(keyStorePath.getInputStream(),
                      keyStorePassword.toCharArray());

        // Load trust store (defines which server certs we accept)
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        trustStore.load(trustStorePath.getInputStream(),
                        trustStorePassword.toCharArray());

        // Build SSLContext with both
        SSLContext sslContext = SSLContextBuilder.create()
                .loadKeyMaterial(keyStore, keyStorePassword.toCharArray())  // our cert (client)
                .loadTrustMaterial(trustStore, null)                         // trusted server CAs
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLContext(sslContext)
                .setSSLHostnameVerifier(new DefaultHostnameVerifier())       // verify server hostname
                .build();

        return new RestTemplate(
                new HttpComponentsClientHttpRequestFactory(httpClient));
    }

    // WebClient version (reactive)
    @Bean
    public WebClient mtlsWebClient() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(keyStorePath.getInputStream(), keyStorePassword.toCharArray());

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, keyStorePassword.toCharArray());

        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        trustStore.load(trustStorePath.getInputStream(), trustStorePassword.toCharArray());

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        SslContext sslContext = SslContextBuilder.forClient()
                .keyManager(kmf)       // client certificate
                .trustManager(tmf)     // trusted server CAs
                .build();

        HttpClient httpClient = HttpClient.create()
                .secure(t -> t.sslContext(sslContext));

        return WebClient.builder()
                .baseUrl("https://inventory-service:8443")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
```

```java
// Usage in Order Service
@Service
public class OrderService {

    private final WebClient mtlsWebClient;

    public OrderService(WebClient mtlsWebClient) {
        this.mtlsWebClient = mtlsWebClient;
    }

    public InventoryResponse checkInventory(String productId) {
        // This request automatically presents order-service's certificate!
        // Inventory service verifies it via mTLS — no Authorization header needed
        return mtlsWebClient.get()
                .uri("/api/inventory/check?productId={id}", productId)
                .retrieve()
                .bodyToMono(InventoryResponse.class)
                .block();
    }
}
```

## Full mTLS Flow — What Happens at Runtime

```mermaid
sequenceDiagram
    participant OS as Order Service<br/>(client)
    participant IS as Inventory Service<br/>(server, client-auth: need)

    Note over OS,IS: TCP connection established

    OS->>IS: ClientHello (TLS negotiation start)
    IS->>OS: ServerHello
    IS->>OS: Certificate: inventory-service cert<br/>(signed by Internal CA)
    IS->>OS: CertificateRequest: "send me your cert"
    IS->>OS: ServerHelloDone

    Note over OS: Verify inventory-service cert<br/>→ signed by Internal CA in truststore? ✓<br/>→ CN = inventory-service? ✓

    OS->>IS: Certificate: order-service cert<br/>(signed by Internal CA)
    OS->>IS: ClientKeyExchange + CertificateVerify
    OS->>IS: Finished

    Note over IS: Verify order-service cert<br/>→ signed by Internal CA in truststore? ✓<br/>→ CN in allowed list? ✓<br/>→ not expired? ✓

    IS->>OS: Finished

    Note over OS,IS: ✅ mTLS established — both sides verified!

    OS->>IS: GET /api/inventory/check (encrypted)
    Note over IS: Optionally: extract CN from cert<br/>= "order-service" → log/audit/authorize
    IS->>OS: 200 OK {"stock": 42} (encrypted)
```

---

# 10. Istio mTLS — How It Works End to End

Istio eliminates the need to write any mTLS code in your application. It handles everything at the infrastructure level using **sidecar proxies**.

## The Core Idea: Sidecar Proxy Interception

```mermaid
graph LR
    subgraph "Pod: Order Service"
        APP1["OrderService<br/>Java App<br/>port 8080<br/>(plain HTTP!)"]
        ENV1["Envoy Sidecar Proxy<br/>(injected by Istio)<br/>iptables intercepts all traffic"]
    end

    subgraph "Pod: Inventory Service"
        ENV2["Envoy Sidecar Proxy<br/>(injected by Istio)"]
        APP2["InventoryService<br/>Java App<br/>port 8080<br/>(plain HTTP!)"]
    end

    APP1 -->|"HTTP (plain)<br/>app thinks it's talking directly"| ENV1
    ENV1 -->|"mTLS 🔒<br/>(Envoy handles cert exchange)"| ENV2
    ENV2 -->|"HTTP (plain)<br/>app thinks caller is local"| APP2

    ISTIOD["istiod<br/>(Istio control plane)<br/>Issues certs, pushes config"]
    ISTIOD -.->|"cert: order-svc<br/>valid 24h"| ENV1
    ISTIOD -.->|"cert: inventory-svc<br/>valid 24h"| ENV2
```

**Your app code stays 100% unchanged.** It sends plain HTTP to `localhost:8080`. Envoy intercepts via iptables, wraps it in mTLS, sends to the destination Envoy, which unwraps and delivers as plain HTTP.

## How Istio Issues Certificates — The Full Flow

```mermaid
sequenceDiagram
    participant K8s as Kubernetes API
    participant Istiod as istiod<br/>(Istio Control Plane)
    participant Envoy as Envoy Sidecar<br/>(in pod)
    participant CA as Istio CA<br/>(part of istiod)

    Note over K8s,Envoy: Pod startup

    K8s->>Envoy: Pod created with istio-injection=enabled label<br/>Istio mutating webhook injects Envoy container

    Envoy->>Istiod: xDS connection (gRPC stream)<br/>"I am order-service in namespace default"

    Note over Istiod: Reads Kubernetes ServiceAccount<br/>Verifies pod identity via K8s token

    Istiod->>Envoy: Sends routing rules, policies<br/>mTLS requirements, etc.

    Envoy->>CA: CSR (Certificate Signing Request)<br/>SPIFFE identity: spiffe://cluster.local/ns/default/sa/order-service

    CA->>Envoy: Signed certificate (valid 24h)<br/>Subject: spiffe://cluster.local/ns/default/sa/order-service<br/>Issued by: Istio internal CA

    Note over Envoy: Stores cert in memory (never on disk)<br/>Starts handling mTLS for all connections

    Note over CA,Envoy: Auto-rotation at 80% of lifetime (~19h)<br/>Zero downtime cert renewal
```

## The SPIFFE Identity

Istio uses **SPIFFE** (Secure Production Identity Framework for Everyone) as the identity format:

```text
SPIFFE URI format:
  spiffe://trust-domain/path

Istio example:
  spiffe://cluster.local/ns/default/sa/order-service
  │          │              │  │         │
  │          │              │  │         └── Kubernetes ServiceAccount name
  │          │              │  └──────────── Namespace
  │          │              └─────────────── "ns" (namespace indicator)
  │          └────────────────────────────── Cluster trust domain
  └───────────────────────────────────────── SPIFFE scheme

This URI is embedded in the certificate's SAN field as a URI SAN.
When mTLS handshake completes, Istio reads the peer's SPIFFE identity
and uses it for authorization policies.
```

## Kubernetes + Istio Setup — Step by Step

### Install Istio

```bash
# Download istioctl
curl -L https://istio.io/downloadIstio | sh -
export PATH=$PWD/istio-1.x.x/bin:$PATH

# Install Istio on cluster
istioctl install --set profile=default -y

# Verify installation
kubectl get pods -n istio-system
# NAME                      READY   STATUS
# istiod-xxx                1/1     Running   ← control plane
# istio-ingressgateway-xxx  1/1     Running   ← ingress
```

### Enable Auto-Injection for Your Namespace

```bash
# Label namespace — Istio will inject Envoy into every pod in this namespace
kubectl label namespace production istio-injection=enabled

# Verify label applied
kubectl get namespace production --show-labels
```

### Deploy Your Services (No Changes to Existing Deployment YAML!)

```yaml
# order-service deployment — EXACTLY as before, no mTLS config needed!
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
  namespace: production
spec:
  replicas: 2
  selector:
    matchLabels:
      app: order-service
  template:
    metadata:
      labels:
        app: order-service
    spec:
      serviceAccountName: order-service  # ← Istio uses this for identity
      containers:
        - name: order-service
          image: myregistry/order-service:latest
          ports:
            - containerPort: 8080
          # No TLS config! App runs plain HTTP.
          # Envoy handles all TLS transparently.
---
apiVersion: v1
kind: ServiceAccount
metadata:
  name: order-service
  namespace: production
```

### Enforce mTLS with PeerAuthentication

```yaml
# Namespace-wide: ALL services in production namespace must use mTLS
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default
  namespace: production
spec:
  mtls:
    mode: STRICT    # STRICT = reject plain HTTP, require valid client cert
                    # PERMISSIVE = accept both mTLS and plain HTTP (migration mode)
                    # DISABLE = no mTLS

---
# OR: Mesh-wide (applies to entire cluster)
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default
  namespace: istio-system  # ← istio-system = mesh-wide
spec:
  mtls:
    mode: STRICT
```

### Configure Client-Side TLS with DestinationRule

```yaml
# Tell order-service to use mTLS when calling inventory-service
apiVersion: networking.istio.io/v1alpha3
kind: DestinationRule
metadata:
  name: inventory-service-mtls
  namespace: production
spec:
  host: inventory-service.production.svc.cluster.local
  trafficPolicy:
    tls:
      mode: ISTIO_MUTUAL    # Use Istio-managed mTLS certs (auto)
                            # Other modes:
                            # SIMPLE = one-way TLS (no client cert)
                            # MUTUAL = bring your own certs
                            # DISABLE = no TLS
```

### Verify mTLS is Working

```bash
# Check all services have mTLS enabled
istioctl x check-inject -n production

# Watch mTLS policy status
kubectl get peerauthentication -n production

# Check if a specific connection uses mTLS
kubectl exec -n production deploy/order-service -c istio-proxy -- \
  curl -s localhost:15000/clusters | grep inventory-service

# Use kiali (Istio dashboard) to visualize mTLS
kubectl apply -f https://raw.githubusercontent.com/istio/istio/release-1.x/samples/addons/kiali.yaml
istioctl dashboard kiali
# Green lock icons = mTLS enabled between services
```

### Add Authorization Policy (Who Can Call What)

```yaml
# Only order-service is allowed to call inventory-service
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: inventory-service-policy
  namespace: production
spec:
  selector:
    matchLabels:
      app: inventory-service
  action: ALLOW
  rules:
    - from:
        - source:
            principals:
              # Only allow calls from order-service
              - "cluster.local/ns/production/sa/order-service"
      to:
        - operation:
            methods: ["GET", "POST"]
            paths: ["/api/inventory/*"]

# Any other service calling inventory-service → 403 Forbidden
# This is enforced by Envoy without any code change to inventory-service!
```

## Full End-to-End Request Flow with Istio

```mermaid
sequenceDiagram
    participant APP_OS as Order Service App<br/>(plain HTTP, port 8080)
    participant ENV_OS as Envoy (Order Service pod)<br/>has cert: order-svc SPIFFE
    participant ENV_IS as Envoy (Inventory Service pod)<br/>has cert: inventory-svc SPIFFE
    participant APP_IS as Inventory Service App<br/>(plain HTTP, port 8080)

    APP_OS->>ENV_OS: HTTP GET /api/inventory/check<br/>to inventory-service:8080<br/>(app thinks this is direct!)

    Note over ENV_OS: iptables intercepts outbound traffic<br/>Envoy knows to use mTLS for this destination<br/>(from DestinationRule config)

    ENV_OS->>ENV_IS: mTLS ClientHello<br/>presents order-svc certificate
    ENV_IS->>ENV_OS: mTLS ServerHello<br/>presents inventory-svc certificate<br/>requests client cert
    ENV_OS->>ENV_IS: client cert + key exchange
    Note over ENV_IS: Verifies order-svc cert<br/>SPIFFE = order-service ✓<br/>Checks AuthorizationPolicy ✓

    ENV_IS->>ENV_OS: Finished — mTLS established

    ENV_OS->>ENV_IS: HTTP GET /api/inventory/check<br/>(inside mTLS tunnel)

    Note over ENV_IS: iptables intercepts inbound traffic<br/>Strips TLS, delivers plain HTTP to app

    ENV_IS->>APP_IS: HTTP GET /api/inventory/check<br/>(plain HTTP — app unchanged!)

    APP_IS->>ENV_IS: HTTP 200 OK {"stock": 42}

    ENV_IS->>ENV_OS: HTTP 200 OK (inside mTLS tunnel)

    ENV_OS->>APP_OS: HTTP 200 OK {"stock": 42}<br/>(plain HTTP — app unchanged!)

    Note over APP_OS,APP_IS: ✅ Complete mTLS with zero code changes!<br/>Zero-trust enforced at network level
```

## Istio vs Manual mTLS — Comparison

| | Manual mTLS (Spring Boot) | Istio mTLS |
|---|---|---|
| **Code changes** | Yes — SSLContext, KeyStore, WebClient config | Zero — app code unchanged |
| **Certificate management** | Manual — you generate, rotate, distribute | Auto — istiod issues + rotates every 24h |
| **Certificate format** | PKCS12/JKS files | In-memory only (never on disk) |
| **Identity** | CN in certificate (e.g., "order-service") | SPIFFE URI (cryptographically bound to K8s SA) |
| **Authorization** | Implement in every service (check cert CN) | Declarative AuthorizationPolicy (Istio enforces) |
| **Rotation** | Manual renewal, redeploy | Automatic, zero downtime |
| **Observability** | Log it yourself | Kiali dashboard, metrics built-in |
| **When to use** | Small setup, no K8s, external APIs | Kubernetes + many services |

---

# 11. Certificate Pinning

```mermaid
graph LR
    subgraph "Normal TLS — trusts any valid cert"
        C1[Client] -->|"connects to api.bank.com"| S1[Real Server ✅]
        C1 -->|"MiTM with VALID cert<br/>from compromised CA"| ATK[Attacker ❌<br/>also accepted!]
    end

    subgraph "Certificate Pinning — only trusts specific cert"
        C2[Client<br/>pinned fingerprint: ABC123] -->|"cert fingerprint = ABC123 ✅"| S2[Real Server ✅]
        C2 -->|"cert fingerprint ≠ ABC123 ❌<br/>REJECTED even if valid CA"| ATK2[Attacker ❌]
    end
```

```text
Used by: banking apps, corporate VPNs, high-security mobile apps

Downside: if cert expires and you pinned it → app breaks
Fix: pin to INTERMEDIATE CA (not the leaf cert):
  → cert can rotate freely as long as same CA signs it
  → only the intermediate CA fingerprint is pinned

In Java (OkHttp):
  CertificatePinner pinner = new CertificatePinner.Builder()
      .add("api.bank.com", "sha256/AAAA...pinned-public-key-hash...==")
      .build();
  OkHttpClient client = new OkHttpClient.Builder()
      .certificatePinner(pinner)
      .build();
```

---

# Interview Preparation — TLS & mTLS

## Q1: Walk me through what happens when a browser connects to https://example.com

**Answer:**

1. **DNS**: browser resolves `example.com` to an IP address
2. **TCP handshake**: SYN → SYN-ACK → ACK (1 RTT)
3. **TLS 1.3 ClientHello**: browser sends supported cipher suites + ECDH key share
4. **ServerHello**: server responds with chosen cipher + its own ECDH value + certificate (encrypted in TLS 1.3)
5. **Key derivation**: both sides independently compute the same session keys from ECDH values — no key ever crossed the network
6. **Certificate verification**: browser checks cert chain traces to trusted root CA, hostname matches SAN, cert not expired, not revoked (OCSP)
7. **Finished**: both sides exchange Finished messages (HMAC of all handshake data, proves no tampering)
8. **HTTP**: browser sends `GET / HTTP/2` — everything encrypted with AES-GCM using the derived session keys

Total: ~2 RTTs (1 TCP + 1 TLS 1.3) before first byte of page HTML.

## Q2: What is mTLS and why do microservices use it?

**Answer:**

Standard TLS authenticates only the **server** — the client is anonymous. mTLS (Mutual TLS) authenticates **both sides** using X.509 certificates.

In microservices, you need to answer: "How does Service A prove to Service B that it IS Service A, not a rogue pod that somehow got into the cluster?" Options:
- JWT tokens: application-level, every service must implement validation, tokens can be stolen/replayed
- mTLS: transport-level, every connection cryptographically proves identity, private key never leaves the service

In Kubernetes with Istio: Istio injects Envoy sidecars into every pod. istiod issues short-lived (24h) SPIFFE certificates. Envoy handles mTLS automatically — your Java app sees only plain HTTP. Zero code changes needed.

## Q3: What is forward secrecy? Why did TLS 1.3 make it mandatory?

**Answer:**

Forward secrecy means compromising the server's private key today cannot decrypt traffic recorded in the past.

Without it (RSA key exchange, removed in TLS 1.3): the client encrypts the session key using the server's public key. An attacker recording encrypted traffic + later stealing the private key can decrypt everything. Nation-state actors do exactly this — "harvest now, decrypt later."

With forward secrecy (ECDHE, now mandatory): both sides generate ephemeral (one-time) key pairs. The session key is derived from the ECDH of these ephemeral keys. Ephemeral private keys are discarded immediately after the session. Even with the server's private key, past sessions cannot be decrypted.

TLS 1.3 made ECDHE mandatory (removed RSA key exchange entirely), so every TLS 1.3 session has forward secrecy by design.

## Q4: How does Istio implement mTLS without any code changes in the application?

**Answer:**

Istio uses two mechanisms:

**1. Sidecar injection**: Kubernetes mutating webhook intercepts pod creation. If the namespace has `istio-injection=enabled`, Istio injects an Envoy proxy container into every pod. iptables rules redirect ALL inbound and outbound TCP traffic through Envoy — the app never knows.

**2. Certificate issuance via xDS**: When Envoy starts, it connects to istiod (control plane) via gRPC. It proves its identity using the Kubernetes ServiceAccount token. istiod issues a SPIFFE certificate (e.g., `spiffe://cluster.local/ns/production/sa/order-service`) valid for 24 hours, stored in Envoy's memory (never on disk). istiod pushes routing rules and mTLS policies via xDS protocol.

**At runtime**: when Service A calls Service B, Envoy-A intercepts the outbound HTTP call, establishes a mTLS connection to Envoy-B (both presenting their SPIFFE certificates), then delivers plain HTTP to Service B's container. Service B's app sees a normal HTTP request with no TLS at all.

PeerAuthentication STRICT mode + AuthorizationPolicy let you declaratively enforce "only order-service can call inventory-service" — enforced by Envoy, not by application code.

## Q5: What is the difference between mTLS and JWT for service authentication?

**Answer:**

| Concern | JWT | mTLS |
|---|---|---|
| Level | Application (header: `Authorization: Bearer ...`) | Transport (TLS handshake) |
| Code needed | Every service must parse + validate JWT | Zero — Istio/Envoy handles it |
| Secret transport | JWT travels in every request (can be intercepted) | Private key NEVER leaves the service |
| Replay attack | JWT can be replayed until expiry | No — TLS session is unique per connection |
| Rotation | Complex (revocation lists or short TTL) | Auto — Istio rotates every 24h |
| User context | Can carry user claims (sub, roles, etc.) | Only service identity |

**In practice**: many systems use **both**. mTLS authenticates the calling **service** at the transport level (zero-trust: "is this caller really order-service?"). JWT authenticates the end **user** at the application level (passed in headers between services). They solve different problems and complement each other.
