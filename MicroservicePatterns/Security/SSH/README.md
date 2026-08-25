# SSH — Deep Dive

SSH (Secure Shell) is the protocol engineers use to securely access remote servers, transfer files, and create secure tunnels. Understanding how it works — especially public key authentication — is essential for DevOps, CI/CD, and production access.

> **SSH is what you use when you type `ssh ubuntu@10.0.0.5`. Understanding the cryptography behind it explains why SSH keys are more secure than passwords and how to use them safely.**

---

# 1. What is SSH and Why It Exists

Before SSH: **Telnet** was used to access remote servers.

```text
Telnet (pre-SSH):
  Client ──── username=admin, password=secret123 ────→ Server
  
  Sent in PLAIN TEXT — anyone on the network could read it!
  Coffee shop WiFi → anyone could see your password

SSH (1995, openssh):
  Client ────── [ENCRYPTED everything] ─────────────→ Server
  Password, commands, output — all encrypted end-to-end
  Man-in-the-middle sees only gibberish
```

SSH provides:
1. **Encrypted channel**: all communication is encrypted
2. **Server authentication**: prevents connecting to a fake server (host key)
3. **Client authentication**: proves who you are to the server (password or key)

---

# 2. SSH Architecture

```text
SSH has three layers:

┌─────────────────────────────────────────┐
│  SSH Connection Layer                    │  (channels: shell, port forward, SCP)
├─────────────────────────────────────────┤
│  SSH Authentication Layer               │  (who are you? password or key)
├─────────────────────────────────────────┤
│  SSH Transport Layer                     │  (TCP, server verification, encryption)
└─────────────────────────────────────────┘
              TCP port 22
```

---

# 3. SSH Connection Flow

```text
Client                              Server (sshd)
  |                                   |
  |── TCP connect (port 22) ─────────→|
  |                                   |
  |←── SSH-2.0-OpenSSH_9.0 ──────────|  SSH version banner
  |── SSH-2.0-OpenSSH_9.3 ──────────→|  Client version banner
  |                                   |
  |════ Key Exchange (KEX) ═══════════|  Negotiate encryption, exchange keys
  |  Client proposes: kex algorithms, |
  |    host key types, ciphers, MACs  |
  |  Server replies: chosen algorithms|
  |  Diffie-Hellman exchange          |
  |  → Both sides compute session key |
  |                                   |
  |←── Server Host Key + Signature ──|  "I am really server 10.0.0.5"
  |  [Client checks known_hosts]      |  ← CRITICAL STEP
  |                                   |
  |══ All further messages encrypted ═|
  |                                   |
  |── Auth request ─────────────────→|  "I am user alice"
  |   Method: publickey or password   |
  |                                   |
  |←── Auth success / failure ────────|
  |                                   |
  |── Channel open (session) ────────→|
  |←── Channel open confirm ──────────|
  |                                   |
  |── Shell request / exec ──────────→|
  |←── Shell / command output ────────|  ← Your terminal session
```

---

# 4. Server Authentication — Host Keys

When you first connect to a server, SSH shows:

```text
$ ssh ubuntu@10.0.0.5
The authenticity of host '10.0.0.5 (10.0.0.5)' can't be established.
ED25519 key fingerprint is SHA256:abc123xyz...
Are you sure you want to continue connecting (yes/no)?
```

**What's happening:**

```text
Server has a host key pair (generated at SSH install):
  /etc/ssh/ssh_host_ed25519_key       (private — never leaves server)
  /etc/ssh/ssh_host_ed25519_key.pub   (public — sent to clients)

First connection:
  Server sends its public host key
  Client: "I've never seen this server before. Is this fingerprint correct?"
  User types "yes"
  → Client stores fingerprint in ~/.ssh/known_hosts
  
Subsequent connections:
  Server sends host key
  Client: checks ~/.ssh/known_hosts for this IP/hostname
  → Fingerprint matches → proceed automatically
  → Fingerprint MISMATCH → BIG WARNING → possible man-in-the-middle attack!
```

```text
$ cat ~/.ssh/known_hosts
10.0.0.5 ssh-ed25519 AAAA...xyz=
github.com ssh-ed25519 AAAA...abc=
```

**The known_hosts warning is serious:**

```text
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
@    WARNING: REMOTE HOST IDENTIFICATION HAS CHANGED!     @
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
IT IS POSSIBLE THAT SOMEONE IS DOING SOMETHING NASTY!
```

This can mean:
- Server was reinstalled (new host key) → verify with admin, then remove old entry from known_hosts
- Man-in-the-middle attack intercepting your connection → do NOT proceed!

