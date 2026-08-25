# TLS — What It Is, How It Works, and Practical Usage

TLS (Transport Layer Security) is the cryptographic protocol that makes HTTPS, secure emails, and encrypted database connections possible. This doc covers TLS from a **practical backend developer perspective** — what it is, how to get certificates, what errors mean, and how to debug it.

> **Read `TLS-mTLS/README.md` after this for the cryptographic internals (handshake step-by-step, cipher suites, forward secrecy, mTLS). This doc focuses on what you need to know to work with TLS in production.**

---

# 1. What is TLS in Plain English

When you type `https://bank.com` in your browser, before any bank page loads, your browser and the bank's server must:

1. **Agree on HOW to encrypt** — which encryption algorithms to use
2. **Verify the server is real** — prove you're talking to the real bank, not a fake
3. **Exchange keys secretly** — negotiate a shared encryption key without anyone intercepting it
4. **Encrypt everything** — your login, your account number, your transactions — all become unreadable to anyone in the middle

TLS handles all four steps. It sits between the application (HTTP) and the network (TCP):

```text
Your App / Browser
       ↓
   HTTP (or any protocol)
       ↓
   TLS  ← encrypts everything below this line
       ↓
   TCP
       ↓
   Network
```

Without TLS:
```text
Network packet visible to:
  Your ISP → their employees, government requests
  WiFi router → coffee shop owner, other patrons
  Any router between you and server → hundreds of hops
  Your employer (if on corporate network)

They see: GET /bank/transfer?from=alice&to=bob&amount=50000
```

With TLS:
```text
They see: ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓ (random encrypted bytes)
```

---

# 2. HTTPS = HTTP + TLS

HTTPS is not a separate protocol — it is simply HTTP running on top of a TLS connection.

```text
HTTP:   Plain text request/response, default port 80
HTTPS:  Same HTTP, but wrapped in TLS encryption, default port 443

http://example.com  → port 80  → everything visible
https://example.com → port 443 → everything encrypted

The HTTP protocol itself is unchanged:
  GET /api/users HTTP/1.1         (same request format)
  Host: api.example.com           (same headers)
  Authorization: Bearer token...  (same auth)

The difference: the bytes that travel over the network are encrypted by TLS.
```

## What TLS Actually Protects

```text
✓ Request URL (path + query string)   — encrypted after TLS handshake
✓ Request headers                      — encrypted
✓ Request body                         — encrypted
✓ Response headers                     — encrypted
✓ Response body                        — encrypted

✗ Domain name (hostname)               — NOT encrypted (SNI — see below)
✗ IP address                           — NOT encrypted (routing needs it)
✗ Port number                          — NOT encrypted
✗ Approximate size of data             — NOT encrypted (padding helps but not fully)
```

### SNI — Server Name Indication

```text
Problem: One IP can serve multiple HTTPS domains.
         TLS handshake happens BEFORE HTTP (before server knows which domain you want).
         Server needs to pick the right certificate for the right domain.

Solution: SNI (Server Name Indication)
  Client includes the hostname in the TLS ClientHello message
  Server picks the correct certificate for that hostname

Consequence: The HOSTNAME you're connecting to is visible to network observers
  (even over HTTPS, your ISP sees you visited bank.com — just not WHAT you did there)

ECH (Encrypted Client Hello): new TLS extension that encrypts the SNI
  Still being deployed (Chrome supports it, widespread adoption in progress)
```

---

# 3. TLS Versions — Which to Use

```text
TLS 1.0 (1999): DEPRECATED — disabled by default in all modern browsers (2020)
TLS 1.1 (2006): DEPRECATED — disabled by default in all modern browsers (2020)
TLS 1.2 (2008): CURRENT — widely used, secure when configured correctly
TLS 1.3 (2018): BEST — faster, more secure, simpler, preferred

SSL 2.0 / SSL 3.0: BROKEN — do not use, remove from all servers
```

**Configure your server to only accept TLS 1.2 and TLS 1.3:**

```nginx
# nginx
ssl_protocols TLSv1.2 TLSv1.3;

# Apache
SSLProtocol all -SSLv3 -TLSv1 -TLSv1.1
```

```yaml
# Spring Boot
server.ssl.enabled-protocols: TLSv1.2,TLSv1.3
```

TLS 1.3 advantages over 1.2:
- **Faster**: 1 RTT handshake (vs 2 RTT), 0-RTT resumption
- **More secure**: removed weak ciphers (CBC, RC4, RSA key exchange)
- **Mandatory forward secrecy**: ECDHE only — past traffic can't be decrypted if key is stolen
- **Simpler**: fewer cipher suites (standardized to 5)

