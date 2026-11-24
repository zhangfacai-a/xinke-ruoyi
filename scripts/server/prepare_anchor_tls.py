"""Prepare a TLS certificate pinned by the desktop app; never print private keys."""
from pathlib import Path
from datetime import datetime, timedelta, timezone
from ipaddress import ip_address
import json, secrets, hashlib
from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.hazmat.primitives.serialization import pkcs12
from cryptography.x509.oid import NameOID

root=Path(__file__).resolve().parents[2]
stage=root/'.codex-deploy/anchor-tls'
stage.mkdir(parents=True,exist_ok=True)
settings=stage/'server.properties'
if not settings.exists():
    key=rsa.generate_private_key(public_exponent=65537,key_size=3072)
    subject=x509.Name([x509.NameAttribute(NameOID.COMMON_NAME,'XinKe Anchor Desktop API')])
    now=datetime.now(timezone.utc)
    cert=(x509.CertificateBuilder().subject_name(subject).issuer_name(subject).public_key(key.public_key())
        .serial_number(x509.random_serial_number()).not_valid_before(now-timedelta(days=1)).not_valid_after(now+timedelta(days=730))
        .add_extension(x509.SubjectAlternativeName([x509.IPAddress(ip_address('47.116.62.234')),x509.DNSName('xinkeerp.cn')]),critical=False)
        .add_extension(x509.BasicConstraints(ca=False,path_length=None),critical=True).sign(key,hashes.SHA256()))
    password=secrets.token_hex(32)
    (stage/'server.p12').write_bytes(pkcs12.serialize_key_and_certificates(b'xinke-anchor',key,cert,None,serialization.BestAvailableEncryption(password.encode())))
    settings.write_text('port=443\nkeystore=/home/deploy/anchor-tls/server.p12\npassword='+password+'\n',encoding='ascii')
    (stage/'certificate.der').write_bytes(cert.public_bytes(serialization.Encoding.DER))
fingerprint=hashlib.sha256((stage/'certificate.der').read_bytes()).hexdigest().upper()
(root/'clients/anchor-metrics-probe/desktop-certificate-pin.txt').write_text(fingerprint,encoding='ascii')
print('TLS configuration prepared; SHA256 pin:',fingerprint)
