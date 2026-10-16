from typing import Any

import numpy as np

from service.sam_runtime import SamRuntime
from service.session_manager import SessionManager
from service.sr_service import SRService


class SegmentationService:
    """Connect image enhancement, SAM runtime, and session management."""

    def __init__(
        self,
        runtime: SamRuntime,
        session_manager: SessionManager,
        sr_service: SRService | None = None,
    ):
        self.runtime = runtime
        self.session_manager = session_manager
        self.sr_service = sr_service or SRService(enabled=False)

    def create_session(self, image: np.ndarray) -> dict[str, Any]:
        if image is None:
            raise ValueError("image 不能为空")
        if not isinstance(image, np.ndarray):
            raise TypeError("image 必须是 numpy.ndarray")
        if image.ndim != 3 or image.shape[2] != 3:
            raise ValueError("image 必须是 RGB 三通道图像")
        if image.dtype != np.uint8:
            raise ValueError("image 必须是 uint8")

        source_height, source_width = image.shape[:2]

        # Enhancement is done once at session creation, not on every prompt.
        enhanced = self.sr_service.enhance(image)
        sam_image = enhanced.image
        result = self.runtime.encode_image(sam_image)

        session = self.session_manager.create(
            image=sam_image,
            embedding=result["embedding"],
            original_size=result["original_size"],
            input_size=result["input_size"],
            source_width=source_width,
            source_height=source_height,
            sr_enabled=enhanced.enabled,
            sr_method=enhanced.method,
        )

        return {
            "session_id": session.session_id,
            # Keep frontend dimensions in the original image coordinate system.
            "width": session.width,
            "height": session.height,
            "original_size": list(session.original_size),
            "sam_width": session.sam_width,
            "sam_height": session.sam_height,
            "sr_enabled": session.sr_enabled,
            "sr_method": session.sr_method,
            "sr_scale_x": session.sr_scale_x,
            "sr_scale_y": session.sr_scale_y,
        }

    def predict_points(
        self,
        session_id: str,
        point_coords: np.ndarray,
        point_labels: np.ndarray,
        multimask_output: bool = False,
    ) -> dict[str, Any]:
        session = self.session_manager.get(session_id)
        if session is None:
            raise ValueError(f"Session 不存在: {session_id}")
        if point_coords is None or point_labels is None:
            raise ValueError("point_coords 和 point_labels 不能为空")
        if len(point_coords) != len(point_labels):
            raise ValueError("point_coords 和 point_labels 数量必须一致")
        if len(point_coords) == 0:
            raise ValueError("至少需要一个点提示")

        # Incoming prompts use source-image pixels. Convert to enhanced-image pixels.
        coords_for_sam = np.asarray(point_coords, dtype=np.float32).copy()
        coords_for_sam[:, 0] *= session.sr_scale_x
        coords_for_sam[:, 1] *= session.sr_scale_y

        return self.runtime.predict_points(
            embedding=session.embedding,
            original_size=session.original_size,
            input_size=session.input_size,
            point_coords=coords_for_sam,
            point_labels=np.asarray(point_labels, dtype=np.int32),
            multimask_output=multimask_output,
        )

    def delete_session(self, session_id: str) -> bool:
        return self.session_manager.delete(session_id)
