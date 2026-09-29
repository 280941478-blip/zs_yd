-- Historical V1/V2 stay immutable. Translate persisted class names for existing installations.
UPDATE infra_file_config
SET config = REPLACE(config, 'cn.iocoder.yudao.module.infra.framework.file.', 'cn.cdzs.module.infra.framework.file.')
WHERE config LIKE '%cn.iocoder.yudao.module.infra.framework.file.%';

UPDATE system_oauth2_client SET name = 'CDZS Starter'
WHERE id = 1 AND name IN ('Yudao Starter', '芋道源码');
