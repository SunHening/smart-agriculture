"""
SAM-Geo 农田检测微服务
POST /detect — 接收地图区域坐标，下载天地图影像，运行 SAM 分割，返回农田边界
"""
import os
import io
import json
import time
import base64
import logging
from datetime import datetime

from PIL import Image
from flask import Flask, request, jsonify
from flask_cors import CORS

from tianditu_downloader import download_tiles
from sam_geo_inference import SamGeoInference

logging.basicConfig(level=logging.INFO, format='%(asctime)s [%(levelname)s] %(message)s')
logger = logging.getLogger(__name__)

app = Flask(__name__)
CORS(app)

# 全局模型实例（延迟加载）
inference = None

def get_inference():
    global inference
    if inference is None:
        logger.info("正在加载 SAM-Geo 模型...")
        inference = SamGeoInference(
            model_type="vit_b",          # vit_h / vit_l / vit_b
            checkpoint_path=None,         # None = 自动下载 SAM 预训练权重
            device="cpu"                  # CPU 模式；有 GPU 改为 "cuda"
        )
        logger.info("SAM-Geo 模型加载完成")
    return inference


@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok", "timestamp": datetime.now().isoformat()})


@app.route("/detect", methods=["POST"])
def detect():
    """
    请求体:
    {
        "bbox": { "north": 36.965, "south": 36.960, "east": 110.808, "west": 110.802 },
        "zoom": 17,                          // 天地图缩放级别
        "tk": "b2d9cc5faa19adaf7b5c80c90e61e5d7",  // 天地图 API Key
        "field_name": "可选-农田名称",
        "method": "auto"                      // "auto" 自动分割 / "prompt" 提示点分割
    }

    返回:
    {
        "success": true,
        "boundaries": [   // 多个农田地块
            {
                "geojson": { "type": "Polygon", "coordinates": [...] },
                "area_mu": 5.48,
                "confidence": 0.95
            }
        ],
        "total_area_mu": 12.35,
        "image_base64": "...",   // 原图 + 掩码叠加的 base64
        "processing_time_ms": 3200
    }
    """
    t_start = time.time()

    try:
        data = request.get_json()
        if not data or "bbox" not in data:
            return jsonify({"success": False, "error": "缺少 bbox 参数"}), 400

        bbox = data["bbox"]
        zoom = data.get("zoom", 17)
        tk = data.get("tk", "b2d9cc5faa19adaf7b5c80c90e61e5d7")

        logger.info(f"检测区域: {bbox}, zoom={zoom}")

        # Step 1: 下载天地图影像
        logger.info("下载天地图影像...")
        image = download_tiles(bbox, zoom, tk)
        if image is None:
            return jsonify({"success": False, "error": "天地图影像下载失败"}), 500

        # Step 2: SAM-Geo 分割
        logger.info(f"运行 SAM-Geo 分割 (图像尺寸: {image.size})...")
        engine = get_inference()
        method = data.get("method", "auto")

        if method == "prompt":
            # 提示点模式：使用 bbox 中心点作为提示
            center_x = image.width // 2
            center_y = image.height // 2
            prompts = [{"type": "point", "data": [center_x, center_y], "label": 1}]
        else:
            prompts = None  # 自动模式

        boundaries, mask_overlay = engine.segment(image, prompts=prompts)

        # Step 3: 计算面积（像素 → 实际面积）
        # 天地图各级别分辨率(m/pixel): zoom 17 ≈ 1.2m/px
        resolutions = {15: 4.8, 16: 2.4, 17: 1.2, 18: 0.6, 19: 0.3}
        gsd = resolutions.get(zoom, 1.2)  # 默认 zoom 17

        for b in boundaries:
            pixel_area = b.get("pixel_area", 0)
            # 像素面积 × GSD² = 平方米
            sqm = pixel_area * (gsd ** 2)
            # 转换为亩 (1亩 = 666.67 m²)
            b["area_mu"] = round(sqm / 666.67, 4)
            b.pop("pixel_area", None)

        total_area_mu = round(sum(b["area_mu"] for b in boundaries), 4)

        # Step 4: 生成叠加图 base64
        overlay_b64 = None
        if mask_overlay is not None:
            buf = io.BytesIO()
            mask_overlay.save(buf, format="PNG")
            overlay_b64 = base64.b64encode(buf.getvalue()).decode("utf-8")

        elapsed = int((time.time() - t_start) * 1000)
        logger.info(f"检测完成: {len(boundaries)} 个地块, 总面积 {total_area_mu} 亩, 耗时 {elapsed}ms")

        return jsonify({
            "success": True,
            "boundaries": boundaries,
            "total_area_mu": total_area_mu,
            "image_base64": overlay_b64,
            "processing_time_ms": elapsed,
            "gsd": gsd
        })

    except Exception as e:
        logger.exception("检测失败")
        return jsonify({"success": False, "error": str(e)}), 500


