from io import BytesIO

import numpy as np
from PIL import Image
from flask import Flask, jsonify, request, send_file
from flask_cors import CORS

from config import CHECKPOINT_PATH, DEVICE, MODEL_TYPE, SR_ENABLED, SR_METHOD, SR_SCALE
from service.sam_runtime import SamRuntime
from service.session_manager import SessionManager
from service.segmentation_service import SegmentationService
from service.sr_service import SRService


app = Flask(__name__)
CORS(app)


# ============================================================
# 核心服务初始化
# ============================================================

runtime = SamRuntime(
    checkpoint_path=CHECKPOINT_PATH,
    model_type=MODEL_TYPE,
    device=DEVICE,
)

session_manager = SessionManager()

sr_service = SRService(
    enabled=SR_ENABLED,
    scale=SR_SCALE,
    method=SR_METHOD,
)

segmentation_service = SegmentationService(
    runtime=runtime,
    session_manager=session_manager,
    sr_service=sr_service,
)


# ============================================================
# Health
# ============================================================

@app.get("/health")
def health():
    return jsonify({
        "status": "ok",
        "service": "sam-segmentation",
    })


# ============================================================
# 创建 SAM Session
# ============================================================

@app.post("/sam/sessions")
def create_session():
    """
    创建一个 SAM ImageSession。

    请求：
        multipart/form-data
        image=<图片文件>

    返回：
        {
            "session_id": "...",
            "width": 1920,
            "height": 1080,
            "original_size": [1080, 1920]
        }
    """

    if "image" not in request.files:
        return jsonify({
            "error": "缺少 image 文件"
        }), 400

    file = request.files["image"]

    if file.filename == "":
        return jsonify({
            "error": "图片文件名为空"
        }), 400

    try:
        # ----------------------------------------------------
        # 1. 读取图片
        # ----------------------------------------------------

        image = Image.open(file.stream).convert("RGB")

        image_np = np.array(image)

        # ----------------------------------------------------
        # 2. 创建 Session
        # ----------------------------------------------------

        result = segmentation_service.create_session(
            image=image_np
        )

        # ----------------------------------------------------
        # 3. 返回 Session 信息
        # ----------------------------------------------------

        return jsonify(result), 201

    except Exception as e:
        app.logger.exception("创建 SAM Session 失败")

        return jsonify({
            "error": str(e)
        }), 500


# ============================================================
# 查询 Session
# ============================================================

@app.get("/sam/sessions/<session_id>")
def get_session(session_id: str):
    """
    查询 Session 是否存在以及基本信息。
    """

    session = session_manager.get(session_id)

    if session is None:
        return jsonify({
            "error": "Session 不存在"
        }), 404

    return jsonify({
        "session_id": session.session_id,
        "width": session.width,
        "height": session.height,
        "original_size": list(session.original_size),
        "sam_width": session.sam_width,
        "sam_height": session.sam_height,
        "sr_enabled": session.sr_enabled,
        "sr_method": session.sr_method,
        "sr_scale_x": session.sr_scale_x,
        "sr_scale_y": session.sr_scale_y,
        "created_at": session.created_at.isoformat(),
        "last_accessed_at": session.last_accessed_at.isoformat(),
    })


# ============================================================
# 点提示预测
# ============================================================

