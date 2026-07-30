import smtplib
import random
import os
import threading
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart

SMTP_HOST = os.getenv("SMTP_HOST", "smtp.gmail.com")
SMTP_PORT = int(os.getenv("SMTP_PORT", "587"))
SMTP_USER = os.getenv("SMTP_USER", "")
SMTP_PASSWORD = os.getenv("SMTP_PASSWORD", "")
SMTP_FROM = os.getenv("SMTP_FROM", SMTP_USER)

def generate_verification_code() -> str:
    return str(random.randint(100000, 999999))

def _send_email(to_email: str, code: str):
    try:
        if not SMTP_USER or not SMTP_PASSWORD:
            print(f"[EMAIL SIMULATED] To: {to_email}, Code: {code}")
            return

        msg = MIMEMultipart()
        msg["From"] = SMTP_FROM
        msg["To"] = to_email
        msg["Subject"] = "TravelHub - Código de verificación"

        body = f"""
        <html>
        <body style="font-family: Arial, sans-serif; padding: 20px;">
            <h2 style="color: #D26A4B;">TravelHub</h2>
            <p>Gracias por registrarte. Tu código de verificación es:</p>
            <h1 style="color: #1A6B6B; font-size: 36px; letter-spacing: 8px;">{code}</h1>
            <p>Este código expira en 10 minutos.</p>
            <p>Si no solicitaste este registro, ignora este correo.</p>
        </body>
        </html>
        """

        msg.attach(MIMEText(body, "html"))

        with smtplib.SMTP(SMTP_HOST, SMTP_PORT, timeout=10) as server:
            server.starttls()
            server.login(SMTP_USER, SMTP_PASSWORD)
            server.send_message(msg)

        print(f"[EMAIL SENT] To: {to_email}")
    except Exception as e:
        print(f"[EMAIL FAILED] To: {to_email}, Error: {e}")

def send_verification_email(to_email: str, code: str):
    threading.Thread(target=_send_email, args=(to_email, code), daemon=True).start()