@app.route("/detect-image", methods=["POST"])
def detect_image():
    """
    直接接收前端截图进行检测（绕过天地图服务端下载）
    请求体: { "image_base64": "data:image/png;base64,...", "gsd": 1.2 }
    返回: boundaries + overlay
    """
    t_start = time.time()
    try:
        data = request.get_json()
        img_b64 = data.get("image_base64", "")
        gsd = data.get("gsd", 1.2)

        # 解码 base64
        if img_b64.startswith("data:"):
            img_b64 = img_b64.split(",", 1)[1]
        img_bytes = base64.b64decode(img_b64)
        image = Image.open(io.BytesIO(img_bytes)).convert("RGB")

        logger.info(f"检测上传图像: {image.size}, GSD={gsd}m")

        engine = get_inference()
        boundaries, overlay = engine.segment(image)

        # 面积计算
        for b in boundaries:
            b["area_mu"] = round(b["pixel_area"] * gsd ** 2 / 666.67, 4)
            b.pop("pixel_area", None)
        total_area_mu = round(sum(b["area_mu"] for b in boundaries), 4)

        # overlay base64
        buf = io.BytesIO()
        overlay.save(buf, format="PNG")
        overlay_b64 = base64.b64encode(buf.getvalue()).decode()
        overlay_b64 = f"data:image/png;base64,{overlay_b64}"

        elapsed = int((time.time() - t_start) * 1000)
        logger.info(f"检测完成: {len(boundaries)} 地块, {total_area_mu} 亩, {elapsed}ms")

        return jsonify({
            "success": True,
            "boundaries": boundaries,
            "total_area_mu": total_area_mu,
            "field_count": len(boundaries),
            "processing_time_ms": elapsed,
            "overlay_base64": overlay_b64,
            "gsd": gsd
        })
    except Exception as e:
        logger.exception("图像检测失败")
        return jsonify({"success": False, "error": str(e)}), 500


@app.route("/test-detect", methods=["GET"])
def test_detect():
    """使用合成测试图像验证检测算法"""
    from PIL import Image, ImageDraw
    import io

    # 生成模拟农田卫星图像
    w, h = 512, 512
    img = Image.new("RGB", (w, h), (120, 100, 60))  # 棕色背景
    draw = ImageDraw.Draw(img)

    # 画多个绿色矩形模拟农田
    fields = [
        (50, 50, 200, 180),
        (220, 40, 420, 200),
        (40, 220, 200, 420),
        (220, 250, 410, 400),
        (340, 140, 460, 230),
    ]
    for x1, y1, x2, y2 in fields:
        draw.rectangle([x1, y1, x2, y2], fill=(80, 145, 65), outline=(40, 100, 30), width=3)

    # 画一条路
    draw.rectangle([180, 0, 195, 512], fill=(150, 140, 120))

    t_start = time.time()
    engine = get_inference()
    boundaries, overlay = engine.segment(img)

    # 面积计算
    gsd = 1.2  # zoom 17
    total = 0
    for b in boundaries:
        b["area_mu"] = round(b["pixel_area"] * gsd ** 2 / 666.67, 4)
        b.pop("pixel_area", None)
        total += b["area_mu"]

    # overlay base64
    buf_overlay = io.BytesIO()
    overlay.save(buf_overlay, format="PNG")
    overlay_b64 = base64.b64encode(buf_overlay.getvalue()).decode()
    overlay_b64 = f"data:image/png;base64,{overlay_b64}"

    # 原图 base64
    buf_orig = io.BytesIO()
    img.save(buf_orig, format="PNG")
    orig_b64 = base64.b64encode(buf_orig.getvalue()).decode()
    orig_b64 = f"data:image/png;base64,{orig_b64}"

    elapsed = int((time.time() - t_start) * 1000)

    return jsonify({
        "success": True,
        "boundaries": boundaries,
        "total_area_mu": round(total, 4),
        "field_count": len(boundaries),
        "processing_time_ms": elapsed,
        "image_base64": overlay_b64,
        "original_base64": orig_b64,
        "method": "SAM-Geo"
    })


if __name__ == "__main__":
    # use_reloader=False：关闭热重载。SAM 推理会导入 torch/samgeo 等大库，
    # 热重载会误判这些库文件变化并中断正在处理的请求（表现为 Connection reset）。
    app.run(host="0.0.0.0", port=5000, debug=True, use_reloader=False)