@app.post("/sam/sessions/<session_id>/predictions")
def predict_points(session_id: str):
    """
    根据 Session + 点提示生成 raw mask。

    请求 JSON：

    {
        "points": [
            {
                "x": 500,
                "y": 300,
                "label": 1
            },
            {
                "x": 700,
                "y": 400,
                "label": 0
            }
        ],
        "multimask_output": false
    }

    返回：
        image/png

    PNG：
        0   = 背景
        255 = mask
    """

    # --------------------------------------------------------
    # 1. 检查 Session
    # --------------------------------------------------------

    session = session_manager.get(session_id)

    if session is None:
        return jsonify({
            "error": "Session 不存在"
        }), 404

    # --------------------------------------------------------
    # 2. 读取 JSON
    # --------------------------------------------------------

    data = request.get_json(silent=True)

    if data is None:
        return jsonify({
            "error": "请求必须是 JSON"
        }), 400

    points = data.get("points")

    if not isinstance(points, list):
        return jsonify({
            "error": "points 必须是数组"
        }), 400

    if len(points) == 0:
        return jsonify({
            "error": "至少需要一个点"
        }), 400

    # --------------------------------------------------------
    # 3. 转换点坐标
    # --------------------------------------------------------

    point_coords = []
    point_labels = []

    for point in points:

        if not isinstance(point, dict):
            return jsonify({
                "error": "每个 point 必须是对象"
            }), 400

        if "x" not in point or "y" not in point:
            return jsonify({
                "error": "point 必须包含 x 和 y"
            }), 400

        x = float(point["x"])
        y = float(point["y"])

        label = int(point.get("label", 1))

        if label not in (0, 1):
            return jsonify({
                "error": "label 必须是 0 或 1"
            }), 400

        # ----------------------------------------------------
        # 坐标范围检查
        # ----------------------------------------------------

        if x < 0 or x >= session.width:
            return jsonify({
                "error": f"x 超出图片范围: {x}"
            }), 400

        if y < 0 or y >= session.height:
            return jsonify({
                "error": f"y 超出图片范围: {y}"
            }), 400

        point_coords.append([x, y])
        point_labels.append(label)

    point_coords = np.asarray(
        point_coords,
        dtype=np.float32
    )

    point_labels = np.asarray(
        point_labels,
        dtype=np.int32
    )

    # --------------------------------------------------------
    # 4. multimask_output
    # --------------------------------------------------------

    multimask_output = bool(
        data.get("multimask_output", False)
    )

    # --------------------------------------------------------
    # 5. 调用业务层
    # --------------------------------------------------------

    try:

        result = segmentation_service.predict_points(
            session_id=session_id,
            point_coords=point_coords,
            point_labels=point_labels,
            multimask_output=multimask_output,
        )

    except ValueError as e:

        return jsonify({
            "error": str(e)
        }), 400

    except Exception as e:

        app.logger.exception("SAM prediction 失败")

        return jsonify({
            "error": str(e)
        }), 500

    # --------------------------------------------------------
    # 6. 获取 raw mask
    # --------------------------------------------------------

    mask = result["mask"]

    # torch Tensor → numpy
    if hasattr(mask, "detach"):
        mask = mask.detach().cpu().numpy()

    mask = np.asarray(mask)

    # --------------------------------------------------------
    # 7. 去除多余维度
    # --------------------------------------------------------

    mask = np.squeeze(mask)

    if mask.ndim != 2:
        return jsonify({
            "error": f"mask 维度错误: {mask.shape}"
        }), 500

    # --------------------------------------------------------
    # 8. 恢复到原始截图尺寸（最近邻，避免二值 mask 被插值模糊）
    # --------------------------------------------------------

    if mask.shape != (session.height, session.width):
        mask_image_for_resize = Image.fromarray(mask.astype(np.uint8) * 255, mode="L")
        mask_image_for_resize = mask_image_for_resize.resize(
            (session.width, session.height),
            resample=Image.Resampling.NEAREST,
        )
        mask = np.asarray(mask_image_for_resize) > 0

    # --------------------------------------------------------
    # 9. 转换成 0 / 255
    # --------------------------------------------------------

    mask_uint8 = mask.astype(np.uint8) * 255

    # --------------------------------------------------------
    # 9. 编码成 PNG
    # --------------------------------------------------------

    mask_image = Image.fromarray(
        mask_uint8,
        mode="L"
    )

    buffer = BytesIO()

    mask_image.save(
        buffer,
        format="PNG"
    )

    buffer.seek(0)

    # --------------------------------------------------------
    # 10. 返回 mask
    # --------------------------------------------------------

    response = send_file(
        buffer,
        mimetype="image/png",
        as_attachment=False,
    )

    # 把一些推理信息放到 HTTP Header
    response.headers["X-SAM-Score"] = str(
        result.get("score", "")
    )

    response.headers["X-SAM-Width"] = str(
        session.width
    )

    response.headers["X-SAM-Height"] = str(
        session.height
    )

    return response



# ============================================================
# 兼容现有 Vue Point 工作流，并复用 SAM Session embedding
# ============================================================