---

# 4. X.509 Certificates — The Identity Document

A TLS certificate is the digital document a server presents to prove its identity. Think of it as a passport — issued by a trusted authority, contains identity information, has an expiry date.

## What's Inside a Certificate

```text
$ openssl x509 -in certificate.pem -text -noout

Certificate:
  Data:
    Version: 3
    Serial Number: 04:c4:e2:64:5f:35:30:78:1e:3a:3a:a9:1c:5c:2c:01
    Signature Algorithm: sha256WithRSAEncryption
    
    Issuer:   C=US, O=DigiCert Inc, CN=DigiCert TLS RSA SHA256 2020 CA1
    ↑ Who signed this certificate (the Certificate Authority)
    
    Validity:
      Not Before: Jan  1 00:00:00 2024 GMT
      Not After : Jan  1 23:59:59 2025 GMT
    ↑ Certificate is only valid within this window
    
    Subject:  CN=api.example.com
    ↑ Who this certificate belongs to (the server)
    
    Subject Public Key Info:
      RSA Public Key: (2048 bit)
    ↑ The server's public key (used during TLS handshake)
    
    X509v3 Subject Alternative Name:
      DNS:api.example.com, DNS:www.example.com, DNS:example.com
    ↑ ALL hostnames this certificate is valid for (SAN — see below)
    
    X509v3 Basic Constraints: CA:FALSE
    ↑ This is a server cert, not a CA cert (cannot sign other certs)
    
  Signature:  [CA's digital signature over all the above]
  ↑ Proves the CA vouches for this certificate's contents
```

## Certificate Types by Validation Level

```text
DV (Domain Validated):
  CA verifies: you control the domain (DNS record or file on server)
  CA does NOT verify: who you are as an organization
  Issued in: minutes (automated)
  Cost: FREE (Let's Encrypt) or cheap
  Shows in browser: 🔒 (padlock)
  Use for: APIs, web apps, internal services — most use cases
  
OV (Organization Validated):
  CA verifies: domain control + organization exists + is legitimate
  Takes: days (manual review by CA)
  Cost: $50-300/year
  Shows in browser: 🔒 (padlock, same as DV visually)
  Use for: e-commerce, financial services where org validation matters
  
EV (Extended Validation):
  CA verifies: rigorous identity verification (legal existence, physical address, phone)
  Takes: days to weeks
  Cost: $200-1000/year
  Was shown in browser: green bar with company name (deprecated by browsers in 2019!)
  Use for: essentially obsolete — browsers no longer show EV differently
  
Wildcard:
  Covers: *.example.com (one cert for ALL subdomains)
  Covers: api.example.com, app.example.com, dev.example.com
  Does NOT cover: sub.api.example.com (only one level deep)
  Does NOT cover: example.com itself (need to add as SAN explicitly)
  Use for: organizations with many subdomains (saves managing multiple certs)
  Risk: one cert compromise = all subdomains compromised
```

## SAN — Subject Alternative Names

Modern certificates use SANs to list all valid hostnames. The old `CN` field for hostname matching is deprecated.

```text
Certificate with SANs:
  CN: example.com (legacy, still present but not used for validation)
  SAN: DNS:example.com
       DNS:www.example.com
       DNS:api.example.com
       DNS:*.internal.example.com  (wildcard SAN)
       IP:192.168.1.1              (IP address SAN — for internal services!)

Browser validates: does the hostname I'm connecting to match any SAN entry?
  → Yes: continue (no hostname mismatch error)
  → No: CERT_HAS_EXPIRED or ERR_CERT_COMMON_NAME_INVALID error
```

---

# 5. Certificate Authorities — The Trust Chain

You trust a website's certificate because you trust who signed it.

```text
Your Browser / OS ships with:
  ~150 root CA certificates pre-installed
  (DigiCert, Let's Encrypt, Sectigo, GlobalSign, Comodo, etc.)
  These are the "root of trust"

Certificate chain for api.example.com:
  
  Root CA: DigiCert Global Root G2
    (self-signed, in your browser's trust store)
    ↓ signs
  
  Intermediate CA: DigiCert TLS RSA SHA256 2020 CA1
    (signed by Root CA, usually installed on web server)
    ↓ signs
  
  End-Entity: api.example.com
    (signed by Intermediate CA, installed on your server)

Browser validates chain bottom-up:
  Is api.example.com cert signed by DigiCert Intermediate? → yes
  Is DigiCert Intermediate signed by DigiCert Root? → yes
  Is DigiCert Root in my trust store? → yes ✓

Chain of trust established → connection is secure
```

