# 业务页面

每个业务领域单独建目录，例如 customer/index.vue。对应 API 放在 src/api/business/customer/index.ts。

业务公共组件放 components；仅单个领域使用的组件放领域目录内。business 子目录已经加入动态路由加载范围。

完整约定见 [业务模块开发约定](../../../../docs/业务模块开发约定.md)。