@app.post("/predict-point")
def predict_point_compat():
    """兼容旧版 Vue 请求；首个请求创建 Session，后续请求复用 embedding。"""
    import base64
    import time
    import cv2

    started_at = time.perf_counter()
    data = request.get_json(silent=True) or {}
    session_id = data.get("session_id")
    image_base64 = data.get("image_base64")
    session = session_manager.get(session_id) if session_id else None

    try:
        # 第一次请求或 Session 失效时，使用前端截图创建 Session。
        if session is None:
            if not image_base64:
                return jsonify({"success": False, "error": "缺少 image_base64 或有效 session_id"}), 400
            encoded = image_base64.split(",", 1)[1] if "," in image_base64 else image_base64
            image_bytes = base64.b64decode(encoded, validate=True)
            source_image = Image.open(BytesIO(image_bytes)).convert("RGB")
            created = segmentation_service.create_session(np.asarray(source_image))
            session_id = created["session_id"]
            session = session_manager.get(session_id)

        if session is None:
            return jsonify({"success": False, "error": "Session 创建失败"}), 500

        x = float(data["x"])
        y = float(data["y"])
        label = int(data.get("label", 1))
        if label not in (0, 1):
            return jsonify({"success": False, "error": "label 必须是 0 或 1"}), 400
        if not (0 <= x < session.width and 0 <= y < session.height):
            return jsonify({"success": False, "error": "提示点超出原始截图范围"}), 400

        prediction = segmentation_service.predict_points(
            session_id=session_id,
            point_coords=np.asarray([[x, y]], dtype=np.float32),
            point_labels=np.asarray([label], dtype=np.int32),
            multimask_output=False,
        )
        mask = prediction["mask"]
        if hasattr(mask, "detach"):
            mask = mask.detach().cpu().numpy()
        mask = np.squeeze(np.asarray(mask)).astype(np.uint8)

        # SAM 输出对应增强图尺寸；前端边界仍使用原始截图像素坐标。
        if mask.shape != (session.height, session.width):
            mask = np.asarray(
                Image.fromarray(mask * 255, mode="L").resize(
                    (session.width, session.height),
                    resample=Image.Resampling.NEAREST,
                )
            ) > 0
        mask_uint8 = (mask.astype(np.uint8) * 255)
        contours, _ = cv2.findContours(
            mask_uint8, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE
        )

        boundaries = []
        for contour in contours:
            if len(contour) < 3 or cv2.contourArea(contour) < 4:
                continue
            epsilon = max(1.0, 0.0015 * cv2.arcLength(contour, True))
            simplified = cv2.approxPolyDP(contour, epsilon, True)
            coords = simplified.reshape(-1, 2).astype(float).tolist()
            if len(coords) >= 3:
                coords.append(coords[0])
                boundaries.append({
                    "geojson": {
                        "type": "Polygon",
                        "coordinates": [coords],
                    }
                })

        return jsonify({
            "success": True,
            "session_id": session_id,
            "boundaries": boundaries,
            "confidence": float(prediction.get("score", 0.0)),
            "processing_time_ms": round((time.perf_counter() - started_at) * 1000, 2),
            "width": session.width,
            "height": session.height,
            "sr_enabled": session.sr_enabled,
            "sr_scale_x": session.sr_scale_x,
            "sr_scale_y": session.sr_scale_y,
        })
    except (KeyError, TypeError, ValueError) as exc:
        return jsonify({"success": False, "error": str(exc)}), 400
    except Exception as exc:
        app.logger.exception("兼容 Point 预测失败")
        return jsonify({"success": False, "error": str(exc)}), 500


# ============================================================
# 删除 Session
# ============================================================

@app.delete("/sam/sessions/<session_id>")
def delete_session(session_id: str):
    """
    删除 SAM Session。
    """

    deleted = segmentation_service.delete_session(
        session_id
    )

    if not deleted:
        return jsonify({
            "error": "Session 不存在"
        }), 404

    return jsonify({
        "message": "Session 已删除",
        "session_id": session_id,
    })


# ============================================================
# 启动
# ============================================================

if __name__ == "__main__":
    app.run(
        host="0.0.0.0",
        port=5000,
        debug=True,
    )