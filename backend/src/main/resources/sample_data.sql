-- ========================================
-- 示例影像检测数据（可选执行）
-- ========================================

-- 示例遥感影像任务
INSERT INTO image_task (user_id, field_id, image_type, original_filename, image_path, resolution, gsd, crs, longitude, latitude, image_width, image_height, status) VALUES
(2, 1, 'REMOTE_SENSING', 'sentinel_2_20250715.tif', '/uploads/sample/sentinel_2_20250715.tif', 10.0000, 10.0000, 'EPSG:4326', 116.4074, 39.9042, 1024, 1024, 'COMPLETED');

-- 示例检测结果
INSERT INTO detection_result (image_task_id, field_id, boundary_geojson, detected_area, confidence, coverage_rate, processing_time_ms, model_name, model_version, status) VALUES
(1, 1, '{"type":"Polygon","coordinates":[[[116.406,39.905],[116.409,39.905],[116.409,39.903],[116.406,39.903],[116.406,39.905]]]}', 5.4800, 0.9520, 0.8700, 3200, 'DeepLabV3Plus', 'v1.0.0', 'SUCCESS');

-- 更新农田检测面积
UPDATE farm_field SET detected_area = 5.4800, boundary_geojson = '{"type":"Polygon","coordinates":[[[116.406,39.905],[116.409,39.905],[116.409,39.903],[116.406,39.903],[116.406,39.905]]]}', last_detection_id = 1 WHERE id = 1;