## Why Intermediate CAs Exist

```text
If Root CA's private key were compromised:
  → Attacker could sign fake certificates for any website
  → Browser vendors would have to push emergency trust store update
  → Catastrophic, takes time to recover

With Intermediate CAs:
  Root CA is kept OFFLINE (air-gapped, hardware security module)
  Root CA signs Intermediate CA certificates (rare operation)
  Intermediate CA does daily certificate signing (online)
  
  If Intermediate CA is compromised:
  → Root CA revokes that Intermediate CA's cert
  → Limited damage — only certs signed by that intermediate are affected
  → Root CA can issue a new Intermediate CA cert
```

---

# 6. How to Get a TLS Certificate

## Option 1: Let's Encrypt (Free, Automated — Recommended)

Let's Encrypt is a free, automated, open CA. It issues DV certificates valid for **90 days** and provides ACME protocol for automatic renewal.

```bash
# Using Certbot (most common ACME client)
# Install certbot:
apt install certbot python3-certbot-nginx

# Get certificate for domain (nginx plugin auto-configures nginx):
certbot --nginx -d example.com -d www.example.com -d api.example.com

# Get certificate without web server (standalone, port 80 must be free):
certbot certonly --standalone -d example.com

# Get wildcard certificate (requires DNS challenge):
certbot certonly --manual --preferred-challenges dns -d "*.example.com" -d example.com

# Certificates stored in:
/etc/letsencrypt/live/example.com/
  ├── cert.pem        → server certificate
  ├── chain.pem       → intermediate CA certificates
  ├── fullchain.pem   → cert.pem + chain.pem (use this for nginx!)
  └── privkey.pem     → private key (NEVER share!)

# Auto-renewal (certbot adds cron job automatically):
certbot renew --dry-run          # test renewal
certbot renew                    # actual renewal
```

### ACME Protocol (How Let's Encrypt Works)

```text
ACME (Automated Certificate Management Environment):

1. Your server generates a key pair (CSR)
2. Requests certificate from Let's Encrypt ACME server
3. ACME server issues a CHALLENGE to prove domain control:

   HTTP-01 challenge:
     Let's Encrypt: "Put this token at http://example.com/.well-known/acme-challenge/TOKEN"
     Your server: creates the file
     Let's Encrypt: fetches the URL, verifies → certificate issued
   
   DNS-01 challenge (needed for wildcard certs):
     Let's Encrypt: "Add this TXT record: _acme-challenge.example.com = TOKEN"
     You: add DNS record (can be automated via DNS API)
     Let's Encrypt: queries DNS, verifies → certificate issued

4. Certificate issued, valid for 90 days
5. Certbot auto-renews at 60 days (before expiry)
```

## Option 2: Commercial Certificate (Paid)

Use when: you need OV validation, long validity, organization name on cert, specific compliance requirement.

```text
Process:
1. Generate private key and CSR (Certificate Signing Request) on your server
2. Submit CSR to CA (DigiCert, Sectigo, GlobalSign, etc.)
3. Complete domain/org validation
4. Download issued certificate files
5. Install on server

Generate CSR:
$ openssl req -newkey rsa:2048 -keyout server.key -out server.csr
  Country Name: IN
  State: Maharashtra
  Locality: Mumbai
  Organization: Example Corp
  Common Name: api.example.com
  Email: admin@example.com

# CSR contains your public key + identity info
# Private key (server.key) stays on your server — NEVER sent to CA
# CSR (server.csr) is sent to CA for signing
```

## Option 3: Self-Signed Certificate (Development Only)

A self-signed certificate is signed by itself (no CA). Browsers will show a security warning.

```bash
# Generate self-signed cert (development only!):
openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem \
  -days 365 -nodes \
  -subj "/CN=localhost" \
  -addext "subjectAltName=DNS:localhost,IP:127.0.0.1"

# For development with mkcert (trusted locally, no browser warnings):
brew install mkcert
mkcert -install          # installs local root CA into your browser trust store
mkcert localhost 127.0.0.1 api.local  # generates cert trusted by YOUR browser
```

---

# 7. Certificate Lifecycle

