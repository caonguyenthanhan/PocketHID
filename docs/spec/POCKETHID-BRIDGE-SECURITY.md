# PocketHID Bridge Security Specification & Threat Model

**Specification Version:** `1.0.0-DRAFT`  
**Status:** ARCHITECTURAL SPECIFICATION & POC THREAT MODEL  
**Authentication Status:** `POC LIMITATION — NOT COMPLETE`  

---

## 1. Threat Model

Because the PocketHID external bridge possesses full capability to inject arbitrary physical keyboard keystrokes, relative mouse motion, clicks, and digitizer strokes directly into the host operating system (Windows, macOS, Linux), unauthorized access represents a critical security risk:

1. **Malicious Keystroke Injection (BadUSB / Remote Keystroke Injection):** An unauthorized nearby attacker connecting to the bridge over BLE could inject arbitrary keystrokes (e.g. `Win+R`, launching PowerShell scripts, downloading malware, adding user accounts).
2. **Replay Attacks:** An eavesdropper recording legitimate BLE packets could replay past keystrokes or mouse clicks.
3. **Session Hijacking:** An attacker disconnecting the legitimate iPhone and immediately establishing a connection to take over control.
4. **Denial of Service / Stuck Keys:** Malicious packets or RF jamming interrupting communication during an active key-down sequence, causing stuck modifiers or repeating keys.

---

## 2. BLE Pairing

- **BLE Security Modes:**
  - *POC Phase:* Bluetooth LE "Just Works" pairing (Unauthenticated pairing with encryption).
  - *Production Target:* Security Mode 1, Level 4 (LE Secure Connections with Numeric Comparison or 6-digit Passkey display on bridge hardware / OLED or Serial log).
- **Bonding Storage:**
  - Bridge maintains bonding records in encrypted non-volatile storage (NVS). Once paired, the bridge can restrict advertisement and connection acceptance exclusively to the bonded iPhone IRK (Identity Resolving Key).

---

## 3. Authentication

```
AUTHENTICATION:
POC LIMITATION — NOT COMPLETE
```

- **Fundamental Rule:** CRC-16-CCITT is an error-detection code; **CRC IS NOT AUTHENTICATION**. Nonces exchanged in `MSG_SYS_HELLO` and `MSG_SYS_READY` without cryptographic message authentication codes (HMAC / AES-CMAC) do NOT constitute a cryptographically authenticated channel.
- **POC Implementation:** The current POC implements protocol session identity, sequence checking, and connection state gating, but intentionally omits ad-hoc custom cryptography to avoid weak "security through obscurity".

---

## 4. Replay Protection

1. **Monotonically Increasing Sequence Numbers:**
   - Every packet carries a 16-bit `SEQUENCE_NO` (`0x0000` to `0xFFFF`).
   - The bridge validates that each incoming sequence number strictly advances beyond the previous valid sequence number:
     $$\Delta = (current\_seq - last\_seq) \pmod{65536}$$
     Valid progression requires $0 < \Delta < 32768$.
   - Any packet with $\Delta = 0$ (exact duplicate) or $\Delta \ge 32768$ (sequence regression) is immediately rejected and increments `diagnostics.total_packets_malformed`.
2. **Session Rollover & Disconnect Reset:**
   - Sequence numbering resets only upon clean `handle_ble_connection(connected = true)` after a completed handshake.

---

## 5. Integrity

- Every packet header and payload is protected by a 16-bit **CRC-16-CCITT** checksum (polynomial `0x1021`, initial value `0xFFFF`).
- The CRC validates against radio packet corruption and transmission errors.
- Any packet failing the CRC check is discarded without updating any HID state.

---

## 6. Authorization

- **Session Authorization State Machine:**
  - `STATE_UNPAIRED`: The bridge accepts only `MSG_SYS_HELLO`. All incoming keyboard/mouse/gamepad/tablet reports are rejected.
  - `STATE_ACTIVE_AUTHORIZED`: Established only after valid handshake and sequence validation.
  - `STATE_FAULT`: Entered if $\ge 3$ consecutive malformed or sequence-regressed packets arrive. Automatically executes `force_all_neutral()` and revokes authorization until re-authenticated.

---

## 7. Key Management

- In the production architecture, keys must be derived during Bluetooth LE Secure Connections pairing via Diffie-Hellman P-256 key exchange.
- Long Term Keys (LTK) must be stored in ESP32-S3 hardware-protected flash with flash encryption enabled.
- Zero hardcoded pre-shared keys (PSKs) may exist in the firmware source code.

---

## 8. POC Limitations

1. **No Application-Layer Cryptographic MAC:** Packets are transmitted over BLE without per-packet AES-CCM / HMAC authentication.
2. **"Just Works" Pairing:** Vulnerable to active Man-in-the-Middle (MITM) attacks during initial pairing if executed in an untrusted radio environment.
3. **Open Advertisement in POC:** The bridge advertises to all scanning centrals during test mode.

---

## 9. Production Requirements

Before any bridge firmware is certified for production deployment:
1. **LE Secure Connections Mandatory:** Mandatory Passkey or Numeric Comparison pairing.
2. **Direct Bonding Restriction:** Whitelist-based directed advertising to bonded iOS device IRK only.
3. **Application-Layer Challenge-Response:** Optional Ed25519 or AES-128-GCM mutual authentication handshake embedded in `MSG_SYS_HELLO` / `MSG_SYS_READY`.
4. **Flash Encryption & Secure Boot v2:** Hardware security features of ESP32-S3 enabled to prevent firmware tampering and key extraction.
