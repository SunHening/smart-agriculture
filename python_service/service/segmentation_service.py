# service/segmentation_service.py

from typing import Any

import numpy as np

from service.sam_runtime import SamRuntime
from service.session_manager import SessionManager


class SegmentationService:
    """
    SAM 分割业务服务。

    负责连接：
        SamRuntime
        SessionManager

    不负责：
        HTTP
        Flask
        JWT
        MySQL
        Session TTL/LRU
        GPU worker/queue
    """

    def __init__(
        self,
        runtime: SamRuntime,
        session_manager: SessionManager,
    ):
        self.runtime = runtime
        self.session_manager = session_manager

    def create_session(self, image: np.ndarray) -> dict[str, Any]:
        """
        创建一个 SAM ImageSession。

        流程：

            image
              ↓
            image encoder
              ↓
            embedding
              ↓
            SessionManager
              ↓
            session
        """

        if image is None:
            raise ValueError("image 不能为空")

        if not isinstance(image, np.ndarray):
            raise TypeError("image 必须是 numpy.ndarray")

        if image.ndim != 3:
            raise ValueError("image 必须是三维图像数组")

        if image.shape[2] != 3:
            raise ValueError("image 必须是 RGB/BGR 三通道图像")

        # 1. 使用 SAM Image Encoder 生成 embedding
        result = self.runtime.encode_image(image)

        embedding = result["embedding"]
        original_size = result["original_size"]
        input_size = result["input_size"]

        # 2. 创建 ImageSession
        session = self.session_manager.create(
            image=image,
            embedding=embedding,
            original_size=original_size,
            input_size=input_size,
        )

        return {
            "session_id": session.session_id,
            "width": session.width,
            "height": session.height,
            "original_size": session.original_size,
        }

    def predict_points(
        self,
        session_id: str,
        point_coords: np.ndarray,
        point_labels: np.ndarray,
        multimask_output: bool = False,
    ) -> dict[str, Any]:
        """
        根据已有 Session 和点提示进行分割。

        流程：

            session_id
                ↓
            SessionManager.get()
                ↓
            embedding
                ↓
            SamRuntime.predict_points()
                ↓
            raw mask
        """

        # 1. 获取 Session
        session = self.session_manager.get(session_id)

        if session is None:
            raise ValueError(f"Session 不存在: {session_id}")

        # 2. 参数检查
        if point_coords is None:
            raise ValueError("point_coords 不能为空")

        if point_labels is None:
            raise ValueError("point_labels 不能为空")

        if len(point_coords) != len(point_labels):
            raise ValueError(
                "point_coords 和 point_labels 数量必须一致"
            )

        if len(point_coords) == 0:
            raise ValueError("至少需要一个点提示")

        # 3. 调用 SAM Runtime
        result = self.runtime.predict_points(
            embedding=session.embedding,
            original_size=session.original_size,
            input_size=session.input_size,
            point_coords=point_coords,
            point_labels=point_labels,
            multimask_output=multimask_output,
        )

        return result

    def delete_session(self, session_id: str) -> bool:
        """
        删除一个 ImageSession。
        """

        return self.session_manager.delete(session_id)