```text
Day 0:   Generate private key + CSR on your server
Day 0:   Submit CSR to CA, complete validation
Day 1:   Certificate issued (valid for 90 days or 1-2 years)

Production workflow:
  Day 60 (Let's Encrypt):  certbot renew runs (auto)
  Day 75:                  WARNING emails from monitoring
  Day 85:                  ALERT — certificate expiring in 5 days!
  Day 90:                  Certificate EXPIRES → HTTPS breaks for ALL users
                           → 503/SSL handshake errors → outage

Monitoring (essential!):
  Check certificate expiry with:
    echo | openssl s_client -connect api.example.com:443 2>/dev/null | \
      openssl x509 -noout -dates
  
  Set up alerts at: 30 days, 14 days, 7 days, 1 day before expiry
  
  Monitoring services:
    AWS Certificate Manager (ACM): auto-renews, sends CloudWatch alerts
    Let's Encrypt + certbot: cron renews at 60 days
    Datadog / Grafana: cert expiry check
```

---

# 8. Common TLS Errors — What They Mean and How to Fix Them

These are errors backend developers regularly face in production and development.

---

### ERR_CERT_AUTHORITY_INVALID / UNABLE_TO_VERIFY_LEAF_SIGNATURE
```text
What it means: Certificate was signed by an unknown/untrusted CA.

Causes:
  1. Self-signed certificate on production server
  2. Internal CA not added to client's trust store
  3. Incomplete certificate chain (intermediate CA not included)
  4. Certificate from CA not trusted by this client (e.g., custom CA)

Fix — incomplete chain (most common):
  Server must send: cert + intermediate CA (fullchain.pem in Let's Encrypt)
  If server only sends: cert (cert.pem)
  → Some clients reject (chain incomplete, can't trace to root)
  
  nginx config:
    ssl_certificate /etc/letsencrypt/live/example.com/fullchain.pem;  ← correct
    ssl_certificate /etc/letsencrypt/live/example.com/cert.pem;       ← WRONG (incomplete)

Fix — custom/internal CA:
  Add your internal CA's root certificate to client trust store
  Java: add to JVM truststore (cacerts) or pass SSLContext
```

---

### ERR_CERT_COMMON_NAME_INVALID / SSL_ERROR_BAD_CERT_DOMAIN
```text
What it means: The hostname you're connecting to doesn't match any SAN in the certificate.

Causes:
  1. Using IP address to connect, but cert has no IP SAN
  2. Connecting to subdomain not listed in cert
  3. Missing www. (cert has example.com, client connects to www.example.com or vice versa)
  4. Internal service using different hostname than cert

Example:
  Cert SAN: DNS:api.example.com
  Client connects to: internal-api.company.internal
  → HOSTNAME MISMATCH

Fix:
  Reissue cert with correct SANs (add all hostnames you'll use)
  For internal services: use wildcard cert or add internal hostname as SAN
  For IP access: add IP SAN: IP:192.168.1.100 in cert

Java equivalent: javax.net.ssl.SSLPeerUnverifiedException: Certificate doesn't match any of
```

---

### CERT_HAS_EXPIRED / SSL_ERROR_EXPIRED_CERT_ALERT
```text
What it means: Certificate's "Not After" date has passed.

This is a production emergency. Fix immediately:
  1. Renew certificate NOW
  2. Install new certificate
  3. Reload/restart web server
  
Prevention:
  Let's Encrypt + certbot: auto-renews at 60 days (certbot renew cron)
  Monitor expiry dates with alerts at 30/14/7 days
  AWS ACM: auto-renews (never expires if you use it correctly)
```

---

### PKIX Path Building Failed (Java)
```text
What it means: Java's TLS client cannot verify the server's certificate chain.
Full error: sun.security.validator.ValidatorException: PKIX path building failed

Causes:
  1. Server using certificate from CA not in Java's cacerts truststore
     (custom/internal CA, Let's Encrypt before they were trusted by Java)
  2. Certificate chain is incomplete
  3. Self-signed certificate

Fix 1: Add CA certificate to Java truststore:
  keytool -import -alias myca -keystore $JAVA_HOME/lib/security/cacerts \
    -file internal-ca.crt -storepass changeit

Fix 2: Create custom SSLContext with your truststore:
  KeyStore trustStore = KeyStore.getInstance("JKS");
  trustStore.load(new FileInputStream("mytruststore.jks"), "password".toCharArray());
  TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
  tmf.init(trustStore);
  SSLContext sslContext = SSLContext.getInstance("TLS");
  sslContext.init(null, tmf.getTrustManagers(), null);

Fix 3 (dev only, NEVER production): disable certificate verification
  // DO NOT DO THIS IN PRODUCTION — makes TLS pointless!
  TrustManager[] trustAll = { new X509TrustManager() {
    public void checkClientTrusted(X509Certificate[] c, String a) {}
    public void checkServerTrusted(X509Certificate[] c, String a) {}
    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
  }};
  sslContext.init(null, trustAll, new SecureRandom());
```