---

# 5. Client Authentication — Two Methods

## Method 1: Password Authentication

```text
Client: "I am alice, my password is: [encrypted]secret123"
Server: checks /etc/shadow for alice's password hash
        bcrypt(secret123) == stored_hash → GRANT ACCESS

Problems:
  ✗ Brute-forceable (attackers try millions of passwords)
  ✗ Password can be guessed or stolen
  ✗ Weak passwords used in practice
  ✗ Requires human to type password each time
  ✗ Can't be used in automated scripts securely

Most security-conscious setups DISABLE password auth:
  /etc/ssh/sshd_config:
    PasswordAuthentication no
```

## Method 2: Public Key Authentication (Recommended)

This is the cryptographically secure method. Understand it deeply.

```text
Math foundation:
  Private key: a secret number K_priv (4096 bits for RSA, 256 bits for Ed25519)
  Public key:  K_pub (derived from K_priv using elliptic curve / RSA math)
  
  Property: sign(data, K_priv) → signature
             verify(data, signature, K_pub) → true/false
  
  Cannot reverse-engineer K_priv from K_pub
  Cannot forge a valid signature without K_priv
```

### Setup

```bash
# Step 1: Generate key pair (on your local machine)
ssh-keygen -t ed25519 -C "alice@company.com" -f ~/.ssh/id_ed25519

# Creates:
~/.ssh/id_ed25519        ← private key (NEVER share! treat like a password)
~/.ssh/id_ed25519.pub    ← public key (safe to share freely)

# Private key (example RSA, abbreviated):
-----BEGIN OPENSSH PRIVATE KEY-----
b3BlbnNzaC1rZXktdjEAAAAA...
-----END OPENSSH PRIVATE KEY-----

# Public key:
ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAI... alice@company.com

# Step 2: Copy public key to server
ssh-copy-id -i ~/.ssh/id_ed25519.pub ubuntu@10.0.0.5
# Appends public key to: /home/ubuntu/.ssh/authorized_keys
```

### Authentication Flow

```text
Client                              Server
  |                                   |
  |── "I'm alice, using key K_pub" ──→|
  |                                   |
  | [Server checks authorized_keys]   |
  |   Is K_pub in authorized_keys? YES|
  |                                   |
  |←── Challenge: random_bytes ───────|
  |                                   |
  | [Client signs with private key]   |
  |   signature = sign(random_bytes, K_priv)
  |                                   |
  |── signature ─────────────────────→|
  |                                   |
  | [Server verifies signature]       |
  |   verify(random_bytes, sig, K_pub)|
  |   → If valid → identity confirmed |
  |                                   |
  |←── Auth success ──────────────────|

Private key NEVER travels over the network.
Attacker capturing traffic cannot derive private key.
```

Why this is more secure than passwords:
- Private key is mathematically impossible to brute-force (256-bit or 4096-bit)
- Private key never transmitted — only a signature (useless without the key)
- Can use passphrase to protect private key at rest
- Automatable — scripts can authenticate without human typing a password

---

# 6. SSH Key Algorithms

```text
RSA (legacy, still widely used):
  Key size: 2048-4096 bits (use 4096)
  Speed:    slower (complex math)
  ssh-keygen -t rsa -b 4096
  Use when: server only supports RSA (old systems)

ECDSA (elliptic curve):
  Key size: 256, 384, 521 bits
  Speed:    faster than RSA
  ssh-keygen -t ecdsa -b 521
  Issue: curves may have NSA backdoors (P-256 paranoia)

Ed25519 (modern, recommended):
  Key size: 256 bits (equivalent security to RSA 3072+)
  Speed:    fastest
  Security: mathematically clean, no NSA curve concerns
  ssh-keygen -t ed25519
  Use this for all new keys!
```

---

# 7. SSH Agent — No More Typing Passphrase

Your private key can have a passphrase (password protecting the key file). Without an SSH agent, you'd type the passphrase on every connection.

```text
ssh-agent stores your decrypted private key in memory for the session:

$ eval $(ssh-agent)              # start agent
$ ssh-add ~/.ssh/id_ed25519      # add key (enter passphrase ONCE)
Enter passphrase: ****

# Now SSH connections use the agent automatically — no passphrase prompt:
$ ssh ubuntu@server1             # agent provides signature
$ ssh ubuntu@server2             # agent provides signature again
$ git push                       # git over SSH — agent handles it
```

SSH agent forwarding allows remote servers to use your local agent:

