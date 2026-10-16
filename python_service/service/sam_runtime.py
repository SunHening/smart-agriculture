# service/sam_runtime.py

import logging
from typing import Any

import numpy as np
import torch

from segment_anything import (
    SamAutomaticMaskGenerator,
    sam_model_registry,
)


logger = logging.getLogger(__name__)


class SamRuntime:
    """
    SAM 模型运行时。

    职责：

    1. 加载共享的 SAM 模型
    2. 对图片执行 Image Encoder
    3. 使用 AMG 自动生成候选 Mask
    4. 根据指定 embedding + prompt 执行 Mask Decoder

    不负责：

    - HTTP
    - 用户
    - Session
    - TTL
    - LRU
    - 数据库
    - 请求队列
    """

    def __init__(
        self,
        model_type: str,
        checkpoint_path: str,
        device: str = "cuda",
    ):
        self.model_type = model_type
        self.checkpoint_path = checkpoint_path
        self.device = torch.device(device)

        # 共享的 SAM 模型
        self.model = None

        # 共享的 AMG
        #
        # SamAutomaticMaskGenerator 内部持有
        # SamPredictor，但使用的是同一个 self.model。
        #
        # 因此：
        #
        # Session 不是一个 AMG
        # ImageSession 也不拥有 SAM 模型
        #
        # 整个进程共享一个模型和一个 AMG。
        self.mask_generator = None

    def load(self) -> None:
        """
        加载一次共享 SAM 模型和 AMG。
        """

        if self.model is not None:
            return

        logger.info(
            "Loading SAM model: type=%s, checkpoint=%s, device=%s",
            self.model_type,
            self.checkpoint_path,
            self.device,
        )

        # -------------------------
        # 1. 加载 SAM 模型
        # -------------------------

        sam = sam_model_registry[self.model_type](
            checkpoint=self.checkpoint_path
        )

        sam.to(device=self.device)
        sam.eval()

        self.model = sam

        # -------------------------
        # 2. 创建共享 AMG
        # -------------------------

        self.mask_generator = SamAutomaticMaskGenerator(
            model=self.model,

            # 每边采样多少个点。
            #
            # points_per_side=32
            # → 32 x 32 = 1024 个采样点
            #
            # 第一版先使用官方默认值。
            points_per_side=32,

            # 每批送入模型的 prompt 数量。
            #
            # 这里影响显存占用和推理吞吐。
            points_per_batch=64,

            # AMG 官方默认质量过滤参数。
            pred_iou_thresh=0.88,
            stability_score_thresh=0.95,
            stability_score_offset=1.0,

            # Mask 之间的 NMS 阈值。
            box_nms_thresh=0.7,

            # 第一版暂时不使用多尺度 crop。
            crop_n_layers=0,

            # 下面这些参数在 crop_n_layers=0 时
            # 不产生主要作用，因此先保持默认值。
            crop_nms_thresh=0.7,
            crop_overlap_ratio=0.3413333333333333,
            crop_n_points_downscale_factor=1,

            # 第一版不主动删除小区域。
            min_mask_region_area=0,

            # 我们需要直接得到 H x W 的 bool Mask。
            output_mode="binary_mask",
        )

        logger.info(
            "SAM model and AMG loaded."
        )

    @torch.inference_mode()
    def encode_image(
        self,
        image: np.ndarray,
    ) -> dict[str, Any]:
        """
        Image -> embedding

        image:
            H x W x 3
            uint8
            RGB

        返回：
            embedding
            original_size
            input_size
        """

        self.load()

        if not isinstance(image, np.ndarray):
            raise TypeError(
                "image 必须是 numpy.ndarray"
            )

        if image.ndim != 3 or image.shape[2] != 3:
            raise ValueError(
                f"image 必须是 HxWx3，实际为 {image.shape}"
            )

        if image.dtype != np.uint8:
            raise ValueError(
                f"image 必须是 uint8，实际为 {image.dtype}"
            )

        image = np.ascontiguousarray(image)

        original_size = image.shape[:2]

        # 与官方 SamPredictor.set_image() 保持一致：
        # 先进行 ResizeLongestSide

        from segment_anything.utils.transforms import (
            ResizeLongestSide,
        )

        transform = ResizeLongestSide(
            self.model.image_encoder.img_size
        )

        input_image = transform.apply_image(
            image
        )

        input_image_torch = torch.as_tensor(
            input_image,
            device=self.device,
        )

        input_image_torch = (
            input_image_torch
            .permute(2, 0, 1)
            .contiguous()
            .unsqueeze(0)
        )

        input_size = tuple(
            input_image_torch.shape[-2:]
        )

        # SAM 官方 preprocess
        input_image_torch = (
            self.model.preprocess(
                input_image_torch
            )
        )

        # Image Encoder
        embedding = self.model.image_encoder(
            input_image_torch
        )

        return {
            "embedding": embedding,
            "original_size": original_size,
            "input_size": input_size,
        }

    @torch.inference_mode()
    def generate_masks(
        self,
        image: np.ndarray,
    ) -> list[dict[str, Any]]:
        """
        使用 SAM Automatic Mask Generator
        自动生成一张图片的候选 Mask。

        image:
            H x W x 3
            uint8
            RGB

        返回：

            [
                {
                    "segmentation": np.ndarray,
                    "area": int,
                    "bbox": [x, y, width, height],
                    "predicted_iou": float,
                    "point_coords": [[x, y]],
                    "stability_score": float,
                    "crop_box": [x, y, width, height],
                },
                ...
            ]

        注意：

        predicted_iou 和 stability_score
        是 Mask 质量相关指标。

        它们不是“农田概率”。
        """

        self.load()

        if not isinstance(image, np.ndarray):
            raise TypeError(
                "image 必须是 numpy.ndarray"
            )

        if image.ndim != 3 or image.shape[2] != 3:
            raise ValueError(
                f"image 必须是 HxWx3，实际为 {image.shape}"
            )

        if image.dtype != np.uint8:
            raise ValueError(
                f"image 必须是 uint8，实际为 {image.dtype}"
            )

        image = np.ascontiguousarray(image)

        logger.info(
            "Generating AMG masks: image_shape=%s",
            image.shape,
        )

        candidates = self.mask_generator.generate(
            image
        )

        logger.info(
            "AMG masks generated: count=%d",
            len(candidates),
        )

        return candidates

    @torch.inference_mode()
    def predict_box(
        self,
        embedding: torch.Tensor,
        original_size: tuple[int, int],
        input_size: tuple[int, int],
        box: np.ndarray,
        multimask_output: bool = False,
    ) -> dict[str, Any]:
        """
        embedding + box prompt -> mask

        box:
            [x1, y1, x2, y2]

        坐标使用原始图像像素坐标。
        """

        self.load()

        box = np.asarray(
            box,
            dtype=np.float32,
        )

        if box.shape != (4,):
            raise ValueError(
                "box 必须为 [x1, y1, x2, y2]"
            )

        x1, y1, x2, y2 = box

        if x1 >= x2 or y1 >= y2:
            raise ValueError(
                "box 必须满足 x1 < x2 且 y1 < y2"
            )

        # -------------------------
        # Box coordinates
        # -------------------------

        from segment_anything.utils.transforms import (
            ResizeLongestSide,
        )

        transform = ResizeLongestSide(
            self.model.image_encoder.img_size
        )

        box_coords = np.array(
            [[x1, y1, x2, y2]],
            dtype=np.float32,
        )

        transformed_box = transform.apply_boxes(
            box_coords,
            original_size,
        )

        box_torch = torch.as_tensor(
            transformed_box,
            dtype=torch.float,
            device=self.device,
        )

        # -------------------------
        # Prompt Encoder
        # -------------------------

        sparse_embeddings, dense_embeddings = (
            self.model.prompt_encoder(
                points=None,
                boxes=box_torch,
                masks=None,
            )
        )

        # -------------------------
        # Mask Decoder
        # -------------------------

        low_res_masks, iou_predictions = (
            self.model.mask_decoder(
                image_embeddings=embedding,
                image_pe=self.model.prompt_encoder.get_dense_pe(),
                sparse_prompt_embeddings=sparse_embeddings,
                dense_prompt_embeddings=dense_embeddings,
                multimask_output=multimask_output,
            )
        )

        # -------------------------
        # Restore original image size
        # -------------------------

        masks = self.model.postprocess_masks(
            low_res_masks,
            input_size,
            original_size,
        )

        masks = (
            masks
            > self.model.mask_threshold
        )

        masks_np = (
            masks[0]
            .detach()
            .cpu()
            .numpy()
        )

        scores_np = (
            iou_predictions[0]
            .detach()
            .cpu()
            .numpy()
        )

        low_res_masks_np = (
            low_res_masks[0]
            .detach()
            .cpu()
            .numpy()
        )

        best_index = int(
            np.argmax(scores_np)
        )

        return {
            "mask": masks_np[best_index],
            "score": float(
                scores_np[best_index]
            ),
            "all_scores": scores_np,
            "low_res_mask": (
                low_res_masks_np[best_index]
            ),
            "original_size": original_size,
        }

    @torch.inference_mode()
    def predict_points(
        self,
        embedding: torch.Tensor,
        original_size: tuple[int, int],
        input_size: tuple[int, int],
        point_coords: np.ndarray,
        point_labels: np.ndarray,
        multimask_output: bool = False,
    ) -> dict[str, Any]:
        """
        embedding + point prompt -> mask
        """

        self.load()

        point_coords = np.asarray(
            point_coords,
            dtype=np.float32,
        )

        point_labels = np.asarray(
            point_labels,
            dtype=np.int32,
        )

        if point_coords.ndim != 2:
            raise ValueError(
                "point_coords 必须为 [N, 2]"
            )

        if point_coords.shape[1] != 2:
            raise ValueError(
                "point_coords 必须为 [N, 2]"
            )

        if point_labels.ndim != 1:
            raise ValueError(
                "point_labels 必须为 [N]"
            )

        if len(point_coords) != len(
            point_labels
        ):
            raise ValueError(
                "point_coords 和 point_labels 数量不一致"
            )

        if len(point_coords) == 0:
            raise ValueError(
                "至少需要一个 point prompt"
            )

        # -------------------------
        # Point coordinates
        # -------------------------

        from segment_anything.utils.transforms import (
            ResizeLongestSide,
        )

        transform = ResizeLongestSide(
            self.model.image_encoder.img_size
        )

        transformed_coords = (
            transform.apply_coords(
                point_coords,
                original_size,
            )
        )

        coords_torch = torch.as_tensor(
            transformed_coords,
            dtype=torch.float,
            device=self.device,
        )

        labels_torch = torch.as_tensor(
            point_labels,
            dtype=torch.int,
            device=self.device,
        )

        coords_torch = (
            coords_torch.unsqueeze(0)
        )

        labels_torch = (
            labels_torch.unsqueeze(0)
        )

        points = (
            coords_torch,
            labels_torch,
        )

        # -------------------------
        # Prompt Encoder
        # -------------------------

        sparse_embeddings, dense_embeddings = (
            self.model.prompt_encoder(
                points=points,
                boxes=None,
                masks=None,
            )
        )

        # -------------------------
        # Mask Decoder
        # -------------------------

        low_res_masks, iou_predictions = (
            self.model.mask_decoder(
                image_embeddings=embedding,
                image_pe=self.model.prompt_encoder.get_dense_pe(),
                sparse_prompt_embeddings=sparse_embeddings,
                dense_prompt_embeddings=dense_embeddings,
                multimask_output=multimask_output,
            )
        )

        # -------------------------
        # Restore original image size
        # -------------------------

        masks = self.model.postprocess_masks(
            low_res_masks,
            input_size,
            original_size,
        )

        masks = (
            masks
            > self.model.mask_threshold
        )

        masks_np = (
            masks[0]
            .detach()
            .cpu()
            .numpy()
        )

        scores_np = (
            iou_predictions[0]
            .detach()
            .cpu()
            .numpy()
        )

        low_res_masks_np = (
            low_res_masks[0]
            .detach()
            .cpu()
            .numpy()
        )

        best_index = int(
            np.argmax(scores_np)
        )

        return {
            "mask": masks_np[best_index],
            "score": float(
                scores_np[best_index]
            ),
            "all_scores": scores_np,
            "low_res_mask": (
                low_res_masks_np[best_index]
            ),
            "original_size": original_size,
        }
