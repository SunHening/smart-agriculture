# service/session_manager.py

from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Optional
from uuid import uuid4

import numpy as np


@dataclass
class ImageSession:
    """A source image and its (possibly enhanced) SAM embedding."""

    session_id: str
    # Image passed to SAM (may be enhanced)
    image: np.ndarray
    # Dimensions of the source image coordinate system used by the frontend
    width: int
    height: int
    # Dimensions of the actual image encoded by SAM
    sam_width: int
    sam_height: int
    sr_enabled: bool
    sr_method: str
    sr_scale_x: float
    sr_scale_y: float
    embedding: object
    original_size: tuple[int, int]
    input_size: tuple[int, int]
    created_at: datetime
    last_accessed_at: datetime


class SessionManager:
    """Manage active image sessions; no model, HTTP, or database responsibilities."""

    def __init__(self):
        self._sessions: dict[str, ImageSession] = {}

    def create(
        self,
        image: np.ndarray,
        embedding: object,
        original_size: tuple[int, int],
        input_size: tuple[int, int],
        source_width: int,
        source_height: int,
        sr_enabled: bool = False,
        sr_method: str = "none",
    ) -> ImageSession:
        session_id = str(uuid4())
        sam_height, sam_width = image.shape[:2]
        now = datetime.now(timezone.utc)
        session = ImageSession(
            session_id=session_id,
            image=image,
            width=source_width,
            height=source_height,
            sam_width=sam_width,
            sam_height=sam_height,
            sr_enabled=sr_enabled,
            sr_method=sr_method,
            sr_scale_x=sam_width / source_width,
            sr_scale_y=sam_height / source_height,
            embedding=embedding,
            original_size=original_size,
            input_size=input_size,
            created_at=now,
            last_accessed_at=now,
        )
        self._sessions[session_id] = session
        return session

    def get(self, session_id: str) -> Optional[ImageSession]:
        session = self._sessions.get(session_id)
        if session is not None:
            session.last_accessed_at = datetime.now(timezone.utc)
        return session

    def delete(self, session_id: str) -> bool:
        if session_id not in self._sessions:
            return False
        del self._sessions[session_id]
        return True

    def exists(self, session_id: str) -> bool:
        return session_id in self._sessions

    def count(self) -> int:
        return len(self._sessions)

    def clear(self) -> None:
        self._sessions.clear()
