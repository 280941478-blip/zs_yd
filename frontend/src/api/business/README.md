# 业务接口

按业务领域建立目录，每个 index.ts 维护该领域 API 与 TypeScript 类型。统一使用 @/config/axios 的 request，不自行拼接 token、租户头或服务地址。

对应页面放 src/views/business；底座 system、infra 的接口保留原目录。
