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

DEVICE = "cuda"


# =========================
# Flask 配置
# =========================

HOST = "0.0.0.0"

PORT = 5000