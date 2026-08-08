from fastapi import APIRouter, WebSocket, WebSocketDisconnect, Depends, Query, HTTPException
from sqlalchemy.orm import Session
from app.db.database import get_db
from app.models.message import Message
from app.models.booking import Booking
from app.models.service import Service
from app.models.user import User
import json
import jwt
from app.config import SECRET_KEY, ALGORITHM

router = APIRouter()

class ConnectionManager:
    def __init__(self):
        self.active_connections: dict[int, list[WebSocket]] = {}

    async def connect(self, websocket: WebSocket, user_id: int):
        await websocket.accept()
        if user_id not in self.active_connections:
            self.active_connections[user_id] = []
        self.active_connections[user_id].append(websocket)

    def disconnect(self, websocket: WebSocket, user_id: int):
        if user_id in self.active_connections:
            self.active_connections[user_id].remove(websocket)
            if not self.active_connections[user_id]:
                del self.active_connections[user_id]

    async def send_to_user(self, user_id: int, message: dict):
        if user_id in self.active_connections:
            for conn in self.active_connections[user_id]:
                await conn.send_json(message)

manager = ConnectionManager()

async def get_user_from_token(token: str, db: Session) -> User:
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        email = payload.get("sub")
        if email is None:
            return None
        user = db.query(User).filter(User.email == email).first()
        return user
    except jwt.InvalidTokenError:
        return None

@router.websocket("/ws/chat")
async def websocket_chat(websocket: WebSocket, token: str = Query(...)):
    db = next(get_db())
    try:
        user = await get_user_from_token(token, db)
        if not user:
            await websocket.close(code=4001, reason="Token inválido")
            return

        await manager.connect(websocket, user.id)
        try:
            while True:
                data = await websocket.receive_json()
                action = data.get("action")

                if action == "send_message":
                    booking_id = data["booking_id"]
                    receiver_id = data["receiver_id"]
                    content = data["content"]

                    booking = db.query(Booking).filter(Booking.id == booking_id).first()
                    if not booking:
                        await websocket.send_json({"error": "Reserva no encontrada"})
                        continue

                    service = db.query(Service).filter(Service.id == booking.service_id).first()
                    if not service:
                        await websocket.send_json({"error": "Servicio no encontrado"})
                        continue
                    if user.id != booking.tourist_id and user.id != service.provider_id:
                        await websocket.send_json({"error": "No autorizado"})
                        continue
                    allowed_receivers = {booking.tourist_id, service.provider_id} - {user.id}
                    if receiver_id not in allowed_receivers:
                        await websocket.send_json({"error": "El receptor no pertenece a esta reserva"})
                        continue

                    new_message = Message(
                        sender_id=user.id,
                        receiver_id=receiver_id,
                        booking_id=booking_id,
                        content=content
                    )
                    db.add(new_message)
                    db.commit()
                    db.refresh(new_message)

                    message_data = {
                        "id": new_message.id,
                        "sender_id": new_message.sender_id,
                        "receiver_id": new_message.receiver_id,
                        "booking_id": new_message.booking_id,
                        "content": new_message.content,
                        "timestamp": str(new_message.timestamp)
                    }

                    await manager.send_to_user(receiver_id, {
                        "action": "new_message",
                        "message": message_data
                    })

                    await websocket.send_json({
                        "action": "message_sent",
                        "message": message_data
                    })

                elif action == "mark_read":
                    booking_id = data["booking_id"]
                    booking = db.query(Booking).filter(Booking.id == booking_id).first()
                    if not booking:
                        await websocket.send_json({"error": "Reserva no encontrada"})
                        continue
                    service = db.query(Service).filter(Service.id == booking.service_id).first()
                    if not service or user.id not in {booking.tourist_id, service.provider_id}:
                        await websocket.send_json({"error": "No autorizado"})
                        continue
                    db.query(Message).filter(
                        Message.booking_id == booking_id,
                        Message.receiver_id == user.id,
                        Message.read == False
                    ).update({"read": True})
                    db.commit()

                    await websocket.send_json({"action": "read_confirmed", "booking_id": booking_id})

        except WebSocketDisconnect:
            manager.disconnect(websocket, user.id)
    finally:
        db.close()