---

### SSL_ERROR_HANDSHAKE_FAILURE / TLS Handshake Timeout
```text
What it means: TLS handshake couldn't complete. No shared cipher suite, or connection issue.

Causes:
  1. Client and server have no cipher suites in common
     (old client + server only allows modern ciphers, or vice versa)
  2. TLS version mismatch (server requires TLS 1.3, client only supports 1.2)
  3. Network firewall blocking TLS traffic
  4. Client connecting to HTTP port (80) with HTTPS (TLS on port 80 makes no sense)
  5. Server certificate chain misconfigured
  6. SNI hostname not matching any virtual host

Debug with: openssl s_client -connect host:443 -tls1_2
            curl -v --tlsv1.2 https://host/
```

---

### Connection Reset / EOF During Handshake
```text
What it means: Connection closed unexpectedly during TLS handshake.

Common cause: connecting to port 443 of a server that is running plain HTTP there
  Server expects: HTTP request (GET / HTTP/1.1)
  Client sends: TLS ClientHello (binary, looks like garbage to HTTP server)
  Server: "what is this?" → closes connection → EOF
  
Also: firewall terminating TLS connections (DPI — Deep Packet Inspection)
      Server using wrong SSL/TLS certificate for this virtual host
```

---

# 9. Debugging TLS — Tools

## openssl s_client — The Essential TLS Debug Tool

```bash
# Connect and see full TLS handshake:
openssl s_client -connect api.example.com:443

# Output shows:
#   Certificate chain (server cert + intermediates)
#   TLS version and cipher suite negotiated
#   Certificate validity dates
#   Verification result

# Check specific TLS version:
openssl s_client -connect api.example.com:443 -tls1_2
openssl s_client -connect api.example.com:443 -tls1_3

# Check certificate expiry:
echo | openssl s_client -connect api.example.com:443 2>/dev/null \
  | openssl x509 -noout -dates

# Send HTTP request over TLS (debug HTTPS):
echo -e "GET / HTTP/1.1\r\nHost: api.example.com\r\n\r\n" \
  | openssl s_client -connect api.example.com:443 -quiet

# Check with SNI (for virtual hosts):
openssl s_client -connect api.example.com:443 -servername api.example.com

# View certificate details:
openssl s_client -connect api.example.com:443 2>/dev/null | openssl x509 -text -noout
```

## curl — Test HTTPS from Command Line

```bash
# Basic HTTPS request (shows TLS handshake with -v):
curl -v https://api.example.com/health

# Skip certificate verification (dev only!):
curl -k https://api.example.com/health
curl --insecure https://api.example.com/health

# Specify TLS version:
curl --tlsv1.3 https://api.example.com/health

# Use custom CA cert:
curl --cacert /path/to/ca.pem https://api.internal.com/health

# Client certificate (mTLS):
curl --cert client.pem --key client.key https://api.example.com/health

# Show only TLS info (not headers/body):
curl -v -o /dev/null https://api.example.com/ 2>&1 | grep -E "TLS|SSL|cert|expire"
```

## ssllabs.com — Full Server TLS Audit

```text
Go to: https://www.ssllabs.com/ssltest/
Enter: your domain
Get: full report including:
  - Certificate chain validity
  - TLS versions supported
  - Cipher suites (weak ones flagged)
  - Forward secrecy support
  - HSTS
  - Overall grade (A+ to F)
  
Target: A or A+ grade
Common issues causing B or below:
  - TLS 1.0/1.1 still enabled
  - Weak cipher suites (RC4, DES, 3DES)
  - Missing HSTS
  - Certificate chain incomplete
```

## keytool — Java Keystore Management

```bash
# List certificates in Java truststore:
keytool -list -v -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit | grep -A2 "Alias"

# Add a custom CA to Java truststore:
keytool -import -alias myCA -keystore $JAVA_HOME/lib/security/cacerts \
  -file ca.crt -storepass changeit

# Create a PKCS12 keystore from PEM files (for Spring Boot):
openssl pkcs12 -export \
  -in fullchain.pem \
  -inkey privkey.pem \
  -out keystore.p12 \
  -name myapp \
  -passout pass:changeit

# View contents of a keystore:
keytool -list -v -keystore keystore.p12 -storetype PKCS12 -storepass changeit
```

---

