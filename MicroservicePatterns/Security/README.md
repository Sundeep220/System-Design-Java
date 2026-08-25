# Security Protocols — Navigation Guide

TLS, mTLS, and SSH are the three foundational security protocols every backend engineer encounters daily. This folder covers how each works internally, not just what it is.

---

## Reading Order

| # | Folder | What You Learn |
|---|---|---|
| 1 | `TLS/` | **Start here.** What TLS is, HTTPS = HTTP + TLS, certificate types (DV/OV/EV/Wildcard/SAN), Let's Encrypt + ACME, certificate lifecycle, common TLS errors and fixes, debugging tools (openssl/curl), HSTS, TLS in Java/Spring Boot/Kubernetes |
| 2 | `TLS-mTLS/` | **Go deeper.** SSL vs TLS history, TLS 1.2 vs 1.3 handshake step-by-step, cryptography fundamentals (symmetric vs asymmetric, forward secrecy), mTLS (mutual auth), mTLS in Istio/service mesh, certificate pinning |
| 3 | `SSH/` | SSH protocol, password vs public key auth, host keys + known_hosts, SSH agent, tunneling/port forwarding, SCP/SFTP, CI/CD deploy keys, SSH vs TLS comparison |

Read `TLS/` first for the practical side, then `TLS-mTLS/` for the cryptographic internals, then `SSH/` — SSH uses similar key concepts and comparing it after TLS makes it much clearer.

---

## The One-Line Difference

```text
TLS:   Server proves its identity to the client. Client is anonymous.
       → HTTPS: you know you're talking to real amazon.com (not a fake site)
         Amazon doesn't cryptographically know who YOU are (you use passwords/cookies for that)

mTLS:  BOTH sides prove identity using certificates.
       → Service A proves to Service B that it's really Service A, not a rogue process
         Used in microservices, zero-trust networks

SSH:   Asymmetric key auth for interactive shell access to servers.
       → You prove to the server you're you (using a private key)
         Used for: deploying code, managing servers, database tunnels
```

---

## Protocol Comparison Table

| | TLS (one-way) | mTLS | SSH |
|---|---|---|---|
| **Who authenticates** | Server only | Both client and server | Server (host key) + client (password or key) |
| **Auth mechanism** | X.509 certificate | X.509 certificate (both sides) | Host key (server) + SSH key pair or password (client) |
| **Primary use** | HTTPS, encrypting any TCP | Service-to-service (microservices), zero-trust | Remote shell, file transfer, tunneling |
| **Port** | 443 (HTTPS), any | Any | 22 |
| **Certificate Authority needed** | Yes (for server cert) | Yes (for both certs) | No (trust on first use, or known_hosts) |
| **Who uses it** | Every website | Microservice mesh (Istio, Linkerd), K8s | DevOps, CI/CD, server admin |
| **Key format** | X.509 / PEM | X.509 / PEM | OpenSSH format (RSA, Ed25519) |

---

## Connection to Other Topics

```
HTTP/Versions/ → TLS-mTLS/
  HTTP/2 and HTTP/3 require TLS in practice
  HTTP/3 (QUIC) has TLS 1.3 built into the protocol

TLS-mTLS/ → MicroserviceRoadmap/
  mTLS is a core zero-trust security pattern for microservices
  Istio service mesh automates mTLS between all pods in Kubernetes

SSH/ → CI/CD pipelines
  GitHub Actions, GitLab CI use SSH keys for deployment
  Database admins use SSH tunnels to access production DBs safely
```
