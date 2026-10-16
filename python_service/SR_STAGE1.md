# SAM 输入图像增强（阶段 1）

## 当前实现范围

- Point 路径：Vue 首次发起提示时将当前地图截图上传到 `POST /sam/sessions`，Python 端创建 embedding；后续 Hover/点击提示复用 `session_id`，不重复执行 image encoder。
- 增强入口：`SegmentationService.create_session()` 在 `SamRuntime.encode_image()` 前调用 `SRService`。
- 坐标约定：前端提示点和返回边界统一使用原始截图像素坐标。Python 将点坐标映射到 SAM 增强图坐标，并将 mask 恢复到原始截图尺寸。
- 当前增强仅实现可选 Bicubic 插值基线，不是学习型超分模型。
- Automatic/SAM-Geo 路径尚未接入：当前仓库的 `python_service/app.py` 没有实现 Spring 控制器所调用的 `/detect` 和 `/detect-image` 路由；不要把 Point Session 路径当成 Automatic 已完成。

## 启动

在 `python_service` 目录使用现有虚拟环境安装依赖：

```bash
pip install -r requirements.txt
```

默认关闭增强，保持原有图像输入行为：

```bash
# Linux/macOS
export SR_ENABLED=false
python app.py
```

开启 2× Bicubic 基线：

```bash
# Linux/macOS
export SR_ENABLED=true
export SR_SCALE=2
export SR_METHOD=bicubic
python app.py
```

Windows PowerShell：

```powershell
$env:SR_ENABLED = "true"
$env:SR_SCALE = "2"
$env:SR_METHOD = "bicubic"
python app.py
```

## 手工验证

1. 启动 Python 服务，确认 `GET http://127.0.0.1:5000/health` 返回成功。
2. 打开地图 Point 模式，确认首个 Hover/点击请求先创建 Session。
3. 检查后续 `/predict-point` 请求带有 `session_id`；同一张截图的多个提示应复用该 ID。
4. 确认返回的 `boundaries` 坐标落在原始截图宽高范围内，地图叠加层与提示点位置一致。
5. 分别用 `SR_ENABLED=false` 和 `true` 测试同一截图，记录首个请求耗时、后续提示耗时和边界质量。

## 注意

- 2× 插值会使 SAM 编码的像素数量增至约 4 倍，可能增加图像编码时间和显存占用；它不保证分割精度提升。
- Session 当前存放在进程内存中，重启服务会失效；生产环境还需要 TTL/清理策略和并发压力测试。
- Automatic 路径需要先补齐独立的 SAM-Geo 推理实现，再在该路径中复用 `SRService`；不能简单地把 Point 的单 mask 输出当成自动农田检测。
