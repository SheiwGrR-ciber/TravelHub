from fastapi import APIRouter, WebSocket, WebSocketDisconnect, Depends
from sqlalchemy.orm import Session
from app.db.database import get_db
from app.models.message import Message
from app.models.booking import Booking
from app.models.service import Service
from app.models.user import User
import json

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

@router.websocket("/ws/chat/{user_id}")
async def websocket_chat(websocket: WebSocket, user_id: int):
    await manager.connect(websocket, user_id)
    try:
        while True:
            data = await websocket.receive_json()
            action = data.get("action")

            if action == "send_message":
                booking_id = data["booking_id"]
                receiver_id = data["receiver_id"]
                content = data["content"]

                db = next(get_db())
                booking = db.query(Booking).filter(Booking.id == booking_id).first()
                if not booking:
                    await websocket.send_json({"error": "Reserva no encontrada"})
                    continue

                service = db.query(Service).filter(Service.id == booking.service_id).first()
                if user_id != booking.tourist_id and user_id != service.provider_id:
                    await websocket.send_json({"error": "No autorizado"})
                    continue

                new_message = Message(
                    sender_id=user_id,
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
                db = next(get_db())
                db.query(Message).filter(
                    Message.booking_id == booking_id,
                    Message.receiver_id == user_id,
                    Message.read == False
                ).update({"read": True})
                db.commit()

                await websocket.send_json({"action": "read_confirmed", "booking_id": booking_id})

    except WebSocketDisconnect:
        manager.disconnect(websocket, user_id)
