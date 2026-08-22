"""
SAM-Geo 推理引擎
使用 Segment Anything Model (SAM) 从遥感/航拍影像中分割农田区域

依赖安装:
    pip install segment-geospatial opencv-python pillow numpy
首次运行将自动下载 SAM 预训练权重（vit_b 约 375MB）。
"""
import logging
import os
import tempfile
import glob
import threading

import numpy as np
import cv2
from PIL import Image

logger = logging.getLogger(__name__)

# SAM 权重缓存目录
SAM_CHECKPOINT_DIR = os.path.join(os.path.expanduser("~"), ".cache", "sam_geo")

# 各模型默认权重文件名
CHECKPOINT_NAMES = {
    "vit_b": "sam_vit_b_01ec64.pth",
    "vit_l": "sam_vit_l_0b3195.pth",
    "vit_h": "sam_vit_h_4b8939.pth",
}


class SamGeoInference:
    """SAM-Geo 推理封装（纯 SAM，不再回退 ExG）"""

    def __init__(self, model_type="vit_b", checkpoint_path=None, device="cpu"):
        """
        model_type: vit_h / vit_l / vit_b
        checkpoint_path: SAM 权重文件路径，None 则自动下载/查找缓存
        device: "cpu" / "cuda"
        """
        self.model_type = model_type
        self.checkpoint_path = checkpoint_path
        self.device = device
        self.model = None
        self._load_attempted = False
        # SAM 模型（SamAutomaticMaskGenerator/SamPredictor）非线程安全，
        # 并发请求会互相覆盖 predictor 状态导致 "An image must be set" 错误，故加锁串行化。
        self._lock = threading.Lock()

    def _resolve_checkpoint(self):
        """查找本地已下载的权重，找不到返回 None（交由 samgeo 自动下载）"""
        if self.checkpoint_path and os.path.exists(self.checkpoint_path):
            return self.checkpoint_path

        name = CHECKPOINT_NAMES.get(self.model_type, f"sam_{self.model_type}.pth")
        patterns = [
            os.path.join(SAM_CHECKPOINT_DIR, name),
            os.path.join(SAM_CHECKPOINT_DIR, "sam_*.pth"),
            # segment-geospatial 默认下载到 torch hub 缓存目录
            os.path.join(os.path.expanduser("~"), ".cache", "torch", "hub", "checkpoints", name),
            os.path.join(os.getcwd(), "sam_*.pth"),
        ]
        for pat in patterns:
            matches = glob.glob(pat)
            if matches:
                return matches[0]
        return None

    def _load_model(self):
        """延迟加载 SAM 模型（权重缺失时自动下载）"""
        if self._load_attempted:
            return
        self._load_attempted = True

        from samgeo import SamGeo

        kwargs = {}
        ckpt = self._resolve_checkpoint()
        if ckpt:
            kwargs["checkpoint"] = ckpt
            logger.info(f"加载 SAM 模型: {self.model_type}, checkpoint={ckpt}")
        else:
            logger.info(f"加载 SAM 模型: {self.model_type}（首次运行将自动下载权重，约 375MB）")

        # device 是 SamGeo 的独立参数，不能放进 sam_kwargs：
        # sam_kwargs 会被转发给 SamAutomaticMaskGenerator，后者不接受 device 参数。
        # points_per_side 默认 32（1024 个采样点），CPU 推理慢；农田为大块连续区域，
        # 降到 16（256 点）约提速 4 倍，对分割效果影响很小。
        self.model = SamGeo(
            model_type=self.model_type,
            device=self.device,
            sam_kwargs={"points_per_side": 16},
            **kwargs
        )
        logger.info("SAM 模型加载成功")

    def segment(self, image, prompts=None):
        """
        分割图像中的农田区域

        Args:
            image: PIL.Image
            prompts: 提示点列表（可选）

        Returns:
            boundaries: [{"geojson": {...}, "pixel_area": float, "confidence": float}, ...]
            overlay: PIL.Image — 原图 + 掩码叠加
        """
        with self._lock:
            return self._segment(image, prompts)

    def _segment(self, image, prompts=None):
        self._load_model()
        img_np = np.array(image)

        tmp_path = None
        mask_path = None
        try:
            with tempfile.NamedTemporaryFile(suffix=".png", delete=False) as f:
                image.save(f.name)
                tmp_path = f.name
            mask_path = tmp_path.replace(".png", "_mask.tif")

            self.model.generate(tmp_path, mask_path)

            mask = cv2.imread(mask_path, cv2.IMREAD_GRAYSCALE)
            if mask is None or np.sum(mask) == 0:
                logger.warning("SAM 未检测到任何区域")
                return [], image

            boundaries = self._mask_to_boundaries(mask)
            overlay = self._create_overlay(img_np, mask)
            return boundaries, overlay
        finally:
            if tmp_path and os.path.exists(tmp_path):
                os.unlink(tmp_path)
            if mask_path and os.path.exists(mask_path):
                os.unlink(mask_path)

    def _mask_to_boundaries(self, mask):
        """SAM 掩码 → 轮廓列表（按对象 ID 拆分）"""
        min_area = 200
        boundaries = []

        for obj_id in np.unique(mask):
            if obj_id == 0:
                continue
            obj_mask = (mask == obj_id).astype(np.uint8) * 255
            contours, _ = cv2.findContours(obj_mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
            for cnt in contours:
                area = cv2.contourArea(cnt)
                if area < min_area:
                    continue
                epsilon = 0.005 * cv2.arcLength(cnt, True)
                approx = cv2.approxPolyDP(cnt, epsilon, True)
                coords = approx.reshape(-1, 2).tolist()
                if len(coords) < 3:
                    continue
                if coords[0] != coords[-1]:
                    coords.append(coords[0])
                boundaries.append({
                    "geojson": {"type": "Polygon", "coordinates": [[[c[0], c[1]] for c in coords]]},
                    "pixel_area": float(area),
                    "confidence": 0.92
                })

        boundaries.sort(key=lambda x: x["pixel_area"], reverse=True)
        logger.info(f"SAM 分割完成: {len(boundaries)} 个地块")
        return boundaries

    def _create_overlay(self, img_np, mask):
        """原图 + 掩码叠加"""
        overlay = img_np.astype(np.float32).copy()
        mask_bool = mask > 0
        green = np.array([67, 160, 71], dtype=np.float32)
        overlay[mask_bool] = overlay[mask_bool] * 0.5 + green * 0.5
        return Image.fromarray(overlay.astype(np.uint8))
