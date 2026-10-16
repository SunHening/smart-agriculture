# service/session_manager.py

from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Optional
from uuid import uuid4

import numpy as np


@dataclass
class MaskCandidate:
    """
    AMG 生成的一个候选分割区域。

    一个 MaskCandidate 对应：
    一个候选 Mask
    +
    该 Mask 的基础质量信息
    """

    # 候选 Mask 在当前 Session 中的唯一编号
    candidate_id: int

    # 二值 Mask
    # shape: H x W
    # dtype: bool
    segmentation: np.ndarray

    # Mask 的像素面积
    area_pixels: int

    # Mask 的外接矩形
    # 格式： [x, y, width, height]
    # 与 SAM AMG 返回的 bbox 保持一致
    bbox: list[float]

    # SAM 对该 Mask 质量的预测
    #
    # 注意：
    # predicted_iou 不是“农田概率”
    # 而是 SAM 对该 Mask 质量的预测
    predicted_iou: float

    # Mask 稳定性指标
    stability_score: float


@dataclass
class ImageSession:
    """
    一个 SAM 图像会话。

    一个 ImageSession 对应：

    一张固定图片
    +
    这张图片的一份 SAM image embedding
    +
    这张图片的一组 AMG 候选 Mask

    SessionManager 只负责保存和管理这些状态，
    不负责执行 SAM 推理。
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

    # AMG 生成的候选 Mask
    candidates: list[MaskCandidate]

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
    - AMG
    """

    def __init__(self):
        self._sessions: dict[str, ImageSession] = {}

    def create(
        self,
        image: np.ndarray,
        embedding: object,
        original_size: tuple[int, int],
        input_size: tuple[int, int],
        candidates: Optional[list[MaskCandidate]] = None,
    ) -> ImageSession:
        """
        创建一个新的 ImageSession。

        参数：

        image:
            原始图片。

        embedding:
            SAM Image Encoder 生成的 image embedding。

        original_size:
            原始图片尺寸。

        input_size:
            SAM Image Encoder 实际使用的输入尺寸。

        candidates:
            AMG 生成的候选 Mask 列表。

            如果暂时没有候选 Mask，
            默认使用空列表。

            这样可以兼容：
            1. 只有 embedding 的旧流程
            2. embedding + AMG 的新流程
        """

        session_id = str(uuid4())

        height, width = image.shape[:2]

        now = datetime.now(timezone.utc)

        if candidates is None:
            candidates = []

        session = ImageSession(
            session_id=session_id,
            image=image,
            width=width,
            height=height,
            embedding=embedding,
            original_size=original_size,
            input_size=input_size,
            candidates=candidates,
            created_at=now,
            last_accessed_at=now,
        )

        self._sessions[session_id] = session

        return session

    def get(
        self,
        session_id: str,
    ) -> Optional[ImageSession]:
        """
        根据 session_id 获取 Session。

        获取成功后更新 last_accessed_at。
        """

        session = self._sessions.get(session_id)

        if session is None:
            return None

        session.last_accessed_at = datetime.now(timezone.utc)

        return session

    def delete(
        self,
        session_id: str,
    ) -> bool:
        """
        删除一个 Session。

        返回：

        True:
            删除成功

        False:
            Session 不存在
        """

        if session_id not in self._sessions:
            return False

        del self._sessions[session_id]

        return True

    def exists(
        self,
        session_id: str,
    ) -> bool:
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
