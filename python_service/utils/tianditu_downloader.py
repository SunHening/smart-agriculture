"""
天地图卫星影像下载器
根据 bbox 和 zoom 级别，下载天地图 WMTS 瓦片并拼接为完整图像
"""
import math
import logging
from io import BytesIO

import numpy as np
from PIL import Image
import requests

logger = logging.getLogger(__name__)

# 天地图 WMTS 瓦片 URL 模板
TILE_URL = (
    "https://t{s}.tianditu.gov.cn/img_w/wmts"
    "?service=wmts&request=getTile&version=1.0.0"
    "&layer=img&style=default&tilematrixset=w"
    "&format=tiles"
    "&tilematrix={z}&tilerow={y}&tilecol={x}"
    "&tk={tk}"
)

SUB_DOMAINS = ["0", "1", "2", "3", "4", "5", "6", "7"]
TILE_SIZE = 256
MAX_TILES = 100  # 最大下载瓦片数，防止过大


def latlon_to_tile(lat, lon, zoom):
    """经纬度 → 瓦片坐标"""
    lat_rad = math.radians(lat)
    n = 2.0 ** zoom
    x = int((lon + 180.0) / 360.0 * n)
    y = int((1.0 - math.asinh(math.tan(lat_rad)) / math.pi) / 2.0 * n)
    return x, y


def tile_to_latlon(x, y, zoom):
    """瓦片坐标 → 经纬度（左上角）"""
    n = 2.0 ** zoom
    lon = x / n * 360.0 - 180.0
    lat_rad = math.atan(math.sinh(math.pi * (1.0 - 2.0 * y / n)))
    lat = math.degrees(lat_rad)
    return lat, lon


def download_tiles(bbox, zoom, tk):
    """
    下载天地图影像瓦片并拼接
    bbox: { north, south, east, west }
    zoom: 天地图缩放级别 (3-18)
    tk: 天地图 API Key
    """
    north, south = bbox["north"], bbox["south"]
    east, west = bbox["east"], bbox["west"]

    # 计算需要的瓦片范围
    x_min, y_min = latlon_to_tile(north, west, zoom)
    x_max, y_max = latlon_to_tile(south, east, zoom)

    # 调整顺序
    if x_min > x_max:
        x_min, x_max = x_max, x_min
    if y_min > y_max:
        y_min, y_max = y_max, y_min

    tile_count = (x_max - x_min + 1) * (y_max - y_min + 1)
    if tile_count > MAX_TILES:
        logger.warning(f"瓦片数 {tile_count} 超过限制 {MAX_TILES}，请缩小区域或降低 zoom")
        return None

    logger.info(f"下载瓦片: x=[{x_min},{x_max}], y=[{y_min},{y_max}], 共 {tile_count} 张")

    # 创建空白画布
    canvas_w = (x_max - x_min + 1) * TILE_SIZE
    canvas_h = (y_max - y_min + 1) * TILE_SIZE
    canvas = Image.new("RGB", (canvas_w, canvas_h))

    # 逐片下载
    domain_idx = 0
    for y in range(y_min, y_max + 1):
        for x in range(x_min, x_max + 1):
            url = TILE_URL.format(
                s=SUB_DOMAINS[domain_idx % len(SUB_DOMAINS)],
                z=zoom, y=y, x=x, tk=tk
            )
            domain_idx += 1

            try:
                headers = {
                    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                    "Referer": "http://localhost:5173"
                }
                resp = requests.get(url, timeout=10, headers=headers)
                resp.raise_for_status()
                tile_img = Image.open(BytesIO(resp.content))
                canvas.paste(tile_img, ((x - x_min) * TILE_SIZE, (y - y_min) * TILE_SIZE))
            except Exception as e:
                logger.error(f"下载瓦片失败 ({x},{y}): {e}")
                # 填充灰色
                gray = Image.new("RGB", (TILE_SIZE, TILE_SIZE), (128, 128, 128))
                canvas.paste(gray, ((x - x_min) * TILE_SIZE, (y - y_min) * TILE_SIZE))

    logger.info(f"影像拼接完成: {canvas.size}")
    return canvas