```text
Your laptop ──ssh──→ Jump Server ──ssh──→ Production Server
  (has private key in agent)

Without forwarding: Production Server can't auth to other services
With forwarding (-A flag or ForwardAgent yes in config):
  Production Server asks your laptop's agent for signatures
  Private key NEVER leaves your laptop!

Security: only use agent forwarding to fully trusted servers
          Malicious root on jump server could abuse forwarding
```

---

# 8. SSH Config File

Instead of remembering long `ssh` commands, use `~/.ssh/config`:

```text
# ~/.ssh/config

Host prod-server
  HostName 10.0.0.5
  User ubuntu
  IdentityFile ~/.ssh/prod_key
  Port 22

Host bastion
  HostName jump.example.com
  User ec2-user
  IdentityFile ~/.ssh/bastion_key

Host prod-via-bastion
  HostName 10.0.0.10
  User ubuntu
  ProxyJump bastion          # ← SSH through bastion first!
  IdentityFile ~/.ssh/prod_key

# Now: ssh prod-server  (instead of: ssh -i ~/.ssh/prod_key ubuntu@10.0.0.5)
```

---

# 9. SSH Tunneling and Port Forwarding

SSH tunnels create secure channels for other traffic — a major feature beyond just shell access.

## Local Port Forwarding

```text
$ ssh -L 5432:db.internal:5432 ubuntu@bastion

Meaning:
  "Open port 5432 on MY machine (localhost:5432)
   and forward that traffic through bastion to db.internal:5432"

Use case: access a database (db.internal) that's only accessible from inside the network
  
  Your Machine              Bastion              Database
  localhost:5432  ───SSH───→ bastion ──────────→ db.internal:5432
  
  psql -h localhost -p 5432 -U postgres  ← connects via tunnel!
```

## Remote Port Forwarding

```text
$ ssh -R 8080:localhost:3000 ubuntu@server.example.com

Meaning:
  "Make port 8080 on the REMOTE server forward to MY localhost:3000"

Use case: expose a local dev server to someone else (webhook testing)

  Remote Server             Your Machine
  server:8080  ───SSH───→   localhost:3000
  
  Webhook provider calls server.example.com:8080 → reaches your local server!
```

## Dynamic Port Forwarding (SOCKS Proxy)

```text
$ ssh -D 1080 ubuntu@jump-server

Creates a SOCKS5 proxy on your local port 1080.
Configure browser to use SOCKS proxy: localhost:1080.
All browser traffic routes through jump-server.

Use case: browse securely from a corporate jump server, bypass network restrictions
```

---

# 10. SCP and SFTP — File Transfer over SSH

SSH includes protocols for secure file transfer:

```bash
# SCP (Secure Copy — simple, one-time transfers)
scp file.txt ubuntu@server:/home/ubuntu/
scp ubuntu@server:/logs/app.log ./downloads/
scp -r ./project ubuntu@server:/home/ubuntu/   # -r for directories

# SFTP (SSH FTP — interactive, like FTP but over SSH)
sftp ubuntu@server
sftp> ls
sftp> put file.txt
sftp> get remote-file.txt
sftp> quit

# rsync over SSH (incremental, efficient for large syncs)
rsync -avz --progress ./build/ ubuntu@server:/var/www/html/
```

---

# 11. SSH in CI/CD Pipelines

```yaml
# GitHub Actions — deploying via SSH
jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - name: Deploy to production
        env:
          SSH_PRIVATE_KEY: ${{ secrets.SSH_PRIVATE_KEY }}
        run: |
          mkdir -p ~/.ssh
          echo "$SSH_PRIVATE_KEY" > ~/.ssh/deploy_key
          chmod 600 ~/.ssh/deploy_key
          ssh -i ~/.ssh/deploy_key -o StrictHostKeyChecking=no \
            ubuntu@prod-server \
            "cd /app && git pull && ./deploy.sh"
```

Best practices for CI/CD SSH:
```text
✓ Use dedicated deploy keys (not your personal SSH key)
✓ Generate key without passphrase (pipeline can't type it)
✓ Store private key in secrets vault (not in code!)
✓ Restrict deploy key: read-only repo access (GitHub deploy keys)
✓ Restrict what commands it can run (authorized_keys command= option)
✓ Rotate keys periodically
```

---

# 12. SSH vs TLS — Side by Side

