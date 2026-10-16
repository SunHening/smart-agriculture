# service/segmentation_service.py

from typing import Any

import numpy as np

from service.sam_runtime import SamRuntime
from service.session_manager import (
    MaskCandidate,
    SessionManager,
)


class SegmentationService:
    """
    SAM 分割业务服务。

    负责连接：
        SamRuntime
        SessionManager

    主要负责：
        1. 创建 ImageSession
        2. 生成 AMG 候选 Mask
        3. 管理 Point / Box 分割请求
        4. 将 SAM Runtime 的结果转换为业务层数据

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

    def create_session(
        self,
        image: np.ndarray,
    ) -> dict[str, Any]:
        """
        创建一个 SAM ImageSession。

        新架构流程：

            image
              │
              ├───────────────┐
              │               │
              ▼               ▼
        Image Encoder       AMG
              │               │
              ▼               ▼
          embedding      raw candidates
              │               │
              └───────┬───────┘
                      ▼
                MaskCandidate
                      │
                      ▼
                ImageSession
        """

        # -------------------------
        # 1. 参数检查
        # -------------------------

        if image is None:
            raise ValueError("image 不能为空")

        if not isinstance(image, np.ndarray):
            raise TypeError(
                "image 必须是 numpy.ndarray"
            )

        if image.ndim != 3:
            raise ValueError(
                "image 必须是三维图像数组"
            )

        if image.shape[2] != 3:
            raise ValueError(
                "image 必须是 RGB/BGR 三通道图像"
            )

        # -------------------------
        # 2. Image Encoder
        # -------------------------

        encode_result = self.runtime.encode_image(
            image
        )

        embedding = encode_result["embedding"]
        original_size = encode_result["original_size"]
        input_size = encode_result["input_size"]

        # -------------------------
        # 3. AMG
        # -------------------------

        raw_candidates = self.runtime.generate_masks(
            image
        )

        # -------------------------
        # 4. 将 SAM AMG 原始数据
        #    转换成我们自己的业务对象
        # -------------------------

        candidates: list[MaskCandidate] = []

        for candidate_id, raw_candidate in enumerate(
            raw_candidates
        ):
            segmentation = raw_candidate["segmentation"]

            segmentation = np.asarray(
                segmentation,
                dtype=bool,
            )

            area_pixels = int(
                raw_candidate["area"]
            )

            bbox = [
                float(value)
                for value in raw_candidate["bbox"]
            ]

            predicted_iou = float(
                raw_candidate["predicted_iou"]
            )

            stability_score = float(
                raw_candidate["stability_score"]
            )

            candidate = MaskCandidate(
                candidate_id=candidate_id,
                segmentation=segmentation,
                area_pixels=area_pixels,
                bbox=bbox,
                predicted_iou=predicted_iou,
                stability_score=stability_score,
            )

            candidates.append(candidate)

        # -------------------------
        # 5. 创建 ImageSession
        # -------------------------

        session = self.session_manager.create(
            image=image,
            embedding=embedding,
            original_size=original_size,
            input_size=input_size,
            candidates=candidates,
        )

        # -------------------------
        # 6. 返回 Session 基础信息
        # -------------------------

        return {
            "session_id": session.session_id,
            "width": session.width,
            "height": session.height,
            "original_size": session.original_size,
            "candidate_count": len(
                session.candidates
            ),
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

        当前阶段仍然保留原来的
        SAM Prompt Decoder 流程。

        后续可以改成：

            Point
              ↓
            Candidate Selector
              ↓
            Candidate Mask
              ↓
            optional SAM refinement
        """

        # -------------------------
        # 1. 获取 Session
        # -------------------------

        session = self.session_manager.get(
            session_id
        )

        if session is None:
            raise ValueError(
                f"Session 不存在: {session_id}"
            )

        # -------------------------
        # 2. 参数检查
        # -------------------------

        if point_coords is None:
            raise ValueError(
                "point_coords 不能为空"
            )

        if point_labels is None:
            raise ValueError(
                "point_labels 不能为空"
            )

        if len(point_coords) != len(
            point_labels
        ):
            raise ValueError(
                "point_coords 和 point_labels 数量必须一致"
            )

        if len(point_coords) == 0:
            raise ValueError(
                "至少需要一个点提示"
            )

        # -------------------------
        # 3. 调用 SAM Runtime
        # -------------------------

        result = self.runtime.predict_points(
            embedding=session.embedding,
            original_size=session.original_size,
            input_size=session.input_size,
            point_coords=point_coords,
            point_labels=point_labels,
            multimask_output=multimask_output,
        )

        return result

    def predict_box(
        self,
        session_id: str,
        box: np.ndarray,
        multimask_output: bool = False,
    ) -> dict[str, Any]:
        """
        根据已有 Session 和 Box 提示进行分割。

        box 格式：

            [x1, y1, x2, y2]

        其中：

            x1, y1 = 左上角像素坐标
            x2, y2 = 右下角像素坐标

        当前阶段仍然保留原来的
        SAM Prompt Decoder 流程。
        """

        # -------------------------
        # 1. 获取 Session
        # -------------------------

        session = self.session_manager.get(
            session_id
        )

        if session is None:
            raise ValueError(
                f"Session 不存在: {session_id}"
            )

        # -------------------------
        # 2. 参数检查
        # -------------------------

        if box is None:
            raise ValueError(
                "box 不能为空"
            )

        box = np.asarray(
            box,
            dtype=np.float32,
        )

        if box.shape != (4,):
            raise ValueError(
                "box 必须是 [x1, y1, x2, y2]"
            )

        x1, y1, x2, y2 = box

        if x1 >= x2 or y1 >= y2:
            raise ValueError(
                "Box 必须满足 x1 < x2 且 y1 < y2"
            )

        # -------------------------
        # 3. 调用 SAM Runtime
        # -------------------------

        result = self.runtime.predict_box(
            embedding=session.embedding,
            original_size=session.original_size,
            input_size=session.input_size,
            box=box,
            multimask_output=multimask_output,
        )

        return result

    def delete_session(
        self,
        session_id: str,
    ) -> bool:
        """
        删除一个 ImageSession。
        """

        return self.session_manager.delete(
            session_id
        )