# 10. HSTS — HTTP Strict Transport Security

HSTS tells browsers: "This site is ALWAYS HTTPS. Never try HTTP."

```text
Without HSTS:
  User types: example.com (no https://)
  Browser: tries http://example.com first
  Server: 301 redirect → https://example.com
  
  Attack window: the FIRST http:// request is unencrypted!
  Man-in-the-middle can intercept that first redirect → SSL stripping attack

With HSTS:
  Server response includes:
    Strict-Transport-Security: max-age=31536000; includeSubDomains; preload

  Browser records: "example.com is HTTPS-only for 1 year"
  Next time user types: example.com
  Browser: immediately uses https:// — no HTTP request ever made
  → SSL stripping attack impossible (for returning visitors)
```

## HSTS Header Parameters

```text
Strict-Transport-Security: max-age=31536000; includeSubDomains; preload

max-age=31536000    → browser remembers HTTPS-only for 1 year (recommended minimum)
includeSubDomains   → applies to ALL subdomains (api., www., app., etc.)
preload             → submit site to browser HSTS preload list
                      → browser KNOWS it's HTTPS-only before first visit!
                      → Submit at: https://hstspreload.org
```

## nginx HSTS Configuration

```nginx
server {
    listen 443 ssl;
    server_name example.com;
    
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;
    
    # Redirect all HTTP to HTTPS
    # (keep in separate server block on port 80)
}

server {
    listen 80;
    server_name example.com www.example.com;
    return 301 https://example.com$request_uri;
}
```

---

# 11. TLS in Java and Spring Boot

## Java Terminology: KeyStore vs TrustStore

```text
KeyStore:
  Contains: YOUR certificate + private key
  Purpose:  "This is who I am" (presented during TLS handshake)
  Used by:  Server (presents server cert to clients)
            Client (for mTLS — presents client cert to server)
  Formats:  PKCS12 (.p12, .pfx) — preferred modern format
            JKS (.jks) — Java-specific, legacy
  
TrustStore:
  Contains: Certificates of CAs you TRUST
  Purpose:  "These are the authorities I accept certs from"
  Used by:  Client (to verify server's certificate chain)
  Default:  $JAVA_HOME/lib/security/cacerts (JVM ships with ~150 trusted CAs)
  
Analogy:
  KeyStore = your passport (proves your identity)
  TrustStore = list of countries whose passports you accept
```

## Spring Boot TLS Configuration

```yaml
# application.yml — enable HTTPS

server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:keystore.p12       # or /etc/ssl/keystore.p12
    key-store-password: ${SSL_KEY_PASSWORD}  # from env var, never hardcode!
    key-store-type: PKCS12
    key-alias: myapp
    
    # Restrict TLS versions:
    protocol: TLS
    enabled-protocols:
      - TLSv1.3
      - TLSv1.2
    
    # Restrict cipher suites (optional, defaults are good):
    ciphers:
      - TLS_AES_128_GCM_SHA256          # TLS 1.3
      - TLS_AES_256_GCM_SHA384          # TLS 1.3
      - TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384  # TLS 1.2
```

## Redirect HTTP to HTTPS in Spring Boot

```java
@Configuration
public class HttpsRedirectConfig {
    
    @Bean
    public TomcatServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {
            @Override
            protected void postProcessContext(Context context) {
                SecurityConstraint constraint = new SecurityConstraint();
                constraint.setUserConstraint("CONFIDENTIAL");   // requires HTTPS
                SecurityCollection collection = new SecurityCollection();
                collection.addPattern("/*");
                constraint.addCollection(collection);
                context.addConstraint(constraint);
            }
        };
        tomcat.addAdditionalTomcatConnectors(httpConnector());
        return tomcat;
    }
    
    private Connector httpConnector() {
        Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        connector.setScheme("http");
        connector.setPort(8080);       // HTTP port
        connector.setSecure(false);
        connector.setRedirectPort(8443); // redirect to HTTPS port
        return connector;
    }
}
```

## Making HTTPS Calls (RestTemplate / WebClient)

```java
// Default: uses JVM default TrustStore (cacerts) — works for public CAs
RestTemplate restTemplate = new RestTemplate();

// Custom TrustStore (for internal/self-signed CAs):
@Bean
public RestTemplate restTemplate() throws Exception {
    SSLContext sslContext = SSLContextBuilder.create()
        .loadTrustMaterial(
            new File("/etc/ssl/internal-ca.jks"),
            "truststore-password".toCharArray()
        )
        .build();
    
    CloseableHttpClient httpClient = HttpClients.custom()
        .setSSLContext(sslContext)
        .build();
    
    return new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
}

// WebClient with custom SSL:
@Bean
public WebClient webClient() throws Exception {
    SslContext sslContext = SslContextBuilder.forClient()
        .trustManager(new File("/etc/ssl/internal-ca.pem"))
        .build();
    
    HttpClient httpClient = HttpClient.create()
        .secure(t -> t.sslContext(sslContext));
    
    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();
}
```