| | TLS | SSH |
|---|---|---|
| **Purpose** | Encrypt any TCP stream (HTTP → HTTPS) | Secure remote shell + file transfer |
| **Transport** | TCP (any port) | TCP port 22 |
| **Server identity** | X.509 certificate (CA-signed) | Host key (Trust On First Use) |
| **Client identity** | Optional (mTLS) | Password or public key (common) |
| **Key format** | X.509 / PEM certificates | OpenSSH key files |
| **PKI needed** | Yes (Certificate Authority) | No (keys are self-generated) |
| **Session resumption** | Yes (session tickets, 0-RTT) | Yes (ControlMaster multiplexing) |
| **Protocol design** | General-purpose encrypted transport | Specific to shell, file transfer, tunneling |
| **Used by** | HTTPS, gRPC, SMTP+TLS, databases | Developers, CI/CD, server management |

## Shared Concepts

Both use:
- **Diffie-Hellman key exchange** (deriving shared session key without transmitting it)
- **Asymmetric keys** for identity verification
- **Symmetric encryption** for actual data (AES, ChaCha20)
- **MAC/HMAC** for data integrity

The cryptographic foundations are nearly identical. TLS is optimized for the web (CA-based PKI, HTTPS), SSH is optimized for interactive server access (TOFU host keys, shell).

---

# Interview Preparation — SSH

## Q1: How does SSH public key authentication work?

**Answer:**

1. You generate a key pair: private key (stays on your machine) + public key (copied to server's `~/.ssh/authorized_keys`)

2. When connecting, client announces: "I have a private key corresponding to this public key"

3. Server looks up the public key in `authorized_keys` — if found, generates a random challenge

4. Client signs the challenge with its private key and sends the signature

5. Server verifies the signature using the stored public key. Valid signature = the client possesses the matching private key = authenticated

The private key **never leaves the client** — only a signature travels. This is why public key auth is vastly more secure than passwords: the secret never crosses the network, and it's mathematically infeasible to brute-force a 256-bit key.

## Q2: What is the purpose of known_hosts?

**Answer:**

`known_hosts` stores the fingerprints of servers you've connected to. It solves the "how do you know you're talking to the real server?" problem.

On first connection, SSH shows the server's fingerprint and asks you to verify it. Once you accept, it's stored in `~/.ssh/known_hosts`. On every subsequent connection, SSH verifies the server's current fingerprint matches the stored one.

If the fingerprint changes — new server setup (legit) or a man-in-the-middle attack (dangerous) — SSH shows a large warning and refuses to connect by default. This protects against attackers intercepting your connection before the encrypted channel is established.

## Q3: What is SSH tunneling and give a real-world use case?

**Answer:**

SSH tunneling creates an encrypted channel that other protocols can use — it's like a VPN through an SSH connection.

**Local port forwarding** real-world use: you have a production database (PostgreSQL on port 5432) that's firewalled — only accessible from inside the VPC. Your jump server has VPC access.

```bash
ssh -L 5432:prod-db.internal:5432 ubuntu@jump-server
```

Now `localhost:5432` on your machine connects through the jump server to the database. You can use any local database tool (DBeaver, psql) to connect to `localhost:5432` as if the database were local — fully encrypted through SSH.

## Q4: Why use Ed25519 keys instead of RSA?

**Answer:**

- **Smaller keys**: Ed25519 uses 256-bit keys providing equivalent security to RSA 3072+ bit keys
- **Faster**: Ed25519 operations are significantly faster (elliptic curve vs RSA modular arithmetic)
- **More secure**: Based on Curve25519, designed with clean security properties; no known NSA-influence concerns (unlike NIST P-curves)
- **Simpler**: Ed25519 has no key size choice to make (RSA: 2048, 3072, 4096?)
- **Modern standard**: supported everywhere modern SSH is deployed (OpenSSH 6.5+, 2014)

Use Ed25519 for all new keys. Only fall back to RSA 4096 when connecting to old systems that don't support Ed25519.

## Q5: What is the difference between SSH and TLS? When would you use each?

**Answer:**

**TLS** is a general-purpose encrypted transport layer used to secure any TCP protocol. HTTPS = HTTP over TLS. gRPC uses TLS. Database connections use TLS. It uses X.509 certificates signed by Certificate Authorities — a proper PKI.

**SSH** is a specific protocol designed for secure remote shell access, file transfer, and tunneling. It uses host keys (Trust On First Use) rather than CA-signed certificates, and client keys stored in `authorized_keys`. No CA infrastructure needed.

Use TLS when: encrypting HTTP traffic, service-to-service API calls, database connections, any network service that needs encryption + server identity verification via CA chain.

Use SSH when: accessing servers interactively, deploying code via CI/CD, transferring files (SCP/rsync), creating secure tunnels to access internal services, connecting to cloud VMs (AWS EC2, GCP).

Both use the same underlying cryptographic primitives (DH key exchange, asymmetric keys for identity, symmetric encryption for data), but are optimized for entirely different use cases.
