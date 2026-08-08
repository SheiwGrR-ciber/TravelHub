import html
import logging
import os
import random
import threading

import requests

logger = logging.getLogger(__name__)

RESEND_API_URL = "https://api.resend.com/emails"
RESEND_API_KEY = os.getenv("RESEND_API_KEY", "")
RESEND_FROM = os.getenv("RESEND_FROM", "")


def generate_verification_code() -> str:
    return str(random.SystemRandom().randint(100000, 999999))


def _verification_html(code: str) -> str:
    safe_code = html.escape(code)
    return f"""
    <html>
      <body style="font-family:Arial,sans-serif;background:#fff8ed;padding:24px;color:#33251f">
        <div style="max-width:520px;margin:auto;background:white;padding:28px;border-radius:16px">
          <h1 style="color:#c76446;margin-top:0">TravelHub</h1>
          <p>Gracias por registrarte. Tu código de verificación es:</p>
          <p style="font-size:36px;font-weight:bold;letter-spacing:8px;color:#176b6b">{safe_code}</p>
          <p>Este código expira en 10 minutos.</p>
          <p style="color:#75655d">Si no solicitaste esta cuenta, puedes ignorar el mensaje.</p>
        </div>
      </body>
    </html>
    """


def _send_email(to_email: str, code: str) -> None:
    if not RESEND_API_KEY or not RESEND_FROM:
        logger.warning("Email simulado para %s. Código: %s", to_email, code)
        return

    try:
        response = requests.post(
            RESEND_API_URL,
            headers={
                "Authorization": f"Bearer {RESEND_API_KEY}",
                "Content-Type": "application/json",
                "User-Agent": "TravelHub/1.0",
            },
            json={
                "from": RESEND_FROM,
                "to": [to_email],
                "subject": "TravelHub - Código de verificación",
                "html": _verification_html(code),
                "text": f"Tu código de verificación de TravelHub es {code}. Expira en 10 minutos.",
            },
            timeout=15,
        )
        response.raise_for_status()
        email_id = response.json().get("id", "desconocido")
        logger.info("Correo de verificación enviado a %s (Resend ID: %s)", to_email, email_id)
    except requests.RequestException as exception:
        response_body = getattr(exception.response, "text", "") if exception.response else ""
        logger.error("Resend no pudo enviar a %s: %s %s", to_email, exception, response_body)


def send_verification_email(to_email: str, code: str) -> None:
    threading.Thread(target=_send_email, args=(to_email, code), daemon=True).start()