## Java KeyStore / TrustStore — Common Operations

```java
// Load a PKCS12 keystore:
KeyStore keyStore = KeyStore.getInstance("PKCS12");
keyStore.load(new FileInputStream("keystore.p12"), "password".toCharArray());

// Get certificate from keystore:
X509Certificate cert = (X509Certificate) keyStore.getCertificate("myapp");
System.out.println("Expires: " + cert.getNotAfter());
System.out.println("Subject: " + cert.getSubjectDN());
System.out.println("Issuer:  " + cert.getIssuerDN());

// Check certificate expiry programmatically (alert before expiry!):
Date expiryDate = cert.getNotAfter();
long daysUntilExpiry = ChronoUnit.DAYS.between(
    Instant.now(), expiryDate.toInstant()
);
if (daysUntilExpiry < 30) {
    alertService.sendCertExpiryAlert(daysUntilExpiry);
}
```

---

# 12. TLS in Docker and Kubernetes

## Docker — Pass Certificates as Volumes

```yaml
# docker-compose.yml
services:
  api:
    image: myapp:latest
    ports:
      - "443:8443"
    volumes:
      - /etc/letsencrypt/live/api.example.com:/certs:ro  # read-only mount
    environment:
      - SSL_KEY_STORE=/certs/keystore.p12
      - SSL_KEY_PASSWORD=${SSL_KEY_PASSWORD}
```

## Kubernetes — TLS Termination at Ingress

In Kubernetes, typically **terminate TLS at the Ingress controller**, not inside each pod:

```text
Client ──HTTPS──→ Ingress Controller (nginx/Traefik) ──HTTP──→ Pod
                    (handles TLS here)              (internal plain HTTP)

Benefits:
  Pods don't need certs (simpler)
  One place to manage certs
  Ingress handles cert rotation
  Inside cluster: traffic is trusted (cluster network)
```

```yaml
# Kubernetes TLS Secret:
apiVersion: v1
kind: Secret
metadata:
  name: api-tls-secret
type: kubernetes.io/tls
data:
  tls.crt: <base64-encoded-fullchain.pem>
  tls.key: <base64-encoded-privkey.pem>

---
# Ingress with TLS:
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: api-ingress
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod   # cert-manager auto-issues cert
spec:
  tls:
    - hosts:
        - api.example.com
      secretName: api-tls-secret
  rules:
    - host: api.example.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: api-service
                port:
                  number: 8080   # HTTP internally
```

## cert-manager — Automatic Certificate Management in Kubernetes

```yaml
# ClusterIssuer (Let's Encrypt production):
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: admin@example.com
    privateKeySecretRef:
      name: letsencrypt-prod-key
    solvers:
      - http01:
          ingress:
            class: nginx    # uses nginx ingress to serve ACME challenge

# cert-manager then:
# 1. Watches Ingress annotations (cert-manager.io/cluster-issuer)
# 2. Creates Certificate request to Let's Encrypt
# 3. Solves HTTP-01 challenge via ingress
# 4. Stores issued cert in Kubernetes Secret
# 5. Auto-renews 30 days before expiry
```

---

# 13. TLS Best Practices Checklist

```text
Certificate:
  ✓ Use Let's Encrypt or trusted CA (not self-signed in production)
  ✓ Use fullchain.pem (cert + intermediates), not just cert.pem
  ✓ Include all necessary SANs (www, api, internal hostnames)
  ✓ Monitor expiry — alert at 30d, 14d, 7d, 1d
  ✓ Auto-renew (certbot cron or cert-manager)

Protocol versions:
  ✓ Enable TLS 1.2 and TLS 1.3 only
  ✗ Disable TLS 1.0 and TLS 1.1 (deprecated, vulnerable)
  ✗ Never allow SSL 2.0 / 3.0

Cipher suites:
  ✓ Use ECDHE key exchange (forward secrecy)
  ✗ Remove RC4, DES, 3DES (broken)
  ✗ Remove MD5, SHA-1 in signatures (weak)

HTTP security:
  ✓ Add HSTS header (Strict-Transport-Security: max-age=31536000; includeSubDomains)
  ✓ Redirect HTTP → HTTPS with 301 (or HSTS preload list)
  ✓ Set Secure flag on all cookies

Secrets:
  ✓ Store private keys with 600 permissions (owner read-only)
  ✓ Never commit private keys to git
  ✓ Use environment variables or secrets vault for keystore passwords
  ✗ Never disable certificate verification (no trustAllCerts in production!)

Testing:
  ✓ Test with ssllabs.com (target A+)
  ✓ Check certificate chain: openssl s_client -connect host:443
  ✓ Verify certificate expiry in monitoring
  ✓ Test TLS version restriction
```

