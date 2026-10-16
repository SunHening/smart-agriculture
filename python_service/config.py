import os
from pathlib import Path


# =========================
# 项目根目录
# =========================

BASE_DIR = Path(__file__).resolve().parent


# =========================
# SAM 模型配置
# =========================

MODEL_TYPE = "vit_b"
CHECKPOINT_PATH = BASE_DIR / "models" / "sam_vit_b.pth"
DEVICE = os.getenv("SAM_DEVICE", "cuda")


# =========================
# 可选图像增强配置
# 默认关闭；启用前先评估耗时和分割指标
# =========================

SR_ENABLED = os.getenv("SR_ENABLED", "false").strip().lower() in ("1", "true", "yes", "on")
SR_SCALE = int(os.getenv("SR_SCALE", "2"))
SR_METHOD = os.getenv("SR_METHOD", "bicubic")


# =========================
# Flask 配置
# =========================

HOST = "0.0.0.0"
PORT = 5000
