from io import BytesIO

import numpy as np
from PIL import Image
from flask import Flask, jsonify, request, send_file
from flask_cors import CORS

from config import CHECKPOINT_PATH, DEVICE, MODEL_TYPE
from service.sam_runtime import SamRuntime
from service.session_manager import SessionManager
from service.segmentation_service import SegmentationService


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

segmentation_service = SegmentationService(
    runtime=runtime,
    session_manager=session_manager,
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
    # 8. 转换成 0 / 255
    # --------------------------------------------------------

    mask_uint8 = (
        mask.astype(np.uint8) * 255
    )

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