---

# Interview Preparation — TLS

## Q1: What is TLS and what problem does it solve?

**Answer:**

TLS (Transport Layer Security) is a cryptographic protocol that sits between the application layer and the transport layer. It solves three problems:

1. **Confidentiality**: encrypts data so intermediaries (ISPs, WiFi operators, hackers) cannot read it
2. **Integrity**: ensures data hasn't been modified in transit (using MACs — Message Authentication Codes)
3. **Authentication**: proves the server is who it claims to be (via X.509 certificates signed by trusted CAs)

Without TLS (plain HTTP), every piece of data — passwords, credit card numbers, private messages — travels as readable text visible to anyone on the network path. With TLS (HTTPS), only the communicating parties can read the data.

## Q2: What is a TLS certificate and what does it contain?

**Answer:**

A TLS certificate is a digital document that proves a server's identity. It's the digital equivalent of a passport.

It contains:
- **Subject**: who the certificate belongs to (`CN=api.example.com`)
- **Subject Alternative Names (SANs)**: all hostnames this cert is valid for
- **Public key**: the server's public key (used in the TLS handshake to establish encrypted session)
- **Issuer**: which Certificate Authority signed this certificate
- **Validity period**: Not Before / Not After dates
- **CA's digital signature**: proves the CA vouches for all the above

The certificate is signed by a Certificate Authority (CA) — a trusted third party like Let's Encrypt, DigiCert, etc. Browsers and OSes ship with ~150 trusted root CA certificates pre-installed. A server cert is trusted if it chains up to one of these root CAs.

## Q3: What is the difference between a keystore and a truststore in Java?

**Answer:**

- **KeyStore**: contains YOUR identity — your certificate and private key. Used when you need to prove who you are (server presenting its cert to clients, or client presenting cert in mTLS). Format: PKCS12 (.p12) or JKS.

- **TrustStore**: contains certificates of CAs you TRUST — used to verify the other party's certificate chain. When a Java client connects to a server, it checks the server's certificate against its truststore. JVM ships with `cacerts` (~150 trusted root CAs) as the default truststore.

Simple analogy: KeyStore = your passport. TrustStore = your list of countries whose passports you accept.

Common error `PKIX path building failed` means the server's cert chain doesn't trace to any CA in your truststore — typically because the server uses an internal/private CA not in `cacerts`.

## Q4: What is HSTS and why is it important?

**Answer:**

HSTS (HTTP Strict Transport Security) is a response header that tells the browser to ALWAYS use HTTPS for this domain, never HTTP — and to remember this for `max-age` seconds.

```
Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
```

Without HSTS: a user navigating to `example.com` (no scheme typed) makes an initial HTTP request. A network attacker can intercept this HTTP request and perform an SSL stripping attack — serving fake HTTP content without TLS ever being established.

With HSTS: after the first HTTPS visit, the browser remembers that this site is HTTPS-only and never makes an HTTP request to it — SSL stripping becomes impossible for returning users.

With HSTS preloading (`preload` directive + submission to hstspreload.org): browsers know the site is HTTPS-only even before the FIRST visit — full protection from day one.

## Q5: What is the difference between DV, OV, and EV certificates?

**Answer:**

- **DV (Domain Validated)**: CA verifies you control the domain (DNS record or HTTP file). Automated, free (Let's Encrypt), issued in minutes. Sufficient for 99% of use cases.

- **OV (Organization Validated)**: CA verifies domain control + that the organization legally exists. Takes days, costs money. Used by organizations where org identity matters (financial, healthcare).

- **EV (Extended Validation)**: Rigorous identity verification. Used to show a green bar with company name in browsers — but major browsers removed this visual indicator in 2019. Now EV and OV look identical to users. EV is largely obsolete.

For most backend developers: **use Let's Encrypt DV certificates**. They're free, automated, and cryptographically identical in strength to paid OV/EV certificates.
