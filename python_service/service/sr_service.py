"""Optional image enhancement adapter for SAM inputs.

The first implementation is a deterministic Bicubic baseline, not a learned
super-resolution model. Keep it behind a feature flag until evaluated.
"""
from dataclasses import dataclass

import numpy as np
from PIL import Image


@dataclass(frozen=True)
class EnhancementResult:
    image: np.ndarray
    enabled: bool
    method: str
    requested_scale: int


class SRService:
    def __init__(self, enabled: bool = False, scale: int = 2, method: str = "bicubic"):
        if scale not in (2, 4):
            raise ValueError("sr scale currently supports 2 or 4")
        if method != "bicubic":
            raise ValueError("only the bicubic baseline is implemented in stage 1")
        self.enabled = enabled
        self.scale = scale
        self.method = method

    def enhance(self, image: np.ndarray) -> EnhancementResult:
        if not isinstance(image, np.ndarray) or image.ndim != 3 or image.shape[2] != 3:
            raise ValueError("image must be an HxWx3 RGB numpy array")
        if image.dtype != np.uint8:
            raise ValueError("image must have dtype uint8")

        if not self.enabled:
            return EnhancementResult(
                image=np.ascontiguousarray(image),
                enabled=False,
                method="none",
                requested_scale=1,
            )

        height, width = image.shape[:2]
        pil_image = Image.fromarray(image, mode="RGB")
        resized = pil_image.resize(
            (width * self.scale, height * self.scale),
            resample=Image.Resampling.BICUBIC,
        )
        return EnhancementResult(
            image=np.ascontiguousarray(np.asarray(resized)),
            enabled=True,
            method=self.method,
            requested_scale=self.scale,
        )
