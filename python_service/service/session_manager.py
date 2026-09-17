# service/session_manager.py

from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Optional
from uuid import uuid4

import numpy as np


@dataclass
class ImageSession:
    """
    一个 SAM 图像会话。

    一个 ImageSession 对应：
    一张固定图片
    +
    这张图片的一份 SAM image embedding
    """

    session_id: str

    # 原始图片
    image: np.ndarray

    # 图片尺寸
    width: int
    height: int

    # SAM image embedding
    embedding: object

    # SAM 推理过程中使用的尺寸信息
    original_size: tuple[int, int]
    input_size: tuple[int, int]

    # Session 生命周期时间
    created_at: datetime
    last_accessed_at: datetime


class SessionManager:
    """
    管理所有活动中的 ImageSession。

    注意：
    SessionManager 不负责：
    - SAM 模型
    - GPU 推理
    - HTTP
    - MySQL
    - JWT
    """

    def __init__(self):
        self._sessions: dict[str, ImageSession] = {}

    def create(
        self,
        image: np.ndarray,
        embedding: object,
        original_size: tuple[int, int],
        input_size: tuple[int, int],
    ) -> ImageSession:
        """
        创建一个新的 ImageSession。
        """

        session_id = str(uuid4())

        height, width = image.shape[:2]

        now = datetime.now(timezone.utc)

        session = ImageSession(
            session_id=session_id,
            image=image,
            width=width,
            height=height,
            embedding=embedding,
            original_size=original_size,
            input_size=input_size,
            created_at=now,
            last_accessed_at=now,
        )

        self._sessions[session_id] = session

        return session

    def get(self, session_id: str) -> Optional[ImageSession]:
        """
        根据 session_id 获取 Session。

        获取成功后更新 last_accessed_at。
        """

        session = self._sessions.get(session_id)

        if session is None:
            return None

        session.last_accessed_at = datetime.now(timezone.utc)

        return session

    def delete(self, session_id: str) -> bool:
        """
        删除一个 Session。

        返回：
        True  = 删除成功
        False = Session 不存在
        """

        if session_id not in self._sessions:
            return False

        del self._sessions[session_id]

        return True

    def exists(self, session_id: str) -> bool:
        """
        判断 Session 是否存在。
        """

        return session_id in self._sessions

    def count(self) -> int:
        """
        当前活动 Session 数量。
        """

        return len(self._sessions)

    def clear(self) -> None:
        """
        清空所有 Session。

        主要用于测试或服务关闭。
        """

        self._sessions.clear()